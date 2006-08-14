// $Id: $
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

package ucar.nc2.dataset;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.util.CancelTask;
import ucar.nc2.Variable;

import java.util.List;
import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 */
public interface ProxyReader {
  public Array read(Variable mainv, CancelTask cancelTask) throws IOException;
  public Array read(Variable mainv, CancelTask cancelTask, List section) throws IOException, InvalidRangeException;
}
