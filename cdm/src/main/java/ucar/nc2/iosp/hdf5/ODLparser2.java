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
package ucar.nc2.iosp.hdf5;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * @author caron
 * @since Aug 7, 2007
 */
public class ODLparser2 {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ODLparser2.class);

  private Document doc;
  private boolean debug = false, show = false;

  void showDoc() {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    try {
      fmt.output(doc, System.out);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void parseFile(String filename) throws IOException {
    String text = new String(thredds.util.IO.readFileToByteArray(filename));
    parseFromString(text);
  }

  public Element parseFromString(String text) throws IOException {
    Element rootElem = new Element("odl");
    doc = new Document(rootElem);

    Element current = rootElem;
    StringTokenizer lineFinder = new StringTokenizer(text, "\t\n\r\f");
    while (lineFinder.hasMoreTokens()) {
      String line = lineFinder.nextToken();
      if (line.startsWith("GROUP")) {
        current = startGroup(current, line);

      } else if (line.startsWith("OBJECT")) {
        current = startObject(current, line);

      } else if (line.startsWith("END_OBJECT")) {
        endObject( current, line);
        current = current.getParentElement();

      } else if (line.startsWith("END_GROUP")) {
        endGroup( current, line);
        current = current.getParentElement();

      } else {
        addField( current, line);
      }
    }

    if (show) showDoc();
    return rootElem;
  }

  Element startGroup(Element parent, String line) throws IOException {
    StringTokenizer stoke = new StringTokenizer(line, "=");
    String toke = stoke.nextToken();
    assert toke.equals("GROUP");
    String name = stoke.nextToken();
    Element group = new Element(name);
    parent.addContent( group);
    return group;
  }

  void endGroup(Element current, String line) throws IOException {
    StringTokenizer stoke = new StringTokenizer(line, "=");
    String toke = stoke.nextToken();
    assert toke.equals("END_GROUP");
    String name = stoke.nextToken();
    if (debug) System.out.println(line+" -> "+current);
    assert name.equals( current.getName());
  }

  Element startObject(Element parent, String line) throws IOException {
    StringTokenizer stoke = new StringTokenizer(line, "=");
    String toke = stoke.nextToken();
    assert toke.equals("OBJECT");
    String name = stoke.nextToken();
    Element obj = new Element(name);
    parent.addContent( obj);
    return obj;
  }

  void endObject(Element current, String line) throws IOException {
    StringTokenizer stoke = new StringTokenizer(line, "=");
    String toke = stoke.nextToken();
    assert toke.equals("END_OBJECT");
    String name = stoke.nextToken();
    if (debug) System.out.println(line+" -> "+current);
    assert name.equals( current.getName());
  }

  void addField(Element parent, String line) throws IOException {
    StringTokenizer stoke = new StringTokenizer(line, "=");
    String name = stoke.nextToken();
    if (stoke.hasMoreTokens()) {
      Element field = new Element(name);
      parent.addContent(field);
      String value = stoke.nextToken();

      if (value.startsWith("(")) {
        parseValueCollection(field, value);
        return;
      }

      value = stripQuotes(value);
      field.addContent(value);
    }
  }

  void parseValueCollection(Element field, String value) {
    if (value.startsWith("(")) value = value.substring(1);
    if (value.endsWith(")")) value = value.substring(0,value.length()-1);
    StringTokenizer stoke = new StringTokenizer(value, "\",");
    while (stoke.hasMoreTokens()) {
      field.addContent(new Element("value").addContent( stripQuotes(stoke.nextToken())));
    }
  }


  String stripQuotes( String name) {
    if (name.startsWith("\"")) name = name.substring(1);
    if (name.endsWith("\"")) name = name.substring(0,name.length()-1);
    return name;
  } 


  static public void main(String args[]) throws IOException {
    ODLparser2 p = new ODLparser2();
    p.parseFile("c:/temp/odl.struct.txt");
  }
}
