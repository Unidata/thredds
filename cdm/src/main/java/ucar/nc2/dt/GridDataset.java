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
