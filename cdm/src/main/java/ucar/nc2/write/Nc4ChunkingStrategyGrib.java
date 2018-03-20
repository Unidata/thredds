/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import ucar.nc2.Variable;

/**
 * chunk on last 2 dimensions, like GRIB
 *
 * @author caron
 * @since 11/26/12
 */
public class Nc4ChunkingStrategyGrib extends Nc4ChunkingDefault {

  public Nc4ChunkingStrategyGrib(int deflateLevel, boolean shuffle) {
    super(deflateLevel, shuffle);
  }

  @Override
  public boolean isChunked(Variable v) {
    if (v.isUnlimited()) return true;
    // if (getChunkAttribute(v) != null) return true;

    int n = v.getRank();
    return n >= 2 && v.getSize() * v.getElementSize() > getMinVariableSize();
  }

  @Override
   public long[] computeChunking(Variable v) {
     /* check attribute
     int[] resultFromAtt = computeChunkingFromAttribute(v);
     if (resultFromAtt != null)
       return convertToLong(resultFromAtt); */

     // no unlimited dimensions
     if (!v.isUnlimited()) {
       int[] result = computeChunkingGrib(v);
       return convertToLong(result);
     }

     // unlimited case
    if (v.getRank() >= 2) {
      long varSize = v.getSize() * v.getElementSize();
      if (varSize > getMinVariableSize())                // getMinVariableSize or getMinChunksize ??
        return convertToLong(computeChunkingGrib(v));
    }

    // small unlimited variable
    int[] result = computeUnlimitedChunking(v.getDimensions(), v.getElementSize());
    return convertToLong(result);
  }


  private int[] computeChunkingGrib(Variable v) {
    int n = v.getRank();
    int[] result = new int[n];
    if( n < 2 ) {
    	result[0] = 1; // Unlimited variable with rank 1

    } else {
    	for (int i=0; i<n; i++)
    		result[i] = (i<n-2) ? 1 : v.getDimension(i).getLength();
    }	
    return result;
  }
}
