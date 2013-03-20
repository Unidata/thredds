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

package thredds.tdm;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.jdom2.input.SAXBuilder;
import org.jdom2.JDOMException;
import org.jdom2.Element;
import org.springframework.util.StringUtils;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.util.xml.RuntimeConfigParser;

/**
 * Duplicate class from thredds.servlet
 * Read and process the threddsConfig.xml file.
 * You can access the values by calling ThreddsConfig.getXXX(name1.name2), where
 * <pre>
 *  <name1>
 *   <name2>value</name2>
 *  </name1>
 * </pre>
 */
public class ThreddsConfig {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ThreddsConfig.class);
  private Element rootElem;

  private List<String> catalogRoots;
  private List<String> contentRootList;

  public ThreddsConfig(File threddsConfigXml) {
    if (!threddsConfigXml.exists())
      throw new IllegalArgumentException("ThreddsConfig " + threddsConfigXml + " does not exist");

    catalogRoots = new ArrayList<String>();
    contentRootList = new ArrayList<String>();

    System.out.println("ThreddsConfig:INFO: reading xml file = " + threddsConfigXml);

    org.jdom2.Document doc;
    try {
      InputStream is = new FileInputStream(threddsConfigXml);
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(is);

    } catch (IOException e) {
      throw new IllegalArgumentException("ThreddsConfig:ERROR: incorrectly formed xml file [" + threddsConfigXml + "]: " + e.getMessage());

    } catch (JDOMException e) {
      throw new IllegalArgumentException("ThreddsConfig:ERROR: incorrectly formed xml file [" + threddsConfigXml + "]: " + e.getMessage());
    }
    rootElem = doc.getRootElement();

    List<Element> rootList = rootElem.getChildren("catalogRoot");
    for (Element catrootElem : rootList) {
      String location = StringUtils.cleanPath(catrootElem.getTextNormalize());
      if (location.length() > 0) {
        catalogRoots.add(location);
        System.out.println("ThreddsConfig:INFO: adding catalogRoot = " + location);
      }
    }

    Element contentRootsElem = rootElem.getChild("contentRoots");
    if (contentRootsElem != null) {
      List<Element> contentRootElemList = contentRootsElem.getChildren("contentRoot");
      for (Element curRoot : contentRootElemList) {
        String location = StringUtils.cleanPath(curRoot.getTextNormalize());
        if (!location.isEmpty()) {
          contentRootList.add(location);
          System.out.println("ThreddsConfig:INFO: adding contentRoot [" + location + "].");
        }
      }
    }

    /* viewer plug-in
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
    } */

    // nj22 runtime loading
    Element elem = rootElem.getChild("nj22Config");
    if (elem != null) {
      StringBuilder errlog = new StringBuilder();
      RuntimeConfigParser.read(elem, errlog);
      if (errlog.length() > 0)
        System.out.println("ThreddsConfig:WARN: " + errlog.toString());
    }
  }

  void addCatalogRoots(List<String> extraList) {
    extraList.addAll(catalogRoots);
  }

  public String get(String paramName, String defValue) {
    String s = getParam(paramName);
    return (s == null) ? defValue : s;
  }

  public boolean hasElement(String paramName) {
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

  public boolean getBoolean(String paramName, boolean defValue) {
    String s = getParam(paramName);
    if (s == null) return defValue;

    try {
      return Boolean.parseBoolean(s);
    } catch (Exception e) {
      log.error("ThreddsConfig: param " + paramName + " not a boolean: " + e.getMessage());
    }
    return defValue;
  }

  public long getBytes(String paramName, long defValue) {
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
        else if (c == 'T') factor = ((long) 1000) * 1000 * 1000 * 1000;
        else if (c == 'P') factor = ((long) 1000) * 1000 * 1000 * 1000 * 1000;
      }

      return factor * Long.parseLong(num);

    } catch (Exception e) {
      log.error("ThreddsConfig: param " + paramName + " not a byte count: " + s + " " + e.getMessage());
    }
    return defValue;
  }

  public int getInt(String paramName, int defValue) {
    String s = getParam(paramName);
    if (s == null) return defValue;

    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      log.error("ThreddsConfig: param " + paramName + " not an integer " + e.getMessage());
    }
    return defValue;
  }

  public int getSeconds(String paramName, int defValue) {
    String s = getParam(paramName);
    if (s == null) return defValue;

    try {
      TimeDuration tu = new TimeDuration(s);
      return (int) tu.getValueInSeconds();
    } catch (Exception e) {
      log.error("ThreddsConfig: param " + paramName + " not udunit time " + e.getMessage());
    }
    return defValue;
  }

  private String getParam(String name) {
    Element elem = rootElem;
    if (elem == null) return null;
    StringTokenizer stoke = new StringTokenizer(name, ".");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken();
      elem = elem.getChild(toke);
      if (null == elem)
        return null;
    }
    String text = elem.getText();
    return (text == null) ? null : text.trim();
  }

}
