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
/**
 * User: rkambic
 * Date: Feb 4, 2009
 * Time: 12:54:03 PM
 */

package ucar.grib.grib2;

import ucar.grib.grib1.Grib1Tables;
import ucar.grid.GridTableLookup;
import ucar.grid.GridParameter;
import ucar.grid.GridDefRecord;
import ucar.grid.GridRecord;

import ucar.grib.GribGridRecord;
import ucar.grib.GribNumbers;

public final class Grib2GridTableLookup implements GridTableLookup {

  static private org.slf4j.Logger logger =
        org.slf4j.LoggerFactory.getLogger(Grib2GridTableLookup.class);
  /**
   * the ProductDefinitionSection of the first record of the Grib file in
   * Grib2PDSVariables format.
   */
  //private final Grib2ProductDefinitionSection firstPDS;
  private final Grib2PDSVariables firstPDSV;

  /**
   * the IdentificationSection of the first record of the Grib file.
   */
  private final Grib2IdentificationSection firstID;

  /**
   * the Local use section of the first record of the Grib file.
   */
  //private final Grib2LocalUseSection firstLUS;

  /**
   * the DataRepresentationSection of the first record of the Grib file.
   */
  private final Grib2DataRepresentationSection firstDRS;

  /**
   * Constructor.
   *
   * @param firstRecord in the Grib file
   */
  public Grib2GridTableLookup(Grib2Record firstRecord) {
    this.firstPDSV = firstRecord.getPDS().getPdsVars();
    this.firstID = firstRecord.getId();
    //this.firstLUS = firstRecord.getLUS();
    this.firstDRS = firstRecord.getDRS();
  }


  /**
   * gets the grid type.
   *
   * @param gds Grib2GridDefinitionSection
   * @return GridName
   */
  public final String getGridName(GridDefRecord gds) {
    return Grib2Tables.codeTable3_1(gds.getParamInt(GridDefRecord.GRID_TYPE));
  }

  /**
   * gets the ShapeName.
   *
   * @param gds Grib2GridDefinitionSection
   * @return ShapeName
   */
  public final String getShapeName(GridDefRecord gds) {
    return Grib2Tables.codeTable3_2(gds.getParamInt(GridDefRecord.GRID_SHAPE_CODE));
  }


  /**
   * gets the DisciplineName.
   *
   * @param gr GridRecord
   * @return DisciplineName
   */
  public final String getDisciplineName(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return ParameterTable.getDisciplineName(ggr.discipline);
  }

  /**
   * gets the CategoryName.
   *
   * @param gr GridRecord
   * @return CategoryName
   */
  public final String getCategoryName(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return ParameterTable.getCategoryName(ggr.discipline, ggr.category);
  }

  /**
   * Get the grid parameter that corresponds to this record
   * Check for local parameters, default is NCEP so check it first.
   * The NCEP local parameters are included in the main table with the WMO ones.
   *
   * @param gr record to check
   * @return Parameter.
   */
  public GridParameter getParameter(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    // NCEP is default, table has all parameters even local ones > 191
    if (firstID.getCenter_id() == 7 ||
        ggr.paramNumber < 192 && ggr.category < 192 &&  ggr.discipline  < 192)
      return ParameterTable.getParameter(ggr.discipline, ggr.category, ggr.paramNumber);
    else  { // get local parameter for center
      return ParameterTable.getParameter(ggr.discipline, ggr.category, ggr.paramNumber, firstID.getCenter_id());
    }
  }

  /**
   * @param gr GridRecord
   * @return int[] representing the parameter discipline, category, and parmeter number
   */
  public int[] getParameterId(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    int[] result = new int[4];
    result[0] = 2;
    result[1] = ggr.discipline;
    result[2] = ggr.category;
    result[3] = ggr.paramNumber;
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
    return Grib2Tables.codeTable4_0(ggr.productTemplate);
  }

  /**
   * gets the ProductDefinition.
   *
   * @param gr GridRecord
   * @return ProductDefinition
   */
  public final int getProductDefinition(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return ggr.productTemplate;
  }

  /**
   * gets the Source, Generating Process or Model.
   *
   * @return source
   */
  public final String getSource() {
    return "Type: "+ firstID.getProductTypeName() +" Status: "+ firstID.getProductStatusName();
  }

  /**
   * gets the  Type of Gen Process Name.
   *
   * @param gr GridRecord
   * @return typeGenProcessName
   */
  public final String getTypeGenProcessName(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return Grib2Tables.codeTable4_3(ggr.typeGenProcess);
  }
  public final String getTypeGenProcessName() {
    return Grib2Tables.codeTable4_3(firstPDSV.getTypeGenProcess());
  }

