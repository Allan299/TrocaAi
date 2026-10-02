package com.example.trocaai.assistente

import com.example.trocaai.model.TipoAnuncio
import java.util.Locale

// Filtros extraídos de uma frase do usuário. É só um conjunto de dados: o interpretador
// nunca monta consultas livres, apenas preenche estes campos.
data class ConsultaAnuncios(
    val termos: List<String> = emptyList(),        // palavras normalizadas, usadas na comparação
    val termosExibicao: List<String> = termos,     // as mesmas palavras como o usuário escreveu
    val categoria: String? = null,
    val tipo: TipoAnuncio? = null,
    val precoMin: Double? = null,
    val precoMax: Double? = null
) {
    val vazia: Boolean
        get() = termos.isEmpty() && categoria == null && tipo == null && precoMin == null && precoMax == null

    // Descrição em português, usada nas respostas do assistente.
    fun descricao(): String {
        val partes = mutableListOf<String>()
        categoria?.let { partes += it.lowercase() }
        if (termosExibicao.isNotEmpty()) partes += "\"" + termosExibicao.joinToString(" ") + "\""
        tipo?.let { partes += "tipo " + it.rotulo.lowercase() }
        precoMin?.let { partes += "a partir de " + moeda(it) }
        precoMax?.let { partes += "até " + moeda(it) }
        return if (partes.isEmpty()) "os anúncios mais recentes" else partes.joinToString(", ")
    }

    private fun moeda(valor: Double) = "R$ " + String.format(Locale.forLanguageTag("pt-BR"), "%.2f", valor)
}

// O que o interpretador entendeu. Qualquer mecanismo (regras, IA local...) só pode devolver
// uma destas opções, o que limita o que o assistente consegue fazer.
sealed interface Intencao {
    data class Buscar(val consulta: ConsultaAnuncios) : Intencao
    data object Saudacao : Intencao
    data object Ajuda : Intencao
    data object AcaoNaoSuportada : Intencao   // pediu para alterar/excluir/publicar por conversa
    data object NaoEntendi : Intencao
}
