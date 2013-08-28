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

import ucar.ma2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.IOServiceProviderWriter;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.nc2.iosp.netcdf3.N3header;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.iosp.netcdf3.N3raf;
import ucar.nc2.write.Nc4Chunking;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Writes Netcdf 3 or 4 formatted files to disk.
 * Replaces NetcdfFileWriteable.
 * Fairly low level wrap of IOServiceProviderWriter.
 * Construct CDM objects, then create the file and write data to it.
 *
 * @author caron
 * @since 7/25/12
 */
public class NetcdfFileWriter {

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFileWriter.class);
  static private Set<DataType> valid = EnumSet.of(DataType.BYTE, DataType.CHAR, DataType.SHORT, DataType.INT,
          DataType.DOUBLE, DataType.FLOAT);


  /**
   * The kinds of netcdf file that can be written.
   */
  public enum Version {
    netcdf3(".nc"),              // java iosp
    netcdf4(".nc4"),             // jni netcdf4 iosp mode = NC_FORMAT_NETCDF4
    netcdf4_classic(".nc4"),     // jni netcdf4 iosp mode = NC_FORMAT_NETCDF4_CLASSIC
    netcdf3c(".nc"),             // jni netcdf4 iosp mode = NC_FORMAT_CLASSIC   (nc3)
    netcdf3c64(".nc"),           // jni netcdf4 iosp mode = NC_FORMAT_64BIT     (nc3 64 bit)
    ncstream(".ncs");            // ncstream iosp

    private String suffix;

    private Version(String suffix) {
      this.suffix = suffix;
    }

    public boolean isNetdf4format() {
      return this == netcdf4 || this == netcdf4_classic;
    }

    public boolean useJniIosp() {
      return this != netcdf3;
    }

    public String getSuffix() {
      return suffix;
    }
  }

  /**
   * Open an existing Netcdf file for writing data. Fill mode is true.
   * Cannot add new objects, you can only read/write data to existing Variables.
   *
   * @param location name of existing file to open.
   * @return existing file that can be written to
   * @throws java.io.IOException on I/O error
   */
  static public NetcdfFileWriter openExisting(String location) throws IOException {
    return new NetcdfFileWriter(null, null, null, location, true, null); // dont know the version yet
  }

  static public NetcdfFileWriter createNew(Version version, String location) throws IOException {
    return new NetcdfFileWriter(version, null, null, location, false, null);
  }

  /**
   * Create a new Netcdf file, with fill mode true.
   *
   * @param version  netcdf-3 or 4
   * @param location name of new file to open; if it exists, will overwrite it.
   * @param chunker  used only for netcdf4, or null for default
   * @return new NetcdfFileWriter
   * @throws IOException on I/O error
   */
  static public NetcdfFileWriter createNew(Version version, String location, Nc4Chunking chunker) throws IOException {
    return new NetcdfFileWriter(version, null, null, location, false, chunker);
  }

  ////////////////////////////////////////////////////////////////////////////////
  private final String location;
  private IOServiceProviderWriter spiw;

  // modes
  private boolean defineMode;

  // state
  private NetcdfFile ncfile;
  private Version version;
  private boolean isNewFile;
  private boolean isLargeFile;
  private boolean fill;
  private int extraHeader;
  private long preallocateSize;

  /**
   * Open an existing or create a new Netcdf file
   *
   * @param version which kind of file to write, if null, use netcdf3 (isExisting= false) else open file and figure out the version
   * @param iospw      IO service provider to use, if null use standard defined by version
   * @param raf        Random access file to use, may be null if iospw is, otherwise must be opened read/write
   * @param location   open a new file at this location
   * @param isExisting true if file already exists
   * @param chunker    used only for netcdf4, or null for default
   * @throws IOException on I/O error
   */
  protected NetcdfFileWriter(Version version, IOServiceProviderWriter iospw, ucar.unidata.io.RandomAccessFile raf,
                             String location, boolean isExisting, Nc4Chunking chunker) throws IOException {

    if (isExisting) {
      if (raf == null)
        raf = new ucar.unidata.io.RandomAccessFile(location, "rw");

      if (H5header.isValidFile(raf)) {
        if (version != null && !version.isNetdf4format())
          throw new IllegalArgumentException(location + " must be netcdf-4 file");
        else version = Version.netcdf4;

      } else if (N3header.isValidFile(raf)) {
        if (version != null && version.isNetdf4format())
          throw new IllegalArgumentException(location + " must be netcdf-3 file");
        else version = Version.netcdf3;

      } else {
        throw new IllegalArgumentException(location + " must be netcdf-3 or netcdf-4 file");
      }

    } else {
      if (version == null) version = Version.netcdf3;
      isNewFile = true;
    }

    this.version = version;
    this.location = location;

    if (iospw == null) {
      if (version.useJniIosp()) {
        IOServiceProviderWriter spi = null;
        try {
          //  Nc4Iosp.setLibraryAndPath(path, name);
          Class iospClass = this.getClass().getClassLoader().loadClass("ucar.nc2.jni.netcdf.Nc4Iosp");
          Constructor ctor = iospClass.getConstructor(Version.class);
          spi = (IOServiceProviderWriter) ctor.newInstance(version);

          Method method = iospClass.getMethod("setChunker", Nc4Chunking.class);
          method.invoke(spi, chunker);

        } catch (Throwable e) {
          throw new IllegalArgumentException("ucar.nc2.jni.netcdf.Nc4Iosp is not on classpath, cannont use version " + version);
        }
        spiw = spi;
      } else {
        spiw = new N3raf();
      }

    } else {
      spiw = iospw;
    }

    this.ncfile = new NetcdfFile(spiw, location);  // package private
    if (isExisting)
      spiw.openForWriting(raf, ncfile, null);
    else
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
    spiw.setFill(fill);
  }

  /**
   * Preallocate the file size, for efficiency. Only used by netcdf-3.
   * Must be in define mode
   * Must call before create() to have any affect.
   *
   * @param size if set to > 0, set length of file to this upon creation - this (usually) pre-allocates contiguous storage.
   */
  public void setLength(long size) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    this.preallocateSize = size;
  }

  /**
   * Set if this should be a "large file" (64-bit offset) format. Only used by netcdf-3.
   * Must be in define mode
   *
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
   *
   * @param extraHeaderBytes # bytes extra for the header
   */
  public void setExtraHeaderBytes(int extraHeaderBytes) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    this.extraHeader = extraHeaderBytes;
  }

  /**
   * Is the file in define mode, which allows objects to be added and changed?
   *
   * @return true if the file in define mode
   */
  public boolean isDefineMode() {
    return defineMode;
  }

  public NetcdfFile getNetcdfFile() {
    return ncfile;
  }

  public Version getVersion() {
    return version;
  }

  public Variable findVariable(String fullNameEscaped) {
    return ncfile.findVariable(fullNameEscaped);
  }

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

  public Dimension addUnlimitedDimension(String dimName) {
    return addDimension(null, dimName, 0, true, true, false);
  }

  /**
   * Add a Dimension to the file. Must be in define mode.
   *
   * @param dimName          name of dimension
   * @param length           size of dimension.
   * @param isShared         if dimension is shared   LOOK what does it mean if false ??
   * @param isUnlimited      if dimension is unlimited
   * @param isVariableLength if dimension is variable length
   * @return the created dimension
   */
  public Dimension addDimension(Group g, String dimName, int length, boolean isShared, boolean isUnlimited, boolean isVariableLength) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    if (!isValidObjectName(dimName))
      throw new IllegalArgumentException("illegal dimension name " + dimName);

    Dimension dim = new Dimension(dimName, length, isShared, isUnlimited, isVariableLength);
    ncfile.addDimension(g, dim);
    return dim;
  }

  private String makeValidObjectName(String name) {
    if (!isValidObjectName(name)) {
      String nname = createValidObjectName(name);
      log.warn("illegal object name= " + name + " change to " + name);
      return nname;
    }
    return name;
  }

  private boolean isValidObjectName(String name) {
    return N3iosp.isValidNetcdfObjectName(name);
  }

  private boolean isValidDataType(DataType dt) {
    return version.isNetdf4format() || valid.contains(dt);
  }

  private String createValidObjectName(String name) {
    return N3iosp.makeValidNetcdfObjectName(name);
  }

  /**
   * Rename a Dimension. Must be in define mode.
   *
   * @param oldName existing dimension has this name
   * @param newName rename to this
   * @return renamed dimension, or null if not found
   */
  public Dimension renameDimension(Group g, String oldName, String newName) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    if (!isValidObjectName(newName)) throw new IllegalArgumentException("illegal dimension name " + newName);

    if (g == null) g = ncfile.getRootGroup();
    Dimension dim = g.findDimension(oldName);
    if (null != dim) dim.setName(newName);
    return dim;
  }

  /**
   * Add a Group to the file. Must be in define mode.
   * If pass in null as the parent then the root group is returned and the name is ignored.
   * This is how you get the root group. Note this is different from other uses of parent group.
   *
   * @param parent the parent of this group, if null then returns the root group.
   * @param name   the name of this group, unique within parent
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
   *
   * @param g   the group to add to. if null, use root group
   * @param att the attribute.
   * @return the created attribute
   */
  public Attribute addGroupAttribute(Group g, Attribute att) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");

    if (!isValidObjectName(att.getShortName())) {
      String attName = createValidObjectName(att.getShortName());
      log.warn("illegal attribute name= " + att.getShortName() + " change to " + attName);
      att = new Attribute(attName, att.getValues());
    }

    return ncfile.addAttribute(g, att);
  }

  /**
   * Add a EnumTypedef to the file. Must be in define mode.
   *
   * @param g  the group to add to. if null, use root group
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
   *
   * @param g       the group to add to. if null, use root group
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
   *
   * @param g       the group to add to. if null, use root group
   * @param oldName existing Attribute has this name
   * @param newName rename to this
   * @return renamed Attribute, or null if not found
   */
  public Attribute renameGlobalAttribute(Group g, String oldName, String newName) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");

    if (!isValidObjectName(newName)) {
      String newnewName = createValidObjectName(newName);
      log.warn("illegal attribute name= " + newName + " change to " + newnewName);
      newName = newnewName;
    }

    if (g == null) g = ncfile.getRootGroup();
    Attribute att = g.findAttribute(oldName);
    if (null == att) return null;

    g.remove(att);
    att = new Attribute(newName, att.getValues());
    g.addAttribute(att);
    return att;
  }

  /**
   * Add a variable to the file. Must be in define mode.
   *
   * @param g         the group to add to. if null, use root group
   * @param shortName name of Variable, must be unique with the file.
   * @param dataType  type of underlying element
   * @param dims      names of Dimensions for the variable, blank seperated.
   *                  Must already have been added. Use an empty string for a scalar variable.
   * @return the Variable that has been added
   */
  public Variable addVariable(Group g, String shortName, DataType dataType, String dims) {
    if (g == null) g = ncfile.getRootGroup();
    // parse the list
    ArrayList<Dimension> list = new ArrayList<Dimension>();
    StringTokenizer stoker = new StringTokenizer(dims);
    while (stoker.hasMoreTokens()) {
      String tok = stoker.nextToken();
      Dimension d = g.findDimension(tok);
      if (null == d) {
        g.findDimension(tok); // debug
        throw new IllegalArgumentException("Cant find dimension " + tok);
      }
      list.add(d);
    }

    return addVariable(g, shortName, dataType, list);
  }

  /**
   * Add a variable to the file. Must be in define mode.
   *
   * @param g         add to this group in the new file
   * @param shortName name of Variable, must be unique with the file.
   * @param dataType  type of underlying element
   * @param dims      list of Dimensions for the variable in the new file, must already have been added.
   *                  Use a list of length 0 for a scalar variable.
   * @return the Variable that has been added
   */
  public Variable addVariable(Group g, String shortName, DataType dataType, List<Dimension> dims) {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");

    shortName = makeValidObjectName(shortName);
    if (!isValidDataType(dataType))
      throw new IllegalArgumentException("illegal dataType: " + dataType + " not supported in netcdf-3");

    // check unlimited if netcdf-3
    if (!version.isNetdf4format()) {
      for (int i = 0; i < dims.size(); i++) {
        Dimension d = dims.get(i);
        if (d.isUnlimited() && (i != 0))
          throw new IllegalArgumentException("Unlimited dimension " + d.getShortName() + " must be first (outermost) in netcdf-3 ");
      }
    }

    Variable v = null;
    if (dataType == DataType.STRUCTURE) {
      v = new Structure(ncfile, g, null, shortName);
    }  else {
      v = new Variable(ncfile, g, null, shortName);
      v.setDataType(dataType);
    }

    v.setDimensions(dims);

    long size = v.getSize() * v.getElementSize();
    if (!version.isNetdf4format() && size > N3iosp.MAX_VARSIZE)
      throw new IllegalArgumentException("Variable size in bytes " + size + " may not exceed " + N3iosp.MAX_VARSIZE);

    ncfile.addVariable(g, v);
    return v;
  }

  /**
   * Add a variable with DataType = String to a netCDF-3 file. Must be in define mode.
   * The variable will be stored in the file as a CHAR variable.
   * A new dimension with name "stringVar.getShortName()_strlen" is automatically
   * added, with length max_strlen, as determined from the data contained in the
   * stringVar.
   *
   * @param g         add to this group in the new file
   * @param stringVar string variable.
   * @param dims      list of Dimensions for the string variable.
   * @return the CHAR variable generated from stringVar
   */
  public Variable addStringVariable(Group g, Variable stringVar, List<Dimension> dims) {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");

    if (!N3iosp.isValidNetcdfObjectName(stringVar.getShortName()))
      throw new IllegalArgumentException("illegal netCDF-3 variable name: " + stringVar.getShortName());

    // convert STRING to CHAR
    int max_strlen = 0;
    Array data = null;

    try {
      data = stringVar.read();
      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext()) {
        String s = (String) ii.getObjectNext();
        max_strlen = Math.max(max_strlen, s.length());
      }
    } catch (IOException e) {
      e.printStackTrace();
      String err = "No data found for Variable " + stringVar.getShortName() +
              ". Cannot determine the lentgh of the new CHAR variable.";
      log.error(err);
      System.out.println(err);
    }

    return addStringVariable(g, stringVar.getShortName(), dims, max_strlen);
  }

  /**
   * Add a variable with DataType = String to the file. Must be in define mode.
   * The variable will be stored in the file as a CHAR variable.
   * A new dimension with name "varName_strlen" is automatically added, with length max_strlen.
   *
   * @param shortName  name of Variable, must be unique within the file.
   * @param dims       list of Dimensions for the variable, must already have been added. Use a list of length 0
   *                   for a scalar variable. Do not include the string length dimension.
   * @param max_strlen maximum string length.
   * @return the Variable that has been added
   */
  public Variable addStringVariable(Group g, String shortName, List<Dimension> dims, int max_strlen) {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");
    shortName = makeValidObjectName(shortName);

    Variable v = new Variable(ncfile, g, null, shortName);
    v.setDataType(DataType.CHAR);

    Dimension d = addDimension(g, shortName + "_strlen", max_strlen);
    ArrayList<Dimension> sdims = new ArrayList<Dimension>(dims);
    sdims.add(d);
    v.setDimensions(sdims);

    ncfile.addVariable(g, v);
    return v;
  }

  /**
   * Rename a Variable. Must be in define mode.
   *
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
   * @param v   Variable to add attribute to
   * @param att Attribute to add.
   * @return true if attribute was added, false if not allowed by CDM.
   */
  public boolean addVariableAttribute(Variable v, Attribute att) {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");

    if (!isValidObjectName(att.getShortName())) {
      String attName = createValidObjectName(att.getShortName());
      log.warn("illegal netCDF-3 attribute name= " + att.getShortName() + " change to " + attName);
      att = new Attribute(attName, att.getValues());
    }

    // these are not allowed in the file - they are added when read
    if (att.getShortName().equals(CDM.CHUNK_SIZE)) return false;
    if (att.getShortName().equals(CDM.COMPRESS)) return false;

    v.addAttribute(att);
    return true;
  }

  /**
   * Delete a variable Attribute. Must be in define mode.
   *
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
   *
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
    att = new Attribute(newName, att.getValues());
    v.addAttribute(att);
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
   * call create() to actually create the file. You must be in define mode.
   * After this call, you are no longer in define mode.
   *
   * @throws java.io.IOException if I/O error
   */
  public void create() throws java.io.IOException {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");
    if (!isNewFile)
      throw new UnsupportedOperationException("can only call create on a new file");

    ncfile.finish(); // ??
    spiw.setFill(fill); // ??
    spiw.create(location, ncfile, extraHeader, preallocateSize, isLargeFile);

    defineMode = false;
  }

  ////////////////////////////////////////////
  // redefine

  /**
   * Set the redefine mode.
   * Designed to emulate nc_redef (redefineMode = true) and
   * nc_enddef (redefineMode = false)
   *
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
      boolean ok = spiw.rewriteHeader(isLargeFile);  // LOOK seems like we should be using isNewFile
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
    File tmpFile = new File(location + ".tmp");
    if (tmpFile.exists()) tmpFile.delete();
    if (!prevFile.renameTo(tmpFile)) {
      System.out.println(prevFile.getPath() + " prevFile.exists " + prevFile.exists() + " canRead = " + prevFile.canRead());
      System.out.println(tmpFile.getPath() + " tmpFile.exists " + tmpFile.exists() + " canWrite " + tmpFile.canWrite());
      throw new RuntimeException("Cant rename " + prevFile.getAbsolutePath() + " to " + tmpFile.getAbsolutePath());
    }

    NetcdfFile oldFile = NetcdfFile.open(tmpFile.getPath());

    /* use record dimension if it has one
    Structure recordVar = null;
    if (oldFile.hasUnlimitedDimension()) {
      oldFile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      recordVar = (Structure) oldFile.findVariable("record");
      /* if (recordVar != null) {
        Boolean result = (Boolean) spiw.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
        if (!result)
          recordVar = null;
      }
      } */

    // create new file with current set of objects
    spiw.create(location, ncfile, extraHeader, preallocateSize, isLargeFile);
    spiw.setFill(fill);
    //isClosed = false;

    /* wait till header is written before adding the record variable to the file
    if (recordVar != null) {
      Boolean result = (Boolean) spiw.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      if (!result)
        recordVar = null;
    } */

    FileWriter2 fileWriter2 = new FileWriter2(this);

    for (Variable v : ncfile.getVariables()) {
      Variable oldVar = oldFile.findVariable(v.getFullNameEscaped());
      if (oldVar != null) {
        fileWriter2.copyAll(oldVar, v);
    }
    }

    // delete old
    oldFile.close();
    if (!tmpFile.delete())
      throw new RuntimeException("Cant delete "+tmpFile.getAbsolutePath());
  }

  public Structure addRecordStructure() {
    if (version.isNetdf4format()) return null;
    boolean ok = (Boolean) ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    if (!ok)
      throw new IllegalStateException("can't add record variable");
    return (Structure) ncfile.findVariable("record");
  }

  ////////////////////////////////////////////
  //// use these calls to write data to the file

  /**
   * Write data to the named variable, origin assumed to be 0. Must not be in define mode.
   *
   * @param v      variable to write to
   * @param values write this array; must be same type and rank as Variable
   * @throws IOException                    if I/O error
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   */
  public void write(Variable v, Array values) throws java.io.IOException, InvalidRangeException {
    if (ncfile != v.getNetcdfFile())
      throw new IllegalArgumentException("Variable is not owned by this writer.");

    write(v, new int[values.getRank()], values);
  }

  /**
   * Write data to the named variable. Must not be in define mode.
   *
   * @param v      variable to write to
   * @param origin offset within the variable to start writing.
   * @param values write this array; must be same type and rank as Variable
   * @throws IOException                    if I/O error
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
   * @param v      variable to write to
   * @param values write this array; must be ArrayObject of String
   * @throws IOException                    if I/O error
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   */
  public void writeStringData(Variable v, Array values) throws java.io.IOException, InvalidRangeException {
    writeStringData(v, new int[values.getRank()], values);
  }

  /**
   * Write String data to a CHAR variable. Must not be in define mode.
   *
   * @param v      variable to write to
   * @param origin offset to start writing, ignore the strlen dimension.
   * @param values write this array; must be ArrayObject of String
   * @throws IOException                    if I/O error
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   */
  public void writeStringData(Variable v, int[] origin, Array values) throws java.io.IOException, InvalidRangeException {

    if (values.getElementType() != String.class)
      throw new IllegalArgumentException("Must be ArrayObject of String ");

    if (v.getDataType() != DataType.CHAR)
      throw new IllegalArgumentException("variable " + v.getFullName() + " is not type CHAR");
    int rank = v.getRank();
    int strlen = v.getShape(rank - 1);

    // turn it into an ArrayChar
    ArrayChar cvalues = ArrayChar.makeFromStringArray((ArrayObject) values, strlen);

    int[] corigin = new int[rank];
    System.arraycopy(origin, 0, corigin, 0, rank - 1);

    write(v, corigin, cvalues);
  }

  /**
   * Flush anything written to disk.
   *
   * @throws IOException if I/O error
   */
  public void flush() throws java.io.IOException {
    spiw.flush();
  }

  /**
   * close the file.
   *
   * @throws IOException if I/O error
   */
  public synchronized void close() throws java.io.IOException {
    if (spiw != null) {
      flush();
      spiw.close();
      spiw = null;
    }
  }

  public void abort() throws java.io.IOException {
    if (spiw != null) {
      spiw.close();
      spiw = null;
    }
  }

}
