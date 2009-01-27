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
package ucar.nc2.dt.radial;


import thredds.catalog.query.Station;
import ucar.nc2.dt.TypedDatasetImpl;
import ucar.nc2.dt.StationRadarCollection;
import ucar.nc2.util.CancelTask;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.Product;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import java.io.IOException;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Feb 13, 2007
 * Time: 1:28:37 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class StationRadarCollectionImpl extends TypedDatasetImpl implements StationRadarCollection {

    private StationRadarCollection radarCollection;
    protected HashMap stations;
    protected HashMap relTimesList;
    protected HashMap absTimesList;
    protected DateUnit timeUnit;

    public StationRadarCollectionImpl() {
      super();
    }

    public StationRadarCollectionImpl( StationRadarCollection radarDataset) {
      this.radarCollection = radarDataset;
    }
        /* if (location.startsWith("thredds:")) { // LOOK need to distinguish between a DQC and a Catalog !!
      location = location.substring(8);
      DqcFactory dqcFactory = new DqcFactory(true);
      QueryCapability dqc = dqcFactory.readXML(location);
      if (dqc.hasFatalError()) {
        if (null != log) log.append(dqc.getErrorMessages());
        return null;
      }

      return ucar.nc2.thredds.DqcStationObsDataset.factory( null, dqc);
    } */

    private LatLonRect rect;
    public LatLonRect getBoundingBox() {
        if (rect == null) {
          List stations = null;
          try {
            stations = radarCollection.getStations();
          } catch (IOException e) {
            return null;
          }
          if (stations.size() == 0)
            return null;

          Station s =  (Station) stations.get(0);
          LatLonPointImpl llpt = new LatLonPointImpl();
          llpt.set( s.getLocation().getLatitude(), s.getLocation().getLongitude());
          rect = new LatLonRect(llpt, .001, .001);

          for (int i = 1; i < stations.size(); i++) {
            s =  (Station) stations.get(i);
            llpt.set( s.getLocation().getLatitude(), s.getLocation().getLongitude());
            rect.extend( llpt);
          }
        }

        return rect;
    }

    public List getStations(LatLonRect boundingBox, CancelTask cancel) throws IOException {
        LatLonPointImpl latlonPt = new LatLonPointImpl();
        ArrayList result = new ArrayList();
        List stationC = radarCollection.getStations();
        for (int i = 0; i < stationC.size(); i++) {
          Station s =  (Station) stationC.get(i);
          latlonPt.set( s.getLocation().getLatitude(), s.getLocation().getLongitude());
          if (boundingBox.contains( latlonPt))
            result.add( s);
          if ((cancel != null) && cancel.isCancel()) return null;
        }
        return result;
    }

  public boolean checkStationProduct(String stationName, Product product) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public Station getStation(String name) throws IOException {
        if (stations == null) {
          List stationC = null;
          try {
            stationC = radarCollection.getStations();
          } catch (IOException e) {
            return null;
          }

          stations = new HashMap( 2*stations.size());
          for (int i = 0; i < stationC.size(); i++) {
            Station s =  (Station) stationC.get(i);
            stations.put( s.getName(), s);
          }
        }

        return (Station) stations.get(name);
    }

    public List getStations( )throws IOException {
          return radarCollection.getStations();
     }

    /** Get all the Stations in the collection, allow user to cancel.
     * @param cancel allow user to cancel. Implementors should return ASAP.
     * @return List of Station */

    public List getStations( CancelTask cancel) throws IOException {
         if ((cancel != null) && cancel.isCancel()) return null;
         return  getStations( );
     }

    /** Get all the Stations within a bounding box.
     * @return List of Station */
    public List getStations( LatLonRect boundingBox)throws IOException {
        LatLonPointImpl latlonPt = new LatLonPointImpl();
        ArrayList result = new ArrayList();
        List stationC = radarCollection.getStations();
        for (int i = 0; i < stationC.size(); i++) {
          Station s =  (Station) stationC.get(i);
          latlonPt.set( s.getLocation().getLatitude(), s.getLocation().getLongitude());
          if (boundingBox.contains( latlonPt))
            result.add( s);
        }
        return result;

    }

}
