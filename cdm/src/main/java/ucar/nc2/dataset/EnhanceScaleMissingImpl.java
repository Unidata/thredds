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
package ucar.nc2.dataset;

import ucar.ma2.*;
import ucar.nc2.*;

import java.util.EnumSet;

/**
 * Implementation of EnhanceScaleMissing for missing data, unsigned, and scale/offset packed data.
 *
 * @author caron
 * @see EnhanceScaleMissing
 */
class EnhanceScaleMissingImpl implements EnhanceScaleMissing {
  // Default fill values, used unless _FillValue variable attribute is set.
  // duplicated from N3iosp, used in getFillValue.
  static private final byte NC_FILL_BYTE = -127;
  static private final char NC_FILL_CHAR = 0;
  static private final short NC_FILL_SHORT = (short) -32767;
  static private final int NC_FILL_INT = -2147483647;
  static private final float NC_FILL_FLOAT = 9.9692099683868690e+36f; /* near 15 * 2^119 */
  static private final double NC_FILL_DOUBLE = 9.9692099683868690e+36;
  static private final String FillValue = "_FillValue";

  static private boolean debug = false, debugRead = false, debugMissing = false;

  private DataType convertedDataType = null;
  private boolean useNaNs = false;

  // defaults from NetcdfDataset modes
  private boolean invalidDataIsMissing = NetcdfDataset.invalidDataIsMissing;
  private boolean fillValueIsMissing = NetcdfDataset.fillValueIsMissing;
  private boolean missingDataIsMissing = NetcdfDataset.missingDataIsMissing;

  private boolean hasScaleOffset = false;
  private double scale = 1.0, offset = 0.0;

  private boolean hasValidRange = false, hasValidMin = false, hasValidMax = false;
  private double valid_min = -Double.MAX_VALUE, valid_max = Double.MAX_VALUE;

  private boolean hasFillValue = false;
  private double fillValue;

  private boolean hasMissingValue = false;
  private double[] missingValue;

  private boolean isUnsigned;


  /**
   * Constructor, when you dont want anything done.
   */
  public EnhanceScaleMissingImpl() {
  }

  /**
   * Constructor, default values.
   *
   * @param forVar the Variable to decorate.
   */
  public EnhanceScaleMissingImpl(VariableDS forVar) {
    this(forVar, NetcdfDataset.useNaNs, NetcdfDataset.fillValueIsMissing,
        NetcdfDataset.invalidDataIsMissing, NetcdfDataset.missingDataIsMissing);
  }

