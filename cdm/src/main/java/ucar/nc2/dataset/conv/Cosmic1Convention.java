/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.dataset.conv;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.ma2.*;

import java.io.IOException;

/**
 * Cosmic data - version 1.
 * Add time coordinate from global atts start_time, stop_time, assuming its linear along the vertical dimension.
 *
 * @author caron
 * @since Jul 29, 2009
 */
public class Cosmic1Convention extends CoordSysBuilder {

  /**
   * @param ncfile the NetcdfFile to test
   * @return true if we think this is a Zebra file.
   */
  public static boolean isMine(NetcdfFile ncfile) {

    //   :start_time = 9.17028312E8; // double
   // :stop_time = 9.170284104681826E8; // double

    if (null == ncfile.findDimension("MSL_alt") && null == ncfile.findDimension("time")) return false;
   // if (null == ncfile.findGlobalAttribute( "start_time")) return false;
   // if (null == ncfile.findGlobalAttribute( "stop_time")) return false;

    String center = ncfile.findAttValueIgnoreCase(null, "center", null);
    return (center != null) && center.equals("UCAR/CDAAC");
  }

  public Cosmic1Convention() {
    this.conventionName = "Cosmic1";
  }

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    Attribute leoAtt = ds.findGlobalAttribute("leoId");

    if( leoAtt == null ) {
        if (ds.findVariable("time") == null) {
          // create a time variable - assume its linear along the vertical dimension
          double start =  ds.readAttributeDouble(null, "start_time", Double.NaN);
          double stop =  ds.readAttributeDouble(null, "stop_time", Double.NaN);

         if(Double.isNaN(start) && Double.isNaN(stop)){
             double top = ds.readAttributeDouble(null, "toptime", Double.NaN);
             double bot = ds.readAttributeDouble(null, "bottime", Double.NaN);

             this.conventionName = "Cosmic2";
             if(top > bot) {
                 stop = top;
                 start = bot;
             } else {
                 stop = bot;
                 start = top;
             }

         }

          Dimension dim = ds.findDimension("MSL_alt");
          Variable dimV = ds.findVariable("MSL_alt");
          Array dimU = dimV.read();
          int inscr = ( dimU.getFloat(1) - dimU.getFloat(0)) > 0 ? 1 : 0;
          int n = dim.getLength();
          double incr = (stop - start) / n;

          String timeUnits = "seconds since 1980-01-06 00:00:00";
          Variable timeVar = new VariableDS(ds, null, null, "time", DataType.DOUBLE, dim.getName(), timeUnits, null);
          ds.addVariable(null, timeVar);
          timeVar.addAttribute(new Attribute("units", timeUnits));
          timeVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
          int dir = ds.readAttributeInteger(null, "irs", 1);
          ArrayDouble.D1 data = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, new int[] {n});
          if(inscr == 0) {
              if(dir == 1) {
                  for (int i=0; i<n; i++)
                      data.set(i, start + i * incr);
              } else {
                  for (int i=0; i<n; i++)
                      data.set(i, stop - i * incr);
              }
          } else {
              for (int i=0; i<n; i++)
                  data.set(i, stop - i * incr);
          }
          timeVar.setCachedData(data, false);
        }

