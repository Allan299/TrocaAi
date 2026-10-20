package com.example.trocaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.trocaai.data.AnunciosRepository
import com.example.trocaai.model.Anuncio
import com.example.trocaai.ui.components.BarraTopo
import com.example.trocaai.ui.components.BotaoFavorito
import com.example.trocaai.ui.components.EtiquetaTipo
import com.example.trocaai.ui.components.MensagemErro
import com.example.trocaai.util.formatar
import com.example.trocaai.util.mensagemEmPortugues
import com.example.trocaai.util.textoPreco
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Composable
fun AnuncioDetalheScreen(
    anuncioId: String,
    onVoltar: () -> Unit,
    onEditar: (String) -> Unit,
    onExcluido: () -> Unit,
    onAbrirAnunciante: (String) -> Unit,
    onIniciarChat: (Anuncio) -> Unit,
    mostrarMensagem: (String) -> Unit
) {
    val repositorio = remember { AnunciosRepository() }
    val escopo = rememberCoroutineScope()

    var anuncio by remember { mutableStateOf<Anuncio?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var confirmarExclusao by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf(false) }

    LaunchedEffect(anuncioId) {
        repositorio.observar(anuncioId)
            .catch { erro = it.mensagemEmPortugues(); carregando = false }
            .collect { anuncio = it; carregando = false }
    }

    fun excluir(alvo: Anuncio) {
        excluindo = true
        escopo.launch {
            try {
                repositorio.excluir(alvo)
                mostrarMensagem("Anúncio excluído com sucesso")
                onExcluido()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mostrarMensagem(e.mensagemEmPortugues())
            } finally {
                excluindo = false
            }
        }
    }

    fun alterarDisponibilidade(alvo: Anuncio) {
        val novoValor = !alvo.disponivel
        escopo.launch {
            try {
                repositorio.alterarDisponibilidade(alvo.id, novoValor)
                mostrarMensagem(if (novoValor) "Anúncio reativado" else "Anúncio marcado como indisponível")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mostrarMensagem(e.mensagemEmPortugues())
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BarraTopo(titulo = "Detalhes", onVoltar = onVoltar)

        val atual = anuncio
        when {
            carregando -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            erro != null -> MensagemErro(erro.orEmpty(), Modifier.padding(16.dp))

            atual == null -> Text("Anúncio não encontrado.", Modifier.padding(16.dp))

            else -> {
                // Os botões só aparecem para o dono (as regras do Firestore também exigem isso)
                val ehDono = atual.vendedorId == repositorio.uidAtual

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (atual.fotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = atual.fotoUrl,
                            contentDescription = atual.titulo,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                atual.categoria,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EtiquetaTipo(atual.tipo)
                                BotaoFavorito(atual.id)
                            }
                        }
                        if (!atual.disponivel) {
                            Text(
                                "Este anúncio está indisponível.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(atual.titulo, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            atual.textoPreco(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Conservação: ${atual.conservacao.rotulo}", style = MaterialTheme.typography.bodyMedium)
                        Text(atual.descricao, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            atual.criadoEm?.let { "Publicado em ${it.formatar()}" } ?: "Publicando...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = { onAbrirAnunciante(atual.vendedorId) }) {
                            Text("Anunciante: ${atual.vendedorNome.ifBlank { "ver perfil" }}")
                        }

                        if (!ehDono && atual.disponivel) {
                            Button(
                                onClick = { onIniciarChat(atual) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text("Conversar com o vendedor")
                            }
                        }

                        if (ehDono) {
                            OutlinedButton(onClick = { alterarDisponibilidade(atual) }, enabled = !excluindo) {
                                Text(if (atual.disponivel) "Marcar como indisponível" else "Reativar anúncio")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onEditar(atual.id) }, enabled = !excluindo) {
                                    Text("Editar")
                                }
                                OutlinedButton(
                                    onClick = { confirmarExclusao = true },
                                    enabled = !excluindo,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(if (excluindo) "Excluindo..." else "Excluir")
                                }
                            }
                        }
                    }
                }

                if (confirmarExclusao) {
                    AlertDialog(
                        onDismissRequest = { confirmarExclusao = false },
                        title = { Text("Excluir anúncio?") },
                        text = { Text("Esta ação não pode ser desfeita.") },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmarExclusao = false
                                excluir(atual)
                            }) { Text("Excluir") }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") }
                        }
                    )
                }
            }
        }
    }
}
