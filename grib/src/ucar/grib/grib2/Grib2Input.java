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

// $Id: Grib2Input.java,v 1.41 2006/04/28 16:18:38 rkambic Exp $


package ucar.grib.grib2;


import ucar.grib.GribNumbers;
import ucar.grib.NotSupportedException;

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.KMPMatch;

import java.io.IOException;

/*
 * Grib2Input.java  1.0  08/31/2004
 * @author Robb Kambic
 *
 */

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uses a RandomAccessFile to scans a GRIB2 file to extract product information.
 * A Grib record starts with the string GRIB. A GRIB record has 8 sections, the
 * detailed information is at:
 * http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml
 *
 */

public final class Grib2Input {

    /*
     *  /ucar/unidata/io/RandomAccessFile
     */
    private final RandomAccessFile raf;

    /*
     * default WMO header of a record
     */
    private String header = "GRIB2";

    /**
     * IDD Pattern to extract header.
     */
    private static final Pattern productID =
        Pattern.compile("(\\w{6} \\w{4} \\d{6})");


  /**
   * Used to find the first occurrence of GRIB in data.
   */
    private static final KMPMatch matcher = new KMPMatch("GRIB".getBytes());

    /*
     * stores record sections: header, is, id, gds, pds, drs, bms and
     * GdsOffset and PdsOffsets. used to return data of record
     */
    private final List<Grib2Record> records = new ArrayList<Grib2Record>();

    /*
     * stores header, is, id, GDSkey , pds, GdsOffset, PdsOffset
     * of the record. a product supplies enough information to extract the
     * data of the record. used in creating Grib file indexes
     */
    private final List<Grib2Product> products = new ArrayList<Grib2Product>();

    /*
     * stores all different GDSs of Grib2 file, there is possibility of more than 1
     */
    private final Map<String,Grib2GridDefinitionSection> gdsHM =
        new HashMap<String,Grib2GridDefinitionSection>();

    // *** constructors *******************************************************
    /**
     * Constructs a Grib2Input object from a raf.
     *
     * @param raf with GRIB content
     *
     */
    public Grib2Input(RandomAccessFile raf) {
        this.raf = raf;
    }

