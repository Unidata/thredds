/*
 * Copyright 1998-2012 University Corporation for Atmospheric Research/Unidata
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ucar.ma2.*;
import ucar.nc2.jni.netcdf.Nc4Chunking;
import ucar.nc2.jni.netcdf.Nc4ChunkingStrategyImpl;

/**
 * Utility class for copying a NetcdfFile object, or parts of one, to a netcdf-3 or netcdf-4 disk file.
 * Uses NetcdfFileWriter.
 * This handles entire CDM model (groups, etc) if you are writing to netcdf-4
 * <p/>
 * Copy a NetcdfFile to a Netcdf-3 or Netcdf-4 local file. This allows you, for example, to create a "view" of another
 * NetcdfFile using NcML, and/or to write a remote or OpenDAP file into a local netcdf file.
 * All metadata and data is copied out of the NetcdfFile and into the NetcdfFileWritable.
 * <p/>
 * <p/>
 * The fileIn may be an NcML file which has a referenced dataset in the location URL, the underlying data
 * (modified by the NcML) is written to the new file. If the NcML does not have a referenced dataset,
 * then the new file is filled with fill values, like ncgen.
 * <p/>
 * <p> Use a NetcdfFileWriter object for a lower level API.
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
  private List<FileWriterProgressListener> progressListeners;

  private final Map<Variable, Variable> varMap = new HashMap<Variable, Variable>();  // oldVar, newVar
  private final List<Variable> varList = new ArrayList<Variable>();        // old Vars
  private final Map<String, Dimension> gdimHash = new HashMap<String, Dimension>(); // name, newDim : global dimensions (classic mode)

  private Nc4Chunking chunker = new Nc4ChunkingStrategyImpl();

  /**
   * Use this constructor to copy entire file. Use this.write() to do actual copy.
   *
   * @param fileIn      copy this file
   * @param fileOutName to this output file
   * @param version     output file version
   * @throws IOException on read/write error
   */
  public FileWriter2(NetcdfFile fileIn, String fileOutName, NetcdfFileWriter.Version version, Nc4Chunking chunker) throws IOException {
    this.fileIn = fileIn;
    this.writer = NetcdfFileWriter.createNew(version, fileOutName, chunker);
    this.version = version;
  }

  public void addProgressListener(FileWriterProgressListener listener) {
    if (progressListeners == null) progressListeners = new ArrayList<FileWriterProgressListener>();
    progressListeners.add(listener);
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

    Variable newVar = null;
    if ((oldVar.getDataType().equals(DataType.STRING)) && (!version.isNetdf4format())) {
      newVar = writer.addStringVariable(null, oldVar,newDims);
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
    List<Dimension> result = new ArrayList<Dimension>(oldVar.getRank());

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

    if (version.isNetdf4format())
      addGroup4(null, fileIn.getRootGroup());
    else
      addNetcdf3();

    if (debugWrite)
      System.out.printf("About to write = %n%s%n", writer.getNetcdfFile());

    // create the file
    writer.create();
    if (debug)
      System.out.printf("File Out= %n%s%n", writer.getNetcdfFile());

    /* see if it has a record dimension we can use
    if (!isNetcdf4 && fileIn.hasUnlimitedDimension()) {
      fileIn.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      writer.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    }
    boolean useRecordDimension = hasRecordStructure(fileIn) && hasRecordStructure(writer.getNetcdfFile());
    Structure recordVar = useRecordDimension ? (Structure) fileIn.findVariable("record") : null;  */

    double total = copyVarData(varList, null);
    writer.flush();
    if (debug) System.out.println("FileWriter done total bytes = " + total);

    // fileIn.sendIospMessage(NetcdfFile.IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE); // major crapola
    return writer.getNetcdfFile();
  }

  private void addNetcdf3() throws IOException {

    if (fileIn.getRootGroup().getGroups().size() != 0) {
      throw new IllegalStateException("Input file has nested groups: cannot write to netcdf-3 format");
    }

    // attributes
    for (Attribute att : fileIn.getGlobalAttributes()) {
      writer.addGroupAttribute(null, att); // atts are immutable
      if (debug) System.out.println("add gatt= " + att);
    }

    // dimensions
    Map<String, Dimension> dimHash = new HashMap<String, Dimension>();
    for (Dimension oldD : fileIn.getDimensions()) {
      Dimension newD = writer.addDimension(null, oldD.getShortName(), oldD.isUnlimited() ? 0 : oldD.getLength(),
              oldD.isShared(), oldD.isUnlimited(), oldD.isVariableLength());
      dimHash.put(oldD.getShortName(), newD);
      if (debug) System.out.println("add dim= " + newD);
    }

    // Variables
    int anonCount = 0;
    for (Variable oldVar : fileIn.getVariables()) {
      List<Dimension> dims = new ArrayList<Dimension>();
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

  private void addGroup4(Group newParent, Group oldGroup) throws IOException {
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
    Map<String, Dimension> dimHash = new HashMap<String, Dimension>();
    for (Dimension oldD : oldGroup.getDimensions()) {
      Dimension newD = writer.addDimension(newGroup, oldD.getShortName(), oldD.isUnlimited() ? 0 : oldD.getLength(),
              oldD.isShared(), oldD.isUnlimited(), oldD.isVariableLength());
      dimHash.put(oldD.getShortName(), newD);
      if (debug) System.out.println("add dim= " + newD);
    }

    // Variables
    for (Variable oldVar : oldGroup.getVariables()) {
      List<Dimension> dims = new ArrayList<Dimension>();
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
      Variable v = writer.addVariable(newGroup, oldVar.getShortName(), newType, dims);
      varMap.put(oldVar, v);
      varList.add(oldVar);
      if (debug) System.out.println("add var= " + v);

      // set chunking using the oldVar
      if (chunker.isChunked(oldVar)) {
        long[] chunk = chunker.computeChunking(oldVar);
        // v.addAttribute(new Attribute(CDM.CHUNK_SIZE, Array.factory(chunk)));
        if (debugChunk) {
          System.out.printf("%s is Chunked = (", v.getFullName());
          for (long c : chunk) System.out.printf("%d,", c);
          System.out.printf(")%n");
        }
      } else if (debugChunk) {
        System.out.printf("%s is not Chunked, size = %d bytes%n", v.getFullName(), v.getSize()*v.getElementSize());
      }

      // attributes
      for (Attribute att : oldVar.getAttributes())
        writer.addVariableAttribute(v, att); // atts are immutable
    }

    // nested groups
    for (Group nested : oldGroup.getGroups())
      addGroup4(newGroup, nested);

  }

  /**
   * Write data from varList into new file. Read/Write a maximum of  maxSize bytes at a time.
   * When theres a record variable, its much more efficient to use it.
   *
   * @param varlist   list of variables from the original file, with data in them
   * @param recordVar the record variable from the original file, or null means dont use record variables
   * @return total number of bytes written
   * @throws IOException if I/O error
   */
  public double copyVarData(List<Variable> varlist, Structure recordVar) throws IOException {

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

      if (size <= maxSize) {
        copyAll(oldVar, varMap.get(oldVar));
      } else {
        copySome(oldVar, varMap.get(oldVar), maxSize);
      }
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

      }
      total += totalRecordBytes;
      totalRecordBytes /= 1000 * 1000;
      if (debug) System.out.println("write record var; total = " + totalRecordBytes + " Mbytes # recs=" + nrecs);
    }
    return total;
  }

  // copy all the data in oldVar to the newVar
  private void copyAll(Variable oldVar, Variable newVar) throws IOException {

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
   * @throws IOException if an I/O error occurs.
   */
  private void copySome(Variable oldVar, Variable newVar, long maxChunkSize) throws IOException {
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
        if (!version.isNetdf4format() && oldVar.getDataType() == DataType.STRING) {
          data = convertToChar(newVar, data);
        }

        if (data.getSize() > 0) {// zero when record dimension = 0
          writeProgressEvent.setWriteStatus("Writing chunk of variable: " + oldVar.getShortName());
          writeProgressEvent.setBytesToWrite(data.getSize());
          if (progressListeners != null) {
            for (FileWriterProgressListener listener : progressListeners) {
              listener.writeProgress(writeProgressEvent);
            }
          }

          writer.write(newVar, chunkOrigin, data);
          if (debugWrite)
            System.out.println(" write " + data.getSize() + " bytes at " + new Section(chunkOrigin, chunkShape));

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

  private boolean hasRecordStructure(NetcdfFile file) {
    Variable v = file.findVariable("record");
    return (v != null) && (v.getDataType() == DataType.STRUCTURE);
  }

  /*
   * Add a Variable to the file. The data is copied when finish() is called.
   * The variable's Dimensions are added for you, if not already been added.
   *
   * @param oldVar copy this Variable to new file.
   *
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

  /*
   * Add a list of Variables to the file. The data is copied when finish() is called.
   * The Variables' Dimensions are added for you, if not already been added.
   *
   * @param varList list of Variable
   *
  public void writeVariables(List<Variable> varList) {
    for (Variable v : varList) {
      writeVariable(v);
    }
  }

  private final Structure recordVar = null;

  /*
   * Read record data from here (when finish is called). Typically much more efficient.
   * Not sure if this allows subsetting, use with caution!!
   *
   * @param recordVar the record Variable.
   *
  public void setRecordVariable(Structure recordVar) {
    this.recordVar = recordVar;
  }
  */

  /*
   * Call this when all attributes, dimensions, and variables have been added. The data from all
   * Variables will be written to the file. You cannot add any other attributes, dimensions, or variables
   * after this call.
   *
   * @throws IOException on read or write error
   *
  public void finish() throws IOException {
    ncfile.create();

    double total = copyVarData(ncfile, varList, recordVar, null);
    ncfile.close();
    if (debug) System.out.println("FileWriter finish total bytes = " + total);
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

  /**
   * Track the progress of file writing.
   * use FileWriter2.addProgressListener()
   */
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

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static void usage() {
    System.out.println("usage: ucar.nc2.FileWriter2 -in <fileIn> -out <fileOut> [-netcdf4]");
  }

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

    System.out.printf("copy %s to%s%n", datasetIn, datasetOut);
    // NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(datasetIn, null);
    NetcdfFile ncfileIn = ucar.nc2.NetcdfFile.open(datasetIn, null);
    FileWriter2 writer2 = new FileWriter2(ncfileIn, datasetOut, version, null); // currently only the default chunker
    NetcdfFile ncfileOut = writer2.write();
    ncfileIn.close();
    ncfileOut.close();
  }

  // Q:/cdmUnitTest/formats/netcdf4/tst/tst_groups.nc

}


