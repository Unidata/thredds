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
package ucar.nc2.dt;

import ucar.unidata.geoloc.Station;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Implementation of Station
 *
 * @author caron
 */

public class StationImpl extends ucar.unidata.geoloc.StationImpl {
  protected List<StationObsDatatype> obsList;
  protected int count = -1;

  public StationImpl() {
  }

  public StationImpl( String name, String desc, double lat, double lon, double alt) {
    super( name, desc, "", lat, lon, alt);
  }

  public StationImpl( String name, String desc, double lat, double lon, double alt, int count) {
    this( name, desc, lat, lon, alt);
    this.count = count;
  }

  public int getNumObservations() { return (obsList == null) ? count : obsList.size(); }

  /////

  public void incrCount() { count++; }

  public List getObservations() throws IOException {
    if (obsList == null)
        obsList = readObservations();
    return obsList;
  }

  // got to use this or subclass readObservations()
  public void addObs( StationObsDatatype sobs) {
    if (null == obsList) obsList = new ArrayList<StationObsDatatype>();
    obsList.add( sobs);
  }

  protected List<StationObsDatatype> readObservations()  throws IOException { return null; }

}