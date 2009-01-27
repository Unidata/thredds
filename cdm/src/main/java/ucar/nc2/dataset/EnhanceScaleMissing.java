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

import ucar.ma2.Array;

import java.io.IOException;

/**
 * A Variable decorator that handles missing data, and scale/offset packed data.
 * Specifically, this handles:
 * <ul>
 * <li> packed data using <i> scale_factor and add_offset </i>
 * <li> invalid/missing data using <i> valid_min, valid_max, valid_range,
 *   missing_value or _FillValue </i>
 * </ul>
 * if those "standard attributes" are present.
 *
 * <h2>Standard Use</h2>
 * <p> <b>Implementation rules for missing data</b>. Here "missing data" is a general
 *   name for invalid/never written/missing values. Use this interface when you dont need to
 *   distinguish these variants. See below for a lower-level interface if you do need to
 *   distinguish between them.
 * <ol>
 * <li> By default, hasMissing() is true if any of hasInvalidData(), hasFillData() or
 *   hasMissingValue() are true (see below). You can modify this behavior in the constuctor
 *   or by calling setInvalidDataIsMissing(), setFillDataIsMissing(), or setMissingValueIsMissing().
 * <li> Test specific values through isMissing(double val). Note that the data is converted and
 *   compared as a double.
 * <li> Data values of float or double NaN are considered missing data and will return
 *   true if called with isMissing(). (However hasMissing() will not detect if you are
 *   setting NaNs yourself).
 * <li> if you do not need to distinguish between _FillValue, missing_value and invalid, then
 *   set useNaNs = true in the constructor. When the Variable element type is float or double
 *   (or is set to double because its packed), then this sets isMissing() data values to NaNs, which
 *   makes further comparisions more efficient.
 * </ol>
 *
 * <p> <b>Implementation rules for scale/offset (no missing data)</b>
 * <ol>
 * <li> If scale_factor and/or add_offset variable attributes are present,
 *   then this is a "packed" Variable.
 * <li> The Variable element type is converted in the following way:
 * <ul>
 *   <li> The dataType is set to the widest type of 1) the data type 2) the scale_factor attribute type
 *     and 3) the add_offset attribute type. (byte < short < int < float < double ).
 * </ul>
 * <li> external (packed) data is converted to internal (unpacked) data transparently
 *   during the read() call.
 * </ol>
 *
 * <p> <b>Implementation rules for missing data with scale/offset</b>
 * <ol>
 * <li> Always converted to a float or double type.
 * <li> _FillValue and missing_value values are always in the units of the external
 *    (packed) data.
 * <li> If valid_range is the same type as scale_factor (actually the wider of
 *     scale_factor and add_offset) and this is wider than the external data, then it
 *     will be interpreted as being in the units of the internal (unpacked) data.
 *     Otherwise it is in the units of the external (packed) data.
 *  <li> The dataType is set to float if all attributes used are float (scale_factor, add_offset
 *     valid_min, valid_max, valid_range, missing_data and _FillValue) otherwise the dataType is
 *     set to double.
 * </ol>
 *
 * <h2> Low Level Access </h2>
 *   The following provide more direct access to missing/invalid data. These are mostly convenience
 *   routines for checking the standard attributes. If you set useNaNs = true in the constructor,
 *   these routines cannot be used when the data has type float or double.
 *
 * <p> <b>Implementation rules for valid_range</b>
 * <ol>
 * <li> if valid_range is present, valid_min and valid_max attributes are
 *   ignored. Otherwise, the valid_min and/or valid_max is used to construct
 *   a valid range. If any of these exist, hasInvalidData() is true.
 * <li> To test a specific value, call isInvalidData(). Note that the data is converted and
 *   compared as a double. Or get the range through getValidMin() and getValidMax().
 * </ol>
 *
 * <p> <b>Implementation rules for _FillData </b>
 * <ol>
 * <li> if the _FillData attribute is present, it must have a scalar value of the same
 *   type as the data. In this case, hasFillData() returns true.
 * <li> Test specific values through isFillValue(). Note that the data is converted and
 *   compared as a double.
 * </ol>
 *
 * <p> <b>Implementation rules for missing_value</b>
 * <ol>
 * <li> if the missing_value attribute is present, it must have a scalar or vector
 *   value of the same type as the data. In this case, hasMissingValue() returns true.
 * <li> Test specific values through isMissingValue(). Note that the data is converted and
 *   compared as a double.
 * </ol>
 *
 * <p> <h2>Strategies for using EnhanceScaleMissing</h2>
 * <ol>
 *   <li> Low-level: use the is/has InvalidData/FillData/missingValue routines
 *     to "roll-your own" tests for the various kinds of missing/invalid data.
 *   <li> Standard: use is/hasMissing() to test for missing data when you dont need to
 *     distinguish between the variants. Use the setXXXisMissing() to customize the behavior
 *     if needed.
 *   <li> Efficient : If you expect to scan more than once for missing values, and
 *     you are not distinguishing between InvalidData/FillData/missingValue, then
 *     set useNaNs in the constructor. This sets isMissing() data values to NaNs when reading,
 *     and subsequent checks are more efficient.
 *  </ol>
 *
 * @author caron
 */

