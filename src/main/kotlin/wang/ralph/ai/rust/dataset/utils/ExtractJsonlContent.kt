package wang.ralph.ai.rust.dataset.utils

fun extractJsonlContent(markdown: String): String {
    val pattern = Regex("""^```jsonl?\n(.*)\n```\n*$""", RegexOption.DOT_MATCHES_ALL)
    val matchResult = pattern.find(markdown)

    val content = matchResult?.groupValues?.get(1) ?: run {
        throw IllegalArgumentException("输出结果格式错误，未找到jsonl")
    }
    return content
}
