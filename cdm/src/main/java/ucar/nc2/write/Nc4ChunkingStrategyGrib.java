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
