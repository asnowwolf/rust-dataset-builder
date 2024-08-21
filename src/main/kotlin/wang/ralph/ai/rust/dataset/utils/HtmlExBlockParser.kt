package wang.ralph.ai.rust.dataset.utils

import org.commonmark.internal.util.Parsing
import org.commonmark.node.Block
import org.commonmark.node.HtmlBlock
import org.commonmark.node.Paragraph
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.*
import java.util.regex.Pattern

class HtmlExBlockParser private constructor(private val closingPattern: Pattern?) : AbstractBlockParser() {
    private val block = HtmlBlock()

    private var finished = false
    private var content: BlockContent? = BlockContent()

    override fun getBlock(): Block {
        return block
    }

    override fun tryContinue(state: ParserState): BlockContinue? {
        if (finished) {
            return BlockContinue.none()
        }

        // Blank line ends type 6 and type 7 blocks
        return if (state.isBlank && closingPattern == null) {
            BlockContinue.none()
        } else {
            BlockContinue.atIndex(state.index)
        }
    }

    override fun addLine(line: SourceLine) {
        content!!.add(line.content)

        if (closingPattern != null && closingPattern.matcher(line.content).find()) {
            finished = true
        }
    }

    override fun closeBlock() {
        block.literal = content!!.string
        content = null
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(state: ParserState, matchedBlockParser: MatchedBlockParser): BlockStart? {
            val nextNonSpace = state.nextNonSpaceIndex
            val line = state.line.content

            if (state.indent < 4 && line[nextNonSpace] == '<') {
                for (blockType in 1..7) {
                    // Type 7 can not interrupt a paragraph (not even a lazy one)
                    if (blockType == 7 && (matchedBlockParser.matchedBlockParser.block is Paragraph ||
                                state.activeBlockParser.canHaveLazyContinuationLines())
                    ) {
                        continue
                    }
                    val opener = BLOCK_PATTERNS[blockType][0]
                    val closer = BLOCK_PATTERNS[blockType][1]
                    val matches = opener!!.matcher(line.subSequence(nextNonSpace, line.length)).find()
                    if (matches) {
                        return BlockStart.of(HtmlExBlockParser(closer)).atIndex(state.index)
                    }
                }
            }
            return BlockStart.none()
        }
    }

    companion object {
        private val BLOCK_PATTERNS = arrayOf(
            arrayOf<Pattern?>(null, null),  // not used (no type 0)
            arrayOf(
                Pattern.compile("^<(?:script|pre|style|textarea|code-example)(?:\\s|>|$)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("</(?:script|pre|style|textarea|code-example)>", Pattern.CASE_INSENSITIVE)
            ),
            arrayOf(
                Pattern.compile("^<!--"),
                Pattern.compile("-->")
            ),
            arrayOf(
                Pattern.compile("^<[?]"),
                Pattern.compile("\\?>")
            ),
            arrayOf(
                Pattern.compile("^<![A-Z]"),
                Pattern.compile(">")
            ),
            arrayOf(
                Pattern.compile("^<!\\[CDATA\\["),
                Pattern.compile("]]>")
            ),
            arrayOf(
                Pattern.compile(
                    "^</?(?:" +
                            "address|article|aside|" +
                            "base|basefont|blockquote|body|" +
                            "caption|center|col|colgroup|" +
                            "dd|details|dialog|dir|div|dl|dt|" +
                            "fieldset|figcaption|figure|footer|form|frame|frameset|" +
                            "h1|h2|h3|h4|h5|h6|head|header|hr|html|" +
                            "iframe|" +
                            "legend|li|link|" +
                            "main|menu|menuitem|" +
                            "nav|noframes|" +
                            "ol|optgroup|option|" +
                            "p|param|" +
                            "section|source|summary|" +
                            "table|tbody|td|tfoot|th|thead|title|tr|track|" +
                            "ul" +
                            ")(?:\\s|[/]?[>]|$)", Pattern.CASE_INSENSITIVE
                ),
                null // terminated by blank line
            ),
            arrayOf(
                Pattern.compile("^(?:" + Parsing.OPENTAG + '|' + Parsing.CLOSETAG + ")\\s*$", Pattern.CASE_INSENSITIVE),
                null // terminated by blank line
            )
        )
    }
}
