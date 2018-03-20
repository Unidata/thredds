/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import java.io.IOException;

/**
 * Reads Coord Axis values, for lazy evaluation.
 *
 * @author caron
 * @since 7/13/2015
 */
public interface CoordAxisReader {

  double[] readCoordValues(CoverageCoordAxis coordAxis) throws IOException;

}
