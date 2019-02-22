/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.util.Misc;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import static ucar.ma2.DataType.*;

/**
 * Implementation of EnhanceScaleMissingUnsigned for unsigned data, scale/offset packed data, and missing data.
 *
 * @author caron
 * @author cwardgar
 * @see EnhanceScaleMissingUnsigned
 */
class EnhanceScaleMissingUnsignedImpl implements EnhanceScaleMissingUnsigned {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private DataType origDataType = null, unsignedConversionType = null, scaledOffsetType = null;

  // defaults from NetcdfDataset modes
  private boolean invalidDataIsMissing = NetcdfDataset.invalidDataIsMissing;
  private boolean fillValueIsMissing = NetcdfDataset.fillValueIsMissing;
  private boolean missingDataIsMissing = NetcdfDataset.missingDataIsMissing;

  private boolean hasScaleOffset = false;
  private double scale = 1.0, offset = 0.0;

  private boolean hasValidRange = false, hasValidMin = false, hasValidMax = false;
  private double validMin = -Double.MAX_VALUE, validMax = Double.MAX_VALUE;

  private boolean hasFillValue = false;
  private double fillValue; // LOOK: making it double not really correct. What about CHAR?

  private boolean hasMissingValue = false;
  private double[] missingValue;  // LOOK: also wrong to make double, for the same reason.

  private DataType.Signedness signedness;


  /**
   * Constructor, when you dont want anything done.
   */
  EnhanceScaleMissingUnsignedImpl() {
  }

  /**
   * Constructor, default values.
   *
   * @param forVar the Variable to decorate.
   */
  EnhanceScaleMissingUnsignedImpl(VariableDS forVar) {
    this(forVar, NetcdfDataset.fillValueIsMissing, NetcdfDataset.invalidDataIsMissing,
        NetcdfDataset.missingDataIsMissing);
  }

