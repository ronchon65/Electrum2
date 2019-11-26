/* Alloy Analyzer 4 -- Copyright (c) 2006-2009, Felix Chang
 * Electrum -- Copyright (c) 2015-present, Nuno Macedo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.mit.csail.sdg.alloy4;

import static edu.mit.csail.sdg.alloy4.OurConsole.style;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

/**
 * Graphical syntax-highlighting StyledDocument.
 * <p>
 * <b>Thread Safety:</b> Can be called only by the AWT event thread
 *
 * @modified Nuno Macedo // [HASLab] electrum-colorful
 */

class OurSyntaxDocument extends DefaultStyledDocument {

    /** This ensures the class can be serialized reliably. */
    private static final long               serialVersionUID = 0;

    /**
     * The style mode at the start of each line. First field is "comment mode" (0 =
     * no comment) (1 = block comment) (2 = javadoc comment) (-1 = unknown).
     * Remainder are colorful features (0 = not marked) (1 = positive mark) (2 =
     * negative mark) (-1 = unknown).
     */
    // [HASLab] colorful, adapted to also consider features
    private final List<List<Mode>>          comments         = new ArrayList<List<Mode>>();

    /**
     * Whether syntax highlighting is currently enabled or not.
     */
    private boolean                         enabled          = true;

    /** The current font name is. */
    private String                          font             = "Monospaced";

    /** The current font size. */
    private int                             fontSize         = 14;

    /** The current tab size. */
    private int                             tabSize          = 4;

    /**
     * The list of font+color styles (eg. regular text, symbols, keywords, comments,
     * etc).
     */
    private final List<MutableAttributeSet> all              = new ArrayList<>();

    /** The character style for regular text. */
    private final MutableAttributeSet       styleNormal      = style(font, fontSize, false, false, false, Color.BLACK, 0);
    {
        all.add(styleNormal);
    }

    // [HASLab] colorful annotations
    private final MutableAttributeSet styleNormal(List<Mode> n) {
        return style(font, fontSize, false, false, false, Color.BLACK, getPos(n), getNeg(n), 0);
    }

    /** The character style for symbols. */
    private final MutableAttributeSet styleSymbol = style(font, fontSize, true, false, false, Color.BLACK, 0);
    {
        all.add(styleSymbol);
    }

    // [HASLab] colorful annotations
    private final MutableAttributeSet styleSymbol(List<Mode> n) {
        return style(font, fontSize, true, false, false, Color.BLACK, getPos(n), getNeg(n), 0);
    }

    /** The character style for YAML header bars. */
    private final MutableAttributeSet yamlHeaderBars = style(font, fontSize, false, false, false, new Color(0xD86556), 0);
    {
        all.add(yamlHeaderBars);
    }
    /** The character style for YAML header field. */
    private final MutableAttributeSet yamlHeaderLines = style(font, fontSize, false, false, false, new Color(0xC58D6D), 0);
    {
        all.add(yamlHeaderLines);
    }

    /** The character style for ### field. */
    private final MutableAttributeSet styleHead3 = style(font, fontSize + 2, true, false, false, new Color(0x772222), 0);
    {
        all.add(styleHead3);
    }
    /** The character style for ## field. */
    private final MutableAttributeSet styleHead2 = style(font, fontSize + 4, true, false, false, new Color(0x772222), 0);
    {
        all.add(styleHead2);
    }
    /** The character style for # field. */
    private final MutableAttributeSet styleHead1 = style(font, fontSize + 20, true, false, false, new Color(0x772222), 0);
    {
        all.add(styleHead1);
    }
    /** The character style for **word**. */
    private final MutableAttributeSet styleBold = style(font, fontSize, true, false, false, Color.BLACK, 0);
    {
        all.add(styleBold);
    }
    /** The character style for _emphasis_. */
    private final MutableAttributeSet styleEmphasis = style(font, fontSize, false, true, false, Color.BLACK, 0);
    {
        all.add(styleEmphasis);
    }

