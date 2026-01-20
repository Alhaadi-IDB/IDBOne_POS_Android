package print.therestsuites.com.printing

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

class PdfDownloader(private val client: OkHttpClient = OkHttpClient()) {
    fun downloadPdf(context: Context, pdfId: String): File {
        val url = "https://pos.therestsuites.com/getPDF/$pdfId"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download PDF (${response.code})")
            }
            val body = response.body ?: throw IOException("Empty PDF response body")
            val file = File(context.cacheDir, "print_$pdfId.pdf")
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return file
        }
    }
}