  /**
   * Constructor.
   * If scale/offset attributes are found, remove them from the decorated variable.
   *
   * @param forVar               the Variable to decorate.
   * @param fillValueIsMissing   use _FillValue for isMissing()
   * @param invalidDataIsMissing use valid_range for isMissing()
   * @param missingDataIsMissing use missing_value for isMissing()
   */
  private EnhanceScaleMissingUnsignedImpl(VariableDS forVar, boolean fillValueIsMissing, boolean invalidDataIsMissing,
          boolean missingDataIsMissing) {
    this.fillValueIsMissing = fillValueIsMissing;
    this.invalidDataIsMissing = invalidDataIsMissing;
    this.missingDataIsMissing = missingDataIsMissing;

    this.origDataType = forVar.getDataType();
    this.unsignedConversionType = origDataType;
    
    // unsignedConversionType is initialized to origDataType, and origDataType may be a non-integral type that doesn't
    // have an "unsigned flavor" (such as FLOAT and DOUBLE). Furthermore, unsignedConversionType may start out as
    // integral, but then be widened to non-integral (i.e. LONG -> DOUBLE). For these reasons, we cannot rely upon
    // unsignedConversionType to store the signedness of the variable. We need a separate field.
    this.signedness = origDataType.getSignedness();
  
    // In the event of conflict, "unsigned" wins. Potential conflicts include:
    // 1. origDataType is unsigned, but variable has "_Unsigned == false" attribute.
    // 2. origDataType is signed,   but variable has "_Unsigned == true"  attribute.
    if (signedness == Signedness.SIGNED) {
      Attribute unsignedAtt = forVar.findAttributeIgnoreCase(CDM.UNSIGNED);
      if (unsignedAtt != null && unsignedAtt.getStringValue().equalsIgnoreCase("true")) {
          this.signedness = Signedness.UNSIGNED;
      }
    }

    if (signedness == Signedness.UNSIGNED) {
      // We may need a larger data type to hold the results of the unsigned conversion.
      this.unsignedConversionType = nextLarger(origDataType).withSignedness(Signedness.UNSIGNED);
      logger.debug("assign unsignedConversionType = {}", unsignedConversionType);
    }
    
    DataType scaleType = null, offsetType = null, validType = null;
    logger.debug("{} for Variable = {}", getClass().getSimpleName(), forVar.getFullName());

    Attribute scaleAtt = forVar.findAttribute(CDM.SCALE_FACTOR);
    if (scaleAtt != null && !scaleAtt.isString()) {
      scaleType = getAttributeDataType(scaleAtt);
      scale = convertUnsigned(scaleAtt.getNumericValue(), scaleType).doubleValue();
      hasScaleOffset = true;
      logger.debug("scale = {}    type = {}", scale, scaleType);
    }

    Attribute offsetAtt = forVar.findAttribute(CDM.ADD_OFFSET);
    if (offsetAtt != null && !offsetAtt.isString()) {
      offsetType = getAttributeDataType(offsetAtt);
      offset = convertUnsigned(offsetAtt.getNumericValue(), offsetType).doubleValue();
      hasScaleOffset = true;
      logger.debug("offset = {}", offset);
    }

    ////// missing data : valid_range. assume here its in units of unpacked data. correct this below
    Attribute validRangeAtt = forVar.findAttribute(CDM.VALID_RANGE);
    if (validRangeAtt != null && !validRangeAtt.isString() && validRangeAtt.getLength() > 1) {
      validType = getAttributeDataType(validRangeAtt);
      validMin = convertUnsigned(validRangeAtt.getNumericValue(0), validType).doubleValue();
      validMax = convertUnsigned(validRangeAtt.getNumericValue(1), validType).doubleValue();
      hasValidRange = true;
      logger.debug("valid_range = {}  {}", validMin, validMax);
    }

    Attribute validMinAtt = forVar.findAttribute(CDM.VALID_MIN);
    Attribute validMaxAtt = forVar.findAttribute(CDM.VALID_MAX);

    // Only process the valid_min and valid_max attributes if valid_range isn't present.
    if (!hasValidRange) {
      if (validMinAtt != null && !validMinAtt.isString()) {
        validType = getAttributeDataType(validMinAtt);
        validMin = convertUnsigned(validMinAtt.getNumericValue(), validType).doubleValue();
        hasValidMin = true;
        logger.debug("valid_min = {}", validMin);
      }

      if (validMaxAtt != null && !validMaxAtt.isString()) {
        validType = largestOf(validType, getAttributeDataType(validMaxAtt));
        validMax = convertUnsigned(validMaxAtt.getNumericValue(), validType).doubleValue();
        hasValidMax = true;
        logger.debug("valid_min = {}", validMax);
      }

      if (hasValidMin && hasValidMax) {
        hasValidRange = true;
      }
    }

    /// _FillValue
    Attribute fillValueAtt = forVar.findAttribute(CDM.FILL_VALUE);
    if (fillValueAtt != null && !fillValueAtt.isString()) {
      DataType fillType = getAttributeDataType(fillValueAtt);
      fillValue = convertUnsigned(fillValueAtt.getNumericValue(), fillType).doubleValue();
      fillValue = applyScaleOffset(fillValue);  // This will fail when _FillValue is CHAR.
      hasFillValue = true;
    } else {
      // No _FillValue attribute found. Instead, if file is NetCDF and variable is numeric, use the default fill value.
      String fileTypeId = forVar.getNetcdfFile() == null ? null : forVar.getNetcdfFile().getFileTypeId();
      
      boolean isNetcdfIosp = DataFormatType.NETCDF.getDescription().equals(fileTypeId) ||
              DataFormatType.NETCDF4.getDescription().equals(fileTypeId);
      
      if (isNetcdfIosp && unsignedConversionType.isNumeric()) {
        fillValue = applyScaleOffset(N3iosp.getFillValueDefault(unsignedConversionType));
        hasFillValue = true;
      }
    }

    /// missing_value
    Attribute missingValueAtt = forVar.findAttribute(CDM.MISSING_VALUE);
    if (missingValueAtt != null) {
      if (missingValueAtt.isString()) {
        String svalue = missingValueAtt.getStringValue();
        if (origDataType == DataType.CHAR) {
          missingValue = new double[1];
          if (svalue.length() == 0) {
            missingValue[0] = 0;
          } else {
            missingValue[0] = svalue.charAt(0);
          }

          hasMissingValue = true;
        } else {  // not a CHAR - try to fix problem where they use a numeric value as a String attribute
          try {
            missingValue = new double[1];
            missingValue[0] = Double.parseDouble(svalue);
            hasMissingValue = true;
          } catch (NumberFormatException ex) {
            logger.debug("String missing_value not parseable as double = {}", missingValueAtt.getStringValue());
          }
        }
      } else { // not a string
        DataType missType = getAttributeDataType(missingValueAtt);
        
        missingValue = new double[missingValueAtt.getLength()];
        for (int i = 0; i < missingValue.length; i++) {
          missingValue[i] = convertUnsigned(missingValueAtt.getNumericValue(i), missType).doubleValue();
          missingValue[i] = applyScaleOffset(missingValue[i]);
        }
        logger.debug("missing_data: {}", Arrays.toString(missingValue));
        
        for (double mv : missingValue) {
          if (!Double.isNaN(mv)) {
            hasMissingValue = true;   // dont need to do anything if its already a NaN
            break;
          }
        }
      }
    }

    /// assign convertedDataType if needed
    if (hasScaleOffset) {
      scaledOffsetType = largestOf(unsignedConversionType, scaleType, offsetType).withSignedness(signedness);
      logger.debug("assign scaledOffsetType = {}", scaledOffsetType);

      // validData may be packed or unpacked
      if (hasValidData()) {
        if (rank(validType) == rank(largestOf(scaleType, offsetType)) &&
                rank(validType) > rank(unsignedConversionType)) {
          // If valid_range is the same type as the wider of scale_factor and add_offset, PLUS
          // it is wider than the (packed) data, we know that the valid_range values were stored as unpacked.
          // We already assumed that this was the case when we first read the attribute values, so there's
          // nothing for us to do here.
        } else {
          // Otherwise, the valid_range values were stored as packed. So now we must unpack them.
          if (hasValidRange || hasValidMin) {
            validMin = applyScaleOffset(validMin);
          }
          if (hasValidRange || hasValidMax) {
            validMax = applyScaleOffset(validMax);
          }
        }
      }
    }
  }
  
