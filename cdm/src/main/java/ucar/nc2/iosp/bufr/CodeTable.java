/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.bufr;

import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.Formatter;
import java.util.List;

/**
 * @author caron
 * @since Jul 10, 2008
 */
public class CodeTable {
  Formatter out;
  BufferedReader dataIS;
  String line;

  CodeTable(String filename) throws IOException {
    out = new Formatter(System.out);
    dataIS = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
    getNextLine();

    int count = 0;
    while (readCodeTable() && count < 100) {
      out.format("%d ", count);
      count++;
    }

    out.flush();
    dataIS.close();
  }

  private boolean getNextLine() throws IOException {
    line = dataIS.readLine();
    return (line != null);
  }

  private boolean readCodeTable() throws IOException {
    int x = Integer.parseInt(line.substring(0, 3));
    int y = Integer.parseInt(line.substring(3, 6));
    int ncodes = Integer.parseInt(line.substring(7, 11)); // incorrect
    out.format("Code 0-%d-%d ncodes=%d %n", x, y, ncodes);

    int countCodes = 0;
    boolean first = true;
    while (readOneCode(first)) {
      if (!getNextLine()) return false;
      first = false;
      countCodes++;
    }

    if (countCodes != ncodes) out.format("*** Really %d codes %n", countCodes);

    return true;
  }

  private boolean readOneCode(boolean first) throws IOException {
    if (!first && line.substring(0, 3).trim().length() != 0) return false; //

    int code = Integer.parseInt(line.substring(12, 16));
    int nlines = Integer.parseInt(line.substring(17, 19));
    String value = line.substring(20);
    if (nlines > 1) {
      for (int j = 1; j < nlines; j++) {
        if (!getNextLine()) return false;
        //value += " ";
        value += line.substring(20);
      }
    }
    out.format("   code %d = %s %n", code, value);
    return true;
  }


  static public void main2(String args[]) throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build("C:/docs/bufr/wmo/Code-FlagTables-11-2007.xml");

      Format pretty = Format.getPrettyFormat();
      String sep = pretty.getLineSeparator();
      String ind = pretty.getIndent();
      String mine = "\r\n";
      pretty.setLineSeparator(mine);

      // wierd - cant pretty print ??!!
      XMLOutputter fmt = new XMLOutputter(pretty);
      Writer pw = new FileWriter("C:/docs/bufr/wmo/wordNice.txt");
      fmt.output(doc, pw);

    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

  }


  static public void transform(Element elem, Element telem) {
    String name = elem.getName();

    if (name.equals("sub-section") || name.equals("tc")) {
      Element nelem = new Element(name);
      telem.addContent(nelem);
      telem = nelem;
    }

    if (name.equals("r") && !hasAncestor(telem, "tc")) {
      Element nelem = new Element(name);
      telem.addContent(nelem);
      telem = nelem;
    }

    String s = elem.getText();
    if (s != null) {
      s = s.trim();
      if (s.length() > 0)
        telem.addContent(s);
    }

    if (name.equals("noBreakHyphen")) {
      telem.addContent("-");
      return;
    }

    for (Object o : elem.getChildren()) {
      Element e = (Element) o;
      transform(e, telem);
    }
  }

  static boolean hasAncestor(Element e, String name) {
    while (e != null) {
      if (e.getName().equals(name)) return true;
      e = e.getParentElement();
    }
    return false;
  }

  static Formatter f = new Formatter(System.out);

  static private void transform2(Element root) {
    String lastRtext = null;
    for (Element e : (List<Element>) root.getChildren("sub-section")) {
      lastRtext = subSection(e, lastRtext);
    }
  }

  static private String subSection(Element elem, String lastRtext) {
    List<Element> rElems = (List<Element>) elem.getChildren("r");
    List<Element> tcElems = (List<Element>) elem.getChildren("tc");
    String desc = rElems.size() > 0 ? rElems.get(0).getText() : "unknown";
    f.format("------%n%s == %s%n", lastRtext, desc);
    for (int i = 2; i < tcElems.size(); i += 2) { // skip first 2
      String value = tcElems.get(i).getText();
      String text = (i + 1 < tcElems.size()) ? tcElems.get(i + 1).getText() : "unknown";
      f.format("%s == %s %n", value, text);
    }
    return (rElems.size() > 0) ? rElems.get(rElems.size() - 1).getText() : null;
  }


  static public void main3(String args[]) throws IOException {
     org.jdom.Document orgDoc;
     try {
       SAXBuilder builder = new SAXBuilder();
       orgDoc = builder.build("C:/docs/bufr/wmo/Code-FlagTables-11-2007.xml");

       org.jdom.Document tdoc = new org.jdom.Document();
       Element root = new Element("tdoc");
       tdoc.setRootElement(root);
       transform(orgDoc.getRootElement(), root);

       /* XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
       Writer pw = new FileWriter("C:/docs/bufr/wmo/Code-FlagTables-11-2007.trans.xml");
       fmt.output(tdoc, pw);
       pw = new PrintWriter(System.out);
       fmt.output(tdoc, pw);  // */

       transform2(root);

     } catch (JDOMException e) {
       throw new IOException(e.getMessage());
     }


   }

  static public void main(String args[]) throws IOException {
     org.jdom.Document tdoc;
     try {
       SAXBuilder builder = new SAXBuilder();
       tdoc = builder.build("C:/docs/bufr/wmo/Code-FlagTables-11-2007.trans1.xml");
       transform2(tdoc.getRootElement());

       /* XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
       Writer pw = new FileWriter("C:/docs/bufr/wmo/Code-FlagTables-11-2007.trans2.xml");
       fmt.output(tdoc, pw);
       pw = new PrintWriter(System.out);
       fmt.output(tdoc, pw);  // */

     } catch (JDOMException e) {
       throw new IOException(e.getMessage());
     }


   }

 }


