/*
 *  Java HTML Tidy - JTidy
 *  HTML parser and pretty printer
 *
 *  Copyright (c) 1998-2000 World Wide Web Consortium (Massachusetts
 *  Institute of Technology, Institut National de Recherche en
 *  Informatique et en Automatique, Keio University). All Rights
 *  Reserved.
 *
 *  Contributing Author(s):
 *
 *     Dave Raggett <dsr@w3.org>
 *     Andy Quick <ac.quick@sympatico.ca> (translation to Java)
 *     Gary L Peskin <garyp@firstech.com> (Java development)
 *     Sami Lempinen <sami@lempinen.net> (release management)
 *     Fabrizio Giustina <fgiust at users.sourceforge.net>
 *
 *  The contributing author(s) would like to thank all those who
 *  helped with testing, bug fixes, and patience.  This wouldn't
 *  have been possible without all of you.
 *
 *  COPYRIGHT NOTICE:
 *
 *  This software and documentation is provided "as is," and
 *  the copyright holders and contributing author(s) make no
 *  representations or warranties, express or implied, including
 *  but not limited to, warranties of merchantability or fitness
 *  for any particular purpose or that the use of the software or
 *  documentation will not infringe any third party patents,
 *  copyrights, trademarks or other rights.
 *
 *  The copyright holders and contributing author(s) will not be
 *  liable for any direct, indirect, special or consequential damages
 *  arising out of any use of the software or documentation, even if
 *  advised of the possibility of such damage.
 *
 *  Permission is hereby granted to use, copy, modify, and distribute
 *  this source code, or portions hereof, documentation and executables,
 *  for any purpose, without fee, subject to the following restrictions:
 *
 *  1. The origin of this source code must not be misrepresented.
 *  2. Altered versions must be plainly marked as such and must
 *     not be misrepresented as being the original source.
 *  3. This Copyright notice may not be removed or altered from any
 *     source or altered source distribution.
 *
 *  The copyright holders and contributing author(s) specifically
 *  permit, without fee, and encourage the use of this source code
 *  as a component for supporting the Hypertext Markup Language in
 *  commercial products. If you use this source code in a product,
 *  acknowledgment is not required but would be appreciated.
 *
 */
package org.w3c.tidy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;


/**
 * Pretty print parse tree. Block-level and unknown elements are printed on new lines and their contents indented 2
 * spaces Inline elements are printed inline. Inline content is wrapped on spaces (except in attribute values or
 * preformatted text, after start tags and before end tags.
 *
 * @author Dave Raggett <a href="mailto:dsr@w3.org">dsr@w3.org </a>
 * @author Andy Quick <a href="mailto:ac.quick@sympatico.ca">ac.quick@sympatico.ca </a> (translation to Java)
 * @author Fabrizio Giustina
 * @version $Revision$ ($Author$)
 */
public class PPrint
{

    /**
     * position: normal.
     */
    private static final short NORMAL = 0;

    /**
     * position: preformatted text.
     */
    private static final short PREFORMATTED = 1;

    /**
     * position: comment.
     */
    private static final short COMMENT = 2;

    /**
     * position: attribute value.
     */
    private static final short ATTRIBVALUE = 4;

    /**
     * position: nowrap.
     */
    private static final short NOWRAP = 8;

    /**
     * position: cdata.
     */
    private static final short CDATA = 16;

    /**
     * Start cdata token.
     */
    private static final String CDATA_START = "<![CDATA[";

    /**
     * End cdata token.
     */
    private static final String CDATA_END = "]]>";

    /**
     * Javascript comment start.
     */
    private static final String JS_COMMENT_START = "//";

    /**
     * Javascript comment end.
     */
    private static final String JS_COMMENT_END = "";

    /**
     * VB comment start.
     */
    private static final String VB_COMMENT_START = "\'";

    /**
     * VB comment end.
     */
    private static final String VB_COMMENT_END = "";

    /**
     * CSS comment start.
     */
    private static final String CSS_COMMENT_START = "/*";

    /**
     * CSS comment end.
     */
    private static final String CSS_COMMENT_END = "*/";

    /**
     * Default comment start.
     */
    private static final String DEFAULT_COMMENT_START = "";

    /**
     * Default comment end.
     */
    private static final String DEFAULT_COMMENT_END = "";

    private int[] linebuf;

    private int lbufsize;

    private int linelen;

    private int wraphere;

    private boolean inAttVal;

    private boolean inString;

    /**
     * Current slide number.
     */
    private int slide;

    /**
     * Total slides count.
     */
    private int count;

    /**
     * The slide content node.
     */
    private Node slidecontent;

    /**
     * current configuration.
     */
    private Configuration configuration;

    /**
     * Instantiates a new PPrint.
     *
     * @param configuration configuration
     */
    public PPrint(Configuration configuration)
    {
        this.configuration = configuration;
    }

    /**
     * Wrap length.
     *
     * @param ind the wrap amount
     * @return the total wrap amount
     */
    int cWrapLen(int ind)
    {
        /* #431953 - start RJ Wraplen adjusted for smooth international ride */
        if ("zh".equals(this.configuration.language))
        {
            // Chinese characters take two positions on a fixed-width screen
            // It would be more accurate to keep a parallel linelen and wraphere incremented by 2 for Chinese characters
            // and 1 otherwise, but this is way simpler.
            return (ind + ((this.configuration.wraplen - ind) / 2));
        }
        if ("ja".equals(this.configuration.language))
        {
            /* average Japanese text is 30% kanji */
            return (ind + (((this.configuration.wraplen - ind) * 7) / 10));
        }
        return (this.configuration.wraplen);
        /* #431953 - end RJ */
    }

    /**
     * Return one less than the number of bytes used by the UTF-8 byte sequence.
     * The Unicode char is returned in ch.
     *
     * @param str   points to the UTF-8 byte sequence
     * @param start starting offset in string
     * @param ch    initialised to 1st byte, passed as an array to allow modification
     * @return one less that the number of bytes used by UTF-8 char
     */
    public static int getUTF8(byte[] str, int start, int[] ch)
    {

        int[] n = new int[1];

        int[] bytes = new int[]{0};

        // first byte "str[0]" is passed in separately from the
        // rest of the UTF-8 byte sequence starting at "str[1]"

        boolean err = EncodingUtils.decodeUTF8BytesToChar(
            n,
            TidyUtils.toUnsigned(str[start]),
            str,
            null,
            bytes,
            start + 1);

        if (err)
        {
            n[0] = 0xFFFD; // replacement char
        }
        ch[0] = n[0];
        return bytes[0] - 1;

    }

    /**
     * Store char c as UTF-8 encoded byte stream.
     *
     * @param buf   the buffer
     * @param start the start offset
     * @param c     the char
     * @return the current position
     */
    public static int putUTF8(byte[] buf, int start, int c)
    {
        int[] count = new int[]{0};

        boolean err = EncodingUtils.encodeCharToUTF8Bytes(c, buf, null, count);
        if (err)
        {
            // replacement char 0xFFFD encoded as UTF-8
            buf[0] = (byte) 0xEF;
            buf[1] = (byte) 0xBF;
            buf[2] = (byte) 0xBD;
            count[0] = 3;
        }

        start += count[0];

        return start;
    }

    /**
     * Add char at index.
     *
     * @param c     the char
     * @param index the index
     */
    private void addC(int c, int index)
    {
        if (index + 1 >= lbufsize)
        {
            while (index + 1 >= lbufsize)
            {
                if (lbufsize == 0)
                {
                    lbufsize = 256;
                }
                else
                {
                    lbufsize = lbufsize * 2;
                }
            }

            int[] temp = new int[lbufsize];
            if (linebuf != null)
            {
                System.arraycopy(linebuf, 0, temp, 0, index);
            }
            linebuf = temp;
        }

        linebuf[index] = c;
    }

    /**
     * Adds an ASCII String.
     *
     * @param str   String to be added
     * @param index actual line length
     * @return final line length
     */
    private int addAsciiString(String str, int index)
    {

        int len = str.length();
        if (index + len >= lbufsize)
        {
            while (index + len >= lbufsize)
            {
                if (lbufsize == 0)
                {
                    lbufsize = 256;
                }
                else
                {
                    lbufsize = lbufsize * 2;
                }
            }

            int[] temp = new int[lbufsize];
            if (linebuf != null)
            {
                System.arraycopy(linebuf, 0, temp, 0, index);
            }
            linebuf = temp;
        }

        for (int ix = 0; ix < len; ++ix)
        {
            linebuf[index + ix] = str.charAt(ix);
        }
        return index + len;
    }

    /**
     * Wrap line.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     */
    private void wrapLine(Out fout, int indent)
    {
        int i, p, q;

        if (wraphere == 0)
        {
            return;
        }

        for (i = 0; i < indent; ++i)
        {
            fout.outc(' ');
        }

        for (i = 0; i < wraphere; ++i)
        {
            fout.outc(linebuf[i]);
        }

        if (inString)
        {
            fout.outc(' ');
            fout.outc('\\');
        }

        fout.newline();

        if (linelen > wraphere)
        {
            p = 0;

            if (linebuf[wraphere] == ' ')
            {
                ++wraphere;
            }

            q = wraphere;
            addC('\0', linelen);

            while (true)
            {
                linebuf[p] = linebuf[q];
                if (linebuf[q] == 0)
                {
                    break;
                }
                p++;
                q++;
            }
            linelen -= wraphere;
        }
        else
        {
            linelen = 0;
        }

        wraphere = 0;
    }