    /** The character style for ~strikethrough~. */
    private final MutableAttributeSet styleStrikeThrough = style(font, fontSize, false, false, true, Color.BLACK, 0);
    {
        all.add(styleEmphasis);
    }

    /** The character style for YAML header field. */
    private final MutableAttributeSet alloyMarker = style(font, fontSize, false, false, false, new Color(0x84D5D0), 0);
    {
        all.add(alloyMarker);
    }

    /** The character style for integer constants. */
    private final MutableAttributeSet styleNumber = style(font, fontSize, true, false, false, new Color(0xA80A0A), 0);
    {
        all.add(styleNumber);
    }

    // [HASLab] colorful
    private final MutableAttributeSet styleNumber(List<Mode> n) {
        return style(font, fontSize, true, false, false, new Color(0xA80A0A), getPos(n), getNeg(n), 0);
    }

    /** The character style for keywords. */
    private final MutableAttributeSet styleKeyword = style(font, fontSize, true, false, false, new Color(0x1E1EA8), 0);
    {
        all.add(styleKeyword);
    }

    // [HASLab] colorful annotations
    private final MutableAttributeSet styleKeyword(List<Mode> n) {
        return style(font, fontSize, true, false, false, new Color(0x1E1EA8), getPos(n), getNeg(n), 0);
    }

    /** The character style for string literals. */
    private final MutableAttributeSet styleString = style(font, fontSize, false, false, false, new Color(0xA80AA8), 0);
    {
        all.add(styleString);
    }

    // [HASLab] colorful annotations
    private final MutableAttributeSet styleString(List<Mode> n) {
        return style(font, fontSize, false, false, false, new Color(0xA80AA8), getPos(n), getNeg(n), 0);
    }

    /** The character style for featured text. */
    // [HASLab] colorful annotations
    private final MutableAttributeSet styleColorMark(List<Mode> n, Color c) {
        return style(font, fontSize, true, false, false, new Color(c.getRed() - 41, c.getGreen() - 41, c.getBlue() - 41), getPos(n), getNeg(n), 0);
    }

    /** Convert the list of positive features (1) into a list of colors. */
    // [HASLab] colorful Alloy
    private static Set<Color> getPos(List<Mode> n) {
        Set<Color> res = new HashSet<Color>();
        for (int i = 1; i <= 9; i++)
            if (n.contains(Mode.valueOf("PFEAT" + i)))
                res.add(OurSyntaxWidget.C[i - 1]);
        return res;
    }

    /** Convert the list of negative features (2) into a list of colors. */
    // [HASLab] colorful Alloy
    private static Set<Color> getNeg(List<Mode> n) {
        Set<Color> res = new HashSet<Color>();
        for (int i = 1; i <= 9; i++)
            if (n.contains(Mode.valueOf("NFEAT" + i)))
                res.add(OurSyntaxWidget.C[i - 1]);
        return res;
    }

    /**
     * The character style for up-to-end-of-line-style comment.
     */
    private final MutableAttributeSet styleComment = style(font, fontSize, false, false, false, new Color(0x0A940A), 0);
    {
        all.add(styleComment);
    }

    /**
     * The character style for non-javadoc-style block comment.
     */
    private final MutableAttributeSet styleBlock = style(font, fontSize, false, false, false, new Color(0x0A940A), 0);
    {
        all.add(styleBlock);
    }

    /** The character style for javadoc-style block comment. */
    private final MutableAttributeSet styleJavadoc = style(font, fontSize, true, false, false, new Color(0x0A940A), 0);
    {
        all.add(styleJavadoc);
    }

    /** The paragraph style for indentation. */
    private final MutableAttributeSet tabset   = new SimpleAttributeSet();

