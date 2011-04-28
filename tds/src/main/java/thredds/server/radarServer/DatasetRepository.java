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
 * User: rkambic
 * Date: Oct 14, 2010
 * Time: 12:15:33 PM
 */

package thredds.server.radarServer;

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
import thredds.servlet.UsageLog;
import ucar.unidata.util.StringUtil;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DatasetRepository {

  public enum RadarType {
    nexrad, terminal
  }

  static public org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( DatasetRepository.class );
  static public InvCatalogImpl cat = null;
  static public final String catName = "radarCollections.xml";
  static public URI catURI;
  static public HashMap<String, RadarDatasetCollection> datasetMap = new HashMap<String, RadarDatasetCollection>();
  static public HashMap<String, String> dataLocation = new HashMap<String, String>();
  static public final String nexradStations = "RadarNexradStations.xml";
  static public final String terminalStations = "RadarTerminalStations.xml";
  static public List<Station> nexradList = new ArrayList<Station>();
  static public List<Station> terminalList = new ArrayList<Station>();
  static public HashMap<String, Station> nexradMap;
  static public HashMap<String, Station> terminalMap;

  static private TdsContext tdsContext;

  public void setTdsContext( TdsContext tdsContext ) {
    this.tdsContext = tdsContext;
  }

  // must start and end with "/"
  public static String getPath() {
    return "/servers/";
  }

  /*
   * Reads the Radar Server catalog and Radar Station information, called by bean
   */
  public static boolean init() {

    if( cat != null )
        return true;

      String contentPath = tdsContext.getContentDirectory().getPath();

      // read in radarCollections.xml catalog no validation
      InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(false);
      cat = readCatalog(factory, getPath() + catName, contentPath + getPath() + catName);
      if (cat == null) {
        log.info("cat initialization failed" + UsageLog.closingMessageNonRequestContext());
        return false;
      }
      cat.setBaseURI( catURI );
      // get path and location from cat
      List parents = cat.getDatasets();
      for (int i = 0; i < parents.size(); i++) {
        InvDataset top = (InvDataset) parents.get(i);
        List datasets = top.getDatasets(); // dataset scans

        for (int j = 0; j < datasets.size(); j++) {
          InvDatasetScan ds = (InvDatasetScan) datasets.get(j);
          if (ds.getPath() != null) {
            dataLocation.put(ds.getPath(), ds.getScanLocation());
            log.info("path =" + ds.getPath() + " location =" + ds.getScanLocation());
          }
          ds.setXlinkHref(ds.getPath() + "/dataset.xml");
        }
      }
      if( nexradList.size() == 0 ) {
         nexradList = readStations( contentPath + getPath() + nexradStations );
         terminalList = readStations( contentPath + getPath() + terminalStations );
         if( nexradList == null || terminalList == null) {
             log.error( "Station initialization problem using "+
                     contentPath + getPath() + nexradStations +" "+
                     contentPath + getPath() + terminalStations );
             return false;
         }
         nexradMap = getStationMap( nexradList );
         terminalMap = getStationMap( terminalList );
      }
      log.info( "DatasetRepository initialization done -  " + UsageLog.closingMessageNonRequestContext());
      return true;
    }

  /**
   * Reads/stores requested dataset
   * @param key  dataset location
   * @param var  if level3, the var dataset
   * @return   Datset Collection
   */
  public static RadarDatasetCollection getRadarDatasetCollection( String key, String var ) {
    String dmkey = key;
    if( var != null)
      dmkey = key + var;
    RadarDatasetCollection rdc = datasetMap.get( dmkey );
    boolean reread = false;
    if ( rdc != null )
      reread = rdc.previousDayNowAvailable();
    if( rdc == null ||  reread ) { // need to read or reread dataset
      Object sync = new Object();
      synchronized( sync ) {
        if ( reread ) {   // remove dataset
          datasetMap.remove( dmkey );
          rdc = null;
        } else {
          rdc = datasetMap.get( dmkey );
        }
        if( rdc != null )
          return rdc;
        rdc = new RadarDatasetCollection( dataLocation.get( key ), var );
        if( rdc == null || rdc.yyyymmdd.size() == 0 || rdc.hhmm.size() == 0 )
          return null;
        datasetMap.put( dmkey, rdc );
      }
    }
    return rdc;
  }

  /**
   * Removes dataset
   * @param key  dataset location
   * @param var  if level3, the var dataset
   */
  public static void removeRadarDatasetCollection( String key, String var ) {
    Object sync = new Object();
    synchronized( sync ) {
      if( var != null)
        datasetMap.remove( key  + var );
      else
        datasetMap.remove( key);
    }
  }

  public static InvCatalogImpl getRadarCatalog() {
      return cat;
  }

  /**
   * Does the actual work of reading a catalog.
   *
   * @param factory         use this InvCatalogFactory
   * @param path            reletive path starting from content root
   * @param catalogFullPath absolute location on disk
   * @return the InvCatalogImpl, or null if failure
   */
  public static InvCatalogImpl readCatalog(InvCatalogFactory factory, String path, String catalogFullPath) {

    InvCatalogImpl acat;
    try {
      catURI = new URI("file:" + StringUtil.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
    }
    catch (URISyntaxException e) {
      log.info("radarServer readCatalog(): URISyntaxException=" + e.getMessage());
      return null;
    }

    // read the catalog
    log.info("radarServer readCatalog(): full path=" + catalogFullPath + "; path=" + path);
    FileInputStream ios = null;
    try {
      ios = new FileInputStream(catalogFullPath);
      acat = factory.readXML(ios, catURI);
    } catch (Throwable t) {
      log.info("radarServer readCatalog(): Exception on catalog=" +
          catalogFullPath + " " + t.getMessage()); //+"\n log="+cat.getLog(), t);
      return null;
    }
    finally {
      if (ios != null) {
        try {
          ios.close();
        }
        catch (IOException e) {
          log.info("radarServer readCatalog(): error closing" + catalogFullPath);
        }
      }
    }
    return acat;
  }

  /**
   * Returns the stations from a (nexrad|terminal)Stations.xml file
   * @param stnLocation TDS servers location
   * @return
   */
  public static List<Station> readStations( String stnLocation ) {

      List<Station> stationList = new ArrayList<Station>();
      DocumentBuilder parser;
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      SelectStation parent = new SelectStation();  //kludge to use Station
      InputStream is = null;

      try {
          parser = factory.newDocumentBuilder();
          //stnLocation = stnLocation.replaceAll( " ", "%20");
          is = new FileInputStream( stnLocation );
          org.w3c.dom.Document doc = parser.parse(is);
          //System.out.println( "root=" + doc.getDocumentElement().getTagName() );
          NodeList stns = doc.getElementsByTagName("station");
          for (int i = 0; i < stns.getLength(); i++) {
              //System.out.println( "node=" + d.item( i ).getNodeName() );
              NamedNodeMap attr  = stns.item(i).getAttributes();
              String name = "", value = "", state = "", country = "" ;
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
                 if ( child.item(j).getNodeName().equals("location3D")) {
                     NamedNodeMap ca  = child.item(j).getAttributes();
                     String latitude = "", longitude = "", elevation = "" ;
                     for (int k = 0; k < ca.getLength(); k++) {
                         if (ca.item(k).getNodeName().equals("latitude")) {
                             latitude = ca.item(k).getNodeValue();
                         } else if (ca.item(k).getNodeName().equals("longitude")) {
                             longitude = ca.item(k).getNodeValue();
                         } else if (ca.item(k).getNodeName().equals("elevation")) {
                             elevation = ca.item(k).getNodeValue();
                         }

                     }
                     location = new Location(latitude, longitude, elevation, null, null, null );
                 }
              }
              Station station = new Station( parent, name, value, state, country, null );
              station.setLocation( location );
              stationList.add( station );
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
      }
      finally {
        if (is != null) {
          try {
            is.close();
          }
          catch (IOException e) {
            log.error("radarServer getStations(): error closing" + stnLocation);
          }
        }
      }
      return stationList;
  }

  /**
   * creates a HashMap of Stations from a List
   * @param list
   * @return Stations HashMap
   */
  public static HashMap<String, Station> getStationMap( List<Station> list )  {
    HashMap<String, Station> stationMap = new HashMap<String, Station>();
    for (Station station : list) {
      stationMap.put(station.getValue(), station);
    }
    return stationMap;
  }
}
