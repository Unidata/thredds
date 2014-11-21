package ucar.nc2.util;

import java.util.*;

/**
 * Count number of times a value appears.
 * value may be string or int.
 *
 * @author caron
 * @since 11/15/2014
 */
public class Counters {
  List<Counter> counters = new ArrayList<>();
  Map<String, Counter> map = new HashMap<>();

  public Counter add(String name) {
    CounterImpl c = new CounterImpl(name);
    counters.add( c);
    map.put(name, c);
    return c;
  }

  public void show(Formatter f) {
    for (Counter c : counters)
      c.show(f);
  }

  public Counter get(String name) {
    return map.get(name);
  }

  public void count(String name, Comparable value) {
    CounterImpl counter = (CounterImpl) map.get(name);
    counter.count(value);
  }

/*  public void countS(String name, String value) {
     CounterOfString counter = (CounterOfString) map.get(name);
     counter.count(value);
   }

  public void countO(String name, Comparable value) {
     CounterOfObject counter = (CounterOfObject) map.get(name);
     counter.count(value);
   }  */

     public void addTo(Counters sub) {
      for (Counter subC : sub.counters) {
        Counter all = map.get(subC.getName());
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

  public static interface Counter {
    public void show(Formatter f);

    public String showRange();

    public String getName();

    public void addTo(Counter sub);

    public int getUnique();

    public Comparable getFirst();

    public Comparable getLast();

    public int getTotal();

    public Counter setShowRange(boolean showRange);
  }

  private static class CounterImpl implements Counter {
    protected String name;
    protected boolean showRange;
    private Comparable first, last;

    public String getName() {
      return name;
    }

    public Counter setShowRange(boolean showRange) {
      this.showRange = showRange;
      return this;
    }

    private Map<Comparable, Integer> set = new HashMap<>();
    private String range;

    public CounterImpl(String name) {
      this.name = name;
    }

    public void count(Comparable value) {
      Integer count = set.get(value);
      if (count == null)
        set.put(value, 1);
      else
        set.put(value, count + 1);
    }

    public void addTo(Counter sub) {
      CounterImpl subs = (CounterImpl) sub;
      for (Map.Entry<Comparable, Integer> entry : subs.set.entrySet()) {
        Integer count = this.set.get(entry.getKey());
        if (count == null)
          count = 0;
        set.put(entry.getKey(), count + entry.getValue());
      }
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
        this.first =  list.get(0);
        this.last =  list.get(n-1);
        f.format("%10s - %10s", first, last);
        range = f.toString();
      }
      return range;
    }

    @Override
    public int getUnique() {
      return set.size();
    }

    @Override
    public Comparable getFirst() {
      return first;
    }

    @Override
    public Comparable getLast() {
      return last;
    }

    @Override
    public int getTotal() {
      int total = 0;
      for (Map.Entry<Comparable, Integer> entry : set.entrySet()) {
        total += entry.getValue();
      }
      return total;
    }
  }

  /* a counter whose keys are ints
  public static class CounterOfInt extends CounterAbstract {
    private Map<Integer, Integer> set = new HashMap<>();

    public CounterOfInt(String name) {
      this.name = name;
    }

    public void reset() {
      set = new HashMap<>();
    }

    public void count(int value) {
      Integer count = set.get(value);
      if (count == null)
        set.put(value, 1);
      else
        set.put(value, count + 1);
    }

    public void addTo(Counter sub) {
      CounterOfInt subi = (CounterOfInt) sub;
      for (Map.Entry<Integer, Integer> entry : subi.set.entrySet()) {
        Integer count = this.set.get(entry.getKey());
        if (count == null)
          count = 0;
        set.put(entry.getKey(), count + entry.getValue());
      }
    }

    @Override
    public int getUnique() {
      return set.size();
    }

    @Override
    public int getTotal() {
      int total = 0;
      for (Map.Entry<Integer, Integer> entry : set.entrySet()) {
        total += entry.getValue();
      }
      return total;
    }

    public void show(Formatter f) {
      f.format("%n%s%n", name);
      java.util.List<Integer> list = new ArrayList<>(set.keySet());
      Collections.sort(list);
      for (int template : list) {
        int count = set.get(template);
        f.format("   %3d: count = %d%n", template, count);
      }
    }
  }

  // a counter whose keys are strings
  public static class CounterOfString extends CounterAbstract {
    private Map<String, Integer> set = new HashMap<>();
    private String range;

    public String getName() {
      return name;
    }

    public CounterOfString(String name) {
      this.name = name;
    }

    public void count(String value) {
      Integer count = set.get(value);
      if (count == null)
        set.put(value, 1);
      else
        set.put(value, count + 1);
    }

    public void addTo(Counter sub) {
      CounterOfString subs = (CounterOfString) sub;
      for (Map.Entry<String, Integer> entry : subs.set.entrySet()) {
        Integer count = this.set.get(entry.getKey());
        if (count == null)
          count = 0;
        set.put(entry.getKey(), count + entry.getValue());
      }
    }

    public void show(Formatter f) {
      f.format("%n%s%n", name);
      java.util.List<String> list = new ArrayList<>(set.keySet());
      Collections.sort(list);
      if (showRange) {
        int n = list.size();
        f.format("   %10s - %10s: count = %d%n", list.get(0), list.get(n-1), getUnique());

      } else {
        for (String key : list) {
          int count = set.get(key);
          f.format("   %10s: count = %d%n", key, count);
        }
      }
    }

    public String showRange() {
      if (range == null) {
        java.util.List<String> list = new ArrayList<>(set.keySet());
        Collections.sort(list);
        int n = list.size();
        Formatter f = new Formatter();
        f.format("%10s - %10s", list.get(0), list.get(n - 1));
        range = f.toString();
      }
      return range;
    }

     @Override
    public int getUnique() {
      return set.size();
    }

    @Override
    public int getTotal() {
      int total = 0;
      for (Map.Entry<String, Integer> entry : set.entrySet()) {
        total += entry.getValue();
      }
      return total;
    }
  }

    // a counter whose keys are Comparable objects
  public static class CounterOfObject extends CounterAbstract {
    private Map<Comparable, Integer> set = new HashMap<>();
    private String range;

    public String getName() {
      return name;
    }

    public CounterOfObject(String name) {
      this.name = name;
    }

    public void count(Comparable value) {
      Integer count = set.get(value);
      if (count == null)
        set.put(value, 1);
      else
        set.put(value, count + 1);
    }

    public void addTo(Counter sub) {
      CounterOfObject subs = (CounterOfObject) sub;
      for (Map.Entry<Comparable, Integer> entry : subs.set.entrySet()) {
        Integer count = this.set.get(entry.getKey());
        if (count == null)
          count = 0;
        set.put(entry.getKey(), count + entry.getValue());
      }
    }

    public void show(Formatter f) {
      java.util.List<Comparable> list = new ArrayList<>(set.keySet());
      f.format("%n%s (%d)%n", name, list.size());
      Collections.sort(list);
      if (showRange) {
        int n = list.size();
        f.format("   %10s - %10s: count = %d%n", list.get(0), list.get(n-1), getUnique());

      } else {
        for (Object key : list) {
          int count = set.get(key);
          f.format("   %10s: count = %d%n", key, count);
        }
      }
    }

    public String showRange() {
      if (range == null) {
        java.util.List<Comparable> list = new ArrayList<>(set.keySet());
        Collections.sort(list);
        int n = list.size();
        Formatter f = new Formatter();
        f.format("%10s - %10s", list.get(0), list.get(n - 1));
        range = f.toString();
      }
      return range;
    }

     @Override
    public int getUnique() {
      return set.size();
    }

    @Override
    public int getTotal() {
      int total = 0;
      for (Map.Entry<Comparable, Integer> entry : set.entrySet()) {
        total += entry.getValue();
      }
      return total;
    }
  }  */
}
