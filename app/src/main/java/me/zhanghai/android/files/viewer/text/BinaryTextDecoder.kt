package me.zhanghai.android.files.viewer.text

import java.io.File
import java.io.StringWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java8.nio.file.Path
import com.android.tools.smali.baksmali.Adaptors.ClassDefinition
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes

/** Converts Android-specific binary formats to editable, human-readable text. */
object BinaryTextDecoder {
    private const val CHUNK_STRING_POOL = 0x0001
    private const val CHUNK_TABLE = 0x0002
    private const val CHUNK_XML = 0x0003
    private const val CHUNK_XML_START_NAMESPACE = 0x0100
    private const val CHUNK_XML_END_NAMESPACE = 0x0101
    private const val CHUNK_XML_START_ELEMENT = 0x0102
    private const val CHUNK_XML_END_ELEMENT = 0x0103
    private const val CHUNK_XML_TEXT = 0x0104

    fun isBinaryXml(bytes: ByteArray): Boolean =
        bytes.size >= 8 && readU16(bytes, 0) == CHUNK_XML && readU16(bytes, 2) >= 8

    fun isDex(bytes: ByteArray): Boolean =
        bytes.size >= 8 && String(bytes, 0, 4, StandardCharsets.US_ASCII) == "dex\n"

    fun isArsc(bytes: ByteArray): Boolean =
        bytes.size >= 8 && readU16(bytes, 0) == CHUNK_TABLE && readU16(bytes, 2) >= 12

    fun decode(file: Path, bytes: ByteArray): String? {
        val name = file.fileName.toString().lowercase(Locale.ROOT)
        return when {
            isDex(bytes) -> decodeDex(file, bytes)
            isBinaryXml(bytes) || name.endsWith(".axml") -> decodeBinaryXml(bytes)
            isArsc(bytes) || name.endsWith(".arsc") -> decodeArsc(bytes)
            looksBinary(bytes) -> decodeHex(bytes)
            else -> null
        }
    }

    private fun decodeDex(file: Path, bytes: ByteArray): String {
        val temp = File.createTempFile("material-files-", ".dex")
        return try {
            temp.writeBytes(bytes)
            val dexFile = DexFileFactory.loadDexFile(temp, Opcodes.getDefault())
            val options = BaksmaliOptions().apply {
                parameterRegisters = true
                localsDirective = true
                sequentialLabels = true
                debugInfo = true
                codeOffsets = false
                accessorComments = false
                implicitReferences = false
            }
            val output = StringBuilder()
            for (classDef in dexFile.classes) {
                val writer = StringWriter()
                val baksmaliWriter = BaksmaliWriter(writer)
                ClassDefinition(options, classDef).writeTo(baksmaliWriter)
                baksmaliWriter.close()
                if (output.isNotEmpty()) output.append("\n\n")
                output.append(writer.toString())
            }
            output.toString().ifEmpty { "# DEX dosyasında çözümlenecek sınıf bulunamadı." }
        } finally {
            temp.delete()
        }
    }

    private fun looksBinary(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val sampleSize = minOf(bytes.size, 8192)
        var control = 0
        for (i in 0 until sampleSize) {
            val b = bytes[i].toInt() and 0xff
            if (b == 0 || (b < 0x09) || (b in 0x0e..0x1f)) control++
        }
        return control.toDouble() / sampleSize > 0.02
    }

    private fun decodeHex(bytes: ByteArray): String {
        val out = StringBuilder()
        out.append("# Binary file\n")
        out.append("# Hex view — read-only\n\n")
        val maxBytes = minOf(bytes.size, 256 * 1024)
        var offset = 0
        while (offset < maxBytes) {
            val count = minOf(16, maxBytes - offset)
            out.append(String.format(Locale.ROOT, "%08x  ", offset))
            for (i in 0 until 16) {
                if (i < count) out.append(String.format(Locale.ROOT, "%02x ", bytes[offset + i].toInt() and 0xff))
                else out.append("   ")
                if (i == 7) out.append(' ')
            }
            out.append(" |")
            for (i in 0 until count) {
                val c = bytes[offset + i].toInt() and 0xff
                out.append(if (c in 0x20..0x7e) c.toChar() else '.')
            }
            out.append("|\n")
            offset += count
        }
        if (bytes.size > maxBytes) out.append("\n# ... truncated after ").append(maxBytes).append(" bytes.\n")
        return out.toString()
    }

