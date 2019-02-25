/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

import java.util.*;
import javax.annotation.Nullable;

/**
 * Count number of times a value appears.
 * value may be any Comparable;
 * equals() is used for uniqueness.
 *
 * @author caron
 * @since 11/15/2014
 */
public class Counters {
  List<Counter> counters = new ArrayList<>();
  Map<String, Counter> map = new HashMap<>();

  public Counter add(String name) {
    Counter c = new Counter(name);
    counters.add(c);
    map.put(name, c);
    return c;
  }

  public void reset() {
    for (Counter c : counters)
      c.reset();
  }

  public void show(Formatter f) {
    for (Counter c : counters)
      c.show(f);
  }

  public String toString() {
    Formatter f = new Formatter();
    show(f);
    return f.toString();
  }

  public Counter get(String name) {
    return map.get(name);
  }

  /**
   * Add value to the named counter.
   * Add counter if it doesnt already exist.
   * @return true if its a new value, not seen before.
   */
  public boolean count(String name, Comparable value) {
    Counter counter = map.get(name);
    if (counter == null) {
      counter = add(name);
    }
    return counter.count(value);
  }

  public void addTo(Counters sub) {
    for (Counter subC : sub.counters) {
      Counter all = map.get(subC.getName());
      if (all == null) {
        all = add(subC.getName());
      }
      all.addTo(subC);
    }
  }

  public Counters makeSubCounters() {
    Counters result = new Counters();
    for (Counter c : counters) {
      result.add(c.getName());
    }
    return result;
  }

  public static class Counter {
    private String name;
    private boolean showRange;
    private Comparable first, last;
    private Map<Comparable, Integer> set = new HashMap<>();
    private String range;

    public Counter(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public Counter setShowRange(boolean showRange) {
      this.showRange = showRange;
      return this;
    }

    public void reset() {
      set = new HashMap<>();
    }

    public boolean count(Comparable value) {
      Integer count = set.get(value);
      if (count == null) {
        set.put(value, 1);
        return true;
      } else {
        set.put(value, count + 1);
        return false;
      }
    }

    public void addTo(Counter sub) {
      for (Map.Entry<Comparable, Integer> entry : sub.set.entrySet()) {
        Integer count = this.set.get(entry.getKey());
        if (count == null)
          count = 0;
        set.put(entry.getKey(), count + entry.getValue());
      }
    }

    public int getUnique() {
      return set.size();
    }

    public Set<Comparable> getValues() {
      return set.keySet();
    }

    public Integer getCount(Comparable key) {
      return set.get(key);
    }

    public Comparable getFirst() {
      return first;
    }

    public Comparable getLast() {
      return last;
    }

    // get the value with greatest number of values.
    // if more than one, but all have same number, return null
    @Nullable
    public Comparable getMode() {
      if (set.size() == 1)
        return set.keySet().iterator().next(); // if only one, return it

      int max = -1;
      Comparable mode = null;
      boolean same = true; // are all keys the same ??
      Comparable testKey = null;
      for (Map.Entry<Comparable, Integer> entry : set.entrySet()) {
        Comparable entryKey = entry.getKey();
        if (testKey != null && entryKey.compareTo(testKey) != 0) same = false;
        testKey = entryKey;

        if (entry.getValue() > max) {
          max = entry.getValue();
          mode = entry.getKey();
        }
      }
      return same ? null : mode;
    }

    public int getTotal() {
      int total = 0;
      for (Map.Entry<Comparable, Integer> entry : set.entrySet()) {
        total += entry.getValue();
      }
      return total;
    }

    public void show(Formatter f) {
      java.util.List<Comparable> list = new ArrayList<>(set.keySet());
      f.format("%n%s (%d)%n", name, list.size());
      Collections.sort(list);

      if (showRange) {
        int n = list.size();
        if (n == 0)
          f.format("none%n");
        else
          f.format("   %10s - %10s: count = %d%n", list.get(0), list.get(n - 1), getUnique());

      } else {
        Comparable prev = null;
        for (Comparable key : list) {
          int count = set.get(key);
          boolean isHashDup = (prev != null) && key.hashCode() == prev.hashCode();
          boolean isNameDup = (prev != null) && key.toString().equals(prev.toString());
          f.format("  %s %10s: count = %d%n", isHashDup != isNameDup ? "*" : " ", key, count);
          prev = key;
        }
      }
    }

    public String showRange() {
      if (range == null) {
        java.util.List<Comparable> list = new ArrayList<>(set.keySet());
        Collections.sort(list);
        int n = list.size();
        if (n == 0)
          return "none";

        Formatter f = new Formatter();
        this.first = list.get(0);
        this.last = list.get(n - 1);
        f.format("%10s - %10s", first, last);
        range = f.toString();
      }
      return range;
    }
  }

}
