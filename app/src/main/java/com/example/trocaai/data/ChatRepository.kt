package com.example.trocaai.data

import com.example.trocaai.model.Anuncio
import com.example.trocaai.model.Conversa
import com.example.trocaai.model.Mensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val colecaoConversas get() = db.collection(COLECAO_CONVERSAS)

    val uidAtual: String? get() = auth.currentUser?.uid

    // Retorna o id de uma conversa existente ou cria uma nova se ainda não existir.
    suspend fun obterOuCriarConversa(anuncio: Anuncio): String {
        val user = auth.currentUser ?: error("Usuário não autenticado")
        check(user.uid != anuncio.vendedorId) { "Você não pode conversar com você mesmo" }

        val idCustomizado = "${anuncio.id}_${user.uid}"
        val docRef = colecaoConversas.document(idCustomizado)
        val snapshot = docRef.get().await()

        if (snapshot.exists()) {
            return idCustomizado
        }

        val nomeComprador = user.displayName.orEmpty().ifBlank { "Comprador" }
        val novaConversa = mapOf(
            "anuncioId" to anuncio.id,
            "anuncioTitulo" to anuncio.titulo,
            "anuncioFotoUrl" to anuncio.fotoUrl,
            "vendedorId" to anuncio.vendedorId,
            "vendedorNome" to anuncio.vendedorNome.ifBlank { "Vendedor" },
            "compradorId" to user.uid,
            "compradorNome" to nomeComprador,
            "ultimaMensagem" to "Conversa iniciada",
            "participantes" to listOf(anuncio.vendedorId, user.uid),
            "atualizadoEm" to FieldValue.serverTimestamp()
        )

        docRef.set(novaConversa).await()
        return idCustomizado
    }

    // Observa todas as conversas do usuário logado (como vendedor ou comprador)
    fun observarConversasDoUsuario(uid: String): Flow<List<Conversa>> = callbackFlow {
        val registro = colecaoConversas
            .whereArrayContains("participantes", uid)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                } else if (snapshot != null) {
                    val lista = snapshot.toObjects(Conversa::class.java)
                        .sortedByDescending { it.atualizadoEm?.time ?: Long.MAX_VALUE }
                    trySend(lista)
                }
            }
        awaitClose { registro.remove() }
    }

    // Observa os detalhes de uma conversa por id
    fun observarConversa(conversaId: String): Flow<Conversa?> = callbackFlow {
        val registro = colecaoConversas.document(conversaId)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                } else if (snapshot != null) {
                    trySend(snapshot.toObject(Conversa::class.java))
                }
            }
        awaitClose { registro.remove() }
    }

    // Observa as mensagens de uma conversa específica em tempo real
    fun observarMensagens(conversaId: String): Flow<List<Mensagem>> = callbackFlow {
        val registro = colecaoConversas.document(conversaId)
            .collection(COLECAO_MENSAGENS)
            .orderBy("criadoEm", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                } else if (snapshot != null) {
                    trySend(snapshot.toObjects(Mensagem::class.java))
                }
            }
        awaitClose { registro.remove() }
    }

    // Envia uma mensagem e atualiza o resumo e horário na conversa pai
    suspend fun enviarMensagem(conversaId: String, texto: String) {
        val textoLimpo = texto.trim()
        if (textoLimpo.isBlank()) return
        val user = auth.currentUser ?: error("Usuário não autenticado")

        val novaMensagem = mapOf(
            "remetenteId" to user.uid,
            "remetenteNome" to user.displayName.orEmpty().ifBlank { "Usuário" },
            "texto" to textoLimpo,
            "criadoEm" to FieldValue.serverTimestamp()
        )

        val conversaRef = colecaoConversas.document(conversaId)
        val mensagemRef = conversaRef.collection(COLECAO_MENSAGENS).document()

        val lote = db.batch()
        lote.set(mensagemRef, novaMensagem)
        lote.update(
            conversaRef, mapOf(
                "ultimaMensagem" to textoLimpo,
                "atualizadoEm" to FieldValue.serverTimestamp()
            )
        )
        lote.commit().await()
    }

    // Apaga todas as conversas e mensagens vinculadas a um anúncio
    suspend fun excluirConversasDoAnuncio(anuncioId: String) {
        if (anuncioId.isBlank()) return
        try {
            val conversasSnap = colecaoConversas
                .whereEqualTo("anuncioId", anuncioId)
                .get()
                .await()

            for (doc in conversasSnap.documents) {
                excluirConversa(doc.id)
            }
        } catch (_: Exception) {}
    }

    // Apaga uma conversa específica e todas as suas mensagens da subcoleção
    suspend fun excluirConversa(conversaId: String) {
        if (conversaId.isBlank()) return
        try {
            val conversaRef = colecaoConversas.document(conversaId)
            val mensagensSnap = conversaRef.collection(COLECAO_MENSAGENS).get().await()

            val lote = db.batch()
            mensagensSnap.documents.forEach { lote.delete(it.reference) }
            lote.delete(conversaRef)
            lote.commit().await()
        } catch (_: Exception) {}
    }

    companion object {
        const val COLECAO_CONVERSAS = "conversas"
        const val COLECAO_MENSAGENS = "mensagens"
    }
}
