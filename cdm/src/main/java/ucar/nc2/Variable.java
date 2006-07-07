// $Id: Variable.java,v 1.50 2006/05/19 23:20:52 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2;

import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

/**
 * A Variable is a logical container for data. It has a dataType, a set of Dimensions that define its array shape,
 * and optionally a set of Attributes.
 *
 * The data is a multidimensional array of primitive types, Strings, or Structures.
 * Data access is done through the read() methods, which return a memory resident Array.
 *
 * @see ucar.ma2.Array
 * @author caron
 * @version $Revision: 1.50 $ $Date: 2006/05/19 23:20:52 $
 */

public class Variable implements VariableIF {
  static public final int defaultSizeToCache = 4000; // bytes
  static protected boolean debugCaching = false;

  protected NetcdfFile ncfile; // used for I/O calls, not necessarily the logical container
  protected Variable orgVar = null; // different for section, VariableDS wrapping. used in I/O calls

  protected Group group;
  protected String shortName;
  protected int[] shape;
  protected DataType dataType;
  protected int elementSize;
  protected ArrayList dimensions = new ArrayList();
  protected ArrayList attributes = new ArrayList();

  protected boolean isCoordinateAxis = false;
  protected boolean isVlen = false;
  protected boolean isMetadata = false;
 // private boolean isUnsigned = false;

  protected Cache cache = new Cache();
  protected int sizeToCache = defaultSizeToCache; // bytes

  protected Structure parent = null; // for variables inside Structure or VariableSection

  /**
   * Get the full name of this Variable, starting from rootGroup. The name is unique within the
   * entire NetcdfFile.
   */
  public String	getName() {
    return ncfile == null ? "" : NetcdfFile.makeFullName( this.group, this);
  }

  /**
   * Get the short name of this Variable. The name is unique within its parent group.
   */
  public String	getShortName() { return shortName; }

  /**
   * Get the data type of the Variable.
   */
  public DataType getDataType() { return dataType; }

  /**
    * Get the shape: length of Variable in each dimension.
    *
    * @return int array whose length is the rank of this
    * and whose values equal the length of that Dimension.
    */
  public int[] getShape() { return (int []) shape.clone(); }

  /**
   * Get the total number of elements in the Variable.
   * If this is an unlimited Variable, will return the current number of elements.
   * If this is a Sequence, will return 0.
   * @return total number of elements in the Variable.
   */
  public long getSize() {
    long size = 1;
    for (int i=0; i<shape.length; i++)
      size *= shape[i];
    return size;
  }

  /**
   * Get the number of bytes for one element of this Variable.
   * For Variables of primitive type, this is equal to getDataType().getSize().
   * Variables of String type dont know their size, so what they return is undefined.
   * Variables of Structure type return the total number of bytes for all the members of
   *  one Structure, plus possibly some extra padding, depending on the underlying format.
   * Variables of Sequence type return the number of bytes of one element.
   * @return total number of bytes for the Variable
   */
  public int getElementSize() {
    return elementSize;
  }
  /**
   * Get the number of dimensions of the Variable.
   */
  public int getRank() { return shape.length; }

  /**
   * Get the containing Group.
   */
  public Group getParentGroup() { return group; }

  /**
   * If this is a coordinate variable or axis, return the corresponding dimension. If not, return null.
   * A coordinate axis has this as its single dimension, and names this Dimensions's the coordinates.
   * A coordinate variable is the same as a coordinate axis, but its name must match the dimension name.
   * If numeric, coordinate axis must be strictly monotonically increasing or decreasing.
   * @see Dimension#getCoordinateVariables
   */
  public Dimension getCoordinateDimension() {
    return isCoordinateAxis ? (Dimension) dimensions.get(0) : null;
  }

 /**
   * Is this variable metadata?. Yes, if needs to be included explicitly in NcML output.
   */
  public boolean isMetadata() { return isMetadata; }

  /**
   * Whether this is a scalar Variable (rank == 0).
   */
  public boolean isScalar() { return getRank() == 0; }

  /**
    * @deprecated use isVariableLength()
    */
   public boolean isUnknownLength() { return isVlen; }

  /**
    * Does this variable have an variable length dimension.
    * If so, it is a one-dimensional array with
    * dimension = Dimension.UNKNOWN.
    */
   public boolean isVariableLength() { return isVlen; }

  /**
    * Is this Variable unsigned?. Only meaningful for byte, short, int, long types.
    */
   public boolean isUnsigned() {
    return findAttribute("_unsigned") != null;
  }

  /**
   * Can this variable's size grow?.
   * This is equivalent to saying at least one of its dimensions is unlimited.
   * @return boolean true iff this variable can grow
   */
  public boolean isUnlimited() {
    for (int i=0; i<dimensions.size(); i++) {
      Dimension d = (Dimension) dimensions.get(i);
      if (d.isUnlimited()) return true;
    }
   return false;
  }

