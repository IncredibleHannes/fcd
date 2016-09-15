package fcd
package test

import markdown._

import org.scalatest._
import scala.language.higherKinds
import language.implicitConversions

class MarkdownParserTests extends FunSpec with Matchers with CustomMatchers {

  def _parsers: MarkdownParsers.type = MarkdownParsers
  override lazy val parsers = _parsers

  import parsers._

  implicit class MoreParserTestMethods[T, P <% Parser[T]](p: => P) {
    def shouldParseWith[ES <% Iterable[Elem]](s: ES, result: T) =
      it (s"""should parse "$s" with correct result""") {
        parse(p, s) shouldBe List(result)
      }
  }
  // ###########################################################################
  // ######################## ATX Heading Tetsts ###############################
  // ###########################################################################
  describe ("Simple headings:") {
    atxHeading shouldParseWith (
      "# foo\n",
      (1, List('f','o','o'))
    )
    atxHeading shouldParseWith (
      "## foo\n",
      (2, List('f','o','o'))
    )
    atxHeading shouldParseWith (
      "### foo\n",
      (3, List('f','o','o'))
    )
    atxHeading shouldParseWith (
      "#### foo\n",
      (4, List('f','o','o'))
    )
    atxHeading shouldParseWith (
      "##### foo\n",
      (5, List('f','o','o'))
    )
    atxHeading shouldParseWith (
      "###### foo\n",
      (6, List('f','o','o'))
    )
  }
  describe ("More than six # characters is not a heading:") {
    atxHeading shouldNotParse ("####### foo\n")
  }
  describe ("At least one space is required between the # characters and the heading’s contents, unless the heading is empty:") {
    atxHeading shouldNotParse ("#5 bolt\n")
    atxHeading shouldNotParse ("#hashtag\n")
  }
  describe ("This is not a heading, because the first # is escaped:") {
    atxHeading shouldNotParse ("\\## foo\n")
  }
  describe ("Contents are parsed as inlines:") {
    atxHeading shouldParseWith (
      "# foo *bar* \\*baz\\*\n",
      (1, List('f','o','o', ' ', '*', 'b', 'a', 'r','*', ' ' , '\\', '*', 'b', 'a', 'z','\\', '*' ))
    )
  }
  describe ("Leading and trailing blanks are ignored in parsing inline content:") {
    atxHeading shouldParseWith (
      "#                  foo                     \n",
      (1, List('f','o','o'))
    )
  }
  describe ("One to three spaces indentation are allowed:") {
    atxHeading shouldParseWith (
      " ### foo\n",
      (3, List('f','o','o'))
    )
    atxHeading shouldParseWith (
      "  ## foo\n",
      (2, List('f','o','o'))
    )
    atxHeading shouldParseWith (
      "   # foo\n",
      (1, List('f','o','o'))
    )
  }
  describe ("Four spaces are too much:") {
    atxHeading shouldNotParse ("    # foo\n")
  }
  describe ("A closing sequence of # characters is optional:") {
    atxHeading shouldParseWith (
      "## foo ##\n",
      (2, List('f','o','o'))
    )
    atxHeading shouldParseWith (
      "  ###   bar    ###\n",
      (3, List('b','a','r'))
    )
  }
  describe ("It need not be the same length as the opening sequence:") {
    atxHeading shouldParseWith (
      "# foo ##################################\n",
      (1, List('f','o','o'))
    )
    atxHeading shouldParseWith (
      "##### foo ##\n",
      (5, List('f','o','o'))
    )
  }
  describe ("Spaces are allowed after the closing sequence:") {
    atxHeading shouldParseWith (
      "### foo ###     \n",
      (3, List('f','o','o'))
    )
  }
  // Abweichend von der CommonMarkSpec:
  describe ("Characters are not allowed in the closing sequence:") {
    atxHeading shouldNotParse ("### foo ### b\n")
  }
  describe ("The closing sequence must be preceded by a space:") {
    atxHeading shouldNotParse ("# foo#\n")
  }
  describe ("ATX headings can be empty:") {
    atxHeading shouldParseWith (
      "## \n",
      (2, List())
    )
    atxHeading shouldParseWith (
      "# \n",
      (1, List())
    )
  }
  // ###########################################################################
  // #################### Indented Code Block Tests ############################
  // ###########################################################################
  describe ("a simple Code Block parser") {
    indentedCodeBlock shouldParseWith  (
      "    a simple\n      indented code block\n",
      "a simple\n  indented code block\n".toList
    )
  }
  describe ("The contents of a code block are literal text, and do not get parsed as Markdown:") {
    indentedCodeBlock shouldParseWith  (
      "    <a/>\n    *hi*\n\n    - one \n",
      "<a/>\n*hi*\n\n- one \n".toList
    )
  }
  describe ("Here we have three chunks separated by blank lines:") {
    indentedCodeBlock shouldParseWith  (
      "    chunk1\n\n    chunk2\n  \n \n \n    chunk3 \n",
      "chunk1\n\nchunk2\n\n\n\nchunk3 \n".toList
    )
  }
  describe ("Any initial spaces beyond four will be included in the content, even in interior blank lines:") {
    indentedCodeBlock shouldParseWith  (
      "    chunk1\n      \n      chunk2\n",
      "chunk1\n  \n  chunk2\n".toList
    )
  }
  describe ("Trailing spaces are included in the code block’s content:") {
    indentedCodeBlock shouldParseWith  (
      "    foo  \n",
      "foo  \n".toList
    )
  }
  describe ("The first line can be indented more than four spaces:") {
    indentedCodeBlock shouldParseWith  (
      "        foo\n    bar\n",
      "    foo\nbar\n".toList
    )
  }
  // ###########################################################################
  // ##################### Fenced Code Block Tests #############################
  // ###########################################################################
  describe ("Here is a simple example with backticks:") {
    fencedCodeBlock shouldParseWith  (
      "```\n<\n >\n```\n",
      "<\n >\n".toList
    )
  }
  describe ("Here is a simple example with tildes:") {
    fencedCodeBlock shouldParseWith  (
      "~~~\n<\n >\n~~~\n",
      "<\n >\n".toList
    )
  }
  describe ("The closing code fence must use the same character as the opening fence:") {
    fencedCodeBlock shouldParseWith  (
      "```\naaa\n~~~\n```\n",
      "aaa\n~~~\n".toList
    )
    fencedCodeBlock shouldParseWith  (
      "~~~\naaa\n```\n~~~\n",
      "aaa\n```\n".toList
    )
  }
  describe ("The closing code fence must be at least as long as the opening fence:") {
    fencedCodeBlock shouldParseWith  (
      "````\naaa\n```\n``````\n",
      "aaa\n```\n".toList
    )
    fencedCodeBlock shouldParseWith  (
      "~~~~\naaa\n~~~\n~~~~\n",
      "aaa\n~~~\n".toList
    )
  }
  describe ("Unclosed code blocks are closed by the end of the document:") {
    fencedCodeBlock shouldParseWith  (
      "```\n",
      "".toList
    )
    fencedCodeBlock shouldParseWith  (
      "`````\n\n```\naaa\n",
      "\n```\naaa\n".toList
    )
  }
  describe ("A code block can have all empty lines as its content:") {
    fencedCodeBlock shouldParseWith  (
      "```\n\n  \n```\n",
      "\n  \n".toList
    )
  }
  describe ("Fences can be indented. If the opening fence is indented, content lines will have equivalent opening indentation removed, if present:") {
    fencedCodeBlock shouldParseWith  (
      " ```\n aaa\naaa\n```\n",
      "aaa\naaa\n".toList
    )
    fencedCodeBlock shouldParseWith  (
      "  ```\naaa\n  aaa\naaa\n  ```\n",
      "aaa\naaa\naaa\n".toList
    )
    fencedCodeBlock shouldParseWith  (
      "   ```\n   aaa\n    aaa\n  aaa\n   ```\n",
      "aaa\n aaa\naaa\n".toList
    )
  }
  describe ("Four spaces indentation produces an indented code block:") {
    fencedCodeBlock shouldNotParse  (
      "    ```\n    aaaa\n    ```\n"
    )
  }
  describe ("Closing fences may be indented by 0-3 spaces, and their indentation need not match that of the opening fence:") {
    fencedCodeBlock shouldParseWith  (
      "```\naaa\n  ```\n",
      "aaa\n".toList
    )
    fencedCodeBlock shouldParseWith  (
      "   ```\naaa\n  ```\n",
      "aaa\n".toList
    )
  }
  describe ("This is not a closing fence, because it is indented 4 spaces:") {
    fencedCodeBlock shouldParseWith  (
      "```\naaa\n    ```\n",
      "aaa\n    ```\n".toList
    )
  }
  describe ("Code fences (opening and closing) cannot contain internal spaces:") {
    fencedCodeBlock shouldNotParse  (
      "``` ```\naaa\n"
    )
    fencedCodeBlock shouldParseWith  (
      "~~~~~~\naaa\n~~~ ~~\n",
      "aaa\n~~~ ~~\n".toList
    )
  }

