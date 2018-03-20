/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

/**
 * Abstraction to allow lightweight datasets.
 * See CoverageCoordSys, CoverageCoordAxis.setDataset()
 *
 * @author caron
 * @since 7/20/2015
 */
public interface CoordSysContainer {

  CoverageTransform findCoordTransform(String transformName);

  CoverageCoordAxis findCoordAxis(String axisName);

}
