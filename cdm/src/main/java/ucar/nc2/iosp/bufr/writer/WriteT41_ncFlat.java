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

package ucar.nc2.iosp.bufr.writer;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.iosp.bufr.BufrIosp;
import ucar.ma2.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 21, 2008
 */
public class WriteT41_ncFlat {
  private static boolean debug = true;

  public WriteT41_ncFlat(NetcdfFile bufr, String fileOutName, boolean fill) throws IOException, InvalidRangeException {

    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(fileOutName, fill);
    if (debug) {
      System.out.println("FileWriter write " + bufr.getLocation() + " to " + fileOutName);
    }

    // global attributes
    List<Attribute> glist = bufr.getGlobalAttributes();
    for (Attribute att : glist) {
      String useName = N3iosp.makeValidNetcdfObjectName(att.getName());
      Attribute useAtt;
      if (att.isArray())
        useAtt = ncfile.addGlobalAttribute(useName, att.getValues());
      else if (att.isString())
        useAtt = ncfile.addGlobalAttribute(useName, att.getStringValue());
      else
        useAtt = ncfile.addGlobalAttribute(useName, att.getNumericValue());
      if (debug) System.out.println("add gatt= " + useAtt);
    }

    // global dimensions
    Dimension obsDim = null;
    Map<String, Dimension> dimHash = new HashMap<String, Dimension>();
    for (Dimension oldD : bufr.getDimensions()) {
      String useName = N3iosp.makeValidNetcdfObjectName(oldD.getName());
      boolean isRecord = useName.equals("record");
      Dimension newD = ncfile.addDimension(useName, oldD.getLength());
      dimHash.put(newD.getName(), newD);
      if (isRecord) obsDim = newD;
      if (debug) System.out.println("add dim= " + newD);
    }

    // Variables
    Structure recordStruct = (Structure) bufr.findVariable(BufrIosp.obsRecord);
    for (Variable oldVar : recordStruct.getVariables()) {
      if (oldVar.getDataType() == DataType.SEQUENCE) continue;

      String varName = N3iosp.makeValidNetcdfObjectName(oldVar.getShortName());
      DataType newType = oldVar.getDataType();

      List<Dimension> newDims = new ArrayList<Dimension>();
      newDims.add(obsDim);
      for (Dimension dim : oldVar.getDimensions()) {
        newDims.add(ncfile.addDimension(oldVar.getShortName() + "_strlen", dim.getLength()));
      }

      Variable newVar = ncfile.addVariable(varName, newType, newDims);
      if (debug) System.out.println("add var= " + newVar);

      // attributes
      List<Attribute> attList = oldVar.getAttributes();
      for (Attribute att : attList) {
        String useName = N3iosp.makeValidNetcdfObjectName(att.getName());
        if (att.isArray())
          ncfile.addVariableAttribute(varName, useName, att.getValues());
        else if (att.isString())
          ncfile.addVariableAttribute(varName, useName, att.getStringValue());
        else
          ncfile.addVariableAttribute(varName, useName, att.getNumericValue());
      }
    }

    int total_seq = countSeq(recordStruct);
    Dimension seqD = ncfile.addDimension("seq", total_seq, true, true, false);

    for (Variable v : recordStruct.getVariables()) {
      if (v.getDataType() != DataType.SEQUENCE) continue;

      Structure seq = (Structure) v;
      for (Variable seqVar : seq.getVariables()) {
        String varName = N3iosp.makeValidNetcdfObjectName(seqVar.getShortName());
        DataType newType = seqVar.getDataType();

        List<Dimension> newDims = new ArrayList<Dimension>();
        newDims.add(seqD);
        for (Dimension dim : seqVar.getDimensions()) {
          newDims.add(ncfile.addDimension(seqVar.getShortName() + "_strlen", dim.getLength()));
        }

        Variable newVar = ncfile.addVariable(varName, newType, newDims);
        if (debug) System.out.println("add var= " + newVar);

        // attributes
        List<Attribute> attList = seqVar.getAttributes();
        for (Attribute att : attList) {
          String useName = N3iosp.makeValidNetcdfObjectName(att.getName());
          if (att.isArray())
            ncfile.addVariableAttribute(varName, useName, att.getValues());
          else if (att.isString())
            ncfile.addVariableAttribute(varName, useName, att.getStringValue());
          else
            ncfile.addVariableAttribute(varName, useName, att.getNumericValue());
        }
      }
    }

    // create the file
    ncfile.create();
    if (debug)
      System.out.println("File Out= " + ncfile.toString());

    // boolean ok = (Boolean) ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    double total = copyVarData(bufr, ncfile, recordStruct);
    ncfile.flush();
    if (debug) System.out.println("FileWriter done total bytes = " + total);
    ncfile.close();
  }

