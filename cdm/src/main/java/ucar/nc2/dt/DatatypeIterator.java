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
package ucar.nc2.dt;

import ucar.ma2.*;
import ucar.nc2.Structure;

import java.io.IOException;

/**
 * An abstract implementation for iterating over datatypes, such as PointObsDatatype, etc.
 *
 * @author caron
 */
public abstract class DatatypeIterator implements DataIterator {

  protected abstract Object makeDatatypeWithData( int recnum, StructureData sdata);

  private Structure.Iterator structIter;
  private int recnum = 0;

  protected DatatypeIterator(Structure struct, int bufferSize) {
    this.structIter = struct.getStructureIterator(bufferSize);
  }

  public boolean hasNext() { return structIter.hasNext(); }

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
    return makeDatatypeWithData( recnum++, sdata);
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

}