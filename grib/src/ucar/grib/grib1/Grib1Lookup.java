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

// $Id: Grib1Lookup.java,v 1.28 2006/08/04 17:59:51 rkambic Exp $

package ucar.grib.grib1;


import ucar.grib.*;
import ucar.grib.Index;
import ucar.grib.Parameter;
import ucar.grid.GridParameter;


/**
 * Grib1Lookup.java
 * get all the information about a Grib file.
 * @deprecated
 */
public final class Grib1Lookup implements ucar.grib.TableLookup {

    /**
     * the Grib1ProductDefinitionSection of the first record of Grib file.
     */
    private final Grib1ProductDefinitionSection firstPDS;

    /**
     * gets the first PDS out of a Grib file.
     * @param firstProduct sets the PDS
     */
    public Grib1Lookup(Grib1Product firstProduct) {
        this.firstPDS = firstProduct.getPDS();
    }

    /**
     * gets the grid type.
     * @param gds
     * @return GridName
     */
    public final String getGridName(Index.GdsRecord gds) {
        return Grib1GridDefinitionSection.getName(gds.grid_type);
    }

    /**
     * gets the ShapeName.
     * @param gds
     * @return ShapeName
     */
    public final String getShapeName(Index.GdsRecord gds) {
        return Grib1GridDefinitionSection.getShapeName(gds.grid_shape_code);
    }

    /**
     * gets parameter table, then grib1 parameter based on number.
     * @param gr Index.GribRecord
     * @return Parameter
     */
    public final GridParameter getParameter(Index.GribRecord gr) {
        // gets parameter table, then grib1 parameter based on number
        try {
            GribPDSParamTable pt;
            if( gr.center == 0 ) {
                pt = GribPDSParamTable.getParameterTable(firstPDS.getCenter(),
                    firstPDS.getSubCenter(), firstPDS.getTableVersion());
            } else {
                 pt = GribPDSParamTable.getParameterTable(gr.center,gr.subCenter, gr.table);
            }
            return pt.getParameter(gr.paramNumber);
        } catch (NotSupportedException noSupport) {
            System.err.println("NotSupportedException : " + noSupport);
        }
        return null;
    }

    /**
     * _more_
     *
     * @param gr _more_
     *
     * @return _more_
     */
    public int[] getParameterId(Index.GribRecord gr) {
        int[] result = new int[4];
        result[0] = 1;
        result[1] = firstPDS.getCenter();
        result[2] = firstPDS.getTableVersion();
        result[3] = gr.paramNumber;
        return result;
    }

    /**
     * gets the DisciplineName.
     * @param  gr
     * @return DisciplineName
     */
    public final String getDisciplineName(Index.GribRecord gr) {
        // all disciplines are the same in grib1
        return "Meteorological Products";
    }

    /**
     * gets the CategoryName.
     * @param  gr
     * @return CategoryName
     */
    public final String getCategoryName(Index.GribRecord gr) {
        // no categories in grib1
        return "Meteorological Parameters";
    }

    /**
     * gets the ProductDefinitionName.
     * @param  gr
     * @return ProductDefinitionName
     */
    public final String getProductDefinitionName(Index.GribRecord gr) {
        return Grib1ProductDefinitionSection.getProductDefinitionName(
            gr.productType);
    }

    /**
     * gets the  Type of Gen Process Name.
     * @param  gr
     * @return  typeGenProcessName
     */
    public final String getTypeGenProcessName(Index.GribRecord gr) {
        return Grib1ProductDefinitionSection.getTypeGenProcessName(
            gr.typeGenProcess);
    }

    /**
     * gets the LevelName.
     * @param  gr
     * @return LevelName
     */
    public final String getLevelName(Index.GribRecord gr) {
        return GribPDSLevel.getNameShort(gr.levelType1);
    }

    /**
     * gets the LevelDescription.
     * @param  gr
     * @return LevelDescription
     */
    public final String getLevelDescription(Index.GribRecord gr) {
        return GribPDSLevel.getLevelDescription(gr.levelType1);  // LOOK should get real description !!
    }

    /**
     * gets the LevelUnit.
     * @param  gr
     * @return LevelUnit
     */
    public final String getLevelUnit(Index.GribRecord gr) {
        return GribPDSLevel.getUnits(gr.levelType1);
    }

    /**
     * gets the TimeRangeUnitName.
     * @return TimeRangeUnitName
     */
    public final String getFirstTimeRangeUnitName() {
        return firstPDS.getTimeUnit();
    }

    /**
     * gets the CenterName.
     * @return CenterName
     */
    public final String getFirstCenterName() {
        return firstPDS.getCenter_idName();
    }

    /**
     * gets the SubcenterId.
     * @return SubcenterId
     */
    public final int getFirstSubcenterId() {
        return firstPDS.getSubCenter();
    }

    /**
     * gets the ProductStatusName.
     * @return ProductStatusName
     */
    public final String getFirstProductStatusName() {
        // no indicator in grib1, assume Operational
        //return "Operational Products"; 
        return null;
    }

    /**
     * gets the ProductTypeName.
     * @return ProductTypeName
     */
    public final String getFirstProductTypeName() {
        // not in grib1, extracting form time range indicator
        return Grib1ProductDefinitionSection.getProductDefinitionName(
            firstPDS.getTimeRange());
    }

    /**
     * gets the SignificanceOfRTName.
     * @return SignificanceOfRTName
     */
    public final String getFirstSignificanceOfRTName() {
        // not in grib1, assuming start of forecast
        return "Start of forecast";
    }

    /**
     * gets the BaseTime Forecastime.
     * @return BaseTime
     */
    public final java.util.Date getFirstBaseTime() {
        return firstPDS.getBaseTime();
    }

