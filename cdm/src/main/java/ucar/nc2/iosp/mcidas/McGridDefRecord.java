/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
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


package ucar.nc2.iosp.mcidas;


import edu.wisc.ssec.mcidas.*;
import ucar.grid.GridDefRecord;

//import ucar.nc2.iosp.grid.*;


/**
 * Class to hold the grid navigation information.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.3 $
 */
public class McGridDefRecord extends GridDefRecord {

    /** raw values */
    int[] vals = null;

    /** projection type */
    private String proj;

    /** Navigation type for pseudo-mercator grids */
    private static final int PSEUDO_MERCATOR = 1;

    /** Navigation type for polar stero or lambert conformal conic grids */
    private static final int PS_OR_LAMBERT_CONIC = 2;

    /** Navigation type for equidistant grids */
    private static final int EQUIDISTANT = 3;

    /** Navigation type for pseudo-mercator (general case) grids */
    private static final int PSEUDO_MERCATOR_GENERAL = 4;

    /** no navigation */
    private static final int NO_NAV = 5;

    /** Navigation type for lambert conformal tangent grids */
    private static final int LAMBERT_CONFORMAL_TANGENT = 6;

    /** Earth radius */
    private static final double EARTH_RADIUS = 6371.23;

    /**
     * Create a new grid nav block
     */
    public McGridDefRecord() {}

    /**
     * Create a new grid nav block with the values
     *
     * @param words   analysis block values
     */
    public McGridDefRecord(int[] words) {
        setValues(words);
    }

    /**
     * Set the grid nav block values
     *
     * @param values   the raw values
     */
    public void setValues(int[] values) {
        vals = values;
        setParams();
        addParam(GDS_KEY, this.toString());
    }

    /**
     * Print out a string representation of this
     *
     * @return  a String representation of this.
     */
    public String toString() {
        // TODO: make this more unique
        StringBuffer buf = new StringBuffer(getParam(PROJ));
        buf.append(" X:");
        buf.append(getParam(NX));
        buf.append(" ");
        buf.append("Y:");
        buf.append(getParam(NY));
        return buf.toString();
    }

    /**
     * Check for equality
     *
     * @param o  object to compare
     *
     * @return true if equal
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ( !(o instanceof McGridDefRecord)) {
            return false;
        }
        return this.toString().equals(o.toString());
    }

    /**
     * Get the hashcode
     *
     * @return the hashcode
     */
    public int hashCode() {
        return toString().hashCode();
    }

    /* common calculation variables
    private int navType;
    private double xnr;             // number of rows
    private double xnc;             // number of columns
    private double xnrow;           // number of rows for calculations
    private double xncol;           // number of columns for calculations
    private boolean wierd = false;  // JTYP = 1, navType > 10
    */

    /* Merc and pseudo_merc parameters
    private double glamx;           // max latitude
    private double glomx;           // max longitude
    private double ginct;           // grid increment in latitude
    private double gincn;           // grid increment in longitude
    */

    /* PS and CONF projection parameters
    private double xrowi;           // row # of North Pole*10000
    private double xcoli;           // column # of North Pole* 10000
    private double xqlon;           // longitude parallel to columns
    private double xspace;          // column spacing at standard latitude
    private double xh;              //
    private double xfac;            //
    private double xblat;           //
    */

    /* Equidistant params
    private double xrot;            // rotation angle
    private double yspace;
    private double xblon;
    */

    /** static debug count */
    private static int cnt = 0;