    /**
     * scans the Grib2 file obtaining Products or Records that contain all
     * needed information for data extraction later. For most purposes,
     * getProductsOnly should be set to true, it's lightweight of getRecords.
     * It is possible that one Grib record can have multiple same sections, so the
     * previous sections don't have to be repeated in another Grib record. An example
     * is the data section is repeated only for U and V componet of Winds in some records.
     * @param getProductsOnly get products verse get records
     * @param oneRecord if true then return first record
     * @return success was the read successfull
     * @throws NotSupportedException NotSupportedException
     * @throws IOException on data read
     */
    public final boolean scan(boolean getProductsOnly, boolean oneRecord)
            throws NotSupportedException, IOException {

        long                       start = System.currentTimeMillis();

        Grib2IndicatorSection      is    = null;
        Grib2IdentificationSection id    = null;
        Grib2LocalUseSection       lus   = null;
        Grib2GridDefinitionSection gds   = null;
        // if raf.getFilePointer() != 0 then called from Grib2IndexExtender
        if (raf.getFilePointer() > 4) {
            raf.seek(raf.getFilePointer() - 4);
            Grib2EndSection es = new Grib2EndSection(raf);
            if ( !es.getEndFound()) {  // ending found somewhere in file
                //System.out.println("Scan failed to find end of record");
                return false;
            }
            //System.out.println( "Scan succeeded to find end of record");
        }
        //System.out.println("Scan file pointer =" + raf.getFilePointer()); 
        long EOR = 0;
        boolean startAtHeader = true;  // otherwise skip to GDS
        boolean processGDS    = true;
        long    GdsOffset     = 0;     // GDS offset from start of file
        long PdsOffset = 0;
        Grib2ProductDefinitionSection pds = null;
        Grib2DataRepresentationSection drs = null;
        Grib2BitMapSection             bms = null;
        Grib2DataSection               ds  = null;
        //int mvm = -1;
        //float pmv = -1, smv = -1;
        while (raf.getFilePointer() < raf.length()) {
            if (startAtHeader) {  // begining of record
                if ( !seekHeader(raf, raf.length())) {
                    //System.out.println( "Scan seekHeader failed" );
                    return false;  // not valid Grib file
                }

                // Read Section 0 Indicator Section
                is = new Grib2IndicatorSection(raf);  // section 0
                //System.out.println( "Grib record length=" + is.getGribLength());
                // EOR (EndOfRecord) calculated so skipping data sections is faster
                EOR = raf.getFilePointer() + is.getGribLength()
                           - is.getLength();
                // TODO: delete is.getDiscipline from if when 40 beta released
                if( is.getGribEdition() == 1 || is.getDiscipline() == 255 ) {
                    //System.out.println( "Error Grib 1 record in Grib2 file" ) ;
                    raf.seek( EOR );
                    continue;
                }
                // Read other SectionsGrib2
                id = new Grib2IdentificationSection(raf);  // Section 1

            }  // end startAtHeader

            try { // catch all exceptions and seek to EOR

                if (processGDS) {
                    // check for Local Use Section 2
                    lus = new Grib2LocalUseSection(raf);

                    // obtain GDS offset in the file for this record
                    GdsOffset = raf.getFilePointer();

                    // Section 3
                    gds = new Grib2GridDefinitionSection(raf, getProductsOnly);
                    //System.out.println( "GDS length=" + gds.getLength() );

                }  // end processGDS

                // obtain PDS offset in the file for this record
                PdsOffset = raf.getFilePointer();

                pds = new Grib2ProductDefinitionSection(raf);  // Section 4

                drs = new Grib2DataRepresentationSection(raf);  // Section 5
// code to test missing value information              
//                if( mvm == -1 ) {
//                  mvm = drs.getMissingValueManagement();
//                  pmv =  drs.getPrimaryMissingValue();
//                  smv = drs.getSecondaryMissingValue();
//                  System.out.println( mvm +" "+ pmv +" "+ smv );
//                } else if( mvm != drs.getMissingValueManagement() || pmv !=  drs.getPrimaryMissingValue()
//                    || smv != drs.getSecondaryMissingValue()) {
//                  mvm = drs.getMissingValueManagement();
//                  pmv =  drs.getPrimaryMissingValue();
//                  smv = drs.getSecondaryMissingValue();
//                  System.out.println( "Change "+ mvm +" "+ pmv +" "+ smv );
//                }

                bms = new Grib2BitMapSection( false, raf, gds);         // Section 6

                //ds =  new Grib2DataSection( getData, raf, gds, drs, bms ); //Section 7
                ds = new Grib2DataSection(false, raf, gds, drs, bms);  //Section 7

            } catch( Exception e ) {
                //System.out.println( "Caught Exception scannning record" );
                e.printStackTrace();
                //startAtHeader = true;  // otherwise skip to GDS
                //processGDS    = true;
                //raf.seek( EOR );
                //continue;
                // tried above but ended with outOfMemory Error
                return true;
            }


            // assume scan ok
            if (getProductsOnly) {
                Grib2Product gp = new Grib2Product(header, is, id, getGDSkey(gds), pds, GdsOffset, PdsOffset);
                  //getGDSkey(gds), gds.getGdsKey(), pds, GdsOffset, PdsOffset);
                products.add(gp);
            } else {
                Grib2Record gr = new Grib2Record(header, is, id, gds, pds, drs, GdsOffset, PdsOffset);
                records.add(gr);
            }
            if (oneRecord) {
                return true;
            }

            // early return because ending "7777" missing
            if (raf.getFilePointer() > raf.length()) {
                raf.seek(0);
                return true;
            }

            // EndSection processing section 8
            int ending = GribNumbers.int4(raf);
            //System.out.println( "ending = " + ending );
            if (ending == 926365495) {  // record ending string 7777 as a number
                startAtHeader = true;
                processGDS    = true;
            } else {
                int section = raf.read();  // check if GDS or PDS section, 3 or 4
                //System.out.println( "section = " + section );
                //reset back to begining of section
                raf.seek(raf.getFilePointer() - 5);

                if (section == 3) {          // start processing at GDS 
                    startAtHeader = false;
                    processGDS    = true;

                } else if (section == 4) {   // start processing at PDS
                    startAtHeader = false;
                    processGDS    = false;

                } else {                     // error
                    Grib2EndSection es = new Grib2EndSection(raf);
                    if (es.getEndFound()) {  // ending found somewhere in file
                        startAtHeader = true;
                        processGDS    = true;
                    } else {
                        //System.err.println( "Grib2Input: possible file corruption");
                        return false;  // record not terminated with 7777
                    }
                }
            }
            //System.out.println( "raf.getFilePointer=" + raf.getFilePointer() );
            //System.out.println( "raf.length()=" + raf.length() );
        }  // end raf.getFilePointer() < raf.length()
        //System.out.println("GribInput: processed in " +
        //   (System.currentTimeMillis()- start) + " milliseconds");
        return true;

    }  // end scan

