/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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


package ucar.nc2.grib.grib1;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;

/**
 * The Indicator Section for GRIB-1 files
 *
 * @author caron
 */
@Immutable
public class Grib1SectionIndicator {
  static final byte[] MAGIC = new byte[]{'G','R','I','B'};

  private final long messageLength;
  private final long startPos;
  private final int messageLengthNotFixed;
  boolean isMessageLengthFixed=false;

  /**
   * Read Grib2SectionIndicator from raf.
   *
   * @param raf RandomAccessFile, with pointer at start (the "GRIB")
   * @throws java.io.IOException on I/O error
   * @throws IllegalArgumentException if not a GRIB-2 record
   */
  public Grib1SectionIndicator(RandomAccessFile raf) throws IOException {
    startPos = raf.getFilePointer();
    byte[] b = new byte[4];
    raf.readFully(b);
    for (int i = 0; i < b.length; i++)
      if (b[i] != MAGIC[i])
        throw new IllegalArgumentException("Not a GRIB record");

    messageLengthNotFixed = GribNumbers.uint3(raf);
    int edition = raf.read();
    if (edition != 1)
      throw new IllegalArgumentException("Not a GRIB-1 record");
    messageLength = Grib1RecordScanner.getFixedTotalLengthEcmwfLargeGrib(raf, messageLengthNotFixed);
    if(messageLength!=messageLengthNotFixed) {
    	isMessageLengthFixed = true;
    }
  }

  public Grib1SectionIndicator(long startPos, long messageLength) {
    this.startPos = startPos;
    this.messageLength = messageLength;
    this.messageLengthNotFixed = (int)messageLength;
    this.isMessageLengthFixed = false;
  }

  /**
   * Get the length of this GRIB record in bytes.
   *
   * @return length in bytes of GRIB record
   */
  public long getMessageLength() {
    return messageLength;
  }

  /**
   * Starting position of the entire GRIB message: the 'G' in GRIB
   * @return the starting position of the entire GRIB message
   */
  public long getStartPos() {
   return startPos;
 }

  /**
   * Ending position of the entire GRIB message: startPos + messageLength
   * @return the ending position of the entire GRIB message
   */
  public long getEndPos() {
   return startPos + messageLength;
 }

}

