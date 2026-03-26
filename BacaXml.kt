import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.F_OK
import platform.posix.access
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.rewind
import platform.posix.size_t
import platform.posix.SEEK_END

@OptIn(ExperimentalForeignApi::class)
class BacaXml(private val jalur: Array<String>) {
    fun terima(): WidgetNode {
        val target = bacaFile()
        val barisXml = target
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return kirim(barisXml)
    }

    fun kirim(terima: List<String>): WidgetNode {
        val xml = terima.joinToString("\n")
        val parser = XmlParser(xml)
        val root = parser.parseElement()

        return buildWidget(root)
    }

    fun jadiKotlin(namaFungsi: String = "buildLayout"): String {
        val root = terima()

        return buildString {
            appendLine("fun $namaFungsi(): WidgetNode {")
            appendLine("    return ${widgetKeKotlin(root, 1)}")
            appendLine("}")
        }
    }

    private fun bacaFile(): String {
        val path = jalur.firstOrNull()?.trim()
            ?: throw IllegalArgumentException("Path file XML tidak boleh kosong")

        require(access(path, F_OK) == 0) { "File tidak ditemukan: $path" }

        val file = fopen(path, "rb")
            ?: throw IllegalArgumentException("Gagal membuka file: $path")

        try {
            fseek(file, 0, SEEK_END)
            val ukuran = ftell(file)
            require(ukuran >= 0) { "Gagal membaca ukuran file: $path" }
            rewind(file)

            if (ukuran == 0L) {
                return ""
            }

            val buffer = ByteArray(ukuran.toInt())
            val jumlahDibaca = buffer.usePinned { pinned ->
                fread(pinned.addressOf(0), 1.convert<size_t>(), ukuran.convert<size_t>(), file)
            }

            require(jumlahDibaca.toLong() == ukuran) {
                "Gagal membaca seluruh isi file: $path"
            }

            return buffer.decodeToString()
        } finally {
            fclose(file)
        }
    }

    private fun buildWidget(element: XmlElement): WidgetNode {
        return when (element.name) {
            "LinearLayout" -> LinearLayout(
                id = ambilAtributUtama(element.attributes, "id"),
                orientation = element.attributes["orientation"] ?: "vertical",
                layoutWidth = ambilAtributUtama(element.attributes, "layout_width") ?: "wrap_content",
                layoutHeight = ambilAtributUtama(element.attributes, "layout_height") ?: "wrap_content",
                children = element.children.map { child -> buildWidget(child) }
            )

            "TextView" -> TextView(
                id = ambilAtributUtama(element.attributes, "id"),
                text = ambilAtributUtama(element.attributes, "text") ?: "",
                layoutWidth = ambilAtributUtama(element.attributes, "layout_width") ?: "wrap_content",
                layoutHeight = ambilAtributUtama(element.attributes, "layout_height") ?: "wrap_content"
            )

            "ImageView" -> ImageView(
                id = ambilAtributUtama(element.attributes, "id"),
                src = ambilAtributUtama(element.attributes, "src") ?: "",
                layoutWidth = ambilAtributUtama(element.attributes, "layout_width") ?: "wrap_content",
                layoutHeight = ambilAtributUtama(element.attributes, "layout_height") ?: "wrap_content"
            )

            else -> throw IllegalArgumentException("Tag tidak didukung: ${element.name}")
        }
    }

    private fun ambilAtributUtama(attributes: Map<String, String>, nama: String): String? {
        return attributes[nama]
            ?: attributes["android:$nama"]
            ?: when (nama) {
                "layout_width" -> attributes["width"]
                "layout_height" -> attributes["height"]
                else -> null
            }
    }

