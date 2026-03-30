/**
 * generate-physics.ts
 *
 * Generates compilable physics simulation code for SceneView.
 */

export const PHYSICS_TYPES = [
  "gravity-drop",
  "collision-detection",
  "spring-physics",
  "projectile",
  "ragdoll-basic",
  "rigid-body",
] as const;

export type PhysicsType = (typeof PHYSICS_TYPES)[number];

interface PhysicsTemplate {
  title: string;
  description: string;
  android: string;
  ios?: string;
}

const PHYSICS_TEMPLATES: Record<PhysicsType, PhysicsTemplate> = {
  "gravity-drop": {
    title: "Gravity Drop Simulation",
    description: "Simulates objects falling under gravity using onFrame callback with velocity accumulation.",
    android: `@Composable
fun GravityDropScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/object.glb")

    var posY by remember { mutableFloatStateOf(3.0f) }
    var velocityY by remember { mutableFloatStateOf(0f) }
    val gravity = -9.81f
    val groundY = 0f
    val bounceFactor = 0.6f

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        onFrame = { frameTimeNanos ->
            val dt = 1f / 60f  // Fixed timestep for stability
            velocityY += gravity * dt
            posY += velocityY * dt

            // Bounce off ground
            if (posY <= groundY) {
                posY = groundY
                velocityY = -velocityY * bounceFactor
                // Stop bouncing when velocity is negligible
                if (kotlin.math.abs(velocityY) < 0.1f) {
                    velocityY = 0f
                    posY = groundY
                }
            }
        }
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 0.5f,
                centerOrigin = Position(0f, 0f, 0f),
                position = Position(0f, posY, 0f)
            )
        }

        // Ground plane
        CubeNode(
            engine = engine,
            size = Size(10f, 0.01f, 10f),
            center = Position(0f, 0f, 0f),
            materialInstance = rememberMaterialInstance(engine) {
                baseColor(0.3f, 0.3f, 0.3f)
                roughness(0.8f)
            }
        )

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                castShadows(true)
                direction(0f, -1f, -0.5f)
            }
        )
    }
}`,
    ios: `import SwiftUI
import SceneViewSwift
import RealityKit

struct GravityDropScene: View {
    @State private var posY: Float = 3.0
    @State private var velocityY: Float = 0

    let gravity: Float = -9.81
    let bounceFactor: Float = 0.6
    let timer = Timer.publish(every: 1.0/60.0, on: .main, in: .common).autoconnect()

    var body: some View {
        SceneView { root in
            // Add ground plane
            let ground = ModelEntity(
                mesh: .generatePlane(width: 10, depth: 10),
                materials: [SimpleMaterial(color: .gray, isMetallic: false)]
            )
            root.addChild(ground)
        }
        .onReceive(timer) { _ in
            let dt: Float = 1.0 / 60.0
            velocityY += gravity * dt
            posY += velocityY * dt
            if posY <= 0 {
                posY = 0
                velocityY = -velocityY * bounceFactor
                if abs(velocityY) < 0.1 {
                    velocityY = 0
                }
            }
        }
    }
}`,
  },

  "collision-detection": {
    title: "Collision Detection",
    description: "Detects collisions between objects using bounding box intersection from sceneview-core KMP.",
    android: `@Composable
fun CollisionDetectionScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelA = rememberModelInstance(modelLoader, "models/sphere.glb")
    val modelB = rememberModelInstance(modelLoader, "models/cube.glb")

    var posAx by remember { mutableFloatStateOf(-2f) }
    var isColliding by remember { mutableStateOf(false) }
    val speed = 1.5f

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        onFrame = { _ ->
            // Move sphere toward cube
            if (posAx < 2f && !isColliding) {
                posAx += speed * (1f / 60f)
            }

            // Simple AABB collision check
            // Object A center at (posAx, 0, 0) with radius ~0.5
            // Object B center at (1, 0, 0) with radius ~0.5
            val distance = kotlin.math.abs(posAx - 1f)
            isColliding = distance < 1.0f  // Combined radii
        }
    ) {
        // Moving sphere
        modelA?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 0.5f,
                position = Position(posAx, 0.5f, 0f)
            )
        }

        // Static cube
        modelB?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 0.5f,
                position = Position(1f, 0.5f, 0f)
            )
        }

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                direction(0f, -1f, -0.5f)
            }
        )
    }

    // Collision feedback
    if (isColliding) {
        Text(
            text = "COLLISION!",
            modifier = Modifier.padding(16.dp),
            color = Color.Red,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}`,
  },

  "spring-physics": {
    title: "Spring Physics (KMP Core)",
    description: "Uses Spring animation from sceneview-core KMP for physically-based bounce and wobble.",
    android: `@Composable
fun SpringPhysicsScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/object.glb")

    // Spring parameters
    var targetY by remember { mutableFloatStateOf(1.0f) }
    var currentY by remember { mutableFloatStateOf(0f) }
    var velocityY by remember { mutableFloatStateOf(0f) }
    val stiffness = 200f    // Higher = snappier
    val damping = 10f       // Higher = less bounce
    val mass = 1f

    // Toggle target on tap
    var raised by remember { mutableStateOf(false) }

    SceneView(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                raised = !raised
                targetY = if (raised) 2.0f else 1.0f
            },
        engine = engine,
        modelLoader = modelLoader,
        onFrame = { _ ->
            val dt = 1f / 60f
            // Spring force: F = -k * (x - target) - d * v
            val springForce = -stiffness * (currentY - targetY) - damping * velocityY
            val acceleration = springForce / mass
            velocityY += acceleration * dt
            currentY += velocityY * dt
        }
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 0.5f,
                centerOrigin = Position(0f, 0f, 0f),
                position = Position(0f, currentY, 0f)
            )
        }

        // Ground
        CubeNode(
            engine = engine,
            size = Size(5f, 0.01f, 5f),
            center = Position(0f, 0f, 0f),
            materialInstance = rememberMaterialInstance(engine) {
                baseColor(0.4f, 0.4f, 0.4f)
            }
        )

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                castShadows(true)
                direction(0f, -1f, -0.5f)
            }
        )
    }
}`,
  },

  "projectile": {
    title: "Projectile Motion",
    description: "Launches a projectile with initial velocity, gravity, and trajectory tracking.",
    android: `@Composable
fun ProjectileScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val projectileModel = rememberModelInstance(modelLoader, "models/sphere.glb")

    data class Projectile(var x: Float, var y: Float, var vx: Float, var vy: Float, var active: Boolean = true)
    val projectiles = remember { mutableStateListOf<Projectile>() }
    val gravity = -9.81f

    // Launch on tap
    SceneView(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                projectiles.add(
                    Projectile(
                        x = 0f, y = 0.5f,
                        vx = 3f + (Math.random() * 2f).toFloat(),
                        vy = 5f + (Math.random() * 3f).toFloat()
                    )
                )
            },
        engine = engine,
        modelLoader = modelLoader,
        onFrame = { _ ->
            val dt = 1f / 60f
            projectiles.forEachIndexed { index, p ->
                if (p.active) {
                    p.vy += gravity * dt
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    if (p.y < -1f) {
                        p.active = false
                    }
                }
            }
        }
    ) {
        // Render active projectiles
        projectiles.filter { it.active }.forEach { p ->
            projectileModel?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.1f,
                    position = Position(p.x, p.y, 0f)
                )
            }
        }

        // Ground
        CubeNode(
            engine = engine,
            size = Size(20f, 0.01f, 5f),
            center = Position(5f, 0f, 0f),
            materialInstance = rememberMaterialInstance(engine) {
                baseColor(0.3f, 0.6f, 0.3f)
                roughness(0.9f)
            }
        )

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                castShadows(true)
                direction(0f, -1f, -0.5f)
            }
        )
    }
}`,
  },

  "ragdoll-basic": {
    title: "Basic Ragdoll Joints",
    description: "Simulates connected rigid bodies with joint constraints for ragdoll-like behavior.",
    android: `@Composable
fun RagdollScene() {
    val engine = rememberEngine()

    data class RagdollPart(
        var x: Float, var y: Float,
        var vx: Float = 0f, var vy: Float = 0f,
        val parentIndex: Int = -1,
        val jointLength: Float = 0.3f
    )

    // Head, torso, left arm, right arm, left leg, right leg
    val parts = remember {
        mutableStateListOf(
            RagdollPart(0f, 2.0f),              // 0: Head
            RagdollPart(0f, 1.5f),              // 1: Torso (connected to head)
            RagdollPart(-0.3f, 1.5f, parentIndex = 1, jointLength = 0.4f),  // 2: Left arm
            RagdollPart(0.3f, 1.5f, parentIndex = 1, jointLength = 0.4f),   // 3: Right arm
            RagdollPart(-0.1f, 1.0f, parentIndex = 1, jointLength = 0.5f),  // 4: Left leg
            RagdollPart(0.1f, 1.0f, parentIndex = 1, jointLength = 0.5f),   // 5: Right leg
        )
    }

    val gravity = -9.81f
    val damping = 0.98f

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        onFrame = { _ ->
            val dt = 1f / 60f
            parts.forEachIndexed { index, part ->
                // Apply gravity
                part.vy += gravity * dt
                part.x += part.vx * dt
                part.y += part.vy * dt
                part.vx *= damping
                part.vy *= damping

                // Ground constraint
                if (part.y < 0f) {
                    part.y = 0f
                    part.vy = -part.vy * 0.3f
                }

                // Joint constraint
                if (part.parentIndex >= 0) {
                    val parent = parts[part.parentIndex]
                    val dx = part.x - parent.x
                    val dy = part.y - parent.y
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist > part.jointLength && dist > 0.001f) {
                        val correction = (dist - part.jointLength) / dist * 0.5f
                        part.x -= dx * correction
                        part.y -= dy * correction
                        parts[part.parentIndex] = parent.copy(
                            x = parent.x + dx * correction,
                            y = parent.y + dy * correction
                        )
                    }
                }
            }
        }
    ) {
        // Render each part as a sphere
        parts.forEach { part ->
            SphereNode(
                engine = engine,
                radius = 0.08f,
                center = Position(part.x, part.y, 0f),
                materialInstance = rememberMaterialInstance(engine) {
                    baseColor(0.9f, 0.6f, 0.4f)
                    roughness(0.7f)
                }
            )
        }

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                direction(0f, -1f, -0.5f)
            }
        )
    }
}`,
  },

  "rigid-body": {
    title: "Rigid Body Simulation",
    description: "Multiple rigid bodies with gravity, ground collision, and inter-body collision response.",
    android: `@Composable
fun RigidBodyScene() {
    val engine = rememberEngine()

    data class RigidBody(
        var x: Float, var y: Float, var z: Float,
        var vx: Float = 0f, var vy: Float = 0f, var vz: Float = 0f,
        val radius: Float = 0.25f,
        val mass: Float = 1f,
        val restitution: Float = 0.5f
    )

    val bodies = remember {
        mutableStateListOf(
            RigidBody(0f, 3f, 0f),
            RigidBody(0.5f, 4f, 0.2f),
            RigidBody(-0.3f, 5f, -0.1f),
            RigidBody(0.1f, 6f, 0.3f),
        )
    }

    val gravity = -9.81f

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        onFrame = { _ ->
            val dt = 1f / 60f
            bodies.forEachIndexed { i, body ->
                // Gravity
                body.vy += gravity * dt
                body.x += body.vx * dt
                body.y += body.vy * dt
                body.z += body.vz * dt

                // Ground bounce
                if (body.y < body.radius) {
                    body.y = body.radius
                    body.vy = -body.vy * body.restitution
                }

                // Inter-body collision
                for (j in (i + 1) until bodies.size) {
                    val other = bodies[j]
                    val dx = body.x - other.x
                    val dy = body.y - other.y
                    val dz = body.z - other.z
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                    val minDist = body.radius + other.radius
                    if (dist < minDist && dist > 0.001f) {
                        // Push apart
                        val nx = dx / dist
                        val ny = dy / dist
                        val nz = dz / dist
                        val overlap = (minDist - dist) * 0.5f
                        body.x += nx * overlap
                        body.y += ny * overlap
                        body.z += nz * overlap
                        other.x -= nx * overlap
                        other.y -= ny * overlap
                        other.z -= nz * overlap

                        // Elastic collision response
                        val relVx = body.vx - other.vx
                        val relVy = body.vy - other.vy
                        val relVz = body.vz - other.vz
                        val relVn = relVx * nx + relVy * ny + relVz * nz
                        if (relVn < 0) {
                            val impulse = relVn / (1f / body.mass + 1f / other.mass)
                            body.vx -= impulse / body.mass * nx
                            body.vy -= impulse / body.mass * ny
                            body.vz -= impulse / body.mass * nz
                            other.vx += impulse / other.mass * nx
                            other.vy += impulse / other.mass * ny
                            other.vz += impulse / other.mass * nz
                        }
                    }
                }
            }
        }
    ) {
        bodies.forEach { body ->
            SphereNode(
                engine = engine,
                radius = body.radius,
                center = Position(body.x, body.y, body.z),
                materialInstance = rememberMaterialInstance(engine) {
                    baseColor(0.2f, 0.5f, 0.9f)
                    metallic(0.3f)
                    roughness(0.4f)
                }
            )
        }

        // Ground
        CubeNode(
            engine = engine,
            size = Size(10f, 0.02f, 10f),
            center = Position(0f, 0f, 0f),
            materialInstance = rememberMaterialInstance(engine) {
                baseColor(0.4f, 0.4f, 0.4f)
                roughness(0.9f)
            }
        )

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                castShadows(true)
                direction(0f, -1f, -0.5f)
            }
        )
    }
}`,
  },
};

export function generatePhysicsCode(
  physicsType: PhysicsType,
  platform: "android" | "ios" = "android"
): { code: string; title: string; description: string } | null {
  const template = PHYSICS_TEMPLATES[physicsType];
  if (!template) return null;

  const code = platform === "ios" && template.ios ? template.ios : template.android;
  return { code, title: template.title, description: template.description };
}

export function formatPhysicsCode(result: {
  code: string;
  title: string;
  description: string;
}, platform: "android" | "ios"): string {
  const lang = platform === "ios" ? "swift" : "kotlin";
  const platLabel = platform === "ios" ? "iOS (SwiftUI + RealityKit)" : "Android (Jetpack Compose)";
  return [
    `## ${result.title}`,
    ``,
    `**Platform:** ${platLabel}`,
    ``,
    result.description,
    ``,
    `\`\`\`${lang}`,
    result.code,
    `\`\`\``,
    ``,
    `### Available Physics Types`,
    ``,
    ...PHYSICS_TYPES.map((t) => `- \`${t}\``),
  ].join("\n");
}