  /**
   * gets the  Type of Gen Process.
   *
   * @param gr GridRecord
   * @return typeGenProcessName
   */
  public final int getTypeGenProcess(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return ggr.typeGenProcess;
  }
  /**
   * Ensemble type variable.
   * @param gr GridRecord
   * @return boolean
   */
  public final boolean isEnsemble(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return (ggr.typeGenProcess == 4 || ggr.typeGenProcess == 193) ;
  }

  /**
   * gets the number of forecast in Ensemble, Derived, or Probability.
   * @param gr GridRecord
   * @return String  number of forecasts as a String
   */
  public final String NumberOfForecastsInEnsemble(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return Integer.toString( ggr.numberForecasts );
  }

  /**
   * Make the ensemble name
   *
   * @param gr     grid record
   * @param lookup lookup table
   * @return name for the level
   */
  public final String makeEnsembleName(GridRecord gr, GridTableLookup lookup) {
    if ( ! (lookup instanceof Grib2GridTableLookup) )
      return "";
    return makeSuffix(gr);
  }

  /**
   * gets the  isProbability type var.
   * @param gr GridRecord
   * @return boolean
   */
  public final boolean isProbability(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return (ggr.typeGenProcess == 5 || ggr.typeGenProcess == 10 ) ;
  }


  /**
   * gets the number of forecasts in Probability.
   * @param gr GridRecord
   * @return String  number of forecasts as a String
   */
  public final String NumberOfForecastsInProbability(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return Integer.toString( ggr.numberForecasts );
  }

  /**
   * Make the probability name
   *
   * @param gr     grid record
   * @param lookup lookup table
   * @return name for the level
   */
  public final String makeProbabilityName(GridRecord gr, GridTableLookup lookup) {
    if ( ! (lookup instanceof Grib2GridTableLookup) )
      return "";
    return makeSuffix(gr);
  }