  // Get the data type of an attribute. Make it unsigned if the variable is unsigned.
  private DataType getAttributeDataType(Attribute attribute) {
    DataType dataType = attribute.getDataType();
    if (signedness == Signedness.UNSIGNED) {
      // If variable is unsigned, make its integral attributes unsigned too.
      dataType = dataType.withSignedness(signedness);
    }
    return dataType;
  }
  
  /**
   * Returns a distinct integer for each of the {@link DataType#isNumeric() numeric} data types that can be used to
   * (roughly) order them by the range of the DataType. {@code BYTE < UBYTE < SHORT < USHORT < INT < UINT <
   * LONG < ULONG < FLOAT < DOUBLE}. {@code -1} will be returned for all non-numeric data types.
   *
   * @param dataType  a numeric data type.
   * @return  a distinct integer for each of the numeric data types that can be used to (roughly) order them by size.
   */
  public static int rank(DataType dataType) {
    if (dataType == null) {
      return -1;
    }

    switch (dataType) {
      case BYTE:   return 0;
      case UBYTE:  return 1;
      case SHORT:  return 2;
      case USHORT: return 3;
      case INT:    return 4;
      case UINT:   return 5;
      case LONG:   return 6;
      case ULONG:  return 7;
      case FLOAT:  return 8;
      case DOUBLE: return 9;
      default:     return -1;
    }
  }
  
  /**
   * Returns the data type that is the largest among the arguments. Relative sizes of data types are determined via
   * {@link #rank(DataType)}.
   *
   * @param dataTypes  an array of numeric data types.
   * @return  the data type that is the largest among the arguments.
   */
  public static DataType largestOf(DataType... dataTypes) {
    DataType widest = null;
    for (DataType dataType : dataTypes) {
      if (widest == null) {
        widest = dataType;
      } else if (rank(dataType) > rank(widest)) {
        widest = dataType;
      }
    }
    return widest;
  }
  
