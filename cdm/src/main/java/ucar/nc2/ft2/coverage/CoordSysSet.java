/* Copyright */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of coverages with the same coordsys.
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class CoordSysSet {
  private final CoverageCoordSys coordSys;
  private final List<Coverage> coverages = new ArrayList<>();

  public CoordSysSet(CoverageCoordSys coordSys) {
    this.coordSys = coordSys;
  }

  void addCoverage(Coverage cov) {
    coverages.add(cov);
  }

  public CoverageCoordSys getCoordSys() {
    return coordSys;
  }

  public List<Coverage> getCoverages() {
    return coverages;
  }
}
