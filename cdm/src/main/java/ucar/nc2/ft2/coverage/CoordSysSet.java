/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import javax.annotation.concurrent.Immutable;

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
