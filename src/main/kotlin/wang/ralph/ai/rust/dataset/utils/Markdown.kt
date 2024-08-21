package wang.ralph.ai.rust.dataset.utils

import org.commonmark.internal.DocumentParser
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.text.TextContentRenderer
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.slf4j.LoggerFactory
import org.jsoup.nodes.Node as HtmlNode
import org.jsoup.parser.Parser as JsoupParser

class Markdown {
    private val logger = LoggerFactory.getLogger(javaClass)
    fun parse(markdown: String): Node {
        val enabledBlockTypes = DocumentParser.getDefaultBlockParserTypes().apply {
            this.remove(IndentedCodeBlock::class.java)
            this.remove(HtmlBlock::class.java)
        }
        val parser = Parser.builder()
            .enabledBlockTypes(enabledBlockTypes)
            .customBlockParserFactory(HtmlExBlockParser.Factory())
            .build()
        return parser.parse(markdown)
    }

    fun toHtml(markdown: String): String {
        val document = parse(markdown)
        val htmlRenderer = HtmlRenderer.builder().build()
        val patternOfCodeExample = Regex(
            """<pre><code>(&lt;(code|live)-example.*?&lt;/(code|live)-example&gt;)
</code></pre>""", RegexOption.DOT_MATCHES_ALL
        )
        return htmlRenderer.render(document).lines().joinToString("\n") { it.trimEnd() }
            .replace(patternOfCodeExample) {
                JsoupParser.unescapeEntities(it.groupValues[1], false)
            }
    }

    fun fromHtml(html: String): String {
        val doc = Jsoup.parse(html)
        doc.outputSettings().prettyPrint(false)

        return domToMarkdown(doc).lines().joinToString("\n") { it.trimEnd() }
    }

    fun toPlainText(markdown: String): String {
        val doc = parse(markdown)
        return doc.toPlainText()
    }

    private var index: Int = 0

    private fun domToMarkdown(dom: HtmlNode): String {
        val nodeName = dom.nodeName()
        val children = dom.childNodes().joinToString("") {
            domToMarkdown(it)
        }
        val markdown = when (nodeName) {
            "h1" -> "\n# $children\n"
            "h2" -> "\n## $children\n"
            "h3" -> "\n### $children\n"
            "h4" -> "\n#### $children\n"
            "h5" -> "\n##### $children\n"
            "h6" -> "\n###### $children\n"
            "p" -> "\n$children\n"
            "pre" -> "\n$children\n"
            "em" -> "*$children*"
            "strong" -> "**$children**"
            "ul" -> {
                if (dom.parent().nodeName() == "li") {
                    children.lines().joinToString("\n") { "    $it" }.trimEnd()
                } else {
                    "\n${children}"
                }
            }

            "ol" -> {
                index = 0
                if (dom.parent().nodeName() == "li") {
                    children.lines().joinToString("\n") { "    $it" }.trimEnd()
                } else {
                    "\n${children}"
                }
            }

            "li" -> if (dom.parent().nodeName() == "ol") {
                "${++index}. $children\n"
            } else {
                "* $children\n"
            }

            "code" -> {
                if (dom.hasAttr("class")) {
                    val language = dom.attr("class").replace(Regex("^language-"), "")
                    "```$language\n${children.trim()}\n```"
                } else {
                    "`$children`"
                }
            }

            "code-example", "live-example" -> "\n${dom.outerHtml().trim()}\n"

            "blockquote" -> {
                val quoteContent =
                    children.trim().lines().joinToString("\n") { if (it.isBlank()) ">" else "> $it" }
                "\n${quoteContent}\n"
            }

            "a" -> if (dom.hasAttr("href")) "[$children](${dom.attr("href")})" else dom.outerHtml()
            "#text" -> {
                val text = (dom as TextNode).wholeText
                if (text != "\n") {
                    text
                } else {
                    ""
                }
            }

            "#document", "html", "head", "body", "div" -> children

            else -> {
                logger.info("Unknown tag: $nodeName")
                when (dom) {
                    is Element -> {
                        val attrs = dom.attributes().html()
                        val tag = "<$nodeName${attrs}>$children</$nodeName>"
                        if (dom.isBlock) {
                            "\n${tag}\n"
                        } else {
                            tag
                        }
                    }

                    is Comment -> {
                        "<!--${dom.data}-->"
                    }

                    else -> {
                        children
                    }
                }
            }
        }
        return markdown
    }

