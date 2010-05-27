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

// $Id: Grib1Dump.java,v 1.26 2006/08/03 22:33:40 rkambic Exp $


package ucar.grib.grib1;


import ucar.grib.*;

// import statements
import ucar.unidata.io.RandomAccessFile;
import ucar.grid.GridParameter;

import java.io.BufferedOutputStream;  // Input/Output functions
import java.io.FileNotFoundException;
import java.io.FileOutputStream;      // Input/Output functions
import java.io.IOException;
import java.io.PrintStream;           // Input/Output functions

import java.lang.*;                   // Standard java functions

import java.util.*;
import java.text.SimpleDateFormat;

/**
 *
 * Dumps the the IS, PDS, and GDS information from a Grib file.
 * @author Robb Kambic  10/31/04 .
 * @version 1.0  .
 *
 */
public final class Grib1Dump {

    static private final SimpleDateFormat dateFormat;
    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));  // same as UTC
    }
    static Calendar calendar;
    /**
     *
     * Dumps usage of the class.
     *
     * @param className Grib1Dump.
     */
    private static void usage(String className) {
        System.out.println();
        System.out.println("Usage of " + className + ":");
        System.out.println("Parameters:");
        System.out.println("<GribFileToRead> reads/scans file");
        System.out.println("<output file> file to store results");
        System.out.println(
            "<true or false> whether to read/display data too");
        System.out.println();
        System.out.println(
            "java " + className
            + " <GribFileToRead> <output file> <true or false>");
        System.exit(0);
    }

    /**
     * Dumps IS, PDS, and GDS meta data information of a Grib file.
     *
     * @param  args input gribfile, [output file] [true|false] output the data.
     */
    public static void main(String args[]) {

        // Function References
        Grib1Dump func = new Grib1Dump();

        // Test usage
        if (args.length < 1) {
            // Get class name as String
            Class cl = func.getClass();
            func.usage(cl.getName());
        }
        boolean displayData = false;

        // Say hello
        Date now = Calendar.getInstance().getTime();
        //System.out.println(now.toString() + " ... Start of Grib1Dump");

        // Reading of Grib files must be inside a try-catch block
        calendar = Calendar.getInstance();
        calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        PrintStream ps = System.out;
        try {
            RandomAccessFile raf = null;
            if (args.length == 3) {  // input file, output file, get data for dump
                raf = new RandomAccessFile(args[0], "r");
                ps = new PrintStream(
                    new BufferedOutputStream(
                        new FileOutputStream(args[1], false)));
                displayData = args[2].equalsIgnoreCase("true");
            } else if (args.length == 2) {  // input file and output file for dump
                raf = new RandomAccessFile(args[0], "r");
                if (args[1].equalsIgnoreCase("true")
                        || args[1].equalsIgnoreCase("false")) {
                    displayData = args[1].equalsIgnoreCase("true");
                } else {
                    ps = new PrintStream(
                        new BufferedOutputStream(
                            new FileOutputStream(args[1], false)));
                }
            } else if (args.length == 1) {
                raf = new RandomAccessFile(args[0], "r");
            } else {
                System.exit(0);
            }
            // test for a user defined parameter table read
            //GribPDSParamTable.addParameterUserLookup( "/local/robb/trunk20081229/grib/resources/resources/grib/tables/userlookup.lst"); 
            raf.order(RandomAccessFile.BIG_ENDIAN);
            // Create Grib1Input instance
            Grib1Input g1i = new Grib1Input(raf);
            // boolean params getProducts, oneRecord 
            g1i.scan(false, false);
            // record contains objects for all 5 Grib1 sections
            ArrayList records = g1i.getRecords();
            for (int i = 0; i < records.size(); i++) {
                Grib1Record record = (Grib1Record) records.get(i);
                Grib1IndicatorSection         is  = record.getIs();
                Grib1ProductDefinitionSection pds = record.getPDS();
                Grib1GridDefinitionSection    gds = record.getGDS();

                // create dump output here
                ps.println(
                    "--------------------------------------------------------------------");
                ps.println("                        Header : "
                           + record.getHeader());
                printIS(is, ps);
                printPDS(pds, ps);
                printGDS(gds, pds, ps);
                ps.println();

                if (displayData) {
                    float[] data = null;
                    ps.println(
                        "--------------------------------------------------------------------");
                    Grib1Data gd = new Grib1Data(raf);
                    // TODO: pds vars needed
                    data = gd.getData(record.getDataOffset(),
                       pds.getDecimalScale(), pds.bmsExists());
                    if (data != null) {
                        for (int j = 0; j < data.length; j++) {
                            ps.println("data[ " + j + " ]=" + data[j]);
                        }
                    }
                    break;  // only display data for 1st record
                }
            }
            raf.close();    // done reading

            // Catch thrown errors from GribFile
        } catch (FileNotFoundException noFileError) {
            System.err.println("FileNotFoundException : " + noFileError);
        } catch (IOException ioError) {
            System.err.println("IOException : " + ioError);
        } catch (NoValidGribException noGrib) {
            System.err.println("NoValidGribException : " + noGrib);
        } finally {
            ps.close();     // done writing
        }

        // Goodbye message
        now = Calendar.getInstance().getTime();
        //System.out.println(now.toString() + " ... End of Grib1Dump!");

    }  // end main

    /*
     * Prints out a Grib1IndicatorSection.
     *
     * @param is
     * @param ps
     *
     */
    private static void printIS(Grib1IndicatorSection is, PrintStream ps) {

        ps.println("                    Discipline : "
                   + "0 Meteorological Products");
        ps.println("                  GRIB Edition : " + is.getGribEdition());
        ps.println("                   GRIB length : " + is.getGribLength());

    }

    /*
     * Prints out a GDS.
     *
     * @param gds
     * @param pds
     * @param ps
     */
    private static void printGDS(Grib1GridDefinitionSection gds,
                                 Grib1ProductDefinitionSection pds,
                                 PrintStream ps) {

        Grib1PDSVariables pdsv = pds.getPdsVars();
        Grib1GDSVariables gdsv = gds.getGdsVars();

        int gdtn = gdsv.getGdtn();
        int numberOfPoints = 0;
        if (pdsv.getTypeGenProcess() == 96) {  //thin grid
            numberOfPoints = 3447;
        } else {
            numberOfPoints = gdsv.getNx() * gdsv.getNy();
        }
        ps.println("         Number of data points : " + numberOfPoints);
        ps.println("                     Grid Name : " + Grib1Tables.getName(gdtn));

        String winds = GribNumbers.isBitSet(gdsv.getResolution(), GribNumbers.BIT_5)
                       ? "Relative"
                       : "True";

        switch (gdtn) {  // Grid Definition Template Number

          case 0 :
          case 4 :
          case 10 :  
          case 40 :
          case 201 :
          case 202 :              // Latitude/Longitude Grid
              //  gaussian grids
              //  Arakawa semi-staggered e-grid rotated
              //  Arakawa filled e-grid rotated

              ps.println("                     Grid Shape: " + gdsv.getShape()
                         + " " + Grib1Tables.getShapeName( gdtn ));
              if (gdsv.getShape() == 0) {
                  ps.println("         Spherical earth radius: "
                             + gdsv.getEarthRadius());

              } else if (gdsv.getShape() == 1) {
                  ps.println("         Oblate earth major axis: "
                             + gdsv.getMajorAxis());
                  ps.println("         Oblate earth minor axis: "
                             + gdsv.getMinorAxis());
              }
              ps.println("                            Nx : " + gdsv.getNx());
              ps.println("                            Ny : " + gdsv.getNy());
              ps.println("                           La1 : " + gdsv.getLa1());
              ps.println("                           Lo1 : " + gdsv.getLo1());
              ps.println("  Resolution & Component flags : "
                         + gdsv.getResolution());
              ps.println("                         Winds : " + winds);
              ps.println("                           La2 : " + gdsv.getLa2());
              ps.println("                           Lo2 : " + gdsv.getLo2());
              ps.println("                            Dx : " + gdsv.getDx());
              if (gdtn == 4) {
                  ps.println("                            Np : "
                             + gdsv.getNy());
                             //+ gdsv.getNp());
              } else {
                  ps.println("                            Dy : "
                             + gdsv.getDy());
              }
              ps.println("                    Grid Units : "
                         + gdsv.getGridUnits());
              ps.println("                 Scanning mode : "
                         + gdsv.getScanMode());
              if (gdtn == 10) {  //Rotated Latitude/longitude
                  ps.println("     Latitude of southern pole : "
                             + gdsv.getSpLat());
                  ps.println("    Longitude of southern pole : "
                             + gdsv.getSpLon());
                  ps.println("             Angle of Rotation : "
                             + gdsv.getAngle());

              } else if (gdtn == 2) {  //Stretched Latitude/longitude
                  //ps.println("              Latitude of pole : " +
                  //  gdsv.getPoleLat());
                  //ps.println("             Longitude of pole : " +
                  //  gdsv.getPoleLon());

              } else if (gdtn == 3) {  //Stretched and Rotated
                  // Latitude/longitude
                  ps.println("     Latitude of southern pole : "
                             + gdsv.getSpLat());
                  ps.println("    Longitude of southern pole : "
                             + gdsv.getSpLon());
                  //ps.println("              Latitude of pole : " +
                  //  gdsv.getPoleLat());
                  //ps.println("             Longitude of pole : " +
                  //  gdsv.getPoleLon());
              }
              break;

          case 1 :  // Mercator
              ps.println("                     Grid Shape: " + gdsv.getShape()
                         + " " + Grib1Tables.getShapeName( gdtn ));
              if (gdsv.getShape() == 0) {
                  ps.println("         Spherical earth radius: "
                             + gdsv.getEarthRadius());

              } else if (gdsv.getShape() == 1) {
                  ps.println("         Oblate earth major axis: "
                             + gdsv.getMajorAxis());
                  ps.println("         Oblate earth minor axis: "
                             + gdsv.getMinorAxis());
              }

              ps.println("                            Nx : " + gdsv.getNx());
              ps.println("                            Ny : " + gdsv.getNy());
              ps.println("                           La1 : " + gdsv.getLa1());
              ps.println("                           Lo1 : " + gdsv.getLo1());
              ps.println("  Resolution & Component flags : "
                         + gdsv.getResolution());
              ps.println("                         Winds : " + winds);
              ps.println("                           La2 : " + gdsv.getLa2());
              ps.println("                           Lo2 : " + gdsv.getLo2());
              ps.println("                         Latin : "
                         + gdsv.getLatin1());
              ps.println("                 Scanning mode : "
                         + gdsv.getScanMode());
              ps.println("                            Dx : " + gdsv.getDx());
              ps.println("                            Dy : " + gdsv.getDy());
              ps.println("                    Grid Units : "
                         + gdsv.getGridUnits());
              break;

          case 5 :  // Polar stereographic projection
              ps.println("                     Grid Shape: " + gdsv.getShape()
                         + " " + Grib1Tables.getShapeName( gdtn ));
              if (gdsv.getShape() == 0) {
                  ps.println("         Spherical earth radius: "
                             + gdsv.getEarthRadius());

              } else if (gdsv.getShape() == 1) {
                  ps.println("         Oblate earth major axis: "
                             + gdsv.getMajorAxis());
                  ps.println("         Oblate earth minor axis: "
                             + gdsv.getMinorAxis());
              }
              ps.println("                            Nx : " + gdsv.getNx());
              ps.println("                            Ny : " + gdsv.getNy());
              ps.println("                           La1 : " + gdsv.getLa1());
              ps.println("                           Lo1 : " + gdsv.getLo1());
              ps.println("  Resolution & Component flags : "
                         + gdsv.getResolution());
              ps.println("                         Winds : " + winds);
              ps.println("                           LoV : " + gdsv.getLoV());
              ps.println("                            Dx : " + gdsv.getDx());
              ps.println("                            Dy : " + gdsv.getDy());
              ps.println("                    Grid Units : "
                         + gdsv.getGridUnits());
              ps.println("             Projection center : "
                         + gdsv.getProjectionFlag());
              ps.println("                 Scanning mode : "
                         + gdsv.getScanMode());
              break;

          case 3 :  // Lambert Conformal
              ps.println("                    Grid Shape : " + gdsv.getShape()
                         + " " + Grib1Tables.getShapeName( gdtn ));
              if (gdsv.getShape() == 0) {
                  ps.println("         Spherical earth radius: "
                             + gdsv.getEarthRadius());

              } else if (gdsv.getShape() == 1) {
                  ps.println("         Oblate earth major axis: "
                             + gdsv.getMajorAxis());
                  ps.println("         Oblate earth minor axis: "
                             + gdsv.getMinorAxis());
              }
              ps.println("                            Nx : " + gdsv.getNx());
              ps.println("                            Ny : " + gdsv.getNy());
              ps.println("                           La1 : " + gdsv.getLa1());
              ps.println("                           Lo1 : " + gdsv.getLo1());
              ps.println("  Resolution & Component flags : "
                         + gdsv.getResolution());
              ps.println("                         Winds : " + winds);
              ps.println("                           LoV : " + gdsv.getLoV());
              ps.println("                            Dx : " + gdsv.getDx());
              ps.println("                            Dy : " + gdsv.getDy());
              ps.println("                    Grid Units : "
                         + gdsv.getGridUnits());
              ps.println("             Projection center : "
                         + gdsv.getProjectionFlag ());
              ps.println("                 Scanning mode : "
                         + gdsv.getScanMode());
              ps.println("                        Latin1 : "
                         + gdsv.getLatin1());
              ps.println("                        Latin2 : "
                         + gdsv.getLatin2());
              ps.println("                         SpLat : "
                         + gdsv.getSpLat());
              ps.println("                         SpLon : "
                         + gdsv.getSpLon());

              break;

          case 41 :
          case 42 :
          case 43 :  // Rotated/Stretched Gaussian
              ps.println("                     Grid Shape: " + gdsv.getShape()
                         + " " + Grib1Tables.getShapeName( gdtn ));
              if (gdsv.getShape() == 0) {
                  ps.println("         Spherical earth radius: "
                             + gdsv.getEarthRadius());

              } else if (gdsv.getShape() == 1) {
                  ps.println("         Oblate earth major axis: "
                             + gdsv.getMajorAxis());
                  ps.println("         Oblate earth minor axis: "
                             + gdsv.getMinorAxis());
              }
              ps.println("Number of points along parallel: " + gdsv.getNx());
              ps.println("Number of points along meridian: " + gdsv.getNy());
              ps.println("  Latitude of first grid point : " + gdsv.getLa1());
              ps.println(" Longitude of first grid point : " + gdsv.getLo1());
              ps.println("  Resolution & Component flags : "
                         + gdsv.getResolution());
              ps.println("                         Winds : " + winds);
              ps.println("                    Grid Units : "
                         + gdsv.getGridUnits());
              ps.println("   Latitude of last grid point : " + gdsv.getLa2());
              ps.println("  Longitude of last grid point : " + gdsv.getLo2());
              ps.println("         i direction increment : " + gdsv.getDx());
              //ps.println("           Number of parallels : " +
              //   gdsv.getN());
              ps.println("                 Scanning mode : "
                         + gdsv.getScanMode());

              if (gdtn == 41) {  //Rotated Gaussian Latitude/longitude
                  ps.println("     Latitude of southern pole : "
                             + gdsv.getSpLat());
                  ps.println("    Longitude of southern pole : "
                             + gdsv.getSpLon());

              } else if (gdtn == 42) {  //Stretched Gaussian
                  // Latitude/longitude
                  //ps.println("              Latitude of pole : " +
                  //   gdsv.getPoleLat());
                  //ps.println("             Longitude of pole : " +
                  //   gdsv.getPoleLon());

              } else if (gdtn == 43) {  //Stretched and Rotated Gaussian
                  // Latitude/longitude
                  ps.println("     Latitude of southern pole : "
                             + gdsv.getSpLat());
                  ps.println("    Longitude of southern pole : "
                             + gdsv.getSpLon());
                  //ps.println("              Latitude of pole : " +
                  //gdsv.getPoleLat());
                  //ps.println("             Longitude of pole : " +
                  //gdsv.getPoleLon());
              }
              break;

          default :
              ps.println("Unknown Grid Type : " + gdtn);

        }  // end switch gdtn
    }      // end printGDS

    /*
     * Prints out a PDS.
     *
     * @param pds
     *
     */

    /**
     * _more_
     *
     * @param pds _more_
     * @param ps _more_
     */
    private static void printPDS(Grib1ProductDefinitionSection pds, PrintStream ps) {
      Grib1PDSVariables pdsv = pds.getPdsVars();
        int center = pdsv.getCenter();
        int subCenter = pdsv.getSubCenter();

        ps.println("            Originating Center : " + center
          + " " + Grib1Tables.getCenter_idName( center ));
        String sc = Grib1Tables.getSubCenter_idName(center, subCenter);
        ps.print("        Originating Sub-Center : " + subCenter );
        if( sc == null )
          ps.println();
        else
          ps.println( " "+  sc);
        //ps.println("                Reference Time : " +
        //pds.getReferenceTime() );
        ps.println(
            "            Product Definition : " + pdsv.getProductDefinition()
            + " " + Grib1Tables.getProductDefinitionName(pdsv.getProductDefinition()));
        ps.println("            Parameter Category : "
                   + "-1 Meteorological Parameters");
        try {
          int pn = pdsv.getParameterNumber();
          GribPDSParamTable parameter_table = GribPDSParamTable.getParameterTable(
            center, subCenter,  pdsv.getTableVersion() );
          GridParameter parameter = parameter_table.getParameter(pn);

          ps.println("                Parameter Name : "
                   + pn + " " + parameter.getName() + " " + parameter.getDescription());
          ps.println("               Parameter Units : " + parameter.getUnit());
        } catch( NotSupportedException nse ) {
          ps.println( "NotSupportedException caught");
        }
        //ps.println("                Reference Time : " + dateFormat.format( pds.getBaseTime()));
        long refTime = pdsv.getRefTime();
        calendar.setTimeInMillis(refTime);
        ps.println("                Reference Time : " + dateFormat.format( calendar.getTime()));
        //ps.println("                    Time Units : " + pds.getTimeUnit());
        ps.println("                    Time Units : " +
            Grib1Tables.getTimeUnit( pdsv.getTimeRangeUnit()  ));
        //ps.println("          Time Range Indicator : "
        //           + pds.getTimeRangeString());
        ps.println("          Time Range Indicator : "
                   + Grib1Tables.getTimeRange( pdsv.getTimeRange() ));
        ps.println("                   Time 1 (P1) : " + pdsv.getP1());
        ps.println("                   Time 2 (P2) : " + pdsv.getP2());
        //String tgp = Integer.toString(pds.getTypeGenProcess());
        int tgp =  pdsv.getTypeGenProcess();
        ps.println("       Generating Process Type : " + tgp + " "
                   + Grib1Tables.getTypeGenProcessName(center, tgp));
        //ps.println("                  ForecastTime : " +
        //pds.getForecastTime1());
        ps.println("                    Level Type : " + pds.getLevelType()
                   + " " + pds.getLevelName());
        //ps.println("                    Level Type : " + pdsv.getTypeFirstFixedSurface()
        //           + " " + Grib1Tables.getLevelName(pdsv.getTypeFirstFixedSurface()));
//        ps.println("                 Level Value 1 : "
//                   + pds.getLevelValue1());
//        ps.println("                 Level Value 2 : "
//                   + pds.getLevelValue2());

        ps.println("                 Level Value 1 : "
                   + pdsv.getValueFirstFixedSurface());
        ps.println("                 Level Value 2 : "
                   + pdsv.getValueSecondFixedSurface());
        ps.println("                    GDS Exists : " + pdsv.gdsExists());
        ps.println("                    BMS Exists : " + pdsv.bmsExists());
    }  // end printPDS
}

