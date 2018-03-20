/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt;

import ucar.ma2.*;
import ucar.nc2.Structure;

import java.io.IOException;

/**
 * An abstract implementation for iterating over datatypes, such as PointObsDatatype, etc.
 *
 * @deprecated use ucar.nc2.ft.*
 * @author caron
 */
public abstract class DatatypeIterator implements DataIterator {

  protected abstract Object makeDatatypeWithData( int recnum, StructureData sdata) throws IOException;

  private StructureDataIterator structIter;
  private int recnum = 0;

  protected DatatypeIterator(Structure struct, int bufferSize) {
    try {
      this.structIter = struct.getStructureIterator(bufferSize);
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  public boolean hasNext() {
    try {
      return structIter.hasNext();
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  public Object nextData() throws IOException {
    StructureData sdata =  structIter.next();
    return makeDatatypeWithData( recnum++, sdata);
  }

  public Object next() { // LOOK needs IOException
    StructureData sdata;
    try {
      sdata = structIter.next();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      return makeDatatypeWithData( recnum++, sdata);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

}