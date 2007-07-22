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

/**
 * A Tiling divides a multidimensional index into tiles. each tile has the same size.
 * Abstraction of HDF5 chunking.
 * <p>
 * Index are points in the original multidimensional index.
 * Tiles are points in the tiled space.
 *
 * @author caron
 * @since Jul 20, 2007
 */
public class Tiling {

  private int rank;
  private int[] shape, tileSize, tile, stride;

  /**
   * Create a Tiling
   * @param shape overall shape of the index space
   * @param tileSize tile size
   */
  public Tiling(int[] shape, int[] tileSize) {
    assert shape.length <= tileSize.length; // convenient to allow tileSize to have (an) extra dimension at the end
                                            // to accomodate hdf5 storage, which has the element size

    this.rank = shape.length;
    this.shape = shape;
    this.tileSize = tileSize;
    this.tile = tile( shape); // dont really need this, but udeful for debugging

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
   * @return order(p1) - order(p2)
   */
  public int compare(int[] p1, int[] p2) {
    return order(p1) - order(p2);
  }
}
