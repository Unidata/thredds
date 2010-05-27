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

// $Id: Grib1ExtractRawData.java,v 1.33 2006/04/28 16:19:14 rkambic Exp $


package ucar.grib.grib1;


import ucar.grib.*;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.DataOutputStream;

/*
 * Grib1ExtractRawData.java  1.0  09/11/2008
 * @author Robb Kambic
 *
 */

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A class that scans a GRIB file to extract product information.
 */

public final class Grib1ExtractRawData {

  /*
  * raf for grib file
  */

  /**
   * _more_
   */
  private ucar.unidata.io.RandomAccessFile raf = null;

  /*
  * the header of Grib record
  */

  /**
   * _more_
   */
  private String header = "GRIB";

  public static boolean getParameter = false;

  /**
   * Pattern to extract header.
   */
  private static final Pattern productID =
      Pattern.compile("(\\w{6} \\w{4} \\d{6})");

  // *** constructors *******************************************************

  /**
   * Constructs a <tt>Grib1Input</tt> object from a raf.
   *
   * @param raf with GRIB content
   */
  public Grib1ExtractRawData(RandomAccessFile raf) {
    this.raf = raf;
  }

  /**
   * scans a Grib file to gather information that could be used to
   * create an index or dump the metadata contents.
   *
   * @param parm 
   * @throws NotSupportedException
   * @throws IOException           if raf does not contain a valid GRIB record
   */
  public final boolean scan(int[] parm)
      throws NotSupportedException, IOException {
    long start = System.currentTimeMillis();
    // stores the number of times a particular GDS is used
    HashMap gdsCounter = new HashMap();
    Grib1ProductDefinitionSection pds = null;
    Grib1GridDefinitionSection gds = null;
    long gdsOffset = 0;
    DataOutputStream dos = new DataOutputStream(System.out);

    //System.out.println("file position =" + raf.getFilePointer());
    long SOR = raf.getFilePointer();
    while (raf.getFilePointer() < raf.length()) {
      SOR = raf.getFilePointer();
      if (seekHeader(raf, raf.length())) {
        // Read Section 0 Indicator Section
        Grib1IndicatorSection is = new Grib1IndicatorSection(raf);
        //System.out.println( "Grib record length=" + is.getGribLength());
        // EOR (EndOfRecord) calculated so skipping data sections is faster
        long EOR = raf.getFilePointer() + is.getGribLength()
            - is.getLength();
        //long SOR = raf.getFilePointer() - is.getLength();

        // skip Grib 2 records in a Grib 1 file
        if (is.getGribEdition() == 2) {
          //System.out.println( "Error Grib 2 record in Grib1 file" ) ;
          raf.seek(EOR);
          continue;
        }

        if (parm[0] == -1) { // extract only 1st record
          raf.seek(SOR);
          byte[] oneRecord = new byte[(int) is.getGribLength()];
          raf.read(oneRecord);
          dos.write(oneRecord, 0, oneRecord.length);
          dos.flush();
          break;
        }
        long dataOffset = 0;
        try { // catch all exceptions and seek to EOR

          // Read Section 1 Product Definition Section PDS
          pds = new Grib1ProductDefinitionSection(raf);
          if (pds.getLengthErr()) {
            raf.seek(EOR);
            continue;
          }
          Grib1PDSVariables pdsv = pds.getPdsVars();
          if (getParameter && parm[0] == pdsv.getParameterNumber()) {
            raf.seek(SOR);
            byte[] oneRecord = new byte[(int) is.getGribLength()];
            raf.read(oneRecord);
            dos.write(oneRecord, 0, oneRecord.length);
            dos.flush();
          } else if ( parm[0] <= pdsv.getForecastTime() && parm[1] >= pdsv.getForecastTime()) {
            raf.seek(SOR);
            byte[] oneRecord = new byte[(int) is.getGribLength()];
            raf.read(oneRecord);
            dos.write(oneRecord, 0, oneRecord.length);
            dos.flush();
          }

          raf.seek(EOR);
          continue;

        } catch (Exception e) {
          //.println( "Caught Exception scannning record" );
          e.printStackTrace();
          raf.seek(EOR);
          continue;
        }
      }  // end if seekHeader
      //System.out.println( "raf.getFilePointer()=" + raf.getFilePointer());
      //System.out.println( "raf.length()=" + raf.length() );
    }  // end while raf.getFilePointer() < raf.length()
    //System.out.println("GribInput: processed in " +
    //   (System.currentTimeMillis()- start) + " milliseconds");
    dos.close();
    return true;
  }  // end scan

  /**
   * Grib edition number 1, 2 or 0 not a Grib file.
   *
   * @return int 0 not a Grib file, 1 Grib1, 2 Grib2
   * @throws NotSupportedException
   * @throws IOException
   */
  public final int getEdition() throws IOException, NotSupportedException {
    int check = 0;  // Not a valid Grib file
    long length = (raf.length() < 4000L)
        ? raf.length()
        : 4000L;
    if (!seekHeader(raf, length)) {
      return 0;  // not valid Grib file
    }
    //  Read Section 0 Indicator Section to get Edition number
    Grib1IndicatorSection is = new Grib1IndicatorSection(raf);  // section 0
    return is.getGribEdition();
  }  // end getEdition

  /**
   * _more_
   *
   * @param raf  _more_
   * @param stop _more_
   * @return _more_
   * @throws IOException _more_
   */
  private boolean seekHeader(RandomAccessFile raf, long stop)
      throws IOException {
    // seek header
    StringBuffer hdr = new StringBuffer();
    int match = 0;

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
   * Outputs first record of raw data to STDOUT
   * or parms based on discipline, category, and number
   *
   * @param args filename
   * @throws IOException
   */
  public static void main(String args[])
      throws IOException, NotSupportedException {

    String fileName;

    int[] number = new int[2];
    number[0] = -1;
    if (args.length == 1) {
      fileName = args[0];
    } else if (args.length == 2) {
      fileName = args[0];
      number[0] = Integer.parseInt(args[1]);
      getParameter = true;
    } else if (args.length == 3) {
      fileName = args[0];
      number[0] = Integer.parseInt(args[1]);
      number[1] = Integer.parseInt(args[2]);
      getParameter = false;

    } else {
      System.out.println("Not correct number of parms, either 1 or 2");
      return;
    }
    RandomAccessFile raf = new RandomAccessFile(fileName, "r");
    Grib1ExtractRawData erd = new Grib1ExtractRawData(raf);
    erd.scan(number);
  }

}  // end Grib1ExtractRawData


