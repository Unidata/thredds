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
 * @since 7/9/2014
 */
public class StructureDataIteratorMediated implements StructureDataIterator {

  private StructureDataIterator org;
  private StructureDataMediator mod;

  public StructureDataIteratorMediated(StructureDataIterator org, StructureDataMediator mod) throws IOException {
    this.org = org;
    this.mod = mod;
  }

  @Override
  public StructureData next() throws IOException {
    return mod.modify(org.next());
  }

  @Override
  public boolean hasNext() throws IOException {
    return org.hasNext();
  }

  @Override
  public StructureDataIterator reset() {
    org.reset();
    return this;
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

