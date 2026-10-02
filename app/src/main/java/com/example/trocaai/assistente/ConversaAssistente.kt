package com.example.trocaai.assistente

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.trocaai.data.AnunciosRepository
import com.example.trocaai.data.FavoritosRepository
import com.example.trocaai.model.Anuncio
import com.example.trocaai.util.mensagemEmPortugues
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

enum class Autor { USUARIO, ASSISTENTE }

// Pergunta de confirmação antes de uma ação que altera dados (adicionar aos favoritos).
data class Confirmacao(val anuncioId: String, val titulo: String, val pendente: Boolean = true)

data class MensagemChat(
    val id: Long,
    val autor: Autor,
    val texto: String,
    val anuncios: List<Anuncio> = emptyList(),
    val confirmacao: Confirmacao? = null
)

// Guarda a conversa e coordena: interpretar -> consultar o Firestore (somente leitura) -> responder.
// A única ação que altera dados é "favoritar", e ela só roda depois que o usuário confirma.
class ConversaAssistente(
    private val interpretador: InterpretadorDeConsulta,
    private val anuncios: AnunciosRepository,
    private val favoritos: FavoritosRepository
) {
    val mensagens = mutableStateListOf<MensagemChat>()
    var digitando by mutableStateOf(false)
        private set

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var proximoId = 0L

    init {
        responder(
            "Olá! Sou o assistente do TrocaAí. Diga o que você procura e eu busco nos anúncios do app. " +
                "Por exemplo: \"livros de programação\" ou \"materiais gratuitos\"."
        )
    }

    fun enviar(texto: String) {
        val limpo = texto.trim()
        if (limpo.isEmpty() || digitando) return
        adicionar(Autor.USUARIO, limpo)
        digitando = true
        escopo.launch {
            try {
                tratar(interpretador.interpretar(limpo))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                responder("Não consegui buscar agora. " + e.mensagemEmPortugues())
            } finally {
                digitando = false
            }
        }
    }

    private suspend fun tratar(intencao: Intencao) {
        when (intencao) {
            Intencao.Saudacao -> responder("Olá! O que você está procurando hoje?")
            Intencao.Ajuda -> responder(TEXTO_AJUDA)
            Intencao.AcaoNaoSuportada -> responder(
                "Eu só faço buscas: não consigo editar, excluir ou publicar anúncios por conversa. " +
                    "Use as telas do app para isso. Com a sua confirmação, posso adicionar um resultado aos favoritos."
            )
            Intencao.NaoEntendi -> responder(
                "Não entendi o que você procura. Tente algo como \"livros de até 50 reais\", " +
                    "\"calculadora científica\" ou \"materiais gratuitos\"."
            )
            is Intencao.Buscar -> buscar(intencao.consulta)
        }
    }

    private suspend fun buscar(consulta: ConsultaAnuncios) {
        val resultado = anuncios.listarRecentes().filtrarPor(consulta)
        val total = resultado.anuncios.size
        val descricao = consulta.descricao()
        when {
            total == 0 -> responder(
                "Não encontrei nenhum anúncio para $descricao. Tente outras palavras, retire o filtro " +
                    "de preço ou de categoria, ou use a aba Buscar."
            )
            else -> {
                val mostrados = resultado.anuncios.take(MAXIMO_RESULTADOS)
                val abertura = if (resultado.parcial) {
                    "Não encontrei anúncios com todas as palavras ($descricao), mas estes combinam com alguma delas:"
                } else {
                    "Encontrei $total anúncio${if (total > 1) "s" else ""} para $descricao."
                }
                val rodape = if (total > MAXIMO_RESULTADOS) {
                    " Mostrando os $MAXIMO_RESULTADOS mais recentes; refine a busca para ver outros."
                } else ""
                responder(abertura + rodape, mostrados)
            }
        }
    }

    // --- Favoritar com confirmação ---

    fun pedirFavoritar(anuncio: Anuncio, jaFavorito: Boolean) {
        if (jaFavorito) {
            responder("\"${anuncio.titulo}\" já está nos seus favoritos.")
        } else {
            responder(
                "Quer adicionar \"${anuncio.titulo}\" aos seus favoritos?",
                confirmacao = Confirmacao(anuncio.id, anuncio.titulo)
            )
        }
    }

    fun confirmar(idMensagem: Long) {
        val confirmacao = encerrarConfirmacao(idMensagem) ?: return
        escopo.launch {
            try {
                favoritos.alternar(confirmacao.anuncioId, favoritar = true)
                responder("Pronto! Adicionei \"${confirmacao.titulo}\" aos seus favoritos.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                responder("Não consegui adicionar aos favoritos. " + e.mensagemEmPortugues())
            }
        }
    }

    fun cancelar(idMensagem: Long) {
        if (encerrarConfirmacao(idMensagem) != null) responder("Tudo bem, não alterei nada.")
    }

    // Marca a confirmação como respondida (some os botões) e devolve os dados, ou null se já foi usada.
    private fun encerrarConfirmacao(idMensagem: Long): Confirmacao? {
        val indice = mensagens.indexOfFirst { it.id == idMensagem }
        val confirmacao = mensagens.getOrNull(indice)?.confirmacao?.takeIf { it.pendente } ?: return null
        mensagens[indice] = mensagens[indice].copy(confirmacao = confirmacao.copy(pendente = false))
        return confirmacao
    }

    fun encerrar() = escopo.cancel()

    private fun responder(
        texto: String,
        anuncios: List<Anuncio> = emptyList(),
        confirmacao: Confirmacao? = null
    ) = adicionar(Autor.ASSISTENTE, texto, anuncios, confirmacao)

    private fun adicionar(
        autor: Autor,
        texto: String,
        anuncios: List<Anuncio> = emptyList(),
        confirmacao: Confirmacao? = null
    ) {
        mensagens += MensagemChat(proximoId++, autor, texto, anuncios, confirmacao)
    }

    private companion object {
        const val MAXIMO_RESULTADOS = 5
        const val TEXTO_AJUDA =
            "Eu ajudo a encontrar materiais publicados no TrocaAí. Você pode pedir, por exemplo:\n" +
                "• \"Quero livros de programação\"\n" +
                "• \"Mostre materiais gratuitos\"\n" +
                "• \"Procuro uma calculadora científica\"\n" +
                "• \"Livros de até 50 reais\"\n" +
                "Eu só consulto anúncios. Para favoritar um resultado, peço a sua confirmação antes."
    }
}