    /**
     * Wrap attribute value.
     *
     * @param fout     where to write output
     * @param indent   the indent amount
     * @param inString value to wrap
     */
    private void wrapAttrVal(Out fout, int indent, boolean inString)
    {
        int i, p, q;

        for (i = 0; i < indent; ++i)
        {
            fout.outc(' ');
        }

        for (i = 0; i < wraphere; ++i)
        {
            fout.outc(linebuf[i]);
        }

        fout.outc(' ');

        if (inString)
        {
            fout.outc('\\');
        }

        fout.newline();

        if (linelen > wraphere)
        {
            p = 0;

            if (linebuf[wraphere] == ' ')
            {
                ++wraphere;
            }

            q = wraphere;
            addC('\0', linelen);

            while (true)
            {
                linebuf[p] = linebuf[q];
                if (linebuf[q] == 0)
                {
                    break;
                }
                p++;
                q++;
            }
            linelen -= wraphere;
        }
        else
        {
            linelen = 0;
        }

        wraphere = 0;
    }

    /**
     * Flush the line.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     * @param shouldAddNewLine <code>true</code> to add new line after flush
     */
    public void flushLine(Out fout, int indent, boolean shouldAddNewLine)
    {
        int i;

        if (linelen > 0)
        {
            if (indent + linelen >= this.configuration.wraplen)
            {
                wrapLine(fout, indent);
            }

            if (!inAttVal || this.configuration.indentAttributes)
            {
                for (i = 0; i < indent; ++i)
                {
                    fout.outc(' ');
                }
            }

            for (i = 0; i < linelen; ++i)
            {
                fout.outc(linebuf[i]);
            }
        }

        if(shouldAddNewLine) {
          fout.newline();
        }
        linelen = 0;
        wraphere = 0;
        inAttVal = false;
    }
    
    /**
     * Flush the line.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     */
    public void flushLine(Out fout, int indent)
    {
      flushLine(fout, indent, true);
    }

    /**
     * Conditional flush the line.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     * @param shouldAddNewLine <code>true</code> to add new line after flush
     */
    public void condFlushLine(Out fout, int indent, boolean shouldAddNewLine)
    {
        int i;

        if (linelen > 0)
        {
            if (indent + linelen >= this.configuration.wraplen)
            {
                wrapLine(fout, indent);
            }

            if (!inAttVal || this.configuration.indentAttributes)
            {
                for (i = 0; i < indent; ++i)
                {
                    fout.outc(' ');
                }
            }

            for (i = 0; i < linelen; ++i)
            {
                fout.outc(linebuf[i]);
            }

            if(shouldAddNewLine) {
              fout.newline();
            }
            linelen = 0;
            wraphere = 0;
            inAttVal = false;
        }
    }
    
    /**
     * Conditional flush the line.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     */
    public void condFlushLine(Out fout, int indent)
    {
      condFlushLine(fout, indent, true);
    }

    /**
     * Print char.
     *
     * @param c    the char to print
     * @param mode the mode to use
     */
    private void printChar(int c, short mode)
    {
        String entity;
        boolean breakable = false; // #431953 - RJ

        if (c == ' ' && !TidyUtils.toBoolean(mode & (PREFORMATTED | COMMENT | ATTRIBVALUE | CDATA)))
        {
            // coerce a space character to a non-breaking space
            if (TidyUtils.toBoolean(mode & NOWRAP))
            {
                // by default XML doesn't define &nbsp;
                if (this.configuration.numEntities || this.configuration.xmlTags)
                {
                    addC('&', linelen++);
                    addC('#', linelen++);
                    addC('1', linelen++);
                    addC('6', linelen++);
                    addC('0', linelen++);
                    addC(';', linelen++);
                }
                else
                {
                    // otherwise use named entity
                    addC('&', linelen++);
                    addC('n', linelen++);
                    addC('b', linelen++);
                    addC('s', linelen++);
                    addC('p', linelen++);
                    addC(';', linelen++);
                }
                return;
            }
            wraphere = linelen;
        }

        // comment characters are passed raw
        if (TidyUtils.toBoolean(mode & (COMMENT | CDATA)))
        {
            addC(c, linelen++);
            return;
        }

        // except in CDATA map < to &lt; etc.
        if (!TidyUtils.toBoolean(mode & CDATA))
        {
            if (c == '<')
            {
                addC('&', linelen++);
                addC('l', linelen++);
                addC('t', linelen++);
                addC(';', linelen++);
                return;
            }

            if (c == '>')
            {
                addC('&', linelen++);
                addC('g', linelen++);
                addC('t', linelen++);
                addC(';', linelen++);
                return;
            }

            // naked '&' chars can be left alone or quoted as &amp;
            // The latter is required for XML where naked '&' are illegal.
            if (c == '&' && this.configuration.quoteAmpersand)
            {
                addC('&', linelen++);
                addC('a', linelen++);
                addC('m', linelen++);
                addC('p', linelen++);
                addC(';', linelen++);
                return;
            }

            if (c == '"' && this.configuration.quoteMarks)
            {
                addC('&', linelen++);
                addC('q', linelen++);
                addC('u', linelen++);
                addC('o', linelen++);
                addC('t', linelen++);
                addC(';', linelen++);
                return;
            }

            if (c == '\'' && this.configuration.quoteMarks)
            {
                addC('&', linelen++);
                addC('#', linelen++);
                addC('3', linelen++);
                addC('9', linelen++);
                addC(';', linelen++);
                return;
            }

            if (c == 160 && !this.configuration.rawOut)
            {
                if (this.configuration.makeBare)
                {
                    addC(' ', linelen++);
                }
                else if (this.configuration.quoteNbsp)
                {
                    addC('&', linelen++);

                    if (this.configuration.numEntities || this.configuration.xmlTags)
                    {
                        addC('#', linelen++);
                        addC('1', linelen++);
                        addC('6', linelen++);
                        addC('0', linelen++);
                    }
                    else
                    {
                        addC('n', linelen++);
                        addC('b', linelen++);
                        addC('s', linelen++);
                        addC('p', linelen++);
                    }

                    addC(';', linelen++);
                }
                else
                {
                    addC(c, linelen++);
                }

                return;
            }
        }

        // #431953 - start RJ
        // Handle encoding-specific issues

        if ("UTF8".equals(this.configuration.getOutCharEncodingName()))
        {
            // Chinese doesn't have spaces, so it needs other kinds of breaks
            // This will also help documents using nice Unicode punctuation
            // But we leave the ASCII range punctuation untouched

            // Break after any punctuation or spaces characters
            if ((c >= 0x2000) && !TidyUtils.toBoolean(mode & PREFORMATTED))
            {
                if (((c >= 0x2000) && (c <= 0x2006))
                    || ((c >= 0x2008) && (c <= 0x2010))
                    || ((c >= 0x2011) && (c <= 0x2046))
                    || ((c >= 0x207D) && (c <= 0x207E))
                    || ((c >= 0x208D) && (c <= 0x208E))
                    || ((c >= 0x2329) && (c <= 0x232A))
                    || ((c >= 0x3001) && (c <= 0x3003))
                    || ((c >= 0x3008) && (c <= 0x3011))
                    || ((c >= 0x3014) && (c <= 0x301F))
                    || ((c >= 0xFD3E) && (c <= 0xFD3F))
                    || ((c >= 0xFE30) && (c <= 0xFE44))
                    || ((c >= 0xFE49) && (c <= 0xFE52))
                    || ((c >= 0xFE54) && (c <= 0xFE61))
                    || ((c >= 0xFE6A) && (c <= 0xFE6B))
                    || ((c >= 0xFF01) && (c <= 0xFF03))
                    || ((c >= 0xFF05) && (c <= 0xFF0A))
                    || ((c >= 0xFF0C) && (c <= 0xFF0F))
                    || ((c >= 0xFF1A) && (c <= 0xFF1B))
                    || ((c >= 0xFF1F) && (c <= 0xFF20))
                    || ((c >= 0xFF3B) && (c <= 0xFF3D))
                    || ((c >= 0xFF61) && (c <= 0xFF65)))
                {
                    wraphere = linelen + 1;
                    breakable = true;
                }
                else
                {
                    switch (c)
                    {
                        case 0xFE63:
                        case 0xFE68:
                        case 0x3030:
                        case 0x30FB:
                        case 0xFF3F:
                        case 0xFF5B:
                        case 0xFF5D:
                            wraphere = linelen + 1;
                            breakable = true;
                        default:
                            break;
                    }
                }
                // but break before a left punctuation
                if (breakable)
                {
                    if (((c >= 0x201A) && (c <= 0x201C)) || ((c >= 0x201E) && (c <= 0x201F)))
                    {
                        wraphere--;
                    }
                    else
                    {
                        switch (c)
                        {
                            case 0x2018:
                            case 0x2039:
                            case 0x2045:
                            case 0x207D:
                            case 0x208D:
                            case 0x2329:
                            case 0x3008:
                            case 0x300A:
                            case 0x300C:
                            case 0x300E:
                            case 0x3010:
                            case 0x3014:
                            case 0x3016:
                            case 0x3018:
                            case 0x301A:
                            case 0x301D:
                            case 0xFD3E:
                            case 0xFE35:
                            case 0xFE37:
                            case 0xFE39:
                            case 0xFE3B:
                            case 0xFE3D:
                            case 0xFE3F:
                            case 0xFE41:
                            case 0xFE43:
                            case 0xFE59:
                            case 0xFE5B:
                            case 0xFE5D:
                            case 0xFF08:
                            case 0xFF3B:
                            case 0xFF5B:
                            case 0xFF62:
                                wraphere--;
                        }
                    }
                }
            }
            else if ("BIG5".equals(this.configuration.getOutCharEncodingName()))
            {
                // Allow linebreak at Chinese punctuation characters
                // There are not many spaces in Chinese
                addC(c, linelen++);
                if (((c & 0xFF00) == 0xA100) && !TidyUtils.toBoolean(mode & PREFORMATTED))
                {
                    wraphere = linelen;
                    // opening brackets have odd codes: break before them
                    if ((c > 0x5C) && (c < 0xAD) && ((c & 1) == 1))
                    {
                        wraphere--;
                    }
                }
                return;
            }
            else if ("SHIFTJIS".equals(this.configuration.getOutCharEncodingName())
                || "ISO2022".equals(this.configuration.getOutCharEncodingName()))
            {
                // ISO 2022 characters are passed raw
                addC(c, linelen++);
                return;
            }
            else
            {
                if (this.configuration.rawOut)
                {
                    addC(c, linelen++);
                    return;
                }
            }
            // #431953 - end RJ
        }

        // if preformatted text, map &nbsp; to space
        if (c == 160 && TidyUtils.toBoolean(mode & PREFORMATTED))
        {
            addC(' ', linelen++);
            return;
        }

        // Filters from Word and PowerPoint often use smart quotes resulting in character codes between 128 and 159.
        // Unfortunately, the corresponding HTML 4.0 entities for these are not widely supported.
        // The following converts dashes and quotation marks to the nearest ASCII equivalent.
        // My thanks to Andrzej Novosiolov for his help with this code.

        if (this.configuration.makeClean && this.configuration.asciiChars || this.configuration.makeBare)
        {
            if (c >= 0x2013 && c <= 0x201E)
            {
                switch (c)
                {
                    case 0x2013: // en dash
                    case 0x2014: // em dash
                        c = '-';
                        break;
                    case 0x2018: // left single quotation mark
                    case 0x2019: // right single quotation mark
                    case 0x201A: // single low-9 quotation mark
                        c = '\'';
                        break;
                    case 0x201C: // left double quotation mark
                    case 0x201D: // right double quotation mark
                    case 0x201E: // double low-9 quotation mark
                        c = '"';
                        break;
                }
            }
        }

        // don't map latin-1 chars to entities
        if ("ISO8859_1".equals(this.configuration.getOutCharEncodingName()))
        {
            if (c > 255) /* multi byte chars */
            {
                if (!this.configuration.numEntities)
                {
                    entity = EntityTable.getDefaultEntityTable().entityName((short) c);
                    if (entity != null)
                    {
                        entity = "&" + entity + ";";
                    }
                    else
                    {
                        entity = "&#" + c + ";";
                    }
                }
                else
                {
                    entity = "&#" + c + ";";
                }

                for (int i = 0; i < entity.length(); i++)
                {
                    addC(entity.charAt(i), linelen++);
                }

                return;
            }

            if (c > 126 && c < 160)
            {
                entity = "&#" + c + ";";

                for (int i = 0; i < entity.length(); i++)
                {
                    addC(entity.charAt(i), linelen++);
                }

                return;
            }

            addC(c, linelen++);
            return;
        }

        // don't map utf8 or utf16 chars to entities
        if (this.configuration.getOutCharEncodingName().startsWith("UTF"))
        {
            addC(c, linelen++);
            return;
        }

        // use numeric entities only for XML
        if (this.configuration.xmlTags)
        {
            // if ASCII use numeric entities for chars > 127
            if (c > 127 && "ASCII".equals(this.configuration.getOutCharEncodingName()))
            {
                entity = "&#" + c + ";";

                for (int i = 0; i < entity.length(); i++)
                {
                    addC(entity.charAt(i), linelen++);
                }

                return;
            }

            // otherwise output char raw
            addC(c, linelen++);
            return;
        }

        // default treatment for ASCII
        if ("ASCII".equals(this.configuration.getOutCharEncodingName()) && (c > 126 || (c < ' ' && c != '\t')))
        {
            if (!this.configuration.numEntities)
            {
                entity = EntityTable.getDefaultEntityTable().entityName((short) c);
                if (entity != null)
                {
                    entity = "&" + entity + ";";
                }
                else
                {
                    entity = "&#" + c + ";";
                }
            }
            else
            {
                entity = "&#" + c + ";";
            }

            for (int i = 0; i < entity.length(); i++)
            {
                addC(entity.charAt(i), linelen++);
            }

            return;
        }

        addC(c, linelen++);
    }

