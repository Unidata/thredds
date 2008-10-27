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

package ucar.nc2.util.xml;

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.iosp.bufr.MessageScanner;
import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.ma2.*;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.zip.GZIPOutputStream;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 5, 2008
 */
public class TimeBufr2Xml {

  /////////////////////////////////////////////////////////////////////

  interface MClosure {
    void run(String filename) throws IOException;
  }

  void test(String filename, MClosure closure) throws IOException {
    File f = new File(filename);
    if (!f.exists()) {
      System.out.println(filename + " does not exist");
      return;
    }
    if (f.isDirectory()) testAllInDir(f, closure);
    else {
      try {
        closure.run(f.getPath());
      } catch (Exception ioe) {
        System.out.println("Failed on " + f.getPath() + ": " + ioe.getMessage());
        ioe.printStackTrace();
      }
    }
  }


  void testAllInDir(File dir, MClosure closure) {
    List<File> list = Arrays.asList(dir.listFiles());
    Collections.sort(list);

    for (File f : list) {
      if (f.getName().endsWith("bfx")) continue;
      if (f.getName().endsWith("txt")) continue;
      if (f.getName().endsWith("zip")) continue;
      if (f.getName().endsWith("csh")) continue;
      if (f.getName().endsWith("rtf")) continue;

      if (f.isDirectory())
        testAllInDir(f, closure);
      else {
        try {
          closure.run(f.getPath());
        } catch (Exception ioe) {
          System.out.println("Failed on " + f.getPath() + ": " + ioe.getMessage());
          ioe.printStackTrace();
        }
      }
    }
  }

  int scan(String filename) throws IOException {
    long start = System.nanoTime();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    out.format("\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    int bad = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      //if (count == 0) new BufrDump2().dump(out, m);

      if (!m.isTablesComplete()) {
        out.format("**INCOMPLETE%n");
        bad++;
        continue;
      }

      int nbitsCounted = m.getTotalBits();
      if (!(Math.abs(m.getCountedDataBytes()- m.dataSection.getDataLength()) <= 1)) {
        out.format("**BitCount Fails expect=%d != dataLength=%d%n", m.getCountedDataBytes(), m.dataSection.getDataLength());
        bad++;
        continue;
      }

      byte[] mbytes = scan.getMessageBytesFromLast(m);

      NetcdfFile ncfile = NetcdfFile.openInMemory("test", mbytes);
      NetcdfDataset ncd = new NetcdfDataset(ncfile);

      writeMessage(m, ncd);
      count++;
    }
    raf.close();

    long took = (System.nanoTime() - start);
    double rate = (took > 0) ? ((double) (1000 * 1000) * count / took) : 0.0;
    out.format("----nmsgs= %d bad=%d nobs = %d took %d msecs rate = %f msgs/msec\n", count, bad, scan.getTotalObs(), took / (1000 * 1000), rate);
    return scan.getTotalObs();
  }

