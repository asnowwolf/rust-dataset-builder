package wang.ralph.ai.rust.dataset.application

import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Component
import wang.ralph.ai.rust.dataset.utils.extractJsonlContent

@Component
class RustDatasetBuilder(private val chatModel: ChatModel) {
    fun extractDataset(docMarkdown: String): String {
        val question =
            """### [Installing `rustup` on Windows](ch01-01-installation.html#installing-rustup-on-windows)

On Windows, go to [https://www.rust-lang.org/install.html](https://www.rust-lang.org/install.html) and follow
the instructions for installing Rust. At some point in the installation, you’ll
receive a message explaining that you’ll also need the C++ build tools for
Visual Studio 2013 or later. The easiest way to acquire the build tools is to
install [Build Tools for Visual Studio 2017](https://www.visualstudio.com/downloads/). The tools are in
the Other Tools and Frameworks section.

The rest of this book uses commands that work in both *cmd.exe* and PowerShell.
If there are specific differences, we’ll explain which to use.

"""

        val answer =
            """{"question": "How do I install Rust on Windows?", "answer": "Go to https://www.rust-lang.org/install.html and follow the instructions."}
{"question": "What is required to install Rust on Windows besides the Rust installer?", "answer": "You will also need the C++ build tools for Visual Studio 2013 or later."}
{"question": "What is the easiest way to acquire the C++ build tools for Visual Studio?", "answer": "Install Build Tools for Visual Studio 2017 from https://www.visualstudio.com/downloads/."}
{"question": "Where can I find the Build Tools for Visual Studio 2017?", "answer": "They are in the Other Tools and Frameworks section."}
{"question": "What command line interfaces are supported by the commands in this book?", "answer": "The commands work in both *cmd.exe* and PowerShell."}"""

        val systemInstruction =
            UserMessage(
                """You are a Rust expert, and I need your help generating a dataset for fine-tuning a Rust model based on the Rust documentation.

Next, I will provide you with a markdown document, and I want you to convert it into a series of question-and-answer pairs.

Please keep in mind:

All knowledge must come from my input; do not create facts yourself.
Question-and-answer pairs should not overlap or contradict each other.
Please output in the jsonl format as in the example answer for programmatic processing.

**Example Question**

$question

**Example Answer**

$answer

""".trimMargin()
            )
        val result =
            chatModel.call(systemInstruction, AssistantMessage("ok"), UserMessage(docMarkdown))
        val content = extractJsonlContent(result)
        return content
    }
}
