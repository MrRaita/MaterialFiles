package me.zhanghai.android.files.viewer.text

import java.util.regex.Pattern

/**
 * Synchronizes Android string resources without touching existing translated values.
 *
 * Resource identity is (resource type, name), not just the name. The synchronizer also
 * repairs the common one-line key-shift that happens when a translated string is inserted
 * or removed manually and the following key is accidentally duplicated.
 */
object TextCompareSynchronizer {
    data class Result(
        val text: String,
        val missingKeys: List<String>,
        val repairedKeys: List<String>,
        val reordered: Boolean,
        val changed: Boolean
    )

    private data class Entry(
        val type: String,
        val key: String,
        val raw: String,
        val start: Int,
        val end: Int,
        val indent: String = ""
    ) {
        val id: String get() = "$type:$key"
    }

    private val resourcePattern = Pattern.compile(
        "(?s)<(string-array|plurals|string)\\b[^>]*\\bname=\\\"([^\\\"]+)\\\"[^>]*>.*?</\\1\\s*>|" +
            "<(string-array|plurals|string)\\b[^>]*\\bname=\\\"([^\\\"]+)\\\"[^>]*/>"
    )

    fun synchronize(reference: String, target: String): Result? {
        if (!reference.contains("<resources") || !target.contains("<resources")) return null

        val ref = parse(reference)
        val tgt = parse(target)
        if (ref.isEmpty() || tgt.isEmpty()) return null

        val refIds = ref.map { it.id }
        if (refIds.size != refIds.toSet().size) return null

        val repaired = repairAdjacentKeyShifts(ref, tgt)
        val repairedTarget = repaired.first
        val repairedKeys = repaired.second

        val targetById = LinkedHashMap<String, Entry>()
        for (entry in repairedTarget) {
            if (targetById.put(entry.id, entry) != null) {
                // Do not silently throw away a translation when the duplicate cannot be
                // explained by the safe adjacent-shift repair above.
                return null
            }
        }

        val missing = ref.filter { it.id !in targetById }.map { it.id }
        val commonTargetOrder = repairedTarget.filter { it.id in refIds }.map { it.id }
        val expectedCommonOrder = ref.filter { it.id in targetById }.map { it.id }
        val reordered = commonTargetOrder != expectedCommonOrder

        if (missing.isEmpty() && !reordered && repairedKeys.isEmpty()) {
            return Result(target, emptyList(), emptyList(), false, false)
        }

        val firstStart = tgt.minOf { it.start }
        val lastEnd = tgt.maxOf { it.end }
        val prefix = target.substring(0, firstStart).replace(Regex("[ \t]+$"), "")
        val suffix = target.substring(lastEnd)
        val firstTarget = tgt.minBy { it.start }
        val targetIndent = firstTarget.indent
        val targetSeparators = tgt.zipWithNext().map { (a, b) -> "\n".repeat(target.substring(a.end, b.start).count { it == '\n' }.coerceAtLeast(1)) }
        val defaultSeparator = targetSeparators.firstOrNull() ?: "\n"

        // Rebuild only the resource body. Existing translations are reused verbatim, but their
        // original indentation and the target file's overall blank-line density are preserved.
        // This avoids the old join("\n\n") behaviour that inserted a blank line after nearly
        // every resource and stripped the four-space indentation.
        val outputEntries = ref.map { referenceEntry ->
            val entry = targetById[referenceEntry.id] ?: referenceEntry
            val raw = entry.raw.trimStart(' ', '\t')
            targetIndent + raw
        }
        val rebuiltBody = buildString {
            outputEntries.forEachIndexed { index, entry ->
                if (index > 0) append(targetSeparators.getOrElse(index - 1) { defaultSeparator })
                append(entry)
            }
        }
        val rebuilt = buildString {
            append(prefix)
            append(rebuiltBody)
            append(suffix)
        }

        return Result(
            rebuilt,
            missing,
            repairedKeys,
            reordered,
            rebuilt != target
        )
    }

    private fun parse(text: String): List<Entry> {
        val matcher = resourcePattern.matcher(text)
        val result = ArrayList<Entry>()
        while (matcher.find()) {
            val type = matcher.group(1) ?: matcher.group(3) ?: continue
            val key = matcher.group(2) ?: matcher.group(4) ?: continue
            val lineStart = text.lastIndexOf('\n', matcher.start() - 1).let { if (it < 0) 0 else it + 1 }
            val indent = text.substring(lineStart, matcher.start()).takeWhile { it == ' ' || it == '\t' }
            result += Entry(type, key, matcher.group(), matcher.start(), matcher.end(), indent)
        }
        return result
    }

    /**
     * Detects the exact corruption pattern seen in translations:
     *
     * reference: A, B
     * target:    B, B
     *
     * The first B is the translated value that belonged to A. We rename only that first
     * block, leaving the real B block untouched. This is deliberately conservative.
     */
    private fun repairAdjacentKeyShifts(
        reference: List<Entry>,
        target: List<Entry>
    ): Pair<List<Entry>, List<String>> {
        val result = target.toMutableList()
        val repaired = mutableListOf<String>()
        var changed = true

        // Work from the resource identities rather than absolute line positions. That matters
        // when the reference gained other entries since the translation was last synchronized.
        while (changed) {
            changed = false
            val positionsById = result.withIndex().groupBy({ it.value.id }, { it.index })
            val targetIds = positionsById.keys

            for (i in 0 until reference.lastIndex) {
                val missing = reference[i]
                val following = reference[i + 1]
                if (missing.id in targetIds) continue

                val positions = positionsById[following.id].orEmpty()
                if (positions.size != 2) continue

                val first = positions[0]
                val second = positions[1]
                if (second != first + 1) continue

                val candidate = result[first]
                if (candidate.type != missing.type) continue

                result[first] = candidate.copy(
                    key = missing.key,
                    raw = replaceName(candidate.raw, missing.key)
                )
                repaired += missing.id
                changed = true
                break
            }
        }
        return result to repaired
    }

    private fun replaceName(raw: String, newName: String): String =
        raw.replaceFirst(
            Regex("(\\bname\\s*=\\s*\\\")[^\\\"]+(\\\")"),
            "$1${newName.replace("\\", "\\\\").replace("$", "\\$")}$2"
        )
}
