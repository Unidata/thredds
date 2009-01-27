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
/*
 *  This file is part of the HTTPClient package
 *  Copyright (C) 1996-2001 Ronald Tschalar
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free
 *  Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA 02111-1307, USA
 *
 *  For questions, suggestions, bug-reports, enhancement-requests etc.
 *  I may be contacted at:
 *
 *  ronald@innovation.ch
 *
 *  The HTTPClient's home page is located at:
 *
 *  http://www.innovation.ch/java/HTTPClient/
 */

package ucar.unidata.io;

import java.io.*;


/**
 * This class decompresses an input stream containing data compressed with
 * the unix "compress" utility (LZC, a LZW variant). This code is based
 * heavily on the <var>unlzw.c</var> code in <var>gzip-1.2.4</var> (written
 * by Peter Jannesen) and the original compress code.
 *
 *
 * <p> This version has been modified from the original 0.3-3 version by the
 *  Unidata Program Center (support@unidata.ucar.edu) to make the constructor
 *  public and to fix a couple of bugs.
 *  Also:
 *   - markSupported() returns false
 *   - add uncompress() static method
 *
 * @version	0.3-3 06/05/2001
 * @author	Ronald Tschalar
 * @author	Unidata Program Center
 */
public class UncompressInputStream extends FilterInputStream {

  // string table stuff
  private static final int TBL_CLEAR = 0x100;
  private static final int TBL_FIRST = TBL_CLEAR + 1;

  private int[] tab_prefix;
  private byte[] tab_suffix;
  private int[] zeros = new int[256];
  private byte[] stack;

  // various state
  private boolean block_mode;
  private int n_bits;
  private int maxbits;
  private int maxmaxcode;
  private int maxcode;
  private int bitmask;
  private int oldcode;
  private byte finchar;
  private int stackp;
  private int free_ent;

  // input buffer
  private byte[] data = new byte[10000];
  private int bit_pos = 0, end = 0, got = 0;
  private boolean eof = false;
  private static final int EXTRA = 64;


  /**
   * @param is the input stream to decompress
   * @throws IOException if the header is malformed
   */
  public UncompressInputStream(InputStream is) throws IOException {
    super(is);
    parse_header();
  }


  private byte[] one = new byte[1];

  @Override
  public synchronized int read() throws IOException {
    int b = read(one, 0, 1);
    if (b == 1)
      return (one[0] & 0xff);
    else
      return -1;
  }

