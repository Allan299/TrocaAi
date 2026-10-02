package com.example.trocaai.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trocaai.data.ResultadoLista
import com.example.trocaai.model.Anuncio
import com.example.trocaai.util.mensagemEmPortugues
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

// Estado de uma lista de anúncios vinda do Firestore.
//  anuncios == null  -> ainda carregando
//  erro != null      -> falha ao carregar
//  semConexao        -> só há dados do aparelho (cache) há mais de 2,5 s
class EstadoLista(
    val anuncios: List<Anuncio>?,
    val erro: String?,
    val semConexao: Boolean,
    val tentarNovamente: () -> Unit
)

// Coleta o Flow do repositório. A chave faz a coleta recomeçar (ex.: outro usuário).
@Composable
fun rememberEstadoLista(chave: Any?, criarFluxo: () -> Flow<ResultadoLista>): EstadoLista {
    var tentativa by remember { mutableIntStateOf(0) }
    var resultado by remember { mutableStateOf<ResultadoLista?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var semConexao by remember { mutableStateOf(false) }

    LaunchedEffect(chave, tentativa) {
        erro = null
        criarFluxo()
            .catch {
                Log.e("TrocaAiLista", "Erro ao carregar lista de anúncios", it)
                erro = it.mensagemEmPortugues()
            }
            .collect { resultado = it }
    }

    // Só avisa "sem conexão" se os dados continuarem vindo do cache por um tempo,
    // para não piscar o aviso a cada abertura do app.
    val deCache = resultado?.deCache == true
    LaunchedEffect(deCache) {
        if (deCache) {
            delay(2500)
            semConexao = true
        } else {
            semConexao = false
        }
    }

    return EstadoLista(
        anuncios = resultado?.anuncios,
        erro = erro,
        semConexao = semConexao,
        tentarNovamente = { tentativa++ }
    )
}

// Mostra, dentro de uma LazyColumn: carregamento, erro, falta de conexão, lista vazia ou os cartões.
// "anuncios" pode ser uma versão filtrada de estado.anuncios (busca).
fun LazyListScope.itensAnuncios(
    estado: EstadoLista,
    textoVazio: String,
    onAbrirAnuncio: (String) -> Unit,
    anuncios: List<Anuncio>? = estado.anuncios
) {
    when {
        estado.erro != null -> item {
            MensagemComTentativa(estado.erro, estado.tentarNovamente)
        }
        anuncios == null -> item {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        anuncios.isEmpty() && estado.anuncios.isNullOrEmpty() && estado.semConexao -> item {
            MensagemComTentativa("Sem conexão com a internet", estado.tentarNovamente)
        }
        anuncios.isEmpty() -> item {
            Text(
                textoVazio,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        else -> {
            if (estado.semConexao) item {
                Text(
                    "Sem conexão: mostrando dados salvos no aparelho.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(anuncios, key = { it.id }) { anuncio ->
                CartaoAnuncio(anuncio, onClick = { onAbrirAnuncio(anuncio.id) })
            }
        }
    }
}

@Composable
private fun MensagemComTentativa(mensagem: String, aoTentar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MensagemErro(mensagem)
        OutlinedButton(onClick = aoTentar) { Text("Tentar novamente") }
    }
}