    private fun widgetKeKotlin(widget: WidgetNode, level: Int): String {
        val indent = "    ".repeat(level)
        val childIndent = "    ".repeat(level + 1)

        return when (widget) {
            is LinearLayout -> buildString {
                appendLine("LinearLayout(")
                appendLine("$childIndent id = ${quoteNullable(widget.id)},")
                appendLine("$childIndent orientation = ${quote(widget.orientation)},")
                appendLine("$childIndent layoutWidth = ${quote(widget.layoutWidth)},")
                appendLine("$childIndent layoutHeight = ${quote(widget.layoutHeight)},")
                appendLine("$childIndent children = listOf(")
                append(
                    widget.children.joinToString(",\n") { child ->
                        "$childIndent    ${widgetKeKotlin(child, level + 2)}"
                    }
                )
                if (widget.children.isNotEmpty()) {
                    appendLine()
                }
                appendLine("$childIndent )")
                append("$indent)")
            }

            is TextView -> buildString {
                appendLine("TextView(")
                appendLine("$childIndent id = ${quoteNullable(widget.id)},")
                appendLine("$childIndent text = ${quote(widget.text)},")
                appendLine("$childIndent layoutWidth = ${quote(widget.layoutWidth)},")
                appendLine("$childIndent layoutHeight = ${quote(widget.layoutHeight)}")
                append("$indent)")
            }

            is ImageView -> buildString {
                appendLine("ImageView(")
                appendLine("$childIndent id = ${quoteNullable(widget.id)},")
                appendLine("$childIndent src = ${quote(widget.src)},")
                appendLine("$childIndent layoutWidth = ${quote(widget.layoutWidth)},")
                appendLine("$childIndent layoutHeight = ${quote(widget.layoutHeight)}")
                append("$indent)")
            }

            else -> throw IllegalArgumentException("Widget tidak didukung untuk generator Kotlin")
        }
    }

    private fun quote(value: String): String {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    private fun quoteNullable(value: String?): String {
        return value?.let { quote(it) } ?: "null"
    }

    private data class XmlElement(
        val name: String,
        val attributes: Map<String, String>,
        val children: List<XmlElement>
    )

    private class XmlParser(private val xml: String) {
        private var index = 0

        fun parseElement(): XmlElement {
            skipWhitespace()
            skipProlog()
            skipWhitespace()

            require(peek() == '<') { "XML tidak valid di index $index" }
            consume('<')

            val tagName = readName()
            val attributes = readAttributes()
            skipWhitespace()

            if (match("/>")) {
                return XmlElement(tagName, attributes, emptyList())
            }

            consume('>')

            val children = mutableListOf<XmlElement>()

            while (true) {
                skipWhitespace()

                if (match("</")) {
                    val closingName = readName()
                    require(closingName == tagName) {
                        "Tag penutup </$closingName> tidak cocok dengan <$tagName>"
                    }
                    skipWhitespace()
                    consume('>')
                    break
                }

                if (peek() != '<') {
                    readTextContent()
                    continue
                }

                children += parseElement()
            }

            return XmlElement(tagName, attributes, children)
        }

        private fun readAttributes(): Map<String, String> {
            val attributes = mutableMapOf<String, String>()

            while (true) {
                skipWhitespace()
                val current = peek()

                if (current == '>' || (current == '/' && peek(1) == '>')) {
                    break
                }

                val name = readName()
                skipWhitespace()
                consume('=')
                skipWhitespace()
                val value = readQuotedValue()
                attributes[name] = value
            }

            return attributes
        }

        private fun readQuotedValue(): String {
            val quote = peek()
            require(quote == '"' || quote == '\'') {
                "Nilai atribut harus diapit tanda kutip di index $index"
            }

            consume(quote)
            val start = index
            while (index < xml.length && xml[index] != quote) {
                index++
            }
            require(index < xml.length) { "Atribut belum ditutup kutipnya" }
            val value = xml.substring(start, index)
            consume(quote)
            return value
        }

        private fun readName(): String {
            val start = index
            while (index < xml.length && isNameChar(xml[index])) {
                index++
            }
            require(start != index) { "Nama tag atau atribut tidak ditemukan di index $index" }
            return xml.substring(start, index)
        }

        private fun readTextContent() {
            while (index < xml.length && xml[index] != '<') {
                index++
            }
        }

        private fun skipProlog() {
            if (match("<?xml")) {
                while (index < xml.length && !match("?>")) {
                    index++
                }
            }
        }

        private fun skipWhitespace() {
            while (index < xml.length && xml[index].isWhitespace()) {
                index++
            }
        }

        private fun consume(expected: Char) {
            require(peek() == expected) { "Diharapkan '$expected' di index $index" }
            index++
        }

        private fun match(expected: String): Boolean {
            if (!xml.startsWith(expected, index)) {
                return false
            }

            index += expected.length
            return true
        }

        private fun peek(offset: Int = 0): Char? {
            val target = index + offset
            return if (target in xml.indices) xml[target] else null
        }

        private fun isNameChar(char: Char): Boolean {
            return char.isLetterOrDigit() || char == '_' || char == '-' || char == ':'
        }
    }
}
