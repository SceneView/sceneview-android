# SceneView 3D Embed

Embed interactive 3D models on any website — like a YouTube embed, but for 3D.

## Quick Start

```html
<iframe
  src="https://sceneview.github.io/embed?model=https://modelviewer.dev/shared-assets/models/Astronaut.glb"
  width="600"
  height="400"
  frameborder="0"
  allow="autoplay; xr-spatial-tracking"
  allowfullscreen
></iframe>
```

## URL Format

```
https://sceneview.github.io/embed?model=URL_TO_GLB
```

The `model` parameter is **required** and must be a publicly accessible URL to a `.glb` or `.gltf` file.

## Parameters

| Parameter    | Default  | Description                                      |
|--------------|----------|--------------------------------------------------|
| `model`      | required | URL to a `.glb` or `.gltf` 3D model              |
| `autorotate` | `true`   | Auto-rotate the model (`true` / `false`)          |
| `ar`         | `false`  | Enable AR button on supported devices             |
| `bg`         | `1a1a2e` | Background color as hex (with or without `#`)     |
| `poster`     | —        | URL to a poster image shown while loading         |
| `nofooter`   | —        | Hide the "Powered by SceneView" footer if present |

## Examples

### Basic embed

```html
<iframe
  src="https://sceneview.github.io/embed?model=https://modelviewer.dev/shared-assets/models/Astronaut.glb"
  width="100%" height="400" frameborder="0"
></iframe>
```

### Custom background, no rotation

```html
<iframe
  src="https://sceneview.github.io/embed?model=https://example.com/shoe.glb&autorotate=false&bg=ffffff"
  width="100%" height="500" frameborder="0"
></iframe>
```

### With AR enabled

```html
<iframe
  src="https://sceneview.github.io/embed?model=https://example.com/chair.glb&ar=true"
  width="100%" height="500" frameborder="0"
  allow="xr-spatial-tracking"
></iframe>
```

### Dark transparent background

```html
<iframe
  src="https://sceneview.github.io/embed?model=https://example.com/robot.glb&bg=000000"
  width="400" height="400" frameborder="0"
></iframe>
```

## Tips

- **CORS**: The model URL must allow cross-origin requests. Most CDNs and GitHub raw URLs work out of the box.
- **File size**: Keep models under 10 MB for fast loading. Use [gltf-transform](https://gltf-transform.dev/) to optimize.
- **Responsive**: Use `width="100%"` on the iframe and set a fixed height for responsive layouts.
- **HTTPS**: Both the embed URL and the model URL must use HTTPS.

## Hosting Your Own Models

You can host `.glb` files anywhere that serves them over HTTPS with proper CORS headers:

- **GitHub Pages** — commit to a repo and use the raw URL
- **Cloudflare R2 / AWS S3** — object storage with public access
- **Your own server** — serve with `Access-Control-Allow-Origin: *`

## License

The embed widget is part of [SceneView](https://github.com/sceneview/sceneview), licensed under Apache 2.0.
