/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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