  /**
   * Returns the smallest numeric data type that:
   * <ol>
   *     <li>can hold a larger integer than {@code dataType} can</li>
   *     <li>if integral, has the same signedness as {@code dataType}</li>
   * </ol>
   * The relative sizes of data types are determined in a manner consistent with {@link #rank(DataType)}.
   * <p/>
   * <table border="1">
   *     <tr>  <th>Argument</th>  <th>Result</th>  </tr>
   *     <tr>  <td>BYTE</td>      <td>SHORT</td>   </tr>
   *     <tr>  <td>UBYTE</td>     <td>USHORT</td>  </tr>
   *     <tr>  <td>SHORT</td>     <td>INT</td>     </tr>
   *     <tr>  <td>USHORT</td>    <td>UINT</td>    </tr>
   *     <tr>  <td>INT</td>       <td>LONG</td>    </tr>
   *     <tr>  <td>UINT</td>      <td>ULONG</td>   </tr>
   *     <tr>  <td>LONG</td>      <td>DOUBLE</td>  </tr>
   *     <tr>  <td>ULONG</td>     <td>DOUBLE</td>  </tr>
   *     <tr>
   *         <td>Any other data type</td>
   *         <td>Just return argument</td>
   *     </tr>
   * </table>
   * <p/>
   * The returned type is intended to be just big enough to hold the result of performing an unsigned conversion of a
   * value of the smaller type. For example, the {@code byte} value {@code -106} equals {@code 150} when interpreted
   * as unsigned. That won't fit in a (signed) {@code byte}, but it will fit in a {@code short}.
   *
   * @param dataType  an integral data type.
   * @return  the next larger type.
   */
  public static DataType nextLarger(DataType dataType) {
    switch (dataType) {
      case BYTE:   return SHORT;
      case UBYTE:  return USHORT;
      case SHORT:  return INT;
      case USHORT: return UINT;
      case INT:    return LONG;
      case UINT:   return ULONG;
      case LONG:   return DOUBLE;
      case ULONG:  return DOUBLE;
      default:     return dataType;
    }
  }

  @Override public double getScaleFactor() {
    return scale;
  }

  @Override public double getOffset() {
    return offset;
  }
  
  @Override public Signedness getSignedness() {
    return signedness;
  }
  
  @Override public DataType getScaledOffsetType() {
    return scaledOffsetType;
  }
  
  @Nonnull @Override public DataType getUnsignedConversionType() {
    return unsignedConversionType;
  }

  @Override
  public boolean hasValidData() {
    return hasValidRange || hasValidMin || hasValidMax;
  }
  
  @Override
  public double getValidMin() {
    return validMin;
  }
  
  @Override
  public double getValidMax() {
    return validMax;
  }
  
  @Override
  public boolean isInvalidData(double val) {
    // valid_min and valid_max may have been multiplied by scale_factor, which could be a float, not a double.
    // That potential loss of precision means that we cannot do the nearlyEquals() comparison with
    // Misc.defaultMaxRelativeDiffDouble.
    boolean greaterThanOrEqualToValidMin =
        Misc.nearlyEquals(val, validMin, Misc.defaultMaxRelativeDiffFloat) || val > validMin;
    boolean lessThanOrEqualToValidMax =
        Misc.nearlyEquals(val, validMax, Misc.defaultMaxRelativeDiffFloat) || val < validMax;

    return (hasValidRange && !(greaterThanOrEqualToValidMin && lessThanOrEqualToValidMax)) ||
           (hasValidMin   && !greaterThanOrEqualToValidMin) ||
           (hasValidMax   && !lessThanOrEqualToValidMax);
  }
  
  @Override
  public boolean hasFillValue() {
    return hasFillValue;
  }
  
  @Override
  public boolean isFillValue(double val) {
    return hasFillValue && Misc.nearlyEquals(val, fillValue, Misc.defaultMaxRelativeDiffFloat);
  }
  
