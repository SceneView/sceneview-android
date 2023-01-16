package io.github.sceneview.texture

import com.google.android.filament.TextureSampler

class TextureSampler2D : TextureSampler(
    MinFilter.LINEAR_MIPMAP_LINEAR,
    MagFilter.LINEAR,
    WrapMode.REPEAT
)

class TextureSamplerExternal : TextureSampler(
    MinFilter.LINEAR,
    MagFilter.LINEAR,
    WrapMode.CLAMP_TO_EDGE
)