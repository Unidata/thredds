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

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;

/**
 * @author john
 */
public class N3channelWriter {
  static private int buffer_size = 1000 * 1000;

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  private ucar.nc2.NetcdfFile ncfile;
  private List vinfoList = new ArrayList(); // output order of the variables
  private boolean debugPos = false, debugWriteData = false;

  public N3channelWriter(ucar.nc2.NetcdfFile ncfile) {
    this.ncfile = ncfile;
  }

  /**
   * Write the header to a stream.
   *
   * @param stream write to this stream.
   * @throws IOException if write fails
   */
  void writeHeader(DataOutputStream stream) throws IOException {

    // make sure ncfile structures were finished
    ncfile.finish();

    // magic number
    stream.write(N3header.MAGIC);
    int count = N3header.MAGIC.length;

    // numrecs
    Dimension udim = ncfile.getUnlimitedDimension();
    int numrec = (udim == null) ? 0 : udim.getLength(); // LOOK do we know this?
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
      count += writeString(stream, dim.getName());
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
        Vinfo vinfo = writeVar(stream, var, offset);
        if (debugPos)
          System.out.println(var.getName() + " begin at = " + offset + " end=" + (offset + vinfo.vsize));

        offset += vinfo.vsize;
        vinfoList.add(vinfo);
      }
    }

    int recStart = offset; // record variables' data starts here
    int recsize = 0;

    // do all record variables
    for (int i = 0; i < nvars; i++) {
      Variable var = (Variable) vars.get(i);

      if (var.isUnlimited()) {
        if (var instanceof Structure) continue;
        Vinfo vinfo = writeVar(stream, var, offset);

        if (debugPos)
          System.out.println(var.getName() + "(record) begin at = " + offset + " end=" + (offset + vinfo.vsize) + " size=" + vinfo.vsize);

        offset += vinfo.vsize;
        recsize += vinfo.vsize;
        vinfoList.add(vinfo);
      }
    }

    if (debugPos) System.out.println("recsize = " + recsize);


  }

  private Vinfo writeVar(DataOutputStream stream, Variable var, int offset) throws IOException {
    int count = 0;
    count += writeString(stream, var.getName());

    // dimensions
    int vsize = var.getDataType().getSize();
    List<Dimension> dims = var.getDimensions();
    if (null != stream) stream.writeInt(dims.size());
    count += 4;

    for (int j = 0; j < dims.size(); j++) {
      Dimension dim = dims.get(j);
      int dimIndex = findDimensionIndex(dim);
      if (null != stream) stream.writeInt(dimIndex);
      count += 4;

      if (!dim.isUnlimited())
        vsize *= dim.getLength();
    }
    vsize += N3header.padding(vsize);

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
    Vinfo vinfo = new Vinfo(var, count, vsize, var.isUnlimited());

    return vinfo;
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

      count += writeString(stream, att.getName());
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
    int pad; // offset of start of data from start of file
    boolean isRecord; // is it a record variable?

    Vinfo(Variable v, int hsize, int vsize, boolean isRecord) {
      this.v = v;
      this.hsize = hsize;
      this.vsize = vsize;
      this.isRecord = isRecord;
    }
  }

  public void writeData(WritableByteChannel out) throws IOException, InvalidRangeException {
    for (int i = 0; i < vinfoList.size(); i++) {
      Vinfo vinfo = (Vinfo) vinfoList.get(i);
      if (!vinfo.isRecord) {
        Variable v = vinfo.v;
        ncfile.readData(v, v.getRanges(), out);
        // pad(stream, vinfo.vsize, (byte) 0);
      }
    }

    // must use record dimension if it exists+
    boolean useRecordDimension = ncfile.hasUnlimitedDimension();
    if (useRecordDimension) {
      ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

      Structure recordVar = (Structure) ncfile.findVariable("record");
      List<Range> ranges =  new ArrayList<Range>();
      ranges.add(null);

      long total = 0;
      int nrecs = (int) recordVar.getSize();
      int sdataSize = recordVar.getElementSize();
      int nr = Math.max(buffer_size/sdataSize, 1);

      for (int count = 0; count < nrecs; count+=nr) {
        ranges.set(0, new Range(count, count+nr-1));
        try {
          total += ncfile.readData(recordVar, ranges, out);
        } catch (InvalidRangeException e) {
          e.printStackTrace();
          break;
        }
      }
      total /= 1000 * 1000;
      System.out.println("write record var; total = " + total + " Mbytes # recs=" + nrecs+" nr= "+nr);

      // remove the record structure this is rather fishy, perhaps better to leave it
      ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE);
      ncfile.finish();
    }

  }

  ////////////////////////////////////////

  public static void writeFromFile(NetcdfFile fileIn, String fileOutName) throws IOException, InvalidRangeException {

    FileOutputStream stream = new FileOutputStream(fileOutName);
    WritableByteChannel channel = stream.getChannel();
    DataOutputStream dout = new DataOutputStream(Channels.newOutputStream(channel));

    N3channelWriter writer = new N3channelWriter(fileIn);
    writer.writeHeader(dout);
    writer.writeData(channel);
    channel.close();
  }


}