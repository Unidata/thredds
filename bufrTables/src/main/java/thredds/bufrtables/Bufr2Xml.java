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
package thredds.bufrtables;

import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.VariableDS;
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

        } else if (m.getDataType() == DataType.STRUCTURE) {
          writeStructureArray((Structure) v, (ArrayStructure) sdata.getArray(m), indent);

        } else if (m.getDataType() == DataType.SEQUENCE) {
          writeSequence((Structure) v, (ArraySequence) sdata.getArray(m), indent);
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

        } else if (m.getDataType() == DataType.STRUCTURE) {
          writeStructureArray((Structure) v, (ArrayStructure) sdata.getArray(m), indent);

        } else if (m.getDataType() == DataType.SEQUENCE) {
          writeSequence((Structure) v, (ArraySequence) sdata.getArray(m), indent);
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

  Bufr2Xml(Message message, NetcdfFile ncfile, OutputStream os) throws IOException {
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