  /**
   * Get the list of dimensions used by this variable.
   * The most slowly varying (leftmost for Java and C programmers) dimension is first.
   * For scalar variables, the list is empty.
   * @return List with objects of type ucar.nc2.Dimension
   */
  public java.util.List getDimensions() { return new ArrayList(dimensions); }

  /** Get the ith dimension.
   * @param i index of the dimension.
   * @return requested Dimension, or null if i is out of bounds.
   */
  public Dimension getDimension(int i) {
    if ((i < 0) || (i >= getRank())) return null;
    return (Dimension) dimensions.get(i);
  }

  /**
   * Get the list of Dimension names, space delineated.
   */
  public String getDimensionsString() { // LOOK what about anon dimensions?
    StringBuffer buff = new StringBuffer();
    List dims = getDimensions();
    for (int i=0; i<dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (i > 0) buff.append( " ");
      buff.append( dim.getName());
    }
    return buff.toString();
  }

  /**
   * Find the index of the named Dimension in this Variable.
   * @param name the name of the dimension
   * @return the index of the named Dimension, or -1 if not found.
   */
  public int findDimensionIndex(String name) {
    for (int i=0; i<dimensions.size(); i++) {
      Dimension d = (Dimension) dimensions.get(i);
      if (name.equals(d.getName()))
        return i;
    }
    return -1;
  }

  /**
   * Returns the set of attributes for this variable.
   * @return List of object type ucar.nc2.Attribute
   */
  public java.util.List getAttributes() { return new ArrayList(attributes); }

  /**
   * Find an Attribute by name.
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttribute(String name) {
    for (int i=0; i<attributes.size(); i++) {
      Attribute a = (Attribute) attributes.get(i);
      if (name.equals(a.getName()))
        return a;
    }
    return null;
  }

  /**
   * Find an Attribute by name, ignoring the case.
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttributeIgnoreCase(String name) {
    for (int i=0; i<attributes.size(); i++) {
      Attribute a = (Attribute) attributes.get(i);
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
   * Default is to use "units" attribute value
   *
   * @return unit string, or null if not found.
   */
  public String getUnitsString() {
    String units = null;
    Attribute att = findAttributeIgnoreCase("units");
    if ((att != null) && att.isString())
      units = att.getStringValue();
    return units;
  }

  /////////////////////////////////////////////////////////////////////////
  // sections
  protected boolean isSection = false, isSlice = false;
  protected List sectionRanges = null;  // section of the original variable.
  protected List sliceRanges = null;  // section of the original variable.
  protected int sliceDim = -1;  // dimension index into original

  /**
   * Get shape as an array of Range objects.
   * @return array of Ranges, one for each Dimension.
   */
  public List getRanges() {
    return Range.factory( shape);
  }

  /**
   * Get index subsection as an array of Range objects, reletive to the original variable.
   * If this is a section, will reflect the index range reletive to the original variable.
   * If its a slice, it will have a rank different from this variable.
   * Otherwise it will correspond to this Variable's shape, ie match getRanges().
   * @return array of Ranges, one for each Dimension.
   */
  public List getSectionRanges() {
    if (!isSection)
      return getRanges();

    ArrayList result = new ArrayList();
    for (int i = 0; i < sectionRanges.size(); i++) {
      Range r =  (Range) sectionRanges.get(i);
      try {
        result.add( new Range(r));
      } catch (InvalidRangeException e) { // cant happpen
      }
    }
    return result;
  }

  /**
   * Is this Variable a section of another variable ?.
   */
  protected boolean isSection() { return isSection; }

  /**
   * Create a new Variable that is a logical subsection of this Variable.
   * No data is read until a read method is called on it.
   * @param section List of type ucar.ma2.Range, with size equal to getRank().
   *   Each Range corresponds to a Dimension, and specifies the section of data to read in that Dimension.
   *   A Range object may be null, which means use the entire dimension.
   * @return a new Variable which is a logical section of this Variable.
   * @throws InvalidRangeException
   */
  public Variable section(List section) throws InvalidRangeException  {
    Variable vs = new Variable( this);
    makeSection( vs, section);
    return vs;
  }

  // work goes here so can be called by subclasses
  protected void makeSection(Variable newVar, List section) throws InvalidRangeException  {
    // check consistency
    if (isSlice)
      throw new InvalidRangeException("Variable.section: cannot section a slice"); // LOOK, could remove restriction
    if (section.size() != getRank())
      throw new InvalidRangeException("Variable.section: section rank "+section.size()+" != "+getRank());
    for (int ii=0; ii<section.size(); ii++) {
      Range r = (Range) section.get(ii);
      if (r == null)
        continue;
      if ((r.first() < 0) || (r.first() >= shape[ii]))
        throw new InvalidRangeException("Bad range starting value at index "+ii+" == "+r.first());
      if ((r.last() < 0) || (r.last() >= shape[ii]))
        throw new InvalidRangeException("Bad range ending value at index "+ii+" == "+r.last());
    }

    newVar.orgVar = (orgVar != null) ? orgVar : this;
    newVar.isSection = true;

    newVar.sectionRanges = makeSectionRanges( this, section);
    newVar.shape  = Range.getShape( newVar.sectionRanges);

    // replace dimensions if needed !! LOOK not shared
    newVar.dimensions = new ArrayList();
    for (int i=0; i<getRank(); i++ ) {
      Dimension oldD = getDimension(i);
      Dimension newD = (oldD.getLength() == newVar.shape[i]) ? oldD : new Dimension( oldD.getName(), newVar.shape[i], false);
      newD.setUnlimited( oldD.isUnlimited());
      newVar.dimensions.add( newD);
    }
  }

