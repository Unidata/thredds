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
package ucar.unidata.geoloc.projection;

import ucar.unidata.geoloc.*;

/*  import geotransform.transforms.Gdc_To_Utm_Converter;
import geotransform.transforms.Utm_To_Gdc_Converter;
import geotransform.ellipsoids.WE_Ellipsoid;
import geotransform.coords.Utm_Coord_3d;
import geotransform.coords.Gdc_Coord_3d; */

/** Test basic projection methods */

public class TestUtm {
  static double maxx_all = 0.0;

  int REPEAT = 100;
  int NPTS = 10000;
  boolean checkit = false;
  boolean calcErrs = true;
  boolean show = false;
  double tolm = 10.0; // tolerence in meters

  long sumNormal = 0;
  long sumArray = 0;

  java.util.Random r = new java.util.Random(System.currentTimeMillis());

  void doOne (double x, double y, int zone, boolean isNorth) {
    ProjectionImpl proj = new UtmProjection( zone, isNorth);

    System.out.println("*** x="+x+" y="+y);
    LatLonPoint latlon = proj.projToLatLon( x, y);
    System.out.println("   lat="+latlon.getLatitude()+" lon="+latlon.getLongitude());
    ProjectionPoint endP = proj.latLonToProj( latlon);
    System.out.println("   x="+endP.getX()+" y="+endP.getY());
  }

  /* void doOneG (double x, double y, int zone, boolean isNorth) {
    Gdc_Coord_3d latlon[] = new Gdc_Coord_3d[1];
    Utm_Coord_3d xy[] = new Utm_Coord_3d[1];
    Utm_Coord_3d xy2[] = new Utm_Coord_3d[1];

    x *= 1000.0;
    y *= 1000.0;

    for (int i = 0; i < 1; i++) {
      latlon[i] = new Gdc_Coord_3d(0.0, 0.0, 0.0);
      xy[i] = new Utm_Coord_3d(x, y, 0., (byte) zone, isNorth);
      xy2[i] = new Utm_Coord_3d(0., 0., 0., (byte) 0, true);
    }

    Gdc_To_Utm_Converter.Init(new WE_Ellipsoid());
    Utm_To_Gdc_Converter.Init(new WE_Ellipsoid());

    System.out.println("***G** x="+x+" y="+y);
    Utm_To_Gdc_Converter.Convert(xy, latlon);
    System.out.println("   lat="+latlon[0].latitude+" lon="+latlon[0].longitude);
    Gdc_To_Utm_Converter.Convert(latlon, xy2, (byte) zone);
    System.out.println("   x="+xy2[0].x+" y="+xy2[0].y);
  } */


