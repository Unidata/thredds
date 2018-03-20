/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft2.coverage.adapter;

import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateSystem;

/**
 * Curvilinear Coordinate System.
 *
 * @author John
 * @since 12/25/12
 */
public class CurvilinearCS extends DtCoverageCS {

  protected CurvilinearCS(DtCoverageCSBuilder builder) {
    super(builder);
  }

  public CoordinateAxis2D getLatAxis() {
    return (CoordinateAxis2D) super.getXHorizAxis();
  }

  public CoordinateAxis2D getLonAxis() {
    return (CoordinateAxis2D) super.getYHorizAxis();
  }

  public int getDomainRank() {
    return 2 + CoordinateSystem.makeDomain(builder.independentAxes).size();
  }


}