  private int countSeq(Structure recordStruct) throws IOException {
    int total = 0;
    int count = 0;
    int max = 0;

    StructureDataIterator iter = recordStruct.getStructureIterator();
    while (iter.hasNext()) {
      StructureData sdata = iter.next();
      ArraySequence seq1 = sdata.getArraySequence("seq1");
      int n = seq1.getStructureDataCount();
      total += n;
      count++;
      max = Math.max(max, n);
    }
    double avg = total / count;
    int wasted = count * max - total;
    double wp = (double) wasted / (count * max);
    System.out.println(" Max = " + max + " avg = " + avg + " wasted = " + wasted + " %= " + wp);
    return total;
  }


  static private long maxSize = 1000 * 1000; // 1 MByte

  private double copyVarData(NetcdfFile bufr, NetcdfFileWriteable ncfile, Structure recordStruct) throws IOException, InvalidRangeException {
    int nrecs = (int) recordStruct.getSize();
    int sdataSize = recordStruct.getElementSize();

    int seqCount = 0;
    double total = 0;
    double totalRecordBytes = 0;
    for (int count = 0; count < nrecs; count++) {

      StructureData recordData = recordStruct.readStructure(count);
      for (StructureMembers.Member m : recordData.getMembers()) {

        if (m.getDataType() == DataType.SEQUENCE) {
          ArraySequence seq1 = recordData.getArraySequence(m);
          StructureDataIterator iter = seq1.getStructureDataIterator();
          while (iter.hasNext()) {
            StructureData seqData = iter.next();
            for (StructureMembers.Member seqm : seqData.getMembers()) {
              Array data = seqData.getArray(seqm);
              int[] shape = data.getShape();
              int[] newShape = new int[data.getRank() + 1];
              newShape[0] = 1;
              for (int i = 0; i < data.getRank(); i++)
                newShape[i + 1] = shape[i];

              int[] origin = new int[data.getRank() + 1];
              origin[0] = seqCount;

              if (debug && (count == 0) && (seqCount == 0)) System.out.println("write to = " + seqm.getName());
              ncfile.write(seqm.getName(), origin, data.reshape(newShape));
            }
            seqCount++;
          }
        } else {

          Array data = recordData.getArray(m);
          int[] shape = data.getShape();
          int[] newShape = new int[data.getRank() + 1];
          newShape[0] = 1;
          for (int i = 0; i < data.getRank(); i++)
            newShape[i + 1] = shape[i];

          int[] origin = new int[data.getRank() + 1];
          origin[0] = count;

          if (debug && (count == 0)) System.out.println("write to = " + m.getName());
          ncfile.write(m.getName(), origin, data.reshape(newShape));
        }
      }
      totalRecordBytes += sdataSize;
    }

    total += totalRecordBytes;
    totalRecordBytes /= 1000 * 1000;
    if (debug) System.out.println("write record var; total = " + totalRecordBytes + " Mbytes # recs=" + nrecs);

    return total;
  }