    /**
     * The line buffer is uint not char so we can hold unicode values un-encoded.
     * The translation to UTF-8 is deferred to
     * the outc routine called to flush the line buffer.
     *
     * @param fout      where to write output
     * @param mode      the mode to use
     * @param indent    the indent amount
     * @param textarray the text
     * @param start     offset to start from
     * @param end       offset to read up to (exclusive)
     */
    private void printText(Out fout, short mode, int indent, byte[] textarray, int start, int end)
    {
        int i, c;
        int[] ci = new int[1];

        for (i = start; textarray != null && i < end; ++i)
        {
            if (indent + linelen >= this.configuration.wraplen)
            {
                wrapLine(fout, indent);
            }

            c = (textarray[i]) & 0xFF; // Convert to unsigned.

            // look for UTF-8 multibyte character
            if (c > 0x7F)
            {
                i += getUTF8(textarray, i, ci);
                c = ci[0];
            }

            if (c == '\n')
            {
                flushLine(fout, indent);
                continue;
            }

            printChar(c, mode);
        }
    }

    /**
     * @param str the string to print
     */
    private void printString(String str)
    {
        for (int i = 0; i < str.length(); i++)
        {
            addC(str.charAt(i), linelen++);
        }
    }

    /**
     * Print the attribute value.
     *
     * @param fout      where to write output
     * @param indent    the indent amount
     * @param value     the value
     * @param delim      the delimiter
     * @param wrappable wrappable or not
     */
    private void printAttrValue(Out fout, int indent, String value, int delim, boolean wrappable)
    {
        int c;
        int[] ci = new int[1];
        boolean wasinstring = false;
        byte[] valueChars = null;
        int i;
        short mode = (wrappable ? (short) (NORMAL | ATTRIBVALUE) : (short) (PREFORMATTED | ATTRIBVALUE));

        if (value != null)
        {
            valueChars = TidyUtils.getBytes(value);
        }

        // look for ASP, Tango or PHP instructions for computed attribute value
        if (valueChars != null && valueChars.length >= 5 && valueChars[0] == '<')
        {
            if (valueChars[1] == '%' || valueChars[1] == '@' || (new String(valueChars, 0, 5)).equals("<?php"))
            {
                mode |= CDATA;
            }
        }

        if (delim == 0)
        {
            delim = '"';
        }

        addC('=', linelen++);

        // don't wrap after "=" for xml documents
        if (!this.configuration.xmlOut)
        {

            if (indent + linelen < this.configuration.wraplen)
            {
                wraphere = linelen;
            }

            if (indent + linelen >= this.configuration.wraplen)
            {
                wrapLine(fout, indent);
            }

            if (indent + linelen < this.configuration.wraplen)
            {
                wraphere = linelen;
            }
            else
            {
                condFlushLine(fout, indent);
            }
        }

        addC(delim, linelen++);

        if (value != null)
        {
            inString = false;

            i = 0;
            while (i < valueChars.length)
            {
                c = (valueChars[i]) & 0xFF; // Convert to unsigned.

                if (wrappable && c == ' ' && indent + linelen < this.configuration.wraplen)
                {
                    wraphere = linelen;
                    wasinstring = inString;
                }

                if (wrappable && wraphere > 0 && indent + linelen >= this.configuration.wraplen)
                {
                    wrapAttrVal(fout, indent, wasinstring);
                }

                if (c == delim)
                {
                    String entity;

                    entity = (c == '"' ? "&quot;" : "&#39;");

                    for (int j = 0; j < entity.length(); j++)
                    {
                        addC(entity.charAt(j), linelen++);
                    }

                    ++i;
                    continue;
                }
                else if (c == '"')
                {
                    if (this.configuration.quoteMarks)
                    {
                        addC('&', linelen++);
                        addC('q', linelen++);
                        addC('u', linelen++);
                        addC('o', linelen++);
                        addC('t', linelen++);
                        addC(';', linelen++);
                    }
                    else
                    {
                        addC('"', linelen++);
                    }

                    if (delim == '\'')
                    {
                        inString = !inString;
                    }

                    ++i;
                    continue;
                }
                else if (c == '\'')
                {
                    if (this.configuration.quoteMarks)
                    {
                        addC('&', linelen++);
                        addC('#', linelen++);
                        addC('3', linelen++);
                        addC('9', linelen++);
                        addC(';', linelen++);
                    }
                    else
                    {
                        addC('\'', linelen++);
                    }

                    if (delim == '"')
                    {
                        inString = !inString;
                    }

                    ++i;
                    continue;
                } else if (c == '\n') {
                    addC('&', linelen++);
                    addC('#', linelen++);
                    addC('1', linelen++);
                    addC('0', linelen++);
                    addC(';', linelen++);
                    ++i;
                    continue;
                }

                // look for UTF-8 multibyte character
                if (c > 0x7F)
                {
                    i += getUTF8(valueChars, i, ci);
                    c = ci[0];
                }

                ++i;

                if (c == '\n')
                {
                    flushLine(fout, indent);
                    continue;
                }

                printChar(c, mode);
            }
        }

        inString = false;
        addC(delim, linelen++);
    }

