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

package ucar.nc2.iosp.grib;

import ucar.grib.GribGridDefRecord;
import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.iosp.grid.*;
import ucar.nc2.units.SimpleUnit;
import ucar.grib.grib2.Grib2GridTableLookup;
import ucar.grib.grib1.Grib1GridTableLookup;
import ucar.grib.grib1.Grib1GDSVariables;

import java.util.*;

/**
 * A Vertical Coordinate variable for a Grib variable.
 *
 * @author caron
 */
public class GribVertCoord extends GridVertCoord {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribVertCoord.class);

  /**
   * Create a new GribVertCoord with the given name.
   * Used by deprecated GridIndex2NC.makeDefinedCoord()
   *
   * @param name name
   */
  GribVertCoord(String name) {
    super(name);
  }

  /**
   * Create a new GribVertCoord with the appropriate params
   *
   * @param records   list of GribRecords that make up this coord
   * @param levelName the name of the level
   * @param lookup    the lookup table
   * @param hcs Horizontal coordinate
   */
  GribVertCoord(List<GridRecord> records, String levelName, GridTableLookup lookup, GridHorizCoordSys hcs) {
    super(records, levelName, lookup, hcs) ;

    // chck for pressure records
    if (typicalRecord.getLevelType1() == 109 && lookup instanceof Grib1GridTableLookup ) {
      GridDefRecord gdr = hcs.getGds();
      Grib1GDSVariables g1dr = (Grib1GDSVariables) ((GribGridDefRecord) gdr).getGdsv();  // LOOK
      if( g1dr == null || ! g1dr.hasVerticalPressureLevels() )
        return;

      // add hybrid numbers
      coordValues = new double[ levels.size()];
        for (int i = 0; i < levels.size(); i++ ) {
          LevelCoord lc = levels.get( i );
          coordValues[ i ] =   lc.value1  ;
        }
      int NV = g1dr.getNV();
      // add new variables
      if (  NV > 2 && NV < 255 ) { // Some data doesn't add Pressure Level values
         factors = g1dr.getVerticalPressureLevels();
      }

    }

  }

  @Override
  protected String getLevelDesc() {
    String desc = lookup.getLevelDescription(typicalRecord);
    if (lookup instanceof Grib2GridTableLookup && usesBounds) {
      desc = "Layer between " + desc;
    }
    return desc;
  }

  @Override
  protected void addExtraAttributes(Variable v) {
    v.addAttribute(new Attribute("GRIB_level_type", Integer.toString(typicalRecord.getLevelType1())));
  }

}
