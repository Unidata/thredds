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

import ucar.ma2.*;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.util.CancelTask;

import java.util.*;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * A Variable is a logical container for data. It has a dataType, a set of Dimensions that define its array shape,
 * and optionally a set of Attributes.
 * <p/>
 * The data is a multidimensional array of primitive types, Strings, or Structures.
 * Data access is done through the read() methods, which return a memory resident Array.
 * <p> Immutable if setImmutable() was called.
 * @author caron
 * @see ucar.ma2.Array
 * @see ucar.ma2.DataType
 */

public class Variable implements VariableIF, ProxyReader {
  /** cache any variable whose size() < defaultSizeToCache */
  static public final int defaultSizeToCache = 4000; // bytes
  static protected boolean debugCaching = false;
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Variable.class);

  protected NetcdfFile ncfile; // physical container for this Variable; where the I/O happens. may be null if Variable is self contained.
  protected Group group; // logical container for this Variable. may not be null.
  protected String shortName; // may not be blank
  protected int[] shape;
  protected Section shapeAsSection;  // derived from the shape, immutable; used for every read, deferred creation

  protected DataType dataType;
  protected int elementSize;
  protected List<Dimension> dimensions = new ArrayList<Dimension>(5);
  protected List<Attribute> attributes = new ArrayList<Attribute>();

  protected boolean isVariableLength = false;
  protected boolean isMetadata = false;
  private boolean immutable = false; // cache can change

  protected Cache cache = new Cache();
  protected int sizeToCache = defaultSizeToCache; // bytes

  protected Structure parent = null; // for variables inside a Structure, aka "structure members"
  protected ProxyReader proxyReader = this;

  /**
   * Get the full name of this Variable, starting from rootGroup. The name is unique within the
   * entire NetcdfFile.
   */
  public String getName() {
    return NetcdfFile.makeFullName(this.group, this);
  }

  /**
   * Get the full name of this Variable, with special characters escaped.
   * @return the full name of this Variable, with special characters escaped.
   */
  public String getNameEscaped() {
    return NetcdfFile.makeFullNameEscaped(this.group, this);
  }

  /**
   * Get the short name of this Variable. The name is unique within its parent group.
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * Get the data type of the Variable.
   */
  public DataType getDataType() {
    return dataType;
  }

  /**
   * Get the shape: length of Variable in each dimension.
   *
   * @return int array whose length is the rank of this Variable
   *         and whose values equal the length of that Dimension.
   */
  public int[] getShape() {
    int[] result = new int[shape.length];  // optimization over clone()
    System.arraycopy(shape, 0, result, 0, shape.length);
    return result;
  }

  /**
   * Get the size of the ith dimension
   *
   * @param index which dimension
   * @return size of the ith dimension
   */
  public int getShape(int index) {
    return shape[index];
  }

  /**
   * Get the total number of elements in the Variable.
   * If this is an unlimited Variable, will use the current number of elements.
   * If this is a Sequence, will return 1.
   * If variable length, will skip vlen dimensions
   *
   * @return total number of elements in the Variable.
   */
  public long getSize() {
    long size = 1;
    for (int i = 0; i < shape.length; i++) {
      if (shape[i] >= 0)
        size *= shape[i];
    }
    return size;
  }

  /**
   * Get the number of bytes for one element of this Variable.
   * For Variables of primitive type, this is equal to getDataType().getSize().
   * Variables of String type dont know their size, so what they return is undefined.
   * Variables of Structure type return the total number of bytes for all the members of
   * one Structure, plus possibly some extra padding, depending on the underlying format.
   * Variables of Sequence type return the number of bytes of one element.
   *
   * @return total number of bytes for the Variable
   */
  public int getElementSize() {
    return elementSize;
  }

  /**
   * Get the number of dimensions of the Variable.
   * @return the rank
   */
  public int getRank() {
    return shape.length;
  }

  /**
   * Get the containing Group.
   * @return the containing Group.
   */
  public Group getParentGroup() {
    return group;
  }

  /**
   * Is this variable metadata?. True if its values need to be included explicitly in NcML output.
   * @return true if Variable values need to be included in NcML
   */
  public boolean isMetadata() {
    return isMetadata;
  }

  /**
   * Whether this is a scalar Variable (rank == 0).
   * @return true if Variable has rank 0
   */
  public boolean isScalar() {
    return getRank() == 0;
  }

  /**
   * Does this variable have a variable length dimension?
   * If so, it has as one of its dimensions Dimension.VLEN.
   * @return true if Variable has a variable length dimension?
   */
  public boolean isVariableLength() {
    return isVariableLength;
  }

  /**
   * Is this Variable unsigned?. Only meaningful for byte, short, int, long types.
   * Looks for attribute "_Unsigned", case insensitive
   * @return true if Variable is unsigned
   */
  public boolean isUnsigned() {
    Attribute att = findAttributeIgnoreCase("_Unsigned");
    return (att != null) && att.getStringValue().equalsIgnoreCase("true");
  }

  /**
   * Can this variable's size grow?.
   * This is equivalent to saying at least one of its dimensions is unlimited.
   *
   * @return boolean true iff this variable can grow
   */
  public boolean isUnlimited() {
    for (Dimension d : dimensions) {
      if (d.isUnlimited()) return true;
    }
    return false;
  }

  /**
   * Get the list of dimensions used by this variable.
   * The most slowly varying (leftmost for Java and C programmers) dimension is first.
   * For scalar variables, the list is empty.
   *
   * @return List<Dimension>, immutable
   */
  public java.util.List<Dimension> getDimensions() {
    return dimensions;
  }

  /**
   * Get the ith dimension.
   *
   * @param i index of the dimension.
   * @return requested Dimension, or null if i is out of bounds.
   */
  public Dimension getDimension(int i) {
    if ((i < 0) || (i >= getRank())) return null;
    return dimensions.get(i);
  }

  /**
   * Get the list of Dimension names, space delineated.
   * @return Dimension names, space delineated
   */
  /* public String getDimensionsStringOld() {
    StringBuilder buff = new StringBuilder();
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension dim = dimensions.get(i);
      if (i > 0) buff.append(" ");
      buff.append(dim.getName());
    }
    return buff.toString();
  }  */

  public String getDimensionsString() {
    Formatter buf = new Formatter();
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension myd = dimensions.get(i);
      String dimName = myd.getName();

      if (i != 0) buf.format(" ");

      if (myd.isVariableLength()) {
        buf.format("*");
      } else if (myd.isShared()) {
        buf.format("%s", dimName);
      } else {
        //if (dimName != null)          // LOOK losing anon dim name
        //  buf.format("%s=", dimName);
        buf.format("%d", myd.getLength());
      }
    }
    return buf.toString();
  }
  /**
   * Find the index of the named Dimension in this Variable.
   *
   * @param name the name of the dimension
   * @return the index of the named Dimension, or -1 if not found.
   */
  public int findDimensionIndex(String name) {
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension d = dimensions.get(i);
      if (name.equals(d.getName()))
        return i;
    }
    return -1;
  }

  /**
   * Returns the set of attributes for this variable.
   *
   * @return List<Attribute>, not a copy, but may be immutable
   */
  public java.util.List<Attribute> getAttributes() {
    return attributes;
  }

  /**
   * Find an Attribute by name.
   *
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttribute(String name) {
    for (Attribute a : attributes) {
      if (name.equals(a.getName()))
        return a;
    }
    return null;
  }

  /**
   * Find an Attribute by name, ignoring the case.
   *
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttributeIgnoreCase(String name) {
    for (Attribute a : attributes) {
      if (name.equalsIgnoreCase(a.getName()))
        return a;
    }
    return null;
  }

  /**
   * Get the description of the Variable.
   * Default is to use "long_name" attribute value. If not exist, look for "description", "title", or
   * "standard_name" attribute value (in that order).
   *
   * @return description, or null if not found.
   */
  public String getDescription() {
    String desc = null;
    Attribute att = findAttributeIgnoreCase("long_name");
    if ((att != null) && att.isString())
      desc = att.getStringValue();

    if (desc == null) {
      att = findAttributeIgnoreCase("description");
      if ((att != null) && att.isString())
        desc = att.getStringValue();
    }

    if (desc == null) {
      att = findAttributeIgnoreCase("title");
      if ((att != null) && att.isString())
        desc = att.getStringValue();
    }

    if (desc == null) {
      att = findAttributeIgnoreCase("standard_name");
      if ((att != null) && att.isString())
        desc = att.getStringValue();
    }

    return desc;
  }

  /**
   * Get the Unit String for the Variable.
   * Looks for the "units" attribute value
   *
   * @return unit string, or null if not found.
   */
  public String getUnitsString() {
    String units = null;
    Attribute att = findAttributeIgnoreCase("units");
    if ((att != null) && att.isString())
      units = att.getStringValue().trim();
    return units;
  }

  /**
   * Get shape as an List of Range objects.
   * The List is immutable.
   * @return List of Ranges, one for each Dimension.
   */
  public List<Range> getRanges() {
    return getShapeAsSection().getRanges();
  }

  /**
   * Get shape as a Section object.
   * @return Section containing List<Range>, one for each Dimension.
   */
  public Section getShapeAsSection() {
    if (shapeAsSection == null) {
      try {
        List<Range> list = new ArrayList<Range>();
        for (Dimension d : dimensions) {
          int len = d.getLength();
          if (len > 0)
            list.add(new Range(d.getName(), 0, len - 1));
          else if (len == 0)
            list.add( Range.EMPTY); // LOOK empty not named
          else
            list.add( Range.VLEN); // LOOK vlen not named
        }
        shapeAsSection = new Section(list).makeImmutable();
        
      } catch (InvalidRangeException e) {
        log.error("Bad shape in variable " + getName(), e);
        throw new IllegalStateException(e.getMessage());
      }
    }
    return shapeAsSection;
  }

  public ProxyReader getProxyReader() {
    return proxyReader;
  }

  public void setProxyReader(ProxyReader proxyReader) {
    this.proxyReader = proxyReader;
  }


  /** Get the proxy reader, or null.
   * @return return the proxy reader, if any
   *
  public ProxyReader getProxyReader() {
    return this.postReader;
  } */

  /**
   * Create a new Variable that is a logical subsection of this Variable.
   * No data is read until a read method is called on it.
   *
   * @param ranges List of type ucar.ma2.Range, with size equal to getRank().
   *               Each Range corresponds to a Dimension, and specifies the section of data to read in that Dimension.
   *               A Range object may be null, which means use the entire dimension.
   * @return a new Variable which is a logical section of this Variable.
   * @throws InvalidRangeException
   */
  public Variable section(List<Range> ranges) throws InvalidRangeException {
    return section(new Section(ranges, shape).makeImmutable());
  }

  /**
   * Create a new Variable that is a logical subsection of this Variable.
   * No data is read until a read method is called on it.
   *
   * @param subsection Section of this variable.
   *                   Each Range in the section corresponds to a Dimension, and specifies the section of data to read in that Dimension.
   *                   A Range object may be null, which means use the entire dimension.
   * @return a new Variable which is a logical section of this Variable.
   * @throws InvalidRangeException if section not compatible with shape
   */
  public Variable section(Section subsection) throws InvalidRangeException {

    subsection = Section.fill(subsection, shape);

    // create a copy of this variable with a proxy reader
    Variable sectionV = copy(); // subclasses must override
    sectionV.setProxyReader( new SectionReader(this, subsection));
    sectionV.shape = subsection.getShape();
    sectionV.createNewCache(); // dont share the cache
    sectionV.setCaching(false); // dont cache
    
    // replace dimensions if needed !! LOOK not shared
    sectionV.dimensions = new ArrayList<Dimension>();
    for (int i = 0; i < getRank(); i++) {
      Dimension oldD = getDimension(i);
      Dimension newD = (oldD.getLength() == sectionV.shape[i]) ? oldD : new Dimension(oldD.getName(), sectionV.shape[i], false);
      newD.setUnlimited(oldD.isUnlimited());
      sectionV.dimensions.add(newD);
    }
    sectionV.resetShape();
    return sectionV;
  }


  /**
   * Create a new Variable that is a logical slice of this Variable, by
   * fixing the specified dimension at the specified index value. This reduces rank by 1.
   * No data is read until a read method is called on it.
   *
   * @param dim   which dimension to fix
   * @param value at what index value
   * @return a new Variable which is a logical slice of this Variable.
   * @throws InvalidRangeException if dimension or value is illegal
   */
  public Variable slice(int dim, int value) throws InvalidRangeException {
    if ((dim < 0) || (dim >= shape.length))
      throw new InvalidRangeException("Slice dim invalid= " + dim);

    // ok to make slice of record dimension with length 0
    boolean recordSliceOk = false;
    if ((dim == 0) && (value == 0)) {
      Dimension d = getDimension(0);
      recordSliceOk = d.isUnlimited();
    }

    // otherwise check slice in range
    if (!recordSliceOk) {
      if ((value < 0) || (value >= shape[dim]))
        throw new InvalidRangeException("Slice value invalid= " + value + " for dimension " + dim);
    }

    // create a copy of this variable with a proxy reader
    Variable sliceV = copy(); // subclasses must override
    Section slice = new Section( getShapeAsSection());
    slice.replaceRange(dim, new Range(value, value)).makeImmutable();
    sliceV.setProxyReader( new SliceReader(this, dim, slice));
    sliceV.createNewCache(); // dont share the cache
    sliceV.setCaching(false); // dont cache

    // remove that dimension - reduce rank
    sliceV.dimensions.remove(dim);
    sliceV.resetShape();
    return sliceV;
  }

  protected Variable copy() {
    return new Variable(this);
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Lookup the enum string for this value.
   * Can only be called on enum types, where dataType.isEnum() is true.
   * @param val the integer value of this enum
   * @return the String value
   */
  public String lookupEnumString(int val) {
    if (!dataType.isEnum())
      throw new UnsupportedOperationException("Can only call Variable.lookupEnumVal() on enum types");
    return enumTypedef.lookupEnumString(val);
  }
  private EnumTypedef enumTypedef;

  /**
   * Public by accident.
   * @param enumTypedef set the EnumTypedef, only use if getDataType.isEnum()
   */
  public void setEnumTypedef(EnumTypedef enumTypedef) {
    if (immutable) throw new IllegalStateException("Cant modify");
    if (!dataType.isEnum())
      throw new UnsupportedOperationException("Can only call Variable.setEnumTypedef() on enum types");
    this.enumTypedef = enumTypedef;
  }

  // public EnumTypedef getEnumTypedef() { return enumTypedef; }

  //////////////////////////////////////////////////////////////////////////////
  // IO
  // implementation notes to subclassers
  // all other calls use them, so override only these:
  //   _read()
  //   _read(Section section)
  //   _readNestedData(Section section, boolean flatten)

  /**
   * Read a section of the data for this Variable and return a memory resident Array.
   * The Array has the same element type as the Variable, and the requested shape.
   * Note that this does not do rank reduction, so the returned Array has the same rank
   * as the Variable. Use Array.reduce() for rank reduction.
   * <p/>
   * <code>assert(origin[ii] + shape[ii]*stride[ii] <= Variable.shape[ii]); </code>
   * <p/>
   *
   * @param origin int array specifying the starting index. If null, assume all zeroes.
   * @param shape  int array specifying the extents in each dimension.
   *               This becomes the shape of the returned Array.
   * @return the requested data in a memory-resident Array
   */
  public Array read(int[] origin, int[] shape) throws IOException, InvalidRangeException {
    if ((origin == null) && (shape == null))
      return read();

    if (origin == null)
      return read(new Section(shape));

    if (shape == null) // LOOK not very useful, origin must be 0 to be valid
      return read(new Section(origin, shape));

    return read(new Section(origin, shape));
  }

  /**
   * Read data section specified by a "section selector", and return a memory resident Array. Uses
   * Fortran 90 array section syntax.
   *
   * @param sectionSpec specification string, eg "1:2,10,:,1:100:10". May optionally have ().
   * @return the requested data in a memory-resident Array
   * @see ucar.ma2.Section for sectionSpec syntax
   */
  public Array read(String sectionSpec) throws IOException, InvalidRangeException {
    return read(new Section(sectionSpec));
  }

  /**
   * Read a section of the data for this Variable from the netcdf file and return a memory resident Array.
   *
   * @param ranges list of Range specifying the section of data to read.
   * @return the requested data in a memory-resident Array
   * @see #read(Section)
   * @throws IOException if error
   * @throws InvalidRangeException if ranges is invalid
   */
  public Array read(List<Range> ranges) throws IOException, InvalidRangeException {
    if (null == ranges)
      return _read();

    return read(new Section(ranges));
  }

  /**
   * Read a section of the data for this Variable from the netcdf file and return a memory resident Array.
   * The Array has the same element type as the Variable, and the requested shape.
   * Note that this does not do rank reduction, so the returned Array has the same rank
   * as the Variable. Use Array.reduce() for rank reduction.
   * <p/>
   * If the Variable is a member of an array of Structures, this returns only the variable's data
   * in the first Structure, so that the Array shape is the same as the Variable.
   * To read the data in all structures, use readAllStructures().
   * <p/>
   * Note this only allows you to specify a subset of this variable.
   * If the variable is nested in a array of structures and you want to subset that, use
   * NetcdfFile.read(String sectionSpec, boolean flatten);
   *
   * @param section list of Range specifying the section of data to read.
   *                Must be null or same rank as variable.
   *                If list is null, assume all data.
   *                Each Range corresponds to a Dimension. If the Range object is null, it means use the entire dimension.
   * @return the requested data in a memory-resident Array
   * @throws IOException if error
   * @throws InvalidRangeException if section is invalid
   */
  public Array read(ucar.ma2.Section section) throws java.io.IOException, ucar.ma2.InvalidRangeException {
    return (section == null) ? _read() : _read(Section.fill(section, shape));
  }

  /**
   * Read all the data for this Variable and return a memory resident Array.
   * The Array has the same element type and shape as the Variable.
   * <p/>
   * If the Variable is a member of an array of Structures, this returns only the variable's data
   * in the first Structure, so that the Array shape is the same as the Variable.
   * To read the data in all structures, use readAllStructures().
   *
   * @return the requested data in a memory-resident Array.
   */
  public Array read() throws IOException {
    return _read();
  }

  /**
   * *********************************************************************
   */
  // scalar reading

  /**
   * Get the value as a byte for a scalar Variable. May also be one-dimensional of length 1.
   *
   * @throws IOException                   if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException  if data type not convertible to byte
   */
  public byte readScalarByte() throws IOException {
    Array data = getScalarData();
    return data.getByte(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a short for a scalar Variable.  May also be one-dimensional of length 1.
   *
   * @throws IOException                   if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable  or one-dimensional of length 1.
   * @throws ForbiddenConversionException  if data type not convertible to short
   */
  public short readScalarShort() throws IOException {
    Array data = getScalarData();
    return data.getShort(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a int for a scalar Variable. May also be one-dimensional of length 1.
   *
   * @throws IOException                   if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException  if data type not convertible to int
   */
  public int readScalarInt() throws IOException {
    Array data = getScalarData();
    return data.getInt(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a long for a scalar Variable.  May also be one-dimensional of length 1.
   *
   * @throws IOException                   if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable
   * @throws ForbiddenConversionException  if data type not convertible to long
   */
  public long readScalarLong() throws IOException {
    Array data = getScalarData();
    return data.getLong(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a float for a scalar Variable.  May also be one-dimensional of length 1.
   *
   * @throws IOException                   if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException  if data type not convertible to float
   */
  public float readScalarFloat() throws IOException {
    Array data = getScalarData();
    return data.getFloat(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a double for a scalar Variable.  May also be one-dimensional of length 1.
   *
   * @throws IOException                   if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException  if data type not convertible to double
   */
  public double readScalarDouble() throws IOException {
    Array data = getScalarData();
    return data.getDouble(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a String for a scalar Variable.  May also be one-dimensional of length 1.
   * May also be one-dimensional of type CHAR, which wil be turned into a scalar String.
   *
   * @throws IOException                   if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar or one-dimensional.
   * @throws ClassCastException            if data type not DataType.STRING or DataType.CHAR.
   */
  public String readScalarString() throws IOException {
    Array data = getScalarData();
    if (dataType == DataType.STRING)
      return (String) data.getObject(Index.scalarIndexImmutable);
    else if (dataType == DataType.CHAR) {
      ArrayChar dataC = (ArrayChar) data;
      return dataC.getString();
    } else
      throw new IllegalArgumentException("readScalarString not STRING or CHAR " + getName());
  }

  protected Array getScalarData() throws IOException {
    Array scalarData = (cache != null && cache.data != null) ? cache.data : read();
    scalarData = scalarData.reduce();

    if ((scalarData.getRank() == 0) || ((scalarData.getRank() == 1) && dataType == DataType.CHAR))
      return scalarData;
    throw new java.lang.UnsupportedOperationException("not a scalar variable =" + this);
  }

  ///////////////
  // internal reads: all other calls go through these.
  // subclasses must override, so that NetcdfDataset wrapping will work.

  // non-structure-member Variables.

  protected Array _read() throws IOException {
    // caching overrides the proxyReader
    // check if already cached
    if (cache != null && cache.data != null) {
      if (debugCaching) System.out.println("got data from cache " + getName());
      return cache.data.copy();
    }

    Array data = proxyReader.reallyRead(this, null);

    // optionally cache it
    if (isCaching()) {
      setCachedData(data);
      if (debugCaching) System.out.println("cache " + getName());
      return cache.data.copy(); // dont let users get their nasty hands on cached data
    } else {
      return data;
    }
  }

 /**
   * public by accident, do not call directly.
   * @return Array
   * @throws IOException on error
   */
  @Override
  public Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
   if (isMemberOfStructure()) { // LOOK should be UnsupportedOperationException ??
     List<String> memList = new ArrayList<String>();
     memList.add(this.getShortName());
     Structure s = parent.select(memList);
     ArrayStructure as = (ArrayStructure) s.read();
     return as.extractMemberArray( as.findMember( shortName));
   }

    try {
      return ncfile.readData(this, getShapeAsSection());
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage()); // cant happen haha
    }
  }

  // section of non-structure-member Variable
  // assume filled, validated Section
  protected Array _read(Section section) throws IOException, InvalidRangeException {
    // check if its really a full read
    if ((null == section) || section.computeSize() == getSize())
      return _read();

    // full read was cached
    if (isCaching()) {
      if (cache.data == null) {
        setCachedData(  _read()); // read and cache entire array
        if (debugCaching) System.out.println("cache " + getName());
      }
      if (debugCaching) System.out.println("got data from cache " + getName());
      return cache.data.sectionNoReduce(section.getRanges()).copy(); // subset it, return copy
    }

    return proxyReader.reallyRead(this, section, null);
  }

   /**
   * public by accident, do not call directly.
   * @return Array
   * @throws IOException on error
   */
  @Override
  public Array reallyRead(Variable client, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    if (isMemberOfStructure()) {
      throw new UnsupportedOperationException("Cannot directly read section of Member Variable="+getName());
    }
    // read just this section
    return ncfile.readData(this, section);
  }

  /* structure-member Variable;  section has a Range for each array in the parent
  // stuctures(s) and for the Variable.
  private Array _readMemberData(List<Range> section, boolean flatten) throws IOException, InvalidRangeException {
    /*Variable useVar = (ioVar != null) ? ioVar : this;
    NetcdfFile useFile = (ncfileIO != null) ? ncfileIO : ncfile;
    return useFile.readMemberData(useVar, section, flatten);
  } */

  public long readToByteChannel(Section section, WritableByteChannel wbc) throws IOException, InvalidRangeException {
    if ((ncfile == null) || hasCachedData())
      return IospHelper.copyToByteChannel( read(section), wbc);
    
    return ncfile.readToByteChannel(this, section, wbc);
  }

  /*******************************************/
  /* nicely formatted string representation */

  /**
   * Get the display name plus the dimensions, eg 'float name(dim1, dim2)'
   * @return display name plus the dimensions
   */
  public String getNameAndDimensions() {
    Formatter buf = new Formatter();
    getNameAndDimensions(buf, true, false);
    return buf.toString();
  }

  /**
   * Get the display name plus the dimensions, eg 'float name(dim1, dim2)'
   * @param strict strictly comply with ncgen syntax, with name escaping. otherwise, get extra info, no escaping
   * @return display name plus the dimensions
   */
  public String getNameAndDimensions(boolean strict) {
    Formatter buf = new Formatter();
    getNameAndDimensions(buf, false, strict);
    return buf.toString();
  }

  /**
   * Get the display name plus the dimensions, eg 'name(dim1, dim2)'
   * @param buf add info to this StringBuilder
   */
  public void getNameAndDimensions(StringBuilder buf) {
    getNameAndDimensions(buf, true, false);
  }

  /**
   * Get the display name plus the dimensions, eg 'name(dim1, dim2)'
   * @param buf add info to this StringBuffer
   * @deprecated use getNameAndDimensions(StringBuilder buf)
   */
  public void getNameAndDimensions(StringBuffer buf) {
    Formatter proxy = new Formatter();
    getNameAndDimensions(proxy, true, false);
    buf.append(proxy.toString());
  }

  /**
   * Add display name plus the dimensions to the StringBuffer
   * @param buf add info to this
   * @param useFullName use full name else short name. strict = true implies short name
   * @param strict strictly comply with ncgen syntax, with name escaping. otherwise, get extra info, no escaping
   */
  public void getNameAndDimensions(StringBuilder buf, boolean useFullName, boolean strict) {
    Formatter proxy = new Formatter();
    getNameAndDimensions(proxy, useFullName, strict);
    buf.append(proxy.toString());
  }


  /**
   * Add display name plus the dimensions to the StringBuffer
   * @param buf add info to this
   * @param useFullName use full name else short name. strict = true implies short name
   * @param strict strictly comply with ncgen syntax, with name escaping. otherwise, get extra info, no escaping
   */
  public void getNameAndDimensions(Formatter buf, boolean useFullName, boolean strict) {
    useFullName = useFullName && !strict;
    String name = useFullName ? getName() : getShortName();
    if (strict) name = NetcdfFile.escapeName( name);
    buf.format("%s", name);

    if (getRank() > 0) buf.format("(");
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension myd = dimensions.get(i);
      String dimName = myd.getName();
      if ((dimName != null) && strict)
        dimName = NetcdfFile.escapeName(dimName);

      if (i != 0) buf.format(", ");

      if (myd.isVariableLength()) {
        buf.format("*");
      } else if (myd.isShared()) {
        if (!strict)
          buf.format("%s=%d",dimName,myd.getLength());
        else
          buf.format("%s",dimName);
      } else {
        if (dimName != null) {
          buf.format("%s=", dimName);
        }
        buf.format("%d", myd.getLength());
      }
    }

    if (getRank() > 0) buf.format(")");
  }

  /**
   * CDL representation of Variable, not strict.
   */
  public String toString() {
    return writeCDL("   ", false, false);
  }

  /**
   * CDL representation of a Variable.
   *
   * @param indent      start each line with this much space
   * @param useFullName use full name, else use short name
   * @param strict      strictly comply with ncgen syntax
   * @return CDL representation of the Variable.
   */
  public String writeCDL(String indent, boolean useFullName, boolean strict) {
    Formatter buf = new Formatter();
    writeCDL(buf, indent, useFullName, strict);
    return buf.toString();
  }

  protected void writeCDL(Formatter buf, String indent, boolean useFullName, boolean strict) {
    buf.format(indent);
    if (dataType.isEnum()) {
      if (enumTypedef == null)
        buf.format("enum UNKNOWN");
      else
        buf.format("enum %s", enumTypedef.getName());
    } else
      buf.format(dataType.toString());

    //if (isVariableLength) buf.append("(*)"); // LOOK
    buf.format(" ");
    getNameAndDimensions(buf, useFullName, strict);
    buf.format(";");
    if (!strict) buf.format(extraInfo());
    buf.format("\n");

    for (Attribute att : getAttributes()) {
      buf.format("%s  ", indent);
      if (strict) buf.format( NetcdfFile.escapeName(getShortName()));
      buf.format(":%s;", att.toString(strict));
      if (!strict && (att.getDataType() != DataType.STRING))
        buf.format(" // %s", att.getDataType());
      buf.format("\n");
    }
  }

  /**
   * String representation of Variable and its attributes.
   */
  public String toStringDebug() {
    return ncfile.toStringDebug(this);
  }

  private static boolean showSize = false;

  protected String extraInfo() {
    return showSize ? " // " + getElementSize() + " " + getSize() : "";
  }

  /**
   * Instances which have same content are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if (!(oo instanceof Variable)) return false;
    Variable o = (Variable) oo;

    if (!getShortName().equals(o.getShortName())) return false;
    if (isScalar() != o.isScalar()) return false;
    if (getDataType() != o.getDataType()) return false;
    if (!getParentGroup().equals(o.getParentGroup())) return false;
    if ((getParentStructure() != null) && !getParentStructure().equals(o.getParentStructure())) return false;
    if (isVariableLength() != o.isVariableLength()) return false;
    if (dimensions.size() != o.getDimensions().size()) return false;
    for (int i=0; i<dimensions.size(); i++)
      if (!getDimension(i).equals(o.getDimension(i))) return false;

    return true;
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getShortName().hashCode();
      if (isScalar()) result++;
      result = 37 * result + getDataType().hashCode();
      result = 37 * result + getParentGroup().hashCode();
      if (parent != null)
        result = 37 * result + parent.hashCode();
      if (isVariableLength) result++;
      result = 37 * result + dimensions.hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  protected int hashCode = 0;

  /**
   * Sort by name
   */
  public int compareTo(VariableSimpleIF o) {
    return getName().compareTo(o.getName());
  }

  /////////////////////////////////////////////////////////////////////////////

  protected Variable() {
  }

  /**
   * Create a Variable. Also must call setDataType() and setDimensions()
   *
   * @param ncfile    the containing NetcdfFile.
   * @param group     the containing group; if null, use rootGroup
   * @param parent    parent Structure, may be null
   * @param shortName variable shortName, must be unique within the Group
   */
  public Variable(NetcdfFile ncfile, Group group, Structure parent, String shortName) {
    this.ncfile = ncfile;
    this.group = (group == null) ? ncfile.getRootGroup() : group;
    this.parent = parent;
    this.shortName = shortName;
  }

  /**
   * Create a Variable. Also must call setDataType() and setDimensions()
   *
   * @param ncfile    the containing NetcdfFile.
   * @param group     the containing group; if null, use rootGroup
   * @param parent    parent Structure, may be null
   * @param shortName variable shortName, must be unique within the Group
   * @param dtype     the Variable's DataType
   * @param dims      space delimited list of dimension names. may be null or "" for scalars.
   */
  public Variable(NetcdfFile ncfile, Group group, Structure parent, String shortName, DataType dtype, String dims) {
    this.ncfile = ncfile;
    this.group = (group == null) ? ncfile.getRootGroup() : group;
    this.parent = parent;
    this.shortName = shortName;
    setDataType( dtype);
    setDimensions( dims);
  }

  /**
   * Copy constructor.
   * The returned Variable is mutable.
   * It shares the cache object and the iosp Object, attributes and dimensions with the original.
   * Does not share the proxyReader.
   * Use for section, slice, "logical views" of original variable.
   *
   * @param from copy from this Variable.
   */
  public Variable(Variable from) {
    this.attributes = new ArrayList<Attribute>(from.attributes); // attributes are immutable
    this.cache = from.cache; // caller should do createNewCache() if dont want to share
    this.dataType = from.getDataType();
    this.dimensions = new ArrayList<Dimension>(from.dimensions); // dimensions are shared
    this.elementSize = from.getElementSize();
    this.enumTypedef = from.enumTypedef;
    this.group = from.group;
    this.isMetadata = from.isMetadata;
    this.isVariableLength = from.isVariableLength;
    this.ncfile = from.ncfile;
    this.parent = from.parent;
    this.shape = from.getShape();
    this.shortName = from.shortName;
    this.sizeToCache = from.sizeToCache;
    this.spiObject = from.spiObject;
  }

  ///////////////////////////////////////////////////
  // the following make this mutable

  /**
   * Set the data type
   *
   * @param dataType set to this value
   */
  public void setDataType(DataType dataType) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.dataType = dataType;
    this.elementSize = getDataType().getSize();
  }

  /**
   * Set the short name
   *
   * @param shortName set to this value
   */
  public void setName(String shortName) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.shortName = shortName;
  }

  /**
   * Set the parent group.
   *
   * @param group set to this value
   */
  public void setParentGroup(Group group) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.group = group;
  }

  /**
   * Set the element size. Usually elementSize is determined by the dataType,
   * use this only for exceptional cases.
   *
   * @param elementSize set to this value
   */
  public void setElementSize(int elementSize) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.elementSize = elementSize;
  }

  /**
   * Add new or replace old if has same name
   *
   * @param att add this Attribute
   * @return the added attribute
   */
  public Attribute addAttribute(Attribute att) {
    if (immutable) throw new IllegalStateException("Cant modify");
    for (int i = 0; i < attributes.size(); i++) {
      Attribute a = attributes.get(i);
      if (att.getName().equals(a.getName())) {
        attributes.set(i, att); // replace
        return att;
      }
    }
    attributes.add(att);
    return att;
  }

  /**
   * Remove an Attribute : uses the attribute hashCode to find it.
   *
   * @param a remove this attribute
   * @return true if was found and removed
   */
  public boolean remove(Attribute a) {
    if (immutable) throw new IllegalStateException("Cant modify");
    return a != null && attributes.remove(a);
  }

  /**
   * Remove an Attribute by name.
   *
   * @param attName if exists, remove this attribute
   * @return true if was found and removed
   */
  public boolean removeAttribute(String attName) {
    if (immutable) throw new IllegalStateException("Cant modify");
    Attribute att = findAttribute(attName);
    return att != null && attributes.remove(att);
  }

  /**
   * Remove an Attribute by name, ignoring case
   *
   * @param attName if exists, remove this attribute
   * @return true if was found and removed
   */
  public boolean removeAttributeIgnoreCase(String attName) {
    if (immutable) throw new IllegalStateException("Cant modify");
    Attribute att = findAttributeIgnoreCase(attName);
    return att != null && attributes.remove(att);
  }

  /**
   * Set the shape with a list of Dimensions. The Dimensions may be shared or not.
   * Dimensions are in order, slowest varying first. Send a null for a scalar.
   * Technically you can use Dimensions from any group; pragmatically you should only use
   * Dimensions contained in the Variable's parent groups.
   *
   * @param dims list of type ucar.nc2.Dimension
   */
  public void setDimensions(List<Dimension> dims) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.dimensions = (dims == null) ? new ArrayList<Dimension>() : new ArrayList<Dimension>(dims);
    resetShape();
  }

  /**
   * Use when dimensions have changed, to recalculate the shape.
   */
  public void resetShape() {
    // if (immutable) throw new IllegalStateException("Cant modify");  LOOK allow this for unlimited dimension updating
    this.shape = new int[dimensions.size()];
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension dim = dimensions.get(i);
      shape[i] = dim.getLength();
      //shape[i] = Math.max(dim.getLength(), 0); // LOOK
      // if (dim.isUnlimited() && (i != 0)) // LOOK only true for Netcdf-3
      //   throw new IllegalArgumentException("Unlimited dimension must be outermost");
      if (dim.isVariableLength()) {
        //if (dimensions.size() != 1)
        //  throw new IllegalArgumentException("Unknown dimension can only be used in 1 dim array");
        //else
          isVariableLength = true;
      }
    }
    this.shapeAsSection = null; // recalc next time its asked for
  }

  /**
   * Set the dimensions using the dimensions names. The dimension is searched for recursively in the parent groups.
   *
   * @param dimString : whitespace seperated list of dimension names, or '*' for Dimension.UNKNOWN. null or empty String is a scalar.
   */
  public void setDimensions(String dimString) {
    if (immutable) throw new IllegalStateException("Cant modify");
    List<Dimension> newDimensions = new ArrayList<Dimension>();

    if ((dimString == null) || (dimString.length() == 0)) { // scalar
      this.dimensions = newDimensions;
      resetShape();
      return;
    }

    StringTokenizer stoke = new StringTokenizer(dimString);
    while (stoke.hasMoreTokens()) {
      String dimName = stoke.nextToken();
      Dimension d = dimName.equals("*") ? Dimension.VLEN : group.findDimension(dimName);
      if (d == null) {
        // if numeric - then its anonymous dimension
        try {
          int len = Integer.parseInt(dimName);
          d = new Dimension("", len, false, false, false);
        } catch (Exception e)  {
          throw new IllegalArgumentException("Variable " + getName() + " setDimensions = " + dimString +
              " FAILED, dim doesnt exist=" + dimName+ " file = "+ncfile.getLocation());
        }
      }
      newDimensions.add(d);
    }

    this.dimensions = newDimensions;
    resetShape();
  }

  /**
   * Reset the dimension array. Anonymous dimensions are left alone.
   * Shared dimensions are searched for recursively in the parent groups.
   */
  public void resetDimensions() {
    if (immutable) throw new IllegalStateException("Cant modify");
    ArrayList<Dimension> newDimensions = new ArrayList<Dimension>();

    for (Dimension dim : dimensions) {
      if (dim.isShared()) {
        Dimension newD = group.findDimension(dim.getName());
        if (newD == null)
          throw new IllegalArgumentException("Variable " + getName() + " resetDimensions  FAILED, dim doesnt exist in parent group=" + dim);
        newDimensions.add(newD);
      } else {
        newDimensions.add( dim);
      }
    }
    this.dimensions = newDimensions;
    resetShape();
  }

  /**
   * Set the dimensions using all anonymous (unshared) dimensions
   *
   * @param shape defines the dimension lengths. must be > 0, or -1 for VLEN
   * @throws ucar.ma2.InvalidRangeException if any shape < 1
   */
  public void setDimensionsAnonymous(int[] shape) throws InvalidRangeException {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.dimensions = new ArrayList<Dimension>();
    for (int i = 0; i < shape.length; i++) {
      if ((shape[i] < 1) &&  (shape[i] != -1)) throw new InvalidRangeException("shape[" + i + "]=" + shape[i] + " must be > 0");
      Dimension anon;
      if (shape[i] == -1) {
        anon = Dimension.VLEN;
        isVariableLength = true;
      } else {
        anon = new Dimension(null, shape[i], false, false, false);
      }

      dimensions.add(anon);
    }
    resetShape();
  }

  /**
   * Set this Variable to be a scalar
   */
  public void setIsScalar() {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.dimensions = new ArrayList<Dimension>();
    resetShape();
  }

  /**
   * Replace a dimension with an equivalent one.
   * @param dim must have the same name, length as old one
   *
  public void replaceDimension( Dimension dim) {
  int idx = findDimensionIndex( dim.getName());
  if (idx >= 0)
  dimensions.set( idx, dim);
  resetShape();
  } */

  /**
   * Replace a dimension with an equivalent one.
   *
   * @param idx index into dimension array
   * @param dim to set
   */
  public void setDimension(int idx, Dimension dim) {
    if (immutable) throw new IllegalStateException("Cant modify");
    dimensions.set(idx, dim);
    resetShape();
  }

  /**
   * Make this immutable.
   * @return this
   */
  public Variable setImmutable() {
    immutable = true;
    dimensions = Collections.unmodifiableList(dimensions);
    attributes = Collections.unmodifiableList(attributes);
    return this;
  }

  /**
   * Is this Variable immutable
   * @return if immutable
   */
  public boolean isImmutable() { return immutable; }


  // for IOServiceProvider
  protected Object spiObject;

  /**
   * Should not be public.
   * @return the IOSP object
   */
  public Object getSPobject() {
    return spiObject;
  }

  /**
   * Should not be public.
   * @param spiObject the IOSP object
   */
  public void setSPobject(Object spiObject) {
    this.spiObject = spiObject;
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // caching

  /**
   * If total data is less than SizeToCache in bytes, then cache.
   *
   * @return size at which caching happens
   */
  public int getSizeToCache() {
    return sizeToCache;
  }

  /**
   * Set the sizeToCache. If not set, use Variable.defaultSizeToCache
   *
   * @param sizeToCache size at which caching happens
   */
  public void setSizeToCache(int sizeToCache) {
    this.sizeToCache = sizeToCache;
  }

  /**
   * Set whether to cache or not. Implies that the entire array will be stored, once read.
   * Normally this is set automatically based on size of data.
   *
   * @param caching set if caching.
   */
  public void setCaching(boolean caching) {
    this.cache.isCaching = caching;
    this.cache.cachingSet = true;
  }

  /**
   * Will this Variable be cached when read.
   * Set externally, or calculated based on total size < sizeToCache.
   *
   * @return true is caching
   */
  public boolean isCaching() {
    if (!this.cache.cachingSet) {
      cache.isCaching = !isVariableLength && (getSize() * getElementSize() < sizeToCache);
      this.cache.cachingSet = true;
    }
    return cache.isCaching;
  }

  /**
   * Invalidate the data cache
   */
  public void invalidateCache() {
    cache.data = null;
  }

  public void setCachedData(Array cacheData) {
    setCachedData(cacheData, false);
  }

  //public Array getCachedData() {
  //  return (cache == null) ? null : cache.data;
  //}
  /**
   * Set the data cache
   *
   * @param cacheData  cache this Array
   * @param isMetadata : synthesized data, set true if must be saved in NcML output (ie data not actually in the file).
   */
  public void setCachedData(Array cacheData, boolean isMetadata) {
    if ((cacheData != null) && (cacheData.getElementType() != getDataType().getPrimitiveClassType()))
      throw new IllegalArgumentException("setCachedData type="+cacheData.getElementType()+" incompatible with variable type="+getDataType());

    //if (cacheData.getSize() > 10 * 1000 * 1000)
    //   System.out.println("HEY");   

    this.cache.data = cacheData;
    this.isMetadata = isMetadata;
    this.cache.cachingSet = true;
    this.cache.isCaching = true;
  }

  /**
   * Create a new data cache, use this when you dont want to share the cache.
   */
  public void createNewCache() {
    this.cache = new Cache();
  }

  /**
   * Has data been read and cached.
   * Use only on a Variable, not a subclass.
   * @return true if data is read and cached
   */
  public boolean hasCachedData() {
    return (cache != null) && (null != cache.data);
  }

  // this indirection allows us to share the cache among the variable's sections and copies
  static protected class Cache {
    public Array data;
    public boolean isCaching = false;
    public boolean cachingSet = false;

    public Cache() {
    }
  }

  ////////////////////////////////////////////////////////////////////////
  // StructureMember - could be a subclass, but that has problems

  /**
   * Is this variable a member of a Structure?.
   */
  public boolean isMemberOfStructure() {
    return parent != null;
  }

  /**
   * Get the parent Variable if this is a member of a Structure, or null if its not.
   */
  public Structure getParentStructure() {
    return parent;
  }

  /**
   * Set the parent structure.
   *
   * @param parent set to this value
   */
  public void setParentStructure(Structure parent) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.parent = parent;
  }

  /**
   * Get list of Dimensions, including parents if any.
   *
   * @return array of Dimension, rank of v plus all parents.
   */
  public List<Dimension> getDimensionsAll() {
    List<Dimension> dimsAll = new ArrayList<Dimension>();
    addDimensionsAll(dimsAll, this);
    return dimsAll;
  }

  private void addDimensionsAll(List<Dimension> result, Variable v) {
    if (v.isMemberOfStructure())
      addDimensionsAll(result, v.getParentStructure());

    for (int i=0; i<v.getRank(); i++)
      result.add( v.getDimension(i));
  }

  public int[] getShapeAll() {
    if (parent == null) return getShape();
    List<Dimension> dimAll = getDimensionsAll();
    int[] shapeAll = new int[dimAll.size()];
    for (int i=0; i<dimAll.size(); i++)
      shapeAll[i] = dimAll.get(i).getLength();
    return shapeAll;
  }


  /*
   * Read data in all structures for this Variable, using a string sectionSpec to specify the section.
   * See readAllStructures(Section section, boolean flatten) method for details.
   *
   * @param sectionSpec specification string, eg "1:2,10,:,1:100:10"
   * @param flatten     if true, remove enclosing StructureData.
   * @return the requested data which has the shape of the request.
   * @see #readAllStructures
   * @deprecated
   *
  public Array readAllStructuresSpec(String sectionSpec, boolean flatten) throws IOException, InvalidRangeException {
    return readAllStructures(new Section(sectionSpec), flatten);
  }

  /*
   * Read data from all structures for this Variable.
   * This is used for member variables whose parent Structure(s) is not a scalar.
   * You must specify a Range for each dimension in the enclosing parent Structure(s).
   * The returned Array will have the same shape as the requested section.
   * <p/>
   * <p>If flatten is false, return nested Arrays of StructureData that correspond to the nested Structures.
   * The innermost Array(s) will match the rank and type of the Variable, but they will be inside Arrays of
   * StructureData.
   * <p/>
   * <p>If flatten is true, remove the Arrays of StructureData that wrap the data, and return an Array of the
   * same type as the Variable. The shape of the returned Array will be an accumulation of all the shapes of the
   * Structures containing the variable.
   *
   * @param sectionAll an array of Range objects, one for each Dimension of the enclosing Structures, as well as
   *                   for the Variable itself. If the list is null, use the full shape for everything.
   *                   If an individual Range is null, use the full shape for that dimension.
   * @param flatten    if true, remove enclosing StructureData. Otherwise, each parent Structure will create a
   *                   StructureData container for the returned data array.
   * @return the requested data which has the shape of the request.
   * @deprecated
   *
  public Array readAllStructures(ucar.ma2.Section sectionAll, boolean flatten) throws java.io.IOException, ucar.ma2.InvalidRangeException {
    Section resolved; // resolve all nulls
    if (sectionAll == null)
      resolved = makeSectionAddParents(null, false); // everything
    else {
      ArrayList<Range> resultAll = new ArrayList<Range>();
      makeSectionWithParents(resultAll, sectionAll.getRanges(), this);
      resolved = new Section(resultAll);
    }

    return _readMemberData(resolved, flatten);
  }

  // recursively create the section (list of Range) array
  private List<Range> makeSectionWithParents(List<Range> result, List<Range> orgSection, Variable v) throws InvalidRangeException {
    List<Range> section = orgSection;

    // do parent stuctures(s) first
    if (v.isMemberOfStructure())
      section = makeSectionWithParents(result, orgSection, v.getParentStructure());

    // process just this variable's subList
    List<Range> myList = section.subList(0, v.getRank());
    Section mySection = new Section(myList, v.getShape());
    result.addAll(mySection.getRanges());

    // return section with this variable's sublist removed
    return section.subList(v.getRank(), section.size());
  } */

  /*
   * Composes this variable's ranges with another list of ranges, adding parent ranges; resolves nulls.
   *
   * @param section   Section of this Variable, same rank as v, may have nulls or be null.
   * @param firstOnly if true, get first parent, else get all parrents.
   * @return Section, rank of v plus parents, no nulls
   * @throws InvalidRangeException if bad
   *
  private Section makeSectionAddParents(Section section, boolean firstOnly) throws InvalidRangeException {
    Section result;
    if (section == null)
      result = new Section(getRanges());
    else
      result = new Section(section.getRanges(), getShape());

    // add parents
    Structure p = getParentStructure();
    while (p != null) {
      Section parentSection = p.getShapeAsSection();
      for (int i = parentSection.getRank() - 1; i >= 0; i--) { // reverse
        Range r = parentSection.getRange(i);
        result.insertRange(0, firstOnly ? new Range(0, 0) : r);
      }
      p = p.getParentStructure();
    }

    return result;
  } */

  /* private Array readMemberOfStructureFlatten(Section section) throws InvalidRangeException, IOException {
    // get through first parents element
    Section sectionAll = makeSectionAddParents(section, true);
    Array data = _readMemberData(sectionAll, true); // flatten

    // remove parent dimensions.
    int n = data.getRank() - getRank();
    for (int i = 0; i < n; i++)
      if (data.getShape()[0] == 1) data = data.reduce(0);
    return data;
  }

  /* structure-member Variable;  section has a Range for each array in the parent
  // stuctures(s) and for the Variable.
  protected Array _readMemberData(Section section, boolean flatten) throws IOException, InvalidRangeException {
    return ncfile.readMemberData(this, section, flatten);
  } */

  ////////////////////////////////

  /**
   * Calculate if this is a classic coordinate variable: has same name as its first dimension.
   * If type char, must be 2D, else must be 1D.
   * @return true if a coordinate variable.
   */
  public boolean isCoordinateVariable() {
    if ((dataType == DataType.STRUCTURE) || isMemberOfStructure()) // Structures and StructureMembers cant be coordinate variables
      return false;

    int n = getRank();
    if (n == 1 && dimensions.size() == 1) {
      Dimension firstd = dimensions.get(0);
      if (shortName.equals(firstd.getName())) { //  : short names match
        return true;
      }
    }
    if (n == 2 && dimensions.size() == 2) {    // two dimensional
      Dimension firstd = dimensions.get(0);
      if (shortName.equals(firstd.getName()) &&  // short names match
          (getDataType() == DataType.CHAR)) {         // must be char valued (really a String)
        return true;
      }
    }

    return false;
  }

  ///////////////////////////////////////////////////////////////////////
  // deprecated
  /**
   * @deprecated use isVariableLength()
   * @return isVariableLength()
   */
  public boolean isUnknownLength() {
    return isVariableLength;
  }
}
