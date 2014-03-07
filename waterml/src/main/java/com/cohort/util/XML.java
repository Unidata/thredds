/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.xpath.XPath;   //requires java 1.5
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** 
 * This has some static methods to facilitate reading and writing an XML file.
 */
public class XML {

    /**
     * This returns the line separator from
     *  <code>System.getProperty("line.separator");</code>
     */
    public static String LS = System.getProperty("line.separator");

    /** For each character 0 - 255, these indicate how the character
     * should appear in HTML content. 
     * See HTML & XHTML book, Appendix F.
     * &quot; and &#39; are encoded to be safe (see encodeAsXML comments)
     * and consistent with encodeAsXML.
     */
    public static String[] HTML_ENTITIES = {
        "", "", "", "", "",   "", "", "", "", "&#9;", //0..  tab
        "\n", "", "", "\r", "",   "", "", "", "", "",   //10..
        "", "", "", "", "",   "", "", "", "", "",   //20..
        "",""," ","!","&quot;", "#","$","&#37;","&amp;","&#39;",   //30..   //% re percent encoding
        "(", ")", "*", "+", ",",   "-", ".", "/", "0", "1",   //40..
        "2", "3", "4", "5", "6",   "7", "8", "9", ":", ";",   //50..
        "&lt;", "=", "&gt;", "?", "@",   "A", "B", "C", "D", "E",   //60..
        "F", "G", "H", "I", "J",   "K", "L", "M", "N", "O",   //70..
        "P", "Q", "R", "S", "T",   "U", "V", "W", "X", "Y",   //80..
        "Z", "[", "\\", "]", "^",   "_", "`", "a", "b", "c",   //90..
        "d", "e", "f", "g", "h",   "i", "j", "k", "l", "m",   //100..
        "n", "o", "p", "q", "r",   "s", "t", "u", "v", "w",   //110..
        "x", "y", "z", "{", "|",   "}", "~", "", "", "",   //120..
"&#130;","&#131;","&#132;","&#133;","&#134;","&#135;","&#136;","&#137;","&#138;","&#139;",   //130.. "forbidden"!
"&#140;","",      "&#142;","",      "",      "&#145;","&#146;","&#147;","&#148;","&#149;",   //140.. "forbidden"!
"&#150;","&#151;","&#152;","&#153;","&#154;","&#155;","&#156;","",      "&#158;","&#159;",   //150.. "forbidden"!
"&nbsp;","&iexcl;","&cent;","&pound;","&curren;", //160
"&yen;","&brvbar;","&sect;","&uml;","&copy;", //165
"&ordf;","&laquo;","&not;","&shy;","&reg;", //170
"&macr;","&deg;","&plusmn;","&sup2;","&sup3;",  //175..
"&acute;","&micro;","&para;","&middot;","&cedil;", //180
"&sup1;","&ordm;","&raquo;","&frac14;","&frac12;", //185..
"&frac34;","&iquest;","&Agrave;","&Aacute;","&Acirc;",//190
"&Atilde;","&Auml;","&Aring;","&AElig;","&Ccedil;", //195..
"&Egrave;","&Eacute;","&Ecirc;","&Euml;","&Igrave;", //200
"&Iacute;","&Icirc;","&Iuml;","&ETH;","&Ntilde;", //205..
"&Ograve;","&Oacute;","&Ocirc;","&Otilde;","&Ouml;",// 210
"&times;","&Oslash;","&Ugrave;","&Uacute;","&Ucirc;", //215..
"&Uuml;","&Yacute;","&THORN;","&szlig;","&agrave;",//220
"&aacute;","&acirc;","&atilde;","&auml;","&aring;", //225..
"&aelig;","&ccedil;","&egrave;","&eacute;","&ecirc;",//230
"&euml;","&igrave;","&iacute;","&icirc;","&iuml;", //235..
"&eth;","&ntilde;","&ograve;","&oacute;","&ocirc;",//240
"&otilde;","&ouml;","&divide;","&oslash;","&ugrave;",//245..
"&uacute;","&ucirc;","&uuml;","&yacute;","&thorn;",//250
"&yuml;"}; //255  

    /**
     * This returns a String with the HTML tags removed and
     * common entities (&amp; &lt; &gt; &quot; &nbsp;) converted
     * to the original characters.
     *
     * @param htmlString
     * @return the plain text version
     */
    public static String removeHTMLTags(String htmlString) {
        //copy non-tags to a StringBuilder
        int htmlStringLength = htmlString.length();
        StringBuilder sb = new StringBuilder();
        int po = 0; //next char to be read
        while (po < htmlStringLength) {
            char ch = htmlString.charAt(po++);

            //is it the start of a tag? skip the tag
            if (ch == '<') {
                while (po < htmlStringLength && ch != '>') {
                    ch = htmlString.charAt(po++);
                }
            } else sb.append(ch);
        }
        return decodeEntities(sb.toString());
    }

