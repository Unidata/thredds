/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import ucar.nc2.Variable;

/**
 * Pluggable component for deciding how to chunk netcdf-4 variables.
 *
 * @author caron
 * @since 11/14/12
 */
public interface Nc4Chunking {

  enum Strategy {standard, grib, none }

  boolean isChunked(Variable v);

  long[] computeChunking(Variable v);

  int getDeflateLevel(Variable v);

  boolean isShuffle(Variable v);

}
