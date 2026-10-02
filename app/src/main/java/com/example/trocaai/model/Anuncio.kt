package com.example.trocaai.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Documento em anuncios/{id}. Todos os campos têm valor padrão: o Firestore precisa
// de um construtor vazio para converter o documento em objeto.
data class Anuncio(
    @DocumentId val id: String = "",
    val titulo: String = "",
    val descricao: String = "",
    val categoria: String = "",
    val tipo: TipoAnuncio = TipoAnuncio.VENDA,
    val preco: Double? = null,
    val conservacao: EstadoConservacao = EstadoConservacao.BOM,
    val fotoUrl: String = "",   // endereço para exibir a foto
    val fotoPath: String = "",  // caminho no Storage (usado para apagar a foto)
    val disponivel: Boolean = true, // false = vendido/indisponível (some das listas públicas)
    val vendedorId: String = "",
    val vendedorNome: String = "",
    @ServerTimestamp val criadoEm: Date? = null
)