    /**
     * Print the attribute.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     * @param node   the node to use
     * @param attr   the attribute value
     */
    private void printAttribute(Out fout, int indent, Node node, AttVal attr)
    {
        String name;
        boolean wrappable = false;

        if (this.configuration.indentAttributes)
        {
            flushLine(fout, indent);
            indent += this.configuration.spaces;
        }

        name = attr.attribute;

        if (indent + linelen >= this.configuration.wraplen)
        {
            wrapLine(fout, indent);
        }

        if (!this.configuration.xmlTags && !this.configuration.xmlOut && attr.dict != null)
        {
            if (AttributeTable.getDefaultAttributeTable().isScript(name))
            {
                wrappable = this.configuration.wrapScriptlets;
            }
            else if (!attr.dict.isNowrap() && this.configuration.wrapAttVals)
            {
                wrappable = true;
            }
        }

        if (indent + linelen < this.configuration.wraplen)
        {
            wraphere = linelen;
            addC(' ', linelen++);
        }
        else
        {
            condFlushLine(fout, indent);
            addC(' ', linelen++);
        }

        for (int i = 0; i < name.length(); i++)
        {
            addC(
                TidyUtils.foldCase(name.charAt(i), this.configuration.upperCaseAttrs, this.configuration.xmlTags),
                linelen++);
        }

        if (indent + linelen >= this.configuration.wraplen)
        {
            wrapLine(fout, indent);
        }

        if (attr.value == null)
        {
            if (this.configuration.xmlTags || this.configuration.xmlOut)
            {
                printAttrValue(fout, indent, (attr.isBoolAttribute() ? attr.attribute : ""), attr.delim, true);
            }
            else if (!attr.isBoolAttribute() && node != null && !node.isNewNode())
            {
                printAttrValue(fout, indent, "", attr.delim, true);
            }
            else if (indent + linelen < this.configuration.wraplen)
            {
                wraphere = linelen;
            }

        }
        else
        {
            printAttrValue(fout, indent, attr.value, attr.delim, wrappable);
        }
    }

    /**
     * Print the attributes.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     * @param node   the node to use
     * @param attr   the attribute value
     */
    private void printAttrs(Out fout, int indent, Node node, AttVal attr)
    {
        // add xml:space attribute to pre and other elements
        if (configuration.xmlOut
            && configuration.xmlSpace
            && ParserImpl.XMLPreserveWhiteSpace(node, configuration.tt)
            && node.getAttrByName("xml:space") == null)
        {
            node.addAttribute("xml:space", "preserve");
            if (attr != null)
            {
                attr = node.attributes;
            }
        }

        if (attr != null)
        {
            if (attr.next != null)
            {
                printAttrs(fout, indent, node, attr.next);
            }

            if (attr.attribute != null)
            {
                Attribute attribute = attr.dict;

                if (!this.configuration.dropProprietaryAttributes
                    || !(attribute == null || TidyUtils.containsAny(attribute.getVersions(), Dict.VERS_PROPRIETARY)))
                {
                    printAttribute(fout, indent, node, attr);
                }
            }
            else if (attr.asp != null)
            {
                addC(' ', linelen++);
                printAsp(fout, indent, attr.asp);
            }
            else if (attr.php != null)
            {
                addC(' ', linelen++);
                printPhp(fout, indent, attr.php);
            }
        }

    }

    /**
     * Line can be wrapped immediately after inline start tag provided if follows a text node ending in a space, or it
     * parent is an inline element that that rule applies to. This behaviour was reverse engineered from Netscape 3.0
     *
     * @param node current Node
     * @return <code>true</code> if the current char follows a space
     */
    private static boolean afterSpace(Node node)
    {
        Node prev;
        int c;

        if (node == null || node.tag == null || !TidyUtils.toBoolean(node.tag.model & Dict.CM_INLINE))
        {
            return true;
        }

        prev = node.prev;

        if (prev != null)
        {
            if (prev.type == Node.TEXT_NODE && prev.end > prev.start && prev.textarray != null)
            {
                c = (prev.textarray[prev.end - 1]) & 0xFF; // Convert to unsigned.

                return c == 160 || c == ' ' || c == '\n';
            }

            return false;
        }

        return afterSpace(node.parent);
    }

    /**
     * Print the tag.
     *
     * @param lexer  the Lexer to use
     * @param fout   where to write output
     * @param mode   the mode to use
     * @param indent the indent amount
     * @param node   the node to use
     */
    private void printTag(Lexer lexer, Out fout, short mode, int indent, Node node)
    {
        String p;
        TagTable tt = this.configuration.tt;

        addC('<', linelen++);

        if (node.type == Node.END_TAG)
        {
            addC('/', linelen++);
        }

        p = node.element;
        for (int i = 0; i < p.length(); i++)
        {
            addC(
                TidyUtils.foldCase(p.charAt(i), this.configuration.upperCaseTags, this.configuration.xmlTags),
                linelen++);
        }

        printAttrs(fout, indent, node, node.attributes);

        if ((this.configuration.xmlOut || this.configuration.xHTML)
            && (node.type == Node.START_END_TAG || TidyUtils.toBoolean(node.tag.model & Dict.CM_EMPTY)))
        {
            addC(' ', linelen++); // Space is NS compatibility hack <br />
            addC('/', linelen++); // Required end tag marker
        }

        addC('>', linelen++);

        if ((node.type != Node.START_END_TAG || configuration.xHTML) && !TidyUtils.toBoolean(mode & PREFORMATTED))
        {
            if (indent + linelen >= this.configuration.wraplen)
            {
                wrapLine(fout, indent);
            }

            if (indent + linelen < this.configuration.wraplen)
            {

                // wrap after start tag if is <br/> or if it's not inline
                // fix for [514348]
                if (!TidyUtils.toBoolean(mode & NOWRAP)
                    && (!TidyUtils.toBoolean(node.tag.model & Dict.CM_INLINE) || (node.tag == tt.tagBr))
                    && afterSpace(node))
                {
                    wraphere = linelen;
                }

            }
        }
        else
        {
            condFlushLine(fout, indent, !hasPreAncestor(node));
        }

    }

    /**
     * Check if the given node has a "pre" as ancestor.
     * 
     * @param node The note to check.
     * @return  <code>true</code> if it has a pre note as ancestor.
     */
    private boolean hasPreAncestor(Node node) 
    {
      boolean hasPreParent = false;
      Node parent = node.parent;
      while (parent != null) {
        if(parent.tag != null && parent.tag.getParser() == ParserImpl.PRE) {
          hasPreParent = true;
          break;
        }
        parent = parent.parent;
      }
      
      return hasPreParent;
    }
    
    /**
     * Print the end tag.
     *
     * @param mode   the mode to use
     * @param indent the indent amount
     * @param node   the node to use
     */
    private void printEndTag(short mode, int indent, Node node)
    {
        String p;

        // Netscape ignores SGML standard by not ignoring a line break before </A> or </U> etc.
        // To avoid rendering this as an underlined space, I disable line wrapping before inline end tags

        // if (indent + linelen < this.configuration.wraplen && !TidyUtils.toBoolean(mode & NOWRAP))
        // {
        // wraphere = linelen;
        // }

        addC('<', linelen++);
        addC('/', linelen++);

        p = node.element;
        for (int i = 0; i < p.length(); i++)
        {
            addC(
                TidyUtils.foldCase(p.charAt(i), this.configuration.upperCaseTags, this.configuration.xmlTags),
                linelen++);
        }

        addC('>', linelen++);
    }

    /**
     * Print comment.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     * @param node   the node to use
     */
    private void printComment(Out fout, int indent, Node node)
    {
        if (this.configuration.hideComments)
        {
            return;
        }

        if (indent + linelen < this.configuration.wraplen)
        {
            wraphere = linelen;
        }

        addC('<', linelen++);
        addC('!', linelen++);
        addC('-', linelen++);
        addC('-', linelen++);

        printText(fout, COMMENT, indent, node.textarray, node.start, node.end);

        // See Lexer.java: AQ 8Jul2000
        addC('-', linelen++);
        addC('-', linelen++);
        addC('>', linelen++);

        if (node.linebreak)
        {
            flushLine(fout, indent);
        }
    }

    /**
     * Print document type.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     * @param lexer  the Lexer to use
     * @param node   the node to use
     */
    private void printDocType(Out fout, int indent, Lexer lexer, Node node)
    {
        int i, c = 0;
        short mode = 0;
        boolean q = this.configuration.quoteMarks;

        this.configuration.quoteMarks = false;

        if (indent + linelen < this.configuration.wraplen)
        {
            wraphere = linelen;
        }

        condFlushLine(fout, indent);

        addC('<', linelen++);
        addC('!', linelen++);
        addC('D', linelen++);
        addC('O', linelen++);
        addC('C', linelen++);
        addC('T', linelen++);
        addC('Y', linelen++);
        addC('P', linelen++);
        addC('E', linelen++);
        addC(' ', linelen++);

        if (indent + linelen < this.configuration.wraplen)
        {
            wraphere = linelen;
        }

        for (i = node.start; i < node.end; ++i)
        {
            if (indent + linelen >= this.configuration.wraplen)
            {
                wrapLine(fout, indent);
            }

            c = node.textarray[i] & 0xFF; // Convert to unsigned.

            // inDTDSubset?
            if (TidyUtils.toBoolean(mode & CDATA))
            {
                if (c == ']')
                {
                    mode &= ~CDATA;
                }
            }
            else if (c == '[')
            {
                mode |= CDATA;
            }
            int[] ci = new int[1];

            // look for UTF-8 multibyte character
            if (c > 0x7F)
            {
                i += getUTF8(node.textarray, i, ci);
                c = ci[0];
            }

            if (c == '\n')
            {
                flushLine(fout, indent);
                continue;
            }

            printChar(c, mode);
        }

        if (linelen < this.configuration.wraplen)
        {
            wraphere = linelen;
        }

        addC('>', linelen++);
        this.configuration.quoteMarks = q;
        condFlushLine(fout, indent);
    }

