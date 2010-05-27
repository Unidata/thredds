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

// $Id: Grib2ProductDefinitionSection.java,v 1.25 2006/08/18 20:22:10 rkambic Exp $

/**
 * TestGrib2ProductDefinitionSection.java  1.1  08/29/2003.
 * @author Robb Kambic
 */
package ucar.grib;


import ucar.grib.GribNumbers;
import ucar.grib.NotSupportedException;
import ucar.grib.NoValidGribException;
import ucar.grib.grib2.Grib2Input;
import ucar.grib.grib2.Grib2Product;
import ucar.grib.grib2.Grib2PDSVariables;
import ucar.grib.grib2.Grib2GDSVariables;
import ucar.grib.grib1.*;

import ucar.unidata.io.RandomAccessFile;
import ucar.grid.GridParameter;

import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;


/**
 * A class to compare the product definition section (PDS) of a GRIB product being
 * created by a direct read manner against the PDS byte[] manner via object
 * Grib(1|2)PDSVariable.
 * A class to compare the grid definition section (GDS) of a GRIB product being
 * created by a direct read manner against the GDS byte[] manner via object
 * Grib(1|2)GDSVariable.
 */

public final class TestCompareGribPDSGDSsections extends TestCase {
  /*
   * verbose output
   */
  public static final boolean verbose = false;

  /**
   * scale factor for Lat/Lon variables in degrees.
   */
  public static final float tenToNegSix = (float) (1 / 1000000.0);
  public static final float tenToSix = (float) 1000000.0;

  /**
   * scale factor for dx and dy variables plus others
   */
  public static final float tenToNegThree = (float) (1 / 1000.0);
  public static final float tenToThree = (float) 1000.0;

 /**
  * Pattern to extract header.
  */
  private static final Pattern productID =
          Pattern.compile("(\\w{6} \\w{4} \\d{6})");

  /**
   * gives status of the test.
   *
   * @return status
   */
  public static Test suite() {
    return new TestSuite(TestCompareGribPDSGDSsections.class);
  }

  public final void testCompare() throws IOException {
    File home = new File("C:/data/grib/idd/binary");
    //File home = new File("C:/data/grib");
    File work = new File("/local/robb/data/grib/idd/binary");
    //File work = new File("/local/robb/data/grib");
    String[] args = new String[1];
    if (home.exists()) {
      args[0] = home.getPath();
      doAll(args);
    } else if ( false && work.exists()) {
      args[0] = work.getPath();
      doAll(args);
    } else {
      args[0] = "/share/testdata/test/motherlode/grid/grib/binary";
      doAll(args);
    }
  }

