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
import org.springframework.util.StringUtils;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.util.xml.RuntimeConfigParser;

/**
 * Read and process the threddsConfig.xml file.
 * You can access the values by calling ThreddsConfig.getXXX(name1.name2), where
 * <pre>
 *  <name1>
 *   <name2>value</name2>
 *  </name1>
 * </pre>
 */
public class ThreddsConfig {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( ThreddsConfig.class );
  private static String _filename;
  private static Element rootElem;

  private static List<String> catalogRoots;
  private static List<String> contentRootList;

  public static void init( String filename) {
    _filename = filename;

    readConfig();
  }

  static void readConfig() {
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

    List<Element> rootList = rootElem.getChildren("catalogRoot");
    for (Element catrootElem : rootList) {
      String location = StringUtils.cleanPath( catrootElem.getTextNormalize() );
      if (location.length() > 0) {
        catalogRoots.add( location );
        log.debug("ThreddsConfig: adding catalogRoot = " + location);
      }
    }

    Element contentRootsElem = rootElem.getChild( "contentRoots" );
    if ( contentRootsElem != null )
    {
      List<Element> contentRootElemList = contentRootsElem.getChildren( "contentRoot" );
      for ( Element curRoot : contentRootElemList )
      {
        String location = StringUtils.cleanPath( curRoot.getTextNormalize() );
        if ( ! location.isEmpty() )
        {
          contentRootList.add( location );
          log.debug( "ThreddsConfig: adding contentRoot [" + location + "]." );
        }
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
    String text =  elem.getText();
    return (text == null) ? null : text.trim();
  }

}