  @Override
  public synchronized int read(byte[] buf, int off, int len)
      throws IOException {
    if (eof) return -1;
    int start = off;

/* Using local copies of various variables speeds things up by as
	 * much as 30% !
	 */
    int[] l_tab_prefix = tab_prefix;
    byte[] l_tab_suffix = tab_suffix;
    byte[] l_stack = stack;
    int l_n_bits = n_bits;
    int l_maxcode = maxcode;
    int l_maxmaxcode = maxmaxcode;
    int l_bitmask = bitmask;
    int l_oldcode = oldcode;
    byte l_finchar = finchar;
    int l_stackp = stackp;
    int l_free_ent = free_ent;
    byte[] l_data = data;
    int l_bit_pos = bit_pos;


// empty stack if stuff still left

    int s_size = l_stack.length - l_stackp;
    if (s_size > 0) {
      int num = (s_size >= len) ? len : s_size;
      System.arraycopy(l_stack, l_stackp, buf, off, num);
      off += num;
      len -= num;
      l_stackp += num;
    }

    if (len == 0) {
      stackp = l_stackp;
      return off - start;
    }


// loop, filling local buffer until enough data has been decompressed

    main_loop: do {
      if (end < EXTRA) fill();

      int bit_in = (got > 0) ? (end - end % l_n_bits) << 3 :
          (end << 3) - (l_n_bits - 1);

      while (l_bit_pos < bit_in) {
        // handle 1-byte reads correctly
        if (len == 0) {
          n_bits = l_n_bits;
          maxcode = l_maxcode;
          maxmaxcode = l_maxmaxcode;
          bitmask = l_bitmask;
          oldcode = l_oldcode;
          finchar = l_finchar;
          stackp = l_stackp;
          free_ent = l_free_ent;
          bit_pos = l_bit_pos;

          return off - start;
        }

        // check for code-width expansion

        if (l_free_ent > l_maxcode) {
          int n_bytes = l_n_bits << 3;
          l_bit_pos = (l_bit_pos - 1) +
              n_bytes - (l_bit_pos - 1 + n_bytes) % n_bytes;

          l_n_bits++;
          l_maxcode = (l_n_bits == maxbits) ? l_maxmaxcode :
              (1 << l_n_bits) - 1;

          if (debug)
            System.err.println("Code-width expanded to " + l_n_bits);

          l_bitmask = (1 << l_n_bits) - 1;
          l_bit_pos = resetbuf(l_bit_pos);
          continue main_loop;
        }


        // read next code

        int pos = l_bit_pos >> 3;
        int code = (((l_data[pos] & 0xFF) | ((l_data[pos + 1] & 0xFF) << 8) |
            ((l_data[pos + 2] & 0xFF) << 16))
            >> (l_bit_pos & 0x7)) & l_bitmask;
        l_bit_pos += l_n_bits;


        // handle first iteration

        if (l_oldcode == -1) {
          if (code >= 256)
            throw new IOException("corrupt input: " + code +
                " > 255");
          l_finchar = (byte) (l_oldcode = code);
          buf[off++] = l_finchar;
          len--;
          continue;
        }


        // handle CLEAR code

        if (code == TBL_CLEAR && block_mode) {
          System.arraycopy(zeros, 0, l_tab_prefix, 0, zeros.length);
          l_free_ent = TBL_FIRST - 1;

          int n_bytes = l_n_bits << 3;
          l_bit_pos = (l_bit_pos - 1) +
              n_bytes - (l_bit_pos - 1 + n_bytes) % n_bytes;
          l_n_bits = INIT_BITS;
          l_maxcode = (1 << l_n_bits) - 1;
          l_bitmask = l_maxcode;

          if (debug) System.err.println("Code tables reset");

          l_bit_pos = resetbuf(l_bit_pos);
          continue main_loop;
        }


        // setup

        int incode = code;
        l_stackp = l_stack.length;


        // Handle KwK case

        if (code >= l_free_ent) {
          if (code > l_free_ent)
            throw new IOException("corrupt input: code=" + code +
                ", free_ent=" + l_free_ent);

          l_stack[--l_stackp] = l_finchar;
          code = l_oldcode;
        }


        // Generate output characters in reverse order

        while (code >= 256) {
          l_stack[--l_stackp] = l_tab_suffix[code];
          code = l_tab_prefix[code];
        }
        l_finchar = l_tab_suffix[code];
        buf[off++] = l_finchar;
        len--;


        // And put them out in forward order

        s_size = l_stack.length - l_stackp;
        int num = (s_size >= len) ? len : s_size;
        System.arraycopy(l_stack, l_stackp, buf, off, num);
        off += num;
        len -= num;
        l_stackp += num;


        // generate new entry in table

        if (l_free_ent < l_maxmaxcode) {
          l_tab_prefix[l_free_ent] = l_oldcode;
          l_tab_suffix[l_free_ent] = l_finchar;
          l_free_ent++;
        }


        // Remember previous code

        l_oldcode = incode;


        // if output buffer full, then return

        if (len == 0) {
          n_bits = l_n_bits;
          maxcode = l_maxcode;
          bitmask = l_bitmask;
          oldcode = l_oldcode;
          finchar = l_finchar;
          stackp = l_stackp;
          free_ent = l_free_ent;
          bit_pos = l_bit_pos;

          return off - start;
        }
      }

      l_bit_pos = resetbuf(l_bit_pos);
    } while (got > 0);

    n_bits = l_n_bits;
    maxcode = l_maxcode;
    bitmask = l_bitmask;
    oldcode = l_oldcode;
    finchar = l_finchar;
    stackp = l_stackp;
    free_ent = l_free_ent;
    bit_pos = l_bit_pos;

    eof = true;
    return off - start;
  }


  // Moves the unread data in the buffer to the beginning and resets the pointers.
  // @return 0
  private int resetbuf(int bit_pos) {
    int pos = bit_pos >> 3;
    System.arraycopy(data, pos, data, 0, end - pos);
    end -= pos;
    return 0;
  }


