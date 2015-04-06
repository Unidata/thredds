package thredds.server.radarServer2;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rmay on 3/26/15.
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

                Element meta = dataset.getChild("metadata", catNS);
                conf.name = dataset.getAttributeValue("name");
                conf.urlPath = dataset.getAttributeValue("path");
                conf.diskPath = dataset.getAttributeValue("location");
                conf.dataType = meta.getChild("dataType", catNS).getValue();
                conf.dataFormat = meta.getChild("dataFormat", catNS).getValue();
                conf.stationFile = meta.getChild("stationFile", catNS).getAttributeValue("path");
                conf.doc = meta.getChild("documentation", catNS).getValue();
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

    static public class RadarConfigEntry {
        public String name, urlPath, diskPath, dataType, dataFormat, stationFile, doc;
        public List<VarInfo> vars;

        static public class VarInfo {
            public String name, vocabName, units;
        }
    }
}
