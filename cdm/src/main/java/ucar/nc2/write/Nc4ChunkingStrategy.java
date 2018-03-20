/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

import javax.annotation.concurrent.Immutable;

/**
 * Abstract superclass for netcdf-4 chunking strategy.

 * @author caron
 * @since 11/14/12
 */
@Immutable
public abstract class Nc4ChunkingStrategy implements Nc4Chunking {

  /**
   * @param type         Strategy type
   * @param deflateLevel 0 corresponds to no compression and 9 to maximum compression,
   * @param shuffle      true to turn shuffling on which may improve compression. This option is ignored unless a non-zero deflation level is specified.
   * @return Nc4Chunking implementation
   */
  public static Nc4Chunking factory(Strategy type, int deflateLevel, boolean shuffle) {
    switch (type) {
      case standard: return new Nc4ChunkingDefault(deflateLevel, shuffle);
      case grib: return new Nc4ChunkingStrategyGrib(deflateLevel, shuffle);
      case none: return new Nc4ChunkingStrategyNone();
    }
    throw new IllegalArgumentException("Illegal Nc4Chunking.Standard " + type);
  }

  ////////////////////////////////////////////////////

  private final int deflateLevel;
  private final boolean shuffle;

  ////////////////////////

  protected Nc4ChunkingStrategy(int deflateLevel, boolean shuffle) {
    this.deflateLevel = deflateLevel;
    this.shuffle = shuffle;
  }

  @Override
  public int getDeflateLevel(Variable v) {
    return deflateLevel;
  }

  @Override
  public boolean isShuffle(Variable v) {
    return shuffle;
  }

  protected Attribute getChunkAttribute(Variable v) {
    Attribute att = v.findAttribute(CDM.CHUNK_SIZES);
    if (att != null && att.getDataType().isIntegral() && att.getLength() == v.getRank())
      return att;
    return null;
  }

  protected int[] computeChunkingFromAttribute(Variable v) {
    Attribute att = getChunkAttribute(v); // use CHUNK_SIZES attribute if it exists
    if (att != null) {
      int[] result = new int[v.getRank()];
      for (int i = 0; i < v.getRank(); i++)
        result[i] = att.getNumericValue(i).intValue();
      return result;
    }

    return null;
  }
}
