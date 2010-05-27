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

// $Id: Grib1Indexer.java,v 1.44 2006/08/03 22:33:40 rkambic Exp $


package ucar.grib.grib1;


import ucar.grib.*;
import ucar.grib.grib2.Grib2WriteIndex;

import ucar.unidata.io.RandomAccessFile;

import java.io.BufferedOutputStream;  // Input/Output functions
import java.io.FileOutputStream;      // Input/Output functions
import java.io.IOException;
import java.io.PrintStream;           // Input/Output functions
import java.io.File;

import java.lang.*;                   // Standard java functions

import java.util.*;
import java.text.SimpleDateFormat;                   // Extra utilities from sun


/**
 * Creates an Index file with file extension .gbx and a memory index for a Grib1 file.
 * see <a href="../../../IndexFormat.txt"> IndexFormat.txt</a>
 * @deprecated
 * @author Robb Kambic  10/20/03.
 */
public class Grib1Indexer {

  /**
   * _more_
   */
  private final boolean showTime = false;

  /**
   * Write a Grib file index; optionally create an in-memory index.
   *
   * @param inputRaf  raf of GRIB file
   * @param ps        write output to here
   * @param makeIndex make an in-memory index if true
   * @return Index if makeIndex is true, else null
   * @throws IOException
   */
  public final Index writeFileIndex(RandomAccessFile inputRaf,
          PrintStream ps, boolean makeIndex)
          throws IOException {

    /**
     * date format "yyyy-MM-dd'T'HH:mm:ss'Z'".
     */
    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));  //same as UTC


    Date now = Calendar.getInstance().getTime();
    //System.out.println(now.toString() + " ... Start of Grib1Indexer");
    long start = System.currentTimeMillis();
    int count = 0;
    // set buffer size for performance
    int rafBufferSize = inputRaf.getBufferSize();
    inputRaf.setBufferSize( Grib2WriteIndex.indexRafBufferSize );

    Index index = makeIndex
            ? new Index()
            : null;

    // Opening of grib data must be inside a try-catch block
    try {
      // Create Grib1Input instance
      inputRaf.seek(0);
      Grib1Input g1i = new Grib1Input(inputRaf);
      // params getProducts (implies  unique GDSs too), oneRecord
      g1i.scan(true, false);

      // Section 1 Global attributes
      // while needed here to process complete stream
      ps.println("index_version = " + GribReadTextIndex.currentTextIndexVersion);
      ps.println("grid_edition = " + 1);
      ps.println("location = " + inputRaf.getLocation().replaceAll( " ", "%20"));
      ps.println("length = " + inputRaf.length());
      ps.println("created = " + dateFormat.format(now));
      //ps.println("version = 1.0");
      if (makeIndex) {
        index.addGlobalAttribute("length",
                Long.toString(inputRaf.length()));
        index.addGlobalAttribute("location", inputRaf.getLocation().replaceAll( " ", "%20"));
        index.addGlobalAttribute("created", dateFormat.format(now));
        //index.addGlobalAttribute( "version", "1.0");
      }

      // section 2 grib records
      HashMap gdsHM = g1i.getGDSs();  // needed for tiled, thin grids check
      ArrayList products = g1i.getProducts();
      //Collections.sort( products );
      for (int i = 0; i < products.size(); i++) {
        Grib1Product product = (Grib1Product) products.get(i);
        Grib1ProductDefinitionSection pds = product.getPDS();
        if (i == 0) {
          ps.println("center = " + pds.getCenter());
          ps.println("sub_center = " + pds.getSubCenter());
          ps.println("table_version = " + pds.getTableVersion());
          checkForTiledThinEnsemble(makeIndex, index, pds, gdsHM,
                  null, ps);
          ps.println(
                  "--------------------------------------------------------------------");
        }
        ps.println(pds.getProductDefinition() + " "
                + product.getDiscipline() + " "
                + product.getCategory() + " "
                + pds.getParameter().getNumber() + " "
                + pds.getTypeGenProcess() + " "
                + pds.getLevelType() + " " + pds.getLevelValue1()
                + " " + "255 "          // Grib 1 doesn't have 2 level types
                + pds.getLevelValue2() + " " + dateFormat.format( pds.getBaseTime()) + " "
                + pds.getForecastTime() + " "
                + product.getGDSkey() + " "
                + product.getOffset1() + " "
                + product.getOffset2() + " "
                + pds.getDecimalScale() + " "
                + pds.bmsExists() + " "
                + pds.getCenter() + " "
                + pds.getSubCenter() + " "
                + pds.getTableVersion());

        if (makeIndex) {
          index.addGribRecord(makeGribRecord(index, product));
        }
        count++;
      }

      // section 3: GDSs in this File
      for (Iterator it = gdsHM.keySet().iterator(); it.hasNext();) {
        ps.println(
                "--------------------------------------------------------------------");
        String key = (String) it.next();
        ps.println("GDSkey = " + key);
        Grib1GridDefinitionSection gds =
                (Grib1GridDefinitionSection) gdsHM.get(key);
        printGDS(gds, ps);
        if (makeIndex) {
          index.addHorizCoordSys(makeGdsRecord(gds));
        }
      }

      // Catch thrown errors from GribFile
    } catch (NoValidGribException noGrib) {
      System.err.println("NoValidGribException : " + noGrib);
    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);

    } finally {
      //reset
      inputRaf.setBufferSize( rafBufferSize );
      ps.close();
    }

    // Goodbye message
    //now = Calendar.getInstance().getTime();
    //ps.println(now.toString() + " ... End of Grib1Indexer!");
    if (showTime) {
      System.out.println(" " + count + " products took "
              + (System.currentTimeMillis() - start)
              + " msec");
    }

    return index;
  }  // end writeFileIndex

  /**
   * checks if this Grib file contains tiles and/or is a thin grid.
   *
   * @param makeIndex
   * @param index
   * @param pds
   * @param gdsHM
   * @param extIdxGDSs
   * @param ps         PrintStream
   */
  protected final void checkForTiledThinEnsemble(boolean makeIndex,
          Index index, Grib1ProductDefinitionSection pds, HashMap gdsHM,
          HashMap extIdxGDSs, PrintStream ps) {

    int tiles = 1;
    boolean thin = false;
    boolean ensemble = false;

    Iterator iter = gdsHM.keySet().iterator();
    String key = (String) iter.next();
    Grib1GridDefinitionSection gds =
            (Grib1GridDefinitionSection) gdsHM.get(key);
    if ((gds.getGridType() == 0) || (gds.getGridType() == 4)) {
      if (extIdxGDSs == null) {
        tiles = gdsHM.size();
      } else {
        tiles = 0;
        for (Iterator it =
                gdsHM.keySet().iterator(); it.hasNext();) {
          key = (String) it.next();
          if (extIdxGDSs.containsKey(key)) {  //already have this tile
            continue;
          }
          tiles++;
        }
        tiles += extIdxGDSs.size();
      }
    }
    thin = gds.getIsThin();

    ps.println("tiles = " + tiles);
    ps.println("thin = " + thin);
    if ((pds.getSubCenter() == 2) && (pds.getCenter() == 7)) {  // ensemble check
      ensemble = true;
    }
    ps.println("ensemble = " + ensemble);
    if (makeIndex) {
      index.addGlobalAttribute("tiles", Integer.toString(tiles));
    }
  }  //end checkForTiledThinEnsemble

  /**
   * makes a GribRecord from a Grib1Product.
   *
   * @param index   _more_
   * @param product
   * @return GribRecord
   */
  protected static final Index.GribRecord makeGribRecord(Index index,
          Grib1Product product) {
    Index.GribRecord gr = index.getGribRecord();

    Grib1ProductDefinitionSection pds = product.getPDS();

    gr.productType = pds.getProductDefinition();
    gr.discipline = product.getDiscipline();
    gr.category = -1;  // grib1 doesn't have category pds.getParameterCategory();
    gr.paramNumber = pds.getParameterNumber();
    gr.typeGenProcess = Integer.toString(pds.getTypeGenProcess());
    gr.levelType1 = pds.getLevelType();
    gr.levelValue1 = pds.getLevelValue1();
    gr.levelType2 = 255;  // Grib1 doesn't have levelType2
    gr.levelValue2 = pds.getLevelValue2();
    gr.refTime = pds.getBaseTime();
    gr.forecastTime = pds.getForecastTime();
    gr.gdsKey = product.getGDSkey().trim().intern();
    gr.offset1 = product.getOffset1();
    gr.offset2 = -1;
    gr.decimalScale = pds.getDecimalScale();
    gr.bmsExists = pds.bmsExists();
    gr.center = pds.getCenter();
    gr.subCenter = pds.getSubCenter();
    gr.table = pds.getTableVersion();


    return gr;
  }

  /**
   * makes a GdsRecord from a GDS.
   *
   * @param gds
   * @return Index.GdsRecord
   */
  protected static final Index.GdsRecord makeGdsRecord(
          Grib1GridDefinitionSection gds) {
    Index.GdsRecord igds = new Index.GdsRecord();

    igds.gdsKey = gds.getCheckSum();
    igds.addParam("GDSkey", gds.getCheckSum());
    igds.grid_type = gds.getGdtn();
    igds.addParam("grid_type", Integer.toString(gds.getGdtn()));

    if (gds.getGdtn() != 50) {  // the same up to winds


      igds.grid_shape_code = gds.getShape();
      igds.addParam("grid_shape_code", Integer.toString(gds.getShape()));
      if (gds.getShape() == 0) {
        igds.radius_spherical_earth = gds.getShapeRadius();
        igds.addParam("grid_radius_spherical_earth", Double.toString(gds.getShapeRadius()));
      } else if (gds.getShape() == 1) {
        igds.major_axis_earth = gds.getShapeMajorAxis();
        igds.addParam("grid_major_axis_earth", Double.toString(gds.getShapeMajorAxis()));
        igds.minor_axis_earth = gds.getShapeMinorAxis();
        igds.addParam("grid_minor_axis_earth", Double.toString(gds.getShapeMinorAxis()));
      }

      igds.nx = gds.getNx();
      igds.addParam("Nx", Integer.toString(gds.getNx()));
      igds.ny = gds.getNy();
      igds.addParam("Ny", Integer.toString(gds.getNy()));
      igds.La1 = gds.getLa1();
      igds.addParam("La1", Double.toString(gds.getLa1()));
      igds.Lo1 = gds.getLo1();
      igds.addParam("Lo1", Double.toString(gds.getLo1()));
      igds.resolution = gds.getResolution();
      igds.addParam("ResCompFlag", Integer.toString(gds.getResolution()));
      String winds = GribNumbers.isBitSet(gds.getResolution(), GribNumbers.BIT_5)
              ? "Relative"
              : "True";
      igds.winds = winds;
      igds.addParam("Winds", winds);
    }

    // in future remove, the addParam are now done on projection basis, not always
    igds.LaD = gds.getLad();
    //igds.addParam("LaD", Double.toString(gds.getLad()));
    igds.LoV = gds.getLov();
    //igds.addParam("LoV", Double.toString(gds.getLov()));
    igds.dx = gds.getDx();
    //igds.addParam("Dx", Double.toString(gds.getDx()));
    igds.dy = gds.getDy();
    //igds.addParam("Dy", Double.toString(gds.getDy()));
    //igds.addParam("La2", Double.toString(gds.getLa2()));
    //igds.addParam("Lo2", Double.toString(gds.getLo2()));
    //igds.addParam("NumberParallels", Double.toString(gds.getNp()));
    //String NpProj = ((gds.getProjectionCenter() & 128) == 0)
    //               ? "true"
    //               : "false";
    //igds.addParam("NpProj", NpProj);
    //System.out.println( "makeGdsRec NpProj ="+ NpProj );
    //igds.addParam("Latin", Double.toString(gds.getLatin()));
    igds.latin1 = gds.getLatin1();
    //igds.addParam("Latin1", Double.toString(gds.getLatin1()));
    igds.latin2 = gds.getLatin2();
    //igds.addParam("Latin2", Double.toString(gds.getLatin2()));
    //igds.addParam("SpLat", Double.toString(gds.getSpLat()));
    //igds.addParam("SpLon", Double.toString(gds.getSpLon()));

    switch (gds.getGdtn()) {  // Grid Definition Template Number

      case 0:
      case 4:
      case 10:
      case 40:
      case 201:
      case 202:              //  Latitude/Longitude
        //  gaussian grids
        //  Arakawa semi-staggered e-grid rotated
        //  Arakawa filled e-grid rotated

        igds.addParam("La2", Double.toString(gds.getLa2()));
        igds.addParam("Lo2", Double.toString(gds.getLo2()));
        igds.addParam("Dx", Double.toString(gds.getDx()));
        if (gds.getGdtn() == 4) {                    //Gaussian Latitude/longitude
          igds.addParam("NumberParallels", Double.toString(gds.getNp()));
        } else {
          igds.addParam("Dy", Double.toString(gds.getDy()));
        }

        // following has to be corrected, might be hard coded
        if  (gds.getGdtn() == 10) {         //Rotated Latitude/longitude
          igds.addParam("SpLat", Double.toString(gds.getSpLat()));
          igds.addParam("SpLon", Double.toString(gds.getSpLon()));
          igds.addParam("RoatationAngle = ", Double.toString(gds.getAngle()));

        } else if (false && (gds.getGdtn() == 2)) {  //Stretched Latitude/longitude
          igds.addParam("SpLat", Double.toString(gds.getSpLat()));
          igds.addParam("SpLon", Double.toString(gds.getSpLon()));

        } else if (false && (gds.getGdtn() == 3)) {  //Stretched and Rotated
          // Latitude/longitude
          igds.addParam("SpLat", Double.toString(gds.getSpLat()));
          igds.addParam("SpLon", Double.toString(gds.getSpLon()));

        } else if (false && (gds.getGdtn() == 4)) {  //Gaussian
        }
        break;

      case 1:                                         // Mercator
        igds.addParam("La2", Double.toString(gds.getLa2()));
        igds.addParam("Lo2", Double.toString(gds.getLo2()));
        igds.addParam("Latin", Double.toString(gds.getLatin()));
        igds.addParam("Dx", Double.toString(gds.getDx()));
        igds.addParam("Dy", Double.toString(gds.getDy()));

        break;

      case 3:  // Lambert Conformal

        igds.addParam("LoV", Double.toString(gds.getLov()));
        igds.addParam("Dx", Double.toString(gds.getDx()));
        igds.addParam("Dy", Double.toString(gds.getDy()));
        String NpProj = ((gds.getProjectionCenter() & 128) == 0)
                ? "true"
                : "false";
        igds.addParam("NpProj", NpProj);
        igds.addParam("Latin1", Double.toString(gds.getLatin1()));
        igds.addParam("Latin2", Double.toString(gds.getLatin2()));
        igds.addParam("SpLat", Double.toString(gds.getSpLat()));
        igds.addParam("SpLon", Double.toString(gds.getSpLon()));

        break;

      case 5:  // Polar stereographic projection
        igds.addParam("LoV", Double.toString(gds.getLov()));
        igds.addParam("Dx", Double.toString(gds.getDx()));
        igds.addParam("Dy", Double.toString(gds.getDy()));
        NpProj = ((gds.getProjectionCenter() & 128) == 0)
                ? "true"
                : "false";
        igds.addParam("NpProj", NpProj);

        break;

      default:
        break;

    }  // end switch gdtn
    return igds;
  }

  /**
   * prints out fields in a GDS.
   *
   * @param gds Grib1GridDefinitionSection
   * @param ps  PrintStream
   */

  protected void printGDS(Grib1GridDefinitionSection gds,
          PrintStream ps) {

    ps.println("grid_type = " + gds.getGdtn());
    ps.println("grid_name = " + gds.getName());

    if (gds.getGdtn() != 50) {  // the same up to winds
      ps.println("grid_shape_code = " + gds.getShape());
      //ps.println("resolution = " + gds.getResolution() );
      ps.println("grid_shape = " + gds.getShapeName());
      if (gds.getShape() == 0) {
        ps.println("grid_radius_spherical_earth = "
                + gds.getShapeRadius());
      } else {
        ps.println("grid_major_axis_earth = "
                + gds.getShapeMajorAxis());
        ps.println("grid_minor_axis_earth = "
                + gds.getShapeMinorAxis());
      }
      ps.println("Nx = " + gds.getNx());
      ps.println("Ny = " + gds.getNy());
      ps.println("La1 = " + gds.getLa1());
      ps.println("Lo1 = " + gds.getLo1());
      ps.println("ResCompFlag = " + gds.getResolution());
      String winds = GribNumbers.isBitSet(gds.getResolution(), GribNumbers.BIT_5)
              ? "Relative"
              : "True";
      ps.println("Winds = " + winds);
    }

    switch (gds.getGdtn()) {  // Grid Definition Template Number

      case 0:
      case 4:
      case 10:
      case 40:
      case 201:
      case 202:              //  Latitude/Longitude
        //  gaussian grids
        //  Arakawa semi-staggered e-grid rotated
        //  Arakawa filled e-grid rotated

        ps.println("La2 = " + gds.getLa2());
        ps.println("Lo2 = " + gds.getLo2());
        ps.println("Dx = " + gds.getDx());
        if (gds.getGdtn() == 4) {                    //Gaussian Latitude/longitude
          ps.println("NumberParallels = " + gds.getNp());
        } else {
          ps.println("Dy = " + gds.getDy());
        }
        ps.println( "grid_units = " + gds.getGrid_units() );
        ps.println("ScanningMode = " + gds.getScanMode());

        // following has to be corrected, might be hard coded
        if (gds.getGdtn() == 10) {         //Rotated Latitude/longitude
          ps.println("SpLat = " + gds.getSpLat());
          ps.println("SpLon = " + gds.getSpLon());
          ps.println("RotationAngle = " + gds.getAngle());
        } else if (false && (gds.getGdtn() == 2)) {  //Stretched Latitude/longitude
          ps.println("SpLat = " + gds.getSpLat());
          ps.println("SpLon = " + gds.getSpLon());

        } else if (false && (gds.getGdtn() == 3)) {  //Stretched and Rotated
          // Latitude/longitude
          ps.println("SpLat = " + gds.getSpLat());
          ps.println("SpLon = " + gds.getSpLon());
          ps.println("SpLat = " + gds.getSpLat());
          ps.println("SpLon = " + gds.getSpLon());

        } else if (false && (gds.getGdtn() == 4)) {  //Gaussian
        }
        break;

      case 1:                                         // Mercator
        ps.println("La2 = " + gds.getLa2());
        ps.println("Lo2 = " + gds.getLo2());
        ps.println("Latin = " + gds.getLatin());
        ps.println("ScanningMode = " + gds.getScanMode());
        ps.println("Dx = " + gds.getDx());
        ps.println("Dy = " + gds.getDy());
        ps.println( "grid_units = " + gds.getGrid_units() );
        break;

      case 3:  // Lambert Conformal

        //ps.println("LaD = " + gds.getLad() );
        ps.println("LoV = " + gds.getLov());
        ps.println("Dx = " + gds.getDx());
        ps.println("Dy = " + gds.getDy());
        ps.println( "grid_units = " + gds.getGrid_units() );
        ps.println("ProjFlag = " + gds.getProjectionCenter());
        ps.println("NpProj = " + ((gds.getProjectionCenter() & 128) == 0));
        ps.println("ScanningMode = " + gds.getScanMode());
        ps.println("Latin1 = " + gds.getLatin1());
        ps.println("Latin2 = " + gds.getLatin2());
        ps.println("SpLat = " + gds.getSpLat());
        ps.println("SpLon = " + gds.getSpLon());

        break;

      case 5:  // Polar stereographic projection
        //ps.println("LaD = " + gds.getLad() );
        ps.println("LoV = " + gds.getLov());
        ps.println("Dx = " + gds.getDx());
        ps.println("Dy = " + gds.getDy());
        ps.println( "grid_units = " + gds.getGrid_units() );
        ps.println("ProjFlag = " + gds.getProjectionCenter());
        ps.println("NpProj = " + ((gds.getProjectionCenter() & 128) == 0));
        ps.println("ScanningMode = " + gds.getScanMode());

        break;

      default:
        ps.println("Unknown Grid Type" + gds.getGdtn());

    }  // end switch gdtn
  }      // end printGDS

  /**
   * Dumps usage of the class, if called without arguments.
   *
   * @param className Grib1Indexer.
   */
  private static void usage(String className) {
    System.out.println();
    System.out.println("Usage of " + className + ":");
    System.out.println("Parameters:");
    System.out.println("<GribFileToRead> reads/scans for index");
    System.out.println(
            "<IndexFile.idx> where to write index, default STDOUT");
    System.out.println();
    System.out.println("java " + className
            + " <GribFileToRead> <IndexFile>");
    System.exit(0);
  }

  /**
   * Makes an index for a Grib1 file.
   *
   * @param args Grib1 file, optionally Index output file
   * @throws IOException
   */
  public static void main(String args[]) throws IOException {

    // Function References
    Grib1Indexer indexer = new Grib1Indexer();

    // Test usage
    if (args.length < 1) {
      // Get class name as String
      Class cl = indexer.getClass();
      usage(cl.getName());
      System.exit(0);
    }

    RandomAccessFile raf = null;
    PrintStream ps = System.out;
    String infile = args[0];
    raf = new RandomAccessFile(infile, "r");
    raf.order(RandomAccessFile.BIG_ENDIAN);

    if (args.length == 2) {  // input file and index file name given
      String idxfile;
      if (args[1].endsWith(".gbx")) {
        idxfile = args[1];
      } else {
        idxfile = args[1].concat(".gbx");
      }
      File idx = new File(idxfile);
      // create tmp  index file
      idxfile = idxfile + ".tmp";
      //System.out.println( "idxfile ="+ idxfile );
      ps = new PrintStream(
              new BufferedOutputStream(
                      new FileOutputStream(idxfile, false)));
      indexer.writeFileIndex(raf, ps, false);
      ps.close();
      File tidx = new File(idxfile);
      tidx.renameTo(idx);

    } else if (args.length == 1) {  // output to STDOUT
      ps = System.out;
      indexer.writeFileIndex(raf, ps, true);
    }

  }

}  // end Grib1Indexer


