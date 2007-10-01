/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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

    StringBuffer sbuff = new StringBuffer(text.length());
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
