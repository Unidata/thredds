/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.grid;

import java.util.Date;

/**
 * An interface for one 2D gridded data record (used by  GRIB, GEMPAK, McIDAS gridded data).
 * A collection of these records comprise a CDM variable, usually adding time and optionally vertical, and ensemble dimensions.
 *
 * @author rkambic
 */
public interface GridRecord {

  /**
   * Get the first vertical level of this GridRecord
   *
   * @return the first vertical level value
   */
  public double getLevel1();

  /**
   * Get the second vertical level of this GridRecord
   *
   * @return the second vertical level value
   */
  public double getLevel2();

  /**
   * Get the vertical level type of this GridRecord
   *
   * @return vertical level type
   */
  public int getLevelType1();

  /**
   * Get the vertical level type of this GridRecord
   *
   * @return vertical level type
   */
  public int getLevelType2();

  /**
   * Get the first reference time of this GridRecord
   *
   * @return reference time
   */
  public Date getReferenceTime();

  /**
   * Get the valid time for this GridRecord.
   *
   * @return valid time
   */
  public Date getValidTime();

  /**
   * Get valid time offset of this GridRecord
   *
   * @return time offset in minutes from getReferenceTime()
   *
  public int getValidTimeOffset(); */

  /**
   * Get the parameter name
   *
   * @return parameter name
   */
  public String getParameterName();

  /**
   * Get the parameter description
   *
   * @return parameter description
   */
  public String getParameterDescription();

  /**
   * Get the grid def record id
   *
   * @return parameter name
   */
  public String getGridDefRecordId();

  /**
   * Get the decimal scale of the values
   *
   * @return decimal scale
   */
  public int getDecimalScale();

  /**
   * Get the time unit as a code, which should be Grib2 table 4.4.
   *
   * @return the time unit as a code
   */
  public int getTimeUnit();


  /**
   * Get the time unit as a String. Must be able to create a udunit like:
   * String udunit = timeUnit + " since " + refDate
   * So this assumes that any conversion factor is applied to the value
   *
   * @return the time unit as a String
   */
  public String getTimeUdunitName();

  /**
   * A hash code to group records into a CDM variable
   *
   * @return group hash code
   */
  public int cdmVariableHash();

  /**
   * A unique name for the CDM variable, must be consistent with cdmVariableHash
   *
   * @return unique CDM variable name
   */
  public String cdmVariableName(GridTableLookup lookup, boolean useLevel, boolean useStat);

}

