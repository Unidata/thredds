/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf5;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.ma2.Section;
import ucar.nc2.iosp.LayoutTiled;
import ucar.nc2.iosp.Layout;

import java.io.IOException;

/**
 * Iterator to read/write subsets of an array.
 * This calculates byte offsets for HD5 chunked datasets.
 * Assumes that the data is stored in chunks, indexed by a Btree.
 * for unfiltered data only
 *
 * @author caron
 */
class H5tiledLayout implements Layout {
  private LayoutTiled delegate;

  private Section want;
  private int[] chunkSize; // from the StorageLayout message (exclude the elemSize)
  private int elemSize; // last dimension of the StorageLayout message

  private boolean debug = false;

  /**
   * Constructor.
   * This is for HDF5 chunked data storage. The data is read by chunk, for efficency.
   *
   * @param vinfo       the vinfo object for this variable
   * @param dtype       type of data. may be different from v2.
   * @param wantSection the wanted section of data, contains a List of Range objects, must be complete
   * @throws InvalidRangeException if section invalid for this variable
   * @throws java.io.IOException on io error
   */
  H5tiledLayout(H5header.Vinfo vinfo, DataType dtype, Section wantSection) throws InvalidRangeException, IOException {
    assert vinfo.isChunked;
    assert vinfo.btree != null;

    // we have to translate the want section into the same rank as the storageSize, in order to be able to call
    // Section.intersect(). It appears that storageSize (actually msl.chunkSize) may have an extra dimension, reletive
    // to the Variable.
    if ((dtype == DataType.CHAR) && (wantSection.getRank() < vinfo.storageSize.length))
      this.want = new Section(wantSection).appendRange(1);
    else
      this.want = wantSection;

    // one less chunk dimension, except in the case of char
    int nChunkDims = (dtype == DataType.CHAR) ? vinfo.storageSize.length : vinfo.storageSize.length - 1;
    this.chunkSize = new int[nChunkDims];
    System.arraycopy(vinfo.storageSize, 0, chunkSize, 0, nChunkDims);

    /*
    if(vinfo.mdt.isVlen) {
        nChunkDims++;
        int[] newchunks = new int[nChunkDims];
        System.arraycopy(chunkSize,0,newchunks,0,nChunkDims-1);
        chunkSize = newchunks;
    } */

    this.elemSize = vinfo.storageSize[vinfo.storageSize.length - 1]; // last one is always the elements size
    if (debug) System.out.println(" H5tiledLayout: " + this);

    // create the data chunk iterator
    LayoutTiled.DataChunkIterator iter = vinfo.btree.getDataChunkIteratorNoFilter(this.want, nChunkDims);
    delegate = new LayoutTiled(iter, chunkSize, elemSize, this.want);
  }

  public long getTotalNelems() {
    return delegate.getTotalNelems();
  }

  public int getElemSize() {
    return delegate.getElemSize();
  }

  public boolean hasNext() {
    return delegate.hasNext();
  }

  public Chunk next() throws IOException {
    return delegate.next();
  }

  public String toString() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("want=").append(want).append("; ");
    sbuff.append("chunkSize=[");
    for (int i = 0; i < chunkSize.length; i++) {
      if (i > 0) sbuff.append(",");
      sbuff.append(chunkSize[i]);
    }
    sbuff.append("] totalNelems=").append(getTotalNelems());
    sbuff.append(" elemSize=").append(elemSize);
    return sbuff.toString();
  }


}