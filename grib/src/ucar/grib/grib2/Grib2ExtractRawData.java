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


package ucar.grib.grib2;


import ucar.grib.NotSupportedException;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Grib2ExtractRawData.java  1.0  08/19/2008
 * @author Robb Kambic
 *
 */

/**
 * A class that scans a GRIB2 file stream to extract product information.
 * Either the first record of the Grib file is returned or all the records
 * matching a certain parameter are return. The parameter is designated by it's
 * Discipline number, Category number, and Parameter number.
 */

public final class Grib2ExtractRawData {

  /*
  *  /ucar/unidata/io/RandomAccessFile
  */
  private RandomAccessFile raf = null;

  /*
  * the WMO header of a record
  */
  private String header = "GRIB";

  /**
   * Pattern to extract header.
   */
  private static final Pattern productID =
      Pattern.compile("(\\w{6} \\w{4} \\d{6})");

  // *** constructors *******************************************************

  /**
   * Constructs a Grib2ExtractRawData object from a raf.
   *
   * @param raf with GRIB content
   */
  public Grib2ExtractRawData(RandomAccessFile raf) {
    this.raf = raf;
  }

  /**
   * scans the Grib2 file obtaining the first record or the data associated with
   * a parameter
   *
   * @return success in reading Grib file
   * @param discipline, parameter discipline number
   * @param category, parameter category number
   * @param number, parameter number
   * @param forecast extract these times
   * @throws NotSupportedException NotSupportedException
   * @throws IOException on data read
   */
  public final boolean scan(int discipline, int category, int number, int forecast)
      throws IOException {

    Grib2IndicatorSection is = null;
    Grib2IdentificationSection id = null;
    Grib2LocalUseSection lus = null;
    Grib2GridDefinitionSection gds = null;
    if (raf.getFilePointer() > 4) {
      raf.seek(raf.getFilePointer() - 4);
      Grib2EndSection es = new Grib2EndSection(raf);
      if (!es.getEndFound()) {  // ending found somewhere in file
        return false;
      }
    }
    long EOR = 0;
    long SOR = 0;
    boolean startAtHeader = true;  // otherwise skip to GDS
    boolean processGDS = true;
    Grib2ProductDefinitionSection pds = null;
    //DataOutputStream dos = new DataOutputStream(System.out);
    DataOutputStream dos = new DataOutputStream(
        new FileOutputStream( raf.getLocation() +".extract"));
    while (raf.getFilePointer() < raf.length()) {
      if (startAtHeader) {  // begining of record
        if (!seekHeader(raf, raf.length())) {
          //System.out.println( "Scan seekHeader failed" );
          return false;  // not valid Grib file
        }

        // Read Section 0 Indicator Section
        is = new Grib2IndicatorSection(raf);  // section 0
        //System.out.println( "Grib record length=" + is.getGribLength());
        // EOR (EndOfRecord) calculated so skipping data sections is faster
        EOR = raf.getFilePointer() + is.getGribLength()
            - is.getLength();
        SOR = raf.getFilePointer() - is.getLength();

        if (is.getGribEdition() == 1) {
          //System.out.println( "Error Grib 1 record in Grib2 file" ) ;
          raf.seek(EOR);
          continue;
        }
        if (discipline == -1 && forecast == -1 ) { // extract only 1st record
          raf.seek(SOR);
          byte[] oneRecord = new byte[(int) is.getGribLength()];
          raf.read(oneRecord);
          dos.write(oneRecord, 0, oneRecord.length);
          dos.flush();
          break;
        }

        // Read other SectionsGrib2
        id = new Grib2IdentificationSection(raf);  // Section 1

      }  // end startAtHeader

      try { // catch all exceptions and seek to EOR

        if (processGDS) {
          // check for Local Use Section 2
          lus = new Grib2LocalUseSection(raf);

          // Section 3
          gds = new Grib2GridDefinitionSection(raf, true);
          //System.out.println( "GDS length=" + gds.getLength() );

        }  // end processGDS

        pds = new Grib2ProductDefinitionSection(raf);  // Section 4
        Grib2PDSVariables pdsv = pds.getPdsVars();
        if (pdsv.getForecastTime() < forecast || (discipline == is.getDiscipline() &&
            category == pdsv.getParameterCategory() &&
            number == pdsv.getParameterNumber())) {
          raf.seek(SOR);
          byte[] oneRecord = new byte[(int) is.getGribLength()];
          raf.read(oneRecord);
          dos.write(oneRecord, 0, oneRecord.length);
          dos.flush();
        }

        raf.seek(EOR);
      } catch (Exception e) {
        //System.out.println( "Caught Exception scannning record" );
        e.printStackTrace();
        return true;
      }
    }  // end raf.getFilePointer() < raf.length()
    dos.close();
    return true;

  }  // end scan


  /**
   * Locates the String GRIB as the header in the record
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
   * Outputs first record of raw data to STDOUT
   * or parms based on discipline, category, and number
   *
   * @param args filename
   * @throws IOException on data reads
   */
  public static void main(String args[])
      throws IOException {

    String fileName;
    int discipline = -1, category = -1, number = -1, forecast = -1;
    if (args.length == 1) {
      fileName = args[0];
    } else if (args.length == 2) {
      fileName = args[0];
      forecast = Integer.parseInt(args[1]);

    } else if (args.length == 4) {
      fileName = args[0];
      discipline = Integer.parseInt(args[1]);
      category = Integer.parseInt(args[2]);
      number = Integer.parseInt(args[3]);

    } else {
      System.out.println("Not correct number of parms, either 1 or 4");
      return;
    }
    RandomAccessFile raf = new RandomAccessFile(fileName, "r");
    Grib2ExtractRawData erd = new Grib2ExtractRawData(raf);
    erd.scan(discipline, category, number, forecast);
  }

}  // end Grib2ExtractRawData


