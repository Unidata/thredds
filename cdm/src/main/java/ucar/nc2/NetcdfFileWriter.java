package ucar.nc2;

import ucar.ma2.*;
import ucar.nc2.iosp.IOServiceProviderWriter;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.nc2.iosp.netcdf3.N3header;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.iosp.netcdf3.N3raf;
import ucar.nc2.jni.netcdf.Nc4Iosp;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Helper class for writing Netcdf 3 or 4 files
 *
 * @author caron
 * @since 7/25/12
 */
public class NetcdfFileWriter {

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFileWriter.class);
  static private Set<DataType> valid = EnumSet.of(DataType.BYTE, DataType.CHAR, DataType.SHORT, DataType.INT,
          DataType.DOUBLE, DataType.FLOAT);

  public enum NetcdfVersion {netcdf3, netcdf4 }

  /**
   * Open an existing Netcdf file for writing data. Fill mode is true.
   * Cannot add new objects, you can only read/write data to existing Variables.
   *
   * @param location name of existing file to open.
   * @return existing file that can be written to
   * @throws java.io.IOException on I/O error
   */
  static public NetcdfFileWriter openExisting(String location) throws IOException {
    return new NetcdfFileWriter(null, location, true);  // dont know the version yet
  }

  /**
   * Create a new Netcdf file, with fill mode true.
   *
   * @param location name of new file to open; if it exists, will overwrite it.
   * @return new file that can be written to
   * @throws IOException on I/O error
   */
  static public NetcdfFileWriter createNew(NetcdfVersion version, String location) throws IOException {
    return new NetcdfFileWriter(version, location, false);
  }

  ////////////////////////////////////////////////////////////////////////////////
  private String location;
  private IOServiceProviderWriter spiw;

  // modes
  private boolean defineMode;

  // state
  private NetcdfFile ncfile;
  private boolean isNetcdf4;
  private boolean isNewFile;
  private boolean isLargeFile;
  private boolean fill;
  private int extraHeader;
  private long preallocateSize;

  /**
   * Open or create a new Netcdf file, put it into define mode to allow writing.
   *
   * @param location open a new file at this location
   * @param isExisting true if file already exists
   * @throws IOException on I/O error
   */
  private NetcdfFileWriter(NetcdfVersion version, String location, boolean isExisting) throws IOException {
    this(version, null, null, location, isExisting);
  }

  /**
   * Open or create a new Netcdf file, put it into define mode to allow
   * writing, using the provided IOSP and RAF.
   * This allows specialized writers. LOOK who did this?
   *
   * @param iospw IO service provider to use, if null use standard
   * @param raf Random access file to use, may be null if iospw is, otherwise must be opened read/write
   * @param location open a new file at this location
   * @param isExisting true if file already exists
   * @throws IOException on I/O error
   */
  protected NetcdfFileWriter(NetcdfVersion version, IOServiceProviderWriter iospw, ucar.unidata.io.RandomAccessFile raf,
          String location, boolean isExisting) throws IOException {
    super();
    this.location = location;


    if (isExisting) {
      if (raf == null)
        raf = new ucar.unidata.io.RandomAccessFile(location, "rw");

      if (H5header.isValidFile(raf))
        isNetcdf4 = true;
      else if (!N3header.isValidFile(raf))
        throw new IllegalArgumentException(location+" must be netcdf-3 or netcdf-4 file");

    } else {
      isNewFile = true;
      isNetcdf4 = version == NetcdfVersion.netcdf4;
    }

    if (iospw == null) {
      spiw = isNetcdf4 ? new Nc4Iosp() : new N3raf();
    } else {
      spiw = iospw;
    }

    this.ncfile = new NetcdfFile(spiw, location);  // package private
    if (isExisting) spiw.open(raf, ncfile, null);
    defineMode = true;
  }

  /**
   * Set the fill flag: call before calling create() or doing any data writing. Only used by netcdf-3 (?).
   * If true, the data is first written with fill values.
   * Default is fill = false.
   * Leave false if you expect to write all data values, set to true if you want to be
   * sure that unwritten data values have the fill value in it.
   *
   * @param fill set fill mode true or false
   */
  public void setFill(boolean fill) {
    this.fill = fill;
    spiw.setFill( fill);
  }

  /**
   * Preallocate the file size, for efficiency. Only used by netcdf-3.
   * Must be in define mode
   * Must call before create() to have any affect.
   * @param size if set to > 0, set length of file to this upon creation - this (usually) pre-allocates contiguous storage.
   */
  public void setLength(long size) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    this.preallocateSize = size;
  }

  /**
   * Set if this should be a "large file" (64-bit offset) format. Only used by netcdf-3.
   * Must be in define mode
   * @param isLargeFile true if large file
   */
  public void setLargeFile(boolean isLargeFile) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    this.isLargeFile = isLargeFile;
  }

  /**
   * Set extra bytes to reserve in the header. Only used by netcdf-3.
   * This can prevent rewriting the entire file on redefine.
   * Must be in define mode
   * @param extraHeaderBytes # bytes extra for the header
   */
  public void setExtraHeaderBytes(int extraHeaderBytes) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    this.extraHeader = extraHeaderBytes;
  }

  /**
   * Is the file in define mode, which allows objects to be added and changed?
   * @return true if the file in define mode
   */
  public boolean isDefineMode() { return defineMode; }

  NetcdfFile getNetcdfFile() { return ncfile; }

  ////////////////////////////////////////////
  //// use these calls in define mode

  /**
   * Add a shared Dimension to the file. Must be in define mode.
   *
   * @param dimName name of dimension
   * @param length  size of dimension.
   * @return the created dimension
   */
  public Dimension addDimension(Group g, String dimName, int length) {
    return addDimension(g, dimName, length, true, false, false);
  }

  /**
   * Add a Dimension to the file. Must be in define mode.
   *
   * @param dimName          name of dimension
   * @param length           size of dimension.
   * @param isShared         if dimension is shared
   * @param isUnlimited      if dimension is unlimited
   * @param isVariableLength if dimension is variable length
   * @return the created dimension
   */
  public Dimension addDimension(Group g, String dimName, int length, boolean isShared, boolean isUnlimited, boolean isVariableLength) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    if (!isValidObjectName(dimName)) throw new IllegalArgumentException("illegal dimension name "+dimName);

    Dimension dim = new Dimension(dimName, length, isShared, isUnlimited, isVariableLength);
    ncfile.addDimension(g, dim);
    return dim;
  }

  private boolean isValidObjectName(String name) {
    return N3iosp.isValidNetcdf3ObjectName(name);
  }

  private boolean isValidDataType(DataType dt) {
    return isNetcdf4 || valid.contains(dt);
  }

  private String createValidObjectName(String name) {
    return N3iosp.createValidNetcdf3ObjectName(name);
  }

  /**
   * Rename a Dimension. Must be in define mode.
   * @param oldName existing dimension has this name
   * @param newName rename to this
   * @return renamed dimension, or null if not found
   */
  public Dimension renameDimension(Group g, String oldName, String newName) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    if (!isValidObjectName(newName)) throw new IllegalArgumentException("illegal dimension name "+newName);

    if (g == null) g = ncfile.getRootGroup();
    Dimension dim = g.findDimension(oldName);
    if (null != dim) dim.setName(newName);
    return dim;
  }

  /**
    * Add a Group to the file. Must be in define mode.
    * @param parent the parent of this group, null if root group.
    * @param name the name of this group, unique qithin parent
    * @return the created group
    */
   public Group addGroup(Group parent, String name) {
     if (!defineMode) throw new UnsupportedOperationException("not in define mode");
     if (parent == null) return ncfile.getRootGroup();

     Group result = new Group(ncfile, parent, name);
     parent.addGroup(result);
     return result;
   }


  /**
   * Add a Global attribute to the file. Must be in define mode.
   * @param att the attribute.
   * @return the created attribute
   */
  public Attribute addGroupAttribute(Group g, Attribute att) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");

    if (!isValidObjectName(att.getName())) {
      String attName = createValidObjectName(att.getName());
      log.warn("illegal attribute name= "+att.getName() + " change to "+ attName);
      att = new Attribute(attName, att.getValues());
    }

    return ncfile.addAttribute(g, att);
  }

  /**
   * Add a EnumTypedef to the file. Must be in define mode.
   * @param td the EnumTypedef.
   * @return the created attribute
   */
  public EnumTypedef addTypedef(Group g, EnumTypedef td) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    g.addEnumeration(td);
    return td;
  }

  /**
   * Delete a group Attribute. Must be in define mode.
   * @param attName existing Attribute has this name
   * @return deleted Attribute, or null if not found
   */
  public Attribute deleteGroupAttribute(Group g, String attName) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    if (g == null) g = ncfile.getRootGroup();
    Attribute att = g.findAttribute(attName);
    if (null == att) return null;
    g.remove(att);
    return att;
  }

  /**
   * Rename a group Attribute. Must be in define mode.
   * @param oldName existing Attribute has this name
   * @param newName rename to this
   * @return renamed Attribute, or null if not found
   */
  public Attribute renameGlobalAttribute(Group g, String oldName, String newName) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");

    if (!isValidObjectName(newName)) {
      String newnewName = createValidObjectName(newName);
      log.warn("illegal attribute name= "+newName + " change to "+ newnewName);
      newName = newnewName;
    }

    if (g == null) g = ncfile.getRootGroup();
    Attribute att = g.findAttribute(oldName);
    if (null == att) return null;

    g.remove(att);
    att = new Attribute( newName, att.getValues());
    g.addAttribute( att);
    return att;
  }

  /**
   * Add a variable to the file. Must be in define mode.
   *
   * @param varName  name of Variable, must be unique with the file.
   * @param dataType type of underlying element
   * @param dims     names of Dimensions for the variable, blank seperated.
   *                 Must already have been added. Use an empty string for a scalar variable.
   * @return the Variable that has been added
   */
  public Variable addVariable(Group g, String varName, DataType dataType, String dims) {
    // parse the list
    ArrayList<Dimension> list = new ArrayList<Dimension>();
    StringTokenizer stoker = new StringTokenizer(dims);
    while (stoker.hasMoreTokens()) {
      String tok = stoker.nextToken();
      Dimension d = ncfile.rootGroup.findDimension(tok);
      if (null == d)
        throw new IllegalArgumentException("Cant find dimension " + tok);
      list.add(d);
    }

    return addVariable(g, varName, dataType, list);
  }

  /**
   * Add a variable to the file. Must be in define mode.
   *
   * @param shortName  name of Variable, must be unique with the file.
   * @param dataType type of underlying element
   * @param dims     list of Dimensions for the variable, must already have been added. Use a list of length 0
   *                 for a scalar variable.
   * @return the Variable that has been added
   */
  public Variable addVariable(Group g, String shortName, DataType dataType, List<Dimension> dims) {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");
    if (!isValidObjectName(shortName))
      throw new IllegalArgumentException("illegal variable name: "+shortName);
    if (!isValidDataType(dataType))
      throw new IllegalArgumentException("illegal dataType: "+dataType);

    // check unlimited
    int count = 0;
    for (Dimension d : dims) {
      if (d.isUnlimited())
        if (count != 0)
          throw new IllegalArgumentException("Unlimited dimension "+d+" must be first instead its  ="+count);
      count++;
    }

    Variable v = new Variable(ncfile, g, null, shortName);
    v.setDataType(dataType);
    v.setDimensions(dims);

    long size = v.getSize() * v.getElementSize();
    if (size > N3iosp.MAX_VARSIZE)
      throw new IllegalArgumentException("Variable size in bytes "+size+" may not exceed "+ N3iosp.MAX_VARSIZE);

    ncfile.addVariable(g, v);
    return v;
  }

  /**
   * Add a variable with DataType = String to the file. Must be in define mode.
   * The variable will be stored in the file as a CHAR variable.
   * A new dimension with name "varName_strlen" is automatically added, with length max_strlen.
   *
   * @param varName    name of Variable, must be unique within the file.
   * @param dims       list of Dimensions for the variable, must already have been added. Use a list of length 0
   *                   for a scalar variable. Do not include the string length dimension.
   * @param max_strlen maximum string length.
   * @return the Variable that has been added
   */
  public Variable addStringVariable(Group g, String varName, List<Dimension> dims, int max_strlen) {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");
    if (!N3iosp.isValidNetcdf3ObjectName(varName))
      throw new IllegalArgumentException("illegal netCDF-3 variable name: "+varName);

    Variable v = new Variable(ncfile, g, null, varName);
    v.setDataType(DataType.CHAR);

    Dimension d = addDimension(g, varName + "_strlen", max_strlen);
    ArrayList<Dimension> sdims = new ArrayList<Dimension>(dims);
    sdims.add(d);
    v.setDimensions(sdims);

    ncfile.addVariable(g, v);
    return v;
  }

  /**
   * Rename a Variable. Must be in define mode.
   * @param oldName existing Variable has this name
   * @param newName rename to this
   * @return renamed Variable, or null if not found
   */
  public Variable renameVariable(String oldName, String newName) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    Variable v = ncfile.findVariable(oldName);
    if (null != v) v.setName(newName);
    return v;
  }

  /**
   * Add an attribute to the named Variable. Must be in define mode.
   *
   * @param v       Variable to add attribute to
   * @param att     Attribute to add.
   */
  public void addVariableAttribute(Variable v, Attribute att) {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");

    if (!N3iosp.isValidNetcdf3ObjectName(att.getName())) {
      String attName = N3iosp.createValidNetcdf3ObjectName(att.getName());
      log.warn("illegal netCDF-3 attribute name= "+att.getName() + " change to "+ attName);
      att = new Attribute(attName, att.getValues());
    }

    v.addAttribute(att);
  }

  /**
   * Delete a variable Attribute. Must be in define mode.
   * @param v       Variable to delete attribute to
   * @param attName existing Attribute has this name
   * @return deleted Attribute, or null if not found
   */
  public Attribute deleteVariableAttribute(Variable v, String attName) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");

    Attribute att = v.findAttribute(attName);
    if (null == att) return null;

    v.remove(att);
    return att;
  }


  /**
   * Rename a variable Attribute. Must be in define mode.
   * @param v       Variable to modify attribute
   * @param attName existing Attribute has this name
   * @param newName rename to this
   * @return renamed Attribute, or null if not found
   */
  public Attribute renameVariableAttribute(Variable v, String attName, String newName) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");

    Attribute att = v.findAttribute(attName);
    if (null == att) return null;

    v.remove(att);
    att = new Attribute( newName, att.getValues());
    v.addAttribute( att);
    return att;
  }

  /**
   * Update the value of an existing attribute. Attribute is found by name, which must match exactly.
   * You cannot make an attribute longer, or change the number of values.
   * For strings: truncate if longer, zero fill if shorter.  Strings are padded to 4 byte boundaries, ok to use padding if it exists.
   * For numerics: must have same number of values.
   *
   * @param v2  variable, or null for global attribute
   * @param att replace with this value
   * @throws IOException if I/O error
   */
  public void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException {
    if (defineMode)
      throw new UnsupportedOperationException("in define mode");
    spiw.updateAttribute(v2, att);
  }

  /**
   * After you have added all of the Dimensions, Variables, and Attributes,
   *   call create() to actually create the file. You must be in define mode.
   * After this call, you are no longer in define mode.
   * @throws java.io.IOException if I/O error
   */
  public void create() throws java.io.IOException {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");

    ncfile.finish(); // ??
    spiw.setFill( fill); // ??
    spiw.create(location, ncfile, extraHeader, preallocateSize, isLargeFile);

    defineMode = false;
  }

  ////////////////////////////////////////////
  // redefine

  /**
   * Set the redefine mode.
   * Designed to emulate nc_redef (redefineMode = true) and
   * nc_enddef (redefineMode = false)
   * @param redefineMode start or end define mode
   * @return true if it had to rewrite the entire file, false if it wrote the header in place
   * @throws java.io.IOException on read/write error
   */
  public boolean setRedefineMode(boolean redefineMode) throws IOException {
    if (redefineMode && !defineMode) {
      defineMode = true;

    } else if (!redefineMode && defineMode) {
      defineMode = false;
      ncfile.finish();

      // try to rewrite header, if it fails, then we have to rewrite entire file
      boolean ok = spiw.rewriteHeader( isLargeFile);  // LOOK seems like we should be using isNewFile
      if (!ok)
        rewrite();
      return !ok;
    }

    return false;
  }

  // rewrite entire file
  private void rewrite() throws IOException {
    // close existing file, rename and open as read-only
    spiw.flush();
    spiw.close();

    File prevFile = new File(location);
    File tmpFile = new File(location+".tmp");
    if (tmpFile.exists()) tmpFile.delete();
    if (!prevFile.renameTo(tmpFile)) {
      System.out.println(prevFile.getPath()+ " prevFile.exists "+prevFile.exists()+" canRead = "+ prevFile.canRead());
      System.out.println(tmpFile.getPath()+" tmpFile.exists "+tmpFile.exists()+" canWrite "+ tmpFile.canWrite());
      throw new RuntimeException("Cant rename "+prevFile.getAbsolutePath()+" to "+ tmpFile.getAbsolutePath());
    }

    NetcdfFile oldFile = NetcdfFile.open(tmpFile.getPath());

    // use record dimension if it has one
    Structure recordVar = null;
    if (oldFile.hasUnlimitedDimension()) {
      oldFile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      recordVar = (Structure) oldFile.findVariable("record");
      /* if (recordVar != null) {
        Boolean result = (Boolean) spiw.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
        if (!result)
          recordVar = null;
      } */
    }

    // create new file with current set of objects
    spiw.create(location, ncfile, extraHeader, preallocateSize, isLargeFile);
    spiw.setFill( fill);
    //isClosed = false;

    // wait till header is written before adding the record variable to the file
    if (recordVar != null) {
      Boolean result = (Boolean) spiw.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      if (!result)
        recordVar = null;
    }

    // copy old file to new
    List<Variable> oldList = new ArrayList<Variable>(ncfile.getVariables().size());
    for (Variable v : ncfile.getVariables()) {
      Variable oldVar = oldFile.findVariable(v.getFullNameEscaped());
      if (oldVar != null)
        oldList.add(oldVar);
    }
    FileWriter2 fileWriter2 = new FileWriter2(oldFile, location, isNetcdf4 ? NetcdfVersion.netcdf4 : NetcdfVersion.netcdf3);
    fileWriter2.copyVarData(oldList, recordVar);  // LOOK ??
    flush();

    // delete old
    oldFile.close();
    if (!tmpFile.delete())
      throw new RuntimeException("Cant delete "+location);
  }

  ////////////////////////////////////////////
  //// use these calls to write data to the file

  /**
   * Write data to the named variable, origin assumed to be 0. Must not be in define mode.
   *
   * @param v variable to write to
   * @param values  write this array; must be same type and rank as Variable
   * @throws IOException if I/O error
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   */
  public void write(Variable v, Array values) throws java.io.IOException, InvalidRangeException {
    write(v, new int[values.getRank()], values);
  }

  /**
   * Write data to the named variable. Must not be in define mode.
   *
   * @param v variable to write to
   * @param origin  offset within the variable to start writing.
   * @param values  write this array; must be same type and rank as Variable
   * @throws IOException if I/O error
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   */
  public void write(Variable v, int[] origin, Array values) throws java.io.IOException, InvalidRangeException {
    if (defineMode)
      throw new UnsupportedOperationException("in define mode");

    spiw.writeData(v, new Section(origin, values.getShape()), values);
    v.invalidateCache();
  }

  /**
   * Write String data to a CHAR variable, origin assumed to be 0. Must not be in define mode.
   *
   * @param v variable to write to
   * @param values  write this array; must be ArrayObject of String
   * @throws IOException if I/O error
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   */
  public void writeStringData(Variable v, Array values) throws java.io.IOException, InvalidRangeException {
    writeStringData(v, new int[values.getRank()], values);
  }

  /**
   * Write String data to a CHAR variable. Must not be in define mode.
   *
   * @param v variable to write to
   * @param origin  offset to start writing, ignore the strlen dimension.
   * @param values  write this array; must be ArrayObject of String
   * @throws IOException if I/O error
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   */
  public void writeStringData(Variable v, int[] origin, Array values) throws java.io.IOException, InvalidRangeException {

    if (values.getElementType() != String.class)
      throw new IllegalArgumentException("Must be ArrayObject of String ");

    if (v.getDataType() != DataType.CHAR)
      throw new IllegalArgumentException("variable " + v.getFullName() + " is not type CHAR");
    int rank = v.getRank();
    int strlen = v.getShape(rank-1);

    // turn it into an ArrayChar
    ArrayChar cvalues = ArrayChar.makeFromStringArray((ArrayObject) values, strlen);

    int[] corigin = new int[rank];
    System.arraycopy(origin, 0, corigin, 0, rank - 1);

    write(v, corigin, cvalues);
  }

  /**
   * Flush anything written to disk.
   * @throws IOException if I/O error
   */
  public void flush() throws java.io.IOException {
    spiw.flush();
  }

  /**
   * close the file.
   * @throws IOException if I/O error
   */
  public synchronized void close() throws java.io.IOException {
    if (spiw != null) {
      flush();
      spiw.close();
      spiw = null;
    }
  }

}
