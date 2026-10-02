package com.example.trocaai.assistente

import com.example.trocaai.model.Anuncio
import com.example.trocaai.util.normalizar

// parcial = true: nenhum anúncio tinha todas as palavras, então a lista traz os que têm ao menos uma.
data class ResultadoBusca(val anuncios: List<Anuncio>, val parcial: Boolean)

// Aplica uma ConsultaAnuncios a uma lista de anúncios já carregada do Firestore.
// Só filtra: nunca cria, altera ou inventa anúncios. A ordem original (mais recentes primeiro) é mantida.
fun List<Anuncio>.filtrarPor(consulta: ConsultaAnuncios): ResultadoBusca {
    val base = filter { anuncio ->
        (consulta.tipo == null || anuncio.tipo == consulta.tipo) &&
            (consulta.categoria == null || anuncio.categoria == consulta.categoria) &&
            precoDentroDaFaixa(anuncio, consulta)
    }
    if (consulta.termos.isEmpty()) return ResultadoBusca(base, parcial = false)

    fun texto(anuncio: Anuncio) = "${anuncio.titulo} ${anuncio.descricao} ${anuncio.categoria}".normalizar()

    val comTodos = base.filter { a -> texto(a).let { t -> consulta.termos.all { it in t } } }
    if (comTodos.isNotEmpty() || consulta.termos.size == 1) return ResultadoBusca(comTodos, parcial = false)

    val comAlgum = base.filter { a -> texto(a).let { t -> consulta.termos.any { it in t } } }
    return ResultadoBusca(comAlgum, parcial = comAlgum.isNotEmpty())
}

// Com filtro de preço, só entram anúncios que têm preço (doações e trocas ficam de fora).
private fun precoDentroDaFaixa(anuncio: Anuncio, consulta: ConsultaAnuncios): Boolean {
    if (consulta.precoMin == null && consulta.precoMax == null) return true
    val preco = anuncio.preco ?: return false
    return (consulta.precoMin == null || preco >= consulta.precoMin) &&
        (consulta.precoMax == null || preco <= consulta.precoMax)
}
