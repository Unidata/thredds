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
package ucar.nc2.iosp.nids;

import ucar.ma2.*;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.*;
import ucar.nc2.iosp.nexrad2.NexradStationDB;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.*;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.projection.FlatEarth;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.io.bzip2.CBZip2InputStream;
import ucar.unidata.io.bzip2.BZip2ReadException;
import ucar.unidata.util.Parameter;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

/**
 * This class reads in an NEXRAD level III and TDWR file.
 * most file need to go through the header part to have enough info to
 * construct the netcdf structure of the radar file
 *
 * @author caron
 */

class Nidsheader{
    final private static boolean useStationDB = false; // use station db for loactions
    static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Nidsheader.class);

    final static int  NEXR_PID_READ = 100;
    final static int  DEF_NUM_ELEMS = 640;   /* default num of elements to send         */
    final static int  DEF_NUM_LINES = 480;   /* default num of lines to send            */
    final static int  NEXR_FILE_READ = -1;   /* # flag to read entire NIDS file         */
    final static int  NEXR_DIR_READ = 356 ;  /* just enough bytes for NIDS directory    */
    final static int  READ_BUFFER_SIZE = 1;   /* # of image lines to buffer on read      */
    final static int  ZLIB_BUF_LEN = 4000;   /* max size of an uncompressed ZLIB buffer */
    byte Z_DEFLATED  = 8;
    byte DEF_WBITS  = 15;
    final static int   Other = 0;
    final static int   Base_Reflect = 1;
    final static int   Velocity = 2;
    final static int   Comp_Reflect = 3;
    final static int   Layer_Reflect_Avg = 4;
    final static int   Layer_Reflect_Max = 5;
    final static int   Echo_Tops = 6;
    final static int   Vert_Liquid = 7;
    final static int   Precip_1 = 8;
    final static int   Precip_3 = 9;
    final static int   Precip_Accum = 10;
    final static int   Precip_Array = 11;
    final static int   BaseReflect248 = 12;
    final static int   StrmRelMeanVel = 13;
    final static int   VAD = 14;
    final static int   SPECTRUM = 15;
    final static int   DigitalHybridReflect = 16;
    final static int   DigitalStormTotalPrecip = 17;
    final static int   Reflect1 = 18;
    final static int   Velocity1 = 19;
    final static int   SPECTRUM1 = 20;
    final static int   BaseReflectivityDR = 21;
    final static int   BaseVelocityDV = 22;
    final static int   EnhancedEcho_Tops = 23;
    final static int   DigitalVert_Liquid = 24;
    final static int   DigitalDifferentialReflectivity = 30;
    final static int   DigitalCorrelationCoefficient = 31;
    final static int   DigitalDifferentialPhase = 32;
    final static int   HydrometeorClassification = 33;
    final static int   OneHourAccumulation = 36;
    final static int   DigitalAccumulationArray = 37;
    final static int   StormTotalAccumulation = 38;
    final static int   DigitalStormTotalAccumulation = 39;
    final static int   Accumulation3Hour  = 40;
    final static int   Accumulation24Hour  = 41;
    final static int   Digital1HourDifferenceAccumulation  = 42;
    final static int   DigitalTotalDifferenceAccumulation  = 43;
    final static int   DigitalInstantaneousPrecipitationRate = 44;
    final static int   HypridHydrometeorClassification = 45;

    // message header block

    short mcode = 0;
    short mdate = 0;
    int mtime = 0;
    int mlength = 0;
    short msource = 0;
    short mdestId = 0;
    short mNumOfBlock = 0;
    // production dessciption block
    short divider = 0;
    double latitude = 0.0;
    double lat_min = 0.0;
    double lat_max = 0.0;
    double longitude = 0.0;
    double lon_min = 0.0;
    double lon_max = 0.0;

    double height = 0;
    short pcode = 0;
    short opmode = 0;
    short volumnScanPattern = 0;
    short sequenceNumber = 0;
    short volumeScanNumber = 0;
    short volumeScanDate = 0;
    int volumeScanTime = 0;
    short productDate = 0;
    int productTime = 0;
    short p1 = 0;
    short p2 = 0;
    short elevationNumber = 0;
    short p3 = 0;
    short [] threshold = new short[16];
    short p4 = 0;
    short p5 = 0;
    short p6 = 0;
    short p7 = 0;
    short p8 = 0;
    short p9 = 0;
    short p10 = 0;
    short numberOfMaps = 0;
    int offsetToSymbologyBlock = 0;
    int offsetToGraphicBlock = 0;
    int offsetToTabularBlock = 0;
    int block_length = 0;
    short number_layers = 0;
    String stationId;
    String stationName = "XXX";
    private boolean noHeader;

    DateFormatter formatter = new DateFormatter();

    /**
     * check if this file is a nids / tdwr file
     * @param raf    input file
     * @return  true  if valid
     */
    public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf) {
        try
        {
            long t = raf.length();
            if(t == 0){
                throw new IOException("zero length file ");
            }
        }
        catch ( IOException e )
        {
            return( false );
        }

        try{
            int p = this.readWMO( raf );
            if( p == 0 ) return false; // not unidata radar mosiac gini file
        }
        catch ( IOException e )
        {
            return( false );
        }
        return true;
    }

    /**
     * read the header of input file and parsing the WMO part
     * @param raf    input file
     * @return        1 if checking passing
     * @throws IOException
     */
    int readWMO(ucar.unidata.io.RandomAccessFile raf ) throws IOException
    {
        int pos = 0;
        //long     actualSize = 0;
        raf.seek(pos);
        int readLen = 35;

        // Read in the contents of the NEXRAD Level III product head
        byte[] b = new byte[readLen];
        int rc = raf.read(b);
        if ( rc != readLen )
        {
            // out.println(" error reading nids product header");
            return 0;
        }

        // new check
        int iarr2_1 = bytesToInt(b[0], b[1], false);
        int iarr2_16 = bytesToInt(b[30], b[31], false);
        int iarr2_10 = bytesToInt(b[18], b[19], false);
        int iarr2_7 = bytesToInt(b[12], b[13], false);
        if ( ( iarr2_1 == iarr2_16 ) &&
             ( ( iarr2_1 >=   16  ) && ( iarr2_1 <= 299) ) &&
             ( iarr2_10  ==   -1 ) &&
             ( iarr2_7   <    10000 ) )  {
             noHeader = true;
             return 1;

        }
        //Get product message header into a string for processing

        String pib = new String(b, CDM.utf8Charset);
        if(  pib.indexOf("SDUS")!= -1){
            noHeader = false;
            return 1;
        } else if ( raf.getLocation().indexOf(".nids") != -1) {
            noHeader = true;
            return 1;
       // } else if(checkMsgHeader(raf) == 1) {
        //    noHeader = true;
       //     return 1;
        } else
            return 0;
    }

    /**
     * read the compressed data
     *
     * @param offset   offset of the compressed data
     * @param len      length of data to compress
     * @return         uncompressed byte array
     */
    public byte[] getUncompData(int offset, int len){
      if( len == 0 ) len = uncompdata.length - offset;
      byte[] data = new byte[len];
      System.arraycopy(uncompdata, offset, data, 0, len);
      return data;
    }
 //////////////////////////////////////////////////////////////////////////////////

    ucar.unidata.io.RandomAccessFile raf;
    private ucar.nc2.NetcdfFile ncfile;
    //private PrintStream out = System.out;
    //private Vinfo myInfo;
    private String   cmemo, ctilt, ctitle, cunit, cname;
    public void setProperty( String name, String value) { }

    private   int numX ;
    private   int numX0;
    private   int numY ;
    private   int numY0;
    private   boolean isR = false;
    private byte[] uncompdata = null;

    /**
     * read and parse the header of the nids/tdwr file
     * @param raf       input file
     * @param ncfile    output file
     * @throws IOException
     */

    void read(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile ) throws IOException {

        int      hedsiz;                  /* NEXRAD header size            */
        int      rc;                      /* function return status        */
        int      hoff = 0;
        int      type;
        int      zlibed;
        boolean  isZ = false;
        int      encrypt;
        long     actualSize ;
        int      readLen ;
        readWMO( raf );
        this.ncfile = ncfile;
        actualSize = raf.length();
        int pos = 0;
        raf.seek(pos);

        // Read in the whole contents of the NEXRAD Level III product since
        // some product require to go through the whole file to build the  struct of file.

        readLen = (int)actualSize;

        byte[] b = new byte[readLen];
        rc = raf.read(b);
        if ( rc != readLen )
        {
            log.warn(" error reading nids product header "+raf.getLocation());
        }

        if( !noHeader ) {
        //Get product message header into a string for processing
            String pib = new String(b, 0, 100, CDM.utf8Charset);
            type = 0;
            pos = pib.indexOf ( "\r\r\n" );
            while ( pos != -1 ) {
                hoff =  pos + 3;
                type++;
                pos = pib.indexOf ( "\r\r\n" , pos+1);
            }
            raf.seek(hoff);

            // Test the next two bytes to see if the image portion looks like
            // it is zlib-compressed.
            byte[] b2 = new byte[2];
            // byte[] b4 = new byte[4];
            System.arraycopy(b, hoff, b2, 0, 2);

            zlibed = isZlibHed( b2 );
            if ( zlibed == 0){
                encrypt = IsEncrypt( b2 );
                if(encrypt == 1){
                    log.error( "error reading encryted product "+raf.getLocation());
                    throw new IOException("unable to handle the product with encrypt code " + encrypt);
                }
            }
           // process product description for station ID
            byte[] b3 = new byte[3];

            switch ( type ) {
              case 0:
                log.warn( "ReadNexrInfo:: Unable to seek to ID "+raf.getLocation());
                break;
              case 1:
              case 2:
              case 3:
              case 4:
                System.arraycopy(b, hoff - 6, b3, 0, 3);
                stationId  = new String(b3, CDM.utf8Charset);
                try {
                  NexradStationDB.init(); // make sure database is initialized
                  NexradStationDB.Station station = NexradStationDB.get("K"+stationId);
                  if (station != null) {
                    stationName = station.name;
                  }
                } catch (IOException ioe) {
                  log.error("NexradStationDB.init "+raf.getLocation(), ioe);
                }
                break;

              default:
                break;
            }

        if ( zlibed == 1 ) {
              isZ = true;
              uncompdata = GetZlibedNexr( b, readLen,  hoff );
              //uncompdata = Nidsiosp.readCompData(hoff, 160) ;
              if ( uncompdata == null ) {
                log.warn( "ReadNexrInfo: error uncompressing image " +raf.getLocation());
                uncompdata = new byte[b.length - hoff];
                System.arraycopy(b, hoff, uncompdata, 0, b.length - hoff);
              }
        }
        else {
             uncompdata = new byte[b.length-hoff];
             System.arraycopy(b, hoff, uncompdata, 0, b.length- hoff);
        }
    } else {
        uncompdata = new byte[b.length];
        System.arraycopy(b, 0, uncompdata, 0, b.length);
        // stationId  = "YYY";
    }
    byte[] b2 = new byte[2];
    ByteBuffer bos = ByteBuffer.wrap(uncompdata);
    rc = read_msghead( bos, 0 );
    hedsiz = 18;
    Pinfo pinfo = read_proddesc( bos, hedsiz );

    hedsiz += 102;



    // Set product-dependent information
    int prod_type = code_typelookup(pinfo.pcode);
    setProductInfo(prod_type, pinfo );

    //int windb = 0;
    int pcode1Number = 0;
    int pcode2Number = 0;
    int pcode8Number = 0;
    int pcode4Number = 0;
    int pcode5Number = 0;
    int pcode10Number = 0;
    int pcode6Number = 0;
    int pcode25Number = 0;
    int pcode12Number = 0;
    int pcode13Number = 0;
    int pcode14Number = 0;
    int pcode15Number = 0;
    int pcode16Number = 0;
    int pcode19Number = 0;
    int pcode20Number = 0;
    int pkcode1Doff[] = null;
    int pkcode2Doff[] = null;
    int pkcode8Doff[] = null;
    int pkcode1Size[] = null;
    int pkcode2Size[] = null;
    int pkcode8Size[] = null;
    int pkcode4Doff[] = null;
    int pkcode5Doff[] = null;
    int pkcode10Doff[] = null;
    int pkcode10Dlen[] = null;
    int pkcode6Doff[] = null;
    int pkcode6Dlen[] = null;
    int pkcode25Doff[] = null;
    int pkcode12Doff[] = null;
    int pkcode13Doff[] = null;
    int pkcode14Doff[] = null;
    int pkcode12Dlen[] = null;
    int pkcode13Dlen[] = null;
    int pkcode14Dlen[] = null;
    int pkcode15Dlen[] = null;
    int pkcode15Doff[] = null;
    int pkcode16Dlen[] = null;
    int pkcode16Doff[] = null;
    int pkcode19Dlen[] = null;
    int pkcode19Doff[] = null;
    int pkcode20Dlen[] = null;
    int pkcode20Doff[] = null;
    // Get product symbology header (needed to get image shape)
    ifloop: if ( pinfo.offsetToSymbologyBlock != 0 ) {

      // Symbology header
      if(pinfo.p8 == 1) {
          // TDWR data and the symbology is compressed
          int size = shortsToInt(pinfo.p9,  pinfo.p10, false);
          uncompdata = uncompressed(bos, hedsiz, size);
          bos = ByteBuffer.wrap(uncompdata);
      }

      Sinfo sinfo = read_dividlen( bos, hedsiz );
      if( rc == 0 || pinfo.divider != -1 )
      {
          log.warn( "error in product symbology header "+raf.getLocation());
      }

      if(sinfo.id != 1)
      {
          if(pinfo.pcode == 82) {
              read_SATab( bos, hedsiz );
          }
          break ifloop;

      }
      hedsiz += 10;

      // Symbology layer
      int klayer = pinfo.offsetToSymbologyBlock*2 + 10;
      for(int i=0; i<sinfo.nlayers ; i++ ){
          hedsiz = klayer;
          bos.position(hedsiz);
          short Divlen_divider = bos.getShort();
          hedsiz += 2;
          int Divlen_length = bos.getInt();
          hedsiz += 4;

          if ( Divlen_divider != -1 ) {
            log.warn( "error reading length divider "+raf.getLocation());
          }

          int icount = 0;
          int plen;

          while (icount < Divlen_length ) {
              int boff = klayer + icount + 6;
              bos.position(boff);
              bos.get(b2);
              int pkcode = getUInt(b2, 2);
              hedsiz += 2;
              boff += 2;
              switch(pkcode)
              {
                 case 18:  //  DPA
                 case 17: //   (pkcode == 0x11)   Digital Precipitation Array
                      hedsiz += 8;
                      plen = pcode_DPA( bos, boff, hoff, hedsiz, isZ, i, pkcode);
                      break;
                 case 10:    //     (pkcode == 0xA)
                      if(pkcode10Doff == null)  {
                          pkcode10Doff = new int[250];
                          pkcode10Dlen = new int[250];
                      }
                      plen =  bos.getShort();   // for unlinked Vector Packet the length of data block
                      pkcode10Doff[pcode10Number] = boff + 2;
                      pkcode10Dlen[pcode10Number] = ( plen - 2 ) / 8;
                      pcode10Number++;
                      //pcode_10n7( bos, boff, hoff, isZ, pkcode );
                      break;
                 case 1:
                      if(pkcode1Doff == null) {
                          pkcode1Doff = new int[250];
                          pkcode1Size = new int[250];
                      }
                      plen = bos.getShort();
                      pkcode1Doff[pcode1Number] = boff + 2;
                      pkcode1Size[pcode1Number] = plen - 4;
                      pcode1Number++;
                      break;
                 case 2:
                      if(pkcode2Doff == null) {
                          pkcode2Doff = new int[250];
                          pkcode2Size = new int[250];
                      }
                      plen = bos.getShort();
                      pkcode2Doff[pcode2Number] = boff + 2;
                      pkcode2Size[pcode2Number] = plen - 4;
                      pcode2Number++;
                      break;
                 case 8:       //text string
                      if(pkcode8Doff == null) {
                          pkcode8Doff = new int[550];
                          pkcode8Size = new int[550];
                      }
                      plen = bos.getShort();
                      pkcode8Doff[pcode8Number] = boff + 2;
                      pkcode8Size[pcode8Number] = plen - 6;
                      pcode8Number++;
                      break;
                 case 3:
                 case 11:
                 case 25:
                      if(pkcode25Doff == null) {
                          pkcode25Doff = new int[250];
                      }
                      plen = bos.getShort();
                      pkcode25Doff[pcode25Number] = boff + 2;
                      pcode25Number++;
                      break;
                 case 12:
                      if(pkcode12Doff == null) {
                          pkcode12Doff = new int[250];
                          pkcode12Dlen = new int[250];
                      }
                      plen = bos.getShort();
                      pkcode12Doff[pcode12Number] = boff + 2;
                      pkcode12Dlen[pcode12Number] = plen/4;
                      pcode12Number++;
                      break;
                 case 13:
                      if(pkcode13Doff == null) {
                          pkcode13Doff = new int[250];
                          pkcode13Dlen = new int[250];
                      }
                      plen = bos.getShort();
                      pkcode13Doff[pcode13Number] = boff + 2;
                      pkcode13Dlen[pcode13Number] = plen/4;
                      pcode13Number++;
                      break;
                 case 14:
                      if(pkcode14Doff == null) {
                          pkcode14Doff = new int[250];
                          pkcode14Dlen = new int[250];
                      }
                      plen = bos.getShort();
                      pkcode14Doff[pcode14Number] = boff + 2;
                      pkcode14Dlen[pcode14Number] = plen/4;
                      pcode14Number++;
                      break;
                 case 15:
                      if(pkcode15Doff == null) {
                          pkcode15Doff = new int[250];
                          pkcode15Dlen = new int[250];
                      }
                      plen = bos.getShort();
                      pkcode15Doff[pcode15Number] = boff + 2;
                      pkcode15Dlen[pcode15Number] = plen/6;
                      pcode15Number++;
                      break;
                 case 166:
                      if(pkcode16Doff == null) {
                          pkcode16Doff = new int[250];
                          pkcode16Dlen = new int[250];
                      }
                      plen = bos.getShort();
                      pkcode16Doff[pcode16Number] = boff + 2;
                      pkcode16Dlen[pcode16Number] = plen/4;
                      pcode16Number++;
                      break;
                  case 19:
                      if(pkcode19Doff == null) {
                          pkcode19Doff = new int[250];
                          pkcode19Dlen = new int[250];
                      }
                      plen = bos.getShort();
                      pkcode19Doff[pcode19Number] = boff + 2;
                      pkcode19Dlen[pcode19Number] = plen/10;
                      pcode19Number++;
                      break;
                  case 20:
                      if(pkcode20Doff == null) {
                          pkcode20Doff = new int[250];
                          pkcode20Dlen = new int[250];
                      }
                      plen = bos.getShort();
                      pkcode20Doff[pcode20Number] = boff + 2;
                      pkcode20Dlen[pcode20Number] = plen/8;
                      pcode20Number++;
                      break;
                 case 4:    // wind barb
                      if(pkcode4Doff == null) {
                          pkcode4Doff = new int[1000];
                      }
                      plen = bos.getShort();
                      pkcode4Doff[pcode4Number] = boff + 2;
                      pcode4Number++;
                      break;
                 case 5:  //   Vector Arrow Data
                      if(pkcode5Doff == null) {
                          pkcode5Doff = new int[1000];
                      }
                      plen = bos.getShort();
                      pkcode5Doff[pcode5Number] = boff + 2;
                      pcode5Number++;
                      break;
                 case 43:
                      plen = bos.getShort();
                      break;
                 case 23:
                 case 24:
                      plen = bos.getShort();
                      int poff = 2;
                      while (poff < plen){
                          int pcode = bos.getShort();
                          int len = bos.getShort();
                          switch (pcode)
                          {
                             case 2:
                                 if(pkcode2Doff == null) {
                                     pkcode2Doff = new int[250];
                                     pkcode2Size = new int[250];
                                 }
                                 pkcode2Doff[pcode2Number] = boff + poff + 4;
                                 pkcode2Size[pcode2Number] = len - 4;
                                 pcode2Number++;
                                 break;
                             case 6:
                                 if(pkcode6Doff == null) {
                                     pkcode6Doff = new int[250];
                                     pkcode6Dlen = new int[250];
                                 }
                                 pkcode6Doff[pcode6Number] = boff + poff + 4;
                                 pkcode6Dlen[pcode6Number] = (len - 6)/4;
                                 pcode6Number++;
                                 break;
                             case 25:
                                 if(pkcode25Doff == null) {
                                     pkcode25Doff = new int[250];
                                 }
                                 pkcode25Doff[pcode25Number] = boff + poff + 4;
                                 pcode25Number++;
                                 break;
                             default:
                                log.error( "error reading pcode= " + pcode+" "+raf.getLocation());
                                throw new IOException("error reading pcode, " +
                                        "unable to handle the packet with code "
                                        + pcode);
                          }
                          poff = poff + len + 4;
                          // Need to advance the file's position
                          bos.position(bos.position() + len);
                      }
                      break;
                  case 0x0802:
                      log.warn("Encountered unhandled packet code 0x0802 (contour color) -- reading past.");
                      Divlen_divider = bos.getShort(); // Color marker
                      if (Divlen_divider != 0x0002) {
                          log.warn("Missing color marker!");
                      }
                      plen = 2;
                      break;
                  case 0x0E03:
                      log.warn("Encountered unhandled packet code 0x0E03 (linked contours) -- reading past.");
                      Divlen_divider = bos.getShort(); // Start marker
                      if (Divlen_divider != 0x8000) {
                          log.warn("Missing start marker!");
                      }
                      // Read past start x, y for now
                      bos.getShort();
                      bos.getShort();
                      plen = 6 + bos.getShort();
                      break;

                 default:
                      if ( pkcode == 0xAF1F  || pkcode == 16) {              /* radial image                  */
                          hedsiz += pcode_radial( bos, hoff, hedsiz, isZ, uncompdata, pinfo.threshold) ;
                          //myInfo = new Vinfo (cname, numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ);
                          plen = Divlen_length;
                          break;
                      }
                      else if ( pkcode == 28) {              /* radial image                  */
                          hedsiz += pcode_generic( bos, hoff, hedsiz, isZ, uncompdata, pinfo.threshold) ;
                          //myInfo = new Vinfo (cname, numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ);
                          plen = Divlen_length;
                          break;
                      }
                      else if (pkcode == 0xBA0F ||pkcode == 0xBA07 )
                      {      /* raster image                  */
                          hedsiz += pcode_raster( bos, (short)pkcode, hoff, hedsiz, isZ, uncompdata);
                          //myInfo = new Vinfo (cname, numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ);
                          plen = Divlen_length;
                          break;
                      }
                      else
                      {
                          log.error( "error reading pkcode equals " + pkcode+" "+raf.getLocation());
                          throw new IOException("error reading pkcode, unable to handle the product with code " + pkcode);
                      }

                  // size and beginning data position in file

              } //end of switch
              icount = icount + plen + 4;
          }

          klayer = klayer + Divlen_length + 6;
     }

      //int curDoff = hedsiz;

      if(pkcode8Doff != null){
          pcode_128( pkcode8Doff, pkcode8Size, 8, hoff, pcode8Number, "textStruct_code8", "",isZ);
      }
      if (pkcode1Doff != null){
          pcode_128( pkcode1Doff, pkcode1Size, 1, hoff, pcode1Number, "textStruct_code1", "", isZ);
      }
      if (pkcode2Doff != null){
          pcode_128( pkcode2Doff, pkcode2Size, 2, hoff, pcode2Number, "textStruct_code2", "", isZ);
      }
      if (pkcode10Doff != null){
          pcode_10n9( pkcode10Doff, pkcode10Dlen, hoff, pcode10Number, isZ);
      }
      if (pkcode4Doff != null){
          pcode_4( pkcode4Doff, hoff, pcode4Number, isZ);
      }
      if (pkcode5Doff != null){
          pcode_5( pkcode5Doff, hoff, pcode5Number, isZ);
      }
      if (pkcode6Doff != null){
          pcode_6n7( pkcode6Doff, pkcode6Dlen, hoff, pcode6Number, isZ, "linkedVector", 6);
      }
      if (pkcode25Doff != null){
          pcode_25( pkcode25Doff, hoff, pcode25Number, isZ);
      }
      if (pkcode12Doff != null){
          pcode_12n13n14( pkcode12Doff, pkcode12Dlen, hoff, pcode12Number, isZ, "TVS", 12);
      }
      if (pkcode13Doff != null){
          pcode_12n13n14( pkcode13Doff, pkcode13Dlen, hoff, pcode13Number, isZ, "hailPositive", 13);
      }
      if (pkcode14Doff != null){
          pcode_12n13n14( pkcode14Doff, pkcode14Dlen, hoff, pcode14Number, isZ, "hailProbable", 14);
      }
      if (pkcode19Doff != null){
          pcode_12n13n14( pkcode19Doff, pkcode19Dlen, hoff, pcode19Number, isZ, "hailIndex", 19);
      }
      if (pkcode20Doff != null){
          pcode_12n13n14( pkcode20Doff, pkcode20Dlen, hoff, pcode20Number, isZ, "mesocyclone", 20);
      }
    } else {
      log.debug ( "GetNexrDirs:: no product symbology block found (no image data) " +raf.getLocation());

    }

    if ( pinfo.offsetToTabularBlock != 0 ) {
         int tlayer = pinfo.offsetToTabularBlock*2;
         bos.position(tlayer);
         if( bos.hasRemaining()) {

             short tab_divider = bos.getShort();
             if ( tab_divider != -1) {
                 log.error ( "Block divider not found "+raf.getLocation());
                 throw new IOException("error reading graphic alphanumeric block" );
             }
             short tab_bid = bos.getShort();
             int tblen = bos.getInt();

             bos.position(tlayer+116);
             int inc = bos.getInt();
             bos.position(tlayer+128); // skip the second header and prod description

             tab_divider = bos.getShort();
             if ( tab_divider != -1) {
                 log.error ( "tab divider not found "+raf.getLocation());
                 throw new IOException("error reading graphic alphanumeric block" );
             }
             int npage = bos.getShort();

             int ppos =  bos.position();

             ArrayList dims =  new ArrayList();
             Dimension tbDim = new Dimension("pageNumber", npage);
             ncfile.addDimension( null, tbDim);
             dims.add( tbDim);
             Variable ppage = new Variable(ncfile, null, null, "TabMessagePage");
             ppage.setDimensions(dims);
             ppage.setDataType(DataType.STRING);
             ppage.addAttribute( new Attribute(CDM.LONG_NAME, "Graphic Product Message"));
             ncfile.addVariable(null, ppage);
             ppage.setSPobject( new Vinfo (npage, 0, tblen, 0, hoff, ppos, isR, isZ, null, null, tab_bid, 0));
         }

     }

     if ( pinfo.offsetToGraphicBlock != 0 ) {
         int gpkcode1Doff[] = null;
         int gpkcode2Doff[] = null;
         int gpkcode10Doff[] = null;
         int gpkcode10Dlen[] = null;
         int gpkcode8Doff[] = null;
         int gpkcode1Size[] = null;
         int gpkcode2Size[] = null;
         int gpkcode8Size[] = null;
         int gpcode1Number = 0;
         int gpcode10Number = 0;
         int gpcode8Number = 0;
         int gpcode2Number = 0;
         int tlayer = pinfo.offsetToGraphicBlock*2;
         bos.position(tlayer);
         short graphic_divider = bos.getShort();
         short graphic_bid = bos.getShort();
         if(graphic_divider!=-1 || graphic_bid != 2) {
               log.error( "error reading graphic alphanumeric block "+raf.getLocation());
               throw new IOException("error reading graphic alphanumeric block" );
         }
         int blen = bos.getInt();
         int clen = 0;
         int npage = bos.getShort();
         int lpage ;
         int ipage = 0;
         int plen ;

         while ( (clen < blen) && (ipage < npage) ) {
          //  bos.position(ppos);
            int ppos = bos.position();
            ipage =  bos.getShort();
            lpage =  bos.getShort();
            int icnt = 0;
            ppos = ppos + 4;
            while (icnt < lpage)
            {
                bos.position(ppos+icnt);
                int pkcode = bos.getShort();
                if(pkcode == 8 ) {
                   if(gpkcode8Doff == null) {
                          gpkcode8Doff = new int[550];
                          gpkcode8Size = new int[550];
                   }
                   plen = bos.getShort();
                   gpkcode8Doff[gpcode8Number] = ppos + 4 + icnt;
                   gpkcode8Size[gpcode8Number] = plen - 6;
                   icnt += plen + 4;
                   gpcode8Number++;
                }
                else if ( pkcode == 1 ) {
                   if(gpkcode1Doff == null) {
                          gpkcode1Doff = new int[550];
                          gpkcode1Size = new int[550];
                   }
                   plen = bos.getShort();
                   gpkcode1Doff[gpcode1Number] = ppos + 4 + icnt;
                   gpkcode1Size[gpcode1Number] = plen - 4;
                   icnt += plen + 4;
                   gpcode1Number++;
                }
                else if ( pkcode == 10 ) {
                   if(gpkcode10Doff == null)  {
                          gpkcode10Doff = new int[250];
                          gpkcode10Dlen = new int[250];
                   }
                   plen =  bos.getShort();   // for unlinked Vector Packet the length of data block

                   gpkcode10Doff[gpcode10Number] = ppos + 4+ icnt;
                   gpkcode10Dlen[gpcode10Number] = ( plen - 2 ) / 8;
                   icnt += plen + 4;
                   gpcode10Number++;
                }  else {
                   plen = bos.getShort();
                   icnt += plen + 4;
                }

              //  else {
              //      out.println( "error reading pkcode equals " + pkcode);
              //      throw new IOException("error reading pkcode in graphic alpha num block " + pkcode);
              //  }

            }
            ppos = ppos + lpage + 4;
            clen = clen + lpage + 4;
         }

         if(gpkcode8Doff != null){
             pcode_128( gpkcode8Doff, gpkcode8Size, 8, hoff, gpcode8Number, "textStruct_code8g", "g", isZ);
         }
         if(gpkcode2Doff != null){
             pcode_128( gpkcode2Doff, gpkcode2Size, 2, hoff, gpcode8Number, "textStruct_code2g", "g", isZ);
         }
         if(gpkcode1Doff != null){
             pcode_128( gpkcode1Doff, gpkcode1Size, 1, hoff, gpcode1Number, "textStruct_code1g", "g", isZ);
         }
         if (gpkcode10Doff != null){
             pcode_10n9( gpkcode10Doff, gpkcode10Dlen, hoff, gpcode10Number, isZ);
         }
         /*
         int ppos = bos.position();
         ArrayList dims =  new ArrayList();
         Dimension tbDim = new Dimension("pageNumber", npage, true);
         ncfile.addDimension( null, tbDim);
         dims.add( tbDim);
         Variable ppage = new Variable(ncfile, null, null, "GraphicMessagePage");
         ppage.setDimensions(dims);
         ppage.setDataType(DataType.STRING);
         ppage.addAttribute( new Attribute(CDM.LONG_NAME, "Graphic Product Message"));
         ncfile.addVariable(null, ppage);
         ppage.setSPobject( new Vinfo (npage, 0, tblen, 0, hoff, ppos, isR, isZ, null, null, graphic_bid));
           */
     }
    // finish
    ncfile.finish();
    }

    /**
    *  construct a dataset for special graphic symbol packet with code 12, 13, and 14
    *
    *
    * @param pos, dlen, hoff, len, isZ, structName, code
    *
    * @return  1 if successful
    */
    int pcode_12n13n14( int[] pos, int[] dlen, int hoff, int len, boolean isZ, String structName, int code )
    {
       //int vlen = len;

        int vlen = 0;
        for(int i=0; i<len; i++ ){
           vlen = vlen + dlen[i];
        }

        ArrayList dims =  new ArrayList();
        Dimension sDim = new Dimension("graphicSymbolSize", vlen);
        ncfile.addDimension( null, sDim);
        dims.add( sDim);

        Structure dist = new Structure(ncfile, null, null, structName);
        dist.setDimensions(dims);
        ncfile.addVariable(null, dist);
        dist.addAttribute( new Attribute(CDM.LONG_NAME, "special graphic symbol for code "+code));


        Variable i0 = new Variable(ncfile, null, dist, "x_start");
        i0.setDimensions((String)null);
        i0.setDataType(DataType.FLOAT);
        i0.addAttribute( new Attribute(CDM.UNITS, "KM"));
        dist.addMemberVariable(i0);
        Variable j0 = new Variable(ncfile, null, dist, "y_start");
        j0.setDimensions((String)null);
        j0.setDataType(DataType.FLOAT);
        j0.addAttribute( new Attribute(CDM.UNITS, "KM"));
        dist.addMemberVariable(j0);


       int[] pos1 = new int[len];
       int[] dlen1 = new int[len];
       System.arraycopy(dlen, 0, dlen1, 0, len);
       System.arraycopy(pos, 0, pos1, 0, len);
       dist.setSPobject( new Vinfo ( 0, 0, 0, 0, hoff, 0, isR, isZ, pos1, dlen1, code, 0));
       return 1;
     }

    /**
    *  construct a dataset for special symbol packet with code 25
    *
    *
    * @param pos, size, code, hoff, len, structName, isZ
    *
    * @return  1 if successful
    */

   int pcode_25( int[] pos, int hoff, int len, boolean isZ )
   {

      ArrayList dims =  new ArrayList();

      Dimension sDim = new Dimension("circleSize", len);
      ncfile.addDimension( null, sDim);
      dims.add( sDim);

      Structure dist = new Structure(ncfile, null, null, "circleStruct");
      dist.setDimensions(dims);
      ncfile.addVariable(null, dist);
      dist.addAttribute( new Attribute(CDM.LONG_NAME, "Circle Packet"));


      Variable ii0 = new Variable(ncfile, null, dist, "x_center");
      ii0.setDimensions((String)null);
      ii0.setDataType(DataType.SHORT);
      dist.addMemberVariable(ii0);
      Variable ii1 = new Variable(ncfile, null, dist, "y_center");
      ii1.setDimensions((String)null);
      ii1.setDataType(DataType.SHORT);
      dist.addMemberVariable(ii1);
      Variable jj0 = new Variable(ncfile, null, dist, "radius");
      jj0.setDimensions((String)null);
      jj0.setDataType(DataType.SHORT);
      dist.addMemberVariable(jj0);

      int[] pos1 = new int[len];
      System.arraycopy(pos, 0, pos1, 0, len);
      dist.setSPobject( new Vinfo (0, 0, 0, 0, hoff, 0, isR, isZ, pos1, null, 25, 0));


      return 1;
   }
    /**
    *  construct a dataset for linked and unlinked vector packet with code 6 and 7
    *
    *
    * @param pos, size, code, hoff, len, structName, isZ
    *
    * @return  1 if successful
    */

   int pcode_6n7( int[] pos, int[] dlen, int hoff, int len, boolean isZ, String vname, int code )
   {

      ArrayList dims =  new ArrayList();

      int vlen = 0;
      for(int i=0; i<len; i++ ){
           vlen = vlen + dlen[i];
      }

      Dimension sDim = new Dimension(vname+"Size", vlen);
      ncfile.addDimension( null, sDim);
      dims.add( sDim);

      Structure dist = new Structure(ncfile, null, null, vname +"Struct");
      dist.setDimensions(dims);
      ncfile.addVariable(null, dist);
      dist.addAttribute( new Attribute(CDM.LONG_NAME, vname+" Packet"));


      Variable ii0 = new Variable(ncfile, null, dist, "x_start");
      ii0.setDimensions((String)null);
      ii0.setDataType(DataType.SHORT);
      dist.addMemberVariable(ii0);
      Variable ii1 = new Variable(ncfile, null, dist, "y_start");
      ii1.setDimensions((String)null);
      ii1.setDataType(DataType.SHORT);
      dist.addMemberVariable(ii1);
      Variable jj0 = new Variable(ncfile, null, dist, "x_end");
      jj0.setDimensions((String)null);
      jj0.setDataType(DataType.SHORT);
      dist.addMemberVariable(jj0);
      Variable jj1 = new Variable(ncfile, null, dist, "y_end");
      jj1.setDimensions((String)null);
      jj1.setDataType(DataType.SHORT);
      dist.addMemberVariable(jj1);

      int[] pos1 = new int[len];
      int[] dlen1 = new int[len];
      System.arraycopy(pos, 0, pos1, 0, len);
      System.arraycopy(dlen, 0, dlen1, 0, len);
      dist.setSPobject( new Vinfo ( 0, 0, 0, 0, hoff, 0, isR, isZ, pos1, dlen1, code, 0));

      return 1;
   }

    /**
    *  construct a dataset for wind barb data packet with code 4
    *
    *
    * @param pos, size, code, hoff, len, structName, isZ
    *
    * @return  1 if successful
    */

    int pcode_4( int[] pos, int hoff, int len, boolean isZ )
    {
      ArrayList dims =  new ArrayList();
      //int vlen =len;

      Dimension sDim = new Dimension("windBarbSize", len);
      ncfile.addDimension( null, sDim);
      dims.add( sDim);

      Structure dist = new Structure(ncfile, null, null, cname);
      dist.setDimensions(dims);
      ncfile.addVariable(null, dist);
      dist.addAttribute( new Attribute(CDM.LONG_NAME, "Wind Barb Data"));

      Variable value = new Variable(ncfile, null, dist, "value");
      value.setDimensions((String)null);
      value.setDataType(DataType.SHORT);
      value.addAttribute( new Attribute(CDM.UNITS, "RMS"));
      dist.addMemberVariable(value);
      Variable i0 = new Variable(ncfile, null, dist, "x_start");
      i0.setDimensions((String)null);
      i0.setDataType(DataType.SHORT);
      i0.addAttribute( new Attribute(CDM.UNITS, "KM"));
      dist.addMemberVariable(i0);
      Variable j0 = new Variable(ncfile, null, dist, "y_start");
      j0.setDimensions((String)null);
      j0.setDataType(DataType.SHORT);
      j0.addAttribute( new Attribute(CDM.UNITS, "KM"));
      dist.addMemberVariable(j0);
      Variable direct = new Variable(ncfile, null, dist, "direction");
      direct.setDimensions((String)null);
      direct.setDataType(DataType.SHORT);
      direct.addAttribute( new Attribute(CDM.UNITS, "degree"));
      dist.addMemberVariable(direct);
      Variable speed = new Variable(ncfile, null, dist, "speed");
      speed.setDimensions((String)null);
      speed.setDataType(DataType.SHORT);
      speed.addAttribute( new Attribute(CDM.UNITS, "knots"));
      dist.addMemberVariable(speed);

      int[] pos1 = new int[len];
      System.arraycopy(pos, 0, pos1, 0, len);
      dist.setSPobject( new Vinfo (0, 0, 0, 0, hoff, 0, isR, isZ, pos1, null, 4, 0));

      return 1;
    }

    /** check level III file header
     *
     * @param raf      input file
     * @return         1 if success, and 0 if fail
     * @throws IOException
     */
    int checkMsgHeader(ucar.unidata.io.RandomAccessFile raf ) throws IOException
    {
        int      rc;
        long     actualSize ;
        int      readLen ;

        actualSize = raf.length();
        int pos = 0;
        raf.seek(pos);

        // Read in the whole contents of the NEXRAD Level III product since
        // some product require to go through the whole file to build the  struct of file.

        readLen = (int)actualSize;

        byte[] b = new byte[readLen];
        rc = raf.read(b);
        if ( rc != readLen )
        {
            log.warn(" error reading nids product header "+raf.getLocation());
        }
        ByteBuffer bos = ByteBuffer.wrap(b);
        return read_msghead( bos, 0 );

    }

    /**
    *  construct a dataset for vector arrow data packet with code 5
    *
    *
    * @param pos, size, code, hoff, len, isZ
    *
    * @return  1 if successful
    */

    int pcode_5( int[] pos, int hoff, int len, boolean isZ )
    {
      ArrayList dims =  new ArrayList();
      //int vlen =len;

      Dimension sDim = new Dimension("windBarbSize", len);
      ncfile.addDimension( null, sDim);
      dims.add( sDim);

      Structure dist = new Structure(ncfile, null, null, "vectorArrow");
      dist.setDimensions(dims);
      ncfile.addVariable(null, dist);
      dist.addAttribute( new Attribute(CDM.LONG_NAME, "Vector Arrow Data"));

      Variable i0 = new Variable(ncfile, null, dist, "x_start");
      i0.setDimensions((String)null);
      i0.setDataType(DataType.SHORT);
      i0.addAttribute( new Attribute(CDM.UNITS, "KM"));
      dist.addMemberVariable(i0);
      Variable j0 = new Variable(ncfile, null, dist, "y_start");
      j0.setDimensions((String)null);
      j0.setDataType(DataType.SHORT);
      j0.addAttribute( new Attribute(CDM.UNITS, "KM"));
      dist.addMemberVariable(j0);
      Variable direct = new Variable(ncfile, null, dist, "direction");
      direct.setDimensions((String)null);
      direct.setDataType(DataType.SHORT);
      direct.addAttribute( new Attribute(CDM.UNITS, "degree"));
      dist.addMemberVariable(direct);
      Variable speed = new Variable(ncfile, null, dist, "arrowLength");
      speed.setDimensions((String)null);
      speed.setDataType(DataType.SHORT);
      speed.addAttribute( new Attribute(CDM.UNITS, "pixels"));
      dist.addMemberVariable(speed);
      Variable speed1 = new Variable(ncfile, null, dist, "arrowHeadLength");
      speed1.setDimensions((String)null);
      speed1.setDataType(DataType.SHORT);
      speed1.addAttribute( new Attribute(CDM.UNITS, "pixels"));
      dist.addMemberVariable(speed1);

      int[] pos1 = new int[len];
      System.arraycopy(pos, 0, pos1, 0, len);
      dist.setSPobject( new Vinfo (0, 0, 0, 0, hoff, 0, isR, isZ, pos1, null, 4, 0));

      return 1;
    }

     /**
     *  construct a dataset for text and special symbol packets with code 1, 2, and 8
     *
     *
     * @param pos, size, code, hoff, len, structName, isZ
     *
     * @return  1 if successful
     */

     int pcode_128( int[] pos, int[] size, int code, int hoff, int len, String structName, String abbre, boolean isZ )
     {
        //int vlen = len;

        ArrayList dims =  new ArrayList();
        Dimension sDim = new Dimension("textStringSize"+ abbre + code, len);
        ncfile.addDimension( null, sDim);
        dims.add( sDim);

        Structure dist = new Structure(ncfile, null, null, structName + abbre);
        dist.setDimensions(dims);
        ncfile.addVariable(null, dist);
        dist.addAttribute( new Attribute(CDM.LONG_NAME, "text and special symbol for code "+code));

        if(code == 8){
            Variable strVal = new Variable(ncfile, null, dist, "strValue");
            strVal.setDimensions((String)null);
            strVal.setDataType(DataType.SHORT);
            strVal.addAttribute( new Attribute(CDM.UNITS, ""));
            dist.addMemberVariable(strVal);
        }
        Variable i0 = new Variable(ncfile, null, dist, "x_start");
        i0.setDimensions((String)null);
        i0.setDataType(DataType.SHORT);
        i0.addAttribute( new Attribute(CDM.UNITS, "KM"));
        dist.addMemberVariable(i0);
        Variable j0 = new Variable(ncfile, null, dist, "y_start");
        j0.setDimensions((String)null);
        j0.setDataType(DataType.SHORT);
        j0.addAttribute( new Attribute(CDM.UNITS, "KM"));
        dist.addMemberVariable(j0);
        Variable tstr = new Variable(ncfile, null, dist, "textString" );
        tstr.setDimensions((String)null);
        tstr.setDataType(DataType.STRING);
        tstr.addAttribute( new Attribute(CDM.UNITS, ""));
        dist.addMemberVariable(tstr);

        int[] pos1 = new int[len];
        System.arraycopy(pos, 0, pos1, 0, len);
        dist.setSPobject( new Vinfo ( 0, 0, 0, 0, hoff, 0, isR, isZ, pos1, size, code, 0));
        return 1;
     }

     /**
     *  construct a dataset for linked vector packet, and unlinked vector packet
     *
     *
     * @param pos, dlen, hoff, len, isZ
     *
     * @return  1 if successful
     */
     int pcode_10n9( int[] pos, int[] dlen, int hoff, int len, boolean isZ )
     {

        ArrayList dims =  new ArrayList();
        Variable v  ;

        int vlen = 0;
        for(int i=0; i<len; i++ ){
           vlen = vlen + dlen[i];
        }

        Dimension sDim = new Dimension("unlinkedVectorSize", vlen);
        ncfile.addDimension( null, sDim);
        dims.add( sDim);

        Structure dist = new Structure(ncfile, null, null, "unlinkedVectorStruct");
        dist.setDimensions(dims);
        ncfile.addVariable(null, dist);
        dist.addAttribute( new Attribute(CDM.LONG_NAME, "Unlinked Vector Packet"));

        v = new Variable(ncfile, null, null, "iValue");
        v.setDataType(DataType.SHORT);
        v.setDimensions((String)null);
        dist.addMemberVariable(v);

        Variable ii0 = new Variable(ncfile, null, dist, "x_start");
        ii0.setDimensions((String)null);
        ii0.setDataType(DataType.SHORT);
        dist.addMemberVariable(ii0);
        Variable ii1 = new Variable(ncfile, null, dist, "y_start");
        ii1.setDimensions((String)null);
        ii1.setDataType(DataType.SHORT);
        dist.addMemberVariable(ii1);
        Variable jj0 = new Variable(ncfile, null, dist, "x_end");
        jj0.setDimensions((String)null);
        jj0.setDataType(DataType.SHORT);
        dist.addMemberVariable(jj0);
        Variable jj1 = new Variable(ncfile, null, dist, "y_end");
        jj1.setDimensions((String)null);
        jj1.setDataType(DataType.SHORT);
        dist.addMemberVariable(jj1);

        int[] pos1 = new int[len];
        int[] dlen1 = new int[len];
        System.arraycopy(pos, 0, pos1, 0, len);
        System.arraycopy(dlen, 0, dlen1, 0, len);
        dist.setSPobject( new Vinfo ( 0, 0, 0, 0, hoff, 0, isR, isZ, pos1, dlen1, 10, 0));
        return 1;
     }
     /**
     *  construct a dataset for NIDS digital precipitation array
     *
     *
     * @param bos, pos, hoff, hedsiz, isZ, slayer, code
     *
     * @return  soff -- not used
     */
     int pcode_DPA( ByteBuffer bos, int pos, int hoff, int hedsiz, boolean isZ, int slayer, int code)
     {
        byte[] b2 = new byte[2];
        int soff;
        ArrayList dims =  new ArrayList();
        bos.position(pos);
        bos.get(b2, 0, 2);  // reserved
        bos.get(b2, 0, 2);  // reserved

        bos.get(b2, 0, 2);
        short numBox = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short numRow = (short)getInt(b2, 2);
        soff = 8;

        numY0 = 0;
        numX0 = 0;
        numX = numBox;
        numY = numRow;
        if(slayer == 0){
            Dimension jDim = new Dimension("y", numY);
            Dimension iDim = new Dimension("x", numX);
            ncfile.addDimension( null, iDim);
            ncfile.addDimension( null, jDim);
             dims.add( jDim);
             dims.add( iDim);
            Variable v = new Variable(ncfile, null, null, cname+"_"+slayer);
            v.setDataType(DataType.SHORT);
            v.setDimensions(dims);
            ncfile.addVariable(null, v);
            v.addAttribute( new Attribute(CDM.LONG_NAME, ctitle+" at Symbology Layer "+ slayer));
            v.setSPobject( new Vinfo (numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ, null, null, code, 0));
            v.addAttribute( new Attribute(CDM.UNITS, cunit));
            v.addAttribute( new Attribute(CDM.MISSING_VALUE, 255));
        }
        //else  if(slayer == 1) {
          //  ncfile.addDimension( null, iDim);
          //  ncfile.addDimension( null, jDim);
        //}

        for (int row=0; row < numRow; row++) {

            int runLen = bos.getShort();

            byte[] rdata = new byte[runLen];
            bos.get(rdata, 0, runLen);
            if (runLen < 2) {
                return soff;
            } else {
                soff += runLen + 2;
            }
        }   //end of for loop

        if(slayer == 0){
            double ddx = code_reslookup( pcode );
            ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.GRID.toString()));
            // create coordinate variables
            Variable xaxis = new Variable( ncfile, null, null, "x");
            xaxis.setDataType( DataType.DOUBLE);
            xaxis.setDimensions("x");
            xaxis.addAttribute( new Attribute(CDM.LONG_NAME, "projection x coordinate"));
            xaxis.addAttribute( new Attribute(CDM.UNITS, "km"));
            xaxis.addAttribute( new Attribute(_Coordinate.AxisType, "GeoX"));
            double[] data1 = new double[numX];
            for (int i = 0; i < numX; i++)
              data1[i] = numX0 + i*ddx;
            Array dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] {numX}, data1);
            xaxis.setCachedData( dataA, false);
            ncfile.addVariable(null, xaxis);

            Variable yaxis = new Variable( ncfile, null, null, "y");
            yaxis.setDataType( DataType.DOUBLE);
            yaxis.setDimensions( "y");
            yaxis.addAttribute( new Attribute(CDM.LONG_NAME, "projection y coordinate"));
            yaxis.addAttribute( new Attribute(CDM.UNITS, "km"));
            yaxis.addAttribute( new Attribute(_Coordinate.AxisType, "GeoY"));
            data1 = new double[numY];
            for (int i = 0; i < numY; i++)
              data1[i] = numY0 + i*ddx;
            dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] {numY}, data1);
            yaxis.setCachedData( dataA, false);
            ncfile.addVariable(null, yaxis);

            ProjectionImpl projection = new FlatEarth(lat_min, lon_max);
            //ProjectionImpl projection = new LambertConformal(latitude, longitude, latitude, latitude);
            // coordinate transform variable
            Variable ct = new Variable( ncfile, null, null, projection.getClassName());
            ct.setDataType( DataType.CHAR);
            ct.setDimensions( "");
            List params = projection.getProjectionParameters();
            for (int i = 0; i < params.size(); i++) {
              Parameter p = (Parameter) params.get(i);
              ct.addAttribute( new Attribute(p));
            }
            ct.addAttribute( new Attribute(_Coordinate.TransformType, "Projection"));
            ct.addAttribute( new Attribute(_Coordinate.Axes, "x y"));
            // fake data
            dataA = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[] {});
            dataA.setChar(dataA.getIndex(), ' ');
            ct.setCachedData(dataA, false);

            ncfile.addVariable(null, ct);
        }
        return soff;
     }
     /**
     *  construct a raster dataset for NIDS raster products;
     *
     *
     * @param bos, pkcode, hoff, hedsiz, isZ, data
     *
     * @return  soff -- not used
     */
    int pcode_raster( ByteBuffer bos, short pkcode, int hoff, int hedsiz, boolean isZ, byte[] data )
    {
        byte[] b2 = new byte[2];
        int soff ;
        ArrayList dims =  new ArrayList();
        int iscale = 1;                         /* data scale                    */
        int ival;

        ival = convertShort2unsignedInt(threshold[0]);
        if ( (ival & ( 1 << 13 )) != 0 ) iscale = 20;
        if ( (ival & ( 1 << 12 )) != 0 ) iscale = 10;

        short[] rasp_code = new short[3];

        rasp_code[0] = pkcode;

        bos.get(b2, 0, 2);
        rasp_code[1] = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        rasp_code[2] = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
//        short rasp_i = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
//        short rasp_j = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short rasp_xscale = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
//        short rasp_xscalefract = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
//        short rasp_yscale = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
//        short rasp_yscalefract = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short num_rows = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
//        short packing = (short)getInt(b2, 2);
        soff= 20;
        hedsiz = hedsiz + soff;

        int nlevel = code_levelslookup( pcode );
        double ddx = code_reslookup( pcode );
        int [] levels = getLevels(nlevel, threshold);

        //prod_info_size = (int) (num_rows * scale);
        //out.println( "resp scale " + (int)rasp_xscale + " and " + (int)rasp_xscalefract+ " and " + (int)rasp_yscale+ " and " + (int)rasp_yscalefract );
        numY0 = 0; //rasp_j;
        numX0 = 0; //rasp_i;
        numX = num_rows;
        numY = num_rows;
        Dimension jDim = new Dimension("y", numY, true, false, false);
        Dimension iDim = new Dimension("x", numX, true, false, false);
        dims.add( jDim);
        dims.add( iDim);
        ncfile.addDimension( null, iDim);
        ncfile.addDimension( null, jDim);
        //ncfile.addAttribute(null, new Attribute("cdm_data_type", thredds.catalog.DataType.GRID.toString()));
        if(cname.startsWith("Precip")) {
            ncfile.addAttribute(null, new Attribute("isRadial", new Integer(3)));
            ddx = ddx * rasp_xscale;
        }
        ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.GRID.toString()));
        //Variable dist = new Variable(ncfile, null, null, "distance");
        //dist.setDataType(DataType.INT);
        //dist.setDimensions(dims);
        //ncfile.addVariable(null, dist);
        //dist.setSPobject( new Vinfo ( numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ, null, null, pkcode, 0));


        String coordinates = "x y time latitude longitude altitude";

        Variable v = new Variable(ncfile, null, null, cname+ "_RAW");
        v.setDataType(DataType.BYTE);
        v.setDimensions(dims);
        ncfile.addVariable(null, v);
        v.addAttribute( new Attribute(CDM.LONG_NAME, ctitle));
        v.addAttribute( new Attribute(CDM.UNITS, cunit));
        v.setSPobject( new Vinfo (numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ, null, null, pkcode, 0));
        v.addAttribute( new Attribute(_Coordinate.Axes, coordinates));

        if( cname.startsWith("VertLiquid")){
          addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                       cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }
        else if( cname.startsWith("EchoTop")){
          addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                       cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }
        else if( cname.startsWith("BaseReflectivityComp") || cname.startsWith("LayerCompReflect")){
          addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                       cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }
        else if( cname.startsWith("Precip")){
          addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                       cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }

        // create coordinate variables
        Variable xaxis = new Variable( ncfile, null, null, "x");
        xaxis.setDataType( DataType.DOUBLE);
        xaxis.setDimensions("x");
        xaxis.addAttribute( new Attribute(CDM.LONG_NAME, "projection x coordinate"));
        xaxis.addAttribute( new Attribute(CDM.UNITS, "km"));
        xaxis.addAttribute( new Attribute(_Coordinate.AxisType, "GeoX"));
        double[] data1 = new double[numX];
        for (int i = 0; i < numX; i++)
          data1[i] = numX0 + i*ddx;
        Array dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] {numX}, data1);
        xaxis.setCachedData( dataA, false);
        ncfile.addVariable(null, xaxis);

        Variable yaxis = new Variable( ncfile, null, null, "y");
        yaxis.setDataType( DataType.DOUBLE);
        yaxis.setDimensions( "y");
        yaxis.addAttribute( new Attribute(CDM.LONG_NAME, "projection y coordinate"));
        yaxis.addAttribute( new Attribute(CDM.UNITS, "km"));
        yaxis.addAttribute( new Attribute(_Coordinate.AxisType, "GeoY"));
        data1 = new double[numY];
        for (int i = 0; i < numY; i++)
          data1[i] = numY0 + i*ddx;
        dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] {numY}, data1);
        yaxis.setCachedData( dataA, false);
        ncfile.addVariable(null, yaxis);

        ProjectionImpl projection = new FlatEarth(lat_min, lon_max);
        //ProjectionImpl projection = new LambertConformal(latitude, longitude, latitude, latitude);
        // coordinate transform variable
        Variable ct = new Variable( ncfile, null, null, projection.getClassName());
        ct.setDataType( DataType.CHAR);
        ct.setDimensions( "");
        List params = projection.getProjectionParameters();
        for (int i = 0; i < params.size(); i++) {
          Parameter p = (Parameter) params.get(i);
          ct.addAttribute( new Attribute(p));
        }
        ct.addAttribute( new Attribute(_Coordinate.TransformType, "Projection"));
        ct.addAttribute( new Attribute(_Coordinate.Axes, "x y"));
        // fake data
        dataA = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[] {});
        dataA.setChar(dataA.getIndex(), ' ');
        ct.setCachedData(dataA, false);

        ncfile.addVariable(null, ct);
        return soff;

    }
     /**
     *  construct a radial dataset for NIDS radial products;
     *
     *
     * @param bos, hoff, hedsiz, isZ, data, threshold
     *
     * @return  soff -- not used
     */
    int pcode_radial( ByteBuffer  bos, int hoff, int hedsiz, boolean isZ, byte[] data, short[] threshold ) throws IOException
    {
        byte[] b2 = new byte[2];
        int soff;
        ArrayList dims =  new ArrayList();
        int iscale = 1;                         /* data scale                    */
        int ival;

        ival = convertShort2unsignedInt(threshold[0]);
        if ( (ival & ( 1 << 13 )) != 0 ) iscale = 20;
        if ( (ival & ( 1 << 12 )) != 0 ) iscale = 10;


        bos.get(b2, 0, 2);
        short first_bin = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short num_bin = (short)(getUInt(b2, 2));
        if(this.pcode == 94 || this.pcode == 99)
            num_bin = addBinSize(num_bin);
        bos.get(b2, 0, 2);
//        short radp_i = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
//        short radp_j = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short radp_scale = (short)getInt(b2, 2);
        if(this.pcode == 134 || this.pcode == 135)
             radp_scale = (short)(radp_scale * 1000);
        bos.get(b2, 0, 2);
        short num_radials = (short)getInt(b2, 2);
        soff = 12;
        hedsiz = hedsiz + soff;
        numY0 = 0;
        numY = num_radials;
        numX0 = first_bin;
        numX = num_bin;
        int nlevel = code_levelslookup( pcode );
        int [] levels;

        //prod_info_size = 2 * (int) (num_bin * scale + 0.5);
        //dimensions: radial, bin
        ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.RADIAL.toString()));
        Dimension radialDim = new Dimension("azimuth", num_radials);
        ncfile.addDimension( null, radialDim);

        Dimension binDim = new Dimension("gate", num_bin);
        ncfile.addDimension( null, binDim);
        dims.add( radialDim);
        dims.add( binDim);

        ArrayList dims1 =  new ArrayList();
        ArrayList dims2 =  new ArrayList();
        dims1.add(radialDim);
        dims2.add(binDim);
        // Variable aziVar = new Variable(ncfile, null, null, "azimuth");
        // aziVar.setDataType(DataType.FLOAT);
        // aziVar.setDimensions(dims1);
        // ncfile.addVariable(null, aziVar);
        // aziVar.addAttribute( new Attribute(CDM.LONG_NAME, "azimuth angle in degrees: 0 = true north, 90 = east"));
        // aziVar.addAttribute( new Attribute(CDM.UNITS, "degrees"));
        // aziVar.setSPobject( new Vinfo (numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ, null, null, 0));

        // dims1 =  new ArrayList();
        // dims1.add(binDim);
        //Variable gateV = new Variable(ncfile, null, null, "gate1");
        //gateV.setDataType(DataType.FLOAT);
        //gateV.setDimensions(dims2);
        //ncfile.addVariable(null, gateV);
        //gateV.addAttribute( new Attribute(CDM.LONG_NAME, "radial distance to start of gate"));
        //gateV.addAttribute( new Attribute(CDM.UNITS, "m"));
        //gateV.setSPobject( new Vinfo (numX, numX0, numY, radp_scale, hoff, hedsiz, isR, isZ, null, null, 0, 0));
        isR = true;

        // add elevation coordinate variable
        String vName = "elevation";
        String lName = "elevation angle in degres: 0 = parallel to pedestal base, 90 = perpendicular";
        Attribute att = new Attribute(_Coordinate.AxisType, AxisType.RadialElevation.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees",hoff, hedsiz, isZ, p3);

        // add azimuth coordinate variable
        vName = "azimuth";
        lName = "azimuth angle in degrees: 0 = true north, 90 = east";
        att = new Attribute(_Coordinate.AxisType, AxisType.RadialAzimuth.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees",hoff, hedsiz, isZ, 0);


        // add gate coordinate variable
        vName = "gate";
        lName = "Radial distance to the start of gate";
        att = new Attribute(_Coordinate.AxisType, AxisType.RadialDistance.toString());
        addParameter(vName, lName, ncfile, dims2, att, DataType.FLOAT, "meters",hoff, hedsiz, isZ, radp_scale);

        // add radial coordinate variable

        vName = "latitude";
        lName = "Latitude of the instrument";
        att = new Attribute(_Coordinate.AxisType, AxisType.Lat.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees",hoff, hedsiz, isZ, 0);

        vName = "longitude";
        lName = "Longitude of the instrument";
        att = new Attribute(_Coordinate.AxisType, AxisType.Lon.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees",hoff, hedsiz, isZ, 0);

        vName = "altitude";
        lName = "Altitude in meters (asl) of the instrument";
        att = new Attribute(_Coordinate.AxisType, AxisType.Height.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "meters",hoff, hedsiz, isZ, 0);

        vName = "rays_time";
        lName = "rays time";
        att = new Attribute(_Coordinate.AxisType, AxisType.Time.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.DOUBLE, "milliseconds since 1970-01-01 00:00 UTC"
                    ,hoff, hedsiz, isZ, 0);
        //add RAW, BRIT variables for all radial variable
        if(pcode == 182 || pcode == 99 ) {
            levels = getTDWRLevels(nlevel, threshold);
            iscale = 10;
        } else if (pcode == 186 || pcode == 94) {
            threshold[0] = -320;
            threshold[1] = 5;
            threshold[2] = 254;
            levels = getTDWRLevels(nlevel, threshold);
            iscale = 10;
        } else if (pcode == 32  ) {
            levels = getTDWRLevels1(nlevel, threshold);
            iscale = 10;
        } else if (pcode == 138) {
            levels = getTDWRLevels1(nlevel, threshold);
            iscale = 100;
        } else if (pcode == 134 ||  pcode == 135) {
            levels = getTDWRLevels2(nlevel, threshold);
            iscale = 1;
        } else if (pcode ==159 || pcode ==161 || pcode == 163
                || pcode == 170 || pcode == 172 || pcode == 173
                || pcode == 174 || pcode == 175
                || pcode == 165 || pcode == 177) {

            levels = getDualpolLevels(threshold);
            iscale = 100;
        } else {
            levels = getLevels(nlevel, threshold);
        }



        Variable v = new Variable(ncfile, null, null, cname + "_RAW");
        v.setDataType(DataType.BYTE);
        v.setDimensions(dims);
        ncfile.addVariable(null, v);
        v.addAttribute( new Attribute(CDM.UNITS, cunit));
        String coordinates = "elevation azimuth gate rays_time latitude longitude altitude";
        v.addAttribute( new Attribute(_Coordinate.Axes, coordinates));
        v.addAttribute( new Attribute(CDM.UNSIGNED, "true"));
        v.setSPobject( new Vinfo (numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ, null, levels, 0, nlevel));


        // addVariable(cname + "_Brightness", ctitle + " Brightness", ncfile, dims, coordinates, DataType.FLOAT,
        //                 cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        if (cname.startsWith("CorrelationCoefficient") || cname.startsWith("HydrometeorClassification") ||
                cname.startsWith("DifferentialReflectivity") ||   cname.startsWith("DifferentialPhase")
                ||   cname.startsWith("HypridHydrometeorClassification") ) {
            addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                    cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        } else if (cname.startsWith("OneHourAccumulation") || cname.startsWith("DigitalAccumulationArray") ||
                cname.startsWith("StormTotalAccumulation") ||   cname.startsWith("DigitalStormTotalAccumulation") ||
                cname.startsWith("Accumulation3Hour") ||   cname.startsWith("Accumulation24Hour") ||
                cname.startsWith("Digital1HourDifferenceAccumulation") ||
                cname.startsWith("DigitalInstantaneousPrecipitationRate") ||
                cname.startsWith("DigitalInstantaneousPrecipitationRate") ||
                cname.startsWith("DigitalTotalDifferenceAccumulation") ) {
            addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                    cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }
        else if( cname.startsWith("BaseReflectivity") || cname.endsWith("Reflectivity") ||
                cname.startsWith("SpectrumWidth")){

          //addVariable(cname + "_VIP", ctitle + " VIP Level", ncfile, dims, coordinates, DataType.FLOAT,
          //             cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);

          addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                       cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }
        else if (cname.startsWith("RadialVelocity") || cname.startsWith("StormMeanVelocity") ||
                cname.startsWith("BaseVelocity")) {

          addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                       cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }
        else if (cname.startsWith("Precip") || cname.endsWith("Precip") ||
            cname.startsWith("EnhancedEchoTop") ||   cname.startsWith("DigitalIntegLiquid")){

           addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                       cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }


        return soff;
    }

    /**
     * for level3 176 product
     *
     * @param datainput
     * @return  arraylist
     */

    public  List parseComponents(ByteBuffer datainput)
            throws IOException
    {
        ArrayList arraylist = null ;
        int i = datainput.getInt();
        if(i != 0)
            i = datainput.getInt();
        for(int j = 0; j < i; j++)
        {
            datainput.getInt();
            int type =  datainput.getInt();
            arraylist = parseData(datainput);
        }

        return arraylist;
    }
    /**
     * for level3 176 product
     *
     * @param datainput
     * @return  arraylist
     */
    public ArrayList parseData(ByteBuffer datainput)
            throws IOException
    {
        ArrayList arraylist = new ArrayList();
        int numRadials;
        int numBins;
        int dataOffset;
        readInString(datainput); // desc
        datainput.getFloat(); // numBins
        float rangeToFirstBin = datainput.getFloat();
        datainput.getInt(); // numOfParms
        datainput.getInt();
        numRadials = datainput.getInt();
        dataOffset = datainput.position();

        //getting numbin  by checking the first radial, but the data offset should be before this read
        datainput.getFloat();
        datainput.getFloat();
        datainput.getFloat();
        numBins = datainput.getInt();

        arraylist.add(numBins);
        arraylist.add(numRadials);
        arraylist.add(rangeToFirstBin);
        arraylist.add(dataOffset);

        //data = null;
    /*    for(int k = 0; k < numRadials; k++)
        {
            angleData[k] = datainput.getFloat();
            datainput.getFloat();
            datainput.getFloat();
            numBins = datainput.getInt();
            readInString(datainput);
            if(data == null)
                data = new short[numRadials * numBins];
            numBins = datainputstream.readInt();
            for(int l = 0; l < numBins; l++)
                data[k * numBins + l] = (short)datainputstream.getInt();

        }  */
        return arraylist;
    }
    /**
     * for level3 176 product
     *
     * @param datainput
     * @return  arraylist
     */
    public List parseParameters(ByteBuffer datainput)
            throws IOException
    {
        ArrayList arraylist = new ArrayList();
        int i = datainput.getInt();
        if(i > 0)
            i = datainput.getInt();
        for(int j = 0; j < i; j++)
        {
            arraylist.add(readInString(datainput));
            HashMap hm = addAttributePairs(readInString(datainput));
            arraylist.add(hm);
        }

        return arraylist;
    }
    /**
     * for level3 176 product
     *
     * @param s
     * @return  attributes
     */
    public HashMap addAttributePairs(String s)
    {
        java.util.regex.Pattern PARAM_PATTERN =
                java.util.regex.Pattern.compile("([\\w*\\s*?]*)\\=([(\\<|\\{|\\[|\\()?\\w*\\s*?\\.?\\,?\\-?\\/?\\%?(\\>|\\}|\\]|\\))?]*)");
        HashMap attributes = new HashMap();
        for(java.util.regex.Matcher matcher = PARAM_PATTERN.matcher(s);
            matcher.find(); attributes.put(matcher.group(1).trim(), matcher.group(2).trim()));

        return attributes;
    }

    /**
     * for level3 176 product
     *
     * @param datainput
     * @return  string
     */
    public static String readInString(ByteBuffer datainput)
            throws IOException
    {
        StringBuffer stringbuffer = new StringBuffer();
        int i = datainput.getInt();
        for(int j = 0; j < i; j++)
        {
            char c = (char)(datainput.get() & 0xff);
            stringbuffer.append(c);
        }

        int k = i % 4;
        if(k != 0)
            k = 4 - k;
        for(int l = 0; l < k; l++)
            datainput.get();

        return stringbuffer.toString();
    }

    /**
     *  construct a generic radial dataset for dualpol radial products;
     *
     *
     * @param bos, hoff, hedsiz, isZ, data, threshold
     *
     * @return  soff -- not used
     */

    int pcode_generic( ByteBuffer  bos, int hoff, int hedsiz, boolean isZ, byte[] data, short[] threshold ) throws IOException
    {
        byte[] b2 = new byte[2];
        int soff = 0;
        ArrayList dims =  new ArrayList();
        int iscale = 1;                         /* data scale                    */
        bos.get(b2, 0, 2);
        bos.get(b2, 0, 2);
        bos.get(b2, 0, 2);

        readInString(bos); // vname
        readInString(bos); // vdesp

        bos.getInt(); // code
        bos.getInt(); // type
        bos.getInt(); // time
        readInString(bos); // rnameStr

        bos.getFloat(); // lat
        bos.getFloat(); // lon
        bos.getFloat(); // height

        bos.getInt(); // vscanStartTime
        bos.getInt(); // eleScanStartTime

        float eleAngle = bos.getFloat();
        p3 = (short) eleAngle;

        bos.getInt(); // volScanNum
        bos.getInt(); // opMode
        bos.getInt(); // volPattern
        bos.getInt(); // eleNum

        bos.getDouble(); // skip 8 bytes
        parseParameters(bos);   // aa - do nothing
        List cc = parseComponents(bos);   // assuming only radial component
        if (cc == null) {
            throw new IOException("Error reading components for radial data");
        }
        int num_radials = (Integer)cc.get(1);
        int num_bin = (Integer)cc.get(0);
        float rangeToFirstBin = (Float)cc.get(2);
        int dataOffset = (Integer)cc.get(3);
        numY0 = 0;
        numY =  num_radials;
        numX0 = (int)rangeToFirstBin ; //first_bin;
        numX = num_bin;
        int nlevel = code_levelslookup( pcode );
        int [] levels;
        short radp_scale = 1000;
        hedsiz = dataOffset;
                //prod_info_size = 2 * (int) (num_bin * scale + 0.5);
        //dimensions: radial, bin
        ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.RADIAL.toString()));
        Dimension radialDim = new Dimension("azimuth", num_radials);
        ncfile.addDimension( null, radialDim);

        Dimension binDim = new Dimension("gate", num_bin);
        ncfile.addDimension( null, binDim);
        dims.add( radialDim);
        dims.add( binDim);

        ArrayList dims1 =  new ArrayList();
        ArrayList dims2 =  new ArrayList();
        dims1.add(radialDim);
        dims2.add(binDim);

        isR = true;

        // add elevation coordinate variable
        String vName = "elevation";
        String lName = "elevation angle in degres: 0 = parallel to pedestal base, 90 = perpendicular";
        Attribute att = new Attribute(_Coordinate.AxisType, AxisType.RadialElevation.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees",hoff, hedsiz, isZ, p3);

        // add azimuth coordinate variable
        vName = "azimuth";
        lName = "azimuth angle in degrees: 0 = true north, 90 = east";
        att = new Attribute(_Coordinate.AxisType, AxisType.RadialAzimuth.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees",hoff, hedsiz, isZ, 0);


        // add gate coordinate variable
        vName = "gate";
        lName = "Radial distance to the start of gate";
        att = new Attribute(_Coordinate.AxisType, AxisType.RadialDistance.toString());
        addParameter(vName, lName, ncfile, dims2, att, DataType.FLOAT, "meters",hoff, hedsiz, isZ, radp_scale);

        // add radial coordinate variable

        vName = "latitude";
        lName = "Latitude of the instrument";
        att = new Attribute(_Coordinate.AxisType, AxisType.Lat.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees",hoff, hedsiz, isZ, 0);

        vName = "longitude";
        lName = "Longitude of the instrument";
        att = new Attribute(_Coordinate.AxisType, AxisType.Lon.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees",hoff, hedsiz, isZ, 0);

        vName = "altitude";
        lName = "Altitude in meters (asl) of the instrument";
        att = new Attribute(_Coordinate.AxisType, AxisType.Height.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "meters",hoff, hedsiz, isZ, 0);

        vName = "rays_time";
        lName = "rays time";
        att = new Attribute(_Coordinate.AxisType, AxisType.Time.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.DOUBLE, "milliseconds since 1970-01-01 00:00 UTC"
                ,hoff, hedsiz, isZ, 0);

        if (  pcode == 176) {
            levels = getDualpolLevels(threshold);
            iscale = 1;
        } else {
            levels = getLevels(nlevel, threshold);
        }

        Variable v = new Variable(ncfile, null, null, cname + "_RAW");
        v.setDataType(DataType.SHORT);
        v.setDimensions(dims);
        ncfile.addVariable(null, v);
        v.addAttribute( new Attribute(CDM.UNITS, cunit));
        String coordinates = "elevation azimuth gate rays_time latitude longitude altitude";
        v.addAttribute( new Attribute(_Coordinate.Axes, coordinates));
        v.addAttribute( new Attribute(CDM.UNSIGNED, "true"));
        v.setSPobject( new Vinfo (numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ, null, levels, 0, nlevel));


        if (cname.startsWith("DigitalInstantaneousPrecipitationRate") ) {
            addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                    cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }

        return soff;
    }
    /**
     * for level3 94 and 99 product
     *
     * @param num_bin
     * @return  size
     */
    public short addBinSize(short num_bin){
        if ((num_bin%2) == 0)
            return num_bin;
        else
            return (short)(num_bin+1);
    }
    /**
     *  get the table to calibrate data value
     *
     * @param nlevel    number of level
     * @param th        thredshold value
     * @return
     */
    public int[] getLevels(int nlevel, short[] th) {
        int [] levels = new int[nlevel];
        int ival;
        int isign;

        for ( int i = 0; i < nlevel; i++ ) {    /* calibrated data values        */
              ival = convertShort2unsignedInt(th[i]);

              if ( (ival & 0x00008000) == 0 ) {
                isign = -1;
                if ( (ival & 0x00000100) == 0 ) isign = 1;
                levels[i] = isign * ( ival & 0x000000FF );
              } else {
                levels[i] = -9999 + ( ival & 0x000000FF);
              }
        }

        return levels;
    }

    /**
     *  get the calibrate data values for TDWR data
     * @param nlevel
     * @param th
     * @return
     */
    public int[] getTDWRLevels(int nlevel, short[] th) {
        int [] levels = new int[ nlevel]; //th[2]+2 ];
        int inc = th[1];
        levels[0] = -9866;
        levels[1] = -9866;
        for ( int i = 2; i < nlevel; i++ ) {    /* calibrated data values        */
            levels[i] = th[0] + (i-2) * inc;
        }

        return levels;
    }

    /**
     * get the calibrate data values for TDWR data
     * @param nlevel
     * @param th
     * @return
     */
    public int[] getTDWRLevels1(int nlevel, short[] th) {
        int [] levels = new int[ nlevel]; //th[2] ];
        int inc = th[1];
        for ( int i = 0; i < nlevel; i++ ) {    /* calibrated data values        */
            levels[i] = th[0] + (i) * inc;
        }

        return levels;
    }
    /**
     * get the calibrate data values for dualpol data
     * @param th
     * @return
     */
    public int[] getDualpolLevels(  short[] th) {

        int inc = th.length;
        int [] levels = new int[ inc]; //th[2] ];
        for ( int i = 0; i < inc; i++ ) {    /* calibrated data values        */
            levels[i] = th[i];
        }

        return levels;
    }
    /**
     * get the calibrate data values for TDWR data
     * @param nlevel
     * @param th
     * @return
     */
    public int[] getTDWRLevels2(int nlevel, short[] th) {

        int inc = th.length;
        int [] levels = new int[ inc]; //th[2] ];
        for ( int i = 0; i < inc; i++ ) {    /* calibrated data values        */
            levels[i] = th[i];
        }

        return levels;
    }
    /**
     * adding new variable to the netcdf file
     * @param pName                 variable name
     * @param longName              variable long name
     * @param nc                    netcdf file
     * @param dims                  variable dimensions
     * @param coordinates            variable coordinate
     * @param dtype                 variable type
     * @param ut                     unit string
     * @param hoff                  header offset
     * @param hedsiz                header size
     * @param isZ                   is compressed file
     * @param nlevel                 calibrated level number
     * @param levels                 calibrate levels
     * @param iscale                 is scale variable
     */
    void addVariable(String pName, String longName, NetcdfFile nc, ArrayList dims, String coordinates,
                    DataType dtype, String ut, long hoff, long hedsiz, boolean isZ, int nlevel, int[] levels, int iscale)
    {
        Variable v = new Variable(nc, null, null, pName);
        v.setDataType(dtype);
        v.setDimensions(dims);
        ncfile.addVariable(null, v);
        v.addAttribute( new Attribute(CDM.LONG_NAME, longName));
        v.addAttribute( new Attribute(CDM.UNITS, ut));
        v.addAttribute( new Attribute(_Coordinate.Axes, coordinates));
        v.setSPobject( new Vinfo (numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ, null, levels, iscale, nlevel));

    }

    /**
     *  adding new parameter to the netcdf file
     * @param pName                variable name
     * @param longName             variable long name
     * @param nc                    netcdf file
     * @param dims                 variable dimensions
     * @param att                  attribute
     * @param dtype                data type
     * @param ut                   unit string
     * @param hoff                 header offset
     * @param doff                 data offset
     * @param isZ                  is compressed file
     * @param y0                   reserved
     */
    void addParameter(String pName, String longName, NetcdfFile nc, ArrayList dims, Attribute att,
                    DataType dtype, String ut, long hoff, long doff, boolean isZ, int y0)
    {
          String vName = pName;
          Variable vVar = new Variable(nc, null, null, vName);
          vVar.setDataType(dtype);
          if( dims != null ) vVar.setDimensions(dims);
          else vVar.setDimensions("");
          if(att != null ) vVar.addAttribute(att);
          vVar.addAttribute( new Attribute(CDM.UNITS, ut));
          vVar.addAttribute( new Attribute(CDM.LONG_NAME, longName));
          nc.addVariable(null, vVar);
          vVar.setSPobject( new Vinfo (numX, numX0, numY, y0, hoff, doff, isR, isZ, null, null, 0, 0));
    }

    /**
     *  misc
     * @param lat
     * @param lon
     * @return
     */
    String StnIdFromLatLon(float lat, float lon )
    {
        return "ID";
    }

    /**
     * Misc
     * @param prod_elevation
     * @return
     */
    private int getProductLevel(int prod_elevation){
        int level = 0;

        if(prod_elevation== 5)
            level =  0;
        else if(prod_elevation== 9)
            level =  1;
        else if(prod_elevation==13 || prod_elevation==15)
            level =  2;
        else if(prod_elevation==18)
            level =  3;
        else if(prod_elevation==24)
            level =  4;
        else if(prod_elevation==31)
            level =  6;

        return level;
    }
    /**
     *  parsing the product information into netcdf dataset
     * @param prod_type      product type
     * @param pinfo          product information
     */
    void setProductInfo(int prod_type, Pinfo pinfo)
    {
                                 /* memo field                */
        String[] cmode = new String[]{"Maintenance", "Clear Air", "Precip Mode"};

        short prod_max = pinfo.p4;
        short prod_min = 0;
        int prod_elevation = 0;
        //int prod_info;
        int prod_top;
        int radial = 0;
        String summary = null;
        java.util.Date endDate;
        java.util.Date startDate;
        String dstring;
        double t1 = 124.0 * 1.853 / 111.26;
        double t2 = 230 / (111.26 * Math.cos(Math.toRadians(latitude)));
        lat_min = latitude -  t1;
        lat_max = latitude + t1;
        lon_min = longitude +  t2; //* Math.cos(Math.toRadians(lat_min));
        lon_max = longitude -  t2; //* Math.cos(Math.toRadians(lat_min));
        startDate = getDate( volumeScanDate, volumeScanTime*1000);
        endDate = getDate( volumeScanDate, volumeScanTime*1000);

        if (prod_type == SPECTRUM) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Base Specturm Width " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

            ctilt = pname_lookup(pcode, prod_elevation/10);
            ctitle = "BREF: Base Spectrum Width";
            cunit = "Knots";
            cname = "SpectrumWidth";
            summary = ctilt + " is a radial image of base reflectivity at tilt " + (prod_elevation/10 + 1) +  " and range 124 nm";
            if(pcode == 28 ){
                t1 = t1 * 0.25;
                t2 = t2 * 0.25;
                lat_min = latitude -  t1;
                lat_max = latitude + t1;
                lon_min = longitude +  t2; //* Math.cos(Math.toRadians(lat_min));
                lon_max = longitude -  t2; //* Math.cos(Math.toRadians(lat_min));
                summary = ctilt + " is a radial image of base reflectivity at tilt " + (prod_elevation/10 + 1) +  " and range 32 nm";
            }
        }
        else if (prod_type == DigitalDifferentialReflectivity) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Differential Reflectivity " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

            int pLevel = getProductLevel(prod_elevation);
            ctilt = pname_lookup(15, pLevel);

            ctitle = "Dualpol: Digital Differential Reflectivity";
            cunit = "dBz";
            cname = "DifferentialReflectivity";
            summary = ctilt + " is a radial image of dual pol differential reflectivity field and its range 162 nm";

        }
        else if (prod_type == DigitalCorrelationCoefficient) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Correlation Coefficient " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

            int pLevel = getProductLevel(prod_elevation);
            ctilt = pname_lookup(16, pLevel);

            ctitle = "Dualpol: Digital Correlation Coefficient";
            cunit = " ";
            cname = "CorrelationCoefficient";
            summary = ctilt + " is a radial image of dual pol Correlation Coefficient field and its range 162 nm";

        }
        else if (prod_type == DigitalDifferentialPhase) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Differential Phase " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

            int pLevel = getProductLevel(prod_elevation);
            ctilt = pname_lookup(17, pLevel);


            ctitle = "Dualpol: Digital Differential Phase";
            cunit = "Degree/km";
            cname = "DifferentialPhase";
            summary = ctilt + " is a radial image of dual pol Differential Phase field and its range 162 nm";

        }
        else if (prod_type == HydrometeorClassification ) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Hydrometeor Classification " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

            int pLevel = getProductLevel(prod_elevation);
            ctilt = pname_lookup(18, pLevel);


            ctitle = "Dualpol: Hydrometeor Classification";
            cunit = " ";
            cname = "HydrometeorClassification";
            summary = ctilt + " is a radial image of dual pol Hydrometeor Classification field and its range 162 nm";

        }
        else if (prod_type == HypridHydrometeorClassification ) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Hyprid Hydrometeor Classification " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

            int pLevel = getProductLevel(prod_elevation);
            ctilt = pname_lookup(18, pLevel);


            ctitle = "Dualpol: Hyprid Hydrometeor Classification";
            cunit = " ";
            cname = "HypridHydrometeorClassification";
            summary = ctilt + " is a radial image of dual pol Hyprid Hydrometeor Classification field and its range 162 nm";

        }
        else if (prod_type == OneHourAccumulation ) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "One Hour Accumulation " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];
            ctilt = "OHA";
            ctitle = "Dualpol: One Hour Accumulation";
            cunit = "IN";
            cname = "OneHourAccumulation";
            summary = ctilt + " is a radial image of dual pol One Hour Accumulation field and its range 124 nm";
        }
        else if (prod_type == DigitalAccumulationArray ) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Digital Accumulation Array " + cmode[pinfo.opmode];
            ctilt = "DAA";
            ctitle = "Dualpol: Digital Accumulation Array";
            cunit = "IN";
            cname = "DigitalAccumulationArray";
            summary = ctilt + " is a radial image of dual pol Digital Accumulation Array field and its range 124 nm";
        }
        else if (prod_type == StormTotalAccumulation ) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Storm Total Accumulation " +  cmode[pinfo.opmode];
            ctilt = "PTA";
            ctitle = "Dualpol: Storm Total Accumulation";
            cunit = "IN";
            cname = "StormTotalAccumulation";
            summary = ctilt + " is a radial image of dual pol Storm Total Accumulation field and its range 124 nm";
        }
        else if (prod_type == DigitalStormTotalAccumulation ) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Digital Storm Total Accumulation  " +  cmode[pinfo.opmode];
            ctilt = "DTA";
            ctitle = "Dualpol: Digital Storm Total Accumulation";
            cunit = "IN";
            cname = "DigitalStormTotalAccumulation";
            summary = ctilt + " is a radial image of dual pol Digital StormTotal Accumulation field and its range 124 nm";
        }
        else if (prod_type == Accumulation3Hour ) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Hyprid Hydrometeor Classification " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];
            int pLevel = getProductLevel(prod_elevation);
            ctilt = pname_lookup(18, pLevel);
            ctitle = "Dualpol: 3-hour Accumulation";
            cunit = "IN";
            cname = "Accumulation3Hour";
            summary = ctilt + " is a radial image of dual pol 3-hour Accumulation field and its range 124 nm";

        }
        else if (prod_type == Digital1HourDifferenceAccumulation) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Digital One Hour Difference Accumulation "  + cmode[pinfo.opmode];
            ctilt = "DOD";
            ctitle = "Dualpol: Digital One Hour Difference Accumulation";
            cunit = "IN";
            cname = "Digital1HourDifferenceAccumulation";
            summary = ctilt + " is a radial image of dual pol Digital One Hour Difference Accumulation field and its range 124 nm";
        }
        else if (prod_type == DigitalTotalDifferenceAccumulation ) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Digital Total Difference Accumulation " + cmode[pinfo.opmode];
            ctilt = "DSD";
            ctitle = "Dualpol: Digital Total Difference Accumulation";
            cunit = "IN";
            cname = "DigitalTotalDifferenceAccumulation";
            summary = ctilt + " is a radial image of dual pol Digital Total Difference Accumulation field and its range 124 nm";
        }
        else if (prod_type == DigitalInstantaneousPrecipitationRate ) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Digital Instantaneous Precipitation Rate " + cmode[pinfo.opmode];
            ctilt = "DPR";
            ctitle = "Dualpol: Digital Instantaneous Precipitation Rate";
            cunit = "IN/Hour";
            cname = "DigitalInstantaneousPrecipitationRate";
            summary = ctilt + " is a radial image of dual pol Digital Instantaneous Precipitation Rate field and its range 124 nm";
        }
        else if (prod_type == BaseReflectivityDR) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Base Reflectivity DR " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

            int pLevel = getProductLevel(prod_elevation);
            ctilt = pname_lookup(94, pLevel);

            ctitle = "HighResolution: Base Reflectivity";
            cunit = "dBz";
            cname = "BaseReflectivityDR";
            summary = ctilt + " is a radial image of base reflectivity field and its range 248 nm";

        }
        else if (prod_type == BaseVelocityDV) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Base Velocity DR " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

            int pLevel = getProductLevel(prod_elevation);
            ctilt = pname_lookup(99, pLevel);

            ctitle = "HighResolution: Base Velocity";
            cunit = "m/s";
            cname = "BaseVelocityDV";
            summary = ctilt + " is a radial image of base velocity field and its range 124 nm";

        } else if (prod_type == DigitalVert_Liquid) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Digital Hybrid Reflect " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

            ctilt = pname_lookup(134, prod_elevation/10);
            ctitle = "Digital: Vertical Integ Liquid";
            cunit = "kg/m^2";
            cname = "DigitalIntegLiquid";
            summary = ctilt + " is a radial image high resolution vertical integral liquid and range 248 nm";

        }
        else if (prod_type == DigitalHybridReflect) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Digital Hybrid Reflect " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

            ctilt = pname_lookup(19, prod_elevation/10);
            ctitle = "DigitalHybrid: Reflectivity";
            cunit = "dBz";
            cname = "DigitalHybridReflectivity";
            summary = ctilt + " is a radial image of base reflectivity at tilt " + (prod_elevation/10 + 1) +  " and range 124 nm";

        } else if (prod_type == Base_Reflect || prod_type == Reflect1) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Base Reflct " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];
            if(prod_type == Reflect1){
                ctilt = "R" + prod_elevation/10;
                summary = ctilt + " is a radial image of base reflectivity at tilt " + (prod_elevation/10 + 1);
            }
            else {
                ctilt = pname_lookup(19, prod_elevation/10);
                summary = ctilt + " is a radial image of base reflectivity at tilt " + (prod_elevation/10 + 1) +  " and range 124 nm";
            }
            ctitle = "BREF: Base Reflectivity";
            cunit = "dBz";
            cname = "BaseReflectivity";

        } else if (prod_type == BaseReflect248) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            cmemo = "Base Reflct 248 " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

            ctilt = pname_lookup(20, prod_elevation/10);
            ctitle = "BREF: 248 nm Base Reflectivity";
            cunit = "dBz";
            cname = "BaseReflectivity248";
            summary = ctilt + " is a radial image of base reflectivity at tilt " + (prod_elevation/10 + 1) +  " and range 248 nm";
            t1 = 248.0 * 1.853 / 111.26;
            t2 = 460 / (111.26 * Math.cos(Math.toRadians(latitude)));
            lat_min = latitude -  t1;
            lat_max = latitude + t1;
            lon_min = longitude + t2;
            lon_max = longitude - t2;
        } else if (prod_type == Comp_Reflect) {
            radial               = 3;
            prod_elevation  = -1;

            ctilt = pname_lookup(pinfo.pcode, elevationNumber);
            if(pinfo.pcode == 36 || pinfo.pcode == 38 ) {
                 t1 = t1 * 2;
                 t2 = t2 * 2;
                 lat_min = latitude -  t1;
                 lat_max = latitude + t1;
                 lon_min = longitude + t2;
                 lon_max = longitude - t2;
            }
            summary = ctilt + "is a raster image of composite reflectivity";
            cmemo = "Composite Reflectivity at " + cmode[pinfo.opmode];
            ctitle = "CREF Composite Reflectivity" + ctilt;
            cunit = "dBz" ;
            cname = "BaseReflectivityComp";
        } else if (prod_type == Layer_Reflect_Avg ||
                 prod_type == Layer_Reflect_Max)   {
            radial               = 3;
            prod_elevation  = pinfo.p5;
            prod_top        = pinfo.p6;
            ctilt = pname_lookup(pcode, 0);
            summary = ctilt + " is a raster image of composite reflectivity at range 124 nm";
            cmemo = "Layer Reflct " + prod_elevation + " - " + prod_top + cmode[pinfo.opmode];
            t1 = t1 * 4;
            t2 = t2 * 4;
            lat_min = latitude - t1;
            lat_max = latitude + t1;
            lon_min = longitude + t2;
            lon_max = longitude - t2;
            ctitle = "LREF: Layer Composite Reflectivity" ;
            cunit = "dBz" ;
            cname = "LayerCompReflect";
        } else if (prod_type == EnhancedEcho_Tops) {
            radial          = 1;
            prod_elevation  = -1;
            summary = "EET is a radial image of echo tops at range 186 nm";
            cmemo = "Enhanced Echo Tops [K FT] " + cmode[pinfo.opmode];
            ctilt = pname_lookup(135, elevationNumber);
            ctitle = "TOPS: Enhanced Echo Tops";
            cunit = "K FT" ;
            cname = "EnhancedEchoTop";
            t1 = t1 * 4;
            t2 = t2 * 4;
            lat_min = latitude -  t1;
            lat_max = latitude + t1;
            lon_min = longitude + t2;
            lon_max = longitude - t2;
        } else if (prod_type == Echo_Tops) {
            radial          = 3;
            prod_elevation  = -1;
            summary = "NET is a raster image of echo tops at range 124 nm";
            cmemo = "Echo Tops [K FT] " + cmode[pinfo.opmode];
            ctilt = pname_lookup(41, elevationNumber);
            ctitle = "TOPS: Echo Tops";
            cunit = "K FT" ;
            cname = "EchoTop";
            t1 = t1 * 4;
            t2 = t2 * 4;
            lat_min = latitude -  t1;
            lat_max = latitude + t1;
            lon_min = longitude + t2;
            lon_max = longitude - t2;
        } else if (prod_type == Precip_1)   {
            radial               = 1;
            prod_elevation  = -1;
            prod_max       /= 10;
            endDate = getDate( pinfo.p7, pinfo.p8 * 60 * 1000);
            summary = "N1P is a raster image of 1 hour surface rainfall accumulation at range 124 nm";
            cmemo = "1-hr Rainfall [IN] " + cmode[pinfo.opmode];
            ctilt = pname_lookup(78, elevationNumber);
            ctitle = "PRE1: Surface 1-hour Rainfall Total";
            cunit = "IN";
            cname = "Precip1hr";
            t1 = t1 * 2;
            t2 = t2 * 2;
            lat_min = latitude -  t1;
            lat_max = latitude + t1;
            lon_min = longitude + t2;
            lon_max = longitude - t2;
        } else if (prod_type == Precip_3)   {
            radial               = 1;
            prod_elevation  = -1;
            prod_max       /= 10;
            endDate = getDate( pinfo.p7, pinfo.p8 * 60 * 1000);
            summary = "N3P is a raster image of 3 hour surface rainfall accumulation at range 124 nm";
            cmemo = "3-hr Rainfall [IN] " + cmode[pinfo.opmode] ;
            ctilt = pname_lookup(79, elevationNumber);
            ctitle = "PRE3: Surface 3-hour Rainfall Total" ;
            cunit = "IN" ;
            cname = "Precip3hr";
            t1 = t1 * 2;
            t2 = t2 * 2;
            lat_min = latitude -  t1;
            lat_max = latitude + t1;
            lon_min = longitude + t2;
            lon_max = longitude - t2;
        } else if (prod_type == DigitalStormTotalPrecip) {
            radial               = 1;
            prod_elevation  = -1;
            //startDate = getDate( pinfo.p5, pinfo.p6 * 60 * 1000);
            endDate = getDate( pinfo.p7, pinfo.p8 * 60 * 1000);
            summary = "DSP is a radial image of digital storm total rainfall";
            cmemo = "Digital Strm Total Precip [IN] " + cmode[pinfo.opmode] ;
            ctilt = pname_lookup(80, elevationNumber);
            ctitle = "DPRE: Digital Storm Total Rainfall" ;
            cunit = "IN" ;
            cname = "DigitalPrecip";
            t1 = t1 * 2;
            t2 = t2 * 2;
            lat_min = latitude -  t1;
            lat_max = latitude + t1;
            lon_min = longitude + t2;
            lon_max = longitude - t2;
        } else if (prod_type == Precip_Accum) {
            radial               = 1;
            prod_elevation  = -1;
            //startDate = getDate( pinfo.p5, pinfo.p6 * 60 * 1000);
            endDate = getDate( pinfo.p7, pinfo.p8 * 60 * 1000);
            summary = "NTP is a raster image of storm total rainfall accumulation at range 124 nm";
            cmemo = "Strm Tot Rain [IN] " + cmode[pinfo.opmode] ;
            ctilt = pname_lookup(80, elevationNumber);
            ctitle = "PRET: Surface Storm Total Rainfall" ;
            cunit = "IN" ;
            cname = "PrecipAccum";
            t1 = t1 * 2;
            t2 = t2 * 2;
            lat_min = latitude -  t1;
            lat_max = latitude + t1;
            lon_min = longitude + t2;
            lon_max = longitude - t2;
         } else if (prod_type == Precip_Array) {
            radial          = 3;
            prod_elevation  = -1;
            summary = "DPA is a raster image of hourly digital precipitation array at range 124 nm";
            endDate = getDate( pinfo.p7, pinfo.p8 * 60 * 1000);
            cmemo = "Precip Array [IN] " + cmode[pinfo.opmode] ;
            ctilt = pname_lookup(81, elevationNumber);
            ctitle = "PRET: Hourly Digital Precipitation Array" ;
            cunit = "dBA" ;
            cname = "PrecipArray";
        } else if (prod_type == Vert_Liquid) {
            radial               = 3;
            prod_elevation  = -1;
            summary = "NVL is a raster image of verticalintegrated liguid at range 124 nm";
            cmemo = "Vert Int Lq H2O [mm] " + cmode[pinfo.opmode] ;
            ctilt = pname_lookup(57, elevationNumber);
            ctitle =  "VIL: Vertically-integrated Liquid Water" ;
            cunit =  "kg/m^2" ;
            cname = "VertLiquid";
            t1 = t1 * 4;
            t2 = t2 * 4;
            lat_min = latitude -  t1;
            lat_max = latitude + t1;
            lon_min = longitude + t2;
            lon_max = longitude - t2;
        } else if (prod_type == Velocity || prod_type == Velocity1) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            prod_min        = pinfo.p4;
            prod_max        = pinfo.p5;
            if(prod_type == Velocity) {
                ctilt = pname_lookup(pinfo.pcode, prod_elevation/10);
            }
            else {
                ctilt = "V" + prod_elevation/10;
            }

            if(pinfo.pcode == 25) {
                t1 = 32.0 * 1.853 / 111.26;
                t2 = 64 / (111.26 * Math.cos(Math.toRadians(latitude)));
                lat_min = latitude -  t1;
                lat_max = latitude + t1;
                lon_min = longitude + t2;
                lon_max = longitude - t2;
                summary = ctilt + " is a radial image of base velocity" + (prod_elevation/10 + 1) +  " and  range 32 nm";
                cunit = "m/s";
            }
            else {
                summary = ctilt + " is a radial image of base velocity at tilt " + (prod_elevation/10 + 1);
                cunit = "m/s";
            }
            cmemo = "Rad Vel "+ prod_elevation/10. + " DEG " + cmode[pinfo.opmode];
            ctitle = "VEL: Radial Velocity" ;
            cname = "RadialVelocity";
        } else if (prod_type == StrmRelMeanVel) {
            radial               = 1;
            prod_elevation  = pinfo.p3;
            prod_min        = pinfo.p4;
            prod_max        = pinfo.p5;
            ctilt = pname_lookup(56, prod_elevation/10);
            summary = ctilt + " is a radial image of storm relative mean radial velocity at tilt " + (prod_elevation/10 + 1) +  " and  range 124 nm";
            cmemo = "StrmRelMnVl " + prod_elevation/10. + " DEG " + cmode[pinfo.opmode];
            ctitle = "SRMV: Storm Relative Mean Velocity" ;
            cunit = "KT" ;
            cname = "StormMeanVelocity";
        } else if (prod_type == VAD) {
            radial               = 0;
            prod_elevation  = pinfo.p3;
            prod_min        = pinfo.p4;
            prod_max        = pinfo.p5;
            summary = "NVW is VAD wind profile which contains wind barbs and alpha numeric data";
            cmemo = "StrmRelMnVl " + prod_elevation/10. + " DEG " + cmode[pinfo.opmode];
            ctilt = pname_lookup(48, elevationNumber);
            ctitle = "SRMV: Velocity Azimuth Display" ;
            cunit = "KT" ;
            cname = "VADWindSpeed";
            lat_min = latitude;
            lat_max = latitude;
            lon_min = longitude;
            lon_max = longitude;
        } else {
            ctilt = "error";
            ctitle = "error" ;
            cunit = "error" ;
            cname = "error";
        }
        /* add geo global att  */
        ncfile.addAttribute(null, new Attribute("summary", "Nexrad level 3 data are WSR-88D radar products." +
                  summary ));
        ncfile.addAttribute(null, new Attribute("keywords_vocabulary", ctilt));
        ncfile.addAttribute(null, new Attribute("conventions", _Coordinate.Convention));
        ncfile.addAttribute(null, new Attribute("format", "Level3/NIDS"));
        ncfile.addAttribute(null, new Attribute("geospatial_lat_min", new Float(lat_min)));
        ncfile.addAttribute(null, new Attribute("geospatial_lat_max", new Float(lat_max)));
        ncfile.addAttribute(null, new Attribute("geospatial_lon_min", new Float(lon_min)));
        ncfile.addAttribute(null, new Attribute("geospatial_lon_max", new Float(lon_max)));
        ncfile.addAttribute(null, new Attribute("geospatial_vertical_min", new Float(height)));
        ncfile.addAttribute(null, new Attribute("geospatial_vertical_max", new Float(height)));

        ncfile.addAttribute(null, new Attribute("RadarElevationNumber", new Integer(prod_elevation)));
        dstring = formatter.toDateTimeStringISO(startDate);
        ncfile.addAttribute(null, new Attribute("time_coverage_start", dstring));
        dstring = formatter.toDateTimeStringISO(endDate);
        ncfile.addAttribute(null, new Attribute("time_coverage_end", dstring));
        ncfile.addAttribute(null, new Attribute("data_min", new Float(prod_min)));
        ncfile.addAttribute(null, new Attribute("data_max", new Float(prod_max)));
        ncfile.addAttribute(null, new Attribute("isRadial", new Integer(radial)));
    }

    /**
     * uncompress the TDWR products
     * @param buf          compressed buffer
     * @param offset       data offset
     * @param uncomplen    uncompressed length
     * @return
     * @throws IOException
     */
    byte[] uncompressed( ByteBuffer buf, int offset, int uncomplen ) throws IOException
    {
        byte[] header = new byte[offset];
        buf.position(0);
        buf.get(header);
        byte[] out = new byte[offset+uncomplen];
        System.arraycopy(header, 0, out, 0, offset);


        CBZip2InputStream cbzip2 = new CBZip2InputStream();

        int numCompBytes = buf.remaining();
        byte[] bufc = new byte[numCompBytes];
        buf.get(bufc, 0, numCompBytes);

        ByteArrayInputStream bis = new ByteArrayInputStream(bufc, 2, numCompBytes - 2);

        //CBZip2InputStream cbzip2 = new CBZip2InputStream(bis);
        cbzip2.setStream(bis);
        int total = 0;
        int nread;
        byte[] ubuff = new byte[40000];
        byte[] obuff = new byte[40000];
        try {
            while ((nread = cbzip2.read(ubuff)) != -1) {
              if (total + nread > obuff.length) {
                byte[] temp = obuff;
                obuff = new byte[temp.length * 2];
                System.arraycopy(temp, 0, obuff, 0, temp.length);
              }
              System.arraycopy(ubuff, 0, obuff, total, nread);
              total += nread;
            }
            if (obuff.length >= 0)
              System.arraycopy(obuff, 0, out, offset, total);
          } catch (BZip2ReadException ioe) {
            log.warn("Nexrad2IOSP.uncompress "+raf.getLocation(), ioe);
        }

        return out;

    }

    /**
     * convert two short into a integer
     * @param s1            short one
     * @param s2            short two
     * @param swapBytes      if swap bytes
     * @return
     */
    public static int shortsToInt(short s1, short s2, boolean swapBytes) {
       byte[] b = new byte[4];
       b[0] = (byte) (s1 >>> 8);
       b[1] = (byte) (s1 >>> 0);
       b[2] =  (byte) (s2 >>> 8);
       b[3] =  (byte) (s2 >>> 0);
       return bytesToInt(b, false);
    }

    /**
     * convert bytes into integer
     * @param bytes           bytes array
     * @param swapBytes       if need to swap
     * @return
     */
    public static int bytesToInt(byte [] bytes, boolean swapBytes) {
        byte a = bytes[0];
        byte b = bytes[1];
        byte c = bytes[2];
        byte d = bytes[3];
        if (swapBytes) {
            return ((a & 0xff) ) +
                ((b & 0xff) << 8 ) +
                ((c & 0xff) << 16 ) +
                ((d & 0xff) << 24);
        } else {
            return ((a & 0xff) << 24 ) +
                ((b & 0xff) << 16 ) +
                ((c & 0xff) << 8 ) +
                ((d & 0xff) );
        }
    }


      /*
      ** Name:       read_dividlen
      **
      ** Purpose:    Read divider ID header from NEXRAD Level III product
      **
      */
    Sinfo read_dividlen( ByteBuffer buf, int offset  )
    {
        byte[] b2 = new byte[2];
        byte[] b4 = new byte[4];
        short D_divider;
        short D_id;
        Short tShort ;

        buf.position(offset);
        buf.get(b2, 0, 2);
        tShort = (Short)convert(b2, DataType.SHORT, -1);
        D_divider  = tShort.shortValue();
        buf.get(b2, 0, 2);
        D_id  = (short)getInt(b2, 2);
        buf.get(b4, 0, 4);
        block_length  = getInt(b4, 4);
        buf.get(b2, 0, 2);
        number_layers  = (short)getInt(b2, 2);

        return new Sinfo ( D_divider, D_id, block_length, number_layers);
    }

    void read_SATab( ByteBuffer buf, int offset  )
    {

          byte[] b2 = new byte[2];

          short B_divider;
          short numPages;
          short numChars;
          Short tShort ;

          buf.position(offset);
          buf.get(b2, 0, 2);
          tShort = (Short)convert(b2, DataType.SHORT, -1);
          B_divider  = tShort.shortValue();
          if(B_divider != -1){
             log.warn( "error reading stand alone tab message "+raf.getLocation());
          }
          buf.get(b2, 0, 2);
          numPages = (Short)convert(b2, DataType.SHORT, -1);
          for(int i = 0; i < numPages; i++){
            buf.get(b2, 0, 2);
            while(getInt(b2, 2) != -1) {
                numChars  = (short)getInt(b2, 2);
                if(numChars < 0){
                    break;
                }
                byte[] tmp = new byte[numChars];
                buf.get(tmp);
                buf.get(b2, 0, 2);
            }
          }
          ArrayList dims =  new ArrayList();
          Dimension tbDim = new Dimension("pageNumber", numPages);
          ncfile.addDimension( null, tbDim);
          dims.add( tbDim);
          Variable ppage = new Variable(ncfile, null, null, "TabMessagePage");
          ppage.setDimensions(dims);
          ppage.setDataType(DataType.STRING);
          ppage.addAttribute( new Attribute(CDM.LONG_NAME, "Stand Alone Tabular Alphanumeric Product Message"));
          ncfile.addVariable(null, ppage);
          //ppage.setSPobject( new Vinfo (numPages, 0, tblen, 0, hoff, ppos, isR, false, null, null, 82, 0));
    }
    /*
    ** Name:       read_msghead
    **
    ** Purpose:    Read message header from NEXRAD Level III product
    **
    **
    */
    int read_msghead( ByteBuffer buf, int offset)
    {

        byte[] b2 = new byte[2];
        byte[] b4 = new byte[4];

        buf.position(0);
        buf.get(b2, 0, 2);
        mcode = (short) getInt(b2, 2);
        buf.get(b2, 0, 2);
        mdate = (short) getInt(b2, 2);
        buf.get(b4, 0, 4);
        mtime = getInt(b4, 4);
        buf.get(b4, 0, 4);
        //out.println( "product date is " + dstring);
        mlength = getInt(b4, 4);
        buf.get(b2, 0, 2);
        msource = (short) getInt(b2, 2);
        if(stationId == null || stationName == null) {
          try {
              NexradStationDB.init(); // make sure database is initialized
              NexradStationDB.Station station = NexradStationDB.getByIdNumber("000"+Short.toString(msource));
              if (station != null) {
                stationId = station.id;
                stationName = station.name;
              }
            } catch (IOException ioe) {
              log.error("NexradStationDB.init "+raf.getLocation(), ioe);
            }
        }
        buf.get(b2, 0, 2);
        mdestId = (short) getInt(b2, 2);
        buf.get(b2, 0, 2);
        mNumOfBlock = (short) getInt(b2, 2);

        return 1;

    }

    /**
     * get unsigned integer from byte array
     * @param b
     * @param num
     * @return
     */
    int getUInt( byte[] b, int num )
    {
        int            base=1;
        int            i;
        int            word=0;

        int bv[] = new int[num];

        for (i = 0; i<num; i++ )
        {
            bv[i] = convertunsignedByte2Short(b[i]);
        }

        /*
        ** Calculate the integer value of the byte sequence
        */

        for ( i = num-1; i >= 0; i-- ) {
            word += base * bv[i];
            base *= 256;
        }

        return word;
    }

    /**
     * get signed integer from bytes
     * @param b
     * @param num
     * @return
     */
    int getInt( byte[] b, int num )
    {
        int            base=1;
        int            i;
        int            word=0;

        int bv[] = new int[num];

        for (i = 0; i<num; i++ )
        {
        bv[i] = convertunsignedByte2Short(b[i]);
        }

        if( bv[0] > 127 )
        {
         bv[0] -= 128;
         base = -1;
        }
        /*
        ** Calculate the integer value of the byte sequence
        */

        for ( i = num-1; i >= 0; i-- ) {
        word += base * bv[i];
        base *= 256;
        }

        return word;

    }
   /***
    * Concatenate two bytes to a 32-bit int value.  <b>a</b> is the high order
    * byte in the resulting int representation, unless swapBytes is true, in
    * which <b>b</b> is the high order byte.
    * @param a high order byte
    * @param b low order byte
    * @param swapBytes byte order swap flag
    * @return 32-bit integer
    */

    public static int bytesToInt(byte a, byte b, boolean swapBytes) {
  		// again, high order bit is expressed left into 32-bit form
  		if (swapBytes) {
  			return (a & 0xff) + ((int)b << 8);
  		} else {
  			return ((int)a << 8) + (b & 0xff);
  		}
    }

    /**
     * convert unsigned byte to short
     * @param b
     * @return
     */
    public short convertunsignedByte2Short(byte b)
    {
        return (short)((b<0)? (short)b + 256 : (short)b);
    }

    /**
     *  convert short to unsigned integer
     * @param b
     * @return
     */
    public int convertShort2unsignedInt(short b)
    {
        return (b<0)? (-1)*b + 32768 : b;
    }

    /**
     * get jave date
     * @param julianDays
     * @param msecs
     * @return
     */
    static public java.util.Date getDate(int julianDays, int msecs) {
        long total = ((long) (julianDays - 1)) * 24 * 3600 * 1000 + msecs;
        return new Date( total);
    }

    /*
    ** Name:       read_proddesc
    **
    ** Purpose:    Read product description header from NEXRAD Level III product
    **
    **
    */
    Pinfo read_proddesc(  ByteBuffer buf, int offset ){
        byte[] b2 = new byte[2];
        byte[] b4 = new byte[4];
        int off = offset;
        Short tShort;
        Integer tInt;
        //Double tDouble = null;

        /* thredds global att */
        ncfile.addAttribute(null, new Attribute("title", "Nexrad Level 3 Data"));
        ncfile.addAttribute(null, new Attribute("keywords", "WSR-88D; NIDS"));
        ncfile.addAttribute(null, new Attribute("creator_name", "NOAA/NWS"));
        ncfile.addAttribute(null, new Attribute("creator_url", "http://www.ncdc.noaa.gov/oa/radar/radarproducts.html"));
        ncfile.addAttribute(null, new Attribute("naming_authority", "NOAA/NCDC"));


        //      ncfile.addAttribute(null, new Attribute("keywords_vocabulary", cname));
        //out.println( "offset of buffer is " + off);
        buf.position(offset);
        buf.get(b2, 0, 2);
        tShort = (Short)convert(b2, DataType.SHORT, -1);
        divider  =  tShort.shortValue();
        ncfile.addAttribute(null, new Attribute("Divider", tShort));

        buf.get(b4, 0, 4);
        tInt = (Integer)convert(b4, DataType.INT, -1);
        latitude = tInt.intValue()/ 1000.0;
        buf.get(b4, 0, 4);
        tInt = (Integer)convert(b4, DataType.INT, -1);
        longitude = tInt.intValue()/ 1000.0;
        buf.get(b2, 0, 2);
        height = getInt(b2, 2)*0.3048; // LOOK now in units of meters


        if (useStationDB) { // override by station table for more accuracy
        try {
          NexradStationDB.init(); // make sure database is initialized
          NexradStationDB.Station station = NexradStationDB.get("K"+stationId);
          if (station != null) {
            latitude = station.lat;
            longitude = station.lon;
            height = station.elev;
            stationName = station.name;
          }
        } catch (IOException ioe) {
          log.error("NexradStationDB.init "+raf.getLocation(), ioe);
        }
        }

        ncfile.addAttribute(null, new Attribute("RadarLatitude", new Double(latitude)));
        ncfile.addAttribute(null, new Attribute("RadarLongitude", new Double(longitude)));
        ncfile.addAttribute(null, new Attribute("RadarAltitude", new Double(height)));

        buf.get(b2, 0, 2);
        pcode = (short)getInt(b2, 2);
        if (stationId != null) ncfile.addAttribute(null, new Attribute("ProductStation", stationId));
        if (stationName != null) ncfile.addAttribute(null, new Attribute("ProductStationName", stationName));
        buf.get(b2, 0, 2);
        opmode = (short)getInt(b2, 2);
        ncfile.addAttribute(null, new Attribute("OperationalMode", new Short(opmode)));
        buf.get(b2, 0, 2);
        volumnScanPattern = (short)getInt(b2, 2);
        ncfile.addAttribute(null, new Attribute("VolumeCoveragePatternName", new Short(volumnScanPattern)));
        buf.get(b2, 0, 2);
        sequenceNumber = (short)getInt(b2, 2);
        ncfile.addAttribute(null, new Attribute("SequenceNumber", new Short(sequenceNumber)));
        buf.get(b2, 0, 2);
        volumeScanNumber = (short)getInt(b2, 2);
        ncfile.addAttribute(null, new Attribute("VolumeScanNumber", new Short(volumeScanNumber)));
        buf.get(b2, 0, 2);
        volumeScanDate = (short)getUInt(b2, 2);
        buf.get(b4, 0, 4);
        volumeScanTime = getUInt(b4, 4);
        buf.get(b2, 0, 2);
        productDate = (short)getUInt(b2, 2);
        buf.get(b4, 0, 4);
        productTime = getUInt(b4, 4);
        java.util.Date pDate = getDate( productDate, productTime*1000);
        String dstring = formatter.toDateTimeStringISO(pDate);
        ncfile.addAttribute(null, new Attribute("DateCreated", dstring));
        buf.get(b2, 0, 2);
        p1 = (short)getInt(b2, 2);
        buf.get(b2, 0, 2);
        p2 = (short)getInt(b2, 2);
        buf.get(b2, 0, 2);
        elevationNumber = (short)getInt(b2, 2);
        ncfile.addAttribute(null, new Attribute("ElevationNumber",new Short(elevationNumber)));
        buf.get(b2, 0, 2);
        p3 = (short)getInt(b2, 2);
        off += 40;
        if(pcode == 182 || pcode == 186 || pcode == 32
                || pcode == 94 || pcode == 99) {
          for(int i = 0; i< 16; i++) {
            buf.get(b2, 0, 2);
            threshold[i] = (short)bytesToInt(b2[0], b2[1], false);
          }
        } else if(pcode == 159 || pcode == 161 || pcode == 163
                || pcode == 170 || pcode == 172 || pcode == 173
                || pcode == 174 || pcode == 175 ) {
            // Scale hw 31 32
            buf.get(b4, 0, 4);
            byte[] b44 = {b4[3], b4[2], b4[1], b4[0]};
            threshold[0] = (short)(java.nio.ByteBuffer.wrap(b44).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat()*100);
            // offset  hw 33 34
            buf.get(b4, 0, 4);
            byte[] b45 = {b4[3], b4[2], b4[1], b4[0]};
            threshold[1] = (short)(java.nio.ByteBuffer.wrap(b45).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat()*100);
            //  hw 35 reserve
            buf.get(b2, 0, 2);
            threshold[2] = 0;
            // hw 36, 37, 38
            for(int i = 3; i< 6; i++) {
                buf.get(b2, 0, 2);
                threshold[i] = (short)bytesToInt(b2[0], b2[1], false);
            }
            buf.get(b4, 0, 4);
            buf.get(b4, 0, 4);
            buf.get(b4, 0, 4);
            buf.get(b4, 0, 4);
        } else if(pcode == 176) {
            // Scale hw 31 32
            buf.get(b4, 0, 4);
            byte[] b44 = {b4[3], b4[2], b4[1], b4[0]};
            threshold[0] = (short)(java.nio.ByteBuffer.wrap(b44).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat());
            // offset  hw 33 34
            buf.get(b4, 0, 4);
            byte[] b45 = {b4[3], b4[2], b4[1], b4[0]};
            threshold[1] = (short)(java.nio.ByteBuffer.wrap(b45).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat());
            //  hw 35 reserve
            buf.get(b2, 0, 2);
            threshold[2] = 0;
            // hw 36, 37, 38
            for(int i = 3; i< 6; i++) {
                buf.get(b2, 0, 2);
                threshold[i] = (short)bytesToInt(b2[0], b2[1], false);
            }
            buf.get(b4, 0, 4);
            buf.get(b4, 0, 4);
            buf.get(b4, 0, 4);
            buf.get(b4, 0, 4);
        }
        else {
          for(int i = 0; i< 16; i++) {
            buf.get(b2, 0, 2);
            threshold[i] = (short)getInt(b2, 2);
          }
        }
        off += 32;
        buf.get(b2, 0, 2);
        p4 = (short)getInt(b2, 2);
        //int t1 = getUInt(b2, 2);
        buf.get(b2, 0, 2);
        p5 = (short)getInt(b2, 2);
        //t1 = getUInt(b2, 2);
        buf.get(b2, 0, 2);
        p6 = (short)getInt(b2, 2);
        //t1 = getUInt(b2, 2);
        buf.get(b2, 0, 2);
        p7 = (short)getInt(b2, 2);
        buf.get(b2, 0, 2);
        p8 = (short)getInt(b2, 2);
        buf.get(b2, 0, 2);
        p9 = (short)getInt(b2, 2);
        buf.get(b2, 0, 2);
        p10 = (short)getUInt(b2, 2) ; //bytesToInt(b2[0], b2[1], true); //       getInt(b2, 2); //
        off += 14;

        buf.get(b2, 0, 2);
        numberOfMaps = (short)getInt(b2, 2);
        ncfile.addAttribute(null, new Attribute("NumberOfMaps",new Short(numberOfMaps)));
        off += 2;
        buf.get(b4, 0, 4);
        //tInt = (Integer)convert(b4, DataType.INT, -1);
        offsetToSymbologyBlock = getInt(b4, 4);
        //ncfile.addAttribute(null, new Attribute("offset_symbology_block",new Integer(offsetToSymbologyBlock)));
        off += 4;
        buf.get(b4, 0, 4);
        //tInt = (Integer)convert(b4, DataType.INT, -1);
        offsetToGraphicBlock = getInt(b4, 4);
        //ncfile.addAttribute(null, new Attribute("offset_graphic_block",new Integer(offsetToGraphicBlock)));
        off += 4;
        buf.get(b4, 0, 4);
        //tInt = (Integer)convert(b4, DataType.INT, -1);
        offsetToTabularBlock = getInt(b4, 4);
        //ncfile.addAttribute(null, new Attribute("offset_tabular_block",new Integer(offsetToTabularBlock)));
        off += 4;

        return  new Pinfo (divider, latitude, longitude, height, pcode, opmode, threshold,
                           sequenceNumber, volumeScanNumber, volumeScanDate, volumeScanTime,
                            productDate, productTime, p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,
                            elevationNumber, numberOfMaps, offsetToSymbologyBlock,
                            offsetToGraphicBlock, offsetToTabularBlock);

        //return pinfo;

    }

  //
  /**
   * this converts a byte array to another primitive array
   * @param barray
   * @param dataType
   * @param nelems
   * @param byteOrder
   * @return
   */
    protected Object convert( byte[] barray, DataType dataType, int nelems, int byteOrder) {

        if (dataType == DataType.BYTE) {
          return barray;
        }

        if (dataType == DataType.CHAR) {
          return IospHelper.convertByteToChar( barray);
        }

        ByteBuffer bbuff = ByteBuffer.wrap( barray);
        if (byteOrder >= 0)
          bbuff.order( byteOrder == ucar.unidata.io.RandomAccessFile.LITTLE_ENDIAN? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        if (dataType == DataType.SHORT) {
          ShortBuffer tbuff = bbuff.asShortBuffer();
          short[] pa = new short[nelems];
          tbuff.get( pa);
          return pa;

        } else if (dataType == DataType.INT) {
          IntBuffer tbuff = bbuff.asIntBuffer();
          int[] pa = new int[nelems];
          tbuff.get( pa);
          return pa;

        } else if (dataType == DataType.FLOAT) {
          FloatBuffer tbuff = bbuff.asFloatBuffer();
          float[] pa = new float[nelems];
          tbuff.get( pa);
          return pa;

        } else if (dataType == DataType.DOUBLE) {
          DoubleBuffer tbuff = bbuff.asDoubleBuffer();
          double[] pa = new double[nelems];
          tbuff.get( pa);
          return pa;
        }

        throw new IllegalStateException();
    }

  //
  /**
   * this converts a byte array to a wrapped primitive (Byte, Short, Integer, Double, Float, Long)
   * @param barray
   * @param dataType
   * @param byteOrder
   * @return
   */
    protected Object convert( byte[] barray, DataType dataType, int byteOrder) {

        if (dataType == DataType.BYTE) {
          return new Byte( barray[0]);
        }

        if (dataType == DataType.CHAR) {
          return new Character((char) barray[0]);
        }

        ByteBuffer bbuff = ByteBuffer.wrap( barray);
        if (byteOrder >= 0)
          bbuff.order( byteOrder == ucar.unidata.io.RandomAccessFile.LITTLE_ENDIAN? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        if (dataType == DataType.SHORT) {
          ShortBuffer tbuff = bbuff.asShortBuffer();
          return new Short(tbuff.get());

        } else if (dataType == DataType.INT) {
          IntBuffer tbuff = bbuff.asIntBuffer();
          return new Integer(tbuff.get());

        } else if (dataType == DataType.LONG) {
          LongBuffer tbuff = bbuff.asLongBuffer();
          return new Long(tbuff.get());

        } else if (dataType == DataType.FLOAT) {
          FloatBuffer tbuff = bbuff.asFloatBuffer();
          return new Float(tbuff.get());

        } else if (dataType == DataType.DOUBLE) {
          DoubleBuffer tbuff = bbuff.asDoubleBuffer();
          return new Double(tbuff.get());
        }

        throw new IllegalStateException();
    }


  //////////////////////////////////////////////////////////////////////////
  // utilities



    /**
    * Flush all data buffers to disk.
    * @throws IOException
    */
    public void flush() throws IOException {
        raf.flush();
    }

    /**
    *  Close the file.
    * @throws IOException
    */
    public void close() throws IOException {
        if (raf != null)
          raf.close();
    }


    /**
    * Name:       IsZlibed
    *
    * Purpose:    Check a two-byte sequence to see if it indicates the start of
    *             a zlib-compressed buffer
    *
    */
    int isZlibHed( byte[] buf ){
        short b0 = convertunsignedByte2Short(buf[0]);
        short b1 = convertunsignedByte2Short(buf[1]);

        if ( (b0 & 0xf) == Z_DEFLATED ) {
          if ( (b0 >> 4) + 8 <= DEF_WBITS ) {
            if ( (((b0 << 8) + b1) % 31)==0 ) {
              return 1;
            }
          }
        }

        return 0;

    }

    /*
    ** Name:       IsEncrypt
    **
    ** Purpose:    Check a two-byte sequence to see if it indicates the start of
    **             an encrypted image.
    **
    */
    int IsEncrypt( byte[] buf )
    {
        /*
        ** These tests were deduced from inspection from encrypted NOAAPORT files.
        */
        String b = new String(buf, CDM.utf8Charset);
        if ( b.startsWith("R3") ) {
        return 1;
        }

        return 0;
    }


    /*
    ** Name:    GetZlibedNexr
    **
    ** Purpose: Read bytes from a NEXRAD Level III product into a buffer
    **          This routine reads compressed image data for Level III formatted file.
    **          We referenced McIDAS GetNexrLine function
    */
    byte[] GetZlibedNexr( byte[] buf, int buflen, int hoff ) throws IOException
    {
      //byte[]  uncompr = new byte[ZLIB_BUF_LEN ]; /* decompression buffer          */
      //long    uncomprLen = ZLIB_BUF_LEN;        /* length of decompress space    */
      int             doff ;                   /* # bytes offset to image       */
      int             numin;                /* # input bytes processed       */



      numin = buflen - hoff ;

      if( numin <= 0 )
      {
        log.warn(" No compressed data to inflate "+raf.getLocation());
        return null;
      }
      //byte[]  compr = new byte[numin-4];  /* compressed portion */
      /*
      ** Uncompress first portion of the image.  This should include:
      **
      **     SHO\r\r\n             <--+
      **     SEQ#\r\r\n               |  hoff bytes long
      **     WMO header\r\r\n         |
      **     PIL\r\r\n             <--+
      **
      **  -> CCB
      **     WMO header
      **     PIL
      **     portion of the image
      **
      */
      /* a new copy of buff with only compressed bytes */

      System.arraycopy( buf, hoff, buf, hoff, numin - 4);

      // decompress the bytes
      int resultLength;
      int result = 0;
      // byte[] inflateData = null;
      byte[] tmp;
      int  uncompLen = 24500;        /* length of decompress space    */
      byte[] uncomp = new byte[uncompLen];
      Inflater inflater = new Inflater( false);

      inflater.setInput(buf, hoff, numin-4);
      int offset = 0;
      int limit = 20000;

      while ( inflater.getRemaining() > 0 )
      {
          try {
            resultLength = inflater.inflate(uncomp, offset, 4000);
          }
          catch (DataFormatException ex) {
            //System.out.println("ERROR on inflation "+ex.getMessage());
            //ex.printStackTrace();
            log.error("nids Inflater", ex);
            throw new IOException( ex.getMessage(), ex);
          }
          offset = offset + resultLength;
          result = result + resultLength;
          if( result > limit ) {
              // when uncomp data larger then limit, the uncomp need to increase size
              tmp = new byte[ result];
              System.arraycopy(uncomp, 0, tmp, 0, result);
              uncompLen = uncompLen + 10000;
              uncomp = new byte[uncompLen];
              System.arraycopy(tmp, 0, uncomp, 0, result);
          }
          if( resultLength == 0 ) {
               int tt = inflater.getRemaining();
               byte [] b2 = new byte[2];
               System.arraycopy(buf,hoff+numin-4-tt, b2, 0, 2);
               if(result+tt > uncompLen){
                      tmp = new byte[ result];
                      System.arraycopy(uncomp, 0, tmp, 0, result);
                      uncompLen = uncompLen + 10000;
                      uncomp = new byte[uncompLen];
                      System.arraycopy(tmp, 0, uncomp, 0, result);
               }
               if( isZlibHed( b2 ) == 0 ) {
                  System.arraycopy(buf, hoff+numin-4-tt, uncomp, result, tt);
                  result = result + tt;
                  break;
               }
               inflater.reset();
               inflater.setInput(buf, hoff+numin-4-tt, tt);
          }

      }

      inflater.end();
      /*
      ** Find out how long CCB is.  This is done by using the lower order
      ** 6 bits from the first uncompressed byte and all 8 bits of the
      ** second uncompressed byte.
      */
      doff  = 2 * (((uncomp[0] & 0x3f) << 8) | (uncomp[1] & 0xFF));

      for ( int i = 0; i < 2; i++ ) {                         /* eat WMO and PIL */
        while ( (doff < result ) && (uncomp[doff] != '\n') ) doff++;
        doff++;
      }

      byte[] data = new byte[ result - doff];

      System.arraycopy(uncomp, doff, data, 0, result - doff);

      //
      /*
      ** Copy header bytes to decompression buffer.  The objective is to
      ** create an output buffer that looks like an uncompressed NOAAPORT
      ** NEXRAD product:
      **
      **   Section               Product               Example             End
      **            +--------------------------------+
      **            |                                |
      **      1     |        start of product        | CTRL-A              \r\r\n
      **            |                                |
      **            +--------------------------------+
      **            |                                |
      **      2     |        sequence number         | 237                 \r\r\n
      **            |                                |
      **            +--------------------------------+
      **            |                                |
      **      3     |          WMO header            | SDUS53 KARX 062213  \r\r\n
      **            |                                |
      **            +--------------------------------+
      **            |                                |
      **      4     |             PIL                | N0RARX              \r\r\n
      **            |                                |
      **            +--------------------------------+
      **            |                                |
      **      5     |                                | AAO130006R2 CH-1
      **            |                                | Interface Control
      **            |             CCB                | Document (ICD)
      **            |                                | for the NWS NWSTG
      **            |                                | Figure 7-1 p 38
      **            |                                |
      **            +--------------------------------+
      **            |                                |
      **      6     |          WMO header            | SDUS53 KARX 062213  \r\r\n
      **            |                                |
      **            +--------------------------------+
      **            |                                |
      **      7     |             PIL                | N0RARX              \r\r\n
      **            |                                |
      **            +--------------------------------+
      **            |                                |
      **            |                                |
      **            |                                |
      **            |                                |
      **      8     |            image               |
      **            |                                |
      **            |                                |
      **            |                                |
      **            |                                |
      **            +--------------------------------+
      **            |                                |
      **      9     |            trailer             | \r\r\nETX
      **            |                                |
      **            +--------------------------------+
      **            |                                |
      **     10     |     Unidata floater trailer    | \0\0
      **            |                                |
      **            +--------------------------------+
      **
      ** Sections 5-8 are zlib compressed.  They must be uncompressed and
      ** read to find out where the image begins.  When this is done, sections
      ** 5-7 are thrown away and 8 is returned immediately following 4.
      ** Section 9 and, if it is there, section 10 are also thrown away.
      **
      */

      return data;

    }




    /*
    ** Name:       code_lookup
    **
    ** Purpose:    Derive some derivable metadata
    **
    */
    static int code_typelookup( int code )
    {
      int type;
      final int[] types = {
        Other, Other, Other, Other, Other,                          /*   0-  9 */
        Other, Other, Other, Other, Other,
        Other, Other, Other, Other, Other,                          /*  10- 19 */
        Other, Base_Reflect, Base_Reflect, Base_Reflect, Base_Reflect,
        BaseReflect248, Base_Reflect, Velocity,                     /*  20- 29 */
        Velocity, Velocity, Velocity, Velocity, Velocity, SPECTRUM, SPECTRUM,
        SPECTRUM, Other, DigitalHybridReflect, Other, Other,        /*  30- 39 */
        Comp_Reflect, Comp_Reflect, Comp_Reflect, Comp_Reflect, Other,
        Other, Echo_Tops, Other, Other, Other,                      /*  40- 49 */
        Other, Other, Other, VAD, Other,
        Other, Other, Other, Other, Other,                          /*  50- 59 */
        StrmRelMeanVel, StrmRelMeanVel, Vert_Liquid, Other, Other,
        Other, Other, Other, Layer_Reflect_Avg,                     /*  60- 69 */
        Layer_Reflect_Avg, Layer_Reflect_Max,
        Layer_Reflect_Max, Other, Other, Other,
        Other, Other, Other, Other, Other,                          /*  70- 79 */
        Other, Other, Other, Precip_1, Precip_3,
        Precip_Accum, Precip_Array, Other,                          /*  80- 89 */
        Other, Other, Other, Other, Other, Other, Layer_Reflect_Avg,
        Layer_Reflect_Max, Other, Other, Other,                     /*  90- 99 */
        BaseReflectivityDR, Other, Other, Other, Other, BaseVelocityDV,
        Other, Other, Other, Other, Other,                          /* 100-109 */
        Other, Other, Other, Other, Other,
        Other, Other, Other, Other, Other,                          /* 110-119 */
        Other, Other, Other, Other, Other,
        Other, Other, Other, Other, Other,                          /* 120-129 */
        Other, Other, Other, Other, Other,
        Other, Other, Other, Other, DigitalVert_Liquid,             /* 130-139 */
        EnhancedEcho_Tops, Other, Other, DigitalStormTotalPrecip, Other,
        Other, Other, Other, Other, Other,                          /* 140-149 */
        Other, Other, Other, Other, Other,
        Other, Other, Other, Other, Other,                          /* 150-159 */
        Other, Other, Other, Other, DigitalDifferentialReflectivity,
        Other, DigitalCorrelationCoefficient, Other, DigitalDifferentialPhase, Other,    /* 160-169 */
        HydrometeorClassification, Other, Other, Other, OneHourAccumulation,
        DigitalAccumulationArray, StormTotalAccumulation, DigitalStormTotalAccumulation,
                                   Accumulation3Hour, Digital1HourDifferenceAccumulation,/* 170-179 */
        DigitalTotalDifferenceAccumulation, DigitalInstantaneousPrecipitationRate,
                                   HypridHydrometeorClassification, Other, Other,
        Reflect1, Reflect1, Velocity1, Velocity1, Other,       /* 180-189 */
        SPECTRUM1, Reflect1, Reflect1, Other, Other,
      };

      if ( code < 0 || code > 189 )
        type     = Other;
      else
        type     = types[code];

      return type;

    }

    /*
     * product id table
     * @param code
     * @param elevation
     * @return
     */
    static String pname_lookup( int code, int elevation )
    {
      String pname = null;
      switch( code ){
          case 15:
              if(elevation == 1)
                  pname = "NAX";
              else if(elevation == 3)
                  pname = "NBX";
              else
                  pname = "N" + elevation/2 + "X";
              break;
          case 16:
              if(elevation == 1)
                  pname = "NAC";
              else if(elevation == 3)
                  pname = "NBC";
              else
                  pname = "N" + elevation/2 + "C";
              break;
          case 17:
              if(elevation == 1)
                  pname = "NAK";
              else if(elevation == 3)
                  pname = "NBK";
              else
                  pname = "N" + elevation/2 + "K";
              break;
          case 18:
              if(elevation == 1)
                  pname = "NAH";
              else if(elevation == 3)
                  pname = "NBH";
              else
                  pname = "N" + elevation/2 + "H";
              break;
          case 19:
              pname = "N" + elevation + "R";

              break;
          case 20:
              pname = "N0Z";
            break;
          case 25:
              pname = "N0W";
            break;
          case 27:
              pname = "N" + elevation + "V";

            break;
          case 28:
            pname = "NSP";
            break;
          case 30:
            pname = "NSW";
            break;
          case 36:
            pname = "NCO";
            break;
          case 37:
            pname = "NCR";
            break;
          case 38:
            pname = "NCZ";
            break;
          case 41:
            pname = "NET";
            break;
          case 48:
            pname = "NVW";
            break;
          case 56:
              pname = "N" + elevation + "S";

            break;
          case 57:
            pname = "NVL";
            break;
          case 65:
            pname = "NLL";
            break;
          case 66:
            pname = "NML";
            break;
          case 78:
            pname = "N1P";
            break;
          case 79:
            pname = "N3P";
            break;
          case 80:
            pname = "NTP";
            break;
          case 81:
            pname = "DPA";
            break;
          case 90:
            pname = "NHL";
            break;
          case 94:
            if(elevation == 1)
                pname = "NAQ";
            else if(elevation == 3)
                pname = "NBQ";
            else
                pname = "N" + elevation/2 + "Q";
            break;
          case 99:
            if(elevation == 1)
                pname = "NAU";
            else if(elevation == 3)
                pname = "NBU";
            else
                pname = "N" + elevation/2 + "U";
            break;
          case 134:
            pname = "DVL";
            break;
          case 135:
            pname = "EET";
            break;
          case 182:
            pname = "DV";
            break;
          case 187:
          case 181:
            pname = "R";
            break;
          case 186:
          case 180:
            pname = "DR";
            break;
          case 183:
            pname = "V";
            break;
          case 185:
            pname = "SW";
            break;

          default:
              break;

      }

      return pname;

    }

    /*
     * product resolution
     * @param code
     * @return
     */
    static double code_reslookup( int code )
    {

      double data_res;

      final  double[] res = {
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /*   0-  9 */
        0,    0,    0,    0,    0,    0,    1,    2,    4,    1,    /*  10- 19 */
        2,    4, 0.25,  0.5,    1, 0.25,  0.5,    1, 0.25,    0,    /*  20- 29 */
        1,    0,    1,    0,    0,    1,    4,    1,    4,    0,    /*  30- 39 */
        0,    4,    0,    0,    0,    0,    0,    0,    0,    0,    /*  40- 49 */
        0,    0,    0,    0,    0,  0.5,    1,    4,    0,    0,    /*  50- 59 */
        0,    0,    0,    4,    4,    4,    4,    0,    0,    0,    /*  60- 69 */
        0,    0,    0,    0,    0,    0,    0,    0,    1,    1,    /*  70- 79 */
        1,    4,    0,    0,    0,    0,    0,    0,    0,    4,    /*  80- 89 */
        4,    0,    0,    0,    1,    0,    0,    0,    0, 0.25,    /*  90- 99 */
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /* 100-109 */
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /* 110-119 */
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /* 120-129 */
        0,    0,    0,    0,    1,    1,    0,    0,    1,    0,    /* 130-139 */
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /* 140-149 */
        0,    0,    0,    0,    0,    0,    0,    0,    0, 0.25,    /* 150-159 */
        0, 0.25,    0, 0.25,    0, 0.25,    0,    0,    0,    2,    /* 160-169 */
     0.25,    2, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25,    0,    0,    /* 170-179 */
        0,  150.0, 150.0, 0,    0,    0, 300.0,   0,    0,    0,    /* 180-189 */
      };


      if ( code < 0 || code > 189 )
        data_res = 0;
      else
        data_res = res[code];

      return data_res;

    }

    /*
     * product level tabel
     * @param code
     * @return
     */
    static int code_levelslookup( int code )
    {

      int level;

      final int[] levels = {
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /*   0-  9 */
        0,    0,    0,    0,    0,    0,    8,    8,    8,   16,    /*  10- 19 */
       16,   16,    8,    8,    8,   16,   16,   16,    8,    0,    /*  20- 29 */
        8,    0,  256,    0,    0,    8,    8,   16,   16,    0,    /*  30- 39 */
        0,   16,    0,    0,    0,    0,    0,    0,    0,    0,    /*  40- 49 */
        0,    0,    0,    0,    0,   16,   16,   16,    0,    0,    /*  50- 59 */
        0,    0,    0,    8,    8,    8,    8,    0,    0,    0,    /*  60- 69 */
        0,    0,    0,    0,    0,    0,    0,    0,   16,   16,    /*  70- 79 */
       16,  256,    0,    0,    0,    0,    0,    0,    0,    8,    /*  80- 89 */
        8,    0,    0,    0,  256,    0,    0,    0,    0,  256,    /*  90- 99 */
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /* 100-109 */
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /* 110-119 */
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /* 120-129 */
        0,    0,    0,    0,  256,  199,    0,    0,  256,    0,    /* 130-139 */
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /* 140-149 */
        0,    0,    0,    0,    0,    0,    0,    0,    0,  256,    /* 150-159 */
        0,  256,    0,  256,    0,  256,    0,    0,    0,   16,    /* 160-169 */
      256,   16,  256,  256,    0,    0,    0,   16,    0,    0,    /* 170-179 */
        0,   16,  256,    0,    0,    0,  256,    0,    0,    0,    /* 180-189 */
      };


      if ( code < 0 || code > 189 )
        level = 0;
      else
        level = levels[code];

      return level;

    }

    // Symbology block info for reading/writing
    static class Sinfo {
      short divider;
      short id;
      int blockLength;
      short nlayers;

      Sinfo( short divider, short id, int length, short layers) {
        this.divider = divider;
        this.id = id;
        this.blockLength = length;
        this.nlayers = layers;
      }
    }

  // variable info for reading/writing
    static class Vinfo {
        int xt;
        int x0;
        int yt;
        int y0;
        boolean isRadial; // is it a radial variable?
        long hoff;    // header offset
        long doff;
        boolean isZlibed;
        int[] pos;
        int[] len;
        int code;
        int level;

        Vinfo( int xt, int x0, int yt, int y0,long hoff, long doff, boolean isRadial, boolean isZ,
               int[] pos, int[] len, int code, int level ) {
            this.xt = xt;
            this.yt = yt;
            this.x0 = x0;
            this.y0 = y0;
            this.hoff = hoff;
            this.doff = doff;
            this.isRadial = isRadial;
            this.isZlibed = isZ;
            this.pos = pos;
            this.len = len;
            this.code = code;
            this.level = level;
        }
    }


  // product info for reading/writing
    static class Pinfo {
        short divider, pcode, opmode, sequenceNumber, volumeScanNumber, volumeScanDate, productDate;
//        double latitude, longitude;
        double height; // meters
        int volumeScanTime,  productTime;
        short p1, p2, p3, p4, p5, p6, p7, p8, p9, p10;
        int offsetToSymbologyBlock, offsetToGraphicBlock, offsetToTabularBlock;
        short [] threshold;
        Pinfo() {
            // do nothing ;
        }
        Pinfo (short divider , double latitude, double longitude, double height, short pcode, short opmode, short[] threshold,
               short sequenceNumber, short volumeScanNumber, short volumeScanDate, int volumeScanTime,
               short productDate, int productTime, short p1,short p2,short p3,short p4,short p5,
               short p6,short p7,short p8, short p9,short p10,
               short elevationNumber, short numberOfMaps, int offsetToSymbologyBlock,
               int offsetToGraphicBlock, int offsetToTabularBlock)   {
          this.divider = divider;
          this.height = height;
          this.pcode = pcode;
          this.opmode = opmode;
          this.sequenceNumber = sequenceNumber ;
          this.volumeScanNumber = volumeScanNumber ;
          this.volumeScanDate = volumeScanDate ;
          this.volumeScanTime = volumeScanTime ;
          this.productDate = productDate ;
          this.productTime = productTime ;
          this.p1 = p1 ;
          this.p2 = p2 ;
          this.p3 = p3 ;
          this.p4 = p4;
          this.p5 = p5 ;
          this.p6 = p6 ;
          this.p7 = p7 ;
          this.p8 = p8 ;
          this.p9 = p9 ;
          this.p10 = p10 ;
          this.threshold = threshold;
          this.offsetToSymbologyBlock = offsetToSymbologyBlock ;
          this.offsetToGraphicBlock = offsetToGraphicBlock ;
          this.offsetToTabularBlock = offsetToTabularBlock ;

        }
    }

}
