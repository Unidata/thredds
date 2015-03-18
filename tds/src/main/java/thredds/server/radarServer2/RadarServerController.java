// This is still experimental. Don't rely on any of these methods.
package thredds.server.radarServer2;

import org.apache.commons.lang3.StringUtils;
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
import java.nio.file.*;
import java.lang.UnsupportedOperationException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import thredds.client.catalog.*;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.CatalogRefBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.client.catalog.writer.CatalogXmlWriter;
import thredds.server.config.TdsContext;
import thredds.servlet.ThreddsConfig;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

import javax.annotation.PostConstruct;

/**
 * Serve up radar data in a way that makes it easy to query. Relevant query
 * data are pulled based on filename and directory structure to avoid needing
 * to crack open large numbers of files.
 *
 * @author rmay
 * @since 01/15/2014
 */

@Controller
@RequestMapping("/radarServer2")
public class RadarServerController {
    Map<String, RadarDataInventory> data;
    final String URLbase = "/thredds/radarServer2";
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

    @PostConstruct
    public void init() {
        enabled = ThreddsConfig.getBoolean("RadarServer.allow", false);
        if (!enabled) return;

        // TODO: Need to pull this information from a config file
        data = new TreeMap<>();
        Path dataRoot = Paths.get("/data/ldm/pub/native/radar");
        String[] paths = {"level3"};
        String contentPath = tdsContext.getContentDirectory().getPath();
        for (String path : paths) {
            RadarDataInventory di = new RadarDataInventory(dataRoot, path);
            di.setName(path);
            di.addVariableDir();
            di.addStationDir();
            di.addDateDir("yyyyMMdd");
            di.addFileTime("yyyyMMdd_HHmm#.nids#");
            di.setNearestWindow(CalendarPeriod.of(1, CalendarPeriod.Field.Hour));
            data.put(path, di);
            StationList sl = di.getStationList();
            sl.loadFromXmlFile(contentPath + "/radar/RadarNexradStations.xml");
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

        for (RadarDataInventory di: data.values()) {
            CatalogRefBuilder crb = new CatalogRefBuilder(mainDB);
            crb.setName(di.getName());
            crb.setTitle(di.getName());
            Path dataBase = di.getRoot().relativize(di.getCollectionDir());
            crb.setHref(dataBase.resolve("dataset.xml").toString());
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

    @RequestMapping(value="{dataset}/dataset.xml")
    @ResponseBody
    public HttpEntity<byte[]> datasetCatalog(@PathVariable String dataset) throws IOException
    {
        if (!enabled) return null;

        RadarDataInventory di = getInventory(dataset);

        CatalogBuilder cb = new CatalogBuilder();
        cb.addService(new Service("radarServer", URLbase,
                "DQC", null, null, new ArrayList<Service>(),
                new ArrayList<Property>()));
        cb.setName("Radar Data");

        Path dataBase = di.getRoot().relativize(di.getCollectionDir());
        DatasetBuilder mainDB = new DatasetBuilder(null);
        mainDB.setName(di.getName());
        mainDB.put(Dataset.Id, dataBase.toString());
        mainDB.put(Dataset.UrlPath, dataBase.toString());
        mainDB.put(Dataset.DataFormatType, "NIDS"); // TODO: Pull from inventory
        mainDB.put(Dataset.FeatureType, FeatureType.RADIAL.toString());
        mainDB.put(Dataset.ServiceName, "radarServer");

        ThreddsMetadata tmd = new ThreddsMetadata();
        Map<String, Object> metadata = tmd.getFlds();
        metadata.put(Dataset.Documentation, new Documentation(null, null, null,
                "summary", di.getDescription()));

        metadata.put(Dataset.GeospatialCoverage,
                new ThreddsMetadata.GeospatialCoverage(
                        new ThreddsMetadata.GeospatialRange(20.0, 40.0, 0.0,
                                "degrees_north"),
                        new ThreddsMetadata.GeospatialRange(-135.0, 70.0, 0.0,
                                "degrees_east"),
                        new ThreddsMetadata.GeospatialRange(0.0, 70.0, 0.0,
                                "km"), new ArrayList<ThreddsMetadata.Vocab>(),
                        null));

        CalendarDate now = CalendarDate.present();
        CalendarDate start = now.subtract(CalendarPeriod.of(14,
                CalendarPeriod.Field.Day));
        metadata.put(Dataset.TimeCoverage, new DateRange(start.toDate(),
                now.toDate()));

        List<ThreddsMetadata.Variable> vars = new ArrayList<>();
        vars.add(new ThreddsMetadata.Variable("Reflectivity", null,
                "EARTH SCIENCE > Spectral/Engineering > Radar > Radar " +
                        "Reflectivity", "dB", null));
        ThreddsMetadata.VariableGroup vg = new ThreddsMetadata.VariableGroup(
                "DIF", null, null, vars);
        metadata.put(Dataset.VariableGroups, vg);

        mainDB.put(Dataset.ThreddsMetadataInheritable, tmd);

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

    @RequestMapping(value="{dataset}", params="station=all")
    @ResponseBody
    public StationList stations(@PathVariable String dataset)
    {
        if (!enabled) return null;
        return listStations(dataset);
    }

    @RequestMapping(value="{dataset}/stations.xml")
    @ResponseBody
    public StationList stationsFile(@PathVariable String dataset)
    {
        if (!enabled) return null;
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
        byte[] bytes = str.getBytes();
        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "text"));
        header.setContentLength(bytes.length);
        return new HttpEntity<>(bytes, header);
    }

    @RequestMapping(value="{dataset}")
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
            @RequestParam(value="var", required=false) String[] vars)
            throws ParseException, UnsupportedOperationException, IOException
    {
        if (!enabled) return null;
        RadarDataInventory di = getInventory(dataset);
        if (di == null) {
            return simpleString("Could not find dataset: " + dataset);
        }

        RadarDataInventory.Query q = di.newQuery();
        if (!setTimeLimits(q, time, start, end, period, temporal))
            throw new UnsupportedOperationException("Either a single time " +
                    "argument, temporal=all, or a combination of time_start, " +
                    "time_end, and time_duration must be provided.");
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
                addQueryElement(queryString, "longitude", lon.toString());
                addQueryElement(queryString, "latitude", lat.toString());
                addQueryElement(queryString, "north", north.toString());
                addQueryElement(queryString, "south", south.toString());
                addQueryElement(queryString, "east", east.toString());
                addQueryElement(queryString, "west", west.toString());
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

        return makeCatalog(di, q, queryString.toString());
    }

    private void addQueryElement(StringBuilder sb, String name,
                                 String[] values) {
        if (values != null) {
            addQueryElement(sb, name, StringUtils.join(values, ','));
        }
    }

    private void addQueryElement(StringBuilder sb, String name, String value) {
        if (value != null) {
            if (sb.length() > 0)
                sb.append('&');
            sb.append(name).append('=').append(value);
        }
    }

    private HttpEntity<byte[]> makeCatalog(RadarDataInventory inv,
                                           RadarDataInventory.Query query,
                                           String queryString) throws
            IOException, ParseException
    {
        Path dataBase = inv.getRoot().relativize(inv.getCollectionDir());

        List<RadarDataInventory.Query.QueryResultItem> res = query.results();
        CatalogBuilder cb = new CatalogBuilder();
        cb.addService(new Service("OPENDAP",
                "/thredds/dodsC/" + dataBase.toString(),
                ServiceType.OPENDAP.toString(), null, null,
                new ArrayList<Service>(), new ArrayList<Property>()));
        cb.setName("Radar " + inv.getName() + " datasets in near real time");

        DatasetBuilder mainDB = new DatasetBuilder(null);
        mainDB.setName("Radar" + inv.getName() +
                        " datasets for available stations and times");
        mainDB.put(Dataset.CollectionType, "TimeSeries");
        mainDB.put(Dataset.Id, queryString);

        ThreddsMetadata tmd = new ThreddsMetadata();
        Map<String, Object> metadata = tmd.getFlds();
        metadata.put(Dataset.DataFormatType, "NIDS");
        metadata.put(Dataset.FeatureType, "Radial");//FeatureType.RADIAL.toString());
        metadata.put(Dataset.ServiceName, "OPENDAP");
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
            query.addDateRange(new CalendarDateRange(timeEnd,
                    (long) -duration.getValueInSeconds()));
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
        for (String stid: stations)
            query.addCriteria(RadarDataInventory.DirType.Station, stid);
    }

    String[] getStations(StationList stations, Double lon, Double lat,
                         Double north, Double south, Double east, Double west)
    {
        if (lat != null && lon != null) {
            // Pull nearest station
            StationList.Station nearest = stations.getNearest(lon, lat);
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
