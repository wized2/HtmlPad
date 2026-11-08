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
     *
     * No closing tag is added when the `>` ends a closing tag, comment,
     * doctype, void tag, self-closing tag (`<div/>`), a tag whose attribute
     * quote is still open, or when `<` is not followed by a tag name.
     */
    fun afterInsert(before: String, cursor: Int, inserted: String): Pair<String, Int> {
        val pos = cursor.coerceIn(0, before.length)
        val left = before.substring(0, pos)
        val right = before.substring(pos)
        val plain = (left + inserted + right) to (pos + inserted.length)

        if (inserted != ">") return plain

        val open = left.lastIndexOf('<')
        if (open < 0) return plain
        val inside = left.substring(open + 1)

        if (inside.isEmpty()) return plain
        // Closing tags, comments, doctype, processing instructions
        if (inside[0] == '/' || inside[0] == '!' || inside[0] == '?') return plain
        // A tag name must start with a letter (so `<3>` or `a <- b >` are ignored)
        if (!inside[0].isLetter()) return plain
        // The last '<' belongs to a tag that was already closed (an unquoted '>')
        if (hasUnquotedGreaterThan(inside)) return plain
        // Typing '>' inside an attribute value, e.g. <a title="a>b"
        if (hasOpenQuote(inside)) return plain
        // Self-closing syntax, e.g. <div/> or <br />
        if (inside.trimEnd().endsWith("/")) return plain

        val name = inside.takeWhile { it.isLetterOrDigit() || it == '-' }
        if (name.isEmpty()) return plain
        // The name must be followed by whitespace (attributes) or nothing
        if (name.length < inside.length && !inside[name.length].isWhitespace()) return plain

        if (isVoid(name)) return plain

        // Don't double-close if the matching closing tag already follows
        val closing = "</$name>"
        if (right.startsWith(closing)) return plain

        return (left + ">" + closing + right) to (pos + 1)
    }

    private fun hasUnquotedGreaterThan(text: String): Boolean {
        var quote: Char? = null
        for (c in text) {
            if (quote == null) {
                if (c == '"' || c == '\'') quote = c else if (c == '>') return true
            } else if (c == quote) {
                quote = null
            }
        }
        return false
    }

    private fun hasOpenQuote(text: String): Boolean {
        var quote: Char? = null
        for (c in text) {
            if (quote == null) {
                if (c == '"' || c == '\'') quote = c
            } else if (c == quote) {
                quote = null
            }
        }
        return quote != null
    }

    fun isVoid(tag: String): Boolean = tag.lowercase() in voidTags
}
