package wang.ralph.ai.rust.dataset.application

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import kotlin.io.path.name

@SpringBootTest
class RustDatasetBuilderTest {

    @Autowired
    lateinit var builder: RustDatasetBuilder

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun extractDatasetFromRustDocs() {
        val files = File("../doc.rust-lang.org").walk()
            .filter { it.isFile && it.extension == "md" && it.toPath().name != "print.md" }
        for (file in files) {
            val jsonlFile = File(file.path.replace(Regex("""\.md$"""), ".jsonl"))
            if (!jsonlFile.exists()) {
                logger.info("Extracting {}", file.path)
                val doc = file.readText()
                val dataset = builder.extractDataset(doc)
                jsonlFile.writeText(dataset)
            }
        }
    }

    @Test
    fun testCombineDataset() {
        val files = File("../doc.rust-lang.org").walk()
            .filter { it.isFile && it.extension == "jsonl" && it.name != "dataset.jsonl" }
        val result = builder.combineDataset(files)
        File("../doc.rust-lang.org/dataset.jsonl").writeText(result)
    }
}