    /**
     * Pretty print processing instruction.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     * @param node   the node to use
     */
    private void printPI(Out fout, int indent, Node node)
    {
        if (indent + linelen < this.configuration.wraplen)
        {
            wraphere = linelen;
        }

        addC('<', linelen++);
        addC('?', linelen++);

        // set CDATA to pass < and > unescaped
        printText(fout, CDATA, indent, node.textarray, node.start, node.end);

        if (node.end <= 0 || node.textarray[node.end - 1] != '?') // #542029 - fix by Terry Teague 10 Apr 02
        {
            addC('?', linelen++);
        }

        addC('>', linelen++);
        condFlushLine(fout, indent);
    }

    /**
     * Pretty print the xml declaration.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     * @param node   the node to use
     */
    private void printXmlDecl(Out fout, int indent, Node node)
    {
        if (indent + linelen < this.configuration.wraplen)
        {
            wraphere = linelen;
        }

        addC('<', linelen++);
        addC('?', linelen++);
        addC('x', linelen++);
        addC('m', linelen++);
        addC('l', linelen++);

        printAttrs(fout, indent, node, node.attributes);

        if (node.end <= 0 || node.textarray[node.end - 1] != '?') // #542029 - fix by Terry Teague 10 Apr 02
        {
            addC('?', linelen++);
        }

        addC('>', linelen++);

        condFlushLine(fout, indent);
    }

    /**
     * Note ASP and JSTE share <% ... %> syntax.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     * @param node   the node to use
     */
    private void printAsp(Out fout, int indent, Node node)
    {
        int savewraplen = this.configuration.wraplen;

        // disable wrapping if so requested

        if (!this.configuration.wrapAsp || !this.configuration.wrapJste)
        {
            this.configuration.wraplen = 0xFFFFFF; // a very large number
        }

        addC('<', linelen++);
        addC('%', linelen++);

        printText(fout, (this.configuration.wrapAsp ? CDATA : COMMENT), indent, node.textarray, node.start, node.end);

        addC('%', linelen++);
        addC('>', linelen++);
        /* condFlushLine(fout, indent); */
        this.configuration.wraplen = savewraplen;
    }

    /**
     * JSTE also supports <# ... #> syntax
     *
     * @param fout   where to write output
     * @param indent the indent amount
     * @param node   the node to use
     */
    private void printJste(Out fout, int indent, Node node)
    {
        int savewraplen = this.configuration.wraplen;

        // disable wrapping if so requested

        if (!this.configuration.wrapJste)
        {
            this.configuration.wraplen = 0xFFFFFF; // a very large number
        }

        addC('<', linelen++);
        addC('#', linelen++);

        printText(fout, (this.configuration.wrapJste ? CDATA : COMMENT), indent, node.textarray, node.start, node.end);

        addC('#', linelen++);
        addC('>', linelen++);
        // condFlushLine(fout, indent);
        this.configuration.wraplen = savewraplen;
    }

    /**
     * PHP is based on XML processing instructions.
     *
     * @param fout   where to write output
     * @param indent the indent amount
     * @param node   the node to use
     */
    private void printPhp(Out fout, int indent, Node node)
    {
        int savewraplen = this.configuration.wraplen;

        // disable wrapping if so requested

        if (!this.configuration.wrapPhp)
        {
            this.configuration.wraplen = 0xFFFFFF; // a very large number
        }

        addC('<', linelen++);
        addC('?', linelen++);

        printText(fout, (this.configuration.wrapPhp ? CDATA : COMMENT), indent, node.textarray, node.start, node.end);

        addC('?', linelen++);
        addC('>', linelen++);
        // PCondFlushLine(fout, indent);
        this.configuration.wraplen = savewraplen;
    }

    /**
     * @param fout   where to write output
     * @param indent the indent amount
     * @param node   the node to use
     */
    private void printCDATA(Out fout, int indent, Node node)
    {
        int savewraplen = this.configuration.wraplen;

        if (!this.configuration.indentCdata)
        {
            indent = 0;
        }

        condFlushLine(fout, indent);

        // disable wrapping
        this.configuration.wraplen = 0xFFFFFF; // a very large number

        addC('<', linelen++);
        addC('!', linelen++);
        addC('[', linelen++);
        addC('C', linelen++);
        addC('D', linelen++);
        addC('A', linelen++);
        addC('T', linelen++);
        addC('A', linelen++);
        addC('[', linelen++);

        printText(fout, COMMENT, indent, node.textarray, node.start, node.end);

        addC(']', linelen++);
        addC(']', linelen++);
        addC('>', linelen++);
        condFlushLine(fout, indent);
        this.configuration.wraplen = savewraplen;
    }

    /**
     * @param fout   where to write output
     * @param indent the indent amount
     * @param node   the node to use
     */
    private void printSection(Out fout, int indent, Node node)
    {
        int savewraplen = this.configuration.wraplen;

        // disable wrapping if so requested

        if (!this.configuration.wrapSection)
        {
            this.configuration.wraplen = 0xFFFFFF; // a very large number
        }

        addC('<', linelen++);
        addC('!', linelen++);
        addC('[', linelen++);

        printText(
            fout,
            (this.configuration.wrapSection ? CDATA : COMMENT),
            indent,
            node.textarray,
            node.start,
            node.end);

        addC(']', linelen++);
        addC('>', linelen++);
        // PCondFlushLine(fout, indent);
        this.configuration.wraplen = savewraplen;
    }

    /**
     * Is the current node inside HEAD?
     *
     * @param node the node to use
     * @return <code>true</code> if node is inside an HEAD tag
     */
    private boolean insideHead(Node node)
    {
        if (node.tag == this.configuration.tt.tagHead)
        {
            return true;
        }

        if (node.parent != null)
        {
            return insideHead(node.parent);
        }
        return false;
    }

    /**
     * Is text node and already ends w/ a newline? Used to pretty print CDATA/PRE text content. If it already ends on a
     * newline, it is not necessary to print another before printing end tag.
     *
     * @param lexer Lexer
     * @param node  text node
     * @return text indent
     */
    private int textEndsWithNewline(Lexer lexer, Node node)
    {
        if (node.type == Node.TEXT_NODE && node.end > node.start)
        {
            int ch, ix = node.end - 1;
            // Skip non-newline whitespace
            while (ix >= node.start
                && TidyUtils.toBoolean(ch = (node.textarray[ix] & 0xff))
                && (ch == ' ' || ch == '\t' || ch == '\r'))
            {
                --ix;
            }

            if (ix >= 0 && node.textarray[ix] == '\n')
            {
                return node.end - ix - 1; // #543262 tidy eats all memory
            }
        }
        return -1;
    }

    /**
     * Does the current node contain a CDATA section?
     *
     * @param lexer Lexer
     * @param node  Node
     * @return <code>true</code> if node contains a CDATA section
     */
    static boolean hasCDATA(Lexer lexer, Node node)
    {
        // Scan forward through the textarray. Since the characters we're
        // looking for are < 0x7f, we don't have to do any UTF-8 decoding.

        if (node.type != Node.TEXT_NODE)
        {
            return false;
        }

        int len = node.end - node.start + 1;
        String start = TidyUtils.getString(node.textarray, node.start, len);

        int indexOfCData = start.indexOf(CDATA_START);
        if (indexOfCData > -1 && indexOfCData <= len) {
        	return true;
        }
        boolean containsSpecialChars = false;
        for (int i = node.start; i < node.end; i++) {
			byte c = node.textarray[i];
			if (c == '>' || c == '<' || c == '&') {
				containsSpecialChars = true;
				break;
			}
		}
        if (!containsSpecialChars) {
        	return true;
        }
        return false;
    }

