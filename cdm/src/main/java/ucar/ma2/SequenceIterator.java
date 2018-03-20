/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ma2;

import java.io.IOException;

/**
 * Creates a StructureDataIterator by wrapping a section of a ArrayStructure.
 *
 * @author caron
 * @since Nov 16, 2009
 */


public class SequenceIterator implements StructureDataIterator {
  private int start, size, count;
  private ArrayStructure abb;

  public SequenceIterator(int start, int size, ArrayStructure abb) {
    this.start = start;
    this.size = size;
    this.abb = abb;
    this.count = 0;
  }

  @Override
  public boolean hasNext() throws IOException {
    return (count < size);
  }

  @Override
  public StructureData next() throws IOException {
    StructureData result = abb.getStructureData(start + count);
    count++;
    return result;
  }

  @Override
  public StructureDataIterator reset() {
    count = 0;
    return this;
  }

  @Override
  public int getCurrentRecno() {
    return count;
  }

}
