/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Collections;

import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;
import org.jdom.Element;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.util.xml.RuntimeConfigParser;

/**
 * Read and process the threddsConfig.xml file
 */
public class ThreddsConfig {
  private static javax.servlet.ServletContext _context;
  private static String _filename;
  private static org.slf4j.Logger log;
  private static Element rootElem;

  //private static HashMap paramHash;
  private static List<String> catalogRoots;
  private static List<String> contentRootList;

  public static void init(javax.servlet.ServletContext context, String filename, org.slf4j.Logger log) {
    _context = context;
    _filename = filename;

    readConfig( log);
  }

  static void readConfig(org.slf4j.Logger log) {
    //paramHash = new HashMap();
    catalogRoots = new ArrayList<String>();
    contentRootList = new ArrayList<String>();

    File file = new File(_filename);
    if (!file.exists()) return;
    log.debug("ThreddsConfig: reading xml file = " + _filename);

    org.jdom.Document doc;
    try {
      InputStream is = new FileInputStream(_filename);
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(is);
    } catch (IOException e) {
      log.error("ThreddsConfig: incorrectly formed xml file " + _filename, e);
      return;
    } catch (JDOMException e) {
      log.error("ThreddsConfig: incorrectly formed xml file " + _filename, e);
      return;
    }
    rootElem = doc.getRootElement();

    /* context-param : may override the ones in web.xml
    List paramList = rootElem.getChildren("context-param");
    for (int j = 0; j < paramList.size(); j++) {
      Element paramElem = (Element) paramList.get(j);
      String name = paramElem.getChildText("param-name");
      String value = paramElem.getChildText("param-value");
      if ((name == null) || (value == null)) {
        log.error("ThreddsConfig: incorrectly formed context-param " + name + " " + value);
        continue;
      }
      paramHash.put(name, value);
      //System.out.println("param= "+ name + " " + value);
    } */

    List<Element> rootList = rootElem.getChildren("catalogRoot");
    for (Element catrootElem : rootList) {
      String location = catrootElem.getText().trim();
      if (location.length() > 0) {
        catalogRoots.add(location);
        log.debug("ThreddsConfig: adding catalogRoot = " + location);
      }
    }

    Element contentRootsElem = rootElem.getChild( "contentRoots" );
    List<Element> contentRootElemList = contentRootsElem.getChildren( "contentRoot" );
    for ( Element curRoot : contentRootElemList )
    {
      String location = curRoot.getTextNormalize();
      if ( ! location.isEmpty() )
      {
        contentRootList.add( location );
        log.debug( "ThreddsConfig: adding contentRoot [" + location + "]." );
      }
    }

    // viewer plug-in
    List<Element> viewerList = rootElem.getChildren("Viewer");
    for (Element elem : viewerList) {
      String className = elem.getText().trim();
      ViewServlet.registerViewer(className);
    }

    // datasetSource plug-in
    List<Element> sourceList = rootElem.getChildren("datasetSource");
    for (Element elem : sourceList) {
      String className = elem.getText().trim();
      DatasetHandler.registerDatasetSource(className);
    }

    // nj22 runtime loading
    Element elem = rootElem.getChild("nj22Config");
    if (elem != null) {
      StringBuilder errlog = new StringBuilder();
      RuntimeConfigParser.read( elem, errlog);
      if (errlog.length() > 0)
        log.warn(errlog.toString());
    }

  }


  static void getCatalogRoots(List<String> extraList) {
    extraList.addAll( catalogRoots);
  }

  public static List<String> getContentRootList()
  {
    return Collections.unmodifiableList( contentRootList);
  }

  /* static public String getInitParameter(String name, String defaultValue) {
    if (null != paramHash.get(name))
      return (String) paramHash.get(name);

    String value = _context.getInitParameter(name);
    return (value == null) ? defaultValue : value;
  } */

  static public String get(String paramName, String defValue) {
    String s = getParam( paramName);
    return (s == null) ? defValue : s;
  }

  static public boolean hasElement(String paramName) {
    Element elem = rootElem;
    if (elem == null) return false;
    StringTokenizer stoke = new StringTokenizer(paramName, ".");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken();
      elem = elem.getChild(toke);
      if (null == elem)
        return false;
    }
    return true;    
  }

  static public boolean getBoolean(String paramName, boolean defValue) {
    String s = getParam( paramName);
    if (s == null) return defValue;

    try {
      return Boolean.parseBoolean(s);
    } catch (Exception e) {
      log.error("ThreddsConfig: param "+paramName+" not a boolean: " + e.getMessage());
    }
    return defValue;
  }

  static public long getBytes(String paramName, long defValue) {
    String s = getParam(paramName);
    if (s == null) return defValue;

    String num = s;
    try {
      long factor = 1;
      int pos = s.indexOf(' ');
      if (pos > 0) {
        num = s.substring(0, pos);
        String units = s.substring(pos + 1).trim();

        char c = Character.toUpperCase(units.charAt(0));
        if (c == 'K') factor = 1000;
        else if (c == 'M') factor = 1000 * 1000;
        else if (c == 'G') factor = 1000 * 1000 * 1000;
        else if (c == 'T') factor = ((long)1000) * 1000 * 1000 * 1000;
        else if (c == 'P') factor = ((long)1000) * 1000 * 1000 * 1000 * 1000;
      }

      return factor * Long.parseLong(num);

    } catch (Exception e) {
      log.error("ThreddsConfig: param " + paramName + " not a byte count: " + s+" "+e.getMessage());
    }
    return defValue;
  }

  static public int getInt(String paramName, int defValue) {
    String s = getParam( paramName);
    if (s == null) return defValue;

    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      log.error("ThreddsConfig: param "+paramName+" not an integer " + e.getMessage());
    }
    return defValue;
  }

  static public int getSeconds(String paramName, int defValue) {
    String s = getParam( paramName);
    if (s == null) return defValue;

    try {
      TimeUnit tu = new TimeUnit(s);
      return (int) tu.getValueInSeconds();
    } catch (Exception e) {
      log.error("ThreddsConfig: param "+paramName+" not udunit time " + e.getMessage());
    }
    return defValue;
  }

  private static String getParam( String name) {
    Element elem = rootElem;
    if (elem == null) return null;
    StringTokenizer stoke = new StringTokenizer(name, ".");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken();
      elem = elem.getChild(toke);
      if (null == elem)
        return null;
    }
    return elem.getText();
  }

}
