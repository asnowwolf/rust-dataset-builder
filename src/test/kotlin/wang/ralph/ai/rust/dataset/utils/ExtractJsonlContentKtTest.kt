package wang.ralph.ai.rust.dataset.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExtractJsonlContentKtTest {

    @Test
    fun testExtractJsonlContent() {
        val result = extractJsonlContent(
            """```json
            |{"q":"q1","a":"a1"}
            |{"q":"q2","a":"a2"}
            |{"q":"q3","a":"a3"}
            |```
        """.trimMargin()
        )
        assertThat(result).isEqualTo(
            """{"q":"q1","a":"a1"}
            |{"q":"q2","a":"a2"}
            |{"q":"q3","a":"a3"}
""".trimMargin()
        )
    }
}
