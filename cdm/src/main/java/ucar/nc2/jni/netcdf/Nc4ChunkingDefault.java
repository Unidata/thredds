package ucar.nc2.jni.netcdf;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

import java.util.List;

/**
 * Default strategy for netcdf-4 chunking
 * <pre>
 * 1) if attribute _ChunkSize exists, use that
 *      :_ChunkSize = 1, 35, 56, 75; // int
 *    FileWriter2 sets these
 * </pre>
 * @author caron
 * @since 11/14/12
 */
public class Nc4ChunkingDefault implements Nc4Chunking {
  private static final int DEFAULT_CHUNKSIZE = (int) Math.pow(2,22); // 4M

  @Override
  public long[] computeChunking(Variable v) {
    // use CHUNK_SIZE attribute if it exists
    Attribute att = v.findAttribute(CDM.CHUNK_SIZE);
    if (att != null && att.getDataType().isIntegral() && att.getLength() == v.getRank()) {
      long[] result = new long[v.getRank()];
      for (int i=0; i<v.getRank(); i++)
        result[i] = att.getNumericValue(i).longValue();
      return result;
    }

    // use entire
    long size = v.getSize();
    if (size < DEFAULT_CHUNKSIZE) return convert(v.getShape()); // all of it

    List<Dimension> dims = v.getDimensions();
    long[] result = new long[dims.size()];
    int count = 0;
    for (Dimension d : dims) {
      if (d.isUnlimited()) result[count++] = 1;
      else result[count++] = d.getLength();
    }
    return result;
  }

  private long[] convert(int[] shape) {
    if (shape.length == 0) shape = new int[1];
    long[] result = new long[shape.length];
    for (int i=0; i<shape.length; i++)
      result[i] = shape[i] > 0 ? shape[i] : 1; // unlimited dim has 0
    return result;
  }

}
