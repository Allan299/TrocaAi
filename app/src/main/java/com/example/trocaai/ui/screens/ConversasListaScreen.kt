package com.example.trocaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.trocaai.data.ChatRepository
import com.example.trocaai.model.Conversa
import com.example.trocaai.ui.components.MensagemErro
import com.example.trocaai.util.formatar
import com.example.trocaai.util.mensagemEmPortugues
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Composable
fun ConversasListaScreen(
    uid: String?,
    onAbrirChat: (String) -> Unit
) {
    val repository = remember { ChatRepository() }
    val scope = rememberCoroutineScope()
    var conversas by remember { mutableStateOf<List<Conversa>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var conversaParaExcluir by remember { mutableStateOf<Conversa?>(null) }

    LaunchedEffect(uid) {
        if (uid == null) {
            conversas = emptyList()
            return@LaunchedEffect
        }
        repository.observarConversasDoUsuario(uid)
            .catch { erro = it.mensagemEmPortugues() }
            .collect { conversas = it }
    }

    fun excluirConversa(conversa: Conversa) {
        scope.launch {
            try {
                repository.excluirConversa(conversa.id)
            } catch (e: Exception) {
                erro = e.mensagemEmPortugues()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Mensagens",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val lista = conversas
        when {
            erro != null -> MensagemErro(erro.orEmpty())
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
                    "Você ainda não possui conversas ativas.\nInicie um papo pela tela de um anúncio!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(lista, key = { it.id }) { conversa ->
                    CartaoConversa(
                        conversa = conversa,
                        uidAtual = uid.orEmpty(),
                        onClick = { onAbrirChat(conversa.id) },
                        onExcluir = { conversaParaExcluir = conversa }
                    )
                }
            }
        }
    }

    if (conversaParaExcluir != null) {
        AlertDialog(
            onDismissRequest = { conversaParaExcluir = null },
            title = { Text("Excluir conversa?") },
            text = { Text("Esta ação apagaria permanentemente todas as mensagens trocadas nesta conversa.") },
            confirmButton = {
                TextButton(onClick = {
                    conversaParaExcluir?.let { excluirConversa(it) }
                    conversaParaExcluir = null
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { conversaParaExcluir = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CartaoConversa(
    conversa: Conversa,
    uidAtual: String,
    onClick: () -> Unit,
    onExcluir: () -> Unit
) {
    val interlocutorNome = if (conversa.compradorId == uidAtual) {
        conversa.vendedorNome.ifBlank { "Vendedor" }
    } else {
        conversa.compradorNome.ifBlank { "Comprador" }
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (conversa.anuncioFotoUrl.isNotBlank()) {
                AsyncImage(
                    model = conversa.anuncioFotoUrl,
                    contentDescription = conversa.anuncioTitulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = interlocutorNome,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    conversa.atualizadoEm?.let { data ->
                        Text(
                            text = data.formatar(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (conversa.anuncioTitulo.isNotBlank()) {
                    Text(
                        text = "Anúncio: ${conversa.anuncioTitulo}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = conversa.ultimaMensagem.ifBlank { "Nova conversa" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onExcluir) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Excluir conversa",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