  void doAll(String args[]) throws IOException {

    String dirB = args[0];
    File dir = new File(dirB);
    if (dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String child : children) {
        //System.out.println( "children i ="+ children[ i ]);
        File aChild = new File(dir, child);
        //System.out.println( "child ="+ child.getName() );
        if (aChild.isDirectory()) {
          continue;
          // skip index *gbx and inventory *xml files
        } else if (
          //child.contains("ECMWF_Global_2p5") ||
          //child.contains("OCEAN_Global_5x2p5deg_20090209_1200.grib1") ||
          //child.contains("SPECTRAL_Global_5x2p5deg_20090114_0000.grib1") ||
          //child.contains("SST_Global_2x2deg_20090211_0000.grib1") ||
          //child.contains("SST_Global_5x2p5deg_20090114_0000.grib1") ||
          //child.contains("GFS_Ensemble_1p25deg") ||
          //child.contains("GFS_Global_0p5deg") ||
          //child.contains("GFS_Global_1p0deg_Ensemble") ||
          //child.contains("GFS_Global_1p0deg_Ensemble") ||
          child.contains("rotatedlatlon.grb") ||
          child.endsWith(GribIndexName.oldSuffix) ||
          child.endsWith(GribIndexName.currentSuffix) ||
          child.endsWith("xml") ||
          child.endsWith("txt") ||
          child.endsWith("tmp") || //index in creation process
          child.length() == 0) { // zero length file, ugh...
        } else {
          System.out.println("\n\nFile " + child);
          if (child.endsWith("grib1") || child.endsWith("grb")) {
            comparePDS1(dirB + "/" + child);
            compareGDS1(dirB + "/" + child);
            //comparePDS1(dirB + "/" + "ECMWF.hybrid.grib1");
            //compareGDS1(dirB + "/" + "ECMWF.hybrid.grib1");
            //System.exit( 0 );
          } else if (child.contains("grib2")) {
            comparePDS2(dirB + "/" + child);
            compareGDS2(dirB + "/" + child);
            //System.exit( 0 );
          }
        }
      }
    } else {
    }
  }

  private void comparePDS2(String gribFile) throws IOException {

    RandomAccessFile raf = new RandomAccessFile(gribFile, "r");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    System.out.println("Comparing PDSs");
    Grib2Input g2i = new Grib2Input(raf);
    // params getProducts (implies  unique GDSs too), oneRecord
    g2i.scan(true, false);
    List<Grib2Product> products = g2i.getProducts();
    boolean passOne = true;
    for (int i = 0; i < products.size(); i++) {
      Grib2Product product = products.get(i);
      raf.seek(product.getPdsOffset());
      PdsReader2 pds = new PdsReader2(raf);
      Grib2PDSVariables gpv = pds.pdsVars;
      if (passOne) {
        System.out.println(" Section = " + gpv.getSection());
        System.out.println(" Length = " + gpv.getLength());
        System.out.println(" ProductDefinition = " + gpv.getProductDefinition());
        passOne = false;
      }

      assert (pds.length == gpv.getLength());
      assert (pds.section == gpv.getSection());
      assert (pds.coordinates == gpv.getCoordinates());
      assert (pds.productDefinition == gpv.getProductDefinition());
      assert (pds.parameterCategory == gpv.getParameterCategory());
      assert (pds.parameterNumber == gpv.getParameterNumber());
      if (pds.productDefinition < 20) {  // NCEP models
        assert (pds.typeGenProcess == gpv.getTypeGenProcess());
        assert (pds.backGenProcess == gpv.getBackGenProcess());
        assert (pds.analysisGenProcess == gpv.getAnalysisGenProcess());
        assert (pds.hoursAfter == gpv.getHoursAfter());
        assert (pds.minutesAfter == gpv.getMinutesAfter());
        assert (pds.timeRangeUnit == gpv.getTimeRangeUnit());
        //System.out.println( i +" "+ pds.forecastTime +" "+ gpv.getForecastTime());
        assert (pds.forecastTime == gpv.getForecastTime());
        assert (pds.typeFirstFixedSurface == gpv.getTypeFirstFixedSurface());
        assert (pds.FirstFixedSurfaceValue == gpv.getValueFirstFixedSurface());
        assert (pds.typeSecondFixedSurface == gpv.getTypeSecondFixedSurface());
        assert (pds.SecondFixedSurfaceValue == gpv.getValueSecondFixedSurface());
      }

      if ((pds.productDefinition == 1) || (pds.productDefinition == 11)) {
        assert (pds.typeEnsemble == gpv.getType());
        assert (pds.perturbNumber == gpv.getPerturbation());
        assert (pds.numberForecasts == gpv.getNumberForecasts());

      } else if (pds.productDefinition == 2) {
        assert (pds.typeEnsemble == gpv.getType());
        assert (pds.numberForecasts == gpv.getNumberForecasts());

      } else if (pds.productDefinition == 5) {
        assert (pds.typeEnsemble == gpv.getType());
        assert (pds.lowerLimit == gpv.getValueLowerLimit());
        assert (pds.upperLimit == gpv.getValueUpperLimit());

      } else if (pds.productDefinition == 9) {
        assert (pds.typeEnsemble == gpv.getType());
        assert (pds.numberForecasts == gpv.getNumberForecasts());
        // probability type
        assert (pds.lowerLimit == gpv.getValueLowerLimit());
        assert (pds.upperLimit == gpv.getValueUpperLimit());
      }
      pds = null;
    }
    products = null;

  }

  private void compareGDS2(String gribFile) throws IOException {

    RandomAccessFile raf = new RandomAccessFile(gribFile, "r");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    System.out.println("Comparing GDSs");
    Grib2Input g2i = new Grib2Input(raf);
    // params getProducts (implies  unique GDSs too), oneRecord
    g2i.scan(true, false);
    List<Grib2Product> products = g2i.getProducts();
    boolean passOne = true;
    for (int i = 0; i < products.size(); i++) {
      Grib2Product product = products.get(i);
      raf.seek(product.getGdsOffset());
      GdsReader2 gds = new GdsReader2(raf, true);
      Grib2GDSVariables gpv = gds.gdsVars;
      if (passOne) {
        System.out.println(" Section = " + gpv.getSection());
        System.out.println(" Length = " + gpv.getLength());
        System.out.println(" Grid Template Number = " + gpv.getGdtn());
        passOne = false;
      }

      assert (gds.length == gpv.getLength());
      assert (gds.section == gpv.getSection());
      assert (gds.numberPoints == gpv.getNumberPoints());
      assert (gds.source == gpv.getSource());
      assert (gds.olon == gpv.getOlon());
      assert (gds.gdtn == gpv.getGdtn());

      int gdtn = gpv.getGdtn();

      switch (gdtn) {  // Grid Definition Template Number

        case 0:
        case 1:
        case 2:
        case 3:       // Latitude/Longitude Grid
          assert (gds.shape == gpv.getShape());
          assert (gds.nx == gpv.getNx());
          //System.out.println( "nx=" + nx);
          assert (gds.ny == gpv.getNy());
          //System.out.println( "ny=" + ny);
          assert (gds.basicAngle == gpv.getBasicAngle());
          assert (gds.subdivisionsangle == gpv.getSubDivisions());
          assert (gds.la1 == gpv.getLa1());
          assert (gds.lo1 == gpv.getLo1());
          assert (gds.resolution == gpv.getResolution());
          assert (gds.la2 == gpv.getLa2());
          assert (gds.lo2 == gpv.getLo2());
          assert (gds.dx == gpv.getDx());
          assert (gds.dy == gpv.getDy());
          assert (gds.grid_units.equals(gpv.getGridUnits()));
          assert (gds.scanMode == gpv.getScanMode());
          assert (gds.angle == gpv.getAngle());
          
          //  1, 2, and 3 needs checked
          if (gdtn == 1) {         //Rotated Latitude/longitude
            assert (gds.spLat == gpv.getSpLat());
            assert (gds.spLon == gpv.getSpLon());
            assert (gds.rotationangle == gpv.getRotationAngle());

          } else if (gdtn == 2) {  //Stretched Latitude/longitude
            assert (gds.poleLat == gpv.getPoleLat());
            assert (gds.poleLon == gpv.getPoleLon());
            assert (gds.factor == gpv.getStretchingFactor());

          } else if (gdtn == 3) {  //Stretched and Rotated
            // Latitude/longitude
            assert (gds.spLat == gpv.getSpLat());
            assert (gds.spLon == gpv.getSpLon());
            assert (gds.rotationangle == gpv.getRotationAngle());
            assert (gds.poleLat == gpv.getPoleLat());
            assert (gds.poleLon == gpv.getPoleLon());
            assert (gds.factor == gpv.getStretchingFactor());
          }
          break;

        case 10:  // Mercator
          // la1, lo1, lad, la2, and lo2 need checked
          assert (gds.shape == gpv.getShape());
          ;
          //System.out.println( "shape=" + shape );
          assert (gds.nx == gpv.getNx());
          //System.out.println( "nx=" + nx);
          assert (gds.ny == gpv.getNy());
          //System.out.println( "ny=" + ny);
          assert (gds.la1 == gpv.getLa1());
          assert (gds.lo1 == gpv.getLo1());
          assert (gds.resolution == gpv.getResolution());
          assert (gds.lad == gpv.getLaD());
          assert (gds.la2 == gpv.getLa2());
          assert (gds.lo2 == gpv.getLo2());
          assert (gds.scanMode == gpv.getScanMode());
          assert (gds.angle == gpv.getAngle());
          assert (gds.dx == gpv.getDx());
          assert (gds.dy == gpv.getDy());
          assert (gds.grid_units.equals(gpv.getGridUnits()));

          break;

        case 20:  // Polar stereographic projection
          // la1, lo1, lad, and lov need checked
          assert (gds.shape == gpv.getShape());
          ;
          //System.out.println( "shape=" + shape );
          assert (gds.nx == gpv.getNx());
          //System.out.println( "nx=" + nx);
          assert (gds.ny == gpv.getNy());
          //System.out.println( "ny=" + ny);
          assert (gds.la1 == gpv.getLa1());
          assert (gds.lo1 == gpv.getLo1());
          assert (gds.resolution == gpv.getResolution());
          assert (gds.lad == gpv.getLaD());
          assert (gds.lov == gpv.getLoV());
          assert (gds.dx == gpv.getDx());
          assert (gds.dy == gpv.getDy());
          assert (gds.grid_units.equals(gpv.getGridUnits()));
          assert (gds.projectionCenter == gpv.getProjectionFlag());
          assert (gds.scanMode == gpv.getScanMode());

          break;

        case 30:  // Lambert Conformal
          assert (gds.shape == gpv.getShape());
          ;
          //System.out.println( "shape=" + shape );
          assert (gds.nx == gpv.getNx());
          //System.out.println( "nx=" + nx);
          assert (gds.ny == gpv.getNy());
          //System.out.println( "ny=" + ny);
          assert (gds.la1 == gpv.getLa1());
          //System.out.println( "la1=" + la1 );
          assert (gds.lo1 == gpv.getLo1());
          //System.out.println( "lo1=" + lo1);
          assert (gds.resolution == gpv.getResolution());
          assert (gds.lad == gpv.getLaD());
          assert (gds.lov == gpv.getLoV());
          assert (gds.dx == gpv.getDx());
          assert (gds.dy == gpv.getDy());
          assert (gds.grid_units.equals(gpv.getGridUnits()));

          assert (gds.projectionCenter == gpv.getProjectionFlag());
          assert (gds.scanMode == gpv.getScanMode());
          assert (gds.latin1 == gpv.getLatin1());
          assert (gds.latin2 == gpv.getLatin2());
          //System.out.println( "latin1=" + latin1);
          //System.out.println( "latin2=" + latin2);
          assert (gds.spLat == gpv.getSpLat());
          assert (gds.spLon == gpv.getSpLon());
          //System.out.println( "spLat=" + spLat);
          //System.out.println( "spLon=" + spLon);

          break;

        case 31:  // Albers Equal Area
          // la1, lo1, lad, and lov need checked
          assert (gds.shape == gpv.getShape());
          //System.out.println( "shape=" + shape );
          assert (gds.nx == gpv.getNx());
          //System.out.println( "nx=" + nx);
          assert (gds.ny == gpv.getNy());
          //System.out.println( "ny=" + ny);
          assert (gds.la1 == gpv.getLa1());
          //System.out.println( "la1=" + la1 );
          assert (gds.lo1 == gpv.getLo1());
          //System.out.println( "lo1=" + lo1);
          assert (gds.resolution == gpv.getResolution());
          assert (gds.lad == gpv.getLaD());
          assert (gds.lov == gpv.getLoV());
          assert (gds.dx == gpv.getDx());
          assert (gds.dy == gpv.getDy());
          assert (gds.grid_units.equals(gpv.getGridUnits()));

          assert (gds.projectionCenter == gpv.getProjectionFlag());
          assert (gds.scanMode == gpv.getScanMode());
          assert (gds.latin1 == gpv.getLatin1());
          assert (gds.latin2 == gpv.getLatin2());
          //System.out.println( "latin1=" + latin1);
          //System.out.println( "latin2=" + latin2);
          assert (gds.spLat == gpv.getSpLat());
          assert (gds.spLon == gpv.getSpLon());
          //System.out.println( "spLat=" + spLat);
          //System.out.println( "spLon=" + spLon);

          break;

        case 40:
        case 41:
        case 42:
        case 43:  // Gaussian latitude/longitude
          // octet 15
          assert (gds.shape == gpv.getShape());
          //System.out.println( "shape=" + shape );
          // octets 31-34
          assert (gds.nx == gpv.getNx());
          //System.out.println( "nx=" + nx);
          // octets 35-38
          assert (gds.ny == gpv.getNy());
          //System.out.println( "ny=" + ny);
          // octets 39-42
          assert (gds.angle == gpv.getBasicAngle());
          // octets 43-46
          assert (gds.subdivisionsangle == gpv.getSubDivisions());
          //System.out.println( "ratio =" + ratio );
          // octets 47-50
          assert (gds.la1 == gpv.getLa1());
          // octets 51-54
          assert (gds.lo1 == gpv.getLo1());
          // octet 55
          assert (gds.resolution == gpv.getResolution());
          // octets 56-59
          assert (gds.la2 == gpv.getLa2());
          // octets 60-63
          assert (gds.lo2 == gpv.getLo2());
          // octets 64-67
          assert (gds.dx == gpv.getDx());
          assert (gds.grid_units.equals(gpv.getGridUnits()));

          // octet 68-71
          assert (gds.np == gpv.getNp());
          // octet 72
          assert (gds.scanMode == gpv.getScanMode());

          if (gdtn == 41) {  //Rotated Gaussian Latitude/longitude
            // octets 73-76
            assert (gds.spLat == gpv.getSpLat());
            // octets 77-80
            assert (gds.spLon == gpv.getSpLon());
            // octets 81-84
            assert (gds.rotationangle == gpv.getRotationAngle());

          } else if (gdtn == 42) {  //Stretched Gaussian
            // Latitude/longitude
            // octets 73-76
            assert (gds.poleLat == gpv.getPoleLat());
            // octets 77-80
            assert (gds.poleLon == gpv.getPoleLon());
            // octets 81-84
            assert (gds.factor == gpv.getStretchingFactor());

          } else if (gdtn == 43) {  //Stretched and Rotated Gaussian
            // Latitude/longitude
            // octets 73-76
            assert (gds.spLat == gpv.getSpLat());
            // octets 77-80
            assert (gds.spLon == gpv.getSpLon());
            // octets 81-84
            assert (gds.rotationangle == gpv.getRotationAngle());
            // octets 85-88
            assert (gds.poleLat == gpv.getPoleLat());
            // octets 89-92
            assert (gds.poleLon == gpv.getPoleLon());
            // octets 93-96
            assert (gds.factor == gpv.getStretchingFactor());
          }
          break;

        case 50:
        case 51:
        case 52:
        case 53:                     // Spherical harmonic coefficients

          gds.j = raf.readFloat();
          gds.k = raf.readFloat();
          gds.m = raf.readFloat();
          gds.method = raf.read();
          gds.mode = raf.read();
          assert (gds.grid_units.equals(gpv.getGridUnits()));
          if (gdtn == 51) {         //Rotated Spherical harmonic coefficients
            assert (gds.spLat == gpv.getSpLat());
            assert (gds.spLon == gpv.getSpLon());
            assert (gds.rotationangle == gpv.getRotationAngle());

          } else if (gdtn == 52) {  //Stretched Spherical
            // harmonic coefficients
            assert (gds.poleLat == gpv.getPoleLat());
            assert (gds.poleLon == gpv.getPoleLon());
            assert (gds.factor == gpv.getStretchingFactor());

          } else if (gdtn == 53) {  //Stretched and Rotated
            // Spherical harmonic coefficients
            assert (gds.spLat == gpv.getSpLat());
            assert (gds.spLon == gpv.getSpLon());
            assert (gds.rotationangle == gpv.getRotationAngle());
            assert (gds.poleLat == gpv.getPoleLat());
            assert (gds.poleLon == gpv.getPoleLon());
            assert (gds.factor == gpv.getStretchingFactor());
          }
          break;

        case 90:  // Space view perspective or orthographic
          assert (gds.shape == gpv.getShape());
          //System.out.println( "shape=" + shape );
          assert (gds.nx == gpv.getNx());
          //System.out.println( "nx=" + nx);
          assert (gds.ny == gpv.getNy());
          //System.out.println( "ny=" + ny);
          assert (gds.lap == gpv.getLap());
          assert (gds.lop == gpv.getLop());
          assert (gds.resolution == gpv.getResolution());
          assert (gds.dx == gpv.getDx());
          assert (gds.dy == gpv.getDy());
          assert (gds.grid_units.equals(gpv.getGridUnits()));
          assert (gds.xp == gpv.getXp());
          assert (gds.yp == gpv.getYp());
          assert (gds.scanMode == gpv.getScanMode());
          assert (gds.angle == gpv.getAngle());
          //altitude = GribNumbers.int4( raf ) * 1000000;
          assert (gds.altitude == gpv.getNr());
          assert (gds.xo == gpv.getXo());
          assert (gds.yo == gpv.getYo());

          break;

          /*
          case 100 :  // Triangular grid based on an icosahedron

              n2          = raf.read();
              checkSum = 7 * checkSum + n2;
              n3          = raf.read();
              checkSum = 7 * checkSum + n3;
              ni          = GribNumbers.int2(raf);
              checkSum = 7 * checkSum + ni;
              nd          = raf.read();
              checkSum = 7 * checkSum + nd;
              poleLat     = GribNumbers.int4(raf) / tenToSix;
              checkSum = 7 * checkSum + poleLat;
              poleLon     = GribNumbers.int4(raf) / tenToSix;
              checkSum = 7 * checkSum + poleLon;
              lonofcenter = GribNumbers.int4(raf);
              position    = raf.read();
              order       = raf.read();
              scanMode    = raf.read();
              n           = GribNumbers.int4(raf);
              grid_units = "";
              break;

          case 110 :  // Equatorial azimuthal equidistant projection
              shape = raf.read();
              //System.out.println( "shape=" + shape );
              scalefactorradius = raf.read();
              scaledvalueradius = GribNumbers.int4(raf);
              scalefactormajor  = raf.read();
              scaledvaluemajor  = GribNumbers.int4(raf);
              scalefactorminor  = raf.read();
              scaledvalueminor  = GribNumbers.int4(raf);
              nx                = GribNumbers.int4(raf);
              //System.out.println( "nx=" + nx);
              ny = GribNumbers.int4(raf);
              //System.out.println( "ny=" + ny);
              la1              = GribNumbers.int4(raf) / tenToSix;
              checkSum = 7 * checkSum + la1;
              lo1              = GribNumbers.int4(raf) / tenToSix;
              checkSum = 7 * checkSum + lo1;
              resolution       = raf.read();
              dx = (float) (GribNumbers.int4(raf) / tenToThree);
              //checkSum = 7 * checkSum + dx;
              dy = (float) (GribNumbers.int4(raf) / tenToThree);
              //checkSum = 7 * checkSum + dy;
              grid_units = "";
              projectionCenter = raf.read();
              scanMode         = raf.read();

              break;

          case 120 :  // Azimuth-range Projection
              nb       = GribNumbers.int4(raf);
              checkSum = 7 * checkSum + nb;
              nr       = GribNumbers.int4(raf);
              checkSum = 7 * checkSum + nr;
              la1      = GribNumbers.int4(raf);
              checkSum = 7 * checkSum + la1;
              lo1      = GribNumbers.int4(raf);
              checkSum = 7 * checkSum + lo1;
              dx       = GribNumbers.int4(raf);
              //checkSum = 7 * checkSum + dx;
              grid_units = "";
              dstart   = raf.readFloat();
              scanMode = raf.read();
              for (int i = 0; i < nr; i++) {
                  // get azi (33+4(Nr-1))-(34+4(Nr-1))
                  // get adelta (35+4(Nr-1))-(36+4(Nr-1))
              }
              System.out.println("need code to get azi and adelta");

              break;
          */
        case 204:  // Curvilinear orthographic
          assert (gds.shape == gpv.getShape());
          //System.out.println( "shape=" + shape );
          assert (gds.nx == gpv.getNx());
          //System.out.println( "nx=" + nx);
          assert (gds.ny == gpv.getNy());
          //System.out.println( "ny=" + ny);
          // octets 39 - 54 not used, set to 0
          assert (gds.resolution == gpv.getResolution());
          // octets 56 - 71 not used
          assert (gds.scanMode == gpv.getScanMode());
          assert (gds.grid_units.equals(gpv.getGridUnits()));
          break;

        default:
          System.out.println("Unknown Grid Type "
              + Integer.toString(gdtn));
      }  // end switch

      // calculate earth radius
      if (((gdtn < 50) || (gdtn > 53)) && (gdtn != 100) && (gdtn != 120)) {
        if (gds.shape == 0) {
          assert (gds.earthRadius == gpv.getEarthRadius());
        } else if (gds.shape == 1) {
          assert (gds.earthRadius == gpv.getEarthRadius());
        } else if (gds.shape == 2) {
          assert (gds.majorAxis == gpv.getMajorAxis());
          assert (gds.minorAxis == gpv.getMinorAxis());
        } else if (gds.shape == 3) {
          //System.out.println( "majorAxisScale =" + scalefactormajor );
          //System.out.println( "majorAxisiValue =" + scaledvaluemajor );
          assert (gds.majorAxis == gpv.getMajorAxis());

          //System.out.println( "minorAxisScale =" + scalefactorminor );
          //System.out.println( "minorAxisValue =" + scaledvalueminor );
          assert (gds.minorAxis == gpv.getMinorAxis());
        } else if (gds.shape == 4) {
          assert (gds.majorAxis == gpv.getMajorAxis());
          assert (gds.minorAxis == gpv.getMinorAxis());
        } else if (gds.shape == 6) {
          assert (gds.earthRadius == gpv.getEarthRadius());
        }
      }
      // This is a quasi-regular grid, save the number of pts in each parallel
      if (gds.olon != 0) {
        //System.out.println( "olon ="+ olon +" iolon ="+ iolon );
        int numPts;
        if ((gds.scanMode & 32) == 0) {
          numPts = gds.ny;
        } else {
          numPts = gds.nx;
        }
        gds.olonPts = new int[numPts];
        //int count = 0;
        gds.maxPts = 0;
        if (gds.olon == 1) {
          for (int ii = 0; i < numPts; i++) {
            gds.olonPts[ii] = raf.read();
            if (gds.maxPts < gds.olonPts[ii]) {
              gds.maxPts = gds.olonPts[ii];
            }
            //count += olonPts[ i ];
            //System.out.println( "parallel =" + i +" number pts ="+ latPts );
          }
        } else if (gds.olon == 2) {
          for (int ii = 0; i < numPts; i++) {
            gds.olonPts[ii] = raf.readUnsignedShort();
            if (gds.maxPts < gds.olonPts[ii]) {
              gds.maxPts = gds.olonPts[ii];
            }
            //count += olonPts[ i ];
            //System.out.println( "parallel =" + i +" number pts ="+ latPts );
          }
        }
        if ((gds.scanMode & 32) == 0) {
          gds.nx = gds.maxPts;
        } else {
          gds.ny = gds.maxPts;
        }
        //double lodiff = gds.getLo2() - gds.getLo1();
        gds.dx = (float) (gds.lo2 - gds.lo1) / (gds.nx - 0);
        //System.out.println( "total number pts ="+ count );
      }
      assert (gds.gdskey == gpv.getGdsKey());
      gds = null;
    }
    products = null;
  }


  private void comparePDS1(String gribFile) throws IOException {

    RandomAccessFile raf = new RandomAccessFile(gribFile, "r");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    System.out.println("Comparing PDSs");
    try {
       Input1 g1i = new  Input1(raf);
      // params getProducts (implies  unique GDSs too), oneRecord
      g1i.scan(true, false);
      ArrayList  products =  g1i.getProducts();
      boolean passOne = true;
      for (int i = 0; i < products.size(); i++) {
        Grib1Product product = (Grib1Product) products.get(i);
        raf.seek(product.getOffset1());
        PdsReader1 pds = new PdsReader1(raf);
        Grib1PDSVariables gpv = pds.pdsVars;
        if (passOne) {
          System.out.println(" Section = " + gpv.getSection());
          System.out.println(" Length = " + gpv.getLength());
          System.out.println(" ProductDefinition = " + gpv.getProductDefinition());
        }
        assert (pds.length == gpv.getLength());
        assert (pds.table_version == gpv.getTableVersion());
        assert (pds.center_id == gpv.getCenter());
        assert (pds.typeGenProcess == gpv.getTypeGenProcess());
        assert (pds.grid_id == gpv.getGrid_Id());
        assert (pds.gds_exists == gpv.gdsExists());
        assert (pds.bms_exists == gpv.bmsExists());
        assert (pds.parameterNumber == gpv.getParameterNumber());
        assert (pds.getLevelType() == gpv.getTypeFirstFixedSurface());
        assert (pds.getLevelValue1() == gpv.getValueFirstFixedSurface());
        assert (pds.getLevelValue2() == gpv.getValueSecondFixedSurface());

        assert (pds.baseTime.equals(gpv.getBaseTime()));
        assert (pds.refTime == gpv.getRefTime());
        //assert( pds.tUnit == gpv.getTimeUnit() );
        assert (pds.p1 == gpv.getP1());
        assert (pds.p2 == gpv.getP2());
        assert (pds.timeRangeValue == gpv.getTimeRange());
        assert (pds.subcenter_id == gpv.getSubCenter());
        assert (pds.decscale == gpv.getDecimalScale());
        assert (pds.forecastTime == gpv.getForecastTime());


        if ( ! verbose )
          return;

        if (passOne) {
          System.out.println(" Center =" + pds.center_id + " Sub Center =" + pds.subcenter_id
              + " table_version =" + pds.table_version);
          passOne = false;
        }
        if (gpv.isEnsemble() && pds.center_id == 98) {
          System.out.println("Class =" + gpv.getType() + " Type =" + gpv.getID()
              + " Stream =" + gpv.getStream() + " Labeling =" + gpv.getOctet50()
              + " NumberOfForecast =" + gpv.getOctet51());
        } else if (gpv.getLength() > 40) {
          // TODO: fix for NCEP ensembles
          //System.out.println("Type =" + gpv.getType() + " ID =" + gpv.getID()
          //    + " Product =" + gpv.getProduct() + " Octet45 =" + gpv.getOctet45());
          System.out.println("Number of members =" + gpv.getNumberForecasts());
        } else if (gpv.getProbabilityProduct() != GribNumbers.UNDEFINED) {
          System.out.println("Probability Product =" + gpv.getProbabilityProduct()
              + " Probability Type =" + gpv.getProbabilityType()
              + "Lower Limit =" + gpv.getValueLowerLimit() + " Upper Limit =" + gpv.getValueUpperLimit());
          //System.out.println("Number of members =" + gpv.getNumberForecasts());

        } else if (gpv.getSizeClusters() != GribNumbers.UNDEFINED) {
          System.out.println("Cluster size =" + gpv.getSizeClusters()
              + " Number of Clusters =" + gpv.getNumberClusters()
              + " Cluster method =" + gpv.getMethod()
              + " Northern latitude =" + gpv.getNorthLatitude()
              + " Southern latitude =" + gpv.getSouthLatitude()
              + " Easthern longitude =" + gpv.getEastLongitude()
              + " Westhern longitude =" + gpv.getWestLongitude());
        }
        pds = null;
      }
      products = null;
    } catch (NoValidGribException nvge) {
    }

  }

  private void compareGDS1(String gribFile) throws IOException {

    RandomAccessFile raf = new RandomAccessFile(gribFile, "r");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    System.out.println("Comparing GDSs");
    try {

      Grib1Input g1i = new  Grib1Input(raf);
      // params getProducts (implies  unique GDSs too), oneRecord
      g1i.scan(true, false);
      List<Grib1Product> products = g1i.getProducts();
      boolean passOne = true;
      for (int i = 0; i < products.size(); i++) {
        Grib1Product product = products.get(i);
        Grib1ProductDefinitionSection pds = product.getPDS();
        Grib1PDSVariables g1pdsv = pds.getPdsVars();
        if (product.getOffset1() == -1)
          break;
        raf.seek(product.getOffset1());
        GdsReader1 gds = new GdsReader1(raf, true);
        Grib1GDSVariables gpv = gds.gdsVars;
        if (passOne) {
          System.out.println(" Section = " + gpv.getSection());
          System.out.println(" Length = " + gpv.getLength());
          System.out.println(" Grid Template Number = " + gpv.getGdtn());
          passOne = false;
        }

        assert (gds.length == gpv.getLength());
        assert (2 == gpv.getSection());
        assert (gds.NV == gpv.getNV());
        //System.out.println( "GDS NV = " + NV );
        if( gpv.hasVerticalPressureLevels() ) {
          System.out.println( "Vertical Pressure Levels" );
          double[] plevels = gpv.getVerticalPressureLevels( g1pdsv.getValueFirstFixedSurface() );
        }
        assert (gds.P_VorL == gpv.getPVorPL());
        //System.out.println( "GDS PL = " + P_VorL );

        // octet 6 (grid type)
        assert (gds.type == gpv.getGdtn());
        //System.out.println( "GDS grid type = " + type );
        //name = getName(type);

        if (gds.type != 50) {  // values same up to resolution

          // octets 7-8 (Nx - number of points along x-axis)
          assert (gds.nx == gpv.getNx());

          // octets 9-10 (Ny - number of points along y-axis)
          assert (gds.ny == gpv.getNy());

          // octets 11-13 (La1 - latitude of first grid point)
          assert (gds.lat1 == gpv.getLa1());

          // octets 14-16 (Lo1 - longitude of first grid point)
          assert (gds.lon1 == gpv.getLo1());

          // octet 17 (resolution and component flags).  See Table 7
          assert (gds.resolution == gpv.getResolution());

        }

        switch (gds.type) {

          //  Latitude/Longitude  grids ,  Arakawa semi-staggered e-grid rotated
          //  Arakawa filled e-grid rotated
          case 0:
          case 4:
          case 10:
          case 40:
          case 201:
          case 202:

            assert (gds.grid_units.equals(gpv.getGridUnits()));
            // octets 18-20 (La2 - latitude of last grid point)
            assert (gds.lat2 == gpv.getLa2());
            // octets 21-23 (Lo2 - longitude of last grid point)
            assert (gds.lon2 == gpv.getLo2());
            // octets 24-25 (Dx - Longitudinal Direction Increment )
            assert (gds.dx == gpv.getDx());
            // octets 26-27 (Dy - Latitudinal Direction Increment )
            //               Np - parallels between a pole and the equator
            if (gds.type == 4) {
              assert (gds.np == gpv.getDy());
            } else {
              assert (gds.dy == gpv.getDy());
            }
            // octet 28 (Scanning mode)  See Table 8
            assert (gds.scan == gpv.getScanMode());
            // octet 29-32 reserved
            if (gds.type == 10) {  //rotated
              // octets 33-35 (lat of southern pole)
              assert (gds.latsp == gpv.getSpLat());
              // octets 36-38 (lon of southern pole)
              assert (gds.lonsp == gpv.getSpLon());
              // octet 35-42 reserved
              assert (gds.angle == gpv.getAngle());
            }

//            if (gds.P_VorL != 255) {
//              if (gds.NV == 0 || gds.NV == 255) {
//                //getPL(raf);
//              } else {
//                //getPV(gds.NV,raf);
//              }
//            }
            break;  // end Latitude/Longitude grids

          case 1:    //  Mercator grids

            assert (gds.grid_units.equals(gpv.getGridUnits()));
            // octets 18-20 (La2 - latitude of last grid point)
            assert (gds.lat2 == gpv.getLa2());
            // octets 21-23 (Lo2 - longitude of last grid point)
            assert (gds.lon2 == gpv.getLo2());
            // octets 24-26 (Latin - latitude where cylinder intersects the earth
            assert (gds.latin1 == gpv.getLatin1());
            // octet 28 (Scanning mode)  See Table 8
            assert (gds.scan == gpv.getScanMode());
            // octets 29-31 (Dx - Longitudinal Direction Increment )
            assert (gds.dx == gpv.getDx());
            // octets 32-34 (Dx - Longitudinal Direction Increment )
            assert (gds.dy == gpv.getDy());
            // octet 35-42 reserved
//            if (gds.P_VorL != 255) {
//              if (gds.NV == 0 || gds.NV == 255) {
//                //getPL(raf);
//              } else {
//                //getPV(gds.NV,raf);
//              }
//            }

            break;  // end Mercator grids

          case 3:    // Lambert Conformal

            assert (gds.grid_units.equals(gpv.getGridUnits()));
            // octets 18-20 (Lov - Orientation of the grid - east lon parallel to y axis)
            assert (gds.lov == gpv.getLoV());
            // octets 21-23 (Dx - the X-direction grid length) See Note 2 of Table D
            assert (gds.dx == gpv.getDx());
            // octets 24-26 (Dy - the Y-direction grid length) See Note 2 of Table D
            assert (gds.dy == gpv.getDy());
            // octets 27 (Projection Center flag) See Note 5 of Table D
            assert (gds.proj_center == gpv.getProjectionFlag());
            // octet 28 (Scanning mode)  See Table 8
            assert (gds.scan == gpv.getScanMode());
            // octets 29-31 (Latin1 - first lat where secant cone cuts spherical earth
            assert (gds.latin1 == gpv.getLatin1());
            // octets 32-34 (Latin2 - second lat where secant cone cuts spherical earth)
            assert (gds.latin2 == gpv.getLatin2());
            // octets 35-37 (lat of southern pole)
            assert (gds.latsp == gpv.getSpLat());
            // octets 38-40 (lon of southern pole)
            assert (gds.lonsp == gpv.getSpLon());
//            // octets 41-42
//            if (gds.P_VorL != 255) {
//              if (gds.NV == 0 || gds.NV == 255) {
//                //getPL(raf);
//              } else {
//                //getPV(gds.NV,raf);
//              }
//            }

            break;  // end Lambert Conformal

          case 5:    //  Polar Stereographic grids

            assert (gds.grid_units.equals(gpv.getGridUnits()));
            // octets 18-20 (Lov - Orientation of the grid - east lon parallel to y axis)
            assert (gds.lov == gpv.getLoV());
            if (gds.type == 87) {
              assert (gds.lon2 == gpv.getLo2());
            }
            // octets 21-23 (Dx - Longitudinal Direction Increment )
            assert (gds.dx == gpv.getDx());
            // octets 24-26(Dy - Latitudinal Direction Increment )
            assert (gds.dy == gpv.getDy());
            // octets 27 (Projection Center flag) See Note 5 of Table D
            assert (gds.proj_center == gpv.getProjectionFlag());
            // octet 28 (Scanning mode)  See Table 8
            assert (gds.scan == gpv.getScanMode());
//            // octet 29-32 reserved
//            if (gds.P_VorL != 255) {
//              if (gds.NV == 0 || gds.NV == 255) {
//                //getPL(raf);
//              } else {
//                //getPV(gds.NV,raf);
//              }
//            }

            break;  // end Polar Stereographic grids

          default:
            System.out.println("Unknown Grid Type : " + gds.type);
            throw new NoValidGribException("GDS: Unknown Grid Type : " + gds.type
                + ") is not supported.");
        }             // end switch grid_type
        assert (gds.gdskey == gpv.getGdsKey());

        //System.out.println( "scan=" + scan );
        if ((gds.scan & 63) != 0) {
          throw new NoValidGribException("GDS: This scanning mode (" +
              gds.scan + ") is not supported.");
        }
        gds = null;
      }
      products = null;
    } catch (NoValidGribException nvge) {

    }

  } // end compareGDS1

  static public void main(String args[]) throws IOException {
    //TestCompareGribPDSGDSsections g2pds = new TestCompareGribPDSGDSsections();
    //g2pds.testCompare();
    junit.textui.TestRunner.run(suite());
  }

  /////////////////////////////////////////////////////////////////////////////
  /**
   * class to have a direct read on a Grib2 PDS section
   */
  final class PdsReader2 {
    /**
     * Length in bytes of this PDS.
     */
    public final int length;

    /**
     * Number of this section, should be 4.
     */
    public final int section;

    /**
     * Number of this coordinates.
     */
    public final int coordinates;

    /**
     * productDefinition.
     */
    public final int productDefinition;

    /**
     * parameterCategory.
     */
    public int parameterCategory;

    /**
     * parameterNumber.
     */
    public int parameterNumber;

    /**
     * typeGenProcess.
     */
    public int typeGenProcess;

    /**
     * backGenProcess.
     */
    public int backGenProcess;

    /**
     * analysisGenProcess.
     */
    public int analysisGenProcess;

    /**
     * hoursAfter.
     */
    public int hoursAfter;

    /**
     * minutesAfter.
     */
    public int minutesAfter;

    /**
     * timeRangeUnit.
     */
    public int timeRangeUnit;

    /**
     * forecastTime.
     */
    public int forecastTime;

    /**
     * typeFirstFixedSurface.
     */
    public int typeFirstFixedSurface;

    /**
     * value of FirstFixedSurface.
     */
    public float FirstFixedSurfaceValue;

    /**
     * typeSecondFixedSurface.
     */
    public int typeSecondFixedSurface;

    /**
     * SecondFixedSurface Value.
     */
    public float SecondFixedSurfaceValue;

    /**
     * Type of Ensemble.
     */
    public int typeEnsemble;

    /**
     * Perturbation number.
     */
    public int perturbNumber;

    /**
     * number of Forecasts.
     */
    public int numberForecasts;

    /**
     * number of bands.
     */
    public int nb;

    /**
     * Model Run/Analysis/Reference time.
     */
    public Date endTI;
    public int timeRanges;
    public int[] timeIncrement;
    public float lowerLimit, upperLimit;

    /**
     * PDS as Variables from a byte[]
     */
    public final Grib2PDSVariables pdsVars;

    // *** constructors *******************************************************
    /**
     * Constructs a Grib2ProductDefinitionSection  object from a raf.
     *
     * @param raf RandomAccessFile with PDS content
     * @throws java.io.IOException if raf contains no valid GRIB file
     */
    public PdsReader2(RandomAccessFile raf)
        throws IOException {

      long sectionEnd = raf.getFilePointer();
      // octets 1-4 (Length of PDS)
      length = GribNumbers.int4(raf);
      //System.out.println( "PDS length=" + length );
      // read in whole PDS as byte[]
      byte[] pdsData = new byte[length];
      // reset to beginning of section and read data
      raf.skipBytes(-4);
      raf.read(pdsData);
      pdsVars = new Grib2PDSVariables(pdsData);
      // reset for variable section read and set sectionEnd
      raf.seek(sectionEnd + 4);
      sectionEnd += length;
      // octet 5
      section = raf.read();
      //System.out.println( "PDS is 4, section=" + section );
      // octet 6-7
      coordinates = GribNumbers.int2(raf);
      //System.out.println( "PDS coordinates=" + coordinates );
      // octet 8-9
      productDefinition = GribNumbers.int2(raf);
      //System.out.println( "PDS productDefinition=" + productDefinition );

      switch (productDefinition) {

        // Analysis or forecast at a horizontal level or in a horizontal
        // layer at a point in time
        case 0:
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
        case 8:
        case 9:
        case 11:
        case 12: {

          // octet 10
          parameterCategory = raf.read();
          //System.out.println( "PDS parameterCategory=" + parameterCategory );
          // octet 11
          parameterNumber = raf.read();
          //System.out.println( "PDS parameterNumber=" + parameterNumber );
          // octet 12
          typeGenProcess = raf.read();
          //System.out.println( "PDS typeGenProcess=" + typeGenProcess );
          // octet 13
          backGenProcess = raf.read();
          //System.out.println( "PDS backGenProcess=" + backGenProcess );
          // octet 14
          analysisGenProcess = raf.read();
          //System.out.println( "PDS analysisGenProcess=" + analysisGenProcess );
          // octet 15-16
          hoursAfter = GribNumbers.int2(raf);
          //System.out.println( "PDS hoursAfter=" + hoursAfter );
          // octet 17
          minutesAfter = raf.read();
          //System.out.println( "PDS minutesAfter=" + minutesAfter );
          // octet 18
          timeRangeUnit = raf.read();
          //System.out.println( "PDS timeRangeUnit=" + timeRangeUnit );
          // octet 19-22
          forecastTime = GribNumbers.int4(raf) * calculateIncrement(timeRangeUnit, 1);
          //System.out.println( "PDS forecastTime=" + forecastTime );
          // octet 23
          typeFirstFixedSurface = raf.read();
          //System.out.println( "PDS typeFirstFixedSurface=" + typeFirstFixedSurface );
          // octet 24
          int scaleFirstFixedSurface = raf.read();
          //System.out.println( "PDS scaleFirstFixedSurface=" + scaleFirstFixedSurface );
          // octet 25-28
          int valueFirstFixedSurface = GribNumbers.int4(raf);
          //System.out.println( "PDS valueFirstFixedSurface=" + valueFirstFixedSurface );
          FirstFixedSurfaceValue = (float) (((scaleFirstFixedSurface
              == 0) || (valueFirstFixedSurface == 0))
              ? valueFirstFixedSurface
              : valueFirstFixedSurface
              * Math.pow(10, -scaleFirstFixedSurface));
          //System.out.println( "PDS FirstFixedSurfaceValue ="+ FirstFixedSurfaceValue );
          // octet 29
          typeSecondFixedSurface = raf.read();
          //System.out.println( "PDS typeSecondFixedSurface=" + typeSecondFixedSurface );
          // octet 30
          int scaleSecondFixedSurface = raf.read();
          //System.out.println( "PDS scaleSecondFixedSurface=" + scaleSecondFixedSurface );
          // octet 31-34
          int valueSecondFixedSurface = GribNumbers.int4(raf);
          //System.out.println( "PDS valueSecondFixedSurface=" + valueSecondFixedSurface );
          SecondFixedSurfaceValue = (float) (((scaleSecondFixedSurface
              == 0) || (valueSecondFixedSurface == 0))
              ? valueSecondFixedSurface
              : valueSecondFixedSurface
              * Math.pow(10, -scaleSecondFixedSurface));

          try {  // catches NotSupportedExceptions

            // Individual ensemble forecast, control and perturbed, at a
            // horizontal level or in a horizontal layer at a point in time
            if ((productDefinition == 1) || (productDefinition == 11)) {
              // octet 35
              typeEnsemble = raf.read();
              // octet 36
              perturbNumber = raf.read();
              // octet 37
              numberForecasts = raf.read();
              //System.out.println( "PDS typeEnsemble ="+ typeEnsemble );
              //System.out.println( "PDS perturbEnsemble ="+ perturbNumber );
              //System.out.println( "PDS numberEnsemble ="+ numberForecasts );
              if (productDefinition == 11) {  // skip 38-74-nn detail info
                //  38-39 bytes
                int year = GribNumbers.int2(raf);
                int month = (raf.read()) - 1;
                int day = raf.read();
                int hour = raf.read();
                int minute = raf.read();
                int second = raf.read();
                //System.out.println( "PDS date:" + year +":" + month +
                //":" + day + ":" + hour +":" + minute +":" + second );
                GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
                c.clear();
                c.set(year, month, day, hour, minute, second);
                endTI = c.getTime();
                // 42
                timeRanges = raf.read();
                //System.out.println( "PDS timeRanges=" + timeRanges ) ;
                // 43 - 46
                int missingDataValues = GribNumbers.int4(raf);
                //System.out.println( "PDS missingDataValues=" + missingDataValues ) ;
                timeIncrement = new int[timeRanges * 6];
                for (int t = 0; t < timeRanges; t += 6) {
                  // 47 statProcess
                  timeIncrement[t] = raf.read();
                  // 48  timeType
                  timeIncrement[t + 1] = raf.read();
                  // 49   time Unit
                  timeIncrement[t + 2] = raf.read();
                  // 50 - 53 lenTimeRange
                  timeIncrement[t + 3] = GribNumbers.int4(raf);
                  // 54 indicatorTU
                  timeIncrement[t + 4] = raf.read();
                  // 55 - 58 timeIncrement
                  timeIncrement[t + 5] = GribNumbers.int4(raf);
                }
                // modify forecast time to reflect the end of the
                // interval according to timeIncrement information.
                // 1 accumulation
                // 2 F.T. inc
                // 1 Hour
                // 3 number of hours to inc F.T.
                // 255 missing
                // 0 continuous processing
                //if( timeRanges == 1 && timeIncrement[ 2 ] == 1) {
                if (timeRanges == 1) {
                  forecastTime += calculateIncrement(timeIncrement[2], timeIncrement[3]);
                } else { // throw flag
                  forecastTime = GribNumbers.UNDEFINED;
                }
              }
              //System.out.println( "typeGenProcess ="+ typeGenProcess );
              //Derived forecast based on all ensemble members at a horizontal
              // level or in a horizontal layer at a point in time
            } else if (productDefinition == 2) {
              // octet 35
              typeEnsemble = raf.read();
              // octet 36
              numberForecasts = raf.read();
              // Derived forecasts based on a cluster of ensemble members over
              // a rectangular area at a horizontal level or in a horizontal layer
              // at a point in time
            } else if (productDefinition == 3) {
              //System.out.println("PDS productDefinition == 3 not done");
              throw new NotSupportedException("PDS productDefinition = 3 not implemented");

              // Derived forecasts based on a cluster of ensemble members
              // over a circular area at a horizontal level or in a horizontal
              // layer at a point in time
            } else if (productDefinition == 4) {
              //System.out.println("PDS productDefinition == 4 not done");
              throw new NotSupportedException("PDS productDefinition = 4 not implemented");

              // Probability forecasts at a horizontal level or in a horizontal
              //  layer at a point in time
            } else if (productDefinition == 5) {
              // 35
              int probabilityNumber = raf.read();
              // 36
              numberForecasts = raf.read();
              // 37
              typeEnsemble = raf.read();
              // 38
              int scaleFactorLL = raf.read();
              // 39-42
              int scaleValueLL = GribNumbers.int4(raf);
              lowerLimit = (float) (((scaleFactorLL == 0) || (scaleValueLL == 0))
                  ? scaleValueLL
                  : scaleValueLL * Math.pow(10, -scaleFactorLL));
              // 43
              int scaleFactorUL = raf.read();
              // 44-47
              int scaleValueUL = GribNumbers.int4(raf);
              upperLimit = (float) (((scaleFactorUL == 0) || (scaleValueUL == 0))
                  ? scaleValueUL
                  : scaleValueUL * Math.pow(10, -scaleFactorUL));

              //System.out.print("PDS productDefinition == 5 PN="+probabilityNumber +" TP="+totalProbabilities +" PT="+probabilityType);
              //System.out.println( " LL="+lowerLimit +" UL="+upperLimit);
              //System.out.println( " typeGenProcess ="+ typeGenProcess );

              // Percentile forecasts at a horizontal level or in a horizontal layer
              // at a point in time
            } else if (productDefinition == 6) {
              //System.out.println("PDS productDefinition == 6 not done");
              throw new NotSupportedException("PDS productDefinition = 6 not implemented");

              // Analysis or forecast error at a horizontal level or in a horizontal
              // layer at a point in time
            } else if (productDefinition == 7) {
              //System.out.println("PDS productDefinition == 7 not done");
              throw new NotSupportedException("PDS productDefinition = 7 not implemented");

              // Average, accumulation, and/or extreme values at a horizontal
              // level or in a horizontal layer in a continuous or non-continuous
              // time interval
            } else if (productDefinition == 8) {
              //System.out.println( "PDS productDefinition == 8 " );
              //  35-41 bytes
              int year = GribNumbers.int2(raf);
              int month = (raf.read()) - 1;
              int day = raf.read();
              int hour = raf.read();
              int minute = raf.read();
              int second = raf.read();
              //System.out.println( "PDS date:" + year +":" + month +
              //":" + day + ":" + hour +":" + minute +":" + second );
              GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
              c.clear();
              c.set(year, month, day, hour, minute, second);
              endTI = c.getTime();
              // 42
              timeRanges = raf.read();
              //System.out.println( "PDS timeRanges=" + timeRanges ) ;
              // 43 - 46
              int missingDataValues = GribNumbers.int4(raf);
              //System.out.println( "PDS missingDataValues=" + missingDataValues ) ;

              timeIncrement = new int[timeRanges * 6];
              for (int t = 0; t < timeRanges; t += 6) {
                // 47 statProcess
                timeIncrement[t] = raf.read();
                // 48  timeType
                timeIncrement[t + 1] = raf.read();
                // 49   time Unit
                timeIncrement[t + 2] = raf.read();
                // 50 - 53 lenTimeRange
                timeIncrement[t + 3] = GribNumbers.int4(raf);
                // 54 indicatorTU
                timeIncrement[t + 4] = raf.read();
                // 55 - 58 timeIncrement
                timeIncrement[t + 5] = GribNumbers.int4(raf);
              }
              // modify forecast time to reflect the end of the
              // interval according to timeIncrement information.
              // 1 accumulation
              // 2 F.T. inc
              // 1 Hour
              // 3 number of hours to inc F.T.
              // 255 missing
              // 0 continuous processing
              //if( timeRanges == 1 && timeIncrement[ 2 ] == 1) {
              if (timeRanges == 1) {
                forecastTime += calculateIncrement(timeIncrement[2], timeIncrement[3]);
              } else { // throw flag
                forecastTime = GribNumbers.UNDEFINED;
              }
              // Probability forecasts at a horizontal level or in a horizontal
              // layer in a continuous or non-continuous time interval
            } else if (productDefinition == 9) {
              //  35-71 bytes
              // 35
              int probabilityNumber = raf.read();
              // 36
              numberForecasts = raf.read();
              // 37
              typeEnsemble = raf.read();
              // 38
              int scaleFactorLL = raf.read();
              // 39-42
              int scaleValueLL = GribNumbers.int4(raf);
              lowerLimit = (float) (((scaleFactorLL == 0) || (scaleValueLL == 0))
                  ? scaleValueLL
                  : scaleValueLL * Math.pow(10, -scaleFactorLL));
              // 43
              int scaleFactorUL = raf.read();
              // 44-47
              int scaleValueUL = GribNumbers.int4(raf);
              upperLimit = (float) (((scaleFactorUL == 0) || (scaleValueUL == 0))
                  ? scaleValueUL
                  : scaleValueUL * Math.pow(10, -scaleFactorUL));
              // 48-49
              int year = GribNumbers.int2(raf);
              // 50
              int month = (raf.read()) - 1;
              // 51
              int day = raf.read();
              // 52
              int hour = raf.read();
              // 53
              int minute = raf.read();
              // 54
              int second = raf.read();
              //System.out.println( "PDS date:" + year +":" + month +
              //":" + day + ":" + hour +":" + minute +":" + second );
              GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
              c.clear();
              c.set(year, month, day, hour, minute, second);
              endTI = c.getTime();
              // 55
              timeRanges = raf.read();
              //System.out.println( "PDS timeRanges=" + timeRanges ) ;
              // 56-59
              int missingDataValues = GribNumbers.int4(raf);
              //System.out.println( "PDS missingDataValues=" + missingDataValues ) ;
              timeIncrement = new int[timeRanges * 6];
              for (int t = 0; t < timeRanges; t += 6) {
                // 60 statProcess
                timeIncrement[t] = raf.read();
                // 61 time type
                timeIncrement[t + 1] = raf.read();
                // 62  time Unit
                timeIncrement[t + 2] = raf.read();
                // 63 - 66  lenTimeRange
                timeIncrement[t + 3] = GribNumbers.int4(raf);
                // 67  indicatorTU
                timeIncrement[t + 4] = raf.read();
                // 68-71 time Inc
                timeIncrement[t + 5] = GribNumbers.int4(raf);
              }
              // modify forecast time to reflect the end of the
              // interval according to timeIncrement information.
              // 1 accumulation
              // 2 F.T. inc
              // 1 Hour
              // 3 number of hours to inc F.T.
              // 255 missing
              // 0 continuous processing
              if (timeRanges == 1) {
                forecastTime += calculateIncrement(timeIncrement[2], timeIncrement[3]);
              } else { // throw flag
                forecastTime = GribNumbers.UNDEFINED;
              }
            } else if (productDefinition == 12) {
              // octet 35
              typeEnsemble = raf.read();
              // octet 36
              numberForecasts = raf.read();
              //  35-41 bytes
              int year = GribNumbers.int2(raf);
              int month = (raf.read()) - 1;
              int day = raf.read();
              int hour = raf.read();
              int minute = raf.read();
              int second = raf.read();
              //System.out.println( "PDS date:" + year +":" + month +
              //":" + day + ":" + hour +":" + minute +":" + second );
              GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
              c.clear();
              c.set(year, month, day, hour, minute, second);
              endTI = c.getTime();
              // 42
              timeRanges = raf.read();
              //System.out.println( "PDS timeRanges=" + timeRanges ) ;
              // 43 - 46
              int missingDataValues = GribNumbers.int4(raf);
              //System.out.println( "PDS missingDataValues=" + missingDataValues ) ;
              timeIncrement = new int[timeRanges * 6];
              for (int t = 0; t < timeRanges; t += 6) {
                // 47 statProcess
                timeIncrement[t] = raf.read();
                // 48  timeType
                timeIncrement[t + 1] = raf.read();
                // 49   time Unit
                timeIncrement[t + 2] = raf.read();
                // 50 - 53 lenTimeRange
                timeIncrement[t + 3] = GribNumbers.int4(raf);
                // 54 indicatorTU
                timeIncrement[t + 4] = raf.read();
                // 55 - 58 timeIncrement
                timeIncrement[t + 5] = GribNumbers.int4(raf);
              }
              // modify forecast time to reflect the end of the
              // interval according to timeIncrement information.
              // 1 accumulation
              // 2 F.T. inc
              // 1 Hour
              // 3 number of hours to inc F.T.
              // 255 missing
              // 0 continuous processing
              //if( timeRanges == 1 && timeIncrement[ 2 ] == 1) {
              if (timeRanges == 1) {
                forecastTime += calculateIncrement(timeIncrement[2], timeIncrement[3]);
              } else { // throw flag
                forecastTime = GribNumbers.UNDEFINED;
              }
            }
            break;

          } catch (NotSupportedException nse) {
            nse.printStackTrace();
          }
        }  // cases 0-14

        // Radar product
        case 20: {

          parameterCategory = raf.read();
          //System.out.println( "PDS parameterCategory=" +
          //parameterCategory );
          parameterNumber = raf.read();
          //System.out.println( "PDS parameterNumber=" + parameterNumber );
          typeGenProcess = raf.read();
          //System.out.println( "PDS typeGenProcess=" + typeGenProcess );
          break;
        }  // case 20

        // Satellite Product
        case 30: {

          parameterCategory = raf.read();
          //System.out.println( "PDS parameterCategory=" + parameterCategory );
          parameterNumber = raf.read();
          //System.out.println( "PDS parameterNumber=" + parameterNumber );
          typeGenProcess = raf.read();
          //System.out.println( "PDS typeGenProcess=" + typeGenProcess );
          backGenProcess = raf.read();
          //System.out.println( "PDS backGenProcess=" + backGenProcess );
          nb = raf.read();
          //System.out.println( "PDS nb =" + nb );
          // nb sometime 0 based, other times 1 base
          for (int j = 0; j < nb; j++) {
            raf.skipBytes(10);
          }
          break;
        }  // case 30

        // CCITTIA5 character string
        case 254: {

          parameterCategory = raf.read();
          //System.out.println( "PDS parameterCategory=" + parameterCategory );
          parameterNumber = raf.read();
          //System.out.println( "PDS parameterNumber=" + parameterNumber );
          //numberOfChars = GribNumbers.int4( raf );
          //System.out.println( "PDS numberOfChars=" + numberOfChars );
          break;
        }  // case 254

        default:
          break;
      }    // end switch

      raf.seek(sectionEnd);
    }

    /**
     * calculates the increment between time intervals
     *
     * @param tui    time unit indicator,
     * @param length of interval
     * @return  increment as int
     */
    private int calculateIncrement(int tui, int length) {
      switch (tui) {

        case 1:
          return length;
        case 10:
          return 3 * length;
        case 11:
          return 6 * length;
        case 12:
          return 12 * length;
        case 0:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
        case 13:
          return length;
        default:
          return GribNumbers.UNDEFINED;
      }
    }

    /**
     * forecastTime.
     *
     * @return ForecastTime
     */
    public final int getForecastTime() {
      return forecastTime;
    }

    /**
     * PDS as Grib2PDSVariables
     *
     * @return Grib2PDSVariables PDS vars
     */
    public Grib2PDSVariables getPdsVars() {
      return pdsVars;
    }

  } // end PDS2Reader


  ////////////////////////////////////////////////////////////////////////////////////////
  private final class GdsReader2 {

    /**
     * Length in bytes of this section.
     */
    public final int length;

    /**
     * section number should be 3.
     */
    public final int section;

    /**
     * source of grid definition.
     */
    public final int source;

    /**
     * number of data points.
     */
    public final int numberPoints;

    /**
     * olon > 0 is a quasi regular grid.
     */
    public final int olon;

    /**
     * are extreme points in the quasi regular grid.
     */
    public final int iolon;

    /**
     * number of points in each parallel for quasi grids.
     */
    public int[] olonPts;

    /**
     * Max number of points in parallel for quasi grids.
     */
    public int maxPts;

    /**
     * Grid Definition Template Number.
     */
    public final int gdtn;

    /**
     * Grid name.
     */
    public String name;

    /**
     * grid definitions from template 3.
     */
    public int shape;

    /**
     * earthRadius
     */
    public float earthRadius;

    /**
     * majorAxis
     */
    public float majorAxis;

    /**
     * minorAxis
     */
    public float minorAxis;

    /**
     * Number of grid columns. (Also Ni).
     */
    public int nx;

    /**
     * Number of grid rows. (Also Nj).
     */
    public int ny;

    /**
     * basicAngle
     */
    public int basicAngle;

    /**
     * subdivisionsangle
     */
    public int subdivisionsangle;

    /**
     * First latitude
     */
    public float la1;

    /**
     * First longitude
     */
    public float lo1;

    /**
     * resolution
     */
    public int resolution;

    /**
     * 2nd latitude
     */
    public float la2;

    /**
     * 2nd longitude
     */
    public float lo2;

    /**
     * lad
     */
    public float lad;

    /**
     * lov
     */
    public float lov;

    /**
     * x-distance between two grid points
     * can be delta-Lon or delta x.
     */
    public float dx;

    /**
     * y-distance of two grid points
     * can be delta-Lat or delta y.
     */
    public float dy;

    /**
     * units of the dx and dy variables
     */
    public String grid_units;

    /**
     * projectionCenter
     */
    public int projectionCenter;

    /**
     * scanMode
     */
    public int scanMode;

    /**
     * angle
     */
    public int angle = GribNumbers.UNDEFINED;

    /**
     * latin1
     */
    public float latin1;

    /**
     * latin2
     */
    public float latin2;

    /**
     * spLat
     */
    public float spLat;

    /**
     * spLon
     */
    public float spLon;

    /**
     * rotationangle
     */
    public float rotationangle;

    /**
     * poleLat
     */
    public float poleLat;

    /**
     * poleLon
     */
    public float poleLon;

    /**
     * lonofcenter
     */
    public int lonofcenter;

    /**
     * factor
     */
    public int factor;

    /**
     * n
     */
    public int np;

    /**
     * j
     */
    public float j;

    /**
     * k
     */
    public float k;

    /**
     * m
     */
    public float m;

    /**
     * method
     */
    public int method;

    /**
     * mode
     */
    public int mode;

    /**
     * xp
     */
    public float xp;

    /**
     * yp
     */
    public float yp;

    /**
     * lap
     */
    public int lap;

    /**
     * lop
     */
    public int lop;

    /**
     * xo
     */
    public int xo;

    /**
     * yo
     */
    public int yo;

    /**
     * altitude
     */
    public int altitude;

    /**
     * n2
     */
    public int n2;

    /**
     * n3
     */
    public int n3;

    /**
     * ni
     */
    public int ni;

    /**
     * nd
     */
    public int nd;

    /**
     * position
     */
    public int position;

    /**
     * order
     */
    public int order;

    /**
     * nb
     */
    public float nb;

    /**
     * nr
     */
    public float nr;

    /**
     * dstart
     */
    public float dstart;

    /**
     * checksum
     */
    //public String checksum = "";

    /**
     * 4.0 indexes use this for the GDS key
     */
    public int gdskey;

    /**
     * GDS as Variables from a byte[]
     */
    public final Grib2GDSVariables gdsVars;
    // *** constructors *******************************************************
    /**
     * Constructs a <tt>Grib2GridDefinitionSection</tt> object from a raf.
     *
     * @param raf        RandomAccessFile
     * @param doCheckSum calculate the checksum
     * @throws IOException if raf contains no valid GRIB product
     */
    public GdsReader2(RandomAccessFile raf, boolean doCheckSum)
        throws IOException {

      double checkSum;
      int scalefactorradius = 0;
      int scaledvalueradius = 0;
      int scalefactormajor = 0;
      int scaledvaluemajor = 0;
      int scalefactorminor = 0;
      int scaledvalueminor = 0;
      long sectionEnd = raf.getFilePointer();

      // octets 1-4 (Length of GDS)
      length = GribNumbers.int4(raf);
      //System.out.println( "GDS length=" + length );

      // read in whole GDS as byte[]
      byte[] gdsData = new byte[length];
      // reset to beginning of section and read data
      raf.skipBytes(-4);
      raf.read(gdsData);
      gdsVars = new Grib2GDSVariables(gdsData);
      // reset for variable section read and set sectionEnd
      raf.seek(sectionEnd + 4);
      sectionEnd += length;
      // octet 5
      section = raf.read();  // This is section 3
      //System.out.println( "GDS is 3, section=" + section );
      // octet 6
      source = raf.read();
      //System.out.println( "GDS source=" + source );
      // octets 7-10
      numberPoints = GribNumbers.int4(raf);
      //System.out.println( "GDS numberPoints=" + numberPoints );
      //checkSum = numberPoints;
      // octet 11
      olon = raf.read();
      //System.out.println( "GDS olon=" + olon );
      // octet 12
      iolon = raf.read();
      //System.out.println( "GDS iolon=" + iolon );
      // octets 13-14
      gdtn = GribNumbers.int2(raf);
      //System.out.println( "GDS gdtn=" + gdtn );
      checkSum = gdtn;
      //name = getGridName(gdtn);

      float ratio;

      switch (gdtn) {  // Grid Definition Template Number

        case 0:
        case 1:
        case 2:
        case 3:       // Latitude/Longitude Grid
          shape = raf.read();
          //System.out.println( "shape=" + shape );
          scalefactorradius = raf.read();
          scaledvalueradius = GribNumbers.int4(raf);
          scalefactormajor = raf.read();
          scaledvaluemajor = GribNumbers.int4(raf);
          scalefactorminor = raf.read();
          scaledvalueminor = GribNumbers.int4(raf);
          nx = GribNumbers.int4(raf);
          //System.out.println( "nx=" + nx);
          ny = GribNumbers.int4(raf);
          //System.out.println( "ny=" + ny);
          basicAngle = GribNumbers.int4(raf);
          subdivisionsangle = GribNumbers.int4(raf);
          if (basicAngle == 0) {
            ratio = tenToNegSix;
          } else {
            ratio = basicAngle / subdivisionsangle;
          }
          //System.out.println( "ratio =" + ratio );
          la1 = (float) (GribNumbers.int4(raf) * ratio);
          checkSum = 7 * checkSum + la1;
          lo1 = (float) (GribNumbers.int4(raf) * ratio);
          checkSum = 7 * checkSum + lo1;
          resolution = raf.read();
          la2 = (float) (GribNumbers.int4(raf) * ratio);
          //checkSum = 7 * checkSum + la2;
          lo2 = (float) (GribNumbers.int4(raf) * ratio);
          //checkSum = 7 * checkSum + lo2;
          dx = (float) (GribNumbers.int4(raf) * ratio);
          dy = (float) (GribNumbers.int4(raf) * ratio);

          grid_units = "degrees";

          scanMode = raf.read();

          //  1, 2, and 3 needs checked
          if (gdtn == 1) {         //Rotated Latitude/longitude
            spLat = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + spLat;
            spLon = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + spLon;
            rotationangle = raf.readFloat();

          } else if (gdtn == 2) {  //Stretched Latitude/longitude
            poleLat = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + poleLat;
            poleLon = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + poleLon;
            factor = GribNumbers.int4(raf);

          } else if (gdtn == 3) {  //Stretched and Rotated
            // Latitude/longitude
            spLat = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + spLat;
            spLon = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + spLon;
            rotationangle = raf.readFloat();
            poleLat = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + poleLat;
            poleLon = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + poleLon;
            factor = GribNumbers.int4(raf);
          }
          break;

        case 10:  // Mercator
          // la1, lo1, lad, la2, and lo2 need checked
          shape = raf.read();
          //System.out.println( "shape=" + shape );
          scalefactorradius = raf.read();
          scaledvalueradius = GribNumbers.int4(raf);
          scalefactormajor = raf.read();
          scaledvaluemajor = GribNumbers.int4(raf);
          scalefactorminor = raf.read();
          scaledvalueminor = GribNumbers.int4(raf);
          nx = GribNumbers.int4(raf);
          //System.out.println( "nx=" + nx);
          ny = GribNumbers.int4(raf);
          //System.out.println( "ny=" + ny);
          la1 = GribNumbers.int4(raf) / tenToSix;
          checkSum = 7 * checkSum + la1;
          lo1 = GribNumbers.int4(raf) / tenToSix;
          checkSum = 7 * checkSum + lo1;
          resolution = raf.read();
          lad = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + lad;
          la2 = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + la2;
          lo2 = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + lo2;
          scanMode = raf.read();
          angle = GribNumbers.int4(raf);
          dx = (float) (GribNumbers.int4(raf) / tenToThree);
          dy = (float) (GribNumbers.int4(raf) / tenToThree);
          grid_units = "m";

          break;

        case 20:  // Polar stereographic projection
          // la1, lo1, lad, and lov need checked
          shape = raf.read();
          //System.out.println( "shape=" + shape );
          scalefactorradius = raf.read();
          scaledvalueradius = GribNumbers.int4(raf);
          scalefactormajor = raf.read();
          scaledvaluemajor = GribNumbers.int4(raf);
          scalefactorminor = raf.read();
          scaledvalueminor = GribNumbers.int4(raf);
          nx = GribNumbers.int4(raf);
          //System.out.println( "nx=" + nx);
          ny = GribNumbers.int4(raf);
          //System.out.println( "ny=" + ny);
          la1 = GribNumbers.int4(raf) / tenToSix;
          checkSum = 7 * checkSum + la1;
          lo1 = GribNumbers.int4(raf) / tenToSix;
          checkSum = 7 * checkSum + lo1;
          resolution = raf.read();
          lad = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + lad;
          lov = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + lov;
          dx = (float) (GribNumbers.int4(raf) / tenToThree);
          dy = (float) (GribNumbers.int4(raf) / tenToThree);
          grid_units = "m";
          projectionCenter = raf.read();
          scanMode = raf.read();

          break;

        case 30:  // Lambert Conformal
          shape = raf.read();
          //System.out.println( "shape=" + shape );
          scalefactorradius = raf.read();
          scaledvalueradius = GribNumbers.int4(raf);
          scalefactormajor = raf.read();
          scaledvaluemajor = GribNumbers.int4(raf);
          scalefactorminor = raf.read();
          scaledvalueminor = GribNumbers.int4(raf);
          nx = GribNumbers.int4(raf);
          //System.out.println( "nx=" + nx);
          ny = GribNumbers.int4(raf);
          //System.out.println( "ny=" + ny);
          la1 = (float) (GribNumbers.int4(raf) / tenToSix);
          checkSum = 7 * checkSum + la1;
          //System.out.println( "la1=" + la1 );
          lo1 = (float) (GribNumbers.int4(raf) / tenToSix);
          checkSum = 7 * checkSum + lo1;
          //System.out.println( "lo1=" + lo1);
          resolution = raf.read();
          lad = (float) (GribNumbers.int4(raf)
              / tenToSix);
          //checkSum = 7 * checkSum + lad;
          lov = (float) (GribNumbers.int4(raf)
              / tenToSix);
          //checkSum = 7 * checkSum + lov;
          dx = (float) (GribNumbers.int4(raf) / tenToThree);
          dy = (float) (GribNumbers.int4(raf) / tenToThree);
          grid_units = "m";

          projectionCenter = raf.read();
          scanMode = raf.read();
          latin1 = (float) (GribNumbers.int4(raf)
              / tenToSix);
          //checkSum = 7 * checkSum + latin1;
          latin2 = (float) (GribNumbers.int4(raf)
              / tenToSix);
          //checkSum = 7 * checkSum + latin2;
          //System.out.println( "latin1=" + latin1);
          //System.out.println( "latin2=" + latin2);
          spLat = (float) (GribNumbers.int4(raf) / tenToSix);
          //checkSum = 7 * checkSum + spLat;
          spLon = (float) (GribNumbers.int4(raf) / tenToSix);
          //checkSum = 7 * checkSum + spLon;
          //System.out.println( "spLat=" + spLat);
          //System.out.println( "spLon=" + spLon);

          break;

        case 31:  // Albers Equal Area
          // la1, lo1, lad, and lov need checked
          shape = raf.read();
          //System.out.println( "shape=" + shape );
          scalefactorradius = raf.read();
          scaledvalueradius = GribNumbers.int4(raf);
          scalefactormajor = raf.read();
          scaledvaluemajor = GribNumbers.int4(raf);
          scalefactorminor = raf.read();
          scaledvalueminor = GribNumbers.int4(raf);
          nx = GribNumbers.int4(raf);
          //System.out.println( "nx=" + nx);
          ny = GribNumbers.int4(raf);
          //System.out.println( "ny=" + ny);
          la1 = GribNumbers.int4(raf) / tenToSix;
          checkSum = 7 * checkSum + la1;
          //System.out.println( "la1=" + la1 );
          lo1 = GribNumbers.int4(raf) / tenToSix;
          checkSum = 7 * checkSum + lo1;
          //System.out.println( "lo1=" + lo1);
          resolution = raf.read();
          lad = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + lad;
          lov = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + lov;
          dx = (float) (GribNumbers.int4(raf) / tenToThree);
          dy = (float) (GribNumbers.int4(raf) / tenToThree);
          grid_units = "m";

          projectionCenter = raf.read();
          scanMode = raf.read();
          latin1 = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + latin1;
          latin2 = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + latin2;
          //System.out.println( "latin1=" + latin1);
          //System.out.println( "latin2=" + latin2);
          spLat = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + spLat;
          spLon = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + spLon;
          //System.out.println( "spLat=" + spLat);
          //System.out.println( "spLon=" + spLon);

          break;

        case 40:
        case 41:
        case 42:
        case 43:  // Gaussian latitude/longitude
          // octet 15
          shape = raf.read();
          //System.out.println( "shape=" + shape );
          // octet 16
          scalefactorradius = raf.read();
          // octets 17-20
          scaledvalueradius = GribNumbers.int4(raf);
          // octet 21
          scalefactormajor = raf.read();
          // octets 22-25
          scaledvaluemajor = GribNumbers.int4(raf);
          // octet 26
          scalefactorminor = raf.read();
          // octets 27-30
          scaledvalueminor = GribNumbers.int4(raf);
          // octets 31-34
          nx = GribNumbers.int4(raf);
          //System.out.println( "nx=" + nx);
          // octets 35-38
          ny = GribNumbers.int4(raf);
          //System.out.println( "ny=" + ny);
          // octets 39-42
          basicAngle = GribNumbers.int4(raf);
          // octets 43-46
          subdivisionsangle = GribNumbers.int4(raf);
          if (basicAngle == 0) {
            ratio = tenToNegSix;
          } else {
            ratio = basicAngle / subdivisionsangle;
          }
          //System.out.println( "ratio =" + ratio );
          // octets 47-50
          la1 = (float) (GribNumbers.int4(raf) * ratio);
          checkSum = 7 * checkSum + la1;
          // octets 51-54
          lo1 = (float) (GribNumbers.int4(raf) * ratio);
          checkSum = 7 * checkSum + lo1;
          // octet 55
          resolution = raf.read();
          // octets 56-59
          la2 = (float) (GribNumbers.int4(raf) * ratio);
          //checkSum = 7 * checkSum + la2;
          // octets 60-63
          lo2 = (float) (GribNumbers.int4(raf) * ratio);
          //checkSum = 7 * checkSum + lo2;
          // octets 64-67
          dx = (float) (GribNumbers.int4(raf) * ratio);
          grid_units = "degrees";

          // octet 68-71
          np = GribNumbers.int4(raf);
          // octet 72
          scanMode = raf.read();

          if (gdtn == 41) {  //Rotated Gaussian Latitude/longitude
            // octets 73-76
            spLat = GribNumbers.int4(raf) * ratio;
            //checkSum = 7 * checkSum + spLat;
            // octets 77-80
            spLon = GribNumbers.int4(raf) * ratio;
            //checkSum = 7 * checkSum + spLon;
            // octets 81-84
            rotationangle = raf.readFloat();

          } else if (gdtn == 42) {  //Stretched Gaussian
            // Latitude/longitude
            // octets 73-76
            poleLat = GribNumbers.int4(raf) * ratio;
            //checkSum = 7 * checkSum + poleLat;
            // octets 77-80
            poleLon = GribNumbers.int4(raf) * ratio;
            //checkSum = 7 * checkSum + poleLon;
            // octets 81-84
            factor = GribNumbers.int4(raf);

          } else if (gdtn == 43) {  //Stretched and Rotated Gaussian
            // Latitude/longitude
            // octets 73-76
            spLat = GribNumbers.int4(raf) * ratio;
            //checkSum = 7 * checkSum + spLat;
            // octets 77-80
            spLon = GribNumbers.int4(raf) * ratio;
            //checkSum = 7 * checkSum + spLon;
            // octets 81-84
            rotationangle = raf.readFloat();
            // octets 85-88
            poleLat = GribNumbers.int4(raf) * ratio;
            //checkSum = 7 * checkSum + poleLat;
            // octets 89-92
            poleLon = GribNumbers.int4(raf) * ratio;
            //checkSum = 7 * checkSum + poleLon;
            // octets 93-96
            factor = GribNumbers.int4(raf);
          }
          break;

        case 50:
        case 51:
        case 52:
        case 53:                     // Spherical harmonic coefficients

          checkSum = 7 * checkSum + GribNumbers.UNDEFINED; // for lat
          checkSum = 7 * checkSum + GribNumbers.UNDEFINED; // for lon
          j = raf.readFloat();
          k = raf.readFloat();
          m = raf.readFloat();
          method = raf.read();
          mode = raf.read();
          grid_units = "";
          if (gdtn == 51) {         //Rotated Spherical harmonic coefficients

            spLat = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + spLat;
            spLon = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + spLon;
            rotationangle = raf.readFloat();

          } else if (gdtn == 52) {  //Stretched Spherical
            // harmonic coefficients

            poleLat = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + poleLat;
            poleLon = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + poleLon;
            factor = GribNumbers.int4(raf);

          } else if (gdtn == 53) {  //Stretched and Rotated
            // Spherical harmonic coefficients

            spLat = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + spLat;
            spLon = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + spLon;
            rotationangle = raf.readFloat();
            poleLat = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + poleLat;
            poleLon = GribNumbers.int4(raf) / tenToSix;
            //checkSum = 7 * checkSum + poleLon;
            factor = GribNumbers.int4(raf);
          }
          break;

        case 90:  // Space view perspective or orthographic

          checkSum = 7 * checkSum + GribNumbers.UNDEFINED; // for lat
          checkSum = 7 * checkSum + GribNumbers.UNDEFINED; // for lon

          shape = raf.read();
          //System.out.println( "shape=" + shape );
          scalefactorradius = raf.read();
          scaledvalueradius = GribNumbers.int4(raf);
          scalefactormajor = raf.read();
          scaledvaluemajor = GribNumbers.int4(raf);
          scalefactorminor = raf.read();
          scaledvalueminor = GribNumbers.int4(raf);
          nx = GribNumbers.int4(raf);
          //System.out.println( "nx=" + nx);
          ny = GribNumbers.int4(raf);
          //System.out.println( "ny=" + ny);
          lap = GribNumbers.int4(raf);
          //checkSum = 7 * checkSum + lap;
          lop = GribNumbers.int4(raf);
          //checkSum = 7 * checkSum + lop;
          resolution = raf.read();
          dx = GribNumbers.int4(raf);
          dy = GribNumbers.int4(raf);
          grid_units = "";
          xp = (float) (GribNumbers.int4(raf) / tenToThree);
          //checkSum = 7 * checkSum + xp;
          yp = (float) (GribNumbers.int4(raf) / tenToThree);
          //checkSum = 7 * checkSum + yp;
          scanMode = raf.read();
          angle = GribNumbers.int4(raf);
          //altitude = GribNumbers.int4( raf ) * 1000000;
          altitude = GribNumbers.int4(raf);
          //checkSum = 7 * checkSum + altitude;
          xo = GribNumbers.int4(raf);
          //checkSum = 7 * checkSum + xo;
          yo = GribNumbers.int4(raf);
          //checkSum = 7 * checkSum + yo;

          break;

        case 100:  // Triangular grid based on an icosahedron
          // TODO: check usage
          checkSum = 7 * checkSum + GribNumbers.UNDEFINED; // for lat
          checkSum = 7 * checkSum + GribNumbers.UNDEFINED; // for lon

          n2 = raf.read();
          //checkSum = 7 * checkSum + n2;
          n3 = raf.read();
          //checkSum = 7 * checkSum + n3;
          ni = GribNumbers.int2(raf);
          //checkSum = 7 * checkSum + ni;
          nd = raf.read();
          //checkSum = 7 * checkSum + nd;
          poleLat = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + poleLat;
          poleLon = GribNumbers.int4(raf) / tenToSix;
          //checkSum = 7 * checkSum + poleLon;
          lonofcenter = GribNumbers.int4(raf);
          position = raf.read();
          order = raf.read();
          scanMode = raf.read();
          int n = GribNumbers.int4(raf);
          grid_units = "";
          break;

        case 110:  // Equatorial azimuthal equidistant projection
          shape = raf.read();
          //System.out.println( "shape=" + shape );
          scalefactorradius = raf.read();
          scaledvalueradius = GribNumbers.int4(raf);
          scalefactormajor = raf.read();
          scaledvaluemajor = GribNumbers.int4(raf);
          scalefactorminor = raf.read();
          scaledvalueminor = GribNumbers.int4(raf);
          nx = GribNumbers.int4(raf);
          //System.out.println( "nx=" + nx);
          ny = GribNumbers.int4(raf);
          //System.out.println( "ny=" + ny);
          la1 = GribNumbers.int4(raf) / tenToSix;
          checkSum = 7 * checkSum + la1;
          lo1 = GribNumbers.int4(raf) / tenToSix;
          checkSum = 7 * checkSum + lo1;
          resolution = raf.read();
          dx = (float) (GribNumbers.int4(raf) / tenToThree);
          dy = (float) (GribNumbers.int4(raf) / tenToThree);
          grid_units = "";
          projectionCenter = raf.read();
          scanMode = raf.read();

          break;

        case 120:  // Azimuth-range Projection
          nb = GribNumbers.int4(raf);
          //checkSum = 7 * checkSum + nb;
          nr = GribNumbers.int4(raf);
          //checkSum = 7 * checkSum + nr;
          la1 = GribNumbers.int4(raf);
          checkSum = 7 * checkSum + la1;
          lo1 = GribNumbers.int4(raf);
          checkSum = 7 * checkSum + lo1;
          dx = GribNumbers.int4(raf);
          grid_units = "";
          dstart = raf.readFloat();
          scanMode = raf.read();
          for (int i = 0; i < nr; i++) {
            // get azi (33+4(Nr-1))-(34+4(Nr-1))
            // get adelta (35+4(Nr-1))-(36+4(Nr-1))
          }
          System.out.println("need code to get azi and adelta");

          break;

        case 204:  // Curvilinear orthographic

          checkSum = 7 * checkSum + GribNumbers.UNDEFINED; // for lat
          checkSum = 7 * checkSum + GribNumbers.UNDEFINED; // for lon

          shape = raf.read();
          //System.out.println( "shape=" + shape );
          scalefactorradius = raf.read();
          scaledvalueradius = GribNumbers.int4(raf);
          scalefactormajor = raf.read();
          scaledvaluemajor = GribNumbers.int4(raf);
          scalefactorminor = raf.read();
          scaledvalueminor = GribNumbers.int4(raf);
          nx = GribNumbers.int4(raf);
          //System.out.println( "nx=" + nx);
          ny = GribNumbers.int4(raf);
          //System.out.println( "ny=" + ny);
          // octets 39 - 54 not used, set to 0
          byte[] dst = new byte[16];
          raf.read(dst);
          resolution = raf.read();
          // octets 56 - 71 not used
          raf.read(dst);
          scanMode = raf.read();
          grid_units = "";
          break;
        default:
          System.out.println("Unknown Grid Type "
              + Integer.toString(gdtn));

      }  // end switch

      // calculate earth radius
      if (((gdtn < 50) || (gdtn > 53)) && (gdtn != 100) && (gdtn != 120)) {
        if (shape == 0) {
          earthRadius = 6367470;
        } else if (shape == 1) {
          earthRadius = scaledvalueradius;
          if (scalefactorradius != 0) {
            earthRadius /= Math.pow(10, scalefactorradius);
          }
        } else if (shape == 2) {
          majorAxis = (float) 6378160.0;
          minorAxis = (float) 6356775.0;
        } else if (shape == 3) {
          majorAxis = scaledvaluemajor;
          //System.out.println( "majorAxisScale =" + scalefactormajor );
          //System.out.println( "majorAxisiValue =" + scaledvaluemajor );
          majorAxis /= Math.pow(10, scalefactormajor);

          minorAxis = scaledvalueminor;
          //System.out.println( "minorAxisScale =" + scalefactorminor );
          //System.out.println( "minorAxisValue =" + scaledvalueminor );
          minorAxis /= Math.pow(10, scalefactorminor);
        } else if (shape == 4) {
          majorAxis = (float) 6378137.0;
          minorAxis = (float) 6356752.314;
        } else if (shape == 6) {
          earthRadius = 6371229;
        }
      }
      // This is a quasi-regular grid, save the number of pts in each parallel
      if (olon != 0) {
        //System.out.println( "olon ="+ olon +" iolon ="+ iolon );
        int numPts;
        if ((scanMode & 32) == 0) {
          numPts = ny;
        } else {
          numPts = nx;
        }
        olonPts = new int[numPts];
        //int count = 0;
        maxPts = 0;
        if (olon == 1) {
          for (int i = 0; i < numPts; i++) {
            olonPts[i] = raf.read();
            if (maxPts < olonPts[i]) {
              maxPts = olonPts[i];
            }
            //count += olonPts[ i ];
            //System.out.println( "parallel =" + i +" number pts ="+ latPts );
          }
        } else if (olon == 2) {
          for (int i = 0; i < numPts; i++) {
            olonPts[i] = raf.readUnsignedShort();
            if (maxPts < olonPts[i]) {
              maxPts = olonPts[i];
            }
            //count += olonPts[ i ];
            //System.out.println( "parallel =" + i +" number pts ="+ latPts );
          }
        }
        if ((scanMode & 32) == 0) {
          nx = maxPts;
        } else {
          ny = maxPts;
        }
        //double lodiff = gds.getLo2() - gds.getLo1();
        dx = (float) (lo2 - lo1) / (nx - 0);
        //System.out.println( "total number pts ="+ count );
      }

      //gdskey = Double.toString(checkSum).hashCode();
      gdskey = gdsVars.getGdsKey();
      //checksum = Integer.toString(gdskey);
    }  // end of Grib2GridDefinitionSection


  } // end GdsReader2


  ///////////////////////////////////////////////////////////////////////////////////////
  /**
   * A class representing the product definition section (PDS) of a GRIB record.
   */

  final class PdsReader1 {

    /**
     * Length in bytes of this PDS.
     */
    public final int length;

    /**
     * Exponent of decimal scale.
     */
    public final int decscale;

    /**
     * ID of grid type.
     */
    public final int grid_id;

    /**
     * True, if GDS exists.
     */
    public final boolean gds_exists;

    /**
     * True, if BMS exists.
     */
    public final boolean bms_exists;

    /**
     * The parameter as defined in the Parameter Table.
     */
    public final GridParameter parameter;

    /**
     * parameterNumber.
     */
    public final int parameterNumber;

    /**
     * Class containing the information about the level.  This helps to actually
     * use the data, otherwise the string for level will have to be parsed.
     */
    public final GribPDSLevel level;

    /**
     * Model Run/Analysis/Reference time.
     */
    public final Date baseTime;

    /**
     * _more_
     */
    public final long refTime;

    /**
     * Forecast time (valid time).
     */
    public int forecastTime;

    /**
     * Forecast time. (valid time 1)
     * Also used as starting time when times represent a period.
     */
    public int p1;

    /**
     * Ending time when times represent a period (valid time 2).
     */
    public int p2;

    /**
     * Strings used in building a string to represent the time(s) for this PDS
     * See the decoder for octet 21 to get an understanding.
     */
    public String timeRange = null;

    /**
     * _more_
     */
    public final int timeRangeValue;

    /**
     * _more_
     */
    public String tUnit = null;

    /**
     * Parameter Table Version number.
     */
    public final int table_version;

    /**
     * Identification of center e.g. 88 for Oslo.
     */
    public final int center_id;

    /**
     * Identification of subcenter.
     */
    public final int subcenter_id;

    /**
     * Identification of Generating Process.
     */
    public final int typeGenProcess;

    /**
     * See GribPDSParamTable class for details.
     */
    public final GribPDSParamTable parameter_table;

    /**
     * PDS length not equal to number bytes read.
     */
    public final boolean lengthErr = false;

    /**
     * PDS as Variables from a byte[]
     */
    public final Grib1PDSVariables pdsVars;
    // *** constructors *******************************************************

    /**
     * Constructs a <tt>Grib1ProductDefinitionSection</tt> object from a raf.
     *
     * @param raf with PDS content
     * @throws java.io.IOException if raf can not be opened etc.
     * @throws ucar.grib.NotSupportedException
     *                             if raf contains no valid GRIB file
     */
    public PdsReader1(RandomAccessFile raf)
        throws IOException {

      long fpStart = raf.getFilePointer();

      // octets 1-3 PDS length
      length = GribNumbers.uint3(raf);
      //System.out.println( "PDS length = " + length );

      // read in whole PDS as byte[]
      byte[] pdsData = new byte[length];
      // reset to beginning of section and read data
      raf.skipBytes(-3);
      raf.read(pdsData);
      pdsVars = new Grib1PDSVariables(pdsData);

      // reset for variable section read and set sectionEnd
      raf.seek(fpStart + 3);

      // Paramter table octet 4
      table_version = raf.read();

      // Center  octet 5
      center_id = raf.read();

      // octet 6 Generating Process - See Table A
      typeGenProcess = raf.read();

      // octet 7 (id of grid type) - not supported yet
      grid_id = raf.read();

      // octet 8 (flag for presence of GDS and BMS)
      int exists = raf.read();
      gds_exists = (exists & 128) == 128;
      bms_exists = (exists & 64) == 64;

      // octet 9 (parameter and unit)
      parameterNumber = raf.read();

      // octets 10-12 (level)
      int levelType = raf.read();
      int levelValue1 = raf.read();
      int levelValue2 = raf.read();
      level = new GribPDSLevel(levelType, levelValue1, levelValue2);

      // octets 13-17 (base time for reference time)
      int year = raf.read();
      int month = raf.read();
      int day = raf.read();
      int hour = raf.read();
      int minute = raf.read();

      // get info for forecast time
      // octet 18 Forecast time unit
      int fUnit = raf.read();

      switch (fUnit) {

        case 0:    // minute
          tUnit = "minute";
          break;

        case 1:    // hours
          tUnit = "hour";
          break;

        case 2:    // day
          tUnit = "day";
          break;

        case 3:    // month
          tUnit = "month";
          break;

        case 4:    //1 year
          tUnit = "1year";
          break;

        case 5:    // decade
          tUnit = "decade";
          break;

        case 6:    // normal
          tUnit = "day";
          break;

        case 7:    // century
          tUnit = "century";
          break;

        case 10:   //3 hours
          tUnit = "3hours";
          break;

        case 11:   // 6 hours
          tUnit = "6hours";
          break;

        case 12:   // 12 hours
          tUnit = "12hours";
          break;

        case 254:  // second
          tUnit = "second";
          break;

        default:
          System.err.println("PDS: Time Unit " + fUnit
              + " is not yet supported");
      }

      // octet 19 & 20 used to create Forecast time
      p1 = raf.read();
      p2 = raf.read();

      // octet 21 (time range indicator)
      timeRangeValue = raf.read();
      // forecast time is always at the end of the range
      //System.out.println( "PDS timeRangeValue =" + timeRangeValue );
      switch (timeRangeValue) {

        case 0:
          timeRange = "product valid at RT + P1";
          forecastTime = p1;
          break;

        case 1:
          timeRange = "product valid for RT, P1=0";
          forecastTime = 0;
          break;

        case 2:
          timeRange = "product valid from (RT + P1) to (RT + P2)";
          forecastTime = p2;
          break;

        case 3:
          timeRange =
              "product is an average between (RT + P1) to (RT + P2)";
          forecastTime = p2;
          break;

        case 4:
          timeRange =
              "product is an accumulation between (RT + P1) to (RT + P2)";
          forecastTime = p2;
          break;

        case 5:
          timeRange =
              "product is the difference (RT + P2) - (RT + P1)";
          forecastTime = p2;
          break;

        case 6:
          timeRange = "product is an average from (RT - P1) to (RT - P2)";
          forecastTime = -p2;
          break;

        case 7:
          timeRange = "product is an average from (RT - P1) to (RT + P2)";
          forecastTime = p2;
          break;

        case 10:
          timeRange = "product valid at RT + P1";
          // p1 really consists of 2 bytes p1 and p2
          forecastTime = p1 = GribNumbers.int2(p1, p2);
          p2 = 0;
          break;

        case 51:
          timeRange = "mean value from RT to (RT + P2)";
          forecastTime = p2;
          break;

        case 113:
          timeRange = "Average of N forecasts, forecast period of P1, reference intervals of P2";
          forecastTime = p1;
          break;

        default:
          System.err.println("PDS: Time Range Indicator "
              + timeRangeValue + " is not yet supported");
      }

      // octet 22 & 23
      int avgInclude = GribNumbers.int2(raf);

      // octet 24
      int avgMissing = raf.read();

      // octet 25
      int century = raf.read() - 1;
      if (century == -1) century = 20;

      // octet 26, sub center
      subcenter_id = raf.read();

      // octets 27-28 (decimal scale factor)
      decscale = GribNumbers.int2(raf);

      Calendar calendar = Calendar.getInstance();
      calendar.clear();
      calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
      calendar.set(Calendar.DST_OFFSET, 0);
      calendar.set((century * 100 + year), month - 1, day, hour, minute, 0);

      baseTime = calendar.getTime();
      refTime = calendar.getTimeInMillis();

      parameter_table = GribPDSParamTable.getParameterTable(center_id,
          subcenter_id, table_version);
      parameter = parameter_table.getParameter(parameterNumber);

      Grib1PDSVariables gpv = pdsVars;
        if (  gpv.isEnsemble() ) {
          System.out.println("Parm ="+ parameterNumber
                  +" Extension ="+ gpv.getExtension()
                  +" Type ="+ gpv.getType()
                  +" ID ="+ gpv.getID()
                  +" ProductID ="+ gpv.getProductID()
                  +" SpatialorProbability ="+ gpv.getSpatialorProbability() );
      } else if (length != 28) {                      // check if all bytes read in section
        //lengthErr = true;
        int extra;
        for (int i = 29; i <= length; i++) {
          extra = raf.read();
        }
      }

    }  // end PDSReader1

    /**
     * Get the numeric value for this level.
     *
     * @return name of level (height or pressure)
     */
    public final int getLevelType() {
      return level.getIndex();
    }

    /**
     * Get the numeric value for this level.
     *
     * @return name of level (height or pressure)
     */
    public final float getLevelValue1() {
      return level.getValue1();
    }

    /**
     * Get value 2 (if it exists) for this level.
     *
     * @return name of level (height or pressure)
     */
    public final float getLevelValue2() {
      return level.getValue2();
    }
  } // end PDSReader1


  ////////////////////////////////////////////////////////////////////////////////////////
  private final class GdsReader1 {

    /**
     * Length in bytes of this section.
     */
    public int length;

    public int NV;

    /**
     * P(V|L).
     * PV - list of vertical coordinate parameters.
     * PL - list of numbers of points in each row.
     * or 255 missing.
     */
    public int P_VorL;

    /**
     * _more_
     */
    public int[] numPV;

    /**
     * Is this a thin grid.
     */
    public boolean isThin = false;

    /**
     * Type of grid (See table 6)ie 1 == Mercator Projection Grid.
     */
    public int type;

    /**
     * Grid name.
     */
    public String name = "";

    /**
     * Number of grid columns. (Also Ni).
     */
    public int nx;

    /**
     * Number of grid rows. (Also Nj).
     */
    public int ny;

    /**
     * Latitude of grid start point.
     */
    public float lat1;

    /**
     * Longitude of grid start point.
     */
    public float lon1;

    /**
     * Latitude of grid last point.
     */
    public float lat2;

    /**
     * Longitude of grid last point.
     */
    public float lon2;

    /**
     * orientation of the grid.
     */
    public float lov;

    /**
     * Resolution of grid (See table 7).
     */
    public int resolution;

    /**
     * x-distance between two grid points
     * can be delta-Lon or delta x.
     */
    public float dx;

    /**
     * y-distance of two grid points
     * can be delta-Lat or delta y.
     */
    public float dy;

    /**
     * units of the dx and dy variables
     */
    public String grid_units;

    /**
     * Number of parallels between a pole and the equator.
     */
    public int np;

    /**
     * Scanning mode (See table 8).
     */
    public int scan;

    /**
     * Projection Center Flag.
     */
    public int proj_center;

    /**
     * Latin 1 - The first latitude from pole at which secant cone cuts the
     * sperical earth.  See Note 8 of ON388.
     */
    public float latin1;

    /**
     * Latin 2 - The second latitude from pole at which secant cone cuts the
     * sperical earth.  See Note 8 of ON388.
     */
    public float latin2;

    /**
     * latitude of south pole.
     */
    public float latsp;

    /**
     * longitude of south pole.
     */
    public float lonsp;

    /**
     * angle of rotation.
     */
    public float angle;

    /**
     * checksum value for this gds.
     */
    public String checksum = "";
    public int gdskey;

    /**
     * GDS as Variables from a byte[]
     */
    public final Grib1GDSVariables gdsVars;

    // *** constructors *******************************************************
    /**
     * constructor
     */
    public GdsReader1() {
      gdsVars = null;
    }

    /**
     * Constructs a <tt>Grib1GridDefinitionSection</tt> object from a raf.
     *
     * @param raf RandomAccessFile with GDS content
     * @throws IOException          if RandomAccessFile has error.
     * @throws NoValidGribException if raf contains no valid GRIB info
     */
    public GdsReader1(RandomAccessFile raf, boolean doCheckSum)
        throws IOException, NoValidGribException {

      double checkSum;
      int reserved;  // used to read empty space
      long sectionEnd = raf.getFilePointer();

      // octets 1-3 (Length of GDS)
      length = GribNumbers.uint3(raf);
      if (length == 0) {  // there's a extra byte between PDS and GDS
        raf.skipBytes(-2);
        length = GribNumbers.uint3(raf);
      }

      // read in whole GDS as byte[]
      byte[] gdsData = new byte[length];
      // reset to beginning of section and read data
      raf.skipBytes(-3);
      raf.read(gdsData);
      gdsVars = new Grib1GDSVariables(gdsData);
      // reset for variable section read and set sectionEnd
      raf.seek(sectionEnd + 3);
      sectionEnd += length;

      //System.out.println( "GDS length = " + length );

      // octets 4 NV
      NV = raf.read();
      //System.out.println( "GDS NV = " + NV );

      // octet 5 PL the location (octet number) of the list of numbers of points in each row
      P_VorL = raf.read();
      //System.out.println( "GDS PL = " + P_VorL );

      // octet 6 (grid type)
      type = raf.read();
      checkSum = type;
      //System.out.println( "GDS grid type = " + type );
      //name = getName(type);

      if (type != 50) {  // values same up to resolution

        // octets 7-8 (Nx - number of points along x-axis)
        nx = raf.readShort();
        nx = (nx == -1)
            ? 1
            : nx;
        //checkSum = 7 * checkSum + nx;

        // octets 9-10 (Ny - number of points along y-axis)
        ny = raf.readShort();
        ny = (ny == -1)
            ? 1
            : ny;
        //checkSum = 7 * checkSum + ny;

        // octets 11-13 (La1 - latitude of first grid point)
        //lat1 = GribNumbers.int3(raf) / 1000.0;
        lat1 = GribNumbers.int3(raf) / tenToThree;
        checkSum = 7 * checkSum + lat1;

        // octets 14-16 (Lo1 - longitude of first grid point)
        lon1 = GribNumbers.int3(raf) / tenToThree;
        checkSum = 7 * checkSum + lon1;

        // octet 17 (resolution and component flags).  See Table 7
        resolution = raf.read();
      }

      switch (type) {

        //  Latitude/Longitude  grids ,  Arakawa semi-staggered e-grid rotated
        //  Arakawa filled e-grid rotated
        case 0:
        case 4:
        case 10:
        case 40:
        case 201:
        case 202:

          grid_units = "degrees";

          // octets 18-20 (La2 - latitude of last grid point)
          lat2 = GribNumbers.int3(raf) / tenToThree;
          //checkSum = 7 * checkSum + lat2;

          // octets 21-23 (Lo2 - longitude of last grid point)
          lon2 = GribNumbers.int3(raf) / tenToThree;
          //checkSum = 7 * checkSum + lon2;

          // octets 24-25 (Dx - Longitudinal Direction Increment )
          //dx = raf.readShort() / tenToThree;
          dx = smartRead(raf, 16, 1000);
          if (dx == -1 || dx == GribNumbers.UNDEFINED) {
          //return calculateDx();
            int add = 0;
            if (lon2 < lon1)
              add = 360;
            dx = (float) ((lon2 + add) - lon1) / (nx - 1);
          }
          // octets 26-27 (Dy - Latitudinal Direction Increment )
          //               Np - parallels between a pole and the equator
          if (type == 4) {
            np = raf.readShort();
          } else {
            //dy = raf.readShort() / tenToThree;
            dy = smartRead(raf, 16, 1000);
          }

          // octet 28 (Scanning mode)  See Table 8
          scan = raf.read();

          // octet 29-32 reserved
          reserved = raf.readInt();

          if (type == 10) {  //rotated
            // octets 33-35 (lat of southern pole)
            latsp = GribNumbers.int3(raf) / tenToThree;
            //checkSum = 7 * checkSum + latsp;

            // octets 36-38 (lon of southern pole)
            lonsp = GribNumbers.int3(raf) / tenToThree;
            //checkSum = 7 * checkSum + lonsp;

            // octet 35-42 reserved
            //angle = raf.readFloat();
            angle = GribNumbers.float4(raf);

          }

          if (P_VorL != 255) {
            if (NV == 0 || NV == 255) {
              getPL(raf);
            } else {
              getPV(NV, raf);
            }
          }
          break;  // end Latitude/Longitude grids

        case 1:    //  Mercator grids

          grid_units = "m";

          // octets 18-20 (La2 - latitude of last grid point)
          lat2 = GribNumbers.int3(raf) / tenToThree;
          //checkSum = 7 * checkSum + lat2;

          // octets 21-23 (Lo2 - longitude of last grid point)
          lon2 = GribNumbers.int3(raf) / tenToThree;
          //checkSum = 7 * checkSum + lon2;

          // octets 24-26 (Latin - latitude where cylinder intersects the earth
          latin1 = GribNumbers.int3(raf) / tenToThree;
          //checkSum = 7 * checkSum + latin1;

          // octet 27 reserved
          reserved = raf.read();

          // octet 28 (Scanning mode)  See Table 8
          scan = raf.read();

          // octets 29-31 (Dx - Longitudinal Direction Increment )
          dx = GribNumbers.int3(raf);

          // octets 32-34 (Dx - Longitudinal Direction Increment )
          dy = GribNumbers.int3(raf);

          // octet 35-42 reserved
          reserved = raf.readInt();
          reserved = raf.readInt();
          if (P_VorL != 255) {
            if (NV == 0 || NV == 255) {
              getPL(raf);
            } else {
              getPV(NV, raf);
            }
          }

          break;  // end Mercator grids

        case 3:    // Lambert Conformal

          grid_units = "m";

          // octets 18-20 (Lov - Orientation of the grid - east lon parallel to y axis)
          lov = GribNumbers.int3(raf) / tenToThree;
          //checkSum = 7 * checkSum + lov;

          // octets 21-23 (Dx - the X-direction grid length) See Note 2 of Table D
          dx = GribNumbers.int3(raf);

          // octets 24-26 (Dy - the Y-direction grid length) See Note 2 of Table D
          dy = GribNumbers.int3(raf);

          // octets 27 (Projection Center flag) See Note 5 of Table D
          proj_center = raf.read();

          // octet 28 (Scanning mode)  See Table 8
          scan = raf.read();

          // octets 29-31 (Latin1 - first lat where secant cone cuts spherical earth
          latin1 = GribNumbers.int3(raf) / tenToThree;
          //checkSum = 7 * checkSum + latin1;

          // octets 32-34 (Latin2 - second lat where secant cone cuts spherical earth)
          latin2 = GribNumbers.int3(raf) / tenToThree;
          //checkSum = 7 * checkSum + latin2;

          // octets 35-37 (lat of southern pole)
          latsp = GribNumbers.int3(raf) / tenToThree;
          //checkSum = 7 * checkSum + latsp;

          // octets 38-40 (lon of southern pole)
          lonsp = GribNumbers.int3(raf) / tenToThree;
          //checkSum = 7 * checkSum + lonsp;

          // octets 41-42
          reserved = raf.readShort();
          if (P_VorL != 255) {
            if (NV == 0 || NV == 255) {
              getPL(raf);
            } else {
              getPV(NV, raf);
            }
          }

          break;  // end Lambert Conformal

        case 5:    //  Polar Stereographic grids
        case 87:

          grid_units = "m";

          // octets 18-20 (Lov - Orientation of the grid - east lon parallel to y axis)
          lov = GribNumbers.int3(raf) / tenToThree;
          //checkSum = 7 * checkSum + lov;

          if (type == 87) {
            lon2 = GribNumbers.int3(raf) / tenToThree;
            //checkSum = 7 * checkSum + lon2;
          }

          // octets 21-23 (Dx - Longitudinal Direction Increment )
          dx = GribNumbers.int3(raf);

          // octets 24-26(Dy - Latitudinal Direction Increment )
          dy = GribNumbers.int3(raf);

          // octets 27 (Projection Center flag) See Note 5 of Table D
          proj_center = raf.read();

          // octet 28 (Scanning mode)  See Table 8
          scan = raf.read();

          // octet 29-32 reserved
          reserved = GribNumbers.int4(raf);
          if (P_VorL != 255) {
            if (NV == 0 || NV == 255) {
              getPL(raf);
            } else {
              getPV(NV, raf);
            }
          }

          break;  // end Polar Stereographic grids

        default:
          System.out.println("Unknown Grid Type : " + type);
          throw new NoValidGribException("GDS: Unknown Grid Type : " + type
              + ") is not supported.");

      }             // end switch grid_type
      //gdskey = Double.toString(checkSum).hashCode();
      gdskey = gdsVars.getGdsKey();
      checksum = Integer.toString(gdskey);

      //System.out.println( "scan=" + scan );
      if ((scan & 63) != 0) {
        throw new NoValidGribException("GDS: This scanning mode (" + scan
            + ") is not supported.");
      }

      // seek to end of section no matter what
      raf.seek(sectionEnd);

    }  // end Grib1GridDefinitionSection( raf )

    /**
     * Check if read returns a missing value by all bits set to 1's
     *
     * @param raf     RandomAccessFile
     * @param bits    number of bits in read
     * @param divider
     * @throws IOException _more_
     */
    private float smartRead(RandomAccessFile raf, int bits, float divider) throws IOException {

      if (bits == 16) {
        short s = raf.readShort();
        if (s == -1) {
          return GribNumbers.UNDEFINED;
        } else {
          return s / divider;
        }
      } else if (bits == 24) { //TODO: check before using
        int i = GribNumbers.int3(raf);
        if (i == -1) {
          return GribNumbers.UNDEFINED;
        } else {
          return i / divider;
        }
      }
      return GribNumbers.UNDEFINED;
    }

    /**
     * Gets the number of points in each parallel for Quasi/Thin grids
     *
     * @param raf RandomAccessFile
     * @throws IOException _more_
     */
    private void getPL(RandomAccessFile raf) throws IOException {

      isThin = true;
      int numPts;

      if ((scan & 32) == 0) {
        numPts = ny;
      } else {
        numPts = nx;
      }
      //System.out.println( "GDS length ="+ length );
      //System.out.println( "GDS  numPts = " + numPts);
      //int count = 0;
      int maxPts = 0;
      numPV = new int[numPts];
      for (int i = 0; i < numPts; i++) {
        numPV[i] = raf.readUnsignedShort();
        //numPV[i] = raf.read();
        if (maxPts < numPV[i]) {
          maxPts = numPV[i];
        }
        //count += numPV[ i ];
        //System.out.println( "parallel =" + i +" number pts ="+ numPV[i] );
      }
      if ((scan & 32) == 0) {
        nx = maxPts;
      } else {
        ny = maxPts;
      }
      int add = 0;
      if (lon2 < lon1)
        add = 360;
      //lon2 += 360;
      //dx = (float) (lon2 - lon1) / (nx - 0);
      dx = (float) ((lon2 + add) - lon1) / (nx - 1);
      //System.out.println( "maxPts ="+ maxPts );
      //System.out.println( "total number pts ="+ count );
    }

    /**
     * Gets the number of vertical coordinate for this parameter and converts
     * them to pressure coordinates.
     *
     * @param NV  number of vertical coordinates
     * @param raf RandomAccessFile
     * @throws IOException _more_
     */
    private void getPV(int NV, RandomAccessFile raf) throws IOException {
       if ( 0 < 1 )  // don't have pds.getPdsVars().getValueFirstFixedSurface() so can't do calculation
           return;
      // Documentation for the conversion process is at:
      // http://www.ecmwf.int/research/ifsdocs/DYNAMICS/Chap2_Discretization4.html
      // read data
      float[] numPV = new float[NV];
      for (int i = 0; i < NV; i++) {
        numPV[i] = GribNumbers.float4(raf);
        //System.out.println( "a and b values [ " + i +" ] ="+ numPV[i] );
      }
      // calculate half layers
      int nlevels = NV / 2;
      double[] pressure0p5 = new double[nlevels];
      for (int i = 0; i < nlevels; i++) {
        //pressure0p5[ i ] = numPV[ i ] + numPV[ i +nlevels ] * pds.getPdsVars().getValueFirstFixedSurface();
        //System.out.println( "Pressure 0p5 [ "+ i +" ] ="+ pressure0p5[ i ] );
      }
      // average adjacent half layers
      double[] pressureLevel = new double[nlevels - 1];
      for (int i = 0; i < nlevels - 1; i++) {
        pressureLevel[i] = (pressure0p5[i] + pressure0p5[i + 1]) * 0.5;
        //System.out.println( "Pressure [ "+ i +" ] ="+ pressureLevel[ i ] );
      }
    }
  }


  //////////////////////////////////////////////////////////
  private final class Input1 {
/*
     * raf for grib file
     */

    /** _more_          */
    private ucar.unidata.io.RandomAccessFile raf = null;

    /*
     * the header of Grib record
     */

    /** _more_          */
    private String header = "GRIB1";


    /*
     * stores records of Grib file, records consist of objects for each section.
     * there are 5 sections to a Grib1 record.
     */

    /** _more_          */
    private final ArrayList<Grib1Record> records = new ArrayList<Grib1Record>();

    /*
     * stores products of Grib file, products have enough info to get the
     * metadata about a parameter and the data. products are lightweight
     * records.
     */

    /** _more_          */
    private final ArrayList<Grib1Product> products = new ArrayList<Grib1Product>();

    /*
     * stores GDS of Grib1 file, there is possibility of more than 1
     */

    /** _more_          */
    private final HashMap<String,Grib1GridDefinitionSection> gdsHM =
        new HashMap<String,Grib1GridDefinitionSection>();

    // *** constructors *******************************************************

    /**
     * Constructs a <tt>Grib1Input</tt> object from a raf.
     *
     * @param raf with GRIB content
     *
     */
    public  Input1(RandomAccessFile raf) {
        this.raf = raf;
    }

    /**
     * scans a Grib file to gather information that could be used to
     * create an index or dump the metadata contents.
     *
     * @param getProducts products have enough information for data extractions
     * @param oneRecord returns after processing one record in the Grib file
     * @throws NoValidGribException _more_
     * @throws NotSupportedException
     * @throws IOException   if raf does not contain a valid GRIB record
     */
    public final void scan(boolean getProducts, boolean oneRecord)
            throws NotSupportedException, NoValidGribException, IOException {
        long start = System.currentTimeMillis();
        // stores the number of times a particular GDS is used
        HashMap<String,String> gdsCounter = new HashMap<String,String>();
        Grib1ProductDefinitionSection pds        = null;
        Grib1GridDefinitionSection    gds        = null;
        long pdsOffset = 0;
        long gdsOffset = 0;

        //System.out.println("file position =" + raf.getFilePointer());
        while (raf.getFilePointer() < raf.length()) {
            if (seekHeader(raf, raf.length())) {
                // Read Section 0 Indicator Section
                Grib1IndicatorSection is = new Grib1IndicatorSection(raf);
                //System.out.println( "Grib record length=" + is.getGribLength());
                // EOR (EndOfRecord) calculated so skipping data sections is faster
                long EOR = raf.getFilePointer() + is.getGribLength()
                           - is.getLength();

                // skip Grib 2 records in a Grib 1 file
                if( is.getGribEdition() == 2 ) {
                    //System.out.println( "Error Grib 2 record in Grib1 file" ) ;
                    raf.seek(EOR);
                    continue;
                }

                long dataOffset = 0;
                try { // catch all exceptions and seek to EOR

                    // Read Section 1 Product Definition Section PDS
                    pdsOffset = raf.getFilePointer();
                    pds = new Grib1ProductDefinitionSection(raf);

                    if (pds.getPdsVars().gdsExists()) {
                        // Read Section 2 Grid Definition Section GDS
                        gdsOffset = raf.getFilePointer();
                        gds = new Grib1GridDefinitionSection(raf);

                    } else {  // GDS doesn't exist so make one
                        //System.out.println("GribRecord: No GDS included.");
                        //System.out.println("Process ID:" + pds.getProcess_Id() );
                        //System.out.println("Grid ID:" + pds.getGrid_Id() );
                        gdsOffset = -1;
                        gds = (Grib1GridDefinitionSection) new Grib1Grid(pds);
                    }
                    // obtain BMS or BDS offset in the file for this product
                    if (pds.getPdsVars().getCenter() == 98) {  // check for ecmwf offset by 1 bug
                        int length = GribNumbers.uint3(raf);  // should be length of BMS
                        if ((length + raf.getFilePointer()) < EOR) {
                            dataOffset = raf.getFilePointer() - 3;  // ok
                        } else {
                            //System.out.println("ECMWF off by 1 bug" );
                            dataOffset = raf.getFilePointer() - 2;
                        }
                    } else {
                        dataOffset = raf.getFilePointer();
                    }

                } catch( Exception e ) {
                    //.println( "Caught Exception scannning record" );
                    e.printStackTrace();
                    raf.seek(EOR);
                    continue;
                }

                // position filePointer to EndOfRecord
                raf.seek(EOR);
                //System.out.println("file offset = " + raf.getFilePointer());

                // assume scan ok
                if (getProducts) {
                    Grib1Product gp = new Grib1Product(header, pds,
                                          getGDSkey(gds, gdsCounter), gds.getGdsKey(),
                                          pdsOffset, gdsOffset );
                    products.add(gp);
                } else {
                    Grib1Record gr = new Grib1Record(header, is, pds, gds,
                                         dataOffset, raf.getFilePointer());
                    records.add(gr);
                }
                if (oneRecord) {
                    return;
                }

                // early return because ending "7777" missing
                if (raf.getFilePointer() > raf.length()) {
                    raf.seek(0);
                    System.err.println(
                        "Grib1Input: possible file corruption");
                    // TODO: if this needed
                    //checkGDSkeys(gds, gdsCounter);
                    return;
                }

            }  // end if seekHeader
            //System.out.println( "raf.getFilePointer()=" + raf.getFilePointer());
            //System.out.println( "raf.length()=" + raf.length() );
        }  // end while raf.getFilePointer() < raf.length()
        //System.out.println("GribInput: processed in " +
        //   (System.currentTimeMillis()- start) + " milliseconds");
        // TODO: if this needed
        //checkGDSkeys(gds, gdsCounter);
        return;
    }  // end scan

    /**
     * Grib edition number 1, 2 or 0 not a Grib file.
     * @throws NotSupportedException
     * @throws IOException
     * @return int 0 not a Grib file, 1 Grib1, 2 Grib2
     */
    public final int getEdition() throws IOException, NotSupportedException {
        int  check  = 0;  // Not a valid Grib file
        long length = (raf.length() < 25000L)
                      ? raf.length()
                      : 25000L;
        if ( !seekHeader(raf, length)) {
            return 0;  // not valid Grib file
        }
        //  Read Section 0 Indicator Section to get Edition number
        Grib1IndicatorSection is = new Grib1IndicatorSection(raf);  // section 0
        return is.getGribEdition();
    }  // end getEdition

    /**
     * _more_
     *
     * @param raf _more_
     * @param stop _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    private boolean seekHeader(RandomAccessFile raf, long stop)
            throws IOException {
        // seek header
        StringBuffer hdr   = new StringBuffer();
        int          match = 0;

        while (raf.getFilePointer() < stop) {
            // code must be "G" "R" "I" "B"
            char c = (char) raf.read();

            hdr.append((char) c);
            if (c == 'G') {
                match = 1;
            } else if ((c == 'R') && (match == 1)) {
                match = 2;
            } else if ((c == 'I') && (match == 2)) {
                match = 3;
            } else if ((c == 'B') && (match == 3)) {
                match = 4;

                Matcher m = productID.matcher(hdr.toString());
                if (m.find()) {
                    header = m.group(1);
                } else {
                    //header = hdr.toString();
                    header = "GRIB1";
                }
                //System.out.println( "header =" + header.toString() );
                return true;
            } else {
                match = 0;  /* Needed to protect against "GaRaIaB" case. */
            }
        }
        return false;
    }  // end seekHeader

    /**
     * _more_
     *
     * @param gds _more_
     * @param gdsCounter _more_
     *
     * @return _more_
     */
    private String getGDSkey(Grib1GridDefinitionSection gds,
                             HashMap<String,String> gdsCounter) {

        //String key = gds.getCheckSum();
        String key = Integer.toString(gds.getGdsKey());
        // only Lat/Lon grids can have > 1 GDSs
        /*
        if ((gds.getGridType() == 0) || (gds.getGridType() == 4)) {
            if ( !gdsHM.containsKey(key)) {     // check if gds is already saved
                gdsHM.put(key, gds);
            }
        } else
        */

        if ( !gdsHM.containsKey(key)) {  // check if gds is already saved
            gdsHM.put(key, gds);
            gdsCounter.put(key, "1");
        } else {
            // increment the counter for this GDS
            int count = Integer.parseInt((String) gdsCounter.get(key));
            gdsCounter.put(key, Integer.toString(++count));
        }
        return key;
    }  // end getGDSkey

    /**
     * _more_
     *
     * @param gds _more_
     * @param gdsCounter _more_
     */
    private void checkGDSkeys(Grib1GridDefinitionSection gds,
                              HashMap gdsCounter) {
        /*
        // lat/lon grids can have > 1 GDSs
        if ((gds == null) || (gds.getGridType() == 0) || (gds.getGridType() == 4)) {
            return;
        }
        String bestKey = "";
        int    count   = 0;
        // find bestKey with the most counts
        for (Iterator it = gdsCounter.keySet().iterator(); it.hasNext(); ) {
            String key      = (String) it.next();
            int    gdsCount = Integer.parseInt((String) gdsCounter.get(key));
            if (gdsCount > count) {
                count   = gdsCount;
                bestKey = key;
            }
        }
        // remove best key from gdsCounter, others will be removed from gdsHM
        gdsCounter.remove(bestKey);
        // remove all GDSs using the gdsCounter
        for (Iterator it = gdsCounter.keySet().iterator(); it.hasNext(); ) {
            String key = (String) it.next();
            gdsHM.remove(key);
        }
        // reset GDS keys in products too
        for (int i = 0; i < products.size(); i++) {
            Grib1Product g1p = (Grib1Product) products.get(i);
            g1p.setGDSkey(bestKey);
            // TODO: if used set both gdskeys
        }
        return;
        */
    }  // end checkGDSkeys

    /**
     * Get products of the GRIB file.
     *
     * @return products
     */
    public final ArrayList getProducts() {
        return products;
    }

    /**
     * Get records of the GRIB file.
     *
     * @return records
     */
    public final ArrayList getRecords() {
        return records;
    }

    /**
     * Get GDS's of the GRIB file.
     *
     * @return gdsHM
     */
    public final HashMap getGDSs() {
        return gdsHM;
    }


  }

}  // end TestGribPdsGds