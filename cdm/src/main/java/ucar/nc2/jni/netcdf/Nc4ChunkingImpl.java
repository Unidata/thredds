package ucar.nc2.jni.netcdf;

import net.jcip.annotations.Immutable;
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
@Immutable
public class Nc4ChunkingImpl implements Nc4Chunking {

  public static Nc4Chunking factory(Nc4Chunking.Standard type, int deflateLevel, boolean shuffle) {
    switch (type) {
      case unlimited: return new Nc4ChunkingImpl(deflateLevel, shuffle);
      case grib: return new Nc4ChunkingGrib(deflateLevel, shuffle);
    }
    throw new IllegalArgumentException("Illegal Nc4Chunking.Standard " + type);
  }

  private static final int DEFAULT_CHUNKSIZE = (int) Math.pow(2,22); // 4M
  //private static final int SMALL_VARIABLE = (int) Math.pow(2,16); // 65K

  private final int deflateLevel;
  private final boolean shuffle;

  public Nc4ChunkingImpl() {
    this.deflateLevel = 5;
    this.shuffle = true;
  }

  public Nc4ChunkingImpl(int deflateLevel, boolean shuffle) {
    this.deflateLevel = deflateLevel;
    this.shuffle = shuffle;
  }

  @Override
  public boolean isChunked(Variable v) {
    if (v.isUnlimited()) return true;
    if (getChunkAttribute(v) != null) return true;
    long size = v.getSize() * v.getElementSize();
    return size >= DEFAULT_CHUNKSIZE;
  }

  @Override
  public int getDeflateLevel(Variable v) {
    return deflateLevel;
  }

  public boolean isShuffle(Variable v) {
    return shuffle;
  }

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

    // use entire if small enough
    long size = v.getSize() * v.getElementSize();
    if (size < DEFAULT_CHUNKSIZE) return convert(v.getShape()); // all of it

    return _computeChunking(v);
  }

  protected long[] _computeChunking(Variable v) {
    List<Dimension> dims = v.getDimensions();
    long[] result = new long[dims.size()];
    int count = 0;
    for (Dimension d : dims) {
      if (d.isUnlimited()) result[count++] = 1;
      else result[count++] = d.getLength();
    }
    return result;
  }

  protected long[] convert(int[] shape) {
    if (shape.length == 0) shape = new int[1];
    long[] result = new long[shape.length];
    for (int i=0; i<shape.length; i++)
      result[i] = shape[i] > 0 ? shape[i] : 1; // unlimited dim has 0
    return result;
  }

  private Attribute getChunkAttribute(Variable v) {
    return v.findAttribute(CDM.CHUNK_SIZE);
  }


}
