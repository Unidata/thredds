/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

/**
 * Springify 11/22/2013 jcaron
 * User: rkambic
 * Date: Oct 14, 2010
 * Time: 12:15:33 PM
 */

package thredds.server.radarServer;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetScan;
import thredds.catalog.query.Location;
import thredds.catalog.query.SelectStation;
import thredds.catalog.query.Station;
import thredds.server.config.TdsContext;
import thredds.servlet.DataRootHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@DependsOn("tdsContext")
public class RadarDatasetRepository {
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  static private final String mainCatalog = "radar/radarCollections.xml";
  static private final String nexradStations = "radar/RadarNexradStations.xml";
  static private final String terminalStations = "radar/RadarTerminalStations.xml";

  public enum RadarType {
    nexrad, terminal
  }

  InvCatalogImpl defaultCat = null;
  HashMap<String, RadarDatasetCollection> datasetMap = new HashMap<String, RadarDatasetCollection>();
  HashMap<String, String> dataRoots = new HashMap<String, String>();
  List<Station> nexradList = new ArrayList<Station>();
  List<Station> terminalList = new ArrayList<Station>();
  HashMap<String, Station> nexradMap;
  HashMap<String, Station> terminalMap;

  //@Autowired
  private TdsContext tdsContext;

  /*
   * Reads the Radar Server catalog and Radar Station information, called by bean
   */
  // @PostConstruct
  public boolean init(TdsContext tdsContext) {
    boolean ok = true;
    if (defaultCat != null)
      return true;
    this.tdsContext = tdsContext;

    File catalogFile = tdsContext.getConfigFileSource().getFile(mainCatalog);
    if (catalogFile == null || !catalogFile.exists()) return false;

    // read in radarCollections.xml catalog no validation
    defaultCat = readCatalog(catalogFile);
    if (defaultCat == null) {
      ok = false;
    }

    // get path and location from cat
    DataRootHandler drh =  DataRootHandler.getInstance();
    for (InvDataset top : defaultCat.getDatasets()) {
      for (InvDataset dataset : top.getDatasets()) {
        InvDatasetScan ds = (InvDatasetScan) dataset;
        if (ds.getPath() != null) {
          String path = ds.getPath();
          String location = drh.findDataRootLocation(path);
          dataRoots.put(ds.getPath(), location);
          startupLog.info("path =" + ds.getPath() + " location =" + ds.getScanLocation());
        }
        ds.setXlinkHref(ds.getPath() + "/dataset.xml");
      }
    }

    if (nexradList.size() == 0) {
      File nexradFile = tdsContext.getConfigFileSource().getFile(nexradStations);
      nexradList = readStations(nexradFile);
      if (nexradList == null) {
        startupLog.error("Station initialization problem using " + nexradFile);
        ok = false;
      }

      File terminalFile = tdsContext.getConfigFileSource().getFile(terminalStations);
      terminalList = readStations(terminalFile);
      if (terminalList == null) {
        startupLog.error("Station initialization problem using " + terminalFile);
        ok = false;
      }

      nexradMap = getStationMap(nexradList);
      terminalMap = getStationMap(terminalList);
    }

    startupLog.info("DatasetRepository initialization done - ok={}", ok);
    return ok;
  }

  /**
   * Reads/stores requested dataset
   *
   * @param key dataset location
   * @param var if level3, the var dataset
   * @return Datset Collection
   */
  public RadarDatasetCollection getRadarDatasetCollection(String key, String var) {
    String dmkey = key;
    if (var != null)
      dmkey = key + var;
    RadarDatasetCollection rdc = datasetMap.get(dmkey);
    boolean reread = false;
    if (rdc != null)
      reread = rdc.previousDayNowAvailable();
    if (rdc == null || reread) { // need to read or reread dataset
      Object sync = new Object();
      synchronized (sync) {
        if (reread) {   // remove dataset
          datasetMap.remove(dmkey);
          rdc = null;
        } else {
          rdc = datasetMap.get(dmkey);
        }
        if (rdc != null)
          return rdc;
        rdc = new RadarDatasetCollection(dataRoots.get(key), var);
        if (rdc == null || rdc.yyyymmdd.size() == 0 || rdc.hhmm.size() == 0)
          return null;
        datasetMap.put(dmkey, rdc);
      }
    }
    return rdc;
  }

  /**
   * Removes dataset
   *
   * @param key dataset location
   * @param var if level3, the var dataset
   */
  public void removeRadarDatasetCollection(String key, String var) {
    Object sync = new Object();
    synchronized (sync) {
      if (var != null)
        datasetMap.remove(key + var);
      else
        datasetMap.remove(key);
    }
  }

  /**
   * Does the actual work of reading a catalog.
   *
   * @param catalogFile absolute location on disk
   * @return the InvCatalogImpl, or null if failure
   */
  public InvCatalogImpl readCatalog(File catalogFile) {
    // InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(false);
    InvCatalogFactory factory = DataRootHandler.getInstance().getCatalogFactory(false);

    URI catURI = catalogFile.toURI();

    InvCatalogImpl result;
    startupLog.info("radarServer readCatalog(): full path= {}", catalogFile.toString());
    try {
      result = factory.readXML(catURI);

    } catch (Throwable t) {
      startupLog.error("radarServer readCatalog(): Exception on catalog={} error={}", catalogFile, t);
      return null;
    }

    return result;
  }