  void writeMessage(Message message, NetcdfFile ncfile) {
    Indent indent = new Indent(1);
    indent.setIndentLevel(1);

    try {
      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeStartElement("bufrMessage");
      staxWriter.writeAttribute("nobs", Integer.toString(message.getNumberDatasets())); 
      indent.incr();

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());

      staxWriter.writeStartElement("center");
      staxWriter.writeCharacters(message.getCenterName());
      staxWriter.writeEndElement();

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeStartElement("table");
      staxWriter.writeCharacters(message.getTableName());
      staxWriter.writeEndElement();

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeStartElement("category");
      staxWriter.writeCharacters(message.getCategoryFullName());
      staxWriter.writeEndElement();

      Structure obs = (Structure) ncfile.findVariable("obsRecord");
      ArrayStructure obsData = (ArrayStructure) obs.read();
      StructureDataIterator sdataIter = obsData.getStructureDataIterator();
      while (sdataIter.hasNext()) {
        StructureData sdata = sdataIter.next();
        staxWriter.writeCharacters("\n");
        staxWriter.writeCharacters(indent.toString());
        staxWriter.writeStartElement("obs");
        indent.incr();

        for (StructureMembers.Member m : sdata.getMembers()) {
          Array mdata = sdata.getArray(m);
          Variable v = obs.findVariable(m.getName());

          if (m.getDataType().isString() || m.getDataType().isNumeric()) {
            writeVariable((VariableDS) v, mdata, indent);

          } else if (m.getDataType() == DataType.STRUCTURE) {
            writeStructureArray((Structure) v, (ArrayStructure) mdata, indent);

          } else if (m.getDataType() == DataType.SEQUENCE) {
            writeSequence((Structure) v, (ArraySequence) mdata, indent);
          }
        }

        indent.decr();
        staxWriter.writeCharacters("\n");
        staxWriter.writeCharacters(indent.toString());
        staxWriter.writeEndElement();
      }

      // ending
      indent.decr();
      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeEndElement();

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  void writeStructureArray(Structure s, ArrayStructure data, Indent indent) throws IOException, XMLStreamException {
    StructureDataIterator sdataIter = data.getStructureDataIterator();
    while (sdataIter.hasNext()) {
      StructureData sdata = sdataIter.next();
      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeStartElement(s.getShortName());

      indent.incr();
      for (StructureMembers.Member m : sdata.getMembers()) {
        Variable v = s.findVariable(m.getName());

        if (m.getDataType().isString() || m.getDataType().isNumeric()) {
          writeVariable((VariableDS) v, sdata.getArray(m), indent);
        }
      }
      indent.decr();

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeEndElement();
    }
  }

  void writeSequence(Structure s, ArraySequence data, Indent indent) throws IOException, XMLStreamException {
    StructureDataIterator sdataIter = data.getStructureDataIterator();
    while (sdataIter.hasNext()) {
      StructureData sdata = sdataIter.next();
      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeStartElement(s.getShortName());

      indent.incr();
      for (StructureMembers.Member m : sdata.getMembers()) {
        Variable v = s.findVariable(m.getName());

        if (m.getDataType().isString() || m.getDataType().isNumeric()) {
          writeVariable((VariableDS) v, sdata.getArray(m), indent);
        }
      }
      indent.decr();

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeEndElement();
    }
  }

  void writeVariable(VariableDS v, Array mdata, Indent indent) throws XMLStreamException, IOException {
    staxWriter.writeCharacters("\n");
    staxWriter.writeCharacters(indent.toString());

    /* small option
 staxWriter.writeStartElement("var");
 String name = v.findAttribute("BUFR:TableB_descriptor").getStringValue();
 staxWriter.writeAttribute("name", name); // */

    // complete option
    staxWriter.writeStartElement("data");
    String name = v.getShortName();
    staxWriter.writeAttribute("name", name);

    String units = v.getUnitsString();
    if ((units != null) && !units.equals(name) && !units.startsWith("Code"))
      staxWriter.writeAttribute("units", v.getUnitsString()); 

    String desc = v.findAttribute("BUFR:TableB_descriptor").getStringValue();
    staxWriter.writeAttribute("Bufr", desc); // */

    // write data value
    if (v.getDataType().isNumeric()) {
      mdata.hasNext();
      double val = mdata.nextDouble();

      if (v.isMissing(val)) {
        staxWriter.writeCharacters("missing");

      } else if ((v.getDataType() == DataType.FLOAT) || (v.getDataType() == DataType.DOUBLE)) {
        Attribute bitWidthAtt = v.findAttribute("BUFR:bitWidth");
        int bitWidth = bitWidthAtt.getNumericValue().intValue();
        double sigDigitsD = Math.log10(2 << bitWidth);
        int sigDigits = (int) (sigDigitsD + 1);

        Formatter stringFormatter = new Formatter();
        String format = "%." + sigDigits + "g";
        stringFormatter.format(format, val);
        staxWriter.writeCharacters(stringFormatter.toString());
        
      } else {  // numeric, not float
        staxWriter.writeCharacters(mdata.toString());
      }

    } else {  // not numeric
      staxWriter.writeCharacters(mdata.toString());
    }


    staxWriter.writeEndElement();
  }

  private static class Indent {
    private int nspaces = 0;
    private int level = 0;
    private StringBuilder blanks;
    private String indent = "";

    // nspaces = how many spaces each level adds.
    // max 100 levels
    public Indent(int nspaces) {
      this.nspaces = nspaces;
      blanks = new StringBuilder();
      for (int i = 0; i < 100 * nspaces; i++)
        blanks.append(" ");
    }

    public Indent incr() {
      level++;
      setIndentLevel(level);
      return this;
    }

    public Indent decr() {
      level--;
      setIndentLevel(level);
      return this;
    }

    public String toString() {
      return indent;
    }

    public void setIndentLevel(int level) {
      this.level = level;
      indent = blanks.substring(0, level * nspaces);
    }
  }


  Formatter out = new Formatter(System.out);
  XMLStreamWriter staxWriter;

  void read2xml() throws IOException, XMLStreamException {
    XMLOutputFactory fac = XMLOutputFactory.newInstance();

    String fileout = "D:/bufr/out/test5.xml.gzip";
    FileOutputStream fos = new FileOutputStream(fileout);
    GZIPOutputStream zos = new GZIPOutputStream(fos);

    //staxWriter = fac.createXMLStreamWriter(System.out, "UTF-8");
    //staxWriter = fac.createXMLStreamWriter(fos, "UTF-8");
    staxWriter = fac.createXMLStreamWriter(zos, "UTF-8");

    staxWriter.writeStartDocument("UTF-8", "1.0");
    staxWriter.writeCharacters("\n");
    staxWriter.writeStartElement("bufrMessages");

    String filename = "D:/bufr/mlodeSorted/IUSUV1KWBC.bufr";
    //String filename = "D:/bufr/out/IUACRJTD-1.bufr";
    //String filename = "D:/bufr/mlodeSorted/ISXAB40KWNO.bufr";
    test(filename, new MClosure() {
      public void run(String filename) throws IOException {
        scan(filename);
      }
    });

    staxWriter.writeCharacters("\n");
    staxWriter.writeEndDocument();
    staxWriter.flush();
    zos.close();

    out.flush();
  }

  static public void main(String args[]) throws IOException, XMLStreamException {
    TimeBufr2Xml test = new TimeBufr2Xml();
    test.read2xml();

  }

}
