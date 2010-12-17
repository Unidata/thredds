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
package ucar.nc2.iosp.bufr.writer;

import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.iosp.bufr.BufrIosp;
import ucar.nc2.iosp.bufr.MessageScanner;
import ucar.nc2.*;
import ucar.nc2.util.Indent;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.SequenceDS;
import ucar.nc2.dataset.StructureDS;
import ucar.ma2.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Formatter;

/**
 * Write BUFR to an ad-hoc XML format
 *
 * @author caron
 * @since Aug 9, 2008
 */
public class Bufr2Xml {

  // private Formatter out = new Formatter(System.out);
  private XMLStreamWriter staxWriter;
  private Indent indent;
  private boolean skipMissing;

  public Bufr2Xml(Message message, NetcdfDataset ncfile, OutputStream os, boolean skipMissing) throws IOException {
    this.skipMissing = skipMissing;

    indent = new Indent(2);
    indent.setIndentLevel(0);

    try {
      XMLOutputFactory fac = XMLOutputFactory.newInstance();
      staxWriter = fac.createXMLStreamWriter(os, "UTF-8");

      staxWriter.writeStartDocument("UTF-8", "1.0");
      //staxWriter.writeCharacters("\n");
      //staxWriter.writeStartElement("bufrMessage");

      writeMessage(message, ncfile);

      staxWriter.writeCharacters("\n");
      staxWriter.writeEndDocument();
      staxWriter.flush();

    } catch (XMLStreamException e) {
      throw new IOException(e.getMessage());
    }
  }

