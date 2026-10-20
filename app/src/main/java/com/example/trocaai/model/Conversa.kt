package com.example.trocaai.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Documento em conversas/{conversaId}.
data class Conversa(
    @DocumentId val id: String = "",
    val anuncioId: String = "",
    val anuncioTitulo: String = "",
    val anuncioFotoUrl: String = "",
    val vendedorId: String = "",
    val vendedorNome: String = "",
    val compradorId: String = "",
    val compradorNome: String = "",
    val ultimaMensagem: String = "",
    val participantes: List<String> = emptyList(),
    @ServerTimestamp val atualizadoEm: Date? = null
)
