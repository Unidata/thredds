// $Id: ThreddsConfig.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
import java.util.HashMap;
import java.util.ArrayList;

import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;
import org.jdom.Element;

/**
 * Read and process the threddsConfig.xml file
 */
public class ThreddsConfig {
  private static javax.servlet.ServletContext _context;
  private static String _filename;

  private static HashMap paramHash;
  private static ArrayList catalogRoots;

  static void init(javax.servlet.ServletContext context, String filename, org.slf4j.Logger log) {
    _context = context;
    _filename = filename;

    readConfig( log);
  }

  static void readConfig(org.slf4j.Logger log) {
    paramHash = new HashMap();
    catalogRoots = new ArrayList();

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

    // context-param : may override the ones in web.xml
    Element rootElem = doc.getRootElement();
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
    }

    List rootList = rootElem.getChildren("catalogRoot");
    for (int j = 0; j < rootList.size(); j++) {
      Element catrootElem = (Element) rootList.get(j);
      String location = catrootElem.getText().trim();
      if (location.length() > 0) {
        catalogRoots.add(location);
        log.debug("ThreddsConfig: adding catalogRoot = " + location);
      }
    }

    // nj22 runtime loading
    List viewerList = rootElem.getChildren("viewer");
    for (int j = 0; j < viewerList.size(); j++) {
      Element elem = (Element) viewerList.get(j);
      String className = elem.getText().trim();
      ViewServlet.registerViewer(className);
    }

    // nj22 runtime loading
    Element elem = rootElem.getChild("runtimeConfig");
    if (elem != null) {
      StringBuffer errlog = new StringBuffer();
      ucar.nc2.util.RuntimeConfigParser.read( elem, errlog);
      if (errlog.length() > 0)
        log.warn(errlog.toString());
    }

  }


  static void getCatalogRoots(List extraList) {
    extraList.addAll( catalogRoots);
  }

  static public String getInitParameter(String name, String defaultValue) {
    if (null != paramHash.get(name))
      return (String) paramHash.get(name);

    String value = _context.getInitParameter(name);
    return (value == null) ? defaultValue : value;
  }

}
