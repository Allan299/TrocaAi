package com.example.trocaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.trocaai.model.TipoAnuncio
import com.example.trocaai.ui.theme.CorDoacao
import com.example.trocaai.ui.theme.CorTroca
import com.example.trocaai.ui.theme.CorVenda

@Composable
fun EtiquetaTipo(tipo: TipoAnuncio, modifier: Modifier = Modifier) {
    val cor = when (tipo) {
        TipoAnuncio.VENDA -> CorVenda
        TipoAnuncio.TROCA -> CorTroca
        TipoAnuncio.DOACAO -> CorDoacao
    }
    Text(
        text = tipo.rotulo,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(cor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