  void run (int zone, boolean isNorth) {
    System.out.println("--------- zone= "+zone+" "+isNorth);

    double[][] from = new double[2][NPTS]; // random x, y
    for (int i=0; i<NPTS; i++) {
      from[0][i] = 800 * r.nextDouble(); // random x point 400 km on either side of central meridian
      from[1][i] = isNorth ? 8000 * r.nextDouble() : 10000.0 - 8000 * r.nextDouble(); // random y point
    }

    int n = REPEAT * NPTS;

    ProjectionImpl proj = new UtmProjection( zone, isNorth);
    double sumx = 0.0, sumy = 0.0, maxx = 0.0;
    long t1 = System.currentTimeMillis();
    for (int k=0; k<REPEAT; k++) {
      for (int i=0; i<NPTS; i++) {
        LatLonPoint latlon = proj.projToLatLon( from[0][i], from[1][i]);
        ProjectionPoint endP = proj.latLonToProj( latlon);

        if (calcErrs) {
          double errx = error( from[0][i], endP.getX());
          sumx += errx;
          sumy  += error( from[1][i], endP.getY());
          maxx = Math.max( maxx, errx);
          maxx_all = Math.max( maxx, maxx_all);
        }

        if (checkit) {
          check_km("y", from[1][i], endP.getY());
          if (check_km("x", from[0][i], endP.getX()) || show)
            System.out.println("   x="+from[0][i]+" y="+from[1][i]+" lat="+latlon.getLatitude()+" lon="+latlon.getLongitude());
        }
      }
    }
    long took = System.currentTimeMillis() - t1;
    sumNormal += took;
    System.out.println(" "+n +  " normal "+ proj.getClassName ()+" took "+took+ " msecs."+
        " avg error x= "+ 1000*sumx/n+" y="+1000*sumy/n+
         " maxx err = "+1000*maxx+" m");

    // array
    double[][] to = new double[2][NPTS];
    double[][] result2 = new double[2][NPTS];
    long start = System.currentTimeMillis();
    for (int k=0; k<REPEAT; k++) {
      proj.projToLatLon (from, to);
      proj.latLonToProj (to, result2);

      if (checkit) {
        for (int i = 0; i < NPTS; i++) {
          check_km("xa", from[0][i], result2[0][i]);
          check_km("ya", from[1][i], result2[1][i]);
          if (show) System.out.println("  x:" + result2[0][i] + " y=" + result2[1][i]);
        }
      }
    }
    took = System.currentTimeMillis() - start;
    sumArray += took;
    System.out.println(" "+n +  " array "+ proj.getClass().getName()+" took "+took+ " msecs "); // == "+ .001*took/NTRIALS+" secs/call ");
//    System.out.println(NTRIALS +  " array "+ proj.getClassName()+" took "+took+ " msecs == "+ .001*took/NTRIALS/REPEAT+" secs/call ");

    /* original geotransform code
    Gdc_Coord_3d latlon[] = new Gdc_Coord_3d[NPTS];
    Utm_Coord_3d xy[] = new Utm_Coord_3d[NPTS];
    Utm_Coord_3d xy2[] = new Utm_Coord_3d[NPTS];

    for (int i = 0; i < NPTS; i++) {
      latlon[i] = new Gdc_Coord_3d(0.0, 0.0, 0.0);
      xy[i] = new Utm_Coord_3d(1000*from[0][i], 1000*from[1][i], 0., (byte) zone, isNorth);
      xy2[i] = new Utm_Coord_3d(0., 0., 0., (byte) 0, true);
    }

    Gdc_To_Utm_Converter.Init(new WE_Ellipsoid());
    Utm_To_Gdc_Converter.Init(new WE_Ellipsoid());

    long t3 = System.currentTimeMillis();

    maxx = 0.0;
    sumx = 0.0;
    sumy = 0.0;
    for (int k = 0; k < REPEAT; k++) {
      Utm_To_Gdc_Converter.Convert(xy, latlon);
      Gdc_To_Utm_Converter.Convert(latlon, xy2, (byte) zone);

      if (calcErrs) {
        for (int i = 0; i < NPTS; i++) {
          double errx = error(xy[i].x, xy2[i].x);
          sumx += errx;
          sumy += error(xy[i].y, xy2[i].y);
          maxx = Math.max( maxx, errx);
        }
      }

      if (checkit) {
        for (int i = 0; i < NPTS; i++) {
          check_m("Gx", xy[i].x, xy2[i].x);
          check_m("Gy", xy[i].y, xy2[i].y);
          if (show) System.out.println("x=" + xy[i].x + " y=" + xy[i].y);

          // check against array results
          check_deg("Glat", latlon[i].latitude, to[0][i]);
          check_deg("Glon", latlon[i].longitude, to[1][i]);
          if (show) System.out.println("  lat=" + latlon[i].latitude + " lon=" + latlon[i].longitude);
        }
      }
    }
    long took3 = System.currentTimeMillis() - t3;
    double msecs = ((double)took3)/(NPTS*REPEAT);
    System.out.println(" "+n+" geotransform (org) took "+took3+ " msecs. avg error = "+ sumx/n+" "+sumy/n+
        " maxx = "+maxx); */
  }

  double error (double d1, double d2) {
    return Math.abs( d1-d2);
  }

  boolean check_km (String what, double d1, double d2) {
    double err = 1000*Math.abs(d1-d2);
    if (err > tolm)
      System.out.println(" *"+what+": "+d1 + "!="+ d2+" err="+err+" m");
    return (err > tolm);
  }

 boolean check_m (String what, double d1, double d2) {
    double err = d1 == 0.0 ? 0.0 : Math.abs(d1-d2);
    if (err > tolm)
      System.out.println(" *"+what+": "+d1 + "!="+ d2+" err="+err+" m");
   return (err > tolm);
  }

 boolean check_deg (String what, double d1, double d2) {
    double err = d1 == 0.0 ? 0.0 : Math.abs(d1-d2);
    if (err > 10e-7)
      System.out.println(" *"+what+": "+d1 + "!="+ d2+" err="+err+" degrees");
    return (err > 10e-7);
  }

  static public void main( String[] args) {
    TestUtm r = new TestUtm();

    //r.doOne( 8.864733394164137, 2020.9206059122835, 2, false);
    //r.doOneG( 8.864733394164137, 2020.9206059122835, 2, false);
    //r.doOne( 858.1318063115505, 93.39736531227544, 22, true);
    //r.doOneG( 858.1318063115505, 93.39736531227544, 22, true);

    /* for (int zone=1; zone<=60; zone++) {
      r.run( zone, true);
      r.run( zone, false);
    } */

     r.run( 1, true);
     r.run( 60, false);

    System.out.println("\nmaxx_all= "+1000*maxx_all+" m");

  }

}
