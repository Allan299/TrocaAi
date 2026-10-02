package com.example.trocaai.navigation

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import com.example.trocaai.assistente.ConversaAssistente
import com.example.trocaai.assistente.InterpretadorPorRegras
import com.example.trocaai.data.AnunciosRepository
import com.example.trocaai.data.AuthRepository
import com.example.trocaai.ui.screens.AssistenteScreen
import com.example.trocaai.data.FavoritosRepository
import com.example.trocaai.ui.components.BarraNavegacao
import com.example.trocaai.ui.components.FavoritosState
import com.example.trocaai.ui.components.LocalFavoritos
import com.example.trocaai.ui.screens.FavoritosScreen
import com.example.trocaai.util.mensagemEmPortugues
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import com.example.trocaai.ui.screens.AnuncioDetalheScreen
import com.example.trocaai.ui.screens.AnuncianteScreen
import com.example.trocaai.ui.screens.AnuncioFormScreen
import com.example.trocaai.ui.screens.BuscarScreen
import com.example.trocaai.ui.screens.CadastroScreen
import com.example.trocaai.ui.screens.InicioScreen
import com.example.trocaai.ui.screens.LoginScreen
import com.example.trocaai.ui.screens.PerfilScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

const val ROTA_LOGIN = "login"
const val ROTA_CADASTRO = "cadastro"
const val ROTA_DETALHE = "detalhe/{id}"
const val ROTA_EDITAR = "editar/{id}"
const val ROTA_ANUNCIANTE = "anunciante/{uid}"
const val ROTA_ASSISTENTE = "assistente"

private fun rotaDetalhe(id: String) = "detalhe/$id"
private fun rotaEditar(id: String) = "editar/$id"
private fun rotaAnunciante(uid: String) = "anunciante/$uid"

private val rotasDaBarra = Destino.entries.map { it.rota }
private val rotasDeAutenticacao = listOf(ROTA_LOGIN, ROTA_CADASTRO)

// Observa a sessão do Firebase Authentication (login, logout e sessão salva ao reabrir o app).
@Composable
private fun rememberUsuarioLogado(auth: FirebaseAuth): State<FirebaseUser?> {
    val usuario = remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(auth) {
        val ouvinte = FirebaseAuth.AuthStateListener { usuario.value = it.currentUser }
        auth.addAuthStateListener(ouvinte)
        onDispose { auth.removeAuthStateListener(ouvinte) }
    }
    return usuario
}

