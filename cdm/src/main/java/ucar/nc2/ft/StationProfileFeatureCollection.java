/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft;

import ucar.unidata.geoloc.Station;

import java.util.List;
import java.io.IOException;

/**
 * A collection of StationProfileFeatures
 * @author caron
 * @since Feb 29, 2008
 */
public interface StationProfileFeatureCollection extends NestedPointFeatureCollection, StationCollection {

  /**
   * Use the internal iterator to check if there is another StationProfileFeature in the iteration.
   * @return true is there is another StationProfileFeature in the iteration.
   * @throws java.io.IOException on read error
   */
  public boolean hasNext() throws java.io.IOException;

  /**
   * Use the internal iterator to get the next StationProfileFeature in the iteration.
   * You must call hasNext() before you call this.
   * @return the next StationProfileFeature in the iteration
   * @throws java.io.IOException on read error
   */
  public StationProfileFeature next() throws java.io.IOException;

  /**
   * Reset the internal iterator for another iteration over the StationProfileFeature in this Collection.
   * @throws java.io.IOException on read error
   */
  public void resetIteration() throws IOException;

  /**
   * Get a subsetted StationProfileFeatureCollection
   *
   * @param stations only contain these stations
   * @return subsetted collection
   * @throws java.io.IOException on i/o error
   */
  public StationProfileFeatureCollection subset(List<Station> stations) throws IOException;

  /**
   * Get the StationProfileFeature for a specific Station.
   *
   * @param s at this station
   * @return collection of data for this Station.
   * @throws java.io.IOException on i/o error
   */
  public StationProfileFeature getStationProfileFeature(Station s) throws IOException;

}