    /**
     * Print script and style elements. For XHTML, wrap the content as follows:
     *
     * <pre>
     *     JavaScript:
     *         //&lt;![CDATA[
     *             content
     *         //]]>
     *     VBScript:
     *         '&lt;![CDATA[
     *             content
     *         ']]>
     *     CSS:
     *         /*&lt;![CDATA[* /
     *             content
     *         /*]]>* /
     *     other:
     *        &lt;![CDATA[
     *             content
     *         ]]>
     * </pre>
     *
     * @param fout   where to write output
     * @param mode   the mode
     * @param indent the indent amount
     * @param lexer  the Lexer to use
     * @param node   the node to use
     */
    private void printScriptStyle(Out fout, short mode, int indent, Lexer lexer, Node node)
    {
        Node content;
        String commentStart = DEFAULT_COMMENT_START;
        String commentEnd = DEFAULT_COMMENT_END;
        boolean hasCData = false;
        int contentIndent = -1;

        if (insideHead(node))
        {
            // flushLine(fout, indent);
        }
        flushLine(fout, indent);

        int oldIndent = indent;
        // start script
        printTag(lexer, fout, mode, indent, node);
        // flushLine(fout, indent); // extra newline

        if (lexer.configuration.xHTML && node.content != null)
        {
            AttVal type = node.getAttrByName("type");
            if (type != null)
            {
                if ("text/javascript".equalsIgnoreCase(type.value))
                {
                    commentStart = JS_COMMENT_START;
                    commentEnd = JS_COMMENT_END;
                }
                else if ("text/css".equalsIgnoreCase(type.value))
                {
                    commentStart = CSS_COMMENT_START;
                    commentEnd = CSS_COMMENT_END;
                }
                else if ("text/vbscript".equalsIgnoreCase(type.value))
                {
                    commentStart = VB_COMMENT_START;
                    commentEnd = VB_COMMENT_END;
                } else if (type.value != null && type.value.indexOf("script") != -1)
                {
                    commentStart = JS_COMMENT_START;
                    commentEnd = JS_COMMENT_END;
                }
            }

            hasCData = hasCDATA(lexer, node.content);
            if (!hasCData)
            {
                // disable wrapping
                int savewraplen = lexer.configuration.wraplen;
                lexer.configuration.wraplen = 0xFFFFFF; // a very large number

                linelen = addAsciiString(commentStart, linelen);
                linelen = addAsciiString(CDATA_START, linelen);
                linelen = addAsciiString(commentEnd, linelen);
                condFlushLine(fout, indent);

                // restore wrapping
                lexer.configuration.wraplen = savewraplen;
            }
        }

        indent = 0;

        boolean hasContent = false;
        for (content = node.content; content != null; content = content.next)
        {
        	if (content.type == Node.TEXT_NODE && content.start == content.end) {
        		// Empty content!
        		continue;
        	}
        	hasContent = true;
            printTree(fout, (short) (mode | PREFORMATTED | NOWRAP | CDATA), 0, lexer, content);

            if (content.next == null)
            {
                contentIndent = textEndsWithNewline(lexer, content);
            }

        }

        if (contentIndent < 0 && hasContent)
        {
            condFlushLine(fout, indent);
            contentIndent = 0;
        }

        if (lexer.configuration.xHTML && node.content != null)
        {
            if (!hasCData)
            {
                // disable wrapping
                int ix, savewraplen = lexer.configuration.wraplen;
                lexer.configuration.wraplen = 0xFFFFFF; // a very large number

                // Add spaces to last text node to align w/ indent
                if (contentIndent > 0 && linelen < contentIndent)
                {
                    linelen = contentIndent;
                }
                for (ix = 0; contentIndent < indent && ix < indent - contentIndent; ++ix)
                {
                    addC(' ', linelen++);
                }

                linelen = addAsciiString(commentStart, linelen);
                linelen = addAsciiString(CDATA_END, linelen);
                linelen = addAsciiString(commentEnd, linelen);

                // restore wrapping
                lexer.configuration.wraplen = savewraplen;
                condFlushLine(fout, 0);
            }
        }

        if (!hasCData) indent = oldIndent;
        printEndTag(mode, indent, node);

        if (!lexer.configuration.indentContent && node.next != null

            && !((node.tag != null && TidyUtils.toBoolean(node.tag.model & Dict.CM_INLINE))

            || node.type != Node.TEXT_NODE

        ))
        {
            flushLine(fout, indent);
        }

        flushLine(fout, indent);
    }

    /**
     * Should tidy indent the give tag?
     *
     * @param node actual node
     * @return <code>true</code> if line should be indented
     */
    private boolean shouldIndent(Node node)
    {
        TagTable tt = this.configuration.tt;

        if (!this.configuration.indentContent)
        {
            return false;
        }

        if (this.configuration.smartIndent)
        {
            if (node.content != null && TidyUtils.toBoolean(node.tag.model & Dict.CM_NO_INDENT))
            {
                for (node = node.content; node != null; node = node.next)
                {
                	if (node.type != Node.TEXT_NODE) {
                		return true;
                	}
                	//*
                    if (node.tag != null && TidyUtils.toBoolean(node.tag.model & Dict.CM_BLOCK))
                    {
                        return true;
                    }
                    //*/
                }

                return false;
            }

//            if (TidyUtils.toBoolean(node.tag.model & Dict.CM_HEADING))
//            {
//                return false;
//            }

            if (node.tag != null && TidyUtils.toBoolean(node.tag.model & Dict.CM_HEADING)
            		|| node.tag == tt.tagP || node.tag == tt.tagA
            		|| node.tag == tt.tagSup
            		|| node.tag == tt.tagSub
            		|| node.tag == tt.tagSmall
            		|| node.tag == tt.tagStrong
            		|| node.tag == tt.tagI
            		|| node.tag == tt.tagS
            		|| node.tag == tt.tagStrike
            		|| node.tag == tt.tagB
            		|| node.tag == tt.tagCite
            		|| node.tag == tt.tagCode
            		|| node.tag == tt.tagSpan
            		|| node.tag == tt.tagLabel
            		|| node.tag == tt.tagUl
            		|| node.tag == tt.tagLi
            		|| node.tag == tt.tagOl
            		|| node.tag == tt.tagH1
            		|| node.tag == tt.tagH2
            		|| node.tag == tt.tagH3
            		|| node.tag == tt.tagH4
            		|| node.tag == tt.tagH5
            		|| node.tag == tt.tagH6
            		|| node.tag == tt.tagDiv
            		|| node.tag == tt.tagNoscript
            		|| node.tag == tt.tagButton
            		|| node.tag == tt.tagTd
            		|| node.tag == tt.tagTr
            		|| node.tag == tt.tagCaption // TODO:
            		|| node.tag == tt.tagFigcaption // TODO:
            		|| node.tag == tt.tagScript
            		)
            {
            	for (node = node.content; node != null; node = node.next)
                {
                    if (node.type != Node.TEXT_NODE)
                    {
                        return true;
                    }
                }
                return false;
            }

            if (node.tag == tt.tagTitle)
            {
                return false;
            }
        }

        if (TidyUtils.toBoolean(node.tag.model & (Dict.CM_FIELD | Dict.CM_OBJECT)))
        {
            return true;
        }

        if (node.tag == tt.tagMap)
        {
            return true;
        }

        return !TidyUtils.toBoolean(node.tag.model & Dict.CM_INLINE);
    }

    /**
     * Print just the content of the body element. Useful when you want to reuse material from other documents.
     *
     * @param fout  where to write output
     * @param lexer the Lexer to use
     * @param root  the root node
     * @param xml   whether to write xml or not
     */
    void printBody(Out fout, Lexer lexer, Node root, boolean xml)
    {
        if (root == null)
        {
            return;
        }

        // Feature request #434940 - fix by Dave Raggett/Ignacio Vazquez-Abrams 21 Jun 01
        // Sebastiano Vigna <vigna@dsi.unimi.it>
        Node body = root.findBody(lexer.configuration.tt);

        if (body != null)
        {
            Node content;
            for (content = body.content; content != null; content = content.next)
            {
                printTree(fout, (short) 0, 0, lexer, content);
            }
        }
    }