  /**
   * Composes a variable's ranges with another list of ranges; resolves nulls.
   * Makes sure that Variables that are sections are handled correctly.
   * @param v the variable
   * @param section List of ucar.ma2.Range, same rank as v, may have nulls.
   * @return List of ucar.ma2.Range, same rank as v, no nulls.
   * @throws InvalidRangeException
   */
  static protected List makeSectionRanges(Variable v, List section) throws InvalidRangeException {
    // all nulls
    if (section == null) return v.getRanges();

    // check individual nulls
    List orgRanges = v.getSectionRanges();
    ArrayList results = new ArrayList(v.getRank());
    for (int i=0; i<v.getRank(); i++) {
      Range r = (Range) section.get(i);
      Range result;
      if (r == null)
        result = new Range( (Range) orgRanges.get(i)); // use entire range
      else if (v.isSection())
        result = new Range( (Range) orgRanges.get(i), r); // compose
      else
        result = new Range(r); // use section
      result.setName( v.getDimension(i).getName()); // used when composing slices and sections
      results.add ( result);
    }

    return results;
  }

  /**
   * Composes this variable's ranges with another list of ranges, adding parent ranges; resolves nulls.
   * @param section List of ucar.ma2.Range, same rank as v, may have nulls.
   * @param firstOnly if true, get first parent, else get all parrents.
   * @return List of ucar.ma2.Range, rank of v and parents, no nulls
   * @throws InvalidRangeException
   */
  protected List makeSectionAddParents(List section, boolean firstOnly) throws InvalidRangeException {
    List result = makeSectionRanges( this, section);

    // add parents
    Variable v = getParentStructure();
    while ( v != null) {
      List parentSection = v.getRanges();
      for (int i = parentSection.size()-1; i >= 0; i--) { // reverse
        Range r = (Range) parentSection.get(i);
        int first = r.first();
        int last = firstOnly ? first : r.last(); // first or all
        Range newr = new Range( first, last);
        result.add(0, newr);
      }
      v = v.getParentStructure();
    }

    return result;
  }

  /**
   * Create a new Variable that is a logical slice of this Variable, by
   * fixing the specified dimension at the specified index value. This reduces rank by 1.
   * No data is read until a read method is called on it.
   * @param dim which dimension to fix
   * @param value at what index value
   * @return a new Variable which is a logical slice of this Variable.
   * @throws InvalidRangeException
   */
  public Variable slice(int dim, int value) throws InvalidRangeException {
    Variable vs = new Variable( this);
    makeSlice(vs, dim, value);
    return vs;
  }

  protected void makeSlice(Variable newVar, int dim, int value) throws InvalidRangeException {
    if (isSection)
      throw new InvalidRangeException("Variable.slice: cannot slice a section"); // LOOK, could remove restriction

    // check consistency
    if ((dim < 0) || (dim >= shape.length))
      throw new InvalidRangeException("Variable.slice: invalid dimension= "+dim);
    if ((value < 0) || (value >= shape[dim]))
      throw new InvalidRangeException("Variable.slice: invalid value= "+value+" for dimension= "+dim);

    // create the new shape
    List dims = getDimensions();
    dims.remove( dim);
    newVar.setDimensions( dims);

    // construct or augment the sliceRanges array
    ArrayList newSlices;
    if (isSlice) { // slice of a slice
      int count = 0;
      newSlices = new ArrayList(sliceRanges);
      for (int i = 0; i < newSlices.size(); i++) {
        Range range = (Range) newSlices.get(i);
        if (range == null) {
          if (dim == count) newSlices.set( dim, new Range(value, value));
          count++;
        }
      }
    } else {
      newSlices = new ArrayList( getRank());
      for (int i = 0; i < shape.length; i++)
        newSlices.add( null);
      newSlices.set( dim, new Range(value, value));
    }
    newVar.sliceRanges = newSlices;

    newVar.orgVar = (orgVar != null) ? orgVar : this;
    newVar.isSlice = true;
  }

  // compose a user requested section with the slice sections to get a full section into the orginal variable
  protected List makeSliceRanges(List section) throws InvalidRangeException {
    int count = 0;
    ArrayList result = new ArrayList(sliceRanges);
    for (int i = 0; i < sliceRanges.size(); i++) {
      Range range = (Range) sliceRanges.get(i);
      if (range == null)
        result.set(i, section.get(count++));
    }

    return result;
  }


