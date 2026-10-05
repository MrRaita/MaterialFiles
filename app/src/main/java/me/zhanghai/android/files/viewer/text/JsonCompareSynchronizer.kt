package me.zhanghai.android.files.viewer.text

/**
 * Synchronizes JSON string-resource files (e.g. "en-US.json" / "tr-TR.json" style localization
 * files), including nested sections (`{"common": {"save": "Save", ...}, ...}`), without ever
 * touching an existing value: only keys genuinely missing from the target are inserted, copied
 * verbatim from the reference. If a whole nested section is missing from the target, it is
 * inserted as one block and every leaf key inside it is still counted individually (matching how
 * the XML comparator counts missing string entries, not missing top-level groups).
 */
object JsonCompareSynchronizer {

    private class Node(val isObject: Boolean, val entries: List<Entry>?, val start: Int, val end: Int)
    private class Entry(val key: String, val value: Node, val keyStart: Int)
    private class Edit(val pos: Int, val insertText: String)

    private class ParseException(message: String) : Exception(message)

    private class Parser(private val text: String) {
        private fun skipWs(pos: Int): Int {
            var p = pos
            while (p < text.length && text[p].isWhitespace()) p++
            return p
        }

        private fun skipString(pos: Int): Int {
            var p = pos + 1
            while (p < text.length && text[p] != '"') {
                if (text[p] == '\\') p++
                p++
            }
            if (p >= text.length) throw ParseException("Unterminated string")
            return p + 1
        }

        private fun skipBalanced(pos: Int, open: Char, close: Char): Int {
            var p = pos + 1
            var depth = 1
            while (p < text.length && depth > 0) {
                val c = text[p]
                if (c == '"') {
                    p = skipString(p)
                    continue
                }
                if (c == open) depth++ else if (c == close) depth--
                p++
            }
            if (depth != 0) throw ParseException("Unbalanced '$open$close'")
            return p
        }

        private fun skipPrimitive(pos: Int): Int {
            var p = pos
            while (p < text.length) {
                val c = text[p]
                if (c == ',' || c == '}' || c == ']' || c.isWhitespace()) break
                p++
            }
            if (p == pos) throw ParseException("Expected a value at $pos")
            return p
        }

        fun parseValue(startPos: Int): Node {
            val pos = skipWs(startPos)
            if (pos >= text.length) throw ParseException("Unexpected end of input")
            return when (text[pos]) {
                '{' -> parseObject(pos)
                '"' -> Node(false, null, pos, skipString(pos))
                '[' -> Node(false, null, pos, skipBalanced(pos, '[', ']'))
                else -> Node(false, null, pos, skipPrimitive(pos))
            }
        }

        private fun parseObject(openPos: Int): Node {
            val entries = ArrayList<Entry>()
            var p = skipWs(openPos + 1)
            if (p >= text.length) throw ParseException("Unterminated object")
            if (text[p] == '}') return Node(true, entries, openPos, p + 1)
            while (true) {
                p = skipWs(p)
                if (p >= text.length || text[p] != '"') throw ParseException("Expected a key at $p")
                val keyStart = p
                val keyEnd = skipString(p)
                val key = text.substring(keyStart + 1, keyEnd - 1)
                p = skipWs(keyEnd)
                if (p >= text.length || text[p] != ':') throw ParseException("Expected ':' at $p")
                p = skipWs(p + 1)
                val value = parseValue(p)
                p = value.end
                entries += Entry(key, value, keyStart)
                p = skipWs(p)
                if (p < text.length && text[p] == ',') {
                    p = skipWs(p + 1)
                    if (p < text.length && text[p] == '}') return Node(true, entries, openPos, p + 1)
                } else if (p < text.length && text[p] == '}') {
                    return Node(true, entries, openPos, p + 1)
                } else {
                    throw ParseException("Expected ',' or '}' at $p")
                }
            }
        }
    }

    private class MergeState {
        val missingKeys = ArrayList<String>()
        var reordered = false
    }

