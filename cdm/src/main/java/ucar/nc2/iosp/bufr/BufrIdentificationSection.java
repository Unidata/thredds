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
package ucar.nc2.iosp.bufr;

import ucar.unidata.io.RandomAccessFile;

import java.util.GregorianCalendar;
import java.util.Date;
import java.io.IOException;


/**
 * A class representing the IdentificationSection (section 1) of a BUFR record.
 * Handles editions 2,3,4.
 *
 * @author Robb Kambic
 * @author caron
 */

public class BufrIdentificationSection {

  /**
   * Master Table number.
   */
  private final int master_table;

  /**
   * Identification of subcenter .
   */
  private final int subcenter_id;

  /**
   * Identification of center.
   */
  private final int center_id;

  /**
   * Update Sequence Number.
   */
  private final int update_sequence;

  /**
   * Optional section exists.
   */
  private final boolean hasOptionalSection;
  private int optionalSectionLen;
  private long optionalSectionPos;

  /**
   * Data category.
   */
  private final int category;

  /**
   * Data sub category.
   */
  private final int subCategory;

  private final int localSubCategory; // edition >= 4

  /**
   * Table Version numbers.
   */
  private final int master_table_version;
  private final int local_table_version;

  /**
   * Time of the obs (nominal)
   */
  private final int year, month, day, hour, minute, second;

  private final byte[] localUse;

  // *** constructors *******************************************************

  /**
   * Constructs a <tt>BufrIdentificationSection</tt> object from a raf.
   *
   * @param raf RandomAccessFile with Section 1 content
   * @param is  the BufrIndicatorSection, needed for the bufr edition number
   * @throws IOException if raf contains no valid BUFR file
   */
  public BufrIdentificationSection(RandomAccessFile raf, BufrIndicatorSection is)
      throws IOException {

    // section 1 octet 1-3 (length of section)
    int length = BufrNumbers.int3(raf);

    //System.out.println( "IdentificationSection length=" + length );

    // master table octet 4
    master_table = raf.read();
    //System.out.println( "master tbl=" + master_table );

    if (is.getBufrEdition() < 4) {

      if (is.getBufrEdition() == 2) {
        subcenter_id = 255;
        // Center  octet 5-6
        center_id = BufrNumbers.int2(raf);

      } else { // edition 3
        // Center  octet 5
        subcenter_id = raf.read();
        //System.out.println( "subcenter_id=" + subcenter_id );
        // Center  octet 6
        center_id = raf.read();
        //System.out.println( "center_id=" + center_id );
      }

      // Update sequence number  octet 7
      update_sequence = raf.read();
      //System.out.println( "update=" + update );

      // Optional section octet 8
      int optional = raf.read();
      hasOptionalSection = (optional & 0x80) != 0;
      //System.out.println( "optional=" + optional );

      // Category  octet 9
      category = raf.read();
      //System.out.println( "category=" + category );

      // Category  octet 10
      subCategory = raf.read();
      //System.out.println( "subCategory=" + subCategory );

      localSubCategory = -1; // not used

      // master table version octet 11
      master_table_version = raf.read();
      //System.out.println( "master tbl_version=" + master_table_version );

      // local table version octet 12
      local_table_version = raf.read();
      //System.out.println( "local tbl_version=" + local_table_version );

      // octets 13-17 (reference time of forecast)
      int lyear = raf.read();
      if (lyear > 100)
        lyear -= 100;
      year = lyear + 2000;
      month = raf.read();
      day = raf.read();
      hour = raf.read();
      minute = raf.read();
      second = 0;

      int n = length - 17;
      localUse = new byte[n];
      raf.read(localUse);

    } else {  // BUFR Edition 4 and above are slightly different
      //    	 Center  octet 5 - 6
      center_id = BufrNumbers.int2(raf);

      // Sub Center  octet 7-8
      subcenter_id = BufrNumbers.int2(raf);

      //	    Update sequence number  octet 9
      update_sequence = raf.read();

      // Optional section octet 10
      int optional = raf.read();
      hasOptionalSection = (optional & 0x40) != 0;

      //	    Category  octet 11
      category = raf.read();

      // International Sub Category  octet 12
      subCategory = raf.read();

      // Local Sub Category Octet 13 - just read this for now
      localSubCategory = raf.read();

      //	    master table version octet 14
      master_table_version = raf.read();

      // local table version octet 15
      local_table_version = raf.read();
      //	    octets 16-22 (reference time of forecast)

      // Octet 16-17 is the 4-digit year
      year = BufrNumbers.int2(raf);
      month = raf.read();
      day = raf.read();
      hour = raf.read();
      minute = raf.read();
      second = raf.read();

      int n = length - 22;
      localUse = new byte[n];
      raf.read(localUse);
    }

    // skip optional section, but store position so can read if caller wants it
    if (hasOptionalSection) {
      int optionalLen = BufrNumbers.int3(raf);
      if (optionalLen % 2 != 0) optionalLen++;
      optionalSectionLen = optionalLen - 4;
      raf.skipBytes(1);
      optionalSectionPos = raf.getFilePointer();

      raf.skipBytes(optionalSectionLen);
    }
  }

  /**
   * Identification of center.
   *
   * @return center id as int
   */
  public final int getCenterId() {
    return center_id;
  }

  /**
   * Identification of subcenter.
   *
   * @return subcenter as int
   */
  public final int getSubCenterId() {
    return subcenter_id;
  }

  /**
   * Get update sequence.
   *
   * @return update_sequence
   */
  public final int getUpdateSequence() {
    return update_sequence;
  }

  /**
   * return record header time as a Date
   *
   * @param cal use this calendar to construct the date
   * @return referenceTime
   */
  public final Date getReferenceTime(GregorianCalendar cal) {
    cal.clear();
    cal.set(year, month-1, day, hour, minute, second);
    return cal.getTime();
  }

  public final int getCategory() {
    return category;
  }

  public final int getSubCategory() {
    return subCategory;
  }

  public final int getLocalSubCategory() {
    return localSubCategory;
  }

  public final int getMasterTableId() {
    return master_table;
  }

  public final int getMasterTableVersion() {
    return master_table_version;
  }

  public final int getLocalTableVersion() {
    return local_table_version;
  }

  /**
   * last bytes of the id section are "reserved for local use by ADP centers.
   *
   * @return local use bytes, if any.
   */
  public final byte[] getLocalUseBytes() {
    return localUse;
  }

  public final byte[] getOptionalSection(RandomAccessFile raf) throws IOException {
    if (!hasOptionalSection) return null;

    byte[] optionalSection = new byte[optionalSectionLen - 4];
    raf.seek(optionalSectionPos);
    raf.read(optionalSection);
    return optionalSection;
  }


}
