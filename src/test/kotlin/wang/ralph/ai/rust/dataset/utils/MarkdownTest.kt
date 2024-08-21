package wang.ralph.ai.rust.dataset.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MarkdownTest {
    private val markdown = Markdown()

    private val md = """
# h1

## h2
<!-- test -->
paragraph

<header class="abc">Test</header>

<section>Test</section>

**strong** *italy* `abc`

```kotlin
val a = 1
```

[link](/link)

> a
> b
>
> c
>
> # d

* a
* b
* c
    1. c1
    2. c2

<code-example language="html">
  &lt;ul&gt; &lt;ng-template
 <a href="api/common/NgFor" class="code-anchor">ngFor</a> let-hero [
 <a href="api/common/NgForOf" class="code-anchor">ngForOf</a>]="heroes"&gt; &lt;li&gt;{{hero.name}}&lt;/li&gt; &lt;/ng-template&gt; &lt;/ul&gt;
</code-example>
"""
    private val html = """<h1>h1</h1>
<h2>h2</h2>
<!-- test -->
<p>paragraph</p>
<header class="abc">Test</header>
<section>Test</section>
<p><strong>strong</strong> <em>italy</em> <code>abc</code></p>
<pre><code class="language-kotlin">val a = 1
</code></pre>
<p><a href="/link">link</a></p>
<blockquote>
<p>a
b</p>
<p>c</p>
<h1>d</h1>
</blockquote>
<ul>
<li>a</li>
<li>b</li>
<li>c
<ol>
<li>c1</li>
<li>c2</li>
</ol>
</li>
</ul>
<code-example language="html">
  &lt;ul&gt; &lt;ng-template
 <a href="api/common/NgFor" class="code-anchor">ngFor</a> let-hero [
 <a href="api/common/NgForOf" class="code-anchor">ngForOf</a>]="heroes"&gt; &lt;li&gt;{{hero.name}}&lt;/li&gt; &lt;/ng-template&gt; &lt;/ul&gt;
</code-example>
"""

    @Test
    fun parse() {
        val dom = markdown.parse(md)
        assertThat(dom.firstChild).isNotNull
    }

    @Test
    fun toHtml() {
        val result = markdown.toHtml(md)
        assertThat(result).isEqualTo(html)
    }

    @Test
    fun htmlToMd() {
        val result = markdown.fromHtml(html)
        assertThat(result).isEqualTo(md)
    }

    @Test
    fun htmlToMdWithCode() {
        val html =
            """<code-example header="src/app/sizer.component.ts" path="two-way-binding/src/app/sizer/sizer.component.ts" region="sizer-component">
        export class SizerComponent {
            @<a href="api/core/Input" class="code-anchor">Input</a>() size!: number | string;
            @<a href="api/core/Output" class="code-anchor">Output</a>() sizeChange = new <a href="api/core/EventEmitter" class="code-anchor">EventEmitter</a>&lt;number&gt;();

            dec() {
                this.resize(-1);
            }
            inc() {
                this.resize(+1);
            }

            resize(delta: number) {
                this.size = Math.min(40, Math.max(8, +this.size + delta));
                this.sizeChange.emit(this.size);
            }
        }

</code-example>
"""
        val md = markdown.fromHtml(html)
        val rebuilt = markdown.toHtml(md)
        assertThat(rebuilt).isEqualTo(html)
    }

    @Test
    fun structuralSame() {
        assertThat(Markdown.structuralDiff("ab*c*", "a*b*c")).isEqualTo(null)
        assertThat(Markdown.structuralDiff("*a*b*c*", "a*b*c")).isEqualTo(
            """[Emphasis]
L: a；c
R: b"""
        )

        assertThat(Markdown.structuralDiff("[a](b)", "[c](b)")).isEqualTo(null)
        assertThat(Markdown.structuralDiff("[a](b)", "[c](c)")).isEqualTo(
            """[Link]
L-R: b
R-L: c"""
        )
    }
}
