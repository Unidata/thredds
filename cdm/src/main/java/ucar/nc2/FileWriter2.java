/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ucar.ma2.*;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.CancelTaskImpl;

/**
 * Utility class for copying a NetcdfFile object, or parts of one, to a netcdf-3 or netcdf-4 disk file.
 * Uses NetcdfFileWriter.
 * This handles the entire CDM model (groups, etc) if you are writing to netcdf-4.
 * <p/>
 * The fileIn may be an NcML file which has a referenced dataset in the location URL, the underlying data
 * (modified by the NcML) is written to the new file. If the NcML does not have a referenced dataset,
 * then the new file is filled with fill values, like ncgen.
 * <p/>
 * <p> Use a NetcdfFileWriter object for a lower level API.
 *
 * @see ucar.nc2.dt.grid.CFGridWriter2
 * @see ucar.nc2.ft.point.writer.CFPointWriter
 *
 * @author caron
 */

public class FileWriter2 {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileWriter2.class);
  static private final long maxSize = 50 * 1000 * 1000; // 50 Mbytes
  static private boolean debug = false, debugWrite = false, debugChunk = false;

  /**
   * Set debugging flags
   *
   * @param debugFlags debug flags
   */
  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlags) {
    debug = debugFlags.isSet("ncfileWriter2/debug");
    debugWrite = debugFlags.isSet("ncfileWriter2/debugWrite");
    debugChunk = debugFlags.isSet("ncfileWriter2/debugChunk");
  }

  //////////////////////////////////////////////////////////////////////////////////////
  private final NetcdfFile fileIn;
  private final NetcdfFileWriter writer;
  private final NetcdfFileWriter.Version version;

  private final Map<Variable, Variable> varMap = new HashMap<>(100);  // oldVar, newVar
  private final List<Variable> varList = new ArrayList<>(100);        // old Vars
  private final Map<String, Dimension> gdimHash = new HashMap<>(33); // name, newDim : global dimensions (classic mode)

  /**
   * Use this constructor to copy entire file. Use this.write() to do actual copy.
   *
   * @param fileIn      copy this file
   * @param fileOutName to this output file
   * @param version     output file version
   * @param chunker     chunking strategy (netcdf4 only)
   * @throws IOException on read/write error
   */
  public FileWriter2(NetcdfFile fileIn, String fileOutName, NetcdfFileWriter.Version version, Nc4Chunking chunker) throws IOException {
    this.fileIn = fileIn;
    this.writer = NetcdfFileWriter.createNew(version, fileOutName, chunker);
    this.version = version;
  }

  public enum N3StructureStrategy {flatten, exclude}

  private N3StructureStrategy n3StructureStrategy;
  public void setN3StructureStrategy(N3StructureStrategy n3StructureStrategy) {
    this.n3StructureStrategy = n3StructureStrategy;
  }

  public NetcdfFileWriter getNetcdfFileWriter() {
    return writer;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  // might be better to push these next up int NetcdfCFWriter, but we want to use copyVarData

  /**
   * Use this constructor to copy specific variables to new file.
   * Only supports classic mode
   * <p/>
   * Use addVariable() to load in variables, then this.write().
   *
   * @param fileWriter this encapsolates new file.
   * @throws IOException on read/write error
   */
  public FileWriter2(NetcdfFileWriter fileWriter) throws IOException {
    this.fileIn = null;
    this.writer = fileWriter;
    this.version = fileWriter.getVersion();
  }

  /**
   * Specify which variable will get written
   *
   * @param oldVar add this variable, and all parent groups
   * @return new Variable.
   */
  public Variable addVariable(Variable oldVar) {
    List<Dimension> newDims = getNewDimensions(oldVar);

    Variable newVar;
    if ((oldVar.getDataType().equals(DataType.STRING)) && (!version.isExtendedModel())) {
      newVar = writer.addStringVariable(null, oldVar, newDims);
    } else {
      newVar = writer.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), newDims);
    }
    varMap.put(oldVar, newVar);
    varList.add(oldVar);

    for (Attribute att : oldVar.getAttributes())
      writer.addVariableAttribute(newVar, att); // atts are immutable

    return newVar;
  }

  private List<Dimension> getNewDimensions(Variable oldVar) {
    List<Dimension> result = new ArrayList<>(oldVar.getRank());

    // dimensions
    for (Dimension oldD : oldVar.getDimensions()) {
      Dimension newD = gdimHash.get(oldD.getShortName());
      if (newD == null) {
        newD = writer.addDimension(null, oldD.getShortName(), oldD.isUnlimited() ? 0 : oldD.getLength(),
                oldD.isShared(), oldD.isUnlimited(), oldD.isVariableLength());
        gdimHash.put(oldD.getShortName(), newD);
        if (debug) System.out.println("add dim= " + newD);
      }
      result.add(newD);
    }
    return result;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  public NetcdfFile write() throws IOException {
    return write(null);
  }

  /**
   * Write the input file to the output file.
   * @param cancel  allow user to cancel; may be null.
   * @return the open output file.
   * @throws IOException
   */
  public NetcdfFile write(CancelTask cancel) throws IOException {

    try {
      if (version.isExtendedModel())
        addGroupExtended(null, fileIn.getRootGroup());
      else
        addGroupClassic();

      if (cancel != null && cancel.isCancel()) return null;

      // create the file
      writer.create();

      if (cancel != null && cancel.isCancel()) return null;
      double total = copyVarData(varList, null, cancel);
      if (cancel != null && cancel.isCancel()) return null;

      writer.flush();
      if (debug) System.out.println("FileWriter done total bytes = " + total);

    } catch (IOException ioe) {
      ioe.printStackTrace();
      writer.abort();  // clean up
      throw ioe;
    }

    return writer.getNetcdfFile();
  }

  private void addGroupClassic() throws IOException {

    if (fileIn.getRootGroup().getGroups().size() != 0) {
      throw new IllegalStateException("Input file has nested groups: cannot write to netcdf-3 format");
    }

    // attributes
    for (Attribute att : fileIn.getGlobalAttributes()) {
      writer.addGroupAttribute(null, att); // atts are immutable
      if (debug) System.out.println("add gatt= " + att);
    }

    // dimensions
    Map<String, Dimension> dimHash = new HashMap<>();
    for (Dimension oldD : fileIn.getDimensions()) {
      Dimension newD = writer.addDimension(null, oldD.getShortName(), oldD.isUnlimited() ? 0 : oldD.getLength(),
              oldD.isShared(), oldD.isUnlimited(), oldD.isVariableLength());
      dimHash.put(oldD.getShortName(), newD);
      if (debug) System.out.println("add dim= " + newD);
    }

    // Variables
    int anonCount = 0;
    for (Variable oldVar : fileIn.getVariables()) {
      if (oldVar instanceof Structure) continue; // ignore for the moment

      List<Dimension> dims = new ArrayList<>();
      for (Dimension oldD : oldVar.getDimensions()) {
        if (!oldD.isShared()) { // netcdf3 dimensions must be shared
          String anonName = "anon" + anonCount;
          anonCount++;
          Dimension newD = writer.addDimension(null, anonName, oldD.getLength());
          dims.add(newD);

        } else {
          Dimension dim = dimHash.get(oldD.getShortName());
          if (dim != null)
            dims.add(dim);
          else
            throw new IllegalStateException("Unknown dimension= " + oldD.getShortName());
        }
      }

      DataType newType = oldVar.getDataType();

      // convert STRING to CHAR
      if (oldVar.getDataType() == DataType.STRING) {
        Array data = oldVar.read();
        IndexIterator ii = data.getIndexIterator();
        int max_len = 0;
        while (ii.hasNext()) {
          String s = (String) ii.getObjectNext();
          max_len = Math.max(max_len, s.length());
        }

        // add last dimension
        String useName = oldVar.getShortName() + "_strlen";
        Dimension newD = writer.addDimension(null, useName, max_len);
        dims.add(newD);

        newType = DataType.CHAR;
      }

      Variable v = writer.addVariable(null, oldVar.getShortName(), newType, dims);
      if (debug) System.out.println("add var= " + v.getNameAndDimensions());
      varMap.put(oldVar, v);
      varList.add(oldVar);

      // attributes
      for (Attribute att : oldVar.getAttributes()) {
        writer.addVariableAttribute(v, att); // atts are immutable
      }
    }
  }

  private void addGroupExtended(Group newParent, Group oldGroup) throws IOException {
    Group newGroup = writer.addGroup(newParent, oldGroup.getShortName());

    // attributes
    for (Attribute att : oldGroup.getAttributes()) {
      writer.addGroupAttribute(newGroup, att); // atts are immutable
      if (debug) System.out.println("add gatt= " + att);
    }

    // typedefs
    for (EnumTypedef td : oldGroup.getEnumTypedefs()) {
      writer.addTypedef(newGroup, td); // td are immutable
      if (debug) System.out.println("add td= " + td);
    }

    // dimensions
    Map<String, Dimension> dimHash = new HashMap<>();
    for (Dimension oldD : oldGroup.getDimensions()) {
      Dimension newD = writer.addDimension(newGroup, oldD.getShortName(), oldD.isUnlimited() ? 0 : oldD.getLength(),
              oldD.isShared(), oldD.isUnlimited(), oldD.isVariableLength());
      dimHash.put(oldD.getShortName(), newD);
      if (debug) System.out.println("add dim= " + newD);
    }

    // Variables
    for (Variable oldVar : oldGroup.getVariables()) {
      List<Dimension> dims = new ArrayList<>();
      for (Dimension oldD : oldVar.getDimensions()) {
        // in case the name changed
        Dimension newD = oldD.isShared() ? dimHash.get(oldD.getShortName()) : oldD;
        if (newD == null)
          newD = newParent.findDimension(oldD.getShortName());
        if (newD == null)
          throw new IllegalStateException("Cant find dimension " + oldD.getShortName());
        dims.add(newD);
      }

      DataType newType = oldVar.getDataType();
      Variable v;
      if (newType == DataType.STRUCTURE) {
        v = writer.addStructure(newGroup, (Structure) oldVar, oldVar.getShortName(), dims);
      } else if(newType.isEnum()) {
        EnumTypedef en = oldVar.getEnumTypedef();
        v = writer.addVariable(newGroup, oldVar.getShortName(), newType, dims);
        v.setEnumTypedef(en);
      } else {
        v = writer.addVariable(newGroup, oldVar.getShortName(), newType, dims);
      }
      varMap.put(oldVar, v);
      varList.add(oldVar);
      if (debug) System.out.println("add var= " + v);

      // attributes
      for (Attribute att : oldVar.getAttributes())
        writer.addVariableAttribute(v, att); // atts are immutable
    }

    // nested groups
    for (Group nested : oldGroup.getGroups())
      addGroupExtended(newGroup, nested);

  }

  /**
   * Write data from varList into new file. Read/Write a maximum of  maxSize bytes at a time.
   * When theres a record variable, its much more efficient to use it.
   *
   * @param oldVars   list of variables from the original file, with data in them
   * @param recordVar the record variable from the original file, or null means dont use record variables
   * @param cancel  allow user to cancel, may be null.
   * @return total number of bytes written
   * @throws IOException if I/O error
   */
  public double copyVarData(List<Variable> oldVars, Structure recordVar, CancelTask cancel) throws IOException {

    boolean useRecordDimension = (recordVar != null);

    // write non-record data
    double total = 0;
    int countVars = 0;
    for (Variable oldVar : oldVars) {
      if (useRecordDimension && oldVar.isUnlimited())
        continue; // skip record variables
      if (oldVar == recordVar)
        continue;

      if (debug)
        System.out.println("write var= " + oldVar.getShortName() + " size = " + oldVar.getSize() + " type=" + oldVar.getDataType());
      if (cancel != null)
        cancel.setProgress("writing " + oldVar.getShortName(), countVars++);

      long size = oldVar.getSize() * oldVar.getElementSize();
      total += size;

      if (size <= maxSize) {
        copyAll(oldVar, varMap.get(oldVar));
      } else {
        copySome(oldVar, varMap.get(oldVar), maxSize, cancel);
      }

      if (cancel != null && cancel.isCancel()) return total;
    }

    // write record data
    if (useRecordDimension) {
      int[] origin = new int[]{0};
      int[] size = new int[]{1};

      int nrecs = (int) recordVar.getSize();
      int sdataSize = recordVar.getElementSize();
      Variable recordVarNew = varMap.get(recordVar);

      double totalRecordBytes = 0;
      for (int count = 0; count < nrecs; count++) {
        origin[0] = count;
        try {
          Array recordData = recordVar.read(origin, size);
          writer.write(recordVarNew, origin, recordData);  // rather magic here - only writes the ones in ncfile !!
          if (debug && (count == 0)) System.out.println("write record size = " + sdataSize);
        } catch (InvalidRangeException e) {
          e.printStackTrace();
          break;
        }
        totalRecordBytes += sdataSize;
        if (cancel != null && cancel.isCancel()) return total;
      }
      total += totalRecordBytes;
      totalRecordBytes /= 1000 * 1000;
      if (debug) System.out.println("write record var; total = " + totalRecordBytes + " Mbytes # recs=" + nrecs);
    }
    return total;
  }

  // copy all the data in oldVar to the newVar
  void copyAll(Variable oldVar, Variable newVar) throws IOException {

    Array data = oldVar.read();
    try {
      if (!version.isNetdf4format() && oldVar.getDataType() == DataType.STRING) {
        data = convertToChar(newVar, data);
      }
      if (data.getSize() > 0)  // zero when record dimension = 0
        writer.write(newVar, data);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage() + " for Variable " + oldVar.getFullName());
    }
  }

  /**
   * Copies data from {@code oldVar} to {@code newVar}. The writes are done in a series of chunks no larger than
   * {@code maxChunkSize} bytes.
   *
   * @param oldVar       a variable from the original file to copy data from.
   * @param newVar       a variable from the original file to copy data from.
   * @param maxChunkSize the size, <b>in bytes</b>, of the largest chunk to write.
   * @param cancel      allow user to cancel, may be null.
   * @throws IOException if an I/O error occurs.
   */
  private void copySome(Variable oldVar, Variable newVar, long maxChunkSize, CancelTask cancel) throws IOException {
    long maxChunkElems = maxChunkSize / oldVar.getElementSize();
    long byteWriteTotal = 0;

    ChunkingIndex index = new ChunkingIndex(oldVar.getShape());
    while (index.currentElement() < index.getSize()) {
      try {
        int[] chunkOrigin = index.getCurrentCounter();
        int[] chunkShape = index.computeChunkShape(maxChunkElems);

        if (cancel != null) cancel.setProgress("Reading chunk "+new Section(chunkOrigin, chunkShape)+" from variable: " + oldVar.getShortName(), -1);
        /* writeProgressEvent.setWriteStatus("Reading chunk from variable: " + oldVar.getShortName());
        if (progressListeners != null) {
          for (FileWriterProgressListener listener : progressListeners) {
            listener.writeProgress(writeProgressEvent);
          }
        } */

        Array data = oldVar.read(chunkOrigin, chunkShape);
        if (!version.isNetdf4format() && oldVar.getDataType() == DataType.STRING) {
          data = convertToChar(newVar, data);
        }

        if (data.getSize() > 0) {// zero when record dimension = 0
          if (cancel != null) cancel.setProgress("Writing chunk "+new Section(chunkOrigin, chunkShape)+" from variable: " + oldVar.getShortName(), -1);

          writer.write(newVar, chunkOrigin, data);
          if (debugWrite)
            System.out.println(" write " + data.getSize() + " bytes at " + new Section(chunkOrigin, chunkShape));

          byteWriteTotal += data.getSize();
        }

        index.setCurrentCounter(index.currentElement() + (int) Index.computeSize(chunkShape));
        if (cancel != null && cancel.isCancel()) return;

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

  /* private boolean hasRecordStructure(NetcdfFile file) {
    Variable v = file.findVariable("record");
    return (v != null) && (v.getDataType() == DataType.STRUCTURE);
  } */

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////
  // contributed by  cwardgar@usgs.gov 4/12/2010

  /**
   * An index that computes chunk shapes. It is intended to be used to compute the origins and shapes for a series
   * of contiguous writes to a multidimensional array.
   * It writes the first n elements (n < maxChunkElems), then the next, etc.
   */
  public static class ChunkingIndex extends Index {
    public ChunkingIndex(int[] shape) {
      super(shape);
    }

    /**
     * Computes the shape of the largest possible <b>contiguous</b> chunk, starting at {@link #getCurrentCounter()}
     * and with {@code numElems <= maxChunkElems}.
     *
     * @param maxChunkElems the maximum number of elements in the chunk shape. The actual element count of the shape
     *                      returned is likely to be different, and can be found with {@link Index#computeSize}.
     * @return the shape of the largest possible contiguous chunk.
     */
    public int[] computeChunkShape(long maxChunkElems) {
      int[] chunkShape = new int[rank];

      for (int iDim = 0; iDim < rank; ++iDim) {
        int size = (int) (maxChunkElems / stride[iDim]);
        size = (size == 0) ? 1 : size;
        size = Math.min(size, shape[iDim] - current[iDim]);
        chunkShape[iDim] = size;
      }

      return chunkShape;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static void usage() {
    System.out.println("usage: ucar.nc2.FileWriter2 -in <fileIn> -out <fileOut> [-netcdf4]");
  }

  /**
   * Better to use ucar.nc.dataset.NetcdfDataset main program instead.
   * <p><strong>ucar.nc2.FileWriter -in fileIn -out fileOut</strong>.
   * <p>where: <ul>
   * <li> fileIn : path of any CDM readable file
   * <li> fileOut: local pathname where netdf-3 file will be written
   * </ol>
   *
   * @param arg -in fileIn -out fileOut [-netcdf4]
   * @throws IOException on read or write error
   */
  public static void main(String arg[]) throws IOException {
    if (arg.length < 4) {
      usage();
      System.exit(0);
    }

    String datasetIn = null, datasetOut = null;
    NetcdfFileWriter.Version version = NetcdfFileWriter.Version.netcdf3;
    for (int i = 0; i < arg.length; i++) {
      String s = arg[i];
      if (s.equalsIgnoreCase("-in")) datasetIn = arg[i + 1];
      if (s.equalsIgnoreCase("-out")) datasetOut = arg[i + 1];
      if (s.equalsIgnoreCase("-netcdf4")) version = NetcdfFileWriter.Version.netcdf4;
    }
    if ((datasetIn == null) || (datasetOut == null)) {
      usage();
      System.exit(0);
    }

    System.out.printf("FileWriter2 copy %s to %s ", datasetIn, datasetOut);
    CancelTaskImpl cancel = new CancelTaskImpl();
    NetcdfFile ncfileIn = ucar.nc2.NetcdfFile.open(datasetIn, cancel);
    if (cancel.isCancel()) return;

    FileWriter2 writer2 = new FileWriter2(ncfileIn, datasetOut, version, null); // currently only the default chunker
    NetcdfFile ncfileOut = writer2.write(cancel);
    if (ncfileOut != null) ncfileOut.close();
    ncfileIn.close();
    System.out.printf("%s%n", cancel);
  }

  // Q:/cdmUnitTest/formats/netcdf4/tst/tst_groups.nc

}


