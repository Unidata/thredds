/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.bufr;

/*
 * Section 3 of BUFR data record
 * @author  Robb Kambic
 * @version 2.0
 */

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents the Data Description Section (4) of a BUFR record.
 * The descriptors are used to interpret the raw observation data.
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
