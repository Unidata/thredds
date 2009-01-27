/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.iosp.hdf5;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.ma2.Section;
import ucar.nc2.iosp.Indexer;
import ucar.nc2.iosp.RegularSectionLayout;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * Iterator to read/write subsets of an array.
 * This calculates byte offsets for HD5 chunked datasets.
 * Assumes that the data is stored in chunks, indexed by a Btree.
 * for unfiltered data only
 *
 * @author caron
 * @deprecated use H5tiledLayout
 */
class H5chunkLayout extends Indexer {
  private Section want;
  private int[] chunkSize; // from the StorageLayout message (exclude the elemSize)
  private int elemSize; // last dimension of the StorageLayout message

  // track the overall iteration
  private long totalNelems, totalNelemsDone; // total number of elemens
  private boolean done = false;

  private boolean debug = false;

  /**
   * Constructor.
   * This is for HDF5 chunked data storage. The data is read by chunk, for efficency.
   *
   * @param v2          Variable to index over; assumes that vinfo is the data object
   * @param dtype       type of data. may be different from v2.
   * @param wantSection the wanted section of data, contains a List of Range objects.
   * @throws InvalidRangeException if section invalid for this variable
   * @throws java.io.IOException on io error
   */
  H5chunkLayout(Variable v2, DataType dtype, Section wantSection) throws InvalidRangeException, IOException {
    //debug = H5iosp.debugChunkIndexer;

    wantSection = Section.fill(wantSection, v2.getShape());
    this.totalNelems = wantSection.computeSize();

    H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
    assert vinfo.isChunked;
    assert vinfo.btree != null;

    // we have to translate the want section into the same rank as the storageSize, in order to be able to call
    // Section.intersect(). It appears that storageSize (actually msl.chunkSize) may have an extra dimension, reletive
    // to the Variable.
    if ((dtype == DataType.CHAR) && (wantSection.getRank() < vinfo.storageSize.length))
      this.want = new Section(wantSection).appendRange(1);
    else
      this.want = wantSection;

    // heres the chunking info
    // one less chunk dimension, except in the case of char
    nChunkDims = (dtype == DataType.CHAR) ? vinfo.storageSize.length : vinfo.storageSize.length - 1;
    this.chunkSize = new int[nChunkDims];
    System.arraycopy(vinfo.storageSize, 0, chunkSize, 0, nChunkDims);
    this.elemSize = vinfo.storageSize[vinfo.storageSize.length - 1]; // last one is always the elements size
    if (debug) System.out.println(" H5chunkIndexer: " + this);

    // create the data chunk iterator
    chunkIterator = vinfo.btree.getDataChunkIterator(this.want);
  }

  public long getTotalNelems() {
    return totalNelems;
  }

  public int getElemSize() {
    return elemSize;
  }

  public boolean hasNext() {
    return !done && (totalNelemsDone < totalNelems);
  }

  private int nChunkDims;
  private H5header.DataBTree.DataChunkIterator chunkIterator;  // iterate over the btree DataChunks
  private Indexer index = null; // iterate within a chunk

  public Chunk next() throws IOException {

    if ((index == null) || !index.hasNext()) { // get new data node

      try {
        Section dataSection;
        H5header.DataBTree.DataChunk dataChunk;

        while (true) { // look for intersecting sections
          if (chunkIterator.hasNext())
            dataChunk = chunkIterator.next();
          else {
            done = true;
            return null;
          }

          // make the dataSection for this chunk
          int[] sectionOrigin = new int[nChunkDims];
          System.arraycopy(dataChunk.offset, 0, sectionOrigin, 0, nChunkDims);
          dataSection = new Section(sectionOrigin, chunkSize);

          // does it intersect ?
          if (dataSection.intersects(want))
            break;
        }

        if (debug) System.out.println(" found intersecting section: " + dataSection+" for filePos "+ dataChunk.filePos);
        index = indexFactory(dataChunk, dataChunk.filePos, elemSize, dataSection, want);

      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e);
      }
    }

    Chunk chunk = index.next();
    if (chunk != null)
      totalNelemsDone += chunk.getNelems();
    return chunk;
  }

  // allow subclasses to filter the data
  protected Indexer indexFactory(H5header.DataBTree.DataChunk dataChunk, long filePos, int elemSize, Section dataSection, Section want) throws IOException, InvalidRangeException {
    return RegularSectionLayout.factory(filePos, elemSize, dataSection, want);
  }

  public String toString() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("want=").append(want).append("; ");
    sbuff.append("chunkSize=[");
    for (int i = 0; i < chunkSize.length; i++) {
      if (i > 0) sbuff.append(",");
      sbuff.append(chunkSize[i]);
    }
    sbuff.append("] totalNelems=").append(totalNelems);
    sbuff.append(" elemSize=").append(elemSize);
    return sbuff.toString();
  }


}