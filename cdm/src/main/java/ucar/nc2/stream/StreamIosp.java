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
package ucar.nc2.stream;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.iosp.AbstractIOServiceProvider;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.*;
import java.nio.ByteBuffer;

/**
 * @author caron
 * @since Jul 18, 2007
 */
public class StreamIosp extends AbstractIOServiceProvider {

  private ucar.unidata.io.RandomAccessFile raf;
  private NetcdfFile ncfile;

  private Map<String,Vinfo> varMap = new HashMap<String,Vinfo>();
  private boolean debug = true;

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    // this is the first time we try to read the file - if theres a problem we get a IOException
    raf.seek(0);
    byte[] magicb = new byte[8];
    readBytes(magicb);
    String magicS = new String(magicb);
    return magicS.equals(StreamWriter.MAGIC_HEADER);
  }

  public void open(ucar.unidata.io.RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;

    if (!isValidFile(raf)) throw new IllegalArgumentException("Not a NetCDF Stream file");

    while (true) {
      String magic = readMagic();

      if (magic.equals(StreamWriter.MAGIC_HEADER)) {
        magic = readHeader(); // typically will return MAGIC_DATA

      } else if (magic.equals(StreamWriter.MAGIC_DATA)) {
        readDataHeader();

      /* } else if (magic.equals(StreamWriter.MAGIC_EOF)) {
          break; */

      } else {
        throw new IllegalStateException("BAD MAGIC " + magic);
      }
    }

  }

  public void close() throws IOException {
    raf.close();
  }

  private String readMagic() throws IOException {
    byte[] magic = new byte[4];
    readBytes(magic);
    String magicS = new String(magic);
    if (debug) System.out.println("Got magic= " + magicS);
    return magicS;
  }

  private String readHeader() throws IOException {
    Group root = ncfile.getRootGroup();

    // keep reading sections till not a header section.
    while (true) {
      String magic = readMagic();

      if (magic.equals(StreamWriter.MAGIC_HEADER))
        magic = readMagic();

      if (magic.equals(StreamWriter.MAGIC_ATTS))
        readAtts(root.getAttributes());

      else if (magic.equals(StreamWriter.MAGIC_DIMS))
        readDims(root.getDimensions());

      else if (magic.equals(StreamWriter.MAGIC_VARS))
        readVars(root.getVariables());

      else return magic;

    }

  }


  private void readAtts(List<Attribute> atts) throws IOException {
    int natts = readVInt();
    if (natts == 0) return;

    for (int i = 0; i < natts; i++) {
      String name = readString();
      int type = readVInt();
      int nvals = readVInt();
      DataType dt = StreamWriter.getDataType(type);
      Array data = Array.factory(dt, new int[]{nvals});
      readValues(data.getIndexIterator(), dt);
      atts.add(new Attribute(name, data));
    }
  }


  private void readValues(IndexIterator ii, DataType dt) throws IOException {
    if (dt == DataType.BYTE) {
      while (ii.hasNext())
        ii.setByteNext(raf.readByte());

    } else if (dt == DataType.CHAR) {
      while (ii.hasNext())
        ii.setCharNext((char) raf.readByte());

    } else if (dt == DataType.SHORT) {
      while (ii.hasNext())
        ii.setShortNext(raf.readShort());

    } else if (dt == DataType.INT) {
      while (ii.hasNext())
        ii.setIntNext(raf.readInt());

    } else if (dt == DataType.FLOAT) {
      while (ii.hasNext())
        ii.setFloatNext(raf.readFloat());

    } else if (dt == DataType.DOUBLE) {
      while (ii.hasNext())
        ii.setDoubleNext(raf.readDouble());

    } else if (dt == DataType.STRING) {
      while (ii.hasNext())
        ii.setObjectNext(readString());

    } else {
      throw new IllegalStateException("unknown data type == " + dt);
    }
  }

  private void readDims(List<Dimension> dims) throws IOException {
    int ndims = readVInt();
    for (int i = 0; i < ndims; i++) {
      String name = readString();
      int length = readVInt();

      int flags = (int) readByte();
      boolean isShared = (flags & 1) != 0;
      boolean isUnlimited = (flags & 2) != 0;
      boolean isVariableLength = (flags & 4) != 0;

      dims.add(new Dimension(name, length, isShared, isUnlimited, isVariableLength));
    }
  }

  private void readVars(List<Variable> vars) throws IOException {
    int nvars = readVInt();
    for (int i = 0; i < nvars; i++) {
      String varname = readString();
      int type = readVInt();
      DataType dt = StreamWriter.getDataType(type);
      if (debug) System.out.println("  var= "+varname+" type = "+type+" dataType = "+dt);

      List<Dimension> dims = new ArrayList<Dimension>();
      readDims(dims);

      Variable v = new Variable(ncfile, null, null, varname);
      v.setDataType(dt);
      v.setDimensions(dims);
      readAtts(v.getAttributes());
      vars.add(v);

      Vinfo vinfo = varMap.get(varname);
      if (vinfo == null) {
        vinfo = new Vinfo(varname, type);
        varMap.put(varname, vinfo);
      }
      vinfo.v = v;
      v.setSPobject(vinfo);
    }
  }

  private void readDataHeader() throws IOException {
    try {
      String varname = readString();
      int type = readVInt();
      Section s = readSection();
      long filePos = raf.getFilePointer();
      Vinfo vinfo = varMap.get(varname);
      if (vinfo == null) {
        vinfo = new Vinfo(varname, type);
        varMap.put(varname, vinfo);
      }
      DataType dtype = StreamWriter.getDataType( type);
      long size = s.computeSize() * dtype.getSize();
      vinfo.data.add( new DataSection(s, filePos, size));
      raf.seek(filePos+size);
    } catch (InvalidRangeException e) {
      throw new RuntimeException("Illegal section");
    }
  }

  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  private void readData() throws IOException, InvalidRangeException {
    String name = readString();
    Section s = readSection();

    Variable v = ncfile.findVariable(name);
    assert v != null;
    DataType dt = v.getDataType();

    if (debug) System.out.println("  var= "+name+" datatype = "+dt+" section = "+s);

    Array data;
    if (dt == DataType.STRUCTURE) {
      data = readStructureData((Structure) v, s);
    } else {
      data = Array.factory(dt, s.getShape());
      readValues(data.getIndexIterator(), v.getDataType());
    }
  }

  private ArrayStructure readStructureData(Structure v, Section s) throws IOException {
    StructureMembers sm = v.makeStructureMembers();
    int offset = 0;
    for (StructureMembers.Member m : sm.getMembers()) {
      m.setDataParam(offset);
      offset += m.getSizeBytes();
    }
    sm.setStructureSize(offset);

    int size = (int) (sm.getStructureSize() * s.computeSize());
    byte[] ba = new byte[size];
    readBytes(ba);
    ByteBuffer bb = ByteBuffer.wrap(ba);
    return new ArrayStructureBB(sm, s.getShape(), bb, 0);
  }

  private Section readSection() throws IOException, InvalidRangeException {
    int rank = readVInt();
    Section s = new Section();
    for (int i = 0; i < rank; i++) {
      int first = readVInt();
      int length = readVInt();
      s.appendRange(new Range(first, first + length - 1));
    }
    return s;
  }

  ////////////////////////////////////////
  // from org.apache.lucene.store.IndexOutput

  private byte readByte() throws IOException {
    return raf.readByte();
  }

  private void readBytes(byte[] b) throws IOException {
    raf.readFully(b, 0, b.length);
  }

  private int readVInt() throws IOException {
    byte b = readByte();
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = readByte();
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  //private char[] chars;
  private String readString() throws IOException {
    int length = readVInt();
    // if (chars == null || length > chars.length)
    char[] chars = new char[length];
    readChars(chars, 0, length);
    return new String(chars, 0, length);
  }

  /**
   * Reads UTF-8 encoded characters into an array.
   *
   * @param buffer the array to read characters into
   * @param start  the offset in the array to start storing characters
   * @param length the number of characters to read
   */
  private void readChars(char[] buffer, int start, int length) throws IOException {
    final int end = start + length;
    for (int i = start; i < end; i++) {
      byte b = readByte();
      if ((b & 0x80) == 0)
        buffer[i] = (char) (b & 0x7F);
      else if ((b & 0xE0) != 0xE0) {
        buffer[i] = (char) (((b & 0x1F) << 6)
            | (readByte() & 0x3F));
      } else
        buffer[i] = (char) (((b & 0x0F) << 12)
            | ((readByte() & 0x3F) << 6)
            | (readByte() & 0x3F));
    }
  }

  private class Vinfo {
    String name;
    Variable v;
    int type;
    List<DataSection> data = new ArrayList<DataSection>();
    Vinfo(String name, int type) {
      this.name = name;
      this.type = type;
    }
  }

  private class DataSection {
    Section s;
    long filePos; // where the data starts
    long sizeBytes; // length of data in bytes
    DataSection(Section s,  long filePos, long sizeBytes) {
      this.s = s;
      this.filePos = filePos;
      this.sizeBytes = sizeBytes;
    }
  }


}
