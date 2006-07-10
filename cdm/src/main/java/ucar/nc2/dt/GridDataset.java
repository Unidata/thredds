
package ucar.nc2.dt;

import ucar.nc2.dt.grid.Grid;

import java.util.*;

/**
 * A dataset containing Grid objects.
 * @author caron
 * @version $Revision: 1.2 $ $Date: 2005/03/04 19:36:18 $
 */

public interface GridDataset extends ucar.nc2.dt.TypedDataset {

  /** get the list of GridDatatype objects contained in this dataset. */
  public List getGrids();

  /** find the named GridDatatype. */
  public Grid getGrid( String name);

  /**
   * Return GridDatatype objects grouped by GridCoordSys. All GridDatatype in a Gridset
   *   have the same GridCoordSys.
   * @return Collection of type GridDataset.Gridset
   */
  public Collection getGridSets();


  /**
   * A set of GridDatatype objects with the same Coordinate System.
   */
  public interface Gridset {
    /** Get list of GeoGrid objects */
    public List getGrids();

    /** all GridDatatype point to this GeoCoordSysImpl */
    public ucar.nc2.dt.grid.GridCoordSystem getGeoCoordSys();
  }

}
