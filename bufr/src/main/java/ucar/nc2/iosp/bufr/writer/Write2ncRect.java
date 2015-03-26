/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp.bufr.writer;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.iosp.bufr.BufrIosp2;
import ucar.ma2.*;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Write BUFR to an nc file, making all substructures into multidimensional (rectangular) arrays.
 * May have nested Structures, but no sequences
 *
 * @author caron
 * @since Jun 21, 2008
 */
public class Write2ncRect {
  private static boolean debug = true;

  Write2ncRect(NetcdfFile bufr, String fileOutName, boolean fill) throws IOException, InvalidRangeException {

    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(fileOutName, fill);
    if (debug) {
      System.out.println("FileWriter write " + bufr.getLocation() + " to " + fileOutName);
    }

    // global attributes
    List<Attribute> glist = bufr.getGlobalAttributes();
    for (Attribute att : glist) {
      String useName = N3iosp.makeValidNetcdfObjectName(att.getShortName());
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
    for (Dimension oldD : bufr.getDimensions()) {
      String useName = N3iosp.makeValidNetcdfObjectName(oldD.getShortName());
      boolean isRecord = useName.equals("record");
      Dimension newD = ncfile.addDimension(useName, oldD.getLength(), true, false, false);
      if (isRecord) recordDim = newD;
      if (debug) System.out.println("add dim= " + newD);
    }

    // Variables
    Structure recordStruct = (Structure) bufr.findVariable(BufrIosp2.obsRecord);
    for (Variable oldVar : recordStruct.getVariables()) {
      if (oldVar.getDataType() == DataType.STRUCTURE) continue;

      String varName = N3iosp.makeValidNetcdfObjectName(oldVar.getShortName());
      DataType newType = oldVar.getDataType();

      List<Dimension> newDims = new ArrayList<>();
      newDims.add(recordDim);
      for (Dimension dim : oldVar.getDimensions()) {
        newDims.add(ncfile.addDimension(oldVar.getShortName() + "_strlen", dim.getLength()));
      }

      Variable newVar = ncfile.addVariable(varName, newType, newDims);
      if (debug) System.out.println("add var= " + newVar);

      // attributes
      List<Attribute> attList = oldVar.getAttributes();
      for (Attribute att : attList) {
        String useName = N3iosp.makeValidNetcdfObjectName(att.getShortName());
        if (att.isArray())
          ncfile.addVariableAttribute(varName, useName, att.getValues());
        else if (att.isString())
          ncfile.addVariableAttribute(varName, useName, att.getStringValue());
        else
          ncfile.addVariableAttribute(varName, useName, att.getNumericValue());
      }
    }

    //int max_seq = countSeq(recordStruct);
    //Dimension seqD = ncfile.addDimension("level", max_seq);

    for (Variable v : recordStruct.getVariables()) {
      if (v.getDataType() != DataType.STRUCTURE) continue;
      String structName = N3iosp.makeValidNetcdfObjectName(v.getShortName());
      int shape[] = v.getShape();

      Dimension structDim = ncfile.addDimension(structName, shape[0]);

      Structure struct = (Structure) v;
      for (Variable seqVar : struct.getVariables()) {
        String varName = N3iosp.makeValidNetcdfObjectName(seqVar.getShortName() + "-" + structName);
        DataType newType = seqVar.getDataType();

        List<Dimension> newDims = new ArrayList<>();
        newDims.add(recordDim);
        newDims.add(structDim);
        for (Dimension dim : seqVar.getDimensions()) {
          newDims.add(ncfile.addDimension(seqVar.getShortName() + "_strlen", dim.getLength()));
        }

        Variable newVar = ncfile.addVariable(varName, newType, newDims);
        if (debug) System.out.println("add var= " + newVar);

        // attributes
        List<Attribute> attList = seqVar.getAttributes();
        for (Attribute att : attList) {
          String useName = N3iosp.makeValidNetcdfObjectName(att.getShortName());
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

    double total = copyVarData(ncfile, recordStruct);
    ncfile.flush();
    System.out.println("FileWriter done total bytes = " + total);
    ncfile.close();
  }

  private double copyVarData(NetcdfFileWriteable ncfile, Structure recordStruct) throws IOException, InvalidRangeException {
    int nrecs = (int) recordStruct.getSize();
    int sdataSize = recordStruct.getElementSize();

    double total = 0;
    double totalRecordBytes = 0;
    for (int count = 0; count < nrecs; count++) {

      StructureData recordData = recordStruct.readStructure(count);
      for (StructureMembers.Member m : recordData.getMembers()) {

        if (m.getDataType() == DataType.STRUCTURE) {
          int countLevel = 0;
          ArrayStructure seq1 = recordData.getArrayStructure(m);
          StructureDataIterator iter = seq1.getStructureDataIterator();
          try {
            while (iter.hasNext()) {
              StructureData seqData = iter.next();
              for (StructureMembers.Member seqm : seqData.getMembers()) {
                Array data = seqData.getArray(seqm);
                int[] shape = data.getShape();
                int[] newShape = new int[data.getRank() + 2];
                newShape[0] = 1;
                newShape[1] = 1;
                System.arraycopy(shape, 0, newShape, 1, data.getRank());

                int[] origin = new int[data.getRank() + 2];
                origin[0] = count;
                origin[1] = countLevel;

                String mname = seqm.getName() + "-" + m.getName();
                if (debug && (count == 0) && (countLevel == 0)) System.out.println("write to = " + mname);
                ncfile.write(mname, origin, data.reshape(newShape));
              }
              countLevel++;
            }
          } finally {
            iter.finish();
          }
        } else {

          Array data = recordData.getArray(m);
          int[] shape = data.getShape();
          int[] newShape = new int[data.getRank() + 1];
          newShape[0] = 1;
          System.arraycopy(shape, 0, newShape, 1, data.getRank());

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

  public static void main(String args[]) throws Exception {

    //String fileIn = "C:/data/bufr/edition3/newIdd/IcingTropopause/IcingTropopause_20080529_0000.bufr";
    String fileIn = "C:\\data\\bufr\\edition3\\meteosat\\METEOSAT7-MVIRI-MTPHRWW-NA-1-20080405123005.000000000Z-909326.bfr ";
    try (NetcdfFile ncf = NetcdfDataset.openFile(fileIn, null)) {
      System.out.println(ncf.toString());
      new Write2ncRect(ncf, "C:/data/bufr2nc.meteosat.nc", true);
    }
  }

}
