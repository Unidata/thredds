/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IsMissingEvaluator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A Variable decorator that handles unsigned data, scale/offset packed data, and missing data. Specifically, it
 * handles:
 * <ul>
 *   <li>unsigned data using {@code _Unsigned}</li>
 *   <li>packed data using {@code scale_factor} and {@code add_offset}</li>
 *   <li>invalid/missing data using {@code valid_min}, {@code valid_max}, {@code valid_range}, {@code missing_value},
 *       or {@code _FillValue}</li>
 * </ul>
 * if those "standard attributes" are present.
 *
 * <h2>Standard Use</h2>
 *
 * <h3>Implementation rules for unsigned data</h3>
 *
 * <ol>
 *   <li>A variable is considered unsigned if it has an {@link DataType#isUnsigned() unsigned data type} or an
 *       {@code _Unsigned} attribute with value {@code true}.</li>
 *   <li>Values will be {@link DataType#widenNumber widened}, which effectively reinterprets signed data as unsigned
 *       data.</li>
 *   <li>To accommodate the unsigned conversion, the variable's data type will be changed to the
 *       {@link EnhanceScaleMissingUnsignedImpl#nextLarger(DataType) next larger type}.</li>
 * </ol>
 *
 * <h3>Implementation rules for scale/offset</h3>
 *
 * <ol>
 *   <li>If scale_factor and/or add_offset variable attributes are present, then this is a "packed" Variable.</li>
 *   <li>The data type of the variable will be set to the {@link EnhanceScaleMissingUnsignedImpl#largestOf largest of}:
 *     <ul>
 *       <li>the original data type</li>
 *       <li>the unsigned conversion type, if applicable</li>
 *       <li>the {@code scale_factor} attribute type</li>
 *       <li>the {@code add_offset} attribute type</li>
 *     </ul>
 *     The signedness of the variable's data type will be preserved. For example, if the variable was originally
 *     unsigned, then {@link #getScaledOffsetType()} will be unsigned as well.
 *   </li>
 *   <li>External (packed) data is converted to internal (unpacked) data transparently during the
 *       {@link #applyScaleOffset(Array)} call.</li>
 * </ol>
 *
 * <h3>Implementation rules for missing data</h3>
 *
 * Here "missing data" is a general name for invalid/never-written/missing values. Use this interface when you don't
 * need to distinguish these variants. See below for a lower-level interface if you do need to distinguish between them.
 *
 * <ol>
 *   <li>By default, hasMissing() is true if any of hasValidData(), hasFillValue() or hasMissingValue() are true
 *       (see below). You can modify this behavior by calling setInvalidDataIsMissing(), setFillValueIsMissing(), or
 *       setMissingDataIsMissing().</li>
 *   <li>Test specific values through isMissing(double). Note that the data is converted and compared as a double.</li>
 *   <li>Data values of float or double NaN are considered missing data and will return true if called with
 *       isMissing(). (However isMissing() will not detect if you are setting NaNs yourself).</li>
 * </ol>
 *
 * <h3>Implementation rules for missing data with scale/offset</h3>
 *
 * <ol>
 *   <li>_FillValue and missing_value values are always in the units of the external (packed) data.</li>
 *   <li>If valid_range is the same type as scale_factor (actually the wider of scale_factor and add_offset) and this
 *       is wider than the external data, then it will be interpreted as being in the units of the internal (unpacked)
 *       data. Otherwise it is in the units of the external (packed) data.</li>
 * </ol>
 *
 * <h2>Low Level Access</h2>
 *
 * The following provide more direct access to missing/invalid data. These are mostly convenience routines for
 * checking the standard attributes. If you set useNaNs = true in the constructor, these routines cannot be used when
 * the data has type float or double.
 *
 * <h3>Implementation rules for valid_range</h3>
 *
 * <ol>
 *   <li>If valid_range is present, valid_min and valid_max attributes are ignored. Otherwise, the valid_min and/or
 *       valid_max is used to construct a valid range. If any of these exist, hasValidData() is true.</li>
 *   <li>To test a specific value, call isInvalidData(). Note that the data is converted and compared as a double. Or
 *       get the range through getValidMin() and getValidMax().</li>
 * </ol>
 *
 * <h3>Implementation rules for _FillData</h3>
 *
 * <ol>
 *   <li>If the _FillData attribute is present, it must have a scalar value of the same type as the data. In this
 *       case, hasFillValue() returns true.</li>
 *   <li>Test specific values through isFillValue(). Note that the data is converted and compared as a double.</li>
 * </ol>
 *
 * <h3>Implementation rules for missing_value</h3>
 *
 * <ol>
 *   <li>If the missing_value attribute is present, it must have a scalar or vector value of the same type as the
 *       data. In this case, hasMissingValue() returns true.</li>
 *   <li>Test specific values through isMissingValue(). Note that the data is converted and compared as a double.</li>
 * </ol>
 *
 * <h3>Strategies for using EnhanceScaleMissingUnsigned</h3>
 *
 * <ol>
 *   <li>Low-level: use the is/has InvalidData/FillValue/MissingValue routines to "roll-your own" tests for the
 *       various kinds of missing/invalid data.</li>
 *   <li>Standard: use is/hasMissing() to test for missing data when you don't need to distinguish between the
 *       variants. Use the setXXXisMissing() to customize the behavior if needed.</li>
 * </ol>
 *
 * @author caron
 * @author cwardgar
 */
public interface EnhanceScaleMissingUnsigned extends IsMissingEvaluator {
  /** true if Variable data will be converted using scale and offset */
  boolean hasScaleOffset();

  /**
   * The data are to be multiplied by this value after the data are read. By default it is {@code 1.0}, i.e. no
   * scaling. It will remain that value if the variable defines no {@link ucar.nc2.constants.CDM#SCALE_FACTOR}
   * attribute.
   *
   * @return  the multiplier to apply to the data.
   */
  double getScaleFactor();

  /**
   * The number to be added to the data after it is read. If both {@link ucar.nc2.constants.CDM#SCALE_FACTOR} and
   * {@link ucar.nc2.constants.CDM#ADD_OFFSET} attributes are present, the data are first scaled before the offset is
   * added. By default it is {@code 0.0}, i.e. no offset. It will remain that value if the variable defines no
   * {@link ucar.nc2.constants.CDM#SCALE_FACTOR} attribute.
   *
   * @return  the number to add to the data.
   */
  double getOffset();

  /** true if Variable has missing data values
   * @return true if Variable has missing data values
   */
  boolean hasMissing();

  /**
   * Returns {@code true} if the argument is a missing value.
   * Note that {@link Float#NaN} and {@link Double#NaN} are considered missing data.
   *
   * @param val an unpacked value.
   * @return  {@code true} if the argument is a missing value.
   */
  boolean isMissing(double val);

  /** true if Variable has valid_range, valid_min or valid_max attributes */
  boolean hasValidData();
  /** return the minimum value in the valid range */
  double getValidMin();
  /** return the maximum value in the valid range */
  double getValidMax();
  /** return true if val is outside the valid range */
  boolean isInvalidData(double val);

  /** true if Variable has _FillValue attribute */
  boolean hasFillValue();
  /** return value of _FillValue attribute */
  double getFillValue();
  /** return true if val equals the _FillValue  */
  boolean isFillValue(double val);

  /** true if Variable has missing_value attribute */
  boolean hasMissingValue();
  /** return values of missing_value attributes */
  double[] getMissingValues();
  /** return true if val equals a missing_value (low-level)  */
  boolean isMissingValue(double val);

  /** set if _FillValue is considered isMissing(); better set in constructor if possible */
  void setFillValueIsMissing(boolean b);
  /** set if valid_range is considered isMissing(); better set in constructor if possible */
  void setInvalidDataIsMissing(boolean b);
  /** set if missing_data is considered isMissing(); better set in constructor if possible */
  void setMissingDataIsMissing(boolean b);
  
  /**
   * Return the data type for values that have undergone scale/offset conversion. This will be {@code null} if the
   * decorated variable lacks {@code scale_factor} or {@code add_offset} attributes. You can check with
   * {@link #hasScaleOffset()}.
   *
   * @return the data type for values that have undergone scale/offset conversion.
   */
  @Nullable
  DataType getScaledOffsetType();
  
  /**
   * Return the data type for values that have undergone unsigned conversion. This will never be {@code null}, even
   * when no unsigned conversion is necessary (because the underlying variable isn't unsigned). In such cases, this
   * data type will be the same as that of the underlying variable.
   *
   * @return  the data type for values that have undergone unsigned conversion.
   */
  @Nonnull
  DataType getUnsignedConversionType();
  
  /**
   * Returns the signedness of the decorated variable.
   * @return the signedness of the decorated variable.
   */
  DataType.Signedness getSignedness();
  
  /**
   * Convert {@code value} to the next largest integral data type by an {@link DataType#widenNumber(Number)
   * unsigned conversion}. The conversion only happens if the decorated variable {@link #getSignedness() is unsigned}
   * and {@code value} is a negative integer. Otherwise, simply return {@code value}.
   *
   * @param value  an integral number to convert.
   * @return  the result of an unsigned conversion of {@code value}.
   */
  Number convertUnsigned(Number value);
  
  /**
   * Performs an {@link #convertUnsigned(Number) unsigned conversion} of each element of {@code in} and returns the
   * result as a new Array. The data type of the returned array will be {@link #getUnsignedConversionType()}.
   *
   * @param in  an Array containing integral Numbers to convert.
   * @return  the result of an unsigned conversion of each element of {@code in}.
   */
  Array convertUnsigned(Array in);
  
  /**
   * Apply scale and offset to the specified value if {@link #hasScaleOffset()}. Otherwise, just return {@code value}.
   */
  double applyScaleOffset(Number value);
  
  /**
   * Apply scale and offset to each element of {@code in} and return the result as a new Array, but only if
   * {@link #hasScaleOffset()}. Otherwise, just return {@code value}. Otherwise, just return {@code data}.
   *
   * @param data convert this
   * @return converted data.
   */
  Array applyScaleOffset(Array data);
  
  /**
   * If {@code value} {@link #isMissing(double) is missing}, return {@link Double#NaN}.
   * Otherwise, simply return {@code value}
   */
  Number convertMissing(Number value);
  
  /**
   * Returns a copy of {@code in}, except that each {@link #isMissing(double) missing} value is replaced by
   * {@link Double#NaN}.
   *
   * @param in  an array containing floating-point numbers to convert.
   * @return  the result of a missing conversion of each element of {@code in}.
   */
  Array convertMissing(Array in);
  
  /**
   * Perform the specified conversions on each element of {@code in} and return the result as a new Array.
   * Note that this method is more efficient than calling {@link #convertUnsigned(Array)}, followed by
   * {@link #applyScaleOffset(Array)}, followed by {@link #convertMissing(Array)}, as only one copy of {@code in} is
   * made.
   *
   * @param in  a numeric array.
   * @param convertUnsigned  {@code true} if we should {@link #convertUnsigned(Number) convert unsigned}.
   * @param applyScaleOffset  {@code true} if we should {@link #applyScaleOffset(Number) apply scale/offset}.
   * @param convertMissing  {@code true} if we should {@link #convertMissing(Number) convert missing}.
   * @return  a new array, with the specified conversions performed.
   */
  Array convert(Array in, boolean convertUnsigned, boolean applyScaleOffset, boolean convertMissing);
}
