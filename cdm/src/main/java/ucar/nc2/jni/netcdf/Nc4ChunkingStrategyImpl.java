package ucar.nc2.jni.netcdf;

import net.jcip.annotations.Immutable;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

import java.util.List;

/**
 * Default strategy for netcdf-4 chunking

 * @author caron
 * @since 11/14/12
 */
@Immutable
public class Nc4ChunkingStrategyImpl implements Nc4Chunking {
  private static final boolean debug = true;

  public static Nc4Chunking factory(Strategy type, int deflateLevel, boolean shuffle) {
    switch (type) {
      case standard: return new Nc4ChunkingStrategyImpl(deflateLevel, shuffle);
      case grib: return new Nc4ChunkingStrategyGrib(deflateLevel, shuffle);
      case fromAttribute: return new Nc4ChunkingStrategyFromAttribute(deflateLevel, shuffle);
    }
    throw new IllegalArgumentException("Illegal Nc4Chunking.Standard " + type);
  }

  private static final int DEFAULT_CHUNKSIZE = (int) Math.pow(2,22); // 4M
  //private static final int SMALL_VARIABLE = (int) Math.pow(2,16); // 65K

  private final int deflateLevel;
  private final boolean shuffle;

  public Nc4ChunkingStrategyImpl() {
    this.deflateLevel = 5;
    this.shuffle = true;
  }

  public Nc4ChunkingStrategyImpl(int deflateLevel, boolean shuffle) {
    this.deflateLevel = deflateLevel;
    this.shuffle = shuffle;
  }

  @Override
  public boolean isChunked(Variable v) {
    if (v.isUnlimited()) return true;
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
    // use entire if small enough
    long size = v.getSize() * v.getElementSize();
    if (size < DEFAULT_CHUNKSIZE) return convert(v.getShape()); // all of it
    if (v.isUnlimited()) return _computeChunkingUnlimited(v);
    return _computeChunking(v);
  }

  private long[] _computeChunkingUnlimited(Variable v) {
    List<Dimension> dims = v.getDimensions();
    long[] result = new long[dims.size()];
    int count = 0;
    for (Dimension d : dims) {
      if (d.isUnlimited()) result[count++] = 1;
      else result[count++] = d.getLength();
    }
    return result;
  }

  private long[] _computeChunking(Variable v) {
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

  protected Attribute getChunkAttribute(Variable v) {
    return v.findAttribute(CDM.CHUNK_SIZE);
  }


}
