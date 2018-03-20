/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.wcs.v1_0_0_1;

import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageCollection;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class WcsDataset {
  private String datasetPath;
  private String datasetName;
  private CoverageCollection dataset;
  private Map<String, WcsCoverage> availableCoverages;

  public WcsDataset(CoverageCollection dataset, String datasetPath) {
    this.datasetPath = datasetPath;
    int pos = datasetPath.lastIndexOf("/");
    this.datasetName = (pos > 0) ? datasetPath.substring(pos + 1) : datasetPath;
    this.dataset = dataset;

    this.availableCoverages = new HashMap<>();

    // ToDo WCS 1.0PlusPlus - compartmentalize coverage to hide GridDatatype vs GridDataset.Gridset ???
    // ToDo WCS 1.0Plus - change FROM coverage for each parameter TO coverage for each coordinate system
    // This is WCS 1.0 coverage for each parameter
    for (Coverage coverage : this.dataset.getCoverages()) {
      CoverageCoordSys gcs = coverage.getCoordSys();
      if (!gcs.isRegularSpatial()) continue;
      this.availableCoverages.put(coverage.getName(), new WcsCoverage(coverage, gcs, this));
    }
    // ToDo WCS 1.0Plus - change FROM coverage for each parameter TO coverage for each coordinate system
    // This is WCS 1.1 style coverage for each coordinate system
    // for ( GridDataset.Gridset curGridSet : this.dataset.getGridsets())
    // {
    //   GridCoordSystem gcs = curGridSet.getGeoCoordSystem();
    //   if ( !gcs.isRegularSpatial() )
    //     continue;
    //   this.availableCoverages.put( gcs.getName(), curGridSet );
    // }
  }

  public String getDatasetPath() {
    return datasetPath;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public CoverageCollection getDataset() {
    return dataset;
  }

  public void close() throws IOException {
    if (this.dataset != null)
      this.dataset.close();
  }

  public boolean isAvailableCoverageName(String fullName) {
    return availableCoverages.containsKey(fullName);
  }

  public WcsCoverage getAvailableCoverage(String fullName) {
    return availableCoverages.get(fullName);
  }

  public Collection<WcsCoverage> getAvailableCoverageCollection() {
    return Collections.unmodifiableCollection(availableCoverages.values());
  }
}
