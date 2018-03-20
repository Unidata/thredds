/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.wcs.v1_0_0_1;

import thredds.server.wcs.Request;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.time.CalendarDateRange;

public class GetCoverageBuilder extends WcsRequestBuilder {
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger(GetCoverageBuilder.class);

  GetCoverageBuilder(String versionString, Request.Operation operation, CoverageCollection dataset, String datasetPath) {
    super(versionString, operation, dataset, datasetPath);
  }

  private String coverageId, crs, responseCRS;
  private Request.BoundingBox bbox;
  private CalendarDateRange timeRange;
  private WcsCoverage.VerticalRange verticalRange;  // parameter
  private Request.Format format;

  public String getCoverageId() {
    return coverageId;
  }

  public GetCoverageBuilder setCoverageId(String coverageId) {
    this.coverageId = coverageId;
    return this;
  }

  public String getCrs() {
    return crs;
  }

  public GetCoverageBuilder setCrs(String crs) {
    this.crs = crs;
    return this;
  }

  public String getResponseCRS() {
    return responseCRS;
  }

  public GetCoverageBuilder setResponseCRS(String responseCRS) {
    this.responseCRS = responseCRS;
    return this;
  }

  public Request.BoundingBox getBbox() {
    return bbox;
  }

  public GetCoverageBuilder setBbox(Request.BoundingBox bbox) {
    this.bbox = bbox;
    return this;
  }

  public CalendarDateRange getTimeRange() {
    return timeRange;
  }

  public GetCoverageBuilder setTimeRange(CalendarDateRange timeRange) {
    this.timeRange = timeRange;
    return this;
  }

  public WcsCoverage.VerticalRange getVerticalRange() {
    return verticalRange;
  }

  public GetCoverageBuilder setVerticalRange(WcsCoverage.VerticalRange verticalRange) {
    this.verticalRange = verticalRange;
    return this;
  }

  public Request.Format getFormat() {
    return format;
  }

  public GetCoverageBuilder setFormat(Request.Format format) {
    this.format = format;
    return this;
  }

  public GetCoverage buildGetCoverage() throws WcsException {
    return new GetCoverage(this.getOperation(),
            this.getVersionString(),
            this.getWcsDataset(),
            coverageId,
            crs, responseCRS,
            bbox, timeRange, verticalRange,
            format);
  }

}