    /**
     * This replaces chars 0 - 255 with their corresponding HTML_ENTITY
     * and higher chars with the hex numbered entity.
     *
     * <p>char 0 - 127 and &gt;=256 are encoded same as encodeAsXML.
     *
     * @param plainText the string to be encoded.
     *    If null, this throws exception.
     * @return the encoded string
     */
    public static String encodeAsHTML(String plainText) {
//future should it:* Pairs of spaces are converted to sp + &nbsp;.
        int size = plainText.length();
        StringBuilder output = new StringBuilder(size * 2);

        for (int i = 0; i < size; i++) {
            int chi = plainText.charAt(i); //note: int
            if (chi <= 255)
                output.append(HTML_ENTITIES[chi]);
            else 
                output.append("&#x" + Integer.toHexString(chi) + ";"); 
        }

        return output.toString();
    }

    /** 
     * If encodeAsHTML is true, this encodes as HTML; otherwise it returns the original string.
     *
     * @param s
     * @param encodeAsHTML
     * @return If encodeAsHTML is true, this encodes as HTML; otherwise it returns the original string.
     */
    public static String encodeAsHTML(String s, boolean encodeAsHTML) {
        return encodeAsHTML? encodeAsHTML(s) : s;
    }

    /**
     * For security reasons, for text that will be used as an HTML attribute, 
     * this replaces non-alphanumeric characters with HTML Entity &amp;#xHHHH; format.
     * See HTML Attribute Encoding at
     * https://www.owasp.org/index.php/XSS_%28Cross_Site_Scripting%29_Prevention_Cheat_Sheet#Output_Encoding_Rules_Summary
     *
     * @param plainText the string to be encoded.
     *    If null, this throws exception.
     * @return the encoded string
     */
    public static String encodeAsHTMLAttribute(String plainText) {
        int size = plainText.length();
        StringBuilder output = new StringBuilder(size * 2);

        for (int i = 0; i < size; i++) {
            int chi = plainText.charAt(i); //note: int
            if (String2.isDigitLetter(chi)) 
                 output.append((char)chi);
            else output.append("&#x" + Integer.toHexString(chi) + ";"); 
        }

        return output.toString();
    }


    /**
     * This replaces '&', '<', '>', '"', ''' in the string with 
     * "&amp;amp;", "&amp;lt;", "&amp;gt;", "&amp;quot;", "&amp;#39;" so plainText can be safely
     * stored as a quoted string within XML.
     *
     * <p>See "XML in a Nutshell" book, pg 20 for info on these 5 character encodings
     * (and no others).
     *
     * <p>char 0 - 127 and &gt;=256 are encoded same as encodeAsHTML.
     *
     * <p>This is part of preventing Cross-site-scripting security vulnerability
     * (which allows hacker to insert his javascript into pages returned by server).
     * See Tomcat (Definitive Guide) pg 147.
     *
     * @param plainText the string to be encoded.
     *    If null, this throws exception.
     * @return the encoded string
     */
    public static String encodeAsXML(String plainText) {
//future should it:* Pairs of spaces are converted to sp + &nbsp;.
        int size = plainText.length();
        StringBuilder output = new StringBuilder(size * 2);

        for (int i = 0; i < size; i++) {
            int chi = plainText.charAt(i); //note: int
            if (chi <= 127)
                //converting " is important to prevent cross site scripting; 
                //it prevents attacker from closing href="..." quotes
                output.append(HTML_ENTITIES[chi]);
            else 
                output.append("&#x" + Integer.toHexString(chi) + ";");
        }

        return output.toString();
    }

    /** 
     * If encodeAsXML is true, this encodes as XML; otherwise it returns the original string.
     *
     * @param s
     * @param encodeAsXML
     * @return If encodeAsXML is true, this encodes as XML; otherwise it returns the original string.
     */
    public static String encodeAsXML(String s, boolean encodeAsXML) {
        return encodeAsXML? encodeAsXML(s) : s;
    }

