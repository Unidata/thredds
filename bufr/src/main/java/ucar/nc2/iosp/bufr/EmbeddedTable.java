/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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
 * LOOK: may be NCEP specific ? if table is embedded, all entries must be from it
 *
 * @author John
 * @since 8/11/11
 */
public class EmbeddedTable {
  private static final boolean showB = false;
  private static final boolean showD = false;

  private final RandomAccessFile raf;
  private final BufrIdentificationSection ids;

  private List<Message> messages = new ArrayList<Message>();
  private boolean tableRead = false;
  private TableB b;
  private TableD d;
  private Structure seq2, seq3, seq4;
  private TableLookup lookup;

  EmbeddedTable(BufrIdentificationSection ids, RandomAccessFile raf) {
    this.raf = raf;
    this.ids = ids;
    b = new TableB("embed", raf.getLocation());
    d = new TableD("embed", raf.getLocation());
  }

  public void addTable(Message m) {
    messages.add(m);
  }

  private void read2() throws IOException {
    Message proto = null;
    //System.out.printf("Reading from %s%n", raf.getLocation());

    int countObs = 0;
    for (Message m : messages) {
      if (proto == null) proto = m;
      int n = m.getNumberDatasets();
      countObs += n;
      //System.out.printf("  num datasets = %d%n", n);
    }

    // this fills the netcdf object
    ConstructNC construct = new ConstructNC(proto, countObs, new FakeNetcdfFile());

    seq2 = (Structure) construct.recordStructure.findVariable("seq2");
    seq3 = (Structure) construct.recordStructure.findVariable("seq3");
    if (seq3 == null)
      System.out.println("HEY");
    seq4 = (Structure) seq3.findVariable("seq4");

    // read all the messages
    ArrayStructure data;
    for (Message m : messages) {
      if (!m.dds.isCompressed()) {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        data = reader.readEntireMessage(construct.recordStructure, proto, m, raf, null);
      } else {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        data = reader.readEntireMessage(construct.recordStructure, proto, m, raf, null);
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
    String name = "", units = null, f = null, x = null, y = null, signScale = null, scaleS = null, signRef = null, refS = null, widthS = null;
    List<StructureMembers.Member> members = sdata.getMembers();
    List<Variable> vars = seq2.getVariables();
    for (int i=0; i<vars.size(); i++) {
      Variable v = vars.get(i);
      StructureMembers.Member m = members.get(i);
      String data = sdata.getScalarString(m);
      if (showB) System.out.printf("%s == %s%n" ,v, data);

      Attribute att = v.findAttribute("BUFR:TableB_descriptor");
      if (att.getStringValue().equals("0-0-10"))
        f = sdata.getScalarString(m);
      else if (att.getStringValue().equals("0-0-11"))
        x = sdata.getScalarString(m);
      else if (att.getStringValue().equals("0-0-12"))
        y = sdata.getScalarString(m);
      else if (att.getStringValue().equals("0-0-13"))
        name = sdata.getScalarString(m);
      else if (att.getStringValue().equals("0-0-14"))
        name += sdata.getScalarString(m);  // append both lines
      else if (att.getStringValue().equals("0-0-15"))
        units = sdata.getScalarString(m);
      else if (att.getStringValue().equals("0-0-16"))
        signScale = sdata.getScalarString(m).trim();
      else if (att.getStringValue().equals("0-0-17"))
        scaleS = sdata.getScalarString(m);
      else if (att.getStringValue().equals("0-0-18"))
        signRef = sdata.getScalarString(m).trim();
      else if (att.getStringValue().equals("0-0-19"))
        refS = sdata.getScalarString(m);
      else if (att.getStringValue().equals("0-0-20"))
        widthS = sdata.getScalarString(m);
    }
    if (showB) System.out.printf("%n");

    // split name and description from appendended line 1 and 2
    String desc = null;
    name = name.trim();
    int pos = name.indexOf(' ');
    if (pos > 0) {
      desc = Util.cleanName(name.substring(pos + 1));
      name = name.substring(0,pos);
      name= Util.cleanName(name);
    }

    units = WmoXmlReader.cleanUnit(units.trim());
    int scale = Integer.parseInt(scaleS.trim());
    int refVal = Integer.parseInt(refS.trim());
    int width = Integer.parseInt(widthS.trim());
    short x1 = Short.parseShort(x.trim());
    short y1 = Short.parseShort(y.trim());

    if ("-".equals(signScale)) scale = -1 * scale;
    if ("-".equals(signRef)) refVal = -1 * refVal;



    b.addDescriptor(x1, y1, scale, refVal, width, name, units, desc);
  }

  private void addTableEntryD(StructureData sdata) throws IOException {
    String f = null, x = null, y = null, name = null;
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

      Attribute att = v.findAttribute("BUFR:TableB_descriptor");
      if (att != null) {
        if (showD) System.out.printf("%s == %s%n" ,v, sdata.getScalarString(m));
        if (att.getStringValue().equals("0-0-10"))
          f = sdata.getScalarString(m);
        else if (att.getStringValue().equals("0-0-11"))
          x = sdata.getScalarString(m);
        else if (att.getStringValue().equals("0-0-12"))
          y = sdata.getScalarString(m);
        else if (att.getStringValue().equals("2-5-64"))
          name = sdata.getScalarString(m);
      }
    }
    if (showD) System.out.printf("%n");

    short f1 = Short.parseShort(f.trim());
    short x1 = Short.parseShort(x.trim());
    short y1 = Short.parseShort(y.trim());
    name = Util.cleanName(name);

    d.addDescriptor(x1, y1, name, dds);
  }

  private List<Short> getDescriptors(ArraySequence seqdata) throws IOException {
    List<Short> list = new ArrayList<Short>();
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

        Attribute att = v.findAttribute("BUFR:TableB_descriptor");
        if (att != null && att.getStringValue().equals("0-0-30"))
          fxyS = sdata.getScalarString(m);
      }
      if (showD) System.out.printf("%n");

      short id = Descriptor.getFxy2(fxyS);
      list.add(id);
    }
    return list;
  }

  TableLookup getTableLookup() throws IOException {
    if (!tableRead) {
      read2();
      tableRead = true;
      lookup = new TableLookup(ids, b, d);
    }
    return lookup;
  }

  static private class FakeNetcdfFile extends NetcdfFile {
  }
}
