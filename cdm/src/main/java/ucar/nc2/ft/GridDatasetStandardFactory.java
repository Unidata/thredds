/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.adapter.DtCoverageAdapter;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCSBuilder;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.Formatter;

/**
 * FeatureDatasetFactory for Grids, using standard coord sys analysis and ucar.nc2.ft2.coverage.adapter.DtCoverageCSBuilder
 * @author caron
 * @since Dec 30, 2008
 */
public class GridDatasetStandardFactory implements FeatureDatasetFactory {

  public Object isMine(FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException {
    DtCoverageCSBuilder dtCoverage = DtCoverageCSBuilder.classify(ncd, errlog);
    if (dtCoverage == null || dtCoverage.getCoverageType() == null) return null;
    if (!match(wantFeatureType, dtCoverage.getCoverageType())) return null;
    return dtCoverage;
  }

  private boolean match(FeatureType wantFeatureType, FeatureType covType) {
    if (wantFeatureType == null || wantFeatureType == FeatureType.ANY) return true;
    // LOOK ever have to return false?
    return true;
  }

  public FeatureDataset open(FeatureType ftype, NetcdfDataset ncd, Object analysis, CancelTask task, Formatter errlog) throws IOException {
    // already been opened by isMine
    // DtCoverageCSBuilder dtCoverage =  (DtCoverageCSBuilder) analysis;

    // look - use GridDataset 2/24/2016
    //DtCoverageDataset dt = DtCoverageDataset.open(ncd);
    //return DtCoverageAdapter.factory(dt, errlog);

    return new ucar.nc2.dt.grid.GridDataset( ncd);
  }

  public FeatureType[] getFeatureTypes() {
    return new FeatureType[] {FeatureType.GRID, FeatureType.FMRC, FeatureType.SWATH};
  }
}
