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

// $Id: Grib1Input.java,v 1.33 2006/04/28 16:19:14 rkambic Exp $


package ucar.grib.grib1;


import ucar.grib.*;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/*
 * Grib1Input.java  1.0  09/31/2004
 * @author Robb Kambic
 *
 */

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A class that scans a GRIB file to extract product information.
 */

public final class Grib1Input {

    /*
     * raf for grib file
     */
    private final ucar.unidata.io.RandomAccessFile raf;

    /*
     * the header of Grib record
     */
    private String header = "GRIB1";

    /**
     * Pattern to extract header.
     */
    private static final Pattern productID = Pattern.compile("(\\w{6} \\w{4} \\d{6})");

    /*
     * stores records of Grib file, records consist of objects for each section.
     * there are 5 sections to a Grib1 record.
     */
    private final ArrayList<Grib1Record> records = new ArrayList<Grib1Record>();

    /*
     * stores products of Grib file, products have enough info to get the
     * metadata about a parameter and the data. products are lightweight
     * records.
     */
    private final ArrayList<Grib1Product> products = new ArrayList<Grib1Product>();

    /*
     * stores GDS of Grib1 file, there is possibility of more than 1
     */
    private final HashMap<String,Grib1GridDefinitionSection> gdsHM =
        new HashMap<String,Grib1GridDefinitionSection>();

    // *** constructors *******************************************************
    /**
     * Constructs a <tt>Grib1Input</tt> object from a raf.
     *
     * @param raf with GRIB content
     *
     */
    public Grib1Input(RandomAccessFile raf) {
        this.raf = raf;
    }

    /**
     * scans a Grib file to gather information that could be used to
     * create an index or dump the metadata contents.
     *
     * @param getProducts products have enough information for data extractions
     * @param oneRecord returns after processing one record in the Grib file
     * @throws NoValidGribException not supported grib feature
     * @throws NotSupportedException not supported grib feature
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
                long EOR = raf.getFilePointer() + is.getGribLength() - is.getLength();

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
                                          gdsOffset, dataOffset);
                                          //gdsOffset, raf.getFilePointer()); // TODO: check delete
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
                    // TODO: check if this is needed anymore
                    //checkGDSkeys(gds, gdsCounter);
                    return;
                }

            }  // end if seekHeader
            //System.out.println( "raf.getFilePointer()=" + raf.getFilePointer());
            //System.out.println( "raf.length()=" + raf.length() );
        }  // end while raf.getFilePointer() < raf.length()
        //System.out.println("GribInput: processed in " +
        //   (System.currentTimeMillis()- start) + " milliseconds");
        // TODO: check if this is needed anymore
        //checkGDSkeys(gds, gdsCounter);
        return;
    }  // end scan

    /**
     * Grib edition number 1, 2 or 0 not a Grib file.
     * @throws IOException  raf read
     * @return int 0 not a Grib file, 1 Grib1, 2 Grib2
     */
    public final int getEdition() throws IOException {
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
     * @param raf RandomAccessFile_
     * @param stop as long
     * @return found header boolean
     * @throws IOException raf read
     */
    private boolean seekHeader(RandomAccessFile raf, long stop) throws IOException {
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

    /**  //TODO: check if this needed anymore
     * @param gds Grib1GridDefinitionSection
     * @param gdsCounter as HashMap
     * @return gds key
     */
    private String getGDSkey(Grib1GridDefinitionSection gds,
                             HashMap<String,String> gdsCounter) {

        //String key = gds.getCheckSum();
        String key = Integer.toString(gds.getGdsKey());
        // only Lat/Lon grids can have > 1 GDSs // TODO: check if this is true
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
     * @deprecated
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
    public final ArrayList<Grib1Product> getProducts () {
        return products;
    }

    /**
     * Get records of the GRIB file.
     *
     * @return records
     */
    public final ArrayList<Grib1Record> getRecords() {
        return records;
    }

    /**
     * Get GDS's of the GRIB file.
     *
     * @return gdsHM
     */
    public final HashMap<String,Grib1GridDefinitionSection> getGDSs() {
        return gdsHM;
    }

}  // end Grib1Input


