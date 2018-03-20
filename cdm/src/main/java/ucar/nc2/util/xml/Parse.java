/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util.xml;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;

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
    org.jdom2.Document doc;
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
    if (text == null) return null;

    boolean bad = false;
    for (int i = 0, len = text.length(); i < len; i++) {
      int ch = text.charAt(i);
      if (!org.jdom2.Verifier.isXMLCharacter(ch)) {
        bad = true;
        break;
      }
    }

    if (!bad) return text;

    StringBuilder sbuff = new StringBuilder(text.length());
    for (int i = 0, len = text.length(); i < len; i++) {
      int ch = text.charAt(i);
      if (org.jdom2.Verifier.isXMLCharacter(ch))
        sbuff.append((char) ch);
    }
    return sbuff.toString();
  }
}
