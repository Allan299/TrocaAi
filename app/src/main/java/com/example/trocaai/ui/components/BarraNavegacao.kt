package com.example.trocaai.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.trocaai.navigation.Destino

@Composable
fun BarraNavegacao(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rotaAtual = backStackEntry?.destination?.route

    NavigationBar {
        Destino.entries.forEach { destino ->
            NavigationBarItem(
                selected = rotaAtual == destino.rota,
                onClick = {
                    navController.navigate(destino.rota) {
                        popUpTo(Destino.INICIO.rota) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destino.icone, contentDescription = destino.rotulo) },
                label = { Text(destino.rotulo) }
            )
        }
    }
}
