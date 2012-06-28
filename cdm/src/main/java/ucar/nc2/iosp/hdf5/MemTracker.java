package ucar.nc2.iosp.hdf5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 6/27/12
 */
public class MemTracker {
  private List<Mem> memList = new ArrayList<Mem>();

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
    f.format("Memory used file size= %d%n" + fileSize);
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

  class Mem implements Comparable {
    public String name;
    public long start, end;

    Mem(String name, long start, long end) {
      this.name = name;
      this.start = start;
      this.end = end;
    }

    public int compareTo(Object o1) {
      Mem m = (Mem) o1;
      return (int) (start - m.start);
    }

  }
}
