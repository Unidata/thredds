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

package ucar.nc2.grib.grib1;

import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.grib1.tables.*;

/**
 * Interprets the raw grib1 info in a way that may be customized.
 *
 * @author caron
 * @since 1/13/12
 */
public class Grib1Customizer implements GribTables {

  static public Grib1Customizer factory(Grib1Record proto) {
    int center = proto.getPDSsection().getCenter();
    int subcenter = proto.getPDSsection().getSubCenter();
    int version = proto.getPDSsection().getTableVersion();
    return factory(center, subcenter, version);
  }

  static public Grib1Customizer factory(int center, int subcenter, int version) {
    if (center == 7) return new NcepTables();
    else if (center == 9) return new NcepRfcTables();
    else return new Grib1Customizer();
  }

  ///////////////////////////////////////
  private Grib1Tables tables;
  private int center, subcenter, version;

  protected Grib1Customizer() {
    this.tables = new Grib1Tables();
    this.center = center;
    this.subcenter = subcenter;
    this.version = version;
  }

  public Grib1Parameter getParameter(int center, int subcenter, int tableVersion, int param_number) {
    return tables.getParameter(center, subcenter, tableVersion, param_number);
  }

  public String getTypeGenProcessName(int genProcess) {
    return null;
  }

  public String getSubCenterName(int center, int subcenter) {
    return tables.getSubCenterName(center, subcenter);
  }

  /////////////////////////////////////////
  // level

  public Grib1ParamLevel getParamLevel(Grib1SectionProductDefinition pds) {
    return new Grib1ParamLevel(this, pds);
  }

  public VertCoord.VertUnit makeVertUnit(int code) {
    return Grib1LevelTypeTable.getLevelUnit(code);
  }

  public boolean isLayer(Grib1SectionProductDefinition pds) {
    return Grib1LevelTypeTable.isLayer(pds.getLevelType());
  }

  public boolean isPositiveUp(Grib1SectionProductDefinition pds) {
    return Grib1LevelTypeTable.isPositiveUp(pds.getLevelType());
  }


  @Override
  public String getLevelNameShort(int levelType) {
    return Grib1LevelTypeTable.getNameShort(levelType);
  }

  public String getLevelDescription(int levelType) {
    return Grib1LevelTypeTable.getLevelDescription(levelType);
  }

  public String getLevelUnits(int levelType) {
    return Grib1LevelTypeTable.getUnits(levelType);
  }

  ///////////////////////////////////////////////////
  // time

  public Grib1ParamTime getParamTime(Grib1SectionProductDefinition pds) {
    return new Grib1ParamTime(this, pds);
  }

  public String getTimeTypeName(int timeRangeIndicator, int p1, int p2) {
    return Grib1TimeTypeTable.getTimeTypeName(timeRangeIndicator, p1, p2);
  }

  public Grib1TimeTypeTable.StatType getStatType(int timeRangeIndicator) {
    return Grib1TimeTypeTable.getStatType(timeRangeIndicator);
  }
}
