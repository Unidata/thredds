/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.nc2.ft;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateRange;

import java.util.*;

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

public interface FeatureDataset {

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
  public String getLocationURI();

  /**
   * Date range for the entire dataset.
   *
   * @return the date range for the entire dataset, or null if unknown
   */
  public DateRange getDateRange();

  /**
   * Starting date for the entire dataset.
   *
   * @return the starting date for the entire dataset, or null if unknown
   */
  public Date getStartDate();

  /**
   * Ending date for the entire dataset.
   *
   * @return the ending date for the entire dataset, or null if unknown
   */
  public Date getEndDate();

  /**
   * The boundingBox for the entire dataset.
   *
   * @return the lat/lon boundingBox for the entire dataset, or null if unknown.
   */
  public ucar.unidata.geoloc.LatLonRect getBoundingBox();

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
   * The shape of this VariableSimpleIF does not necessarily match the StuctureData member.
   *
   * @return List of subclass of VariableSimpleIF, may be empty but not null
   */
  public List<? extends VariableSimpleIF> getDataVariables();

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

}