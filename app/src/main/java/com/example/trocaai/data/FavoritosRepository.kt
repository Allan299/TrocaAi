package com.example.trocaai.data

import com.example.trocaai.model.Anuncio
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.tasks.await

// Ids dos anúncios favoritados (mais recentes primeiro).
data class ResultadoFavoritos(val ids: List<String>, val deCache: Boolean)

// Favoritos ficam em users/{uid}/favoritos/{idDoAnuncio}. Cada usuário só acessa a própria
// subcoleção (garantido pelas regras do Firestore e porque todas as consultas usam o uid logado).
class FavoritosRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun favoritos(uid: String) =
        db.collection(AuthRepository.COLECAO_USUARIOS).document(uid).collection(COLECAO_FAVORITOS)

    fun observarIds(uid: String): Flow<ResultadoFavoritos> = callbackFlow {
        val registro = favoritos(uid).orderBy("criadoEm", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, erro ->
                if (erro != null) close(erro)
                else if (snapshot != null) {
                    trySend(ResultadoFavoritos(snapshot.documents.map { it.id }, snapshot.metadata.isFromCache))
                }
            }
        awaitClose { registro.remove() }
    }

    // Busca os anúncios de cada favorito. Anúncios excluídos ou indisponíveis ficam de fora.
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarAnuncios(uid: String): Flow<ResultadoLista> =
        observarIds(uid).mapLatest { resultado ->
            val anuncios = coroutineScope {
                resultado.ids.map { id ->
                    async {
                        runCatching {
                            db.collection(AnunciosRepository.COLECAO_ANUNCIOS).document(id).get().await()
                                .toObject(Anuncio::class.java)
                        }.getOrNull()
                    }
                }.awaitAll()
            }
            ResultadoLista(anuncios.filterNotNull().filter { it.disponivel }, resultado.deCache)
        }

    suspend fun alternar(anuncioId: String, favoritar: Boolean) {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado")
        val ref = favoritos(uid).document(anuncioId)
        if (favoritar) {
            ref.set(mapOf("criadoEm" to FieldValue.serverTimestamp())).await()
        } else {
            ref.delete().await()
        }
    }

    companion object {
        const val COLECAO_FAVORITOS = "favoritos"
    }
}
