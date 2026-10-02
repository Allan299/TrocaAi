package com.example.trocaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trocaai.util.SugestaoPreco
import com.example.trocaai.util.paraCampoDePreco

// Mostra a sugestão de preço, deixando claro que é apenas uma estimativa.
@Composable
fun CartaoSugestaoPreco(
    sugestao: SugestaoPreco,
    categoria: String,
    onUsarValor: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (sugestao) {
                is SugestaoPreco.Estimativa -> {
                    Text(
                        "Sugestão: R$ ${sugestao.mediana.paraCampoDePreco()}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    val base = if (sugestao.baseadoNoTitulo) "anúncios parecidos" else "anúncios da categoria $categoria"
                    Text(
                        "Mediana de ${sugestao.quantidade} $base à venda " +
                            "(de R$ ${sugestao.minimo.paraCampoDePreco()} a R$ ${sugestao.maximo.paraCampoDePreco()}).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(onClick = { onUsarValor(sugestao.mediana) }) {
                        Text("Usar este valor")
                    }
                }
                is SugestaoPreco.DadosInsuficientes -> {
                    Text("Sem dados suficientes", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Há apenas ${sugestao.quantidade} anúncio(s) à venda em $categoria com preço " +
                            "(mínimo de 3). Defina o preço manualmente.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text(
                "Valor apenas estimado com base nos anúncios do TrocaAí. Não é uma avaliação oficial.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
