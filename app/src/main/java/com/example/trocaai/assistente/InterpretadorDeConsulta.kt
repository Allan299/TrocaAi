package com.example.trocaai.assistente

import com.example.trocaai.model.TipoAnuncio
import com.example.trocaai.util.normalizar

// Ponto de troca do "cérebro" do assistente: hoje são regras de texto
// (InterpretadorPorRegras); no futuro pode ser um modelo de IA local que também devolva uma Intencao.
interface InterpretadorDeConsulta {
    suspend fun interpretar(mensagem: String): Intencao
}

// Interpretador baseado em regras: normaliza o texto (minúsculas, sem acentos), procura
// preço, tipo e categoria e trata o que sobra como palavras-chave de busca.
class InterpretadorPorRegras : InterpretadorDeConsulta {

    override suspend fun interpretar(mensagem: String): Intencao {
        val original = mensagem.trim().lowercase()
        val texto = original.normalizar()
        val palavras = PALAVRA.findAll(texto).map { it.value }.toList()
        if (palavras.isEmpty()) return Intencao.NaoEntendi

        if (palavras.any { it in ACOES_PROIBIDAS }) return Intencao.AcaoNaoSuportada
        if (palavras.all { it in SAUDACOES }) return Intencao.Saudacao
        if (palavras.any { it in AJUDA } || AJUDA_FRASES.any { it in texto }) return Intencao.Ajuda

        // 1) Preço: extrai e remove do texto os trechos de valor
        var restante = texto.replace("r\$", " ")
        var precoMin: Double? = null
        var precoMax: Double? = null

        fun extrair(regex: Regex): MatchResult? {
            val resultado = regex.find(restante) ?: return null
            restante = restante.replaceRange(resultado.range, " ")
            return resultado
        }

        extrair(ENTRE)?.let {
            val a = parseValor(it.groupValues[1])
            val b = parseValor(it.groupValues[2])
            precoMin = minOf(a, b)
            precoMax = maxOf(a, b)
        }
        extrair(MAXIMO)?.let { precoMax = parseValor(it.groupValues[1]) }
        extrair(MINIMO)?.let { precoMin = parseValor(it.groupValues[1]) }
        // "livros 50 reais" sem palavra de apoio: entende como valor máximo
        extrair(VALOR_SOLTO)?.let { if (precoMax == null) precoMax = parseValor(it.groupValues[1]) }

        // 2) Tipo, categoria e palavras-chave
        val tokens = PALAVRA.findAll(restante).map { it.value }.toList()
        val tipo = when {
            tokens.any { it in TIPO_DOACAO } -> TipoAnuncio.DOACAO
            tokens.any { it in TIPO_TROCA } -> TipoAnuncio.TROCA
            tokens.any { it in TIPO_VENDA } -> TipoAnuncio.VENDA
            else -> null
        }
        val categoria = tokens.firstNotNullOfOrNull { CATEGORIAS_POR_PALAVRA[it] }

        val originais = PALAVRA_ORIGINAL.findAll(original).map { it.value }.toList()
        val exibicaoPorTermo = originais.associateBy { radical(it.normalizar()) }

        val termos = tokens
            .filter { it !in STOPWORDS && it !in CATEGORIAS_POR_PALAVRA && it !in PALAVRAS_DE_TIPO }
            .filter { it.length >= 2 && !it.all(Char::isDigit) }
            .map { radical(it) }
            .distinct()

        val consulta = ConsultaAnuncios(
            termos = termos,
            termosExibicao = termos.map { exibicaoPorTermo[it] ?: it },
            categoria = categoria,
            tipo = tipo,
            precoMin = precoMin,
            precoMax = precoMax
        )

        return when {
            !consulta.vazia -> Intencao.Buscar(consulta)
            palavras.any { it in PEDIDO_GERAL } -> Intencao.Buscar(consulta) // "mostre os anúncios"
            else -> Intencao.NaoEntendi
        }
    }

    // "1.500,50" -> 1500.5 | "12,5" -> 12.5 | "1.500" -> 1500.0 | "50" -> 50.0
    private fun parseValor(texto: String): Double = when {
        ',' in texto -> texto.replace(".", "").replace(',', '.').toDouble()
        MILHAR.matches(texto) -> texto.replace(".", "").toDouble()
        else -> texto.toDouble()
    }

