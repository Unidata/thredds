
package ucar.nc2.dt;

import java.util.*;

/**
 * A dataset containing Grid objects.
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public interface GridDataset extends ucar.nc2.dt.TypedDataset {

  /** get the list of GridDatatype objects contained in this dataset. */
  public List getGrids();

  /** find the named GridDatatype. */
  public GridDatatype findGridDatatype( String name);

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
    public ucar.nc2.dt.GridCoordSystem getGeoCoordSystem();
  }

}
