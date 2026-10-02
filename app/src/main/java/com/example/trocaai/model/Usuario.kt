package com.example.trocaai.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Documento em users/{uid}. O id é o mesmo uid do Firebase Authentication.
data class Usuario(
    @DocumentId val uid: String = "",
    val nome: String = "",
    val email: String = "",
    val curso: String = "",
    @ServerTimestamp val criadoEm: Date? = null
)
