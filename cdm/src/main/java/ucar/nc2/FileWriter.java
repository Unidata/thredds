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
package ucar.nc2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ucar.ma2.*;
import ucar.nc2.iosp.netcdf3.N3iosp;

/**
 * Copy a NetcdfFile to a Netcdf-3 local file. This allows you, for example, to create a "view" of another
 * NetcdfFile using NcML, and/or to write a remote or OpenDAP file into a local netcdf file.
 * All metadata and data is copied out of the NetcdfFile and into the NetcdfFileWritable.
 * <p/>
 * <p/>
 * The fileIn may be an NcML file which has a referenced dataset in the location URL, the underlying data
 * (modified by the NcML) is written to the new file. If the NcML does not have a referenced dataset,
 * then the new file is filled with fill values, like ncgen.
 * <p/>
 * <p> Use the static methods writeToFile() to copy an entire file. Create a FileWriter object to control exactly
 * what gets written to the file.
 *
 * @author caron
 * @author Steve Ansari
 * @see ucar.nc2.NetcdfFile
 */

public class FileWriter {
  /**
   * Set debugging flags
   *
   * @param debugFlags debug flags
   */
  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlags) {
    debug = debugFlags.isSet("ncfileWriter/debug");
    debugWrite = debugFlags.isSet("ncfileWriter/debugWrite");
  }

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileWriter.class);

  static private boolean debug = false, debugWrite = false;

  /**
   * Copy a NetcdfFile to a physical file, using Netcdf-3 file format.
   * Cannot do groups, etc, until we get a Netcdf-4 file format.
   *
   * @param fileIn      write from this NetcdfFile
   * @param fileOutName write to this local file
   * @return NetcdfFile that was written to. It remains open for reading or writing.
   * @throws IOException on read or write error
   */
  public static NetcdfFile writeToFile(NetcdfFile fileIn, String fileOutName) throws IOException {
    return writeToFile(fileIn, fileOutName, false, false, null);
  }

  /**
    * Copy a NetcdfFile to a physical file, using Netcdf-3 file format.
    * Cannot do groups, etc, until we get a Netcdf-4 file format.
    *
    * @param fileIn      write from this NetcdfFile
    * @param fileOutName write to this local file
    * @param fill        use fill mode
    * @return NetcdfFile that was written to. It remains open for reading or writing.
    * @throws IOException on read or write error
    */
   public static NetcdfFile writeToFile(NetcdfFile fileIn, String fileOutName, boolean fill) throws IOException {
     return writeToFile(fileIn, fileOutName, fill, false, null);
   }

  /**
   * Copy a NetcdfFile to a physical file, using Netcdf-3 file format.
   * Cannot do groups, etc, until we get a Netcdf-4 file format.
   *
   * @param fileIn            write from this NetcdfFile
   * @param fileOutName       write to this local file
   * @param fill              use fill mode
   * @param isLargeFile       if true, make large file format (> 2Gb offsets)
   * @return NetcdfFile that was written to. It remains open for reading or writing.
   * @throws IOException on read or write error
   */
  public static NetcdfFile writeToFile(NetcdfFile fileIn, String fileOutName, boolean fill, boolean isLargeFile) throws IOException {
    return writeToFile(fileIn, fileOutName, fill, isLargeFile, null);
  }

  /**
   * Copy a NetcdfFile to a physical file, using Netcdf-3 file format.
   *
   * @param fileIn            write from this NetcdfFile
   * @param fileOutName       write to this local file
   * @param fill              use fill mode
   * @param isLargeFile       if true, make large file format (> 2Gb offsets)
   * @param progressListeners List of progress listeners, use null or empty list if there are none.
   * @return NetcdfFile that was written. It remains open for reading or writing.
   * @throws IOException on read or write error
   */
  public static NetcdfFile writeToFile(NetcdfFile fileIn, String fileOutName, boolean fill, boolean isLargeFile,
                                       List<FileWriterProgressListener> progressListeners) throws IOException {

    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(fileOutName, fill);
    if (debug) {
      System.out.println("FileWriter write " + fileIn.getLocation() + " to " + fileOutName);
      System.out.println("File In = " + fileIn);
    }
    ncfile.setLargeFile(isLargeFile);

    // global attributes
    List<Attribute> glist = fileIn.getGlobalAttributes();
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

    // copy dimensions LOOK anon dimensions
    Map<String, Dimension> dimHash = new HashMap<String, Dimension>();
    for (Dimension oldD : fileIn.getDimensions()) {
      String useName = N3iosp.makeValidNetcdfObjectName(oldD.getName());
      Dimension newD = ncfile.addDimension(useName, oldD.isUnlimited() ? 0 : oldD.getLength(),
              oldD.isShared(), oldD.isUnlimited(), oldD.isVariableLength());
      dimHash.put(newD.getName(), newD);
      if (debug) System.out.println("add dim= " + newD);
    }

    // Variables
    List<Variable> varlist = fileIn.getVariables();
    for (Variable oldVar : varlist) {
      // copy dimensions LOOK what about anon dimensions
      List<Dimension> dims = new ArrayList<Dimension>();
      List<Dimension> dimvList = oldVar.getDimensions();
      for (Dimension oldD : dimvList) {
        String useName = N3iosp.makeValidNetcdfObjectName(oldD.getName());
        Dimension dim = dimHash.get(useName);
        if (dim != null)
          dims.add(dim);
        else
          throw new IllegalStateException("Unknown dimension= " + oldD.getName());
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
        String useName = N3iosp.makeValidNetcdfObjectName(oldVar.getShortName() + "_strlen");
        Dimension newD = ncfile.addDimension(useName, max_len);
        dims.add(newD);

        newType = DataType.CHAR;
      }

      String varName = N3iosp.makeValidNetcdfObjectName(oldVar.getShortName());
      Variable v = ncfile.addVariable(varName, newType, dims);
      if (debug) System.out.println("add var= " + v);

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

    // create the file
    ncfile.create();
    if (debug)
      System.out.println("File Out= " + ncfile.toString());

    // see if it has a record dimension we can use
    if (fileIn.hasUnlimitedDimension()) {
      fileIn.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    }
    boolean useRecordDimension = hasRecordStructure(fileIn) && hasRecordStructure(ncfile);
    Structure recordVar = useRecordDimension ? (Structure) fileIn.findVariable("record") : null;

    double total = copyVarData(ncfile, varlist, recordVar, progressListeners);
    ncfile.flush();
    if (debug) System.out.println("FileWriter done total bytes = " + total);

    fileIn.sendIospMessage(NetcdfFile.IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE);
    return ncfile;
  }

  private static boolean hasRecordStructure(NetcdfFile file) {
    Variable v = file.findVariable("record");
    return (v != null) && (v.getDataType() == DataType.STRUCTURE);
  }

  /**
   * Write data from varList into new file. Read/Write a maximum of  maxSize bytes at a time.
   * When theres a record variable, its much more efficient to use it.
   *
   * @param ncfile            write tot this file
   * @param varlist           list of varibles from the original file, with data in them
   * @param recordVar         the record variable from the original file, or null means dont use record variables
   * @param progressListeners List of progress event listeners, may be null
   * @return total number of bytes written
   * @throws IOException if I/O error
   */
  public static double copyVarData(NetcdfFileWriteable ncfile, List<Variable> varlist, Structure recordVar,
                                   List<FileWriterProgressListener> progressListeners) throws IOException {

    boolean useRecordDimension = (recordVar != null);

    // write non-record data
    double total = 0;
    for (Variable oldVar : varlist) {
      if (useRecordDimension && oldVar.isUnlimited())
        continue; // skip record variables
      if (oldVar == recordVar)
        continue;

      if (debug)
        System.out.println("write var= " + oldVar.getShortName() + " size = " + oldVar.getSize() + " type=" + oldVar.getDataType());

      long size = oldVar.getSize() * oldVar.getElementSize();
      total += size;

      long maxSize = 50 * 1000 * 1000; // 50 Mbytes
      if (size <= maxSize) {
        copyAll(ncfile, oldVar);
      } else {
        copySome(ncfile, oldVar, maxSize, progressListeners);
      }
    }

    // write record data
    if (useRecordDimension) {
      int[] origin = new int[]{0};
      int[] size = new int[]{1};

      int nrecs = (int) recordVar.getSize();
      int sdataSize = recordVar.getElementSize();

      double totalRecordBytes = 0;
      for (int count = 0; count < nrecs; count++) {
        origin[0] = count;
        try {
          Array recordData = recordVar.read(origin, size);
          ncfile.write("record", origin, recordData);  // rather magic here - only writes the ones in ncfile !!
          if (debug && (count == 0)) System.out.println("write record size = " + sdataSize);
        } catch (InvalidRangeException e) {
          e.printStackTrace();
          break;
        }
        totalRecordBytes += sdataSize;

      }
      total += totalRecordBytes;
      totalRecordBytes /= 1000 * 1000;
      if (debug) System.out.println("write record var; total = " + totalRecordBytes + " Mbytes # recs=" + nrecs);
    }
    return total;
  }

  private static void copyAll(NetcdfFileWriteable ncfile, Variable oldVar) throws IOException {
    String newName = N3iosp.makeValidNetcdfObjectName(oldVar.getShortName());
    newName = NetcdfFile.escapeName(newName);

    Array data = oldVar.read();
    try {
      if (oldVar.getDataType() == DataType.STRING) {
        data = convertToChar(ncfile.findVariable(newName), data);
      }
      if (data.getSize() > 0)  // zero when record dimension = 0
        ncfile.write(newName, data);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage() + " for Variable " + oldVar.getFullName());
    }
  }

  /////////////////////////////////////////////
  // contributed by  cwardgar@usgs.gov 4/12/2010

  /**
   * An index that computes chunk shapes. It is intended to be used to compute the origins and shapes for a series
   * of contiguous writes to a multidimensional array.
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
        chunkShape[iDim] = (int) (maxChunkElems / stride[iDim]);
        chunkShape[iDim] = (chunkShape[iDim] == 0) ? 1 : chunkShape[iDim];
        chunkShape[iDim] = Math.min(chunkShape[iDim], shape[iDim] - current[iDim]);
      }

      return chunkShape;
    }
  }

  /**
   * Copies data from {@code oldVar} to {@code ncfile}. The writes are done in a series of chunks no larger than
   * {@code maxChunkSize} bytes.
   *
   * @param ncfile       the NetCDF file to write to.
   * @param oldVar       a variable from the original file to copy data from.
   * @param maxChunkSize the size, <b>in bytes</b>, of the largest chunk to write.
   * @throws IOException if an I/O error occurs.
   */
  private static void copySome(NetcdfFileWriteable ncfile, Variable oldVar, long maxChunkSize, List<FileWriterProgressListener> progressListeners) throws IOException {
    String newName = N3iosp.makeValidNetcdfObjectName(oldVar.getShortName());
    long maxChunkElems = maxChunkSize / oldVar.getElementSize();
    long byteWriteTotal = 0;

    FileWriterProgressEvent writeProgressEvent = new FileWriterProgressEvent();
    writeProgressEvent.setStatus("Variable: " + oldVar.getShortName());
    if (progressListeners != null) {
      for (FileWriterProgressListener listener : progressListeners) {
        listener.writeStatus(writeProgressEvent);
      }
    }

    ChunkingIndex index = new ChunkingIndex(oldVar.getShape());
    while (index.currentElement() < index.getSize()) {
      try {
        int[] chunkOrigin = index.getCurrentCounter();
        int[] chunkShape = index.computeChunkShape(maxChunkElems);

        writeProgressEvent.setWriteStatus("Reading chunk from variable: " + oldVar.getShortName());
        if (progressListeners != null) {
          for (FileWriterProgressListener listener : progressListeners) {
            listener.writeProgress(writeProgressEvent);
          }
        }

        Array data = oldVar.read(chunkOrigin, chunkShape);
        if (oldVar.getDataType() == DataType.STRING) {
          data = convertToChar(ncfile.findVariable(newName), data);
        }

        if (data.getSize() > 0) {// zero when record dimension = 0
          writeProgressEvent.setWriteStatus("Writing chunk of variable: " + oldVar.getShortName());
          writeProgressEvent.setBytesToWrite(data.getSize());
          if (progressListeners != null) {
            for (FileWriterProgressListener listener : progressListeners) {
              listener.writeProgress(writeProgressEvent);
            }
          }

          ncfile.write(newName, chunkOrigin, data);
          if (debugWrite) {
            System.out.println(" write " + data.getSize() + " bytes at "+ new Section(chunkOrigin, chunkShape));
          }
          byteWriteTotal += data.getSize();

          writeProgressEvent.setBytesWritten(byteWriteTotal);
          writeProgressEvent.setProgressPercent(100.0 * byteWriteTotal / oldVar.getSize());
          if (progressListeners != null) {
            for (FileWriterProgressListener listener : progressListeners) {
              listener.writeProgress(writeProgressEvent);
            }
          }

        }

        index.setCurrentCounter(index.currentElement() + (int) Index.computeSize(chunkShape));
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        throw new IOException(e.getMessage());
      }
    }
  }

  private static Array convertToChar(Variable newVar, Array oldData) {
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

  //////////////////////////////////////////////////////////////////////////////////////
  private NetcdfFileWriteable ncfile;
  private HashMap<String, Dimension> dimHash = new HashMap<String, Dimension>();
  private List<Variable> varList = new ArrayList<Variable>();

  /**
   * For writing parts of a NetcdfFile to a new Netcdf-3 local file.
   * To copy all the contents, the static method FileWriter.writeToFile() is preferred.
   * These are mostly convenience methods on top of NetcdfFileWriteable.
   *
   * @param fileOutName file name to write to.
   * @param fill        use fill mode or not
   * @throws java.io.IOException on bad
   */
  public FileWriter(String fileOutName, boolean fill) throws IOException {
    this(fileOutName, fill, false, -1);
  }

  /**
   * For writing parts of a NetcdfFile to a new Netcdf-3 local file.
   * To copy all the contents, the static method FileWriter.writeToFile() is preferred.
   * These are mostly convenience methods on top of NetcdfFileWriteable.
   *
   * @param fileOutName file name to write to.
   * @param fill        use fill mode or not
   * @param isLargeFile true if large file format
   * @param extraHeaderBytes add extra bytes in the header, or -1
   * @throws java.io.IOException on bad
   */
  public FileWriter(String fileOutName, boolean fill, boolean isLargeFile, int extraHeaderBytes) throws IOException {
    ncfile = NetcdfFileWriteable.createNew(fileOutName, fill);
    if (isLargeFile)
      ncfile.setLargeFile(isLargeFile);
    if (extraHeaderBytes > 0)
      ncfile.setExtraHeaderBytes(extraHeaderBytes);
  }

  /**
   * Get underlying NetcdfFileWriteable
   *
   * @return underlying NetcdfFileWriteable
   */
  public NetcdfFileWriteable getNetcdf() {
    return ncfile;
  }

  /**
   * Write a global attribute to the file.
   *
   * @param att take attribute name, value, from here
   */
  public void writeGlobalAttribute(Attribute att) {
    String useName = N3iosp.makeValidNetcdfObjectName(att.getName());
    if (att.isArray()) // why rewrite them ??
      ncfile.addGlobalAttribute(useName, att.getValues());
    else if (att.isString())
      ncfile.addGlobalAttribute(useName, att.getStringValue());
    else
      ncfile.addGlobalAttribute(useName, att.getNumericValue());
  }

  /**
   * Write a Variable attribute to the file.
   *
   * @param varName name of variable to attach attribute to
   * @param att     take attribute name, value, from here
   */
  public void writeAttribute(String varName, Attribute att) {
    String attName = N3iosp.makeValidNetcdfObjectName(att.getName());
    varName = N3iosp.makeValidNetcdfObjectName(varName);
    if (att.isArray())
      ncfile.addVariableAttribute(varName, attName, att.getValues());
    else if (att.isString())
      ncfile.addVariableAttribute(varName, attName, att.getStringValue());
    else
      ncfile.addVariableAttribute(varName, attName, att.getNumericValue());
  }

  /**
   * Add a Dimension to the file
   *
   * @param dim copy this dimension
   * @return the new Dimension
   */
  public Dimension writeDimension(Dimension dim) {
    String useName = N3iosp.makeValidNetcdfObjectName(dim.getName());
    Dimension newDim = ncfile.addDimension(useName, dim.isUnlimited() ? 0 : dim.getLength(),
            dim.isShared(), dim.isUnlimited(), dim.isVariableLength());
    dimHash.put(useName, newDim);
    if (debug) System.out.println("write dim= " + newDim);
    return newDim;
  }


  /**
   * Add a Variable to the file. The data is copied when finish() is called.
   * The variable's Dimensions are added for you, if not already been added.
   *
   * @param oldVar copy this Variable to new file.
   */
  public void writeVariable(Variable oldVar) {

    Dimension[] dims = new Dimension[oldVar.getRank()];
    List<Dimension> dimvList = oldVar.getDimensions();
    for (int j = 0; j < dimvList.size(); j++) {
      Dimension oldD = dimvList.get(j);
      Dimension newD = dimHash.get(N3iosp.makeValidNetcdfObjectName(oldD.getName()));
      if (null == newD) {
        newD = writeDimension(oldD);
        dimHash.put(newD.getName(), newD);
      }
      dims[j] = newD;
    }

    String useName = N3iosp.makeValidNetcdfObjectName(oldVar.getShortName());
    if (oldVar.getDataType() == DataType.STRING) {
      try {
        // need to get the maximum string length
        int max_strlen = 0;
        ArrayObject data = (ArrayObject) oldVar.read();
        IndexIterator ii = data.getIndexIterator();
        while (ii.hasNext()) {
          String s = (String) ii.next();
          max_strlen = Math.max(max_strlen, s.length());
        }

        ncfile.addStringVariable(useName, Arrays.asList(dims), max_strlen);
      } catch (IOException ioe) {
        log.error("Error reading String variable " + oldVar, ioe);
        return;
      }
    } else
      ncfile.addVariable(useName, oldVar.getDataType(), dims);


    varList.add(oldVar);
    if (debug) System.out.println("write var= " + oldVar);

    List<Attribute> attList = oldVar.getAttributes();
    for (Attribute att : attList)
      writeAttribute(useName, att);
  }

  /**
   * Add a list of Variables to the file. The data is copied when finish() is called.
   * The Variables' Dimensions are added for you, if not already been added.
   *
   * @param varList list of Variable
   */
  public void writeVariables(List<Variable> varList) {
    for (Variable v : varList) {
      writeVariable(v);
    }
  }

  private Structure recordVar = null;

  /**
   * Read record data from here (when finish is called). Typically much more efficient.
   * LOOK: Not sure if this allows subsetting, use with caution!!
   *
   * @param recordVar the record Variable.
   */
  public void setRecordVariable(Structure recordVar) {
    this.recordVar = recordVar;
  }

  /**
   * Call this when all attributes, dimensions, and variables have been added. The data from all
   * Variables will be written to the file. You cannot add any other attributes, dimensions, or variables
   * after this call.
   *
   * @throws IOException on read or write error
   */
  public void finish() throws IOException {
    ncfile.create();

    double total = copyVarData(ncfile, varList, recordVar, null);
    ncfile.close();
    if (debug) System.out.println("FileWriter finish total bytes = " + total);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static void usage() {
    System.out.println("usage: ucar.nc2.FileWriter -in <fileIn> -out <fileOut> [-delay <millisecs>]");
  }

  public static class FileWriterProgressEvent {
    private double progressPercent;
    private long bytesWritten;
    private long bytesToWrite;
    private String status;
    private String writeStatus;

    public void setProgressPercent(double progressPercent) {
      this.progressPercent = progressPercent;
    }

    public double getProgressPercent() {
      return progressPercent;
    }

    public void setBytesWritten(long bytesWritten) {
      this.bytesWritten = bytesWritten;
    }

    public long getBytesWritten() {
      return bytesWritten;
    }

    public void setBytesToWrite(long bytesToWrite) {
      this.bytesToWrite = bytesToWrite;
    }

    public long getBytesToWrite() {
      return bytesToWrite;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public String getStatus() {
      return status;
    }

    public void setWriteStatus(String writeStatus) {
      this.writeStatus = writeStatus;
    }

    public String getWriteStatus() {
      return writeStatus;
    }

  }

  public interface FileWriterProgressListener {
    void writeProgress(FileWriterProgressEvent event);

    void writeStatus(FileWriterProgressEvent event);
  }

  ////////////////////
  // deprecated - dont support  delay anymore

  /**
   * @deprecated
   */
  public static double copyVarData(NetcdfFileWriteable ncfile, List<Variable> varlist, Structure recordVar, long delay) throws IOException {
    return copyVarData( ncfile, varlist, recordVar, null);
  }


  /**
   * @deprecated
   */
  public static NetcdfFile writeToFile(NetcdfFile fileIn, String fileOutName, boolean fill, int delay) throws IOException {
    return writeToFile(fileIn, fileOutName, fill, false, null);
  }

  /**
   * @deprecated
   */
  public static NetcdfFile writeToFile(NetcdfFile fileIn, String fileOutName, boolean fill, int delay, boolean isLargeFile) throws IOException {
    return writeToFile(fileIn, fileOutName, fill, isLargeFile, null);
  }

    //////////////////////////////////////

  /**
   * Main program.
   * <p><strong>ucar.nc2.FileWriter -in fileIn -out fileOut</strong>.
   * <p>where: <ul>
   * <li> fileIn : path of any CDM readable file
   * <li> fileOut: local pathname where netdf-3 file will be written
   * <li> delay: if set and file has record dimension, delay between writing each record, for testing files that
   * are growing
   * </ol>
   *
   * @param arg -in fileIn -out fileOut
   * @throws IOException on read or write error
   */
  public static void main(String arg[]) throws IOException {
    if (arg.length < 4) {
      usage();
      System.exit(0);
    }

    String datasetIn = null, datasetOut = null;
    int delay = 0;
    for (int i = 0; i < arg.length; i++) {
      String s = arg[i];
      if (s.equalsIgnoreCase("-in")) datasetIn = arg[i + 1];
      if (s.equalsIgnoreCase("-out")) datasetOut = arg[i + 1];
    }
    if ((datasetIn == null) || (datasetOut == null)) {
      usage();
      System.exit(0);
    }

    // NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(datasetIn, null);  LOOK was
    NetcdfFile ncfileIn = ucar.nc2.NetcdfFile.open(datasetIn, null);
    NetcdfFile ncfileOut = ucar.nc2.FileWriter.writeToFile(ncfileIn, datasetOut, false, false, null);
    ncfileIn.close();
    ncfileOut.close();
    System.out.println("NetcdfFile written = " + ncfileOut);
  }

}


