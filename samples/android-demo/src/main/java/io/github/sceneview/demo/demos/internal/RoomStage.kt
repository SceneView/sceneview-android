package io.github.sceneview.demo.demos.internal

import androidx.compose.ui.graphics.Color

/**
 * The finish of the room the `ai-scene` demo rebuilds from a scan: matt plaster walls on an oak
 * floor, a dark aluminium window, and the furniture the scan recognised — a made bed, a white
 * radiator, a potted plant — when no catalogue model stands in for it.
 *
 * The values are `DESIGN.md`'s `stage-room-*` rows. Like `stage-tray-*` they are fixed in both
 * themes: the room is an object on the stage, lit by the studio, not a themed surface. Only the
 * stage sky, the labels and the analysis inks follow light and dark.
 *
 * The oak is drawn by `tray_wood.filamat`, a procedural grain between an early and a late tone.
 */
object RoomStage {

    /** `stage-room-plaster` — matt white plaster, warm enough not to read as a render's grey. */
    val PLASTER: Color = Color(0xFFECE7DF)
    const val PLASTER_ROUGHNESS: Float = 0.92f

    /** `stage-room-skirting` — the painted skirting board along the foot of each wall. */
    val SKIRTING: Color = Color(0xFFF6F3EE)
    const val SKIRTING_ROUGHNESS: Float = 0.45f

    /** `stage-room-oak-early` / `-late` — the oak planks, satin-varnished. */
    val OAK_EARLY: Color = Color(0xFFC9A57A)
    val OAK_LATE: Color = Color(0xFF8E6440)
    const val OAK_RING_DENSITY: Float = 26f
    const val OAK_ROUGHNESS: Float = 0.5f
    const val OAK_VARNISH: Float = 0.35f
    const val OAK_VARNISH_ROUGHNESS: Float = 0.3f

    /** `stage-room-slab` — the screed under the planks, seen only on their cut edge. */
    val SLAB: Color = Color(0xFF8C8780)

    /** `stage-room-frame` — the window's anthracite aluminium frame and mullion. */
    val FRAME: Color = Color(0xFF2F3437)
    const val FRAME_ROUGHNESS: Float = 0.38f

    /** `stage-room-glass` — the glazing: a faint blue-green, as float glass is on its edge. */
    val GLASS: Color = Color(0xFFDDEEEC)
    const val GLASS_ROUGHNESS: Float = 0.03f
    const val GLASS_IOR: Float = 1.5f

    /** `stage-room-linen` / `-pillow` / `-throw` / `-headboard` — the made bed. */
    val LINEN: Color = Color(0xFFF1EEE8)
    val PILLOW: Color = Color(0xFFFAF8F4)
    val THROW: Color = Color(0xFF5E7186)
    val HEADBOARD: Color = Color(0xFF6B6359)
    const val FABRIC_ROUGHNESS: Float = 0.9f

    /** `stage-room-radiator` — a white steel panel radiator, gloss enamel. */
    val RADIATOR: Color = Color(0xFFF4F4F2)
    const val RADIATOR_ROUGHNESS: Float = 0.3f

    /** `stage-room-pot` / `-leaf` / `-leaf-light` — a terracotta pot and its two greens. */
    val POT: Color = Color(0xFFB8664A)
    val LEAF: Color = Color(0xFF2F6B3A)
    val LEAF_LIGHT: Color = Color(0xFF4E8F4A)
    const val LEAF_ROUGHNESS: Float = 0.55f

    /** `stage-room-brass` / `-shade` — the stand-in floor lamp: a brass stem, a linen shade. */
    val BRASS: Color = Color(0xFFC9A55C)
    const val BRASS_ROUGHNESS: Float = 0.28f
    val SHADE: Color = Color(0xFFF3EBDD)

    /**
     * `stage-room-paint-*` — the matt paints `set_material` gives a rebuilt wall, the floor, a
     * frame or an item ("make the wall blue"), by the finish name the tool takes.
     */
    val PAINTS: Map<String, Color> = mapOf(
        "white" to Color(0xFFF2F0EB),
        "blue" to Color(0xFF4F78A8),
        "navy" to Color(0xFF263A5A),
        "green" to Color(0xFF6E8B6A),
        "yellow" to Color(0xFFE3C35A),
        "red" to Color(0xFFB0463C),
        "pink" to Color(0xFFE3A9A8),
        "grey" to Color(0xFF9A9C9C),
        "black" to Color(0xFF2B2B2D),
    )
}
