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

import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;

/**
 * Helper for reading data that has been bit packed.
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
   * @param raf
   * @param startPos pints to start of data in data section
   * @throws IOException
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
   * Read the next nb bits and return an Unsigned Int .
   *
   * @param nb the number of bits to convert to int, must be < 32.
   * @return result
   * @throws java.io.IOException on read error
   */
  public int bits2UInt(int nb) throws IOException {

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
