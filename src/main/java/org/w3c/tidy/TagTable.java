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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Tag dictionary node hash table.
 *
 * @author Dave Raggett <a href="mailto:dsr@w3.org">dsr@w3.org </a>
 * @author Andy Quick <a href="mailto:ac.quick@sympatico.ca">ac.quick@sympatico.ca </a> (translation to Java)
 * @author Fabrizio Giustina
 * @version $Revision$ ($Author$)
 */
public final class TagTable
{

    /**
     * dummy entry for all xml tags.
     */
    public static final Dict XML_TAGS = new Dict(null, Dict.VERS_ALL, Dict.CM_BLOCK, null, null);

    /**
     * all the known tags.
     */
    private static final Dict[] TAGS = {
        new Dict(
            "html",
            Dict.VERS_ALL,
            (Dict.CM_HTML | Dict.CM_OPT | Dict.CM_OMITST),
            ParserImpl.HTML,
            TagCheckImpl.HTML),
        new Dict("head", Dict.VERS_ALL, (Dict.CM_HTML | Dict.CM_OPT | Dict.CM_OMITST), ParserImpl.HEAD, null),
        new Dict("title", Dict.VERS_ALL, Dict.CM_HEAD | Dict.CM_BLOCK, ParserImpl.TITLE, null),
        new Dict("base", Dict.VERS_ALL, (Dict.CM_HEAD | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        //new Dict("link", Dict.VERS_ALL, (Dict.CM_HEAD | Dict.CM_EMPTY), ParserImpl.EMPTY, TagCheckImpl.LINK),
        new Dict("link", Dict.VERS_ALL, (Dict.CM_HEAD | Dict.CM_BLOCK | Dict.CM_EMPTY), ParserImpl.EMPTY, TagCheckImpl.LINK),
        new Dict("meta", Dict.VERS_ALL, (Dict.CM_HEAD | Dict.CM_BLOCK | Dict.CM_EMPTY), ParserImpl.EMPTY, TagCheckImpl.META),
        new Dict(
            "style",
            Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC),
            //Dict.CM_HEAD,
            (Dict.CM_HEAD | Dict.CM_BLOCK),
            ParserImpl.SCRIPT,
            TagCheckImpl.STYLE),
        new Dict(
            "script",
            Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC),
            (Dict.CM_HEAD | Dict.CM_MIXED | Dict.CM_BLOCK),
            ParserImpl.SCRIPT,
            TagCheckImpl.SCRIPT),
        new Dict(
            "server",
            Dict.VERS_NETSCAPE,
            (Dict.CM_HEAD | Dict.CM_MIXED | Dict.CM_BLOCK | Dict.CM_INLINE),
            ParserImpl.SCRIPT,
            null),
        new Dict("body", Dict.VERS_ALL, (Dict.CM_HTML | Dict.CM_OPT | Dict.CM_OMITST), ParserImpl.BODY, null),
        new Dict("frameset", Dict.VERS_FRAMESET, (Dict.CM_HTML | Dict.CM_FRAMES), ParserImpl.FRAMESET, null),
        new Dict("p", Dict.VERS_ALL, (Dict.CM_BLOCK | Dict.CM_OPT), ParserImpl.INLINE, null),
        new Dict("h1", Dict.VERS_ALL, (Dict.CM_BLOCK | Dict.CM_HEADING), ParserImpl.INLINE, null),
        new Dict("h2", Dict.VERS_ALL, (Dict.CM_BLOCK | Dict.CM_HEADING), ParserImpl.INLINE, null),
        new Dict("h3", Dict.VERS_ALL, (Dict.CM_BLOCK | Dict.CM_HEADING), ParserImpl.INLINE, null),
        new Dict("h4", Dict.VERS_ALL, (Dict.CM_BLOCK | Dict.CM_HEADING), ParserImpl.INLINE, null),
        new Dict("h5", Dict.VERS_ALL, (Dict.CM_BLOCK | Dict.CM_HEADING), ParserImpl.INLINE, null),
        new Dict("h6", Dict.VERS_ALL, (Dict.CM_BLOCK | Dict.CM_HEADING), ParserImpl.INLINE, null),
        new Dict("ul", Dict.VERS_ALL, Dict.CM_BLOCK, ParserImpl.LIST, null),
        new Dict("ol", Dict.VERS_ALL, Dict.CM_BLOCK, ParserImpl.LIST, null),
        new Dict("dl", Dict.VERS_ALL, Dict.CM_BLOCK, ParserImpl.DEFLIST, null),
        new Dict("dir", Dict.VERS_LOOSE, (Dict.CM_BLOCK | Dict.CM_OBSOLETE), ParserImpl.LIST, null),
        new Dict("menu", Dict.VERS_LOOSE, (Dict.CM_BLOCK | Dict.CM_OBSOLETE), ParserImpl.LIST, null),
        new Dict("pre", Dict.VERS_ALL, Dict.CM_BLOCK, ParserImpl.PRE, null),
        new Dict("listing", Dict.VERS_ALL, (Dict.CM_BLOCK | Dict.CM_OBSOLETE), ParserImpl.PRE, null),
        new Dict("xmp", Dict.VERS_ALL, (Dict.CM_BLOCK | Dict.CM_OBSOLETE), ParserImpl.PRE, null),
        new Dict("plaintext", Dict.VERS_ALL, (Dict.CM_BLOCK | Dict.CM_OBSOLETE), ParserImpl.PRE, null),
        new Dict("address", Dict.VERS_ALL, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("blockquote", Dict.VERS_ALL, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("form", Dict.VERS_ALL, Dict.CM_BLOCK, ParserImpl.BLOCK, TagCheckImpl.FORM),
        new Dict("isindex", Dict.VERS_LOOSE, (Dict.CM_BLOCK | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict("fieldset", Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC), Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("table", Dict.VERS_FROM32, Dict.CM_BLOCK, ParserImpl.TABLETAG, TagCheckImpl.TABLE),
        new Dict(
            "hr",
            Dict.without(Dict.VERS_ALL, Dict.VERS_BASIC),
            (Dict.CM_BLOCK | Dict.CM_EMPTY),
            ParserImpl.EMPTY,
            TagCheckImpl.HR),
        new Dict("div", Dict.VERS_FROM32, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("multicol", Dict.VERS_NETSCAPE, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("nosave", Dict.VERS_NETSCAPE, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("layer", Dict.VERS_NETSCAPE, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("ilayer", Dict.VERS_NETSCAPE, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict(
            "nolayer",
            Dict.VERS_NETSCAPE,
            (Dict.CM_BLOCK | Dict.CM_INLINE | Dict.CM_MIXED),
            ParserImpl.BLOCK,
            null),
        new Dict("align", Dict.VERS_NETSCAPE, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("center", Dict.VERS_LOOSE, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict(
            "ins",
            Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC),
            (Dict.CM_INLINE | Dict.CM_BLOCK | Dict.CM_MIXED),
            ParserImpl.INLINE,
            null),
        new Dict(
            "del",
            Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC),
            (Dict.CM_INLINE | Dict.CM_BLOCK | Dict.CM_MIXED),
            ParserImpl.INLINE,
            null),
        new Dict("li", Dict.VERS_ALL, (Dict.CM_LIST | Dict.CM_OPT | Dict.CM_NO_INDENT), ParserImpl.BLOCK, null),
        new Dict("dt", Dict.VERS_ALL, (Dict.CM_DEFLIST | Dict.CM_OPT | Dict.CM_NO_INDENT), ParserImpl.INLINE, null),
        new Dict("dd", Dict.VERS_ALL, (Dict.CM_DEFLIST | Dict.CM_OPT | Dict.CM_NO_INDENT), ParserImpl.BLOCK, null),
        new Dict("caption", Dict.VERS_FROM32, Dict.CM_TABLE, ParserImpl.INLINE, TagCheckImpl.CAPTION),
        new Dict("colgroup", Dict.VERS_HTML40, (Dict.CM_TABLE | Dict.CM_OPT), ParserImpl.COLGROUP, null),
        new Dict("col", Dict.VERS_HTML40, (Dict.CM_TABLE | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict(
            "thead",
            Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC),
            (Dict.CM_TABLE | Dict.CM_ROWGRP | Dict.CM_OPT),
            ParserImpl.ROWGROUP,
            null),
        new Dict(
            "tfoot",
            Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC),
            (Dict.CM_TABLE | Dict.CM_ROWGRP | Dict.CM_OPT),
            ParserImpl.ROWGROUP,
            null),
        new Dict(
            "tbody",
            Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC),
            (Dict.CM_TABLE | Dict.CM_ROWGRP | Dict.CM_OPT),
            ParserImpl.ROWGROUP,
            null),
        new Dict("tr", Dict.VERS_FROM32, (Dict.CM_TABLE | Dict.CM_OPT), ParserImpl.ROW, null),
        new Dict(
            "td",
            Dict.VERS_FROM32,
            (Dict.CM_ROW | Dict.CM_OPT | Dict.CM_NO_INDENT),
            ParserImpl.BLOCK,
            TagCheckImpl.TABLECELL),
        new Dict(
            "th",
            Dict.VERS_FROM32,
            (Dict.CM_ROW | Dict.CM_OPT | Dict.CM_NO_INDENT),
            ParserImpl.BLOCK,
            TagCheckImpl.TABLECELL),
        new Dict("q", Dict.VERS_HTML40, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("a", Dict.VERS_ALL, Dict.CM_INLINE, ParserImpl.INLINE, TagCheckImpl.ANCHOR),
        new Dict("br", Dict.VERS_ALL, (Dict.CM_INLINE | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict(
            "img",
            Dict.VERS_ALL,
            (Dict.CM_INLINE | Dict.CM_IMG | Dict.CM_EMPTY),
            ParserImpl.EMPTY,
            TagCheckImpl.IMG),
        new Dict(
            "object",
            Dict.VERS_HTML40,
            (Dict.CM_OBJECT | Dict.CM_HEAD | Dict.CM_IMG | Dict.CM_INLINE | Dict.CM_PARAM),
            ParserImpl.BLOCK,
            null),
        new Dict(
            "applet",
            Dict.VERS_LOOSE,
            (Dict.CM_OBJECT | Dict.CM_IMG | Dict.CM_INLINE | Dict.CM_PARAM),
            ParserImpl.BLOCK,
            null),
        new Dict(
            "servlet",
            Dict.VERS_SUN,
            (Dict.CM_OBJECT | Dict.CM_IMG | Dict.CM_INLINE | Dict.CM_PARAM),
            ParserImpl.BLOCK,
            null),
        new Dict("param", Dict.VERS_FROM32, (Dict.CM_INLINE | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict("embed", Dict.VERS_NETSCAPE, (Dict.CM_INLINE | Dict.CM_IMG | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict("noembed", Dict.VERS_NETSCAPE, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("iframe", Dict.VERS_HTML40_LOOSE, Dict.CM_INLINE, ParserImpl.BLOCK, null),
        new Dict("frame", Dict.VERS_FRAMESET, (Dict.CM_FRAMES | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict("noframes", Dict.VERS_IFRAME, (Dict.CM_BLOCK | Dict.CM_FRAMES), ParserImpl.NOFRAMES, null),
        new Dict(
            "noscript",
            Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC),
            (Dict.CM_BLOCK | Dict.CM_INLINE | Dict.CM_MIXED),
            ParserImpl.BLOCK,
            null),
        new Dict("b", Dict.without(Dict.VERS_ALL, Dict.VERS_BASIC), Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("i", Dict.without(Dict.VERS_ALL, Dict.VERS_BASIC), Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("u", Dict.VERS_LOOSE, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("tt", Dict.without(Dict.VERS_ALL, Dict.VERS_BASIC), Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("s", Dict.VERS_LOOSE, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("strike", Dict.VERS_LOOSE, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("big", Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC), Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("small", Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC), Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("sub", Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC), Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("sup", Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC), Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("em", Dict.VERS_ALL, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("strong", Dict.VERS_ALL, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("mark", Dict.VERS_ALL, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("dfn", Dict.VERS_ALL, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("code", Dict.VERS_ALL, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("samp", Dict.VERS_ALL, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("kbd", Dict.VERS_ALL, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("var", Dict.VERS_ALL, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("cite", Dict.VERS_ALL, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("abbr", Dict.VERS_HTML40, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("acronym", Dict.VERS_HTML40, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("span", Dict.VERS_FROM32, Dict.CM_INLINE, ParserImpl.INLINE, null),
        //new Dict("span", Dict.VERS_FROM32, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("blink", Dict.VERS_PROPRIETARY, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("nobr", Dict.VERS_PROPRIETARY, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("wbr", Dict.VERS_PROPRIETARY, (Dict.CM_INLINE | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict("marquee", Dict.VERS_MICROSOFT, (Dict.CM_INLINE | Dict.CM_OPT), ParserImpl.INLINE, null),
        new Dict("bgsound", Dict.VERS_MICROSOFT, (Dict.CM_HEAD | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict("comment", Dict.VERS_MICROSOFT, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("spacer", Dict.VERS_NETSCAPE, (Dict.CM_INLINE | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict("keygen", Dict.VERS_NETSCAPE, (Dict.CM_INLINE | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict(
            "nolayer",
            Dict.VERS_NETSCAPE,
            (Dict.CM_BLOCK | Dict.CM_INLINE | Dict.CM_MIXED),
            ParserImpl.BLOCK,
            null),
        new Dict("ilayer", Dict.VERS_NETSCAPE, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict(
            "map",
            Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC),
            Dict.CM_INLINE,
            ParserImpl.BLOCK,
            TagCheckImpl.MAP),
        new Dict(
            "area",
            Dict.without(Dict.VERS_ALL, Dict.VERS_BASIC),
            (Dict.CM_BLOCK | Dict.CM_EMPTY),
            ParserImpl.EMPTY,
            TagCheckImpl.AREA),
        //new Dict("input", Dict.VERS_ALL, (Dict.CM_INLINE | Dict.CM_IMG | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict("input", Dict.VERS_ALL, (Dict.CM_BLOCK | Dict.CM_IMG | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict("select", Dict.VERS_ALL, (Dict.CM_INLINE | Dict.CM_FIELD), ParserImpl.SELECT, null),
        new Dict("option", Dict.VERS_ALL, (Dict.CM_FIELD | Dict.CM_OPT), ParserImpl.TEXT, null),
        new Dict(
            "optgroup",
            Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC),
            (Dict.CM_FIELD | Dict.CM_OPT),
            ParserImpl.OPTGROUP,
            null),
        new Dict("textarea", Dict.VERS_ALL, (Dict.CM_INLINE | Dict.CM_FIELD), ParserImpl.TEXT, null),
        //new Dict("label", Dict.VERS_HTML40, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("label", Dict.VERS_HTML40, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("legend", Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC), Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("button", Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC), Dict.CM_INLINE, ParserImpl.INLINE, null),
        //new Dict("button", Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC), Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("basefont", Dict.VERS_LOOSE, (Dict.CM_INLINE | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict("font", Dict.VERS_LOOSE, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("bdo", Dict.without(Dict.VERS_HTML40, Dict.VERS_BASIC), Dict.CM_INLINE, ParserImpl.INLINE, null),
        // elements for XHTML 1.1
        new Dict("ruby", Dict.VERS_HTML5, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("rbc", Dict.VERS_XHTML11, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("rtc", Dict.VERS_XHTML11, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("rb", Dict.VERS_HTML5, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("rt", Dict.VERS_HTML5, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("", Dict.VERS_XHTML11, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("rp", Dict.VERS_HTML5, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("article", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("aside", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("bdi", Dict.VERS_HTML5, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("details", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("dialog", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("figcaption", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("figure", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("footer", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("header", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        //new Dict("main", Dict.VERS_HTML5, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("main", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("menuitem", Dict.VERS_HTML5, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("meter", Dict.VERS_HTML5, Dict.CM_INLINE, ParserImpl.INLINE, null),
        //new Dict("nav", Dict.VERS_HTML5, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("nav", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("progress", Dict.VERS_HTML5, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("section", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("summary", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("time", Dict.VERS_HTML5, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("wbr", Dict.VERS_XHTML11, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("datalist", Dict.VERS_XHTML11, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("output", Dict.VERS_XHTML11, Dict.CM_INLINE, ParserImpl.INLINE, null),
        new Dict("audio", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("video", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("canvas", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("svg", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("source", Dict.VERS_HTML5,  (Dict.CM_BLOCK | Dict.CM_EMPTY), ParserImpl.EMPTY, null),
        new Dict("track", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("embed", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
        new Dict("picture", Dict.VERS_HTML5, Dict.CM_BLOCK, ParserImpl.BLOCK, null),
    };

    /**
     * html tag.
     */
    protected Dict tagHtml;

    /**
     * head tag.
     */
    protected Dict tagHead;

    /**
     * body tag.
     */
    protected Dict tagBody;

    /**
     * frameset tag.
     */
    protected Dict tagFrameset;

    /**
     * frame tag.
     */
    protected Dict tagFrame;

    /**
     * iframe tag.
     */
    protected Dict tagIframe;

    /**
     * noframes tag.
     */
    protected Dict tagNoframes;

    /**
     * meta tag.
     */
    protected Dict tagMeta;

    /**
     * title tag.
     */
    protected Dict tagTitle;

    /**
     * base tag.
     */
    protected Dict tagBase;

    /**
     * hr tag.
     */
    protected Dict tagHr;

    /**
     * pre tag.
     */
    protected Dict tagPre;

    /**
     * listing tag.
     */
    protected Dict tagListing;

    /**
     * h1 tag.
     */
    protected Dict tagH1;

    /**
     * h2 tag.
     */
    protected Dict tagH2;
    protected Dict tagH3;
    protected Dict tagH4;
    protected Dict tagH5;
    protected Dict tagH6;

    /**
     * p tag.
     */
    protected Dict tagP;

    /**
     * ul tag.
     */
    protected Dict tagUl;

    /**
     * ol tag.
     */
    protected Dict tagOl;

    /**
     * dir tag.
     */
    protected Dict tagDir;

    /**
     * li tag.
     */
    protected Dict tagLi;

    /**
     * dt tag.
     */
    protected Dict tagDt;

    /**
     * dd tag.
     */
    protected Dict tagDd;

    /**
     * dl tag.
     */
    protected Dict tagDl;

    /**
     * td tag.
     */
    protected Dict tagTd;

    /**
     * th tag.
     */
    protected Dict tagTh;

    /**
     * tr tag.
     */
    protected Dict tagTr;

    /**
     * col tag.
     */
    protected Dict tagCol;

    /**
     * colgroup tag.
     */
    protected Dict tagColgroup;

    /**
     * br tag.
     */
    protected Dict tagBr;

    /**
     * a tag.
     */
    protected Dict tagA;

    /**
     * link tag.
     */
    protected Dict tagLink;

    /**
     * b tag.
     */
    protected Dict tagB;

    /**
     * i tag.
     */
    protected Dict tagI;
    protected Dict tagS;
    protected Dict tagStrike;

    /**
     * strong tag.
     */
    protected Dict tagStrong;
    protected Dict tagSub;
    protected Dict tagSup;

    protected Dict tagButton;

    /**
     * mark tag.
     */
    protected Dict tagMark;

    /**
     * em tag.
     */
    protected Dict tagEm;

    /**
     * big tag.
     */
    protected Dict tagBig;

    /**
     * small tag.
     */
    protected Dict tagSmall;

    /**
     * param tag.
     */
    protected Dict tagParam;

    /**
     * option tag.
     */
    protected Dict tagOption;

    /**
     * optgroup tag.
     */
    protected Dict tagOptgroup;

    /**
     * img tag.
     */
    protected Dict tagImg;

    /**
     * map tag.
     */
    protected Dict tagMap;

    /**
     * area tag.
     */
    protected Dict tagArea;

    /**
     * nobr tag.
     */
    protected Dict tagNobr;

    /**
     * font tag.
     */
    protected Dict tagFont;

    /**
     * spacer tag.
     */
    protected Dict tagSpacer;

    /**
     * layer tag.
     */
    protected Dict tagLayer;

    /**
     * center tag.
     */
    protected Dict tagCenter;

    /**
     * style tag.
     */
    protected Dict tagStyle;

    /**
     * script tag.
     */
    protected Dict tagScript;

    /**
     * noscript tag.
     */
    protected Dict tagNoscript;

    /**
     * table tag.
     */
    protected Dict tagTable;

    /**
     * caption tag.
     */
    protected Dict tagCaption;

    /**
     * form tag.
     */
    protected Dict tagForm;

    /**
     * textarea tag.
     */
    protected Dict tagTextarea;

    /**
     * blockquote tag.
     */
    protected Dict tagBlockquote;

    /**
     * applet tag.
     */
    protected Dict tagApplet;

    /**
     * object tag.
     */
    protected Dict tagObject;

    /**
     * div tag.
     */
    protected Dict tagDiv;

    /**
     * span tag.
     */
    protected Dict tagSpan;
    protected Dict tagCite;
    protected Dict tagCode;

    /**
     * input tag.
     */
    protected Dict tagInput;

    /**
     * tag.
     */
    protected Dict tagQ;

    /**
     * article tag.
     */
    protected Dict tagArticle;

    /**
     * aside tag.
     */
    protected Dict tagAside;

    /**
     * bdi tag.  Inline Right-to-Left text
     */
    protected Dict tagBdi;

    /**
     * details tag.
     */
    protected Dict tagDetails;

    /**
     * dialog tag.
     */
    protected Dict tagDialog;

    /**
     * figcaption tag.
     */
    protected Dict tagFigcaption;

    /**
     * figure tag.
     */
    protected Dict tagFigure;

    /**
     * footer tag.
     */
    protected Dict tagFooter;

    /**
     * header tag.
     */
    protected Dict tagHeader;

    /**
     * main tag.
     */
    protected Dict tagMain;

    /**
     * menuitem tag.
     */
    protected Dict tagMenuitem;

    /**
     * meter tag.
     */
    protected Dict tagMeter;

    /**
     * nav tag.
     */
    protected Dict tagNav;

    protected Dict tagLabel;

    /**
     * progress tag.
     */
    protected Dict tagProgress;

    /**
     * rp tag. Defines what to show in browsers that do not support ruby annotations
     */
    protected Dict tagRp;

    /**
     * rt tag. Defines an explanation/pronunciation of characters (for East Asian typography)
     */
    protected Dict tagRt;

    /**
     * ruby tag. Defines a ruby annotation (for East Asian typography)
     */
    protected Dict tagRuby;

    /**
     * section tag. Defines a section in the document
     */
    protected Dict tagSection;

    /**
     * summary tag. Defines a visible heading for a &lt;details&gt; element
     */
    protected Dict tagSummary;

    /**
     * time tag. Defines a date/time
     */
    protected Dict tagTime;

    /**
     * wbr tag. Defines a possible line-break
     */
    protected Dict tagWbr;

    /**
     * datalist tag. Defines pre-defined options for input controls
     */
    protected Dict tagDatalist;

    /**
     * output tag.
     */
    protected Dict tagOutput;

    /**
     * svg tag. Draw scalable vector graphics
     */
    protected Dict tagSvg;

    /**
     * canvas tag. Draw graphics, on the fly, via scripting (usually JavaScript)
     */
    protected Dict tagCanvas;

    /**
     * audio tag. Defines sound content
     */
    protected Dict tagAudio;

    /**
     * embed tag. Defines containers for external applications (like plug-ins)
     */
    protected Dict tagEmbed;

    /**
     * audio tag. Defines sources for &lt;video&gt; and &lt;audio&gt;
     */
    protected Dict tagSource;

    /**
     * audio tag. Defines tracks for &lt;video&gt; and &lt;audio&gt;
     */
    protected Dict tagTrack;

    /**
     * audio tag. Defines video or movie content
     */
    protected Dict tagVideo;

    /**
     * audio tag. Defines video or movie content
     */
    protected Dict tagPicture;

    /**
     * a proprietary tag added by Tidy, along with tag_nobr, tag_wbr.
     */
    protected Dict tagBlink;

    /**
     * anchor/node hash.
     */
    protected Anchor anchorList;

    /**
     * configuration.
     */
    private Configuration configuration;

    /**
     * hashTable containing tags.
     */
    private Map<String, Dict> tagHashtable = new HashMap<>();

    /**
     * Instantiates a new tag table with known tags.
     */
    protected TagTable()
    {
        for (Dict TAG : TAGS)
        {
            install(TAG);
        }
        tagHtml = lookup("html");
        tagHead = lookup("head");
        tagBody = lookup("body");
        tagFrameset = lookup("frameset");
        tagFrame = lookup("frame");
        tagIframe = lookup("iframe");
        tagNoframes = lookup("noframes");
        tagMeta = lookup("meta");
        tagTitle = lookup("title");
        tagBase = lookup("base");
        tagHr = lookup("hr");
        tagPre = lookup("pre");
        tagListing = lookup("listing");
        tagH1 = lookup("h1");
        tagH2 = lookup("h2");
        tagH3 = lookup("h3");
        tagH4 = lookup("h4");
        tagH5 = lookup("h5");
        tagH6 = lookup("h6");
        tagP = lookup("p");
        tagUl = lookup("ul");
        tagOl = lookup("ol");
        tagDir = lookup("dir");
        tagLi = lookup("li");
        tagDt = lookup("dt");
        tagDd = lookup("dd");
        tagDl = lookup("dl");
        tagTd = lookup("td");
        tagTh = lookup("th");
        tagTr = lookup("tr");
        tagCol = lookup("col");
        tagColgroup = lookup("colgroup");
        tagBr = lookup("br");
        tagA = lookup("a");
        tagLink = lookup("link");
        tagB = lookup("b");
        tagI = lookup("i");
        tagS = lookup("s");
        tagStrike = lookup("strike");
        tagStrong = lookup("strong");
        tagSup = lookup("sup");
        tagSub = lookup("sub");
        tagButton = lookup("button");
        tagEm = lookup("em");
        tagBig = lookup("big");
        tagSmall = lookup("small");
        tagParam = lookup("param");
        tagOption = lookup("option");
        tagOptgroup = lookup("optgroup");
        tagImg = lookup("img");
        tagMap = lookup("map");
        tagArea = lookup("area");
        tagNobr = lookup("nobr");
        tagWbr = lookup("wbr");
        tagFont = lookup("font");
        tagSpacer = lookup("spacer");
        tagLayer = lookup("layer");
        tagCenter = lookup("center");
        tagStyle = lookup("style");
        tagScript = lookup("script");
        tagNoscript = lookup("noscript");
        tagTable = lookup("table");
        tagCaption = lookup("caption");
        tagForm = lookup("form");
        tagTextarea = lookup("textarea");
        tagBlockquote = lookup("blockquote");
        tagApplet = lookup("applet");
        tagObject = lookup("object");
        tagDiv = lookup("div");
        tagSpan = lookup("span");
        tagCite = lookup("cite");
        tagCode = lookup("code");
        tagInput = lookup("input");
        tagQ = lookup("q");
        tagBlink = lookup("blink");
        tagArticle = lookup("article");
        tagAside = lookup("aside");
        tagBdi = lookup("bdi");
        tagDetails = lookup("details");
        tagDialog = lookup("dialog");
        tagFigcaption = lookup("figcaption");
        tagFigure = lookup("figure");
        tagFooter = lookup("footer");
        tagHeader = lookup("header");
        tagMain = lookup("main");
        tagMark = lookup("mark");
        tagMenuitem = lookup("menuitem");
        tagMeter = lookup("meter");
        tagNav = lookup("nav");
        tagLabel = lookup("label");
        tagProgress = lookup("progress");
        tagRp = lookup("rp");
        tagRt = lookup("rt");
        tagRuby = lookup("ruby");
        tagSection = lookup("section");
        tagSummary = lookup("summary");
        tagTime = lookup("time");
        tagDatalist = lookup("datalist");
        tagOutput = lookup("output");
        tagAudio = lookup("audio");
        tagEmbed = lookup("embed");
        tagSource = lookup("source");
        tagTrack = lookup("track");
        tagVideo = lookup("video");
        tagCanvas = lookup("canvas");
        tagSvg = lookup("svg");
        tagPicture = lookup("picture");
    }

    /**
     * Setter for the current configuration instance.
     *
     * @param configuration configuration instance
     */
    public void setConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }

    /**
     * Lookup a tag definition by its name.
     *
     * @param name tag name
     * @return tag definition (Dict)
     */
    public Dict lookup(String name)
    {
        return tagHashtable.get(name);
    }

    /**
     * Installs a new tag in the tag table, or modify an existing one.
     *
     * @param dict tag definition
     * @return installed Dict instance
     */
    public Dict install(Dict dict)
    {
        Dict d = tagHashtable.get(dict.name);
        if (d != null)
        {
            d.versions = dict.versions;
            d.model |= dict.model;
            d.setParser(dict.getParser());
            d.setChkattrs(dict.getChkattrs());
            return d;
        }

        tagHashtable.put(dict.name, dict);
        return dict;

    }

    /**
     * Finds a tag by name.
     *
     * @param node Node to find. If the element is found the tag property of node will be set.
     * @return true if the tag is found, false otherwise
     */
    public boolean findTag(Node node)
    {
        Dict np;

        if (configuration != null && configuration.xmlTags)
        {
            node.tag = XML_TAGS;
            return true;
        }

        if (node.element != null)
        {
            np = lookup(node.element);
            if (np != null)
            {
                node.tag = np;
                return true;
            }
        }

        return false;
    }

    /**
     * Finds a parser fo the given node.
     *
     * @param node Node
     * @return parser for the node
     */
    public Parser findParser(Node node)
    {
        Dict np;

        if (node.element != null)
        {
            np = lookup(node.element);
            if (np != null)
            {
                return np.getParser();
            }
        }

        return null;
    }

    /**
     * May id or name serve as anchor?
     *
     * @param node Node
     * @return <code>true</code> if tag can serve as an anchor
     */
    boolean isAnchorElement(Node node)
    {
        return node.tag == this.tagA
            || node.tag == this.tagApplet
            || node.tag == this.tagForm
            || node.tag == this.tagFrame
            || node.tag == this.tagIframe
            || node.tag == this.tagImg
            || node.tag == this.tagMap;
    }

    /**
     * Defines a new tag.
     *
     * @param tagType tag type. Can be TAGTYPE_BLOCK | TAGTYPE_EMPTY | TAGTYPE_PRE | TAGTYPE_INLINE
     * @param name    tag name
     */
    public void defineTag(short tagType, String name)
    {
        Parser tagParser;
        short model;

        switch (tagType)
        {
            case Dict.TAGTYPE_BLOCK:
                model = (short) (Dict.CM_BLOCK | Dict.CM_NO_INDENT | Dict.CM_NEW);
                tagParser = ParserImpl.BLOCK;
                break;

            case Dict.TAGTYPE_EMPTY:
                model = (short) (Dict.CM_EMPTY | Dict.CM_NO_INDENT | Dict.CM_NEW);
                tagParser = ParserImpl.BLOCK;
                break;

            case Dict.TAGTYPE_PRE:
                model = (short) (Dict.CM_BLOCK | Dict.CM_NO_INDENT | Dict.CM_NEW);
                tagParser = ParserImpl.PRE;
                break;

            case Dict.TAGTYPE_INLINE:
            default:
                // default to inline tag
                model = (short) (Dict.CM_INLINE | Dict.CM_NO_INDENT | Dict.CM_NEW);
                tagParser = ParserImpl.INLINE;
                break;
        }

        install(new Dict(name, Dict.VERS_PROPRIETARY, model, tagParser, null));
    }

    /**
     * return a List containing all the user-defined tag names.
     *
     * @param tagType one of Dict.TAGTYPE_EMPTY | Dict.TAGTYPE_INLINE | Dict.TAGTYPE_BLOCK | Dict.TAGTYPE_PRE
     * @return List containing all the user-defined tag names
     */
    List findAllDefinedTag(short tagType)
    {
        List<String> tagNames = new ArrayList<>();
        for (Dict curDictEntry : tagHashtable.values())
        {
            if (curDictEntry == null || (curDictEntry.versions != Dict.VERS_PROPRIETARY))
            {
                continue;
            }
            switch (tagType)
            {
                // defined tags can be empty + inline
                case Dict.TAGTYPE_EMPTY:
                    if ((curDictEntry.model & Dict.CM_EMPTY) == Dict.CM_EMPTY && !curDictEntry.equals(tagWbr))
                    {
                        tagNames.add(curDictEntry.name);
                    }
                    break;
                // defined tags can be empty + inline
                case Dict.TAGTYPE_INLINE:
                    if ((curDictEntry.model & Dict.CM_INLINE) == Dict.CM_INLINE &&
                        !curDictEntry.equals(tagBlink) &&
                        !curDictEntry.equals(tagNobr) &&
                        !curDictEntry.equals(tagWbr))
                    {
                        tagNames.add(curDictEntry.name);
                    }
                    break;
                // defined tags can be empty + block
                case Dict.TAGTYPE_BLOCK:
                    if ((curDictEntry.model & Dict.CM_BLOCK) == Dict.CM_BLOCK &&
                        curDictEntry.getParser() == ParserImpl.BLOCK)
                    {
                        tagNames.add(curDictEntry.name);
                    }
                    break;
                case Dict.TAGTYPE_PRE:
                    if (((curDictEntry.model & Dict.CM_BLOCK) == Dict.CM_BLOCK)
                        && (curDictEntry.getParser() == ParserImpl.PRE))
                    {
                        tagNames.add(curDictEntry.name);
                    }
                    break;
                default:
                    break;
            }
        }
        return tagNames;
    }

    /**
     * Free node's attributes.
     *
     * @param node Node
     */
    public void freeAttrs(Node node)
    {
        while (node.attributes != null)
        {
            AttVal av = node.attributes;
            if ("id".equalsIgnoreCase(av.attribute) || "name".equalsIgnoreCase(av.attribute) && isAnchorElement(node))
            {
                removeAnchorByNode(node);
            }

            node.attributes = av.next;
        }
    }

    /**
     * Removes anchor for specific node.
     *
     * @param node Node
     */
    void removeAnchorByNode(Node node)
    {
        Anchor found;
        Anchor prev = null;
        Anchor next;

        for (found = anchorList; found != null; found = found.next)
        {
            next = found.next;

            if (found.node == node)
            {
                if (prev != null)
                {
                    prev.next = next;
                }
                else
                {
                    anchorList = next;
                }
            }
            else
            {
                prev = found;
            }
        }
    }

    /**
     * Initialize a new anchor.
     *
     * @return a new anchor element
     */
    Anchor newAnchor()
    {
        return new Anchor();
    }

    /**
     * Adds a new anchor to namespace.
     *
     * @param name anchor name
     * @param node destination for this anchor
     * @return Anchor
     */
    Anchor addAnchor(String name, Node node)
    {
        Anchor a = newAnchor();

        a.name = name;
        a.node = node;

        if (anchorList == null)
        {
            anchorList = a;
        }
        else
        {
            Anchor here = anchorList;

            while (here.next != null)
            {
                here = here.next;
            }
            here.next = a;
        }

        return anchorList;
    }

    /**
     * Return node associated with anchor.
     *
     * @param name anchor name
     * @return node associated with anchor
     */
    Node getNodeByAnchor(String name)
    {
        Anchor found;

        for (found = anchorList; found != null; found = found.next)
        {
            if (name.equalsIgnoreCase(found.name))
            {
                break;
            }
        }

        if (found != null)
        {
            return found.node;
        }

        return null;
    }

    /**
     * free all anchors.
     */
    void freeAnchors()
    {
        anchorList = null;
    }

}
