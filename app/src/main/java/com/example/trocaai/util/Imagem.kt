package com.example.trocaai.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

class ErroImagem(mensagem: String) : Exception(mensagem)

object Imagem {
    // Reduz a foto da galeria (lado maior = ladoMax) e converte para JPEG comprimido.
    // Mantém o tamanho da imagem em torno de 40~80 KB para compatibilidade com Storage e Base64 no Firestore.
    suspend fun comprimir(context: Context, uri: Uri, ladoMax: Int = 1024): ByteArray =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver

            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
            if (limites.outWidth <= 0) throw ErroImagem("Não foi possível ler a imagem escolhida")

            var amostra = 1
            while (max(limites.outWidth, limites.outHeight) / (amostra * 2) >= ladoMax) amostra *= 2

            val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
            var bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opcoes) }
                ?: throw ErroImagem("Não foi possível ler a imagem escolhida")

            val rotacao = try {
                resolver.openInputStream(uri)?.use {
                    when (ExifInterface(it).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                    )) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (e: Exception) {
                0f
            }

            val escala = ladoMax.toFloat() / max(bitmap.width, bitmap.height)
            if (rotacao != 0f || escala < 1f) {
                val matriz = Matrix().apply {
                    if (escala < 1f) postScale(escala, escala)
                    if (rotacao != 0f) postRotate(rotacao)
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
            }

            ByteArrayOutputStream().use { saida ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, saida)
                saida.toByteArray()
            }
        }
}
