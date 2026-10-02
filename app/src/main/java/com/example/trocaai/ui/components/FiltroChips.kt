package com.example.trocaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Linha de chips com rolagem horizontal. selecionada == null significa "todos".
@Composable
fun <T> FiltroChips(
    rotuloTodos: String,
    opcoes: List<T>,
    selecionada: T?,
    rotulo: (T) -> String,
    aoSelecionar: (T?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selecionada == null,
                onClick = { aoSelecionar(null) },
                label = { Text(rotuloTodos) }
            )
        }
        items(opcoes) { opcao ->
            FilterChip(
                selected = opcao == selecionada,
                onClick = { aoSelecionar(if (opcao == selecionada) null else opcao) },
                label = { Text(rotulo(opcao)) }
            )
        }
    }
}
