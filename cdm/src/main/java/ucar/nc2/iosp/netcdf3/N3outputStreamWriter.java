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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.IospHelper;

import java.util.List;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

/**
 * Experimental.
 * @author john
 */
public class N3outputStreamWriter extends N3streamWriter {

  public N3outputStreamWriter(ucar.nc2.NetcdfFile ncfile) {
    super(ncfile);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

  public void writeDataAll(DataOutputStream stream) throws IOException {
    for (Vinfo vinfo : vinfoList) {
      if (!vinfo.isRecord) {
        Variable v = vinfo.v;
        assert filePos == vinfo.offset;
        if (debugPos) System.out.println(" writing at "+filePos+" should be "+vinfo.offset+" "+v.getName());
        int nbytes = writeDataFast(v, stream, v.read());
        filePos += nbytes;
        filePos += pad(stream, nbytes, (byte) 0);
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
      int nrec = 0;

      Structure recordVar = (Structure) ncfile.findVariable("record");
      StructureDataIterator ii = recordVar.getStructureIterator();
      while (ii.hasNext()) {
        StructureData sdata = ii.next();
        int count = 0;

        for (Vinfo vinfo : vinfoList) {
          if (vinfo.isRecord) {
            Variable v = vinfo.v;
            int nbytes = writeDataFast(v, stream, sdata.getArray(v.getName()));
            count += nbytes;
            count += pad(stream, nbytes, (byte) 0);
            if (first && debugWriteData) System.out.println(v.getName() + " wrote " + count + " bytes");
          }
        }
        if (first && debugWriteData) {
          System.out.println("wrote " + count + " bytes");
          first = false;
        }
        nrec++;
      }
      if (debugWriteData) System.out.println("wrote " + nrec + " records");
      stream.flush();

      // remove the record structure this is rather fishy, perhaps better to leave it
      ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE);
      ncfile.finish();
    }
  }

  public void writeNonRecordData(Variable v, DataOutputStream stream, Array data) throws IOException {
    Vinfo vinfo = vinfoMap.get(v);
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
      if (first && debugWriteData) System.out.println("  write record var " + v.getNameAndDimensions()+" filePos="+filePos);
      filePos += writeData(v, stream, v.read());

      Vinfo vinfo = vinfoMap.get(v);
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
    N3outputStreamWriter writer = new N3outputStreamWriter(fileIn);
    int numrec = fileIn.getUnlimitedDimension() == null ? 0 : fileIn.getUnlimitedDimension().getLength();
    writer.writeHeader(stream, numrec);
    writer.writeDataAll(stream);
    stream.close();
  }

  static public void main( String args[]) throws IOException {
    writeFromFile(NetcdfFile.open("C:/data/metars/Surface_METAR_20070331_0000.nc"), "C:/temp/streamOut.nc");
  }
}