  private void fill() throws IOException {
    got = in.read(data, end, data.length - 1 - end);
    if (got > 0) end += got;
  }


  @Override
  public synchronized long skip(long num) throws IOException {
    byte[] tmp = new byte[(int) num];
    int got = read(tmp, 0, (int) num);

    if (got > 0)
      return (long) got;
    else
      return 0L;
  }


  @Override
  public synchronized int available() throws IOException {
    if (eof) return 0;

    // Fred Hansen, 2008
    // the old code follows. it fails because read() can return bytes even after exhausting in.read()
    // return in.available();

    int avail = in.available(); 
    return (avail == 0) ? 1 : avail;
  }


  private static final int LZW_MAGIC = 0x1f9d;
  private static final int MAX_BITS = 16;
  private static final int INIT_BITS = 9;
  private static final int HDR_MAXBITS = 0x1f;
  private static final int HDR_EXTENDED = 0x20;
  private static final int HDR_FREE = 0x40;
  private static final int HDR_BLOCK_MODE = 0x80;

  private void parse_header() throws IOException {
// read in and check magic number

    int t = in.read();
    if (t < 0) throw new EOFException("Failed to read magic number");
    int magic = (t & 0xff) << 8;
    t = in.read();
    if (t < 0) throw new EOFException("Failed to read magic number");
    magic += t & 0xff;
    if (magic != LZW_MAGIC)
      throw new IOException("Input not in compress format (read " +
          "magic number 0x" +
          Integer.toHexString(magic) + ")");


// read in header byte

    int header = in.read();
    if (header < 0) throw new EOFException("Failed to read header");

    block_mode = (header & HDR_BLOCK_MODE) > 0;
    maxbits = header & HDR_MAXBITS;

    if (maxbits > MAX_BITS)
      throw new IOException("Stream compressed with " + maxbits +
          " bits, but can only handle " + MAX_BITS +
          " bits");

    if ((header & HDR_EXTENDED) > 0)
      throw new IOException("Header extension bit set");

    if ((header & HDR_FREE) > 0)
      throw new IOException("Header bit 6 set");

    if (debug) {
      System.err.println("block mode: " + block_mode);
      System.err.println("max bits:   " + maxbits);
    }


// initialize stuff

    maxmaxcode = 1 << maxbits;
    n_bits = INIT_BITS;
    maxcode = (1 << n_bits) - 1;
    bitmask = maxcode;
    oldcode = -1;
    finchar = 0;
    free_ent = block_mode ? TBL_FIRST : 256;

    tab_prefix = new int[1 << maxbits];
    tab_suffix = new byte[1 << maxbits];
    stack = new byte[1 << maxbits];
    stackp = stack.length;

    for (int idx = 255; idx >= 0; idx--)
      tab_suffix[idx] = (byte) idx;
  }

  /**
   * This stream does not support mark/reset on the stream.
   *
   * @return false
   */
  @Override
  public boolean markSupported() {
    return false;
  }

  static public void uncompress( String fileInName, FileOutputStream out) throws IOException {
    long start = System.currentTimeMillis();

    InputStream in = new UncompressInputStream(  new FileInputStream(fileInName));

    int total = 0;
    byte[] buffer = new byte[100000];
    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1) break;
      out.write(buffer, 0, bytesRead);
      total += bytesRead;
    }
    in.close();
    out.close();

    if (debugTiming) {
      long end = System.currentTimeMillis();
      System.err.println("Decompressed " + total + " bytes");
      System.err.println("Time: " + (end - start) / 1000. + " seconds");
    }
  }


  private static final boolean debug = false, debugTiming = false;

  /** test */
  public static void main(String args[]) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: UncompressInputStream <file>");
      System.exit(1);
    }

    InputStream in =
        new UncompressInputStream(new FileInputStream(args[0]));

    byte[] buf = new byte[100000];
    int tot = 0;
    long beg = System.currentTimeMillis();

    while (true) {
      int got = in.read(buf);
      if (got < 0) break;
      System.out.write(buf, 0, got);
      tot += got;
    }

    long end = System.currentTimeMillis();
    System.err.println("Decompressed " + tot + " bytes");
    System.err.println("Time: " + (end - beg) / 1000. + " seconds");
  }
}

