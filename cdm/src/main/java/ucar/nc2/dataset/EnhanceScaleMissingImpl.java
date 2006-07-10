// $Id: EnhanceScaleMissingImpl.java,v 1.11 2005/11/17 00:48:17 caron Exp $
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
package ucar.nc2.dataset;

import ucar.ma2.*;
import ucar.nc2.*;

/**
 * Implementation of EnhanceScaleMissing for missing data and scale/offset packed data.
 *
 * @see EnhanceScaleMissing
 * @author caron
 * @version $Revision: 1.11 $ $Date: 2005/11/17 00:48:17 $
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

  private DataType convertedDataType = null;

  // defaults from NetcdfDataset modes
  private boolean useNaNs = NetcdfDataset.useNaNs;
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

  private boolean debug = false, debugRead = false;

  /** Constructor, when you dont want anything done. */
  public EnhanceScaleMissingImpl() { }

  /**
   * Constructor, default values.
   * @param forVar the Variable to decorate.
   */
  public EnhanceScaleMissingImpl( VariableDS forVar) {
    this(forVar, NetcdfDataset.useNaNs, NetcdfDataset.fillValueIsMissing,
         NetcdfDataset.invalidDataIsMissing, NetcdfDataset.missingDataIsMissing);
  }

  /**
   * Constructor.
   * @param forVar the original Variable to decorate.
   * @param useNaNs pre-fill isMissing() data with NaNs
   * @param fillValueIsMissing  use _FillValue for isMissing()
   * @param invalidDataIsMissing use valid_range for isMissing()
   * @param missingDataIsMissing use missing_value for isMissing()
   */
  public EnhanceScaleMissingImpl( VariableDS forVar, boolean useNaNs, boolean fillValueIsMissing,
    boolean invalidDataIsMissing, boolean missingDataIsMissing) {

    this.useNaNs = useNaNs;
    this.fillValueIsMissing = fillValueIsMissing;
    this.invalidDataIsMissing = invalidDataIsMissing;
    this.missingDataIsMissing = missingDataIsMissing;

    // see if underlying variable has scale/offset already applied
    Variable orgVar = forVar.getOriginalVariable();
    if (orgVar instanceof VariableDS) {
      VariableDS orgVarDS = (VariableDS) orgVar;
      if (orgVarDS.isEnhanced()) return;
    }

    this.isUnsigned = forVar.isUnsigned();

    DataType scaleType = null, missType = null, validType = null, fillType = null;
    if (debug) System.out.println("EnhancementsImpl for Variable = "+ forVar.getName());
    Attribute att;

      // scale and offset
    if (null != (att = forVar.findAttribute("scale_factor"))) {
      if (!att.isString()) {
        scale = att.getNumericValue().doubleValue();
        hasScaleOffset = true;
        scaleType = att.getDataType();
        if (debug) System.out.println("scale = "+ scale+" type "+scaleType);
      }
    }
    if (null != (att = forVar.findAttribute("add_offset"))) {
      if (!att.isString()) {
        offset = att.getNumericValue().doubleValue();
        hasScaleOffset = true;
        DataType offType = att.getDataType();
        if (rank(offType) > rank(scaleType))
          scaleType = offType;
        if (debug) System.out.println("offset = "+ offset);
      }
    }

      ////// missing data : valid_range
    if (null != (att = forVar.findAttribute("valid_range"))) {
      if (!att.isString() && att.isArray()) {
        valid_min = att.getNumericValue(0).doubleValue();
        valid_max = att.getNumericValue(1).doubleValue();
        hasValidRange = true;
        validType = att.getDataType();
        if (debug) System.out.println("valid_range = "+ valid_min+" "+valid_max);
      }
    }
    if (!hasValidRange) {
      if (null != (att = forVar.findAttribute("valid_min"))) {
        if (!att.isString()) {
          valid_min = att.getNumericValue().doubleValue();
          hasValidMin = true;
          validType = att.getDataType();
          if (debug) System.out.println("valid_min = "+ valid_min);
        }
      }
      if (null != (att = forVar.findAttribute("valid_max"))) {
        if (!att.isString()) {
          valid_max = att.getNumericValue().doubleValue();
          hasValidMax = true;
          DataType t = att.getDataType();
          if (rank(t) > rank(validType))
            validType = t;
          if (debug) System.out.println("valid_min = "+ valid_max);
        }
      }
      if (hasValidMin && hasValidMax)
        hasValidRange = true;
    }
    boolean hasValidData = hasValidMin || hasValidMax || hasValidRange;

      /// _FillValue
    if ((null != (att = forVar.findAttribute("_FillValue"))) && !att.isString()) {
      fillValue = att.getNumericValue().doubleValue();
      hasFillValue = true;
      fillType = att.getDataType();
      if (debug) System.out.println("missing_datum from _FillValue = "+ fillValue);
    }

      /// missing_value
    if (null != (att = forVar.findAttribute("missing_value"))) {
      if (att.isString()) {
        try {
          missingValue = new double[1];
          missingValue[0] = Double.parseDouble(att.getStringValue());
          missType = att.getDataType();
          hasMissingValue = true;
        } catch (NumberFormatException ex) {
          if (debug) System.out.println("String missing_value not parsable as double= "+ att.getStringValue());
        }

      } else if (!att.isArray()) {
        missingValue = new double[1];
        missingValue[0] = att.getNumericValue().doubleValue();
        if (debug) System.out.println("missing_datum = "+ missingValue[0]);
        missType = att.getDataType();
        hasMissingValue = true;

      } else {
        int n = att.getLength();
        missingValue = new double[n];
        if (debug) System.out.print("missing_data = ");
        for (int i=0; i<n; i++) {
          missingValue[i] = att.getNumericValue(i).doubleValue();
          if (debug) System.out.print(" "+missingValue[i]);
        }
        if (debug) System.out.println();
        missType = att.getDataType();
        hasMissingValue = true;
      }

    }

    // missing
    boolean hasMissing = (invalidDataIsMissing && hasValidData) ||
                ( fillValueIsMissing && hasFillValue) ||
                ( missingDataIsMissing && hasMissingValue);

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
      if (debug) System.out.println("assign dataType = "+ convertedDataType);

      // deal with case when theres both missing data and scaled data
      // fillValue always external (packed) LOOK BOGUS FLOAT COMPARE ??
      if (hasFillValue) {
        fillValue = scale * fillValue + offset;
        if (debug) System.out.println("scale the fillValue");
      }

      // missingValue always external (packed) LOOK BOGUS FLOAT COMPARE ??
      if (hasMissingValue) {
        for (int i=0; i<missingValue.length; i++)
          missingValue[i] = scale * missingValue[i] + offset;
        if (debug) System.out.println("scale the missing values");
      }

      // validData may be external or internal
      if (hasValidData) {
        DataType orgType = forVar.getDataType();

        // If valid_range is the same type as scale_factor (actually the wider of
        // scale_factor and add_offset) and this is wider than the external data, then it
        // will be interpreted as being in the units of the internal (unpacked) data.
        // Otherwise it is in the units of the external (unpacked) data.
        if ( !((rank(validType) == rank(scaleType)) && (rank(scaleType) > rank(orgType))) ) {
          if (hasValidRange || hasValidMin)
            valid_min = scale * valid_min + offset;
          if (hasValidRange || hasValidMax)
            valid_max = scale * valid_max + offset;
          if (debug) System.out.println("scale the range");
        }
      }

      useNaNs = useNaNs && ((convertedDataType == DataType.DOUBLE) ||
                          (convertedDataType == DataType.FLOAT));
    }
    if (debug) System.out.println("useNaNs = "+useNaNs);
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
   * Get the converted DataType, if hasScaleOffset is true.
   */
  public DataType getConvertedDataType() {
    return convertedDataType;
  }

  /** true if Variable has valid_range, valid_min or valid_max attributes */
  public boolean hasInvalidData() { return hasValidRange || hasValidMin || hasValidMax; }
  /** return the minimum value in the valid range */
  public double getValidMin() { return valid_min; }
  /** return the maximum value in the valid range */
  public double getValidMax() { return valid_max; }
  /** return true if val is outside the valid range */
  public boolean isInvalidData( double val ) {
    if (hasValidRange)
      return ((val < valid_min) || (val > valid_max));
    else if (hasValidMin)
      return (val < valid_min);
    else if (hasValidMax)
      return (val > valid_max);
    return false;
  }

  /** true if Variable has _FillValue attribute */
  public boolean hasFillValue() { return hasFillValue; }
  /** return true if val equals the _FillValue  */
  public boolean isFillValue( double val ) { return hasFillValue && (val == fillValue); }

  /** true if Variable data will be converted using scale and offet */
  public boolean hasScaleOffset() { return hasScaleOffset; }
  /** true if Variable has missing_value attribute */
  public boolean hasMissingValue() { return hasMissingValue; }
  /** return true if val equals a missing_value (low level)  */
  public boolean isMissingValue( double val ) {
    if (!hasMissingValue)
      return false;
    for (int i=0; i<missingValue.length; i++)
      if (val == missingValue[i])
        return true;
    return false;
  }

  /** set whether to use NaNs for missing values, for efficiency */
  public void setUseNaNs(boolean useNaNs) {
    this.useNaNs = useNaNs;
  }

    /** get whether to use NaNs for missing values, for efficiency */
  public boolean getUseNaNs() { return useNaNs; }

  /** set if _FillValue is considered isMissing(); better set in constructor if possible */
  public void setFillValueIsMissing( boolean b) { this.fillValueIsMissing = b; }
  /** set if valid_range is considered isMissing(); better set in constructor if possible */
  public void setInvalidDataIsMissing( boolean b) { this.invalidDataIsMissing = b; }
  /** set if missing_data is considered isMissing(); better set in constructor if possible */
  public void setMissingDataIsMissing( boolean b) { this.missingDataIsMissing = b; }
  /** true if Variable has missing data values */
  public boolean hasMissing() {
    return (invalidDataIsMissing && hasInvalidData()) ||
           (fillValueIsMissing && hasFillValue()) ||
           (missingDataIsMissing && hasMissingValue());
  }

  /** true if val is a missing data value */
  public boolean isMissing( double val ) {
    if ( Double.isNaN(val)) return true;
    if (!hasMissing()) return false;
    return (invalidDataIsMissing && isInvalidData(val)) ||
      (fillValueIsMissing && isFillValue( val)) ||
      (missingDataIsMissing && isMissingValue( val));
  }

  public Object getFillValue(DataType dt) {
    DataType useType = convertedDataType == null ? dt : convertedDataType;
    if (useType == DataType.BYTE) {
      byte[] result = new byte[1];
      result[0] = hasFillValue ? (byte) fillValue : NC_FILL_BYTE;
      return result;
    }

    else if (useType == DataType.BOOLEAN) {
      boolean[] result = new boolean[1];
      result[0] = false;
      return result;
    }

    else if (useType == DataType.CHAR) {
      char[] result = new char[1];
      result[0] = hasFillValue ? (char) fillValue : NC_FILL_CHAR;
      return result;
    }

    else if (useType == DataType.SHORT) {
      short[] result = new short[1];
      result[0] = hasFillValue ? (short) fillValue : NC_FILL_SHORT;
      return result;
    }

    else if (useType == DataType.INT) {
      int[] result = new int[1];
      result[0] = hasFillValue ? (int) fillValue : NC_FILL_INT;
      return result;
    }

    else if (useType == DataType.LONG) {
      long[] result = new long[1];
      result[0] = hasFillValue ? (long) fillValue : NC_FILL_INT;
      return result;
    }

    else if (useType == DataType.FLOAT) {
      float[] result = new float[1];
      result[0] = hasFillValue ? (float) fillValue : NC_FILL_FLOAT;
      return result;
    }

    else if (useType == DataType.DOUBLE) {
      double[] result = new double[1];
      result[0] = hasFillValue ? fillValue : NC_FILL_DOUBLE;
      return result;
    }

    else {
      String[] result = new String[1];
      result[0] = FillValue;
      return result;
    }

  }


  public double convertScaleOffsetMissing(byte valb){
    if (!hasScaleOffset)
      return useNaNs && isMissing((double) valb) ? Double.NaN : (double) valb;

    double convertedValue;
    if (isUnsigned)
      convertedValue = scale * DataType.unsignedByteToShort(valb) + offset;
    else
      convertedValue = scale * valb + offset;

    return useNaNs && isMissing(convertedValue) ? Double.NaN : convertedValue;
  }

  public double convertScaleOffsetMissing(short vals){
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

  public double convertScaleOffsetMissing(long vall){
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


  /**
   * Convert Data with scale and offset.
   * Also translate missing data to NaNs if useNaNs = true.
   * @param in data to convert
   * @return converted data.
   */
  public Array convertScaleOffset(Array in) {
    if (!hasScaleOffset) return in;
    if (debugRead) System.out.println("convertScaleOffset ");

    Array out = Array.factory( convertedDataType.getPrimitiveClassType(), in.getShape());
    IndexIterator iterIn = in.getIndexIteratorFast();
    IndexIterator iterOut = out.getIndexIteratorFast();

    if (isUnsigned && in.getElementType() == byte.class)
      convertScaleOffsetUnsignedByte( iterIn, iterOut);
    else if (isUnsigned && in.getElementType() == short.class)
      convertScaleOffsetUnsignedShort( iterIn, iterOut);
    else if (isUnsigned && in.getElementType() == int.class)
      convertScaleOffsetUnsignedInt( iterIn, iterOut);
    else {
      while (iterIn.hasNext()) {
        double val = scale * iterIn.getDoubleNext() + offset;
        iterOut.setDoubleNext(useNaNs && isMissing(val) ? Double.NaN : val);
      }
    }

    return out;
  }

  private void convertScaleOffsetUnsignedByte(IndexIterator iterIn, IndexIterator iterOut) {
    while (iterIn.hasNext()) {
      byte valb = iterIn.getByteNext();
      double val = scale * DataType.unsignedByteToShort(valb) + offset;
      iterOut.setDoubleNext(useNaNs && isMissing(val) ? Double.NaN : val);
    }
  }

  private void convertScaleOffsetUnsignedShort(IndexIterator iterIn, IndexIterator iterOut) {
    while (iterIn.hasNext()) {
      short valb = iterIn.getShortNext();
      double val = scale * DataType.unsignedShortToInt(valb) + offset;
      iterOut.setDoubleNext(useNaNs && isMissing(val) ? Double.NaN : val);
    }
  }

  private void convertScaleOffsetUnsignedInt(IndexIterator iterIn, IndexIterator iterOut) {
    while (iterIn.hasNext()) {
      int valb = iterIn.getIntNext();
      double val = scale * DataType.unsignedIntToLong(valb) + offset;
      iterOut.setDoubleNext(useNaNs && isMissing(val) ? Double.NaN : val);
    }
  }

  /** Translate missing data to NaNs. Data must be DOUBLE or FLOAT */
  public Array convertMissing(Array in) {
    if (debugRead) System.out.println("convertMissing ");

    IndexIterator iterIn = in.getIndexIteratorFast();
    if (in.getElementType() == double.class) {
      while (iterIn.hasNext()) {
        double val = iterIn.getDoubleNext();
        if (isMissing(val))
          iterIn.setDoubleCurrent( Double.NaN);
      }
    } else if (in.getElementType() == float.class) {
      while (iterIn.hasNext()) {
        float val = iterIn.getFloatNext();
        if (isMissing(val))
          iterIn.setFloatCurrent( Float.NaN);
      }
    }
    return in;
  }

  /**
   * Convert (in place) all values in the given array that are considered
   * as "missing" to Float.NaN
   * @param values input array
   * @return input array, with missing values converted to NaNs.
   */
    public float[] setMissingToNaN (float[] values) {
      if (!hasMissing()) return values;
      final int length = values.length;
      for (int i = 0; i < length; i++) {
        float value = values[i];
        if ((invalidDataIsMissing && isInvalidData(value)) ||
            (fillValueIsMissing && isFillValue(value)) ||
            (missingDataIsMissing && isMissingValue( value))) {
          values[i] = Float.NaN;
        }
      }
      return values;
    }

  static public void main( String[] args) {
    double d = Double.NaN;
    float f = (float) d;
    System.out.println(" f="+f+" "+Float.isNaN(f)+" "+Double.isNaN((double)f));

  }

}