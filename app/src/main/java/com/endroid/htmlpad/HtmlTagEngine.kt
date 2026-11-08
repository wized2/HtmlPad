package com.endroid.htmlpad

/**
 * Auto-close HTML tags while typing. Pure logic for unit tests.
 */
object HtmlTagEngine {

    /** Tags that must not auto-close (void / self-closing in HTML). */
    private val voidTags = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr",
    )

    /**
     * After the user inserts [inserted] at [cursor] into [before],
     * return the full new text and the preferred new cursor index.
     *
     * When the user types `>` that completes an opening tag like `<h2>`,
     * we insert the matching closing tag and place the cursor between them.
     */
    fun afterInsert(before: String, cursor: Int, inserted: String): Pair<String, Int> {
        if (inserted != ">" || cursor < 1) {
            val next = before.substring(0, cursor) + inserted + before.substring(cursor)
            return next to (cursor + inserted.length)
        }
        val left = before.substring(0, cursor)
        val right = before.substring(cursor)
        // Find last '<' before cursor
        val open = left.lastIndexOf('<')
        if (open < 0) {
            val next = left + ">" + right
            return next to (cursor + 1)
        }
        val inside = left.substring(open + 1)
        // Ignore closing tags, comments, doctype
        if (inside.startsWith("/") || inside.startsWith("!") || inside.startsWith("?")) {
            val next = left + ">" + right
            return next to (cursor + 1)
        }
        // Already has '>' unfinished? take tag name token
        val name = inside.trim().split(Regex("\\s+|/")).firstOrNull().orEmpty()
        if (name.isEmpty() || !name.all { it.isLetterOrDigit() || it == '-' }) {
            val next = left + ">" + right
            return next to (cursor + 1)
        }
        val lower = name.lowercase()
        if (lower in voidTags) {
            val next = left + ">" + right
            return next to (cursor + 1)
        }
        // Don't double-close if closing tag already immediately follows
        val closing = "</$name>"
        if (right.startsWith(closing)) {
            val next = left + ">" + right
            return next to (cursor + 1)
        }
        val next = left + ">" + closing + right
        val newCursor = cursor + 1 // after '>'
        return next to newCursor
    }

    fun isVoid(tag: String): Boolean = tag.lowercase() in voidTags
}
