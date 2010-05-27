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

// $Id: Grib1Grid.java,v 1.6 2006/08/03 22:33:40 rkambic Exp $

/*
 * Grib1Grid.java  1.0  11/19/04
 * @author Robb Kambic
 *
 */

package ucar.grib.grib1;


import ucar.grib.*;

import java.io.IOException;


/**
 * A class that represents a canned grid definition section (GDS) .
 */

public final class Grib1Grid extends Grib1GridDefinitionSection {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib1Grid.class);

  /**
   * Constructs a <tt>Grib1Grid</tt> object from a pds.
   *
   * @param pds Grib1ProductDefinitionSection to formulate grib
   */
  public Grib1Grid(Grib1ProductDefinitionSection pds) {

    super();

    //int generatingProcess = pds.getTypeGenProcess();
    int gridNumber = pds.getPdsVars().getGrid_Id();

    // gdskey = 1000 + grid number
    gdskey = 1000 + gridNumber;
    //checksum = Integer.toString(gdskey);

    switch (gridNumber) {

      case 21:
      case 22:
      case 23:
      case 24: {
        type = 0;  // Latitude/Longitude
        name = getName(type);

        // (Nx - number of points along x-axis)
        nx = 37;

        // (Ny - number of points along y-axis)
        ny = 36;

        // (resolution and component flags).  See Table 7
        resolution = 0x88;

        // (Dx - Longitudinal Direction Increment )
        dx = 5.0;

        // (Dy - Latitudinal Direction Increment )
        dy = 2.5;

        grid_units = "degrees";

        // (Scanning mode)  See Table 8
        scan = 64;

        if (gridNumber == 21) {
          // (La1 - latitude of first grid point)
          lat1 = 0.0;

          // (Lo1 - longitude of first grid point)
          lon1 = 0.0;

          // (La2 - latitude of last grid point)
          lat2 = 90.0;

          // (Lo2 - longitude of last grid point)
          lon2 = 180.0;

        } else if (gridNumber == 22) {
          // (La1 - latitude of first grid point)
          lat1 = 0.0;

          // (Lo1 - longitude of first grid point)
          lon1 = -180.0;

          // (La2 - latitude of last grid point)
          lat2 = 90.0;

          // (Lo2 - longitude of last grid point)
          lon2 = 0.0;

        } else if (gridNumber == 23) {
          // (La1 - latitude of first grid point)
          lat1 = -90.0;

          // (Lo1 - longitude of first grid point)
          lon1 = 0.0;

          // (La2 - latitude of last grid point)
          lat2 = 0.0;

          // (Lo2 - longitude of last grid point)
          lon2 = 180.0;

        } else if (gridNumber == 24) {
          // (La1 - latitude of first grid point)
          lat1 = -90.0;

          // (Lo1 - longitude of first grid point)
          lon1 = -180.0;

          // (La2 - latitude of last grid point)
          lat2 = 0.0;

          // (Lo2 - longitude of last grid point)
          lon2 = 0.0;

        }
      }
      break;

      case 25:
      case 26: {
        type = 0;  // Latitude/Longitude
        name = getName(type);

        // (Nx - number of points along x-axis)
        nx = 72;

        // (Ny - number of points along y-axis)
        ny = 18;

        // (resolution and component flags).  See Table 7
        resolution = 0x88;

        // (Dx - Longitudinal Direction Increment )
        dx = 5.0;

        // (Dy - Latitudinal Direction Increment )
        dy = 5.0;

        grid_units = "degrees";

        // (Scanning mode)  See Table 8
        scan = 64;

        if (gridNumber == 25) {
          // (La1 - latitude of first grid point)
          lat1 = 0.0;

          // (Lo1 - longitude of first grid point)
          lon1 = 0.0;

          // (La2 - latitude of last grid point)
          lat2 = 90.0;

          // (Lo2 - longitude of last grid point)
          lon2 = 355.0;

        } else if (gridNumber == 26) {
          // (La1 - latitude of first grid point)
          lat1 = -90.0;

          // (Lo1 - longitude of first grid point)
          lon1 = 0.0;

          // (La2 - latitude of last grid point)
          lat2 = 0.0;

          // (Lo2 - longitude of last grid point)
          lon2 = 355.0;
        }
      }
      break;

      case 61:
      case 62:
      case 63:
      case 64: {
        type = 0;  // Latitude/Longitude
        name = getName(type);

        // (Nx - number of points along x-axis)
        nx = 91;

        // (Ny - number of points along y-axis)
        ny = 45;

        // (resolution and component flags).  See Table 7
        resolution = 0x88;

        // (Dx - Longitudinal Direction Increment )
        dx = 2.0;

        // (Dy - Latitudinal Direction Increment )
        dy = 2.0;

        grid_units = "degrees";

        // (Scanning mode)  See Table 8
        scan = 64;

        if (gridNumber == 61) {
          // (La1 - latitude of first grid point)
          lat1 = 0.0;

          // (Lo1 - longitude of first grid point)
          lon1 = 0.0;

          // (La2 - latitude of last grid point)
          lat2 = 90.0;

          // (Lo2 - longitude of last grid point)
          lon2 = 180.0;

        } else if (gridNumber == 62) {
          // (La1 - latitude of first grid point)
          lat1 = 0.0;

          // (Lo1 - longitude of first grid point)
          lon1 = -180.0;

          // (La2 - latitude of last grid point)
          lat2 = 90.0;

          // (Lo2 - longitude of last grid point)
          lon2 = 0.0;

        } else if (gridNumber == 63) {
          // (La1 - latitude of first grid point)
          lat1 = -90.0;

          // (Lo1 - longitude of first grid point)
          lon1 = 0.0;

          // (La2 - latitude of last grid point)
          lat2 = 0.0;

          // (Lo2 - longitude of last grid point)
          lon2 = 180.0;

        } else if (gridNumber == 64) {
          // (La1 - latitude of first grid point)
          lat1 = -90.0;

          // (Lo1 - longitude of first grid point)
          lon1 = -180.0;

          // (La2 - latitude of last grid point)
          lat2 = 0.0;

          // (Lo2 - longitude of last grid point)
          lon2 = 0.0;

        }
        break;
      }
      case 87: {
        type = 5;  //
        name = getName(type);
        // (Nx - number of points along x-axis)
        nx = 81;
        // (Ny - number of points along y-axis)
        ny = 62;
        // (La1 - latitude of first grid point)
        lat1 = 22.8756;
        // (Lo1 - longitude of first grid point)
        lon1 = 239.5089;
        // (resolution and component flags).  See Table 7
        resolution = 0x08;
        // octets 18-20 (Lov - Orientation of the grid - east lon parallel to y axis)
        lov = 255.000;
        // (La2 - latitude of last grid point)
        lat2 = 0.000;
        // (Dx - Longitudinal Direction Increment )
        dx = 68153.0;
        // (Dy - Latitudinal Direction Increment )
        dy = 68153.0;

        grid_units = "m";

        // (Scanning mode)  See Table 8
        scan = 64;
        break;
      }
      default:
        System.out.println("Grid " + gridNumber
            + " not configured yet");
        break;
    }
  }  // end Grib1Grid

  /**
   * Prints out GDS for records with predefined GDS
   * @param gdsKey
   */
  public static void PrintGDS( int gdsKey ) {
    int gridNumber = gdsKey - 1000;

    System.out.println(GribGridDefRecord.GDS_KEY +" = "+ Integer.toString(gdsKey));

    int gdtn = -1;
    int resolution = 0;
    int shape = 0;
    switch (gridNumber) {

      case 21:
      case 22:
      case 23:
      case 24: {
        gdtn = 0;  // Latitude/Longitude
        System.out.println(GribGridDefRecord.GRID_TYPE +" = "+ gdtn);
        System.out.println(GribGridDefRecord.GRID_NAME +" = "+ Grib1Tables.getName(gdtn));

        // (Nx - number of points along x-axis)
        //nx = 37;
        System.out.println(GribGridDefRecord.NX +" = "+ 37);
        // (Ny - number of points along y-axis)
        //ny = 36;
        System.out.println(GribGridDefRecord.NY +" = "+ 36);

        // (resolution and component flags).  See Table 7
        resolution = 0x88;
        System.out.println(GribGridDefRecord.RESOLUTION +" = "+ resolution);
        // (Dx - Longitudinal Direction Increment )
        //dx = 5.0;
        System.out.println(GribGridDefRecord.DX +" = "+ 5.0);
        // (Dy - Latitudinal Direction Increment )
        //dy = 2.5;
        System.out.println(GribGridDefRecord.DY +" = "+ 2.5);
        //grid_units = "degrees";
        System.out.println(GribGridDefRecord.GRID_UNITS +" = "+ "degrees");

        if (gridNumber == 21) {
          // (La1 - latitude of first grid point)
          //lat1 = 0.0;
          System.out.println(GribGridDefRecord.LA1 +" = "+ 0.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          System.out.println(GribGridDefRecord.LO1 +" = "+ 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 90.0;
          System.out.println(GribGridDefRecord.LA2 +" = "+ 90.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 180.0;
          System.out.println(GribGridDefRecord.LO2 +" = "+ 180.0);
        } else if (gridNumber == 22) {
          // (La1 - latitude of first grid point)
          //lat1 = 0.0;
          System.out.println(GribGridDefRecord.LA1 +" = "+ 0.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = -180.0;
          System.out.println(GribGridDefRecord.LO1 +" = "+ -180.0);
          // (La2 - latitude of last grid point)
          //lat2 = 90.0;
          System.out.println(GribGridDefRecord.LA2 +" = "+ 90.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 0.0;
          System.out.println(GribGridDefRecord.LO2 +" = "+ 0.0);
        } else if (gridNumber == 23) {
          // (La1 - latitude of first grid point)
          //lat1 = -90.0;
          System.out.println(GribGridDefRecord.LA1 +" = "+ -90.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          System.out.println(GribGridDefRecord.LO1 +" = "+ 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 0.0;
          System.out.println(GribGridDefRecord.LA2 +" = "+ 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 180.0;
          System.out.println(GribGridDefRecord.LO2 +" = "+ 180.0);
        } else if (gridNumber == 24) {
          // (La1 - latitude of first grid point)
          //lat1 = -90.0;
          System.out.println(GribGridDefRecord.LA1 +" = "+ -90.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = -180.0;
          System.out.println(GribGridDefRecord.LO1 +" = "+ -180.0);
          // (La2 - latitude of last grid point)
          //lat2 = 0.0;
          System.out.println(GribGridDefRecord.LA2 +" = "+ 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 0.0;
          System.out.println(GribGridDefRecord.LO2 +" = "+ 0.0);
        }
      }
      break;

      case 25:
      case 26: {
        gdtn = 0;  // Latitude/Longitude
        System.out.println(GribGridDefRecord.GRID_TYPE +" = "+ gdtn);
        System.out.println(GribGridDefRecord.GRID_NAME +" = "+ Grib1Tables.getName(gdtn));

        // (Nx - number of points along x-axis)
        //nx = 72;
        System.out.println(GribGridDefRecord.NX +" = "+ 72);
        // (Ny - number of points along y-axis)
        //ny = 19;
        System.out.println(GribGridDefRecord.NY +" = "+ 18);
        // (resolution and component flags).  See Table 7
        resolution = 0x88;
        System.out.println(GribGridDefRecord.RESOLUTION +" = "+ resolution);
        // (Dx - Longitudinal Direction Increment )
        //dx = 5.0;
        System.out.println(GribGridDefRecord.DX +" = "+ 5.0);
        // (Dy - Latitudinal Direction Increment )
        //dy = 5.0;
        System.out.println(GribGridDefRecord.DY +" = "+ 5.0);
        //grid_units = "degrees";
        System.out.println(GribGridDefRecord.GRID_UNITS +" = "+ "degrees");
        if (gridNumber == 25) {
          // (La1 - latitude of first grid point)
          //lat1 = 0.0;
          System.out.println(GribGridDefRecord.LA1 +" = "+ 0.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          System.out.println(GribGridDefRecord.LO1 +" = "+ 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 90.0;
          System.out.println(GribGridDefRecord.LA2 +" = "+ 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 355.0;
          System.out.println(GribGridDefRecord.LO2 +" = "+ 355.0);
        } else if (gridNumber == 26) {
          // (La1 - latitude of first grid point)
          //lat1 = -90.0;
          System.out.println(GribGridDefRecord.LA1 +" = "+ -90.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          System.out.println(GribGridDefRecord.LO1 +" = "+ 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 0.0;
          System.out.println(GribGridDefRecord.LA2 +" = "+ 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 355.0;
          System.out.println(GribGridDefRecord.LO2 +" = "+ 355.0);
        }
      }
      break;

      case 61:
      case 62:
      case 63:
      case 64: {
        gdtn = 0;  // Latitude/Longitude
        System.out.println(GribGridDefRecord.GRID_TYPE +" = "+ gdtn);
        System.out.println(GribGridDefRecord.GRID_NAME +" = "+ Grib1Tables.getName(gdtn));

        // (Nx - number of points along x-axis)
        //nx = 91;
        System.out.println(GribGridDefRecord.NX +" = "+ 91);
        // (Ny - number of points along y-axis)
        //ny = 45;
        System.out.println(GribGridDefRecord.NY +" = "+ 45);
        // (resolution and component flags).  See Table 7
        resolution = 0x88;
        System.out.println(GribGridDefRecord.RESOLUTION +" = "+ resolution);
        // (Dx - Longitudinal Direction Increment )
        //dx = 2.0;
        System.out.println(GribGridDefRecord.DX +" = "+ 2.0);
        // (Dy - Latitudinal Direction Increment )
        //dy = 2.0;
        System.out.println(GribGridDefRecord.DY +" = "+ 2.0);
        //grid_units = "degrees";
        System.out.println(GribGridDefRecord.GRID_UNITS +" = "+ "degrees");

        if (gridNumber == 61) {
          // (La1 - latitude of first grid point)
          //lat1 = 0.0;
          System.out.println(GribGridDefRecord.LA1 +" = "+ 0.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          System.out.println(GribGridDefRecord.LO1 +" = "+ 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 90.0;
          System.out.println(GribGridDefRecord.LA2 +" = "+ 90.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 180.0;
          System.out.println(GribGridDefRecord.LO2 +" = "+ 180.0);
        } else if (gridNumber == 62) {
          // (La1 - latitude of first grid point)
          //lat1 = 0.0;
          System.out.println(GribGridDefRecord.LA1 +" = "+ 0.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = -180.0;
          System.out.println(GribGridDefRecord.LO1 +" = "+ -180.0);
          // (La2 - latitude of last grid point)
          //lat2 = 90.0;
          System.out.println(GribGridDefRecord.LA2 +" = "+ 90.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 0.0;
          System.out.println(GribGridDefRecord.LO2 +" = "+ 0.0);
        } else if (gridNumber == 63) {
          // (La1 - latitude of first grid point)
          //lat1 = -90.0;
          System.out.println(GribGridDefRecord.LA1 +" = "+ -90.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          System.out.println(GribGridDefRecord.LO1 +" = "+ 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 0.0;
          System.out.println(GribGridDefRecord.LA2 +" = "+ 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 180.0;
          System.out.println(GribGridDefRecord.LO2 +" = "+ 180.0);
        } else if (gridNumber == 64) {
          // (La1 - latitude of first grid point)
          //lat1 = -90.0;
          System.out.println(GribGridDefRecord.LA1 +" = "+ -90.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = -180.0;
          System.out.println(GribGridDefRecord.LO1 +" = "+ -180.0);
          // (La2 - latitude of last grid point)
          //lat2 = 0.0;
          System.out.println(GribGridDefRecord.LA2 +" = "+ 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 0.0;
          System.out.println(GribGridDefRecord.LO2 +" = "+ 0.0);
        }
        break;
      }
      case 87: {
        gdtn = 5;  //
        System.out.println(GribGridDefRecord.GRID_TYPE +" = "+ gdtn);
        System.out.println(GribGridDefRecord.GRID_NAME +" = "+ Grib1Tables.getName(gdtn));
        // (Nx - number of points along x-axis)
        //nx = 81;
        System.out.println(GribGridDefRecord.NX +" = "+ 81);
        // (Ny - number of points along y-axis)
        //ny = 62;
        System.out.println(GribGridDefRecord.NY +" = "+ 62);
        // (La1 - latitude of first grid point)
        //lat1 = 22.8756;
        System.out.println(GribGridDefRecord.LA1 +" = "+ 22.8756);
        // (Lo1 - longitude of first grid point)
        //lon1 = 239.5089;
        System.out.println(GribGridDefRecord.LO1 +" = "+ 239.5089);
        // (resolution and component flags).  See Table 7
        resolution = 0x08;
        System.out.println(GribGridDefRecord.RESOLUTION +" = "+ resolution);
        // octets 18-20 (Lov - Orientation of the grid - east lon parallel to y axis)
        //lov = 255.000;
        System.out.println(GribGridDefRecord.LOV +" = "+ 255.0);
        // (La2 - latitude of last grid point)
        //lat2 = 0.000;
        System.out.println(GribGridDefRecord.LA2 +" = "+ 0.0);
        // (Dx - Longitudinal Direction Increment )
        //dx = 68153.0;
        System.out.println(GribGridDefRecord.DX +" = "+ 68153.0);
        // (Dy - Latitudinal Direction Increment )
        //dy = 68153.0;
        System.out.println(GribGridDefRecord.DY +" = "+ 68153.0);
        //grid_units = "m";
        System.out.println(GribGridDefRecord.GRID_UNITS +" = "+ "m");
        break;
      }
      default:
        log.error("Grid " + gridNumber + " not configured yet");
        break;
    }

    String winds = GribNumbers.isBitSet(resolution, GribNumbers.BIT_5)
        ? "Relative"
        : "True";
    System.out.println(GribGridDefRecord.WIND_FLAG +" = "+ winds);

    int res = resolution >> 6;
    if ((res == 1) || (res == 3)) {
      shape = 1;
    } else {
      shape = 0;
    }
    System.out.println(GribGridDefRecord.GRID_SHAPE_CODE +" = "+ shape);
    System.out.println(GribGridDefRecord.GRID_SHAPE +" = "+ Grib1Tables.getShapeName(shape));
    if (shape == 0) {
      System.out.println(GribGridDefRecord.RADIUS_SPHERICAL_EARTH +" = "+ 6367.47);
    } else {
      System.out.println(GribGridDefRecord.MAJOR_AXIS_EARTH +" = "+ 6378.160);
      System.out.println(GribGridDefRecord.MINOR_AXIS_EARTH +" = "+ 6356.775);
    }

  }


  /**
   * Populates a GridDefRecord according to gridNumber.
   *
   * @param ggdr   GridDefRecord
   * @param gdsKey as int
   */
  public static void PopulateGDS(GribGridDefRecord ggdr, int gdsKey) {

    int gridNumber = gdsKey - 1000;

    ggdr.addParam(GribGridDefRecord.GDS_KEY, Integer.toString(gdsKey));

    int gdtn = -1;
    int resolution = 0;
    int shape = 0;
    switch (gridNumber) {

      case 21:
      case 22:
      case 23:
      case 24: {
        gdtn = 0;  // Latitude/Longitude
        ggdr.addParam(GribGridDefRecord.GRID_TYPE, gdtn);
        ggdr.addParam(GribGridDefRecord.GRID_NAME, Grib1Tables.getName(gdtn));

        // (Nx - number of points along x-axis)
        //nx = 37;
        ggdr.addParam(GribGridDefRecord.NX, 37);
        // (Ny - number of points along y-axis)
        //ny = 36;
        ggdr.addParam(GribGridDefRecord.NY, 36);

        // (resolution and component flags).  See Table 7
        resolution = 0x88;
        ggdr.addParam(GribGridDefRecord.RESOLUTION, resolution);
        // (Dx - Longitudinal Direction Increment )
        //dx = 5.0;
        ggdr.addParam(GribGridDefRecord.DX, 5.0);
        // (Dy - Latitudinal Direction Increment )
        //dy = 2.5;
        ggdr.addParam(GribGridDefRecord.DY, 2.5);
        //grid_units = "degrees";
        ggdr.addParam(GribGridDefRecord.GRID_UNITS, "degrees");

        if (gridNumber == 21) {
          // (La1 - latitude of first grid point)
          //lat1 = 0.0;
          ggdr.addParam(GribGridDefRecord.LA1, 0.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          ggdr.addParam(GribGridDefRecord.LO1, 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 90.0;
          ggdr.addParam(GribGridDefRecord.LA2, 90.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 180.0;
          ggdr.addParam(GribGridDefRecord.LO2, 180.0);
        } else if (gridNumber == 22) {
          // (La1 - latitude of first grid point)
          //lat1 = 0.0;
          ggdr.addParam(GribGridDefRecord.LA1, 0.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = -180.0;
          ggdr.addParam(GribGridDefRecord.LO1, -180.0);
          // (La2 - latitude of last grid point)
          //lat2 = 90.0;
          ggdr.addParam(GribGridDefRecord.LA2, 90.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 0.0;
          ggdr.addParam(GribGridDefRecord.LO2, 0.0);
        } else if (gridNumber == 23) {
          // (La1 - latitude of first grid point)
          //lat1 = -90.0;
          ggdr.addParam(GribGridDefRecord.LA1, -90.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          ggdr.addParam(GribGridDefRecord.LO1, 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 0.0;
          ggdr.addParam(GribGridDefRecord.LA2, 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 180.0;
          ggdr.addParam(GribGridDefRecord.LO2, 180.0);
        } else if (gridNumber == 24) {
          // (La1 - latitude of first grid point)
          //lat1 = -90.0;
          ggdr.addParam(GribGridDefRecord.LA1, -90.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = -180.0;
          ggdr.addParam(GribGridDefRecord.LO1, -180.0);
          // (La2 - latitude of last grid point)
          //lat2 = 0.0;
          ggdr.addParam(GribGridDefRecord.LA2, 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 0.0;
          ggdr.addParam(GribGridDefRecord.LO2, 0.0);
        }
      }
      break;

      case 25:
      case 26: {
        gdtn = 0;  // Latitude/Longitude
        ggdr.addParam(GribGridDefRecord.GRID_TYPE, gdtn);
        ggdr.addParam(GribGridDefRecord.GRID_NAME, Grib1Tables.getName(gdtn));

        // (Nx - number of points along x-axis)
        //nx = 72;
        ggdr.addParam(GribGridDefRecord.NX, 72);
        // (Ny - number of points along y-axis)
        //ny = 19;
        ggdr.addParam(GribGridDefRecord.NY, 18);
        // (resolution and component flags).  See Table 7
        resolution = 0x88;
        ggdr.addParam(GribGridDefRecord.RESOLUTION, resolution);
        // (Dx - Longitudinal Direction Increment )
        //dx = 5.0;
        ggdr.addParam(GribGridDefRecord.DX, 5.0);
        // (Dy - Latitudinal Direction Increment )
        //dy = 5.0;
        ggdr.addParam(GribGridDefRecord.DY, 5.0);
        //grid_units = "degrees";
        ggdr.addParam(GribGridDefRecord.GRID_UNITS, "degrees");
        if (gridNumber == 25) {
          // (La1 - latitude of first grid point)
          //lat1 = 0.0;
          ggdr.addParam(GribGridDefRecord.LA1, 0.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          ggdr.addParam(GribGridDefRecord.LO1, 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 90.0;
          ggdr.addParam(GribGridDefRecord.LA2, 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 355.0;
          ggdr.addParam(GribGridDefRecord.LO2, 355.0);
        } else if (gridNumber == 26) {
          // (La1 - latitude of first grid point)
          //lat1 = -90.0;
          ggdr.addParam(GribGridDefRecord.LA1, -90.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          ggdr.addParam(GribGridDefRecord.LO1, 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 0.0;
          ggdr.addParam(GribGridDefRecord.LA2, 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 355.0;
          ggdr.addParam(GribGridDefRecord.LO2, 355.0);
        }
      }
      break;

      case 61:
      case 62:
      case 63:
      case 64: {
        gdtn = 0;  // Latitude/Longitude
        ggdr.addParam(GribGridDefRecord.GRID_TYPE, gdtn);
        ggdr.addParam(GribGridDefRecord.GRID_NAME, Grib1Tables.getName(gdtn));

        // (Nx - number of points along x-axis)
        //nx = 91;
        ggdr.addParam(GribGridDefRecord.NX, 91);
        // (Ny - number of points along y-axis)
        //ny = 45;
        ggdr.addParam(GribGridDefRecord.NY, 45);
        // (resolution and component flags).  See Table 7
        resolution = 0x88;
        ggdr.addParam(GribGridDefRecord.RESOLUTION, resolution);
        // (Dx - Longitudinal Direction Increment )
        //dx = 2.0;
        ggdr.addParam(GribGridDefRecord.DX, 2.0);
        // (Dy - Latitudinal Direction Increment )
        //dy = 2.0;
        ggdr.addParam(GribGridDefRecord.DY, 2.0);
        //grid_units = "degrees";
        ggdr.addParam(GribGridDefRecord.GRID_UNITS, "degrees");

        if (gridNumber == 61) {
          // (La1 - latitude of first grid point)
          //lat1 = 0.0;
          ggdr.addParam(GribGridDefRecord.LA1, 0.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          ggdr.addParam(GribGridDefRecord.LO1, 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 90.0;
          ggdr.addParam(GribGridDefRecord.LA2, 90.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 180.0;
          ggdr.addParam(GribGridDefRecord.LO2, 180.0);
        } else if (gridNumber == 62) {
          // (La1 - latitude of first grid point)
          //lat1 = 0.0;
          ggdr.addParam(GribGridDefRecord.LA1, 0.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = -180.0;
          ggdr.addParam(GribGridDefRecord.LO1, -180.0);
          // (La2 - latitude of last grid point)
          //lat2 = 90.0;
          ggdr.addParam(GribGridDefRecord.LA2, 90.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 0.0;
          ggdr.addParam(GribGridDefRecord.LO2, 0.0);
        } else if (gridNumber == 63) {
          // (La1 - latitude of first grid point)
          //lat1 = -90.0;
          ggdr.addParam(GribGridDefRecord.LA1, -90.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = 0.0;
          ggdr.addParam(GribGridDefRecord.LO1, 0.0);
          // (La2 - latitude of last grid point)
          //lat2 = 0.0;
          ggdr.addParam(GribGridDefRecord.LA2, 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 180.0;
          ggdr.addParam(GribGridDefRecord.LO2, 180.0);
        } else if (gridNumber == 64) {
          // (La1 - latitude of first grid point)
          //lat1 = -90.0;
          ggdr.addParam(GribGridDefRecord.LA1, -90.0);

          // (Lo1 - longitude of first grid point)
          //lon1 = -180.0;
          ggdr.addParam(GribGridDefRecord.LO1, -180.0);
          // (La2 - latitude of last grid point)
          //lat2 = 0.0;
          ggdr.addParam(GribGridDefRecord.LA2, 0.0);
          // (Lo2 - longitude of last grid point)
          //lon2 = 0.0;
          ggdr.addParam(GribGridDefRecord.LO2, 0.0);
        }
        break;
      }
      case 87: {
        gdtn = 5;  //
        ggdr.addParam(GribGridDefRecord.GRID_TYPE, gdtn);
        ggdr.addParam(GribGridDefRecord.GRID_NAME, Grib1Tables.getName(gdtn));
        // (Nx - number of points along x-axis)
        //nx = 81;
        ggdr.addParam(GribGridDefRecord.NX, 81);
        // (Ny - number of points along y-axis)
        //ny = 62;
        ggdr.addParam(GribGridDefRecord.NY, 62);
        // (La1 - latitude of first grid point)
        //lat1 = 22.8756;
        ggdr.addParam(GribGridDefRecord.LA1, 22.8756);
        // (Lo1 - longitude of first grid point)
        //lon1 = 239.5089;
        ggdr.addParam(GribGridDefRecord.LO1, 239.5089);
        // (resolution and component flags).  See Table 7
        resolution = 0x08;
        ggdr.addParam(GribGridDefRecord.RESOLUTION, resolution);
        // octets 18-20 (Lov - Orientation of the grid - east lon parallel to y axis)
        //lov = 255.000;
        ggdr.addParam(GribGridDefRecord.LOV, 255.0);
        // (La2 - latitude of last grid point)
        //lat2 = 0.000;
        ggdr.addParam(GribGridDefRecord.LA2, 0.0);
        // (Dx - Longitudinal Direction Increment )
        //dx = 68153.0;
        ggdr.addParam(GribGridDefRecord.DX, 68153.0);
        // (Dy - Latitudinal Direction Increment )
        //dy = 68153.0;
        ggdr.addParam(GribGridDefRecord.DY, 68153.0);
        //grid_units = "m";
        ggdr.addParam(GribGridDefRecord.GRID_UNITS, "m");
        break;
      }
      default:
        log.error("Grid " + gridNumber + " not configured yet");
        break;
    }

    String winds = GribNumbers.isBitSet(resolution, GribNumbers.BIT_5)
        ? "Relative"
        : "True";
    ggdr.addParam(GribGridDefRecord.WIND_FLAG, winds);

    int res = resolution >> 6;
    if ((res == 1) || (res == 3)) {
      shape = 1;
    } else {
      shape = 0;
    }
    ggdr.addParam(GribGridDefRecord.GRID_SHAPE_CODE, shape);
    ggdr.addParam(GribGridDefRecord.GRID_SHAPE, Grib1Tables.getShapeName(shape));
    if (shape == 0) {
      ggdr.addParam(GribGridDefRecord.RADIUS_SPHERICAL_EARTH, 6367.47);
    } else {
      ggdr.addParam(GribGridDefRecord.MAJOR_AXIS_EARTH, 6378.160);
      ggdr.addParam(GribGridDefRecord.MINOR_AXIS_EARTH, 6356.775);
    }

  }  // end populateGDS

}


