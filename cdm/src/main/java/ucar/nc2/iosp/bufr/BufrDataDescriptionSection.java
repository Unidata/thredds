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
 * Data Description Section (4) of a BUFR record.
 */

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents the Data Description Section (4) of a BUFR record.
 */

public class BufrDataDescriptionSection {

  /**
   * Offset to start of BufrDataDescriptionSection.
   */
  private final long offset;

  /**
   * Number of data sets.
   */
  private final int ndatasets;

  /**
   * data type (observed or compressed).
   */
  private final int datatype;

  /**
   * List of data set descriptors.
   */
  private final List<Short> descriptors = new ArrayList<Short>();

  /**
   * Constructs a BufrDataDescriptionSection object by reading section 3 from a BUFR file.
   *
   * @param raf RandomAccessFile, position must be on a BUFR section 3
   * @throws IOException on read error
   */
  public BufrDataDescriptionSection(RandomAccessFile raf) throws IOException {
    offset = raf.getFilePointer();
    int length = BufrNumbers.uint3(raf);
    long EOS = offset + length;

    // reserved byte
    raf.read();

    // octets 5-6 number of datasets
    ndatasets = BufrNumbers.uint2(raf);

    // octet 7 data type bit 2 is for compressed data 192 or 64,
    // non-compressed data is 0 or 128
    datatype = raf.read();

    // get descriptors
    int ndesc = (length - 7) / 2;
    for (int i = 0; i < ndesc; i++) {
      int ch1 = raf.read();
      int ch2 = raf.read();
      short fxy = (short) ((ch1 << 8) + (ch2));
      descriptors.add(fxy);
    }

    // reset for any offset discrepancies
    raf.seek(EOS);
  }

  /**
   * Offset to the beginning of BufrDataDescriptionSection.
   *
   * @return offset in bytes of BUFR record
   */
  public final long getOffset() {
    return offset;
  }

  /**
   * Number of data sets in this record.
   *
   * @return datasets
   */
  public final int getNumberDatasets() {
    return ndatasets;
  }

  /**
   * Data type (compressed or non-compressed).
   *
   * @return datatype
   */
  public final int getDataType() {
    return datatype;
  }

  /**
   * Observation data
   *
   * @return true if observation data
   */
  public boolean isObserved() {
    return (datatype & 0x80) != 0;
  }

  /**
   * Is data compressed?
   *
   * @return true if data is compressed
   */
  public boolean isCompressed() {
    return (datatype & 0x40) != 0;
  }

  /**
   * get list of data descriptors as Shorts
   *
   * @return descriptors as List<Short>
   */
  public final List<Short> getDataDescriptors() {
    return descriptors;
  }

  /**
   * get list of data descriptors as Strings
   *
   * @return descriptors as List<String>
   */
  public final List<String> getDescriptors() {
    List<String> desc = new ArrayList<String>();
    for (short fxy : descriptors)
      desc.add(Descriptor.makeString(fxy));
    return desc;
  }
}
