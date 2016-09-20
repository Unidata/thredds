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
 * The Data Representation section (5) for GRIB-2 files
 *
 * @author caron
 * @since 3/29/11
 */
@Immutable
public class Grib2SectionDataRepresentation {
  private final long startingPosition;
  private final int dataPoints;
  private final int dataTemplate;
  private final int length; // dont have length in index

  public Grib2SectionDataRepresentation(RandomAccessFile raf) throws IOException {
    startingPosition = raf.getFilePointer();

    // octets 1-4 (Length of DRS)
    length = GribNumbers.int4(raf);
    if (length == 0)
      throw new IllegalArgumentException("Not a GRIB-2 Data representation section");

   // octet 5
    int section = raf.read();
    if (section != 5)
      throw new IllegalArgumentException("Not a GRIB-2 Data representation section");

    // octets 6-9 number of datapoints
    dataPoints = GribNumbers.int4(raf);

    // octet 10
    int dt = GribNumbers.uint2(raf);
    dataTemplate = (dt == 40000) ? 40 : dt; // ?? NCEP bug ??

    raf.seek(startingPosition+length);
  }

  public Grib2SectionDataRepresentation(long startingPosition, int dataPoints, int dataTemplate) {
    this.startingPosition = startingPosition;
    this.dataPoints = dataPoints;
    this.dataTemplate = dataTemplate;
    this.length = 0;
  }

  /*
  Number of data points where one or more values are specified in Section 7 when a bit map
  is present, total number of data points when a bit map is absent.
   */
  public int getDataPoints() {
    return dataPoints;
  }

  public int getDataTemplate() {
    return dataTemplate;
  }

  public long getStartingPosition() {
    return startingPosition;
  }

  // debug
  public long getLength(RandomAccessFile raf) throws IOException {
    if (length == 0) {
      raf.seek(startingPosition);
      return GribNumbers.int4(raf);
    }
    return length;
  }

  public Grib2Drs getDrs(RandomAccessFile raf) throws IOException {
    raf.seek(startingPosition+11);
    return Grib2Drs.factory(dataTemplate, raf);
  }
}
