/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.nc2.iosp.bufr;

import ucar.nc2.*;
import ucar.nc2.iosp.netcdf3.N3iosp;
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
public class WriteT41_ncRect {
  private static boolean debug = true;

  WriteT41_ncRect(NetcdfFile bufr, String fileOutName, boolean fill) throws IOException, InvalidRangeException {

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
    Dimension recordDim = null;
    Map<String, Dimension> dimHash = new HashMap<String, Dimension>();
    for (Dimension oldD : bufr.getDimensions()) {
      String useName = N3iosp.makeValidNetcdfObjectName(oldD.getName());
      boolean isRecord = useName.equals("record");
      Dimension newD = ncfile.addDimension(useName, isRecord ? 0 : oldD.getLength(),
              oldD.isShared(), isRecord, oldD.isVariableLength());
      dimHash.put(newD.getName(), newD);
      if (isRecord) recordDim = newD;
      if (debug) System.out.println("add dim= " + newD);
    }

    // Variables
    Structure recordStruct = (Structure) bufr.findVariable("obsRecord");
    for (Variable oldVar : recordStruct.getVariables()) {
      if (oldVar.getDataType() == DataType.SEQUENCE) continue;

      String varName = N3iosp.makeValidNetcdfObjectName(oldVar.getShortName());
      DataType newType = oldVar.getDataType();

      List<Dimension> newDims = new ArrayList<Dimension>();
      newDims.add(recordDim);
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

    int max_seq = countSeq(recordStruct);
    Dimension seqD = ncfile.addDimension("level", max_seq);

    for (Variable v : recordStruct.getVariables()) {
      if (v.getDataType() != DataType.SEQUENCE) continue;

      Structure seq = (Structure) v;
      for (Variable seqVar : seq.getVariables()) {
        String varName = N3iosp.makeValidNetcdfObjectName(seqVar.getShortName());
        DataType newType = seqVar.getDataType();

        List<Dimension> newDims = new ArrayList<Dimension>();
        newDims.add(recordDim);
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
    return max;
  }


  static private long maxSize = 1000 * 1000; // 1 MByte

  private double copyVarData(NetcdfFile bufr, NetcdfFileWriteable ncfile, Structure recordStruct) throws IOException, InvalidRangeException {
    int nrecs = (int) recordStruct.getSize();
    int sdataSize = recordStruct.getElementSize();

    double total = 0;
    double totalRecordBytes = 0;
    for (int count = 0; count < nrecs; count++) {

      StructureData recordData = recordStruct.readStructure(count);
      for (StructureMembers.Member m : recordData.getMembers()) {

        if (m.getDataType() == DataType.SEQUENCE) {
          int countLevel = 0;
          ArraySequence seq1 = recordData.getArraySequence(m);
          StructureDataIterator iter = seq1.getStructureDataIterator();
          while (iter.hasNext()) {
            StructureData seqData = iter.next();
            for (StructureMembers.Member seqm : seqData.getMembers()) {
              Array data = seqData.getArray(seqm);
              int[] shape = data.getShape();
              int[] newShape = new int[data.getRank() + 2];
              newShape[0] = 1;
              newShape[1] = 1;
              for (int i = 0; i < data.getRank(); i++)
                newShape[i + 1] = shape[i];

              int[] origin = new int[data.getRank() + 2];
              origin[0] = count;
              origin[1] = countLevel;

              if (debug && (count == 0) && (countLevel == 0)) System.out.println("write to = " + seqm.getName());
              ncfile.write(seqm.getName(), origin, data.reshape(newShape));
            }
            countLevel++;
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

}
