package com.idb.idbonepos.printing

object EscPosCommands {
    fun parseHexString(input: String): ByteArray {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ByteArray(0)

        val tokens = trimmed.split(Regex("[,\\s]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val bytes = ByteArray(tokens.size)
        for (i in tokens.indices) {
            val token = tokens[i]
            val normalized = token.removePrefix("0x").removePrefix("0X")
            val value = normalized.toInt(16)
            require(value in 0..255) { "Byte out of range: $token" }
            bytes[i] = value.toByte()
        }
        return bytes
    }
}
