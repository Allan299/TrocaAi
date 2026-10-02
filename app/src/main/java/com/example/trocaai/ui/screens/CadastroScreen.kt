package com.example.trocaai.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.trocaai.data.AuthRepository
import com.example.trocaai.ui.components.BotaoGoogle
import com.example.trocaai.ui.components.BotaoPrincipal
import com.example.trocaai.ui.components.CampoTexto
import com.example.trocaai.ui.components.MensagemErro
import com.example.trocaai.util.Validacao
import com.example.trocaai.util.mensagemEmPortugues
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CadastroScreen(onVoltarParaLogin: () -> Unit) {
    val repositorio = remember { AuthRepository() }
    val escopo = rememberCoroutineScope()
    val context = LocalContext.current

    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var erroNome by remember { mutableStateOf<String?>(null) }
    var erroEmail by remember { mutableStateOf<String?>(null) }
    var erroSenha by remember { mutableStateOf<String?>(null) }
    var erroGeral by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }
    var carregandoGoogle by remember { mutableStateOf(false) }

    fun cadastrar() {
        erroNome = Validacao.nome(nome)
        erroEmail = Validacao.email(email)
        erroSenha = Validacao.senha(senha)
        erroGeral = null
        if (erroNome != null || erroEmail != null || erroSenha != null) return

        carregando = true
        escopo.launch {
            try {
                repositorio.cadastrar(nome.trim(), email.trim(), senha)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                erroGeral = e.mensagemEmPortugues()
            } finally {
                carregando = false
            }
        }
    }

    fun entrarComGoogle() {
        erroGeral = null
        carregandoGoogle = true
        escopo.launch {
            try {
                repositorio.entrarComGoogle(context)
            } catch (_: GetCredentialCancellationException) {
                // Usuário cancelou a seleção da conta
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("TrocaAiCadastro", "Erro no cadastro com Google", e)
                val msg = e.mensagemEmPortugues()
                if (msg.isNotBlank()) erroGeral = msg
            } finally {
                carregandoGoogle = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Text("Criar conta", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

        CampoTexto(
            valor = nome,
            aoMudar = { nome = it },
            rotulo = "Nome",
            erro = erroNome,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        CampoTexto(
            valor = email,
            aoMudar = { email = it },
            rotulo = "E-mail",
            erro = erroEmail,
            tipoTeclado = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = senha,
            aoMudar = { senha = it },
            rotulo = "Senha (mínimo 6 caracteres)",
            erro = erroSenha,
            senha = true,
            modifier = Modifier.fillMaxWidth()
        )

        erroGeral?.let { MensagemErro(it) }

        BotaoPrincipal(
            texto = "Cadastrar",
            onClick = ::cadastrar,
            carregando = carregando,
            enabled = !carregandoGoogle,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "  ou  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        BotaoGoogle(
            texto = "Cadastrar com o Google",
            onClick = ::entrarComGoogle,
            carregando = carregandoGoogle,
            enabled = !carregando,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(onClick = onVoltarParaLogin) {
            Text("Já tem conta? Entrar")
        }
    }
}
