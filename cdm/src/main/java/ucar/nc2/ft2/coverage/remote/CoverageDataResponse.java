/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage.remote;

import ucar.nc2.ft2.coverage.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 7/15/2015
 */
class CoverageDataResponse implements CoordSysContainer {

  public List<CoverageCoordAxis> axes;
  public List<CoverageCoordSys> coordSys;
  public List<CoverageTransform> transforms;

  public List<GeoReferencedArray> arrayResponse;

  public CoverageDataResponse(List<CoverageCoordAxis> axes, List<CoverageCoordSys> coordSys, List<CoverageTransform> transforms) {
    this.axes = axes;
    this.coordSys = coordSys;
    this.transforms = transforms;
    this.arrayResponse = new ArrayList<>(); // set after the constructor is done, because we need to put coordsys in geoArray

    for (CoverageCoordSys csys : coordSys) {
      csys.setDataset(this); // LOOK More that should be done ??
      csys.setHorizCoordSys(csys.makeHorizCoordSys());
    }
  }

  public CoverageCoordSys findCoordSys(String csysName) {
    for (CoverageCoordSys csys : coordSys)
      if (csys.getName().equalsIgnoreCase(csysName)) return csys;
    return null;
  }

  public CoverageTransform findCoordTransform(String transformName) {
    for (CoverageTransform ct : transforms)
      if (ct.getName().equalsIgnoreCase(transformName)) return ct;
    return null;
  }

  public CoverageCoordAxis findCoordAxis(String axisName) {
    for (CoverageCoordAxis axis : axes) {
      if (axis.getName().equalsIgnoreCase(axisName)) return axis;
    }
    return null;
  }
}
