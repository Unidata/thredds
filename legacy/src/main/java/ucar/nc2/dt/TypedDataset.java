/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt;

import java.io.Closeable;
import java.util.Date;
import java.util.List;

import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;

/**
 * Superclass for "scientific type" datasets.
 *
 * This interface defines general "discovery metadata".
 * Its subtypes define type-specific information.
 * Implementations may or may not have a NetcdfFile underneath.
 *
 * @deprecated use ucar.nc2.ft.FeatureCollection
 * @author caron
 */

public interface TypedDataset extends Closeable {

  /** @return Title of the dataset. */
  String getTitle();

  /** @return Text information about this dataset. */
  String getDescription();

  /** @return The URI location of the dataset */
  String getLocationURI();

  /** @return Start date for the entire dataset. */
  Date getStartDate();

  /** @return End date for the entire dataset. */
  Date getEndDate();

  /** @return the boundingBox for the entire dataset. */
  ucar.unidata.geoloc.LatLonRect getBoundingBox();

  /** List of global attributes.
   * @return List of type ucar.nc2.Attribute */
  List<Attribute> getGlobalAttributes();

  /** @return the global attribute with the given name, ingnoring case.
   * @param name attribute name
   */
  ucar.nc2.Attribute findGlobalAttributeIgnoreCase( String name );

  /** The data Variables available in this dataset.
   * Should just be data variable others might be searching for, not metadata or coordinate
   * system variables, etc.
   * The shape of this VariableSimpleIF does not necessarily match the 
   * @return List of type VariableSimpleIF */
  List<VariableSimpleIF> getDataVariables();

  /** Get the named data Variable.
   * @param shortName of data Variable.
   * @return VariableSimpleIF or null. */
  VariableSimpleIF getDataVariable( String shortName);

  /** @return  underlying NetcdfFile, or null if none. */
  ucar.nc2.NetcdfFile getNetcdfFile();

  /** Close all resources associated with this dataset.
   * @throws java.io.IOException on I/O error
   */
  @Override
  void close() throws java.io.IOException;

  /** @return  debug / underlying implementation details */
  String getDetailInfo();
}
