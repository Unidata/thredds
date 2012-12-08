package ucar.nc2.jni.netcdf;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

/**
 * use _ChunkSize attribute if it exists
 * otherwise use default strategy
 * <pre>
 *      :_ChunkSize = 1, 35, 56, 75; // int
 * </pre>
 *
 * @author caron
 * @since 12/7/12
 */
public class Nc4ChunkingStrategyFromAttribute extends Nc4ChunkingStrategyImpl {

  public Nc4ChunkingStrategyFromAttribute(int deflateLevel, boolean shuffle) {
    super(deflateLevel, shuffle);
  }

  @Override
  public boolean isChunked(Variable v) {
    if (getChunkAttribute(v) != null) return true;
    return super.isChunked(v);
  }

  @Override
  public long[] computeChunking(Variable v) {
    // use CHUNK_SIZE attribute if it exists
    Attribute att = v.findAttribute(CDM.CHUNK_SIZE);
    if (att != null && att.getDataType().isIntegral() && att.getLength() == v.getRank()) {
      long[] result = new long[v.getRank()];
      for (int i = 0; i < v.getRank(); i++)
        result[i] = att.getNumericValue(i).longValue();
      return result;
    }

    return super.computeChunking(v);
  }
}

