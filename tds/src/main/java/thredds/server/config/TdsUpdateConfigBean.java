/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
