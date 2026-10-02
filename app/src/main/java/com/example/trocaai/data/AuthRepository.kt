package com.example.trocaai.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.trocaai.R
import com.example.trocaai.model.Usuario
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Cadastro, login e logout. A sessão é mantida pelo próprio Firebase Authentication.
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val usuarioAtual: FirebaseUser? get() = auth.currentUser

    suspend fun cadastrar(nome: String, email: String, senha: String): FirebaseUser {
        val user = auth.createUserWithEmailAndPassword(email, senha).await().user
            ?: error("Não foi possível criar o usuário")
        // Assim que a conta é criada o app navega para o Início e a tela de cadastro
        // sai da composição; NonCancellable garante que o perfil seja gravado mesmo assim.
        withContext(NonCancellable) {
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(nome).build()).await()
            garantirPerfil(user, nome)
        }
        return user
    }

    suspend fun entrar(email: String, senha: String): FirebaseUser {
        val user = auth.signInWithEmailAndPassword(email, senha).await().user
            ?: error("Não foi possível entrar")
        withContext(NonCancellable) { garantirPerfil(user, user.displayName.orEmpty()) }
        return user
    }

    suspend fun entrarComGoogle(context: Context): FirebaseUser {
        try {
            val credentialManager = CredentialManager.create(context)
            val webClientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (_: Exception) {
                WEB_CLIENT_ID_FALLBACK
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user ?: error("Não foi possível entrar com a conta Google")

                withContext(NonCancellable) {
                    garantirPerfil(user, user.displayName.orEmpty())
                }
                return user
            } else {
                error("Credencial do Google inválida")
            }
        } catch (e: Exception) {
            Log.e("TrocaAiAuth", "Erro no entrarComGoogle", e)
            throw e
        }
    }

    fun sair() = auth.signOut()

    suspend fun buscarPerfil(uid: String): Usuario? {
        val doc = db.collection(COLECAO_USUARIOS).document(uid).get().await()
        val perfilExistente = doc.toObject(Usuario::class.java)
        if (perfilExistente != null) return perfilExistente

        val user = auth.currentUser
        if (user != null && user.uid == uid) {
            val nome = user.displayName.orEmpty().ifBlank { "Estudante" }
            val novoPerfil = Usuario(nome = nome)
            try {
                db.collection(COLECAO_USUARIOS).document(uid).set(novoPerfil).await()
            } catch (_: Exception) {}
            return novoPerfil
        }
        return null
    }

    // Atualiza o nome no Authentication e no Firestore (users/{uid}) e também o nome
    // do vendedor nos anúncios do usuário, para os anúncios antigos não ficarem com o nome velho.
    suspend fun atualizarNome(nome: String) {
        val user = auth.currentUser ?: error("Usuário não autenticado")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(nome).build()).await()
        db.collection(COLECAO_USUARIOS).document(user.uid)
            .set(mapOf("nome" to nome), SetOptions.merge())
            .await()
        try {
            val anuncios = db.collection(AnunciosRepository.COLECAO_ANUNCIOS)
                .whereEqualTo("vendedorId", user.uid).get().await()
            val lote = db.batch()
            anuncios.documents.forEach { lote.update(it.reference, "vendedorNome", nome) }
            lote.commit().await()
        } catch (e: Exception) {
            // O nome do perfil já foi salvo; só os anúncios ficaram com o nome antigo.
        }
    }

    // Cria o documento users/{uid} se ainda não existir (ex.: falha de rede no cadastro).
    private suspend fun garantirPerfil(user: FirebaseUser, nome: String) {
        val ref = db.collection(COLECAO_USUARIOS).document(user.uid)
        if (!ref.get().await().exists()) {
            val nomeFinal = nome.ifBlank { user.displayName.orEmpty().ifBlank { "Usuário" } }
            ref.set(Usuario(nome = nomeFinal)).await()
        }
    }

    companion object {
        const val COLECAO_USUARIOS = "users"
        private const val WEB_CLIENT_ID_FALLBACK = "1059724544882-o70rba20317bfl6flrqbcgdnrlhla884.apps.googleusercontent.com"
    }
}
