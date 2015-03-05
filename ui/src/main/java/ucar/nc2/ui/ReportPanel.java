package ucar.nc2.ui;

import thredds.inventory.*;
import thredds.inventory.MCollection;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Superclass for report panels
 *
 * @author caron
 * @since 8/22/13
 */
public abstract class ReportPanel extends JPanel {

  protected PreferencesExt prefs;
  protected TextHistoryPane reportPane;
  //protected JPanel buttPanel;

  protected ReportPanel(PreferencesExt prefs) {
    this.prefs = prefs;
    this.reportPane = new TextHistoryPane();
    setLayout(new BorderLayout());
    add(reportPane, BorderLayout.CENTER);
  }

  protected void addOptions(JPanel buttPanel) {

  }

  public void save() {
  }

  //public abstract void showInfo(Formatter f);

  public abstract Object[] getOptions();

  public void doReport(String spec, boolean useIndex, boolean eachFile, boolean extra, Object option) throws IOException {
    Formatter f = new Formatter();
    f.format("%s on %s useIndex=%s eachFile=%s extra=%s%n", option, spec, useIndex, eachFile, extra);

    try (MCollection dcm = getCollection(spec, f)) {
      if (dcm == null) {
        return;
      }

      f.format("top dir = %s%n", dcm.getRoot());
      reportPane.setText(f.toString());

      File top = new File(dcm.getRoot());
      if (!top.exists()) {
        f.format("top dir = %s does not exist%n", dcm.getRoot());
      } else {
        doReport(f, option, dcm, useIndex, eachFile, extra);
      }

      reportPane.setText(f.toString());
      reportPane.gotoTop();

    } catch (IOException ioe) {
      StringWriter sw = new StringWriter(50000);
      ioe.printStackTrace(new PrintWriter(sw));
      f.format(sw.toString());
      ioe.printStackTrace();
    }
  }

  protected abstract void doReport(Formatter f, Object option, MCollection dcm, boolean useIndex, boolean eachFile, boolean extra) throws IOException;

  protected boolean showCollection(String spec) {
    Formatter f = new Formatter();
    f.format("collection = %s%n", spec);
    boolean hasFiles = false;

    try (MCollection dcm = getCollection(spec, f)) {
      if (dcm == null) return false;

      try {
        for (MFile mfile : dcm.getFilesSorted()) {
          f.format(" %s%n", mfile.getPath());
          hasFiles = true;
        }
      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }

      reportPane.setText(f.toString());
      reportPane.gotoTop();
      return hasFiles;
    }
  }

  MCollection getCollection(String spec, Formatter f) {
    try {
      return CollectionAbstract.open(spec, spec, null, f);
    } catch (IOException e) {
      StringWriter sw = new StringWriter(10000);
      e.printStackTrace(new PrintWriter(sw));
      reportPane.setText(sw.toString());
      return null;
    }
  }

  ///////////////////////////////////////////////
  public static class Counters {
    List<Counter> counters = new ArrayList<>();
    Map<String, Counter> map = new HashMap<>();

    public void add(Counter c) {
      counters.add(c);
      map.put(c.getName(), c);
    }

    public void show (Formatter f) {
      for (Counter c : counters)
        c.show(f);
    }

    public void reset () {
      for (Counter c : counters)
        c.reset();
    }

    public void count(String name, int value) {
      CounterOfInt counter = (CounterOfInt) map.get(name);
      counter.count(value);
    }

    public void countS(String name, String value) {
      CounterOfString counter = (CounterOfString) map.get(name);
      counter.count(value);
    }

    public void addTo(Counters sub) {
      for (Counter subC : sub.counters) {
        Counter all = map.get(subC.getName());
        all.addTo(subC);
      }
    }

    public Counters makeSubCounters() {
      Counters result = new Counters();
      for (Counter c : counters) {
        if (c instanceof CounterOfInt)
          result.add( new CounterOfInt(c.getName()));
        else
          result.add( new CounterOfString(c.getName()));
      }
      return result;
    }

  }

  public static interface Counter {
    public void show(Formatter f);
    public String getName();
    public void addTo(Counter sub);
    public void reset();
  }


  // a counter whose keys are ints
  public static class CounterOfInt implements Counter {
    private Map<Integer, Integer> set = new HashMap<>();
    private String name;

    public String getName() {
      return name;
    }

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
  public static class CounterOfString implements Counter {
    private Map<String, Integer> set = new HashMap<>();
    private String name;

    public String getName() {
      return name;
    }

    public CounterOfString(String name) {
      this.name = name;
    }

    public void reset() {
      set = new HashMap<>();
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
      for (String key : list) {
        int count = set.get(key);
        f.format("   %10s: count = %d%n", key, count);
      }
    }

  }

}
