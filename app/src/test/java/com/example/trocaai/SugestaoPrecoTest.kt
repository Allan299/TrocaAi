package com.example.trocaai

import com.example.trocaai.model.Anuncio
import com.example.trocaai.model.TipoAnuncio
import com.example.trocaai.util.CalculadoraDePreco
import com.example.trocaai.util.SugestaoPreco
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SugestaoPrecoTest {

    private fun venda(id: String, titulo: String, categoria: String, preco: Double?) =
        Anuncio(id = id, titulo = titulo, categoria = categoria, tipo = TipoAnuncio.VENDA, preco = preco)

    @Test
    fun medianaComQuantidadeImparEPar() {
        assertEquals(30.0, CalculadoraDePreco.mediana(listOf(10.0, 100.0, 30.0)), 0.0)
        assertEquals(25.0, CalculadoraDePreco.mediana(listOf(10.0, 20.0, 30.0, 40.0)), 0.0)
        assertEquals(12.35, CalculadoraDePreco.mediana(listOf(12.34, 12.35, 12.36, 12.35)), 0.0)
    }

    @Test
    fun usaAnunciosParecidosQuandoHaPeloMenosTres() {
        val anuncios = listOf(
            venda("1", "Cálculo Vol. 1", "Livros", 40.0),
            venda("2", "Livro de Cálculo", "Livros", 50.0),
            venda("3", "Cálculo Diferencial", "Livros", 60.0),
            venda("4", "Romance de ficção", "Livros", 500.0)
        )
        val r = CalculadoraDePreco.sugerir(anuncios, "Livros", "Cálculo 2") as SugestaoPreco.Estimativa
        assertEquals(50.0, r.mediana, 0.0)
        assertEquals(40.0, r.minimo, 0.0)
        assertEquals(60.0, r.maximo, 0.0)
        assertEquals(3, r.quantidade)
        assertTrue(r.baseadoNoTitulo)
    }

    @Test
    fun usaACategoriaInteiraQuandoNaoHaParecidosSuficientes() {
        val anuncios = listOf(
            venda("1", "Cálculo", "Livros", 40.0),
            venda("2", "Física", "Livros", 60.0),
            venda("3", "Química", "Livros", 80.0)
        )
        val r = CalculadoraDePreco.sugerir(anuncios, "Livros", "Algoritmos") as SugestaoPreco.Estimativa
        assertEquals(60.0, r.mediana, 0.0)
        assertFalse(r.baseadoNoTitulo)
    }

    @Test
    fun ignoraOutrasCategoriasDoacoesTrocasEAnunciosSemPreco() {
        val anuncios = listOf(
            venda("1", "A", "Livros", 40.0),
            venda("2", "B", "Livros", null),
            venda("3", "C", "Livros", 0.0),
            venda("4", "D", "Papelaria", 10.0),
            Anuncio(id = "5", titulo = "E", categoria = "Livros", tipo = TipoAnuncio.DOACAO),
            Anuncio(id = "6", titulo = "F", categoria = "Livros", tipo = TipoAnuncio.TROCA, preco = 99.0)
        )
        val r = CalculadoraDePreco.sugerir(anuncios, "Livros", "qualquer")
        assertEquals(SugestaoPreco.DadosInsuficientes(1), r)
    }

    @Test
    fun semDadosSuficientesNaoInventaValor() {
        val anuncios = listOf(venda("1", "A", "Livros", 40.0), venda("2", "B", "Livros", 50.0))
        assertEquals(SugestaoPreco.DadosInsuficientes(2), CalculadoraDePreco.sugerir(anuncios, "Livros", "A"))
        assertEquals(SugestaoPreco.DadosInsuficientes(0), CalculadoraDePreco.sugerir(emptyList(), "Livros", "A"))
    }

    @Test
    fun naoUsaOProprioAnuncioNaEdicao() {
        val anuncios = listOf(
            venda("1", "A", "Livros", 40.0),
            venda("2", "B", "Livros", 50.0),
            venda("3", "C", "Livros", 60.0)
        )
        assertEquals(SugestaoPreco.DadosInsuficientes(2), CalculadoraDePreco.sugerir(anuncios, "Livros", "A", ignorarId = "3"))
    }
}
