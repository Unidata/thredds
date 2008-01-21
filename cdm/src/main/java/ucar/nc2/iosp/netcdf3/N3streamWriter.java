// $Id: $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.netcdf3;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.IospHelper;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

/**
 * @author john
 */
public class N3streamWriter {

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  private ucar.nc2.NetcdfFile ncfile;
  private List<Vinfo> vinfoList = new ArrayList<Vinfo>(); // output order of the variables
  private boolean debugPos = false, debugWriteData = false;
  private int recStart, recSize;
  private long filePos = 0;

  public N3streamWriter(ucar.nc2.NetcdfFile ncfile) {
    this.ncfile = ncfile;
  }

  /**
   * Write the header to a stream.
   *
   * @param stream write to this stream.
   * @throws IOException if write fails
   */
  public void writeHeader(DataOutputStream stream) throws IOException {

    // make sure ncfile structures were finished
    ncfile.finish();

    // magic number
    stream.write(N3header.MAGIC);
    int count = N3header.MAGIC.length;

    // numrecs
    Dimension udim = ncfile.getUnlimitedDimension();
    int numrec = (udim == null) ? 0 : -1; // -1 means "streaming" - calc numrec through file length
    stream.writeInt(numrec);
    count += 4;

    // dims
    List dims = ncfile.getDimensions();
    int numdims = dims.size();
    if (numdims == 0) {
      stream.writeInt(0);
      stream.writeInt(0);
    } else {
      stream.writeInt(N3header.MAGIC_DIM);
      stream.writeInt(numdims);
    }
    count += 8;

    for (int i = 0; i < numdims; i++) {
      Dimension dim = (Dimension) dims.get(i);
      count += writeString(stream, N3iosp.makeValidNetcdfObjectName( dim.getName()));
      stream.writeInt(dim.isUnlimited() ? 0 : dim.getLength());
      count += 4;
    }

    // global attributes
    count += writeAtts(stream, ncfile.getGlobalAttributes());

    // variables
    List vars = ncfile.getVariables();
    int nvars = vars.size();
    if (nvars == 0) {
      stream.writeInt(0);
      stream.writeInt(0);
    } else {
      stream.writeInt(N3header.MAGIC_VAR);
      stream.writeInt(nvars);
    }
    count += 8;

    // we have to calculate how big the header is before we can actually write it
    // so we set stream = null
    for (int i = 0; i < nvars; i++) {
      Variable var = (Variable) vars.get(i);
      if (var instanceof Structure) continue;
      Vinfo vinfo = writeVar(null, var, 0);
      count += vinfo.hsize;
    }

    // now calculate where things go
    int dataStart = count; // data starts right after the header
    int offset = dataStart; // track data offset

    // do all non-record variables first
    for (int i = 0; i < nvars; i++) {
      Variable var = (Variable) vars.get(i);
      //if (var instanceof Structure) continue;

      if (!var.isUnlimited()) {
        Vinfo vinfo = writeVar(stream, var, (int) offset);
        var.setSPobject(vinfo);
        if (debugPos)
          System.out.println(" " + var.getNameAndDimensions() + " begin at = " + offset + " end=" + (offset + vinfo.vsize));

        offset += vinfo.vsize;
        vinfoList.add(vinfo);
      }
    }

    recStart = (int) offset; // record variables' data starts here
    recSize = 0;

    // do all record variables
    for (int i = 0; i < nvars; i++) {
      Variable var = (Variable) vars.get(i);

      if (var.isUnlimited()) {
        if (var instanceof Structure) continue;
        Vinfo vinfo = writeVar(stream, var, (int) offset);
        var.setSPobject(vinfo);

        if (debugPos)
          System.out.println(" " + var.getNameAndDimensions() + "(record) begin at = " + offset + " end=" + (offset + vinfo.vsize) + " size=" + vinfo.vsize);

        offset += vinfo.vsize;
        recSize += vinfo.vsize;
        vinfoList.add(vinfo);
      }
    }

    filePos = count;
    if (debugPos) System.out.println("header written filePos= " + filePos);


  }

  private Vinfo writeVar(DataOutputStream stream, Variable var, int offset) throws IOException {
    int count = 0;
    count += writeString(stream, N3iosp.makeValidNetcdfObjectName( var.getName()));

    // dimensions
    int vsize = var.getDataType().getSize();
    List<Dimension> dims = var.getDimensions();
    if (null != stream) stream.writeInt(dims.size());
    count += 4;

    for (Dimension dim : dims) {
      int dimIndex = findDimensionIndex(dim);
      if (null != stream) stream.writeInt(dimIndex);
      count += 4;

      if (!dim.isUnlimited())
        vsize *= dim.getLength();
    }
    int pad = N3header.padding(vsize);
    vsize += pad;

    // variable attributes
    count += writeAtts(stream, var.getAttributes());

    // data type, variable size, beginning file position
    int type = N3header.getType(var.getDataType());
    if (null != stream) {
      stream.writeInt(type);
      stream.writeInt(vsize);
      stream.writeInt(offset);
    }
    count += 12;

    //if (debug) out.println(" name= "+name+" type="+type+" vsize="+vsize+" begin= "+begin+" isRecord="+isRecord+"\n");
    return new Vinfo(var, count, vsize, offset, pad, var.isUnlimited());
  }


