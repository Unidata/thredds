/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.FileWriter2;
import ucar.nc2.Variable;

import java.util.List;

/**
 * Default chunking strategy
 *
 * @author caron
 * @since 5/10/14
 */
public class Nc4ChunkingDefault extends Nc4ChunkingStrategy {

  private static final int DEFAULT_CHUNKSIZE_BYTES = (int) Math.pow(2,18); // 256K
  private static final int MIN_VARIABLE_BYTES = (int) Math.pow(2,16); // 65K
  private static final int MIN_CHUNKSIZE_BYTES = (int) Math.pow(2,13); // 8K

  ////////////////////////////////////////////////////

  private int defaultChunkSize = DEFAULT_CHUNKSIZE_BYTES;
  private int minVariableSize = MIN_VARIABLE_BYTES;
  private int minChunksize = MIN_CHUNKSIZE_BYTES;

  public int getDefaultChunkSize() {
    return defaultChunkSize;
  }

  public void setDefaultChunkSize(int defaultChunkSize) {
    this.defaultChunkSize = defaultChunkSize;
  }

  public int getMinVariableSize() {
    return minVariableSize;
  }

  public void setMinVariableSize(int minVariableSize) {
    this.minVariableSize = minVariableSize;
  }

  public int getMinChunksize() {
    return minChunksize;
  }

  public void setMinChunksize(int minChunksize) {
    this.minChunksize = minChunksize;
  }

  public Nc4ChunkingDefault() {
    super(5, true);
  }

  public Nc4ChunkingDefault(int deflateLevel, boolean shuffle) {
    super(deflateLevel, shuffle);
  }

  @Override
  public boolean isChunked(Variable v) {
    if (v.isUnlimited()) return true;
    // if (getChunkAttribute(v) != null) return true;

    long size = v.getSize() * v.getElementSize();
    return (size > minVariableSize);
  }

  @Override
  public long[] computeChunking(Variable v) {
    /* check attribute
    int[] resultFromAtt = computeChunkingFromAttribute(v);
    if (resultFromAtt != null)
      return convertToLong(resultFromAtt); */

    int maxElements = defaultChunkSize / v.getElementSize();

    // no unlimited dimensions
    if (!v.isUnlimited()) {
      int[] result = fillRightmost(v.getShape(), maxElements);
      return convertToLong(result);
    }

    // unlimited case
    int[] result = computeUnlimitedChunking(v.getDimensions(), v.getElementSize());
    return convertToLong(result);
  }

  private int[] fillRightmost(int shape[], int maxElements) {
    // fill up rightmost dimensions first, until maxElements is reached
    FileWriter2.ChunkingIndex index = new FileWriter2.ChunkingIndex(shape);
    return index.computeChunkShape(maxElements);
  }

  // make it easy to test by using dimension list
  public int[] computeUnlimitedChunking(List<Dimension> dims, int elemSize) {
    int maxElements = defaultChunkSize / elemSize;
    int[] result = fillRightmost(convertUnlimitedShape(dims), maxElements);
    long resultSize = new Section(result).computeSize();
    if (resultSize < minChunksize) {
      maxElements = minChunksize / elemSize;
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
    if ( countUnlimitedDims <= 1) need = needFactor;
    else if ( countUnlimitedDims == 2) need = (int) Math.sqrt(needFactor);
    else if ( countUnlimitedDims == 3) need = (int) Math.cbrt(needFactor);
    else {
      // nth root?? hmm roundoff !!
      need = (int)  Math.pow(needFactor, 1.0 / countUnlimitedDims);
    }

    int[] result = new int[shape.length];
      int count = 0;
      for (Dimension d : dims) {
        result[count] = (d.isUnlimited()) ? need : shape[count];
        count++;
      }
    return result;
  }

  protected int[] convertUnlimitedShape(List<Dimension> dims) {
    int[] result = new int[dims.size()];
    int count = 0;
    for (Dimension d : dims) {
      result[count++] = (d.isUnlimited()) ? 1 : d.getLength();
    }
    return result;
  }

  protected long[] convertToLong(int[] shape) {
    if (shape.length == 0) shape = new int[1];
    long[] result = new long[shape.length];
    for (int i=0; i<shape.length; i++)
      result[i] = shape[i] > 0 ? shape[i] : 1; // unlimited dim has 0
    return result;
  }

}
