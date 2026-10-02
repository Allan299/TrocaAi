package com.example.trocaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Escolha de uma opção entre várias, usando chips (categoria, tipo, conservação).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SeletorOpcoes(
    titulo: String,
    opcoes: List<T>,
    selecionada: T?,
    rotulo: (T) -> String,
    aoSelecionar: (T) -> Unit,
    modifier: Modifier = Modifier,
    erro: String? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opcoes.forEach { opcao ->
                FilterChip(
                    selected = opcao == selecionada,
                    onClick = { aoSelecionar(opcao) },
                    label = { Text(rotulo(opcao)) }
                )
            }
        }
        erro?.let { MensagemErro(it) }
    }
}