  /**
   * Constructor.
   * If scale/offset attributes are found, remove them from the decorated variable.
   *
   * @param forVar               the Variable to decorate.
   * @param useNaNs              pre-fill isMissing() data with NaNs
   * @param fillValueIsMissing   use _FillValue for isMissing()
   * @param invalidDataIsMissing use valid_range for isMissing()
   * @param missingDataIsMissing use missing_value for isMissing()
   */
  public EnhanceScaleMissingImpl(VariableDS forVar, boolean useNaNs, boolean fillValueIsMissing,
                                 boolean invalidDataIsMissing, boolean missingDataIsMissing) {

    this.fillValueIsMissing = fillValueIsMissing;
    this.invalidDataIsMissing = invalidDataIsMissing;
    this.missingDataIsMissing = missingDataIsMissing;

    // see if underlying variable has scale/offset already applied
    Variable orgVar = forVar.getOriginalVariable();
    if (orgVar instanceof VariableDS) {
      VariableDS orgVarDS = (VariableDS) orgVar;
      EnumSet<NetcdfDataset.Enhance> orgEnhanceMode = orgVarDS.getEnhanceMode();
      if ((orgEnhanceMode != null) && orgEnhanceMode.contains(NetcdfDataset.Enhance.ScaleMissing))
        return;
    }

    // the other possibility is that you want to apply scale and offset to a signed value, then declare the result unsigned
    // this.isUnsigned = (orgVar != null) ? orgVar.isUnsigned() : forVar.isUnsigned();
    this.isUnsigned = forVar.isUnsigned();
    this.convertedDataType = forVar.getDataType();

    DataType scaleType = null, missType = null, validType = null, fillType = null;
    if (debug) System.out.println("EnhancementsImpl for Variable = " + forVar.getName());
    Attribute att;

    // scale and offset
    if (null != (att = forVar.findAttribute("scale_factor"))) {
      if (!att.isString()) {
        scale = att.getNumericValue().doubleValue();
        hasScaleOffset = true;
        scaleType = att.getDataType();
        forVar.remove(att);
        if (debug) System.out.println("scale = " + scale + " type " + scaleType);
      }
    }
    if (null != (att = forVar.findAttribute("add_offset"))) {
      if (!att.isString()) {
        offset = att.getNumericValue().doubleValue();
        hasScaleOffset = true;
        DataType offType = att.getDataType();
        if (rank(offType) > rank(scaleType))
          scaleType = offType;
        forVar.remove(att);
        if (debug) System.out.println("offset = " + offset);
      }
    }

    ////// missing data : valid_range. assume here its in units of unpacked data. correct this below
    Attribute validRangeAtt = null;
    if (null != (validRangeAtt = forVar.findAttribute("valid_range"))) {
      if (!validRangeAtt.isString() && validRangeAtt.getLength() > 1) {
        valid_min = validRangeAtt.getNumericValue(0).doubleValue();
        valid_max = validRangeAtt.getNumericValue(1).doubleValue();
        hasValidRange = true;
        validType = validRangeAtt.getDataType();
        if (hasScaleOffset) forVar.remove(validRangeAtt);
        if (debug) System.out.println("valid_range = " + valid_min + " " + valid_max);
      }
    }

    Attribute validMinAtt = null, validMaxAtt = null;
    if (!hasValidRange) {
      if (null != (validMinAtt = forVar.findAttribute("valid_min"))) {
        if (!validMinAtt.isString()) {
          valid_min = validMinAtt.getNumericValue().doubleValue();
          hasValidMin = true;
          validType = validMinAtt.getDataType();
          if (hasScaleOffset) forVar.remove(validMinAtt);
          if (debug) System.out.println("valid_min = " + valid_min);
        }
      }

      if (null != (validMaxAtt = forVar.findAttribute("valid_max"))) {
        if (!validMaxAtt.isString()) {
          valid_max = validMaxAtt.getNumericValue().doubleValue();
          hasValidMax = true;
          DataType t = validMaxAtt.getDataType();
          if (rank(t) > rank(validType))
            validType = t;
          if (hasScaleOffset) forVar.remove(validMaxAtt);
          if (debug) System.out.println("valid_min = " + valid_max);
        }
      }
    }
    boolean hasValidData = hasValidMin || hasValidMax || hasValidRange;
    if (hasValidMin && hasValidMax)
      hasValidRange = true;

    /// _FillValue
    if ((null != (att = forVar.findAttribute("_FillValue"))) && !att.isString()) {
      double[] values = getValueAsDouble(att);
      fillValue = values[0];
      hasFillValue = true;
      fillType = att.getDataType();
      if (hasScaleOffset) forVar.remove(att);
      if (debug) System.out.println("missing_datum from _FillValue = " + fillValue);
    }

    /// missing_value
    if (null != (att = forVar.findAttribute("missing_value"))) {
      String svalue = att.getStringValue();
      if (att.isString()) {
        if (forVar.getDataType() == DataType.CHAR) {
          missingValue = new double[1];
          if (svalue.length() == 0) missingValue[0] = 0;
          else missingValue[0] = svalue.charAt(0);

          missType = DataType.CHAR;
          hasMissingValue = true;

        } else {  // not a CHAR - try to fix problem where they use a numeric value as a String attribute

          try {
            missingValue = new double[1];
            missingValue[0] = Double.parseDouble(svalue);
            missType = att.getDataType();
            hasMissingValue = true;
          } catch (NumberFormatException ex) {
            if (debug) System.out.println("String missing_value not parsable as double= " + att.getStringValue());
          }
        }

      } else { // not a string
        missingValue = getValueAsDouble(att);
        missType = att.getDataType();
        hasMissingValue = true;
      }
      if (hasScaleOffset) forVar.remove(att);
    }

    // missing
    boolean hasMissing = (invalidDataIsMissing && hasValidData) ||
        (fillValueIsMissing && hasFillValue) ||
        (missingDataIsMissing && hasMissingValue);

    /// assign convertedDataType if needed
    if (hasScaleOffset) {

      convertedDataType = forVar.getDataType();
      if (hasMissing) {
        // has missing data : must be float or double
        if (rank(scaleType) > rank(convertedDataType))
          convertedDataType = scaleType;
        if (missingDataIsMissing && rank(missType) > rank(convertedDataType))
          convertedDataType = missType;
        if (fillValueIsMissing && rank(fillType) > rank(convertedDataType))
          convertedDataType = fillType;
        if (invalidDataIsMissing && rank(validType) > rank(convertedDataType))
          convertedDataType = validType;
        if (rank(convertedDataType) < rank(DataType.DOUBLE))
          convertedDataType = DataType.FLOAT;

      } else {
        // no missing data; can use wider of data and scale
        if (rank(scaleType) > rank(convertedDataType))
          convertedDataType = scaleType;
      }
      if (debug) System.out.println("assign dataType = " + convertedDataType);

      // validData may be external or internal
      if (hasValidData) {
        DataType orgType = forVar.getDataType();

        // If valid_range is the same type as scale_factor (actually the wider of
        // scale_factor and add_offset) and this is wider than the external data, then it
        // will be interpreted as being in the units of the internal (unpacked) data.
        // Otherwise it is in the units of the external (unpacked) data.
        // we assumed unpacked data above, redo if its realy packed data
        if (!((rank(validType) == rank(scaleType)) && (rank(scaleType) >= rank(orgType)))) {
          if (validRangeAtt != null) {
            double[] values = getValueAsDouble(validRangeAtt);
            valid_min = values[0];
            valid_max = values[1];
          } else {
            if (validMinAtt != null) {
              double[] values = getValueAsDouble(validMinAtt);
              valid_min = values[0];
            }
            if (validMaxAtt != null) {
              double[] values = getValueAsDouble(validMaxAtt);
              valid_max = values[0];
            }
          }
        }
      }
    }

    if (hasMissing && ((convertedDataType == DataType.DOUBLE) || (convertedDataType == DataType.FLOAT)))
      this.useNaNs = useNaNs;
    if (debug) System.out.println("this.useNaNs = " + this.useNaNs);
  }