    /**
     * Set the parameters for the GDS
     */
    private void setParams() {

        /* PS and CONF projection parameters */
        double xrowi;   // row # of North Pole*10000
        double xcoli;   // column # of North Pole* 10000
        double xqlon;   // longitude parallel to columns
        double xspace;  // column spacing at standard latitude
        double yspace;  // row spacing at standard latitude
        double xt1;     // standard latitude
        double xt2;     // 2nd standard latitude
        double xh;      // 
        double xfac;    //
        double xblat;   //
        /* Merc and pseudo_merc parameters */
        double glamx;  // max latitude
        double glomx;  // max longitude
        double glamn;  // min latitude
        double glomn;  // min longitude
        double ginct;  // grid increment in latitude
        double gincn;  // grid increment in longitude

        try {
            GRIDnav nav      = new GRIDnav(vals);

            int     gridType = vals[33];
            int     navType  = (int) gridType % 10;
            addParam("Proj", String.valueOf(navType));
            boolean wierd = gridType / 10 == 1;
            int     ny    = vals[1];
            int     nx    = vals[2];
            addParam(PROJ, getProjName(navType));
            addParam(NX, String.valueOf(nx));
            addParam(NY, String.valueOf(ny));
            double[][] input = new double[2][2];
            if (nav.isFlippedRowCoordinates()) {
                input = new double[][] {
                    { 1, nx }, { 1, ny }
                };
            } else {
                input = new double[][] {
                    { 1, nx }, { ny, 1 }
                };
            }
            double[][] llur = nav.toLatLon(input);
            /*
            if (cnt == 0 && navType == 6) {
                System.out.println("Proj = " + getProjName(navType) + ":" + navType);
                System.out.println("isFlipped = " + nav.isFlippedRowCoordinates());
                ucar.unidata.util.Misc.printArray("input x", input[0]);
                ucar.unidata.util.Misc.printArray("input y", input[1]);
                ucar.unidata.util.Misc.printArray("lats", llur[0]);
                ucar.unidata.util.Misc.printArray("lons", llur[1]);
                cnt++;
            }
            */
            addParam(LA1, String.valueOf(llur[0][0]));
            addParam(LO1, String.valueOf(llur[1][0]));
            addParam(LA2, String.valueOf(llur[0][1]));
            addParam(LO2, String.valueOf(llur[1][1]));

            switch (navType) {

              case PSEUDO_MERCATOR :
              case PSEUDO_MERCATOR_GENERAL :
                  glamx = vals[34] / 10000.;
                  glomx = -vals[35] / 10000.;
                  glamn = vals[34] / 10000.;
                  glomn = -vals[35] / 10000.;
                  ginct = vals[38] / 10000.;
                  gincn = (navType == PSEUDO_MERCATOR_GENERAL)
                          ? vals[39] / 10000.
                          : ginct;
                  addParam("Latin", String.valueOf(20));
                  //addParam(DX, String.valueOf(gincn));
                  //addParam(DY, String.valueOf(ginct));
                  /*
                  if (wierd) {
                    double x = xnr;
                    xnr = xnc;
                    xnc = x;
                  }
                  */
                  break;

              case PS_OR_LAMBERT_CONIC :
                  xrowi  = vals[34] / 10000.;  // row # of the North pole*10000
                  xcoli  = vals[35] / 10000.;  // col # of the North pole*10000
                  xspace = vals[36];  // column spacing at standard lat (m)
                  xqlon = -vals[37] / 10000.;  // lon parallel to cols (deg*10000)
                  xt1 = vals[38] / 10000.;  // first standard lat
                  xt2 = vals[39] / 10000.;  // second standard lat
                  addParam(LATIN1, String.valueOf(xt1));
                  addParam(LOV, String.valueOf(xqlon));
                  addParam(LATIN2, String.valueOf(xt2));
                  addParam(DX, String.valueOf(xspace));
                  addParam(DY, String.valueOf(xspace));
                  /*
                  xh = (xt1 >= 0) ? 1. : -1.;
                  xt1 =(90.-xh*xt1)*xrad;
                  xt2 =(90.-xh*xt2)*xrad;
                  xfac =1.0;
                  if (xt1 != xt2)
                     xfac = (Math.log(Math.sin(xt1))-Math.log(Math.sin(xt2)))/
                            (Math.log(Math.tan(.5*xt1))-Math.log(Math.tan(.5*xt2)));
                  xfac = 1.0/xfac;
                  xblat = 6370. * Math.sin(xt1)/
                           (xspace*xfac*(Math.pow(Math.tan(xt1*.5),xfac)));
                  if (wierd) {
                     double x=xnr;
                     xnr=xnc;
                     xnc=x;
                     x=xcoli;
                     xcoli=xrowi;
                     xrowi=xnr-x+1.0;
                     xqlon=xqlon+90.;
                  }
                  */

                  break;

              case EQUIDISTANT :
                  xrowi = 1.;
                  xcoli = 1.;
                  glamx = vals[34] / 10000.;   // lat of (1,1) degrees*10000
                  glomx = -vals[35] / 10000.;  // lon of (1,1) degrees*10000
                  //xrot  = -xrad*vals[36]/10000.; // clockwise rotation of col 1
                  xspace = vals[37] / 1000.;   // column spacing
                  yspace = vals[38] / 1000.;   // row spacing
                  addParam(LA1, String.valueOf(glamx));
                  addParam(LO1, String.valueOf(glomx));
                  addParam(DX, String.valueOf(xspace));
                  addParam(DY, String.valueOf(yspace));
                  /*
                  xblat = EARTH_RADIUS*xrad/yspace;
                  xblon = EARTH_RADIUS*xrad/xspace;

                  if (wierd) {
                    double x = xnr;
                    xnr = xnc;
                    xnc = x;
                  }
                  */

                  break;

              case LAMBERT_CONFORMAL_TANGENT :
                  xrowi  = vals[34] / 10000.;  // lat at (1,1)
                  xcoli  = vals[35] / 10000.;  // lon at (1,1)
                  xspace = vals[36];  // column spacing at standard lat (m)
                  xqlon = -vals[37] / 10000.;  // lon parallel to cols (deg*10000)
                  xt1 = vals[38] / 10000.;  // standard lat 1
                  xt2 = vals[39] / 10000.;  // standard lat 2
                  if ((xt2 == McIDASUtil.MCMISSING) || (xt2 == 0)) {
                      xt2 = xt1;
                  }
                  addParam(LATIN1, String.valueOf(xt1));
                  addParam(LOV, String.valueOf(xqlon));
                  addParam(LATIN2, String.valueOf(xt2));
                  addParam(DX, String.valueOf(xspace));
                  addParam(DY, String.valueOf(xspace));
                  /*
                  xh = (xt1 >= 0) ? 1. : -1.;
                  xt1 = (90. - xh * xt1) * xrad;
                  xfac = Math.cos(xt1);
                  xblat = EARTH_RADIUS * Math.tan(xt1) /
                             (xspace * Math.pow(Math.tan(xt1*.5), xfac));
                             */

                  break;

              default :
                  break;
            }
        } catch (McIDASException me) {
            System.out.println("couldn't set nav");
        }

    }

    /**
     * Get a short name for this GDSKey for the netCDF group.
     * Subclasses should implement as a short description
     * @return short name
     */
    public String getGroupName() {
        StringBuffer buf = new StringBuffer();
        buf.append(getParam(PROJ));
        buf.append("_");
        buf.append(getParam(NX));
        buf.append("x");
        buf.append(getParam(NY));
        return buf.toString();
    }

    /**
     * Get the name for the projection type
     *
     * @param type the projection type
     *
     * @return  a String name for the type
     */
    public String getProjName(int type) {
        String projName = "UNKN";
        switch (type) {

          case PSEUDO_MERCATOR :
          case PSEUDO_MERCATOR_GENERAL :
              projName = "MERC";
              break;

          case PS_OR_LAMBERT_CONIC :
              projName = (vals[38] == vals[39])
                         ? "PS"
                         : "CONF";
              break;

          case EQUIDISTANT :
              projName = "EQUI";
              break;

          case LAMBERT_CONFORMAL_TANGENT :
              projName = "CONF";
              break;

          default :
              projName = "NAV" + type;
        }
        return projName;

    }

}

