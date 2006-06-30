
package ucar.nc2.dt;

import java.util.*;

/**
 * A dataset containing GridDatatype objects.
 * @author caron
 * @version $Revision: 1.2 $ $Date: 2005/03/04 19:36:18 $
 */

public interface GridDataset extends ucar.nc2.dt.TypedDataset {

  /** get the list of GridDatatype objects contained in this dataset. */
  public List getGrids();

  /** find the named GridDatatype. */
  public GridDatatype getGrid( String name);

  /**
   * Return GridDatatype objects grouped by GridCoordSys. All GridDatatype in a Gridset
   *   have the same GridCoordSys.
   * @return List of type GridDataset.Gridset
   */
  public List getGridSets();


  /**
   * A set of GridDatatype objects with the same Coordinate System.
   */
  public interface Gridset {
    /** Get list of GeoGrid objects */
    public List getGrids();

    /** all GridDatatype point to this GeoCoordSysImpl */
    public ucar.nc2.dt.grid.GridCoordSys getGeoCoordSys();
  }

}
