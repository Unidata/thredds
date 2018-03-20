/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.wcs.v1_0_0_1;

import thredds.server.wcs.Request;
import ucar.nc2.ft2.coverage.CoverageCollection;

import java.util.List;

public class DescribeCoverageBuilder extends WcsRequestBuilder {
  private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DescribeCoverageBuilder.class);

  DescribeCoverageBuilder(String versionString, Request.Operation operation, CoverageCollection dataset, String datasetPath) {
    super(versionString, operation, dataset, datasetPath);
  }

  private List<String> coverageIdList;

  public List<String> getCoverageIdList() {
    return coverageIdList;
  }

  public DescribeCoverageBuilder setCoverageIdList(List<String> coverageIdList) {
    this.coverageIdList = coverageIdList;
    return this;
  }

  public DescribeCoverage buildDescribeCoverage()
          throws WcsException {
    if (this.coverageIdList == null)
      throw new IllegalStateException("Null coverage list not allowed.");
    if (this.coverageIdList.isEmpty())
      throw new IllegalStateException("Empty coverage list not allowed.");

    return new DescribeCoverage(this.getOperation(), this.getVersionString(),
            this.getWcsDataset(), this.coverageIdList);
  }
}
