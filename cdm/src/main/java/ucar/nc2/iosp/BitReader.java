/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp;

import ucar.unidata.io.RandomAccessFile;

import java.io.EOFException;
import java.io.IOException;

/**
 * Helper for reading data that has been bit packed.
 *
 * @author caron
 * @since Apr 7, 2008
 */
public class BitReader {

  private static final int BIT_LENGTH = Byte.SIZE;
  private static final int BYTE_BITMASK = 0xFF;
  private static final long LONG_BITMASK = Long.MAX_VALUE;

  private RandomAccessFile raf = null;
  private long startPos;

  private byte[] data;
  private int dataPos;

  private byte bitBuf = 0;
  private int bitPos = 0; // Current bit position in bitBuf.

  // for testing
  public BitReader(byte[] test) {
    this.data = test;
    this.dataPos = 0;
  }

  /**
   * Constructor
   *
   * @param raf      the RandomAccessFile
   * @param startPos points to start of data in data section, in bytes
   * @throws IOException on read error
   */
  public BitReader(RandomAccessFile raf, long startPos) throws IOException {
    this.raf = raf;
    this.startPos = startPos;
    raf.seek(startPos);
  }

  /**
   * Go to the next byte in the stream
   */
  public void incrByte() {
    this.bitPos = 0;
  }

  /**
   * Position file at bitOffset from startPos
   *
   * @param bitOffset bit offset from starting position
   * @throws IOException on io error
   */
  public void setBitOffset(int bitOffset) throws IOException {
    if (bitOffset % 8 == 0) {
      raf.seek(startPos + bitOffset / 8);
      bitPos = 0;
      bitBuf = 0;
    } else {
      raf.seek(startPos + bitOffset / 8);
      bitPos = 8 - (bitOffset % 8);
      bitBuf = (byte) raf.read();
      bitBuf &= 0xff >> (8 - bitPos);   // mask off consumed bits      
    }
  }

  public long getPos() throws IOException {
    if (raf != null) {
      return raf.getFilePointer();
    } else {
      return dataPos;
    }
  }

  /**
   * Read the next nb bits and return an Unsigned Long .
   *
   * @param nb the number of bits to convert to int, must be 0 <= nb <= 64.
   * @return result
   * @throws java.io.IOException on read error
   */
  public long bits2UInt(int nb) throws IOException {
    assert nb <= 64;
    assert nb >= 0;

    long result = 0;
    int bitsLeft = nb;

    while (bitsLeft > 0) {

      // we ran out of bits - fetch the next byte...
      if (bitPos == 0) {
        bitBuf = nextByte();
        bitPos = BIT_LENGTH;
      }

      // -- retrieve bit from current byte ----------
      // how many bits to read from the current byte
      int size = Math.min(bitsLeft, bitPos);
      // move my part to start
      int myBits = bitBuf >> (bitPos - size);
      // mask-off sign-extending
      myBits &= BYTE_BITMASK;
      // mask-off bits of next value
      myBits &= ~(BYTE_BITMASK << size);

      // -- put bit to result ----------------------
      // where to place myBits inside of result
      int shift = bitsLeft - size;
      assert shift >= 0;

      // put it there
      result |= myBits << shift;

      // -- put bit to result ----------------------
      // update information on what we consumed
      bitsLeft -= size;
      bitPos -= size;
    }

    return result;
  }

  /**
   * Read the next nb bits and return an Signed Long .
   *
   * @param nb the number of bits to convert to int, must be <= 64.
   * @return result
   * @throws java.io.IOException on read error
   */
  public long bits2SInt(int nb) throws IOException {

    long result = bits2UInt(nb);

    // check if we're negative
    if (getBit(result, nb)) {
      // it's negative! reset leading bit
      result = setBit(result, nb, false);
      // build 2's-complement
      result = ~result & LONG_BITMASK;
      result = result + 1;
    }

    return result;

  }

  private byte nextByte() throws IOException {
    if (raf != null) {
      int result = raf.read();
      if (result == -1)
        throw new EOFException();
      return (byte) result;
    } else {
      return data[dataPos++];
    }
  }

  public static long setBit(long decimal, int N, boolean value) {
    return value ? decimal | (1 << (N - 1)) : decimal & ~(1 << (N - 1));
  }

  public static boolean getBit(long decimal, int N) {
    int constant = 1 << (N - 1);
    return (decimal & constant) > 0;
  }

}
