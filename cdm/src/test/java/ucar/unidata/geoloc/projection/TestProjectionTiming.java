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
