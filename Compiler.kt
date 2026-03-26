fun main(target: Array<String>) {
    require(target.isNotEmpty()) {
        "Butuh argumen path XML. Contoh: compiler layout.xml [hasil.kt] [output.klib]"
    }

    val xmlPath = target[0]
    require(xmlPath.endsWith(".xml")) {
        "File input harus berekstensi .xml: $xmlPath"
    }

    val generatedKtPath = target.getOrElse(1) { gantiEkstensi(xmlPath, ".generated.kt") }
    val outputPath = target.getOrElse(2) { gantiEkstensi(xmlPath, ".ui") }
    val namaFungsi = "build" + ambilNamaDasar(xmlPath).toPascalCase()

    val pembaca = BacaXml(arrayOf(xmlPath))
    val sourceKotlin = pembaca.jadiKotlin(namaFungsi)

    Hasil(sourceKotlin).tulisKe(generatedKtPath)
    Binari().compileKlib(
        generatedKtPath = generatedKtPath,
        outputPath = outputPath
    )

    println("XML      : $xmlPath")
    println("Cache KT : $generatedKtPath")
    println("Binary   : $outputPath")
}

private fun gantiEkstensi(path: String, ekstensiBaru: String): String {
    val namaDasar = path.substringBeforeLast('.', path)
    return namaDasar + ekstensiBaru
}

private fun ambilNamaDasar(path: String): String {
    return path.substringAfterLast('/').substringBeforeLast('.')
}

private fun String.toPascalCase(): String {
    return split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotEmpty() }
        .joinToString("") { bagian ->
            bagian.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
        .ifEmpty { "Layout" }
}
