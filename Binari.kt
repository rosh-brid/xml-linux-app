import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.system

@OptIn(ExperimentalForeignApi::class)
class Binari {
    fun compileKlib(
        generatedKtPath: String,
        outputPath: String,
        hapusCache: Boolean = false
    ) {
        val command = buildCompileCommand(generatedKtPath, outputPath)
        val exitCode = system(command)

        require(exitCode == 0) {
            "Gagal compile binary dengan exit code $exitCode"
        }

        if (hapusCache) {
            Hasil("").hapus(generatedKtPath)
        }
    }

    private fun buildCompileCommand(generatedKtPath: String, outputPath: String): String {
        val sumber = listOf(
            "lib/xml-linux/widget/WidgetNode.kt",
            "lib/xml-linux/widget/LinearLayout.kt",
            "lib/xml-linux/widget/TextView.kt",
            "lib/xml-linux/widget/ImageView.kt",
            generatedKtPath
        ).joinToString(" ") { quoteArg(it) }

        return buildString {
            append("kotlinc-native ")
            append(sumber)
            append(" -produce library -o ")
            append(quoteArg(outputPath))
        }
    }

    private fun quoteArg(value: String): String {
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }
}