  /**
   * Makes a Ensemble, Derived, Probability or error Suffix
   * @param gr GridRecord
   * @return suffix
   */
  public String makeSuffix( GridRecord gr ) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return ggr.makeSuffix( );
  }

  /**
   * gets the LevelName.
   *
   * @param gr GridRecord
   * @return LevelName
   */
  public final String getLevelName(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return Grib2Tables.getTypeSurfaceNameShort(ggr.levelType1);
  }

  /**
   * gets the LevelDescription.
   *
   * @param gr GridRecord
   * @return LevelDescription
   */
  public final String getLevelDescription(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return Grib2Tables.codeTable4_5(ggr.levelType1);
  }

  /**
   * gets the LevelUnit.
   *
   * @param gr GridRecord
   * @return LevelUnit
   */
  public final String getLevelUnit(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return Grib2Tables.getTypeSurfaceUnit(ggr.levelType1);
  }

  /**
   * Get the first base time
   *
   * @return FirstBaseTime.
   */
  public final java.util.Date getFirstBaseTime() {
    return firstID.getBaseTime();
  }

  /**
   * gets the TimeRangeUnitName.
   *
   * @return TimeRangeUnitName
   */
  public final String getTimeRangeUnitName( int tunit ) {
    return Grib2Tables.getTimeUnitFromTable4_4( tunit );
  }

  /**
   * gets the TimeRangeUnitName.  Not always correct
   * @deprecate
   *
   * @return TimeRangeUnitName
   */
  public final String getFirstTimeRangeUnitName() {
    return Grib2Tables.getTimeUnitFromTable4_4(firstPDSV.getTimeRangeUnit());
  }

  /**
   * gets the CenterName.
   *
   * @return CenterName
   */
  public final String getFirstCenterName() {
    //return Grib1Tables.getCenter_idName( firstID.getCenter_id() );
    return Grib1Tables.getCenter_idName( firstID.getCenter_id() ) +" ("+
        Integer.toString( firstID.getCenter_id() ) +")";
  }

  /**
   * gets the SubcenterId.
   *
   * @return SubcenterId
   */
  public final int getFirstSubcenterId() {
    return firstID.getSubcenter_id();
  }

  /**
   * gets the Subcenter Name, dependant on center.
   *
   * @return Subcenter Name
   */
  public final String getFirstSubcenterName() {
    //return Grib1Tables.getSubCenter_idName( firstID.getCenter_id(), firstID.getSubcenter_id());
    String subcenter = Grib1Tables.getSubCenter_idName( firstID.getCenter_id(), firstID.getSubcenter_id());
    if( subcenter == null ) {
      return null;
    } else {
      return subcenter +" ("+  Integer.toString( firstID.getSubcenter_id() ) +")";
    }
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
    return firstID.getProductStatusName();
  }

  /**
   * comment for CF conventions.
   *
   * @return comment
   */
  public final String getComment() {
    //return firstID.getProductStatusName();
    return null;
  }

  /**
   * gets the ProductTypeName.
   *
   * @return ProductTypeName
   */
  public final String getFirstProductTypeName() {
    return firstID.getProductTypeName();
  }

  /**
   * gets the CF title.
   *
   * @return title
   */
  public final String getTitle() {
    StringBuilder title = new StringBuilder( Grib1Tables.getCenter_idName( firstID.getCenter_id() ) );
    String model = Grib1Tables.getModelName(firstID.getCenter_id(), firstPDSV.getAnalysisGenProcess());;
    if( model != null ) {
      title.append( " ");
      title.append( model );
    }
    String productType = firstID.getProductTypeName();
    if( productType != null ) {
      title.append( " ");
      title.append( productType );
    }
    return title.toString();
  }

  /**
   * gets the SignificanceOfRTName.
   *
   * @return SignificanceOfRTName
   */
  public final String getFirstSignificanceOfRTName() {
    return firstID.getSignificanceOfRTName();
  }

  /**
   * is this a LatLon grid.
   *
   * @param gds GridDefRecord
   * @return isLatLon
   */
  public final boolean isLatLon(GridDefRecord gds) {
    int grid_type = gds.getParamInt(GridDefRecord.GRID_TYPE);
    return (grid_type == 0)
        || ((grid_type >= 40) && (grid_type < 44));
  }

  // code table 3.1
  /**
   * gets the ProjectionType.
   *
   * @param gds GridDefRecord
   * @return ProjectionType
   */
  public final int getProjectionType(GridDefRecord gds) {
    return Grib2Tables.getProjectionType(gds.getParamInt(GridDefRecord.GRID_TYPE) );
  }

  /**
   * is this a VerticalCoordinate.
   *
   * @param gr GridRecord
   * @return isVerticalCoordinate
   */
  public final boolean isVerticalCoordinate(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    if (ggr.levelType1 == 105)
      return true;

    String units = getLevelUnit(gr);
    if ((units == null) || (units.length() == 0)) {
      return false;
    }
    if (ggr.levelType1 == 0) {
      return false;
    }
    if (ggr.levelType1 == 104) {
      return false;
    }
//    if (ggr.levelType1 == 105) {
//      return false;
//    }
    return true;
  }

  /**
   * is this a PositiveUp VerticalCoordinate.
   *
   * @param gr GridRecord
   * @return isPositiveUp
   */
  public final boolean isPositiveUp(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    if ((ggr.levelType1 == 20) || (ggr.levelType1 == 100)
        || (ggr.levelType1 == 106) || (ggr.levelType1 == 160)) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * gets the MissingValue. Grib Package is written to use Float.NaN.
   *
   * @return MissingValue
   */
  public final float getFirstMissingValue() {
    if( Grib2DataSection.isStaticMissingValueInUse() ) {
      return Float.NaN;
    } else if (firstDRS.getMissingValueManagement() == 0 ) {
      return Float.NaN;
    } else if (firstDRS.getMissingValueManagement() == 1 ) {
      return firstDRS.getPrimaryMissingValue();
    } else if (firstDRS.getMissingValueManagement() == 2 ) {
      return firstDRS.getSecondaryMissingValue();
    } else {
      return GribNumbers.UNDEFINED; // punt
    }
  }

  /**
   * Check to see if this grid is a layer variable
   *
   * @param gr record to check
   * @return true if a layer
   */
  public final boolean isLayer(GridRecord gr) {
    //if (gr.getLevelType1() == 0) return false;
    if (gr.getLevelType2() == 255 || gr.getLevelType2() == 0)
      return false;
    return true;
  }

  /**
   *  is this GridRecord an Interval
   *
   * @param gr GridRecord
   * @return isInterval
   */
  public final boolean isInterval(GridRecord gr) {
    GribGridRecord ggr = (GribGridRecord) gr;
    return ggr.isInterval();
  }

  /**
   * gets the Number of Bands in the radar PDS.
   *
   * @return NB int
   */
  public final int getNB() {
    return firstPDSV.getNB();
  }

  /**
   * gets the Satellite Series in the radar PDS.
   *
   * @return series1 int[]
   */
  public final int[] getSatelliteSeries() {
    return firstPDSV.getSatelliteSeries();
  }

  /**
   * gets the Satellite in the radar PDS.
   *
   * @return satellite int[]
   */
  public final int[] getSatellite() {
    return firstPDSV.getSatellite();
  }

  /**
   * gets the Satellite Instrument in the radar PDS.
   *
   * @return Instrument int[]
   */
  public final int[] getSatelliteInstrument() {
    return firstPDSV.getSatelliteInstrument();
  }

  /**
   * gets the Satellite Wave in the radar PDS.
   *
   * @return series2 int[]
   */
  public final float[] getSatelliteWave() {
    return firstPDSV.getSatelliteWave();
  }

  /**
   * gets the grid type.
   *
   * @return GridType
   */
  public final String getGridType() {
    return "GRIB-2";
  }
}
