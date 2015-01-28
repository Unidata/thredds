/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.grib.grib1.tables;

import ucar.nc2.grib.GribLevelType;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.grib1.Grib1ParamLevel;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Describe
 *
 * @author caron
 * @since 1/27/2015
 */
public class JmaTables extends Grib1Customizer {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JmaTables.class);

  private static Map<Integer, GribLevelType> levelTypesMap;  // shared by all instances

  JmaTables(Grib1ParamTables tables) {
    super(34, tables);
  }

  /// levels
  @Override
  public Grib1ParamLevel getParamLevel(Grib1SectionProductDefinition pds) {
    int levelType = pds.getLevelType();
    int pds11 = pds.getLevelValue1();
    int pds12 = pds.getLevelValue2();
    int pds1112 = pds11 << 8 | pds12;

    switch (levelType) {
      case 211:
      case 212:
        return new Grib1ParamLevel(this, levelType, GribNumbers.MISSING, GribNumbers.MISSING);

      case 100:
         return new Grib1ParamLevel(this, levelType, pds1112, GribNumbers.MISSING);

      case 213:
         return new Grib1ParamLevel(this, levelType, pds1112, GribNumbers.MISSING);

       default:
        return new Grib1ParamLevel(this, pds);
    }
  }

  protected GribLevelType getLevelType(int code) {
    if (levelTypesMap == null) makeLevelTypesMap();
    GribLevelType levelType = levelTypesMap.get(code);
    if (levelType != null) return levelType;
    return super.getLevelType(code);
  }

  static private void makeLevelTypesMap() {
    levelTypesMap = new HashMap<>(10);
                // (int code, String desc, String abbrev, String units, String datum, boolean isPositiveUp, boolean isLayer)
    levelTypesMap.put(100, new GribLevelType(100, "Isobaric Surface", "isobaric_surface_low", "hPa", null, false, false));   // 3D
    levelTypesMap.put(211, new GribLevelType(211, "Entire soil", "entire_soil", "", null, false, false));
    levelTypesMap.put(212, new GribLevelType(212, "The bottom of land surface model", "bottom_of_model", "", null, false, false));
    levelTypesMap.put(213, new GribLevelType(213, "Underground layer number of land surface model", "underground_layer", "layer", null, false, false));   // 3D
   }

}
