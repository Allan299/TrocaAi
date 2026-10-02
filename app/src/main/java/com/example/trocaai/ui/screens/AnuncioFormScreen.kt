package com.example.trocaai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.net.Uri
import coil3.compose.AsyncImage
import com.example.trocaai.data.AnunciosRepository
import com.example.trocaai.model.Anuncio
import com.example.trocaai.model.CATEGORIAS
import com.example.trocaai.model.EstadoConservacao
import com.example.trocaai.model.TipoAnuncio
import com.example.trocaai.ui.components.BarraTopo
import com.example.trocaai.ui.components.BotaoPrincipal
import com.example.trocaai.ui.components.CampoTexto
import com.example.trocaai.ui.components.MensagemErro
import com.example.trocaai.ui.components.SeletorOpcoes
import com.example.trocaai.ui.components.CartaoSugestaoPreco
import com.example.trocaai.util.CalculadoraDePreco
import com.example.trocaai.util.Imagem
import com.example.trocaai.util.SugestaoPreco
import com.example.trocaai.util.Validacao
import com.example.trocaai.util.mensagemEmPortugues
import com.example.trocaai.util.paraCampoDePreco
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Formulário usado para criar (anuncioId == null) e para editar um anúncio.
@Composable
fun AnuncioFormScreen(
    anuncioId: String?,
    mostrarMensagem: (String) -> Unit,
    onConcluir: () -> Unit,
    onVoltar: (() -> Unit)? = null
) {
    val repositorio = remember { AnunciosRepository() }
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    val editando = anuncioId != null

    // Dados do anúncio em edição (carregados do Firestore)
    var original by remember { mutableStateOf<Anuncio?>(null) }
    var carregandoDados by remember { mutableStateOf(editando) }
    var erroCarregar by remember { mutableStateOf<String?>(null) }

    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf<String?>(null) }
    var tipo by remember { mutableStateOf(TipoAnuncio.VENDA) }
    var preco by remember { mutableStateOf("") }
    var conservacao by remember { mutableStateOf(EstadoConservacao.BOM) }
    var fotoNova by remember { mutableStateOf<Uri?>(null) }

    var erroTitulo by remember { mutableStateOf<String?>(null) }
    var erroDescricao by remember { mutableStateOf<String?>(null) }
    var erroCategoria by remember { mutableStateOf<String?>(null) }
    var erroPreco by remember { mutableStateOf<String?>(null) }
    var erroGeral by remember { mutableStateOf<String?>(null) }
    var salvando by remember { mutableStateOf(false) }
    var sugestao by remember { mutableStateOf<SugestaoPreco?>(null) }
    var buscandoSugestao by remember { mutableStateOf(false) }

    val seletorFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) fotoNova = uri
    }

    LaunchedEffect(anuncioId) {
        if (anuncioId == null) return@LaunchedEffect
        try {
            val anuncio = repositorio.buscarPorId(anuncioId)
            when {
                anuncio == null -> erroCarregar = "Anúncio não encontrado"
                anuncio.vendedorId != repositorio.uidAtual ->
                    erroCarregar = "Você só pode editar os seus próprios anúncios"
                else -> {
                    original = anuncio
                    titulo = anuncio.titulo
                    descricao = anuncio.descricao
                    categoria = anuncio.categoria
                    tipo = anuncio.tipo
                    preco = anuncio.preco?.paraCampoDePreco().orEmpty()
                    conservacao = anuncio.conservacao
                }
            }
        } catch (e: Exception) {
            erroCarregar = e.mensagemEmPortugues()
        } finally {
            carregandoDados = false
        }
    }

    // Busca os anúncios no Firestore e calcula a mediana (veja CalculadoraDePreco).
    fun pedirSugestaoDePreco() {
        erroCategoria = Validacao.categoria(categoria)
        if (erroCategoria != null) {
            mostrarMensagem("Escolha uma categoria para sugerir um preço")
            return
        }
        buscandoSugestao = true
        escopo.launch {
            try {
                val anuncios = repositorio.listarRecentes()
                sugestao = CalculadoraDePreco.sugerir(anuncios, categoria.orEmpty(), titulo, ignorarId = anuncioId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sugestao = null
                mostrarMensagem(e.mensagemEmPortugues())
            } finally {
                buscandoSugestao = false
            }
        }
    }

    fun salvar() {
        erroTitulo = Validacao.tituloAnuncio(titulo)
        erroDescricao = Validacao.descricao(descricao)
        erroCategoria = Validacao.categoria(categoria)
        erroPreco = if (tipo == TipoAnuncio.VENDA) Validacao.preco(preco) else null
        erroGeral = null
        if (listOf(erroTitulo, erroDescricao, erroCategoria, erroPreco).any { it != null }) return

        val anuncio = (original ?: Anuncio()).copy(
            titulo = titulo.trim(),
            descricao = descricao.trim(),
            categoria = categoria.orEmpty(),
            tipo = tipo,
            preco = if (tipo == TipoAnuncio.VENDA) Validacao.converterPreco(preco) else null,
            conservacao = conservacao
        )

        salvando = true
        escopo.launch {
            try {
                val foto = fotoNova?.let { Imagem.comprimir(contexto, it) }
                repositorio.salvar(anuncio, foto)
                mostrarMensagem(if (editando) "Anúncio atualizado com sucesso!" else "Anúncio publicado com sucesso!")
                onConcluir()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val mensagem = e.mensagemEmPortugues()
                erroGeral = mensagem
                mostrarMensagem(mensagem)
            } finally {
                salvando = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (onVoltar != null) {
            BarraTopo(titulo = "Editar anúncio", onVoltar = onVoltar)
        }

        when {
            carregandoDados -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            erroCarregar != null -> MensagemErro(erroCarregar.orEmpty(), Modifier.padding(16.dp))

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!editando) {
                    Text("Novo anúncio", style = MaterialTheme.typography.headlineMedium)
                }

                CampoTexto(
                    valor = titulo,
                    aoMudar = { titulo = it },
                    rotulo = "Título",
                    erro = erroTitulo,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    isError = erroDescricao != null,
                    supportingText = erroDescricao?.let { { Text(it) } },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                SeletorOpcoes(
                    titulo = "Categoria",
                    opcoes = CATEGORIAS,
                    selecionada = categoria,
                    rotulo = { it },
                    aoSelecionar = {
                        if (it != categoria) sugestao = null // a sugestão antiga era de outra categoria
                        categoria = it
                    },
                    erro = erroCategoria
                )

                SeletorOpcoes(
                    titulo = "Tipo de anúncio",
                    opcoes = TipoAnuncio.entries,
                    selecionada = tipo,
                    rotulo = { it.rotulo },
                    aoSelecionar = { tipo = it }
                )

                if (tipo == TipoAnuncio.VENDA) {
                    CampoTexto(
                        valor = preco,
                        aoMudar = { preco = it },
                        rotulo = "Preço (R$)",
                        erro = erroPreco,
                        tipoTeclado = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = ::pedirSugestaoDePreco,
                        enabled = !buscandoSugestao
                    ) {
                        Text(if (buscandoSugestao) "Calculando..." else "Sugerir preço")
                    }
                    sugestao?.let { resultado ->
                        CartaoSugestaoPreco(
                            sugestao = resultado,
                            categoria = categoria.orEmpty(),
                            onUsarValor = {
                                preco = it.paraCampoDePreco()
                                erroPreco = null
                            }
                        )
                    }
                }

                SeletorOpcoes(
                    titulo = "Estado de conservação",
                    opcoes = EstadoConservacao.entries,
                    selecionada = conservacao,
                    rotulo = { it.rotulo },
                    aoSelecionar = { conservacao = it }
                )

                // Foto (opcional)
                Text("Foto (opcional)", style = MaterialTheme.typography.labelLarge)
                val fotoExibida: Any? = fotoNova ?: original?.fotoUrl?.takeIf { it.isNotBlank() }
                if (fotoExibida != null) {
                    AsyncImage(
                        model = fotoExibida,
                        contentDescription = "Foto do produto",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        seletorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text(if (fotoExibida == null) "Escolher da galeria" else "Trocar foto")
                    }
                    if (fotoNova != null) {
                        TextButton(onClick = { fotoNova = null }) { Text("Remover") }
                    }
                }

                erroGeral?.let { MensagemErro(it) }

                BotaoPrincipal(
                    texto = if (editando) "Salvar alterações" else "Publicar anúncio",
                    onClick = ::salvar,
                    carregando = salvando,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
