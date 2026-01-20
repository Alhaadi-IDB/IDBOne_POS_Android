package print.therestsuites.com.ui.theme

import android.R
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4A0E6E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6C18A4),
    onPrimaryContainer = Color(0xFFF5EEFC),
    secondary = Color(0xFF8B3DB8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5D1D8A),
    onSecondaryContainer = Color(0xFFEBDCF6),
    tertiary = Color(0xFFA857CC),
    onTertiary = Color.White,
    background = Color(0xFF1A1022),
    onBackground = Color(0xFFF5EEFC),
    surface = Color(0xFF24162E),
    onSurface = Color(0xFFF5EEFC),
    surfaceVariant = Color(0xFF3A2351),
    onSurfaceVariant = Color(0xFFDCC9EB),
    outline = Color(0xFF6E4A86)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6C18A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4B3E8),
    onPrimaryContainer = Color(0xFF2B0038),
    secondary = Color(0xFF8B3DB8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8D4F2),
    onSecondaryContainer = Color(0xFF3F0A5C),
    tertiary = Color(0xFFA857CC),
    onTertiary = Color.White,
    background = Color(0xFFF5EEFC),
    onBackground = Color(0xFF2B0038),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2B0038),
    surfaceVariant = Color(0xFFE8D4F2),
    onSurfaceVariant = Color(0xFF4A0E6E),
    outline = Color(0xFFB08DCC)
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