public interface EnhanceScaleMissing {

  /** true if Variable has missing data values */
  public boolean hasMissing();

  /** true if val is a missing data value.
   * @return true if this value is a misssing value
   */
  public boolean isMissing( double val );

  /** true if val is a missing data value.
   * if useNaNs is true (default) and the variable is of type float or double,
   *   then assume that missing values have been set to NaN, which speeds up the test considerably.
   * @return true if this value is a misssing value
   */
  public boolean isMissingFast( double val ); 

  /** true if Variable has valid_range, valid_min or valid_max attributes */
  public boolean hasInvalidData();
  /** return the minimum value in the valid range */
  public double getValidMin();
  /** return the maximum value in the valid range */
  public double getValidMax();
  /** return true if val is outside the valid range */
  public boolean isInvalidData( double val );

  /** true if Variable has _FillValue attribute */
  public boolean hasFillValue();
  /** return true if val equals the _FillValue  */
  public boolean isFillValue( double val );

  /** true if Variable has missing_value attribute */
  public boolean hasMissingValue();
  /** return true if val equals a missing_value  */
  public boolean isMissingValue( double val );

  /** set if _FillValue is considered isMissing(); better set in constructor if possible */
  public void setFillValueIsMissing( boolean b);
  /** set if valid_range is considered isMissing(); better set in constructor if possible */
  public void setInvalidDataIsMissing( boolean b);
  /** set if missing_data is considered isMissing(); better set in constructor if possible */
  public void setMissingDataIsMissing( boolean b);
  /** set whether to use NaNs for missing float/double values, for efficiency */
  public void setUseNaNs(boolean useNaNs);
  /** get whether to use NaNs for missing float/double values (for efficiency) */
  public boolean getUseNaNs();

  /** true if Variable data will be converted using scale and offet */
  public boolean hasScaleOffset();

  /**
   * Convert data if hasScaleOffset, using scale and offset.
   * Also if useNaNs = true, return NaN if value is missing data.
   * @param data convert this
   * @return converted data.
   */
  public Array convertScaleOffsetMissing(Array data) throws IOException;

  /** Convert this byte value to a double, using scale/offset/missing value if applicable */
  public double convertScaleOffsetMissing(byte value);
  /** Convert this short value to a double, using scale/offset/missing value if applicable */
  public double convertScaleOffsetMissing(short value);
  /** Convert this int value to a double, using scale/offset/missing value if applicable */
  public double convertScaleOffsetMissing(int value);
  /** Convert this long value to a double, using scale/offset/missing value if applicable */
  public double convertScaleOffsetMissing(long value);
  /** Convert this double value using scale/offset/missing value if applicable */
  public double convertScaleOffsetMissing(double value); 
}
