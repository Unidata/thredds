// $Id:NetcdfFileWriteable.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
 * Create/Write netCDF files. <p>
 * Because of the limitations of the underlying implementation, netcdf
 * files can only have Dimensions, Attributes and Variables added to it
 * at creation time. Thus, when a file is first opened, it in is "define mode"
 * where these may added. Once create() is called, you can no longer add, delete, or modify
 * the Dimensions, Attributes or Variables. <p>
 * After create has been called you can then write the Variables' data values.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 * @see NetcdfFile
 */

public class NetcdfFileWriteable extends NetcdfFile {
  private HashMap varHash = new HashMap(50);
  private boolean defineMode;
  private boolean fill = false;
  private ucar.nc2.IOServiceProviderWriter spiw;

  /**
   * Open an existing Netcdf file for writing data. Fill mode is true.
   * Cannot add new objects, you can only read/write data to existing Variables.
   *
   * @param location name of existing file to open.
   */
  static public NetcdfFileWriteable openExisting(String location) throws IOException {
    return openExisting( location, true);
  }

  /**
   * Open an existing Netcdf file for writing data.
   * Cannot add new objects, you can only read/write data to existing Variables.
   *
   * @param location name of existing file to open.
   * @param fill     if true, the data is first written with fill values.
   */
  static public NetcdfFileWriteable openExisting(String location, boolean fill) throws IOException {
    NetcdfFileWriteable result = new NetcdfFileWriteable(location);
    result.setFill(fill);
    return result;
  }

  /**
    * Create a new Netcdf file, with fill mode true.
    * @param location name of new file to open; if it exists, will overwrite it.
    */
   static public NetcdfFileWriteable createNew(String location) {
     return createNew(location, true);
   }

  /**
    * Create a new Netcdf file, put it into define mode. Make calls to addXXX(), then
    * when all objects are added, call create(). You cannot read or write data until create() is called.
    *
    * @param location name of new file to open; if it exists, will overwrite it.
    * @param fill     if true, the data is first written with fill values.
    *                 Leave false if you expect to write all data values, set to true if you want to be
    *                 sure that unwritten data values have the fill value in it. (default is false)
    */
   static public NetcdfFileWriteable createNew(String location, boolean fill) {
     return new NetcdfFileWriteable(location, fill);
   }

   /**
   * Create a new Netcdf file, put it into define mode.
   *
   * @deprecated use createNew(String filename, boolean fill)
   */
  public NetcdfFileWriteable(String location, boolean fill) {
    super();
    this.location = location;
    this.fill = fill;
    defineMode = true;
  }

  /**
   * Open a new Netcdf file, put it into define mode.
   *
   * @deprecated use createNew(String filename, boolean fill)
   */
  public NetcdfFileWriteable() {
    super();
    defineMode = true;
  }

  /**
   * Open an existing Netcdf file for writing data.
   *
   * @deprecated use openExisting(String filename, boolean fill)
   */
  public NetcdfFileWriteable(String location) throws IOException {
    super();
    this.location = location;
    ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(location, "rw");
    spi = SPFactory.getServiceProvider();
    spiw = (ucar.nc2.IOServiceProviderWriter) spi;
    spiw.open(raf, this, null);
    defineMode = false;
  }

  /**
   * Set the filename of a new file to be created: call before calling create().
   *
   * @param filename name of new file to create.
   * @deprecated use NetcdfFileWriteable(String filename);
   */
  public void setName(String filename) {
    this.location = filename;
  }

  /**
   * Set the fill flag: call before calling create().
   * If true, the data is first written with fill values.
   * Default is fill = false.
   * Leave false if you expect to write all data values, set to true if you want to be
   * sure that unwritten data values have the fill value in it.
   *
   * @param fill set fill mode true or false
   * @deprecated use openExisting(String filename, boolean fill) or createNew(String filename, boolean fill)
   */
  public void setFill(boolean fill) {
    this.fill = fill;
  }

  ////////////////////////////////////////////
  //// use these calls in define mode