  private int writeAtts(DataOutputStream stream, List<Attribute> atts) throws IOException {
    int natts = atts.size();
    if (null != stream) {
      if (natts == 0) {
        stream.writeInt(0);
        stream.writeInt(0);
      } else {
        stream.writeInt(N3header.MAGIC_ATT);
        stream.writeInt(natts);
      }
    }
    int count = 8;

    for (int i = 0; i < natts; i++) {
      Attribute att = atts.get(i);

      count += writeString(stream, N3iosp.makeValidNetcdfObjectName( att.getName()));
      int type = N3header.getType(att.getDataType());
      if (null != stream) stream.writeInt(type);
      count += 4;

      if (type == 2) {
        count += writeStringValues(stream, att);
      } else {
        int nelems = att.getLength();
        if (null != stream) stream.writeInt(nelems);
        count += 4;

        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += writeAttributeValue(stream, att.getNumericValue(j));
        count += nbytes;

        count += pad(stream, nbytes, (byte) 0);
      }
    }

    return count;
  }

  private int writeStringValues(DataOutputStream stream, Attribute att) throws IOException {
    int n = att.getLength();
    if (n == 1)
      return writeString(stream, att.getStringValue());
    else {
      StringBuffer values = new StringBuffer();
      for (int i = 0; i < n; i++)
        values.append(att.getStringValue(i));
      return writeString(stream, values.toString());
    }
  }

  private int writeAttributeValue(DataOutputStream stream, Number numValue) throws IOException {
    if (numValue instanceof Byte) {
      if (null != stream) stream.write(numValue.byteValue());
      return 1;

    } else if (numValue instanceof Short) {
      if (null != stream) stream.writeShort(numValue.shortValue());
      return 2;

    } else if (numValue instanceof Integer) {
      if (null != stream) stream.writeInt(numValue.intValue());
      return 4;

    } else if (numValue instanceof Float) {
      if (null != stream) stream.writeFloat(numValue.floatValue());
      return 4;

    } else if (numValue instanceof Double) {
      if (null != stream) stream.writeDouble(numValue.doubleValue());
      return 8;
    }

    throw new IllegalStateException("unknown attribute type == " + numValue.getClass().getName());
  }

  // write a string then pad to 4 byte boundary
  private int writeString(DataOutputStream stream, String s) throws IOException {
    byte[] b = s.getBytes();
    if (null != stream) {
      stream.writeInt(b.length);
      stream.write(b);
    }
    int n = pad(stream, b.length, (byte) 0);
    return n + 4 + b.length;
  }

