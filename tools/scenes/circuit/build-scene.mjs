// Night circuit — builds the one GLB of the homepage showpiece scene:
// the Car Concept (Khronos glTF sample, CC BY 4.0) on a procedural start grid.
//   node build-scene.mjs <CarConcept.glb> <texturesDir> <out.glb> [--meshopt]
// Changes made to the car: the third-party logos are replaced (number plate, tyre
// sidewalls), the largest textures are resized, the paint's flake normal is toned down
// (under small bright lamps it read as grain). Everything else in the file is generated here.
import fs from 'node:fs';
import path from 'node:path';
import { NodeIO, PropertyType, TextureInfo } from '@gltf-transform/core';
import { ALL_EXTENSIONS, KHRMaterialsEmissiveStrength, KHRMaterialsUnlit, KHRTextureTransform, EXTMeshoptCompression } from '@gltf-transform/extensions';
import { dedup, prune, quantize, reorder, meshopt, weld } from '@gltf-transform/functions';
import { MeshoptEncoder, MeshoptDecoder } from 'meshoptimizer';
import { execFileSync } from 'node:child_process';
import { buildSet, EMIT, MAT, sky, rng } from './circuit-def.mjs';

const [carPath, texDir, outPath] = process.argv.slice(2);
const useMeshopt = process.argv.includes('--meshopt');
await MeshoptEncoder.ready; await MeshoptDecoder.ready;
const io = new NodeIO().registerExtensions(ALL_EXTENSIONS)
  .registerDependencies({ 'meshopt.encoder': MeshoptEncoder, 'meshopt.decoder': MeshoptDecoder });
const doc = await io.read(carPath);
const root = doc.getRoot(), scene = root.listScenes()[0], buf = root.listBuffers()[0];
const read = (f) => fs.readFileSync(path.join(texDir, f));

// ── Car textures ──
const tex = root.listTextures();
const bySize = (w, h) => tex.filter((t) => { const s = t.getSize(); return s && s[0] === w && s[1] === h; });
// Number plate / badge artwork (the 512x128 wordmark used by the plate and as an emissive mask).
const plate = bySize(512, 128).find((t) => t.listParents().some((p) => p.getName && p.getName() === 'License'));
plate.setImage(read('plate.png')).setMimeType('image/png');
// Tyre sidewall: base colour (logos) and normal map (embossed logos).
const side = root.listMaterials().find((m) => m.getName() === 'Tireside');
side.getBaseColorTexture().setImage(read('tire_color.png')).setMimeType('image/png');
side.getNormalTexture().setImage(read('tire_normal.png')).setMimeType('image/png');
for (const m of root.listMaterials()) if (/^Paint \d Carmine$/.test(m.getName())) m.setNormalScale(m.getNormalScale() * 0.35);
// Largest textures: resize / re-encode with ffmpeg (no logos in these).
function reencode(t, args, mime) {
  const tmpIn = path.join(texDir, '_in.png'), tmpOut = path.join(texDir, mime === 'image/jpeg' ? '_out.jpg' : '_out.png');
  fs.writeFileSync(tmpIn, t.getImage());
  execFileSync('ffmpeg', ['-loglevel', 'error', '-y', '-i', tmpIn, ...args, tmpOut]);
  t.setImage(fs.readFileSync(tmpOut)).setMimeType(mime);
  fs.unlinkSync(tmpIn); fs.unlinkSync(tmpOut);
}
for (const t of tex) {
  const s = t.getSize(), bytes = t.getImage().byteLength;
  if (t === plate || !s) continue;
  if (s[0] === 2048) reencode(t, ['-vf', 'scale=1024:1024:flags=lanczos', '-q:v', '3'], 'image/jpeg');
  else if (bytes > 90000 && t.getMimeType() === 'image/png') reencode(t, ['-q:v', '2'], 'image/jpeg');
}

