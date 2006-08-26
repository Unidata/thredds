
package ucar.nc2.dt;

import java.util.*;

/**
 * A dataset containing Grid objects.
 * @author caron
 */

public interface GridDataset extends ucar.nc2.dt.TypedDataset {

  /** get the list of GridDatatype objects contained in this dataset. */
  public List getGrids();

  /** find the named GridDatatype. */
  public GridDatatype findGridDatatype( String name);

  /**
   * Return GridDatatype objects grouped by GridCoordSystem. All GridDatatype in a Gridset
   *   have the same GridCoordSystem.
   * @return List of type GridDataset.Gridset
   */
  public List getGridSets();


  /**
   * A set of GridDatatype objects with the same Coordinate System.
   */
  public interface Gridset {
    /** Get list of GridDatatype objects */
    public List getGrids();

    /** all the GridDatatype in this set use this GridCoordSystem */
    public ucar.nc2.dt.GridCoordSystem getGeoCoordSystem();
  }

}
