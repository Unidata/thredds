package ucar.util.prefs;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import ucar.nc2.util.IO;
import ucar.nc2.util.Indent;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Describe
 *
 * @author caron
 * @since 6/23/12
 */
public class Convert2Version2 {
  String filename;
  Convert2Version2(String filename) {
    this.filename = filename;
  }

  void convert() throws IOException {
    org.jdom2.Document doc;
    InputStream is = null;
    try {
      is = new FileInputStream(filename);
      SAXBuilder builder = new SAXBuilder(false);
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    } finally {
      if (is != null) is.close();
    }

    if (false) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("***Convert2Version2 showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
    }

    Element root = doc.getRootElement();
    check(root, new Indent(2));

    /*
    org.jdom.Document convertDoc = new Document();
    Element convertRoot = new Element("java");
    convertRoot.setAttribute("version", "1.4.1_01");
    convertRoot.setAttribute("class", "java.beans.XMLDecoder");
    convertDoc.setRootElement(convertRoot);
    add(root, convertRoot);

    System.out.printf("%n************************************%n");
    if (true) {
      XMLOutputter xmlOut = new XMLOutputter();
      xmlOut.setFormat(Format.getPrettyFormat());
      System.out.println("***Convert2Version2 converted = \n" + xmlOut.outputString(convertDoc) + "\n*******");
    }  */
  }

  void add(Element elem, Element parent) {
    if (elem.getName().equals("object"))
      parent.addContent( (Element) elem.clone());

    for (Object child : elem.getChildren()) {
      add((Element) child, parent);
    }
  }

  void show(Element e, Indent indent) {
    String name = e.getName();
    if (name.equals("beanObject")) {
      String key = e.getAttributeValue("key");
      Element obj = e.getChild("object");
      String c = obj.getAttributeValue("class");
      System.out.printf("%s%s key=%s class=%s%n", indent, name, key, c);

    } else if (name.equals("beanCollection")) {
      String key = e.getAttributeValue("key");
      String c = e.getAttributeValue("class");
      System.out.printf("%s%s key=%s class=%s%n", indent, name, key, c);

    } else if (name.equals("node")) {
      String nname = e.getAttributeValue("name");
      System.out.printf("%n%s%s %s%n", indent, name, nname);

    } else if (name.equals("entry")) {
      String key = e.getAttributeValue("key");
      String value = e.getAttributeValue("value");
      System.out.printf("%s%s key=%s value=%s%n", indent, name, key, value);

    } else {
      System.out.printf("%s%s%n", indent, name);
    }

    if (!name.equals("beanObject") && !name.equals("beanCollection")) {
      indent.incr();
      for (Object co : e.getChildren())
        show((Element)co,indent);
      indent.decr();
    }
  }

  void check(Element e, Indent indent) {
    String name = e.getName();
    if (name.equals("beanObject")) {
      String key = e.getAttributeValue("key");
      Element obj = e.getChild("object");
      assert obj != null;
      String c = obj.getAttributeValue("class");
      System.out.printf("%s%s key=%s class=%s%n", indent, name, key, c);

    }

      for (Object co : e.getChildren())
        check((Element)co,indent);
  }


  /** testing */
  public static void main(String args[]) throws IOException {
    String test = "C:\\Users\\caron\\.unidata\\NetcdfUI22.xml";
    Convert2Version2 im = new Convert2Version2(test);
    im.convert();
  }

}
