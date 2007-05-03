package ucar.nc2.thredds;

import ucar.nc2.dt.radial.StationaryRadarCollectionImpl;
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPointImpl;

import thredds.catalog.*;
import thredds.catalog.query.*;
import thredds.catalog.query.Station;

import java.io.IOException;
import java.util.*;
import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Feb 14, 2007
 * Time: 1:09:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class DqcRadarDatasetCollection extends StationaryRadarCollectionImpl {

    static public DqcRadarDatasetCollection factory(InvDataset ds, String dqc_location, StringBuffer errlog) throws IOException {
        return factory(ds.getDocumentation("summary"), dqc_location, errlog);
    }

    static public DqcRadarDatasetCollection factory(String desc, String dqc_location, StringBuffer errlog) throws IOException {

      DqcFactory dqcFactory = new DqcFactory(true);
      QueryCapability dqc = dqcFactory.readXML(dqc_location+"?returns=dqc");
      if (dqc.hasFatalError()) {
        errlog.append(dqc.getErrorMessages());
        return null;
      }

      // have a look at what selectors there are before proceeding
      SelectStation selStation = null;
      SelectList selTime = null;
      SelectService selService = null;
      //SelectGeoRegion selRegion = null;

      ArrayList selectors = dqc.getSelectors();
      for (int i = 0; i < selectors.size(); i++) {
        Selector s =  (Selector) selectors.get(i);
        if (s instanceof SelectStation)
          selStation = (SelectStation) s;
        if (s instanceof SelectList)
          selTime = (SelectList) s;
        if (s instanceof SelectService)
          selService = (SelectService) s;
       // if (s instanceof SelectGeoRegion)
       //   selRegion = (SelectGeoRegion) s;
       }

      // gotta have these
      if (selService == null) {
        errlog.append("DqcRadarDatasetCollection must have Service selector");
        return null;
      }
      if (selStation == null) {
        errlog.append("DqcRadarDatasetCollection must have Station selector");
        return null;
      }
      if (selTime == null) {
        errlog.append("DqcRadarDatasetCollection must have Date selector");
        return null;
      }
     // if (selRegion == null) {
     //   errlog.append("DqcRadarDatasetCollection must have GeoRegion selector");
     //   return null;
     // }

      // decide on which service
      SelectService.ServiceChoice wantServiceChoice = null;
      List services = selService.getChoices();
      for (int i = 0; i < services.size(); i++) {
        SelectService.ServiceChoice serviceChoice =  (SelectService.ServiceChoice) services.get(i);
        if (serviceChoice.getService().equals("HTTPServer") && serviceChoice.getDataFormat().equals("text/xml") )
          // && serviceChoice.getReturns().equals("data")     ) // LOOK kludge
          wantServiceChoice = serviceChoice;
      }


      if (wantServiceChoice == null){
        errlog.append("DqcStationObsDataset must have HTTPServer Service with DataFormat=text/plain, and returns=data");
        return null;
      }

      return new DqcRadarDatasetCollection( desc, dqc, selService, wantServiceChoice, selStation, null, selTime);
    }

    //////////////////////////////////////////////////////////////////////////////////

   // private InvDataset ds;
    private QueryCapability dqc;
    private SelectService selService;
    private SelectStation selStation;
    private SelectList selTime;
    private SelectGeoRegion selRegion;
    private SelectService.ServiceChoice service;
    private HashMap dqcStations;
   // private List avbTimesList;
    private boolean debugQuery = false;

    private DqcRadarDatasetCollection(String desc, QueryCapability dqc, SelectService selService, SelectService.ServiceChoice service,
        SelectStation selStation, SelectGeoRegion selRegion, SelectList selTime) {
      super();
    //  this.ds = ds;
      this.desc = desc;
      this.dqc = dqc;
      this.selService = selService;
      this.selStation = selStation;
      this.selRegion = selRegion;
      this.selTime = selTime;
      this.service = service;

      ArrayList stationList = selStation.getStations();
      stations = new HashMap(stationList.size());
      for (int i = 0; i < stationList.size(); i++) {
        thredds.catalog.query.Station station = (thredds.catalog.query.Station) stationList.get(i);
        DqcRadarStation dd = new DqcRadarStation(station);
        stations.put( station.getValue(), station);
      }

      ArrayList timeList = selTime.getChoices();
      relTimesList = new HashMap(timeList.size());
      for (int i = 0; i < timeList.size(); i++) {
        thredds.catalog.query.Choice tt = (thredds.catalog.query.Choice) timeList.get(i);
        relTimesList.put(tt.getValue(), tt);
      }

      String ql = dqc.getQuery().getUriResolved().toString();

      startDate = new Date();
      endDate = new Date();

      try {
        timeUnit = new DateUnit("hours since 1991-01-01T00:00");
      } catch (Exception e) {
        e.printStackTrace();
      }

    }

    protected void setTimeUnits() { }
    protected void setStartDate() { }
    protected void setEndDate() { }
    protected void setBoundingBox() { }

    public String getTitle() { return dqc.getName(); }
    public String getLocationURI() {return dqc.getCreateFrom(); }
    public String getDescription() { return desc; }

  /** get all radar station.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public List getRadarStations() throws IOException {
        return getStations();
    }
   /** get all radar station.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public List getStations() {
        List sl = selStation.getStations();
        ArrayList dsl = new ArrayList();
        dqcStations = new HashMap();

        for ( Iterator it = sl.iterator(); it.hasNext(); )
        {
           Station s = (Station)it.next();
           DqcRadarStation drs = new DqcRadarStation(s);
           dqcStations.put(s.getValue(), drs);
           dsl.add(drs);
        }
        return dsl;
    }
 /** get all radar station within box.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public List getRadarStations(ucar.nc2.util.CancelTask cancel) throws IOException {
        return getStations(cancel);
    }

  /** get all radar station within box.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public List getStations(ucar.nc2.util.CancelTask cancel) throws IOException {
        if ((cancel != null) && cancel.isCancel()) return null;
        return getRadarStations();
    }
 /** get all radar station within box.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public List getRadarStations(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
         return getStations(boundingBox);
    }
 /** get all radar station within box.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public List getStations(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
        List sl = selStation.getStations();
        ArrayList dsl = new ArrayList();

        for ( Iterator it = sl.iterator(); it.hasNext(); )
        {
           Station s =  (Station)it.next();
           LatLonPointImpl latlonPt = new LatLonPointImpl();
           latlonPt.set( s.getLocation().getLatitude(), s.getLocation().getLongitude());
           if (boundingBox.contains( latlonPt)) {
                DqcRadarStation drs = new DqcRadarStation(s);
                dsl.add(drs);
           }
        }

        return dsl;
    }

 /** get all radar station within box.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
 
    public List getRadarStations(ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException {
        if ((cancel != null) && cancel.isCancel()) return null;
        return getRadarStations(boundingBox);
    }

 /** get all radar station.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public DqcRadarStation getRadarStation( String name) {
        return (DqcRadarStation)dqcStations.get(name);
    }

 /** The dqcstationaryradardataset contains dqcradarstation in the dataset.
   * inner class
  **/
    public class DqcRadarStation extends StationImpl {
      private ArrayList absTimeList;
      private HashMap datasetsDateURI;
      private HashMap invDatasets;

      public DqcRadarStation( thredds.catalog.query.Station s) {
        this.name = s.getValue();
        this.desc = s.getName();
         
        //InvDocumentation doc = s.getDescription();
        //this.desc = (doc == null) ? "" : doc.getInlineContent();
        this.lat = s.getLocation().getLatitude();
        this.lon = s.getLocation().getLongitude();
        this.alt = s.getLocation().getElevation();

         // init
        absTimeList = new ArrayList();
        datasetsDateURI = new HashMap();
        invDatasets = new HashMap();
      }

      // LOOK: currently implementing only "get all"
  /*    protected ArrayList readObservations()  throws IOException {
        ArrayList datasetList = new ArrayList();
        List dsets = queryRadarStation(this.name, null, null);

        for (int i = 0; i< dsets.size(); i++) {
            InvDataset tdata = (InvDataset)dsets.get(i);
            ThreddsDataFactory tdFactory = new ThreddsDataFactory();
            ThreddsDataFactory.Result result;
            result = tdFactory.openDatatype(tdata, null);
            datasetList.add(result.tds);
        }

        return datasetList;
      }        */

      public List getDqcRadarStationTimes() throws IOException {
        queryDqcRadarStation(this.name, null, null );
        return absTimeList;
      }

      public List getDqcRadarStationTimes(Date start, Date end) throws IOException {
        queryDqcRadarStation(this.name,start, end );
        return absTimeList;
      }

   /*   public List getObservations()  throws IOException {
        return readObservations();
      }       */

   /*   public List getData() throws IOException {
        return readObservations();
      }     */

      public RadialDatasetSweep getDqcRadarDataset(Date absTime) throws IOException {
        return queryRadarStationDataset(this.name, absTime);
      }

      public URI getDqcRadarDatasetURI(Date absTime) throws IOException {
        return queryRadarStationURIs(this.name, absTime);
      }

      public List getDqcRadarStationURIs(String rTime) throws IOException {
        return queryRadarStationURIs(this.name, rTime);
      }

      public Iterator getDqcRadarStationDatasets(String rTime)throws IOException {
        return queryRadarStationDatasets(this.name, rTime);
      }

      public ArrayList getDqcRadarStationURIs(Date start, Date end, int interval, int roundTo, int preInt, int postInt) throws IOException {
        return  getDataURIs(this.name, start, end, interval, roundTo, preInt, postInt);
      }

      public ArrayList getDqctRadarStationDatasets(Date start, Date end, int interval, int roundTo, int preInt, int postInt) throws IOException {
        return  getData(this.name, start, end, interval, roundTo, preInt, postInt);
      }

      public int getDqcStationDataCount(Date start, Date end, int interval, int roundTo, int preInt, int postInt) throws IOException {
         ArrayList al =  getDataURIs(this.name, start, end,interval, roundTo, preInt, postInt);
        return al.size();
      }

    }

    /**
     * Getting dataset for a single radar station.
     * @param stnName radar station name
     * @param absTime is absolute time
     * @return RadialDatasetSweep object
     * @throws IOException java io exception
     */
    public RadialDatasetSweep queryRadarStationDataset(String stnName, Date absTime) throws IOException {
        // absTime is a member of  datasetsDateURI
        InvDataset invdata = null;
        DqcRadarStation drs = getRadarStation( stnName);

        if(drs.invDatasets != null)
            invdata = (InvDataset)drs.invDatasets.get(DateUnit.getStandardOrISO(absTime.toString()));

        if(invdata == null){
            InvDataset idata =  queryRadarStation(stnName, absTime);
            List dsets = idata.getDatasets();
            int siz = dsets.size();
            if(siz != 1)
                return null;
             
            invdata = (InvDataset)dsets.get(0);
        }

        if( invdata == null ) {
            throw new IOException("Invalid time selected: " + absTime.toString() + "\n");
        }

        ThreddsDataFactory tdFactory = new ThreddsDataFactory();
        ThreddsDataFactory.Result result;

        result = tdFactory.openDatatype(invdata, null);

        return (RadialDatasetSweep)result.tds;
    }

    /**
     * Getting URI for a single radar station.
     * @param stnName radar station name
     * @param absTime is absolute time
     * @return URI
     * @throws IOException  java io exception
     */
    public URI queryRadarStationURIs(String stnName, Date absTime) throws IOException {
        // absTime is a member of  datasetsDateURI
        URI ui = null;
        DqcRadarStation drs = getRadarStation( stnName);
        if(drs.datasetsDateURI != null) {
            String stime = getISOTime(absTime);
            ui = (URI)drs.datasetsDateURI.get(stime);
        }

        if( ui == null ) {
            InvDataset idata =  queryRadarStation(stnName, absTime);
            List dsets = idata.getDatasets();
            int siz = dsets.size();
            if(siz != 1)
                return null;

            InvDataset invdata = (InvDataset)dsets.get(0);
            List acess = invdata.getAccess();
            InvAccess ia = (InvAccess)acess.get(0);
            ui = ia.getStandardUri();
        }

        if( ui == null ) {
            throw new IOException("Invalid time selected: " + absTime.toString() + "\n");
        }

        return ui;
    }

    /**
     * Getting URI for a single radar station.
     * @param stnName radar station name
     * @param absTime is absolute time
     * @return InvDataset
     * @throws IOException  java io exception
     */
    private InvDataset queryRadarStation(String stnName, Date absTime) throws IOException {
        String stime = getISOTime(absTime);
        // construct a query like this:
        // http://motherlode.ucar.edu:9080/thredds/idd/radarLevel2?returns=catalog&stn=KFTG&dtime=latest
        StringBuffer queryb = new StringBuffer();

        queryb.append( dqc.getQuery().getUriResolved().toString());
        queryb.append("serviceType=OPENDAP");
        queryb.append("&stn="+stnName);
        queryb.append("&dtime="+stime);


        URI catalogURI;

        try {
          catalogURI =  new URI(queryb.toString());
        } catch (java.net.URISyntaxException e) {
          throw new IOException( "** MalformedURLException on URL <"+">\n"+e.getMessage()+"\n");
        }

        InvCatalogFactory factory = new InvCatalogFactory("default", false);

        InvCatalogImpl catalog = (InvCatalogImpl) factory.readXML( catalogURI);

        StringBuffer buff = new StringBuffer();
        if (!catalog.check( buff)) {
          throw new IOException("Invalid catalog <"+ catalogURI+">\n"+buff.toString());
        }

        List datasets = catalog.getDatasets();

        InvDataset idata = (InvDataset)datasets.get(0);

        return idata;
    }

    /**
     * Getting invDataset list for a single radar station.
     * @param stnName radar station name
     * @param rTime is relative time, such as latest, 1hour
     * @return list of invDatasets
     * @throws IOException java io exception
     */
    private List queryRadarStation(String stnName, String rTime) throws IOException {
        // rTime is relative time, 1hour, 6hour, etc.., and it is a member of  relTimesList
        if( relTimesList.get(rTime) == null ) {
            throw new IOException("Invalid time selected: " + rTime + "\n");
        }

        // construct a query like this:
        // http://motherlode.ucar.edu:9080/thredds/idd/radarLevel2?returns=catalog&stn=KFTG&dtime=latest
        StringBuffer queryb = new StringBuffer();

        queryb.append( dqc.getQuery().getUriResolved().toString());
        queryb.append("serviceType=OPENDAP");
        queryb.append("&stn="+stnName);
        queryb.append("&dtime="+rTime);


        URI catalogURI;

        try {
          catalogURI =  new URI(queryb.toString());
        } catch (java.net.URISyntaxException e) {
          throw new IOException( "** MalformedURLException on URL <"+">\n"+e.getMessage()+"\n");
        }

        InvCatalogFactory factory = new InvCatalogFactory("default", false);

        InvCatalogImpl catalog = (InvCatalogImpl) factory.readXML( catalogURI);

        StringBuffer buff = new StringBuffer();
        if (!catalog.check( buff)) {
          throw new IOException("Invalid catalog <"+ catalogURI+">\n"+buff.toString());
        }

        List datasets = catalog.getDatasets();

        return datasets;
    }

    /**
     * Getting dataset list for a single radar station.
     * @param stnName radar station name
     * @param rTime is relative time, such as latest, 1hour
     * @return iterator
     * @throws IOException java io exception
     */
    private Iterator queryRadarStationDatasets(String stnName, String rTime) throws IOException {
        // rTime is relative time, 1hour, 6hour, etc.., and it is a member of  relTimesList

        List datasets = queryRadarStation(stnName, rTime);

       // for (int i = 0; i< datasets.size(); i++) {
        ArrayList datasetList = new ArrayList();

        InvDataset idata = (InvDataset) datasets.get(0);
        //    List ddate = idata.getDates();
        List dsets = idata.getDatasets();
        for (int i = 0; i< dsets.size(); i++) {
            InvDataset tdata = (InvDataset)dsets.get(i);
            ThreddsDataFactory tdFactory = new ThreddsDataFactory();
            ThreddsDataFactory.Result result;
            result = tdFactory.openDatatype(tdata, null);
            datasetList.add(result.tds);
        }

        return datasetList.iterator();
    }

    /**
     * Getting URI list for a single radar station.
     * @param stnName radar station name
     * @param rTime is relative time, such as latest, 1hour
     * @return list of URIs
     * @throws IOException java io exception
     */
    public ArrayList queryRadarStationURIs(String stnName, String rTime) throws IOException {
        // rTime is relative time, 1hour, 6hour, etc.., and it is a member of  relTimesList

        List datasets = queryRadarStation(stnName, rTime);

        ArrayList datasetsURI = new ArrayList();

        InvDataset idata = (InvDataset) datasets.get(0);
        //    List ddate = idata.getDates();
        List dsets = idata.getDatasets();
        for (int i = 0; i< dsets.size(); i++) {
            InvDataset tdata = (InvDataset)dsets.get(i);
            List acess = tdata.getAccess();
            InvAccess ia = (InvAccess)acess.get(0);
            URI d = ia.getStandardUri();
            datasetsURI.add(d);
        }


        return datasetsURI;
    }

    /**
     * Getting invDataset list for a single radar station.
     * @param stnName radar station name
     * @param start of the time
     * @param end of the time
     * @return list of invDataset
     * @throws IOException java io exception
     */
    private ArrayList queryRadarStation(String stnName, Date start, Date end) throws IOException {
        // http://motherlode.ucar.edu:9080/thredds/idd/radarLevel2?returns=catalog&stn=KFTG&dtime=latest
        StringBuffer queryb = new StringBuffer();

        queryb.append( dqc.getQuery().getUriResolved().toString());
        queryb.append("serviceType=OPENDAP");
        queryb.append("&stn="+stnName);
        if(start == null && end == null)
            queryb.append("&dtime=all");
        else {
            String stime = getISOTime(start);
            String etime = getISOTime(end);
            queryb.append("&dateStart="+stime);
            queryb.append("&dateEnd="+etime);
        }


        URI catalogURI;
        try {
          catalogURI =  new URI(queryb.toString());
        } catch (java.net.URISyntaxException e) {
          throw new IOException( "** MalformedURLException on URL <"+">\n"+e.getMessage()+"\n");
        }

        InvCatalogFactory factory = new InvCatalogFactory("default", false);

        InvCatalogImpl catalog = (InvCatalogImpl) factory.readXML( catalogURI);
        StringBuffer buff = new StringBuffer();
        if (!catalog.check( buff)) {
          throw new IOException("Invalid catalog <"+ catalogURI+">\n"+buff.toString());
        }

        List datasets = catalog.getDatasets();

        InvDataset idata = (InvDataset) datasets.get(0);
        //    List ddate = idata.getDates();
        ArrayList dsets = (ArrayList)idata.getDatasets();

        return dsets;
    }

    /**
     * Getting data Iterator for a single radar station.
     * @param stnName radar station name
     * @param start of the time
     * @param end of the time
     * @return data URI list
     * @throws IOException java io exception
     */
     private ArrayList getRadarStationURIs(String stnName, Date start, Date end ) throws IOException {
        DqcRadarStation drs = getRadarStation( stnName);
        queryDqcRadarStation(stnName, start,  end );
        ArrayList datasetsURI = new ArrayList();

        HashMap uMap = drs.datasetsDateURI;
        Iterator it = uMap.values().iterator();
        while(it.hasNext()){
            datasetsURI.add(it.next());
        }

        return datasetsURI;
    }

    /**
     * Getting data Iterator for a single radar station.
     * @param stnName radar station name
     * @param start of the time
     * @param end of the time
     * @return dataset list
     * @throws IOException java io exception
     */
    private ArrayList getRadarStationDatasets(String stnName, Date start, Date end) throws IOException {

        ArrayList datasetList = new ArrayList();

        List dsets = queryRadarStation(stnName, start, end);

        for (int i = 0; i< dsets.size(); i++) {
            InvDataset tdata = (InvDataset)dsets.get(i);
            ThreddsDataFactory tdFactory = new ThreddsDataFactory();
            ThreddsDataFactory.Result result;
            result = tdFactory.openDatatype(tdata, null);
            datasetList.add(result.tds);
        }

        return datasetList;
    }

    /**
     * Getting data URI list for a single radar station.
     * @param stnName radar station name
     * @param start of the time
     * @param end of the time
     * @return list of URIs
     * @throws IOException java io exception
     */
    private void queryDqcRadarStation(String stnName,Date start, Date end ) throws IOException {
        DqcRadarStation drs = getRadarStation( stnName);
        int sz = drs.absTimeList.size();
        int bf = 9*60*1000;
        if( sz > 0 && start != null && end != null) {
            String t1 = (String) drs.absTimeList.get(0);
            String t2 = (String) drs.absTimeList.get(sz-1);
            Date ts1 = (Date)DateUnit.getStandardOrISO(t1);
            Date ts2 = (Date)DateUnit.getStandardOrISO(t2);
            if (Math.abs(end.getTime() - ts1.getTime()) <= bf
                    && Math.abs(start.getTime() - ts2.getTime()) <= bf ) {
                return;
            }
        }

        List dsets = queryRadarStation(stnName, start, end);
        drs.datasetsDateURI = new HashMap();
        drs.absTimeList = new ArrayList();
        drs.invDatasets = new HashMap();
        for (int i = 0; i< dsets.size(); i++) {
            InvDataset tdata = (InvDataset)dsets.get(i);
            List acess = tdata.getAccess();
            List dates = tdata.getDates();
            InvAccess ia = (InvAccess)acess.get(0);
            URI d = ia.getStandardUri();
            drs.datasetsDateURI.put(dates.get(0).toString(), d);
            drs.absTimeList.add(dates.get(0).toString());
            drs.invDatasets.put(dates.get(0).toString(), tdata);
          /*  try {

                drs.invDatasets.put(dates.get(0).toString(), tdata);
            } catch (java.lang.Exception e) {
                throw new IOException( e.getMessage()+"\n");
            }  */

        }


    }


    /**
     * Getting data URI list for a single radar station.
     * @param stnName radar station name
     * @param start of the time
     * @param end of the time
     * @return list of URIs
     * @throws IOException java io exception
     */
    public ArrayList getRadarStationTimes(String stnName,Date start, Date end ) throws IOException {
        DqcRadarStation drs = getRadarStation( stnName);
        queryDqcRadarStation(stnName, start,  end );

        return drs.absTimeList;
    }

    public HashMap getRadarStationURIsMap(String stnName,Date start, Date end ) throws IOException {
        DqcRadarStation drs = getRadarStation( stnName);
        queryDqcRadarStation(stnName, start,  end );

        return drs.datasetsDateURI;
    }


    /**
     * Getting data Iterator for a single radar station.
     * @param bufferSize
     * @return data iterator
     * @throws IOException java io exception
     */
    public DataIterator getDataIterator(int bufferSize) throws IOException {
        return null;
    }

    /**
     * Getting data relative time list for a single radar station.
     * @param stn radar station name
     *  @return list of relative times
     * @throws IOException java io exception
     */
    private List queryRadarStationRTimes( String stn) throws IOException{
            return selTime.getChoices();
    }



    /**
     * Getting data URIs for a single radar station, with time range.
     * @param sName radar station name
     * @param start the start time
     * @param end the end time
     * @param interval the time interval-- in seconds
     * @param roundTo the round to time -- in seconds
     * @param preInt the time range before interval
     * @param postInt the time range after interval
     * @return list of URIs
     */
    public ArrayList getDataURIs( String sName, Date start, Date end, int interval, int roundTo, int preInt,
                             int postInt, ucar.nc2.util.CancelTask cancel) throws IOException {
        if ((cancel != null) && cancel.isCancel())
            return null;
        return getDataURIs( sName, start, end, interval, roundTo, preInt, postInt);
    }

    /**
     * Getting data for a single radar station, with time range.
     * @param sName radar station name
     * @param start the start time
     * @param end the end time
     * @param interval the time interval-- in seconds
     * @param roundTo the round to time -- in seconds
     * @param preInt the time range before interval
     * @param postInt the time range after interval
     *  @return list of radialDatasetSweep
     */

    public ArrayList getData( String sName, Date start, Date end, int interval, int roundTo, int preInt,
                         int postInt, ucar.nc2.util.CancelTask cancel) throws IOException {
        if ((cancel != null) && cancel.isCancel())
            return null;
        return getData( sName, start, end, interval, roundTo, preInt, postInt);
    }

    /**
     * Getting data for a single radar station, with time range.
     * @param sName radar station name
     * @param start the start time
     * @param end the end time
     * @param interval the time interval-- in seconds
     * @param roundTo the round to time -- in seconds
     * @param preInt the time range before interval
     * @param postInt the time range after interval
     *  @return list of radialDatasetSweep
     */
    public ArrayList getData( String sName, Date start, Date end, int interval, int roundTo, int preInt,
                         int postInt) throws IOException {

        if(interval == 0) {
           return getRadarStationDatasets(sName, start, end );
        }

        DqcRadarStation drs = getRadarStation(sName);
        queryDqcRadarStation(sName, start,  end);

        // create a list to hold URIs
        ArrayList datasetList = new ArrayList();
        int size = drs.absTimeList.size();
        long [] tList = new long[size];
        for ( int i = 0; i<  size; i++ )
        {
            Date d = (Date)DateUnit.getStandardOrISO((String)drs.absTimeList.get(i));
            tList[i] = d.getTime() ;
        }

        // in time order
        Arrays.sort(tList);

        // get the interval list of time
        ArrayList ivlList = getTimeIntervalList(drs.absTimeList, interval, roundTo);
        int sz = ivlList.size();
        //
        Calendar cal = Calendar.getInstance();
        cal.setTime(DateUnit.getStandardOrISO((String)ivlList.get(0)));
                if(start == null) {
            start = cal.getTime();
        }
        if(end == null){
            cal.setTime(DateUnit.getStandardOrISO((String)ivlList.get(sz-1)));
            end = cal.getTime();
        }
        Date nextStart = cal.getTime();  // same as start
        // for the first select
        Date preNextStart = start;
        cal.setTime(nextStart);
        cal.add(Calendar.SECOND, postInt);
        Date postNextStart = cal.getTime();

        long [] stList = new long[size];
        stList[0] = nextStart.getTime();
        long timeInd = 0;
        long deltTime = (postInt >= preInt)? postInt*1000 : preInt*1000;
        int j = 0;
        for (int i = 0; i < size; i++ )
        {
           long d =  tList[i];

           if (d  >= preNextStart.getTime() && d  <= postNextStart.getTime() ) {
                int l = 0;
                if( Math.abs(d - nextStart.getTime()) <= deltTime ){
                    deltTime = Math.abs(d - nextStart.getTime());
                    timeInd = d;
                }
           }

           if( d > postNextStart.getTime()) {
                j++;
                if( timeInd != 0) {
                    cal.setTimeInMillis(timeInd);
                    Date dd = cal.getTime();
                    InvDataset tdata  = (InvDataset)drs.invDatasets.get(getISOTime(dd));
                    ThreddsDataFactory tdFactory = new ThreddsDataFactory();
                    ThreddsDataFactory.Result result;
                    result = tdFactory.openDatatype(tdata, null);
                    datasetList.add(result.tds);
                }
                timeInd = 0; //reset init value
                deltTime = (postInt >= preInt)? postInt*1000 : preInt*1000;

                cal.setTime(DateUnit.getStandardOrISO((String)ivlList.get(j)));
                nextStart = cal.getTime();

                //cal.setTime(nextStart);
                cal.add(Calendar.SECOND, postInt);
                postNextStart = cal.getTime();
                //
                cal.setTime(nextStart);
                cal.add(Calendar.SECOND, (0 - preInt));
                preNextStart = cal.getTime();
                tList[j] = nextStart.getTime();
                if( nextStart.getTime() >= end.getTime()) {
                    nextStart = end;

                    postNextStart = end;
                    //
                    cal.setTime(end);
                    cal.add(Calendar.SECOND, (0- preInt));
                    preNextStart = cal.getTime();
                }
                else if(postNextStart.getTime() >= end.getTime() ) {
                    postNextStart = end;
                }
           }

        }


        return datasetList;

    }

   /**
   * Getting data for a single radar station, with time range and interval.
   * @param sName radar station  name
   * @param start the start time
   * @param end the end time
   * @param interval the time interval
   * @param preInt the time range before interval
   * @param postInt the time range after interval
   * @return list of radialDatasetSweep
   * @throws IOException java io exception
   */

    public ArrayList getDataURIs( String sName, Date start, Date end, int interval, int roundTo,
                                  int preInt, int postInt) throws IOException {
        if(interval == 0) {
           return getRadarStationURIs(sName, start, end );
        }

        DqcRadarStation drs = getRadarStation(sName);
        queryDqcRadarStation(sName, start,  end);

        // create a list to hold URIs
        ArrayList datasetsURI = new ArrayList();
        int size = drs.absTimeList.size();
        long [] tList = new long[size];
        for ( int i = 0; i<  size; i++ )
        {
            Date d = (Date)DateUnit.getStandardOrISO((String)drs.absTimeList.get(i));
            tList[i] = d.getTime() ;
        }

        // in time order
        Arrays.sort(tList);

        // get the interval list of time
        ArrayList ivlList = getTimeIntervalList(drs.absTimeList, interval, roundTo);
        int sz = ivlList.size();
        //
        Calendar cal = Calendar.getInstance();
        cal.setTime(DateUnit.getStandardOrISO((String)ivlList.get(0)));
        if(start == null) {
            start = cal.getTime();
        }
        if(end == null){
            cal.setTime(DateUnit.getStandardOrISO((String)ivlList.get(sz-1)));
            end = cal.getTime();
        }
        Date nextStart = start;  // same as start
        // for the first select
        Date preNextStart = nextStart;
        cal.setTime(nextStart);
        cal.add(Calendar.SECOND, postInt);
        Date postNextStart = cal.getTime();

        long [] stList = new long[size];
        stList[0] = nextStart.getTime();
        long timeInd = 0;
        long deltTime = (postInt >= preInt)? postInt*1000 : preInt*1000;
        int j = 0;
        for (int i = 0; i < size; i++ )
        {
           long d =  tList[i];

           if (d  >= preNextStart.getTime() && d  <= postNextStart.getTime() ) {
                int l = 0;
                if( Math.abs(d - nextStart.getTime()) <= deltTime ){
                    deltTime = Math.abs(d - nextStart.getTime());
                    timeInd = d;
                }
           }

           if( d > postNextStart.getTime()) {
                j++;
                if( timeInd != 0) {
                   cal.setTimeInMillis(timeInd);
                   Date dd = cal.getTime();
                   URI ui = (URI)drs.datasetsDateURI.get(getISOTime(dd));
                   datasetsURI.add(ui);
                }
                timeInd = 0; //reset init value
                deltTime = (postInt >= preInt)? postInt*1000 : preInt*1000;

                cal.setTime(DateUnit.getStandardOrISO((String)ivlList.get(j)));
                nextStart = cal.getTime();

                //cal.setTime(nextStart);
                cal.add(Calendar.SECOND, postInt);
                postNextStart = cal.getTime();
                //
                cal.setTime(nextStart);
                cal.add(Calendar.SECOND, (0 - preInt));
                preNextStart = cal.getTime();
                tList[j] = nextStart.getTime();
                if( nextStart.getTime() >= end.getTime()) {
                    nextStart = end;

                    postNextStart = end;
                    //
                    cal.setTime(end);
                    cal.add(Calendar.SECOND, (0- preInt));
                    preNextStart = cal.getTime();
                }
                else if(postNextStart.getTime() >= end.getTime() ) {
                    postNextStart = end;
                }
           }

        }


        return datasetsURI;
    }
    /**
     * Getting data URI for a single radar station, with time range and interval.
     * @param timeList is the list of time to be parsed
     * @param interval the interval time
     * @param roundTo the round to time
     * @return list of times
     * @throws IOException java io exception
     */
    public  ArrayList getTimeIntervalList( ArrayList timeList, int interval, int roundTo) throws IOException {

        // create a list to hold URIs

        ArrayList intervalList = new ArrayList();
        int size = timeList.size();
        long [] tList = new long[size];
        for ( int i = 0; i<  size; i++ )
        {
            Date d = (Date)DateUnit.getStandardOrISO((String)timeList.get(i));
            tList[i] = d.getTime() ;
        }

        // in time order
        Arrays.sort(tList);
        //
        if(interval == 0 && roundTo == 0) {
             for ( int i = 0; i<  size; i++ )
            {
                Date d = new Date(tList[i]);
                intervalList.add(getISOTime(d));

            }
            return intervalList;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis((long)tList[0]);
        Date start = cal.getTime();
        cal.setTimeInMillis((long)tList[size-1]);
        Date end = cal.getTime();

        cal.setTime(start);
        intervalList.add(getISOTime(start));

        // same as start
        Date nextStart = cal.getTime();

        while ( nextStart.getTime() < (end.getTime() - interval*1000) )
        {
            cal.setTime(nextStart);
            cal.add(Calendar.SECOND, interval);
            long rti = cal.getTimeInMillis();
            long r2i = roundTo(roundTo, rti/1000)*1000;
            cal.setTimeInMillis(r2i);
            nextStart = cal.getTime();
            intervalList.add(getISOTime(nextStart));
        }

        // add the last one of the list
        intervalList.add(getISOTime(end));

        return intervalList;
    }

    public String getISOTime(Date d){
        java.text.SimpleDateFormat isoDateTimeFormat;
        isoDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

        return isoDateTimeFormat.format(d);
    }


  /**
   * Getting data for a single radar station, with time range and interval.
   * @param roundTo
   * @param seconds to be round to
   * @return round to second
   */
    public static long roundTo(long roundTo, long seconds) {
        int roundToSeconds = (int) (roundTo );
        if (roundToSeconds == 0) {
            return seconds;
        }
        return seconds - ((int) seconds) % roundToSeconds;
    }



    public static void main(String args[]) throws IOException {
        StringBuffer errlog = new StringBuffer();
        String dqc_location = "http://motherlode.ucar.edu:9080/thredds/idd/radarLevel2";
        DqcRadarDatasetCollection ds = factory("test", dqc_location, errlog);
        System.out.println(" errs= "+errlog);

        List stns = ds.getRadarStations();
        System.out.println(" nstns= "+stns.size());



        DqcRadarStation stn = (DqcRadarStation)(stns.get(2));

        List absList = stn.getDqcRadarStationURIs("1hour");
        assert null != absList;
       // List ulist = stn.getRadarStationURIs();
       // assert null != ulist;
        Date ts1 = (Date)DateUnit.getStandardOrISO("2007-04-29T12:12:00");
        Date ts2 = (Date)DateUnit.getStandardOrISO("2007-04-29T23:12:00");
        URI stURL = stn.getDqcRadarDatasetURI(ts1);
        List tlist = stn.getDqcRadarStationTimes(ts1, ts2);
        int sz = tlist.size();
        
        //Date ts0 = DateFromString.getDateUsingCompleteDateFormat((String)tlist.get(1),"yyyy-MM-dd'T'HH:mm:ss");
        Date ts = (Date)DateUnit.getStandardOrISO((String)tlist.get(1));
        java.text.SimpleDateFormat isoDateTimeFormat;
        isoDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        String st = isoDateTimeFormat.format(ts);

        stURL = stn.getDqcRadarDatasetURI(ts);
        assert null != stURL;
        tlist = stn.getDqcRadarStationTimes(ts1, ts2);
        ArrayList rtList = new ArrayList();
        ArrayList rbList = new ArrayList();
      //  ArrayList al = getTimeIntervalList((ArrayList)tlist, 3600, 60*60);
        for(int i= 0; i< tlist.size(); i++) {
            Date tsi = (Date)DateUnit.getStandardOrISO((String)tlist.get(i));
            long rti = tsi.getTime();
            long r2i = roundTo(15*60, rti/1000)*1000;
            rbList.add( isoDateTimeFormat.format(new Date(r2i)));
            rtList.add( isoDateTimeFormat.format(tsi));
        }

        
        sz = tlist.size();

        ArrayList dList = ds.getData("KABX", ts1, ts2, 3600, 60*60, 500, 500);
        assert null != dList;
        ArrayList iList = ds.getDataURIs("KABX", ts1, ts2, 3600, 60*60, 500, 500 );
        assert null != iList;

        Iterator it = stn.getDqcRadarStationDatasets("1hour");
        assert null != it;

        /* List data = ds.getData((DqcRadarStation)stns.get(2), ts1, ts2);
        Iterator it1 = data.iterator();
        while(it1.hasNext()) {
            RadialDatasetSweep rds = (RadialDatasetSweep)it1.next();
            assert null != rds;
        }  */
     }
}
