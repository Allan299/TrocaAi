package com.example.trocaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trocaai.data.AnunciosRepository
import com.example.trocaai.data.AuthRepository
import com.example.trocaai.model.Usuario
import com.example.trocaai.ui.components.BarraTopo
import com.example.trocaai.ui.components.MensagemErro
import com.example.trocaai.ui.components.itensAnuncios
import com.example.trocaai.ui.components.rememberEstadoLista
import com.example.trocaai.util.formatar
import com.example.trocaai.util.mensagemEmPortugues

// Perfil básico de quem publicou o anúncio: nome, curso, data de entrada e anúncios disponíveis.
// O e-mail não é exibido de propósito (privacidade).
@Composable
fun AnuncianteScreen(uid: String, onVoltar: () -> Unit, onAbrirAnuncio: (String) -> Unit) {
    val authRepositorio = remember { AuthRepository() }
    val anunciosRepositorio = remember { AnunciosRepository() }
    val estado = rememberEstadoLista(uid) { anunciosRepositorio.observarDoUsuario(uid, apenasDisponiveis = true) }

    var perfil by remember { mutableStateOf<Usuario?>(null) }
    var carregandoPerfil by remember { mutableStateOf(true) }
    var erroPerfil by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        try {
            perfil = authRepositorio.buscarPerfil(uid)
        } catch (e: Exception) {
            erroPerfil = e.mensagemEmPortugues()
        } finally {
            carregandoPerfil = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BarraTopo(titulo = "Anunciante", onVoltar = onVoltar)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    val dados = perfil
                    val nomeAnunciante = dados?.nome?.ifBlank { null }
                        ?: estado.anuncios?.firstOrNull()?.vendedorNome?.ifBlank { null }
                        ?: "Anunciante"

                    val temAnuncios = !estado.anuncios.isNullOrEmpty()

                    when {
                        carregandoPerfil && estado.anuncios == null -> CircularProgressIndicator()
                        erroPerfil != null && !temAnuncios -> MensagemErro(erroPerfil.orEmpty())
                        dados == null && !temAnuncios && !carregandoPerfil && estado.anuncios != null -> {
                            Text("Usuário não encontrado")
                        }
                        else -> {
                            Text(
                                nomeAnunciante,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            if (dados?.curso?.isNotBlank() == true) {
                                Text(dados.curso, style = MaterialTheme.typography.bodyLarge)
                            }
                            dados?.criadoEm?.let {
                                Text(
                                    "Membro desde ${it.formatar()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Anúncios disponíveis",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            itensAnuncios(
                estado = estado,
                textoVazio = "Este usuário não tem anúncios disponíveis.",
                onAbrirAnuncio = onAbrirAnuncio
            )
        }
    }
}
