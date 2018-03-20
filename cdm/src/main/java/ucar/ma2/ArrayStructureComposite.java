/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ma2;

import java.util.List;
import java.util.ArrayList;

/**
 * An ArrayStructure compose of other ArrayStructures.
 * Doesnt work because of read(StructureMembers.Member). this need to be withdrawn.
 *
 *    int total = 0;
    List<ArrayStructure> list = new ArrayList<ArrayStructure> (msgs.size());
    for (Message m : msgs) {
     ArrayStructure oneMess;
     if (!m.dds.isCompressed()) {
       MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
       oneMess = reader.readEntireMessage(s, protoMessage, m, raf, null);
     } else {
       MessageCompressedDataReader reader = new MessageCompressedDataReader();
       oneMess = reader.readEntireMessage(s, protoMessage, m, raf, null);
     }
      list.add(oneMess);
      total += (int) oneMess.getSize();
    }

    return (list.size() == 1) ? list.get(0) : new ArrayStructureComposite(sm, list, total);
         
 *
 * @author caron
 * @since Nov 19, 2009
 */
public class ArrayStructureComposite extends ArrayStructure {
  private List<ArrayStructure> compose = new ArrayList<>();
  private int[] start;

  public ArrayStructureComposite(StructureMembers members, List<ArrayStructure> c, int total) {
    super(members, new int[total]);
    this.compose = c;

    start = new int[total];
    int count = 0;
    int i = 0;
    for (ArrayStructure as : compose) {
      start[i++] = count;
      count += (int) as.getSize();
    }
  }


  @Override
  protected StructureData makeStructureData(ArrayStructure me, int recno) {
    for (int i=0; i< start.length; i++) {
      if (recno >= start[i]) {
        ArrayStructure as = compose.get(i);
        return as.makeStructureData(as, recno - start[i]);
      }
    }
    throw new IllegalArgumentException();
  }
}
