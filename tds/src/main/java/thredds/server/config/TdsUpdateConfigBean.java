/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.config;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sarms on 02/13/2015.
 * @author sarms
 * @since 02/13/2015.
 */

@Component
public class TdsUpdateConfigBean {
    static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

    // fetch tds version info from Unidata and log it.
    private boolean logVersionInfo;

    public boolean isLogVersionInfo() {
        return logVersionInfo;
    }

    public void setLogVersionInfo(boolean logVersionInfo) {
        this.logVersionInfo = logVersionInfo;
    }

    /**
     * Retrieve the latest stable and development versions
     * available from Unidata. Needs to connect to
     * http://www.unidata.ucar.edu in order to get the
     * latest version numbers. The idea is to easily let users
     * know if the version of TDS they are running is out of
     * date, as this information is recorded in the
     * serverStartup.log file.
     *
     * @return A hashmap containing versionTypes as key (i.e.
     * "stable", "development") and their corresponding
     * version numbers (i.e. 4.5.2)
     */
    public Map<String, String> getLatestVersionInfo(String version) {
      int socTimeout = 1; // http socket timeout in seconds
      int connectionTimeout = 3; // http connection timeout in seconds
      Map<String, String> latestVersionInfo = new HashMap<>();

      String versionUrl = "http://www.unidata.ucar.edu/software/thredds/latest.xml";
      try {
        try (HTTPMethod method = HTTPFactory.Get(versionUrl)) {
          HTTPSession httpClient = method.getSession();
          httpClient.setSoTimeout(socTimeout * 1000);
          httpClient.setConnectionTimeout(connectionTimeout * 1000);
          httpClient.setUserAgent("TDS_" + version.replace(" ", ""));
          method.execute();
          InputStream responseIs = method.getResponseBodyAsStream();

          DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
          DocumentBuilder db = dbf.newDocumentBuilder();
          Document dom = db.parse(responseIs);
          Element docEle = dom.getDocumentElement();
          NodeList versionElements = docEle.getElementsByTagName("version");
          if(versionElements != null && versionElements.getLength() > 0) {
            for(int i = 0;i < versionElements.getLength();i++) {
              //get the version element
              Element versionElement = (Element) versionElements.item(i);
              String verType = versionElement.getAttribute("name");
              String verStr = versionElement.getAttribute("value");
              latestVersionInfo.put(verType, verStr);
            }
          }
        }
      } catch (IOException e) {
        startupLog.warn("TdsContext - Could not get latest version information from Unidata.");
      } catch (ParserConfigurationException e) {
        startupLog.error("TdsContext - Error configuring latest version xml parser" + e.getMessage() + ".");
      } catch (SAXException e) {
        startupLog.error("TdsContext - Could not parse latest version information.");
      }
      return latestVersionInfo;
    }

}
