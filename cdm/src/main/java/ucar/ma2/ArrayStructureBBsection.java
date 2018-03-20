/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Describe
 *
 * @author caron
 * @since 5/31/2015
 */
public class ArrayStructureBBsection extends ArrayStructureBB {
  protected int[] orgRecnum;

  /**
   * Make a section of an ArrayStructureBB
   *
   * @param org the original
   * @param section of the whole
   * @return original, if section is empty or the same saze as the original, else a section of the original
   */
  static public ArrayStructureBB factory(ArrayStructureBB org, Section section) {
    if (section == null || section.computeSize() == org.getSize())
      return org;
    return new ArrayStructureBBsection(org.getStructureMembers(), org.getShape(), org.getByteBuffer(), section);
  }

  private ArrayStructureBBsection(StructureMembers members, int[] shape, ByteBuffer bbuffer, Section section) {
    super(members, shape, bbuffer, 0);
    int n = (int) section.computeSize();
    Section.Iterator iter = section.getIterator(shape);
    orgRecnum = new int[n];
    int count = 0;
    while (iter.hasNext()) {
      orgRecnum[count++] = iter.next(null);
    }
  }


  @Override
  protected int calcOffsetSetOrder(int recnum, StructureMembers.Member m) {
    if (null != m.getDataObject())
      bbuffer.order( (ByteOrder) m.getDataObject());
    return bb_offset + orgRecnum[recnum] * getStructureSize() + m.getDataParam();
  }
}
