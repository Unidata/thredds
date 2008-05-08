package ucar.nc2.thredds;


import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import thredds.catalog.*;
import ucar.nc2.dt.DataIterator;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dt.StationImpl;
import ucar.nc2.dt.radial.StationRadarCollectionImpl;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.DateSelection;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.DatedThing;
import ucar.unidata.util.Product;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Feb 6, 2008
 * Time: 2:38:57 PM
 * To change this template use File | Settings | File Templates.
 */



/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Jan 8, 2008
 * Time: 10:55:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class TDSRadarDatasetCollection extends StationRadarCollectionImpl {

    //////////////////////////////////////////////////////////////////////////////////

    // private InvDataset ds;


    /** _more_ */
    protected HashMap stationHMap;

    /** _more_ */
    private List radarTimeSpan;

    /** _more_ */
    private LatLonRect radarRegion;

    /** _more_ */
    private String dsc_location;

    /** _more_ */
    private List radarProducts;

    /** _more_ */
    private String summary;

    /** _more_          */
    protected static final Namespace defNS =
        Namespace.getNamespace(
            thredds.catalog.XMLEntityResolver.DQC_NAMESPACE_04);


    /** _more_          */
    private URI docURI;

    /**
     * tds radar dataset collection  factory
     *
     * @param ds _more_
     * @param dsc_location _more_
     * @param errlog _more_
     *
     * @return any foctory
     *
     * @throws java.io.IOException _more_
     *
     * @throws IOException _more_
     * @throws java.net.URISyntaxException _more_
     */

    static public TDSRadarDatasetCollection factory(InvDataset ds,
            String dsc_location,
            StringBuffer errlog) throws IOException,
                                        java.net.URISyntaxException {

        // URI catalogURI = new URI(dsc_location);
        //this.docURI =    catalogURI;
        return factory(ds.getDocumentation("summary"), dsc_location, errlog);
    }

    /**
     *  tds radar dataset collection  factory
     *
     * @param desc _more_
     * @param dsc_location _more_
     * @param errlog _more_
     *
     * @return dataset collection
     *
     * @throws IOException _more_
     */
    static public TDSRadarDatasetCollection factory(String desc,
            String dsc_location, StringBuffer errlog) throws IOException {
        // super();
        SAXBuilder        builder;
        Document          doc  = null;
        XMLEntityResolver jaxp = new XMLEntityResolver(true);
        builder = jaxp.getSAXBuilder();

        try {
            doc = builder.build(dsc_location);
        } catch (JDOMException e) {
            errlog.append(e.toString());
        }

        Element   qcElem = doc.getRootElement();
        Namespace ns     = qcElem.getNamespace();


        return new TDSRadarDatasetCollection(desc, dsc_location, qcElem, ns,
                                             errlog);
    }

    /**
     * initiate a new TDS radar dataset collection object
     *
     * @param desc description
     * @param dsc_location location of dataset
     * @param elem dataset root element
     * @param ns dataset namespace
     * @param errlog error log
     *
     * @throws IOException _more_
     */
    private TDSRadarDatasetCollection(
            String desc, String dsc_location, Element elem, Namespace ns,
            StringBuffer errlog) throws IOException {
        super();
        Element serviceElem = readElements(elem, "service");
        Element dsElem      = readElements(elem, "dataset");

        Element metaElem    = readElements(dsElem, "metadata");
        // HashMap stationHMap = readSelectStations(metaElem, ns);
        String sts = dsc_location.replaceFirst("dataset.xml", "stations.xml");
        HashMap    stationHMap   = readRadarStations(sts);
        LatLonRect radarRegion   = readSelectRegion(metaElem, ns);
        List       radarTimeSpan = readSelectTime(metaElem, ns);
        List       productList   = readSelectVariable(metaElem, ns);
        String     summary       = readSelectDocument(metaElem, ns);

        // gotta have these
        if (stationHMap == null) {
            errlog.append(
                "TDSRadarDatasetCollection must have station selected");
            return;
        }
        if (radarRegion == null) {
            errlog.append(
                "TDSRadarDatasetCollection must have region selected");
            return;
        }
        if (radarTimeSpan == null) {
            errlog.append(
                "TDSRadarDatasetCollection must have time span selected");
            return;
        }

        this.desc          = desc;
        this.dsc_location  = dsc_location;
        this.radarProducts = productList;
        this.summary       = summary;
        this.stationHMap   = stationHMap;
        this.radarRegion   = radarRegion;
        this.radarTimeSpan = radarTimeSpan;
        this.startDate =
            DateUnit.getStandardOrISO((String) radarTimeSpan.get(0));
        this.endDate =
            DateUnit.getStandardOrISO((String) radarTimeSpan.get(0));

        try {
            timeUnit = new DateUnit("hours since 1991-01-01T00:00");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * retrieve all radar stations in this dataset collection
     *
     *
     * @param stsXML_location _more_
     * @return station hashmap
     *
     * @throws IOException _more_
     */
    public HashMap readRadarStations(
            String stsXML_location) throws IOException {
        SAXBuilder        builder;
        Document          doc  = null;
        XMLEntityResolver jaxp = new XMLEntityResolver(true);
        builder = jaxp.getSAXBuilder();
        HashMap stations = new HashMap();

        try {
            doc = builder.build(stsXML_location);
        } catch (JDOMException e) {
            e.printStackTrace();
        }

        Element        rootElem = doc.getRootElement();
        java.util.List children = rootElem.getChildren();
        for (int j = 0; j < children.size(); j++) {
            StationImpl s;
            if (null != (s = readStation((Element) children.get(j)))) {
                stations.put(s.getName(), s);
            }
        }

        return stations;
    }

    /**
     * get named element from parent element
     *
     *
     * @param elem _more_
     * @param eleName _more_
     * @return _more_
     */
    public Element readElements(Element elem, String eleName) {
        java.util.List children = elem.getChildren();
        for (int j = 0; j < children.size(); j++) {
            Element child     = (Element) children.get(j);
            String  childName = child.getName();
            if (childName.equals(eleName)) {
                return child;
            }
        }
        return null;
    }

    /**
     * get station object from parent element
     *
     *
     * @param elem _more_
     * @return stationImpl
     */
    private StationImpl readStation(Element elem) {
        // look for stations
        String name = elem.getAttributeValue("id");
        //latitude
        Element desc  = elem.getChild("name");
        String  descv = desc.getValue();
        Element lat   = elem.getChild("latitude");
        String  latv  = lat.getValue();
        Element lon   = elem.getChild("longitude");
        String  lonv  = lon.getValue();
        Element alt   = elem.getChild("elevation");
        String  altv  = alt.getValue();

        StationImpl station = new StationImpl(name, descv,
                                  Double.valueOf(latv), Double.valueOf(lonv),
                                  Double.valueOf(altv));

        return station;
    }

    /**
     * get region from parent element
     *
     *
     * @param elem _more_
     * @param ns _more_
     * @return _more_
     */
    public LatLonRect readSelectRegion(Element elem, Namespace ns) {
        Element region = elem.getChild("LatLonBox", ns);
        //lat, lon
        Element north = region.getChild("north", ns);
        String  nv    = north.getValue();
        Element south = region.getChild("south", ns);
        String  sv    = south.getValue();
        Element east  = region.getChild("east", ns);
        String  ev    = east.getValue();
        Element west  = region.getChild("west", ns);
        String  wv    = west.getValue();

        LatLonPointImpl p1 = new LatLonPointImpl(Double.valueOf(sv),
                                 Double.valueOf(wv));
        LatLonPointImpl p2 = new LatLonPointImpl(Double.valueOf(nv),
                                 Double.valueOf(ev));
        LatLonRect llr = new LatLonRect(p1, p2);


        return llr;
    }

    /**
     * get start and end elemnt form parent element
     *
     *
     * @param elem _more_
     * @param ns _more_
     * @return list of times
     */
    public List readSelectTime(Element elem, Namespace ns) {
        // look for stations

        Element        region     = elem.getChild("TimeSpan", ns);

        java.util.List regionInfo = region.getChildren();
        //lat, lon
        Element start = region.getChild("start", ns);
        String  sv    = start.getValue();
        Element end   = region.getChild("end", ns);
        String  ev    = end.getValue();

        List    ll    = new ArrayList();
        ll.add(sv);
        ll.add(ev);
        return ll;
    }

    /**
     * get variable list from parent element
     *
     *
     * @param elem _more_
     * @param ns _more_
     * @return list of varibles
     */
    public List readSelectVariable(Element elem, Namespace ns) {
        // look for stations
        List           varlist = new ArrayList();
        Element        v       = elem.getChild("Variables", ns);

        java.util.List varInfo = v.getChildren();
        //lat, lon
        for (int j = 0; j < varInfo.size(); j++) {
            Product s;
            Element p  = (Element) varInfo.get(j);
            String  id = p.getAttributeValue("name");
            if (id.contains("/")) {
                String c[] = id.split("/");
                s = new Product(c[0], c[1]);
                varlist.add(s);

            } else {
                String name = p.getAttributeValue("vocabulary_name");
                s = new Product(id, name);
                varlist.add(s);
            }
        }



        return varlist;
    }

    /**
     * get document from parent element
     *
     *
     * @param elem _more_
     * @param ns _more_
     * @return _more_
     */
    private String readSelectDocument(Element elem, Namespace ns) {

        Element doc = elem.getChild("documentation", ns);
        return doc.getValue();

    }


    /**
     * _more_
     */
    protected void setTimeUnits() {}

    /**
     * _more_
     */
    protected void setStartDate() {}

    /**
     * _more_
     */
    protected void setEndDate() {}

    /**
     * _more_
     */
    protected void setBoundingBox() {}

    /**
     * get title of dataset collection
     *
     * @return _more_
     */
    public String getTitle() {
        return this.summary;
    }

    /**
     * get dataset collection URI
     *
     * @return _more_
     */
    public String getLocationURI() {
        return this.dsc_location;
    }

    /**
     * get dataset collection description
     *
     * @return _more_
     */
    public String getDescription() {
        return desc;
    }

    /**
     * get bounding box of dataset collection
     *
     * @return _more_
     */
    public LatLonRect getRadarsBoundingBox() {
        return radarRegion;
    }

    /**
     * get start and end time of dataset collection
     *
     * @return _more_
     */
    public List getRadarTimeSpan() {
        return this.radarTimeSpan;
    }

    /**
     * get products of dataset collection
     *
     * @return _more_
     */
    public List getRadarProducts() {
        return this.radarProducts;
    }

    /**
     * check the exist of a product in this dataset collection
     *
     * @param sName _more_
     * @param product _more_
     *
     * @return _more_
     */
    public boolean checkStationProduct(String sName, Product product) {

        for (int j = 0; j < radarProducts.size(); j++) {
            Product s = (Product) radarProducts.get(j);
            if (s.equals(product)) {
                return true;
            }
        }
        return false;
    }

    /**
     * check the exist of a product in this dataset collection
     *
     * @param product _more_
     *
     * @return _more_
     */
    public boolean checkStationProduct(Product product) {
        return checkStationProduct(null, product);
    }

    /**
     *get the number of products
     *
     * @param sName _more_
     *
     * @return _more_
     */
    public int getStationProductCount(String sName) {
        return radarProducts.size();
    }

    /**
     * get all radar station.
     * @return List of type DqcRadarStation objects
     * @throws IOException java io exception
     */
    public List getStations() throws IOException {
        return getRadarStations();
    }

    /**
     * get all radar station.
     * @return List of type DqcRadarStation objects
     * @throws IOException java io exception
     */
    public List getRadarStations() {
        ArrayList slist = new ArrayList();
        Iterator  it    = this.stationHMap.values().iterator();
        while (it.hasNext()) {
            slist.add(it.next());
        }

        return slist;
    }

    /**
     * get one radar station.
     *
     * @param sName _more_
     * @return StationImpl object
     * @throws
     */
    public StationImpl getRadarStation(String sName) {

        return (StationImpl) this.stationHMap.get(sName);

    }

    /**
     * get all radar station within box.
     *
     * @param cancel _more_
     *  @return List of type DqcRadarStation objects
     *  @throws IOException java io exception
     */
    public List getStations(
            ucar.nc2.util.CancelTask cancel) throws IOException {
        return getStations(null, cancel);
    }

    /**
     * get all radar station within box.
     *
     * @param boundingBox _more_
     *  @return List of type DqcRadarStation objects
     *  @throws IOException java io exception
     */
    public List getStations(
            ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
        return getStations(boundingBox, null);
    }

    /**
     * get all radar station within box.
     *
     * @param boundingBox _more_
     * @param cancel _more_
     *  @return List of type DqcRadarStation objects
     *  @throws IOException java io exception
     */
    public List getStations(
            ucar.unidata.geoloc.LatLonRect boundingBox,
            ucar.nc2.util.CancelTask cancel) throws IOException {
        List      sl  = (List) stationHMap.values();
        ArrayList dsl = new ArrayList();

        if ( !boundingBox.containedIn(radarRegion)) {
            return null;
        }

        for (Iterator it = sl.iterator(); it.hasNext(); ) {
            StationImpl     s        = (StationImpl) it.next();
            LatLonPointImpl latlonPt = new LatLonPointImpl();
            latlonPt.set(s.getLatitude(), s.getLongitude());
            if (boundingBox.contains(latlonPt)) {
                dsl.add(s);
            }
            if ((cancel != null) && cancel.isCancel()) {
                return null;
            }
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
    public RadialDatasetSweep getRadarDataset(String stnName,
            Date absTime) throws IOException {
        // absTime is a member of  datasetsDateURI
        // long start = System.currentTimeMillis();
        InvDataset invdata = queryRadarStation(stnName, absTime);
        // double took =  0.001* (System.currentTimeMillis() - start);
        //    System.out.println("it took1 =" + took);
        if (invdata == null) {
            throw new IOException("Invalid time selected: "
                                  + absTime.toString() + "\n");
        }

        ThreddsDataFactory        tdFactory = new ThreddsDataFactory();
        ThreddsDataFactory.Result result;
        //start = System.currentTimeMillis();
        result = tdFactory.openDatatype(invdata, null);
        // took =  0.001* (System.currentTimeMillis() - start);
        //    System.out.println("it took2 =" + took);
        return (RadialDatasetSweep) result.tds;
    }

    /**
     * Getting dataset for a single radar station.
     * @param stnName radar station name
     * @param productID _more_
     * @param absTime is absolute time
     * @return RadialDatasetSweep object
     * @throws IOException java io exception
     */
    public RadialDatasetSweep getRadarDataset(String stnName,
            String productID, Date absTime) throws IOException {
        // absTime is a member of  datasetsDateURI
        // long start = System.currentTimeMillis();
        InvDataset invdata = queryRadarStation(stnName, productID, absTime);
        // double took =  0.001* (System.currentTimeMillis() - start);
        //    System.out.println("it took1 =" + took);
        if (invdata == null) {
            throw new IOException("Invalid time selected: "
                                  + absTime.toString() + "\n");
        }

        ThreddsDataFactory        tdFactory = new ThreddsDataFactory();
        ThreddsDataFactory.Result result;
        //start = System.currentTimeMillis();
        result = tdFactory.openDatatype(invdata, null);
        // took =  0.001* (System.currentTimeMillis() - start);
        //    System.out.println("it took2 =" + took);
        return (RadialDatasetSweep) result.tds;
    }

    /**
     * Getting URI for a single radar station.
     * @param stnName radar station name
     * @param absTime is absolute time
     * @return URI
     * @throws IOException  java io exception
     */
    public URI getRadarDatasetURI(String stnName,
                                  Date absTime) throws IOException {
        // absTime is a member of  datasetsDateURI
        InvDataset invdata = queryRadarStation(stnName, absTime);
        /*  List dsets = idata.getDatasets();
          int siz = dsets.size();
          if(siz != 1)
              return null;

          InvDataset invdata = (InvDataset)dsets.get(0);     */
        List      acess = invdata.getAccess();
        InvAccess ia    = (InvAccess) acess.get(0);
        URI       ui    = ia.getStandardUri();

        if (ui == null) {
            throw new IOException("Invalid time selected: "
                                  + absTime.toString() + "\n");
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
    private InvDataset queryRadarStation(String stnName,
                                         Date absTime) throws IOException {
        return queryRadarStation(stnName, (String) null, absTime);
    }

    /**
     * Getting URI for a single radar station.
     * @param stnName radar station name
     * @param productID _more_
     * @param absTime is absolute time
     * @return InvDataset
     * @throws IOException  java io exception
     */
    private InvDataset queryRadarStation(String stnName, String productID,
                                         Date absTime) throws IOException {
        String stime = DateUtil.getTimeAsISO8601(absTime).replaceAll("GMT",
                           "");
        // construct a query like this:
        // http://motherlode.ucar.edu:9080/thredds/idd/radarLevel2?returns=catalog&stn=KFTG&dtime=latest
        StringBuffer queryb  = new StringBuffer();
        String       baseURI = dsc_location.replaceFirst("/dataset.xml", "?");
        queryb.append(baseURI);
        queryb.append("&stn=" + stnName);
        if (productID != null) {
            queryb.append("&var=" + productID);
        }
        if (absTime == null) {
            queryb.append("&time=present");
        } else {

            queryb.append("&time=" + stime);
        }


        URI catalogURI;

        try {
            catalogURI = new URI(queryb.toString());
        } catch (java.net.URISyntaxException e) {
            throw new IOException("** MalformedURLException on URL <" + ">\n"
                                  + e.getMessage() + "\n");
        }

        InvCatalogFactory factory = new InvCatalogFactory("default", false);

        InvCatalogImpl catalog = (InvCatalogImpl) factory.readXML(catalogURI);

            StringBuilder      buff    = new StringBuilder();
        if ( !catalog.check(buff)) {
            throw new IOException("Invalid catalog <" + catalogURI + ">\n"
                                  + buff.toString());
        }
        catalog.writeXML(System.out);  // debugg

        List       datasets = catalog.getDatasets();

        InvDataset idata    = (InvDataset) datasets.get(0);
        ArrayList  dsets    = (ArrayList) idata.getDatasets();

        InvDataset tdata    = (InvDataset) dsets.get(0);
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
    private TDSRadarDatasetInfo queryRadarStation(String stnName, Date start,
            Date end) throws IOException {
        return queryRadarStation(stnName, null, start, end);
    }

    /**
     * Getting invDataset list for a single radar station.
     * @param stnName radar station name
     * @param productID _more_
     * @param start of the time
     * @param end of the time
     * @return list of invDataset
     * @throws IOException java io exception
     */
    private TDSRadarDatasetInfo queryRadarStation(String stnName,
            String productID, Date start, Date end) throws IOException {
        // http://motherlode.ucar.edu:9080/thredds/idd/radarLevel2?returns=catalog&stn=KFTG&dtime=latest
        StringBuffer queryb  = new StringBuffer();
        String       baseURI = dsc_location.replaceFirst("/dataset.xml", "?");
        queryb.append(baseURI);
        queryb.append("&stn=" + stnName);
        if (productID != null) {
            queryb.append("&var=" + productID);
        }
        if ((start == null) && (end == null)) {
            queryb.append("&time=present");
        } else if (end == null) {
            String stime = DateUtil.getTimeAsISO8601(start).replaceAll("GMT",
                               "");
            queryb.append("&time_start=" + stime);
            queryb.append("&time_end=present");
        } else {
            String stime = DateUtil.getTimeAsISO8601(start).replaceAll("GMT",
                               "");
            String etime = DateUtil.getTimeAsISO8601(end).replaceAll("GMT",
                               "");
            queryb.append("&time_start=" + stime);
            queryb.append("&time_end=" + etime);
        }


        URI catalogURI;
        try {
            catalogURI = new URI(queryb.toString());
        } catch (java.net.URISyntaxException e) {
            throw new IOException("** MalformedURLException on URL <" + ">\n"
                                  + e.getMessage() + "\n");
        }

        InvCatalogFactory factory = new InvCatalogFactory("default", false);

        InvCatalogImpl catalog = (InvCatalogImpl) factory.readXML(catalogURI);
            StringBuilder      buff    = new StringBuilder();
        if ( !catalog.check(buff)) {
            throw new IOException("Invalid catalog <" + catalogURI + ">\n"
                                  + buff.toString());
        }

        List       datasets = catalog.getDatasets();

        InvDataset idata    = (InvDataset) datasets.get(0);
        //    List ddate = idata.getDates();

        ArrayList dsets       = (ArrayList) idata.getDatasets();

        ArrayList absTimeList = new ArrayList();
        ArrayList dURIList    = new ArrayList();
        ArrayList dInvList    = new ArrayList();

        for (int i = 0; i < dsets.size(); i++) {
            InvDataset tdata = (InvDataset) dsets.get(i);
            List       acess = tdata.getAccess();
            List       dates = tdata.getDates();
            InvAccess  ia    = (InvAccess) acess.get(0);
            URI        d     = ia.getStandardUri();
            Date date = DateUnit.getStandardOrISO(dates.get(0).toString());
            absTimeList.add(date);
            dURIList.add(new DatasetURIInfo(d, date));
            dInvList.add(new InvDatasetInfo(tdata, date));
        }

        TDSRadarDatasetInfo dri = new TDSRadarDatasetInfo(absTimeList,
                                      dURIList, dInvList);

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
    public ArrayList getRadarStationURIs(String stnName, Date start,
                                         Date end) throws IOException {

        TDSRadarDatasetInfo dri = queryRadarStation(stnName, start, end);
        ArrayList           uList       = dri.getURIList();

        int                 size        = uList.size();
        ArrayList           datasetsURI = new ArrayList();

        for (int i = 0; i < size; i++) {
            DatasetURIInfo du = (DatasetURIInfo) uList.get(i);
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
    public ArrayList getRadarStationDatasets(String stnName, Date start,
                                             Date end) throws IOException {

        ArrayList           datasetList = new ArrayList();

        TDSRadarDatasetInfo dri = queryRadarStation(stnName, start, end);
        ArrayList           iList       = dri.getInvList();
        int                 size        = iList.size();

        for (int i = 0; i < size; i++) {
            InvDatasetInfo            iv = (InvDatasetInfo) iList.get(i);
            InvDataset                tdata     = iv.inv;
            ThreddsDataFactory        tdFactory = new ThreddsDataFactory();
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
    public ArrayList getRadarStationTimes(String stnName, Date start,
                                          Date end) throws IOException {

        return getRadarStationTimes(stnName, null, start, end);

    }

    /**
     * Getting data URI list for a single radar station.
     * @param stnName radar station name
     * @param productID _more_
     * @param start of the time
     * @param end of the time
     * @return list of URIs
     * @throws IOException java io exception
     */
    public ArrayList getRadarStationTimes(String stnName, String productID,
                                          Date start,
                                          Date end) throws IOException {

        TDSRadarDatasetInfo dri = queryRadarStation(stnName, productID,
                                      start, end);
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
    private List queryRadarStationRTimes(String stn) throws IOException {
        return radarTimeSpan;
    }



    /**
     * Getting data URIs for a single radar station, with time range.
     * @param sName radar station name
     * @param dateInfo the date selection information
     * @return list of URIs
     *
     * @throws IOException _more_
     */
    public ArrayList getDataURIs(String sName,
                                 DateSelection dateInfo) throws IOException {

        return getDataURIs(sName, dateInfo, null);
    }

    /**
     * Getting data for a single radar station, with time range.
     * @param sName radar station name
     * @param dateInfo the date time selection information
     * @return list of radialDatasetSweep
     *
     * @throws IOException _more_
     */

    public ArrayList getData(String sName,
                             DateSelection dateInfo) throws IOException {

        return getData(sName, dateInfo, null);
    }

    /**
     * Getting data for a single radar station, with time range.
     * @param sName radar station name
     * @param dateSelect the date time selection information
     * @param cancel _more_
     * @return list of radialDatasetSweep
     *
     * @throws IOException _more_
     */

    public ArrayList getData(
            String sName, DateSelection dateSelect,
            ucar.nc2.util.CancelTask cancel) throws IOException {
        if ((cancel != null) && cancel.isCancel()) {
            return null;
        }
        TDSRadarDatasetInfo dri = queryRadarStation(sName,
                                      dateSelect.getStartFixedDate(),
                                      dateSelect.getEndFixedDate());
        ArrayList datasetList = new ArrayList();

        List      datasetINVs = dateSelect.apply(dri.getInvList());

        Iterator  it          = datasetINVs.iterator();
        while (it.hasNext()) {
            InvDatasetInfo            ifo       = (InvDatasetInfo) it.next();
            InvDataset                tdata     = ifo.inv;
            ThreddsDataFactory        tdFactory = new ThreddsDataFactory();
            ThreddsDataFactory.Result result;
            result = tdFactory.openDatatype(tdata, null);
            datasetList.add(result.tds);
            if ((cancel != null) && cancel.isCancel()) {
                return null;
            }
        }

        return datasetList;

    }



    /**
     * getting data uri list
     *
     * @param sName _more_
     * @param dateSelect _more_
     * @param cancel _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public ArrayList getDataURIs(
            String sName, DateSelection dateSelect,
            ucar.nc2.util.CancelTask cancel) throws IOException {
        if ((cancel != null) && cancel.isCancel()) {
            return null;
        }
        TDSRadarDatasetInfo dri = queryRadarStation(sName,
                                      dateSelect.getStartFixedDate(),
                                      dateSelect.getEndFixedDate());

        // create a list to hold URIs
        List      datasetsURIs = dateSelect.apply(dri.getURIList());
        ArrayList uriList      = new ArrayList();

        Iterator  it           = datasetsURIs.iterator();
        while (it.hasNext()) {
            DatasetURIInfo ufo = (DatasetURIInfo) it.next();
            URI            u   = ufo.uri;
            uriList.add(u);
            if ((cancel != null) && cancel.isCancel()) {
                return null;
            }
        }


        return uriList;
    }


    /**
     * Getting data for a single radar station, with time range and interval.
     * @param roundTo
     * @param seconds to be round to
     * @return round to second
     */
    public static long roundTo(long roundTo, long seconds) {
        int roundToSeconds = (int) (roundTo);
        if (roundToSeconds == 0) {
            return seconds;
        }
        return seconds - ((int) seconds) % roundToSeconds;
    }


    /**
     * Class DatasetURIInfo _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class DatasetURIInfo implements DatedThing {

        /** _more_ */
        private URI uri = null;

        /** _more_ */
        private Date date = null;

        /**
         * _more_
         *
         * @param u _more_
         * @param date _more_
         */
        public DatasetURIInfo(URI u, Date date) {
            this.uri  = u;
            this.date = date;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Date getDate() {
            return date;
        }

    }


    /**
     * Class InvDatasetInfo _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class InvDatasetInfo implements DatedThing {

        /** _more_ */
        private InvDataset inv = null;

        /** _more_ */
        private Date date = null;

        /**
         * _more_
         *
         * @param u _more_
         * @param date _more_
         */
        public InvDatasetInfo(InvDataset u, Date date) {
            this.inv  = u;
            this.date = date;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Date getDate() {
            return date;
        }

    }


    /**
     * Class DqcRadarDatasetInfo _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class TDSRadarDatasetInfo {

        /** _more_ */
        private ArrayList absTimeList;

        /** _more_ */
        private ArrayList datasetInfoList;

        /** _more_ */
        private ArrayList invDatasetList;

        /**
         * _more_
         */
        public TDSRadarDatasetInfo() {}

        /**
         * _more_
         *
         * @param absTimeList _more_
         * @param datasetInfoList _more_
         * @param invDatasetList _more_
         */
        public TDSRadarDatasetInfo(ArrayList absTimeList,
                                   ArrayList datasetInfoList,
                                   ArrayList invDatasetList) {
            this.absTimeList     = absTimeList;
            this.datasetInfoList = datasetInfoList;
            this.invDatasetList  = invDatasetList;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public ArrayList getTimeList() {
            return this.absTimeList;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public ArrayList getURIList() {
            return this.datasetInfoList;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public ArrayList getInvList() {
            return this.invDatasetList;
        }


    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws IOException _more_
     */
    public static void main(String args[]) throws IOException {
        StringBuffer errlog = new StringBuffer();
        String ds_location =
            "http://motherlode.ucar.edu:8081/thredds/radarServer/nexrad/level2/IDD/dataset.xml";
        // "http://motherlode.ucar.edu:8080/thredds/idd/radarLevel2";
        TDSRadarDatasetCollection dsc =
            TDSRadarDatasetCollection.factory("test", ds_location, errlog);
        System.out.println(" errs= " + errlog);

        List stns = dsc.getStations();
        System.out.println(" nstns= " + stns.size());

        StationImpl stn = dsc.getRadarStation("KFTG");  //(StationImpl)stns.get(12);

        // List ulist = stn.getRadarStationURIs();
        // assert null != ulist;
        Date ts1   = DateUnit.getStandardOrISO("2008-02-05T12:12:00");
        Date ts2   = DateUnit.getStandardOrISO("2008-02-05T23:12:00");

        List tlist = dsc.getRadarStationTimes(stn.getName(), ts1, ts2);
        int  sz    = tlist.size();
        for (int i = 0; i < 3; i++) {
            Date ts0 = (Date) tlist.get(i);
            //  Date ts0 = DateUnit.getStandardOrISO((String) tlist.get(1));
            RadialDatasetSweep rds = dsc.getRadarDataset(stn.getName(), ts0);
        }
        System.exit(0);
        Date ts0   = (Date) tlist.get(0);
        URI  stURL = dsc.getRadarDatasetURI(stn.getName(), ts0);
        assert null != stURL;
        DateSelection dateS = new DateSelection(ts1, ts2);
        dateS.setInterval((double) 3600 * 1000);
        dateS.setRoundTo((double) 3600 * 1000);
        dateS.setPreRange((double) 500 * 1000);
        dateS.setPostRange((double) 500 * 1000);



        for (int i = 0; i < stns.size(); i++) {
            stn = (StationImpl) stns.get(i);
            List times = dsc.getRadarStationTimes(
                             stn.getName(),
                             new Date(
                                 System.currentTimeMillis()
                                 - 3600 * 1000 * 24 * 100), new Date(
                                     System.currentTimeMillis()));
            if (times.size() > 0) {
                System.err.println(stn + " times:" + times.size() + " "
                                   + times.get(0) + " - "
                                   + times.get(times.size() - 1));
            } else {
                System.err.println(stn + " no times");
            }
        }


        List jList = dsc.getDataURIs("KABX", dateS);

        assert null != jList;
        List mList = dsc.getData("KABX", dateS, null);
        assert null != mList;



        //Date ts0 = DateFromString.getDateUsingCompleteDateFormat((String)tlist.get(1),"yyyy-MM-dd'T'HH:mm:ss");
        Date                       ts = (Date) tlist.get(1);
        java.text.SimpleDateFormat isoDateTimeFormat;
        isoDateTimeFormat =
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        String st = isoDateTimeFormat.format(ts);


    }



}