  //////////////////////////////////////////////////////////////////////////////
  // IO
  // implementation notes to subclassers
  // all other calls use them, so override only these:
  //   _read()
  //   _read(List section)
  //   _readNestedData(List section, boolean flatten)

  /**
   * Read a section of the data for this Variable and return a memory resident Array.
   * The Array has the same element type as the Variable, and the requested shape.
   * Note that this does not do rank reduction, so the returned Array has the same rank
   *  as the Variable. Use Array.reduce() for rank reduction.
   * <p>
   * <code>assert(origin[ii] + shape[ii]*stride[ii] <= Variable.shape[ii]); </code>
   * <p>
   * @param origin int array specifying the starting index. If null, assume all zeroes.
   * @param shape int array specifying the extents in each dimension. If null, assume getShape();
   *  This becomes the shape of the returned Array.
   * @return the requested data in a memory-resident Array
   */
  public Array read(int [] origin, int [] shape) throws IOException, InvalidRangeException  {
    ArrayList section = new ArrayList(getRank());
    for (int i=0; i<getRank(); i++ ) {
      int first = (origin==null) ? 0 : origin[i];
      int last = (shape==null) ? getShape()[i] : first + shape[i] - 1;
      Range r = new Range( first, last);
      r.setName( getDimension(i).getName()); // ??
      section.add( r);
    }
    return read( section);
  }

  /**
   * Read data section specified by a "section selector", and return a memory resident Array. Uses
   * Fortran 90 array section syntax.
   *
   * @param sectionSpec specification string, eg "1:2,10,:,1:100:10". May optionally have ().
   * @return the requested data in a memory-resident Array
   * @see ucar.ma2.Range#parseSpec(String sectionSpec) for sectionSpec syntax
   */
  public Array read(String sectionSpec) throws IOException, InvalidRangeException  {
    List section = Range.parseSpec(sectionSpec);
    return read( section);
  }

  /**
   * Read a section of the data for this Variable from the netcdf file and return a memory resident Array.
   * The Array has the same element type as the Variable, and the requested shape.
   * Note that this does not do rank reduction, so the returned Array has the same rank
   *  as the Variable. Use Array.reduce() for rank reduction.
   * <p>
   * If the Variable is a member of an array of Structures, this returns only the variable's data
   * in the first Structure, so that the Array shape is the same as the Variable.
   * To read the data in all structures, use readAllStructures().
   * <p>
   * Note this only allows you to specify a subset of this variable.
   * If the variable is nested in a array of structures and you want to subset that, use
   * NetcdfFile.read(String sectionSpec, boolean flatten);
   *
   * @param section list of Range specifying the section of data to read.
   *   Must be null or same rank as variable.
   *   If list is null, assume all data.
   *   Each Range corresponds to a Dimension. If the Range object is null, it means use the entire dimension.
   *
   * @return the requested data in a memory-resident Array
   * @see #readAllStructures to read member variables in all structures
   * @see NetcdfFile#read to read nested variables with Structure subsetting
   */
  public Array read(List section) throws IOException, InvalidRangeException  {
    if (null == section)
      return read();

    if (isMemberOfStructure()) // read using first element of parents
      return readMemberOfStructureFlatten( section);

    if (isSection)
      return _read( makeSectionRanges(this, section));

    if (isSlice) {
      Array data = _read( makeSliceRanges(section));
      int count = 0;
      for (int i = 0; i < sliceRanges.size(); i++) {
        Range range = (Range) sliceRanges.get(i);
        if (range != null) {
          data = data.reduce(i-count); // reduce each slice dimension
          count++;
        }
      }
      return data;
    }

    return _read(section);
  }

  /**
   * Read all the data for this Variable and return a memory resident Array.
   * The Array has the same element type and shape as the Variable.
   * <p>
   * If the Variable is a member of an array of Structures, this returns only the variable's data
   * in the first Structure, so that the Array shape is the same as the Variable.
   * To read the data in all structures, use readAllStructures().
   *
   * @return the requested data in a memory-resident Array.
   * @see #readAllStructures to read member variables in all structures
   */
  public Array read() throws IOException {
    if (isMemberOfStructure()) { // LOOK - could see if parent structure is cached ??
      try {
        return readMemberOfStructureFlatten( getRanges());
      } catch (InvalidRangeException e) {
        return null; // cant happen
      }
    }

    try {
      if (isSection)
        return _read(sectionRanges);

      if (isSlice)
        return read(getRanges());

      return _read();

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      return null;
    }
  }

  private Array readMemberOfStructureFlatten(List section) throws InvalidRangeException, IOException {
    // get through first parents element
    List sectionAll = makeSectionAddParents(section, true);
    Array data = _readMemberData( sectionAll, true); // flatten

    // remove parent dimensions.
    int n = data.getRank() - getRank();
    for (int i=0; i<n; i++)
       if (data.getShape()[0] == 1) data = data.reduce(0);
    return data;
  }