    /** This encodes spaces as (char)160 (nbsp) when they are leading, trailing,
     * or more than 1 consecutive.
     *
     * @param s
     * @return s with some spaces encoded as (char)160 (nbsp)
     */
    public static String minimalEncodeSpaces(String s) {
        //String2.log("s=\"" + s + "\"");
        int sLength = s.length();
        if (sLength == 0)
            return s;

        //count spaces at end
        int nSpacesAtEnd = 0;
        while (nSpacesAtEnd < sLength && s.charAt(sLength - (nSpacesAtEnd + 1)) == ' ')
            nSpacesAtEnd++;
        StringBuilder sb = new StringBuilder();

        if (nSpacesAtEnd < sLength) {

            //leading spaces
            int tsLength = sLength - nSpacesAtEnd;
            int po = 0;
            while (po < tsLength && s.charAt(po) == ' ') {
                sb.append((char)160); //"&nbsp;"
                po++; 
            }    

            //internal more than 1 consecutive
            while (po < tsLength - 1) {  //-1 so safe to look at po+1
                if (s.charAt(po) == ' ' && s.charAt(po + 1) == ' ') {
                    while (po < tsLength - 1 && s.charAt(po) == ' ') {
                        sb.append((char)160); //"&nbsp;"
                        po++;
                    }
                } else {
                    sb.append(s.charAt(po++));
                }
            }
            sb.append(s.charAt(tsLength - 1));
        }   

        //trailing spaces
        for (int i = 0; i < nSpacesAtEnd; i++)
            sb.append((char)160); //"&nbsp;"

        return sb.toString();
    }

    /**
     * This is like encodeAsHTML but treats plainText as &lt;pre&gt; text.
     *
     * @param plainText
     * @param maxLineLength  if lines are longer, they are broken
     */
    public static String encodeAsPreHTML(String plainText, int maxLineLength) {
        String s = String2.noLongLinesAtSpace(plainText, maxLineLength, "");
        s = encodeAsHTML(s);  //after noLongLines so tags aren't broken
        s = String2.replaceAll(s, "\r", "");
        s = String2.replaceAll(s, "\n", "<br>");  //after encodeAsHTML; 
        return s;
    }


    /**
     * This replaces HTML character entities (and the XML subset)
     * (e.g., "&amp;amp;", "&amp;lt;", "&amp;gt;", "&amp;quot;", etc.) in the string
     * with characters (e.g., '&', '<', '>', '"', etc.) 
     * so the original string can be recovered.
     * "&amp;nbsp;" is decoded to regular ' '.
     * Unrecognized/invalid entities are left intact so appear as e.g., &amp;#A;.
     *
     * @param s the string to be decoded
     * @return the decoded string
     */
    public static String decodeEntities(String s) {
        int size = s.length();
        StringBuilder output = new StringBuilder(size * 2);

        int i = 0;
        while (i < size) {
            char ch = s.charAt(i++);

            if (ch == '&') {
                int po = s.indexOf(';', i);
                if (po > 0) {
                    String entity = s.substring(i-1, po+1);
                    if (entity.charAt(1) == '#') {  //e.g., &#37;
                        String num = entity.substring(2, entity.length() - 1);
                        if (num.length() == 0) {
                            //falls through, so shown as &#;
                        } else if (num.charAt(0) == 'x') {
                            num = "0" + num; //xhhh  hex number -> 0xhhh
                        }
                        int v = String2.parseInt(num);  //this relies on leading 0's being ignored -> decimal (not octal)
                        output.append(
                            v == 160? " " :  //nbsp
                            v < Character.MAX_VALUE? "" + (char)v : 
                            entity); //show intact original entity as plain text
                    //check for common entities first
                    } else if (entity.equals("&amp;"))  { output.append('&');
                    } else if (entity.equals("&quot;")) { output.append('"');
                    } else if (entity.equals("&lt;"))   { output.append('<');
                    } else if (entity.equals("&gt;"))   { output.append('>');
                    } else {
                        //search HTML_ENTITIES
                        //make faster: store in hashmap  (but these are less common)
                        int which = String2.indexOf(HTML_ENTITIES, entity);
                        if (which >= 0) 
                            output.append((char)which);
                        else 
                            output.append(entity); //leave intact                         
                    }
                    i = po + 1;
                } else { //no closing ';'!  leave & in place
                    output.append(ch);
                }
            } else output.append(ch);
        }
        return output.toString();
    }



    /**
     * This writes s to 'out'.
     * If no error occurs or error.length()!=0, this returns 'error';
     *   otherwise, this returns a new error message. 
     *
     * @param out 
     * @param s the string to be written
     * @param error any previous error ("" if no error)
     * @return an error String ("" if no error)
     */
    public static String toWriter(Writer out, String s, String error) {
        try {
            out.write(s); 
        } catch (Exception e) {
            if (error.length() == 0) 
                error = MustBe.throwableToString(e);
        }
        return error;
    }

