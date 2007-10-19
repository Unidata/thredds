/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.Index;

import java.util.List;
import java.util.Collections;
import java.util.Arrays;

/**
 * An Attribute has a name and a value, used for associating arbitrary metadata with a Variable or a Group.
 * The value can be a one dimensional array of Strings or numeric values.
 * <p/>
 * Attributes are immutable.
 *
 * @author caron
 */

public class Attribute {
  protected String name;
  protected DataType dataType;
  protected int nelems;
  protected Array values;

  /**
   * Get the name of this Attribute.
   * Attribute names are unique within a NetcdfFile's global set, and within a Variable's set.
   *
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the data type of the Attribute value.
   *
   * @return DataType
   */
  public DataType getDataType() {
    return dataType;
  }

  /**
   * True if value is a String or String[].
   *
   * @return if its a String.
   */
  public boolean isString() {
    return dataType == DataType.STRING;
  }

  /**
   * True if value is an array (getLength() > 1)
   *
   * @return if its an array.
   */
  public boolean isArray() {
    return (getLength() > 1);
  }

  /**
   * Get the length of the array of values; = 1 if scaler.
   *
   * @return number of elementss in the array.
   */
  public int getLength() {
    return nelems;
  }

  /**
   * Get the value as an Array.
   *
   * @return Array of values.
   */
  public Array getValues() {
    return values;
  }

  /**
   * Retrieve String value; only call if isString() is true.
   *
   * @return String if this is a String valued attribute, else null.
   * @see Attribute#isString
   */
  public String getStringValue() {
    return getStringValue(0);
  }

  /**
   * Retrieve ith String value; only call if isString() is true.
   *
   * @param index which index
   * @return ith String value (if this is a String valued attribute and index in range), else null.
   * @see Attribute#isString
   */
  public String getStringValue(int index) {
    if (!isString() || (index < 0) || (index >= nelems))
      return null;
    return (String) values.getObject(ima().set0(index));
  }

  /**
   * Retrieve numeric value.
   * Equivalent to <code>getNumericValue(0)</code>
   *
   * @return the first element of the value array, or null if its a String.
   */
  public Number getNumericValue() {
    return getNumericValue(0);
  }

  /// these deal with array-valued attributes

  /**
   * Retrieve a numeric value by index. If its a String, it will try to parse it as a double.
   *
   * @param index the index into the value array.
   * @return Number <code>value[index]</code>, or null if its a non-parsable String or
   *         the index is out of range.
   */
  public Number getNumericValue(int index) {
    if (isString() || (index < 0) || (index >= nelems))
      return null;

    if (dataType == DataType.STRING) {
      try {
        return new Double(getStringValue(index));
      }
      catch (NumberFormatException e) {
        return null;
      }
    }

    if (dataType == DataType.BYTE)
      return values.getByte(ima().set0(index));
    else if (dataType == DataType.SHORT)
      return values.getShort(ima().set0(index));
    else if (dataType == DataType.INT)
      return values.getInt(ima().set0(index));
    else if (dataType == DataType.FLOAT)
      return values.getFloat(ima().set0(index));
    else if (dataType == DataType.DOUBLE)
      return values.getDouble(ima().set0(index));
    else if (dataType == DataType.LONG)
      return values.getLong(ima().set0(index));

    return null;
  }

