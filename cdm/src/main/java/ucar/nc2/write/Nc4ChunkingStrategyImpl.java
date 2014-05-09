package ucar.nc2.write;

import net.jcip.annotations.Immutable;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.FileWriter2;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

import java.util.List;

/**
 * Default strategy for netcdf-4 chunking.

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

  private static final int DEFAULT_CHUNKSIZE_BYTES = (int) Math.pow(2,18); // 256K
  private static final int MIN_VARIABLE_BYTES = (int) Math.pow(2,16); // 65K
  private static final int MIN_CHUNKSIZE_BYTES = (int) Math.pow(2,13); // 8K

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

  // LOOK - also consider deflate - needs to be chunked....
  @Override
  public boolean isChunked(Variable v) {
    if (v.isUnlimited()) return true;
    long size = v.getSize() * v.getElementSize();
    return (size > MIN_VARIABLE_BYTES);
  }

  @Override
  public int getDeflateLevel(Variable v) {
    return deflateLevel;
  }

  public boolean isShuffle(Variable v) {
    return shuffle;
  }

  @Override
  public boolean chunkByAttribute() {
    return false;
  }

  @Override
  public long[] computeChunking(Variable v) {
    long maxElements = DEFAULT_CHUNKSIZE_BYTES / v.getElementSize();

    if (!v.isUnlimited()) {
      int[] result = fillRightmost(v.getShape(), maxElements);
      return convertToLong(result);
    }

    // unlimited case
    int[] result =  computeUnlimitedChunking(v.getDimensions(), DEFAULT_CHUNKSIZE_BYTES / v.getElementSize());
    return convertToLong(result);
  }

  private int[] fillRightmost(int shape[], long maxElements) {
    // fill up rightmost dimensions first, until maxElements is reached
    FileWriter2.ChunkingIndex index = new FileWriter2.ChunkingIndex(shape);
    return index.computeChunkShape(maxElements);
  }

  // make it easy to test by using dimension list
  public int[] computeUnlimitedChunking(List<Dimension> dims, int elemSize) {
    long maxElements = DEFAULT_CHUNKSIZE_BYTES / elemSize;
    int[] result = fillRightmost(convertUnlimitedShape(dims), maxElements);
    long resultSize = new Section(result).computeSize();
    if (resultSize < MIN_CHUNKSIZE_BYTES) {
      maxElements = MIN_CHUNKSIZE_BYTES / elemSize;
      result = incrUnlimitedShape(dims, result, maxElements);
    }

    return result;
  }


  private int[] incrUnlimitedShape(List<Dimension> dims, int[] shape, long maxElements) {
    int countUnlimitedDims = 0;
    for (Dimension d : dims) {
      if (d.isUnlimited()) countUnlimitedDims++;
    }
    long shapeSize = new Section(shape).computeSize(); // shape with unlimited dimensions == 1
    int needFactor = (int) (maxElements / shapeSize);

    // distribute needFactor amongst the n unlimited dimensions
    int need;
    if ( countUnlimitedDims == 1) need = needFactor;
    else if ( countUnlimitedDims == 2) need = (int) Math.sqrt(needFactor);
    else if ( countUnlimitedDims == 3) need = (int) Math.cbrt(needFactor);
    else {
      // nth root?? hmm roundoff !!
      need = (int)  Math.pow(needFactor, 1 / countUnlimitedDims);
    }

    int[] result = new int[shape.length];
      int count = 0;
      for (Dimension d : dims) {
        result[count] = (d.isUnlimited()) ? need : shape[count];
        count++;
      }
    return result;
  }

  private int[] convertUnlimitedShape(List<Dimension> dims) {
    int[] result = new int[dims.size()];
    int count = 0;
    for (Dimension d : dims) {
      result[count++] = (d.isUnlimited()) ? 1 : d.getLength();
    }
    return result;
  }

  private long[] convertToLong(int[] shape) {
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
