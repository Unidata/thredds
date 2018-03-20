/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.mcidas;


import edu.wisc.ssec.mcidas.GridDirectory;
import edu.wisc.ssec.mcidas.McIDASException;
import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.nc2.iosp.grid.*;

import java.util.Formatter;


/**
 * A class to hold McIDAS grid record information
 *
 * @author dmurray
 */
public class McIDASGridRecord extends GridDirectory implements GridRecord {

  /**
   * offset to header
   */
  private int offsetToHeader;  // offset to header

  /**
   * grid definition
   */
  private McGridDefRecord gridDefRecord;

  /**
   * decimal scale
   */
  private int decimalScale = 0;

  /**
   * Create a grid header from the integer bits
   *
   * @param offset offset to grid header in file
   * @param header header words
   * @throws McIDASException Problem creating the record
   */
  public McIDASGridRecord(int offset, int[] header) throws McIDASException {
    super(header);
    gridDefRecord = new McGridDefRecord(header);
    offsetToHeader = offset;
  }

  /**
   * Get the first level of this GridRecord
   *
   * @return the first level value
   */
  public double getLevel1() {
    return getLevelValue();
  }

  /**
   * Get the second level of this GridRecord
   *
   * @return the second level value
   */
  public double getLevel2() {
    return getSecondLevelValue();
  }

  /**
   * Get the type for the first level of this GridRecord
   *
   * @return level type
   */
  public int getLevelType1() {
    // TODO:  flush this out
    int gribLevel = getDirBlock()[51];
    int levelType = 0;
    if (!((gribLevel == McIDASUtil.MCMISSING) || (gribLevel == 0))) {
      levelType = gribLevel;
    } else {
      levelType = 1;
    }
    return levelType;
  }

  /**
   * Get the type for the second level of this GridRecord
   *
   * @return level type
   */
  public int getLevelType2() {
    return getLevelType1();
  }

  /**
   * Get valid time offset (minutes) of this GridRecord
   *
   * @return time offset
   */
  public int getValidTimeOffset() {
    return getForecastHour();
  }

  /**
   * Get the parameter name
   *
   * @return parameter name
   */
  public String getParameterName() {
    return getParamName();
  }

  /**
   * Get the parameter description
   *
   * @return parameter description
   */
  public String getParameterDescription() {
    return getParamName();
  }

  /**
   * Get the decimal scale
   *
   * @return decimal scale
   */
  public int getDecimalScale() {
    return decimalScale;
  }

  /**
   * Get the grid def record id
   *
   * @return parameter name
   */
  public String getGridDefRecordId() {
    return gridDefRecord.toString();
  }

  /**
   * Get the grid def record id
   *
   * @return parameter name
   */
  public McGridDefRecord getGridDefRecord() {
    return gridDefRecord;
  }

  /**
   * Get the offset to the grid header (4 byte words)
   *
   * @return word offset
   */
  public int getOffsetToHeader() {
    return offsetToHeader;
  }

  /**
   * Had GRIB info?
   *
   * @return true if has grib info
   */
  public boolean hasGribInfo() {
    int gribSection = getDirBlock()[48];
    return (!((gribSection == McIDASUtil.MCMISSING) || (gribSection == 0)));
  }

  @Override
  public int getTimeUnit() {
    return 0;
  }

  @Override
  public String getTimeUdunitName() {
    return "minutes";
  }

  @Override
  // this determines how records get grouped into a cdm variable
  // unique parameter name and level type.
  public int cdmVariableHash() {
    return getParamName().hashCode() + 37 * getLevelType1();
  }

  @Override
  public String cdmVariableName(GridTableLookup lookup, boolean useLevel, boolean useStat) {
    Formatter f = new Formatter();
    f.format("%s", getParameterName());

    if (useLevel) {
      String levelName = lookup.getLevelName(this);
      if (levelName.length() != 0) {
        if (lookup.isLayer(this))
          f.format("_%s_layer", lookup.getLevelName(this));
        else
          f.format("_%s", lookup.getLevelName(this));
      }
    }

    return f.toString();
  }

}