  /*************************************************************************/
  // this section for a variable that is a member of a structure

 /**
   * Is this variable is a member of a Structure?.
   */
  public boolean isMemberOfStructure() { return parent != null; }

  /**
   * Get the parent Variable if this is a member of a Structure, or null if its not.
   */
  public Structure getParentStructure() { return parent; }

  /*
   * Get index subsection as an array of Range objects, including parents if any. If not isMemberOfStructure(),
   * this is the same as getRanges();
   * @return array of Ranges, rank of v plus all parents.
   *
  public List getRangesAll() {
    if (isMemberOfStructure())
      try {
        return makeSectionAddParents(null, false);
      } catch (InvalidRangeException e) {
        return null; // cant happen
      }
    else
      return getRanges();
  } */

  /**
   * Get list of Dimensions, including parents if any.
   * @return array of Dimension, rank of v plus all parents.
   */
  public List getDimensionsAll() {
    if (dimsAll == null) {
      dimsAll = new ArrayList();
      getDimensionsAll( dimsAll, this);
    }
    return dimsAll;
  }
  private ArrayList dimsAll = null;

  private void getDimensionsAll(List result, Variable v) {
    if (v.isMemberOfStructure())
      getDimensionsAll(result, v.getParentStructure());

    for (int i=0; i<v.getRank(); i++)
      result.add( v.getDimension(i));
  }

  /**
   * Read data in all structures for this Variable, using a string sectionSpec to specify the section.
   *  See readAllStructures(List section, boolean flatten) method for details.
   * @param sectionSpec specification string, eg "1:2,10,:,1:100:10"
   * @param flatten if true, remove enclosing StructureData.
   *
   * @return the requested data which has the shape of the request.
   * @see #readAllStructures
   * @see ucar.ma2.Range#parseSpec(String sectionSpec)
   */
  public Array readAllStructuresSpec(String sectionSpec, boolean flatten) throws IOException, InvalidRangeException {
    List section = Range.parseSpec(sectionSpec);
    return readAllStructures( section, flatten);
  }

  /**
   * Read data from all structures for this Variable.
   * This is used for member variables whose parent Structure(s) is not a scalar.
   * You must specify a Range for each dimension in the enclosing parent Structure(s).
   * The returned Array will have the same shape as the requested section.
   *
   * <p>If flatten is false, return nested Arrays of StructureData that correspond to the nested Structures.
   * The innermost Array(s) will match the rank and type of the Variable, but they will be inside Arrays of
   * StructureData.
   *
   * <p>If flatten is true, remove the Arrays of StructureData that wrap the data, and return an Array of the
   * same type as the Variable. The shape of the returned Array will be an accumulation of all the shapes of the
   * Structures containing the variable.
   *
   * @param sectionAll an array of Range objects, one for each Dimension of the enclosing Structures, as well as
   *   for the Variable itself. If the list is null, use the full shape for everything.
   *   If an individual Range is null, use the full shape for that dimension.
   *
   * @param flatten if true, remove enclosing StructureData. Otherwise, each parent Structure will create a
   *   StructureData container for the returned data array.
   *
   * @return the requested data which has the shape of the request.
   */
  public Array readAllStructures(List sectionAll, boolean flatten) throws IOException, InvalidRangeException {
    if (!isMemberOfStructure())
      return read( sectionAll);

    ArrayList resultAll = new ArrayList();
    makeSectionWithParents( resultAll, sectionAll, this);

    return _readMemberData( resultAll, flatten);
  }

  // recursively create the section (list of Range) array
  protected List makeSectionWithParents(List result, List orgSection, Variable v) throws InvalidRangeException {
    List section = orgSection;

    // do parent stuctures(s) first
    if (v.isMemberOfStructure())
      section = makeSectionWithParents(result, orgSection, v.getParentStructure());

    // process just this variable's subList
    result.addAll(makeSectionRanges(v, section));

    // return section with this variable's sublist removed
    return (orgSection == null) ? null : section.subList(v.getRank(), section.size());
  }

  /*************************************************************************/
  // scalar reading
  protected Index scalarIndex = new Index0D( new int[0]);

  /**
   * Get the value as a byte for a scalar Variable. May also be one-dimensional of length 1.
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException if data type not convertible to byte
   */
  public byte readScalarByte() throws IOException  {
    Array data = getScalarData();
    return data.getByte(scalarIndex);
  }

  /**
   * Get the value as a short for a scalar Variable.  May also be one-dimensional of length 1.
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable  or one-dimensional of length 1.
   * @throws ForbiddenConversionException if data type not convertible to short
   */
  public short readScalarShort() throws IOException  {
    Array data = getScalarData();
    return data.getShort(scalarIndex);
  }

  /**
   * Get the value as a int for a scalar Variable. May also be one-dimensional of length 1.
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException if data type not convertible to int
   */
  public int readScalarInt() throws IOException  {
    Array data = getScalarData();
    return data.getInt(scalarIndex);
  }

