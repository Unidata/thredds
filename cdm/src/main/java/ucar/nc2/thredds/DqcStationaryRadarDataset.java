package ucar.nc2.thredds;

import ucar.nc2.dt.radial.StationaryRadarCollectionImpl;
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.units.Unit;

import thredds.catalog.*;
import thredds.catalog.query.*;
import thredds.catalog.query.Station;

import java.io.IOException;
import java.util.*;
import java.net.URI;
import java.text.DateFormat;

import visad.QuickSort;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Feb 14, 2007
 * Time: 1:09:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class DqcStationaryRadarDataset extends StationaryRadarCollectionImpl {

    static public DqcStationaryRadarDataset factory(InvDataset ds, String dqc_location, StringBuffer errlog) throws IOException {
        return factory(ds.getDocumentation("summary"), dqc_location, errlog);
    }

    static public DqcStationaryRadarDataset factory(String desc, String dqc_location, StringBuffer errlog) throws IOException {

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
        errlog.append("DqcStationaryRadarDataset must have Service selector");
        return null;
      }
      if (selStation == null) {
        errlog.append("DqcStationaryRadarDataset must have Station selector");
        return null;
      }
      if (selTime == null) {
        errlog.append("DqcStationaryRadarDataset must have Date selector");
        return null;
      }
     // if (selRegion == null) {
     //   errlog.append("DqcStationaryRadarDataset must have GeoRegion selector");
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

      return new DqcStationaryRadarDataset( desc, dqc, selService, wantServiceChoice, selStation, null, selTime);
    }

    //////////////////////////////////////////////////////////////////////////////////

   // private InvDataset ds;
    private QueryCapability dqc;
    private SelectService selService;
    private SelectStation selStation;
    private SelectList selTime;
    private SelectGeoRegion selRegion;
    private SelectService.ServiceChoice service;
   // private List avbTimesList;
    private boolean debugQuery = true;
    private ArrayList absTimeList;
    private HashMap datasetsDateURI;
    private HashMap invDatasets;

    private DqcStationaryRadarDataset(String desc, QueryCapability dqc, SelectService selService, SelectService.ServiceChoice service,
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
   * @return List of type DqcRadarStation objects */
    public List getRadarStations() throws IOException {
        return getStations();
    }
/** get all radar station.
   * @return List of type DqcRadarStation objects */
    public List getStations() {
        List sl = selStation.getStations();
        ArrayList dsl = new ArrayList();

        for ( Iterator it = sl.iterator(); it.hasNext(); )
        {
           DqcRadarStation drs = new DqcRadarStation((Station)it.next());
           dsl.add(drs);
        }
        return dsl;
    }
/** get all radar station within box.
   * @return List of type DqcRadarStation objects */
    public List getRadarStations(ucar.nc2.util.CancelTask cancel) throws IOException {
        return getStations(cancel);
    }
/** get all radar station within box.
   * @return List of type DqcRadarStation objects */
    public List getStations(ucar.nc2.util.CancelTask cancel) throws IOException {
        if ((cancel != null) && cancel.isCancel()) return null;
        return getRadarStations();
    }
/** get all radar station within box.
   * @return List of type DqcRadarStation objects */
    public List getRadarStations(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
         return getStations(boundingBox);
    }
/** get all radar station within box.
   * @return List of type DqcRadarStation objects */
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
   * @return List of type DqcRadarStation objects */
    public List getRadarStations(ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException {
        if ((cancel != null) && cancel.isCancel()) return null;
        return getRadarStations(boundingBox);
    }

 /** get all radar station.
   * @return List of type DqcRadarStation objects */
    public DqcRadarStation getRadarStation( String name) {
        return new DqcRadarStation((Station)stations.get(name));
    }

 /** The dqcstationaryradardataset contains dqcradarstation in the dataset.
   * inner class
  **/
    public class DqcRadarStation extends StationImpl {

      public DqcRadarStation( thredds.catalog.query.Station s) {
        this.name = s.getValue();
        this.desc = s.getName();
         
        //InvDocumentation doc = s.getDescription();
        //this.desc = (doc == null) ? "" : doc.getInlineContent();
        this.lat = s.getLocation().getLatitude();
        this.lon = s.getLocation().getLongitude();
        this.alt = s.getLocation().getElevation();
      }

      // LOOK: currently implementing only "get all"
      protected ArrayList readObservations()  throws IOException {
        ArrayList datasetList = new ArrayList();
        List dsets = queryRadarStation(this.name);

        for (int i = 0; i< dsets.size(); i++) {
            InvDataset tdata = (InvDataset)dsets.get(i);
            ThreddsDataFactory tdFactory = new ThreddsDataFactory();
            ThreddsDataFactory.Result result;
            result = tdFactory.openDatatype(tdata, null);
            datasetList.add(result.tds);
        }

        return datasetList;
      }

      public List getRadarStationTimes() throws IOException {
        return queryRadarStationABSTimes(this.name);
      }

      public List getRadarStationTimes(Date start, Date end) throws IOException {
        if(absTimeList == null)
           queryRadarStationURIs(this.name);
        ArrayList subList = new ArrayList();

        for ( Iterator it = absTimeList.iterator(); it.hasNext(); )
        {
           Date d =  (Date)it.next();

           if (d.getTime() >= start.getTime() && d.getTime() <= end.getTime() ) {
                subList.add(DateUnit.getStandardOrISO(d.toString()));
           }
        }
        return subList;
      }
        
      public ArrayList getObservations()  throws IOException {
        return readObservations();
      }

      public List getData() throws IOException {
        return readObservations();
      }

      public RadialDatasetSweep getRadarDataset(Date absTime) throws IOException {
        return queryRadarStationDataset(this.name, absTime);
      }

      public URI getRadarDatasetURI(Date absTime) throws IOException {
        return queryRadarStationURIs(this.name, absTime);
      }

      public List getRadarStationURIs(String rTime) throws IOException {
        return queryRadarStationURIs(this.name, rTime);
      }

      public Iterator getRadarStationDatasets(String rTime)throws IOException {
        return queryRadarStationDatasets(this.name, rTime);
      }

      public ArrayList getRadarStationURIs() throws IOException {
        return  queryRadarStationURIs(this.name);
      }

      public Iterator getRadarStationDatasets() throws IOException {
        return  queryRadarStationDatasets(this.name);
      }

      public int getStationDataCount( ) throws IOException {
        if( absTimeList == null)
           queryRadarStationURIs(this.name);
        return absTimeList.size();
      }


    }

    /**
     * Getting dataset for a single radar station.
     * @param stnName radar station name
     * @param absTime, is absolute time
     */
    private RadialDatasetSweep queryRadarStationDataset(String stnName, Date absTime) throws IOException {
        // absTime is a member of  datasetsDateURI

        if(invDatasets == null)
            queryRadarStation(stnName);

        InvDataset invdata = (InvDataset)invDatasets.get(DateUnit.getStandardOrISO(absTime.toString()));
        ThreddsDataFactory tdFactory = new ThreddsDataFactory();
        ThreddsDataFactory.Result result;

        result = tdFactory.openDatatype(invdata, null);

        return (RadialDatasetSweep)result.tds;
    }
    /**
     * Getting URI for a single radar station.
     * @param stnName radar station name
     * @param absTime, is absolute time
     */
    private URI queryRadarStationURIs(String stnName, Date absTime) throws IOException {
        // absTime is a member of  datasetsDateURI
        if(datasetsDateURI == null)
            queryRadarStationURIs(stnName);

        URI ui = (URI)datasetsDateURI.get(absTime);
        if( ui == null ) {
            throw new IOException("Invalid time selected: " + absTime.toString() + "\n");
        }

        return ui;
    }

    /**
     * Getting invDataset list for a single radar station.
     * @param stnName radar station name
     * @param rTime, is relative time, such as latest, 1hour
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
     * @param rTime, is relative time, such as latest, 1hour
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
     * @param rTime, is relative time, such as latest, 1hour
     */
    private ArrayList queryRadarStationURIs(String stnName, String rTime) throws IOException {
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
     */
    private List queryRadarStation(String stnName) throws IOException {

        ArrayList atimeList = new ArrayList();
        // http://motherlode.ucar.edu:9080/thredds/idd/radarLevel2?returns=catalog&stn=KFTG&dtime=latest
        StringBuffer queryb = new StringBuffer();

        queryb.append( dqc.getQuery().getUriResolved().toString());
        queryb.append("serviceType=OPENDAP");
        queryb.append("&stn="+stnName);
        queryb.append("&dtime=all");


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
        List dsets = idata.getDatasets();

        return dsets;
    }

    /**
     * Getting data Iterator for a single radar station.
     * @param stnName radar station name
     */
    private Iterator queryRadarStationDatasets(String stnName) throws IOException {

        List datasetList = new ArrayList();
        List dsets = queryRadarStation(stnName);

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
     * Getting data URI list for a single radar station.
     * @param stnName radar station name
     */
    private ArrayList queryRadarStationURIs(String stnName) throws IOException {
        absTimeList = new ArrayList();
        datasetsDateURI = new HashMap();
        invDatasets = new HashMap();
        ArrayList datasetsURI = new ArrayList();
        List dsets = queryRadarStation(stnName);

        for (int i = 0; i< dsets.size(); i++) {
            InvDataset tdata = (InvDataset)dsets.get(i);
            List acess = tdata.getAccess();
            List dates = tdata.getDates();
            InvAccess ia = (InvAccess)acess.get(0);
            URI d = ia.getStandardUri();
            datasetsURI.add(d);
            absTimeList.add(DateUnit.getStandardOrISO(dates.get(0).toString()));
            try {
                datasetsDateURI.put(DateUnit.getStandardOrISO(dates.get(0).toString()), d);
                invDatasets.put(DateUnit.getStandardOrISO(dates.get(0).toString()), tdata);
            } catch (java.lang.Exception e) {
                throw new IOException( e.getMessage()+"\n");
            }

        }

        return datasetsURI;
    }

    /**
     * Getting data Iterator for a single radar station.
     * @param bufferSize
     */
    public DataIterator getDataIterator(int bufferSize) throws IOException {
        return null;
    }
    /**
     * Getting data relative time list for a single radar station.
     * @param stn radar station name
     */
    private List queryRadarStationRTimes( String stn) throws IOException{
            return selTime.getChoices();
    }

    /**
     * Getting data absolute time list for a single radar station.
     * @param stn radar station name
     */
    private List queryRadarStationABSTimes( String stn) throws IOException{
        if(datasetsDateURI == null)
            queryRadarStationURIs(stn);
        return absTimeList ;
    }

    /**
     * Getting data URIs for a single radar station, with time range.
     * @param s radar station
     * @param start, the start time
     * @param end, the end time
     */
    public List getDataURIs( ucar.nc2.dt.Station s, Date start, Date end) throws IOException {
        if(absTimeList == null)
           queryRadarStationURIs(s.getName());
        ArrayList datasetsURI = new ArrayList();

        for ( Iterator it = absTimeList.iterator(); it.hasNext(); )
        {
           Date d =  (Date)it.next();

           if (d.getTime() >= start.getTime() && d.getTime() <= end.getTime() ) {
                URI ui = (URI)datasetsDateURI.get(d);
                datasetsURI.add(ui);
           }
        }

        return datasetsURI;
    }

    /**
     * Getting data for a single radar station, with time range.
     * @param s radar station
     * @param start, the start time
     * @param end, the end time
     */
    public List getData( ucar.nc2.dt.Station s, Date start, Date end) throws IOException {
        if(absTimeList == null)
           queryRadarStationURIs(s.getName());
        List datasets = new ArrayList();

        for ( Iterator it = absTimeList.iterator(); it.hasNext(); )
        {
           Date d =  (Date)it.next();

           if (d.getTime() >= start.getTime() && d.getTime() <= end.getTime() ) {
               InvDataset ivd = (InvDataset)invDatasets.get(d);
               ThreddsDataFactory tdFactory = new ThreddsDataFactory();
               ThreddsDataFactory.Result result;
               result = tdFactory.openDatatype(ivd, null);
               datasets.add(result.tds);
           }
        }

        return datasets;
    }
    /**
     * Getting data URIs for a single radar station, with time range.
     * @param s radar station
     * @param start, the start time
     * @param end, the end time
     */
    public List getDataURIs( ucar.nc2.dt.Station s, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException {
        if(absTimeList == null)
           queryRadarStationURIs(s.getName());
        ArrayList datasetsURI = new ArrayList();

        for ( Iterator it = absTimeList.iterator(); it.hasNext(); )
        {
           Date d =  (Date)it.next();

           if (d.getTime() >= start.getTime() && d.getTime() <= end.getTime() ) {
               URI ui = (URI)datasetsDateURI.get(d);
               datasetsURI.add(ui);
               if ((cancel != null) && cancel.isCancel()) return null;
           }
        }

        return datasetsURI;
    }

    /**
     * Getting data for a single radar station, with time range.
     * @param s radar station
     * @param start, the start time
     * @param end, the end time
     */

    public List getData( ucar.nc2.dt.Station s, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException {
        if(absTimeList == null)
           queryRadarStationURIs(s.getName());
        ArrayList datasets = new ArrayList();

        for ( Iterator it = absTimeList.iterator(); it.hasNext(); )
        {
           Date d =  (Date)it.next();

           if (d.getTime() >= start.getTime() && d.getTime() <= end.getTime() ) {
               InvDataset ivd = (InvDataset)invDatasets.get(d);
               ThreddsDataFactory tdFactory = new ThreddsDataFactory();
               ThreddsDataFactory.Result result;
               result = tdFactory.openDatatype(ivd, null);
               datasets.add(result.tds);
               if ((cancel != null) && cancel.isCancel()) return null;
           }
        }

        return datasets;
    }

    /**
     * Getting data URI for a single radar station, with time range and interval.
     * @param s radar station
     * @param start, the start time
     * @param end, the end time
     * @param t, the time interval
     * @param tUnit, the time unit, such as "HOUR", "Minute"
     */

    public List getDataURIs( ucar.nc2.dt.Station s, Date start, Date end, int t, String tUnit) throws IOException {
        if(absTimeList == null)
           queryRadarStationURIs(s.getName());
        ArrayList datasetsURI = new ArrayList();
        int size = absTimeList.size();
        long [] tList = new long[size];
        for ( int i = 0; i<  size; i++ )
        {
            Date d = (Date) absTimeList.get(i);
            tList[i] = d.getTime() ;
        }

        Arrays.sort(tList);

        
        for (int i = 0; i < size; i++ )
        {
           long d =  tList[i];

           if (d  >= start.getTime() && d  <= end.getTime() ) {
               if( tUnit.equalsIgnoreCase("HOUR") ) {
                   Calendar cal = Calendar.getInstance();
                   cal.setTime(start);
                   cal.add(Calendar.HOUR, t);
                   Date nextStart = cal.getTime();
                   cal.setTimeInMillis(d);
                   Date dd = cal.getTime();
                   URI ui = (URI)datasetsDateURI.get(dd);
                   datasetsURI.add(ui);
                   start = nextStart;
               }
               else if( tUnit.equalsIgnoreCase("MINUTE") ) {
                   Calendar cal = Calendar.getInstance();
                   cal.setTime(start);
                   cal.add(Calendar.MINUTE, t);
                   Date nextStart = cal.getTime();
                   cal.setTimeInMillis(d);
                   Date dd = cal.getTime();
                   URI ui = (URI)datasetsDateURI.get(dd);
                   datasetsURI.add(ui);
                   start = nextStart;
               }
           }
        }

        return datasetsURI;
    }

  /**
   * Getting data for a single radar station, with time range and interval.
   * @param s radar station
   * @param start, the start time
   * @param end, the end time
   * @param t, the time interval
   * @param tUnit, the time unit, such as "HOUR", "Minute"
   */
    public List getData( ucar.nc2.dt.Station s, Date start, Date end, int t, String tUnit) throws IOException {
        if(absTimeList == null)
           queryRadarStationURIs(s.getName());
        ArrayList datasets = new ArrayList();
        int size = absTimeList.size();
        long [] tList = new long[size];
        for ( int i = 0; i<  size; i++ )
        {
            Date d = (Date) absTimeList.get(i);
            tList[i] = d.getTime() ;
        }

        Arrays.sort(tList);


        for (int i = 0; i < size; i++ )
        {
           long d =  tList[i];

           if (d  >= start.getTime() && d  <= end.getTime() ) {
               if( tUnit.equalsIgnoreCase("HOUR") ) {
                   Calendar cal = Calendar.getInstance();
                   cal.setTime(start);
                   cal.add(Calendar.HOUR, t);
                   Date nextStart = cal.getTime();
                   cal.setTimeInMillis(d);
                   Date dd = cal.getTime();
                   URI ui = (URI)invDatasets.get(dd);
                   datasets.add(ui);
                   start = nextStart;
               }
               else if( tUnit.equalsIgnoreCase("minute") ) {
                   Calendar cal = Calendar.getInstance();
                   cal.setTime(start);
                   cal.add(Calendar.HOUR, t);
                   Date nextStart = cal.getTime();
                   cal.setTimeInMillis(d);
                   Date dd = cal.getTime();
                   URI ui = (URI)invDatasets.get(dd);
                   datasets.add(ui);
                   start = nextStart;
               }
           }
        }

        return datasets;
    }

       
    public static void main(String args[]) throws IOException {
        StringBuffer errlog = new StringBuffer();
        String dqc_location = "http://motherlode.ucar.edu:9080/thredds/idd/radarLevel2";
        DqcStationaryRadarDataset ds = factory("test", dqc_location, errlog);
        System.out.println(" errs= "+errlog);

        List stns = ds.getRadarStations();
        System.out.println(" nstns= "+stns.size());

        List absList;

        DqcRadarStation stn = (DqcRadarStation)(stns.get(2));

        absList = stn.getRadarStationURIs("1hour");
        List ulist = stn.getRadarStationURIs();
        List tlist = stn.getRadarStationTimes();
        int sz = tlist.size();
        Date ts = (Date)tlist.get(1);
        URI stURL = stn.getRadarDatasetURI( ts);
        List dList = ds.getDataURIs((ucar.nc2.dt.Station)stns.get(2), (Date)tlist.get(sz-1), (Date)tlist.get(0), 12, "HOUR");

        List rList = stn.getRadarStationTimes((Date)tlist.get(3), (Date)tlist.get(0));
        Iterator it = stn.getRadarStationDatasets("1hour");

        List data = ds.getData((DqcRadarStation)stns.get(2), (Date)tlist.get(6), (Date)tlist.get(0));
        Iterator it1 = data.iterator();
        while(it1.hasNext()) {
            RadialDatasetSweep rds = (RadialDatasetSweep)it1.next();
            assert null != rds;
        }
     }
}
