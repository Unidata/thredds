/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft;

import java.io.IOException;
import java.util.List;

import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

/**
 * A collection of StationProfileFeatures
 * @author caron
 * @since Feb 29, 2008
 */
public interface StationProfileFeatureCollection extends PointFeatureCCC, Iterable<StationProfileFeature> {

  List<StationFeature> getStationFeatures() throws IOException;
  List<StationFeature> getStationFeatures( List<String> stnNames)  throws IOException;
  List<StationFeature> getStationFeatures( ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  StationFeature findStationFeature(String name);
  StationProfileFeature getStationProfileFeature(StationFeature s) throws IOException;

  // subsetting
  StationProfileFeatureCollection subset(List<StationFeature> stations) throws IOException;
  StationProfileFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;
  StationProfileFeatureCollection subset(List<StationFeature> stns, CalendarDateRange dateRange) throws IOException;
  StationProfileFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException;


  ///////////////////////////////

  /**
   * Use the internal iterator to check if there is another StationProfileFeature in the iteration.
   * @return true is there is another StationProfileFeature in the iteration.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  boolean hasNext() throws java.io.IOException;

  /**
   * Use the internal iterator to get the next StationProfileFeature in the iteration.
   * You must call hasNext() before you call this.
   * @return the next StationProfileFeature in the iteration
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  StationProfileFeature next() throws java.io.IOException;

  /**
   * Reset the internal iterator for another iteration over the StationProfileFeature in this Collection.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  void resetIteration() throws IOException;

  /**
   * @deprecated use foreach
   */
  PointFeatureCCIterator getNestedPointFeatureCollectionIterator() throws java.io.IOException;


}
