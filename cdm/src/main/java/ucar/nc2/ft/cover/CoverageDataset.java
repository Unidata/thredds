package ucar.nc2.ft.cover;

import java.util.List;

/**
 * A FeatureDataset of Coverage objects
 *
 * @author John
 * @since 12/25/12
 */
public interface CoverageDataset extends ucar.nc2.ft.FeatureDataset {

  /** get the list of Coverage objects contained in this dataset.
   * @return  list of Coverage
   */
  public List<Coverage> getCoverages();

  /** find the named Coverage.
   * @param name full unescaped name
   * @return  the named Coverage, or null if not found
   */
  public Coverage findCoverage(String name);

  /**
   * Return Coverage objects grouped by CoverageSet. All Coverage in a CoverageSet
   *   have the same CoverageCS.
   * @return List of type GridDataset.Gridset
   */
  public List<CoverageSet> getCoverageSets();

  /**
   * A set of Coverage objects with the same Coordinate System.
   */
  public interface CoverageSet {

    /** Get list of Coverage objects with same Coordinate System
     * @return list of Coverage
     */
    public List<Coverage> getCoverages();

    /** all the Coverage in this CoverageSet use this CoverageCS
     * @return  the common CoverageCS
     */
    public ucar.nc2.ft.cover.CoverageCS getCoverageCS();
  }

}
