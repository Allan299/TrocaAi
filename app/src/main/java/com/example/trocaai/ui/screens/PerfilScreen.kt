package com.example.trocaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trocaai.data.AnunciosRepository
import com.example.trocaai.data.AuthRepository
import com.example.trocaai.data.ResultadoLista
import com.example.trocaai.ui.components.BotaoPrincipal
import com.example.trocaai.ui.components.CampoTexto
import com.example.trocaai.ui.components.itensAnuncios
import com.example.trocaai.ui.components.rememberEstadoLista
import com.example.trocaai.util.Validacao
import com.example.trocaai.util.mensagemEmPortugues
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun PerfilScreen(
    usuario: FirebaseUser?,
    onSair: () -> Unit,
    onAbrirAnuncio: (String) -> Unit,
    mostrarMensagem: (String) -> Unit
) {
    val authRepositorio = remember { AuthRepository() }
    val anunciosRepositorio = remember { AnunciosRepository() }
    val escopo = rememberCoroutineScope()
    val uid = usuario?.uid

    // nome = último nome salvo; campo = o que está digitado no campo de edição
    var nome by remember { mutableStateOf(usuario?.displayName.orEmpty()) }
    var campo by remember { mutableStateOf(usuario?.displayName.orEmpty()) }
    var erroNome by remember { mutableStateOf<String?>(null) }
    var salvando by remember { mutableStateOf(false) }

    // Aqui entram também os anúncios indisponíveis: o dono precisa poder reativá-los.
    val estado = rememberEstadoLista(uid) {
        val fluxo: Flow<ResultadoLista> =
            if (uid == null) emptyFlow() else anunciosRepositorio.observarDoUsuario(uid, apenasDisponiveis = false)
        fluxo
    }

    // O nome vem do Firestore; enquanto carrega (ou se falhar) usa o do Authentication.
    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            authRepositorio.buscarPerfil(uid)?.nome?.takeIf { it.isNotBlank() }?.let {
                nome = it
                campo = it
            }
        } catch (e: Exception) {
            // Mantém o nome atual
        }
    }

    fun salvarNome() {
        erroNome = Validacao.nome(campo)
        if (erroNome != null) return
        val novoNome = campo.trim()
        salvando = true
        escopo.launch {
            try {
                authRepositorio.atualizarNome(novoNome)
                nome = novoNome
                campo = novoNome
                mostrarMensagem("Nome atualizado com sucesso!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mostrarMensagem(e.mensagemEmPortugues())
            } finally {
                salvando = false
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Text(nome.ifBlank { "Estudante" }, style = MaterialTheme.typography.headlineMedium)
                Text(
                    usuario?.email.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CampoTexto(
                    valor = campo,
                    aoMudar = { campo = it },
                    rotulo = "Nome",
                    erro = erroNome,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                BotaoPrincipal(
                    texto = "Salvar nome",
                    onClick = ::salvarNome,
                    carregando = salvando,
                    enabled = campo.trim() != nome,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = onSair, modifier = Modifier.fillMaxWidth()) {
                    Text("Sair da conta")
                }
            }
        }
        item {
            Text(
                "Meus anúncios",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        itensAnuncios(
            estado = estado,
            textoVazio = "Você ainda não publicou nenhum anúncio.",
            onAbrirAnuncio = onAbrirAnuncio
        )
    }
}