    /**
     * This stores the currently recognized set of reserved keywords.
     */
    private static final String[]     keywords = new String[] { // [HASLab] colorful keywords
                                                               "abstract", "all", "and", "as", "assert", "but", "check", "disj", "disjoint", "else", "enum", "exactly", "exh", "exhaustive", "expect", "extends", "fact", "for", "fun", "iden", "iff", "implies", "in", "Int", "int", "let", "lone", "module", "no", "none", "not", "one", "open", "or", "part", "partition", "pred", "private", "run", "seq", "set", "sig", "some", "String", "sum", "this", "univ", "with"
    };

    /**
     * Returns true if array[start .. start+len-1] matches one of the reserved
     * keyword.
     */
    private static final boolean do_keyword(String array, int start, int len) {
        if (len >= 2 && len <= 10)
            for (int i = keywords.length - 1; i >= 0; i--) {
                String str = keywords[i];
                if (str.length() == len)
                    for (int j = 0;; j++)
                        if (j == len)
                            return true;
                        else if (str.charAt(j) != array.charAt(start + j))
                            break;
            }
        return false;
    }

    /**
     * Returns true if "c" can be in the start or middle or end of an identifier.
     */
    private static final boolean do_iden(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '$' || (c >= '0' && c <= '9') || c == '_' || c == '\'' || c == '\"';
    }

    /** The first positive/negative color feature delimiters. */
    // [HASLab] colorful annotations
    public static char O1 = '\u2780', E1 = '\u278A';

    /** Whether a positive color feature delimiter. */
    // [HASLab] colorful annotations
    private static final boolean isPositiveColor(char c) {
        return (c >= O1 && c <= (char) (O1 + 8));
    }

    /** Whether a negative color feature delimiter. */
    // [HASLab] colorful annotations
    private static final boolean isNegativeColor(char c) {
        return (c >= E1 && c <= (char) (E1 + 8));
    }

    /** Constructor. */
    public OurSyntaxDocument(String fontName, int fontSize) {
        putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
        tabSize++;
        do_setFont(fontName, fontSize, tabSize - 1); // assigns the given font,
                                                    // and also forces
                                                    // recomputation of the
                                                    // tab size
    }

    /** Enables or disables syntax highlighting. */
    public final void do_enableSyntax(boolean flag) {
        if (enabled == flag)
            return;
        else {
            enabled = flag;
            comments.clear();
        }
        if (flag)
            do_reapplyAll();
        else
            setCharacterAttributes(0, getLength(), styleNormal, false);
    }

    /**
     * Return the number of lines represented by the current text (where partial
     * line counts as a line).
     * <p>
     * For example: count("")==1, count("x")==1, count("x\n")==2, and
     * count("x\ny")==2
     */
    public final int do_getLineCount() {
        String txt = toString();
        for (int n = txt.length(), ans = 1, i = 0;; i++)
            if (i >= n)
                return ans;
            else if (txt.charAt(i) == '\n')
                ans++;
    }

    /**
     * Return the starting offset of the given line (If "line" argument is too
     * large, it will return the last line's starting offset)
     * <p>
     * For example: given "ab\ncd\n", start(0)==0, start(1)==3, start(2...)==6. Same
     * thing when given "ab\ncd\ne".
     */
    public final int do_getLineStartOffset(int line) {
        String txt = toString();
        for (int n = txt.length(), ans = 0, i = 0, y = 0;; i++)
            if (i >= n || y >= line)
                return ans;
            else if (txt.charAt(i) == '\n') {
                ans = i + 1;
                y++;
            }
    }

    /**
     * Return the line number that the offset is in (If "offset" argument is too
     * large, it will just return do_getLineCount()-1).
     * <p>
     * For example: given "ab\ncd\n", offset(0..2)==0, offset(3..5)==1,
     * offset(6..)==2. Same thing when given "ab\ncd\ne".
     */
    public final int do_getLineOfOffset(int offset) {
        String txt = toString();
        for (int n = txt.length(), ans = 0, i = 0;; i++)
            if (i >= n || i >= offset)
                return ans;
            else if (txt.charAt(i) == '\n')
                ans++;
    }