    companion object {
        fun structuralDiff(original: String, translation: String): String? {
            class StructuralVisitor : AbstractVisitor() {
                private var codes: MutableSet<Code> = mutableSetOf()
                private var emphasises: MutableSet<Emphasis> = mutableSetOf()
                private var strongEmphasises: MutableSet<StrongEmphasis> = mutableSetOf()
                private var inlineHtmls: MutableSet<HtmlInline> = mutableSetOf()
                private val images: MutableSet<Image> = mutableSetOf()
                private val links: MutableSet<Link> = mutableSetOf()
                private val linkReferenceDefinitions: MutableSet<LinkReferenceDefinition> = mutableSetOf()
                fun diff(another: StructuralVisitor): String {
                    return listOfNotNull(
                        codes.diff(another.codes) { it.literal },
                        emphasises.diffIndexed(another.emphasises) { i, _ -> "${i + 1}" },
                        strongEmphasises.diffIndexed(another.strongEmphasises) { i, _ -> "${i + 1}" },
                        inlineHtmls.diff(another.inlineHtmls) { it.literal },
                        images.diff(another.images) { it.destination },
                        links.diff(another.links) { it.destination },
                        linkReferenceDefinitions.diff(another.linkReferenceDefinitions) { it.destination }
                    ).joinToString("\n\n")
                }

                override fun visit(code: Code?) {
                    code?.let { codes.add(it) }
                    super.visit(code)
                }

                override fun visit(emphasis: Emphasis?) {
                    emphasis?.let { emphasises.add(it) }
                    super.visit(emphasis)
                }

                override fun visit(htmlInline: HtmlInline?) {
                    htmlInline?.let { inlineHtmls.add(it) }
                    super.visit(htmlInline)
                }

                override fun visit(image: Image?) {
                    image?.let { images.add(it) }
                    super.visit(image)
                }

                override fun visit(link: Link?) {
                    link?.let { links.add(it) }
                    super.visit(link)
                }

                override fun visit(strongEmphasis: StrongEmphasis?) {
                    strongEmphasis?.let { strongEmphasises.add(it) }
                    super.visit(strongEmphasis)
                }

                override fun visit(linkReferenceDefinition: LinkReferenceDefinition?) {
                    linkReferenceDefinition?.let { linkReferenceDefinitions.add(it) }
                    super.visit(linkReferenceDefinition)
                }
            }

            val node1 = Markdown().parse(original)
            val node2 = Markdown().parse(translation)

            val visitor1 = StructuralVisitor()
            visitor1.visit(node1 as Document)
            val visitor2 = StructuralVisitor()
            visitor2.visit(node2 as Document)
            return visitor1.diff(visitor2).takeIf { it.isNotBlank() }
        }
    }
}

fun Node.toPlainText(): String {
    val renderer = TextContentRenderer.builder().build()
    return renderer.render(this)
}

private fun <T : Node> Iterable<T>.diff(another: Iterable<T>, predicate: (value: T) -> String): String? {
    val className = (this.firstOrNull() ?: another.firstOrNull())?.let { it::class.simpleName }
    return className?.let {
        val thisValues = this.map(predicate).map { it.replace("angular.io", "angular.cn") }.toSet()
        val anotherValues = another.map(predicate).map { it.replace("angular.io", "angular.cn") }.toSet()
        thisValues.diff(anotherValues, className)
    }
}

private fun <T : Node> Iterable<T>.diffIndexed(
    another: Iterable<T>,
    predicate: (index: Int, value: T) -> String
): String? {
    val className = (this.firstOrNull() ?: another.firstOrNull())?.let { it::class.simpleName }
    return className?.let {
        val thisValues = this.mapIndexed(predicate).toSet()
        val anotherValues = another.mapIndexed(predicate).toSet()
        thisValues.diff(anotherValues, className)
            ?.let { "[$className]\nL: ${this.joinToString("；") { it.toPlainText() }}\nR: ${another.joinToString("；") { it.toPlainText() }}" }
    }
}

private fun Set<String>.diff(anotherValues: Set<String>, className: String): String? {
    val leftOnly = this.subtract(anotherValues).takeIf { it.isNotEmpty() }?.joinToString(";", "L-R: ")
    val rightOnly = anotherValues.subtract(this).takeIf { it.isNotEmpty() }?.joinToString(";", "R-L: ")
    return listOfNotNull(leftOnly, rightOnly).takeIf { it.isNotEmpty() }
        ?.joinToString("\n", "[$className]\n")
}
