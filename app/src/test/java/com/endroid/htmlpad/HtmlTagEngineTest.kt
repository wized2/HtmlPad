package com.endroid.htmlpad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlTagEngineTest {

    @Test
    fun autoClosesH2() {
        val before = "Hello <h2"
        val (text, cursor) = HtmlTagEngine.afterInsert(before, before.length, ">")
        assertEquals("Hello <h2></h2>", text)
        assertEquals("Hello <h2>".length, cursor)
    }

    @Test
    fun autoClosesDivWithAttrs() {
        val before = "<div class=\"x\""
        val (text, cursor) = HtmlTagEngine.afterInsert(before, before.length, ">")
        assertEquals("<div class=\"x\"></div>", text)
        assertEquals("<div class=\"x\">".length, cursor)
    }

    @Test
    fun doesNotCloseBr() {
        val before = "line<br"
        val (text, _) = HtmlTagEngine.afterInsert(before, before.length, ">")
        assertEquals("line<br>", text)
    }

    @Test
    fun doesNotCloseImg() {
        val before = "<img src=\"a.png\""
        val (text, _) = HtmlTagEngine.afterInsert(before, before.length, ">")
        assertEquals("<img src=\"a.png\">", text)
    }

    @Test
    fun ignoresClosingTag() {
        val before = "</h2"
        val (text, _) = HtmlTagEngine.afterInsert(before, before.length, ">")
        assertEquals("</h2>", text)
    }

    @Test
    fun nonGtInsertIsPlain() {
        val before = "ab"
        val (text, cursor) = HtmlTagEngine.afterInsert(before, 1, "X")
        assertEquals("aXb", text)
        assertEquals(2, cursor)
    }

    @Test
    fun doesNotCloseSelfClosingTag() {
        val before = "<div/"
        val (text, cursor) = HtmlTagEngine.afterInsert(before, before.length, ">")
        assertEquals("<div/>", text)
        assertEquals(before.length + 1, cursor)
    }

    @Test
    fun doesNotCloseWhenAttributeQuoteIsOpen() {
        val before = "<a title=\"x"
        val (text, _) = HtmlTagEngine.afterInsert(before, before.length, ">")
        assertEquals("<a title=\"x>", text)
    }

    @Test
    fun closesAfterQuotedAttributeContainingGreaterThan() {
        val before = "<a title=\"x>y\""
        val (text, _) = HtmlTagEngine.afterInsert(before, before.length, ">")
        assertEquals("<a title=\"x>y\"></a>", text)
    }

    @Test
    fun ignoresNonTagLessThan() {
        val before = "1 <3"
        val (text, _) = HtmlTagEngine.afterInsert(before, before.length, ">")
        assertEquals("1 <3>", text)
    }

    @Test
    fun ignoresTagThatWasAlreadyClosed() {
        val before = "<p>hello"
        val (text, _) = HtmlTagEngine.afterInsert(before, before.length, ">")
        assertEquals("<p>hello>", text)
    }

    @Test
    fun ignoresCommentAndDoctype() {
        val comment = "<!-- note --"
        assertEquals(
            "<!-- note -->",
            HtmlTagEngine.afterInsert(comment, comment.length, ">").first,
        )
        val doctype = "<!DOCTYPE html"
        assertEquals(
            "<!DOCTYPE html>",
            HtmlTagEngine.afterInsert(doctype, doctype.length, ">").first,
        )
    }

    @Test
    fun doesNotDoubleCloseWhenClosingTagFollows() {
        val before = "<h2"
        val after = "</h2>"
        val full = before + after
        val (text, cursor) = HtmlTagEngine.afterInsert(full, before.length, ">")
        assertEquals("<h2>$after", text)
        assertEquals("<h2>".length, cursor)
    }

    @Test
    fun closesInTheMiddleOfText() {
        val full = "<section></section>\n<ul"
        val (text, cursor) = HtmlTagEngine.afterInsert(full, full.length, ">")
        assertEquals("<section></section>\n<ul></ul>", text)
        assertEquals(full.length + 1, cursor)
    }

    @Test
    fun closesCustomElement() {
        val before = "<my-widget"
        val (text, _) = HtmlTagEngine.afterInsert(before, before.length, ">")
        assertEquals("<my-widget></my-widget>", text)
    }

    @Test
    fun voidHelpers() {
        assertTrue(HtmlTagEngine.isVoid("br"))
        assertTrue(HtmlTagEngine.isVoid("IMG"))
        assertFalse(HtmlTagEngine.isVoid("div"))
    }
}
