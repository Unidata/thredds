/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.netcdf3;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.io.RandomAccessFile;

import java.util.*;
import java.io.IOException;
import java.io.PrintStream;


/**
 * Netcdf header reading and writing for version 3 file format.
 * This is used by N3iosp.
 *
 * @author caron
 */

public class N3header {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(N3header.class);

  static final byte[] MAGIC = new byte[]{0x43, 0x44, 0x46, 0x01};
  static final int MAGIC_DIM = 10;
  static final int MAGIC_VAR = 11;
  static final int MAGIC_ATT = 12;

  static public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    // this is the first time we try to read the file - if theres a problem we get a IOException
    raf.seek(0);
    byte[] b = new byte[4];
    raf.read(b);
    for (int i = 0; i < 3; i++)
      if (b[i] != MAGIC[i])
        return false;
    if ((b[3] != 1) && (b[3] != 2)) return false;
    return true;
  }

  static public boolean disallowFileTruncation = false;  // see NetcdfFile.setDebugFlags
  static public boolean debugHeaderSize = false;  // see NetcdfFile.setDebugFlags

  private boolean debug = false, debugPos = false, debugString = false, debugVariablePos = false;

  private ucar.unidata.io.RandomAccessFile raf;
  private ucar.nc2.NetcdfFile ncfile;
  private PrintStream out = System.out;
  private List<Variable> uvars = new ArrayList<Variable>(); // vars that have the unlimited dimension
  private Dimension udim; // the unlimited dimension

  boolean isStreaming = false;
  int numrecs = 0; // number of records written
  int recsize = 0; // size of each record (padded)
  long dataStart = Long.MAX_VALUE; // where the data starts
  long recStart = Integer.MAX_VALUE; // where the record data starts
  private long nonRecordData = 0; // length of non-record data

  private long globalAttsPos = 0; // global attributes start here - used for update


  /**
   * Read the header and populate the ncfile
   *
   * @param raf    read from this file
   * @param ncfile fill this NetcdfFile object (originally empty)
   * @param out    optional for debug message, may be null
   * @throws IOException on read error
   */
  void read(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile, PrintStream out) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;
    if (out == null)
      out = System.out;
    this.out = out;

    long actualSize = raf.length();

    // netcdf magic number
    long pos = 0;
    raf.order(RandomAccessFile.BIG_ENDIAN);
    raf.seek(pos);

    byte[] b = new byte[4];
    raf.read(b);
    for (int i = 0; i < 3; i++)
      if (b[i] != MAGIC[i])
        throw new IOException("Not a netCDF file");
    if ((b[3] != 1) && (b[3] != 2))
      throw new IOException("Not a netCDF file");
    boolean useLongOffset = (b[3] == 2);

    // number of records
    numrecs = raf.readInt();
    if (debug) out.println("numrecs= " + numrecs);
    if (numrecs == -1) {
      isStreaming = true;
      numrecs = 0;
    }

    // dimensions
    int numdims = 0;
    int magic = raf.readInt();
    if (magic == 0) {
      raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_DIM)
        throw new IOException("Misformed netCDF file - dim magic number wrong");
      numdims = raf.readInt();
      if (debug) out.println("numdims= " + numdims);
    }

    for (int i = 0; i < numdims; i++) {
      if (debugPos) out.println("  dim " + i + " pos= " + raf.getFilePointer());
      String name = readString();
      int len = raf.readInt();
      Dimension dim;
      if (len == 0) {
        dim = new Dimension(name, numrecs, true, true, false);
        udim = dim;
      } else {
        dim = new Dimension(name, len, true, false, false);
      }

      ncfile.addDimension(null, dim);
      if (debug) out.println(" added dimension " + dim);
    }

    // global attributes
    globalAttsPos = raf.getFilePointer();
    readAtts(ncfile.getRootGroup().getAttributes());

    // variables
    int nvars = 0;
    magic = raf.readInt();
    if (magic == 0) {
      raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_VAR)
        throw new IOException("Misformed netCDF file  - var magic number wrong");
      nvars = raf.readInt();
      if (debug) out.println("numdims= " + numdims);
    }
    if (debug) out.println("num variables= " + nvars);

    // loop over variables
    for (int i = 0; i < nvars; i++) {
      long startPos = raf.getFilePointer();
      String name = readString();
      Variable var = new Variable(ncfile, ncfile.getRootGroup(), null, name);

      // get dimensions
      int velems = 1;
      boolean isRecord = false;
      int rank = raf.readInt();
      List<Dimension> dims = new ArrayList<Dimension>();
      for (int j = 0; j < rank; j++) {
        int dimIndex = raf.readInt();
        Dimension dim = ncfile.getRootGroup().getDimensions().get(dimIndex); // note relies on ordering
        if (dim.isUnlimited()) {
          isRecord = true;
          uvars.add(var); // track record variables
        } else
          velems *= dim.getLength();

        dims.add(dim);
      }
      var.setDimensions(dims);

      if (debug) {
        out.print("---name=<" + name + "> dims = [");
        for (int j = 0; j < rank; j++) {
          Dimension dim = dims.get(j);
          out.print(dim.getName() + " ");
        }
        out.println("]");
      }

      // variable attributes
      long varAttsPos = raf.getFilePointer();
      readAtts(var.getAttributes());

      // data type
      int type = raf.readInt();
      var.setDataType(getDataType(type));

      // size and beginning data position in file
      int vsize = raf.readInt();
      long begin = useLongOffset ? raf.readLong() : (long) raf.readInt();
      if (debug)
        out.println(" name= " + name + " type=" + type + " vsize=" + vsize + " velems=" + velems + " begin= " + begin +
            " isRecord=" + isRecord + " attsPos= " + varAttsPos + "\n");
      var.setSPobject(new Vinfo(vsize, begin, isRecord, varAttsPos));

      // track how big each record is
      if (isRecord) {
        recsize += vsize;
        recStart = Math.min(recStart, (int) begin);
      } else {
        nonRecordData = Math.max(nonRecordData, begin + vsize);
      }

      dataStart = Math.min(dataStart, (int) begin);

      if (debugVariablePos)
        System.out.println(var.getName() + " begin at = " + begin + " end=" + (begin + vsize) + " isRecord=" + isRecord + " nonRecordData=" + nonRecordData);
      if (debugHeaderSize)
        System.out.println(var.getName() + " header size= "+ (raf.getFilePointer() - startPos) + " data size= "+ vsize);

      ncfile.addVariable(null, var);
    }

    pos = (int) raf.getFilePointer();

    if (nonRecordData > 0) // if there are non-record variables
      nonRecordData -= dataStart;
    if (uvars.size() == 0) // if there are no record variables
      recStart = 0;

    if (debugHeaderSize) {
      System.out.println("  filePointer = " + pos + " dataStart=" + dataStart);
      System.out.println("  recStart = " + recStart + " dataStart+nonRecordData =" + (dataStart + nonRecordData));
      System.out.println("  nonRecordData size= " + nonRecordData);
      System.out.println("  recsize= " + recsize);
      System.out.println("  numrecs= " + numrecs);
      System.out.println("  actualSize= " + actualSize);
    }

    // check for streaming file - numrecs must be caclulated
    if (isStreaming) {
      long recordSpace = actualSize - recStart;
      numrecs = (int) (recordSpace / recsize);
      //if (debug)
        System.out.println(" isStreaming recordSpace=" + recordSpace + " numrecs="+numrecs+
            " has extra bytes = " + (recordSpace % recsize));

      // set it in the unlimited dimension, all of the record variables
      if (udim != null) {
        udim.setLength(this.numrecs);
        for (Variable uvar : uvars) {
          uvar.resetShape();
          uvar.invalidateCache();
        }
      }
    }

    // check for truncated files
    // theres a "wart" that allows a file to be up to 3 bytes smaller than you expect.
    long calcSize = dataStart + nonRecordData + recsize * numrecs;
    if (calcSize > actualSize + 3) {
      if (disallowFileTruncation)
        throw new IOException("File is truncated calculated size= " + calcSize + " actual = " + actualSize);
      else {
        //System.out.println("File is truncated calculated size= "+calcSize+" actual = "+actualSize);
        raf.setExtendMode();
      }
    }

    // finish
    // ncfile.finish();
  }

  synchronized boolean removeRecordStructure() {
    boolean found = false;
    for (Variable v : uvars) {
      if (v.getName().equals("record")) {
        uvars.remove(v);
        ncfile.getRootGroup().getVariables().remove(v);
        found = true;
        break;
      }
    }

    ncfile.finish();
    return found;
  }

  synchronized boolean makeRecordStructure() {
    // create record structure
    if (uvars.size() > 0) {
      Structure recordStructure = new Structure(ncfile, ncfile.getRootGroup(), null, "record");
      recordStructure.setDimensions(udim.getName());
      for (Variable v : uvars) {
        Variable memberV = null;
        try {
          memberV = v.slice(0,0); // set unlimited dimension to 0
        } catch (InvalidRangeException e) {
          log.error("Cant slice variable "+v);
          return false;
        }
        memberV.setParentStructure(recordStructure);
        //memberV.createNewCache(); // decouple caching - LOOK could use this ??

        //remove record dimension
        //List<Dimension> dims = new ArrayList<Dimension>(v.getDimensions());
        //dims.remove(0);
        //memberV.setDimensions(dims);

        recordStructure.addMemberVariable(memberV);
      }

      uvars.add(recordStructure);
      ncfile.getRootGroup().addVariable(recordStructure);
      ncfile.finish();
      return true;
    }

    return false;
  }


  private int readAtts(List<Attribute> atts) throws IOException {
    int natts = 0;
    int magic = raf.readInt();
    if (magic == 0) {
      raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_ATT)
        throw new IOException("Misformed netCDF file  - att magic number wrong");
      natts = raf.readInt();
    }
    if (debug) out.println(" num atts= " + natts);

    for (int i = 0; i < natts; i++) {
      if (debugPos) out.println("***att " + i + " pos= " + raf.getFilePointer());
      String name = readString();
      int type = raf.readInt();
      Attribute att;

      if (type == 2) {
        if (debugPos) out.println(" begin read String val pos= " + raf.getFilePointer());
        String val = readString();
        if (debugPos) out.println(" end read String val pos= " + raf.getFilePointer());
        att = new Attribute(name, val); // no validation !!

      } else {
        if (debugPos) out.println(" begin read val pos= " + raf.getFilePointer());
        int nelems = raf.readInt();

        DataType dtype = getDataType(type);
        int[] shape = {nelems};
        Array arr = Array.factory(dtype.getPrimitiveClassType(), shape);
        IndexIterator ii = arr.getIndexIterator();
        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += readAttributeValue(dtype, ii);

        att = new Attribute(name, arr); // no validation !!

        skip(nbytes);
        if (debugPos) out.println(" end read val pos= " + raf.getFilePointer());
      }

      atts.add(att);
      if (debug) out.println("  " + att.toString() + "\n");
    }

    return natts;
  }

  private int readAttributeValue(DataType type, IndexIterator ii) throws IOException {
    if (type == DataType.BYTE) {
      byte b = (byte) raf.read();
      //if (debug) out.println("   byte val = "+b);
      ii.setByteNext(b);
      return 1;

    } else if (type == DataType.CHAR) {
      char c = (char) raf.read();
      //if (debug) out.println("   char val = "+c);
      ii.setCharNext(c);
      return 1;

    } else if (type == DataType.SHORT) {
      short s = raf.readShort();
      //if (debug) out.println("   short val = "+s);
      ii.setShortNext(s);
      return 2;

    } else if (type == DataType.INT) {
      int i = raf.readInt();
      //if (debug) out.println("   int val = "+i);
      ii.setIntNext(i);
      return 4;

    } else if (type == DataType.FLOAT) {
      float f = raf.readFloat();
      //if (debug) out.println("   float val = "+f);
      ii.setFloatNext(f);
      return 4;

    } else if (type == DataType.DOUBLE) {
      double d = raf.readDouble();
      //if (debug) out.println("   double val = "+d);
      ii.setDoubleNext(d);
      return 8;
    }
    return 0;
  }

  // read a string = (nelems, byte array), then skip to 4 byte boundary
  private String readString() throws IOException {
    int nelems = raf.readInt();
    if (debugString) printBytes(nelems);
    byte[] b = new byte[nelems];
    raf.read(b);
    skip(nelems); // pad to 4 byte boundary

    // null terminates
    int count = 0;
    while (count < nelems)
      if (b[count++] == 0) break;

    return new String(b, 0, count, "UTF-8"); // all strings are considered to be UTF-8 unicode.
  }

  // skip to a 4 byte boundary in the file
  private void skip(int nbytes) throws IOException {
    int pad = padding(nbytes);
    if (pad > 0)
      raf.seek(raf.getFilePointer() + pad);
  }

  // find number of bytes needed to pad to a 4 byte boundary
  static int padding(int nbytes) {
    int pad = nbytes % 4;
    if (pad != 0) pad = 4 - pad;
    return pad;
  }

  private void printBytes(int n) throws IOException {
    long savePos = raf.getFilePointer();
    long pos;
    for (pos = savePos; pos < savePos + n - 9; pos += 10) {
      out.print(pos + ": ");
      _printBytes(10);
    }
    if (pos < savePos + n) {
      out.print(pos + ": ");
      _printBytes((int) (savePos + n - pos));
    }
    raf.seek(savePos);
  }

  private void _printBytes(int n) throws IOException {
    for (int i = 0; i < n; i++) {
      byte b = (byte) raf.read();
      int ub = (b < 0) ? b + 256 : b;
      out.print(ub + "(");
      out.write(b);
      out.print(") ");
    }
    out.println();
  }

  private DataType getDataType(int type) {
    switch (type) {
      case 1:
        return DataType.BYTE;
      case 2:
        return DataType.CHAR;
      case 3:
        return DataType.SHORT;
      case 4:
        return DataType.INT;
      case 5:
        return DataType.FLOAT;
      case 6:
        return DataType.DOUBLE;
    }
    throw new IllegalArgumentException("unknown type == " + type);
  }

  static int getType(DataType dt) {
    if (dt == DataType.BYTE) return 1;
    else if ((dt == DataType.CHAR) || (dt == DataType.STRING)) return 2;
    else if (dt == DataType.SHORT) return 3;
    else if (dt == DataType.INT) return 4;
    else if (dt == DataType.FLOAT) return 5;
    else if (dt == DataType.DOUBLE) return 6;

    throw new IllegalArgumentException("unknown DataType == " + dt);
  }

  /**
   * Write the header out, based on ncfile structures.
   *
   * @param raf    write to this <Dimension>
   * @param ncfile the header of this NetcdfFile
   * @param fill   use fill or not
   * @param out    debugging output
   * @throws IOException on write error
   */
  void create(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile, boolean fill, PrintStream out) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;
    if (out != null) this.out = out;

    // make sure ncfile structures were finished LOOK assume done
    // ncfile.finish();

    // magic number
    raf.seek(0);
    raf.write(N3header.MAGIC);

    // numrecs
    raf.writeInt(0);

    // dims
    List dims = ncfile.getDimensions();
    int numdims = dims.size();
    if (numdims == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(N3header.MAGIC_DIM);
      raf.writeInt(numdims);
    }
    for (int i = 0; i < numdims; i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (debugPos && (out != null)) out.println("  dim " + i + " pos= " + raf.getFilePointer());
      writeString(dim.getName());
      raf.writeInt(dim.isUnlimited() ? 0 : dim.getLength());
      if (dim.isUnlimited()) udim = dim;
    }

    // global attributes
    globalAttsPos = raf.getFilePointer();
    writeAtts(ncfile.getGlobalAttributes());

    // variables
    List<Variable> vars = ncfile.getVariables();
    writeVars(vars);

    // now calculate where things go
    dataStart = raf.getFilePointer();
    long pos = dataStart;

    // non-record variable starting positions
    for (Variable var : vars) {
      Vinfo vinfo = (Vinfo) var.getSPobject();
      if (!vinfo.isRecord) {
        raf.seek(vinfo.begin);
        raf.writeInt((int) pos); // LOOK int not long
        vinfo.begin = pos;
        if (debugVariablePos)
          System.out.println(var.getName() + " begin at = " + vinfo.begin + " end=" + (vinfo.begin + vinfo.vsize));
        pos += vinfo.vsize;
      }
    }

    recStart = pos; // record variables start here

    // record variable starting positions
    for (Variable var : vars) {
      Vinfo vinfo = (Vinfo) var.getSPobject();
      if (vinfo.isRecord) {
        raf.seek(vinfo.begin);
        raf.writeInt((int) pos);   // LOOK int not long
        vinfo.begin = pos;
        if (debug) System.out.println(var.getName() + " record begin at = " + dataStart);
        pos += vinfo.vsize;
        uvars.add(var); // track record variables
      }
    }

  }

  void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException {
    long pos;
    if (v2 == null)
      pos = findAtt(globalAttsPos, att.getName());
    else {
      N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
      pos = findAtt(vinfo.attsPos, att.getName());
    }

    raf.seek(pos);
    int type = raf.readInt();
    DataType have = getDataType(type);
    DataType want = att.getDataType();
    if (want == DataType.STRING) want = DataType.CHAR;
    if (want != have)
      throw new IllegalArgumentException("Update Attribute must have same type or original = " + have);

    if (type == 2) {  // String
      String s = att.getStringValue();
      int org = raf.readInt();
      int size = org + padding(org); // ok to use the padding
      int max = Math.min(size, s.length()); // cant make any longer than size
      if (max > org) { // adjust if its using the padding, but not if its shorter
        raf.seek(pos + 4);
        raf.writeInt(max);
      }

      byte[] b = new byte[size];
      for (int i = 0; i < max; i++)
        b[i] = (byte) s.charAt(i);
      raf.write(b);

    } else {
      int nelems = raf.readInt();
      int max = Math.min(nelems, att.getLength()); // cant make any longer
      for (int j = 0; j < max; j++)
        writeAttributeValue(att.getNumericValue(j));
    }
  }

  private long findAtt(long start_pos, String want) throws IOException {
    raf.seek(start_pos + 4);

    int natts = raf.readInt();
    for (int i = 0; i < natts; i++) {
      String name = readString();
      if (name.equals(want))
        return raf.getFilePointer();

      int type = raf.readInt();

      if (type == 2) {
        readString();
      } else {
        int nelems = raf.readInt();
        DataType dtype = getDataType(type);
        int[] shape = {nelems};
        Array arr = Array.factory(dtype.getPrimitiveClassType(), shape);
        IndexIterator ii = arr.getIndexIterator();
        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += readAttributeValue(dtype, ii);
        skip(nbytes);
      }
    }

    throw new IllegalArgumentException("no such attribute " + want);
  }

  private void writeAtts(List<Attribute> atts) throws IOException {

    int n = atts.size();
    if (n == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(MAGIC_ATT);
      raf.writeInt(n);
    }

    for (int i = 0; i < n; i++) {
      if (debugPos) out.println("***att " + i + " pos= " + raf.getFilePointer());
      Attribute att = atts.get(i);

      writeString(att.getName());
      int type = getType(att.getDataType());
      raf.writeInt(type);

      if (type == 2) {
        writeStringValues(att);
      } else {
        int nelems = att.getLength();
        raf.writeInt(nelems);
        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += writeAttributeValue(att.getNumericValue(j));
        pad(nbytes, (byte) 0);
        if (debugPos) out.println(" end write val pos= " + raf.getFilePointer());
      }
      if (debug) out.println("  " + att.toString() + "\n");
    }
  }

  private void writeStringValues(Attribute att) throws IOException {
    int n = att.getLength();
    if (n == 1)
      writeString(att.getStringValue());
    else {
      StringBuffer values = new StringBuffer();
      for (int i = 0; i < n; i++)
        values.append(att.getStringValue(i));
      writeString(values.toString());
    }
  }

  private int writeAttributeValue(Number numValue) throws IOException {
    if (numValue instanceof Byte) {
      raf.write(numValue.byteValue());
      return 1;

    } else if (numValue instanceof Short) {
      raf.writeShort(numValue.shortValue());
      return 2;

    } else if (numValue instanceof Integer) {
      raf.writeInt(numValue.intValue());
      return 4;

    } else if (numValue instanceof Float) {
      raf.writeFloat(numValue.floatValue());
      return 4;

    } else if (numValue instanceof Double) {
      raf.writeDouble(numValue.doubleValue());
      return 8;
    }

    throw new IllegalStateException("unknown attribute type == " + numValue.getClass().getName());
  }

  private void writeVars(List<Variable> vars) throws IOException {
    int n = vars.size();
    if (n == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(MAGIC_VAR);
      raf.writeInt(n);
    }

    for (int i = 0; i < n; i++) {
      Variable var = vars.get(i);
      writeString(var.getName());

      // dimensions
      int vsize = var.getDataType().getSize();
      List<Dimension> dims = var.getDimensions();
      raf.writeInt(dims.size());
      for (Dimension dim : dims) {
        int dimIndex = findDimensionIndex(ncfile, dim);
        raf.writeInt(dimIndex);

        if (!dim.isUnlimited())
          vsize *= dim.getLength();
      }
      vsize += padding(vsize);

      // variable attributes
      long varAttsPos = raf.getFilePointer();
      writeAtts(var.getAttributes());

      // data type, variable size, beginning file position
      int type = getType(var.getDataType());
      raf.writeInt(type);
      raf.writeInt(vsize);
      long pos = raf.getFilePointer();
      raf.writeInt(0); // come back to this later

      //if (debug) out.println(" name= "+name+" type="+type+" vsize="+vsize+" begin= "+begin+" isRecord="+isRecord+"\n");
      var.setSPobject(new Vinfo(vsize, pos, var.isUnlimited(), varAttsPos));

      // keep track of the record size
      if (var.isUnlimited())
        recsize += vsize;
    }
  }

  // write a string then pad to 4 byte boundary
  private void writeString(String s) throws IOException {
    byte[] b = s.getBytes("UTF-8"); // all strings are encoded in UTF-8 Unicode.
    raf.writeInt(b.length);
    raf.write(b);
    pad(b.length, (byte) 0);
  }

  private int findDimensionIndex(NetcdfFile ncfile, Dimension wantDim) {
    List<Dimension> dims = ncfile.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = dims.get(i);
      if (dim.equals(wantDim)) return i;
    }
    throw new IllegalStateException("unknown Dimension == " + wantDim);
  }

  // pad to a 4 byte boundary
  private void pad(int nbytes, byte fill) throws IOException {
    int pad = padding(nbytes);
    for (int i = 0; i < pad; i++)
      raf.write(fill);
  }

  void writeNumrecs() throws IOException {
    // set number of records in the header
    raf.seek(4);
    raf.writeInt(numrecs);
  }

  void setNumrecs(int n) throws IOException {
    this.numrecs = n;
  }

  synchronized boolean synchNumrecs() throws IOException {
    // check number of records in the header
    // gotta bypass the RAF buffer
    int n = raf.readIntUnbuffered(4);
    if (n == this.numrecs)
      return false;

    // update everything
    this.numrecs = n;

    // set it in the unlimited dimension
    udim.setLength(this.numrecs);

    // set it in all of the record variables
    for (Variable uvar : uvars) {
      uvar.resetShape();
      uvar.invalidateCache();
    }

    return true;
  }


  // variable info for reading/writing
  static class Vinfo {
    int vsize; // size of array in bytes. if isRecord, size per record.
    long begin; // offset of start of data from start of file
    boolean isRecord; // is it a record variable?
    long attsPos = 0; //  attributes start here - used for update

    Vinfo(int vsize, long begin, boolean isRecord, long attsPos) {
      this.vsize = vsize;
      this.begin = begin;
      this.isRecord = isRecord;
      this.attsPos = attsPos;
    }
  }

  /* static public void main( String args[]) {
    System.out.println("Charset.defaultCharset() = "+Charset.defaultCharset());
    SortedMap<String,Charset> avail = Charset.availableCharsets();
    Iterator<Charset> iter = avail.values().iterator();
    while (iter.hasNext()) {
      Charset cs = iter.next();
      System.out.println(cs+" "+cs.isRegistered());
    }
  } */

}