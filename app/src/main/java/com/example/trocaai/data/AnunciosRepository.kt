package com.example.trocaai.data

import android.util.Log
import com.example.trocaai.model.Anuncio
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

// Resultado de uma consulta. deCache = true quando os dados vêm do aparelho (sem servidor).
data class ResultadoLista(val anuncios: List<Anuncio>, val deCache: Boolean)

// Anúncios no Firestore e fotos no Storage.
class AnunciosRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val colecao get() = db.collection(COLECAO_ANUNCIOS)

    val uidAtual: String? get() = auth.currentUser?.uid

    // Os Flows abaixo usam listeners em tempo real: a lista atualiza sozinha depois de
    // criar, editar, excluir ou marcar um anúncio como indisponível. Anúncios indisponíveis
    // são filtrados aqui, no cliente (assim documentos sem o campo "disponivel" também funcionam).
    fun observarRecentes(limite: Long = 200): Flow<ResultadoLista> = callbackFlow {
        val registro = colecao.orderBy("criadoEm", Query.Direction.DESCENDING)
            .limit(limite)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, erro ->
                if (erro != null) close(erro)
                else if (snapshot != null) {
                    val lista = snapshot.toObjects(Anuncio::class.java).filter { it.disponivel }
                    trySend(ResultadoLista(lista, snapshot.metadata.isFromCache))
                }
            }
        awaitClose { registro.remove() }
    }

    // Sem orderBy no servidor (evita criar índice composto); a ordenação é feita aqui.
    fun observarDoUsuario(uid: String, apenasDisponiveis: Boolean): Flow<ResultadoLista> = callbackFlow {
        val registro = colecao.whereEqualTo("vendedorId", uid)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, erro ->
                if (erro != null) close(erro)
                else if (snapshot != null) {
                    val lista = snapshot.toObjects(Anuncio::class.java)
                        .filter { !apenasDisponiveis || it.disponivel }
                        .sortedByDescending { it.criadoEm?.time ?: Long.MAX_VALUE }
                    trySend(ResultadoLista(lista, snapshot.metadata.isFromCache))
                }
            }
        awaitClose { registro.remove() }
    }

    fun observar(id: String): Flow<Anuncio?> = callbackFlow {
        val registro = colecao.document(id).addSnapshotListener { snapshot, erro ->
            if (erro != null) close(erro)
            else if (snapshot != null) trySend(snapshot.toObject(Anuncio::class.java))
        }
        awaitClose { registro.remove() }
    }

    // Leitura única dos anúncios disponíveis mais recentes (usada pelo assistente).
    suspend fun listarRecentes(limite: Long = 200): List<Anuncio> =
        colecao.orderBy("criadoEm", Query.Direction.DESCENDING)
            .limit(limite)
            .get().await()
            .toObjects(Anuncio::class.java)
            .filter { it.disponivel }

    suspend fun buscarPorId(id: String): Anuncio? =
        colecao.document(id).get().await().toObject(Anuncio::class.java)

    // Cria (id vazio) ou edita um anúncio. foto = JPEG já comprimido, ou null para manter a atual.
    suspend fun salvar(anuncio: Anuncio, foto: ByteArray?): String {
        val user = auth.currentUser ?: error("Usuário não autenticado")

        var fotoUrl = anuncio.fotoUrl
        var fotoPath = anuncio.fotoPath
        if (foto != null) {
            val novoPath = "$COLECAO_ANUNCIOS/${user.uid}/${UUID.randomUUID()}.jpg"
            try {
                val ref = storage.reference.child(novoPath)
                val metadados = StorageMetadata.Builder().setContentType("image/jpeg").build()
                ref.putBytes(foto, metadados).await()
                fotoUrl = ref.downloadUrl.await().toString()
                fotoPath = novoPath
            } catch (e: Exception) {
                // Se o Firebase Storage falhar (ex: plano Blaze exigido ou regras),
                // armazena a foto comprimida em Base64 diretamente no documento do Firestore.
                Log.w("TrocaAiStorage", "Storage indisponível, usando fallback Base64 no Firestore", e)
                val base64 = android.util.Base64.encodeToString(foto, android.util.Base64.NO_WRAP)
                fotoUrl = "data:image/jpeg;base64,$base64"
                fotoPath = ""
            }
        }

        val dadosEditaveis = mapOf(
            "titulo" to anuncio.titulo,
            "descricao" to anuncio.descricao,
            "categoria" to anuncio.categoria,
            "tipo" to anuncio.tipo.name,
            "preco" to anuncio.preco,
            "conservacao" to anuncio.conservacao.name,
            "fotoUrl" to fotoUrl,
            "fotoPath" to fotoPath
        )

        try {
            val id = if (anuncio.id.isBlank()) {
                val novo = dadosEditaveis + mapOf(
                    "disponivel" to true,
                    "vendedorId" to user.uid,
                    "vendedorNome" to user.displayName.orEmpty(),
                    "criadoEm" to FieldValue.serverTimestamp()
                )
                colecao.add(novo).await().id
            } else {
                // update() mantém vendedorId e criadoEm intactos
                colecao.document(anuncio.id).update(dadosEditaveis).await()
                anuncio.id
            }
            // Troca de foto: apaga a antiga do Storage
            if (foto != null && anuncio.fotoPath.isNotBlank()) apagarFoto(anuncio.fotoPath)
            return id
        } catch (e: Exception) {
            Log.e("TrocaAiStorage", "Erro ao salvar anuncio ou imagem", e)
            // Não deixa a foto nova órfã no Storage se o Firestore falhar
            if (foto != null) apagarFoto(fotoPath)
            throw e
        }
    }

    suspend fun alterarDisponibilidade(id: String, disponivel: Boolean) {
        colecao.document(id).update("disponivel", disponivel).await()
    }

    suspend fun excluir(anuncio: Anuncio) {
        colecao.document(anuncio.id).delete().await()
        if (anuncio.fotoPath.isNotBlank()) apagarFoto(anuncio.fotoPath)
    }

    // Falhas ao apagar a foto são ignoradas: o anúncio já foi tratado.
    private suspend fun apagarFoto(path: String) = withContext(NonCancellable) {
        try {
            storage.reference.child(path).delete().await()
        } catch (e: Exception) {
            // ignora
        }
    }

    companion object {
        const val COLECAO_ANUNCIOS = "anuncios"
    }
}
