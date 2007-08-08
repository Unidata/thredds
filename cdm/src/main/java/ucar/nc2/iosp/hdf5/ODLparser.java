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
 * @since Jul 23, 2007
 */
public class ODLparser {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ODLparser.class);

  private Document doc;
  private boolean debug = true;

  void parseFile(String filename) throws IOException {
    String text = new String(thredds.util.IO.readFileToByteArray(filename));
    parseString(text);
  }

  Element parseString(String text) throws IOException {
    Element rootElem = new Element("odl");
    doc = new Document(rootElem);

    StringTokenizer stoke = new StringTokenizer(text, "= \t\n\r\f");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken();
      if (toke.equals("GROUP"))
        rootElem.addContent(processGroup(stoke));
      else if (toke.equals("END"))
        break;
      else
        log.error("Not Group =" + toke);
    }

    if (debug) showDoc();
    return rootElem;
  }

  void showDoc() {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    try {
      fmt.output(doc, System.out);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  Element processGroup(StringTokenizer stoke) {
    String name = stoke.nextToken();

    Element elem = new Element(name);
    //elem.setAttribute("name", name);
    if (debug) System.out.println("Group "+name);

    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken();
      if (toke.equals("GROUP"))
        elem.addContent(processGroup(stoke));

      else if (toke.equals("END_GROUP")) {
        stoke.nextToken();
        return elem;

      } else if (toke.equals("CLASS"))
        elem.setAttribute("class", stoke.nextToken());

      else if (toke.equals("OBJECT"))
        elem.addContent(processObject(stoke));

      else if (toke.equals("GROUPTYPE"))
        elem.setAttribute("type", stoke.nextToken());

      else {
        elem.addContent(processContent(toke, stoke));
      }
    }
    log.warn("No END_GROUP for " + name);
    return elem;
  }

  Element processObject(StringTokenizer stoke) {
    Element elem = new Element("object");
    String name = stoke.nextToken();
    elem.setAttribute("name", name);
    if (debug) System.out.println("Object "+name);

    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken();
      if (toke.equals("OBJECT")) {
        elem.addContent(processObject(stoke));

      } else if (toke.equals("END_OBJECT")) {
        stoke.nextToken();
        return elem;

      } else if (toke.equals("GROUP")) {
        elem.addContent(processGroup(stoke));

      } else if (toke.equals("CLASS")) {
        elem.setAttribute("class", stoke.nextToken());

      } else if (toke.equals("NUM_VAL")) {
        elem.addContent(new Element("num_val").addContent(stoke.nextToken()));

      } else if (toke.equals("VALUE")) {
        elem.addContent(processValue(stoke));

      } else if (toke.equals("DataType")) {
        continue; // these have no value, so they screw up the parsing

      } else {
        elem.addContent(processContent(toke, stoke));
      }
    }
    log.warn("No END_OBJECT for " + name);
    return elem;
  }

  Element processContent(String name, StringTokenizer stoke) {
    Element elem = new Element(name);
    if (debug) System.out.println("content= "+name);

    String toke = stoke.nextToken();
    if (toke.startsWith("\"")) {
      elem.addContent(processString(toke, stoke, "\""));

    } else if (toke.startsWith("(")) {
      String s = processString(toke, stoke, ")");
      processStrings(s, elem);

    } else {
      elem.addContent(stripQuotes(toke));
    }

    return elem;
  }


  Element processValue(StringTokenizer stoke) {
    Element elem = new Element("value");

    String toke = stoke.nextToken();
    if (toke.startsWith("\"")) {
      elem.addContent(processString(toke, stoke, "\""));

    } else if (toke.startsWith("(")) {
      String s = processString(toke, stoke, ")");
      processStrings(s, elem);

    } else {
      elem.addContent(stripQuotes(toke));
    }

    return elem;
  }

  String processString(String start, StringTokenizer stoke, String delim) {
    StringBuffer sbuff = new StringBuffer(start);
    sbuff.deleteCharAt(0); // remove the starting char
    if (start.endsWith(delim)) {
      sbuff.deleteCharAt(sbuff.length() - 1); // remove the delim
      return sbuff.toString();
    }

    sbuff.append(" ");

    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken();
      sbuff.append(toke);
      if (toke.endsWith(delim)) {
        sbuff.deleteCharAt(sbuff.length() - 1); // remove the delim
        return sbuff.toString();
      }
      sbuff.append(" ");      
    }
    log.warn("No final \" for " + start);
    return sbuff.toString();
  }

  void processStrings(String s, Element e) {
    StringTokenizer stoke = new StringTokenizer(s, "\",");
    while (stoke.hasMoreTokens())
      e.addContent(new Element("value").addContent(stoke.nextToken()));
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
