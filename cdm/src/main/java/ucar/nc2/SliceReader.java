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
package ucar.nc2;

import ucar.ma2.*;
import ucar.nc2.util.CancelTask;

import java.io.IOException;

/**
 * A ProxyReader for slices.
 *
 * @author caron
 * @see Variable#slice(int, int)
 */

class SliceReader implements ProxyReader {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SliceReader.class);

  private Variable orgVar;
  private int sliceDim;    // dimension index into original
  private Section slice;   // section of the original

  SliceReader(Variable orgVar, int dim, Section slice) throws InvalidRangeException {
    this.orgVar = orgVar;
    this.sliceDim = dim;
    this.slice = slice;
  }

  public Array read(Variable mainv, CancelTask cancelTask) throws IOException {
    Array data;
    try {
      data = orgVar._read( slice);
    } catch (InvalidRangeException e) {
      log.error("InvalidRangeException in slice, var="+orgVar.getName());
      throw new IllegalStateException(e.getMessage());
    }
    data = data.reduce( sliceDim);
    return data;
  }

  public Array read(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    Section orgSection = new Section(section.getRanges());
    orgSection.insertRange(sliceDim, slice.getRange(sliceDim));
    Array data = orgVar._read( orgSection);
    data.reduce( sliceDim);
    return data;
  }


}
