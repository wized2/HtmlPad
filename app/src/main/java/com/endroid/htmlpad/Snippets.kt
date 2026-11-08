package com.endroid.htmlpad

/**
 * A button on the symbol bar. [cursor] is where the caret ends up,
 * counted from the start of [text] (so tags land between open and close).
 */
data class Snippet(
    val label: String,
    val text: String,
    val cursor: Int = text.length,
)

object Snippets {
    val all: List<Snippet> = listOf(
        Snippet("!", "!"),
        Snippet("<", "<"),
        Snippet(">", ">"),
        Snippet("</", "</"),
        Snippet("/>", " />"),
        Snippet("\"", "\""),
        Snippet("/", "/"),
        Snippet("=", "="),
        Snippet("&", "&"),
        Snippet("div", "<div></div>", cursor = "<div>".length),
        Snippet("p", "<p></p>", cursor = "<p>".length),
        Snippet("h1", "<h1></h1>", cursor = "<h1>".length),
        Snippet("h2", "<h2></h2>", cursor = "<h2>".length),
        Snippet("span", "<span></span>", cursor = "<span>".length),
        Snippet("ul", "<ul>\n  <li></li>\n</ul>", cursor = "<ul>\n  <li>".length),
        Snippet("li", "<li></li>", cursor = "<li>".length),
        Snippet("br", "<br>"),
        Snippet("a", "<a href=\"\"></a>", cursor = "<a href=\"".length),
        Snippet("img", "<img src=\"\" alt=\"\">", cursor = "<img src=\"".length),
    )
}
