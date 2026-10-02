package com.example.trocaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trocaai.data.AnunciosRepository
import com.example.trocaai.model.CATEGORIAS
import com.example.trocaai.model.TipoAnuncio
import com.example.trocaai.ui.components.FiltroChips
import com.example.trocaai.ui.components.itensAnuncios
import com.example.trocaai.ui.components.rememberEstadoLista
import com.example.trocaai.util.filtrar

@Composable
fun BuscarScreen(onAbrirAnuncio: (String) -> Unit) {
    val repositorio = remember { AnunciosRepository() }
    val estado = rememberEstadoLista(Unit) { repositorio.observarRecentes() }

    var texto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf<String?>(null) }
    var tipo by remember { mutableStateOf<TipoAnuncio?>(null) }

    val resultados = estado.anuncios?.filtrar(texto, categoria, tipo)
    val filtrando = texto.isNotBlank() || categoria != null || tipo != null

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            placeholder = { Text("Buscar por título") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (texto.isNotEmpty()) {
                    IconButton(onClick = { texto = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        FiltroChips(
            rotuloTodos = "Todos os tipos",
            opcoes = TipoAnuncio.entries,
            selecionada = tipo,
            rotulo = { it.rotulo },
            aoSelecionar = { tipo = it },
            modifier = Modifier.fillMaxWidth(),
        )
        FiltroChips(
            rotuloTodos = "Todas as categorias",
            opcoes = CATEGORIAS,
            selecionada = categoria,
            rotulo = { it },
            aoSelecionar = { categoria = it },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (resultados != null && estado.erro == null) {
                item {
                    Text(
                        "${resultados.size} anúncio(s) encontrado(s)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itensAnuncios(
                estado = estado,
                anuncios = resultados,
                textoVazio = if (filtrando) "Nenhum anúncio encontrado com esses filtros."
                else "Ainda não há anúncios publicados.",
                onAbrirAnuncio = onAbrirAnuncio
            )
        }
    }
}