// ── Set geometry, merged per material ──
class Geo {
  constructor() { this.p = []; this.n = []; this.uv = []; this.i = []; }
  quad(a, b, c, d, uvs) {
    const e1 = [b[0] - a[0], b[1] - a[1], b[2] - a[2]], e2 = [d[0] - a[0], d[1] - a[1], d[2] - a[2]];
    let n = [e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]];
    const l = Math.hypot(...n) || 1; n = n.map((x) => x / l);
    const base = this.p.length / 3;
    for (const v of [a, b, c, d]) { this.p.push(...v); this.n.push(...n); }
    for (const t of (uvs || [[0, 0], [1, 0], [1, 1], [0, 1]])) this.uv.push(...t);
    this.i.push(base, base + 1, base + 2, base, base + 2, base + 3);
  }
  box([cx, cy, cz], [sx, sy, sz]) {
    const x0 = cx - sx / 2, x1 = cx + sx / 2, y0 = cy - sy / 2, y1 = cy + sy / 2, z0 = cz - sz / 2, z1 = cz + sz / 2;
    this.quad([x0, y1, z1], [x1, y1, z1], [x1, y1, z0], [x0, y1, z0]);   // +Y
    this.quad([x0, y0, z0], [x1, y0, z0], [x1, y0, z1], [x0, y0, z1]);   // -Y
    this.quad([x1, y0, z1], [x1, y0, z0], [x1, y1, z0], [x1, y1, z1]);   // +X
    this.quad([x0, y0, z0], [x0, y0, z1], [x0, y1, z1], [x0, y1, z0]);   // -X
    this.quad([x0, y0, z1], [x1, y0, z1], [x1, y1, z1], [x0, y1, z1]);   // +Z
    this.quad([x1, y0, z0], [x0, y0, z0], [x0, y1, z0], [x1, y1, z0]);   // -Z
  }
  panel(c, u, v, uv) {
    const P = (su, sv) => [c[0] + su * u[0] + sv * v[0], c[1] + su * u[1] + sv * v[1], c[2] + su * u[2] + sv * v[2]];
    const [u0, v0, u1, v1] = uv || [0, 0, 1, 1];                        // image top (v0) at +v
    this.quad(P(-1, -1), P(1, -1), P(1, 1), P(-1, 1), [[u0, v1], [u1, v1], [u1, v0], [u0, v0]]);
  }
}
function addMesh(name, geo, material, colors) {
  const prim = doc.createPrimitive()
    .setAttribute('POSITION', doc.createAccessor().setType('VEC3').setArray(new Float32Array(geo.p)).setBuffer(buf))
    .setAttribute('NORMAL', doc.createAccessor().setType('VEC3').setArray(new Float32Array(geo.n)).setBuffer(buf))
    .setAttribute('TEXCOORD_0', doc.createAccessor().setType('VEC2').setArray(new Float32Array(geo.uv)).setBuffer(buf))
    .setIndices(doc.createAccessor().setType('SCALAR').setArray(new Uint32Array(geo.i)).setBuffer(buf))
    .setMaterial(material);
  if (colors) prim.setAttribute('COLOR_0', doc.createAccessor().setType(colors.length === geo.p.length ? 'VEC3' : 'VEC4').setArray(new Float32Array(colors)).setBuffer(buf));
  const node = doc.createNode(name).setMesh(doc.createMesh(name).addPrimitive(prim));
  scene.addChild(node);
  return node;
}

const set = buildSet();
const emissiveStrength = doc.createExtension(KHRMaterialsEmissiveStrength);
const groups = {};
for (const b of set.boxes) (groups[b.mat] = groups[b.mat] || new Geo()).box(b.c, b.s);
for (const [name, geo] of Object.entries(groups)) {
  const m = MAT[name];
  addMesh('Set_' + name, geo, doc.createMaterial('Set ' + name)
    .setBaseColorFactor([...m.color, 1]).setRoughnessFactor(m.rough).setMetallicFactor(m.metal));
}
const lit = {}, emissiveTex = {};
for (const p of set.panels) (lit[p.emit] = lit[p.emit] || new Geo()).panel(p.c, p.u, p.v, p.uv);
for (const [name, geo] of Object.entries(lit)) {
  const e = EMIT[name];
  const m = doc.createMaterial('Light ' + name)
    .setBaseColorFactor([0, 0, 0, 1]).setRoughnessFactor(1).setMetallicFactor(0).setDoubleSided(true)
    .setEmissiveFactor(e.tex ? [1, 1, 1] : e.color)
    .setExtension('KHR_materials_emissive_strength', emissiveStrength.createEmissiveStrength().setEmissiveStrength(e.strength));
  if (e.tex) {
    emissiveTex[e.tex] = emissiveTex[e.tex] || doc.createTexture(e.tex).setImage(read(e.tex)).setMimeType(e.tex.endsWith('.png') ? 'image/png' : 'image/jpeg');
    m.setEmissiveTexture(emissiveTex[e.tex]);
  }
  addMesh('Light_' + name, geo, m);
}

