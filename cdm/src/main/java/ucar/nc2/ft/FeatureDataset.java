/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.util.cache.FileCacheable;

import java.io.Closeable;
import java.util.Date;
import java.util.List;

/**
 * Superclass for "scientific feature type" datasets.
 * These are collections of features of the same feature type.
 * <p/>
 * <p/>
 * This interface defines general "discovery metadata".
 * Its subtypes define type-specific information.
 * Implementations may or may not have a NetcdfFile underneath.
 *
 * @author caron
 */

public interface FeatureDataset extends FileCacheable, Closeable {

  /**
   * Contains collections of this FeatureType.
   *
   * @return FeatureType of data
   */
  public FeatureType getFeatureType();

  /**
   * Title of the dataset.
   *
   * @return the title of the dataset, or null
   */
  public String getTitle();

  /**
   * Text information about this dataset.
   *
   * @return any text information about this dataset, or null.
   */
  public String getDescription();

  /**
   * The URI location of the dataset
   *
   * @return the URI location of the dataset, or null
   */
  public String getLocation();

  /**
   * Calendar Date range for the entire dataset.
   *
   * @return the date range for the entire dataset, or null if unknown
   */
  public CalendarDateRange getCalendarDateRange();

  /**
   * Starting Calendar date for the entire dataset.
   *
   * @return the starting date for the entire dataset, or null if unknown
   */
  public CalendarDate getCalendarDateStart();

  /**
   * Ending Calendar date for the entire dataset.
   *
   * @return the ending date for the entire dataset, or null if unknown
   */
  public CalendarDate getCalendarDateEnd();

  /**
   * The boundingBox for the entire dataset.
   *
   * @return the lat/lon boundingBox for the entire dataset, or null if unknown.
   */
  public ucar.unidata.geoloc.LatLonRect getBoundingBox();

  /**
   * Caclulate date range and bounding box, even if the data has to be scanned.
   * This ensures that getDateRange() and getBoundingBox() return non-null.
   * If the collection already knows its date range and bounding box, then this has no effect.
   *
   * @throws java.io.IOException or read error.
   */
  public void calcBounds() throws java.io.IOException;

  /**
   * List of global attributes.
   *
   * @return List of type ucar.nc2.Attribute, may be empty but not null
   */
  public List<ucar.nc2.Attribute> getGlobalAttributes();

  /**
   * Return the global attribute with the given name, ignoring case.
   *
   * @param name attribute name
   * @return the global attribute, or null
   */
  public ucar.nc2.Attribute findGlobalAttributeIgnoreCase(String name);

  /**
   * The data Variables available in this dataset.
   * Should just be data variables others might be searching for, not metadata or coordinate
   * system variables, etc.
   * The shapes of the VariableSimpleIF do not necessarily match the StructureData member.
   *
   * @return List of subclass of VariableSimpleIF, may be empty but not null
   */
  public List<VariableSimpleIF> getDataVariables();

  /**
   * Get the named data Variable.
   *
   * @param shortName of data Variable.
   * @return VariableSimpleIF or null if not found
   */
  public VariableSimpleIF getDataVariable(String shortName);

  /**
   * Return underlying NetcdfFile, or null if none.
   *
   * @return the underlying NetcdfFile, or null if none.
   */
  public ucar.nc2.NetcdfFile getNetcdfFile();

  /**
   * Close all resources associated with this dataset.
   *
   * @throws java.io.IOException on i/o error
   */
  public void close() throws java.io.IOException;

  /**
   * Show debug / underlying implementation details
   *
   * @param sf append info here
   */
  public void getDetailInfo(java.util.Formatter sf);

  /**
   * Show who is implementing
   * @return name of implementor
   */
  public String getImplementationName();

  /////////////////////////////////////////////////
  // deprecated
  /**
   * Date range for the entire dataset.
   *
   * @return the date range for the entire dataset, or null if unknown
   * @deprecated use getCalendarDateRange
   */
  public DateRange getDateRange();

  /**
   * Starting date for the entire dataset.
   *
   * @return the starting date for the entire dataset, or null if unknown
   * @deprecated use getCalendarDateStart
   */
  public Date getStartDate();

  /**
   * Ending date for the entire dataset.
   *
   * @return the ending date for the entire dataset, or null if unknown
   * @deprecated use getCalendarDateEnd
   */
  public Date getEndDate();



}
