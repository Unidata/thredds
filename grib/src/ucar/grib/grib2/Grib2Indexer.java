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

// $Id: Grib2Indexer.java,v 1.36 2006/08/18 20:22:10 rkambic Exp $


package ucar.grib.grib2;


/**
 * Grib2Indexer.java
 * @author Robb Kambic  10/20/03
 *
 *
 */

// import statements
import ucar.unidata.io.RandomAccessFile;

    import ucar.grib.*;

    import java.io.BufferedOutputStream;  // Input/Output functions
import java.io.FileOutputStream;      // Input/Output functions
import java.io.IOException;
import java.io.PrintStream;           // Input/Output functions
import java.io.File;

import java.lang.*;                   // Standard java functions

import java.util.*;                   // Extra utilities from sun


/**
 * Creates an index for a given Grib file.
 * see <a href="../../../IndexFormat.txt"> IndexFormat.txt</a>
 *  @deprecated
 */
public class Grib2Indexer {

    /**
     * Write a Grib file index; optionally create an in-memory index.
     * @param inputRaf GRIB file raf
     * @param ps write output to here
     * @param makeIndex  make an in-memory index if true
     * @throws IOException
     * @return Index if makeIndex is true, else null
     */
    public final Index writeFileIndex(RandomAccessFile inputRaf,
                                             PrintStream ps,
                                             boolean makeIndex)
            throws IOException {

       /**
        * date format "yyyy-MM-dd'T'HH:mm:ss'Z'".
        */
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));  //same as UTC

        Date now = Calendar.getInstance().getTime();
        //System.out.println(now.toString() + " ... Start of GribIndexer");
        long  start = System.currentTimeMillis();
        int   count = 0;
        // set buffer size for performance
        int rafBufferSize = inputRaf.getBufferSize();
        inputRaf.setBufferSize( Grib2WriteIndex.indexRafBufferSize );

        Index index = makeIndex
                      ? new Index()
                      : null;

        // Opening of grib data must be inside a try-catch block
        try {
            inputRaf.seek(0);
            // Create Grib2Input instance
            Grib2Input g2i = new Grib2Input(inputRaf);
            // params getProducts (implies  unique GDSs too), oneRecord
            g2i.scan(true, false);

            // Section 1 Global attributes
            // while needed here to process complete stream
            ps.println("index_version = " + GribReadTextIndex.currentTextIndexVersion);
            ps.println("grid_edition = " + 2);
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
            List products = g2i.getProducts();
            for (int i = 0; i < products.size(); i++) {
                Grib2Product product = (Grib2Product) products.get(i);
                Grib2ProductDefinitionSection pds = product.getPDS();
                Grib2IdentificationSection    id  = product.getID();
                if (i == 0) {
                    ps.println("center = " + id.getCenter_id());
                    ps.println("sub_center = " + id.getSubcenter_id());
                    ps.println("table_version = "
                               + id.getLocal_table_version());
                    ps.println(
                        "--------------------------------------------------------------------");
                }
                // skips records that have missing parameters
                if( product.getDiscipline() == 255 || pds.getParameterCategory() == 255 )
                    continue;
                // 
                ps.println(pds.getProductDefinition() + " "
                           + product.getDiscipline() + " "
                           + pds.getParameterCategory() + " "
                           + pds.getParameterNumber() + " "
                           + pds.getTypeGenProcess() + " "
                           + pds.getTypeFirstFixedSurface() + " "
                           + pds.getValueFirstFixedSurface() + " "
                           + pds.getTypeSecondFixedSurface() + " "
                           + pds.getValueSecondFixedSurface() + " "
                           + dateFormat.format( product.getBaseTime()) + " "
                           + pds.getForecastTime() + " "
                           + product.getGDSkey() + " "
                           + product.getGdsOffset() + " "
                           + product.getPdsOffset());

                if (makeIndex) {
                    index.addGribRecord(makeGribRecord(index, product));
                }
                count++;
            }

            // section 3: GDSs in this File
            Map gdsHM = g2i.getGDSs();
            for (Iterator it = gdsHM.keySet().iterator(); it.hasNext(); ) {
                ps.println(
                    "--------------------------------------------------------------------");
                String key = (String) it.next();
                ps.println("GDSkey = " + key);
                Grib2GridDefinitionSection gds =
                    (Grib2GridDefinitionSection) gdsHM.get(key);
                printGDS(gds, ps);
                if (makeIndex) {
                    index.addHorizCoordSys(makeGdsRecord(gds));
                }
            }

            // Catch thrown errors from GribFile
        } catch (NotSupportedException noSupport) {
            System.err.println("NotSupportedException : " + noSupport);

        } finally {
          //reset
          inputRaf.setBufferSize( rafBufferSize );
          ps.close();
        }

        //System.out.println(" "+count+" products took "+(System.currentTimeMillis() - start) + " msec");

        return index;
    }  // end writeFileIndex

    /**
     * creates a GribRecord from a Grib2Product.
     *
     * @param index _more_
     * @param product
     * @return Index.GribRecord
     */

    protected static final Index.GribRecord makeGribRecord(Index index,
            Grib2Product product) {
        Index.GribRecord              gr  = index.getGribRecord();

        Grib2ProductDefinitionSection pds = product.getPDS();

        gr.productType    = pds.getProductDefinition();
        gr.discipline     = product.getDiscipline();
        gr.category       = pds.getParameterCategory();
        gr.paramNumber    = pds.getParameterNumber();
        gr.typeGenProcess = pds.getTypeGenProcess();
        gr.levelType1     = pds.getTypeFirstFixedSurface();
        gr.levelValue1    = pds.getValueFirstFixedSurface();
        gr.levelType2     = pds.getTypeSecondFixedSurface();
        gr.levelValue2    = pds.getValueSecondFixedSurface();
        gr.refTime        = product.getBaseTime();
        gr.forecastTime   = pds.getForecastTime();
        gr.gdsKey         = product.getGDSkey().trim().intern();
        gr.offset1        = product.getGdsOffset();
        gr.offset2        = product.getPdsOffset();

        return gr;
    }

    /**
     * Creates a Index.GdsRecord from a GDS
     * @param gds
     * @return Index.GdsRecord
     */
    protected static final Index.GdsRecord makeGdsRecord(
            Grib2GridDefinitionSection gds) {

        Index.GdsRecord igds = new Index.GdsRecord();

        igds.gdsKey = gds.getCheckSum();
        igds.addParam("GDSkey", gds.getCheckSum());
        //System.out.println( "igds.gdsKey ="+ igds.gdsKey );

        String winds = GribNumbers.isBitSet(gds.getResolution(), GribNumbers.BIT_5)
                       ? "Relative"
                       : "True";

        if (((gds.getGdtn() < 50) || (gds.getGdtn() > 53))
                && (gds.getGdtn() != 100) && (gds.getGdtn() != 120)
                && (gds.getGdtn() != 1200)) {
            igds.grid_shape_code = gds.getShape();
            igds.addParam("grid_shape_code",
                          Integer.toString(gds.getShape()));
            igds.grid_type = gds.getGdtn();
            igds.addParam("grid_type", Integer.toString(gds.getGdtn()));
            igds.addParam("grid_shape", gds.getShapeName());

            if ((gds.getShape() < 2) || (gds.getShape() == 6)) {
                igds.radius_spherical_earth = gds.getEarthRadius();
                igds.addParam("grid_radius_spherical_earth",
                              Double.toString(gds.getEarthRadius()));

            } else if ((gds.getShape() > 1) && (gds.getShape() < 5)) {
                igds.major_axis_earth = gds.getMajorAxis();
                igds.addParam("grid_major_axis_earth",
                              Double.toString(gds.getMajorAxis()));
                igds.minor_axis_earth = gds.getMinorAxis();
                igds.addParam("grid_minor_axis_earth",
                              Double.toString(gds.getMinorAxis()));
            }
        }
        switch (gds.getGdtn()) {                // Grid Definition Template Number

          case 0 :
          case 1 :
          case 2 :
          case 3 :                              // Latitude/Longitude Grid

              igds.nx = gds.getNx();
              igds.addParam("Nx", Integer.toString(gds.getNx()));
              igds.ny = gds.getNy();
              igds.addParam("Ny", Integer.toString(gds.getNy()));
              igds.La1 = gds.getLa1();
              igds.addParam("La1", Double.toString(gds.getLa1()));
              igds.Lo1 = gds.getLo1();
              igds.addParam("Lo1", Double.toString(gds.getLo1()));
              igds.resolution = gds.getResolution();
              igds.addParam("ResCompFlag", Integer.toString(gds.getResolution() ));
              igds.winds      = winds;
              igds.addParam("La2", Double.toString(gds.getLa2()));
              igds.addParam("Lo2", Double.toString(gds.getLo2()));
              igds.dx = gds.getDx();
              igds.addParam("Dx", Double.toString(gds.getDx()));
              igds.dy = gds.getDy();
              igds.addParam("Dy", Double.toString(gds.getDy()));

              if (gds.getGdtn() == 1) {         //Rotated Latitude/longitude
                  igds.addParam("SpLat", Double.toString(gds.getSpLat()));
                  igds.addParam("SpLon", Double.toString(gds.getSpLon()));

              } else if (gds.getGdtn() == 2) {  //Stretched Latitude/longitude
                  igds.addParam("pLat", Double.toString(gds.getPoleLat()));
                  igds.addParam("pLon", Double.toString(gds.getPoleLon()));

              } else if (gds.getGdtn() == 3) {  //Stretched and Rotated
                  // Latitude/longitude
                  igds.addParam("SpLat", Double.toString(gds.getSpLat()));
                  igds.addParam("SpLon", Double.toString(gds.getSpLon()));

              }
              break;

          case 10 :                              // Mercator

              igds.nx = gds.getNx();
              igds.addParam("Nx", Integer.toString(gds.getNx()));
              igds.ny = gds.getNy();
              igds.addParam("Ny", Integer.toString(gds.getNy()));
              igds.La1 = gds.getLa1();
              igds.addParam("La1", Double.toString(gds.getLa1()));
              igds.Lo1 = gds.getLo1();
              igds.addParam("Lo1", Double.toString(gds.getLo1()));
              igds.resolution = gds.getResolution();
              igds.addParam("ResCompFlag", Integer.toString(gds.getResolution() ));
              igds.winds      = winds;
              igds.addParam("La2", Double.toString(gds.getLa2()));
              igds.addParam("Lo2", Double.toString(gds.getLo2()));
              igds.addParam("Angle", Double.toString(gds.getAngle()));
              igds.dx = gds.getDx();
              igds.addParam("Dx", Double.toString(gds.getDx()));
              igds.dy = gds.getDy();
              igds.addParam("Dy", Double.toString(gds.getDy()));

              break;

          case 20 :  // Polar stereographic projection

              igds.nx = gds.getNx();
              igds.addParam("Nx", Integer.toString(gds.getNx()));
              igds.ny = gds.getNy();
              igds.addParam("Ny", Integer.toString(gds.getNy()));
              igds.La1 = gds.getLa1();
              igds.addParam("La1", Double.toString(gds.getLa1()));
              igds.Lo1 = gds.getLo1();
              igds.addParam("Lo1", Double.toString(gds.getLo1()));
              igds.resolution = gds.getResolution();
              igds.addParam("ResCompFlag", Integer.toString(gds.getResolution() ));
              String NpProj = ((gds.getProjectionCenter() & 128) == 0)
                       ? "true"
                       : "false";
              igds.addParam("NpProj", NpProj);
              //System.out.println( "makeGdsRec NpProj ="+ NpProj );
              igds.winds      = winds;
              igds.LaD        = gds.getLad();
              igds.addParam("Lad", Double.toString(gds.getLad()));
              igds.LoV = gds.getLov();
              igds.addParam("Lov", Double.toString(gds.getLov()));
              igds.dx = gds.getDx();
              igds.addParam("Dx", Double.toString(gds.getDx()));
              igds.dy = gds.getDy();
              igds.addParam("Dy", Double.toString(gds.getDy()));

              break;

          case 30 :                              // Lambert Conformal

              igds.nx = gds.getNx();
              igds.addParam("Nx", Integer.toString(gds.getNx()));
              igds.ny = gds.getNy();
              igds.addParam("Ny", Integer.toString(gds.getNy()));
              igds.La1 = gds.getLa1();
              igds.addParam("La1", Double.toString(gds.getLa1()));
              igds.Lo1 = gds.getLo1();
              igds.addParam("Lo1", Double.toString(gds.getLo1()));
              igds.resolution = gds.getResolution();
              igds.addParam("ResCompFlag", Integer.toString(gds.getResolution() ));
              NpProj = ((gds.getProjectionCenter() & 128) == 0)
                       ? "true"
                       : "false";
              igds.addParam("NpProj", NpProj);
              //System.out.println( "makeGdsRec NpProj ="+ NpProj );
              igds.winds      = winds;
              igds.LaD        = gds.getLad();
              igds.addParam("Lad", Double.toString(gds.getLad()));
              igds.LoV = gds.getLov();
              igds.addParam("Lov", Double.toString(gds.getLov()));
              igds.dx = gds.getDx();
              igds.addParam("Dx", Double.toString(gds.getDx()));
              igds.dy = gds.getDy();
              igds.addParam("Dy", Double.toString(gds.getDy()));
              igds.latin1 = gds.getLatin1();
              igds.addParam("Latin1", Double.toString(gds.getLatin1()));
              igds.latin2 = gds.getLatin2();
              igds.addParam("Latin2", Double.toString(gds.getLatin2()));
              igds.addParam("SpLat", Double.toString(gds.getSpLat()));
              igds.addParam("SpLon", Double.toString(gds.getSpLon()));

              break;

          case 31 :                              // Albers equal area

              igds.nx = gds.getNx();
              igds.addParam("Nx", Integer.toString(gds.getNx()));
              igds.ny = gds.getNy();
              igds.addParam("Ny", Integer.toString(gds.getNy()));
              igds.La1 = gds.getLa1();
              igds.addParam("La1", Double.toString(gds.getLa1()));
              igds.Lo1 = gds.getLo1();
              igds.addParam("Lo1", Double.toString(gds.getLo1()));
              igds.resolution = gds.getResolution();
              igds.addParam("ResCompFlag", Integer.toString(gds.getResolution() ));
              NpProj = ((gds.getProjectionCenter() & 128) == 0)
                       ? "true"
                       : "false";
              igds.addParam("NpProj", NpProj);
              //System.out.println( "makeGdsRec NpProj ="+ NpProj );
              igds.winds      = winds;
              igds.LaD        = gds.getLad();
              igds.addParam("Lad", Double.toString(gds.getLad()));
              igds.LoV = gds.getLov();
              igds.addParam("Lov", Double.toString(gds.getLov()));
              igds.dx = gds.getDx();
              igds.addParam("Dx", Double.toString(gds.getDx()));
              igds.dy = gds.getDy();
              igds.addParam("Dy", Double.toString(gds.getDy()));
              igds.latin1 = gds.getLatin1();
              igds.addParam("Latin1", Double.toString(gds.getLatin1()));
              igds.latin2 = gds.getLatin2();
              igds.addParam("Latin2", Double.toString(gds.getLatin2()));
              igds.addParam("SpLat", Double.toString(gds.getSpLat()));
              igds.addParam("SpLon", Double.toString(gds.getSpLon()));

              break;

          case 40 :
          case 41 :
          case 42 :
          case 43 :  // Gaussian latitude/longitude

              igds.nx = gds.getNx();
              igds.addParam("Nx", Integer.toString(gds.getNx()));
              igds.ny = gds.getNy();
              igds.addParam("Ny", Integer.toString(gds.getNy()));
              igds.La1 = gds.getLa1();
              igds.addParam("La1", Double.toString(gds.getLa1()));
              igds.Lo1 = gds.getLo1();
              igds.addParam("Lo1", Double.toString(gds.getLo1()));
              igds.resolution = gds.getResolution();
              igds.addParam("ResCompFlag", Integer.toString(gds.getResolution() ));
              igds.winds      = winds;
              igds.addParam("La2", Double.toString(gds.getLa2()));
              igds.addParam("Lo2", Double.toString(gds.getLo2()));
              igds.dx = gds.getDx();
              igds.addParam("Dx", Double.toString(gds.getDx()));
              igds.addParam("NumberParallels", Double.toString(gds.getN()));

              if (gds.getGdtn() == 41) {         //Rotated Gaussian Latitude/longitude
                  igds.addParam("SpLat", Double.toString(gds.getSpLat()));
                  igds.addParam("SpLon", Double.toString(gds.getSpLon()));

              } else if (gds.getGdtn() == 42) {  //Stretched Gaussian
                  igds.addParam("pLat", Double.toString(gds.getPoleLat()));
                  igds.addParam("pLon", Double.toString(gds.getPoleLon()));
                  // Latitude/longitude
              } else if (gds.getGdtn() == 43) {  //Stretched and Rotated Gaussian
                  // Latitude/longitude
                  igds.addParam("SpLat", Double.toString(gds.getSpLat()));
                  igds.addParam("SpLon", Double.toString(gds.getSpLon()));
                  igds.addParam("Angle", Double.toString(gds.getAngle()));
                  igds.addParam("pLat", Double.toString(gds.getPoleLat()));
                  igds.addParam("pLon", Double.toString(gds.getPoleLon()));

              }

              break;

          case 50 :
          case 51 :
          case 52 :
          case 53 :   // Spherical harmonic coefficients

              break;

          case 90 :   // Space view perspective or orthographic

              igds.nx = gds.getNx();
              igds.addParam("Nx", Integer.toString(gds.getNx()));
              igds.ny = gds.getNy();
              igds.addParam("Ny", Integer.toString(gds.getNy()));
              igds.addParam("Lap", Double.toString(gds.getLap()));
              igds.addParam("Lop", Double.toString(gds.getLop()));
              igds.resolution = gds.getResolution();
              igds.addParam("ResCompFlag", Integer.toString(gds.getResolution() ));
              igds.winds      = winds;
              igds.dx         = gds.getDx();
              igds.addParam("Dx", Double.toString(gds.getDx()));
              igds.dy = gds.getDy();
              igds.addParam("Dy", Double.toString(gds.getDy()));
              igds.addParam("Xp", Double.toString(gds.getXp()));
              igds.addParam("Yp", Double.toString(gds.getYp()));
              igds.addParam("Angle", Double.toString(gds.getAngle()));
              igds.addParam("Nr", Double.toString(gds.getAltitude()));
              igds.addParam("Xo", Double.toString(gds.getXo()));
              igds.addParam("Yo", Double.toString(gds.getYo()));

              break;

          case 100 :  // Triangular grid based on an icosahedron

              igds.addParam("pLat", Double.toString(gds.getPoleLat()));
              igds.addParam("pLon", Double.toString(gds.getPoleLon()));

              break;

          case 110 :  // Equatorial azimuthal equidistant projection

              igds.nx = gds.getNx();
              igds.addParam("Nx", Integer.toString(gds.getNx()));
              igds.ny = gds.getNy();
              igds.addParam("Ny", Integer.toString(gds.getNy()));
              igds.La1 = gds.getLa1();
              igds.addParam("La1", Double.toString(gds.getLa1()));
              igds.Lo1 = gds.getLo1();
              igds.addParam("Lo1", Double.toString(gds.getLo1()));
              igds.resolution = gds.getResolution();
              igds.addParam("ResCompFlag", Integer.toString(gds.getResolution() ));
              NpProj = ((gds.getProjectionCenter() & 128) == 0)
                       ? "true"
                       : "false";
              igds.addParam("NpProj", NpProj);
              //System.out.println( "makeGdsRec NpProj ="+ NpProj );
              igds.winds      = winds;
              igds.dx         = gds.getDx();
              igds.addParam("Dx", Double.toString(gds.getDx()));
              igds.dy = gds.getDy();
              igds.addParam("Dy", Double.toString(gds.getDy()));

              break;


          case 120 :  // Azimuth-range Projection

              igds.La1 = gds.getLa1();
              igds.addParam("La1", Double.toString(gds.getLa1()));
              igds.Lo1 = gds.getLo1();
              igds.addParam("Lo1", Double.toString(gds.getLo1()));
              igds.dx = gds.getDx();
              igds.addParam("Dx", Double.toString(gds.getDx()));

              break;

          case 204 :   // Curvilinear orthographic

              igds.nx = gds.getNx();
              igds.addParam("Nx", Integer.toString(gds.getNx()));
              igds.ny = gds.getNy();
              igds.addParam("Ny", Integer.toString(gds.getNy()));
              igds.resolution = gds.getResolution();
              igds.addParam("ResCompFlag", Integer.toString(gds.getResolution() ));
              igds.winds      = winds;
              //igds.dx         = gds.getDx();
              //igds.addParam("Dx", Double.toString(gds.getDx()));
              //igds.dy = gds.getDy();
              //igds.addParam("Dy", Double.toString(gds.getDy()));

              break;
          /*
                 case 1000: //  Cross-Section Grid with Points Equally Spaced on the Horizontal

                 igds.La1  = gds.getLa1();
                 igds.addParam( "La1", Double.toString( gds.getLa1() ) );
                 igds.Lo1  = gds.getLo1();
                 igds.addParam( "Lo1", Double.toString( gds.getLo1() ) );
                 igds.resolution  = gds.getResolution();
                 igds.addParam("ResCompFlag", Integer.toString(gds.getResolution() ));
                 igds.winds = winds;
                 igds.addParam( "La2", Double.toString( gds.getLa2() ) );
                 igds.addParam( "Lo2", Double.toString( gds.getLo2() ) );

                 break;
          */

        }  // end switch gdtn

        return igds;

    }


    /**
     * prints out a products GDS.
     * @param gds
     * @param ps printStream used to write Index
     */
    public void printGDS(Grib2GridDefinitionSection gds,
                                PrintStream ps) {

        ps.println("grid_type = " + gds.getGdtn());
        ps.println("grid_name = " + gds.getName());

        String winds = GribNumbers.isBitSet(gds.getResolution(), GribNumbers.BIT_5)
                       ? "Relative"
                       : "True";

        if (((gds.getGdtn() < 50) || (gds.getGdtn() > 53))
                && (gds.getGdtn() != 100) && (gds.getGdtn() != 120)
                && (gds.getGdtn() != 1200)) {
            ps.println("grid_shape_code = " + gds.getShape());
            ps.println("grid_shape = " + gds.getShapeName());
            if ((gds.getShape() < 2) || (gds.getShape() == 6)) {
                ps.println("grid_radius_spherical_earth = "
                           + gds.getEarthRadius());
            } else if ((gds.getShape() > 1) && (gds.getShape() < 5)) {
                ps.println("grid_major_axis_earth = " + gds.getMajorAxis());
                ps.println("grid_minor_axis_earth = " + gds.getMinorAxis());
            }
        }

        switch (gds.getGdtn()) {                // Grid Definition Template Number

          case 0 :
          case 1 :
          case 2 :
          case 3 :                              // Latitude/Longitude Grid

              ps.println("Nx = " + gds.getNx());
              ps.println("Ny = " + gds.getNy());
              ps.println("La1 = " + gds.getLa1());
              ps.println("Lo1 = " + gds.getLo1());
              ps.println("ResCompFlag = " + gds.getResolution());
              ps.println("Winds = " + winds);
              ps.println("La2 = " + gds.getLa2());
              ps.println("Lo2 = " + gds.getLo2());
              ps.println("Dx = " + gds.getDx());
              ps.println("Dy = " + gds.getDy());
              ps.println( "grid_units = " + gds.getGrid_units() );
              //ps.println("ScanningMode = " + gds.getScanMode());

              if (gds.getGdtn() == 1) {         //Rotated Latitude/longitude
                  ps.println("SpLat = " + gds.getSpLat());
                  ps.println("SpLon = " + gds.getSpLon());
                  ps.println("RotationAngle = " + gds.getRotationangle());

              } else if (gds.getGdtn() == 2) {  //Stretched Latitude/longitude
                  ps.println("pLat = " + gds.getPoleLat());
                  ps.println("pLon = " + gds.getPoleLon());
                  ps.println("StretchingFactor = " + gds.getFactor());

              } else if (gds.getGdtn() == 3) {  //Stretched and Rotated 
                  // Latitude/longitude
                  ps.println("SpLat = " + gds.getSpLat());
                  ps.println("SpLon = " + gds.getSpLon());
                  ps.println("RotationAngle = " + gds.getRotationangle());
                  ps.println("pLat = " + gds.getPoleLat());
                  ps.println("pLon = " + gds.getPoleLon());
                  ps.println("StretchingFactor = " + gds.getFactor());
              }
              break;

          case 10 :                              // Mercator
              ps.println("Nx = " + gds.getNx());
              ps.println("Ny = " + gds.getNy());
              ps.println("La1 = " + gds.getLa1());
              ps.println("Lo1 = " + gds.getLo1());
              ps.println("ResCompFlag = " + gds.getResolution());
              ps.println("Winds = " + winds);
              ps.println("LaD = " + gds.getLad());
              ps.println("La2 = " + gds.getLa2());
              ps.println("Lo2 = " + gds.getLo2());
              //ps.println("ScanningMode = " + gds.getScanMode());
              ps.println("BasicAngle = " + gds.getAngle());
              ps.println("Dx = " + gds.getDx());
              ps.println("Dy = " + gds.getDy());
              ps.println( "grid_units = " + gds.getGrid_units() );

              break;

          case 20 :  // Polar stereographic projection

              ps.println("Nx = " + gds.getNx());
              ps.println("Ny = " + gds.getNy());
              ps.println("La1 = " + gds.getLa1());
              ps.println("Lo1 = " + gds.getLo1());
              ps.println("ResCompFlag = " + gds.getResolution());
              ps.println("Winds = " + winds);
              ps.println("LaD = " + gds.getLad());
              ps.println("LoV = " + gds.getLov());
              ps.println("Dx = " + gds.getDx());
              ps.println("Dy = " + gds.getDy());
              ps.println( "grid_units = " + gds.getGrid_units() );
              ps.println("ProjFlag = " + gds.getProjectionCenter());
              ps.println("NpProj = " + ((gds.getProjectionCenter() & 128) == 0));
              //ps.println("ScanningMode = " + gds.getScanMode());

              break;

          case 30 :                              // Lambert Conformal

              ps.println("Nx = " + gds.getNx());
              ps.println("Ny = " + gds.getNy());
              ps.println("La1 = " + gds.getLa1());
              ps.println("Lo1 = " + gds.getLo1());
              ps.println("ResCompFlag = " + gds.getResolution());
              ps.println("Winds = " + winds);
              ps.println("LaD = " + gds.getLad());
              ps.println("LoV = " + gds.getLov());
              ps.println("Dx = " + gds.getDx());
              ps.println("Dy = " + gds.getDy());
              ps.println( "grid_units = " + gds.getGrid_units() );
              ps.println("ProjFlag = " + gds.getProjectionCenter());
              ps.println("NpProj = " + ((gds.getProjectionCenter() & 128) == 0));
              //ps.println("ScanningMode = " + gds.getScanMode());
              ps.println("Latin1 = " + gds.getLatin1());
              ps.println("Latin2 = " + gds.getLatin2());
              ps.println("SpLat = " + gds.getSpLat());
              ps.println("SpLon = " + gds.getSpLon());

              break;

          case 40 :
          case 41 :
          case 42 :
          case 43 :  // Gaussian latitude/longitude
              ps.println("Nx = " + gds.getNx());
              ps.println("Ny = " + gds.getNy());
              ps.println("La1 = " + gds.getLa1());
              ps.println("Lo1 = " + gds.getLo1());
              ps.println("ResCompFlag = " + gds.getResolution());
              ps.println("Winds = " + winds);
              ps.println("La2 = " + gds.getLa2());
              ps.println("Lo2 = " + gds.getLo2());
              ps.println("Dx = " + gds.getDx());
              ps.println( "grid_units = " + gds.getGrid_units() );
              ps.println("StretchingFactor = " + gds.getFactor());
              ps.println("NumberParallels = " + gds.getN());
              //ps.println("ScanningMode = " + gds.getScanMode());

              if (gds.getGdtn() == 41) {         //Rotated Gaussian Latitude/longitude
                  ps.println("SpLat = " + gds.getSpLat());
                  ps.println("SpLon = " + gds.getSpLon());
                  ps.println("RotationAngle = " + gds.getRotationangle());

              } else if (gds.getGdtn() == 42) {  //Stretched Gaussian 
                  // Latitude/longitude
                  ps.println("pLat = " + gds.getPoleLat());
                  ps.println("pLon = " + gds.getPoleLon());
                  ps.println("StretchingFactor = " + gds.getFactor());

              } else if (gds.getGdtn() == 43) {  //Stretched and Rotated Gaussian  
                  // Latitude/longitude
                  ps.println("SpLat = " + gds.getSpLat());
                  ps.println("SpLon = " + gds.getSpLon());
                  ps.println("RotationAngle = " + gds.getRotationangle());
                  ps.println("pLat = " + gds.getPoleLat());
                  ps.println("pLon = " + gds.getPoleLon());
                  ps.println("StretchingFactor = " + gds.getFactor());

              }
              break;

          case 50 :
          case 51 :
          case 52 :
          case 53 :                       // Spherical harmonic coefficients
              ps.println("J = " + gds.getJ());
              ps.println("K = " + gds.getK());
              ps.println("M = " + gds.getM());
              ps.println("MethodNorm = " + gds.getMethod());
              ps.println("ModeOrder = " + gds.getMode());
              ps.println( "grid_units = " + gds.getGrid_units() );
              if (gds.getGdtn() == 51) {  //Rotated Spherical harmonic coefficients
                  ps.println("SpLat = " + gds.getSpLat());
                  ps.println("SpLon = " + gds.getSpLon());
                  ps.println("RotationAngle = " + gds.getRotationangle());

              } else if (gds.getGdtn() == 52) {  //Stretched Spherical 
                  // harmonic coefficients
                  ps.println("pLat = " + gds.getPoleLat());
                  ps.println("pLon = " + gds.getPoleLon());
                  ps.println("StretchingFactor = " + gds.getFactor());

              } else if (gds.getGdtn() == 53) {  //Stretched and Rotated 
                  // Spherical harmonic coefficients
                  ps.println("SpLat = " + gds.getSpLat());
                  ps.println("SpLon = " + gds.getSpLon());
                  ps.println("RotationAngle = " + gds.getRotationangle());
                  ps.println("pLat = " + gds.getPoleLat());
                  ps.println("pLon = " + gds.getPoleLon());
                  ps.println("StretchingFactor = " + gds.getFactor());

              }
              break;

          case 90 :  // Space view perspective or orthographic
              ps.println("Nx = " + gds.getNx());
              ps.println("Ny = " + gds.getNy());
              ps.println("Lap = " + gds.getLap());
              ps.println("Lop = " + gds.getLop());
              //ps.println("NumberPointsParallel = " + gds.getNx());
              //ps.println("NumberPointsMeridian = " + gds.getNy());
              //ps.println("LatitudeSub-satellitePoint = " + gds.getLap());
              //ps.println("LongitudeSub-satellitePoint = " + gds.getLop());
              ps.println("ResCompFlag = " + gds.getResolution());
              ps.println("Winds = " + winds);
              ps.println("Dx = " + gds.getDx());
              ps.println("Dy = " + gds.getDy());
              ps.println( "grid_units = " + gds.getGrid_units() );
              ps.println("Xp = " + gds.getXp());
              ps.println("Yp = " + gds.getYp());
              //ps.println("ScanningMode = " + gds.getScanMode());
              ps.println("Angle = " + gds.getAngle());
              ps.println("Nr = " + gds.getAltitude());
              ps.println("Xo = " + gds.getXo());
              ps.println("Yo = " + gds.getYo());

              break;

          case 100 :  // Triangular grid based on an icosahedron
              ps.println("Exponent2Intervals = " + gds.getN2());
              ps.println("Exponent3Intervals = " + gds.getN3());
              ps.println("NumberIntervals = " + gds.getNi());
              ps.println("NumberDiamonds = " + gds.getNd());
              ps.println("pLat = " + gds.getPoleLat());
              ps.println("pLon = " + gds.getPoleLon());
              ps.println("GridPointPosition = " + gds.getPosition());
              ps.println("NumberOrderDiamonds = " + gds.getOrder());
              //ps.println("ScanningMode = " + gds.getScanMode());
              ps.println("NumberParallels = " + gds.getN());
              ps.println( "grid_units = " + gds.getGrid_units() );

              break;

          case 110 :  // Equatorial azimuthal equidistant projection
              ps.println("Nx = " + gds.getNx());
              ps.println("Ny = " + gds.getNy());
              ps.println("La1 = " + gds.getLa1());
              ps.println("Lo1 = " + gds.getLo1());
              ps.println("ResCompFlag = " + gds.getResolution());
              ps.println("NpProj = " + ((gds.getProjectionCenter() & 128) == 0));
              ps.println("Winds = " + winds);
              ps.println("Dx = " + gds.getDx());
              ps.println("Dy = " + gds.getDy());
              ps.println( "grid_units = " + gds.getGrid_units() );
              ps.println("ProjFlag = " + gds.getProjectionCenter());
              //ps.println("ScanningMode = " + gds.getScanMode());

              break;

          case 120 :  // Azimuth-range Projection
              ps.println("NumberDataBins = " + gds.getNb());
              ps.println("NumberRadials = " + gds.getNr());
              ps.println("NumberPointsParallel = " + gds.getNx());
              ps.println("La1 = " + gds.getLa1());
              ps.println("Lo1 = " + gds.getLo1());
              ps.println("Dx = " + gds.getDx());
              ps.println( "grid_units = " + gds.getGrid_units() );
              ps.println("OffsetFromOrigin = " + gds.getDstart());
              //ps.println( "need code to get azi and adelta" );

              break;

          case 204 :  // Curvilinear orthographic
              ps.println("Nx = " + gds.getNx());
              ps.println("Ny = " + gds.getNy());
              ps.println("ResCompFlag = " + gds.getResolution());
              ps.println("Winds = " + winds);
              //ps.println("Dx = " + gds.getDx());
              //ps.println("Dy = " + gds.getDy());
              ps.println( "grid_units = " + gds.getGrid_units() );
              //ps.println("ScanningMode = " + gds.getScanMode());

              break;

           default :
              ps.println("Unknown Grid Type" + gds.getGdtn());
        }             // end switch gdtn
        if (gds.getOlon() == 0) {
            ps.println("Quasi = false");
        } else {
            ps.println("Quasi = true");
        }
    }  // end printGDS

    /**
     *
     * Dumps usage of the class.
     * @param className Grib2Indexer
     *
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
     * creates a Grib2 index for given Grib2 file.
     * @param args 2 if Grib file and index file name given
     * @throws IOException
     */
    public static void main(String args[]) throws IOException {

        // Function References
        Grib2Indexer indexer = new Grib2Indexer();

        // test case for paths and files with spaces
//        String testName = "C:/Data Files/for grib data/test1.grib1";
//        testName = testName.replaceAll( " ", "%20");
//        System.out.println( "testName ="+ testName);
        // Test usage
        if (args.length < 1) {
            // Get class name as String
            Class cl = indexer.getClass();
            usage(cl.getName());
            System.exit(0);
        }

        RandomAccessFile raf    = null;
        PrintStream      ps;
        String           infile = args[0];
        raf = new RandomAccessFile(infile, "r");
        raf.order(RandomAccessFile.BIG_ENDIAN);

        if (args.length == 2) {  // input file and index file name given
            String idxfile;
            if (args[1].endsWith(".gbx")) {
                idxfile = args[1];
            } else {
                idxfile = args[1].concat(".gbx");
            }
            File idx = new File( idxfile );
            // create tmp  index file
            idxfile = idxfile +".tmp";
            //System.out.println( "idxfile ="+ idxfile );
            ps = new PrintStream(
                new BufferedOutputStream(
                    new FileOutputStream(idxfile, false)));
            indexer.writeFileIndex(raf, ps, false);
            ps.close();
            File tidx = new File( idxfile );
            tidx.renameTo( idx );

        } else if (args.length == 1) {  // output to STDOUT
            ps = System.out;
            indexer.writeFileIndex(raf, ps, false);
        }
    }
}  // end Grib2Indexer