// ── Wet asphalt: one large quad (colour and dampness tiled every 16 m, grain every 2 m),
//    and the standing water as a second mirror-flat layer 6 mm above it, around the grid ──
{
  const G = 720, T = 16, geo = new Geo();
  geo.quad([-G, 0, G], [G, 0, G], [G, 0, -G], [-G, 0, -G], [[-G / T, G / T], [G / T, G / T], [G / T, -G / T], [-G / T, -G / T]]);
  const t = (f, mime) => doc.createTexture(f).setImage(read(f)).setMimeType(mime);
  const transform = doc.createExtension(KHRTextureTransform);
  const asphalt = doc.createMaterial('Wet asphalt')
    .setBaseColorTexture(t('asphalt_color.jpg', 'image/jpeg')).setBaseColorFactor([0.6, 0.6, 0.64, 1])
    .setMetallicRoughnessTexture(t('asphalt_orm.jpg', 'image/jpeg')).setRoughnessFactor(1).setMetallicFactor(0)
    .setNormalTexture(t('asphalt_normal.jpg', 'image/jpeg')).setNormalScale(1);
  asphalt.getNormalTextureInfo().setExtension('KHR_texture_transform', transform.createTransform().setScale([8, 8]));
  addMesh('Asphalt', geo, asphalt);

  const X = 13, Z = 96, water = new Geo(), y = 0.006;
  water.quad([-X, y, Z], [X, y, Z], [X, y, -Z], [-X, y, -Z], [[-X / T, Z / T], [X / T, Z / T], [X / T, -Z / T], [-X / T, -Z / T]]);
  addMesh('Water', water, doc.createMaterial('Standing water')
    .setBaseColorTexture(t('puddles.png', 'image/png')).setAlphaMode('MASK').setAlphaCutoff(0.5)
    .setRoughnessFactor(0.03).setMetallicFactor(0));
}

// ── Sky dome: unlit, vertex colours from the same sky() the environment map uses ──
{
  const R = 700, SEG = 64, RINGS = 28, geo = new Geo(), colors = [];
  const ring = (k) => { const t = k / RINGS; return -0.06 + (Math.PI / 2 + 0.06) * t * t; };   // dense near the horizon
  for (let k = 0; k <= RINGS; k++) for (let s = 0; s <= SEG; s++) {
    const el = ring(k), az = (s / SEG) * 2 * Math.PI;
    const d = [Math.sin(az) * Math.cos(el), Math.sin(el), Math.cos(az) * Math.cos(el)];
    geo.p.push(d[0] * R, d[1] * R, d[2] * R); geo.n.push(-d[0], -d[1], -d[2]); geo.uv.push(s / SEG, k / RINGS);
    colors.push(...sky(d[0], d[1], d[2]));
  }
  for (let k = 0; k < RINGS; k++) for (let s = 0; s < SEG; s++) {
    const a = k * (SEG + 1) + s, b = a + 1, c = a + SEG + 1, d = c + 1;
    geo.i.push(a, b, c, b, d, c);
  }
  const unlit = doc.createExtension(KHRMaterialsUnlit);
  addMesh('Sky', geo, doc.createMaterial('Sky').setBaseColorFactor([1, 1, 1, 1]).setDoubleSided(true)
    .setExtension('KHR_materials_unlit', unlit.createUnlit()), colors);
}

