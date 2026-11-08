package com.endroid.htmlpad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetsTest {

    @Test
    fun cursorsStayInsideSnippetText() {
        Snippets.all.forEach {
            assertTrue("${it.label} cursor out of range", it.cursor in 0..it.text.length)
        }
    }

    @Test
    fun labelsAreUnique() {
        val labels = Snippets.all.map { it.label }
        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun tagSnippetsPutCursorBetweenOpenAndClose() {
        fun snippet(label: String) = Snippets.all.first { it.label == label }

        listOf("div", "p", "h1", "h2", "span", "li").forEach { tag ->
            val s = snippet(tag)
            assertEquals("<$tag>", s.text.substring(0, s.cursor))
            assertEquals("</$tag>", s.text.substring(s.cursor))
        }
    }

    @Test
    fun linkAndImageCursorLandsInsideFirstQuotes() {
        val a = Snippets.all.first { it.label == "a" }
        assertEquals("<a href=\"", a.text.substring(0, a.cursor))
        assertTrue(a.text.substring(a.cursor).startsWith("\">"))

        val img = Snippets.all.first { it.label == "img" }
        assertEquals("<img src=\"", img.text.substring(0, img.cursor))
        assertTrue(img.text.substring(img.cursor).startsWith("\" alt="))
    }

    @Test
    fun listSnippetCursorIsInsideItem() {
        val ul = Snippets.all.first { it.label == "ul" }
        assertEquals("<ul>\n  <li>", ul.text.substring(0, ul.cursor))
        assertTrue(ul.text.substring(ul.cursor).startsWith("</li>"))
    }

    @Test
    fun plainSymbolsKeepCursorAtEnd() {
        Snippets.all.filter { it.label in setOf("<", ">", "</", "=", "br") }.forEach {
            assertEquals(it.text.length, it.cursor)
        }
    }
}