    private fun decodeArsc(bytes: ByteArray): String {
        // Keep the resource-table decoder lightweight. The string pool is the most useful
        // part when inspecting a standalone resources.arsc and can be decoded without
        // pulling a complete APK/resource framework into the editor.
        val parser = ChunkReader(bytes)
        val out = StringBuilder()
        out.append("# Android resource table (resources.arsc)\n")
        out.append("# Binary resource data decoded by Material Files\n\n")
        if (parser.readU16(0) != CHUNK_TABLE) return out.append("# Geçersiz ARSC başlığı.").toString()
        val packageCount = parser.readI32(8)
        out.append("package_count = ").append(packageCount).append("\n")
        var offset = parser.readU16(2)
        val end = parser.readI32(4).coerceAtMost(bytes.size)
        var stringCount = 0
        while (offset + 8 <= end) {
            val type = parser.readU16(offset)
            val size = parser.readI32(offset + 4)
            if (size < 8 || offset + size > end) break
            if (type == CHUNK_STRING_POOL) {
                val strings = StringPool.parse(bytes, offset)
                out.append("\n[string_pool]\n")
                val displayCount = minOf(strings.size, MAX_ARSC_STRINGS)
                for (index in 0 until displayCount) {
                    out.append(index).append(" = ").append(quote(strings[index])).append("\n")
                }
                stringCount = strings.size
                out.append("count = ").append(stringCount).append("\n")
                if (strings.size > displayCount) {
                    out.append("# ... ").append(strings.size - displayCount).append(" strings omitted.\n")
                }
            }
            offset += size
        }
        if (stringCount == 0) out.append("\n# String pool bulunamadı veya boş.\n")
        return out.toString()
    }