@Composable
fun TrocaAiApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    val repositorio = remember { AuthRepository() }
    val usuario by rememberUsuarioLogado(auth)
    val navController = rememberNavController()
    val rotaAtual = navController.currentBackStackEntryAsState().value?.destination?.route
    val destinoInicial = remember { if (auth.currentUser != null) Destino.INICIO.rota else ROTA_LOGIN }

    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val mostrarMensagem: (String) -> Unit = { texto -> escopo.launch { snackbar.showSnackbar(texto) } }

    // Favoritos do usuário logado (users/{uid}/favoritos), observados em tempo real.
    // Ao trocar de usuário a lista é zerada e passa a ler a subcoleção do novo uid.
    val favoritosRepositorio = remember { FavoritosRepository() }
    var idsFavoritos by remember { mutableStateOf(emptySet<String>()) }
    val uidAtual = usuario?.uid
    LaunchedEffect(uidAtual) {
        idsFavoritos = emptySet()
        if (uidAtual == null) return@LaunchedEffect
        favoritosRepositorio.observarIds(uidAtual)
            // Ao sair da conta o Firestore pode avisar "sem permissão" antes da tela trocar: não mostrar.
            .catch {
                Log.e("TrocaAiApp", "Erro ao observar favoritos", it)
                if (auth.currentUser != null) mostrarMensagem(it.mensagemEmPortugues())
            }
            .collect { idsFavoritos = it.ids.toSet() }
    }
    // A conversa do assistente é criada por usuário (nova conta = conversa nova) e sobrevive
    // à troca de abas; é cancelada quando o usuário sai.
    val anunciosRepositorio = remember { AnunciosRepository() }
    val conversa = remember(uidAtual) {
        ConversaAssistente(InterpretadorPorRegras(), anunciosRepositorio, favoritosRepositorio)
    }
    DisposableEffect(conversa) { onDispose { conversa.encerrar() } }

    // remember: o valor só muda quando a lista de favoritos muda (evita recompor todas as telas)
    val favoritos = remember(idsFavoritos) {
        FavoritosState(idsFavoritos) { anuncioId ->
            val favoritar = anuncioId !in idsFavoritos
            escopo.launch {
                try {
                    favoritosRepositorio.alternar(anuncioId, favoritar)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    mostrarMensagem(e.mensagemEmPortugues())
                }
            }
        }
    }

    // Proteção das telas: sem sessão, qualquer tela interna volta para o login;
    // com sessão, sai das telas de login/cadastro.
    LaunchedEffect(usuario, rotaAtual) {
        val destino = when {
            usuario == null && rotaAtual != null && rotaAtual !in rotasDeAutenticacao -> ROTA_LOGIN
            usuario != null && rotaAtual in rotasDeAutenticacao -> Destino.INICIO.rota
            else -> null
        }
        if (destino != null) {
            navController.navigate(destino) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    fun irParaInicio() {
        navController.navigate(Destino.INICIO.rota) {
            popUpTo(Destino.INICIO.rota)
            launchSingleTop = true
        }
    }

    CompositionLocalProvider(LocalFavoritos provides favoritos) {
        Scaffold(
            bottomBar = { if (rotaAtual in rotasDaBarra) BarraNavegacao(navController) },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = destinoInicial,
                modifier = Modifier.padding(padding).consumeWindowInsets(padding)
            ) {
                composable(ROTA_LOGIN) {
                    LoginScreen(onIrParaCadastro = { navController.navigate(ROTA_CADASTRO) })
                }
                composable(ROTA_CADASTRO) {
                    CadastroScreen(onVoltarParaLogin = { navController.popBackStack() })
                }
                composable(Destino.INICIO.rota) {
                    InicioScreen(
                        onAbrirAnuncio = { navController.navigate(rotaDetalhe(it)) },
                        onAbrirAssistente = { navController.navigate(ROTA_ASSISTENTE) }
                    )
                }
                composable(Destino.BUSCAR.rota) {
                    BuscarScreen(onAbrirAnuncio = { navController.navigate(rotaDetalhe(it)) })
                }
                composable(Destino.ANUNCIAR.rota) {
                    AnuncioFormScreen(
                        anuncioId = null,
                        mostrarMensagem = mostrarMensagem,
                        onConcluir = ::irParaInicio
                    )
                }
                composable(Destino.FAVORITOS.rota) {
                    FavoritosScreen(
                        uid = usuario?.uid,
                        onAbrirAnuncio = { navController.navigate(rotaDetalhe(it)) }
                    )
                }
                composable(Destino.PERFIL.rota) {
                    PerfilScreen(
                        usuario = usuario,
                        onSair = { repositorio.sair() },
                        onAbrirAnuncio = { navController.navigate(rotaDetalhe(it)) },
                        mostrarMensagem = mostrarMensagem
                    )
                }
                composable(ROTA_DETALHE) { entrada ->
                    val id = entrada.arguments?.getString("id").orEmpty()
                    AnuncioDetalheScreen(
                        anuncioId = id,
                        onVoltar = { navController.popBackStack() },
                        onEditar = { navController.navigate(rotaEditar(it)) },
                        onExcluido = { navController.popBackStack() },
                        onAbrirAnunciante = { navController.navigate(rotaAnunciante(it)) },
                        mostrarMensagem = mostrarMensagem
                    )
                }
                composable(ROTA_ASSISTENTE) {
                    AssistenteScreen(
                        conversa = conversa,
                        onVoltar = { navController.popBackStack() },
                        onAbrirAnuncio = { navController.navigate(rotaDetalhe(it)) }
                    )
                }
                composable(ROTA_ANUNCIANTE) { entrada ->
                    val uid = entrada.arguments?.getString("uid").orEmpty()
                    AnuncianteScreen(
                        uid = uid,
                        onVoltar = { navController.popBackStack() },
                        onAbrirAnuncio = { navController.navigate(rotaDetalhe(it)) }
                    )
                }
                composable(ROTA_EDITAR) { entrada ->
                    val id = entrada.arguments?.getString("id").orEmpty()
                    AnuncioFormScreen(
                        anuncioId = id,
                        mostrarMensagem = mostrarMensagem,
                        onConcluir = { navController.popBackStack() },
                        onVoltar = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
