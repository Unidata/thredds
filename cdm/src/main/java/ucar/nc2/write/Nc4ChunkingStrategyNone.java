/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import ucar.nc2.Variable;

/**
 * Describe
 *
 * @author caron
 * @since 6/10/14
 */
public class Nc4ChunkingStrategyNone extends Nc4ChunkingDefault {
  @Override
  public boolean isChunked(Variable v) {
    return v.isUnlimited();  // must chunk
  }

  @Override
  public int getDeflateLevel(Variable v) {
    return 0;
  }

  @Override
  public boolean isShuffle(Variable v) {
    return false;
  }
}
