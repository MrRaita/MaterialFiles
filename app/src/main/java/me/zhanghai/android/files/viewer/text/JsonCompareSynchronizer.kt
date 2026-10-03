package me.zhanghai.android.files.viewer.text

import java.util.regex.Pattern

/**
 * Synchronizes flat JSON string-resource files (e.g. "en-US.json" style localization files:
 * a single top-level object mapping keys directly to string/number/boolean/null values)
 * without touching existing translated values. Nested objects/arrays as values are not
 * supported — if either file contains one, this bails out (returns null) rather than risking
 * corrupting it, exactly like TextCompareSynchronizer does for malformed/unrecognized XML.
 */
object JsonCompareSynchronizer {
    private data class Entry(val key: String, val value: String, val start: Int, val end: Int)

    // "key": <value>, where <value> is a JSON string, number, boolean, or null literal.
    // Object/array values ({...} or [...]) deliberately do not match, so a file that uses them
    // is detected as unsupported (parse() below requires every top-level member to match).
    private val entryPattern = Pattern.compile(
        "\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|true|false|null)"
    )
    // How many top-level JSON members exist in total (matched or not), used to confirm the
    // whole file is flat key -> primitive, not just that it contains SOME matching entries.
    private val memberSeparatorPattern = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"\\s*:")

    fun synchronize(reference: String, target: String): TextCompareSynchronizer.Result? {
        val refBody = objectBody(reference) ?: return null
        val tgtBody = objectBody(target) ?: return null

        val ref = parse(reference, refBody.first, refBody.second) ?: return null
        val tgt = parse(target, tgtBody.first, tgtBody.second) ?: return null
        if (ref.isEmpty() || tgt.isEmpty()) return null

        val refKeys = ref.map { it.key }
        if (refKeys.size != refKeys.toSet().size) return null

        val targetByKey = LinkedHashMap<String, Entry>()
        for (entry in tgt) {
            // A duplicate key in the target is invalid JSON already; don't guess which one to
            // keep, just refuse to touch the file.
            if (targetByKey.put(entry.key, entry) != null) return null
        }

        val missing = ref.filter { it.key !in targetByKey }.map { it.key }
        val commonTargetOrder = tgt.filter { it.key in refKeys }.map { it.key }
        val expectedCommonOrder = ref.filter { it.key in targetByKey }.map { it.key }
        val reordered = commonTargetOrder != expectedCommonOrder

        if (missing.isEmpty() && !reordered) {
            return TextCompareSynchronizer.Result(target, emptyList(), emptyList(), false, false)
        }

        // Detect the target's own indent and newline style from its first entry, so the
        // rebuilt object still looks like the rest of the file.
        val firstEntryLineStart =
            target.lastIndexOf('\n', tgt.first().start - 1).let { if (it < 0) 0 else it + 1 }
        val indent = target.substring(firstEntryLineStart, tgt.first().start)
            .takeWhile { it == ' ' || it == '\t' }
        val usesCrlf = target.substring(tgtBody.first, tgtBody.second).contains("\r\n")
        val newline = if (usesCrlf) "\r\n" else "\n"

        val rebuiltBody = buildString {
            append(newline)
            ref.forEachIndexed { index, referenceEntry ->
                val value = targetByKey[referenceEntry.key]?.value ?: referenceEntry.value
                append(indent)
                append('"').append(escapeJsonKey(referenceEntry.key)).append("\": ").append(value)
                if (index != ref.lastIndex) append(',')
                append(newline)
            }
        }
        val rebuilt = target.substring(0, tgtBody.first) + rebuiltBody +
            target.substring(tgtBody.second)

        return TextCompareSynchronizer.Result(rebuilt, missing, emptyList(), reordered, rebuilt != target)
    }

    /** Returns the [start, end) character range strictly between the outermost '{' and '}'. */
    private fun objectBody(text: String): Pair<Int, Int>? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end < 0 || end <= start) return null
        return (start + 1) to end
    }

    /** Parses top-level "key": value members; returns null if any member isn't a flat literal. */
    private fun parse(text: String, bodyStart: Int, bodyEnd: Int): List<Entry>? {
        val body = text.substring(bodyStart, bodyEnd)
        var totalMembers = 0
        val separatorMatcher = memberSeparatorPattern.matcher(body)
        while (separatorMatcher.find()) totalMembers++
        val matcher = entryPattern.matcher(body)
        val result = ArrayList<Entry>()
        while (matcher.find()) {
            result += Entry(
                unescapeJsonKey(matcher.group(1)),
                matcher.group(2),
                bodyStart + matcher.start(),
                bodyStart + matcher.end()
            )
        }
        // If some top-level members didn't match the flat-literal pattern (e.g. a nested object
        // or array value), refuse to synchronize rather than silently dropping them.
        if (result.size != totalMembers) return null
        return result
    }

    private fun unescapeJsonKey(raw: String): String =
        raw.replace("\\\"", "\"").replace("\\\\", "\\")

    private fun escapeJsonKey(key: String): String =
        key.replace("\\", "\\\\").replace("\"", "\\\"")
}