  private double[] getValueAsDouble(Attribute att) {
    int n = att.getLength();
    double[] value = new double[n];

    if (debugMissing) System.out.printf("missing_data: ");
    for (int i=0; i<n; i++) {
      if (isUnsigned && att.getDataType() == DataType.BYTE)
        value[i] = convertScaleOffsetMissing( att.getNumericValue(i).byteValue());
      else if (isUnsigned && att.getDataType() == DataType.SHORT)
        value[i] = convertScaleOffsetMissing( att.getNumericValue(i).shortValue());
      else if (isUnsigned && att.getDataType() == DataType.INT)
        value[i] = convertScaleOffsetMissing( att.getNumericValue(i).intValue());
      else
        value[i] = scale * att.getNumericValue(i).doubleValue() + offset;
      if (debugMissing) System.out.print(" " + value[i]);
    }
    if (debugMissing) System.out.println();
    return value;
  }

  private int rank(DataType c) {
    if (c == DataType.BYTE)
      return 0;
    else if (c == DataType.SHORT)
      return 1;
    else if (c == DataType.INT)
      return 2;
    else if (c == DataType.LONG)
      return 3;
    else if (c == DataType.FLOAT)
      return 4;
    else if (c == DataType.DOUBLE)
      return 5;
    else
      return -1;
  }