  // Infostrings werden nicht implementiert!

  // ###########################################################################
  // ####################### Thematic Breaks Tests #############################
  // ###########################################################################
  describe ("A line consisting of 0-3 spaces of indentation, followed by a sequence of three or more matching -, _, or * characters, each followed optionally by any number of spaces, forms a thematic break:") {
    thematicBreak shouldParse  ("***\n")
    thematicBreak shouldParse  ("---\n")
    thematicBreak shouldParse  ("___\n")
  }
  describe ("Wrong characters:") {
    thematicBreak shouldNotParse  ("+++\n")
    thematicBreak shouldNotParse  ("===\n")
  }
  describe ("Not enough characters:") {
    thematicBreak shouldNotParse  ("--\n")
    thematicBreak shouldNotParse  ("**\n")
    thematicBreak shouldNotParse  ("__\n")
  }
  describe ("One to three spaces indent are allowed:") {
    thematicBreak shouldParse  (" ***\n")
    thematicBreak shouldParse  ("  ***\n")
    thematicBreak shouldParse  ("   ***\n")
  }
  describe ("Four spaces is too many:") {
    thematicBreak shouldNotParse  ("    ***\n")
  }
  describe ("More than three characters may be used:") {
    thematicBreak shouldParse  ("_____________________________________\n")
  }
  describe ("Spaces are allowed between the characters:") {
    thematicBreak shouldParse  (" - - -\n")
    thematicBreak shouldParse  (" **  * ** * ** * **\n")
    thematicBreak shouldParse  ("-     -      -      -\n")
  }
  describe ("Spaces are allowed at the end:") {
    thematicBreak shouldParse  ("- - - -    \n")
  }
  describe ("However, no other characters may occur in the line:") {
    thematicBreak shouldNotParse  ("_ _ _ _ a\n")
    thematicBreak shouldNotParse  ("a------\n")
    thematicBreak shouldNotParse  ("---a---\n")
  }
  describe ("It is required that all of the non-whitespace characters be the same. So, this is not a thematic break:") {
    thematicBreak shouldNotParse  (" *-*\n")
  }

