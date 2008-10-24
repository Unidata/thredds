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

  /**
   * Length in bytes of BufrIndicatorSection.
   * Currently only BUFR edition 3 supported - length is 8 octets/bytes.
   */
  private final int length;

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
    length = 8;
  }

  /**
   * Get the byte length of this BUFR record.
   *
   * @return length in bytes of BUFR record
   */
  public final int getBufrLength() {
    return bufrLength;
  }

  /**
   * Get the byte length of the IndicatorSection0 section.
   *
   * @return length in bytes of IndicatorSection0 section
   */
  public final int getLength() {
    return length;
  }

  /**
   * Get the edition of the BUFR specification used.
   *
   * @return edition number of BUFR specification
   */
  public final int getBufrEdition() {
    return edition;
  }

  /**
   * Get starting position in the file.
   *
   * @return byte offset in file of start of BUFR meessage.
   */
  public final long getStartPos() {
    return startPos;
  }
}
