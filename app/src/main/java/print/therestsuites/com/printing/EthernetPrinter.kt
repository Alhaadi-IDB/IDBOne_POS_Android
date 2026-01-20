package print.therestsuites.com.printing

import java.io.IOException
import java.net.Socket

class EthernetPrinter {
    fun print(address: String, chunks: List<ByteArray>) {
        val socket = Socket(address, 9100)

        try {
            val output = socket.getOutputStream()
            for (chunk in chunks) {
                if (chunk.isNotEmpty()) {
                    output.write(chunk)
                    output.flush()
                }
            }
            output.flush()
        } finally {
            try {
                socket.close()
            } catch (_: IOException) {
                // Ignore close errors.
            }
        }
    }
}
