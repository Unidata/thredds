/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.*;
import junit.framework.*;

import java.lang.invoke.MethodHandles;

/** Test basic projection methods */

public class TestProjectionTiming extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  int REPEAT = 100;
  int NPTS = 1000;
  double TOLERENCE = 1.0e-6;
  boolean checkit = true;

  /////////////////// testLatLonArea /////////////////

  long sumNormal = 0;
  long sumArray = 0;

  void timeProjection (ProjectionImpl proj) {
    java.util.Random r = new java.util.Random((long)this.hashCode());
    LatLonPointImpl startL = new LatLonPointImpl();


    double[][] from = new double[2][NPTS];
    for (int i=0; i<NPTS; i++) {
      from[0][i] = (180.0 * (r.nextDouble() - .5)); // random latlon point
      from[1][i] = (360.0 * (r.nextDouble() - .5)); // random latlon point
    }

    int n = REPEAT * NPTS;

   //double[][] result = new double[2][NTRIALS];

    // normal
    long t1 = System.currentTimeMillis();
    for (int k=0; k<REPEAT; k++) {
      for (int i=0; i<NPTS; i++) {
        ProjectionPoint p = proj.latLonToProj( from[0][i], from[1][i]);
        LatLonPoint endL = proj.projToLatLon( p);

        if (checkit) {
          assert Misc.nearlyEquals(from[0][i], endL.getLatitude()) : "lat: "+from[0][i] + "!="+ endL.getLatitude();
          assert nearlyEqualsLon(from[1][i], endL.getLongitude())  : "lon: "+from[1][i] + "!="+ endL.getLongitude();
        }
      }
    }
    long took = System.currentTimeMillis() - t1;
    sumNormal += took;
    System.out.println(n +  " normal "+ proj.getClassName ()+" took "+took+ " msecs "); // == "+ .001*took/NTRIALS+" secs/call ");


    // array
    long t2 = System.currentTimeMillis();
    for (int k=0; k<REPEAT; k++) {
      double[][] to = proj.latLonToProj (from);
      double[][] result2 = proj.projToLatLon (to);

        if (checkit) {
          for (int i=0; i<NPTS; i++) {
            assert Misc.nearlyEquals(from[0][i], result2[0][i]) : "lat: "+from[0][i] + "!="+ result2[0][i];
            assert nearlyEqualsLon(from[1][i], result2[1][i])  : "lon: "+from[1][i] + "!="+ result2[1][i];
          }
        }
    }
    took = System.currentTimeMillis() - t2;
    sumArray += took;
    System.out.println(n +  " array "+ proj.getClassName ()+" took "+took+ " msecs "); // == "+ .001*took/NTRIALS+" secs/call ");
//    System.out.println(NTRIALS +  " array "+ proj.getClassName()+" took "+took+ " msecs == "+ .001*took/NTRIALS/REPEAT+" secs/call ");

  }

  public static boolean nearlyEqualsLon( double v1, double v2) {
    return Misc.nearlyEquals(LatLonPointImpl.lonNormal(v1), LatLonPointImpl.lonNormal(v2) );
  }

  public void testEachProjection() {
    //timeProjection( new LatLonProjection());
    timeProjection( new LambertConformal());     // note: testing default paramters only
    timeProjection( new LambertConformal());     // note: testing default paramters only
    timeProjection( new LambertConformal());     // note: testing default paramters only
    timeProjection( new LambertConformal());     // note: testing default paramters only

    System.out.println(" normal  took "+sumNormal+ " msecs ");
    System.out.println(" array  took "+sumArray+ " msecs ");

    //testProjection( new TransverseMercator());
    //testProjection( new Stereographic());
  }

}