    /**
     * This writes one element to an XML stream.
     * This handles encoding the value string.
     * If no error occurs or error.length()!=0, this returns 'error';
     *   otherwise, this returns a new error message. 
     *
     * @param out
     * @param indent
     * @param name the name of the start element
     * @param error the previous error ("" if no error)
     * @return an error String ("" if no error)
     */
    public static String writeXMLStartElement(Writer out, 
            String indent, String name, String error) {
        return toWriter(out, indent + '<' + name + '>' + LS, error);
    }

    /**
     * This writes one element to an XML stream.
     * This handles encoding the value string.
     * If no error occurs or error.length()!=0, this returns 'error';
     *   otherwise, this returns a new error message. 
     *
     * @param out
     * @param indent
     * @param name the name of the element
     * @param error the previous error ("" if no error)
     * @return an error String ("" if no error)
     */
    public static String writeXMLEndElement(Writer out, 
            String indent, String name, String error) {
        return toWriter(out, indent + "</" + name + '>' + LS, error);
    }

    /**
     * This writes one element to an XML stream.
     * This handles encoding the value string.
     * If no error occurs or error.length()!=0, this returns 'error';
     *   otherwise, this returns a new error message. 
     *
     * @param out
     * @param indent
     * @param name the name of the element
     * @param value the value of the element (not yet htmlEncoded)
     * @param error the previous error ("" if no error)
     * @return an error String ("" if no error)
     */
    public static String writeXMLElement(Writer out, 
            String indent, String name, String value, String error) {
        return toWriter(out, 
            indent + '<' + name + '>' + encodeAsXML(value) + 
            "</" + name + '>' + LS, 
        error);
    }

    /**
     * This makes the specified text into a valid xmlName.
     * <P>FYI: XML names may contain letters, digits, '_', '-', '.'.
     *     XML names must start with a letter or '_'.
     *     One colon may be used to identify namespace (e.g., "namespace:name").
     *     See XML in a Nutshell, 3rd ed, pg 18.
     * <P>FYI: Java identifiers may contain letters, digits, '_', '$'.
     *     Java identifiers must start with a letter, '_', or '$'.
     *     But it is a little more complex than this, 
     *     see Java API for Character.isJavaIdentifierPart and
     *     isJavaIdentifierStart.
     *
     * @param text The text for the xmlName. 
     *     If name is null, this returns "",
     *     which will be detected as invalid by <code>EnofClass.isValid</code>.
     *     This converts spaces to '_'.     
     *     In keeping with the requirements for XML element names, 
     *     only letters, digits, underscores, hyphens, and periods 
     *     in the name are kept. Other characters are removed.
     *     If the first character is not a letter or underscore, 
     *     "_" is prepended.
     */
    public static String textToXMLName(String text) {

        //ensure it is a valid xml name: letters, digits, "_", "-", "." only
        text = text.trim();
        text = XML.removeHTMLTags(text);
        int textLength = text.length();
        StringBuilder sb = new StringBuilder(textLength + 1);
        for (int i = 0; i < textLength; i++) {
            char ch = text.charAt(i);
            if (ch == ' ')
                sb.append('_');
            else if (String2.isDigitLetter(ch) ||
                ch == '_' ||
                ch == '-' ||
                ch == '.')
                sb.append(ch);
        }

        //ensure first character is letter or '_'
        if (sb.length() > 0 && 
            !String2.isLetter(sb.charAt(0)) && 
            sb.charAt(0) != '_') 
            sb.insert(0, '_');

        //return the result (which may be "" and therefore invalid)
        return sb.toString();   
    }    

    /**
     * This substitutes the substitutions.
     * This routine is not very smart -- it can be fooled by tags within
     * comments. So it is best to remove comments first.
     *
     * @param template an XML document template with some elements
     *    e.g., <tagA><!-- put something here --></tagA>  
     *    The results are put in here.
     * @param substitutions Each line is in the form: <tag1><tag2>data,
     *   where there may be 1 or more elements. 
     * @throws Exception if trouble
     */
    public static void substitute(StringBuilder template, String substitutions[]) {

        //for each substitution
        for (int subN = 0; subN < substitutions.length; subN++) {
            int temPo = 0;    //one character beyond last tag found
            String sub = substitutions[subN];
            int tagStart = 0; //in sub
            //String2.log("sub=" + encodeAsXML(sub));

            //find each tag which identifies the 
            String tag = null; //with throw null pointer if no tag found
            while (sub.charAt(tagStart) == '<') {
                //get the tag name
                int tagEnd = sub.indexOf('>', tagStart + 1);
                tag = sub.substring(tagStart, tagEnd + 1);
                tagStart = tagEnd + 1;
                //String2.log("  tag=" + encodeAsXML(tag));

                //find its location in the template
                temPo = template.indexOf(tag, temPo);
                Test.ensureNotEqual(temPo, -1, "FGDC.substitute subN=" + subN + 
                    " tag not found =" + encodeAsXML(tag) + 
                    "\n  substitution=" + encodeAsXML(sub));
                temPo += tag.length();
            }

            //find the close tag
            String closeTag = "</" + tag.substring(1);
            int closeTagAt = template.indexOf(closeTag, temPo);
            Test.ensureNotEqual(closeTagAt, -1, "FGDC.substitute subN=" + subN + 
                " close tag not found =" + encodeAsXML(closeTag) + 
                "\n  substitution=" + encodeAsXML(sub));

            //delete any data between the close and open tags
            template.delete(temPo, closeTagAt);

            //get the data and insert it into the template
            String data = sub.substring(tagStart);
            template.insert(temPo, data);
            //String2.log("FGDC.substitute substituting data=" + data);
        }

        //ensure the result is valid XML?

    }

