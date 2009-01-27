/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.util.xml;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;

import ucar.unidata.util.StringUtil;

/**
 * Miscellaneous XML parsing methods
 * created Jul 3, 2007
 *
 * @author caron
 */
public class Parse {

  /**
   * Read an XML Document from a URL and return the root element.
   *
   * @param location the URL location
   * @return the root element of the Document
   * @throws java.io.IOException on read error
   */
  static public Element readRootElement(String location) throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(location);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    return doc.getRootElement();
  }

  /**
   * Make sure that text is XML safe
   * @param text check this
   * @return original text if ok, else with bad characters removed
   */
  static public String cleanCharacterData(String text) {
    if (text == null) return text;

    boolean bad = false;
    for (int i = 0, len = text.length(); i < len; i++) {
      int ch = text.charAt(i);
      if (!org.jdom.Verifier.isXMLCharacter(ch)) {
        bad = true;
        break;
      }
    }

    if (!bad) return text;

    StringBuilder sbuff = new StringBuilder(text.length());
    for (int i = 0, len = text.length(); i < len; i++) {
      int ch = text.charAt(i);
      if (org.jdom.Verifier.isXMLCharacter(ch))
        sbuff.append(ch);
    }
    return sbuff.toString();
  }


  /// probably not needed  - use Stax or Jdom

    /**
   * Replace special characters with entities for XML attributes.
   * special: '&', '<', '>', '\'', '"', '\r', '\n'
   *
   * @param x string to quote
   * @return equivilent string using entities for any special chars
   */
  static public String quoteXmlContent(String x) {
    return StringUtil.replace(x, xmlInC, xmlOutC);
  }

  /**
   * Reverse XML quoting to recover the original string.
   *
   * @param x string to quote
   * @return equivilent string
   */
  static public String unquoteXmlContent(String x) {
    return StringUtil.unreplace(x, xmlOutC, xmlInC);
  }

  /**
   * these chars must get replaced in XML
   */
  private static char[] xmlInC = {
          '&', '<', '>'
  };

  /**
   * replacement strings
   */
  private static String[] xmlOutC = {
          "&amp;", "&lt;", "&gt;"
  };
}
