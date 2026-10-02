package com.example.trocaai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EsquemaClaro = lightColorScheme(
    primary = Verde,
    onPrimary = Superficie,
    primaryContainer = VerdeClaro,
    onPrimaryContainer = VerdeEscuro,
    secondary = Laranja,
    onSecondary = TextoPrincipal,
    secondaryContainer = LaranjaClaro,
    onSecondaryContainer = TextoPrincipal,
    background = Fundo,
    onBackground = TextoPrincipal,
    surface = Superficie,
    onSurface = TextoPrincipal,
    onSurfaceVariant = TextoSecundario,
    error = Erro
)

@Composable
fun TrocaAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography,
        content = content
    )
}
