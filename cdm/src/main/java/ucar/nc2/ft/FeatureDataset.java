/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft;

import java.io.Closeable;
import java.util.List;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.cache.FileCacheable;
import javax.annotation.Nullable;

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
  FeatureType getFeatureType();

  /**
   * Title of the dataset.
   *
   * @return the title of the dataset, or null
   */
  String getTitle();

  /**
   * Text information about this dataset.
   *
   * @return any text information about this dataset, or null.
   */
  String getDescription();

  /**
   * The URI location of the dataset
   *
   * @return the URI location of the dataset, or null
   */
  String getLocation();

  /**
   * Calendar Date range for the entire dataset.
   *
   * @return the date range for the entire dataset, or null if unknown
   */
  CalendarDateRange getCalendarDateRange();

  /**
   * Starting Calendar date for the entire dataset.
   *
   * @return the starting date for the entire dataset, or null if unknown
   */
  CalendarDate getCalendarDateStart();

  /**
   * Ending Calendar date for the entire dataset.
   *
   * @return the ending date for the entire dataset, or null if unknown
   */
  CalendarDate getCalendarDateEnd();

  /**
   * The lat/lon boundingBox for the entire dataset.
   *
   * @return the lat/lon boundingBox for the entire dataset, or null if unknown.
   */
  ucar.unidata.geoloc.LatLonRect getBoundingBox();

  /**
   * List of global attributes.
   *
   * @return List of type ucar.nc2.Attribute, may be empty but not null
   */
  List<ucar.nc2.Attribute> getGlobalAttributes();

  /**
   * Return the global attribute with the given name, ignoring case.
   *
   * @param name attribute name
   * @return the global attribute, or null
   */
  ucar.nc2.Attribute findGlobalAttributeIgnoreCase(String name);

  /**
   * The data Variables available in this dataset.
   * Should just be data variables others might be searching for, not metadata or coordinate
   * system variables, etc.
   * The shapes of the VariableSimpleIF do not necessarily match the StructureData member.
   *
   * @return List of subclass of VariableSimpleIF, may be empty but not null
   */
  List<VariableSimpleIF> getDataVariables();

  /**
   * Get the named data Variable.
   *
   * @param shortName of data Variable.
   * @return VariableSimpleIF or null if not found
   */
  VariableSimpleIF getDataVariable(String shortName);

  /**
   * Return underlying NetcdfFile, or null if none.
   *
   * @return the underlying NetcdfFile, or null if none.
   */
  @Nullable
  ucar.nc2.NetcdfFile getNetcdfFile();

  /**
   * Close all resources associated with this dataset.
   *
   * @throws java.io.IOException on i/o error
   */
  void close() throws java.io.IOException;

  /**
   * Show debug / underlying implementation details
   *
   * @param sf append info here
   */
  void getDetailInfo(java.util.Formatter sf);

  /**
   * Show who is implementing
   * @return name of implementor
   */
  String getImplementationName();

}