  /**
   * Add a Dimension to the file. Must be in define mode.
   *
   * @param dimName name of dimension
   * @param length  size of dimension.
   * @return the created dimension
   */
  public Dimension addDimension(String dimName, int length) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    if (length <= 0) throw new IllegalArgumentException("length must be > 0");
    Dimension dim = new Dimension(dimName, length, true, false, false);
    super.addDimension(null, dim);
    return dim;
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
  public Dimension addDimension(String dimName, int length, boolean isShared, boolean isUnlimited, boolean isVariableLength) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    Dimension dim = new Dimension(dimName, length, isShared, isUnlimited, isVariableLength);
    super.addDimension(null, dim);
    return dim;
  }

  /**
   * Add an unlimited Dimension to the file. Must be in define mode.
   *
   * @param dimName name of unlimited dimension
   * @return the created dimension
   */
  public Dimension addUnlimitedDimension(String dimName) {
    return addDimension(dimName, Dimension.UNLIMITED.getLength(), true, true, false);
  }

  /**
   * Add a Global attribute to the file. Must be in define mode.
   *
   * @param att the attribute.
   */
  public void addGlobalAttribute(Attribute att) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    super.addAttribute(null, att);
  }

  /**
   * Add a Global attribute of type String to the file. Must be in define mode.
   *
   * @param name  name of attribute.
   * @param value value of atribute.
   */
  public void addGlobalAttribute(String name, String value) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    super.addAttribute(null, new Attribute(name, value));
  }

  /**
   * Add a Global attribute of type Number to the file. Must be in define mode.
   *
   * @param name  name of attribute.
   * @param value must be of type Float, Double, Integer, Short or Byte
   */
  public void addGlobalAttribute(String name, Number value) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    super.addAttribute(null, new Attribute(name, value));
  }

  /**
   * Add a Global attribute of type Array to the file. Must be in define mode.
   *
   * @param name   name of attribute.
   * @param values Array of values
   */
  public void addGlobalAttribute(String name, Array values) {
    if (!defineMode) throw new UnsupportedOperationException("not in define mode");
    super.addAttribute(null, new Attribute(name, values));
  }
  /**
   * Add a Global attribute of type Array to the file. Must be in define mode.
   * @param name name of attribute.
   * @param value must be 1D array of double, float, int, short, char, or byte
   * @deprecated use addGlobalAttribute(String name, Array value);
   *
  public void addGlobalAttribute(String name, Object value) {
  if (!defineMode)
  throw new UnsupportedOperationException("not in define mode");
  Attribute att = new Attribute(name);
  att.setValueOld( value);
  super.addGlobalAttribute( att);
  } */

  /**
   * Add a variable to the file. Must be in define mode.
   *
   * @param varName       name of Variable, must be unique with the file.
   * @param componentType type of underlying element: String, double or Double, etc.
   * @param dims          array of Dimensions for the variable, must already have been added.
   * @deprecated use addVariable(String varName, DataType dataType, ArrayList dims);
   */
  public Variable addVariable(String varName, Class componentType, Dimension[] dims) {
    ArrayList list = new ArrayList();
    for (int i = 0; i < dims.length; i++)
      list.add(dims[i]);

    return addVariable(varName, DataType.getType(componentType), list);
  }

  /**
   * Add a variable to the file. Must be in define mode.
   *
   * @param varName  name of Variable, must be unique with the file.
   * @param dataType type of underlying element
   * @param dims     array of Dimensions for the variable, must already have been added. Use an array of length 0
   *                 for a scalar variable.
   */
  public Variable addVariable(String varName, DataType dataType, Dimension[] dims) {
    ArrayList list = new ArrayList();
    for (int i = 0; i < dims.length; i++)
      list.add(dims[i]);

    return addVariable(varName, dataType, list);
  }

  /**
   * Add a variable to the file. Must be in define mode.
   *
   * @param varName  name of Variable, must be unique with the file.
   * @param dataType type of underlying element
   * @param dims     names of Dimensions for the variable, blank seperated.
   *    Must already have been added. Use an empty string for a scalar variable.
   */
  public Variable addVariable(String varName, DataType dataType, String dims) {
    // parse the list
    ArrayList list = new ArrayList();
    StringTokenizer stoker = new StringTokenizer( dims);
    while (stoker.hasMoreTokens()) {
      String tok = stoker.nextToken();
      Dimension d = rootGroup.findDimension(tok);
      if (null == d)
        throw new IllegalArgumentException("Canat find dimension "+tok);
      list.add(d);
    }

    return addVariable( varName, dataType, list);
  }

  /**
   * Add a variable to the file. Must be in define mode.
   * If you use DataType = String, then a new dimension with name varName_strlen in automatically added.
   * You should use writeData(), then the length will be calculated.
   *
   * @param varName  name of Variable, must be unique with the file.
   * @param dataType type of underlying element
   * @param dims     list of Dimensions for the variable, must already have been added. Use a list of length 0
   *                 for a scalar variable.
   */
  public Variable addVariable(String varName, DataType dataType, List dims) {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");

    Variable v = new Variable(this, rootGroup, null, varName);

    if (dataType == DataType.STRING) {
      dataType = DataType.CHAR;
      Dimension d = addDimension(varName+"_strlen", 1);
      ArrayList sdims = new ArrayList( dims);
      sdims.add(d);
      dims = sdims;
    }

    v.setDataType(dataType);
    v.setDimensions(dims);
    varHash.put(varName, v);

    super.addVariable(null, v);
    return v;
  }

  /**
   * Add an attribute to the named Variable. Must be in define mode.
   *
   * @param varName name of variable. must already have been added to the file.
   * @param att     Attribute to add.
   */
  public void addVariableAttribute(String varName, Attribute att) {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");

    Variable v = (Variable) varHash.get(varName);
    if (null == v)
      throw new IllegalArgumentException("addVariableAttribute variable name not found = <" + varName + ">");
    v.addAttribute(att);
  }

  /**
   * Add an attribute of type String to the named Variable. Must be in define mode.
   *
   * @param varName name of variable. must already have been added to the file.
   * @param attName name of attribute.
   * @param value   String value of atribute.
   */
  public void addVariableAttribute(String varName, String attName, String value) {
    addVariableAttribute(varName, new Attribute(attName, value));
  }


  /**
   * Add an attribute of type Number to the named Variable. Must be in define mode.
   *
   * @param varName name of attribute. IllegalArgumentException if not valid name.
   * @param attName name of attribute.
   * @param value   must be of type Float, Double, Integer, Short or Byte
   */
  public void addVariableAttribute(String varName, String attName, Number value) {
    addVariableAttribute(varName, new Attribute(attName, value));
  }

  /**
   * Add an attribute of type Array to the named Variable. Must be in define mode.
   *
   * @param varName name of attribute. IllegalArgumentException if not valid name.
   * @param attName name of attribute.
   * @param value   Array of valkues
   */
  public void addVariableAttribute(String varName, String attName, Array value) {
    Attribute att = new Attribute(attName);
    att.setValues(value);
    addVariableAttribute(varName, att);
  }

  /*
   * Add an attribute of type Array to the named Variable. Must be in define mode.
   *
   * @param varName name of attribute. IllegalArgumentException if not valid name.
   * @param attName name of attribute.
   * @param value must be 1D array of double, float, int, short, char, or byte
   * @deprecated use addVariableAttribute(String varName, String attName, Array value);
   *
  public void addVariableAttribute(String varName, String attName, Object value) {
    Attribute att = new Attribute(attName);
    att.setValueOld( value);
    addVariableAttribute( varName, att);
  } */

  /**
   * After you have added all of the Dimensions, Variables, and Attributes,
   * call create() to actually create the file. You must be in define mode.
   * After this call, you are no longer in define mode, and cannot return to it.
   */
  public void create() throws java.io.IOException {
    if (!defineMode)
      throw new UnsupportedOperationException("not in define mode");

    spi = SPFactory.getServiceProvider();
    spiw = (ucar.nc2.IOServiceProviderWriter) spi;
    spiw.create(location, this, fill);

    defineMode = false;
  }

  ////////////////////////////////////////////
  //// use these calls to write to the file

  /**
   * Write data to the named variable, origin assumed to be 0. Must not be in define mode.
   *
   * @param varName name of variable. IllegalArgumentException if variable name does not exist.
   * @param values  write this array; must be same type and rank as Variable
   * @throws IOException
   */
  public void write(String varName, Array values) throws java.io.IOException, InvalidRangeException {
    if (values.getElementType() == String.class) {
      ArrayChar cvalues =  ArrayChar.makeFromStringArray((ArrayObject) values);

      // set the string dimension length - really its private to the variable
      int[] shape = cvalues.getShape();
      int strlen = shape[ cvalues.getRank()-1];
      Dimension d = findDimension(varName+"_strlen");
      d.setLength( strlen);

      ucar.nc2.Variable v2 = findVariable(varName);
      if (v2 == null)
        throw new IllegalArgumentException("NetcdfFileWriteable.write illegal variable name = " + varName);
      v2.
      write(varName, new int[ cvalues.getRank()], cvalues);

    } else {
      write(varName, new int[ values.getRank()], values);
    }
  }

  /**
   * Write data to the named variable. Must not be in define mode.
   *
   * @param varName name of variable. IllegalArgumentException if variable name does not exist.
   * @param origin  offset within the variable to start writing.
   * @param values  write this array; must be same type and rank as Variable
   * @throws IOException
   */
  public void write(String varName, int [] origin, Array values) throws java.io.IOException, InvalidRangeException {
    if (defineMode)
      throw new UnsupportedOperationException("in define mode");
    ucar.nc2.Variable v2 = findVariable(varName);
    if (v2 == null)
      throw new IllegalArgumentException("NetcdfFileWriteable.write illegal variable name = " + varName);

    spiw.writeData(v2, Range.factory(origin, values.getShape()), values);
    v2.invalidateCache();
  }

  /**
   * Flush anything written to disk.
   */
  public void flush() throws java.io.IOException {
    spiw.flush();
  }

  /**
   * close the file.
   */
  public synchronized void close() throws java.io.IOException {
    flush();
    spiw.close();
    isClosed = true;
  }

}