        Variable v = ds.findVariable("Lat");
        if(v == null)
            v = ds.findVariable("GEO_lat");
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
        Variable v1 = ds.findVariable("Lon");
        if(v1==null)
            v1 = ds.findVariable("GEO_lon");
        v1.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    } else {
        Dimension dim = ds.findDimension("time");
        int n = dim.getLength();
        Variable latVar = new VariableDS(ds, null, null, "Lat", DataType.DOUBLE, dim.getName(), "degree", null);
        latVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
        ds.addVariable(null, latVar);
        Variable lonVar = new VariableDS(ds, null, null, "Lon", DataType.DOUBLE, dim.getName(), "degree", null);
        lonVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
        ds.addVariable(null, lonVar);
        Variable altVar = new VariableDS(ds, null, null, "MSL_alt", DataType.DOUBLE, dim.getName(), "meter", null);
        altVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
        ds.addVariable(null, altVar);
        ArrayDouble.D1 latData = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, new int[] {n});
        ArrayDouble.D1 lonData = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, new int[] {n});
        ArrayDouble.D1 altData = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, new int[] {n});
        this.conventionName = "Cosmic3";
        int iyr = ds.readAttributeInteger(null, "year", 2009);
        int mon = ds.readAttributeInteger(null, "month", 0);
        int iday = ds.readAttributeInteger(null, "day", 0);
        int ihr = ds.readAttributeInteger(null, "hour", 0);
        int min = ds.readAttributeInteger(null, "minute", 0);
        int sec = ds.readAttributeInteger(null,"second", 0);
        int t = 0;
        // cal the dtheta based pm attributes
        double dtheta = gast(iyr,mon,iday,ihr,min,sec,t);

        Variable v = ds.findVariable("xLeo");
        Array xLeo = v.read();
        v = ds.findVariable("yLeo");
        Array yLeo = v.read();
        v = ds.findVariable("zLeo");
        Array zLeo = v.read();
        double pi = 3.1415926;
        double a = 6378137.0;
        double b = 6356752.3142;
        IndexIterator iiter0  = xLeo.getIndexIterator();
        IndexIterator iiter1  = yLeo.getIndexIterator();
        IndexIterator iiter2  = zLeo.getIndexIterator();
        int i = 0;

        while(iiter0.hasNext()) {
           
            double [] v_inertial = new double[3];
            v_inertial[0] = iiter0.getDoubleNext()*1000; //.getDouble(i); //.nextDouble();
            v_inertial[1] = iiter1.getDoubleNext()*1000; //.getDouble(i); //.nextDouble();
            v_inertial[2] = iiter2.getDoubleNext()*1000; //.getDouble(i); //.nextDouble();
            double [] uvz = new double[3];
            uvz[0] = 0.0;
            uvz[1] = 0.0;
            uvz[2] = 1.0;
            // v_ecef should be in the (approximate) ECEF frame
            double [] v_ecf = spin(v_inertial, uvz, -180.0*dtheta/pi);

            // cal lat/lon here
            double [] llh = xyzell(a, b, v_ecf);
            latData.set(i, llh[0]);
            lonData.set(i, llh[1]);
            altData.set(i, llh[2]);
            i++;
        }

        latVar.setCachedData(latData, false);
        lonVar.setCachedData(lonData, false);
        altVar.setCachedData(altData, false);
    }
    ds.finish();
  }

  protected AxisType getAxisType(NetcdfDataset ncDataset, VariableEnhanced v) {
    String name =  v.getShortName();
    if (name.equals("time")) return AxisType.Time;
    if (name.equals("Lat") || name.equals("GEO_lat")) return AxisType.Lat;
    if (name.equals("Lon") || name.equals("GEO_lon")) return AxisType.Lon;
    // if (name.equals("xLeo") ) return AxisType.GeoX;
    // if (name.equals("yLeo") ) return AxisType.GeoY;
    if (name.equals("MSL_alt") ) return AxisType.Height;
    return null;
  }

   /**
    *
    *  NAME       :  XYZELL
    *
    *     CALL XYZELL(A,B,XSTAT,XSTELL)
    *
    *  PURPOSE    :  COMPUTATION OF ELLIPSOIDAL COORDINATES "XSTELL"
    *                GIVEN THE CARTESIAN COORDINATES "XSTAT"
    *
    *  PARAMETERS :
    *          IN :  A   : SEMI-MAJOR AXIS OF THE REFERENCE    R*8
    *                         ELLIPSOID IN METERS
    *                B   : SEMI-MINOR AXIS OF THE REFERENCE    R*8
    *                         ELLIPSOID IN METERS
    *                DXELL(3): TRANSLATION COMPONENTS FROM THE    R*8
    *                         ORIGIN OF THE CART. COORD. SYSTEM
    *                         (X,Y,Z) TO THE CENTER OF THE REF.
    *                         ELLIPSOID (IN METRES)
    *                SCELL  : SCALE FACTOR BETWEEN REF. ELLIPSOID R*8
    *                         AND WGS-84
    *                XSTAT(3): CARTESIAN COORDINATES (M)          R*8
    *         OUT :  XSTELL(3): ELLIPSOIDAL COORDINATES           R*8
    *                         XSTELL(1): ELL. LATITUDE (RADIAN)
    *                         XSTELL(2): ELL. LONGITUDE (RADIAN)
    *                         XSTELL(3): ELL. HEIGHT (M)
    *
    *  SR CALLED  :  DMLMTV
    *
    *  REMARKS    :  ---
    *
    *  AUTHOR     :  M. ROTHACHER
    *
    *  VERSION    :  3.4  (JAN 93)
    *
    *  CREATED    :  87/11/03 12:32        LAST MODIFIED :  88/11/21 17:36
    *
    *  COPYRIGHT  :  ASTRONOMICAL INSTITUTE
    *       1987      UNIVERSITY OF BERNE
    *                     SWITZERLAND
    */
    public double[] xyzell(double a,double b, double[] xstat) {


          double [] dxell = new double[3];
          double [] xstell = new double[3];
          double scell;
          double [] xp = new double[3];
          double e2, s, rlam, zps, h, phi, n, hp, phip;
          int i, niter;

          e2=(a*a-b*b)/(a*a);
          s=Math.sqrt(xstat[0]*xstat[0] +xstat[1]*xstat[1]);
          rlam=Math.atan2(xstat[1],xstat[0]);
          zps=xstat[2]/s;
          h=Math.sqrt(xstat[0]*xstat[0]+xstat[1]*xstat[1]+xstat[2]*xstat[2]) - a;
          phi=Math.atan(zps/(1.0-e2*a/(a+h)));

          niter = 0;
          for( i=1; i <=10000000; i++) {
             n=a/Math.sqrt(1.0-e2*Math.sin(phi)*Math.sin(phi));
             hp=h;
             phip=phi;
             h=s/Math.cos(phi)-n;
             phi=Math.atan(zps/(1.0-e2*n/(n+h)));
             niter = niter + 1;
             if ((Math.abs(phip-phi) <= 1.e-11) && (Math.abs(hp-h) <= 1.e-5)) {
               break;
             }
             if (niter >= 10) {
                phi  = -999.0;
                rlam = -999.0;
                h    = -999.0;
                break;
              }

          }

          xstell[0]=phi;
          xstell[1]=rlam;
          xstell[2]=h;

          return xstell;

     }


    /********************************************************************
    *    @call gast.f
    *     Compute hour angle dtheta

          ! iyr, mon, iday, ihr, min and sec form a base (epoch) time,
          ! t is an offset from the base time in seconds
          ! dtheta is the output hour angle in radians

    * Calculation of local time

    ! glon -- East longitude in degrees, -180 to 180

    ********************************************************************
    *      @call vprod.f   spin.f   rnorm.f
    * Calculation of the unit vector normal to the occultation plane
    * (clockwise rotated from GPS to LEO) */
   /*
        double dtheta = gast(iyr,mon,iday,ihr,min,sec,t)
        utc=ihr*1.d0+min/60.d0
        timloc=utc+24.d0*glon/360.d0
        if (timloc.gt.24.d0) timloc=timloc-24.d0
        if (timloc.lt.0.d0) timloc=timloc+24.d0
    */

    // In the inertial reference frame
       /*
        v_inertial(1)= ! Inertial GPS position vectors, XYZ
        v_inertial(2)=
        v_inertial(3)=
       */
    // In the Earth-fixed reference frame

    //  Z axis to rotate around (unit vector Z)
     /*
        uvz(1)=0.0;
        uvz(2)=0.0;
        uvz(3)=1.0;

        double [] v_ecf = spin(v_inertial,uvz,-180.d0*dtheta/pi)
    */ 
    // after this call, v_ecef should be in the (approximate) ECEF frame



   /**----------------------------------------------------------------------
    * @file       gast.f
    *
    * This subroutine computes the Greenwich Apparent Siderial
    * Time angle given a UTC date and time.
    *
    * @sub        gast
    * @parameter  Input parameters:
    * @ Inputs:
    * @  iyr, integer, 1995
    * @  imon, integer, 5
    * @  iday, integer, 5
    * @  ihr, integer, 5
    * @  imin, integer, 5
    * @  sec, double, 31.0
    * @  dsec, double, 0.0
    * @ Outputs:
    * @  theta, GAST angle in radians
    *
    * @author     Bill Schreiner
    * @since      May 1995
    * @version    $URL: svn://ursa.cosmic.ucar.edu/trunk/src/roam/gast.f $ $Id: gast.f 10129 2008-07-30 17:10:52Z dhunt $
    * -----------------------------------------------------------------------*/


     public double gast(int iyr,int imon,int iday,int ihr,int imin, double sec, double dsec) {
    //
      //    implicit double precision (a-h,o-z)
      //    character(len=*), parameter :: header = '$URL: svn://ursa.cosmic.ucar.edu/trunk/src/roam/gast.f $ $Id: gast.f 10129 2008-07-30 17:10:52Z dhunt $'
    //
    // Coordinate transform from the celestial inertial reference frame to the geo-
    // centered Greenwich reference frame.
    // Call a subroutine to calculate the Julian day "djd":
          double djd = juday(imon, iday, iyr);   //djd=julean day.
          double tu = (djd - 2451545.0)/36525.0;
          double gmst = 24110.548410 + 8640184.8128660*tu +
                        0.093104*tu*tu - 6.2e-6*Math.pow(tu, 3); //       !gmst=Greenwich mean...
          double utco = (ihr*3600)+(imin*60)+sec;
          double theta =  togreenw( dsec, utco, gmst);

          return theta;
    }

    /***********************************************************************

    * JDAY calculates the Julian Day number (JD) from the Gregorian month *
    * ,day, and year (M,D,Y). (NOT VALID BEFORE 10/15/1582)               *
    ***********************************************************************/
    public double juday(int M,int D,int Y) {
          double JD;

          int IY = Y - (12-M)/10;
          int IM = M + 1 + 12*((12-M)/10);
          int I  = IY/100;
          long J  = 2 - I + I/4 + Math.round(365.25*IY) + Math.round(30.6001*IM);
          JD = (J + D + 1720994.50);
          return JD;
    }
    /***********************************************************************
     * This subroutine is to transform the locations and velocities of the GPS and
     * LEO satellites from the celestial inertial reference frame to the Earth
     * centered Greenwich reference frame.
     * The dummy arguments iyear, month and iday are the calender year, month and
     * day of the occultation event.  The dummy arguments ihour, minute and sec
     * are the UTC time.
     * Reference: Astronomical Alamanus, 1993

     * Modified subroutine from Dasheng's code.
     ***************************************************************************/
    public double togreenw( double rectt, double utco, double gmst) {

        double pi = Math.acos(-1.00);
    //
    // For each occultation ID, its TU and GMST are the same.  However, every
    // occultation event takes place at gmst+uts, uts is progressively increasing
    // with every occultation event.
        double  utc = (utco + rectt) * 1.0027379093;
        gmst = gmst + utc;              //in seconds, without eoe correction.
    //  gmst may be a positive number or may be a negative number.
        while (gmst < 0.0)
            gmst = gmst + 86400.00;


        while (gmst > 86400.00)
            gmst = gmst - 86400.00;

    // gmst = the Greenwich mean sidereal time.
    // This gmst is without the corrections from the equation of equinoxes.  For
    // GPS/MET applications, the corrections from equation of equinoxes is not
    // necessary because of the accurary needed.
        double theta = gmst*2.0*pi/86400.0;   //!*** This is the THETA in radian.
    //
        return theta;
    }


    /**----------------------------------------------------------------------
    * @file       spin.f
    *
    * This subroutine rotates vector V1 around vector VS
    * at angle A. V2 is the vector after the rotation.
    *
    * @sub        spin
    * @parameter  Input parameters:
    * @ v1   - Vector to be rotated
    * @ vs   - Vector around which to rotate v1
    * @ a    - angle of rotation
    * @ Output parameters:
    * @ v2   - output vector
    *
    * @author     S.V.Sokolovskiy
    * @version    $URL: svn://ursa.cosmic.ucar.edu/trunk/src/roam/spin.f $ $Id: spin.f 10129 2008-07-30 17:10:52Z dhunt $
    * -----------------------------------------------------------------------*/

    public double [] spin(double [] v1, double [] vs, double a){

    //     implicit real*8(a-h,o-z)
    //     dimension v1(3),vs(3),vsn(3),v2(3),v3(3),s(3,3)
    //     Calculation of the unit vector around which
    //     the rotation should be done.
        double [] v2 = new double[3];
        double [] vsn = new double[3];
        double [] v3  = new double[3];

        double vsabs=Math.sqrt(vs[0]*vs[0]+vs[1]*vs[1] +vs[2]*vs[2]);

        for(int i=0; i<3; i++)
             vsn[i]=vs[i]/vsabs;

   // Calculation of the rotation matrix.

        double  a1=Math.cos(a);
        double  a2=1.0-a1;
        double  a3=Math.sin(a);
        double [][] s = new double[3][3];

        s[0][0]=a2*vsn[0]*vsn[0] + a1;
        s[0][1]=a2*vsn[0]*vsn[1]-a3*vsn[2];
        s[0][2]=a2*vsn[0]*vsn[2]+a3*vsn[1];
        s[1][0]=a2*vsn[1]*vsn[0]+a3*vsn[2];
        s[1][1]=a2*vsn[1]*vsn[1]+a1;
        s[1][2]=a2*vsn[1]*vsn[2]-a3*vsn[0];
        s[2][0]=a2*vsn[2]*vsn[0]-a3*vsn[1];
        s[2][1]=a2*vsn[2]*vsn[1]+a3*vsn[0];
        s[2][2]=a2*vsn[2]*vsn[2]+a1;

    // Calculation of the rotated vector.

        for(int i=0; i<3; i++)
            v3[i]=s[i][0]*v1[0]+s[i][1]*v1[1]+s[i][2]*v1[2];

        for(int i=0; i<3; i++)
            v2[i]=v3[i];

        return v2;

    }






}
