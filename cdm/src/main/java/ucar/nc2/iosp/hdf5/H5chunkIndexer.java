/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.hdf5;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.nc2.iosp.Indexer;
import ucar.nc2.*;

import java.io.IOException;

/**
 * Iterator to read/write subsets of an array.
 * This calculates byte offsets for HD5 chunked datasets.
 * Assumes that the data is stored in chunks, indexed by a Btree.
 *
 * Typical use:
 * <pre>
    Indexer index = new H5chunkIndexer( v2, origin, shape);
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
 */
class H5chunkIndexer extends Indexer {

  private int[] wantShape, wantOrigin; // what we want
  private int[] varShape; // original variable
  private int varRank; // original variable
  private int[] chunkSize; // from the StorageLayout message (exclude the elemSize)
  private int chunkNelems; // how many elements to read at a time
  private int elemSize; // last dimension of the StorageLayout message

  // iterate over the data chunks
  private H5header.DataBTree.DataChunkIterator chunkListIter;
  private H5header.DataBTree.DataChunk currentDataNode;
  private int currentDataNelems, currentDataNelemsDone; // within the data chunk

  // track the overall iteration
  private int totalNelems, totalNelemsDone; // total number of elemens
  private boolean done = false;
  private MyIndex resultIndex; // current index in result array, for iterating

  private Chunk chunk; // pass results back
  private boolean debug = false;

  /**
   * Constructor.
   * This is for HDF5 chunked data storage. The data is read by chunk, for efficency.
   * @param v2 Variable to index over; assumes that vinfo is the data object
   * @param wantOrigin want subset origin
   * @param wantShape want subset shape
   * @throws InvalidRangeException if origin, shape is invalid
   * @throws java.io.IOException on read error
   */
  H5chunkIndexer( Variable v2, int[] wantOrigin, int[] wantShape) throws InvalidRangeException, IOException {
    debug = H5iosp.debugChunkIndexer;

    this.varShape = v2.getShape();
    varRank = varShape.length;

    // check ranges are valid
    if (wantOrigin.length != varRank)
      throw new InvalidRangeException("Bad origin rank");
    if (wantShape.length != varRank)
      throw new InvalidRangeException("Bad shape rank");
    for (int ii=0; ii<varRank; ii++) {
      // if (isRecord && (ii==0)) continue; // skip for unlimited dimension // why ?
      if ((wantOrigin[ii] < 0) || (wantOrigin[ii] + wantShape[ii] > varShape[ii]))
        throw new InvalidRangeException("Bad range for dimension "+ii);
    }

    // heres the subset that we want
    this.wantOrigin = wantOrigin;
    this.wantShape = wantShape.clone();
    // compute total elements in the subset
    this.totalNelems = 1;
    for (int ii = 0; ii < varRank; ii++)
      totalNelems *= wantShape[ii];

    H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();

    // heres the chunking info
    // one less chunk dimension, except in the case of char
    int nChunkDims = (v2.getDataType() == DataType.CHAR) ? vinfo.storageSize.length : vinfo.storageSize.length - 1;
    this.chunkSize = new int[nChunkDims];
    for (int i=0; i<nChunkDims; i++)
      chunkSize[i] = vinfo.storageSize[i];
    this.elemSize = vinfo.storageSize[vinfo.storageSize.length-1]; // last one is always the elements size

    // generally we can only read this many elements at once
    this.chunkNelems = chunkSize[nChunkDims-1];
    //this.canDoAll = (chunkNelems == varShape[ varRank-1]); // optimization
    //if (canDoAll)
    //  chunkNelems =
    if (debug) H5header.debugOut.println(" H5chunkIndexer: totalNelems  = "+totalNelems+" elemSize= "+getElemSize()+
                                  " chunkNelems ="+chunkNelems); // + " canDoAll ="+canDoAll);

    // the index within the result array
    this.resultIndex = new MyIndex( wantShape);

    // load in the first data node
    H5header.DataBTree btree = vinfo.btree;
    chunkListIter = btree.getDataChunkIterator(null);

    // holds the chunk info as we iterate
    this.chunk = new Chunk(0L, 0, 0);
  }


  public long getTotalNelems() { return totalNelems; }
  public int getElemSize() { return elemSize; }
  public boolean hasNext() { return !done && (totalNelemsDone < totalNelems); }
  public Chunk next() throws IOException {

    if (currentDataNelemsDone == currentDataNelems) { // get new data node
      if (chunkListIter.hasNext())
        currentDataNode = chunkListIter.next();
      else
        done = true;

      currentDataNelems = currentDataNode.size / elemSize;
      currentDataNelemsDone = 0;
      chunk.setFilePos(currentDataNode.address);
      // set origin, figure out how many elements are left in the row
      resultIndex.setOffset( currentDataNode.offset);
      this.chunkNelems = Math.min( chunkSize[varRank-1], resultIndex.getRemainingInRow());

      if (debug) H5header.debugOut.println(" new dataNode = "+currentDataNode);

    } else { // incr within existing data node
      chunk.incrFilePos(chunk.getNelems() * getElemSize()); // how many done last time
      resultIndex.incrRow();
    }

    // how big a chunk can we do?
    chunk.setNelems(chunkNelems); // LOOK Math.min(chunkNelems, size - count);

    // heres the position within the result array
    chunk.setStartElem( resultIndex.currentElement());

    if (debug) H5header.debugOut.println(" next hchunk = "+chunk+" totalNelemsDone="+totalNelemsDone);
    currentDataNelemsDone += chunk.getNelems();
    totalNelemsDone += chunk.getNelems();

    return chunk;
  }

  private class MyIndex extends ucar.ma2.Index {
    protected MyIndex( int[] shape) { super( shape); }
    private void incrRow() {
      if (rank < 2) return;
      current[rank-2]++;
    }
    private void setOffset(int[] offset) {
      for (int i=0; i<rank; i++) current[i] = offset[i];
    }
    private int getRemainingInRow() { return shape[rank-1] - current[rank-1]; }
  }

}