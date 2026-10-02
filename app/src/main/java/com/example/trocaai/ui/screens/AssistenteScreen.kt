package com.example.trocaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.trocaai.assistente.Autor
import com.example.trocaai.assistente.ConversaAssistente
import com.example.trocaai.assistente.MensagemChat
import com.example.trocaai.model.Anuncio
import com.example.trocaai.ui.components.BarraTopo
import com.example.trocaai.ui.components.EtiquetaTipo
import com.example.trocaai.ui.components.LocalFavoritos
import com.example.trocaai.util.textoPreco

private val SUGESTOES = listOf(
    "Livros de programação",
    "Materiais gratuitos",
    "Calculadora científica",
    "Livros de até 50 reais"
)

@Composable
fun AssistenteScreen(
    conversa: ConversaAssistente,
    onVoltar: () -> Unit,
    onAbrirAnuncio: (String) -> Unit
) {
    val favoritos = LocalFavoritos.current
    val listaEstado = rememberLazyListState()
    var texto by remember { mutableStateOf("") }

    val mensagens = conversa.mensagens
    val mostrarSugestoes = mensagens.size == 1
    val totalItens = mensagens.size + (if (conversa.digitando) 1 else 0) + (if (mostrarSugestoes) 1 else 0)

    // Rola até a última mensagem sempre que algo novo aparece
    LaunchedEffect(totalItens) {
        if (totalItens > 0) listaEstado.animateScrollToItem(totalItens - 1)
    }

    fun enviar(mensagem: String) {
        conversa.enviar(mensagem)
        texto = ""
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        BarraTopo(titulo = "Assistente", onVoltar = onVoltar)

        LazyColumn(
            state = listaEstado,
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(mensagens, key = { it.id }) { mensagem ->
                BolhaMensagem(
                    mensagem = mensagem,
                    idsFavoritos = favoritos.ids,
                    onAbrirAnuncio = onAbrirAnuncio,
                    onFavoritar = { conversa.pedirFavoritar(it, it.id in favoritos.ids) },
                    onConfirmar = { conversa.confirmar(mensagem.id) },
                    onCancelar = { conversa.cancelar(mensagem.id) }
                )
            }
            if (conversa.digitando) {
                item {
                    Text(
                        "Procurando...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (mostrarSugestoes) {
                item { Sugestoes(onEscolher = ::enviar) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                placeholder = { Text("O que você procura?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { enviar(texto) }),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { enviar(texto) },
                enabled = texto.isNotBlank() && !conversa.digitando
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Sugestoes(onEscolher: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Experimente:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SUGESTOES.forEach { sugestao ->
                SuggestionChip(onClick = { onEscolher(sugestao) }, label = { Text(sugestao) })
            }
        }
    }
}

@Composable
private fun BolhaMensagem(
    mensagem: MensagemChat,
    idsFavoritos: Set<String>,
    onAbrirAnuncio: (String) -> Unit,
    onFavoritar: (Anuncio) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val doUsuario = mensagem.autor == Autor.USUARIO

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (doUsuario) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (doUsuario) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(mensagem.texto, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
        }

        mensagem.anuncios.forEach { anuncio ->
            ResultadoNoChat(
                anuncio = anuncio,
                favorito = anuncio.id in idsFavoritos,
                onAbrir = { onAbrirAnuncio(anuncio.id) },
                onFavoritar = { onFavoritar(anuncio) }
            )
        }

        mensagem.confirmacao?.takeIf { it.pendente }?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirmar) { Text("Sim, adicionar") }
                OutlinedButton(onClick = onCancelar) { Text("Não") }
            }
        }
    }
}

// Cartão compacto de um anúncio real: título, preço, tipo e acesso aos detalhes.
@Composable
private fun ResultadoNoChat(
    anuncio: Anuncio,
    favorito: Boolean,
    onAbrir: () -> Unit,
    onFavoritar: () -> Unit
) {
    Card(
        onClick = onAbrir,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    anuncio.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                EtiquetaTipo(anuncio.tipo)
            }
            Text(
                anuncio.textoPreco(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row {
                TextButton(onClick = onAbrir) { Text("Ver detalhes") }
                TextButton(onClick = onFavoritar, enabled = !favorito) {
                    Text(if (favorito) "Nos favoritos" else "Favoritar")
                }
            }
        }
    }
}
