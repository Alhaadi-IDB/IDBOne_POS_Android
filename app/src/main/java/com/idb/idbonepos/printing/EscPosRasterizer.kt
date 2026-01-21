package com.idb.idbonepos.printing

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlin.math.min

object EscPosRasterizer {
    fun bitmapToRasterBytes(bitmap: Bitmap, threshold: Int = 128): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8

        val xL = (bytesPerRow and 0xFF).toByte()
        val xH = ((bytesPerRow shr 8) and 0xFF).toByte()
        val yL = (height and 0xFF).toByte()
        val yH = ((height shr 8) and 0xFF).toByte()

        val output = ByteArrayOutputStream(8 + bytesPerRow * height)
        output.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

        val row = IntArray(width)
        for (y in 0 until height) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            var byte = 0
            var bitCount = 0
            for (x in 0 until width) {
                val color = row[x]
                val alpha = Color.alpha(color)
                val isBlack = if (alpha < 128) {
                    false
                } else {
                    val r = Color.red(color)
                    val g = Color.green(color)
                    val b = Color.blue(color)
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
                    luminance < threshold
                }

                byte = byte shl 1
                if (isBlack) {
                    byte = byte or 0x01
                }
                bitCount++

                if (bitCount == 8) {
                    output.write(byte)
                    byte = 0
                    bitCount = 0
                }
            }

            if (bitCount > 0) {
                byte = byte shl (8 - bitCount)
                output.write(byte)
            }
        }

        return output.toByteArray()
    }

    fun bitmapToRasterChunks(
        bitmap: Bitmap,
        threshold: Int = 128,
        maxRowsPerChunk: Int = 64
    ): List<ByteArray> {
        require(maxRowsPerChunk > 0) { "maxRowsPerChunk must be > 0" }

        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8
        val row = IntArray(width)

        val chunks = mutableListOf<ByteArray>()
        var y = 0
        while (y < height) {
            val chunkHeight = min(maxRowsPerChunk, height - y)
            val xL = (bytesPerRow and 0xFF).toByte()
            val xH = ((bytesPerRow shr 8) and 0xFF).toByte()
            val yL = (chunkHeight and 0xFF).toByte()
            val yH = ((chunkHeight shr 8) and 0xFF).toByte()

            val output = ByteArrayOutputStream(8 + bytesPerRow * chunkHeight)
            output.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

            for (rowOffset in 0 until chunkHeight) {
                val rowY = y + rowOffset
                bitmap.getPixels(row, 0, width, 0, rowY, width, 1)
                var byte = 0
                var bitCount = 0
                for (x in 0 until width) {
                    val color = row[x]
                    val alpha = Color.alpha(color)
                    val isBlack = if (alpha < 128) {
                        false
                    } else {
                        val r = Color.red(color)
                        val g = Color.green(color)
                        val b = Color.blue(color)
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
                        luminance < threshold
                    }

                    byte = byte shl 1
                    if (isBlack) {
                        byte = byte or 0x01
                    }
                    bitCount++

                    if (bitCount == 8) {
                        output.write(byte)
                        byte = 0
                        bitCount = 0
                    }
                }

                if (bitCount > 0) {
                    byte = byte shl (8 - bitCount)
                    output.write(byte)
                }
            }

            chunks.add(output.toByteArray())
            y += chunkHeight
        }

        return chunks
    }
}
