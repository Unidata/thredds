package ucar.nc2.ui;

import thredds.inventory.*;
import thredds.inventory.MCollection;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

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
      ByteArrayOutputStream bos = new ByteArrayOutputStream(50000);
      ioe.printStackTrace(new PrintStream(bos));
      f.format(bos.toString());
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
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      e.printStackTrace(new PrintStream(bos));
      reportPane.setText(bos.toString());
      return null;
    }
  }

  ///////////////////////////////////////////////

  // a counter whose keys are ints
  protected static class Counter {
    Map<Integer, Integer> set = new HashMap<>();
    String name;

    Counter(String name) {
      this.name = name;
    }

    void reset() {
      set = new HashMap<>();
    }

    void count(int value) {
      Integer count = set.get(value);
      if (count == null)
        set.put(value, 1);
      else
        set.put(value, count + 1);
    }

    void add(Counter sub) {
      for (int key : sub.set.keySet()) {
        Integer value = sub.set.get(key);
        Integer count = this.set.get(key);
        if (count == null)
          count = 0;
        set.put(key, count + value);
      }
    }

    void show(Formatter f) {
      f.format("%n%s%n", name);
      java.util.List<Integer> list = new ArrayList<Integer>(set.keySet());
      Collections.sort(list);
      for (int template : list) {
        int count = set.get(template);
        f.format("   %3d: count = %d%n", template, count);
      }
    }
  }


  // a counter whose keys are strings
  protected static class CounterS {
    Map<String, Integer> set = new HashMap<String, Integer>();
    String name;

    CounterS(String name) {
      this.name = name;
    }

    void count(String value) {
      Integer count = set.get(value);
      if (count == null)
        set.put(value, 1);
      else
        set.put(value, count + 1);
    }

    void show(Formatter f) {
      f.format("%n%s%n", name);
      java.util.List<String> list = new ArrayList<String>(set.keySet());
      Collections.sort(list);
      for (String key : list) {
        int count = set.get(key);
        f.format("   %10s: count = %d%n", key, count);
      }
    }

  }

}