    /**
     * @param fout   where to write output
     * @param mode   the mode to use
     * @param indent the indent amount
     * @param lexer  the Lexer to use
     * @param node   the node to use
     */
    public void printTree(Out fout, short mode, int indent, Lexer lexer, Node node)
    {
        Node content, last;
        TagTable tt = this.configuration.tt;

        if (node == null)
        {
            return;
        }

        if (node.type == Node.TEXT_NODE || (node.type == Node.CDATA_TAG && lexer.configuration.escapeCdata))
        {
            printText(fout, mode, indent, node.textarray, node.start, node.end);
        }
        else if (node.type == Node.COMMENT_TAG)
        {
            printComment(fout, indent, node);
        }
        else if (node.type == Node.ROOT_NODE)
        {
            for (content = node.content; content != null; content = content.next)
            {
                printTree(fout, mode, indent, lexer, content);
            }
        }
        else if (node.type == Node.DOCTYPE_TAG)
        {
            printDocType(fout, indent, lexer, node);
        }
        else if (node.type == Node.PROC_INS_TAG)
        {
            printPI(fout, indent, node);
        }
        else if (node.type == Node.XML_DECL)
        {
            printXmlDecl(fout, indent, node);
        }
        else if (node.type == Node.CDATA_TAG)
        {
            printCDATA(fout, indent, node);
        }
        else if (node.type == Node.SECTION_TAG)
        {
            printSection(fout, indent, node);
        }
        else if (node.type == Node.ASP_TAG)
        {
            printAsp(fout, indent, node);
        }
        else if (node.type == Node.JSTE_TAG)
        {
            printJste(fout, indent, node);
        }
        else if (node.type == Node.PHP_TAG)
        {
            printPhp(fout, indent, node);
        }
        else if (TidyUtils.toBoolean(node.tag.model & Dict.CM_EMPTY)
            || (node.type == Node.START_END_TAG && !configuration.xHTML))
        {
            if (!TidyUtils.toBoolean(node.tag.model & Dict.CM_INLINE))
            {
                condFlushLine(fout, indent);
            }

            if (node.tag == tt.tagBr
                && node.prev != null
                && node.prev.tag != tt.tagBr
                && this.configuration.breakBeforeBR)
            {
                flushLine(fout, indent);
            }

            if (this.configuration.makeClean && node.tag == tt.tagWbr)
            {
                printString(" ");
            }
            else
            {
                printTag(lexer, fout, mode, indent, node);
            }

            if (node.tag == tt.tagParam || node.tag == tt.tagArea)
            {
                condFlushLine(fout, indent);
            }
            else if (node.tag == tt.tagBr || node.tag == tt.tagHr
            		|| node.tag == tt.tagMeta
            		|| node.tag == tt.tagLink
                    || node.tag == tt.tagStyle
            		|| node.tag == tt.tagScript)
            {
                flushLine(fout, indent);
            }
        }
        else
        {
            if (node.type == Node.START_END_TAG)
            {
                node.type = Node.START_TAG;
            }

            // some kind of container element
            if (node.tag != null && node.tag.getParser() == ParserImpl.PRE)
            {
                condFlushLine(fout, indent);

                printTag(lexer, fout, mode, indent, node);
                //flushLine(fout, indent, false);
                condFlushLine(fout, indent);

                int oldIndent = indent;
                condFlushLine(fout, indent);
                indent = 0;

                for (content = node.content; content != null; content = content.next)
                {
                    printTree(fout, (short) (mode | PREFORMATTED | NOWRAP), indent, lexer, content);
                }

                condFlushLine(fout, indent, false);
                printEndTag(mode, indent, node);
                //flushLine(fout, indent, false);
                condFlushLine(fout, indent);

                if (!this.configuration.indentContent && node.next != null)
                {
                    flushLine(fout, indent);
                }
                indent = oldIndent;
            }
            else if (node.tag == tt.tagStyle || node.tag == tt.tagScript)
            {
                printScriptStyle(fout, (short) (mode | PREFORMATTED | NOWRAP | CDATA), indent, lexer, node);
            }
            else if (TidyUtils.toBoolean(node.tag.model & Dict.CM_INLINE))
            {
                if (this.configuration.makeClean)
                {
                    // discards <font> and </font> tags
                    if (node.tag == tt.tagFont)
                    {
                        for (content = node.content; content != null; content = content.next)
                        {
                            printTree(fout, mode, indent, lexer, content);
                        }
                        return;
                    }

                    // replace <nobr> ... </nobr> by &nbsp; or &#160; etc.
                    if (node.tag == tt.tagNobr)
                    {
                        for (content = node.content; content != null; content = content.next)
                        {
                            printTree(fout, (short) (mode | NOWRAP), indent, lexer, content);
                        }
                        return;
                    }
                }

                // otherwise a normal inline element

                printTag(lexer, fout, mode, indent, node);

                // indent content for SELECT, TEXTAREA, MAP, OBJECT and APPLET

                if (shouldIndent(node))
                {
                    condFlushLine(fout, indent);
                	boolean alreadyNewLine = true;
                    indent += this.configuration.spaces;

                    for (content = node.content; content != null; content = content.next)
                    {
                        printTree(fout, mode, indent, lexer, content);
                        alreadyNewLine = false;
                        if (content.next != null && content.type != Node.TEXT_NODE && content.tag != tt.tagLi) {
                        	condFlushLine(fout, indent);
                        	alreadyNewLine = true;
                        } else if (content.next != null && content.next.type != Node.TEXT_NODE && content.next.tag != null && shouldIndent(content.next)) {
                        	condFlushLine(fout, indent);
                        	alreadyNewLine = true;
                        }
                    }
                    if (!alreadyNewLine) {
                    	condFlushLine(fout, indent);
                    }
                    indent -= this.configuration.spaces;
                    if (!alreadyNewLine) {
                    	condFlushLine(fout, indent);
                    }
                }
                else
                {

                    for (content = node.content; content != null; content = content.next)
                    {
                        printTree(fout, mode, indent, lexer, content);
                        if (content.next != null && content.type != Node.TEXT_NODE && content.tag != tt.tagLi) {
                        	condFlushLine(fout, indent);
                        } else if (content.next != null && content.next.type != Node.TEXT_NODE && content.next.tag != null && shouldIndent(content.next)) {
                        	condFlushLine(fout, indent);
                        }
                    }
                }

                printEndTag(mode, indent, node);
                if (node.tag == tt.tagA)
                {
                    flushLine(fout, indent);
                }
            }
            else
            {
                // other tags
                condFlushLine(fout, indent);

                if (this.configuration.smartIndent && node.prev != null)
                {
                	if (node.prev.type == Node.TEXT_NODE && node.prev.start == node.prev.end) {
                		//System.out.println("Warning " + node.element + " has 0-length text!");
                	} else if (node.tag != tt.tagLi) {
                		flushLine(fout, indent);
                    }
                }

                // do not omit elements with attributes
                if (!this.configuration.hideEndTags
                    || !(node.tag != null && TidyUtils.toBoolean(node.tag.model & Dict.CM_OMITST))
                    || node.attributes != null)
                {
                    printTag(lexer, fout, mode, indent, node);

                    if (shouldIndent(node))
                    {
                        condFlushLine(fout, indent);
                    }
                    else if (TidyUtils.toBoolean(node.tag.model & Dict.CM_HTML)
                        || node.tag == tt.tagNoframes
                        || (TidyUtils.toBoolean(node.tag.model & Dict.CM_HEAD) && !(node.tag == tt.tagTitle)))
                    {
                        flushLine(fout, indent);
                    }
                }

                if (node.tag == tt.tagBody && this.configuration.burstSlides)
                {
                    printSlide(fout, mode, (this.configuration.indentContent
                        ? indent + this.configuration.spaces
                        : indent), lexer);
                }
                else
                {
                    last = null;

                    for (content = node.content; content != null; content = content.next)
                    {
                        // kludge for naked text before block level tag
                        if (last != null
                            && !this.configuration.indentContent
                            && last.type == Node.TEXT_NODE
                            && content.tag != null
                            && !TidyUtils.toBoolean(content.tag.model & Dict.CM_INLINE))
                        {
                            flushLine(fout, indent);
                        }

                        int currentIndent = (shouldIndent(node) ? indent + this.configuration.spaces : indent);
                        printTree(
                            fout,
                            mode,
                            currentIndent,
                            lexer,
                            content);

                        last = content;
                        if (content.next != null && content.type != Node.TEXT_NODE && content.tag != tt.tagLi) {
                        	condFlushLine(fout, currentIndent);
                        } else if (content.next != null && content.next.type != Node.TEXT_NODE && content.next.tag != null && shouldIndent(content.next)) {
                        	condFlushLine(fout, indent);
                        }
                    }
                }

                // don't flush line for td and th
                if (shouldIndent(node)
                    || ((TidyUtils.toBoolean(node.tag.model & Dict.CM_HTML) || node.tag == tt.tagNoframes || //
                    (TidyUtils.toBoolean(node.tag.model & Dict.CM_HEAD) && !(node.tag == tt.tagTitle))) && //
                    !this.configuration.hideEndTags))
                {
                    condFlushLine(
                        fout,
                        (this.configuration.indentContent ? indent + this.configuration.spaces : indent));

                    if (!this.configuration.hideEndTags || !TidyUtils.toBoolean(node.tag.model & Dict.CM_OPT))
                    {
                        printEndTag(mode, indent, node);

                        // #603128 tidy adds newslines after </html> tag
                        // Fix by Fabrizio Giustina 12-02-2004
                        // fix is different from the one in original tidy
                        if (!lexer.seenEndHtml)
                        {
                            flushLine(fout, indent);
                        }
                    }
                }
                else
                {
                    if (!this.configuration.hideEndTags || !TidyUtils.toBoolean(node.tag.model & Dict.CM_OPT))
                    {
                        printEndTag(mode, indent, node);
                    }

                    flushLine(fout, indent);
                }

                // FG commented out: double newlines
                // if (!this.configuration.indentContent
                // && node.next != null
                // && !this.configuration.hideEndTags
                // && (node.tag.model
                // & TidyUtils.toBoolean(Dict.CM_BLOCK | Dict.CM_TABLE | Dict.CM_LIST | Dict.CM_DEFLIST)))
                // {
                // flushLine(fout, indent);
                // }
            }
        }
    }