  /**
   * @return converted DataType, else null if hasScaleOffset is true.
   */
  public DataType getConvertedDataType() {
    return convertedDataType;
  }

  /**
   * true if Variable has valid_range, valid_min or valid_max attributes
   */
  public boolean hasInvalidData() {
    return hasValidRange || hasValidMin || hasValidMax;
  }

  /**
   * return the minimum value in the valid range
   */
  public double getValidMin() {
    return valid_min;
  }

  /**
   * return the maximum value in the valid range
   */
  public double getValidMax() {
    return valid_max;
  }

  /**
   * return true if val is outside the valid range
   */
  public boolean isInvalidData(double val) {
    if (hasValidRange)
      return ((val < valid_min) || (val > valid_max));
    else if (hasValidMin)
      return (val < valid_min);
    else if (hasValidMax)
      return (val > valid_max);
    return false;
  }

  /**
   * true if Variable has _FillValue attribute
   */
  public boolean hasFillValue() {
    return hasFillValue;
  }

  /**
   * return true if val equals the _FillValue
   */
  public boolean isFillValue(double val) {
    return hasFillValue && (val == fillValue);
  }

  /**
   * true if Variable data will be converted using scale and offet
   */
  public boolean hasScaleOffset() {
    return hasScaleOffset;
  }

  /**
   * true if Variable has missing_value attribute
   */
  public boolean hasMissingValue() {
    return hasMissingValue;
  }

  /**
   * return true if val equals a missing_value (low level)
   */
  public boolean isMissingValue(double val) {
    if (!hasMissingValue)
      return false;
    for (int i = 0; i < missingValue.length; i++)
      if (ucar.nc2.util.Misc.closeEnough(val, missingValue[i]))
        return true;
    return false;
  }

  /**
   * set whether to use NaNs for missing values, for efficiency
   */
  public void setUseNaNs(boolean useNaNs) {
    this.useNaNs = useNaNs;
  }

  /**
   * @return whether to use NaNs for missing values (for efficiency)
   */
  public boolean getUseNaNs() {
    return useNaNs;
  }

  /**
   * set if _FillValue is considered isMissing(); better set in constructor if possible
   */
  public void setFillValueIsMissing(boolean b) {
    this.fillValueIsMissing = b;
  }

  /**
   * set if valid_range is considered isMissing(); better set in constructor if possible
   */
  public void setInvalidDataIsMissing(boolean b) {
    this.invalidDataIsMissing = b;
  }

  /**
   * set if missing_data is considered isMissing(); better set in constructor if possible
   */
  public void setMissingDataIsMissing(boolean b) {
    this.missingDataIsMissing = b;
  }

  /**
   * true if Variable has missing data values
   */
  public boolean hasMissing() {
    return (invalidDataIsMissing && hasInvalidData()) ||
        (fillValueIsMissing && hasFillValue()) ||
        (missingDataIsMissing && hasMissingValue());
  }

  /**
   * Is this a missing value ?
   * @param val check this value
   * @return true if missing
   */
  public boolean isMissing(double val) {
    if (Double.isNaN(val)) return true;
    if (!hasMissing()) return false;
    return isMissing_(val);
  }

   /**
   * Optimize "Is this a missing value"? Assumes NaNs have already been set if its missing.
   * @param val check this value
   * @return true if missing
   */
  public boolean isMissingFast( double val) {
    if (useNaNs) return Double.isNaN(val); // no need to check again
    if (Double.isNaN(val)) return true;
    if (!hasMissing()) return false;
    return isMissing_(val);
  }

  // find data values that match a missing value
  /* assumes that hasMissing() == true
  private final boolean isMissing_(double val) {
    return (invalidDataIsMissing && isInvalidData(val)) ||
        (fillValueIsMissing && isFillValue(val)) ||
        (missingDataIsMissing && isMissingValue(val));
  } */

  private final  boolean isMissing_(double val) {
    if (missingDataIsMissing && hasMissingValue && isMissingValue(val))
      return true;
    if (fillValueIsMissing && hasFillValue && isFillValue(val))
      return true;
    if (invalidDataIsMissing)
      return isInvalidData(val);
    return false;
  }