  /**
   * Instances which have same content are equal.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if ((o == null) || !(o instanceof Attribute)) return false;

    final Attribute attribute = (Attribute) o;

    if (!name.equals(attribute.name)) return false;
    if (nelems != attribute.nelems) return false;
    if (!dataType.equals(attribute.dataType)) return false;

    if (values != null) {
      for (int i = 0; i < getLength(); i++) {
        int r1 = isString() ? getStringValue(i).hashCode() : getNumericValue(i).hashCode();
        int r2 = attribute.isString() ? attribute.getStringValue(i).hashCode() : attribute.getNumericValue(i).hashCode();
        if (r1 != r2) return false;
      }
    }

    return true;
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getName().hashCode();
      result = 37 * result + getDataType().hashCode();
      for (int i = 0; i < getLength(); i++) {
        int h = isString() ? getStringValue(i).hashCode() : getNumericValue(i).hashCode();
        result = 37 * result + h;
      }
      hashCode = result;
    }
    return hashCode;
  }

  private volatile int hashCode = 0;

  /**
   * String representation
   */
  @Override
  public String toString() {
    StringBuffer buff = new StringBuffer();
    buff.append(getName());
    if (isString()) {
      buff.append(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0) buff.append(", ");
        buff.append("\"").append(getStringValue(i)).append("\"");
      }
    } else {
      buff.append(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0) buff.append(", ");
        buff.append(getNumericValue(i));
        if (dataType == DataType.FLOAT)
          buff.append("f");
        else if (dataType == DataType.SHORT)
          buff.append("s");
        else if (dataType == DataType.BYTE)
          buff.append("b");
      }
    }
    return buff.toString();
  }


  ///////////////////////////////////////////////////////////////////////////////

  /**
   * Copy constructor
   *
   * @param name name of Attribute
   * @param from copy value from here.
   */
  public Attribute(String name, Attribute from) {
    this.name = name;
    //validate(name);
    this.dataType = from.dataType;
    this.nelems = from.nelems;
    this.values = from.values;
    //this.ima = from.ima;
  }

  /**
   * Create a String-valued Attribute.
   *
   * @param name name of Attribute
   * @param val  value of Attribute
   */
  public Attribute(String name, String val) {
    this.name = name;
    //validate(name);
    setStringValue(val);
  }

  /**
   * Create a scalar numeric-valued Attribute.
   *
   * @param name name of Attribute
   * @param val  value of Attribute
   */
  public Attribute(String name, Number val) {
    this.name = name;
    validate(name);
    int[] shape = new int[1];
    shape[0] = 1;
    DataType dt = DataType.getType(val.getClass());
    Array vala = Array.factory(dt.getPrimitiveClassType(), shape);
    Index ima = vala.getIndex();
    vala.setDouble(ima.set0(0), val.doubleValue());
    setValues(vala);
  }

  /**
   * Construct attribute with Array of values.
   *
   * @param name   name of attribute
   * @param values array of values.
   */
  public Attribute(String name, Array values) {
    this.name = name;
    //validate(name);
    setValues(values);
  }

  /**
   * Construct attribute with list of String values.
   *
   * @param name   name of attribute
   * @param values list of values.
   */
  public Attribute(String name, List<String> values) {
    this.name = name;
    int n = values.size();
    String[] va = new String[n];
    int count = 0;
    for (String v : values) va[count++] = v;
    setValues( Array.factory(String.class, new int[] {n}, va));
  }


  /**
   * A copy constructor using a ucar.unidata.util.Parameter.
   * Need to do this so ucar.unidata.geoloc package doesnt depend on ucar.nc2 library
   *
   * @param param copy info from here.
   */
  public Attribute(ucar.unidata.util.Parameter param) {
    this.name = param.getName();
    validate(name);

    if (param.isString()) {
      setStringValue(param.getStringValue());

    } else {
      double[] values = param.getNumericValues();
      int n = values.length;
      Array vala = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[]{n}, values);
      setValues(vala);
    }
  }

  //////////////////////////////////////////
  // the following make this mutable, but its restricted to subclasses


  /**
   * Constructor. Must also set value
   *
   * @param name name of Attribute
   */
  protected Attribute(String name) {
    this(name, true);
  }

  /**
   * Constructor. Must also set value
   *
   * @param name     name of Attribute
   * @param validate whether to validate the name.
   */
  protected Attribute(String name, boolean validate) {
    this.name = name;
    if (validate) validate(name);
  }

  private void validate(String name) {
    if (!NetcdfFile.isValidNetcdfObjectName(name))
      throw new IllegalArgumentException("Illegal Netcdf Object Name = '" + name + "', should be " +
              NetcdfFile.getValidNetcdfObjectNamePattern());
  }


  /**
   * Set the name of the Attribute.
   * @param name name of Attribute
   *
  private void setName(String name) {
    this.name = name;
    //validate(name);
  } */

  /**
   * Create an Attribute with a String or a 1-dimensional array of primitives.
   *
   * @param val value of Attribute
   * @deprecated use setValues( Array val)
   *
  private void setValueOld(Object val) {
    if (val instanceof String)
      setStringValue((String) val);
    else {
      int n = java.lang.reflect.Array.getLength(val);
      int[] shape = new int[]{n};
      Class elemType = val.getClass().getComponentType();
      Array vala = Array.factory(elemType, shape, val);
      setValues(vala);
    }
    hashCode = 0;
  } */

  /**
   * set the value as a String, trimming trailing zeroes
   *
   * @param val value of Attribute
   */
  private void setStringValue(String val) {
    // get rid of trailing zeroes
    int len = val.length();
    while ((len > 0) && (val.charAt(len - 1) == 0))
      len--;

    if (len != val.length())
      val = val.substring(0, len);
    values = Array.factory(String.class, new int[]{1});
    values.setObject(values.getIndex(), val);
    setValues(values);
  }

  /**
   * set the values from an Array
   *
   * @param arr value of Attribute
   */
  protected void setValues(Array arr) {
    if (DataType.getType(arr.getElementType()) == null)
      throw new IllegalArgumentException("Cant set Attribute with type " + arr.getElementType());

    if (arr.getElementType() == char.class) { // turn CHAR into STRING
      ArrayChar carr = (ArrayChar) arr;
      arr = carr.make1DStringArray();
    }
    if (arr.getRank() != 1)
      arr = arr.reshape(new int[]{(int) arr.getSize()}); // make sure 1D

    this.values = arr;
    this.nelems = (int) arr.getSize();
    this.dataType = DataType.getType(arr.getElementType());
    //ima = values.getIndex();
    hashCode = 0;
  }

  protected Index ima;
  private Index ima() {
    if (ima == null) ima = values.getIndex();
    return ima;
  }

}