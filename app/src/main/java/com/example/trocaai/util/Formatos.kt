package com.example.trocaai.util

import com.example.trocaai.model.Anuncio
import com.example.trocaai.model.TipoAnuncio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ptBR = Locale.forLanguageTag("pt-BR")

fun Anuncio.textoPreco(): String = when {
    tipo == TipoAnuncio.VENDA && preco != null -> "R$ " + String.format(ptBR, "%.2f", preco)
    tipo == TipoAnuncio.TROCA -> "Aceita troca"
    else -> "Gratuito"
}

fun Date.formatar(): String = SimpleDateFormat("dd/MM/yyyy", ptBR).format(this)