  public Object getFillValue(DataType dt) {
    DataType useType = convertedDataType == null ? dt : convertedDataType;
    if ((useType == DataType.BYTE) || (useType == DataType.ENUM1)) {
      byte[] result = new byte[1];
      result[0] = hasFillValue ? (byte) fillValue : NC_FILL_BYTE;
      return result;
    } else if (useType == DataType.BOOLEAN) {
      boolean[] result = new boolean[1];
      result[0] = false;
      return result;
    } else if (useType == DataType.CHAR) {
      char[] result = new char[1];
      result[0] = hasFillValue ? (char) fillValue : NC_FILL_CHAR;
      return result;
    } else if ((useType == DataType.SHORT) || (useType == DataType.ENUM2)) {
      short[] result = new short[1];
      result[0] = hasFillValue ? (short) fillValue : NC_FILL_SHORT;
      return result;
    } else if ((useType == DataType.INT) || (useType == DataType.ENUM4)) {
      int[] result = new int[1];
      result[0] = hasFillValue ? (int) fillValue : NC_FILL_INT;
      return result;
    } else if (useType == DataType.LONG) {
      long[] result = new long[1];
      result[0] = hasFillValue ? (long) fillValue : NC_FILL_INT;
      return result;
    } else if (useType == DataType.FLOAT) {
      float[] result = new float[1];
      result[0] = hasFillValue ? (float) fillValue : NC_FILL_FLOAT;
      return result;
    } else if (useType == DataType.DOUBLE) {
      double[] result = new double[1];
      result[0] = hasFillValue ? fillValue : NC_FILL_DOUBLE;
      return result;
    } else {
      String[] result = new String[1];
      result[0] = FillValue;
      return result;
    }
  }

  public double convertScaleOffsetMissing(byte valb) {
    if (!hasScaleOffset)
      return useNaNs && isMissing((double) valb) ? Double.NaN : (double) valb;

    double convertedValue;
    if (isUnsigned)
      convertedValue = scale * DataType.unsignedByteToShort(valb) + offset;
    else
      convertedValue = scale * valb + offset;

    return useNaNs && isMissing(convertedValue) ? Double.NaN : convertedValue;
  }

  public double convertScaleOffsetMissing(short vals) {
    if (!hasScaleOffset)
      return useNaNs && isMissing((double) vals) ? Double.NaN : (double) vals;

    double convertedValue;
    if (isUnsigned)
      convertedValue = scale * DataType.unsignedShortToInt(vals) + offset;
    else
      convertedValue = scale * vals + offset;

    return useNaNs && isMissing(convertedValue) ? Double.NaN : convertedValue;
  }

  public double convertScaleOffsetMissing(int vali) {
    if (!hasScaleOffset)
      return useNaNs && isMissing((double) vali) ? Double.NaN : (double) vali;

    double convertedValue;
    if (isUnsigned)
      convertedValue = scale * DataType.unsignedIntToLong(vali) + offset;
    else
      convertedValue = scale * vali + offset;

    return useNaNs && isMissing(convertedValue) ? Double.NaN : convertedValue;
  }

  public double convertScaleOffsetMissing(long vall) {
    if (!hasScaleOffset)
      return useNaNs && isMissing((double) vall) ? Double.NaN : (double) vall;

    double convertedValue = scale * vall + offset;
    return useNaNs && isMissing(convertedValue) ? Double.NaN : convertedValue;
  }

  public double convertScaleOffsetMissing(double value) {
    if (!hasScaleOffset)
      return useNaNs && isMissing(value) ? Double.NaN : value;

    double convertedValue = scale * value + offset;
    return useNaNs && isMissing(convertedValue) ? Double.NaN : convertedValue;
  }

  public Array convertScaleOffsetMissing(Array data) {
    if (hasScaleOffset())
      data = convertScaleOffset(data);
    else if (hasMissing() && getUseNaNs())
      data = convertMissing(data);
    return data;
  }

