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

/*
 * BufrIndicatorSection.java  1.0  01/09/2006
 * @author  Robb Kambic
 * @version 1.0
 *
 */

import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;

/**
 * Section 0 of BUFR data record
 */

public class BufrIndicatorSection {
  private long startPos;

  /**
   * Length in bytes of BUFR record.
   */
  private final int bufrLength;

  /*
   * Length in bytes of BufrIndicatorSection.
   * Currently only BUFR edition 3 supported - length is 8 octets/bytes.
   */
  //private final int length;

  /**
   * Edition of BUFR specification used.
   */
  private final int edition;

  // *** constructors *******************************************************

  /**
   * Constructs a <tt>BufrIndicatorSection</tt> object from a raf.
   *
   * @param raf RandomAccessFile with IndicatorSection content
   * @throws IOException on read error
   */
  public BufrIndicatorSection(RandomAccessFile raf) throws IOException {
    this.startPos = raf.getFilePointer() - 4; // start of BUFR message, including "BUFR"
    bufrLength = BufrNumbers.uint3(raf);
    edition = raf.read();
    //length = 8;
  }

  /**
   * Get the byte length of this BUFR record.
   *
   * @return length in bytes of BUFR record
   */
  public final int getBufrLength() {
    return bufrLength;
  }

  /*
   * Get the byte length of the IndicatorSection0 section.
   *
   * @return length in bytes of IndicatorSection0 section
   *
  public final int getLength() {
    return length;
  }  */

  /**
   * Get the edition of the BUFR specification used.
   *
   * @return edition number of BUFR specification
   */
  public final int getBufrEdition() {
    return edition;
  }

  /**
   * Get starting position in the file. This should point to the "BUFR" chars .
   *
   * @return byte offset in file of start of BUFR meessage.
   */
  public final long getStartPos() {
    return startPos;
  }
}
