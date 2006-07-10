// $Id: DatatypeIterator.java,v 1.4 2006/06/06 16:07:13 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
import java.util.Iterator;

/**
 * An abstract implementation for iterating over datatypes, such as PointObsDatatype, etc.
 *
 * @author caron
 * @version $Revision: 1.18 $ $Date: 2006/05/24 00:12:56 $
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

/* Change History:
   $Log: DatatypeIterator.java,v $
   Revision 1.4  2006/06/06 16:07:13  caron
   *** empty log message ***

   Revision 1.3  2006/05/08 02:47:33  caron
   cleanup code for 1.5 compile
   modest performance improvements
   dapper reading, deal with coordinate axes as structure members
   improve DL writing
   TDS unit testing

   Revision 1.2  2006/02/13 19:51:30  caron
   javadoc

   Revision 1.1  2005/05/15 23:06:52  caron
   add fast iterator over Structures, Datatypes
   add convertScalarDouble to StructureData

*/