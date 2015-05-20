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
import java.util.ArrayList;
import java.util.List;

/**
 * Handle configuration for the Radar Server
 */
public class RadarServerConfig {
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

                Element meta = dataset.getChild("metadata", catNS);
                conf.name = dataset.getAttributeValue("name");
                conf.urlPath = dataset.getAttributeValue("path");
                conf.diskPath = dataset.getAttributeValue("location");
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
        public String name, urlPath, diskPath, dataFormat, stationFile, doc;
        public String dateParseRegex, dateFmt, layout;
        public DateRange timeCoverage;
        public GeoInfo  spatialCoverage;
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