  @Override
  public double getFillValue() {
    return fillValue;
  }
  
  @Override
  public boolean hasScaleOffset() {
    return hasScaleOffset;
  }
  
  @Override
  public boolean hasMissingValue() {
    return hasMissingValue;
  }
  
  @Override
  public boolean isMissingValue(double val) {
    if (!hasMissingValue) {
      return false;
    }
    for (double aMissingValue : missingValue) {
      if (Misc.nearlyEquals(val, aMissingValue, Misc.defaultMaxRelativeDiffFloat)) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public double[] getMissingValues() {
    return missingValue;
  }
  
  @Override
  public void setFillValueIsMissing(boolean b) {
    this.fillValueIsMissing = b;
  }
  
  @Override
  public void setInvalidDataIsMissing(boolean b) {
    this.invalidDataIsMissing = b;
  }
  
  @Override
  public void setMissingDataIsMissing(boolean b) {
    this.missingDataIsMissing = b;
  }
  
  @Override
  public boolean hasMissing() {
    return (invalidDataIsMissing && hasValidData()) || (fillValueIsMissing && hasFillValue()) ||
        (missingDataIsMissing && hasMissingValue());
  }
  
  @Override
  public boolean isMissing(double val) {
    if (Double.isNaN(val)) {
      return true;
    } else {
      return (missingDataIsMissing && isMissingValue(val)) || (fillValueIsMissing && isFillValue(val)) ||
             (invalidDataIsMissing && isInvalidData(val));
    }
  }
  
  
  @Override
  public Number convertUnsigned(Number value) {
    return convertUnsigned(value, signedness);
  }
  
  private static Number convertUnsigned(Number value, DataType dataType) {
    return convertUnsigned(value, dataType.getSignedness());
  }
  
  private static Number convertUnsigned(Number value, Signedness signedness) {
    if (signedness == Signedness.UNSIGNED) {
      // Handle integral types that should be treated as unsigned by widening them if necessary.
      return DataType.widenNumberIfNegative(value);
    } else {
      return value;
    }
  }
  
  @Override
  public Array convertUnsigned(Array in) {
    return convert(in, true, false, false);
  }

  @Override
  public double applyScaleOffset(Number value) {
    double convertedValue = value.doubleValue();
    return hasScaleOffset ? scale * convertedValue + offset : convertedValue;
  }
  
  @Override
  public Array applyScaleOffset(Array in) {
    return convert(in, false, true, false);
  }
  
  @Override
  public Number convertMissing(Number value) {
    return isMissing(value.doubleValue()) ? Double.NaN : value;
  }
  
  @Override
  public Array convertMissing(Array in) {
    return convert(in, false, false, true);
  }
  
  @Override
  public Array convert(Array in, boolean convertUnsigned, boolean applyScaleOffset, boolean convertMissing) {
    if (!in.getDataType().isNumeric() || (!convertUnsigned && !applyScaleOffset && !convertMissing)) {
      return in;  // Nothing to do!
    }
    
    if (getSignedness() == Signedness.SIGNED) {
      convertUnsigned = false;
    }
    if (!hasScaleOffset()) {
      applyScaleOffset = false;
    }
    
    DataType outType = origDataType;
    if (convertUnsigned) {
      outType = getUnsignedConversionType();
    }
    if (applyScaleOffset) {
      outType = getScaledOffsetType();
    }
  
    if (outType != DataType.FLOAT && outType != DataType.DOUBLE) {
      convertMissing = false;
    }
    
    Array out = Array.factory(outType, in.getShape());
    IndexIterator iterIn = in.getIndexIterator();
    IndexIterator iterOut = out.getIndexIterator();
  
    while (iterIn.hasNext()) {
      Number value = (Number) iterIn.getObjectNext();
      
      if (convertUnsigned) {
        value = convertUnsigned(value);
      }
      if (applyScaleOffset) {
        value = applyScaleOffset(value);
      }
      if (convertMissing) {
        value = convertMissing(value);
      }
      
      iterOut.setObjectNext(value);
    }
  
    return out;
  }
}