  /**
   * Get the value as a long for a scalar Variable.  May also be one-dimensional of length 1.
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable
   * @throws ForbiddenConversionException if data type not convertible to long
   */
  public long readScalarLong() throws IOException  {
    Array data = getScalarData();
    return data.getLong(scalarIndex);
  }

  /**
   * Get the value as a float for a scalar Variable.  May also be one-dimensional of length 1.
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException if data type not convertible to float
   */
  public float readScalarFloat() throws IOException  {
    Array data = getScalarData();
    return data.getFloat(scalarIndex);
  }

  /**
   * Get the value as a double for a scalar Variable.  May also be one-dimensional of length 1.
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException if data type not convertible to double
   */
  public double readScalarDouble() throws IOException  {
    Array data = getScalarData();
    return data.getDouble(scalarIndex);
  }

  /**
   * Get the value as a String for a scalar Variable.  May also be one-dimensional of length 1.
   * May also be one-dimensional of type CHAR, which wil be turned into a scalar String.
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar or one-dimensional.
   * @throws ClassCastException if data type not DataType.STRING or DataType.CHAR.
   */
  public String readScalarString() throws IOException  {
    Array data = getScalarData();
    if (dataType == DataType.STRING)
      return (String) data.getObject(scalarIndex);
    else if (dataType == DataType.CHAR) {
      ArrayChar dataC = (ArrayChar) data;
      return dataC.getString();
    } else
      throw new IllegalArgumentException("readScalarString not STRING or CHAR "+getName());
  }

  private Array getScalarData() throws IOException  {
    Array scalarData = (cache.data != null) ? cache.data : read();
    scalarData = scalarData.reduce();

    // LOOK isMember case
    if ((scalarData.getRank() == 0) || ((scalarData.getRank() == 1) && dataType == DataType.CHAR))
      return scalarData;
    throw new java.lang.UnsupportedOperationException("not a scalar variable ="+this);
  }

  ///////////////
  // internal reads: all other calls go through these.
  // subclasses must override, so that NetcdfDataset wrapping will work.

  // non-structure-member Variables.
  protected Array _read() throws IOException {
    if (cache.data != null) {
      if (debugCaching) System.out.println("got data from cache "+getName());
      return cache.data.copy();
    }
    Array data = null;
    try {
      Variable useVar = (orgVar != null) ? orgVar : this;
      data = ncfile.readData( useVar, useVar.getRanges());
    } catch (InvalidRangeException e) { } // cant happen

    if (isCaching()) {
      cache.data = data;
      if (debugCaching) System.out.println("cache "+getName());
      return cache.data.copy(); // dont let users get their nasty hands on cached data
    } else {
      return data;
    }
  }

  // section of non-structure-member Variable
  protected Array _read(List section) throws IOException, InvalidRangeException  {
    if (null == section)
      return _read();

    if (isCaching()) {
      Array data = (cache.data != null) ? cache.data : _read(); // read and cache entire array
      if (debugCaching) System.out.println("got data from cache "+getName());
      return data.sectionNoReduce( section).copy(); // subset it
    }

    Variable useVar = (orgVar != null) ? orgVar : this;
    String err = Range.checkInRange( section, useVar.getShape());
    if (err != null)
      throw new InvalidRangeException( err);

    // cant cache it
    return ncfile.readData( useVar, section);
  }

  // structure-member Variable;  section has a Range for each array in the parent
  // stuctures(s) and for the Variable.
  protected Array _readMemberData(List section, boolean flatten) throws IOException, InvalidRangeException  {
    // LOOK what about caching ??
    Variable useVar = (orgVar != null) ? orgVar : this;
    return ncfile.readMemberData(useVar, section, flatten);
  }


  /*******************************************/
  /** nicely formatted string representation */

  /** display name plus the dimensions */
  public void getNameAndDimensions(StringBuffer buf, boolean useFullName, boolean showDimLength) {
    buf.append(useFullName ? getName() : getShortName());
    if (getRank() > 0) buf.append("(");
    for (int i=0; i<dimensions.size(); i++) {
      Dimension myd = (Dimension) dimensions.get(i);
      if (i!=0)
        buf.append(", ");
      if (myd.isVariableLength()) {
        buf.append( "*" );
      } else if (myd.isShared()) {
        if (showDimLength)
          buf.append( myd.getName()+"="+myd.getLength() );
        else
          buf.append( myd.getName() );
      } else {
        if (myd.getName() != null)
          buf.append( myd.getName()+"=");
        buf.append( myd.getLength() );
      }
    }
    if (getRank() > 0) buf.append(")");
  }

  /** String representation of Variable and its attributes. */
  public String toString() {
    return writeCDL("   ", false, false);
  }

