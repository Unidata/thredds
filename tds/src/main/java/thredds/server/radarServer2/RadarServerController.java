// This is still experimental. Don't rely on any of these methods.
package thredds.server.radarServer2;

import com.google.common.base.Joiner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.UnsupportedOperationException;
import java.text.ParseException;
import java.util.*;

import org.springframework.web.servlet.HandlerMapping;
import thredds.client.catalog.*;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.CatalogRefBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.client.catalog.writer.CatalogXmlWriter;
import thredds.server.admin.DebugController;
import thredds.server.config.TdsContext;
import thredds.servlet.ThreddsConfig;
import ucar.nc2.constants.CDM;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

/**
 * Serve up radar data in a way that makes it easy to query. Relevant query
 * data are pulled based on filename and directory structure to avoid needing
 * to crack open large numbers of files.
 *
 * @author rmay
 * @since 01/15/2014
 */

@Controller
@RequestMapping("/radarServer")
public class RadarServerController {
    Map<String, RadarDataInventory> data;
    static final String appName = "/thredds/";
    static final String entryPoint = "radarServer/";
    static final String URLbase = appName + entryPoint;
    static Map<String, List<RadarServerConfig.RadarConfigEntry.VarInfo>> vars;
    boolean enabled = false;

    @Autowired
    TdsContext tdsContext;

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public String handleException(Exception exc) {
        StringWriter sw = new StringWriter(5000);
        exc.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseBody
    public String badQuery(Exception exc) {
        return exc.getMessage();
    }

    public RadarServerController() {
    }

    void setupDebug() {
        DebugController.Category debugHandler = DebugController.find("RadarServer");
        DebugController.Action act = new DebugController.Action("showDatasets", "Show Datasets") {
            public void doAction(DebugController.Event e) {
                try {
                    for (Map.Entry<String, RadarDataInventory> ent : data.entrySet()) {
                        e.pw.println(ent.getKey());

                        RadarDataInventory di = ent.getValue();
                        if (di == null) {
                            e.pw.println("Dataset is null");
                            continue;
                        }
                        e.pw.printf("Collection Dir: %s%n", di.getCollectionDir().toString());
                        e.pw.printf("Last Update: %s%n", di.getLastUpdate());
                        e.pw.println("Dates:");
                        for (String item : di.listItems(RadarDataInventory.DirType.Date)) {
                            e.pw.println("\t" + item);
                        }
                        e.pw.println("Stations:");
                        for (String item : di.listItems(RadarDataInventory.DirType.Station)) {
                            e.pw.println("\t" + item);
                        }
                        e.pw.println("Vars:");
                        for (String item : di.listItems(RadarDataInventory.DirType.Variable)) {
                            e.pw.println("\t" + item);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(e.pw);
                }
            }
        };
        debugHandler.addAction(act);
    }

    @PostConstruct
    public void init() {
        enabled = ThreddsConfig.getBoolean("RadarServer.allow", false);
        if (!enabled) return;

        setupDebug();

        data = new TreeMap<>();
        vars = new TreeMap<>();
        String contentPath = tdsContext.getContentDirectory().getPath();
        List<RadarServerConfig.RadarConfigEntry> configs = RadarServerConfig.readXML(contentPath + "/radar/radarCollections.xml");
        for (RadarServerConfig.RadarConfigEntry conf : configs) {
            RadarDataInventory di = new RadarDataInventory(conf.dataPath,
                    conf.crawlItems);
            di.setName(conf.name);
            di.setDescription(conf.doc);

            for (String part: conf.layout.split("/")) {
                switch (part) {
                    case "STATION":
                        di.addStationDir();
                        break;
                    case "VARIABLE":
                        di.addVariableDir();
                        break;
                    default: // Assume date format
                        di.addDateDir(part);
                }
            }

            di.addFileTime(conf.dateParseRegex, conf.dateFmt);
            di.setNearestWindow(CalendarPeriod.of(1, CalendarPeriod.Field.Hour));

            // TODO: These needs to come from files instead
            di.setDataFormat(conf.dataFormat);
            di.setTimeCoverage(conf.timeCoverage);
            di.setGeoCoverage(conf.spatialCoverage);

            data.put(conf.urlPath, di);
            vars.put(conf.urlPath, conf.vars);
            StationList sl = di.getStationList();
            sl.loadFromXmlFile(contentPath + "/" + conf.stationFile);
        }
    }

    @RequestMapping(value="catalog.xml")
    @ResponseBody
    public HttpEntity<byte[]> topLevelCatalog() throws IOException
    {
        if (!enabled) return null;

        CatalogBuilder cb = new CatalogBuilder();
        cb.addService(new Service("radarServer", URLbase,
                "QueryCapability", null, null, new ArrayList<Service>(),
                new ArrayList<Property>()));
        cb.setName("THREDDS Radar Server");

        DatasetBuilder mainDB = new DatasetBuilder(null);
        mainDB.setName("Radar Data");

        for (Map.Entry<String, RadarDataInventory> ent: data.entrySet()) {
            RadarDataInventory di = ent.getValue();
            CatalogRefBuilder crb = new CatalogRefBuilder(mainDB);
            crb.setName(di.getName());
            crb.setTitle(di.getName());
            crb.setHref(ent.getKey() + "/dataset.xml");
            mainDB.addDataset(crb);
        }
        cb.addDataset(mainDB);

        CatalogXmlWriter writer = new CatalogXmlWriter();
        ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
        writer.writeXML(cb.makeCatalog(), os);
        byte[] xmlBytes = os.toByteArray();

        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "xml"));
        header.setContentLength(xmlBytes.length);
        return new HttpEntity<>(xmlBytes, header);
    }

    // Old IDV code doesn't actually parse a catalog, but a custom XML file.
    // This code tweaks our catalog output to match.
    private String idvDatasetCatalog(String xml)
    {
        String ret = xml.replace("variables", "Variables");
        ret = ret.replace("timeCoverage", "TimeSpan");
        StringBuilder sub = new StringBuilder(ret.substring(0,
                ret.indexOf("<geospatialCoverage>")));
        sub.append("<LatLonBox>\n\t<north>90.0</north>\n\t<south>-90.0</south>");
        sub.append("\n\t<east>180.0</east>\n\t<west>-180.0</west></LatLonBox>");
        String endCoverage = "</geospatialCoverage>";
        sub.append(ret.substring(ret.indexOf(endCoverage) + endCoverage.length()));
        return sub.toString();
    }

    // Old IDV can't handle all that we put out as a time coverage. This
    // function forces the DateRange to use fixed times rather than, e.g.,
    // present and 14 days.
    private DateRange idvCompatibleRange(DateRange range)
    {
        CalendarDate start = range.getStart().getCalendarDate();
        CalendarDate end = range.getEnd().getCalendarDate();
        return new DateRange(start.toDate(), end.toDate());
    }


    private String parseDatasetFromURL(final HttpServletRequest request)
    {
        String match = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        return match.substring(match.indexOf(entryPoint) + entryPoint.length());
    }

    private String parseDatasetFromURL(final HttpServletRequest request, String ending)
    {
        String match = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        return match.substring(match.indexOf(entryPoint) + entryPoint.length(),
                match.indexOf(ending));
    }

    @RequestMapping(value="**/dataset.xml")
    @ResponseBody
    public HttpEntity<byte[]> datasetCatalog(final HttpServletRequest request) throws IOException
    {
        if (!enabled) return null;

        // Check the user-agent to try to guess if this request is coming from
        // the IDV--if so, we'll need to tweak the returned catalog XML.
        String agent = request.getHeader("user-agent");
        boolean makeIDVCatalog = false;
        if (agent != null && agent.startsWith("Java/1."))
            makeIDVCatalog = true;

        // Parse the request URL to get the name of the dataset that was
        // requested.
        String dataset = parseDatasetFromURL(request, "/dataset.xml");
        RadarDataInventory di = getInventory(dataset);

        CatalogBuilder cb = new CatalogBuilder();
        cb.addService(new Service("radarServer", URLbase,
                "DQC", null, null, new ArrayList<Service>(),
                new ArrayList<Property>()));
        cb.setName("Radar Data");

        DatasetBuilder mainDB = new DatasetBuilder(null);
        mainDB.setName(di.getName());
        mainDB.put(Dataset.Id, dataset);
        mainDB.put(Dataset.UrlPath, dataset);
        mainDB.put(Dataset.DataFormatType, di.getDataFormat());
        mainDB.put(Dataset.FeatureType, di.getFeatureType().toString());
        mainDB.put(Dataset.ServiceName, "radarServer");

        ThreddsMetadata tmd = new ThreddsMetadata();
        Map<String, Object> metadata = tmd.getFlds();
        metadata.put(Dataset.Documentation, new Documentation(null, null, null,
                "summary", di.getDescription()));

        RadarServerConfig.RadarConfigEntry.GeoInfo gi = di.getGeoCoverage();
        metadata.put(Dataset.GeospatialCoverage,
                new ThreddsMetadata.GeospatialCoverage(
                        new ThreddsMetadata.GeospatialRange(gi.eastWest.start,
                                gi.eastWest.size, 0.0, gi.eastWest.units),
                        new ThreddsMetadata.GeospatialRange(gi.northSouth.start,
                                gi.northSouth.size, 0.0, gi.northSouth.units),
                        new ThreddsMetadata.GeospatialRange(gi.upDown.start,
                                gi.upDown.size, 0.0, gi.upDown.units),
                        new ArrayList<ThreddsMetadata.Vocab>(), null));

        DateRange range = di.getTimeCoverage();
        if (makeIDVCatalog) range = idvCompatibleRange(range);
        metadata.put(Dataset.TimeCoverage, range);

        // TODO: Need to be able to get this from the inventory
        List<ThreddsMetadata.Variable> catalogVars = new ArrayList<>();
        for (RadarServerConfig.RadarConfigEntry.VarInfo vi: vars.get(dataset)) {
            catalogVars.add(new ThreddsMetadata.Variable(vi.name, null,
                    vi.vocabName, vi.units, null));
        }
        ThreddsMetadata.VariableGroup vg = new ThreddsMetadata.VariableGroup(
                "DIF", null, null, catalogVars);
        metadata.put(Dataset.VariableGroups, vg);

        mainDB.put(Dataset.ThreddsMetadataInheritable, tmd);

        cb.addDataset(mainDB);

        CatalogXmlWriter writer = new CatalogXmlWriter();
        ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
        writer.writeXML(cb.makeCatalog(), os);

        byte[] xmlBytes;
        if (makeIDVCatalog) {
            String xml = os.toString(CDM.UTF8);
            xml = idvDatasetCatalog(xml);
            xmlBytes = xml.getBytes(CDM.utf8Charset);
        } else {
            xmlBytes = os.toByteArray();
        }

        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "xml"));
        header.setContentLength(xmlBytes.length);
        return new HttpEntity<>(xmlBytes, header);
    }

    @RequestMapping(value="**/{dataset}", params="station=all")
    @ResponseBody
    public StationList stations(@PathVariable String dataset,
                                final HttpServletRequest request)
    {
        if (!enabled) return null;
        dataset = parseDatasetFromURL(request);
        return listStations(dataset);
    }

    @RequestMapping(value="**/stations.xml")
    @ResponseBody
    public StationList stationsFile(final HttpServletRequest request)
    {
        if (!enabled) return null;
        String dataset = parseDatasetFromURL(request, "/stations.xml");
        return listStations(dataset);
    }

    StationList listStations(String dataset)
    {
        RadarDataInventory di = getInventory(dataset);
        return di.getStationList();
    }

    RadarDataInventory getInventory(String dataset)
    {
        return data.get(dataset);
    }

    HttpEntity<byte[]> simpleString(String str)
    {
        byte[] bytes = str.getBytes(CDM.utf8Charset);
        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "text"));
        header.setContentLength(bytes.length);
        return new HttpEntity<>(bytes, header);
    }

    @RequestMapping(value="**/{dataset}")
    HttpEntity<byte[]> handleQuery(@PathVariable String dataset,
            @RequestParam(value="stn", required=false) String[] stations,
            @RequestParam(value="longitude", required=false) Double lon,
            @RequestParam(value="latitude", required=false) Double lat,
            @RequestParam(value="east", required=false) Double east,
            @RequestParam(value="west", required=false) Double west,
            @RequestParam(value="north", required=false) Double north,
            @RequestParam(value="south", required=false) Double south,
            @RequestParam(value="time", required=false) String time,
            @RequestParam(value="time_start", required=false) String start,
            @RequestParam(value="time_end", required=false) String end,
            @RequestParam(value="time_duration", required=false) String period,
            @RequestParam(value="temporal", required=false) String temporal,
            @RequestParam(value="var", required=false) String[] vars,
            final HttpServletRequest request)
            throws ParseException, UnsupportedOperationException, IOException
    {
        if (!enabled) return null;

        dataset = parseDatasetFromURL(request);
        RadarDataInventory di = getInventory(dataset);
        if (di == null) {
            return simpleString("Could not find dataset: " + dataset);
        }

        RadarDataInventory.Query q = di.newQuery();
        try {
            if (!setTimeLimits(q, time, start, end, period, temporal))
                throw new UnsupportedOperationException("Either a single time " +
                        "argument, temporal=all, or a combination of time_start, " +
                        "time_end, and time_duration must be provided.");
        } catch (ParseException e) {
            throw new UnsupportedOperationException("Invalid time string passed: " + e.getMessage());
        }
        StringBuilder queryString = new StringBuilder();
        addQueryElement(queryString, "time", time);
        addQueryElement(queryString, "time_start", start);
        addQueryElement(queryString, "time_end", end);
        addQueryElement(queryString, "time_duration", period);
        addQueryElement(queryString, "temporal", temporal);

        if (stations == null) {
            try {
                stations = getStations(di.getStationList(), lon, lat, north,
                        south, east, west);
                addQueryElement(queryString, "longitude", lon);
                addQueryElement(queryString, "latitude", lat);
                addQueryElement(queryString, "north", north);
                addQueryElement(queryString, "south", south);
                addQueryElement(queryString, "east", east);
                addQueryElement(queryString, "west", west);
            } catch (UnsupportedOperationException e) {
                throw new UnsupportedOperationException("Either a list of " +
                        "stations, a lat/lon point, or a box defined by " +
                        "north, south, east, and west parameters must be " +
                        "provided.");
            }
        } else {
            addQueryElement(queryString, "stn", stations);
        }
        setStations(q, stations);

        if (di.needsVar()) {
            if (!setVariables(q, vars))
                throw new UnsupportedOperationException("One or more variables " +
                        "required.");
            addQueryElement(queryString, "var", vars);
        }

        return makeCatalog(dataset, di, q, queryString.toString());
    }

    private void addQueryElement(StringBuilder sb, String name,
                                 String[] values) {
        if (values != null) {
            addQueryElement(sb, name, Joiner.on(',').join(values));
        }
    }

    private void addQueryElement(StringBuilder sb, String name, Double value) {
        if (value != null) {
            addQueryElement(sb, name, value.toString());
        }
    }

    private void addQueryElement(StringBuilder sb, String name, String value) {
        if (value != null) {
            if (sb.length() > 0)
                sb.append('&');
            sb.append(name).append('=').append(value);
        }
    }

    private HttpEntity<byte[]> makeCatalog(String dataset,
                                           RadarDataInventory inv,
                                           RadarDataInventory.Query query,
                                           String queryString) throws
            IOException, ParseException
    {
        Collection<RadarDataInventory.Query.QueryResultItem> res = query.results();
        CatalogBuilder cb = new CatalogBuilder();

        // At least the IDV needs to have the trailing slash included
        if (!dataset.endsWith("/"))
            dataset += "/";

        Service dap = new Service("OPENDAP", "/thredds/dodsC/" + dataset,
                ServiceType.OPENDAP.toString(), null, null,
                new ArrayList<Service>(), new ArrayList<Property>());
        Service cdmr = new Service("CDMRemote", "/thredds/cdmremote/" + dataset,
                ServiceType.CdmRemote.toString(), null, null,
                new ArrayList<Service>(), new ArrayList<Property>());
        Service files = new Service("HTTPServer", "/thredds/fileServer/" + dataset,
                ServiceType.HTTPServer.toString(), null, null,
                new ArrayList<Service>(), new ArrayList<Property>());
        cb.addService(new Service("RadarServices", "",
                ServiceType.Compound.toString(), null, null,
                Arrays.asList(dap, files, cdmr), new ArrayList<Property>()));
        cb.setName("Radar " + inv.getName() + " datasets in near real time");

        DatasetBuilder mainDB = new DatasetBuilder(null);
        mainDB.setName("Radar" + inv.getName() +
                        " datasets for available stations and times");
        mainDB.put(Dataset.CollectionType, "TimeSeries");
        mainDB.put(Dataset.Id, queryString);

        ThreddsMetadata tmd = new ThreddsMetadata();
        Map<String, Object> metadata = tmd.getFlds();
        metadata.put(Dataset.DataFormatType, inv.getDataFormat());
        metadata.put(Dataset.FeatureType, inv.getFeatureType().toString());
        metadata.put(Dataset.ServiceName, "RadarServices");
        metadata.put(Dataset.Documentation, new Documentation(null, null, null,
                null, res.size() + " datasets found for query"));

        mainDB.put(Dataset.ThreddsMetadataInheritable, tmd);

        for (RadarDataInventory.Query.QueryResultItem i: res) {
            DatasetBuilder fileDB = new DatasetBuilder(mainDB);
            fileDB.setName(i.file.getFileName().toString());
            fileDB.put(Dataset.Id, String.valueOf(i.file.hashCode()));

            fileDB.put(Dataset.Dates, new DateType(i.time.toString(), null,
                    "start of ob", i.time.getCalendar()));

            // TODO: Does this need to be converted from the on-disk path
            // to a mapped url path?
            fileDB.put(Dataset.UrlPath,
                    inv.getCollectionDir().relativize(i.file).toString());
            mainDB.addDataset(fileDB);
        }

        cb.addDataset(mainDB);

        CatalogXmlWriter writer = new CatalogXmlWriter();
        ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
        writer.writeXML(cb.makeCatalog(), os);
        byte[] xmlBytes = os.toByteArray();

        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "xml"));
        header.setContentLength(xmlBytes.length);
        return new HttpEntity<>(xmlBytes, header);
    }

    boolean setTimeLimits(RadarDataInventory.Query query, String timePoint,
                          String start, String end, String period,
                          String temporal)
            throws ParseException
    {
        CalendarDate time = parseTime(timePoint);
        if (time != null) {
            query.addDateRange(CalendarDateRange.of(time, time));
            return true;
        }

        CalendarDate timeStart = parseTime(start);
        CalendarDate timeEnd = parseTime(end);

        TimeDuration duration = null;
        if (period != null)
            duration = TimeDuration.parseW3CDuration(period);

        if (timeStart != null) {
            if (timeEnd != null) {
                query.addDateRange(CalendarDateRange.of(timeStart, timeEnd));
                return true;
            } else if (duration != null) {
                query.addDateRange(new CalendarDateRange(timeStart,
                        (long) duration.getValueInSeconds()));
                return true;
            }
        } else if (timeEnd != null && duration != null) {
            query.addDateRange(new CalendarDateRange(
                    timeEnd.add((long) -duration.getValueInSeconds(), CalendarPeriod.Field.Second),
                    (long) duration.getValueInSeconds()));
            return true;
        }

        if (temporal != null) {
            // Use null to indicate no range
            query.addDateRange(null);
            return true;
        }
        return false;
    }

    CalendarDate parseTime(String timeString)
    {
        if (timeString == null)
            return null;

        if (timeString.equalsIgnoreCase("present")) {
            return CalendarDate.present();
        } else {
            return CalendarDate.parseISOformat(null, timeString);
        }
    }

    void setStations(RadarDataInventory.Query query, String[] stations)
    {
        if (stations.length == 0)
            throw new UnsupportedOperationException("No stations " +
                    "found for specified coordinates.");
        for (String stid: stations)
            query.addCriteria(RadarDataInventory.DirType.Station, stid);
    }

    String[] getStations(StationList stations, Double lon, Double lat,
                         Double north, Double south, Double east, Double west)
    {
        if (lat != null && lon != null) {
            // Pull nearest station
            StationList.Station nearest = stations.getNearest(lon, lat);
            if (nearest == null) {
                throw new UnsupportedOperationException("No stations " +
                        "available to search for nearest.");
            }
            return new String[]{nearest.getStid()};
        } else if(north != null && south != null && east != null &&
                west != null) {
            // Pull all stations within box
            List<StationList.Station> inBox = stations.getStations(east, west,
                    north, south);
            List<String> stIds = new ArrayList<>(inBox.size());
            for(StationList.Station s: inBox) {
                stIds.add(s.getStid());
            }
            return stIds.toArray(new String[stIds.size()]);
        } else {
            throw new UnsupportedOperationException("Either station, " +
                    "a lat/lon point, or a box defined by north, " +
                    "south, east, and west parameters must be provided.");
        }
    }

    boolean setVariables(RadarDataInventory.Query query, String[] variables)
    {
        if (variables == null)
            return false;

        for (String var: variables)
            query.addCriteria(RadarDataInventory.DirType.Variable, var);

        return true;
    }
}
