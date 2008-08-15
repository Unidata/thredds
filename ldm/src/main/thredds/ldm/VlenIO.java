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

package thredds.ldm;

import java.io.IOException;
import java.io.EOFException;

/**
 * Class Description.
 *
 * @author org.apache.lucene.store.IndexOutput
 * @author caron
 * @since Aug 14, 2008
 */
public abstract class VlenIO {

  abstract protected void write(byte b) throws IOException;
  abstract protected void write(byte[] b, int offset, int length) throws IOException;

  public int writeByte(byte b) throws IOException {
    write(b);
    return 1;
  }

  public int writeBytes(byte[] b, int offset, int length) throws IOException {
    write(b, offset, length);
    return length;
  }

  public int writeBytes(byte[] b) throws IOException {
    return writeBytes(b, 0, b.length);
  }

  // bigendian
  public final void writeInt(int v) throws IOException {
    write( int3(v));
    write( int2(v));
    write( int1(v));
    write( int0(v));
  }

  // bigendian
  public void writeShort(short v) throws IOException {
    write( short1(v));
    write( short0(v));
  }

  /**
   * Writes an int in a variable-length format.  Writes between one and five
   * bytes.  Smaller values take fewer bytes.  Negative numbers are not
   * supported.
   */
  public int writeVInt(int i) throws IOException {
    int count = 0;
    while ((i & ~0x7F) != 0) {
      writeByte((byte) ((i & 0x7f) | 0x80));
      i >>>= 7;
      count++;
    }
    writeByte((byte) i);
    return count + 1;
  }

  /**
   * Writes an long in a variable-length format.  Writes between one and nine(?)
   * bytes.  Smaller values take fewer bytes.  Negative numbers are not
   * supported.
   */
  public int writeVLong(long i) throws IOException {
    int count = 0;
    while ((i & ~0x7F) != 0) {
      writeByte((byte) ((i & 0x7f) | 0x80));
      i >>>= 7;
      count++;
    }
    writeByte((byte) i);
    return count + 1;
  }

  /**
   * Writes a string.
   *   (vlen) [char]
   */
  public int writeString(String s) throws IOException {
    int length = s.length();
    int count = writeVInt(length);
    count += writeChars(s, 0, length);
    return count;
  }

  /**
   * Writes a sequence of UTF-8 encoded characters from a string.
   *
   * @param s      the source of the characters
   * @param start  the first character in the sequence
   * @param length the number of characters in the sequence
   */
  public int writeChars(String s, int start, int length) throws IOException {
    final int end = start + length;
    int count = 0;
    for (int i = start; i < end; i++) {
      final int code = (int) s.charAt(i);
      if (code >= 0x01 && code <= 0x7F) {
        writeByte((byte) code);
        count++;
      } else if (((code >= 0x80) && (code <= 0x7FF)) || code == 0) {
        writeByte((byte) (0xC0 | (code >> 6)));
        writeByte((byte) (0x80 | (code & 0x3F)));
        count += 2;
      } else {
        writeByte((byte) (0xE0 | (code >>> 12)));
        writeByte((byte) (0x80 | ((code >> 6) & 0x3F)));
        writeByte((byte) (0x80 | (code & 0x3F)));
        count += 3;
      }
    }
    return count;
  }

  // from java.nio.Bits
  private static byte int3(int x) { return (byte)(x >> 24); }
  private static byte int2(int x) { return (byte)(x >> 16); }
  private static byte int1(int x) { return (byte)(x >>  8); }
  private static byte int0(int x) { return (byte)(x >>  0); }

  private static byte short1(short x) { return (byte)(x >> 8); }
  private static byte short0(short x) { return (byte)(x >> 0); }


  /////////////////////////////////////////////////

  abstract protected  byte readByte() throws IOException;
  //  return in.readByte();

  abstract protected  void readBytes(byte[] b) throws IOException;
  //  in.readFully(b, 0, b.length);

  public short readShort() throws IOException {
    byte b1 = readByte();
    byte b0 = readByte();
    return (short)((b1 << 8) | (b0 & 0xff));
  }

  public int readVInt() throws IOException {
    byte b = readByte();
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = readByte();
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  public long readVLong() throws IOException {
    byte b = readByte();
    long lval = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = readByte();
      lval |= (b & 0x7F) << shift;
    }
    return lval;
  }

  //private char[] chars;
  public String readString() throws IOException {
    int length = readVInt();
    // if (chars == null || length > chars.length)
    char[] chars = new char[length];
    readChars(chars, 0, length);
    return new String(chars, 0, length);
  }

  /**
   * Reads UTF-8 encoded characters into an array.
   *
   * @param buffer the array to read characters into
   * @param start  the offset in the array to start storing characters
   * @param length the number of characters to read
   */
  public void readChars(char[] buffer, int start, int length) throws IOException {
    final int end = start + length;
    for (int i = start; i < end; i++) {
      byte b = readByte();
      if ((b & 0x80) == 0)
        buffer[i] = (char) (b & 0x7F);
      else if ((b & 0xE0) != 0xE0) {
        buffer[i] = (char) (((b & 0x1F) << 6)
            | (readByte() & 0x3F));
      } else
        buffer[i] = (char) (((b & 0x0F) << 12)
            | ((readByte() & 0x3F) << 6)
            | (readByte() & 0x3F));
    }
  }
  

}
