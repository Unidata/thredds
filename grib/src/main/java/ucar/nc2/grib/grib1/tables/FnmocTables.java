/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1.tables;

import javax.annotation.Nullable;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.grib.GribResourceReader;
import ucar.nc2.grib.coord.VertCoordType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FNMOC local tables
 *
 * @author caron
 * @since 1/14/12
 */
public class FnmocTables extends Grib1Customizer {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FnmocTables.class);
  private static final String fnmocTableA = "resources/grib1/fnmoc/US058MMTA-ALPdoc.pntabs-prodname-masterModelTableOrdered.GRIB1.TblA.xml";
  private static final String fnmocTable2 = "resources/grib1/fnmoc/US058MMTA-ALPdoc.pntabs-prodname-masterParameterTableOrdered.GRIB1.Tbl2.xml";
  private static final String fnmocTable3 = "resources/grib1/fnmoc/US058MMTA-ALPdoc.pntabs-prodname-masterLevelTypeTableOrdered.GRIB1.Tbl3.xml";

  private static Map<Integer, VertCoordType> levelTypesMap;  // shared by all instances
  private static Map<Integer, String> genProcessMap;  // shared by all instances

  FnmocTables(Grib1ParamTables tables) {
    super(58, tables);
  }

  // genProcess

  @Override
  @Nullable
  public String getGeneratingProcessName(int genProcess) {
    if (genProcessMap == null)
      genProcessMap = readGenProcess(fnmocTableA);
    if (genProcessMap == null) return null;
    return genProcessMap.get(genProcess);
  }

  /*
  <fnmocTable>
    <entry>
      <grib1Id>0004</grib1Id>
      <fnmocId>0004</fnmocId>
      <name>MISC_GRIDS</name>
      <fullName>Miscellaneous Grids</fullName>
      <description>atmospheric model</description>
      <status>current</status>
    </entry>
    <entry>
      <grib1Id>0008</grib1Id>
      <fnmocId>0008</fnmocId>
      <name>STRATO</name>
      <fullName>NOGAPS Stratosphere Functions</fullName>
      <description>atmospheric stratosphere model</description>
      <status>current</status>
    </entry>
   */
  @Nullable
  private Map<Integer, String> readGenProcess(String path) {
    try (InputStream is = GribResourceReader.getInputStream(path)) {
      SAXBuilder builder = new SAXBuilder();
      org.jdom2.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      Map<Integer, String> result = new HashMap<>(200);
      Element fnmocTable = root.getChild("fnmocTable");
      List<Element> params = fnmocTable.getChildren("entry");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getChildText("grib1Id"));
        String desc = elem1.getChildText("fullName");
        result.put(code, desc);
      }

      return Collections.unmodifiableMap(result);  // all at once - thread safe

    } catch (IOException | JDOMException e) {
      logger.error("Cant read FNMOC Table 1 = " + path, e);
      return null;
    }
  }


  /// levels
  protected VertCoordType getLevelType(int code) {
    if (levelTypesMap == null)
      levelTypesMap = readFnmocTable3(fnmocTable3);
    if (levelTypesMap == null)
      return super.getLevelType(code);

    VertCoordType levelType = levelTypesMap.get(code);
    if (levelType != null) return levelType;
    return super.getLevelType(code);
  }

  /*
      <entry>
      <grib1Id>222</grib1Id>
      <fnmocId>mid_cld</fnmocId>
      <name>mid_cld</name>
      <description>middle cloud layer</description>
      <status>deprecated</status>
    </entry>

  <fnmocTable>
    <entry>
      <grib1Id>001</grib1Id>
      <fnmocId>surface</fnmocId>
      <name>surface</name>
      <description>ground or water surface (the atmosphere's lower boundary, land/sea surface)</description>
      <status>current</status>
    </entry>
   */
  @Nullable
  private HashMap<Integer, VertCoordType> readFnmocTable3(String path) {
    try (InputStream is = GribResourceReader.getInputStream(path)) {
      SAXBuilder builder = new SAXBuilder();
      org.jdom2.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      HashMap<Integer, VertCoordType> result = new HashMap<>(200);
      Element fnmocTable = root.getChild("fnmocTable");
      List<Element> params = fnmocTable.getChildren("entry");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getChildText("grib1Id"));
        if (code < 129) continue;
        String desc = elem1.getChildText("description");
        String abbrev = elem1.getChildText("name");
        String units = elem1.getChildText("units");
        if (units == null) units = (code == 219) ? "Pa" : "";
        String datum = elem1.getChildText("datum");
        boolean isLayer = elem1.getChild("isLayer") != null;
        boolean isPositiveUp = elem1.getChild("isPositiveUp")  != null;
        VertCoordType lt = new VertCoordType(code, desc, abbrev, units, datum, isPositiveUp, isLayer);
        result.put(code, lt);
      }

      return result;  // all at once - thread safe

    } catch (IOException | JDOMException e) {
      logger.error("Cant read FnmocTable3 = " + path, e);
      return null;
    }
  }

}
