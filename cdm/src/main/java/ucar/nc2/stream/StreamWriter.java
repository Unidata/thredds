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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.stream;

import ucar.ma2.*;
import ucar.nc2.*;

import java.util.List;
import java.io.*;

/**
 * @author caron
 * @since Jul 12, 2007
 */
public class StreamWriter {
  static final byte[] MAGIC_DATA = new byte[]{0x43, 0x44, 0x46, 0x55};  // 'DATA'
  static final byte[] MAGIC_HEADER = new byte[]{0x43, 0x44, 0x46, 0x55};  // 'CDFS'
  static final byte MAGIC_DIM = 10;
  static final byte MAGIC_VAR = 11;
  static final byte MAGIC_ATT = 12;

  private NetcdfFile ncfile;
  private DataOutputStream out;

  public void StreamWriter(NetcdfFile ncfile, DataOutputStream out) throws IOException {
    this.ncfile = ncfile;
    this.out = out;

    writeHeader();

    List<Variable> vars = ncfile.getVariables();
    for (Variable v : vars) {
      writeData(v);
    }
  }

  int writeHeader() throws IOException {

    // magic number
    int count = writeBytes(MAGIC_HEADER);

    // dims
    List<Dimension> dims = ncfile.getDimensions();
    int ndims = dims.size();
    if (ndims > 0) {
      count += writeByte(MAGIC_DIM);
      count += writeVInt(ndims);
      for (int i = 0; i < ndims; i++) {
        Dimension dim = dims.get(i);
        count += writeString(dim.getName());
        count += writeVInt(dim.getLength());
      }
    }

    // global attributes
    count += writeAtts(ncfile.getGlobalAttributes());

    // variables
    List<Variable> vars = ncfile.getVariables();
    int nvars = vars.size();
    if (nvars > 0) {
      count += writeByte(MAGIC_VAR);
      count += writeVInt(nvars);

      for (int i = 0; i < nvars; i++) {
        Variable v = vars.get(i);
        count += writeVar(v);
      }
    }
    return count;
  }

  private int writeVar(Variable var) throws IOException {
    int count = 0;
    count += writeString(var.getName());

    int type = getType(var.getDataType());
    count += writeVInt(type);

    // dimensions
    List<Dimension> dims = var.getDimensions();
    count += writeVInt(dims.size());
    for (Dimension dim : dims) {
      int dimIndex = findDimensionIndex(dim);
      count += writeVInt(dimIndex);
    }

    // variable attributes
    count += writeAtts(var.getAttributes());

    return count;
  }


  private int writeAtts(List<Attribute> atts) throws IOException {
    int natts = atts.size();
    if (natts == 0) return 0;

    int count = writeByte(MAGIC_ATT);
    count += writeVInt(natts);
    for (int i = 0; i < natts; i++) {
      Attribute att = atts.get(i);
      count += writeString(att.getName());
      int type = getType(att.getDataType());
      writeVInt(type);

      if (type == 2) {
        count += writeStringValues(att);
      } else {
        int nelems = att.getLength();
        count += writeVInt(nelems);

        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += writeAttributeValue( att.getNumericValue(j));
        count += nbytes;

        // count += pad(stream, nbytes, (byte) 0); no padding !!
      }
    }

    return count;
  }

  private int writeStringValues(Attribute att) throws IOException {
    int n = att.getLength();
    if (n == 1)
      return writeString(att.getStringValue());
    else {
      StringBuffer values = new StringBuffer();
      for (int i = 0; i < n; i++)
        values.append(att.getStringValue(i));
      return writeString(values.toString());
    }
  }

  private int writeAttributeValue(Number numValue) throws IOException {
    if (numValue instanceof Byte) {
      out.write(numValue.byteValue());
      return 1;

    } else if (numValue instanceof Short) {
      out.writeShort(numValue.shortValue());
      return 2;

    } else if (numValue instanceof Integer) {
      out.writeInt(numValue.intValue());
      return 4;

    } else if (numValue instanceof Float) {
      out.writeFloat(numValue.floatValue());
      return 4;

    } else if (numValue instanceof Double) {
      out.writeDouble(numValue.doubleValue());
      return 8;
    }

    throw new IllegalStateException("unknown attribute type == " + numValue.getClass().getName());
  }

