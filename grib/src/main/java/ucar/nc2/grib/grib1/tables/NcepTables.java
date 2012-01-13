/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib1.tables;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import ucar.grib.GribResourceReader;
import ucar.nc2.grib.grib1.Grib1Customizer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * NCEP overrides of GRIB tables
 *
 * @author caron
 * @since 1/13/12
 */
public class NcepTables extends Grib1Customizer {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NcepTables.class);

  private static HashMap<Integer, String> genProcessMap;  // shared by all instances
  private static HashMap<Integer, LevelType> levelTypesMap;  // shared by all instances

  public NcepTables() {
    super();
  }

  public String getTypeGenProcessName(int genProcess) {
    if (genProcessMap == null) readNcepGenProcess("resources/grib1/ncep/ncepTableA.xml");
    if (genProcessMap == null) return null;
    return genProcessMap.get(genProcess);
  }

  private void readNcepGenProcess(String path) {
    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) {
        logger.error("Cant find NCEP Table 1 = " + path);
        return;
      }

      SAXBuilder builder = new SAXBuilder();
      org.jdom.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      HashMap<Integer, String> result = new HashMap<Integer, String>(200);
      List<Element> params = root.getChildren("parameter");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description");
        result.put(code, desc);
      }

      genProcessMap = result;  // all at once - thread safe
      return;

    } catch (IOException ioe) {
      logger.error("Cant read NCEP Table 1 = " + path, ioe);
      return;

    } catch (JDOMException e) {
      logger.error("Cant parse NCEP Table 1 = " + path, e);
      return;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }
  }

  @Override
  public String getLevelNameShort(int code) {
    LevelType lt = getLevelType(code);
    return (lt == null) ? super.getLevelNameShort(code) : lt.abbrev;
  }

  @Override
  public String getLevelDescription(int code) {
    LevelType lt = getLevelType(code);
    return (lt == null) ? super.getLevelNameShort(code) : lt.desc;
  }

  @Override
  public String getLevelUnits(int code) {
    LevelType lt = getLevelType(code);
    return (lt == null) ? super.getLevelNameShort(code) : lt.units;
  }

  private LevelType getLevelType(int code) {
    if (levelTypesMap == null) readNcepLevelTypes("resources/grib1/ncep/ncepTable3.xml");
    if (levelTypesMap == null) return null;
    return levelTypesMap.get(code);
  }

  private void readNcepLevelTypes(String path) {
    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) {
        logger.error("Cant find NCEP Table 1 = " + path);
        return;
      }

      SAXBuilder builder = new SAXBuilder();
      org.jdom.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      HashMap<Integer, LevelType> result = new HashMap<Integer, LevelType>(200);
      List<Element> params = root.getChildren("parameter");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description");
        String abbrev = elem1.getChildText("abbrev");
        String units = elem1.getChildText("units");
        result.put(code, new LevelType(code, desc, abbrev, units));
      }

      levelTypesMap = result;  // all at once - thread safe
      return;

    } catch (IOException ioe) {
      logger.error("Cant read NCEP Table 1 = " + path, ioe);
      return;

    } catch (JDOMException e) {
      logger.error("Cant parse NCEP Table 1 = " + path, e);
      return;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }
  }

  private class LevelType {
    int code;
    String desc;
    String abbrev;
    String units;

    private LevelType(int code, String desc, String abbrev, String units) {
      this.code = code;
      this.desc = desc;
      this.abbrev = abbrev;
      this.units = units;
    }

  }

}