    /**
     * @param fout   where to write output
     * @param mode   the mode to use
     * @param indent the indent amount
     * @param lexer  the Lexer to use
     * @param node   the node to use
     */
    public void printXMLTree(Out fout, short mode, int indent, Lexer lexer, Node node)
    {
        TagTable tt = this.configuration.tt;

        if (node == null)
        {
            return;
        }

        if (node.type == Node.TEXT_NODE || (node.type == Node.CDATA_TAG && lexer.configuration.escapeCdata))
        {
            printText(fout, mode, indent, node.textarray, node.start, node.end);
        }
        else if (node.type == Node.COMMENT_TAG)
        {
            condFlushLine(fout, indent);
            printComment(fout, 0, node);
            condFlushLine(fout, 0);
        }
        else if (node.type == Node.ROOT_NODE)
        {
            Node content;

            for (content = node.content; content != null; content = content.next)
            {
                printXMLTree(fout, mode, indent, lexer, content);
            }
        }
        else if (node.type == Node.DOCTYPE_TAG)
        {
            printDocType(fout, indent, lexer, node);
        }
        else if (node.type == Node.PROC_INS_TAG)
        {
            printPI(fout, indent, node);
        }
        else if (node.type == Node.XML_DECL)
        {
            printXmlDecl(fout, indent, node);
        }
        else if (node.type == Node.CDATA_TAG)
        {
            printCDATA(fout, indent, node);
        }
        else if (node.type == Node.SECTION_TAG)
        {
            printSection(fout, indent, node);
        }
        else if (node.type == Node.ASP_TAG)
        {
            printAsp(fout, indent, node);
        }
        else if (node.type == Node.JSTE_TAG)
        {
            printJste(fout, indent, node);
        }
        else if (node.type == Node.PHP_TAG)
        {
            printPhp(fout, indent, node);
        }
        else if (TidyUtils.toBoolean(node.tag.model & Dict.CM_EMPTY)
            || node.type == Node.START_END_TAG
            && !configuration.xHTML)
        {
            condFlushLine(fout, indent);
            printTag(lexer, fout, mode, indent, node);
            // fgiust: Remove empty lines between tags in XML.
            // flushLine(fout, indent);

            // CPR: folks don't want so much vertical spacing in XML
            // if (node.next != null) { flushLine(fout, indent); }

        }
        else
        {
            // some kind of container element
            Node content;
            boolean mixed = false;
            int cindent;

            for (content = node.content; content != null; content = content.next)
            {
                if (content.type == Node.TEXT_NODE)
                {
                    mixed = true;
                    break;
                }
            }

            condFlushLine(fout, indent);

            if (ParserImpl.XMLPreserveWhiteSpace(node, tt))
            {
                indent = 0;
                cindent = 0;
                mixed = false;
            }
            else if (mixed)
            {
                cindent = indent;
            }
            else
            {
                cindent = indent + this.configuration.spaces;
            }

            printTag(lexer, fout, mode, indent, node);

            if (!mixed && node.content != null)
            {
                flushLine(fout, indent);
            }

            for (content = node.content; content != null; content = content.next)
            {
                printXMLTree(fout, mode, cindent, lexer, content);
            }

            if (!mixed && node.content != null)
            {
                condFlushLine(fout, cindent);
            }
            printEndTag(mode, indent, node);
            // condFlushLine(fout, indent);

            // CPR: folks don't want so much vertical spacing in XML
            // if (node.next != null) { flushLine(fout, indent); }

        }
    }

    /**
     * Split parse tree by h2 elements and output to separate files. Counts number of h2 children (if any) belonging to
     * node.
     *
     * @param node root node
     * @return number of slides (number of h2 elements)
     */
    public int countSlides(Node node)
    {
        // assume minimum of 1 slide
        int n = 1;

        TagTable tt = this.configuration.tt;

        // fix for [431716] avoid empty slides
        if (node != null && node.content != null && node.content.tag == tt.tagH2)
        {
            // "first" slide is empty, so ignore it
            n--;
        }

        if (node != null)
        {
            for (node = node.content; node != null; node = node.next)
            {
                if (node.tag == tt.tagH2)
                {
                    ++n;
                }
            }
        }

        return n;
    }

    /**
     * @param fout   where to write output
     * @param indent the indent amount
     */
    private void printNavBar(Out fout, int indent)
    {
        String buf;

        condFlushLine(fout, indent);
        printString("<center><small>");

        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMinimumIntegerDigits(3);

        if (slide > 1)
        {
            buf = "<a href=\"slide" + numberFormat.format(slide - 1) + ".html\">previous</a> | ";
            // #427666 - fix by Eric Rossen 02 Aug 00
            printString(buf);
            condFlushLine(fout, indent);

            if (slide < count)
            {
                printString("<a href=\"slide001.html\">start</a> | ");
                // #427666 - fix by Eric Rossen 02 Aug 00
            }
            else
            {
                printString("<a href=\"slide001.html\">start</a>");
                // #427666 - fix by Eric Rossen 02 Aug 00
            }

            condFlushLine(fout, indent);
        }

        if (slide < count)
        {
            buf = "<a href=\"slide" + numberFormat.format(slide + 1) + ".html\">next</a>";
            // #427666 - fix by Eric Rossen 02 Aug 00
            printString(buf);
        }

        printString("</small></center>");
        condFlushLine(fout, indent);
    }

    /**
     * Called from printTree to print the content of a slide from the node slide content.
     * On return slide content points to the node starting the next slide or null.
     * The variables slide and count are used to customise the navigation
     * bar.
     *
     * @param fout   where to write output
     * @param mode   the mode to use
     * @param indent the indent amount
     * @param lexer  the Lexer to use
     */
    public void printSlide(Out fout, short mode, int indent, Lexer lexer)
    {
        Node content, last;
        TagTable tt = this.configuration.tt;

        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMinimumIntegerDigits(3);

        /* insert div for onclick handler */
        String s;
        s = "<div onclick=\"document.location='slide"
            + numberFormat.format(slide < count ? slide + 1 : 1)
            + ".html'\">";
        // #427666 - fix by Eric Rossen 02 Aug 00
        printString(s);
        condFlushLine(fout, indent);

        /* first print the h2 element and navbar */
        if (slidecontent != null && slidecontent.tag == tt.tagH2)
        {
            printNavBar(fout, indent);

            /* now print an hr after h2 */

            addC('<', linelen++);

            addC(TidyUtils.foldCase('h', this.configuration.upperCaseTags, this.configuration.xmlTags), linelen++);
            addC(TidyUtils.foldCase('r', this.configuration.upperCaseTags, this.configuration.xmlTags), linelen++);

            if (this.configuration.xmlOut)
            {
                printString(" />");
            }
            else
            {
                addC('>', linelen++);
            }

            if (this.configuration.indentContent)
            {
                condFlushLine(fout, indent);
            }

            // PrintVertSpacer(fout, indent);

            // condFlushLine(fout, indent);

            // print the h2 element
            printTree(
                fout,
                mode,
                (this.configuration.indentContent ? indent + this.configuration.spaces : indent),
                lexer,
                slidecontent);

            slidecontent = slidecontent.next;
        }

        // now continue until we reach the next h2

        last = null;
        content = slidecontent;

        for (; content != null; content = content.next)
        {
            if (content.tag == tt.tagH2)
            {
                break;
            }

            // kludge for naked text before block level tag
            if (last != null
                && !this.configuration.indentContent
                && last.type == Node.TEXT_NODE
                && content.tag != null
                && TidyUtils.toBoolean(content.tag.model & Dict.CM_BLOCK))
            {
                flushLine(fout, indent);
                flushLine(fout, indent);
            }

            printTree(
                fout,
                mode,
                (this.configuration.indentContent ? indent + this.configuration.spaces : indent),
                lexer,
                content);

            last = content;
        }

        slidecontent = content;

        // now print epilog

        condFlushLine(fout, indent);

        printString("<br clear=\"all\">");
        condFlushLine(fout, indent);

        addC('<', linelen++);

        addC(TidyUtils.foldCase('h', this.configuration.upperCaseTags, this.configuration.xmlTags), linelen++);
        addC(TidyUtils.foldCase('r', this.configuration.upperCaseTags, this.configuration.xmlTags), linelen++);

        if (this.configuration.xmlOut)
        {
            printString(" />");
        }
        else
        {
            addC('>', linelen++);
        }

        if (this.configuration.indentContent)
        {
            condFlushLine(fout, indent);
        }

        printNavBar(fout, indent);

        // end tag for div
        printString("</div>");
        condFlushLine(fout, indent);
    }

    /**
     * Add meta element for page transition effect, this works on IE but not NS.
     *
     * @param lexer    the Lexer instance to use
     * @param root     the root node
     * @param duration transition duration
     */
    public void addTransitionEffect(Lexer lexer, Node root, double duration)
    {
        Node head = root.findHEAD(lexer.configuration.tt);
        String transition;

        transition = "blendTrans(Duration=" + (new Double(duration)).toString() + ")";

        if (head != null)
        {
            Node meta = lexer.inferredTag("meta");
            meta.addAttribute("http-equiv", "Page-Enter");
            meta.addAttribute("content", transition);
            head.insertNodeAtStart(meta);
        }
    }

    /**
     * Creates slides from h2.
     *
     * @param lexer Lexer
     * @param root  root node
     */
    public void createSlides(Lexer lexer, Node root)
    {
        final File slidesDir = createSlidesDir();
        Node body;
        File slideFile;

        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMinimumIntegerDigits(3);

        body = root.findBody(lexer.configuration.tt);
        count = countSlides(body);
        slidecontent = body.content;

        addTransitionEffect(lexer, root, 3.0);

        for (slide = 1; slide <= count; ++slide)
        {
            slideFile = createSlideFile(slidesDir, numberFormat);

            try
            {
                FileOutputStream fis = new FileOutputStream(slideFile);
                Out out = OutFactory.getOut(configuration, fis);

                printTree(out, (short) 0, 0, lexer, root);
                flushLine(out, 0);

                fis.close();
            }
            catch (IOException e)
            {
                System.err.println(slideFile + e.toString());
            }
        }

        // delete superfluous slides by deleting slideN.html for N = count+1, count+2, etc.
        // until no such file is found.

        // #427666 - fix by Eric Rossen 02 Aug 00
        while (createSlideFile(slidesDir, numberFormat).delete())
        {
            ++slide;
        }
    }

    /**
     * Create the slides output directory if necessary.
     *
     * @return the path to the directory created or null if not set
     */
    private File createSlidesDir()
    {
        final String path = System.getProperty("org.jtidy.slides.dir");
        if (path == null || path.isEmpty())
            return null;
        final File d = new File(path);
        d.mkdirs();
        return d;
    }

    /**
     * Create slide file name with optional parent directory.
     *
     * @param parent the parent directory to use or null for working directory
     * @param frm    the number format to use for file name
     * @return file instance
     */
    private File createSlideFile(final File parent, final NumberFormat frm)
    {
        final String fname = "slide" + frm.format(slide) + ".html";
        if (parent == null)
        {
            return new File(fname);
        }
        else
        {
            return new File(parent, fname);
        }
    }

}
