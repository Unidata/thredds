/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp.hdf5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Track use of space in an HDF5 file
 *
 * @author caron
 * @since 6/27/12
 */
class MemTracker {
  private List<Mem> memList = new ArrayList<>();

  private long fileSize;

  MemTracker(long fileSize) {
    this.fileSize = fileSize;
  }

  void add(String name, long start, long end) {
    memList.add(new Mem(name, start, end));
  }

  void addByLen(String name, long start, long size) {
    memList.add(new Mem(name, start, start + size));
  }

  void report(Formatter f) {
    f.format("Memory used file size= %d%n", fileSize);
    f.format("  start    end   size   name");
    Collections.sort(memList);
    Mem prev = null;
    for (Mem m : memList) {
      if ((prev != null) && (m.start > prev.end))
        f.format(" + %6d %6d %6d %6s%n", prev.end, m.start, m.start - prev.end, "HOLE");
      char c = ((prev != null) && (prev.end != m.start)) ? '*' : ' ';
      f.format(" %s %6d %6d %6d %6s%n", c, m.start, m.end, m.end - m.start, m.name);
      prev = m;
    }
    f.format("%n");
  }

  static class Mem implements Comparable<Mem> {
    public String name;
    public long start, end;

    Mem(String name, long start, long end) {
      this.name = name;
      this.start = start;
      this.end = end;
    }

    public int compareTo(Mem m) {
      return Long.compare(start, m.start);
    }

  }
}
