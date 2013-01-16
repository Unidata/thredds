package ucar.nc2.ft.grid;

import java.util.List;

/**
 * Description
 *
 * @author John
 * @since 12/25/12
 */
public interface CoverageDataset extends ucar.nc2.ft.FeatureDataset {

  /** get the list of GridDatatype objects contained in this dataset.
   * @return  list of GridDatatype
   */
  public List<Coverage> getCoverages();

  /** find the named GridDatatype.
   * @param name full unescaped name
   * @return  the named GridDatatype, or null if not found
   */
  public Coverage findCoverage(String name);

  /**
   * Return GridDatatype objects grouped by GridCoordSystem. All GridDatatype in a Gridset
   *   have the same GridCoordSystem.
   * @return List of type GridDataset.Gridset
   */
  public List<CoverageSet> getCoverageSets();

  /**
   * A set of GridDatatype objects with the same Coordinate System.
   */
  public interface CoverageSet {

    /** Get list of Coverage objects with same Coordinate System
     * @return list of GridDatatype
     */
    public List<Coverage> getCoverages();

    /** all the Coverage in this Gridset use this CoverageCS
     * @return  the common CoverageCS
     */
    public ucar.nc2.ft.grid.CoverageCS getCoverageCS();
  }

}
