package ucar.nc2.ft2.coverage.grid;

import ucar.nc2.Attribute;

import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 5/4/2015
 */
public interface GridCoverageDatasetIF {
  String getName();

  List<GridCoverage> getGrids();

  List<Attribute> getGlobalAttributes();

  List<GridCoordSys> getCoordSys();

  List<GridCoordTransform> getCoordTransforms();

  List<GridCoordAxis> getCoordAxes();

  GridCoverage findCoverage(String name);

}
