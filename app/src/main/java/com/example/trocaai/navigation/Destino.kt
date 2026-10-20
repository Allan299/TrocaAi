package com.example.trocaai.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destino(
    val rota: String,
    val rotulo: String,
    val icone: ImageVector
) {
    INICIO("inicio", "Início", Icons.Filled.Home),
    BUSCAR("buscar", "Buscar", Icons.Filled.Search),
    ANUNCIAR("anunciar", "Anunciar", Icons.Filled.Add),
    MENSAGENS("conversas", "Mensagens", Icons.AutoMirrored.Filled.Send),
    FAVORITOS("favoritos", "Favoritos", Icons.Filled.Favorite)
}
