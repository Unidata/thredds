/*
 * $Id$
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.geoloc.vertical;

import ucar.ma2.Range;
import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A subset of a vertical transform.
 *
 * @author  Unidata Development Team
 * @version $Revision$
 */
public class VerticalTransformSubset extends VerticalTransformImpl {
  private VerticalTransform original;
  private Range t_range;
  private ArrayList subsetList = new ArrayList();

  /**
   * Create a subset of an existing VerticalTransform
   * @param original make a subset of this
   * @param t_range subset the time dimension, or null if you want all of it
   * @param z_range subset the vertical dimension, or null if you want all of it
   * @param y_range subset the y dimension, or null if you want all of it
   * @param x_range subset the x dimension, or null if you want all of it
   */
  public VerticalTransformSubset( VerticalTransform original, Range t_range, Range z_range, Range y_range, Range x_range) {
    super( null); // timeDim not used in this class

    this.original = original;
    this.t_range = t_range;
    subsetList.add( z_range);
    subsetList.add( y_range);
    subsetList.add( x_range);

    units = original.getUnitString();
  }

  public ArrayDouble.D3 getCoordinateArray(int subsetIndex) throws IOException, InvalidRangeException {
    int orgIndex = subsetIndex;   
    if (isTimeDependent() && t_range != null)
      orgIndex = t_range.element( subsetIndex);

    ArrayDouble.D3 data = original.getCoordinateArray( orgIndex);

    return (ArrayDouble.D3) data.sectionNoReduce( subsetList);
  }

  public boolean isTimeDependent() {
    return original.isTimeDependent();
  }
}
