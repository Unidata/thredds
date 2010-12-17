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
/**
 * User: rkambic
 * Date: Jun 4, 2009
 * Time: 10:47:55 AM
 */

package ucar.grib.grib1;

import ucar.unidata.io.RandomAccessFile;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;


/**
 * A class that scans a GRIB file to split file according to Grid ID.
 */

public final class Grib1SplitByGridID {

  /*
  * raf for grib file
  */
  private ucar.unidata.io.RandomAccessFile raf = null;

  /*
  * the header of Grib record
  */
  private String header = "GRIB";

  public static String fileName;
  public static Map<String, DataOutputStream> gridFiles = new HashMap<String, DataOutputStream>();

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
  public Grib1SplitByGridID(RandomAccessFile raf) {
    this.raf = raf;
  }

  /**
   * scans a Grib file to gather information that could be used to
   * create an index or dump the metadata contents.
   *
   * @return boolean  if file read successful
   * @throws java.io.IOException if raf does not contain a valid GRIB record
   */
  public final boolean scan() throws IOException {
    Grib1ProductDefinitionSection pds = null;

    //System.out.println("file position =" + raf.getFilePointer());
    long start = raf.getFilePointer();
    while (raf.getFilePointer() < raf.length()) {
      start = raf.getFilePointer();
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
          System.out.println("Error Grib 2 record in Grib1 file");
          raf.seek(EOR);
          continue;
        }

        try { // catch all exceptions and seek to EOR

          // Read Section 1 Product Definition Section PDS
          pds = new Grib1ProductDefinitionSection(raf);
          if (pds.getLengthErr()) {
            raf.seek(EOR);
            continue;
          }
          String gridID = Integer.toString(pds.getGrid_ID());
          DataOutputStream dos = getOS(gridID);
          long size = EOR - start;//+1;
          byte[] data = new byte[(int) size];
          raf.seek(start);
          raf.read(data);
          dos.write(data, 0, data.length);
          dos.flush();
          raf.seek(EOR);

        } catch (Exception e) {
          //.println( "Caught Exception scannning record" );
          e.printStackTrace();
          raf.seek(EOR);
        }
      }  // end if seekHeader
    }  // end while raf.getFilePointer() < raf.length()
    // close all files.
    java.util.Set<String> ids = gridFiles.keySet();
    for (String id : ids) {
      DataOutputStream os = gridFiles.get(id);
      os.close();
    }
    return true;
  }  // end scan

  /**
   * @param raf  file to read
   * @param stop how far to search before giving up
   * @return boolean  if header found
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
   * @param gid Grid ID
   * @return DataOutputStream
   * @throws IOException _more_
   */
  private DataOutputStream getOS(String gid) throws IOException {
    DataOutputStream os = gridFiles.get(gid);
    if (os != null)
      return os;
    int idx = fileName.indexOf("20");
    String gFile = fileName.substring(0, idx) + "G" + gid + "_" + fileName.substring(idx);
    os = new DataOutputStream(new FileOutputStream(gFile));
    gridFiles.put(gid, os);
    return os;
  }

  /**
   * Splits a Grib1 file according to Grid IDs in the PDS section
   *
   * @param args filename
   * @throws IOException error when reading file
   */
  public static void main(String args[]) throws IOException {

    if (args.length == 1) {
      fileName = args[0];
    } else {
      System.out.println("No file name given");
      return;
    }
    RandomAccessFile raf = new RandomAccessFile(fileName, "r");
    Grib1SplitByGridID sbgid = new Grib1SplitByGridID(raf);
    sbgid.scan();
  }


}