  /**
   * Convert Data with scale and offset.
   * Also translate missing data to NaNs if useNaNs = true.
   *
   * @param in data to convert
   * @return converted data.
   */
  private Array convertScaleOffset(Array in) {
    if (!hasScaleOffset) return in;
    if (debugRead) System.out.println("convertScaleOffset ");

    Array out = Array.factory(convertedDataType.getPrimitiveClassType(), in.getShape());
    IndexIterator iterIn = in.getIndexIterator();
    IndexIterator iterOut = out.getIndexIterator();

    if (isUnsigned && in.getElementType() == byte.class)
      convertScaleOffsetUnsignedByte(iterIn, iterOut);
    else if (isUnsigned && in.getElementType() == short.class)
      convertScaleOffsetUnsignedShort(iterIn, iterOut);
    else if (isUnsigned && in.getElementType() == int.class)
      convertScaleOffsetUnsignedInt(iterIn, iterOut);
    else {
      boolean checkMissing = useNaNs && hasMissing();
      while (iterIn.hasNext()) {
        double val = scale * iterIn.getDoubleNext() + offset;
        iterOut.setDoubleNext(checkMissing && isMissing_(val) ? Double.NaN : val);
      }
    }

    return out;
  }

  private void convertScaleOffsetUnsignedByte(IndexIterator iterIn, IndexIterator iterOut) {
    boolean checkMissing = useNaNs && hasMissing();
    while (iterIn.hasNext()) {
      byte valb = iterIn.getByteNext();
      double val = scale * DataType.unsignedByteToShort(valb) + offset;
      iterOut.setDoubleNext(checkMissing && isMissing_(val) ? Double.NaN : val);
    }
  }

  private void convertScaleOffsetUnsignedShort(IndexIterator iterIn, IndexIterator iterOut) {
    boolean checkMissing = useNaNs && hasMissing();
    while (iterIn.hasNext()) {
      short valb = iterIn.getShortNext();
      double val = scale * DataType.unsignedShortToInt(valb) + offset;
      iterOut.setDoubleNext(checkMissing && isMissing_(val) ? Double.NaN : val);
    }
  }

  private void convertScaleOffsetUnsignedInt(IndexIterator iterIn, IndexIterator iterOut) {
    boolean checkMissing = useNaNs && hasMissing();
    while (iterIn.hasNext()) {
      int valb = iterIn.getIntNext();
      double val = scale * DataType.unsignedIntToLong(valb) + offset;
      iterOut.setDoubleNext(checkMissing && isMissing_(val) ? Double.NaN : val);
    }
  }

  /**
   * Translate missing data to NaNs. Data must be DOUBLE or FLOAT
   *
   * @param in convert this array
   * @return same array, with missing values replaced by NaNs
   */
  private Array convertMissing(Array in) {
    if (debugRead) System.out.println("convertMissing ");

    IndexIterator iterIn = in.getIndexIterator();
    if (in.getElementType() == double.class) {
      while (iterIn.hasNext()) {
        double val = iterIn.getDoubleNext();
        if (isMissing_(val))
          iterIn.setDoubleCurrent(Double.NaN);
      }
    } else if (in.getElementType() == float.class) {
      while (iterIn.hasNext()) {
        float val = iterIn.getFloatNext();
        if (isMissing_(val))
          iterIn.setFloatCurrent(Float.NaN);
      }
    }
    return in;
  }

  /**
   * Convert (in place) all values in the given array that are considered
   * as "missing" to Float.NaN
   *
   * @param values input array
   * @return input array, with missing values converted to NaNs.
   */
  public float[] setMissingToNaN(float[] values) {
    if (!hasMissing()) return values;
    for (int i = 0; i < values.length; i++) {
      if (isMissing_(values[i]))
        values[i] = Float.NaN;
    }
    return values;
  }

  static public void main(String[] args) {
    double d = Double.NaN;
    float f = (float) d;
    System.out.println(" f=" + f + " " + Float.isNaN(f) + " " + Double.isNaN((double) f));

  }

}