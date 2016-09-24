/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
