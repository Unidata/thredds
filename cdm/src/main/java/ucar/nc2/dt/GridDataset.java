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

import java.util.*;

/**
 * A dataset containing Grid objects.
 * @author caron
 */

public interface GridDataset extends ucar.nc2.dt.TypedDataset {

  /** get the list of GridDatatype objects contained in this dataset.
   * @return  list of GridDatatype
   */
  public List<GridDatatype> getGrids();

  /** find the named GridDatatype.
   * @param name look for this name
   * @return  the named GridDatatype, or null if not found
   */
  public GridDatatype findGridDatatype( String name);

  /**
   * Return GridDatatype objects grouped by GridCoordSystem. All GridDatatype in a Gridset
   *   have the same GridCoordSystem.
   * @return List of type GridDataset.Gridset
   */
  public List<Gridset> getGridsets();

  /**
   * A set of GridDatatype objects with the same Coordinate System.
   */
  public interface Gridset {

    /** Get list of GridDatatype objects with same Coordinate System
     * @return list of GridDatatype
     */
    public List<GridDatatype> getGrids();

    /** all the GridDatatype in this Gridset use this GridCoordSystem
     * @return  the common GridCoordSystem
     */
    public ucar.nc2.dt.GridCoordSystem getGeoCoordSystem();
  }

}
