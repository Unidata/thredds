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

package ucar.grib.grib2;

import ucar.grib.*;
import ucar.grib.Index;
import ucar.grib.grib1.Grib1Tables;
import ucar.grid.GridParameter;

/**
 * lookup functions for Grib2 files
 *
 * @deprecated
 */

public final class Grib2Lookup implements ucar.grib.TableLookup {

  /**
   * the ProductDefinitionSection of the first record of the Grib file.
   */
  private final Grib2ProductDefinitionSection firstPDS;

  /**
   * the IdentificationSection of the first record of the Grib file.
   */
  private final Grib2IdentificationSection firstID;

  /**
   * the DataRepresentationSection of the first record of the Grib file.
   */
  private final Grib2DataRepresentationSection firstDRS;

  /**
   * Constructor.
   *
   * @param firstRecord in the Grib file
   */
  public Grib2Lookup(Grib2Record firstRecord) {
    this.firstPDS = firstRecord.getPDS();
    this.firstID = firstRecord.getId();
    this.firstDRS = firstRecord.getDRS();
  }


  public final String getGridName(Index.GdsRecord gds) {
    return Grib2GridDefinitionSection.getGridName(gds.grid_type);
  }


  public final String getShapeName(Index.GdsRecord gds) {
    return Grib2GridDefinitionSection.getShapeName(gds.grid_shape_code);
  }

  /**
   * gets parameter based on discipline, category, and number.
   *
   * @param gr Index.GribRecord
   * @return Parameter
   */
  public final GridParameter getParameter(GribGridRecord gr) {
    return ParameterTable.getParameter(gr.discipline, gr.category, gr.paramNumber);
  }

  public final GridParameter getParameter(Index.GribRecord gr) {
    GridParameter gd = ParameterTable.getParameter(gr.discipline, gr.category, gr.paramNumber);
    return new GridParameter(gd.getNumber(), gd.getName(), gd.getDescription(), gd.getUnit());
    //  return (Parameter) ParameterTable.getParameter(gr.discipline, gr.category, gr.paramNumber);
  }


  public int[] getParameterId(Index.GribRecord gr) {
    int[] result = new int[4];
    result[0] = 2;
    result[1] = gr.discipline;
    result[2] = gr.category;
    result[3] = gr.paramNumber;
    return result;
  }


  public final String getDisciplineName(Index.GribRecord gr) {
    return ParameterTable.getDisciplineName(gr.discipline);
  }


  public final String getCategoryName(Index.GribRecord gr) {
    return ParameterTable.getCategoryName(gr.discipline, gr.category);
  }


  public final String getProductDefinitionName(Index.GribRecord gr) {
    return Grib2Tables.codeTable4_0(gr.productType);
  }


  public final String getTypeGenProcessName(Index.GribRecord gr) {
    return Grib2Tables.getTypeGenProcessName(gr.typeGenProcess);
  }

  /*
   * gets the  Type Ensemble.
   *
   * @param gr
   * @return TypeEnsemble
   *
  public final int getTypeEnsemble(Index.GribRecord gr) {
    return Grib2Tables.getTypeEnsemble(gr.typeGenProcess);
  } */


  public final String getLevelName(Index.GribRecord gr) {
    return Grib2Tables.getTypeSurfaceNameShort( gr.levelType1);
  }


  public final String getLevelDescription(Index.GribRecord gr) {
    return Grib2Tables.codeTable4_5( gr.levelType1);
  }

  public final String getLevelUnit(Index.GribRecord gr) {
    return Grib2Tables.getTypeSurfaceUnit( gr.levelType1);
  }


  public final String getFirstTimeRangeUnitName() {
    return Grib2Tables.codeTable4_4(firstPDS.timeRangeUnit);
  }


  public final String getFirstCenterName() {
    return Grib1Tables.getCenter_idName(firstID.getCenter_id());
  }


  public final int getFirstSubcenterId() {
    return firstID.getSubcenter_id();
  }


  public final String getFirstProductStatusName() {
    return firstID.getProductStatusName();
  }


  public final String getFirstProductTypeName() {
    return firstID.getProductTypeName();
  }


  public final String getFirstSignificanceOfRTName() {
    return firstID.getSignificanceOfRTName();
  }


  public final java.util.Date getFirstBaseTime() {
    return firstID.getBaseTime();
  }


  public final boolean isLatLon(Index.GdsRecord gds) {
    return (gds.grid_type == 0) || ((gds.grid_type >= 40) && (gds.grid_type < 44));
  }

  // code table 3.1

  public final int getProjectionType(Index.GdsRecord gds) {
    switch (gds.grid_type) {
      case 1:
        return RotatedLatLon;

      case 10:
        return Mercator;

      case 20:
        return PolarStereographic;

      case 30:
        return LambertConformal;

      case 31:
        return AlbersEqualArea;

      case 40:
        return GaussianLatLon;

      case 90:
        return Orthographic;

      default:
        return -1;
    }
  }

  public final boolean isVerticalCoordinate(Index.GribRecord gr) {

    //TODO: fix for all levels
    if (gr.levelType1 == 105) {
      return true;
    }
    String units = getLevelUnit(gr);
    if ((units == null) || (units.length() == 0)) {
      return false;
    }
    if (gr.levelType1 == 0) {
      return false;
    }
    if (gr.levelType1 == 104) {
      return false;
    }
    if (gr.levelType1 == 105) {
      return false;
    }
    return true;
  }


  public final boolean isPositiveUp(Index.GribRecord gr) {
    if ((gr.levelType1 == 20) || (gr.levelType1 == 100)
            || (gr.levelType1 == 106) || (gr.levelType1 == 160)) {
      return false;
    } else {
      return true;
    }
  }


  public final float getFirstMissingValue() {
    return firstDRS.getPrimaryMissingValue();
  }

}