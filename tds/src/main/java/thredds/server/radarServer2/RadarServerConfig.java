package thredds.server.radarServer2;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.constants.CDM;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle configuration for the Radar Server
 */
public class RadarServerConfig {
    static ConcurrentHashMap<String, FileSystemProvider> fsproviders = new ConcurrentHashMap<>();

    static public List<RadarConfigEntry> readXML(String filename) {
        List<RadarConfigEntry> configs = new ArrayList<>();

        SAXBuilder builder = new SAXBuilder();
        File f = new File(filename);
        try {
            Document doc = builder.build(f);
            Element cat = doc.getRootElement();
            Namespace catNS = cat.getNamespace();
            Element topDS = cat.getChild("dataset", cat.getNamespace());
            for (Element dataset: topDS.getChildren("datasetScan", catNS)) {
                RadarConfigEntry conf = new RadarConfigEntry();
                configs.add(conf);

                Element collection = dataset.getChild("radarCollection", catNS);
                conf.dateParseRegex = collection.getAttributeValue("dateRegex");
                conf.dateFmt = collection.getAttributeValue("dateFormat");
                conf.layout = collection.getAttributeValue("layout");
                String crawlItems = collection.getAttributeValue("crawlItems");
                if (crawlItems != null) {
                    conf.crawlItems = Integer.parseInt(crawlItems);
                } else {
                    conf.crawlItems = 5;
                }

                Element meta = dataset.getChild("metadata", catNS);
                conf.name = dataset.getAttributeValue("name");
                conf.urlPath = dataset.getAttributeValue("path");
                conf.dataPath = getPath(dataset.getAttributeValue("location"));
                conf.dataFormat = meta.getChild("dataFormat", catNS).getValue();
                conf.stationFile = meta.getChild("stationFile", catNS).getAttributeValue("path");
                conf.doc = meta.getChild("documentation", catNS).getValue();

                conf.timeCoverage = readTimeCoverage(
                        meta.getChild("timeCoverage", catNS));

                conf.spatialCoverage = readGeospatialCoverage(
                        meta.getChild("geospatialCoverage", catNS));

                Element variables = meta.getChild("variables", catNS);
                conf.vars = new ArrayList<>();
                for (Element var: variables.getChildren("variable", catNS)) {
                    RadarConfigEntry.VarInfo inf = new RadarConfigEntry.VarInfo();
                    conf.vars.add(inf);

                    inf.name = var.getAttributeValue("name");
                    inf.vocabName = var.getAttributeValue("vocabulary_name");
                    inf.units = var.getAttributeValue("units");
                }
            }
        } catch (IOException | JDOMException e) {
            e.printStackTrace();
        }

        return configs;
    }

    private static Path getPath(String location) throws IOException {
        FileSystem fs;

        // If we're given an actual URI, use that to find the file system.
        // Otherwise, use the default.
        if(location.contains(":")) {
            URI uri = URI.create(location);

            // Fix parsing of s3:// (note two '/') style paths
            if (uri.getPath().isEmpty()) {
                uri = URI.create(location.replace("//", "///"));
            }

            location = uri.getPath();
            fs = getFS(uri);
        } else {
            fs = FileSystems.getDefault();
        }
        return fs.getPath(location);
    }

    private static FileSystem getFS(URI uri) throws IOException {
        FileSystem fs;

        try {
            fs = getProvider(uri).getFileSystem(uri);
        } catch (ProviderNotFoundException e) {
            System.out.println("Provider not found: " + e.getMessage());
            System.out.println("Using default file system.");
            fs = FileSystems.getDefault();
        }
        return fs;
    }

    // This is to work around the fact that when we *get* a filesystem, we
    // cannot pass in the class loader. This results in custom providers (say
    // S3) not being found. However, the filesystem already exists, so the
    // filesystem can't be re-created either.
    private static FileSystemProvider getProvider(URI uri) throws IOException {
        if(fsproviders.containsKey(uri.getScheme())) {
            return fsproviders.get(uri.getScheme());
        } else {
            FileSystem fs;
            try {
                fs = FileSystems.newFileSystem(uri,
                        new HashMap<String, Object>(),
                        Thread.currentThread().getContextClassLoader());
            } catch (FileSystemAlreadyExistsException e) {
                fs = FileSystems.getFileSystem(uri);
            }
            fsproviders.put(uri.getScheme(), fs.provider());
            return fs.provider();
        }
    }

    protected static RadarConfigEntry.GeoInfo readGeospatialCoverage(Element gcElem) {
        if (gcElem == null) return null;
        Namespace defNS = gcElem.getNamespace();

        RadarConfigEntry.GeoInfo gi = new RadarConfigEntry.GeoInfo();
        gi.northSouth = readGeospatialRange(gcElem.getChild("northsouth", defNS), CDM.LAT_UNITS);
        gi.eastWest = readGeospatialRange(gcElem.getChild("eastwest", defNS), CDM.LON_UNITS);
        gi.upDown = readGeospatialRange(gcElem.getChild("updown", defNS), "m");

        return gi;
    }

    protected static RadarConfigEntry.RangeInfo readGeospatialRange(Element spElem, String defUnits) {
        if (spElem == null) return null;

        Namespace defNS = spElem.getNamespace();
        double start = readDouble(spElem.getChild("start", defNS));
        double size = readDouble(spElem.getChild("size", defNS));

        String units = spElem.getChildText("units", defNS);
        if (units == null) units = defUnits;

        RadarConfigEntry.RangeInfo ri = new RadarConfigEntry.RangeInfo();
        ri.units = units;
        ri.start = start;
        ri.size = size;
        return ri;
    }

    protected static double readDouble(Element elem) {
        if (elem == null) return Double.NaN;
        String text = elem.getText();
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    protected static DateRange readTimeCoverage(Element tElem) {
        if (tElem == null) return null;
        Namespace defNS = tElem.getNamespace();
        DateType start = readDate(tElem.getChild("start", defNS));
        DateType end = readDate(tElem.getChild("end", defNS));
        TimeDuration duration = readDuration(tElem.getChild("duration", defNS));
        TimeDuration resolution = readDuration(tElem.getChild("resolution", defNS));

        try {
            return new DateRange(start, end, duration, resolution);
        } catch (java.lang.IllegalArgumentException e) {
            return null;
        }
    }

    protected static DateType readDate(Element elem) {
        if (elem == null) return null;
        String format = elem.getAttributeValue("format");
        String type = elem.getAttributeValue("type");
        return makeDateType(elem.getText(), format, type);
    }

    protected static DateType makeDateType(String text, String format, String type) {
        if (text == null) return null;
        try {
            return new DateType(text, format, type);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    protected static TimeDuration readDuration(Element elem) {
        if (elem == null) return null;
        String text;
        try {
            text = elem.getText();
            return new TimeDuration(text);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    static public class RadarConfigEntry {
        public Path dataPath;
        public String name, urlPath, dataFormat, stationFile, doc;
        public String dateParseRegex, dateFmt, layout;
        public DateRange timeCoverage;
        public GeoInfo  spatialCoverage;
        public int crawlItems;
        public List<VarInfo> vars;

        static public class GeoInfo {
            RangeInfo eastWest, northSouth, upDown;
        }

        static public class RangeInfo {
            public String units;
            public double start, size;
        }

        static public class VarInfo {
            public String name, vocabName, units;
        }
    }
}
