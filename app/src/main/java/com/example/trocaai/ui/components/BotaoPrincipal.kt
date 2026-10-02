package com.example.trocaai.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Botão de ação principal; mostra um indicador de progresso enquanto carregando = true.
@Composable
fun BotaoPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    carregando: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !carregando,
        modifier = modifier
    ) {
        if (carregando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current
            )
        } else {
            Text(texto)
        }
    }
}
