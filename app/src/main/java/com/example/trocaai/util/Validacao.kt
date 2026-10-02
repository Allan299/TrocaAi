package com.example.trocaai.util

import android.util.Patterns

// Cada função devolve a mensagem de erro em português, ou null se o campo estiver válido.
object Validacao {
    fun nome(nome: String): String? = when {
        nome.isBlank() -> "Informe seu nome"
        nome.trim().length < 3 -> "O nome deve ter pelo menos 3 letras"
        else -> null
    }

    fun email(email: String): String? = when {
        email.isBlank() -> "Informe seu e-mail"
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "E-mail inválido"
        else -> null
    }

    fun senha(senha: String): String? = when {
        senha.isBlank() -> "Informe sua senha"
        senha.length < 6 -> "A senha deve ter pelo menos 6 caracteres"
        else -> null
    }

    fun tituloAnuncio(titulo: String): String? = when {
        titulo.isBlank() -> "Informe o título"
        titulo.trim().length < 3 -> "O título deve ter pelo menos 3 letras"
        titulo.trim().length > 100 -> "O título deve ter no máximo 100 caracteres"
        else -> null
    }

    fun descricao(descricao: String): String? = when {
        descricao.isBlank() -> "Informe a descrição"
        descricao.trim().length > 1000 -> "A descrição deve ter no máximo 1000 caracteres"
        else -> null
    }

    fun categoria(categoria: String?): String? =
        if (categoria.isNullOrBlank()) "Escolha uma categoria" else null

    // Aceita "12,50" ou "12.50"
    fun converterPreco(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

    fun preco(texto: String): String? {
        val valor = converterPreco(texto)
        return when {
            texto.isBlank() -> "Informe o preço"
            valor == null -> "Preço inválido"
            valor <= 0.0 -> "O preço deve ser maior que zero"
            valor > 100000.0 -> "Preço muito alto"
            else -> null
        }
    }
}
