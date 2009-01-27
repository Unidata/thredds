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
package ucar.nc2.util;

import java.io.*;

/**
 * An unsynchronized version of a BufferedWriter, for performance.
 * @author com.elharo.io
 * @since Nov 4, 2007
 */
public class UnsynchronizedBufferedWriter extends Writer {

private final static int CAPACITY = 8192;

private char[]  buffer = new char[CAPACITY];
private int     position = 0;
private Writer  out;
private boolean closed = false;

public UnsynchronizedBufferedWriter(Writer out) {
  this.out = out;
}

public void write(char[] text, int offset, int length) throws IOException {
  checkClosed();
  while (length > 0) {
    int n = Math.min(CAPACITY - position, length);
    System.arraycopy(text, offset, buffer, position, n);
    position += n;
    offset += n;
    length -= n;
    if (position >= CAPACITY) flushInternal();
  }
}

public void write(Reader reader) throws IOException {
  checkClosed();
  while (true) {
    int n = reader.read(buffer);
    if (n < 0) break;
    out.write(buffer, 0, n);
  }
  out.flush();
}

public void write(String s) throws IOException {
   write(s, 0, s.length());
}

public void write(String s, int offset, int length) throws IOException {
  checkClosed();
  while (length > 0) {
    int n = Math.min(CAPACITY - position, length);
    s.getChars(offset, offset + n, buffer, position);
    position += n;
    offset += n;
    length -= n;
    if (position >= CAPACITY) flushInternal();
  }
}

public void write(int c) throws IOException {
  checkClosed();
  if (position >= CAPACITY) flushInternal();
  buffer[position] = (char) c;
  position++;
}

public void flush() throws IOException {
  flushInternal();
  out.flush();
}

private void flushInternal() throws IOException {
  if (position != 0) {
    out.write(buffer, 0, position);
    position = 0;
  }
}

public void close() throws IOException {
  closed = true;
  this.flush();
  out.close();
}

private void checkClosed() throws IOException {
  if (closed) throw new IOException("Writer is closed");
}
}


