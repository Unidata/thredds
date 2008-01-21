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
package ucar.ma2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Variation of ArrayStructureBB, where the offsets into the ByteBuffer are uneven and must be
 * passed in by the user.
 *
 * @author caron
 */
public class ArrayStructureBBpos extends ArrayStructureBB {
  protected int[] positions;

  /**
   * Construct an ArrayStructureBB with the given ByteBuffer.
   *
   * @param members the list of structure members.
   * @param shape the shape of the structure array
   * @param bbuffer the data is stored in this ByteBuffer. bbuffer.order must already be set.
   * @param positions offset from the start of the ByteBufffer to each record. must have length consistent with shape.
   */
  public ArrayStructureBBpos(StructureMembers members, int[] shape, ByteBuffer bbuffer, int[] positions) {
    super(members, shape, bbuffer, 0);
    this.positions = positions;
  }

  /*
   *    * LOOK doesnt work, because of the methods using recnum, not Index (!)
   * create new Array with given indexImpl and the same backing store
   *
  public Array createView(Index index) {
    return new ArrayStructureBBpos(members, index, nelems, sdata, bbuffer, positions);
  }

  /*
   * Create a new Array using the given IndexArray and backing store.
   * used for sections, and factory.
   *
   * @param members     a description of the structure members
   * @param ima         use this IndexArray as the index
   * @param nelems      the total number of StructureData elements in the backing array
   * @param sdata       the backing StructureData array; may be null.
   * @param bbuffer     use this for the ByteBuffer storage.
   *
  public ArrayStructureBBpos(StructureMembers members, Index ima, int nelems, StructureData[] sdata, ByteBuffer bbuffer, int[] positions) {
    super(members, ima, nelems, sdata, bbuffer);
    this.positions = positions;
  } */


  @Override
  protected int calcOffsetSetOrder(int recnum, StructureMembers.Member m) {
    if (null != m.getDataObject())
      bbuffer.order( (ByteOrder) m.getDataObject());
    return positions[recnum] + m.getDataParam();
  }
}