  /**
   * Returns the stations from a (nexrad|terminal)Stations.xml file
   *
   * @param stnLocation TDS servers location
   * @return list of stations
   */
  public List<Station> readStations(File stnLocation) {

    // LOOK why use dom not jdom ?
    DocumentBuilder parser;
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    SelectStation parent = new SelectStation();  //kludge to use Station

    List<Station> stationList = new ArrayList<Station>();
    InputStream is = null;
    try {
      parser = factory.newDocumentBuilder();
      //stnLocation = stnLocation.replaceAll( " ", "%20");
      is = new FileInputStream(stnLocation);
      org.w3c.dom.Document doc = parser.parse(is);
      //System.out.println( "root=" + doc.getDocumentElement().getTagName() );
      NodeList stns = doc.getElementsByTagName("station");
      for (int i = 0; i < stns.getLength(); i++) {
        //System.out.println( "node=" + d.item( i ).getNodeName() );
        NamedNodeMap attr = stns.item(i).getAttributes();
        String name = "", value = "", state = "", country = "";
        for (int j = 0; j < attr.getLength(); j++) {
          if (attr.item(j).getNodeName().equals("value")) {
            value = attr.item(j).getNodeValue();
          } else if (attr.item(j).getNodeName().equals("name")) {
            name = attr.item(j).getNodeValue();
          } else if (attr.item(j).getNodeName().equals("state")) {
            state = attr.item(j).getNodeValue();
          } else if (attr.item(j).getNodeName().equals("country")) {
            country = attr.item(j).getNodeValue();
          }

        }
        NodeList child = stns.item(i).getChildNodes();  //Children of station
        Location location = null;
        for (int j = 0; j < child.getLength(); j++) {
          //System.out.println( "child =" + child.item( j ).getNodeName() );
          if (child.item(j).getNodeName().equals("location3D")) {
            NamedNodeMap ca = child.item(j).getAttributes();
            String latitude = "", longitude = "", elevation = "";
            for (int k = 0; k < ca.getLength(); k++) {
              if (ca.item(k).getNodeName().equals("latitude")) {
                latitude = ca.item(k).getNodeValue();
              } else if (ca.item(k).getNodeName().equals("longitude")) {
                longitude = ca.item(k).getNodeValue();
              } else if (ca.item(k).getNodeName().equals("elevation")) {
                elevation = ca.item(k).getNodeValue();
              }

            }
            location = new Location(latitude, longitude, elevation, null, null, null);
          }
        }
        Station station = new Station(parent, name, value, state, country, null);
        station.setLocation(location);
        stationList.add(station);
      }

    } catch (SAXException e) {
      e.printStackTrace();
      stationList = null;
    } catch (IOException e) {
      e.printStackTrace();
      stationList = null;
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      stationList = null;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          startupLog.error("radarServer getStations(): error closing" + stnLocation);
        }
      }
    }
    return stationList;
  }

  /**
   * creates a HashMap of Stations from a List
   *
   * @param list
   * @return Stations HashMap
   */
  public HashMap<String, Station> getStationMap(List<Station> list) {
    HashMap<String, Station> stationMap = new HashMap<String, Station>();
    for (Station station : list) {
      stationMap.put(station.getValue(), station);
    }
    return stationMap;
  }

  //////////////////////////
  // moved from RadarServerUtil

  /**
   * Determine if any of the given station names are actually in the dataset.
   *
   * @param stations List of station names
   * @return true if list is empty, ie no names are in the actual station list
   * @throws java.io.IOException if read error
   */
  public boolean isStationListEmpty(List<String> stations, RadarDatasetRepository.RadarType radarType ) {

    if( stations.get( 0 ).toUpperCase().equals( "ALL") )
        return false;

    for (String s : stations ) {
      if( isStation( s, radarType ))
        return false;
    }
    return true;
  }

  /**
   * returns true if a station
   * @param station
   * @param radarType
   * @return  boolean  isStation
   */
  public boolean isStation( String station, RadarDatasetRepository.RadarType radarType ) {

    if( station.toUpperCase().equals( "ALL") )
        return true;

    Station stn = null;
    // terminal level3 station
    if( station.length() == 3 && radarType.equals( RadarDatasetRepository.RadarType.terminal ) ) {
      stn = terminalMap.get( "T"+ station );
    } else if( station.length() == 3 ) {
      for( Station stn3 : nexradList ) {
         if( stn3.getValue().endsWith( station ) ) {
           stn = stn3;
           break;
         }
      }
    } else if( radarType.equals( RadarDatasetRepository.RadarType.terminal ) ) {
      stn = terminalMap.get( station );
    } else {
       stn = nexradMap.get( station );
    }
    if( stn != null)
      return true;
    return false;
  }

  /**
   * returns station or null
   * @param station
   * @param radarType
   * @return  station
   */
  public Station getStation( String station, RadarDatasetRepository.RadarType radarType ) {

    Station stn = null;
    // terminal level3 station
    if( station.length() == 3 && radarType.equals( RadarDatasetRepository.RadarType.terminal ) ) {
      stn = terminalMap.get( "T"+ station );
    } else if( station.length() == 3 ) {
      for( Station stn3 : nexradList ) {
         if( stn3.getValue().endsWith( station ) ) {
           stn = stn3;
           break;
         }
      }
    } else if( radarType.equals( RadarDatasetRepository.RadarType.terminal ) ) {
      stn = terminalMap.get( station );
    } else {
       stn = nexradMap.get( station );
    }
    return stn;
  }
}
