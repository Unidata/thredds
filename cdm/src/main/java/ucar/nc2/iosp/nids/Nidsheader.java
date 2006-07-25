// $Id:Nidsheader.java 63 2006-07-12 21:50:51Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.nids;

import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.*;
import ucar.nc2.iosp.nexrad2.NexradStationDB;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.util.Parameter;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;


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
  boolean noHeader = false;

  DateFormatter formatter = new DateFormatter();

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

  int readWMO(ucar.unidata.io.RandomAccessFile raf ) throws IOException
  {
    int pos = 0;
    //long     actualSize = 0;
    raf.seek(pos);
    int readLen = 25;
    int rc = 0;

    // Read in the contents of the NEXRAD Level III product head
    byte[] b = new byte[readLen];
    rc = raf.read(b);
    if ( rc != readLen )
    {
        // out.println(" error reading nids product header");
        return 0;
    }

    //Get product message header into a string for processing

    String pib = new String(b);
    if(  pib.indexOf("SDUS")!= -1){
        return 1;
    } else if ( raf.getLocation().indexOf(".nids") != -1) {
        noHeader = true;
        return 1;
    } else if(checkMsgHeader(raf) == 1) {
        return 1;
    } else
        return 0;
  }

  public byte[] getUncompData(int offset, int len){
      if( len == 0 ) len = uncompdata.length - offset;
      byte[] data = new byte[len];
      System.arraycopy(uncompdata, offset, data, 0, len);
      return data;
  }
 //////////////////////////////////////////////////////////////////////////////////

  private ucar.unidata.io.RandomAccessFile raf;
  private ucar.nc2.NetcdfFile ncfile;
  private PrintStream out = System.out;
  //private Vinfo myInfo;
  private String   cmemo, ctilt, ctitle, cunit, cname ;
  public void setProperty( String name, String value) { }

  private   int numX ;
  private   int numX0;
  private   int numY ;
  private   int numY0;
  private   boolean isR = false;
  private byte[] uncompdata = null;

  /////////////////////////////////////////////////////////////////////////////
  // reading header

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
        out.println(" error reading nids product header");
    }

    if( !noHeader ) {
        //Get product message header into a string for processing
        String pib = new String(b, 0, 100);
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
                out.println( "error reading encryted product " );
                throw new IOException("unable to handle the product with encrypt code " + encrypt);
            }
        }
       // process product description for station ID
        byte[] b3 = new byte[3];
        //byte[] uncompdata = null;

        switch ( type ) {
          case 0:
            out.println( "ReadNexrInfo:: Unable to seek to ID ");
            break;
          case 1:
          case 2:
          case 3:
          case 4:
            System.arraycopy(b, hoff - 6, b3, 0, 3);
            stationId  = new String(b3);
            break;

          default:
            break;
        }

        if ( zlibed == 1 ) {
              isZ = true;
              uncompdata = GetZlibedNexr( b, readLen,  hoff );
              //uncompdata = Nidsiosp.readCompData(hoff, 160) ;
              if ( uncompdata == null ) {
                out.println( "ReadNexrInfo:: error uncompressing image" );
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

    bos.position();
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
    // Get product symbology header (needed to get image shape)
    ifloop: if ( pinfo.offsetToSymbologyBlock != 0 ) {

      // Symbology header
      Sinfo sinfo = read_dividlen( bos, hedsiz );
      if( rc == 0 || pinfo.divider != -1 )
      {
          out.println( "error in product symbology header" );
      }
      if(sinfo.id != 1)
      {
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
            out.println( "error reading length divider" );
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
                                out.println( "error reading pcode equals " + pcode);
                                throw new IOException("error reading pcode, unable to handle the product with code " + pcode);
                          }
                          poff = poff + len + 4;
                      }
                      break;

                 default:
                      if ( pkcode == 0xAF1F ) {              /* radial image                  */
                          hedsiz += pcode_radial( bos, hoff, hedsiz, isZ, uncompdata, pinfo.threshold) ;
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
                          out.println( "error reading pkcode equals " + pkcode);
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
          pcode_128( pkcode8Doff, pkcode8Size, 8, hoff, pcode8Number, "textStruct_code8", isZ);
      }
      if (pkcode1Doff != null){
          pcode_128( pkcode1Doff, pkcode1Size, 1, hoff, pcode1Number, "textStruct_code1", isZ);
      }
      if (pkcode2Doff != null){
          pcode_128( pkcode2Doff, pkcode2Size, 2, hoff, pcode2Number, "textStruct_code2", isZ);
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
          pcode_12n13n14( pkcode13Doff, pkcode12Dlen, hoff, pcode13Number, isZ, "hailPositive", 13);
      }
      if (pkcode14Doff != null){
          pcode_12n13n14( pkcode14Doff, pkcode12Dlen, hoff, pcode14Number, isZ, "hailProbable", 14);
      }
    } else {
      out.println ( "GetNexrDirs:: no product symbology block found (no image data)" );

    }

    if ( pinfo.offsetToTabularBlock != 0 ) {
         int tlayer = pinfo.offsetToTabularBlock*2;
         bos.position(tlayer);
         short tab_divider = bos.getShort();
         if ( tab_divider != -1) {
             out.println ( "Block divider not found" );
             throw new IOException("error reading graphic alphanumeric block" );
         }
         short tab_bid = bos.getShort();
         int tblen = bos.getInt();

         bos.position(tlayer+116);
         int inc = bos.getInt();
         bos.position(tlayer+128); // skip the second header and prod description

         tab_divider = bos.getShort();
         if ( tab_divider != -1) {
             out.println ( "tab divider not found" );
             throw new IOException("error reading graphic alphanumeric block" );
         }
         int npage = bos.getShort();

         int ppos =  bos.position();

         ArrayList dims =  new ArrayList();
         Dimension tbDim = new Dimension("pageNumber", npage, true);
         ncfile.addDimension( null, tbDim);
         dims.add( tbDim);
         Variable ppage = new Variable(ncfile, null, null, "TabMessagePage");
         ppage.setDimensions(dims);
         ppage.setDataType(DataType.STRING);
         ppage.addAttribute( new Attribute("long_name", "Graphic Product Message"));
         ncfile.addVariable(null, ppage);
         ppage.setSPobject( new Vinfo (npage, 0, tblen, 0, hoff, ppos, isR, isZ, null, null, tab_bid, 0));

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
               out.println( "error reading graphic alphanumeric block");
               throw new IOException("error reading graphic alphanumeric block" );
         }
         int blen = bos.getInt();
         int clen = 0;
         int npage = bos.getShort();
         int lpage ;
         int ipage = 0;
         int plen ;
         int ppos = bos.position();
         while ( (clen < blen) && (ipage < npage) ) {
            bos.position(ppos);

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
                   gpkcode8Doff[gpcode8Number] = plen - 6;
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
                   icnt += plen + 4;
                   gpkcode10Dlen[gpcode10Number] = ( plen - 2 ) / 8;
                   gpcode10Number++;
                }
                else {
                    out.println( "error reading pkcode equals " + pkcode);
                    throw new IOException("error reading pkcode in graphic alpha num block " + pkcode);
                }

            }
            ppos = ppos + lpage + 4;
            clen = clen + lpage + 4;
         }

         if(gpkcode8Doff != null){
             pcode_128( gpkcode8Doff, gpkcode8Size, 8, hoff, gpcode8Number, "textStruct_code8", isZ);
         }
         if(gpkcode2Doff != null){
             pcode_128( gpkcode2Doff, gpkcode2Size, 2, hoff, gpcode8Number, "textStruct_code8", isZ);
         }
         if(gpkcode1Doff != null){
             pcode_128( gpkcode1Doff, gpkcode1Size, 1, hoff, gpcode1Number, "textStruct_code1", isZ);
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
         ppage.addAttribute( new Attribute("long_name", "Graphic Product Message"));
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
        Dimension sDim = new Dimension("graphicSymbolSize", vlen, true);
        ncfile.addDimension( null, sDim);
        dims.add( sDim);

        Structure dist = new Structure(ncfile, null, null, structName);
        dist.setDimensions(dims);
        ncfile.addVariable(null, dist);
        dist.addAttribute( new Attribute("long_name", "special graphic symbol for code "+code));


        Variable i0 = new Variable(ncfile, null, dist, "x_start");
        i0.setDimensions((String)null);
        i0.setDataType(DataType.SHORT);
        i0.addAttribute( new Attribute("units", "KM/4"));
        dist.addMemberVariable(i0);
        Variable j0 = new Variable(ncfile, null, dist, "y_start");
        j0.setDimensions((String)null);
        j0.setDataType(DataType.SHORT);
        j0.addAttribute( new Attribute("units", "KM/4"));
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

      Dimension sDim = new Dimension("circleSize", len, true);
      ncfile.addDimension( null, sDim);
      dims.add( sDim);

      Structure dist = new Structure(ncfile, null, null, "circleStruct");
      dist.setDimensions(dims);
      ncfile.addVariable(null, dist);
      dist.addAttribute( new Attribute("long_name", "Circle Packet"));


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

      Dimension sDim = new Dimension(vname+"Size", vlen, true);
      ncfile.addDimension( null, sDim);
      dims.add( sDim);

      Structure dist = new Structure(ncfile, null, null, vname +"Struct");
      dist.setDimensions(dims);
      ncfile.addVariable(null, dist);
      dist.addAttribute( new Attribute("long_name", vname+" Packet"));


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

      Dimension sDim = new Dimension("windBarbSize", len, true);
      ncfile.addDimension( null, sDim);
      dims.add( sDim);

      Structure dist = new Structure(ncfile, null, null, cname);
      dist.setDimensions(dims);
      ncfile.addVariable(null, dist);
      dist.addAttribute( new Attribute("long_name", "Wind Barb Data"));

      Variable value = new Variable(ncfile, null, dist, "value");
      value.setDimensions((String)null);
      value.setDataType(DataType.SHORT);
      value.addAttribute( new Attribute("units", "RMS"));
      dist.addMemberVariable(value);
      Variable i0 = new Variable(ncfile, null, dist, "x_start");
      i0.setDimensions((String)null);
      i0.setDataType(DataType.SHORT);
      i0.addAttribute( new Attribute("units", "KM/4"));
      dist.addMemberVariable(i0);
      Variable j0 = new Variable(ncfile, null, dist, "y_start");
      j0.setDimensions((String)null);
      j0.setDataType(DataType.SHORT);
      j0.addAttribute( new Attribute("units", "KM/4"));
      dist.addMemberVariable(j0);
      Variable direct = new Variable(ncfile, null, dist, "direction");
      direct.setDimensions((String)null);
      direct.setDataType(DataType.SHORT);
      direct.addAttribute( new Attribute("units", "degree"));
      dist.addMemberVariable(direct);
      Variable speed = new Variable(ncfile, null, dist, "speed");
      speed.setDimensions((String)null);
      speed.setDataType(DataType.SHORT);
      speed.addAttribute( new Attribute("units", "knots"));
      dist.addMemberVariable(speed);

      int[] pos1 = new int[len];
      System.arraycopy(pos, 0, pos1, 0, len);
      dist.setSPobject( new Vinfo (0, 0, 0, 0, hoff, 0, isR, isZ, pos1, null, 4, 0));

      return 1;
    }

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
            out.println(" error reading nids product header");
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

      Dimension sDim = new Dimension("windBarbSize", len, true);
      ncfile.addDimension( null, sDim);
      dims.add( sDim);

      Structure dist = new Structure(ncfile, null, null, "vectorArrow");
      dist.setDimensions(dims);
      ncfile.addVariable(null, dist);
      dist.addAttribute( new Attribute("long_name", "Vector Arrow Data"));

      Variable i0 = new Variable(ncfile, null, dist, "x_start");
      i0.setDimensions((String)null);
      i0.setDataType(DataType.SHORT);
      i0.addAttribute( new Attribute("units", "KM/4"));
      dist.addMemberVariable(i0);
      Variable j0 = new Variable(ncfile, null, dist, "y_start");
      j0.setDimensions((String)null);
      j0.setDataType(DataType.SHORT);
      j0.addAttribute( new Attribute("units", "KM/4"));
      dist.addMemberVariable(j0);
      Variable direct = new Variable(ncfile, null, dist, "direction");
      direct.setDimensions((String)null);
      direct.setDataType(DataType.SHORT);
      direct.addAttribute( new Attribute("units", "degree"));
      dist.addMemberVariable(direct);
      Variable speed = new Variable(ncfile, null, dist, "arrowLength");
      speed.setDimensions((String)null);
      speed.setDataType(DataType.SHORT);
      speed.addAttribute( new Attribute("units", "pixels"));
      dist.addMemberVariable(speed);
      Variable speed1 = new Variable(ncfile, null, dist, "arrowHeadLength");
      speed1.setDimensions((String)null);
      speed1.setDataType(DataType.SHORT);
      speed1.addAttribute( new Attribute("units", "pixels"));
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

     int pcode_128( int[] pos, int[] size, int code, int hoff, int len, String structName, boolean isZ )
     {
        //int vlen = len;

        ArrayList dims =  new ArrayList();
        Dimension sDim = new Dimension("textStringSize", len, true);
        ncfile.addDimension( null, sDim);
        dims.add( sDim);

        Structure dist = new Structure(ncfile, null, null, structName);
        dist.setDimensions(dims);
        ncfile.addVariable(null, dist);
        dist.addAttribute( new Attribute("long_name", "text and special symbol for code "+code));

        if(code == 8){
            Variable strVal = new Variable(ncfile, null, dist, "strValue");
            strVal.setDimensions((String)null);
            strVal.setDataType(DataType.SHORT);
            strVal.addAttribute( new Attribute("units", ""));
            dist.addMemberVariable(strVal);
        }
        Variable i0 = new Variable(ncfile, null, dist, "x_start");
        i0.setDimensions((String)null);
        i0.setDataType(DataType.SHORT);
        i0.addAttribute( new Attribute("units", "KM/4"));
        dist.addMemberVariable(i0);
        Variable j0 = new Variable(ncfile, null, dist, "y_start");
        j0.setDimensions((String)null);
        j0.setDataType(DataType.SHORT);
        j0.addAttribute( new Attribute("units", "KM/4"));
        dist.addMemberVariable(j0);
        Variable tstr = new Variable(ncfile, null, dist, "textString");
        tstr.setDimensions((String)null);
        tstr.setDataType(DataType.STRING);
        tstr.addAttribute( new Attribute("units", ""));
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

        Dimension sDim = new Dimension("unlinkedVectorSize", vlen, true);
        ncfile.addDimension( null, sDim);
        dims.add( sDim);

        Structure dist = new Structure(ncfile, null, null, "unlinkedVectorStruct");
        dist.setDimensions(dims);
        ncfile.addVariable(null, dist);
        dist.addAttribute( new Attribute("long_name", "Unlinked Vector Packet"));

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
        Dimension jDim = new Dimension("Row", numY, true);
        Dimension iDim = new Dimension("Box", numX, true);

        dims.add( jDim);
        dims.add( iDim);

        Variable v = new Variable(ncfile, null, null, cname+"_"+slayer);
        v.setDataType(DataType.BYTE);
        v.setDimensions(dims);
        ncfile.addVariable(null, v);
        v.addAttribute( new Attribute("long_name", ctitle+" at Symbology Layer "+ slayer));
        v.setSPobject( new Vinfo (numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ, null, null, code, 0));
        v.addAttribute( new Attribute("units", "KM/4"));

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
        short rasp_i = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short rasp_j = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short rasp_xscale = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short rasp_xscalefract = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short rasp_yscale = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short rasp_yscalefract = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short num_rows = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short packing = (short)getInt(b2, 2);
        soff= 20;
        hedsiz = hedsiz + soff;

        int nlevel = code_levelslookup( pcode );
        int [] levels = getLevels(nlevel, threshold);

        //prod_info_size = (int) (num_rows * scale);
        out.println( "resp scale " + (int)rasp_xscale + " and " + (int)rasp_xscalefract+ " and " + (int)rasp_yscale+ " and " + (int)rasp_yscalefract );
        numY0 = rasp_j;
        numX0 = rasp_i;
        numX = num_rows;
        numY = num_rows;
        Dimension jDim = new Dimension("y", numY, true, false, false);
        Dimension iDim = new Dimension("x", numX, true, false, false);
        dims.add( jDim);
        dims.add( iDim);
        ncfile.addDimension( null, iDim);
        ncfile.addDimension( null, jDim);
        ncfile.addAttribute(null, new Attribute("cdm_data_type", "Grid"));

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
        v.addAttribute( new Attribute("long_name", ctitle));
        v.addAttribute( new Attribute("units", cunit));
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
        else if( cname.startsWith("BaseReflectivityComp")){
          addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                       cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }

        // create coordinate variables
        Variable xaxis = new Variable( ncfile, null, null, "x");
        xaxis.setDataType( DataType.DOUBLE);
        xaxis.setDimensions("x");
        xaxis.addAttribute( new Attribute("long_name", "projection x coordinate"));
        xaxis.addAttribute( new Attribute("units", "km/4"));
        xaxis.addAttribute( new Attribute(_Coordinate.AxisType, "GeoX"));
        double[] data1 = new double[numX];
        for (int i = 0; i < numX; i++)
          data1[i] = (double) (numX0 + i);
        Array dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] {numX}, data1);
        xaxis.setCachedData( dataA, false);
        ncfile.addVariable(null, xaxis);

        Variable yaxis = new Variable( ncfile, null, null, "y");
        yaxis.setDataType( DataType.DOUBLE);
        yaxis.setDimensions( "y");
        yaxis.addAttribute( new Attribute("long_name", "projection y coordinate"));
        yaxis.addAttribute( new Attribute("units", "km/4"));
        yaxis.addAttribute( new Attribute(_Coordinate.AxisType, "GeoY"));
        data1 = new double[numY];
        for (int i = 0; i < numY; i++)
          data1[i] = numY0 + i;
        dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] {numY}, data1);
        yaxis.setCachedData( dataA, false);
        ncfile.addVariable(null, yaxis);

        ProjectionImpl projection = new LambertConformal(latitude, longitude, latitude, latitude);
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
        short num_bin = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short radp_i = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short radp_j = (short)getInt(b2, 2);
        bos.get(b2, 0, 2);
        short radp_scale = (short)getInt(b2, 2);
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
        ncfile.addAttribute(null, new Attribute("cdm_data_type", "Radial"));
        Dimension radialDim = new Dimension("azimuth", num_radials, true);
        ncfile.addDimension( null, radialDim);

        Dimension binDim = new Dimension("gate", num_bin, true);
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
        // aziVar.addAttribute( new Attribute("long_name", "azimuth angle in degrees: 0 = true north, 90 = east"));
        // aziVar.addAttribute( new Attribute("units", "degrees"));
        // aziVar.setSPobject( new Vinfo (numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ, null, null, 0));

        // dims1 =  new ArrayList();
        // dims1.add(binDim);
        //Variable gateV = new Variable(ncfile, null, null, "gate1");
        //gateV.setDataType(DataType.FLOAT);
        //gateV.setDimensions(dims2);
        //ncfile.addVariable(null, gateV);
        //gateV.addAttribute( new Attribute("long_name", "radial distance to start of gate"));
        //gateV.addAttribute( new Attribute("units", "m"));
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
        addParameter(vName, lName, ncfile, dims1, att, DataType.DOUBLE, "seconds since 1970-01-01 00:00 UTC"
                    ,hoff, hedsiz, isZ, 0);

        Variable v = new Variable(ncfile, null, null, cname + "_RAW");
        v.setDataType(DataType.BYTE);
        v.setDimensions(dims);
        ncfile.addVariable(null, v);
        v.addAttribute( new Attribute("units", cunit));
        String coordinates = "elevation azimuth gate rays_time latitude longitude altitude";
        v.addAttribute( new Attribute(_Coordinate.Axes, coordinates));
        v.setSPobject( new Vinfo (numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ, null, null, 0, 0));

        //add RAW, BRIT variables for all radial variable
        levels = getLevels(nlevel, threshold);

        // addVariable(cname + "_Brightness", ctitle + " Brightness", ncfile, dims, coordinates, DataType.FLOAT,
        //                 cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);

        if( cname.startsWith("BaseReflectivity")){

          //addVariable(cname + "_VIP", ctitle + " VIP Level", ncfile, dims, coordinates, DataType.FLOAT,
          //             cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);

          addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                       cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }
        else if (cname.startsWith("RadialVelocity") || cname.startsWith("StormMeanVelocity") ) {

          addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                       cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }
        else if (cname.startsWith("Precip") ) {

           addVariable(cname, ctitle, ncfile, dims, coordinates, DataType.FLOAT,
                       cunit, hoff, hedsiz, isZ, nlevel, levels, iscale);
        }

        return soff;
    }

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

  void addVariable(String pName, String longName, NetcdfFile nc, ArrayList dims, String coordinates,
                    DataType dtype, String ut, long hoff, long hedsiz, boolean isZ, int nlevel, int[] levels, int iscale)
  {
      Variable v = new Variable(nc, null, null, pName);
      v.setDataType(dtype);
      v.setDimensions(dims);
      ncfile.addVariable(null, v);
      v.addAttribute( new Attribute("long_name", longName));
      v.addAttribute( new Attribute("units", ut));
      v.addAttribute( new Attribute(_Coordinate.Axes, coordinates));
      v.setSPobject( new Vinfo (numX, numX0, numY, numY0, hoff, hedsiz, isR, isZ, null, levels, iscale, nlevel));

  }

  void addParameter(String pName, String longName, NetcdfFile nc, ArrayList dims, Attribute att,
                    DataType dtype, String ut, long hoff, long doff, boolean isZ, int y0)
  {
      String vName = pName;
      Variable vVar = new Variable(nc, null, null, vName);
      vVar.setDataType(dtype);
      if( dims != null ) vVar.setDimensions(dims);
      else vVar.setDimensions("");
      if(att != null ) vVar.addAttribute(att);
      vVar.addAttribute( new Attribute("units", ut));
      vVar.addAttribute( new Attribute("long_name", longName));
      nc.addVariable(null, vVar);
      vVar.setSPobject( new Vinfo (numX, numX0, numY, y0, hoff, doff, isR, isZ, null, null, 0, 0));
  }

  String StnIdFromLatLon(float lat, float lon )
  {
    return "ID";
  }

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
    lat_min = latitude -  t1;
    lat_max = latitude + t1;
    lon_min = longitude + t1 * Math.cos(latitude);
    lon_max = longitude - t1 * Math.cos(latitude);
    startDate = getDate( volumeScanDate, volumeScanTime*1000);
    endDate = getDate( volumeScanDate, volumeScanTime*1000);

    if (prod_type == Base_Reflect) {
      radial               = 1;
      prod_elevation  = pinfo.p3;
      cmemo = "Base Reflct " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

      ctilt = pname_lookup(19, prod_elevation/10);
      ctitle = "BREF: Base Reflectivity";
      cunit = "dbZ";
      cname = "BaseReflectivity";
      summary = ctilt + " is a radial image of base reflectivity at tilt " + (prod_elevation/10 + 1) +  " and range 124 nm";

    } else if (prod_type == BaseReflect248) {
      radial               = 1;
      prod_elevation  = pinfo.p3;
      cmemo = "Base Reflct 248 " + prod_elevation/10 + " DEG " + cmode[pinfo.opmode];

      ctilt = pname_lookup(20, prod_elevation/10);
      ctitle = "BREF: 248 nm Base Reflectivity";
      cunit = "dbZ";
      cname = "BaseReflectivity248";
      summary = ctilt + " is a radial image of base reflectivity at tilt " + (prod_elevation/10 + 1) +  " and range 248 nm";
      t1 = 248.0 * 1.853 / 111.26;
      lat_min = latitude -  t1;
      lat_max = latitude + t1;
      lon_min = longitude + t1 * Math.cos(latitude);
      lon_max = longitude - t1 * Math.cos(latitude);
    } else if (prod_type == Comp_Reflect) {
      radial               = 0;
      prod_elevation  = -1;
      summary = "NCR is a raster image of composite reflectivity at range 124 nm";
      cmemo = "Composite Reflect " + cmode[pinfo.opmode];
      ctilt = pname_lookup(37, elevationNumber);
      ctitle = "CREF Composite Reflectivity" ;
      cunit = "dbZ" ;
      cname = "BaseReflectivityComp";

    } else if (prod_type == Layer_Reflect_Avg ||
             prod_type == Layer_Reflect_Max)   {
      radial               = 0;
      prod_elevation  = pinfo.p5;
      prod_top        = pinfo.p6;
      summary = "NCR is a raster image of composite reflectivity at range 124 nm";
      cmemo = "Layer Reflct " + prod_elevation + " - " + prod_top + cmode[pinfo.opmode];

      ctilt = pname_lookup(63, elevationNumber);
      ctitle = "LREF: Layer Composite Reflectivity" ;
      cunit = "dbZ" ;
      cname = "LayerCompReflect";
    } else if (prod_type == Echo_Tops) {
      radial               = 0;
      prod_elevation  = -1;
      summary = "NET is a raster image of echo tops at range 124 nm";
      cmemo = "Echo Tops [K FT] " + cmode[pinfo.opmode];
      ctilt = pname_lookup(41, elevationNumber);
      ctitle = "TOPS: Echo Tops";
      cunit = "K FT" ;
      cname = "EchoTop";
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
    } else if (prod_type == Precip_Accum) {
      radial               = 1;
      prod_elevation  = -1;
      startDate = getDate( pinfo.p5, pinfo.p6 * 60 * 1000);
      endDate = getDate( pinfo.p7, pinfo.p8 * 60 * 1000);
      summary = "NTP is a raster image of storm total rainfall accumulation at range 124 nm";
      cmemo = "Strm Tot Rain [IN] " + cmode[pinfo.opmode] ;
      ctilt = pname_lookup(80, elevationNumber);
      ctitle = "PRET: Surface Storm Total Rainfall" ;
      cunit = "IN" ;
      cname = "PrecipAccum";
     } else if (prod_type == Precip_Array) {
      radial          = 0;
      prod_elevation  = -1;
      summary = "DPA is a raster image of hourly digital precipitation array at range 124 nm";
      endDate = getDate( pinfo.p7, pinfo.p8 * 60 * 1000);
      cmemo = "Precip Array [IN] " + cmode[pinfo.opmode] ;
      ctilt = pname_lookup(81, elevationNumber);
      ctitle = "PRET: Hourly Digital Precipitation Array" ;
      cunit = "IN" ;
      cname = "PrecipArray";
    } else if (prod_type == Vert_Liquid) {
      radial               = 0;
      prod_elevation  = -1;
      summary = "NVL is a raster image of verticalintegrated liguid at range 124 nm";
      cmemo = "Vert Int Lq H2O [mm] " + cmode[pinfo.opmode] ;
      ctilt = pname_lookup(57, elevationNumber);
      ctitle =  "VIL: Vertically-integrated Liquid Water" ;
      cunit =  "kg/m^2" ;
      cname = "VertLiquid";
    } else if (prod_type == Velocity) {
      radial               = 1;
      prod_elevation  = pinfo.p3;
      prod_min        = pinfo.p4;
      prod_max        = pinfo.p5;
      ctilt = pname_lookup(27, prod_elevation/10);
      summary = ctilt + " is a radial image of base velocity at tilt " + (prod_elevation/10 + 1) +  " and  range 124 nm";
      cmemo = "Rad Vel "+ prod_elevation/10. + " DEG " + cmode[pinfo.opmode];
      ctitle = "VEL: Radial Velocity" ;
      cunit = "KT" ;
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
              " There are total 41 products, and " + summary ));
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
  /*
  ** Name:       read_dividlen
  **
  ** Purpose:    Read divider ID header from NEXRAD Level III product
  **
  */
  Sinfo read_dividlen( ByteBuffer buf, int offset  )
  {
      int off = offset;
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
      off = off + 10;

      return new Sinfo ( D_divider, D_id, block_length, number_layers);

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
      java.util.Date volumnDate = getDate( mdate, mtime*1000);
      String dstring = formatter.toDateTimeStringISO(volumnDate);
      //out.println( "product date is " + dstring);
      mlength = getInt(b4, 4);
      buf.get(b2, 0, 2);
      msource = (short) getInt(b2, 2);
      if(stationId == null) {
          try {
              NexradStationDB.init(); // make sure database is initialized
              NexradStationDB.Station station = NexradStationDB.getByIdNumber("000"+Short.toString(msource));
              if (station != null) {
                stationId = station.id;
              }
            } catch (IOException ioe) {
              log.error("NexradStationDB.init", ioe);
            }
      }
      buf.get(b2, 0, 2);
      mdestId = (short) getInt(b2, 2);
      buf.get(b2, 0, 2);
      mNumOfBlock = (short) getInt(b2, 2);

      return 1;

  }
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

  //public Vinfo getVarInfo( )
  //{
  //   return myInfo;
  //}
  public short convertunsignedByte2Short(byte b)
  {
     return (short)((b<0)? (short)b + 256 : (short)b);
  }

  public int convertShort2unsignedInt(short b)
  {
     return (b<0)? (-1)*b + 32768 : b;
  }

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
      ncfile.addAttribute(null, new Attribute("keywords", "WSR-88D; NIDS; N0R; N1R; N2R; N3R; N0V; N0S; N1S; N2S; NVL; NTP; N1P; N0Z; NET"));
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
          }
        } catch (IOException ioe) {
          log.error("NexradStationDB.init", ioe);
        }
      }

      ncfile.addAttribute(null, new Attribute("RadarLatitude", new Double(latitude)));
      ncfile.addAttribute(null, new Attribute("RadarLongitude", new Double(longitude)));
      ncfile.addAttribute(null, new Attribute("RadarAltitude", new Double(height)));

      buf.get(b2, 0, 2);
      pcode = (short)getInt(b2, 2);
      ncfile.addAttribute(null, new Attribute("ProductStation", stationId));
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
      for(int i = 0; i< 16; i++) {
        buf.get(b2, 0, 2);
        threshold[i] = (short)getInt(b2, 2);
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
      p10 = (short)getInt(b2, 2);
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

  // this converts a byte array to another primitive array
  protected Object convert( byte[] barray, DataType dataType, int nelems, int byteOrder) {

    if (dataType == DataType.BYTE) {
      return barray;
    }

    if (dataType == DataType.CHAR) {
      return convertByteToChar( barray);
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

  // this converts a byte array to a wrapped primitive (Byte, Short, Integer, Double, Float, Long)
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




    // convert byte array to char array
  protected char[] convertByteToChar( byte[] byteArray) {
    int size = byteArray.length;
    char[] cbuff = new char[size];
    for (int i=0; i<size; i++)
      cbuff[i] = (char) byteArray[i];
    return cbuff;
  }

   // convert char array to byte array
  protected byte[] convertCharToByte( char[] from) {
    int size = from.length;
    byte[] to = new byte[size];
    for (int i=0; i<size; i++)
      to[i] = (byte) from[i];
    return to;
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


  /*
  ** Name:       IsZlibed
  **
  ** Purpose:    Check a two-byte sequence to see if it indicates the start of
  **             a zlib-compressed buffer
  **
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
      String b = new String(buf);
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
        out.println(" No compressed data to inflate ");
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
            System.out.println("ERROR on inflation "+ex.getMessage());
            ex.printStackTrace();
            throw new IOException( ex.getMessage());
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
      byte   b1, b2;
      b1 = uncomp[0];
      b2 = uncomp[1];
      doff  = 2 * (((b1 & 63) << 8) + b2);

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
        Velocity, Velocity, Velocity, Velocity, Velocity, Other, Other,
        Other, Other, Other, Other, Other,                          /*  30- 39 */
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
        Other, Other, Other, Other, Other, Other,
        Other, Other, Other, Other, Other,                          /* 100-109 */
        Other, Other, Other, Other, Other,
      };


      if ( code < 0 || code > 109 )
        type     = Other;
      else
        type     = types[code];

      return type;

    }

    static String pname_lookup( int code, int elevation )
    {
      String pname = null;
      switch( code ){
          case 19:
              if(elevation == 0) pname = "N0R";
              else if (elevation == 1) pname = "N1R";
              else if (elevation == 2) pname = "N2R";
              else if (elevation == 3) pname = "N3R";
            break;
          case 20:
              pname = "N0Z";
            break;
          case 27:
              if(elevation == 0) pname = "N0V";
              else if (elevation == 1) pname = "N1V";
            break;
          case 37:
            pname = "NCR";
            break;
          case 41:
            pname = "NET";
            break;
          case 48:
            pname = "NVW";
          case 56:
              if(elevation == 0) pname = "N0S";
              else if (elevation == 1) pname = "N1S";
              else if (elevation == 2) pname = "N2S";
              else if (elevation == 3) pname = "N3S";
            break;
          case 57:
            pname = "NVL";
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

          default:
              break;

      }

      return pname;

    }
    static double code_reslookup( int code )
    {

      double data_res;
      final  double[] res = {
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /*   0-  9 */
        0,    0,    0,    0,    0,    0,    1,    2,    4,    1,    /*  10- 19 */
        2,    4, 0.25,  0.5,    1, 0.25,  0.5,    1,    0,    0,    /*  20- 29 */
        0,    0,    0,    0,    0,    1,    4,    1,    4,    0,    /*  30- 39 */
        0,    4,    0,    0,    0,    0,    0,    0,    0,    0,    /*  40- 49 */
        0,    0,    0,    0,    0,  0.5,    1,    4,    0,    0,    /*  50- 59 */
        0,    0,    0,    4,    4,    4,    4,    0,    0,    0,    /*  60- 69 */
        0,    0,    0,    0,    0,    0,    0,    0,    2,    2,    /*  70- 79 */
        2,    0,    0,    0,    0,    0,    0,    0,    0,    4,    /*  80- 89 */
        4,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /*  90- 99 */
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /* 100-109 */
      };


      if ( code < 0 || code > 109 )
        data_res = 0;
      else
        data_res = res[code];

      return data_res;

    }

    static int code_levelslookup( int code )
    {

      int level;
      final int[] levels = {
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /*   0-  9 */
        0,    0,    0,    0,    0,    0,    8,    8,    8,   16,    /*  10- 19 */
       16,   16,    8,    8,    8,   16,   16,   16,    0,    0,    /*  20- 29 */
        0,    0,    0,    0,    0,    8,    8,   16,   16,    0,    /*  30- 39 */
        0,   16,    0,    0,    0,    0,    0,    0,    0,    0,    /*  40- 49 */
        0,    0,    0,    0,    0,   16,   16,   16,    0,    0,    /*  50- 59 */
        0,    0,    0,    8,    8,    8,    8,    0,    0,    0,    /*  60- 69 */
        0,    0,    0,    0,    0,    0,    0,    0,   16,   16,    /*  70- 79 */
       16,    0,    0,    0,    0,    0,    0,    0,    0,    8,    /*  80- 89 */
        8,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /*  90- 99 */
        0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    /* 100-109 */
      };


      if ( code < 0 || code > 109 )
        level = 0;
      else
        level = levels[code];

      return level;

    }

  // Symbology block info for reading/writing
  class Sinfo {
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
  class Vinfo {
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
  class Pinfo {
    short divider, pcode, opmode, sequenceNumber, volumeScanNumber, volumeScanDate, productDate;
    double latitude, longitude;
    double height; // meters
    int volumeScanTime,  productTime;
    short p1, p2, p3, p4, p5, p6, p7, p8, p9, p10;
    short elevationNumber, numberOfMaps;
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
      this.latitude= latitude;
      this.longitude = longitude;
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
      this.elevationNumber = elevationNumber ;
      this.numberOfMaps = numberOfMaps ;
      this.offsetToSymbologyBlock = offsetToSymbologyBlock ;
      this.offsetToGraphicBlock = offsetToGraphicBlock ;
      this.offsetToTabularBlock = offsetToTabularBlock ;

    }
  }

}



/* Change History:
   $Log: Nidsheader.java,v $
   Revision 1.32  2006/07/11 17:55:48  yuanho
   changed variable name velocity to radialVelocity

   Revision 1.31  2006/06/28 21:35:56  yuanho
   changing  raster data product setting

   Revision 1.30  2006/06/05 22:37:05  yuanho
   changing  variable name (removing _ from all coordinate var)

   Revision 1.29  2006/05/30 22:53:27  yuanho
   changing Precipi product to radial type since it is radial image

   Revision 1.28  2006/05/30 21:52:09  yuanho
   adding calibrated radial data for precip which is radial image

   Revision 1.27  2006/05/12 20:19:28  caron
   dapper sequences
   nexrad station db
   Aggregation stride bug

   Revision 1.26  2006/04/19 20:22:33  yuanho
   radial dataset sweep for all radar dataset

   Revision 1.25  2006/03/28 19:57:01  caron
   remove DateUnit static methods - not thread safe
   bugs in ForecasstModelRun interactions with external indexer

   Revision 1.24  2005/12/16 20:40:48  yuanho
   fixing to read incomplete tabular data and VAD wind data

   Revision 1.23  2005/11/09 21:10:08  yuanho
   adding new pkcode api, but with no availbale test file

   Revision 1.22  2005/11/08 23:10:40  yuanho
   adding new pkcode api, but with no availbale test file

   Revision 1.21  2005/11/07 23:33:35  yuanho
   change to read variable size string

   Revision 1.20  2005/11/07 23:22:22  yuanho
   change to read variable size string

   Revision 1.19  2005/07/28 20:55:43  yuanho
   remove static for info classes, and remove some systemarraycopy during the uncomp.

   Revision 1.18  2005/07/25 23:27:32  yuanho
   fix compressed bug, the end of some nids files have uncompressed data.

   Revision 1.15  2005/07/20 17:42:08  yuanho
   fix elev. number bug

   Revision 1.14  2005/07/20 16:43:48  yuanho
   add n3r at elev. number 6

   Revision 1.13  2005/07/08 21:41:40  yuanho
   nids global atts added

   Revision 1.12  2005/05/11 00:10:05  caron
   refactor StuctureData, dt.point

   Revision 1.11  2005/04/27 16:22:55  caron
   infinite loop when inflater fails

   Revision 1.10  2005/02/18 01:14:57  caron
   no message

   Revision 1.9  2005/02/02 22:52:40  yuanho
   Graphic alphanumeric block fixed

   Revision 1.8  2005/01/20 21:34:34  yuanho
   adding reader to 18 nids products

   Revision 1.5  2004/12/08 21:46:17  yuanho
   read gate data

   Revision 1.4  2004/12/08 21:38:42  yuanho
   read gate data

   Revision 1.3  2004/12/08 20:41:32  caron
   no message

   Revision 1.2  2004/12/07 21:51:59  yuanho
   test nids code

   Revision 1.1  2004/12/07 21:51:41  yuanho
   test nids code

   Revision 1.5  2004/08/17 19:20:03  caron
   2.2 alpha (2)

   Revision 1.4  2004/08/16 20:53:45  caron
   2.2 alpha (2)

   Revision 1.3  2004/07/12 23:40:16  caron
   2.2 alpha 1.0 checkin

   Revision 1.2  2004/07/06 19:28:09  caron
   pre-alpha checkin

   Revision 1.1.1.1  2003/12/04 21:05:27  caron
   checkin 2.2

 */