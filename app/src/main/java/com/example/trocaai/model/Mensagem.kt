package com.example.trocaai.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Documento em conversas/{conversaId}/mensagens/{mensagemId}.
data class Mensagem(
    @DocumentId val id: String = "",
    val remetenteId: String = "",
    val remetenteNome: String = "",
    val texto: String = "",
    @ServerTimestamp val criadoEm: Date? = null
)
