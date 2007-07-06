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

/**
 * Miscellaneous XML parsing methods
 * created Jul 3, 2007
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
}