// ── Rain: a volume of thin crossed streaks around the grid, unlit and blended, the tail
//    fading along fade.png's alpha (uv v = 0 at the bright bottom; the glTF ubershader
//    ignores vertex alpha). The mesh repeats every RAIN_H metres in Y and the GLB's one
//    animation ("Rain") slides it down one period in a loop, so the fall is seamless.
//    Blended materials cast no shadow in Filament, so the key light is not affected. ──
const unlit = doc.createExtension(KHRMaterialsUnlit);
const fade = doc.createTexture('fade').setImage(read('fade.png')).setMimeType('image/png');
const fadeMaterial = (name, rgba) => {
  const m = doc.createMaterial(name).setBaseColorFactor(rgba).setBaseColorTexture(fade).setAlphaMode('BLEND')
    .setDoubleSided(true).setExtension('KHR_materials_unlit', unlit.createUnlit());
  m.getBaseColorTextureInfo().setWrapS(TextureInfo.WrapMode.CLAMP_TO_EDGE).setWrapT(TextureInfo.WrapMode.CLAMP_TO_EDGE);
  return m;
};
{
  // The camera orbits the car at 6.5–7.3 m (up to ×1.9 for the page's framing): a streak
  // a metre from the lens would fill the frame, so the orbit's annulus stays dry and the
  // rain lives around the car (inner disk) and over the far grid (outer ring).
  const RAIN_H = 6, X = 16, Z0 = -14, Z1 = 22, L = 0.3, W = 0.007, R0 = 4.6, R1 = 14.2, geo = new Geo();
  const CX = 0, CZ = 0.25, r = rng(23);
  const streak = (x, y, z) => {
    geo.quad([x - W, y, z], [x + W, y, z], [x + W, y + L, z], [x - W, y + L, z]);   // faces ±Z
    geo.quad([x, y, z - W], [x, y, z + W], [x, y + L, z + W], [x, y + L, z - W]);   // faces ±X
  };
  let inner = 0, outer = 0;
  while (inner < 420 || outer < 900) {
    const x = (r() - 0.5) * 2 * X, z = Z0 + r() * (Z1 - Z0), y = r() * RAIN_H, d = Math.hypot(x - CX, z - CZ);
    if (d < R0) { if (inner++ >= 420) continue; } else if (d > R1) { if (outer++ >= 900) continue; } else continue;
    streak(x, y, z); streak(x, y + RAIN_H, z);                            // and its copy one period up
  }
  const rain = addMesh('Rain', geo, fadeMaterial('Rain', [0.78, 0.86, 1, 0.42]));
  const times = doc.createAccessor().setType('SCALAR').setArray(new Float32Array([0, 0.6])).setBuffer(buf);
  const values = doc.createAccessor().setType('VEC3').setArray(new Float32Array([0, 0, 0, 0, -RAIN_H, 0])).setBuffer(buf);
  const sampler = doc.createAnimationSampler().setInput(times).setOutput(values).setInterpolation('LINEAR');
  const channel = doc.createAnimationChannel().setTargetNode(rain).setTargetPath('translation').setSampler(sampler);
  doc.createAnimation('Rain').addSampler(sampler).addChannel(channel);
}

// ── Headlight beams: two faint unlit cones in the haze ahead of the car, brightest at the
//    lamp (fade.png along the cone). The page scales their alpha with the headlights
//    (setMaterial('Beam', { baseColor })). ──
{
  const geo = new Geo();
  const cone = (apex, dir, len, r0, r1, seg) => {
    const l = Math.hypot(...dir), d = dir.map((v) => v / l);
    const up = Math.abs(d[1]) < 0.9 ? [0, 1, 0] : [1, 0, 0];
    let u = [up[1] * d[2] - up[2] * d[1], up[2] * d[0] - up[0] * d[2], up[0] * d[1] - up[1] * d[0]];
    const ul = Math.hypot(...u); u = u.map((v) => v / ul);
    const v = [d[1] * u[2] - d[2] * u[1], d[2] * u[0] - d[0] * u[2], d[0] * u[1] - d[1] * u[0]];
    const ring = (t, rad) => Array.from({ length: seg }, (_, k) => {
      const a = (k / seg) * 2 * Math.PI, c = Math.cos(a) * rad, s = Math.sin(a) * rad;
      return [apex[0] + d[0] * len * t + u[0] * c + v[0] * s, apex[1] + d[1] * len * t + u[1] * c + v[1] * s, apex[2] + d[2] * len * t + u[2] * c + v[2] * s];
    });
    const A = ring(0, r0), B = ring(1, r1);
    for (let k = 0; k < seg; k++) geo.quad(A[k], A[(k + 1) % seg], B[(k + 1) % seg], B[k]);
  };
  cone([0.72, 0.66, 2.35], [0.06, -0.05, 1], 7.5, 0.1, 1.25, 18);
  cone([-0.72, 0.66, 2.35], [-0.06, -0.05, 1], 7.5, 0.1, 1.25, 18);
  addMesh('Beams', geo, fadeMaterial('Beam', [0.72, 0.85, 1, 0.045]));
}

// Materials stay out of dedup: the five start-light columns and the two unlit blended
// materials (Rain, Beam) are equal by value and distinct by name on purpose.
await doc.transform(dedup({ propertyTypes: [PropertyType.ACCESSOR, PropertyType.MESH, PropertyType.TEXTURE] }), prune());
if (useMeshopt) {
  doc.createExtension(EXTMeshoptCompression).setRequired(true);
  await doc.transform(reorder({ encoder: MeshoptEncoder }), meshopt({ encoder: MeshoptEncoder, level: 'medium', quantizeColor: 16 }));
} else {
  await doc.transform(quantize({ quantizePosition: 16, quantizeNormal: 10, quantizeTexcoord: 14, quantizeColor: 16 }));
}
await io.write(outPath, doc);
console.log(outPath, fs.statSync(outPath).size, 'bytes; boxes', set.boxes.length, 'panels', set.panels.length);
