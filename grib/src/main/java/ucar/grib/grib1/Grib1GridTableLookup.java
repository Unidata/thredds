/*
 * Copyright 1998-2010 University Corporation for Atmospheric Research/Unidata
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
/**
 *
 * By:   Robb Kambic
 * Date: Feb 10, 2009
 *
 */

package ucar.grib.grib1;

import ucar.grid.GridTableLookup;
import ucar.grid.GridDefRecord;
import ucar.grid.GridRecord;
import ucar.grid.GridParameter;
import ucar.grib.GribGridRecord;
import ucar.grib.GribNumbers;
import ucar.grib.NotSupportedException;

public class Grib1GridTableLookup implements GridTableLookup {
  static private org.slf4j.Logger logger =
        org.slf4j.LoggerFactory.getLogger(Grib1GridTableLookup.class);

  /**
   * the ProductDefinitionSection of the first record as a Grib1PDSVariables.
   */
  private final Grib1Pds firstPDSV;

  /**
   * the IdentificationSection of the first record of the Grib file.
   */
  //private final Grib1IndicatorSection firstIS;

  /**
   * Constructor.
   *
   * @param firstRecord in the Grib file
   */
  public Grib1GridTableLookup(Grib1Record firstRecord) {
    this.firstPDSV = firstRecord.getPDS().getPdsVars();
  }

  /**
   * gets the grid name.
   *
   * @param gds GridDefRecord
   * @return GridName
   */
  public final String getGridName(GridDefRecord gds) {
    //return Grib1GridDefinitionSection.getName(
    //    gds.getParamInt(gds.GRID_TYPE));
    return Grib1Tables.getGridName( gds.getParamInt(GridDefRecord.GRID_TYPE) );
  }

  /**
   * gets the ShapeName.
   *
   * @param gds GridDefRecord
   * @return ShapeName
   */
  public final String getShapeName(GridDefRecord gds) {
    return Grib1Tables.getShapeName( gds.getParamInt(GridDefRecord.GRID_SHAPE_CODE) );
  }

  /**
   * gets the DisciplineName.
   *
   * @param gr GridRecord
   * @return DisciplineName
   */
  public final String getDisciplineName(GridRecord gr) {
    // all disciplines are the same in grib1
    return "Meteorological Products";
  }

  /**
   * gets the Category Name.
   *
   * @param gr GridRecord
   * @return Category Name
   */
  public final String getCategoryName(GridRecord gr) {
    // no categories in grib1
    return "Meteorological Parameters";
  }

  /**
   * Get the grid parameter that corresponds to this record
   * gets parameter table, then grib1 parameter based on number.
   *
   * @param gr GridRecord
   * @return Parameter.
   */
  public final GridParameter getParameter(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    try {
      GribPDSParamTable pt;
      pt = GribPDSParamTable.getParameterTable(ggr.getCenter(), ggr.getSubCenter(), ggr.getTableVersion());
      return pt.getParameter(ggr.getParameterNumber());
    } catch (NotSupportedException noSupport) {
      logger.error("Grib1GridTableLookup: Parameter "+ ggr.getParameterNumber() +" not found for center"+
          ggr.getCenter() +" subcenter "+ ggr.getSubCenter() +" table number "+ ggr.getTableVersion());
      logger.error("NotSupportedException : " + noSupport);
      return new GridParameter();
    }
  }

  /**
   * @param gr GridRecord
   * @return result
   */
  public int[] getParameterId(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    int[] result = new int[4];
    result[0] = 1;
    result[1] = ggr.getCenter(); //firstPDS.getCenter();
    result[2] = ggr.getTableVersion(); //firstPDS.getTableVersion();
    result[3] = ggr.getParameterNumber();
    return result;
  }

  /**
   * gets the ProductDefinitionName.
   *
   * @param gr GridRecord
   * @return ProductDefinitionName
   */
  public final String getProductDefinitionName(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return Grib1Tables.getTimeRangeIndicatorName( ggr.getTimeUnit()); // pretty bloody wierd
  }

  /**
   * gets the Source, type and status unknown for Grib1
   * so use Product definition by TimeRangeUnit
   *
   * @return source
   */
  public final String getSource() {
    return  Grib1Tables.getTimeRangeIndicatorName( firstPDSV.getTimeRangeIndicator());

  }

