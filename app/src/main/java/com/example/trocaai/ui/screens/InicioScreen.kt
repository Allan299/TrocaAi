package com.example.trocaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trocaai.data.AnunciosRepository
import com.example.trocaai.ui.components.itensAnuncios
import com.example.trocaai.ui.components.rememberEstadoLista

@Composable
fun InicioScreen(
    onAbrirAnuncio: (String) -> Unit,
    onAbrirAssistente: () -> Unit,
    onAbrirPerfil: () -> Unit
) {
    val repositorio = remember { AnunciosRepository() }
    val estado = rememberEstadoLista(Unit) { repositorio.observarRecentes() }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TrocaAí",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onAbrirPerfil) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Meu Perfil",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                "Compre, venda, troque e doe materiais acadêmicos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Card(
                onClick = onAbrirAssistente,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Face, contentDescription = null)
                    Column {
                        Text("Assistente TrocaAí", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Converse e encontre materiais mais rápido.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Text("Anúncios recentes", style = MaterialTheme.typography.titleLarge)
        }
        itensAnuncios(
            estado = estado,
            textoVazio = "Nenhum anúncio ainda. Seja o primeiro a anunciar!",
            onAbrirAnuncio = onAbrirAnuncio
        )
    }
}
