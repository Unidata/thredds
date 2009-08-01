// $Id: $
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
package ucar.nc2.iosp.netcdf3;

import ucar.nc2.*;
import ucar.ma2.DataType;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.DataOutputStream;

/**
 * Common superclass for N3outputStreamWriter and N3channelStreamWriter.
 *  Experimental
 * @author john
 */
public abstract class N3streamWriter {

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  protected ucar.nc2.NetcdfFile ncfile;
  protected Map<Variable,Vinfo> vinfoMap = new HashMap<Variable,Vinfo>();
  protected List<Vinfo> vinfoList = new ArrayList<Vinfo>(); // output order of the variables
  protected boolean debug=false, debugPos=false, debugWriteData = false;
  protected int recStart, recSize;
  protected boolean usePadding = true;
  protected long filePos = 0;

  protected N3streamWriter(ucar.nc2.NetcdfFile ncfile) {
    this.ncfile = ncfile;
  }

  /**
   * Write the header to a stream.
   *
   * @param stream write to this stream.
   * @param numrec pass in number of record is you know it, else -1 for "streaming" format variant
   * @throws IOException if write fails
   */
  public void writeHeader(DataOutputStream stream, int numrec) throws IOException {

    // make sure ncfile structures were finished
    ncfile.finish();

    // magic number
    stream.write(N3header.MAGIC);
    int count = N3header.MAGIC.length;

    // numrecs
    Dimension udim = ncfile.getUnlimitedDimension();
    if (numrec < 0) {
      numrec = (udim == null) ? 0 : -1; // -1 means "streaming" - calc numrec through file length
    }
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

    if (debug) System.out.println("vars header starts at "+count);

    // variables
    List<Variable> vars = ncfile.getVariables();
    int nvars = vars.size();
    if (nvars == 0) {
      stream.writeInt(0);
      stream.writeInt(0);
    } else {
      stream.writeInt(N3header.MAGIC_VAR);
      stream.writeInt(nvars);
    }
    count += 8;

    /* Note on padding: In the special case of only a single record variable of character, byte, or short
    // type, no padding is used between data values.
    if (nvars == 1) {
      Variable var = vars.get(0);
      DataType dtype = var.getDataType();
      if ((dtype == DataType.CHAR) || (dtype == DataType.BYTE) || (dtype == DataType.SHORT))
        usePadding = false;
    }  */

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
    if (debug) System.out.println(" non-record vars start at "+dataStart);

    // do all non-record variables first
    for (int i = 0; i < nvars; i++) {
      Variable var = (Variable) vars.get(i);
      //if (var instanceof Structure) continue;

      if (!var.isUnlimited()) {
        Vinfo vinfo = writeVar(stream, var, offset);
        vinfoMap.put(var, vinfo);
        if (debugPos)
          System.out.println(" " + var.getNameAndDimensions() + " begin at = " + offset + " end=" + (offset + vinfo.vsize));

        offset += vinfo.vsize;
        vinfoList.add(vinfo);
      }
    }

    if (debug) System.out.println(" record vars start at "+offset);
    recStart = offset; // record variables' data starts here
    recSize = 0;

    // do all record variables
    for (int i = 0; i < nvars; i++) {
      Variable var = (Variable) vars.get(i);

      if (var.isUnlimited()) {
        if (var instanceof Structure) continue;
        Vinfo vinfo = writeVar(stream, var, offset);
        vinfoMap.put(var, vinfo);

        if (debugPos)
          System.out.println(" " + var.getNameAndDimensions() + "(record) begin at = " + offset + " end=" + (offset + vinfo.vsize) + " size=" + vinfo.vsize);

        offset += vinfo.vsize;
        recSize += vinfo.vsize;
        vinfoList.add(vinfo);
      }
    }

    filePos = count;
    if (debugPos) System.out.println("header written filePos= " + filePos+" recsize= "+recSize);
  }

  private Vinfo writeVar(DataOutputStream stream, Variable var, int offset) throws IOException {
    int hsize = 0;
    hsize += writeString(stream, N3iosp.makeValidNetcdfObjectName( var.getName()));

    // dimensions
    int vsize = var.getDataType().getSize();
    List<Dimension> dims = var.getDimensions();
    if (null != stream) stream.writeInt(dims.size());
    hsize += 4;

    for (Dimension dim : dims) {
      int dimIndex = findDimensionIndex(dim);
      if (null != stream) stream.writeInt(dimIndex);
      hsize += 4;

      if (!dim.isUnlimited())
        vsize *= dim.getLength();
    }
    int pad =  (usePadding) ? N3header.padding(vsize) : 0;
    vsize += pad;

    // variable attributes
    hsize += writeAtts(stream, var.getAttributes());

    // data type, variable size, beginning file position
    int type = N3header.getType(var.getDataType());
    if (null != stream) {
      stream.writeInt(type);
      stream.writeInt(vsize);
      stream.writeInt(offset);
    }
    hsize += 12;

    //if (debug) out.println(" name= "+name+" type="+type+" vsize="+vsize+" begin= "+begin+" isRecord="+isRecord+"\n");
    return new Vinfo(var, hsize, vsize, offset, pad, var.isUnlimited());
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
    int hsize = 8;

    for (int i = 0; i < natts; i++) {
      Attribute att = atts.get(i);

      hsize += writeString(stream, N3iosp.makeValidNetcdfObjectName( att.getName()));
      int type = N3header.getType(att.getDataType());
      if (null != stream) stream.writeInt(type);
      hsize += 4;

      if (type == 2) {
        hsize += writeStringValues(stream, att);
      } else {
        int nelems = att.getLength();
        if (null != stream) stream.writeInt(nelems);
        hsize += 4;

        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += writeAttributeValue(stream, att.getNumericValue(j));
        hsize += nbytes;

        hsize += pad(stream, nbytes, (byte) 0);
      }
    }

    return hsize;
  }

  private int writeStringValues(DataOutputStream stream, Attribute att) throws IOException {
    int n = att.getLength();
    if (n == 1)
      return writeString(stream, att.getStringValue());
    else {
      StringBuilder values = new StringBuilder();
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
  protected int pad(DataOutputStream stream, int nbytes, byte fill) throws IOException {
    int pad = N3header.padding(nbytes);
    if (null != stream) {
      for (int i = 0; i < pad; i++)
        stream.write(fill);
    }
    return pad;
  }

  // variable info for reading/writing
  static protected class Vinfo {
    Variable v;
    int hsize; // header size
    int vsize; // size of array in bytes. if isRecord, size per record. includes padding
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

    public String toString() { return v.getName()+" vsize= "+vsize+" pad="+pad; }
  }

}