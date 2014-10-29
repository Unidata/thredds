/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp.bufr;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.bufr.tables.TableB;
import ucar.nc2.iosp.bufr.tables.TableD;
import ucar.nc2.iosp.bufr.tables.WmoXmlReader;
import ucar.nc2.wmo.Util;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * BUFR allows you to encode a BUFR table in BUFR.
 * if table is embedded, all entries must be from it
 * LOOK: may be NCEP specific ?
 *
 * @author John
 * @since 8/11/11
 */
public class EmbeddedTable {
  private static final boolean showB = false;
  private static final boolean showD = false;

  private final RandomAccessFile raf;
  private final BufrIdentificationSection ids;

  private List<Message> messages = new ArrayList<>();
  private boolean tableRead = false;
  private TableB b;
  private TableD d;
  private Structure seq2, seq3, seq4;
  private TableLookup tlookup;

  EmbeddedTable(Message m, RandomAccessFile raf) {
    this.raf = raf;
    this.ids = m.ids;
    b = new TableB("embed", raf.getLocation());
    d = new TableD("embed", raf.getLocation());
  }

  public void addTable(Message m) {
    messages.add(m);
  }

  private void read2() throws IOException {
    Message proto = messages.get(0);
    BufrConfig config = BufrConfig.openFromMessage(raf, proto, null);
    Construct2 construct = new Construct2(proto, config, new NetcdfFileSubclass());

    Sequence obs = construct.getObsStructure();
    seq2 = (Structure) obs.findVariable("seq2");
    seq3 = (Structure) obs.findVariable("seq3");
    seq4 = (Structure) seq3.findVariable("seq4");

    // read all the messages
    ArrayStructure data;
    for (Message m : messages) {
      if (!m.dds.isCompressed()) {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        data = reader.readEntireMessage(obs, proto, m, raf, null);
      } else {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        data = reader.readEntireMessage(obs, proto, m, raf, null);
      }
      while ( data.hasNext()) {
        StructureData sdata = (StructureData) data.next();
        add(sdata);
      }
    }
  }

  private void add(StructureData data) throws IOException {
    for (StructureMembers.Member m : data.getMembers()) {
      if (showB) System.out.printf("%s%n", m);
      if (m.getDataType() == DataType.SEQUENCE) {
        if (m.getName().equals("seq2")) {
          ArraySequence seq = data.getArraySequence(m);
          StructureDataIterator iter = seq.getStructureDataIterator();
          while (iter.hasNext())
            addTableEntryB(iter.next());
        } else if (m.getName().equals("seq3")) {
          ArraySequence seq = data.getArraySequence(m);
          StructureDataIterator iter = seq.getStructureDataIterator();
          while (iter.hasNext())
            addTableEntryD(iter.next());
        }
      }
    }
  }

