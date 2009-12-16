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
import java.io.IOException;

/**
 * Helper for reading data that has been bit packed.
 *
 * @author caron
 * @since Apr 7, 2008
 */
public class BitReader {
  private RandomAccessFile raf;
  private long startPos;

  private byte[] test;
  private int testPos = 0;

  private int bitBuf = 0; // current byte
  private int bitPos = 0; // Current bit position in bitBuf.

  // for testing
  BitReader(byte[] test) {
    this.test = test;
    this.testPos = 0;
    this.bitBuf = 0;
    this.bitPos = 0;
  }

  /**
   * Constructor
   * @param raf the RandomAccessFile
   * @param startPos points to start of data in data section, in bytes
   * @throws IOException on read error
   */
  public BitReader( RandomAccessFile raf, long startPos) throws IOException {
    this.raf = raf;
    this.startPos = startPos;
    raf.seek(startPos);
  }

  private void setPos( int bitPos, int bitBuf) throws IOException {
    this.bitPos = bitPos;
    this.bitBuf = bitBuf;
  }

  /**
   * Position file at bitOffset from startPos
   * @param bitOffset bit offset from starting position
   * @throws IOException on io error
   */
  public void setBitOffset(int bitOffset) throws IOException {
    if (bitOffset % 8 == 0) {
      raf.seek(startPos + bitOffset/8);
      bitPos = 0;
      bitBuf = 0;

    } else {
      raf.seek(startPos + bitOffset/8);
      bitPos = 8 - (bitOffset % 8);
      bitBuf = raf.read();
      bitBuf &= 0xff >> (8 - bitPos);   // mask off consumed bits      
    }

    //     System.out.println("pos="+pos+" obs="+(pos  + bitOffset/8)+" bitPos="+bitPos+" bitBuf="+bitBuf);
  }

  private int nextByte() throws IOException {
    if (raf != null) return raf.read();
    return (int) test[testPos++];
  }

  /**
   * Read the next nb bits and return an Unsigned Long .
   *
   * @param nb the number of bits to convert to int, must be <= 64.
   * @return result
   * @throws java.io.IOException on read error
   */
  public long bits2UInt(int nb) throws IOException {

    int bitsLeft = nb;
    int result = 0;

    if (bitPos == 0) {
      bitBuf = nextByte();
      bitPos = 8;
    }

    while (true) {
      int shift = bitsLeft - bitPos;
      if (shift > 0) {
        // Consume the entire buffer
        result |= bitBuf << shift;
        bitsLeft -= bitPos;

        // Get the next byte from the RandomAccessFile
        bitBuf = nextByte();
        bitPos = 8;
      } else {
        // Consume a portion of the buffer
        result |= bitBuf >> -shift;
        bitPos -= bitsLeft;
        bitBuf &= 0xff >> (8 - bitPos);   // mask off consumed bits
        //System.out.println( "bitBuf = " + bitBuf );

        //System.out.println( "result = " + result );
        return result;
      }
    }
  }

  // debugging
  public long getPos() throws IOException { return raf.getFilePointer(); }

  static public void main( String args[]) throws IOException {
    BitReader bu = new BitReader(new byte[] {-1,2,4,8});
    bu.bits2UInt(7);
    bu.bits2UInt(1);
  }
}