  /** String representation of a Variable and its attributes.
   *
   * @param indent start each line with this much space
   * @param useFullName use full name, else use short name
   * @param strict stictly comply with ncgen syntax
   * @return CDL representation of the Variable.
   */
  public String writeCDL(String indent, boolean useFullName, boolean strict) {
    StringBuffer buf = new StringBuffer();
    buf.setLength(0);
    buf.append(indent);
    buf.append(dataType.toString());
    buf.append(" ");
    getNameAndDimensions( buf, useFullName, !strict);
    buf.append(";");
    if (!strict) buf.append(extraInfo());
    buf.append("\n");

    Iterator iter = getAttributes().iterator();
    while (iter.hasNext()) {
      buf.append( indent + "  ");
      if (strict) buf.append( getName());
      buf.append( ":");
      Attribute att = (Attribute) iter.next();
      buf.append(att.toString());
      buf.append(";");
      if (!strict && (att.getDataType() != DataType.STRING))
          buf.append(" // "+att.getDataType());
      buf.append("\n");

    }
    return buf.toString();
  }

  /** String representation of Variable and its attributes. */
  public String toStringDebug() {
    return ncfile.toStringDebug(this);
  }

  private static boolean showSize = false;
  protected String extraInfo() { return showSize ? " // "+getElementSize() +" " + getSize() : ""; }

  /**
   * Instances which have same content are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if ( !(oo instanceof Variable)) return false;
    return hashCode() == oo.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37*result + getName().hashCode();
      if (isScalar()) result++;
      result = 37*result + getDataType().hashCode();
      //if (isMetadata()) result++;
      result = 37*result + getDimensions().hashCode();
      if (isSection) result++;
      result = 37*result + getParentGroup().hashCode();
      if (isVlen) result++;
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0;


  /////////////////////////////////////////////////////////////////////////////
  /** Create a Variable. Also must call setDataType() and setDimensions()
    * @param ncfile the containing NetcdfFile.
    * @param group the containing group; if null, use rootGroup
    * @param parentStructure the containing structure; may be null
    * @param shortName variable shortName.
    */
  public Variable(NetcdfFile ncfile, Group group, Structure parentStructure, String shortName) {
    this.ncfile = ncfile;
    this.group = (group == null) ? ncfile.getRootGroup() : group;
    this.parent = parentStructure;
    this.shortName = shortName;
  }

  /** Copy constructor */
  public Variable( Variable from) {
    this.attributes = new ArrayList( from.getAttributes());
    this.cache = from.cache; // share the cache
    this.dataType = from.getDataType();
    this.dimensions = new ArrayList( from.getDimensions());
    this.elementSize = from.getElementSize();
    this.group = from.group;
    this.isCoordinateAxis = from.isCoordinateAxis;
    this.isMetadata = from.isMetadata;
    this.isSection = from.isSection;
    this.isVlen = from.isVlen;
    this.ncfile = from.ncfile;
    this.orgVar = from;
    this.parent = from.parent;
    this.sectionRanges = from.sectionRanges;
    this.shape = from.getShape();
    this.shortName = from.shortName;
    this.spiObject = from.spiObject;
  }

  /** Set the data type */
  public void setDataType( DataType dataType) {
    this.dataType = dataType;
    this.elementSize = getDataType().getSize();
  }

  /** Set the short name */
  public void setName( String shortName) { this.shortName = shortName; }

  /** Set the parent structure. */
  public void setParentStructure(Structure parent) { this.parent = parent; }

  /** Set the parent group. */
  public void setParentGroup(Group group) {
    this.group = group;
 }

  /** Set the element size. Usually elementSize is determined by the dataType,
   *  use this only for exceptional cases.
   */
  public void setElementSize( int elementSize) { this.elementSize = elementSize; }
  protected ArrayList attributes() { return attributes; }

  /** Add new or replace old if has same name */
  public void addAttribute(Attribute att) {
    for (int i=0; i<attributes.size(); i++) {
      Attribute a = (Attribute) attributes.get(i);
      if (att.getName().equals(a.getName())) {
        attributes.set(i, att); // replace
        return;
      }
    }
    attributes.add( att);
  }

  /** Remove an Attribute : uses the attribute hashCode to find it.
   * @return true if was found and removed */
  public boolean remove( Attribute a) {
    if (a == null) return false;
    return attributes.remove( a);
  }

  /**
   * Set the shape with a list of Dimensions. The Dimensions may be shared or not.
   * Technically you can use Dimensions from any group; pragmatically you should only use
   *  Dimensions contained in the Variable's parent groups.
   * @param dims list of type ucar.nc2.Dimension
   */
  public void setDimensions(List dims) {
    this.dimensions = new ArrayList(dims);
    this.shape = new int[ dims.size()];
    for (int i=0; i<dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      shape[i] = dim.getLength();
      if (dim.isUnlimited() && (i != 0))
        throw new IllegalArgumentException("Unlimited dimension must be outermost");
      if (dim.isVariableLength()) {
        if (dims.size() != 1)
          throw new IllegalArgumentException("Unknown dimension can only be used in 1 dim array");
        else
          isVlen = true;
      }
    }
  }