    private fun decodeBinaryXml(bytes: ByteArray): String {
        val strings = StringPool.findFirst(bytes)
            ?: return "<!-- Material Files: binary XML string pool çözümlenemedi. -->"
        val out = StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        val namespaces = mutableMapOf<Int, String>()
        val parser = ChunkReader(bytes)
        var offset = 0
        var depth = 0
        val end = bytes.size
        while (offset + 8 <= end) {
            val type = parser.readU16(offset)
            val size = parser.readI32(offset + 4)
            if (size < 8 || offset + size > end) break
            when (type) {
                CHUNK_XML_START_NAMESPACE -> {
                    val prefix = parser.readI32(offset + 16)
                    val uri = parser.readI32(offset + 20)
                    if (prefix >= 0 && uri >= 0) namespaces[uri] = strings.getOrNull(prefix).orEmpty()
                }
                CHUNK_XML_START_ELEMENT -> {
                    val ns = parser.readI32(offset + 16)
                    val name = parser.readI32(offset + 20)
                    val attributeStart = parser.readU16(offset + 24)
                    val attributeSize = parser.readU16(offset + 26)
                    val attributeCount = parser.readU16(offset + 28)
                    indent(out, depth).append('<').append(qName(ns, strings, namespaces, name))
                    var attrOffset = offset + attributeStart
                    repeat(attributeCount) {
                        val attrNs = parser.readI32(attrOffset)
                        val attrName = parser.readI32(attrOffset + 4)
                        val rawValue = parser.readI32(attrOffset + 8)
                        val dataType = parser.readU8(attrOffset + 15)
                        val data = parser.readI32(attrOffset + 16)
                        out.append(' ').append(qName(attrNs, strings, namespaces, attrName)).append("=\"")
                        out.append(escapeXml(attributeValue(strings, rawValue, dataType, data))).append('"')
                        attrOffset += attributeSize
                    }
                    out.append(">\n")
                    depth++
                }
                CHUNK_XML_END_ELEMENT -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    val ns = parser.readI32(offset + 16)
                    val name = parser.readI32(offset + 20)
                    indent(out, depth).append("</").append(qName(ns, strings, namespaces, name)).append(">\n")
                }
                CHUNK_XML_TEXT -> {
                    val textIndex = parser.readI32(offset + 16)
                    val text = strings.getOrNull(textIndex).orEmpty()
                    if (text.isNotEmpty()) indent(out, depth).append(escapeXml(text)).append('\n')
                }
            }
            offset += size
        }
        return out.toString().trimEnd() + "\n"
    }

    private fun qName(nsIndex: Int, strings: List<String>, namespaces: Map<Int, String>, nameIndex: Int): String {
        val name = strings.getOrNull(nameIndex).orEmpty().ifEmpty { "unknown" }
        val uri = strings.getOrNull(nsIndex).orEmpty()
        val prefix = namespaces.entries.firstOrNull { it.key == nsIndex }?.value
            ?: if (uri == "http://schemas.android.com/apk/res/android") "android" else ""
        return if (prefix.isEmpty()) name else "$prefix:$name"
    }

    private fun attributeValue(strings: List<String>, rawIndex: Int, type: Int, data: Int): String = when (type) {
        0x03 -> strings.getOrNull(data).orEmpty()
        0x10 -> data.toString()
        0x11 -> "0x" + data.toUInt().toString(16)
        0x12 -> if (data != 0) "true" else "false"
        0x01 -> "@0x" + data.toUInt().toString(16)
        0x02 -> "?0x" + data.toUInt().toString(16)
        0x04 -> java.lang.Float.intBitsToFloat(data).toString()
        else -> strings.getOrNull(rawIndex).orEmpty().ifEmpty { "0x" + data.toUInt().toString(16) }
    }

    private fun indent(out: StringBuilder, depth: Int): StringBuilder {
        repeat(depth) { out.append("    ") }
        return out
    }

    private fun quote(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    private fun escapeXml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private const val MAX_ARSC_STRINGS = 50_000

    private fun readU16(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xffff

    private class ChunkReader(private val bytes: ByteArray) {
        fun readU8(offset: Int): Int = bytes[offset].toInt() and 0xff
        fun readU16(offset: Int): Int = BinaryTextDecoder.readU16(bytes, offset)
        fun readI32(offset: Int): Int = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private object StringPool {
        fun findFirst(bytes: ByteArray): List<String>? {
            var offset = 0
            while (offset + 8 <= bytes.size) {
                val type = readU16(bytes, offset)
                val size = readI32(bytes, offset + 4)
                if (size < 8 || offset + size > bytes.size) return null
                if (type == CHUNK_STRING_POOL) return parse(bytes, offset)
                if (type == CHUNK_XML) {
                    val headerSize = readU16(bytes, offset + 2)
                    offset += headerSize.coerceAtLeast(8)
                    continue
                }
                offset += size
            }
            return null
        }

        fun parse(bytes: ByteArray, offset: Int): List<String> {
            if (offset + 28 > bytes.size) return emptyList()
            val count = readI32(bytes, offset + 8)
            val flags = readI32(bytes, offset + 16)
            val stringsStart = readI32(bytes, offset + 20)
            if (count < 0 || count > 1_000_000) return emptyList()
            val utf8 = (flags and 0x100) != 0
            val offsetsBase = offset + 28
            val result = ArrayList<String>(count)
            for (i in 0 until count) {
                val stringOffset = readI32(bytes, offsetsBase + i * 4)
                val start = offset + stringsStart + stringOffset
                if (start < 0 || start >= bytes.size) {
                    result.add("")
                    continue
                }
                result.add(if (utf8) readUtf8(bytes, start) else readUtf16(bytes, start))
            }
            return result
        }

        private fun readUtf8(bytes: ByteArray, start: Int): String {
            var p = start
            val first = readLength8(bytes, p)
            p += first.second
            val second = readLength8(bytes, p)
            p += second.second
            val len = second.first
            if (len < 0 || p + len > bytes.size) return ""
            return String(bytes, p, len, StandardCharsets.UTF_8)
        }

        private fun readLength8(bytes: ByteArray, offset: Int): Pair<Int, Int> {
            val b = bytes[offset].toInt() and 0xff
            return if (b and 0x80 == 0) b to 1 else (((b and 0x7f) shl 8) or (bytes[offset + 1].toInt() and 0xff)) to 2
        }

        private fun readUtf16(bytes: ByteArray, start: Int): String {
            var p = start
            var len = readU16(bytes, p)
            p += 2
            if (len and 0x8000 != 0) {
                len = ((len and 0x7fff) shl 16) or readU16(bytes, p)
                p += 2
            }
            val byteCount = len * 2
            if (len < 0 || p + byteCount > bytes.size) return ""
            return String(bytes, p, byteCount, StandardCharsets.UTF_16LE)
        }

        private fun readU16(bytes: ByteArray, offset: Int): Int = BinaryTextDecoder.readU16(bytes, offset)
        private fun readI32(bytes: ByteArray, offset: Int): Int =
            ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }
}
