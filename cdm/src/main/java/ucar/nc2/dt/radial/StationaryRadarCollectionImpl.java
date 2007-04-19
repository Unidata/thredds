package ucar.nc2.dt.radial;


import ucar.nc2.dt.point.PointObsDatasetImpl;
import ucar.nc2.dt.StationaryRadarCollection;
import ucar.nc2.dt.Station;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dt.TypedDatasetImpl;
import ucar.nc2.util.CancelTask;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.io.IOException;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Feb 13, 2007
 * Time: 1:28:37 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class StationaryRadarCollectionImpl extends TypedDatasetImpl implements StationaryRadarCollection {

    private StationaryRadarCollection radarCollection;
    protected HashMap stations;
    protected HashMap relTimesList;
    protected HashMap absTimesList;
    protected DateUnit timeUnit;

    public StationaryRadarCollectionImpl() {
      super();
    }

    public StationaryRadarCollectionImpl( StationaryRadarCollection radarDataset) {
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
            stations = radarCollection.getRadarStations();
          } catch (IOException e) {
            return null;
          }
          if (stations.size() == 0)
            return null;

          Station s =  (Station) stations.get(0);
          LatLonPointImpl llpt = new LatLonPointImpl();
          llpt.set( s.getLatitude(), s.getLongitude());
          rect = new LatLonRect(llpt, .001, .001);

          for (int i = 1; i < stations.size(); i++) {
            s =  (Station) stations.get(i);
            llpt.set( s.getLatitude(), s.getLongitude());
            rect.extend( llpt);
          }
        }

        return rect;
    }

    public List getStations(LatLonRect boundingBox, CancelTask cancel) throws IOException {
        LatLonPointImpl latlonPt = new LatLonPointImpl();
        ArrayList result = new ArrayList();
        List stationC = radarCollection.getRadarStations();
        for (int i = 0; i < stationC.size(); i++) {
          Station s =  (Station) stationC.get(i);
          latlonPt.set( s.getLatitude(), s.getLongitude());
          if (boundingBox.contains( latlonPt))
            result.add( s);
          if ((cancel != null) && cancel.isCancel()) return null;
        }
        return result;
    }

    public Station getStation(String name) throws IOException {
        if (stations == null) {
          List stationC = null;
          try {
            stationC = radarCollection.getRadarStations();
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
         return null;
     }



    public List getStations( CancelTask cancel) throws IOException {
         if ((cancel != null) && cancel.isCancel()) return null;
         return  getStations( );
     }

    public List getStations( LatLonRect boundingBox)throws IOException {
        LatLonPointImpl latlonPt = new LatLonPointImpl();
        ArrayList result = new ArrayList();
        List stationC = radarCollection.getRadarStations();
        for (int i = 0; i < stationC.size(); i++) {
          Station s =  (Station) stationC.get(i);
          latlonPt.set( s.getLatitude(), s.getLongitude());
          if (boundingBox.contains( latlonPt))
            result.add( s);
        }
        return result;

    }

    public int getStationDataCount( Station s) {
          return 0;
    }



    public List getData(Station s, Date start, Date end, CancelTask cancel) throws IOException {
        double startTime = timeUnit.makeValue( start);
        double endTime = timeUnit.makeValue( end);
        ArrayList result = new ArrayList();
        List stationObs = radarCollection.getData( s, start, end);

        for (int i = 0; i < stationObs.size(); i++) {
            RadialDatasetSweep rds =  (RadialDatasetSweep) stationObs.get(i);
                
            double timeValue = rds.getStartDate().getTime();
            if ((timeValue >= startTime) && (timeValue <= endTime))
              result.add( rds);
            if ((cancel != null) && cancel.isCancel()) return null;
        }

        return result;

    }



   // public List getData(List stations, Date start, Date end) throws IOException {
    //        return null;
   // }


   // public List getData(List stations, String product, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException {
   //       if ((cancel != null) && cancel.isCancel()) return null;
   //       return getData( stations, start, end);
   // }

    public boolean checkStationProduct(Station s, String product) {
        return true;
    }

     public boolean checkStationProduct(String product) {
         return true;
     }


     public List getStationDataProducts( Station s) {
         return null;
     }







}
