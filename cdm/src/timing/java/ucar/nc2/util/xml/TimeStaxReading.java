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

package ucar.nc2.util.xml;

import ucar.unidata.util.Format;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.DataType;
import ucar.ma2.StructureDataIterator;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.List;

/**
 * Class Description.
 *
 * @author caron
 */
public class TimeStaxReading {
  static boolean show = false, process = true, showFields = false;

  XMLStreamReader r;
  int nmetars = 0;
  long nelems = Long.MAX_VALUE;
  boolean readFields = false;

  TimeStaxReading(XMLInputFactory myFactory, String filename) throws FileNotFoundException {
    long start = System.currentTimeMillis();

    InputStream in = new BufferedInputStream(new FileInputStream(filename));

    try {
      r = myFactory.createXMLStreamReader(in);
      int eventType = r.next();
      if (eventType == XMLStreamReader.START_ELEMENT) readElement();
    } catch (XMLStreamException e) {
      e.printStackTrace();
      String text = r.hasText() ? r.getText().trim() : "";
      System.out.println("BAD  text=(" + text + ")");
    }

    System.out.println("Read metar XML; # metars= " + nmetars);
    double took =  .001 * (System.currentTimeMillis() - start);
    System.out.println(" that took = " + took + "sec; "+ Format.d(nmetars/took,0)+" metars/sec");
    
    for (MetarField f : MetarField.fields.values())
      System.out.println(" "+f.name+ " = "+f.sum);
  }

  void readElement() throws XMLStreamException {
    tab++;
    indent();
    if (show) System.out.print(r.getLocalName());
    if (r.getLocalName().equals("metar")) nmetars++;
            
    int natts = r.getAttributeCount();
    String fldName = null;
    for (int i = 0; i < natts; i++) {
      String name = r.getAttributeLocalName(i);
      String val = r.getAttributeValue(i);
      if (show) System.out.print(" " + name + "='" + val + "'");
      if (name.equals("name")) fldName = val;
    }
    if (show) System.out.println();
    if (!readFields && r.getLocalName().equals("data")) {
      if (MetarField.fields.get(fldName) != null) readFields = true;
      else new MetarField(fldName);
    }

    while (r.hasNext() && (nmetars < nelems)) {
      int eventType = r.next();
      if (XMLStreamReader.END_ELEMENT == eventType)
        break;
      else if (XMLStreamReader.START_ELEMENT == eventType)
        readElement();
      else if (XMLStreamReader.CHARACTERS == eventType) {
        String text = r.hasText() ? r.getText().trim() : "";
        if (process && text.length() > 0) {
          MetarField fld = MetarField.fields.get(fldName);
          if (null != fld) fld.sum(text);
          indent();
          if (show) System.out.println("  text=(" + text + ")");
        }
      } else {
        String text = r.hasText() ? r.getText().trim() : "";
        String name = r.hasName() ? r.getLocalName() : "";
        indent();
        if (show) System.out.print(eventName(eventType) + ": " + name);
        if (text.length() > 0)
          if (show) System.out.print(" text=(" + text + ")");
        if (show) System.out.println();
      }
    }
    tab--;
    //if (count % 1000 == 0) System.out.println("did " + count);
  }

  int tab = 0;

  void indent() {
    if (!show) return;
    for (int i = 0; i < tab; i++)
      System.out.print("  ");
  }

  static String eventName(int type) {
    switch (type) {
      case XMLStreamReader.START_DOCUMENT:
        return " startDoc";
      case XMLStreamReader.START_ELEMENT:
        return "startElem";
      case XMLStreamReader.END_DOCUMENT:
        return "   endDoc";
      case XMLStreamReader.END_ELEMENT:
        return "  endElem";
      case XMLStreamReader.ATTRIBUTE:
        return "attribute";
      case XMLStreamReader.CHARACTERS:
        return "    chars";
      default:
        return " " + type;
    }
  }

  /* static HashMap<String,Field> fields = new HashMap<String,Field>();
  static class Field {
    String name;
    boolean isText;
    double sum = 0.0;

    Field( String name) {
      this.name = name;
      fields.put(name,this);
      if (showFields) System.out.println(name+" added");
    }

    void sum( StructureData sdata, StructureMembers.Member m) {
      if (m.getDataType() == DataType.DOUBLE)
        sum(sdata.getScalarDouble(m));
      else if (m.getDataType() == DataType.FLOAT)
        sum(sdata.getScalarFloat(m));
      else if (m.getDataType() == DataType.INT)
        sum(sdata.getScalarInt(m));
    }

    void sum(String text) {
      if (isText) return;
      try {
        sum( Double.parseDouble(text));
      } catch (NumberFormatException e) {
        if (showFields) System.out.println(name+" is text");
        isText = true;
      }
    }

    void sum(double d) {
      if (!Misc.closeEnough(d, -99999.0))
        sum += d; // LOOK kludge for missing data
    }
  }   */

  static void readFromNetcdf(String filename) throws IOException {
    long start = System.currentTimeMillis();

    NetcdfFile ncfile = NetcdfFile.open(filename);
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    Structure record = (Structure) ncfile.findVariable("record");
    List<Variable> members = record.getVariables();
    for (Variable v : members) {
      if (v.getDataType() != DataType.CHAR)
        new MetarField(v.getShortName());
    }

    StructureDataIterator siter = record.getStructureIterator();
    int count = 0;
    while (siter.hasNext()) {
      StructureData sdata = siter.next();
      List<StructureMembers.Member> sm = sdata.getMembers();
      for (StructureMembers.Member m : sm) {
        MetarField f = MetarField.fields.get(m.getName());
        if (null != f) f.sum( sdata, m);
      }
      count++;
    }

    System.out.println("Read from NetCDF; # metars= " + count);
    double took = .001 * (System.currentTimeMillis() - start);
    System.out.println(" that took = " + took + " sec; "+ Format.d(count/took,0)+" metars/sec");

    for (MetarField f : MetarField.fields.values())
      System.out.println(" "+f.name+ " = "+f.sum);
            
    ncfile.close();
  }

  public static void main(String args[]) throws XMLStreamException, IOException {

    XMLInputFactory myFactory = XMLInputFactory.newInstance();
    //myFactory.setXMLReporter(myXMLReporter);
    //myFactory.setXMLResolver(myXMLResolver);
    myFactory.setProperty("javax.xml.stream.isCoalescing", Boolean.TRUE);
    new TimeStaxReading(myFactory, "C:/TEMP/thredds.xml");

    /* String dir = "C:/doc/metarEncoding/save/";
    new TimeStaxReading(myFactory, dir+"xmlC.xml");

    readFromNetcdf(dir+"netcdfC.nc");

    readFromNetcdf(dir+"netcdfStreamC.nc"); */
  }

}
