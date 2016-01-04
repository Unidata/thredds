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

package ucar.nc2.dt;

import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;

import java.io.Closeable;
import java.util.Date;
import java.util.List;

/**
 * Superclass for "scientific type" datasets.
 *
 * This interface defines general "discovery metadata".
 * Its subtypes define type-specific information.
 * Implementations may or may not have a NetcdfFile underneath.
 *
 * @deprecated use ucar.nc2.ft.*
 * @author caron
 */

public interface TypedDataset extends Closeable {

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

  /** @return the boundingBox for the entire dataset. */
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
