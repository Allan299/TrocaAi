package com.example.trocaai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.trocaai.data.ChatRepository
import com.example.trocaai.model.Conversa
import com.example.trocaai.model.Mensagem
import com.example.trocaai.util.formatar
import com.example.trocaai.util.mensagemEmPortugues
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversaId: String,
    onVoltar: () -> Unit,
    onAbrirAnuncio: (String) -> Unit
) {
    val repository = remember { ChatRepository() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var conversa by remember { mutableStateOf<Conversa?>(null) }
    var mensagens by remember { mutableStateOf<List<Mensagem>?>(null) }
    var textoMensagem by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val uidAtual = repository.uidAtual

    LaunchedEffect(conversaId) {
        repository.observarConversa(conversaId)
            .catch { erro = it.mensagemEmPortugues() }
            .collect { conversa = it }
    }

    LaunchedEffect(conversaId) {
        repository.observarMensagens(conversaId)
            .catch { erro = it.mensagemEmPortugues() }
            .collect { mensagens = it }
    }

    // Auto-scroll para a última mensagem enviada/recebida
    LaunchedEffect(mensagens?.size) {
        val tamanho = mensagens?.size ?: 0
        if (tamanho > 0) {
            listState.animateScrollToItem(tamanho - 1)
        }
    }

    fun enviar() {
        val texto = textoMensagem.trim()
        if (texto.isBlank() || enviando) return

        textoMensagem = ""
        enviando = true
        scope.launch {
            try {
                repository.enviarMensagem(conversaId, texto)
            } catch (e: Exception) {
                erro = e.mensagemEmPortugues()
            } finally {
                enviando = false
            }
        }
    }

    val conversaAtual = conversa
    val interlocutorNome = if (conversaAtual?.compradorId == uidAtual) {
        conversaAtual?.vendedorNome
    } else {
        conversaAtual?.compradorNome
    } ?: "Chat"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!conversaAtual?.anuncioFotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = conversaAtual?.anuncioFotoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(
                                interlocutorNome,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (!conversaAtual?.anuncioTitulo.isNullOrBlank()) {
                                Surface(
                                    onClick = {
                                        conversaAtual?.anuncioId?.let { id ->
                                            if (id.isNotBlank()) onAbrirAnuncio(id)
                                        }
                                    }
                                ) {
                                    Text(
                                        "Anúncio: ${conversaAtual?.anuncioTitulo}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textoMensagem,
                    onValueChange = { textoMensagem = it },
                    placeholder = { Text("Digite sua mensagem...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 4
                )
                IconButton(
                    onClick = ::enviar,
                    enabled = textoMensagem.isNotBlank() && !enviando
                ) {
                    if (enviando) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val lista = mensagens
            when {
                lista == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                lista.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Inicie a conversa enviando uma mensagem!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(lista, key = { it.id }) { mensagem ->
                        BalaoMensagem(
                            mensagem = mensagem,
                            ehEu = mensagem.remetenteId == uidAtual
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalaoMensagem(mensagem: Mensagem, ehEu: Boolean) {
    val alinhamento = if (ehEu) Alignment.End else Alignment.Start
    val corFundo = if (ehEu) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val corTexto = if (ehEu) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    val formatoForma = if (ehEu) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alinhamento
    ) {
        Box(
            modifier = Modifier
                .clip(formatoForma)
                .background(corFundo)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = mensagem.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = corTexto
                )
                mensagem.criadoEm?.let { data ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.formatar(),
                        style = MaterialTheme.typography.labelSmall,
                        color = corTexto.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
