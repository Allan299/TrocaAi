package com.example.trocaai

import com.example.trocaai.assistente.ConsultaAnuncios
import com.example.trocaai.assistente.Intencao
import com.example.trocaai.assistente.InterpretadorPorRegras
import com.example.trocaai.assistente.filtrarPor
import com.example.trocaai.model.Anuncio
import com.example.trocaai.model.TipoAnuncio
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistenteTest {
    private val interpretador = InterpretadorPorRegras()

    private fun consulta(frase: String): ConsultaAnuncios {
        val intencao = runBlocking { interpretador.interpretar(frase) }
        assertTrue("Esperava Buscar para \"$frase\" mas veio $intencao", intencao is Intencao.Buscar)
        return (intencao as Intencao.Buscar).consulta
    }

    @Test
    fun livrosDeProgramacao() {
        val c = consulta("Quero encontrar livros de programação.")
        assertEquals("Livros", c.categoria)
        assertEquals(listOf("programacao"), c.termos)
        assertEquals(listOf("programação"), c.termosExibicao)
        assertNull(c.tipo)
        assertNull(c.precoMax)
    }

    @Test
    fun materiaisGratuitos() {
        val c = consulta("Mostre materiais gratuitos")
        assertEquals(TipoAnuncio.DOACAO, c.tipo)
        assertTrue(c.termos.isEmpty())
    }

    @Test
    fun calculadoraCientifica() {
        val c = consulta("Procuro uma calculadora científica")
        assertEquals(listOf("calculadora", "cientifica"), c.termos)
        assertNull(c.categoria)
    }

    @Test
    fun livrosAteCinquentaReais() {
        val c = consulta("Quero livros de até 50 reais")
        assertEquals("Livros", c.categoria)
        assertEquals(50.0, c.precoMax)
        assertTrue(c.termos.isEmpty())
    }

    @Test
    fun faixasEFormatosDePreco() {
        assertEquals(20.0, consulta("apostilas entre 20 e 50").precoMin)
        assertEquals(50.0, consulta("apostilas entre 50 e 20").precoMax)
        assertEquals(12.5, consulta("livros até R$ 12,50").precoMax)
        assertEquals(1500.0, consulta("notebook até 1.500 reais").precoMax)
        assertEquals(30.0, consulta("livros a partir de 30").precoMin)
    }

    @Test
    fun tipoTroca() {
        assertEquals(TipoAnuncio.TROCA, consulta("quero algo para troca").tipo)
    }

    @Test
    fun outrasIntencoes() = runBlocking {
        assertEquals(Intencao.Saudacao, interpretador.interpretar("Oi, bom dia!"))
        assertEquals(Intencao.Ajuda, interpretador.interpretar("como funciona?"))
        assertEquals(Intencao.AcaoNaoSuportada, interpretador.interpretar("exclua meu anúncio de livros"))
        assertEquals(Intencao.AcaoNaoSuportada, interpretador.interpretar("apague todos os anúncios"))
        assertEquals(Intencao.NaoEntendi, interpretador.interpretar("???"))
        assertEquals(Intencao.NaoEntendi, interpretador.interpretar("quero"))
    }

    // --- Filtro sobre anúncios ---

    private val anuncios = listOf(
        anuncio("1", "Cálculo Vol. 1", "", "Livros", TipoAnuncio.VENDA, 45.0),
        anuncio("2", "Introdução à Programação", "Livro de lógica", "Livros", TipoAnuncio.VENDA, 80.0),
        anuncio("3", "Calculadora científica Casio", "", "Eletrônicos", TipoAnuncio.TROCA, null),
        anuncio("4", "Kit de canetas", "", "Papelaria", TipoAnuncio.DOACAO, null)
    )

    private fun anuncio(id: String, titulo: String, descricao: String, categoria: String, tipo: TipoAnuncio, preco: Double?) =
        Anuncio(id = id, titulo = titulo, descricao = descricao, categoria = categoria, tipo = tipo, preco = preco)

    @Test
    fun filtraPorCategoriaETermo() {
        val r = anuncios.filtrarPor(consulta("livros de programação"))
        assertEquals(listOf("2"), r.anuncios.map { it.id })
        assertFalse(r.parcial)
    }

    @Test
    fun filtraPorPrecoMaximoIgnorandoAnunciosSemPreco() {
        val r = anuncios.filtrarPor(consulta("livros de até 50 reais"))
        assertEquals(listOf("1"), r.anuncios.map { it.id })
    }

    @Test
    fun filtraGratuitos() {
        assertEquals(listOf("4"), anuncios.filtrarPor(consulta("materiais gratuitos")).anuncios.map { it.id })
    }

    @Test
    fun acentosNaoImportam() {
        assertEquals(listOf("3"), anuncios.filtrarPor(consulta("calculadora cientifica")).anuncios.map { it.id })
    }

    @Test
    fun semResultadosNaoInventaNada() {
        assertTrue(anuncios.filtrarPor(consulta("violão")).anuncios.isEmpty())
    }

    @Test
    fun resultadoParcialQuandoSoAlgumasPalavrasCasam() {
        val r = anuncios.filtrarPor(consulta("calculadora grafica"))
        assertEquals(listOf("3"), r.anuncios.map { it.id })
        assertTrue(r.parcial)
    }
}