    /**
     * is this a LatLon grid.
     * @param  gds
     * @return isLatLon
     */
    public final boolean isLatLon(Index.GdsRecord gds) {
      return ((gds.grid_type == 0)
          // Guassian
          || (gds.grid_type == 4) || (gds.grid_type == 14)
          || (gds.grid_type == 24) || (gds.grid_type == 34));
    }

    // table 6

    /**
     * gets the ProjectionType.
     * @param  gds
     * @return ProjectionType
     */
    public final int getProjectionType(Index.GdsRecord gds) {
        switch (gds.grid_type) {

          case 1 :
              return Mercator;

          case 3 :
              return LambertConformal;

          case 4 :
              return GaussianLatLon;

          case 5 :
              return PolarStereographic;

          case 6 :
              return UTM;

          case 8 :
              return AlbersEqualArea;

          case 10 :
              return RotatedLatLon;
          default :
              return -1;
        }
    }

    /**
     * is this a VerticalCoordinate.
     * @param  gr
     * @return isVerticalCoordinate
     */
    public final boolean isVerticalCoordinate(Index.GribRecord gr) {
        if (gr.levelType1 == 20) {
            return true;
        }
        if (gr.levelType1 == 100) {
            return true;
        }
        if (gr.levelType1 == 101) {
            return true;
        }
        if ((gr.levelType1 >= 103) && (gr.levelType1 <= 128)) {
            return true;
        }
        if (gr.levelType1 == 141) {
            return true;
        }
        if (gr.levelType1 == 160) {
            return true;
        }
        return false;
    }

    /**
     * is this a PositiveUp VerticalCoordinate.
     * @param  gr
     * @return isPositiveUp
     */
    public final boolean isPositiveUp(Index.GribRecord gr) {
        if (gr.levelType1 == 103) {
            return true;
        }
        if (gr.levelType1 == 104) {
            return true;
        }
        if (gr.levelType1 == 105) {
            return true;
        }
        if (gr.levelType1 == 106) {
            return true;
        }
        if (gr.levelType1 == 111) {
            return true;
        }
        if (gr.levelType1 == 112) {
            return true;
        }
        if (gr.levelType1 == 125) {
            return true;
        }
        return false;
    }

    /**
     * gets the MissingValue.
     * @return MissingValue
     */
    public final float getFirstMissingValue() {
        return (float) GribNumbers.UNDEFINED;
    }

}  // end Grib1Lookup

/*
 *  Change History:
 *  $Log: Grib1Lookup.java,v $
 *  Revision 1.28  2006/08/04 17:59:51  rkambic
 *  update
 *
 *  Revision 1.27  2006/08/03 22:33:40  rkambic
 *  typeGenProcess
 *
 *  Revision 1.26  2006/04/20 22:16:14  caron
 *  add int[] getParameterId()
 *
 *  Revision 1.25  2005/12/08 21:00:05  rkambic
 *  code analysis
 *
 *  Revision 1.24  2005/11/17 23:27:17  rkambic
 *  analysis
 *
 *  Revision 1.23  2005/11/09 18:28:05  rkambic
 *  intellij analysis
 *
 *  Revision 1.22  2005/11/05 20:07:31  caron
 *  add new projections (Albers, Orthographic) to table lookups
 *
 *  Revision 1.21  2005/03/03 01:15:16  caron
 *  improve isVerticalCoordinate
 *
 *  Revision 1.20  2005/02/09 22:35:16  rkambic
 *  now uses description table for levels
 *
 *  Revision 1.19  2005/02/08 22:28:48  rkambic
 *  level/tables fixes
 *
 *  Revision 1.18  2005/02/04 00:22:33  caron
 *  Grib1Lookup.isLatLon was hosed
 *
 *  Revision 1.17  2005/02/01 22:05:03  caron
 *  getLevelName, getLevelDescription in Lookup
 *
 *  Revision 1.16  2005/01/26 22:59:50  rkambic
 *  gds fixes
 *
 *  Revision 1.15  2005/01/19 19:53:36  caron
 *  no message
 *
 *  Revision 1.14  2005/01/18 17:19:39  rkambic
 *  exception created for missing tables
 *
 *  Revision 1.13  2005/01/14 21:25:50  caron
 *  add Mercator
 *
 *  Revision 1.12  2004/12/10 23:39:09  caron
 *  no message
 *
 *  Revision 1.11  2004/11/19 16:41:16  rkambic
 *  now using Parameter.java
 *
 *  Revision 1.10  2004/11/11 23:33:39  rkambic
 *  level stuff
 *
 *  Revision 1.9  2004/11/09 21:42:07  rkambic
 *  offsets for GDS and PDS
 *
 *  Revision 1.8  2004/11/03 20:15:27  caron
 *  fix missing values
 *
 *  Revision 1.7  2004/10/25 15:58:08  caron
 *   empty log message
 *
 *  Revision 1.6  2004/10/22 19:31:51  caron
 *  no message
 *
 *  Revision 1.5  2004/10/21 16:52:05  rkambic
 *  grib1 changes for itegration into nj22
 *
 *  Revision 1.4  2004/10/19 19:46:04  rkambic
 *  support fo Grib1Lookup
 *
 *  Revision 1.3  2004/10/15 22:09:11  rkambic
 *  raf stuff
 *
 *  Revision 1.2  2004/10/14 19:34:51  rkambic
 *  changed exceptions
 *  remove gini iosp until isValidFile() works
 *
 *  Revision 1.1  2004/10/13 19:48:08  caron
 *  add strict NCDump
 *
 */

