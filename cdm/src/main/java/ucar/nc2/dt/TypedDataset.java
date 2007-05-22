/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.nc2.dt;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Attribute;

import java.util.*;

/**
 * Superclass for "scientific type" datasets.
 *
 * This interface defines general "discovery metadata".
 * Its subtypes define type-specific information.
 * Implementations may or may not have a NetcdfFile underneath.
 *
 * @author caron
 */

public interface TypedDataset {

  /** @return Title of the dataset. */
  public String getTitle();

  /** @return Text information about this dataset. */
  public String getDescription();

  /** @return The URI location of the dataset */
  public String getLocationURI();

  /** @return Start date for the entire dataset. */
  public Date getStartDate();

  /** @return End date for the entire dataset. */
  public Date getEndDate();

  /** @return he boundingBox for the entire dataset. */
  public ucar.unidata.geoloc.LatLonRect getBoundingBox();

  /** List of global attributes.
   * @return List of type ucar.nc2.Attribute */
  public List<Attribute> getGlobalAttributes();

  /** @return the global attribute with the given name, ingnoring case.
   * @param name attribute name
   */
  public ucar.nc2.Attribute findGlobalAttributeIgnoreCase( String name );

  /** The data Variables available in this dataset.
   * Should just be data variable others might be searching for, not metadata or coordinate
   * system variables, etc.
   * The shape of this VariableSimpleIF does not necessarily match the 
   * @return List of type VariableSimpleIF */
  public List<VariableSimpleIF> getDataVariables();

  /** Get the named data Variable.
   * @param shortName of data Variable.
   * @return VariableSimpleIF or null. */
  public VariableSimpleIF getDataVariable( String shortName);

  /** @return  underlying NetcdfFile, or null if none. */
  public ucar.nc2.NetcdfFile getNetcdfFile();

  /** Close all resources associated with this dataset.
   * @throws java.io.IOException on I/O error
   */
  public void close() throws java.io.IOException;

  /** @return  debug / underlying implementation details */
  public String getDetailInfo();
}