    fun synchronize(reference: String, target: String): TextCompareSynchronizer.Result? {
        val refRoot: Node
        val tgtRoot: Node
        try {
            refRoot = Parser(reference).parseValue(0)
            tgtRoot = Parser(target).parseValue(0)
        } catch (e: Exception) {
            return null
        }
        if (!refRoot.isObject || !tgtRoot.isObject) return null

        val state = MergeState()
        val edits = collectEdits(reference, refRoot, target, tgtRoot, "", state)

        if (state.missingKeys.isEmpty() && !state.reordered) {
            return TextCompareSynchronizer.Result(target, emptyList(), emptyList(), false, false)
        }

        val sortedEdits = edits.sortedByDescending { it.pos }
        val result = StringBuilder(target)
        for (edit in sortedEdits) {
            result.insert(edit.pos, edit.insertText)
        }
        val rebuilt = result.toString()
        return TextCompareSynchronizer.Result(
            rebuilt, state.missingKeys, emptyList(), state.reordered, rebuilt != target
        )
    }

    private fun collectLeafPaths(node: Node, path: String, state: MergeState) {
        if (node.isObject) {
            if (node.entries!!.isEmpty()) {
                state.missingKeys += path
            } else {
                for (entry in node.entries) collectLeafPaths(entry.value, "$path.${entry.key}", state)
            }
        } else {
            state.missingKeys += path
        }
    }

    private fun collectEdits(
        refText: String, ref: Node, tgtText: String, tgt: Node, path: String, state: MergeState
    ): List<Edit> {
        val edits = ArrayList<Edit>()
        val tgtByKey = LinkedHashMap<String, Entry>()
        for (entry in tgt.entries!!) {
            // A duplicate key means the target isn't well-formed per JSON's own rules already;
            // don't guess, just stop touching this file.
            if (tgtByKey.put(entry.key, entry) != null) throw ParseException("Duplicate key")
        }

        val toInsert = ArrayList<Entry>()
        for (refEntry in ref.entries!!) {
            val fullPath = if (path.isEmpty()) refEntry.key else "$path.${refEntry.key}"
            val tgtEntry = tgtByKey[refEntry.key]
            if (tgtEntry == null) {
                collectLeafPaths(refEntry.value, fullPath, state)
                toInsert += refEntry
            } else if (refEntry.value.isObject && tgtEntry.value.isObject) {
                edits += collectEdits(refText, refEntry.value, tgtText, tgtEntry.value, fullPath, state)
            }
            // Otherwise present in both (as leaf, or as mismatched shape) -> leave target as-is.
        }

        val refKeysPresentInTarget = ref.entries.map { it.key }.filter { it in tgtByKey }
        val targetOrderOfCommonKeys = tgt.entries.map { it.key }.filter { it in refKeysPresentInTarget.toSet() }
        if (targetOrderOfCommonKeys != refKeysPresentInTarget) state.reordered = true

        if (toInsert.isNotEmpty()) {
            val targetHasEntries = tgt.entries.isNotEmpty()
            val indent = if (targetHasEntries) {
                val keyStart = tgt.entries[0].keyStart
                val lineStart = tgtText.lastIndexOf('\n', keyStart).let { if (it < 0) 0 else it + 1 }
                tgtText.substring(lineStart, keyStart).takeWhile { it == ' ' || it == '\t' }
            } else {
                val lineStart = tgtText.lastIndexOf('\n', tgt.start).let { if (it < 0) 0 else it + 1 }
                tgtText.substring(lineStart, tgt.start).takeWhile { it == ' ' || it == '\t' } + "  "
            }
            val block = buildString {
                for (entry in toInsert) {
                    append(',').append('\n').append(indent)
                    append('"').append(entry.key).append("\": ")
                    append(refText.substring(entry.value.start, entry.value.end))
                }
            }
            val blockText = if (targetHasEntries) block else block.removePrefix(",")
            val insertPos = if (targetHasEntries) tgt.entries.last().value.end else tgt.start + 1
            edits += Edit(insertPos, blockText)
        }
        return edits
    }
}