    // Plural simples: "livros" -> "livro", "calculadoras" -> "calculadora"
    private fun radical(palavra: String): String =
        if (palavra.length > 3 && palavra.endsWith("s") && !palavra.endsWith("ss")) palavra.dropLast(1) else palavra

    private companion object {
        val PALAVRA = Regex("[a-z0-9]+")
        val PALAVRA_ORIGINAL = Regex("[\\p{L}\\p{N}]+")
        val MILHAR = Regex("\\d{1,3}(\\.\\d{3})+")

        const val NUM = "(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)"
        val ENTRE = Regex("\\bentre\\s+$NUM\\s+e\\s+$NUM")
        val MAXIMO = Regex("\\b(?:ate|menos de|no maximo|maximo de|maximo|abaixo de|menor que|por ate)\\s+$NUM")
        val MINIMO = Regex("\\b(?:acima de|mais de|a partir de|no minimo|minimo de|minimo|maior que)\\s+$NUM")
        val VALOR_SOLTO = Regex("\\b$NUM\\s*(?:reais|real)\\b")

        val TIPO_DOACAO = setOf(
            "gratis", "gratuito", "gratuitos", "gratuita", "gratuitas", "graca",
            "doacao", "doacoes", "doar", "doado", "doados", "doada", "doadas"
        )
        val TIPO_TROCA = setOf("troca", "trocas", "trocar", "trocado", "trocados")
        val TIPO_VENDA = setOf("comprar", "compra", "venda", "vendas")
        val PALAVRAS_DE_TIPO = TIPO_DOACAO + TIPO_TROCA + TIPO_VENDA

        // Só palavras que são o próprio nome da categoria viram filtro de categoria.
        // "calculadora" continua sendo busca por título (pode estar em qualquer categoria).
        val CATEGORIAS_POR_PALAVRA = mapOf(
            "livro" to "Livros", "livros" to "Livros",
            "apostila" to "Apostilas", "apostilas" to "Apostilas",
            "eletronico" to "Eletrônicos", "eletronicos" to "Eletrônicos",
            "eletronica" to "Eletrônicos", "eletronicas" to "Eletrônicos",
            "papelaria" to "Papelaria",
            "uniforme" to "Uniformes", "uniformes" to "Uniformes"
        )

        // Pedidos para alterar dados por conversa: o assistente recusa (só consulta).
        val ACOES_PROIBIDAS = setOf(
            "excluir", "exclua", "apagar", "apague", "deletar", "delete", "remover", "remova",
            "editar", "edite", "alterar", "altere", "atualizar", "atualize",
            "favoritar", "favorite", "publicar", "publique"
        )
        val SAUDACOES = setOf(
            "oi", "ola", "bom", "boa", "dia", "tarde", "noite", "e", "ai", "tudo", "bem",
            "eae", "opa", "hey", "hello", "salve"
        )
        val AJUDA = setOf("ajuda", "help", "socorro")
        val AJUDA_FRASES = listOf("o que voce faz", "o que voce pode", "como funciona", "o que posso")
        val PEDIDO_GERAL = setOf("todos", "tudo", "recentes", "novidades", "anuncios", "anuncio", "materiais", "produtos")

        val STOPWORDS = setOf(
            "quero", "queria", "gostaria", "encontrar", "encontre", "achar", "ache", "procuro", "procurar",
            "procurando", "busco", "buscar", "busque", "mostre", "mostrar", "mostra", "me", "ver", "tem",
            "tenho", "existe", "existem", "ha", "algum", "alguma", "alguns", "algumas", "um", "uma", "uns",
            "umas", "o", "a", "os", "as", "de", "do", "da", "dos", "das", "no", "na", "nos", "nas", "em",
            "para", "pra", "por", "com", "que", "e", "ou", "favor", "preciso", "precisando", "material",
            "materiais", "produto", "produtos", "anuncio", "anuncios", "coisa", "coisas", "sobre", "voce",
            "pode", "poderia", "seria", "ate", "reais", "real", "rs", "r", "menos", "mais", "maximo",
            "minimo", "abaixo", "acima", "entre", "valor", "preco", "custa", "custando", "barato",
            "baratos", "barata", "baratas", "meu", "minha", "seu", "sua", "ai", "la"
        )
    }
}