    /**
     * This method is called by Swing to insert a String into this document. We
     * intentionally ignore "attr" and instead use our own coloring.
     */
    @Override
    public void insertString(int offset, String string, AttributeSet attr) throws BadLocationException {
        if (string.indexOf('\r') >= 0)
            string = Util.convertLineBreak(string); // we don't want '\r'
        if (!enabled) {
            super.insertString(offset, string, styleNormal);
            return;
        }
        int startLine = do_getLineOfOffset(offset);
        for (int i = 0; i < string.length(); i++) { // For each inserted '\n' we
                                                   // need to shift the values
                                                   // in "comments" array down
            if (string.charAt(i) == '\n') {
                if (startLine < comments.size() - 1)
                    comments.add(startLine + 1, new ArrayList<Mode>(Arrays.asList(Mode.NONE))); // [HASLab] colorful annotations
            }
        }
        super.insertString(offset, string, styleNormal);
        try {
            do_update(startLine);
        } catch (Exception ex) {
            comments.clear();
        }
    }

    /**
     * This method is called by Swing to delete text from this document.
     */
    @Override
    public void remove(int offset, int length) throws BadLocationException {
        if (!enabled) {
            super.remove(offset, length);
            return;
        }
        int i = 0, startLine = do_getLineOfOffset(offset);
        for (String oldText = toString(); i < length; i++) { // For each deleted
                                                            // '\n' we need
                                                            // to shift the
                                                            // values in
                                                            // "comments"
                                                            // array up
            if (oldText.charAt(offset + i) == '\n')
                if (startLine < comments.size() - 1)
                    comments.remove(startLine + 1);
        }
        super.remove(offset, length);
        try {
            do_update(startLine);
        } catch (Exception ex) {
            comments.clear();
        }
    }

    /**
     * This method is called by Swing to replace text in this document.
     */
    @Override
    public void replace(int offset, int length, String string, AttributeSet attrs) throws BadLocationException {
        if (length > 0)
            this.remove(offset, length);
        if (string != null && string.length() > 0)
            this.insertString(offset, string, styleNormal);
    }

    /**
     * Reapply styles assuming the given line has just been modified
     */
    private final void do_update(int line) throws BadLocationException {
        String content = toString();
        int lineCount = do_getLineCount();
        while (line > 0 && (line >= comments.size() || comments.get(line).contains(Mode.NONE))) // [HASLab] colorful annotations
            line--; // "-1" in comments array are always contiguous
        List<Mode> comment = do_reapply(line == 0 ? new ArrayList<Mode>(Arrays.asList(Mode.ALLOY)) : comments.get(line), content, line); // [HASLab] colorful annotations
        for (line++; line < lineCount; line++) { // update each subsequent line
                                                // until it already starts
                                                // with its expected comment
                                                // mode
            if (line < comments.size() && comments.get(line) == comment)
                break;
            else
                comment = do_reapply(comment, content, line);
        }
    }

    /**
     * Re-color the given line assuming it starts with a given comment mode, then
     * return the comment mode for start of next line.
     */

    enum Mode {
               NONE,
               ALLOY,
               BLOCK_COMMENT,
               LINE_COMMENT,
               YAML,
               MARKDOWN,
               // [HASLab] colorful modes
               PFEAT1,
               PFEAT2,
               PFEAT3,
               PFEAT4,
               PFEAT5,
               PFEAT6,
               PFEAT7,
               PFEAT8,
               PFEAT9,
               NFEAT1,
               NFEAT2,
               NFEAT3,
               NFEAT4,
               NFEAT5,
               NFEAT6,
               NFEAT7,
               NFEAT8,
               NFEAT9,
               FEATURESCOPE;
    }

