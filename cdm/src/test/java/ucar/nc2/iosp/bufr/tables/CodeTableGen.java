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
package ucar.nc2.iosp.bufr.tables;

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
public class CodeTableGen {
  static String root = "C:/dev/tds/bufr/resources/resources/bufr/codes/";
  static String orgXml = root + "Code-FlagTables-11-2007.xml";
  static String trans1 = root + "Code-FlagTables-11-2007.trans1.xml";
  static String trans2 = root + "Code-FlagTables-11-2007.trans2.xml";

  Formatter out;
  BufferedReader dataIS;
  String line;

  CodeTableGen(String filename) throws IOException {
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

  //////////////////////////////////////////////////////////////////////////

  // try to pretty print the WORD xml

  static public void prettyPrint() throws IOException {
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

  ///////////////////////////////////////////////////////////////////////

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

  // tranform the WORD xml into something more pareseable
  // unfortunately, we then have to hand-edit the result
  // next time, probably beter to start with the NCEP HTML pages
  static public void passOne() throws IOException {
    org.jdom.Document orgDoc;
    try {
      SAXBuilder builder = new SAXBuilder();
      orgDoc = builder.build(orgXml);

      org.jdom.Document tdoc = new org.jdom.Document();
      Element root = new Element("tdoc");
      tdoc.setRootElement(root);
      transform(orgDoc.getRootElement(), root);

      /* XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
  Writer pw = new FileWriter("C:/docs/bufr/wmo/Code-FlagTables-11-2007.trans.xml");
  fmt.output(tdoc, pw);
  pw = new PrintWriter(System.out);
  fmt.output(tdoc, pw);  // */


    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////


  static private void transform2(Element root, Element nroot) {
    String lastRtext = null;
    for (Element e : (List<Element>) root.getChildren("sub-section")) {
      lastRtext = subSection(e, lastRtext, nroot);
    }
  }

  static private String subSection(Element elem, String lastRtext, Element nroot) {

    List<Element> rElems = (List<Element>) elem.getChildren("r");
    List<Element> tcElems = (List<Element>) elem.getChildren("tc");
    String desc = rElems.size() > 0 ? rElems.get(0).getText() : "unknown";

    if ((lastRtext == null) || lastRtext.equals("0 20 003") || // lastRtext.equals("0 31 021") ||
        lastRtext.equals("0 31 031") || lastRtext.equals("0 35 000")) {
      f.format("skip %s %s %n", lastRtext, desc);

    } else {
      Element tableElem = new Element("table");
      nroot.addContent(tableElem);
      tableElem.setAttribute("name", lastRtext);
      tableElem.setAttribute("desc", desc);
      f.format("------%n%s == %s%n", lastRtext, desc);

      if (tcElems.size()>0) {
        String kind;
        String kinds = tcElems.get(0).getText().toLowerCase();
        if (kinds.startsWith("code")) kind = "code";
        else if (kinds.startsWith("bit")) kind = "bit";
        else kind="unknown";
        tableElem.setAttribute("kind", kind);
      }

      for (int i = 2; i < tcElems.size(); i += 2) { // skip first 2
        String value = tcElems.get(i).getText();
        String text = (i + 1 < tcElems.size()) ? tcElems.get(i + 1).getText() : "unknown";

        Element codeElem = new Element("code");
        tableElem.addContent(codeElem);
        codeElem.setAttribute("value", value);
        codeElem.addContent(text);

        f.format("%s == %s %n", value, text);
      }
    }
    return (rElems.size() > 0) ? rElems.get(rElems.size() - 1).getText() : null;
  }


  // pass 2 - transform the hand-edited XML to its final form
  static public void passTwo() throws IOException {
    org.jdom.Document tdoc;
    try {
      SAXBuilder builder = new SAXBuilder();
      tdoc = builder.build(trans1);

      org.jdom.Document ndoc = new org.jdom.Document();
      Element nroot = new Element("ndoc");
      ndoc.setRootElement(nroot);

      transform2(tdoc.getRootElement(), nroot);

      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      Writer pw = new FileWriter(trans2);
      fmt.output(ndoc, pw);
      pw = new PrintWriter(System.out);
      fmt.output(ndoc, pw);

    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
  }

  ///////////////////////////////////////////////////////////////////////////

  static private void transform3(Element root) {
    for (Element elem : (List<Element>) root.getChildren("table")) {
      boolean tableShown = false;

      String name = elem.getAttributeValue("name");
      String desc = elem.getAttributeValue("desc");

      List<Element> cElems = (List<Element>) elem.getChildren("code");
      if (cElems.size() == 0) {
        //f.format("skip %s %n", name);
        continue;
      }

      String kind = elem.getAttributeValue("kind");
      if ((kind == null) || kind.equals("unknown")) {
        f.format("%nTable %s %s kind=%s %n", name, desc, kind);
        tableShown = true;
      }
      
      for (Element cElem : cElems) {
        String value = cElem.getAttributeValue("value").trim();
        String text = cElem.getText();
        if (text.toLowerCase().startsWith("reserved"))
          ; // f.format(" skip code %s == %s %n", value, text);
        else if (text.toLowerCase().startsWith("not used"))
          ; // f.format(" skip code %s == %s %n", value, text);
         else {
          try {
            Integer.parseInt(value);
          } catch (NumberFormatException e) {
            if (!tableShown)
              f.format("%nTable %s %s kind=%s %n", name, desc, kind);
            tableShown = true;
            
            if (0 == parseAll(value)) {
              f.format(" problem parsing code %s == %s %n", value, text);
            }
          }
        }
      }
    }
  }

  static int parseAll(String text) {
    String[] tok = text.split(" ");
    if (tok.length != 2) return 0;
    if (!tok[0].equalsIgnoreCase("all")) return 0;
    try {
      int n = Integer.parseInt(tok[1]);
      int n2 = (int) Math.pow(2,n)-1;
      f.format(" parse %s == %d %n", text, n2);
      return n2;
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  // pass 3 - look for problems
  static public void passThree() throws IOException {
    org.jdom.Document tdoc;
    try {
      SAXBuilder builder = new SAXBuilder();
      tdoc = builder.build(trans2);

      /* org.jdom.Document ndoc = new org.jdom.Document();
      Element nroot = new Element("ndoc");
      ndoc.setRootElement(nroot);  */

      transform3(tdoc.getRootElement());

      /*  XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
   Writer pw = new FileWriter("C:/docs/bufr/wmo/Code-FlagTables-11-2007.trans2.xml");
   fmt.output(ndoc, pw);
   pw = new PrintWriter(System.out);
   fmt.output(ndoc, pw);  */

    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
  }

  ////////////////////////////////////////////////////////////////

  static Formatter f = new Formatter(System.out);


  static public void main(String args[]) throws IOException {
    //passTwo();
    passThree();
  }

}