    /**
     * returns Grib file type, 1 or 2, or 0 not a Grib file.
     * @return GribFileType
     * @throws IOException  on data read
     * @throws NotSupportedException NotSupportedException
     */
    public final int getEdition() throws IOException, NotSupportedException {
        raf.seek(0);
        if (!raf.searchForward(matcher, 8000)) return 0; // must find "GRIB" in first 8k
        raf.skipBytes(4);
        //  Read Section 0 Indicator Section to get Edition number
        Grib2IndicatorSection is = new Grib2IndicatorSection(raf);  // section 0
        return is.getGribEdition();
    }

    /**
     * @param raf RandomAccessFile
     * @param stop don't go pass this point
     * @return true or false, header found
     * @throws IOException raf read
     */
    private boolean seekHeader(RandomAccessFile raf, long stop)
            throws IOException {
        // seek header
        StringBuffer hdr   = new StringBuffer();
        int          match = 0;
        while (raf.getFilePointer() < stop) {
            // code must be "G" "R" "I" "B"
            byte c = raf.readByte();
            hdr.append((char) c);
            //System.out.println( (char) c );
            if (c == 'G') {
                match = 1;
            } else if ((c == 'R') && (match == 1)) {
                match = 2;
            } else if ((c == 'I') && (match == 2)) {
                match = 3;
            } else if ((c == 'B') && (match == 3)) {
                match = 4;
                //System.out.println( "hdr=" + hdr.toString() );
                Matcher m = productID.matcher(hdr.toString());
                if (m.find()) {
                    header = m.group(1);
                } else {
                    //header = hdr.toString();
                    header = "GRIB2";
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
     * @param gds Grib2GridDefinitionSection
     * @return key  that the GDS was stored under
     */
    /*
    private String getGDSkey(Grib2GridDefinitionSection gds) {

        //String key = gds.getCheckSum();
        String key = Integer.toString(gds.getGdsVars().getGdsKey());
        if ( !gdsHM.containsKey(key)) {  // check if gds is already saved
            gdsHM.put(key, gds);
        }
        return key;
    }  // end getGDSkey
    */
     /**
     * Test for similar GDS keys. Since only lat1 and lon1 are used to make the keys,
     * a test for closeEnough is also done on lat1 and lon1.
     *
     * @param newgds Grib2GridDefinitionSection
     * @return key  that the GDS was stored under
     */
    private String getGDSkey(Grib2GridDefinitionSection newgds) {

        Grib2GDSVariables newgdsv = newgds.getGdsVars();
        String newkey = Integer.toString(newgdsv.getGdsKey());

        if( gdsHM.size() == 0 ) {
          gdsHM.put(newkey, newgds);
          return newkey;
        } else if( gdsHM.containsKey( newkey ) ) {
          return newkey;
        } else {
          float newlat1 = newgdsv.get80La1();
          float newlon1 = newgdsv.get80Lo1();
          java.util.Set<String> keys = gdsHM.keySet();
          for( String key : keys ) {
            Grib2GridDefinitionSection gds = gdsHM.get( key );
            Grib2GDSVariables gdsv = gds.getGdsVars();
            float lat1 = gdsv.get80La1();
            float lon1 = gdsv.get80Lo1();
            if( GribNumbers.closeEnough( newlat1, lat1) &&
                GribNumbers.closeEnough( newlon1, lon1 ) ) {
                return key;
            }
          }
        }
        // no match
        gdsHM.put(newkey, newgds);
        return newkey;
    }  // end getGDSkey

    /**
     * Get products of the GRIB file.
     *
     * @return products
     */
    public final List<Grib2Product> getProducts() {
        return products;
    }

    /**
     * Get records of the GRIB file.
     *
     * @return records
     */
    public final List<Grib2Record> getRecords() {
        return records;
    }

    /**
     * Get GDS's of the GRIB file.
     *
     * @return gdsHM
     */
    public final Map<String,Grib2GridDefinitionSection> getGDSs() {
        return gdsHM;
    }

}  // end Grib2Input