    /**
     * This removes any comments from the XML document.
     *
     * @param document
     * @throws Exception if trouble
     */
    public static void removeComments(StringBuilder document) {
        int startPo = document.indexOf("<!--");
        while (startPo >= 0) {
            int endPo = document.indexOf("-->", startPo + 4);
            document.delete(startPo, endPo + 3);
            startPo = document.indexOf("<!--", startPo);
        }
    }

    /**
     * Parse an XML file and return a DOM Document.
     * If validating is true, the XML is validated against the DTD
     * specified by DOCTYPE in the file.
     *
     * @param fileName e.g., c:/temp/test.xml
     * @param validating  use true to validate the file against the DTD specified
     *    in the file.
     * @return a DOM Document
     * @throws Exception if trouble
     */
    public static Document parseXml(String fileName, boolean validating) throws Exception {
        return parseXml(new InputSource(fileName), validating);
    }

    /**
     * Parse XML from a Reader and return a DOM Document.
     * If validating is true, the XML is validated against the DTD
     * specified by DOCTYPE in the file.
     *
     * @param input
     * @param validating  use true to validate the file against the DTD specified
     *    in the file.
     * @return a DOM Document
     * @throws Exception if trouble
     */
    public static Document parseXml(Reader input, boolean validating) throws Exception {
        return parseXml(new InputSource(input), validating);
    }

    /**
     * Parse XML from a Reader and return a DOM Document.
     * If validating is true, the XML is validated against the DTD
     * specified by DOCTYPE in the file.
     *
     * @param inputSource 
     * @param validating  use true to validate the file against the DTD specified
     *    in the file.
     * @return a DOM Document
     * @throws Exception if trouble
     */
    public static Document parseXml(InputSource inputSource, boolean validating) throws Exception {
        //create a builder factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(validating);

        //create the builder and parse the file
        return factory.newDocumentBuilder().parse(inputSource);
    }

    /**
     * Returns an xPath object.
     *
     * @return an XPath object.
     */
    public static XPath getXPath() {
        return javax.xml.xpath.XPathFactory.newInstance().newXPath();
    }

    /**
     * This gets a nodeList for an XPath query.
     * <br>See http://download.oracle.com/javase/7/docs/api/javax/xml/xpath/package-summary.html 
     * <br>See javadoc for xpath
     * <br>See examples at http://javaalmanac.com/egs/org.w3c.dom/xpath_GetElemByAttr.html?l=rel
     * <br>See examples at http://javaalmanac.com/egs/org.w3c.dom/xpath_GetAbsElem.html?l=rel
     *
     * @param item usually a Document from parseXml() above, but may be a NodeList or a Node.
     * @param xPath  from getXPath()
     * @param xPathQuery  e.g., "/testr/level1". 
     *    See XPath documentation: http://www.w3.org/TR/xpath
     * @return the NodeList of matching Nodes (it may be of length 0)
     * @throws Exception if trouble
     */
    public static NodeList getNodeList(Object item, XPath xPath, String xPathQuery) throws Exception {
        //NODESET maps to an actual NodeList
        return (NodeList)xPath.evaluate(xPathQuery, item, javax.xml.xpath.XPathConstants.NODESET);
    }

    /**
     * This gets the first node matching an XPath query.
     *
     * @param item usually a Document from parseXml() above, but may be a NodeList or a Node.
     * @param xPath  from getXPath()
     * @param xPathQuery  e.g., "/testr/level1". 
     *    See XPath documentation: http://www.w3.org/TR/xpath
     * @return the first node matching an XPath query (or null if none).
     */
    public static Node getFirstNode(Object item, XPath xPath, String xPathQuery) throws Exception {
        //NODESET maps to an actual NodeList
        NodeList nodeList = getNodeList(item, xPath, xPathQuery);
        return nodeList.getLength() == 0? null : nodeList.item(0);
    }

