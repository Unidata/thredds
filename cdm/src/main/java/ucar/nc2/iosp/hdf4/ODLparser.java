/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf4;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.Format;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import ucar.nc2.constants.CDM;
import ucar.nc2.util.IO;

/**
 * Turn ODL into XML
 * @author caron
 * @since Aug 7, 2007
 */

/*
 http://newsroom.gsfc.nasa.gov/sdptoolkit/hdfeosfaq.html

 3.2 What types of metadata are embedded in an HDF-EOS file and what are the added storage requirements?
 An HDF-EOS file must contain ECS "core" metadata which is essential for ECS search services. Core metadata are populated
  using the SDP Toolkit, rather than through HDF-EOS calls. "Archive" metadata (supplementary information included by the
  data provider) may also be present. If grid, point, or swath data types have been used, there also will be structural
  metadata describing how these data types have been translated into standard HDF data types. Metadata resides in
  human-readable form in the Object Descriptor Language (ODL). Structural metadata uses 32K of storage, regardless of
  the amount actually required. The sizes of the core and archive metadata vary depending on what has been entered by the user.

 3.3 What are the options for adding ECS metadata to standard HDF files?
 For data products that will be accessed by ECS but which remain in native HDF, there is a choice of
 1) adding no ECS metadata in the HDF file,
 2) inserting ECS metadata into the HDF file, or
 3) "appending" ECS metadata to the HDF file. "Append" means updating the HDF location table so that the appended metadata
 becomes known to the HDF libraries/tools.

 3.4 Some DAACs currently provide descriptor files that give background information about the data. Will this information be included in an HDF-EOS file?
 Yes. The descriptor file will be retained. It can be viewed by EOSView if it stored either as a global attribute or a file annotation.

 */
public class ODLparser {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ODLparser.class);
  private static boolean debug = false, showRaw = false, show = false;

  private Document doc;

  void showDoc(PrintWriter out) {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    try {
      fmt.output(doc, out);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void parseFile(String filename) throws IOException {
    String text = new String(IO.readFileToByteArray(filename), CDM.utf8Charset);
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
      if (line == null) continue;

      if (line.startsWith("GROUP")) {
        current = startGroup(current, line);

      } else if (line.startsWith("OBJECT")) {
        current = startObject(current, line);

      } else if (line.startsWith("END_OBJECT")) {
        endObject( current, line);
        current = current.getParentElement();
        if (current == null) throw new IllegalStateException();

      } else if (line.startsWith("END_GROUP")) {
        endGroup( current, line);
        current = current.getParentElement();
        if (current == null) throw new IllegalStateException();

      } else {
        addField( current, line);
      }
    }

    if (show) showDoc(new PrintWriter( new OutputStreamWriter(System.out, CDM.utf8Charset)));
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
