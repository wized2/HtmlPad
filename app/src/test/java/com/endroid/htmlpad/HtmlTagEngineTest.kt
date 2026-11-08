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
    fun voidHelpers() {
        assertTrue(HtmlTagEngine.isVoid("br"))
        assertTrue(HtmlTagEngine.isVoid("IMG"))
        assertFalse(HtmlTagEngine.isVoid("div"))
    }
}
