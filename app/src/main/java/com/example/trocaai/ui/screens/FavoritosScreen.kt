package com.example.trocaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.example.trocaai.data.FavoritosRepository
import com.example.trocaai.data.ResultadoLista
import com.example.trocaai.ui.components.itensAnuncios
import com.example.trocaai.ui.components.rememberEstadoLista
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// Lista só os favoritos do usuário logado (users/{uid}/favoritos).
@Composable
fun FavoritosScreen(uid: String?, onAbrirAnuncio: (String) -> Unit) {
    val repositorio = remember { FavoritosRepository() }
    val estado = rememberEstadoLista(uid) {
        val fluxo: Flow<ResultadoLista> = if (uid == null) emptyFlow() else repositorio.observarAnuncios(uid)
        fluxo
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Favoritos", style = MaterialTheme.typography.headlineMedium) }
        itensAnuncios(
            estado = estado,
            textoVazio = "Você ainda não tem favoritos. Toque no coração de um anúncio para salvá-lo aqui.",
            onAbrirAnuncio = onAbrirAnuncio
        )
    }
}
