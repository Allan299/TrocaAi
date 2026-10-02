package com.example.trocaai.util

import com.example.trocaai.model.Anuncio
import com.example.trocaai.model.TipoAnuncio
import java.util.Locale

// Resultado do cálculo de sugestão de preço.
sealed interface SugestaoPreco {
    // mediana = valor sugerido | minimo/maximo = faixa observada | quantidade = anúncios usados
    // baseadoNoTitulo = true quando só foram usados anúncios com palavras parecidas com o título
    data class Estimativa(
        val mediana: Double,
        val minimo: Double,
        val maximo: Double,
        val quantidade: Int,
        val baseadoNoTitulo: Boolean
    ) : SugestaoPreco

    // Poucos dados: o usuário deve definir o preço manualmente.
    data class DadosInsuficientes(val quantidade: Int) : SugestaoPreco
}

// Como a sugestão é calculada (explicação para a apresentação):
//
// 1. Candidatos: anúncios JÁ cadastrados que sejam de VENDA, tenham preço maior que zero e
//    sejam da MESMA CATEGORIA do anúncio novo. Doações, trocas e anúncios sem preço são ignorados.
//    (Na edição, o próprio anúncio editado também é ignorado.)
// 2. Semelhança: entre os candidatos, separamos os que têm pelo menos uma palavra em comum
//    com o título digitado (ex.: "Cálculo Vol. 2" e "Cálculo Vol. 1" compartilham "calculo").
// 3. Amostra: se houver pelo menos MINIMO_AMOSTRAS anúncios semelhantes, usamos só eles;
//    senão, se a categoria inteira tiver pelo menos MINIMO_AMOSTRAS, usamos a categoria inteira;
//    senão, não há dados suficientes e nenhum valor é sugerido.
// 4. Cálculo: a MEDIANA dos preços da amostra (valor do meio da lista ordenada; com quantidade par,
//    média dos dois do meio). Usamos a mediana, e não a média, porque um anúncio com preço
//    muito alto ou muito baixo quase não a altera.
//
// É só uma estimativa com base nos anúncios do próprio app; não consulta preços externos.
object CalculadoraDePreco {
    const val MINIMO_AMOSTRAS = 3

    fun sugerir(
        anuncios: List<Anuncio>,
        categoria: String,
        titulo: String,
        ignorarId: String? = null
    ): SugestaoPreco {
        val daCategoria = anuncios.filter {
            it.tipo == TipoAnuncio.VENDA &&
                (it.preco ?: 0.0) > 0.0 &&
                it.categoria == categoria &&
                it.id != ignorarId
        }

        val palavrasDoTitulo = palavrasSignificativas(titulo)
        val parecidos = daCategoria.filter { anuncio ->
            palavrasSignificativas(anuncio.titulo).any { it in palavrasDoTitulo }
        }

        val (amostra, porTitulo) = when {
            parecidos.size >= MINIMO_AMOSTRAS -> parecidos to true
            daCategoria.size >= MINIMO_AMOSTRAS -> daCategoria to false
            else -> return SugestaoPreco.DadosInsuficientes(daCategoria.size)
        }

        val precos = amostra.mapNotNull { it.preco }
        return SugestaoPreco.Estimativa(
            mediana = mediana(precos),
            minimo = precos.min(),
            maximo = precos.max(),
            quantidade = precos.size,
            baseadoNoTitulo = porTitulo
        )
    }

    fun mediana(valores: List<Double>): Double {
        val ordenados = valores.sorted()
        val meio = ordenados.size / 2
        val mediana = if (ordenados.size % 2 == 1) ordenados[meio] else (ordenados[meio - 1] + ordenados[meio]) / 2
        return Math.round(mediana * 100) / 100.0 // duas casas decimais
    }

    // Palavras do título sem acento, com 3+ letras, sem plural simples e sem palavras comuns.
    private fun palavrasSignificativas(titulo: String): Set<String> =
        Regex("[a-z0-9]+").findAll(titulo.normalizar())
            .map { it.value }
            .filter { it.length >= 3 && it !in PALAVRAS_COMUNS }
            .map { if (it.length > 3 && it.endsWith("s")) it.dropLast(1) else it }
            .toSet()

    private val PALAVRAS_COMUNS = setOf(
        "para", "com", "uma", "uns", "dos", "das", "vol", "volume", "edicao", "usado", "usada", "novo", "nova"
    )
}

// "45,00" (formato brasileiro, também usado para preencher o campo de preço)
fun Double.paraCampoDePreco(): String = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", this)