    /**
     * This returns the value of the specified attribute of a node.
     *
     * @param node
     * @param attributeName
     * @return attribute's value (null if node=null or attributeName not present)
     */
    public static String getAttribute(Node node, String attributeName) {
        if (node == null) 
            return null;
        Node att = node.getAttributes().getNamedItem(attributeName);
        if (att == null)
            return null;
        return att.getNodeValue();
    }

    /**
     * This returns the text content contained in this node (and all subelements),
     *   with leading and trailing white space removed.
     *
     * @param node
     * @return the text content contained in this node (and all subelements),
     *   with leading and trailing white space removed.
     *   This won't return null. This may return "" (if node is null or no content).
     */
    public static String getTextContent(Node node) {
        if (node == null)
            return "";
        String s = node.getTextContent();
        int length = s.length();
        int firstValid = 0;
        while (firstValid < length && String2.isWhite(s.charAt(firstValid))) 
            firstValid++; 
        int lastValid = length - 1;
        while (lastValid > firstValid && String2.isWhite(s.charAt(lastValid))) 
            lastValid--;
        return s.substring(firstValid, lastValid + 1);
    }

    /** This returns the text content for the first node matching an XPath query.
     *
     * @param item usually a Document from parseXml() above, but may be a NodeList or a Node.
     * @param xPath  from getXPath()
     * @param xPathQuery  e.g., "/testr/level1". 
     *    See XPath documentation: http://www.w3.org/TR/xpath
     * @return the text content (or "" if no matching node or no content).
     */
    public static String getTextContent1(Object item, XPath xPath, String xPathQuery) throws Exception {
        return getTextContent(getFirstNode(item, xPath, xPathQuery));
    }

    /* SEE NEW prettyXml BELOW.
     * This reformats an xml file without newlines to have some newlines. * /
    public static void prettyXml(String inFileName, String outFileName) {
        String2.log("prettyXml\n in=" + inFileName + "\nout=" + outFileName);
        String in[] = String2.readFromFile(inFileName);
        String2.log(in[0]);
        in[1] = String2.replaceAll(in[1], "<", "\n<");
        in[1] = String2.replaceAll(in[1], "\n</", "</");
        String2.writeToFile(outFileName, in[1]);
    }
    */

    /** This reformats an xml file to have newlines and nice indentation. 
     * This throws RuntimeException if trouble.
     */
    public static void prettyXml(String inFileName, String outFileName) {
        String2.log("prettyXml\n in=" + inFileName + "\nout=" + outFileName);
        if (inFileName.equals(outFileName))
            throw new RuntimeException("Error: inFileName equals outFileName!");
        String in[] = String2.readFromFile(inFileName, "UTF-8");
        if (in[0].length() > 0)
            throw new RuntimeException("Error while reading " + inFileName + "\n" + in[0]);
        String xml = in[1];
        int xmlLength = xml.length();
        StringBuilder sb = new StringBuilder();
        //start and end of a tag, and start of next tag
        int start, end, nextStart = xml.indexOf('<');
        int indent = -2;  //first tag should be <?xml version="1.0" encoding="UTF-8"?> and no closing tag
        boolean lastHadContent = false;
        while (nextStart >= 0 && nextStart < xmlLength) {
/*
<?xml version="1.0" encoding="UTF-8"?>

  <gmd:axisDimensionProperties>
    <gmd:MD_Dimension>
      <gmd:dimensionName>
        <gmd:MD_DimensionNameTypeCode codeList="http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode" codeListValue="column">column</gmd:MD_DimensionNameTypeCode>
      </gmd:dimensionName>
      <gmd:dimensionSize gco:nilReason="unknown"/>
      <gmd:resolution>
        <gco:Measure uom="degrees_east">1.0</gco:Measure>
      </gmd:resolution>
    </gmd:MD_Dimension>
  </gmd:axisDimensionProperties> */
            start = nextStart;

            //deal with comment: <!--  -->
            if (xml.substring(start, start + 4).equals("<!--")) {
                end = xml.indexOf("-->", start + 4);
                if (end < 0) throw new RuntimeException("No end '-->' for last comment.");
                nextStart = xml.indexOf('<', end + 3);
                if (nextStart < 0)
                    nextStart = xmlLength;
                //trim() because xml content begin/end whitespace is not significant
                String content = xml.substring(end + 3, nextStart).trim(); 

                //write indent
                if (start > 0) 
                    sb.append('\n');
                for (int i = 0; i < indent; i++) {
                    sb.append(' ');
                }
                //write tag
                sb.append(xml.substring(start, end + 3));
                //write content
                sb.append(content);
                lastHadContent = false;
                continue;
            }

            //deal with CDATA: <![CDATA[     ]]>
            if (xml.substring(start, start + 9).equals("<![CDATA[")) {
                end = xml.indexOf("]]>", start + 9);
                if (end < 0) throw new RuntimeException("No end ']]>' for last <![CDATA[.");
                nextStart = xml.indexOf('<', end + 3);
                if (nextStart < 0)
                    nextStart = xmlLength;
                //trim() because xml content begin/end whitespace is not significant
                String content = xml.substring(end + 3, nextStart).trim(); 

                //write tag
                sb.append(xml.substring(start, end + 3));
                //write content
                sb.append(content);
                lastHadContent = true;
                continue;
            }

            //deal with regular tag
            end = xml.indexOf('>', start + 1);
            if (end < 0) throw new RuntimeException("No '>' for last tag.");
            nextStart = xml.indexOf('<', end + 1);
            if (nextStart < 0)
                nextStart = xmlLength;
            //trim() because xml content begin/end whitespace is not significant
            String content = xml.substring(end + 1, nextStart).trim(); 

            //write the tag and content
            if (xml.charAt(start+1) == '/')
                indent -= 2;
            if (!lastHadContent) {
                //write indent
                if (start > 0) sb.append('\n');
                for (int i = 0; i < indent; i++) {
                    sb.append(' ');
                }
            }
            sb.append(xml.substring(start, end + 1));
            sb.append(content);
            lastHadContent = content.length() > 0;

            if (xml.charAt(end - 1) == '/' ||
                xml.charAt(start+1) == '/') {
                //indent unchanged
            } else {
                indent += 2;
            }
        }
        //String2.log(sb.toString());
        String2.writeToFile(outFileName, sb.toString());
    }


