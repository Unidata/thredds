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

/**
 * A Tiling divides a multidimensional index into tiles.
 * Abstraction of HDF5 chunking.
 * <p>
 * Index are points in the original multidimensional index.
 * Tiles are points in the tiled space.
 * <p>
 * Each tile has the same size, given by tileSize.
 *
 * @author caron
 * @since Jul 20, 2007
 */
public class Tiling {

  private int rank;
  private int[] shape, tileSize, tile, stride;

  /**
   * Create a Tiling
   * @param shape overall shape of the dataset's index space
   * @param tileSize tile size. may be larger than the shape.
   */
  public Tiling(int[] shape, int[] tileSize) {
    assert shape.length <= tileSize.length; // convenient to allow tileSize to have (an) extra dimension at the end
                                            // to accomodate hdf5 storage, which has the element size
    this.rank = shape.length;
    this.tileSize = tileSize;
    this.shape = new int[rank];
    for (int i=0; i<rank; i++)
      this.shape[i] = Math.max(shape[i], tileSize[i]);
    this.tile = tile( this.shape); // dont really need this, but useful for debugging

    this.stride = new int[rank];
    int strider = 1;
    for (int k = rank-1; k >= 0; k--) {
      stride[k] = strider;
      strider *= tile[k];
    }
  }

  /**
   * Compute the tile
   * @param pt index point
   * @return corresponding tile
   */
  public int[] tile(int[] pt) {
//    assert pt.length == rank;
    int[] tile = new int[rank];
    for (int i = 0; i < rank; i++) {
      if (shape[i] < pt[i])
        System.out.println("HEY");

      assert shape[i] >= pt[i];
      tile[i] = pt[i] / tileSize[i];
    }
    return tile;
  }

  /**
   * Get order based on which tile the pt belongs to
   * @param pt index point
   * @return order number based on which tile the pt belongs to
   */
  public int order(int[] pt) {
    int[] tile = tile(pt);
    int order = 0;
    for (int i = 0; i < rank; i++)
      order += stride[i] * tile[i];
    return order;
  }

  /**
   * Create an ordering of index points based on which tile the point is in.
   * @param p1 index point 1
   * @param p2 index point 2
   * @return order(p1) - order(p2) : negetive if p1 < p2, positive if p1 > p2 , 0 if equal
   */
  public int compare(int[] p1, int[] p2) {
    return order(p1) - order(p2);
  }
}