  private void copyAll(NetcdfFileWriteable ncfile, Variable oldVar) throws IOException {
    String newName = N3iosp.makeValidNetcdfObjectName(oldVar.getName());

    Array data = oldVar.read();
    try {
      if (oldVar.getDataType() == DataType.STRING) {
        data = convertToChar(ncfile.findVariable(newName), data);
      }
      if (data.getSize() > 0)  // zero when record dimension = 0
        ncfile.write(newName, data);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage() + " for Variable " + oldVar.getName());
    }
  }

  private void copySome(NetcdfFileWriteable ncfile, Variable oldVar, int nelems) throws IOException {
    String newName = N3iosp.makeValidNetcdfObjectName(oldVar.getName());

    int[] shape = oldVar.getShape();
    int[] origin = new int[oldVar.getRank()];
    int size = shape[0];

    for (int i = 0; i < size; i += nelems) {
      origin[0] = i;
      int left = size - i;
      shape[0] = Math.min(nelems, left);

      Array data;
      try {
        data = oldVar.read(origin, shape);
        if (oldVar.getDataType() == DataType.STRING) {
          data = convertToChar(ncfile.findVariable(newName), data);
        }
        if (data.getSize() > 0) {// zero when record dimension = 0
          ncfile.write(newName, origin, data);
          if (debug) System.out.println("write " + data.getSize() + " bytes");
        }

      } catch (InvalidRangeException e) {
        e.printStackTrace();
        throw new IOException(e.getMessage());
      }
    }
  }

  private Array convertToChar(Variable newVar, Array oldData) {
    ArrayChar newData = (ArrayChar) Array.factory(DataType.CHAR, newVar.getShape());
    Index ima = newData.getIndex();
    IndexIterator ii = oldData.getIndexIterator();
    while (ii.hasNext()) {
      String s = (String) ii.getObjectNext();
      int[] c = ii.getCurrentCounter();
      for (int i = 0; i < c.length; i++)
        ima.setDim(i, c[i]);
      newData.setString(ima, s);
    }
    return newData;
  }

    /**
   * main.
   */
  public static void main(String args[]) throws Exception, IOException,
      InstantiationException, IllegalAccessException {

    //String fileIn = "C:/data/dt2/point/bufr/IUA_CWAO_20060202_12.bufr";
    //String fileIn = "C:/data/bufr/edition3/idd/profiler/PROFILER_3.bufr";
    //String fileIn = "C:/data/bufr/edition3/ecmwf/synop.bufr";
    //String fileIn = "R:/testdata2/bufr/edition3/idd/profiler/PROFILER_1.bufr";
    String fileIn = "D:/motherlode/bufr/cat.out";
    NetcdfDataset ncf = NetcdfDataset.openDataset(fileIn);
    System.out.println(ncf.toString());

    /* Structure s = (Structure) ncf.findVariable(obsRecord);
    StructureData sdata = s.readStructure(2);
    PrintWriter pw = new PrintWriter(System.out);
    NCdumpW.printStructureData(pw, sdata);  */
    new WriteT41_ncFlat(ncf, "D:/motherlode/bufr/cat2.nc", true);

    //Variable v = ncf.findVariable("recordIndex");
    //NCdumpW.printArray(v.read(), "recordIndex", pw, null);

    /* ucar.nc2.Variable v;

    v = ncf.findVariable("trajectory_id");
    if (v != null) {
      Array data = v.read();
      NCdump.printArray(data, v.getName(), System.out, null);
    }
    v = ncf.findVariable("station_id");
    if (v != null) {
      Array data = v.read();
      NCdump.printArray(data, v.getName(), System.out, null);
    }
    v = ncf.findVariable("firstChild");
    if (v != null) {
      Array data = v.read();
      NCdump.printArray(data, v.getName(), System.out, null);
    }
    v = ncf.findVariable("numChildren");
    if (v != null) {
      Array data = v.read();
      NCdump.printArray(data, v.getName(), System.out, null);
    }
    System.out.println();

    v = ncf.findVariable("record");
    //ucar.nc2.Variable v = ncf.findVariable("Latitude");
    //ucar.nc2.Variable v = ncf.findVariable("time");
    //System.out.println();
    //System.out.println( v.toString());

    if (v instanceof Structure) {
      Structure s = (Structure) v;
      StructureDataIterator iter = s.getStructureIterator();
      int count = 0;
      PrintWriter pw = new PrintWriter( System.out);
      while (iter.hasNext()) {
        System.out.println("record "+count);
        NCdumpW.printStructureData(pw, iter.next());
        count++;
      }
      Array data = v.read();
      NCdump.printArray(data, "record", System.out, null);
    } else {
      Array data = v.read();
      int[] length = data.getShape();
      System.out.println();
      System.out.println("v2 length =" + length[0]);

      IndexIterator ii = data.getIndexIterator();
      for (; ii.hasNext();) {
        System.out.println(ii.getFloatNext());
      }
    }
    ncf.close();  */
  }
}
