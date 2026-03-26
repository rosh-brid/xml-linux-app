import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.remove
import platform.posix.size_t

@OptIn(ExperimentalForeignApi::class)
class Hasil(private val terima: String) {
    fun tulisKe(path: String) {
        val file = fopen(path, "w")
            ?: throw IllegalArgumentException("Gagal membuat file hasil: $path")

        try {
            val bytes = terima.encodeToByteArray()
            bytes.usePinned { pinned ->
                fwrite(
                    pinned.addressOf(0),
                    1.convert<size_t>(),
                    bytes.size.convert<size_t>(),
                    file
                )
            }
        } finally {
            fclose(file)
        }
    }

    fun hapus(path: String): Boolean {
        return remove(path) == 0
    }
}