  // ###########################################################################
  // ####################### Block Detection Tests #############################
  // ###########################################################################
/*
  describe ("ATX headings can be empty:") {
    blockParser shouldParseWith  (
      """|Foo bar
         |# baz
         |Bar foo
         |""".stripMargin('|'),
      """|<p>Foo bar</p>
         |<h1>baz</h1>
         |<p>Bar foo</p>""".stripMargin('|').toList
    )
    blockParser shouldParseWith (
      """|****
         |## foo
         |****
         |""".stripMargin('|'),
      """|<hr />
         |<h2>foo</h2>
         |<hr />""".stripMargin('|').toList
    )
  }
  describe ("Four spaces are too much:") {
    blockParser shouldParseWith  (
      """|foo
         |    # bar
         |""".stripMargin('|'),
      """|<p>foo
         |# bar</p>""".stripMargin('|').toList
    )
  }
  describe ("f there is any ambiguity between an interpretation of indentation as a code block and as indicating that material belongs to a list item, the list item interpretation takes precedence:") {
    blockParser shouldParseWith  (
      """|  - foo
         |
         |    bar""".stripMargin('|'),
      """|<ul>
         |<li>
         |<p>foo</p>
         |<p>bar</p>
         |</li>
         |</ul>""".stripMargin('|').toList
    )
    blockParser shouldParseWith  (
      """|1.  foo
         |
         |    - bar""".stripMargin('|'),
      """|<ol>
         |<li>
         |<p>foo</p>
         |<ul>
         |<li>bar</li>
         |</ul>
         |</li>
         |</ul>""".stripMargin('|').toList
    )
    describe ("An indented code block cannot interrupt a paragraph. ") {
      indentedCodeBlock shouldParseWith  (
        "Foo\n    bar\n",
        "Foo\nbar".toList
      )
    }
    describe ("Thematic breaks can interrupt a paragraph:") {
      indentedCodeBlock shouldParseWith  (
        "Foo\n***\nbar\n",
        "<p>Foo</p>\n<hr />\n<p>bar</p>".toList
      )
    }
    describe ("When both a thematic break and a list item are possible interpretations of a line, the thematic break takes precedence:") {
      indentedCodeBlock shouldParseWith  (
        "* Foo\n* * *bar\n* Bar\n",
        "<ul>\n<li>Foo</li>\n</ul>\n<hr />\n<ul>\n<li>bar</li>\n</ul>\n".toList
      )
    }
    describe ("If you want a thematic break in a list item, use a different bullet:") {
      indentedCodeBlock shouldParseWith  (
        "- Foo\n- * * *bar\n",
        "<ul>\n<li>Foo</li>\n<li>\n<hr />\n</li>\n</ul>\n".toList
      )
    }
  }
*/
}
