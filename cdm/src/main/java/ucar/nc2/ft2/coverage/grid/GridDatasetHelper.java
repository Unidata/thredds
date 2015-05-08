/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since 5/8/2015
 */
public class GridDatasetHelper {
  GridCoverageDataset gds;
  GridDatasetHelper(GridCoverageDataset gds) {
    this.gds = gds;
  }

  public static class Gridset {
    GridCoordSys coordSys;
    List<GridCoverage> grids;

    public Gridset(GridCoordSys coordSys) {
      this.coordSys = coordSys;
      grids = new ArrayList<>();
    }
  }

  public List<Gridset> getGridsets() {
    Map<String, Gridset> map = new HashMap<>();

    for (GridCoordSys csys : gds.getCoordSys()) {
      map.put(csys.getName(), new Gridset(csys));
  		}

    for (GridCoverage grid : gds.getGrids()) {
      Gridset gset = map.get(grid.getCoordSysName());
      gset.grids.add(grid);
    }

    List<Gridset> result = new ArrayList<>(map.values());
    Collections.sort(result, new Comparator<Gridset>() {
      @Override
      public int compare(Gridset o1, Gridset o2) {
        return o1.coordSys.getName().compareTo(o2.coordSys.getName());
      }
    });

 		return result;
 	}
}
