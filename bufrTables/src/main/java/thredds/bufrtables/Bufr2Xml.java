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
package thredds.bufrtables;

import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.iosp.bufr.BufrIosp;
import ucar.nc2.*;
import ucar.nc2.util.Indent;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.SequenceDS;
import ucar.nc2.dataset.StructureDS;
import ucar.ma2.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Formatter;

/**
 * @author caron
 * @since Aug 9, 2008
 */
public class Bufr2Xml {

  void writeMessage(Message message, NetcdfDataset ncfile) {
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

      SequenceDS obs = (SequenceDS) ncfile.findVariable(BufrIosp.obsRecord);
      StructureDataIterator sdataIter = obs.getStructureIterator(-1);
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
            writeStructureArray((StructureDS) v, (ArrayStructure) mdata, indent);

          } else if (m.getDataType() == DataType.SEQUENCE) {
            writeSequence((SequenceDS) v, (ArraySequence) mdata, indent);
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

  void writeStructureArray(StructureDS s, ArrayStructure data, Indent indent) throws IOException, XMLStreamException {
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
  }

  void writeSequence(SequenceDS s, ArraySequence data, Indent indent) throws IOException, XMLStreamException {
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

    Attribute att = v.findAttribute("BUFR:TableB_descriptor");
    String desc = (att == null) ? "N/A" : att.getStringValue();
    staxWriter.writeAttribute("bufr", desc); // */

    // write data value
    if (v.getDataType().isNumeric()) {
      mdata.resetLocalIterator();
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

  Formatter out = new Formatter(System.out);
  XMLStreamWriter staxWriter;

  Bufr2Xml(Message message, NetcdfDataset ncfile, OutputStream os) throws IOException {
    try {
      XMLOutputFactory fac = XMLOutputFactory.newInstance();
      staxWriter = fac.createXMLStreamWriter(os, "UTF-8");

      staxWriter.writeStartDocument("UTF-8", "1.0");
      staxWriter.writeCharacters("\n");
      staxWriter.writeStartElement("bufrMessages");

      writeMessage(message, ncfile);

      staxWriter.writeCharacters("\n");
      staxWriter.writeEndDocument();
      staxWriter.flush();
      os.close();

      out.flush();
    } catch (XMLStreamException e) {
      throw new IOException(e.getMessage());
    }
  }

}


