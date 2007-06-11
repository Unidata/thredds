package ucar.nc2.thredds;

import ucar.nc2.dt.radial.StationRadarCollectionImpl;
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.DateSelectionInfo;
import ucar.unidata.util.Product;
import ucar.unidata.util.DateSelection;
import ucar.unidata.util.DatedThing;

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
public class DqcRadarDatasetCollection extends StationRadarCollectionImpl {

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
      //  DqcRadarStation dd = new DqcRadarStation(station);
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

    public boolean checkStationProduct(String sName, Product product){
        if(dqc.getName().contains("Level2")) {
            if( product.getID().equals("Reflectivity") ||
                product.getID().equals("RadialVelocity") ||
                product.getID().equals("SpectrumWidth")  )
             return true;
        }
        return false;
    }

    public boolean checkStationProduct(Product product) {
         return checkStationProduct(null, product);
    }

    public int getStationProductCount( String sName) {
        if(dqc.getName().contains("Level2")) {
             return 3;
        }
        return 0;
    }
  /** get all radar station.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public List getStations() throws IOException {
        return getRadarStations( );
    }

   /** get all radar station.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public List getRadarStations() {
        List sl = selStation.getStations();
        ArrayList dsl = new ArrayList();

        for ( Iterator it = sl.iterator(); it.hasNext(); )
        {
           Station s = (Station)it.next();
           dsl.add(s);
        }
        return dsl;
    }

 /** get all radar station within box.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public List getStations(ucar.nc2.util.CancelTask cancel) throws IOException {
        return getStations(null, cancel);
    }

 /** get all radar station within box.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public List getStations(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
        return getStations(boundingBox, null);
    }

 /** get all radar station within box.
   * @return List of type DqcRadarStation objects
   * @throws IOException java io exception
   * */
    public List getStations(ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException {
        List sl = selStation.getStations();
        ArrayList dsl = new ArrayList();

        for ( Iterator it = sl.iterator(); it.hasNext(); )
        {
           Station s =  (Station)it.next();
           LatLonPointImpl latlonPt = new LatLonPointImpl();
           latlonPt.set( s.getLocation().getLatitude(), s.getLocation().getLongitude());
           if (boundingBox.contains( latlonPt)) {
                dsl.add(s);
           }
           if ((cancel != null) && cancel.isCancel()) return null;
        }

        return dsl;
    }


    /**
     * Getting dataset for a single radar station.
     * @param stnName radar station name
     * @param absTime is absolute time
     * @return RadialDatasetSweep object
     * @throws IOException java io exception
     */
    public RadialDatasetSweep getRadarDataset(String stnName, Date absTime) throws IOException {
        // absTime is a member of  datasetsDateURI
        InvDataset invdata = queryRadarStation(stnName, absTime);

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
    public URI getRadarDatasetURI(String stnName, Date absTime) throws IOException {
        // absTime is a member of  datasetsDateURI
        InvDataset invdata =  queryRadarStation(stnName, absTime);
      /*  List dsets = idata.getDatasets();
        int siz = dsets.size();
        if(siz != 1)
            return null;

        InvDataset invdata = (InvDataset)dsets.get(0);     */
        List acess = invdata.getAccess();
        InvAccess ia = (InvAccess)acess.get(0);
        URI ui = ia.getStandardUri();

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
        ArrayList dsets = (ArrayList)idata.getDatasets();

        InvDataset tdata = (InvDataset)dsets.get(0);
        return tdata;
    }


    /**
     * Getting invDataset list for a single radar station.
     * @param stnName radar station name
     * @param start of the time
     * @param end of the time
     * @return list of invDataset
     * @throws IOException java io exception
     */
    private DqcRadarDatasetInfo queryRadarStation(String stnName, Date start, Date end) throws IOException {
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

        ArrayList absTimeList = new ArrayList();
        ArrayList dURIList = new ArrayList();
        ArrayList dInvList = new ArrayList();

        for (int i = 0; i< dsets.size(); i++) {
            InvDataset tdata = (InvDataset)dsets.get(i);
            List acess = tdata.getAccess();
            List dates = tdata.getDates();
            InvAccess ia = (InvAccess)acess.get(0);
            URI d = ia.getStandardUri();

            absTimeList.add(dates.get(0).toString());
            dURIList.add(new DatasetURIInfo(d, dates.get(0).toString()));
            dInvList.add(new InvDatasetInfo(tdata, dates.get(0).toString()));
        }

        DqcRadarDatasetInfo dri = new DqcRadarDatasetInfo(absTimeList, dURIList, dInvList);

        return dri;
    }

    /**
     * Getting data for a single radar station.
     * @param stnName radar station name
     * @param start of the time
     * @param end of the time
     * @return data URI list
     * @throws IOException java io exception
     */
     public ArrayList getRadarStationURIs(String stnName, Date start, Date end ) throws IOException {

        DqcRadarDatasetInfo dri = queryRadarStation(stnName, start,  end );
        ArrayList uList = dri.getURIList();

        int size = uList.size();
        ArrayList datasetsURI =  new ArrayList();

        for(int i = 0; i < size; i++) {
            DatasetURIInfo du = (DatasetURIInfo)uList.get(i);
            datasetsURI.add(du.uri);
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
    public ArrayList getRadarStationDatasets(String stnName, Date start, Date end) throws IOException {

        ArrayList datasetList = new ArrayList();

        DqcRadarDatasetInfo dri = queryRadarStation(stnName, start, end);
        ArrayList iList = dri.getInvList();      
        int size = iList.size();

        for (int i = 0; i< size; i++) {
            InvDatasetInfo iv = (InvDatasetInfo)iList.get(i);
            InvDataset tdata = iv.inv;
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
    public ArrayList getRadarStationTimes(String stnName,Date start, Date end ) throws IOException {

        DqcRadarDatasetInfo dri = queryRadarStation(stnName, start, end);
        return dri.absTimeList;
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
     * @param dateInfo the date selection information
     * @return list of URIs
     */
    public ArrayList getDataURIs( String sName, DateSelection dateInfo) throws IOException {

        return getDataURIs( sName, dateInfo, null);
    }

    /**
     * Getting data for a single radar station, with time range.
     * @param sName radar station name
     * @param dateInfo the date time selection information
     * @return list of radialDatasetSweep
     */

    public ArrayList getData( String sName, DateSelection dateInfo) throws IOException {

        return getData( sName, dateInfo, null);
    }

    /**
     * Getting data for a single radar station, with time range.
     * @param sName radar station name
     * @param dateSelect the date time selection information
     * @return list of radialDatasetSweep
     */

    public ArrayList getData( String sName, DateSelection dateSelect, ucar.nc2.util.CancelTask cancel) throws IOException {
        if ((cancel != null) && cancel.isCancel())
            return null;
        DqcRadarDatasetInfo dri = queryRadarStation(sName, dateSelect.getStartFixedDate(), dateSelect.getEndFixedDate());
        ArrayList datasetList = new ArrayList();

        List datasetINVs = dateSelect.apply(dri.getInvList()) ;

        Iterator it = datasetINVs.iterator();
        while(it.hasNext()) {
            InvDatasetInfo ifo = (InvDatasetInfo)it.next();
            InvDataset tdata  = ifo.inv;
            ThreddsDataFactory tdFactory = new ThreddsDataFactory();
            ThreddsDataFactory.Result result;
            result = tdFactory.openDatatype(tdata, null);
            datasetList.add(result.tds);
            if ((cancel != null) && cancel.isCancel())
                return null;
        }

        return datasetList;

    }



    public ArrayList getDataURIs( String sName, DateSelection dateSelect, ucar.nc2.util.CancelTask cancel) throws IOException {
        if ((cancel != null) && cancel.isCancel())
                        return null;
        DqcRadarDatasetInfo dri = queryRadarStation(sName, dateSelect.getStartFixedDate(), dateSelect.getEndFixedDate());

        // create a list to hold URIs
        List datasetsURIs = dateSelect.apply(dri.getURIList()) ;
        ArrayList uriList = new ArrayList();

        Iterator it = datasetsURIs.iterator();
        while(it.hasNext()) {
            DatasetURIInfo ufo = (DatasetURIInfo)it.next();
            URI u  = ufo.uri;
            uriList.add(u);
            if ((cancel != null) && cancel.isCancel())
                return null;
        }


        return uriList;
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


    public class DatasetURIInfo implements DatedThing {
        private URI uri = null;
        private String t = null;
        private Date date = null;
        
        public DatasetURIInfo(URI u, String t) {
            this.uri = u;
            this.t = t;
        }

        public Date getDate() {
            if( date == null)
                this.date = DateUnit.getStandardOrISO((String)this.t);
            return  date;
        }

    }


    public class InvDatasetInfo implements DatedThing {
        private InvDataset inv = null;
        private String t = null;
        private Date date = null;

        public InvDatasetInfo(InvDataset u, String t) {
            this.inv = u;
            this.t = t;
        }

        public Date getDate() {
            if( date == null)
                this.date = DateUnit.getStandardOrISO((String)this.t);
            return  date;
        }

    }


    public class DqcRadarDatasetInfo {
        private ArrayList absTimeList;
        private ArrayList datasetInfoList;
        private ArrayList invDatasetList;

        public DqcRadarDatasetInfo( ) {}

        public DqcRadarDatasetInfo(ArrayList absTimeList, ArrayList datasetInfoList, ArrayList invDatasetList) {
          this.absTimeList =  absTimeList;
          this.datasetInfoList = datasetInfoList;
          this.invDatasetList = invDatasetList;
        }

        public ArrayList getTimeList() {
          return this.absTimeList;
        }

        public ArrayList getURIList() {
          return this.datasetInfoList;
        }

        public ArrayList getInvList() {
            return this.invDatasetList;
        }


    }

    public static void main(String args[]) throws IOException {
        StringBuffer errlog = new StringBuffer();
        String dqc_location = "http://motherlode.ucar.edu:8080/thredds/idd/radarLevel2";
        DqcRadarDatasetCollection ds = factory("test", dqc_location, errlog);
        System.out.println(" errs= "+errlog);

        List stns = ds.getStations();
        System.out.println(" nstns= "+stns.size());

        Station stn = (Station)(stns.get(2));

       // List ulist = stn.getRadarStationURIs();
       // assert null != ulist;
        Date ts1 = DateUnit.getStandardOrISO("2007-06-9T12:12:00");
        Date ts2 = DateUnit.getStandardOrISO("2007-06-9T23:12:00");

        List tlist = ds.getRadarStationTimes(stn.getValue(), ts1, ts2);
        int sz = tlist.size();
        Date ts0 = DateUnit.getStandardOrISO((String)tlist.get(1));
        RadialDatasetSweep rds = ds.getRadarDataset(stn.getValue(), ts0);
        URI stURL = ds.getRadarDatasetURI(stn.getValue(), ts0);
        assert null != stURL;
        assert 0 != sz;
        DateSelection dateS = new DateSelection(ts1, ts2);
        dateS.setInterval((double)3600*1000);
        dateS.setRoundTo((double)3600*1000);
        dateS.setPreRange((double)500*1000);
        dateS.setPostRange((double)500*1000);

        List jList = ds.getDataURIs("KABX", dateS );

        assert null != jList;
        List mList = ds.getData("KABX", dateS, null);
        assert null != mList;



        //Date ts0 = DateFromString.getDateUsingCompleteDateFormat((String)tlist.get(1),"yyyy-MM-dd'T'HH:mm:ss");
        Date ts = DateUnit.getStandardOrISO((String)tlist.get(1));
        java.text.SimpleDateFormat isoDateTimeFormat;
        isoDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        String st = isoDateTimeFormat.format(ts);

  
     }

}
