/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.unidata.io;

import java.io.*;

/**
 * Similar to a DataInputStream that keeps track of position.
 * position must always increase, no going backwards.
 * cant handle byte order yet - assume big endian(?).
 * @author caron
 * @since Jan 3, 2008
 */
public class PositioningDataInputStream {
  private DataInputStream delegate;
  private long cpos = 0;

  public PositioningDataInputStream(InputStream is) {
    if (is instanceof DataInputStream)
      delegate = (DataInputStream) is;
    else
      delegate = new DataInputStream(is);
  }

  private void seek(long pos) throws IOException {
    if (pos < cpos) throw new IllegalArgumentException("Cannot go backwards; current="+cpos+" request="+pos);
    long want = pos-cpos;
    while (want > 0)
      want -= delegate.skip(want);
    cpos=pos;
    //System.out.println("now at "+pos);
  }

  public void read(long pos, byte dest[], int off, int len) throws IOException {
    seek(pos);
    delegate.readFully(dest, off, len);
    cpos += len;
  }

  public void readShort(long pos, short dest[], int off, int len) throws IOException {
    seek(pos);
    for (int i=0; i<len; i++)
      dest[off+i] = delegate.readShort();
    cpos += len * 2;
  }

  public void readInt(long pos, int dest[], int off, int len) throws IOException {
    seek(pos);
    for (int i=0; i<len; i++)
      dest[off+i] = delegate.readInt();
    cpos += len * 4;
  }

  public void readLong(long pos, long dest[], int off, int len) throws IOException {
    seek(pos);
    for (int i=0; i<len; i++)
      dest[off+i] = delegate.readLong();
    cpos += len * 8;
  }

  public void readFloat(long pos, float dest[], int off, int len) throws IOException {
    seek(pos);
    for (int i=0; i<len; i++)
      dest[off+i] = delegate.readFloat();
    cpos += len * 4;
  }

  public void readDouble(long pos, double dest[], int off, int len) throws IOException {
    seek(pos);
    for (int i=0; i<len; i++)
      dest[off+i] = delegate.readDouble();
    cpos += len * 8;
  }
}