    // [HASLab] list of color modes rather than single comment mode
    private final List<Mode> do_reapply(List<Mode> mode, final String txt, final int line) {
        // [HASLab] colorful modes
        while (line >= comments.size())
            comments.add(new ArrayList<Mode>(Arrays.asList(Mode.NONE))); // enlarge array if needed // [HASLab]

        // [HASLab] colorful Alloy, must clone list
        comments.set(line, new ArrayList<Mode>(mode)); // record the fact that this line starts
        // with the given comment mode
        int startOfLine = do_getLineStartOffset(line);
        int endOfLine = txt.indexOf('\n', startOfLine);

        if (mode.contains(Mode.ALLOY) && line == 0 && match(txt, 0, "---\n")) { // [HASLab]
            setCharacterAttributes(startOfLine, 4, yamlHeaderBars, false);
            return new ArrayList<Mode>(Arrays.asList(Mode.YAML)); // [HASLab]
        }

        if (mode.contains(Mode.YAML)) { // [HASLab]
            if (match(txt, startOfLine, "---\n")) {
                setCharacterAttributes(startOfLine, 4, yamlHeaderBars, false);
                return new ArrayList<Mode>(Arrays.asList(Mode.MARKDOWN));
            } else {
                setCharacterAttributes(startOfLine, endOfLine - startOfLine, yamlHeaderLines, false);
                return new ArrayList<Mode>(Arrays.asList(Mode.YAML)); // [HASLab]
            }
        }

        if (mode.contains(Mode.MARKDOWN)) { // [HASLab]
            if (match(txt, startOfLine, "```alloy\n")) {
                setCharacterAttributes(startOfLine, endOfLine - startOfLine, alloyMarker, false);
                return new ArrayList<Mode>(Arrays.asList(Mode.ALLOY)); // [HASLab]
            }
        }

        if (mode.contains(Mode.ALLOY)) { // [HASLab]
            if (match(txt, startOfLine, "```\n")) {
                setCharacterAttributes(startOfLine, endOfLine - startOfLine, alloyMarker, false);
                return new ArrayList<Mode>(Arrays.asList(Mode.MARKDOWN)); // [HASLab]
            }
        }

        if (mode.contains(Mode.MARKDOWN)) { // [HASLab]
            if (match(txt, startOfLine, "###")) {
                setCharacterAttributes(startOfLine, endOfLine - startOfLine, styleHead3, false);
            } else if (match(txt, startOfLine, "##")) {
                setCharacterAttributes(startOfLine, endOfLine - startOfLine, styleHead2, false);
            } else if (match(txt, startOfLine, "#")) {
                setCharacterAttributes(startOfLine, endOfLine - startOfLine, styleHead1, false);
            } else {
                setCharacterAttributes(startOfLine, endOfLine - startOfLine, styleNormal, false);
                Pattern charStyles = Pattern.compile("(([*_~])\\2?)[^*_\n]+\\1");
                Matcher m = charStyles.matcher(txt);
                if (endOfLine < txt.length() && startOfLine <= endOfLine && startOfLine > 0 && endOfLine >= startOfLine) {
                    m.region(startOfLine, endOfLine);
                    while (m.find()) {
                        String type = m.group(1);
                        int start = m.start(0) + type.length();
                        int end = m.end(0) - type.length() - m.start(0) - 1;
                        switch (type) {
                            case "**" :
                                setCharacterAttributes(start, end, styleBold, true);
                                break;
                            case "__" :
                                setCharacterAttributes(start, end, styleEmphasis, true);
                                break;
                            case "~~" :
                            case "~" :
                                setCharacterAttributes(start, end, styleStrikeThrough, true);
                                break;
                            case "_" :
                                setCharacterAttributes(start, end, styleEmphasis, true);
                                break;
                            case "*" :
                                setCharacterAttributes(start, end, styleBold, true);
                                break;
                        }
                    }
                }
            }
            return new ArrayList<Mode>(Arrays.asList(Mode.MARKDOWN)); // [HASLab]
        }

        for (int n = txt.length(), i = startOfLine; i < n;) {
            final int oldi = i;
            final char c = txt.charAt(i);

            if (c == '\n')
                break;

            if (mode.contains(Mode.ALLOY) && c == '/' && i < n - 3 && txt.charAt(i + 1) == '*' && txt.charAt(i + 2) == '*' && txt.charAt(i + 3) != '/') // [HASLab]
                mode = new ArrayList<Mode>(Arrays.asList(Mode.LINE_COMMENT));
            if (mode.contains(Mode.ALLOY) && c == '/' && i == n - 3 && txt.charAt(i + 1) == '*' && txt.charAt(i + 2) == '*') // [HASLab]
                mode = new ArrayList<Mode>(Arrays.asList(Mode.LINE_COMMENT));
            if (mode.contains(Mode.ALLOY) && c == '/' && i < n - 1 && txt.charAt(i + 1) == '*') { // [HASLab]
                mode = new ArrayList<Mode>(Arrays.asList(Mode.BLOCK_COMMENT));
                i = i + 2;
            }

            if (!mode.contains(Mode.ALLOY)) { // [HASLab]
                AttributeSet style = (mode.contains(Mode.BLOCK_COMMENT) ? styleBlock : styleJavadoc); // [HASLab]
                while (i < n && txt.charAt(i) != '\n' && (txt.charAt(i) != '*' || i + 1 == n || txt.charAt(i + 1) != '/'))
                    i = i + 1;
                if (i < n - 1 && txt.charAt(i) == '*' && txt.charAt(i + 1) == '/') {
                    i = i + 2;
                    mode = new ArrayList<Mode>(Arrays.asList(Mode.ALLOY)); // [HASLab]
                }
                setCharacterAttributes(oldi, i - oldi, style, false); // [HASLab] colorful, must force update for strikes
            } else if ((c == '/' || c == '-') && i < n - 1 && txt.charAt(i + 1) == c) {
                i = txt.indexOf('\n', i);
                setCharacterAttributes(oldi, i < 0 ? (n - oldi) : (i - oldi), styleComment, false); // [HASLab] colorful, must force update for strikes
                break;
            } else if (c == '\"') {
                for (i++; i < n; i++) {
                    if (txt.charAt(i) == '\n')
                        break;
                    if (txt.charAt(i) == '\"') {
                        i++;
                        break;
                    }
                    if (txt.charAt(i) == '\\' && i + 1 < n && txt.charAt(i + 1) != '\n')
                        i++;
                }
                setCharacterAttributes(oldi, i - oldi, styleString(mode), false); // [HASLab] colorful, must force update for strikes
            } else if ((isNegativeColor(c) || isPositiveColor(c))) { // [HASLab] colorful, check for delimiters and change style mode
                i++;
                boolean opens = true;
                // if already with style, invert
                if (isPositiveColor(c) && mode.contains(Mode.valueOf("PFEAT" + (c - O1 + 1))) && !mode.contains(Mode.FEATURESCOPE)) {
                    mode.remove(Mode.valueOf("PFEAT" + (c - O1 + 1)));
                    opens = false;
                } else if (isNegativeColor(c) && mode.contains(Mode.valueOf("NFEAT" + (c - E1 + 1))) && !mode.contains(Mode.FEATURESCOPE)) {
                    mode.remove(Mode.valueOf("NFEAT" + (c - E1 + 1)));
                    opens = false;
                }
                for (int k = 0; k < 9; k++) // paint the delimiters
                    if (c == (char) (O1 + k) || c == (char) (E1 + k))
                        setCharacterAttributes(oldi, i - oldi, styleColorMark(mode, OurSyntaxWidget.C[k]), false);
                // if not in style, apply
                if (opens && isPositiveColor(c) && !mode.contains(Mode.valueOf("PFEAT" + (c - O1 + 1))) && !mode.contains(Mode.FEATURESCOPE)) {
                    mode.add(Mode.valueOf("PFEAT" + (c - O1 + 1)));
                } else if (opens && isNegativeColor(c) && !mode.contains(Mode.valueOf("NFEAT" + (c - E1 + 1))) && !mode.contains(Mode.FEATURESCOPE)) {
                    mode.add(Mode.valueOf("NFEAT" + (c - E1 + 1)));
                }
            } else if (do_iden(c)) {
                for (i++; i < n && do_iden(txt.charAt(i)); i++) {}
                AttributeSet style = (c >= '0' && c <= '9') ? styleNumber(mode) : (do_keyword(txt, oldi, i - oldi) ? styleKeyword(mode) : styleNormal(mode)); // [HASLab]
                setCharacterAttributes(oldi, i - oldi, style, false); // [HASLab] colorful Alloy, must force update for strikes
                if (do_keyword(txt, oldi, i - oldi)) { // [HASLab]
                    if (txt.charAt(oldi) == 'w')
                        mode.add(Mode.FEATURESCOPE);
                    else if (txt.charAt(oldi) != 'e')
                        mode.remove(Mode.FEATURESCOPE);
                }
            } else {
                for (i++; i < n && !do_iden(txt.charAt(i)) && txt.charAt(i) != '\n' && txt.charAt(i) != '-' && txt.charAt(i) != '/' && !isPositiveColor(txt.charAt(i)) && !isNegativeColor(txt.charAt(i)); i++) {}  // [HASLab] colorful, do not ignore color marks
                setCharacterAttributes(oldi, i - oldi, styleSymbol(mode), false); // [HASLab] colorful, must force update for strikes
            }
        }
        mode.remove(Mode.FEATURESCOPE); // [HASLab]
        return mode;
    }

