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
package ucar.nc2.iosp.hdf4;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.io.IOException;
import java.util.StringTokenizer;

import ucar.nc2.util.IO;

/**
 * Turn ODL into XML
 * @author caron
 * @since Aug 7, 2007
 */
public class ODLparser {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ODLparser.class);

  private Document doc;
  private boolean debug = false, showRaw = false, show = false;

  void showDoc(java.io.OutputStream out) {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    try {
      fmt.output(doc, out);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void parseFile(String filename) throws IOException {
    String text = new String(IO.readFileToByteArray(filename));
    parseFromString(text);
  }

  public Element parseFromString(String text) throws IOException {
    if (showRaw) System.out.println("Raw ODL=\n"+text);
    
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

    if (show) showDoc(System.out);
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
    assert name.equals( current.getName()) : name +" !+ "+ current.getName();
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
    ODLparser p = new ODLparser();
    p.parseFile("c:/temp/odl.struct.txt");
  }
}