  /**
   * gets the Type of Gen Process Name.
   *
   * @param gr GridRecord
   * @return typeGenProcessName
   */
  public final String getGenProcessName(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return Grib1Tables.getTypeGenProcessName( firstPDSV.getCenter(), ggr.getPds().getGenProcessId() );
  }

  /**
   * gets the LevelName.
   *
   * @param gr GridRecord
   * @return LevelName
   */
  public final String getLevelName(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return GribPDSLevel.getNameShort(ggr.getLevelType1());
  }

  /**
   * gets the LevelDescription.
   *
   * @param gr GridRecord
   * @return LevelDescription
   */
  public final String getLevelDescription(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return GribPDSLevel.getLevelDescription(ggr.getLevelType1());
  }

  /**
   * gets the LevelUnit.
   *
   * @param gr GridRecord
   * @return LevelUnit
   */
  public final String getLevelUnit(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return GribPDSLevel.getUnits(ggr.getLevelType1());
  }

  /**
   * Get the first base time
   *
   * @return FirstBaseTime.
   */
  public final java.util.Date getFirstBaseTime() {
    return firstPDSV.getReferenceDate() ;
  }

  public final String getTimeRangeUnitName( int tunit ) {
    return Grib1Tables.getTimeUnit( firstPDSV.getTimeUnit(), false) ;
  }

  /**
   * gets the CenterName.
   *
   * @return CenterName
   */
  public final String getFirstCenterName() {
    return Grib1Tables.getCenter_idName( firstPDSV.getCenter() )
        +" ("+  Integer.toString( firstPDSV.getCenter() ) +")";
  }

  /**
   * gets the SubcenterId.
   *
   * @return SubcenterId
   */
  public final int getFirstSubcenterId() {
    return firstPDSV.getSubCenter();
  }

  /**
   * gets the Subcenter Name.
   *
   * @return Subcenter Name
   */
  public final String getFirstSubcenterName() {
    String subcenter = Grib1Tables.getSubCenter_idName( firstPDSV.getCenter(), firstPDSV.getSubCenter());
    if( subcenter == null )
      return null;

    return subcenter +" ("+  Integer.toString( firstPDSV.getSubCenter() ) +")";
  }

  /**
   * Institution for CF conventions
   * @return  Institution
   */
    public String getInstitution() {
      String subcenter = getFirstSubcenterName();
      if( subcenter == null ) {
        return "Center "+ getFirstCenterName();
      } else {
        return "Center "+ getFirstCenterName() +" Subcenter "+ subcenter;
      }
    }

  /**
   * gets the ProductStatusName.
   *
   * @return ProductStatusName
   */
  public final String getFirstProductStatusName() {
    // no indicator in grib1, assume Operational
    //return "Operational Products";
    return null;
  }

  /**
   * comment for CF conventions.
   *
   * @return comment
   */
  public final String getComment() {
    // no indicator in grib1, assume Operational
    return null;
  }

  /**
   * gets the ProductTypeName.
   *
   * @return ProductTypeName
   */
  public final String getFirstProductTypeName() {
    // not in grib1, extracting from time range indicator
    return Grib1Tables.getTimeRangeIndicatorName( firstPDSV.getTimeRangeIndicator());
  }

  /**
   * gets the SignificanceOfRTName.
   *
   * @return SignificanceOfRTName
   */
  public final String getFirstSignificanceOfRTName() {
    // not in grib1, assuming start of forecast
    return "Start of forecast";
  }

  /**
   * is this a LatLon grid.
   *
   * @param gds GridDefRecord
   * @return isLatLon
   */
  public final boolean isLatLon(GridDefRecord gds) {
    int grid_type = gds.getParamInt(GridDefRecord.GRID_TYPE);
    return ((grid_type == 0)
        // Guassian
        || (grid_type == 4) || (grid_type == 14)
        || (grid_type == 24) || (grid_type == 34));
  }