    private boolean match(String source, int startIndex, String find) {
        int j = 0;
        for (; startIndex + j < source.length() && j < find.length(); j++) {
            char a = find.charAt(j);
            char b = source.charAt(startIndex + j);
            if (a != b)
                return false;
        }
        return j == find.length();
    }

    /** Reapply the appropriate style to the entire document. */
    private final void do_reapplyAll() {
        setCharacterAttributes(0, getLength(), styleNormal, true);
        comments.clear();
        String content = toString();
        List<Mode> mode = new ArrayList<Mode>(Arrays.asList(Mode.ALLOY)); // [HASLab] colorful annotations
        for (int i = 0, n = do_getLineCount(); i < n; i++)
            mode = do_reapply(mode, content, i); // [HASLab] colorful annotations
    }

    /** Changes the font and tabsize for the document. */
    public final void do_setFont(String fontName, int fontSize, int tabSize) {
        if (tabSize < 1)
            tabSize = 1;
        else if (tabSize > 100)
            tabSize = 100;
        if (fontName.equals(this.font) && fontSize == this.fontSize && tabSize == this.tabSize)
            return;
        this.font = fontName;
        this.fontSize = fontSize;
        this.tabSize = tabSize;
        for (MutableAttributeSet s : all) {
            StyleConstants.setFontFamily(s, fontName);
            StyleConstants.setFontSize(s, fontSize);
        }
        do_reapplyAll();
        BufferedImage im = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB); // this
                                                                                 // is
                                                                                 // used
                                                                                 // to
                                                                                 // derive
                                                                                 // the
                                                                                 // tab
                                                                                 // width
        int gap = tabSize * im.createGraphics().getFontMetrics(new Font(fontName, Font.PLAIN, fontSize)).charWidth('X');
        TabStop[] pos = new TabStop[100];
        for (int i = 0; i < 100; i++) {
            pos[i] = new TabStop(i * gap + gap);
        }
        StyleConstants.setTabSet(tabset, new TabSet(pos));
        setParagraphAttributes(0, getLength(), tabset, false);
    }

    /**
     * Overriden to return the full text of the document.
     *
     * @return the entire text
     */
    @Override
    public String toString() {
        try {
            return getText(0, getLength());
        } catch (BadLocationException ex) {
            return "";
        }
    }
}