  private int findDimensionIndex(Dimension wantDim) {
    List dims = ncfile.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (dim.equals(wantDim)) return i;
    }
    throw new IllegalStateException("unknown Dimension == " + wantDim);
  }

  // pad to a 4 byte boundary
  private int pad(DataOutputStream stream, int nbytes, byte fill) throws IOException {
    int pad = N3header.padding(nbytes);
    if (null != stream) {
      for (int i = 0; i < pad; i++)
        stream.write(fill);
    }
    return pad;
  }

  // variable info for reading/writing
  static class Vinfo {
    Variable v;
    int hsize; // header size
    int vsize; // size of array in bytes. if isRecord, size per record.
    int offset; // offset of start of data from start of file
    int pad; // number of padding bytes
    boolean isRecord; // is it a record variable?

    Vinfo(Variable v, int hsize, int vsize, int offset, int pad, boolean isRecord) {
      this.v = v;
      this.hsize = hsize;
      this.vsize = vsize;
      this.offset = offset;
      this.pad = pad;
      this.isRecord = isRecord;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

  public void writeDataAll(DataOutputStream stream) throws IOException {
    for (Vinfo vinfo : vinfoList) {
      if (!vinfo.isRecord) {
        Variable v = vinfo.v;
        writeDataFast(v, stream, v.read());
        pad(stream, vinfo.vsize, (byte) 0);
      }
    }

    // see if it has a record dimension we can use
    // see if it has a record dimension we can use
    boolean useRecordDimension = ncfile.hasUnlimitedDimension();
    if (useRecordDimension) {
      ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    }

    // write record data
    if (useRecordDimension) {
      boolean first = true;

      Structure recordVar = (Structure) ncfile.findVariable("record");
      Structure.Iterator ii = recordVar.getStructureIterator();
      while (ii.hasNext()) {
        StructureData sdata = ii.next();
        int count = 0;

        for (Vinfo vinfo : vinfoList) {
          if (vinfo.isRecord) {
            Variable v = vinfo.v;
            int nbytes = (int) writeDataFast(v, stream, sdata.getArray(v.getName()));
            count += nbytes;
            count += pad(stream, nbytes, (byte) 0);
            if (first && debugWriteData) System.out.println(v.getName() + " wrote " + count + " bytes");

          }
        }
        if (first && debugWriteData) System.out.println("wrote " + count + " bytes");
        first = false;
      }

      // remove the record structure this is rather fishy, perhaps better to leave it
      ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE);
      ncfile.finish();
    }
  }

  public void writeNonRecordData(Variable v, DataOutputStream stream, Array data) throws IOException {
    Vinfo vinfo = (Vinfo) v.getSPobject();
    if (debugWriteData)
      System.out.println("Write " + v.getName() + " at filePos= " + filePos + " vinfo.offset= " + vinfo.offset);
    if (filePos != vinfo.offset) throw new IllegalStateException();

    filePos += writeData(v, stream, data);
    if (vinfo.pad > 0) {
      byte[] dummy = new byte[vinfo.pad];
      stream.write(dummy);
      filePos += vinfo.pad;
    }
  }

  private int recno = 0;
  private boolean first = true;

  public void writeRecordData(DataOutputStream stream, List<Variable> varList) throws IOException {
    long want = recStart + recno * recSize;
    if (debugWriteData) System.out.println("Write record at filePos= " + filePos + " should be= " + want);
    if (filePos != want) throw new IllegalStateException();

    for (Variable v : varList) {
      if (first && debugWriteData) System.out.println("  write record var " + v.getNameAndDimensions());
      filePos += writeData(v, stream, v.read());

      Vinfo vinfo = (Vinfo) v.getSPobject();
      if (vinfo.pad > 0) {
        byte[] dummy = new byte[vinfo.pad];
        stream.write(dummy);
        filePos += vinfo.pad;
      }

    }
    first = false;

    recno++;
  }

  /////////////////////////////////////////////


  private long writeData(Variable v, DataOutputStream stream, Array values) throws java.io.IOException {
    DataType dataType = v.getDataType();
    IndexIterator ii = values.getIndexIterator();

    if (dataType == DataType.BYTE) {
      while (ii.hasNext())
        stream.write(ii.getByteNext());
      return values.getSize();

    } else if (dataType == DataType.CHAR) {
      while (ii.hasNext())
        stream.write(ii.getByteNext());
      return values.getSize();

    } else if (dataType == DataType.SHORT) {
      while (ii.hasNext())
        stream.writeShort(ii.getShortNext());
      return 2 * values.getSize();

    } else if (dataType == DataType.INT) {
      while (ii.hasNext())
        stream.writeInt(ii.getIntNext());
      return 4 * values.getSize();

    } else if (dataType == DataType.FLOAT) {
      while (ii.hasNext())
        stream.writeFloat(ii.getFloatNext());
      return 4 * values.getSize();

    } else if (dataType == DataType.DOUBLE) {
      while (ii.hasNext())
        stream.writeDouble(ii.getDoubleNext());
      return 8 * values.getSize();
    }

    throw new IllegalStateException("dataType= " + dataType);
  }

  private int writeDataFast(Variable v, DataOutputStream stream, Array values) throws java.io.IOException {
    DataType dataType = v.getDataType();

    if (dataType == DataType.BYTE) {
      byte[] pa = (byte[]) values.get1DJavaArray(byte.class);
      for (int i = 0; i < pa.length; i++)
        stream.write(pa[i]);
      return pa.length;

    } else if (dataType == DataType.CHAR) {
      byte[] pa = IospHelper.convertCharToByte((char[]) values.get1DJavaArray(char.class));
      for (int i = 0; i < pa.length; i++)
        stream.write(pa[i]);
      return pa.length;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short[]) values.get1DJavaArray(short.class);
      for (int i = 0; i < pa.length; i++)
        stream.writeShort(pa[i]);
      return 2 * pa.length;

    } else if (dataType == DataType.INT) {
      int[] pa = (int[]) values.get1DJavaArray(int.class);
      for (int i = 0; i < pa.length; i++)
        stream.writeInt(pa[i]);
      return 4 * pa.length;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) values.get1DJavaArray(float.class);
      for (int i = 0; i < pa.length; i++)
        stream.writeFloat(pa[i]);
      return 4 * pa.length;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) values.get1DJavaArray(double.class);
      for (int i = 0; i < pa.length; i++)
        stream.writeDouble(pa[i]);
      return 8 * pa.length;
    }

    throw new IllegalStateException("dataType= " + dataType);
  }

  ////////////////////////////////////////

  public static void writeFromFile(NetcdfFile fileIn, String fileOutName) throws IOException {

    DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileOutName), 10 * 1000));
    N3streamWriter writer = new N3streamWriter(fileIn);
    writer.writeHeader(stream);
    writer.writeDataAll(stream);
    stream.close();
  }


}