  // code table 6
  /**
   * gets the ProjectionType.
   *
   * @param gds GridDefRecord
   * @return ProjectionType
   */
  public final int getProjectionType(GridDefRecord gds) {
    switch (gds.getParamInt(GridDefRecord.GRID_TYPE)) {

      case 1:
        return Mercator;

      case 3:
        return LambertConformal;

      case 4:
        return GaussianLatLon;

      case 5:
        return PolarStereographic;

      case 6:
        return UTM;

      case 8:
        return AlbersEqualArea;

      case 10:
        return RotatedLatLon;
      default:
        return -1;
    }
  }

  /**
   * is this a VerticalCoordinate.
   *
   * @param gr GridRecord
   * @return isVerticalCoordinate
   */
  public final boolean isVerticalCoordinate(GridRecord gr) {

    GribGridRecord ggr = (GribGridRecord) gr;
    int levelType = ggr.getLevelType1();

    if (levelType == 20) {
      return true;
    }
    if (levelType == 100) {
      return true;
    }
    if (levelType == 101) {
      return true;
    }
    if ((levelType >= 103) && (levelType <= 128)) {
      return true;
    }
    if (levelType == 141) {
      return true;
    }
    if (levelType == 160) {
      return true;
    }
    return false;
  }

  /**
   * is this a PositiveUp VerticalCoordinate.
   *
   * @param gr GridRecord
   * @return isPositiveUp
   */
  public final boolean isPositiveUp(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    int levelType = ggr.getLevelType1();

    if (levelType == 103) {
      return true;
    }
    if (levelType == 104) {
      return true;
    }
    if (levelType == 105) {
      return true;
    }
    if (levelType == 106) {
      return true;
    }
    if (levelType == 111) {
      return true;
    }
    if (levelType == 112) {
      return true;
    }
    if (levelType == 125) {
      return true;
    }
    return false;
  }

  /**
   * gets the MissingValue.
   *
   * @return MissingValue
   */
  public final float getFirstMissingValue() {
    return (float) GribNumbers.UNDEFINED;
  }

  /**
   * Check to see if this grid is a layer variable
   *
   * @param gr GridRecord
   * @return true if a layer
   */
  public final boolean isLayer(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    int levelType = ggr.getLevelType1();

    if (levelType == 101) return true;
    if (levelType == 104) return true;
    if (levelType == 106) return true;
    if (levelType == 108) return true;
    if (levelType == 110) return true;
    if (levelType == 112) return true;
    if (levelType == 114) return true;
    if (levelType == 116) return true;
    if (levelType == 120) return true;
    if (levelType == 121) return true;
    if (levelType == 128) return true;
    if (levelType == 141) return true;
    return false;
  }

  // CF Conventions Global Attributes
  /**
   * gets the CF title.
   *
   * @return title
   */
  public final String getTitle() {
    StringBuilder title = new StringBuilder( Grib1Tables.getCenter_idName( firstPDSV.getCenter() ) );
    String model = Grib1Tables.getModelName( firstPDSV.getCenter(), firstPDSV.getGenProcessId() );
    if( model != null ) {
      title.append( " ");
      title.append( model );
    }

    // Next try to get Grid type from WMO Table 6
    if( firstPDSV.getCenter() != 7 ) {
      String grid = Grib1Tables.getGridDefinition( firstPDSV.getGenProcessId() );
      if( grid != null ) {
        title.append( " ");
        title.append( grid );
      }
    }

    String productType = Grib1Tables.getTimeRangeIndicatorName( firstPDSV.getTimeRangeIndicator());
    if( productType != null ) {
      title.append( " ");
      title.append( productType );
    }
    return title.toString();

    /*
    // Try to get Model, only defined for center 7 so far
    String model = Grib1Tables.getModelName( firstPDSV.getCenter(), firstPDSV.getTypeGenProcess() );
    if( model != null )
      return model;
    // Next try to get Grid type from WMO Table 6
    if( firstPDSV.getCenter() != 7 ) {
      model = Grib1Tables.getGridDefinition( firstPDSV.getTypeGenProcess() );
      if( model != null )
        return model;
    }
    // next try product definition name
    return getSource() +" Type GRID data";
    */
  }

  public final String getModel() {
    return Grib1Tables.getModelName( firstPDSV.getCenter(), firstPDSV.getGenProcessId() );
  }

  /**
   * gets the grid type.
   *
   * @return GridType
   */
  public final String getGridType() {
    return "GRIB-1";
  }
}
