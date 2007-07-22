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
package ucar.nc2.iosp;

/**
 * An Indexer that has exactly one chunk.
 * @author caron
 * @since Jul 13, 2007
 */
public class SingleChunkIndexer extends Indexer {
  private Chunk chunk;
  private boolean done = false;
  private int elemSize;
  private int totalElems;

  public SingleChunkIndexer(long filePos, int totalElems, int elemSize) {
    this.chunk = new Chunk(filePos, totalElems, 0);
    this.totalElems = totalElems;
    this.elemSize = elemSize;
  }

  public long getTotalNelems() {
    return totalElems;
  }

  public int getElemSize() {
    return elemSize;
  }

  public boolean hasNext() {
    return !done;
  }

  public Chunk next() {
    done = true;
    return chunk;
  }
}
