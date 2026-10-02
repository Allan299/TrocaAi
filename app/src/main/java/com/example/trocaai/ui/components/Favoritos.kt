package com.example.trocaai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

// Favoritos do usuário logado, compartilhados com qualquer tela via CompositionLocal.
// Quem fornece o valor é o TrocaAiApp, que observa o Firestore.
class FavoritosState(val ids: Set<String>, val alternar: (String) -> Unit)

val LocalFavoritos = compositionLocalOf { FavoritosState(emptySet()) { } }

// Coração que adiciona ou remove o anúncio dos favoritos.
@Composable
fun BotaoFavorito(anuncioId: String, modifier: Modifier = Modifier) {
    val favoritos = LocalFavoritos.current
    val ativo = anuncioId in favoritos.ids
    IconButton(onClick = { favoritos.alternar(anuncioId) }, modifier = modifier) {
        Icon(
            imageVector = if (ativo) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (ativo) "Remover dos favoritos" else "Adicionar aos favoritos",
            tint = if (ativo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