  private void addTableEntryB(StructureData sdata) throws IOException {
    String name = "", units = "", signScale = null, signRef = null;
    int scale = 0, refVal = 0, width = 0;
    short x1 = 0, y1 = 0;
    List<StructureMembers.Member> members = sdata.getMembers();
    List<Variable> vars = seq2.getVariables();
    for (int i=0; i<vars.size(); i++) {
      Variable v = vars.get(i);
      StructureMembers.Member m = members.get(i);
      String data = sdata.getScalarString(m);
      if (showB) System.out.printf("%s == %s%n" ,v, data);

      Attribute att = v.findAttribute(BufrIosp2.fxyAttName);
      if (att.getStringValue().equals("0-0-10")) {
        sdata.getScalarString(m);
      } else if (att.getStringValue().equals("0-0-11")) {
        String x = sdata.getScalarString(m);
        x1 = Short.parseShort(x.trim());
      } else if (att.getStringValue().equals("0-0-12")) {
        String y = sdata.getScalarString(m);
        y1 = Short.parseShort(y.trim());
      } else if (att.getStringValue().equals("0-0-13")) {
        name = sdata.getScalarString(m);
      } else if (att.getStringValue().equals("0-0-14")) {
        name += sdata.getScalarString(m);  // append both lines
      } else if (att.getStringValue().equals("0-0-15")) {
        units = sdata.getScalarString(m);
        units = WmoXmlReader.cleanUnit(units.trim());
      } else if (att.getStringValue().equals("0-0-16")) {
        signScale = sdata.getScalarString(m).trim();
      } else if (att.getStringValue().equals("0-0-17")) {
        String scaleS = sdata.getScalarString(m);
        scale = Integer.parseInt(scaleS.trim());
      } else if (att.getStringValue().equals("0-0-18")) {
        signRef = sdata.getScalarString(m).trim();
      } else if (att.getStringValue().equals("0-0-19")) {
        String refS = sdata.getScalarString(m);
        refVal = Integer.parseInt(refS.trim());
      } else if (att.getStringValue().equals("0-0-20")) {
        String widthS = sdata.getScalarString(m);
        width = Integer.parseInt(widthS.trim());
      }
    }
    if (showB) System.out.printf("%n");

    // split name and description from appended line 1 and 2
    String desc = null;
    name = name.trim();
    int pos = name.indexOf(' ');
    if (pos > 0) {
      desc = Util.cleanName(name.substring(pos + 1));
      name = name.substring(0, pos);
      name = Util.cleanName(name);
    }

    if ("-".equals(signScale)) scale = -1 * scale;
    if ("-".equals(signRef)) refVal = -1 * refVal;

    b.addDescriptor(x1, y1, scale, refVal, width, name, units, desc);
  }

  private void addTableEntryD(StructureData sdata) throws IOException {
    String name = null;
    short x1 = 0, y1 = 0;
    List<Short> dds = null;

    List<StructureMembers.Member> members = sdata.getMembers();
    List<Variable> vars = seq3.getVariables();
    for (int i=0; i<vars.size(); i++) {
      Variable v = vars.get(i);
      StructureMembers.Member m = members.get(i);
      if (m.getName().equals("seq4")) {
        dds = getDescriptors(sdata.getArraySequence(m));
        continue;
      }

      Attribute att = v.findAttribute(BufrIosp2.fxyAttName);
      if (att != null) {
        if (showD) System.out.printf("%s == %s%n" ,v, sdata.getScalarString(m));
        if (att.getStringValue().equals("0-0-10")) {
          sdata.getScalarString(m);
        } else if (att.getStringValue().equals("0-0-11")) {
          String x = sdata.getScalarString(m);
          x1 = Short.parseShort(x.trim());
        } else if (att.getStringValue().equals("0-0-12")) {
          String y = sdata.getScalarString(m);
          y1 = Short.parseShort(y.trim());
        } else if (att.getStringValue().equals("2-5-64")) {
          name = sdata.getScalarString(m);
        }
      }
    }
    if (showD) System.out.printf("%n");

    name = Util.cleanName(name);

    d.addDescriptor(x1, y1, name, dds);
  }

  private List<Short> getDescriptors(ArraySequence seqdata) throws IOException {
    List<Short> list = new ArrayList<>();
    String fxyS = null;
    List<Variable> vars = seq4.getVariables();

    StructureDataIterator iter = seqdata.getStructureDataIterator();
    while (iter.hasNext()) {
      StructureData sdata = iter.next();

      List<StructureMembers.Member> members = sdata.getMembers();
      for (int i=0; i<vars.size(); i++) {
        Variable v = vars.get(i);
        StructureMembers.Member m = members.get(i);
        String data = sdata.getScalarString(m);
        if (showD) System.out.printf("%s == %s%n" ,v, data);

        Attribute att = v.findAttribute(BufrIosp2.fxyAttName);
        if (att != null && att.getStringValue().equals("0-0-30"))
          fxyS = sdata.getScalarString(m);
      }
      if (showD) System.out.printf("%n");

      if (fxyS != null) {
          short id = Descriptor.getFxy2(fxyS);
          list.add(id);
      }
    }
    return list;
  }

  TableLookup getTableLookup() throws IOException {
    if (!tableRead) {
      read2();
      tableRead = true;
      tlookup = new TableLookup(ids, b, d);
    }
    return tlookup;
  }

}