  private int findDimensionIndex(Dimension wantDim) {
    List dims = ncfile.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (dim.equals(wantDim)) return i;
    }
    throw new IllegalStateException("unknown Dimension == " + wantDim);
  }

  static int getType(DataType dt) {
    if (dt == DataType.BYTE) return 1;
    else if ((dt == DataType.CHAR) || (dt == DataType.STRING)) return 2;
    else if (dt == DataType.SHORT) return 3;
    else if (dt == DataType.INT) return 4;
    else if (dt == DataType.FLOAT) return 5;
    else if (dt == DataType.DOUBLE) return 6;

    throw new IllegalStateException("unknown DataType == " + dt);
  }

  public int writeData(Variable v) throws IOException {
    int count = writeBytes(MAGIC_DATA);
    count += writeData(v.getDataType(), v.read());
    return count;
  }

  /////////////////////////////////////////////

  private int writeData(DataType dataType, Array values) throws java.io.IOException {

    if (dataType == DataType.BYTE) {
      byte[] pa = (byte[]) values.get1DJavaArray(byte.class);
      for (byte b : pa) out.write(b);
      return pa.length;

    } else if (dataType == DataType.CHAR) {
      char[] pa =(char[]) values.get1DJavaArray(char.class);
      for (char c : pa) out.write( (byte) c);
      return pa.length;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short[]) values.get1DJavaArray(short.class);
      for (short s : pa) out.writeShort(s);
      return 2 * pa.length;

    } else if (dataType == DataType.INT) {
      int[] pa = (int[]) values.get1DJavaArray(int.class);
      for (int i : pa) out.writeInt(i);
      return 4 * pa.length;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) values.get1DJavaArray(float.class);
      for (float f : pa) out.writeFloat(f);
      return 4 * pa.length;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) values.get1DJavaArray(double.class);
      for (double d : pa) out.writeDouble(d);
      return 8 * pa.length;
    }

    throw new IllegalStateException("dataType= " + dataType);
  }

  ////////////////////////////////////////
  // from org.apache.lucene.store.IndexOutput

  private int writeByte(byte b) throws IOException {
    out.write(b);
    return 1;
  }

  private int writeBytes(byte[] b, int offset, int length) throws IOException {
    out.write(b, offset, length);
    return length;
  }

  private int writeBytes(byte[] b) throws IOException {
    return writeBytes(b, 0, b.length);
  }

  private int writeVInt(int i) throws IOException {
    int count = 0;
    while ((i & ~0x7F) != 0) {
      writeByte((byte) ((i & 0x7f) | 0x80));
      i >>>= 7;
      count++;
    }
    writeByte((byte) i);
    return count + 1;
  }

  /**
   * Writes an long in a variable-length format.  Writes between one and five
   * bytes.  Smaller values take fewer bytes.  Negative numbers are not
   * supported.
   */
  private int writeVLong(long i) throws IOException {
    int count = 0;
    while ((i & ~0x7F) != 0) {
      writeByte((byte) ((i & 0x7f) | 0x80));
      i >>>= 7;
      count++;
    }
    writeByte((byte) i);
    return count + 1;
  }

  /**
   * Writes a string.
   */
  private int writeString(String s) throws IOException {
    int length = s.length();
    int count = writeVInt(length);
    count += writeChars(s, 0, length);
    return count;
  }

  /**
   * Writes a sequence of UTF-8 encoded characters from a string.
   *
   * @param s      the source of the characters
   * @param start  the first character in the sequence
   * @param length the number of characters in the sequence
   */
  private int writeChars(String s, int start, int length) throws IOException {
    final int end = start + length;
    int count = 0;
    for (int i = start; i < end; i++) {
      final int code = (int) s.charAt(i);
      if (code >= 0x01 && code <= 0x7F) {
        writeByte((byte) code);
        count++;
      } else if (((code >= 0x80) && (code <= 0x7FF)) || code == 0) {
        writeByte((byte) (0xC0 | (code >> 6)));
        writeByte((byte) (0x80 | (code & 0x3F)));
        count += 2;
      } else {
        writeByte((byte) (0xE0 | (code >>> 12)));
        writeByte((byte) (0x80 | ((code >> 6) & 0x3F)));
        writeByte((byte) (0x80 | (code & 0x3F)));
        count += 3;
      }
    }
    return count;
  }


}