  /**
   * Set the dimensions using the dimensions names. The dimension is searched for recursively in the parent groups.
   * @param dimString : whitespace seperated list of dimension names, or '*' for Dimension.UNKNOWN.
   */
  public void setDimensions(String dimString) {
    if (dimString == null) { // scalar
      this.shape = new int[0];
      return;
    }

    ArrayList dims = new ArrayList();
    StringTokenizer stoke = new StringTokenizer(dimString);
    while (stoke.hasMoreTokens()) {
      String dimName = stoke.nextToken();
      Dimension d = dimName.equals("*") ? Dimension.UNKNOWN : group.findDimension(dimName);
      if (d == null)
        throw new IllegalArgumentException("Variable "+getName()+" setDimensions = "+dimString+" FAILED, dim doesnt exist="+ dimName);
      dims.add( d);
    }
    setDimensions(dims);
  }

  /**
   * Set the dimensions using all anonymous (unshared) dimensions
   * @param shape defines the dimension lengths
   */
  public void setDimensionsAnonymous( int[] shape) {
    this.shape = (int []) shape.clone();
    for (int i=0; i<shape.length; i++) {
      Dimension anon = new Dimension(null, shape[i], false, false, false);
      dimensions.add( anon);
    }
  }

  /**
   * Replace a dimension with an equivalent one.
   * @param dim must have the same name, length as old one
   */
  public void replaceDimension( Dimension dim) {
    int idx = findDimensionIndex( dim.getName());
    if (idx >= 0)
      dimensions.set( idx, dim);
  }

  /**
   * Set a dimension with an equivalent one.
   * @param idx index into dimension array
   * @param dim to set
   */
  public void setDimension( int idx, Dimension dim) {
    dimensions.set( idx, dim);
  }

  // is this a coordinate variable ?
  protected void calcIsCoordinateVariable() {
    this.isCoordinateAxis = false;
    if (dataType == DataType.STRUCTURE) return;

    int n = getRank();
    if (n == 1 && dimensions.size() == 1) {
      Dimension firstd = (Dimension) dimensions.get(0);
      if (shortName.equals( firstd.getName())) { //  : short names match
        firstd.addCoordinateVariable( this);
        this.isCoordinateAxis = true;
      }
    }
    if (n == 2 && dimensions.size() == 2) {    // two dimensional
      Dimension firstd = (Dimension) dimensions.get(0);
      if (shortName.equals( firstd.getName()) &&  // short names match
          (getDataType() == DataType.CHAR)) {         // must be char valued (really a String)
        firstd.addCoordinateVariable( this);
        this.isCoordinateAxis = true;
      }
    }
  }

  /** true is its a 1D coordinate axis or variable for its dimension */
  public void setIsCoordinateAxis(Dimension dim) {
    isCoordinateAxis = true;
    dim.addCoordinateVariable( this);
  }

  // for IOServiceProvider
  private Object spiObject;
  /** should not be public */
  public Object getSPobject() { return spiObject; }
  /** should not be public */
  public void setSPobject( Object spiObject ) { this.spiObject = spiObject; }
  /** should not be public. */
  public Variable getIOVar() { return orgVar; }
  /** should not be public. */
  public void setIOVar( Variable orgVar) { // use this variable for IO
    this.ncfile = orgVar.ncfile;
    this.orgVar = orgVar;
  }


  ////////////////////////////////////////////////////////////////////////////////////
  // caching

  /** If total data is less than SizeToCache in bytes, then cache. */
  public int getSizeToCache() { return sizeToCache; }
  /** Set sizeToCache. */
  public void setSizeToCache( int sizeToCache) { this.sizeToCache = sizeToCache; }

  /**
   * Set whether to cache or not. Implies that the entire array will be stored, once read.
   * Normally this is set automatically based on size of data.
   * @param caching set if caching.
   */
  public void setCaching(boolean caching) {
    this.cache.isCaching = caching;
    this.cache.cachingSet = true;
  }

  /**
   * Will this Variable be cached when read.
   * Set externally, or calculated based on total size < sizeToCache.
   * @return true is caching
   */
  public boolean isCaching() {
    if (!this.cache.cachingSet) {
      if (isVlen) cache.isCaching = false;
      else cache.isCaching = getSize()*getElementSize() < sizeToCache;

      this.cache.cachingSet = true;
    }
    return cache.isCaching;
  }
  /** Invalidate the data cache */
  public void invalidateCache() { cache.data = null; }

  /** Set the data cache
   * @param isMetadata : synthesized data, set true if must be saved in NcML output (ie data not actually in the file).
   */
  public void setCachedData(Array cacheData, boolean isMetadata) {
    this.cache.data = cacheData;
    this.isMetadata = isMetadata;
    this.cache.cachingSet = true;
    this.cache.isCaching = true;
  }
  /** Does this have its data read in and cached? */
  public boolean hasCachedData() { return null != cache.data; }

  // this indirection allows us to share the cache among the variable's sections and copies
  static protected class Cache {
    public Array data;
    public boolean isCaching = false;
    public boolean cachingSet = false;
  }

}
