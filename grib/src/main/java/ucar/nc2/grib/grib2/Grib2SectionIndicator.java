/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib2;

import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;

/**
 * The Indicator Section for GRIB-2 files
 *
 * @author caron
 * @since 3/28/11
 */
@Immutable
public class Grib2SectionIndicator {
  static final byte[] MAGIC = new byte[]{'G','R','I','B'};

  private final long messageLength;
  private final int discipline;
  private final long startPos;

  /**
   * Read Grib2SectionIndicator from raf.
   *
   * @param raf RandomAccessFile, with pointer at start (the "GRIB")
   * @throws java.io.IOException on I/O error
   * @throws IllegalArgumentException if not a GRIB-2 record
   */
  public Grib2SectionIndicator(RandomAccessFile raf) throws IOException {
    startPos = raf.getFilePointer();
    byte[] b = new byte[4];
    raf.readFully(b);
    for (int i = 0; i < b.length; i++)
      if (b[i] != MAGIC[i])
        throw new IllegalArgumentException("Not a GRIB record");

    raf.skipBytes(2);
    discipline = raf.read();
    int edition = raf.read();
    if (edition != 2)
      throw new IllegalArgumentException("Not a GRIB-2 record");

    messageLength = GribNumbers.int8(raf);
  }

  public Grib2SectionIndicator(long startPos, long messageLength, int discipline) {
    this.startPos = startPos;
    this.messageLength = messageLength;
    this.discipline = discipline;
  }

  /**
   * Get the length of this GRIB record in bytes.
   *
   * @return length in bytes of GRIB record
   */
  public long getMessageLength() {
    return messageLength;
  }

  public long getStartPos() {
   return startPos;
 }

  public long getEndPos() {
   return startPos + messageLength;
 }

  /**
   * Discipline - GRIB Master Table Number.
   *
   * @return discipline number
   */
  public int getDiscipline() {
    return discipline;
  }
}
