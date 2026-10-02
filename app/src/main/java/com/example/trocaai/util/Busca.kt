package com.example.trocaai.util

import com.example.trocaai.model.Anuncio
import com.example.trocaai.model.TipoAnuncio
import java.text.Normalizer

// Minúsculas e sem acentos: "Cálculo" casa com "calculo".
fun String.normalizar(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .lowercase()

// Busca simples feita no aparelho sobre a lista já carregada do Firestore:
// título contém o texto digitado, e (opcionalmente) categoria e tipo iguais.
fun List<Anuncio>.filtrar(texto: String, categoria: String?, tipo: TipoAnuncio?): List<Anuncio> {
    val busca = texto.trim().normalizar()
    return filter { anuncio ->
        (busca.isEmpty() || anuncio.titulo.normalizar().contains(busca)) &&
            (categoria == null || anuncio.categoria == categoria) &&
            (tipo == null || anuncio.tipo == tipo)
    }
}
