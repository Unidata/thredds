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
package ucar.nc2.iosp.hdf4;

import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;

/**
 * @author caron
 * @since Jan 9, 2008
 */
public class H4chunkLayout extends ucar.nc2.iosp.LayoutTiled {

  public H4chunkLayout(DataChunkIterator chunkIterator, int[] chunkSize, int elemSize, int[] srcShape, Section wantSection) throws InvalidRangeException, IOException {
    super(chunkIterator, chunkSize, elemSize, srcShape, wantSection);
  }
}