    /**
     * Test the methods in this class.
     *
     * @throws Exception if trouble
     */
    public static void test() throws Exception {
        String2.log("\n*********************************************************** XML.test");

        //test removeHTMLTags
        String2.log("test removeHTMLTags");
        Test.ensureEqual(removeHTMLTags("Hi, <b>bob.simons&amp;</b>!"), 
            "Hi, bob.simons&!", "a");

        //test encodeAsXML
        String2.log("test encode");
        Test.ensureEqual(encodeAsXML( "Hi &<>\"°\u1234Bob"), "Hi &amp;&lt;&gt;&quot;&#xb0;&#x1234;Bob", "XML");
        Test.ensureEqual(encodeAsHTML("Hi &<>\"°\u1234Bob"), "Hi &amp;&lt;&gt;&quot;&deg;&#x1234;Bob", "HTML");
        Test.ensureEqual(encodeAsHTMLAttribute(
                                      "Hi &<>\"°\u1234Bob"), "Hi&#x20;&#x26;&#x3c;&#x3e;&#x22;&#xb0;&#x1234;Bob", "HTML");

        //test decodeEntities
        String2.log("test decodeEntities"); //037 tests leading 0, which is valid
        Test.ensureEqual(decodeEntities("Hi&#037;&#37;&#x025;&#x25; &amp;&lt;&gt;&quot;&nbsp;&#176;&deg;"), 
            "Hi%%%% &<>\"\u00a0°°", "decode");

        for (int ch = 0; ch < 260; ch++) {
            char ch1 = (char)ch;
            String ch2 = decodeEntities(encodeAsXML("" + ch1));
            if (ch2.length() > 0 && ch != 160)   //#160=nbsp decodes as #20=' ' 
                Test.ensureEqual(ch2, "" + ch1, "XML encode/decode ch=" + ch);
            ch2 = decodeEntities(encodeAsHTML("" + ch1));
            if (ch2.length() > 0) 
                Test.ensureEqual(ch2, "" + ch1, "HTML encode/decode ch=" + ch);
        }


        //test textToXMLName
        String2.log("test textToXMLName");
        Test.ensureEqual(textToXMLName("3 my._-te#st"), "_3_my._-test", "e");

        //test substitute
        String doc = "<!-- comment --> " +
            "<tag1>a<tag2>bb</tag2>ccc<tag3>dddd<tag4>eeee</tag4></tag3></tag1>f<!--comment-->g";
        String sub[] = new String[]{"<tag1><tag2>Nate", "<tag1><tag2><tag3><tag4>Nancy"};
        StringBuilder sb = new StringBuilder(doc);
        substitute(sb, sub);
        Test.ensureEqual(sb.toString(), "<!-- comment --> " +
            "<tag1>a<tag2>Nate</tag2>ccc<tag3>dddd<tag4>Nancy</tag4></tag3></tag1>f<!--comment-->g",
            "f");

        sub = new String[]{"<tag1><tag2> <tag3><tag4>Nancy"}; //test non-contiguous
        sb = new StringBuilder(doc);
        substitute(sb, sub);
        Test.ensureEqual(sb.toString(), "<!-- comment --> " +
            "<tag1>a<tag2> <tag3><tag4>Nancy</tag2>ccc<tag3>dddd<tag4>eeee</tag4></tag3></tag1>f<!--comment-->g",
            "g");

        //test removeComments
        sb = new StringBuilder(doc);
        removeComments(sb);
        Test.ensureEqual(sb.toString(), " " +
            "<tag1>a<tag2>bb</tag2>ccc<tag3>dddd<tag4>eeee</tag4></tag3></tag1>fg",
            "h");
        
        //***** tests of XPath
        XPath xPath = getXPath();
        Document document = parseXml(new BufferedReader(new StringReader(
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
            "<testr>\n" +
            "  <level1 att1=\"value1\" att2=\"value 2\" > level 1 &amp; <!-- comment -->text  \n" +
            "  </level1>\n" +
            "  <levela />\n" +   //"empty tag" appears as two tags, begin and end
            "  <levelb> <!-- < > --> stuff</levelb>\n" +
            "  <level1>test of level 1</level1>\n" +
            "\n" +
            "\n" +
            "\n" +
            "</testr>")), 
            false);
    
        //test of XPath with valid xml
        NodeList nodeList = getNodeList(document, xPath, "/*");     
        Test.ensureEqual(nodeList.getLength(), 1, "");
        Test.ensureEqual(nodeList.item(0).getNodeName(), "testr", "get root node");

        //get all <level1> tags
        nodeList = getNodeList(document, xPath, "/testr/level1"); 
        Test.ensureEqual(nodeList.getLength(), 2, "");
        //get all contained text (even for sub elements)
        Test.ensureEqual(getTextContent(nodeList.item(0)), "level 1 & text", "get text");
        Test.ensureEqual(getTextContent(nodeList.item(1)), "test of level 1", "get text");
        Test.ensureEqual(getTextContent(getFirstNode(document, xPath, "/testr/level1")), "level 1 & text", "get text");
        Test.ensureEqual(getTextContent(null), "", "get text");
        Test.ensureEqual(getTextContent1(document, xPath, "/testr/level1"), "level 1 & text", "get text");

        //get all <level1> tags that have an att1 attribute
        nodeList = getNodeList(document, xPath, "/testr/level1[@att1]"); 
        Test.ensureEqual(nodeList.getLength(), 1, "");
        Test.ensureEqual(getAttribute(nodeList.item(0), "att1"), "value1", "get attribute");
        Test.ensureEqual(getAttribute(nodeList.item(0), "zz"),   null,     "get attribute");

        Test.ensureEqual(getAttribute(getFirstNode(document, xPath, "/testr/level1[@att1]"), "att1"), "value1", "get attribute");
        Test.ensureEqual(getAttribute(null, "att1"), null, "get attribute");

        //get all <level1> tags that have an att1 and att2 attributes
        nodeList = getNodeList(document, xPath, "/testr/level1[@att1 and @att2]"); 
        Test.ensureEqual(nodeList.getLength(), 1, "");

        //get all <level1> tags that have an att1 attribute = "value1"
        //there are also options for contains and startsWith
        nodeList = getNodeList(document, xPath, "/testr/level1[@att1='value1']"); 
        Test.ensureEqual(nodeList.getLength(), 1, "");


        //test minimalEncodeSpaces
        Test.ensureEqual(minimalEncodeSpaces(""), "", "");
        Test.ensureEqual(minimalEncodeSpaces(" "), "\u00A0", "");
        Test.ensureEqual(minimalEncodeSpaces("  "), "\u00A0\u00A0", "");
        Test.ensureEqual(minimalEncodeSpaces(" a "), "\u00A0a\u00A0", "");
        Test.ensureEqual(minimalEncodeSpaces("  a  "), "\u00A0\u00A0a\u00A0\u00A0", "");
        Test.ensureEqual(minimalEncodeSpaces(" ab "), "\u00A0ab\u00A0", "");
        Test.ensureEqual(minimalEncodeSpaces("a b"), "a b", "");
        Test.ensureEqual(minimalEncodeSpaces(" a  b "), "\u00A0a\u00A0\u00A0b\u00A0", "");
        Test.ensureEqual(minimalEncodeSpaces("  a   bc d  "), "\u00A0\u00A0a\u00A0\u00A0\u00A0bc d\u00A0\u00A0", "");

        String2.log("XML.test finished successfully.");

    }

}