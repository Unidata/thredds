package ucar.unidata.geoloc.projection;

import ucar.unidata.geoloc.*;
import junit.framework.*;
import ucar.unidata.util.Format;
import java.util.*;

/** Test basic projection methods */

public class TestProjectionTiming extends TestCase {
  int REPEAT = 100;
  int NPTS = 1000;
  double TOLERENCE = 1.0e-6;
  boolean checkit = true;

  public TestProjectionTiming( String name) {
    super(name);
  }

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
          assert close(from[0][i], endL.getLatitude()) : "lat: "+from[0][i] + "!="+ endL.getLatitude();
          assert close(from[1][i], endL.getLongitude())  : "lon: "+from[1][i] + "!="+ endL.getLongitude();
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
            assert close(from[0][i], result2[0][i]) : "lat: "+from[0][i] + "!="+ result2[0][i];
            assert close(from[1][i], result2[1][i])  : "lon: "+from[1][i] + "!="+ result2[1][i];
          }
        }
    }
    took = System.currentTimeMillis() - t2;
    sumArray += took;
    System.out.println(n +  " array "+ proj.getClassName ()+" took "+took+ " msecs "); // == "+ .001*took/NTRIALS+" secs/call ");
//    System.out.println(NTRIALS +  " array "+ proj.getClassName()+" took "+took+ " msecs == "+ .001*took/NTRIALS/REPEAT+" secs/call ");

  }

  boolean close (double d1, double d2) {
    return Math.abs( d1-d2) < TOLERENCE;
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
