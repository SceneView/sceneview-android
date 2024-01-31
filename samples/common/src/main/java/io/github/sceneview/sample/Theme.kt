package io.github.sceneview.sample

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object Colors {
    val Purple200 = Color(0xFFBB86FC)
    val Purple500 = Color(0xFF6200EE)
    val Purple700 = Color(0xFF3700B3)
    val Teal200 = Color(0xFF03DAC5)

    val LightPrimary = Color(0xFF7346B3)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFEDDCFF)
    val LightOnPrimaryContainer = Color(0xFF280056)
    val LightSecondary = Color(0xFF645A70)
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightSecondaryContainer = Color(0xFFEBDDF7)
    val LightOnSecondaryContainer = Color(0xFF1F182A)
    val LightTertiary = Color(0xFF7F525B)
    val LightOnTertiary = Color(0xFFFFFFFF)
    val LightTertiaryContainer = Color(0xFFFFD9DF)
    val LightOnTertiaryContainer = Color(0xFF321019)
    val LightError = Color(0xFFBA1A1A)
    val LightErrorContainer = Color(0xFFFFDAD6)
    val LightOnError = Color(0xFFFFFFFF)
    val LightOnErrorContainer = Color(0xFF410002)
    val LightBackground = Color(0xFFFFFBFF)
    val LightOnBackground = Color(0xFF1D1B1E)
    val LightOutline = Color(0xFF7B757F)
    val LightInverseOnSurface = Color(0xFFF5EFF4)
    val LightInverseSurface = Color(0xFF322F33)
    val LightInversePrimary = Color(0xFFD7BAFF)
    val LightSurfaceTint = Color(0xFF7346B3)
    val LightOutlineVariant = Color(0xFFCBC4CF)
    val LightScrim = Color(0xFF000000)
    val LightSurface = Color(0xFFFEF8FC)
    val LightOnSurface = Color(0xFF1D1B1E)
    val LightSurfaceVariant = Color(0xFFE8E0EB)
    val LightOnSurfaceVariant = Color(0xFF4A454E)

    val DarkPrimary = Color(0xFFD7BAFF)
    val DarkOnPrimary = Color(0xFF420A82)
    val DarkPrimaryContainer = Color(0xFF5A2C99)
    val DarkOnPrimaryContainer = Color(0xFFEDDCFF)
    val DarkSecondary = Color(0xFFCEC2DA)
    val DarkOnSecondary = Color(0xFF352D40)
    val DarkSecondaryContainer = Color(0xFF4C4357)
    val DarkOnSecondaryContainer = Color(0xFFEBDDF7)
    val DarkTertiary = Color(0xFFF1B7C2)
    val DarkOnTertiary = Color(0xFF4B252E)
    val DarkTertiaryContainer = Color(0xFF643B44)
    val DarkOnTertiaryContainer = Color(0xFFFFD9DF)
    val DarkError = Color(0xFFFFB4AB)
    val DarkErrorContainer = Color(0xFF93000A)
    val DarkOnError = Color(0xFF690005)
    val DarkOnErrorContainer = Color(0xFFFFDAD6)
    val DarkBackground = Color(0xFF1D1B1E)
    val DarkOnBackground = Color(0xFFE7E1E6)
    val DarkOutline = Color(0xFF958E99)
    val DarkInverseOnSurface = Color(0xFF1D1B1E)
    val DarkInverseSurface = Color(0xFFE7E1E6)
    val DarkInversePrimary = Color(0xFF7346B3)
    val DarkSurfaceTint = Color(0xFFD7BAFF)
    val DarkOutlineVariant = Color(0xFF4A454E)
    val DarkScrim = Color(0xFF000000)
    val DarkSurface = Color(0xFF141316)
    val DarkOnSurface = Color(0xFFCAC5CA)
    val DarkSurfaceVariant = Color(0xFF4A454E)
    val DarkOnSurfaceVariant = Color(0xFFCBC4CF)


    val Seed = Color(0xFF6639A6)
}

private val LightColorScheme = lightColorScheme(
    primary = Colors.LightPrimary,
    onPrimary = Colors.LightOnPrimary,
    primaryContainer = Colors.LightPrimaryContainer,
    onPrimaryContainer = Colors.LightOnPrimaryContainer,
    secondary = Colors.LightSecondary,
    onSecondary = Colors.LightOnSecondary,
    secondaryContainer = Colors.LightSecondaryContainer,
    onSecondaryContainer = Colors.LightOnSecondaryContainer,
    tertiary = Colors.LightTertiary,
    onTertiary = Colors.LightOnTertiary,
    tertiaryContainer = Colors.LightTertiaryContainer,
    onTertiaryContainer = Colors.LightOnTertiaryContainer,
    error = Colors.LightError,
    errorContainer = Colors.LightErrorContainer,
    onError = Colors.LightOnError,
    onErrorContainer = Colors.LightOnErrorContainer,
    background = Colors.LightBackground,
    onBackground = Colors.LightOnBackground,
    outline = Colors.LightOutline,
    inverseOnSurface = Colors.LightInverseOnSurface,
    inverseSurface = Colors.LightInverseSurface,
    inversePrimary = Colors.LightInversePrimary,
    surfaceTint = Colors.LightSurfaceTint,
    outlineVariant = Colors.LightOutlineVariant,
    scrim = Colors.LightScrim,
    surface = Colors.LightSurface,
    onSurface = Colors.LightOnSurface,
    surfaceVariant = Colors.LightSurfaceVariant,
    onSurfaceVariant = Colors.LightOnSurfaceVariant,
)


private val DarkColorScheme = darkColorScheme(
    primary = Colors.DarkPrimary,
    onPrimary = Colors.DarkOnPrimary,
    primaryContainer = Colors.DarkPrimaryContainer,
    onPrimaryContainer = Colors.DarkOnPrimaryContainer,
    secondary = Colors.DarkSecondary,
    onSecondary = Colors.DarkOnSecondary,
    secondaryContainer = Colors.DarkSecondaryContainer,
    onSecondaryContainer = Colors.DarkOnSecondaryContainer,
    tertiary = Colors.DarkTertiary,
    onTertiary = Colors.DarkOnTertiary,
    tertiaryContainer = Colors.DarkTertiaryContainer,
    onTertiaryContainer = Colors.DarkOnTertiaryContainer,
    error = Colors.DarkError,
    errorContainer = Colors.DarkErrorContainer,
    onError = Colors.DarkOnError,
    onErrorContainer = Colors.DarkOnErrorContainer,
    background = Colors.DarkBackground,
    onBackground = Colors.DarkOnBackground,
    outline = Colors.DarkOutline,
    inverseOnSurface = Colors.DarkInverseOnSurface,
    inverseSurface = Colors.DarkInverseSurface,
    inversePrimary = Colors.DarkInversePrimary,
    surfaceTint = Colors.DarkSurfaceTint,
    outlineVariant = Colors.DarkOutlineVariant,
    scrim = Colors.DarkScrim,
    surface = Colors.DarkSurface,
    onSurface = Colors.DarkOnSurface,
    surfaceVariant = Colors.DarkSurfaceVariant,
    onSurfaceVariant = Colors.DarkOnSurfaceVariant,
)

val Typography = Typography()

@Composable
fun SceneviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable() () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}