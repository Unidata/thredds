/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Variation of ArrayStructureBB, where the offsets of the records into the ByteBuffer are uneven and must be
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
   * @param positions offset from the start of the ByteBufffer to each record. must have length = with shape.getSize()
   */
  public ArrayStructureBBpos(StructureMembers members, int[] shape, ByteBuffer bbuffer, int[] positions) {
    super(members, shape, bbuffer, 0);
    this.positions = positions;
  }


  @Override
  protected int calcOffsetSetOrder(int recnum, StructureMembers.Member m) {
    if (null != m.getDataObject())
      bbuffer.order( (ByteOrder) m.getDataObject());
    return positions[recnum] + m.getDataParam();
  }
}