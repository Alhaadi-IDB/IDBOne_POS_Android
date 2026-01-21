package print.therestsuites.com.ui.theme

import android.R
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9D7DF7),
    onPrimary = Color(0xFF2B0038),
    primaryContainer = Color(0xFF6C18A4),
    onPrimaryContainer = Color(0xFFF5EEFC),
    secondary = Color(0xFFB4A0F0),
    onSecondary = Color(0xFF1A0A2E),
    secondaryContainer = Color(0xFF5D1D8A),
    onSecondaryContainer = Color(0xFFEBDCF6),
    tertiary = Color(0xFFC59EF8),
    onTertiary = Color(0xFF2B0038),
    error = Color(0xFFF44336),
    errorContainer = Color(0xFF7A1F1F),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1022),
    onBackground = Color(0xFFF5EEFC),
    surface = Color(0xFF24162E),
    onSurface = Color(0xFFF5EEFC),
    surfaceVariant = Color(0xFF3A2351),
    onSurfaceVariant = Color(0xFFDCC9EB),
    outline = Color(0xFF6E4A86),
    outlineVariant = Color(0xFF4A2A66),
    inverseSurface = Color(0xFFF5EEFC),
    inverseOnSurface = Color(0xFF2B0038),
    inversePrimary = Color(0xFF6C18A4),
    scrim = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6C18A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8D4F2),
    onPrimaryContainer = Color(0xFF2B0038),
    secondary = Color(0xFF8B3DB8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5EEFC),
    onSecondaryContainer = Color(0xFF3F0A5C),
    tertiary = Color(0xFFA857CC),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0E5F5),
    onSurfaceVariant = Color(0xFF4A0E6E),
    outline = Color(0xFF8B7294),
    outlineVariant = Color(0xFFE8D4F2),
    inverseSurface = Color(0xFF32302F),
    inverseOnSurface = Color(0xFFF5EEFC),
    inversePrimary = Color(0xFFD4B3E8),
    scrim = Color(0xFF000000)
)

@Composable
fun PrintBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
