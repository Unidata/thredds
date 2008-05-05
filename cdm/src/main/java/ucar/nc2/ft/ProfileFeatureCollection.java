/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.nc2.ft;

import java.io.IOException;

/**
 * A collection of ProfileFeature.
 *
 * @author caron
 * @since Mar 19, 2008
 */
public interface ProfileFeatureCollection extends NestedPointFeatureCollection {

  /**
   * Use the internal iterator to check if there is another ProfileFeature in the iteration.
   * @return true is there is another ProfileFeature in the iteration.
   * @throws java.io.IOException on read error
   */
  public boolean hasNext() throws java.io.IOException;

  /**
   * Use the internal iterator to get the next ProfileFeature in the iteration.
   * You must call hasNext() before you call this.
   * @return the next ProfileFeature in the iteration
   * @throws java.io.IOException on read error
   */
  public ProfileFeature next() throws java.io.IOException;

  /**
   * Reset the internal iterator for another iteration over the ProfileFeatures in this Collection.
   * @throws java.io.IOException on read error
   */
  public void resetIteration() throws IOException;

  /**
   * Subset this collection by boundingBox
   * @param boundingBox want only profiles in this lat/lon bounding box.
   * @return subsetted collection, may be null if empty
   * @throws IOException on read error
   */
  public ucar.nc2.ft.ProfileFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;
}