  void writeMessage(Message message, NetcdfDataset ncfile) {

    try {
      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeStartElement("bufrMessage");
      staxWriter.writeAttribute("nobs", Integer.toString(message.getNumberDatasets()));
      indent.incr();

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeStartElement("edition");
      staxWriter.writeCharacters(Integer.toString(message.is.getBufrEdition()));
      staxWriter.writeEndElement();

      String header = message.getHeader().trim();
      if (header.length() > 0) {
        staxWriter.writeCharacters("\n");
        staxWriter.writeCharacters(indent.toString());
        staxWriter.writeStartElement("header");
        staxWriter.writeCharacters(header);
        staxWriter.writeEndElement();
      }

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeStartElement("tableVersion");
      staxWriter.writeCharacters(message.getTableName());
      staxWriter.writeEndElement();

      staxWriter.writeStartElement("center");
      staxWriter.writeCharacters(message.getCenterName());
      staxWriter.writeEndElement();

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeStartElement("category");
      staxWriter.writeCharacters(message.getCategoryFullName());
      staxWriter.writeEndElement();

      SequenceDS obs = (SequenceDS) ncfile.findVariable(BufrIosp.obsRecord);
      StructureDataIterator sdataIter = obs.getStructureIterator(-1);

      writeSequence(obs, sdataIter);

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

  /* void writeStructureArray(StructureDS s, ArrayStructure data, Indent indent) throws IOException, XMLStreamException {
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

        } else if (m.getDataType() == DataType.STRUCTURE) {
          writeStructureArray((StructureDS) v, (ArrayStructure) sdata.getArray(m), indent);

        } else if (m.getDataType() == DataType.SEQUENCE) {
          writeSequence((SequenceDS) v, (ArraySequence) sdata.getArray(m), indent);
        }

        if (m.getDataType().isString() || m.getDataType().isNumeric()) {
          writeVariable((VariableDS) v, sdata.getArray(m), indent);
        }
      }
      indent.decr();

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeEndElement();
    }
  } */

  // iterate through the observations

  private void writeSequence(StructureDS s, StructureDataIterator sdataIter) throws IOException, XMLStreamException {

    int count = 0;
    while (sdataIter.hasNext()) {
      //out.format("%sSequence %s count=%d%n", indent, s.getShortName(), count++);
      StructureData sdata = sdataIter.next();

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeStartElement("struct");
      staxWriter.writeAttribute("name", StringUtil.quoteXmlAttribute(s.getShortName()));
      staxWriter.writeAttribute("count", Integer.toString(count++));

      for (StructureMembers.Member m : sdata.getMembers()) {
        Variable v = s.findVariable(m.getName());
        indent.incr();

        if (m.getDataType().isString() || m.getDataType().isNumeric()) {
          writeVariable((VariableDS) v, sdata.getArray(m));

        } else if (m.getDataType() == DataType.STRUCTURE) {
          StructureDS sds = (StructureDS) v;
          ArrayStructure data = (ArrayStructure) sdata.getArray(m);
          writeSequence(sds, data.getStructureDataIterator());

        } else if (m.getDataType() == DataType.SEQUENCE) {
          SequenceDS sds = (SequenceDS) v;
          ArraySequence data = (ArraySequence) sdata.getArray(m);
          writeSequence(sds, data.getStructureDataIterator());
        }
        indent.decr();
      }

      staxWriter.writeCharacters("\n");
      staxWriter.writeCharacters(indent.toString());
      staxWriter.writeEndElement();
    }
  }

  void writeVariable(VariableDS v, Array mdata) throws XMLStreamException, IOException {
    staxWriter.writeCharacters("\n");
    staxWriter.writeCharacters(indent.toString());

    // complete option
    staxWriter.writeStartElement("data");
    String name = v.getShortName();
    staxWriter.writeAttribute("name", StringUtil.quoteXmlAttribute(name));

    String units = v.getUnitsString();
    if ((units != null) && !units.equals(name) && !units.startsWith("Code"))
      staxWriter.writeAttribute("units", StringUtil.quoteXmlAttribute(v.getUnitsString()));

    Attribute att = v.findAttribute("BUFR:TableB_descriptor");
    String desc = (att == null) ? "N/A" : att.getStringValue();
    staxWriter.writeAttribute("bufr", StringUtil.quoteXmlAttribute(desc)); // */

    if (v.getDataType() == DataType.CHAR) {
      ArrayChar ac = (ArrayChar) mdata;
      staxWriter.writeCharacters(ac.getString()); // turn into a string

    } else {

      int count = 0;
      mdata.resetLocalIterator();
      while (mdata.hasNext()) {
        if (count > 0) staxWriter.writeCharacters(" ");
        count++;

        if (v.getDataType().isNumeric()) {
          double val = mdata.nextDouble();

          if (v.isMissing(val)) {
            staxWriter.writeCharacters("missing");

          } else if ((v.getDataType() == DataType.FLOAT) || (v.getDataType() == DataType.DOUBLE)) {
            writeFloat(v, val);

          } else {  // numeric, not float
            staxWriter.writeCharacters(mdata.toString());
          }

        } else {  // not numeric
          String s = StringUtil.filter7bits(mdata.next().toString());
          staxWriter.writeCharacters(StringUtil.quoteXmlContent(s));
        }
      }
    }


    staxWriter.writeEndElement();
  }

  private void writeFloat(Variable v, double val) throws XMLStreamException {
    Attribute bitWidthAtt = v.findAttribute("BUFR:bitWidth");
    int sigDigits;
    if (bitWidthAtt == null)
      sigDigits = 7;
    else {
      int bitWidth = bitWidthAtt.getNumericValue().intValue();
      if (bitWidth < 30) {
        double sigDigitsD = Math.log10(2 << bitWidth);
        sigDigits = (int) (sigDigitsD + 1);
      } else {
        sigDigits = 7;
      }
    }

    Formatter stringFormatter = new Formatter();
    String format = "%." + sigDigits + "g";
    stringFormatter.format(format, val);
    staxWriter.writeCharacters(stringFormatter.toString());
  }

  public static void main(String arg[]) throws Exception {

    //String filename = "C:/temp/cache/uniqueMessages.bufr";
    String filename = "C:/data/formats/bufr/uniqueExamples.bufr";
    Message message = null;
    RandomAccessFile raf = null;
    OutputStream out = null;
    int size = 0;


    try {

      int count = 0;
      raf = new RandomAccessFile(filename, "r");
      MessageScanner scan = new MessageScanner(raf, 0);
      while (scan.hasNext()) {
        message = scan.next();
        if (!message.isTablesComplete() || !message.isBitCountOk()) continue;
        byte[] mbytes = scan.getMessageBytesFromLast(message);
        try {
          out = new FileOutputStream("C:/data/formats/bufr/uniqueE/" + count + ".xml");
          NetcdfFile ncfile = NetcdfFile.openInMemory("test", mbytes, "ucar.nc2.iosp.bufr.BufrIosp");
          NetcdfDataset ncd = new NetcdfDataset(ncfile);
          new Bufr2Xml(message, ncd, out, true);
          out.close();
          count++;
          size += message.getMessageSize();
        } catch (Throwable e) {
          // e.printStackTrace();
        }
      }

    } finally {
      if (raf != null) raf.close();
      if (out != null) out.close();
    }

    System.out.printf("total size= %f Kb %n", .001 * size);
  }
}


