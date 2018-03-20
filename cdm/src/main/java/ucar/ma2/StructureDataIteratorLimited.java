/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.io.IOException;

/**
 * Read a maximum number of StructureData objects from a StructureDataIterator.
 *
 * @author caron
 * @since Apr 23, 2008
 */
public class StructureDataIteratorLimited implements StructureDataIterator {

  private StructureDataIterator org;
  private int limit, count;

  public StructureDataIteratorLimited(StructureDataIterator org, int limit) throws IOException {
    this.org = org;
    this.limit = limit;
    this.count = 0;
  }

  @Override
  public StructureData next() throws IOException {
    return org.next();
  }

  @Override
  public boolean hasNext() throws IOException {
    return count < limit && org.hasNext();
  }

  @Override
  public StructureDataIterator reset() {
    this.count = 0;
    org = org.reset();
    return (org == null) ? null : this;
  }

  @Override
  public void setBufferSize(int bytes) {
    org.setBufferSize(bytes);
  }

  @Override
  public int getCurrentRecno() {
    return org.getCurrentRecno();
  }

  @Override
  public void close() {
    org.close();
  }

}
