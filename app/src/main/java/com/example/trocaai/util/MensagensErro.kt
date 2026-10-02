package com.example.trocaai.util

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException

// Converte as exceções do Firebase em mensagens amigáveis em português.
fun Throwable.mensagemEmPortugues(): String = when (this) {
    is GetCredentialCancellationException -> ""
    is GetCredentialException -> when {
        message?.contains("10") == true || message?.contains("DEVELOPER_ERROR", ignoreCase = true) == true ->
            "Erro de configuração: cadastre a chave SHA-1 no Firebase Console"
        message?.contains("No credentials", ignoreCase = true) == true || message?.contains("16") == true ->
            "Adicione uma conta do Google nas Configurações do seu aparelho/emulador"
        else -> "Não foi possível conectar com a conta Google. Verifique se o provedor Google está ativo no Firebase"
    }
    is ErroImagem -> message ?: "Erro ao processar a imagem"
    is FirebaseAuthWeakPasswordException -> "Senha muito fraca. Use pelo menos 6 caracteres"
    is FirebaseAuthUserCollisionException -> "Este e-mail já está cadastrado"
    is FirebaseAuthInvalidUserException -> "E-mail ou senha incorretos"
    is FirebaseAuthInvalidCredentialsException -> when (errorCode) {
        "ERROR_INVALID_EMAIL" -> "E-mail inválido"
        else -> "E-mail ou senha incorretos"
    }
    is FirebaseTooManyRequestsException -> "Muitas tentativas. Aguarde um pouco e tente de novo"
    is FirebaseNetworkException -> "Sem conexão com a internet"
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Você não tem permissão para esta ação"
        FirebaseFirestoreException.Code.UNAVAILABLE -> "Sem conexão com a internet"
        else -> "Erro ao acessar os dados. Tente novamente"
    }
    is StorageException -> when (errorCode) {
        StorageException.ERROR_NOT_AUTHORIZED -> "Sem permissão para enviar a imagem"
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> "Sem conexão com a internet"
        else -> "Erro ao enviar a imagem. Verifique se o Storage está ativado"
    }
    else -> "Ocorreu um erro inesperado. Tente novamente"
}
