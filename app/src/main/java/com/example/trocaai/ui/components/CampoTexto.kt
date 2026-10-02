package com.example.trocaai.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

// Campo de texto com rótulo e mensagem de erro. Com senha = true, oculta o texto.
@Composable
fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    erro: String? = null,
    senha: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text
) {
    var mostrarSenha by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (senha) KeyboardType.Password else tipoTeclado
        ),
        visualTransformation = if (senha && !mostrarSenha) PasswordVisualTransformation()
        else VisualTransformation.None,
        trailingIcon = if (senha) {
            {
                TextButton(onClick = { mostrarSenha = !mostrarSenha }) {
                    Text(if (mostrarSenha) "Ocultar" else "Mostrar")
                }
            }
        } else null,
        modifier = modifier
    )
}

@Composable
fun MensagemErro(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}
