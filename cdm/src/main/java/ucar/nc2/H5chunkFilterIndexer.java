// $Id: H5chunkFilterIndexer.java,v 1.3 2004/08/16 20:53:45 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

import ucar.ma2.InvalidRangeException;

/**
 * This is for transferring decompressed bytes into the result byte array.
 * Everything is done in bytes.
 *
 * Typical use:
 * <pre>
    Indexer index = new Indexer( v2, origin, shape, v2.isUnlimited() ? recsize : -1);
    int chunk = index.getChunkSize();
    int size = index.getTotalSize();
    if (dataType == DataType.SHORT) {
      short[] pa = new short[size];
      while (index.hasNext()) {
        myRaf.seek ((long) beginOffset + index.next());
        myRaf.readShort( pa, offset, chunk); // copy into primitive array
        offset += chunk;
      }
      return pa;
    }
 * </pre>
 *
 * @author caron
 * @version $Revision: 1.3 $ $Date: 2004/08/16 20:53:45 $
 */
class H5chunkFilterIndexer extends Indexer {

  private int[] storageSize;
  private MyIndex resultIndex; // current index in result array, for iterating
  private int elemSize; // last dimension of the StorageLayout message
  private int chunkNumElemsTotal, chunkNumElemsDone, contNumElems;
  private Chunk chunk; // pass results back
  private boolean first = false;

  //private boolean debug = false, debugNext = false;

  /**
   * Constructor.
   * This is for HDF5 chunked data storage. The data is read by chunk, for efficency.
   * @param v Variable to index over
   * @param storageSize storage sizes from HDF5 header
   * @throws InvalidRangeException
   */
  H5chunkFilterIndexer( Variable v, int[] storageSize) throws InvalidRangeException {
    this.storageSize = storageSize;
    resultIndex = new MyIndex( v.getShape());

    chunkNumElemsTotal = 1;
    for (int j=0; j<storageSize.length; j++)
      chunkNumElemsTotal *= storageSize[j];

    int rank = storageSize.length;
    // how big a chunk can we do?
    elemSize = storageSize[rank-1];
    contNumElems = storageSize[rank-2] * elemSize;

    this.chunk = new Chunk(0L, contNumElems, 0);
  }

  void setChunkOffset( long[] offset) {
    //if (debugNext) H5header.debugOut.println(" setChunkOffset = "+);
    resultIndex.setOffset( offset);
    chunkNumElemsDone = 0;
    chunk.indexPos = 0;
    first = true;
  }

  public int getTotalNelems() { return chunkNumElemsTotal; }
  public int getElemSize() { return 1; }
  public boolean hasNext() { return chunkNumElemsDone < chunkNumElemsTotal; }
  public Chunk next() {

    if (!first) {
      resultIndex.incrRow();
      chunk.indexPos += contNumElems;
    }
    first = false;

    // heres the position within the result array
    chunk.filePos = resultIndex.currentElement() * elemSize;

    if (H5iosp.debugFilterIndexer) H5header.debugOut.println(" next hchunk = "+chunk+" totalNelemsDone="+chunkNumElemsDone);
    chunkNumElemsDone += contNumElems;

    return chunk;
  }


  private class MyIndex extends ucar.ma2.Index {
    protected MyIndex( int[] shape) { super( shape); }
    private void incrRow() {
      if (rank < 2) return;
      current[rank-2]++;
    }
    private void setOffset(long[] offset) {
      for (int i=0; i<rank; i++) current[i] = (int) offset[i];
    }
  }

}