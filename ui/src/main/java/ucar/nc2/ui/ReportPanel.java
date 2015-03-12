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
      MCollection org =  CollectionAbstract.open(spec, spec, null, f);
      CollectionFiltered filteredCollection = new CollectionFiltered("GribReportPanel", org, new MFileFilter() {
        public boolean accept(MFile mfile) {
          String suffix = mfile.getName();
          if (suffix.contains(".ncx")) return false;
          if (suffix.contains(".gbx")) return false;
          return true;
        }
      });
      return filteredCollection;

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

/*

summary on Q:/cdmUnitTest/formats/grib1/.*$ useIndex=true eachFile=true extra=false
top dir = Q:/cdmUnitTest/formats/grib1
 Q:/cdmUnitTest/formats/grib1/007.060.100.001.00.0.1993010100.000.GRIB

referenceDate
   1993-01-01T00:00:00Z: count = 1

table version
      131-0-2: count = 1

param
   100: count = 1

timeUnit
     0: count = 1

vertCoord
     1: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/05072712_gfsK.grb

referenceDate
   2005-07-27T12:00:00Z: count = 845

table version
        7-0-2: count = 845

param
     1: count = 33
     2: count = 11
     7: count = 154
    11: count = 154
    33: count = 154
    34: count = 154
    39: count = 66
    41: count = 11
    52: count = 88
    61: count = 10
    63: count = 10

timeUnit
     4: count = 20
    10: count = 825

vertCoord
     1: count = 31
     6: count = 55
     7: count = 55
   100: count = 649
   102: count = 11
   105: count = 44

earthShape
     0: count = 845

uvIsReletive
        false: count = 845

vertCoordInGDS

predefined

thin
     0: count = 845
 Q:/cdmUnitTest/formats/grib1/07010418_arw_d01.GrbF01500

referenceDate
   2007-01-04T18:00:00Z: count = 788

table version
      7-0-129: count = 52
      7-0-130: count = 10
        7-0-2: count = 726

param
     1: count = 12
     2: count = 1
     7: count = 48
    11: count = 50
    17: count = 43
    20: count = 1
    24: count = 1
    33: count = 48
    34: count = 48
    35: count = 42
    39: count = 42
    41: count = 9
    51: count = 44
    52: count = 49
    54: count = 1
    57: count = 1
    58: count = 42
    59: count = 1
    61: count = 1
    62: count = 1
    63: count = 1
    65: count = 2
    71: count = 1
    72: count = 1
    81: count = 1
    83: count = 1
    84: count = 1
    85: count = 5
    86: count = 1
    87: count = 1
    91: count = 1
   121: count = 1
   122: count = 1
   124: count = 1
   125: count = 1
   130: count = 1
   131: count = 1
   132: count = 1
   134: count = 1
   135: count = 2
   136: count = 1
   137: count = 1
   138: count = 1
   139: count = 1
   140: count = 2
   141: count = 2
   142: count = 1
   143: count = 1
   144: count = 4
   145: count = 1
   153: count = 42
   155: count = 1
   156: count = 4
   157: count = 4
   158: count = 42
   170: count = 42
   171: count = 43
   180: count = 1
   181: count = 1
   190: count = 2
   196: count = 1
   197: count = 1
   207: count = 1
   208: count = 1
   211: count = 46
   212: count = 3
   214: count = 1
   216: count = 2
   219: count = 1
   221: count = 1
   223: count = 1
   224: count = 1
   225: count = 1
   226: count = 1
   230: count = 1
   231: count = 1
   238: count = 1
   240: count = 1
   241: count = 2
   242: count = 2
   246: count = 1
   247: count = 1
   248: count = 1
   249: count = 1
   250: count = 2
   251: count = 2
   252: count = 1
   253: count = 1

timeUnit
     0: count = 777
     3: count = 6
     4: count = 5

vertCoord
     1: count = 54
     2: count = 2
     3: count = 3
     4: count = 1
     7: count = 1
   100: count = 639
   101: count = 1
   102: count = 2
   105: count = 8
   106: count = 4
   109: count = 13
   111: count = 1
   112: count = 10
   116: count = 29
   200: count = 10
   204: count = 1
   206: count = 1
   207: count = 1
   242: count = 1
   243: count = 1
   245: count = 1
   248: count = 1
   249: count = 1
   251: count = 1
   252: count = 1

earthShape
     0: count = 788

uvIsReletive
        false: count = 788

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/07050400_gfsK.grb

referenceDate
   2007-05-04T00:00:00Z: count = 869

table version
        7-0-2: count = 869

param
     1: count = 33
     2: count = 11
     7: count = 174
    11: count = 154
    33: count = 154
    34: count = 154
    39: count = 66
    41: count = 11
    52: count = 88
    61: count = 14
    63: count = 10

timeUnit
     4: count = 24
    10: count = 845

vertCoord
     1: count = 35
     6: count = 55
     7: count = 55
   100: count = 669
   102: count = 11
   105: count = 44

earthShape
     0: count = 869

uvIsReletive
        false: count = 869

vertCoordInGDS

predefined

thin
     0: count = 869
 Q:/cdmUnitTest/formats/grib1/07111906_nmm.GrbF00000

referenceDate
   2007-11-19T06:00:00Z: count = 792

table version
      7-0-129: count = 56
      7-0-130: count = 11
        7-0-2: count = 725

param
     1: count = 12
     2: count = 1
     7: count = 49
    11: count = 50
    17: count = 43
    20: count = 2
    24: count = 1
    33: count = 48
    34: count = 48
    35: count = 42
    39: count = 42
    41: count = 9
    51: count = 44
    52: count = 49
    54: count = 1
    58: count = 42
    59: count = 1
    65: count = 1
    71: count = 1
    72: count = 1
    73: count = 1
    74: count = 1
    75: count = 1
    81: count = 1
    83: count = 1
    84: count = 1
    85: count = 5
    86: count = 1
    87: count = 1
    91: count = 1
   118: count = 1
   121: count = 1
   122: count = 1
   124: count = 1
   125: count = 1
   130: count = 1
   131: count = 1
   132: count = 1
   134: count = 1
   135: count = 2
   136: count = 2
   137: count = 1
   138: count = 1
   139: count = 1
   140: count = 2
   141: count = 2
   142: count = 1
   143: count = 1
   144: count = 4
   145: count = 1
   153: count = 42
   155: count = 1
   156: count = 4
   157: count = 4
   158: count = 42
   161: count = 1
   170: count = 42
   171: count = 43
   180: count = 1
   181: count = 1
   190: count = 2
   196: count = 1
   197: count = 1
   203: count = 2
   204: count = 2
   205: count = 2
   206: count = 1
   207: count = 1
   208: count = 1
   211: count = 45
   212: count = 2
   214: count = 1
   216: count = 2
   219: count = 1
   221: count = 1
   223: count = 1
   224: count = 1
   225: count = 1
   226: count = 1
   230: count = 1
   231: count = 1
   238: count = 1
   240: count = 1
   246: count = 1
   247: count = 1
   248: count = 1
   249: count = 1
   250: count = 2
   251: count = 2
   252: count = 1
   253: count = 1

timeUnit
     0: count = 792

vertCoord
     1: count = 55
     2: count = 3
     3: count = 4
     4: count = 1
     7: count = 1
   100: count = 639
   101: count = 1
   102: count = 2
   105: count = 8
   106: count = 5
   109: count = 9
   111: count = 1
   112: count = 10
   116: count = 29
   200: count = 10
   204: count = 1
   206: count = 1
   207: count = 1
   214: count = 1
   215: count = 1
   224: count = 1
   234: count = 1
   242: count = 1
   243: count = 1
   245: count = 1
   248: count = 1
   249: count = 1
   251: count = 1
   252: count = 1

earthShape
     0: count = 792

uvIsReletive
        false: count = 792

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/07112006_nmm.GrbF00000

referenceDate
   2007-11-20T06:00:00Z: count = 792

table version
      7-0-129: count = 56
      7-0-130: count = 11
        7-0-2: count = 725

param
     1: count = 12
     2: count = 1
     7: count = 49
    11: count = 50
    17: count = 43
    20: count = 2
    24: count = 1
    33: count = 48
    34: count = 48
    35: count = 42
    39: count = 42
    41: count = 9
    51: count = 44
    52: count = 49
    54: count = 1
    58: count = 42
    59: count = 1
    65: count = 1
    71: count = 1
    72: count = 1
    73: count = 1
    74: count = 1
    75: count = 1
    81: count = 1
    83: count = 1
    84: count = 1
    85: count = 5
    86: count = 1
    87: count = 1
    91: count = 1
   118: count = 1
   121: count = 1
   122: count = 1
   124: count = 1
   125: count = 1
   130: count = 1
   131: count = 1
   132: count = 1
   134: count = 1
   135: count = 2
   136: count = 2
   137: count = 1
   138: count = 1
   139: count = 1
   140: count = 2
   141: count = 2
   142: count = 1
   143: count = 1
   144: count = 4
   145: count = 1
   153: count = 42
   155: count = 1
   156: count = 4
   157: count = 4
   158: count = 42
   161: count = 1
   170: count = 42
   171: count = 43
   180: count = 1
   181: count = 1
   190: count = 2
   196: count = 1
   197: count = 1
   203: count = 2
   204: count = 2
   205: count = 2
   206: count = 1
   207: count = 1
   208: count = 1
   211: count = 45
   212: count = 2
   214: count = 1
   216: count = 2
   219: count = 1
   221: count = 1
   223: count = 1
   224: count = 1
   225: count = 1
   226: count = 1
   230: count = 1
   231: count = 1
   238: count = 1
   240: count = 1
   246: count = 1
   247: count = 1
   248: count = 1
   249: count = 1
   250: count = 2
   251: count = 2
   252: count = 1
   253: count = 1

timeUnit
     0: count = 792

vertCoord
     1: count = 55
     2: count = 3
     3: count = 4
     4: count = 1
     7: count = 1
   100: count = 639
   101: count = 1
   102: count = 2
   105: count = 8
   106: count = 5
   109: count = 9
   111: count = 1
   112: count = 10
   116: count = 29
   200: count = 10
   204: count = 1
   206: count = 1
   207: count = 1
   214: count = 1
   215: count = 1
   224: count = 1
   234: count = 1
   242: count = 1
   243: count = 1
   245: count = 1
   248: count = 1
   249: count = 1
   251: count = 1
   252: count = 1

earthShape
     0: count = 792

uvIsReletive
        false: count = 792

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/191.2004346.1500.0.255.grib

referenceDate
   2004-12-11T15:00:00Z: count = 60

table version
        7-8-2: count = 60

param
   186: count = 30
   236: count = 30

timeUnit
     1: count = 60

vertCoord
   103: count = 60

earthShape
     0: count = 60

uvIsReletive
        false: count = 60

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/19970101000000.grb

referenceDate
   1997-01-01T00:00:00Z: count = 86

table version
      58-42-2: count = 86

param
     2: count = 1
     7: count = 6
    11: count = 7
    17: count = 1
    20: count = 1
    33: count = 7
    34: count = 7
    52: count = 6
    60: count = 1
    61: count = 1
    66: count = 1
    71: count = 1
    73: count = 1
    74: count = 1
    75: count = 1
   133: count = 1
   142: count = 1
   144: count = 1
   146: count = 1
   147: count = 1
   151: count = 1
   161: count = 1
   162: count = 1
   170: count = 1
   186: count = 1
   187: count = 1
   191: count = 1
   200: count = 1
   208: count = 1
   240: count = 1
   241: count = 1
   242: count = 1
   243: count = 1
   244: count = 1
   245: count = 1
   246: count = 1
   247: count = 1
   248: count = 1
   251: count = 20

timeUnit
     1: count = 86

vertCoord
     1: count = 55
   100: count = 30
   102: count = 1

earthShape
     0: count = 86

uvIsReletive
        false: count = 86

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/19970101060000.grb

referenceDate
   1997-01-01T06:00:00Z: count = 86

table version
      58-42-2: count = 86

param
     2: count = 1
     7: count = 6
    11: count = 7
    17: count = 1
    20: count = 1
    33: count = 7
    34: count = 7
    52: count = 6
    60: count = 1
    61: count = 1
    66: count = 1
    71: count = 1
    73: count = 1
    74: count = 1
    75: count = 1
   133: count = 1
   142: count = 1
   144: count = 1
   146: count = 1
   147: count = 1
   151: count = 1
   161: count = 1
   162: count = 1
   170: count = 1
   186: count = 1
   187: count = 1
   191: count = 1
   200: count = 1
   208: count = 1
   240: count = 1
   241: count = 1
   242: count = 1
   243: count = 1
   244: count = 1
   245: count = 1
   246: count = 1
   247: count = 1
   248: count = 1
   251: count = 20

timeUnit
     0: count = 86

vertCoord
     1: count = 55
   100: count = 30
   102: count = 1

earthShape
     0: count = 86

uvIsReletive
        false: count = 86

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/2004092000_000_2011_200.grib

referenceDate
   2004-09-20T00:00:00Z: count = 1

table version
     78-255-2: count = 1

param
    11: count = 1

timeUnit
     0: count = 1

vertCoord
   105: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS
   105: count = 1

predefined

thin
 Q:/cdmUnitTest/formats/grib1/2005-07-14-0900_us057g1010t04q060003000

referenceDate
   2005-07-14T06:00:00Z: count = 448

table version
       57-1-2: count = 448

param
     1: count = 41
     7: count = 65
    11: count = 66
    33: count = 67
    34: count = 67
    40: count = 66
    52: count = 66
    71: count = 1
   122: count = 1
   137: count = 1
   191: count = 1
   230: count = 1
   231: count = 1
   233: count = 1
   240: count = 1
   241: count = 1
   244: count = 1

timeUnit
     0: count = 447
     4: count = 1

vertCoord
     1: count = 10
   100: count = 144
   105: count = 6
   107: count = 287
   200: count = 1

earthShape
     0: count = 448

uvIsReletive
        false: count = 448

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/200906281800.d01.grb

referenceDate
   2009-06-28T18:00:00Z: count = 37

table version
    7-138-130: count = 37

param
     1: count = 1
    11: count = 1
    51: count = 1
    57: count = 1
    65: count = 1
    66: count = 1
    81: count = 1
    85: count = 4
    86: count = 4
    87: count = 1
   111: count = 1
   112: count = 1
   121: count = 1
   122: count = 1
   148: count = 1
   155: count = 1
   161: count = 2
   162: count = 2
   177: count = 1
   186: count = 1
   187: count = 1
   189: count = 1
   192: count = 2
   199: count = 1
   204: count = 1
   205: count = 1
   210: count = 1
   223: count = 1

timeUnit
     1: count = 27
     7: count = 10

vertCoord
     1: count = 29
   112: count = 8

earthShape
     0: count = 37

uvIsReletive
        false: count = 37

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/AVN-I.wmo

referenceDate
   2005-01-22T18:00:00Z: count = 1233

table version
        7-0-2: count = 1233

param
     1: count = 42
     2: count = 21
     7: count = 210
    11: count = 231
    33: count = 231
    34: count = 231
    41: count = 57
    52: count = 147
    54: count = 21
   131: count = 21
   136: count = 21

timeUnit
    10: count = 1233

vertCoord
     1: count = 42
     7: count = 105
   100: count = 939
   102: count = 21
   105: count = 21
   108: count = 21
   116: count = 63
   200: count = 21

earthShape
     0: count = 1233

uvIsReletive
        false: count = 1233

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/AVN.wmo

referenceDate
   2004-10-24T12:00:00Z: count = 215

table version
        7-0-2: count = 215

param
     1: count = 6
     2: count = 3
     7: count = 30
    11: count = 33
    33: count = 37
    34: count = 36
    39: count = 26
    41: count = 11
    52: count = 21
    54: count = 3
    61: count = 3
   131: count = 3
   136: count = 3

timeUnit
     4: count = 3
    10: count = 212

vertCoord
     1: count = 9
     7: count = 16
   100: count = 169
   102: count = 3
   105: count = 3
   108: count = 3
   116: count = 9
   200: count = 3

earthShape
     0: count = 215

uvIsReletive
        false: count = 215

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/CCCma_SRES_A2_HGT500_1-10.grb

referenceDate
   1900-01-01T12:00:00Z: count = 1
   1900-02-01T12:00:00Z: count = 1
   1900-03-01T12:00:00Z: count = 1
   1900-04-01T12:00:00Z: count = 1
   1900-05-01T12:00:00Z: count = 1
   1900-06-01T12:00:00Z: count = 1
   1900-07-01T12:00:00Z: count = 1
   1900-08-01T12:00:00Z: count = 1
   1900-09-01T12:00:00Z: count = 1
   1900-10-01T12:00:00Z: count = 1

table version
       54-0-1: count = 10

param
     7: count = 10

timeUnit
     1: count = 10

vertCoord
   100: count = 10

earthShape
     0: count = 10

uvIsReletive
        false: count = 10

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/COAMPS-Temp-Regional.grib

referenceDate
   2004-12-15T12:00:00Z: count = 1

table version
       58-0-2: count = 1

param
    11: count = 1

timeUnit
     0: count = 1

vertCoord
   100: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/D2.2006091400.F012.002M.CLWMR.GRIB

referenceDate
   2006-09-14T00:00:00Z: count = 1

table version
     60-255-2: count = 1

param
   153: count = 1

timeUnit
     0: count = 1

vertCoord
   105: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/D2.2006091400.F012.MSL.PRES.GRIB

referenceDate
   2006-09-14T00:00:00Z: count = 1

table version
     60-255-2: count = 1

param
     2: count = 1

timeUnit
     0: count = 1

vertCoord
   102: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/FOAM_atl19_anal.130502.scal.grb

referenceDate
   2002-05-13T00:00:00Z: count = 45

table version
       74-0-2: count = 45

param
    11: count = 20
    66: count = 1
    67: count = 1
    88: count = 20
    91: count = 1
    92: count = 1
   129: count = 1

timeUnit
     0: count = 45

vertCoord
     1: count = 1
   100: count = 4
   160: count = 40

earthShape
     0: count = 45

uvIsReletive
        false: count = 45

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/GEM_20050915_0000.grib1

referenceDate
   2005-09-15T00:00:00Z: count = 565

table version
       54-0-2: count = 565

param
     1: count = 9
     2: count = 9
     7: count = 81
    11: count = 90
    18: count = 90
    33: count = 90
    34: count = 90
    39: count = 81
    59: count = 8
    61: count = 8
    71: count = 9

timeUnit
     4: count = 8
    10: count = 557

vertCoord
     1: count = 34
   100: count = 486
   102: count = 9
   119: count = 36

earthShape
     0: count = 565

uvIsReletive
        false: count = 565

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/HIRLAMhybrid.grib

referenceDate
   2009-07-01T06:00:00Z: count = 205

table version
       99-0-1: count = 205

param
     1: count = 5
    52: count = 200

timeUnit
     0: count = 164
     1: count = 41

vertCoord
   103: count = 5
   109: count = 200

earthShape
     0: count = 205

uvIsReletive
        false: count = 205

vertCoordInGDS
   103: count = 5
   109: count = 200

predefined

thin
 Q:/cdmUnitTest/formats/grib1/INDOuestN_20652.2.grb

referenceDate
   2006-07-17T12:00:00Z: count = 28

table version
       98-0-2: count = 28

param
     2: count = 7
    33: count = 7
    34: count = 7
   100: count = 7

timeUnit
     0: count = 28

vertCoord
   102: count = 14
   105: count = 14

earthShape
     0: count = 28

uvIsReletive
        false: count = 28

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/INDOuestN_20652.grb

referenceDate
   2006-07-17T12:00:00Z: count = 28

table version
       98-0-2: count = 28

param
     2: count = 7
    33: count = 7
    34: count = 7
   100: count = 7

timeUnit
     0: count = 28

vertCoord
   102: count = 14
   105: count = 14

earthShape
     0: count = 28

uvIsReletive
        false: count = 28

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/MRF.wmo

referenceDate
   2004-12-12T00:00:00Z: count = 901

table version
        7-0-2: count = 901

param
     2: count = 21
     7: count = 126
    11: count = 147
    33: count = 147
    34: count = 147
    41: count = 84
    52: count = 168
    61: count = 20
    63: count = 20
   222: count = 21

timeUnit
     4: count = 40
    10: count = 861

vertCoord
     1: count = 40
   100: count = 735
   102: count = 21
   108: count = 21
   116: count = 84

earthShape
     0: count = 901

uvIsReletive
        false: count = 901

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/Mercator.grib1

referenceDate
   2006-10-21T00:00:00Z: count = 901

table version
        7-0-2: count = 901

param
     2: count = 21
     7: count = 126
    11: count = 147
    33: count = 147
    34: count = 147
    41: count = 84
    52: count = 168
    61: count = 20
    63: count = 20
   222: count = 21

timeUnit
     4: count = 40
    10: count = 861

vertCoord
     1: count = 40
   100: count = 735
   102: count = 21
   108: count = 21
   116: count = 84

earthShape
     0: count = 901

uvIsReletive
        false: count = 901

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/NOGAPS-Temp-Global.grib

referenceDate
   2004-12-15T12:00:00Z: count = 1

table version
       58-0-2: count = 1

param
    11: count = 1

timeUnit
     0: count = 1

vertCoord
   100: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/NOGAPS-Temp-Regional.grib

referenceDate
   2005-02-01T18:00:00Z: count = 1

table version
       58-0-2: count = 1

param
    11: count = 1

timeUnit
     0: count = 1

vertCoord
   105: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/OCEAN.wmo

referenceDate
   2004-11-12T00:00:00Z: count = 4

table version
        7-4-3: count = 4

param
    80: count = 4

timeUnit
     1: count = 4

vertCoord
     1: count = 4

earthShape
     0: count = 4

uvIsReletive
        false: count = 4

vertCoordInGDS

predefined
    61: count = 1
    62: count = 1
    63: count = 1
    64: count = 1

thin
 Q:/cdmUnitTest/formats/grib1/PRMSL_000

referenceDate
   2005-04-15T12:00:00Z: count = 1

table version
        7-0-2: count = 1

param
     2: count = 1

timeUnit
     0: count = 1

vertCoord
   102: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/QPE.20101005.009.157

referenceDate
   2010-10-05T00:00:00Z: count = 4
   2010-10-05T01:00:00Z: count = 4
   2010-10-05T02:00:00Z: count = 4
   2010-10-05T03:00:00Z: count = 6
   2010-10-05T04:00:00Z: count = 6
   2010-10-05T05:00:00Z: count = 4
   2010-10-05T06:00:00Z: count = 6
   2010-10-05T07:00:00Z: count = 5
   2010-10-05T08:00:00Z: count = 5
   2010-10-05T09:00:00Z: count = 5
   2010-10-05T10:00:00Z: count = 3
   2010-10-05T11:00:00Z: count = 2
   2010-10-05T12:00:00Z: count = 12
   2010-10-05T13:00:00Z: count = 4
   2010-10-05T14:00:00Z: count = 4
   2010-10-05T15:00:00Z: count = 4
   2010-10-05T16:00:00Z: count = 4
   2010-10-05T17:00:00Z: count = 3
   2010-10-05T18:00:00Z: count = 4
   2010-10-05T19:00:00Z: count = 3
   2010-10-05T20:00:00Z: count = 5
   2010-10-05T21:00:00Z: count = 4
   2010-10-05T22:00:00Z: count = 2
   2010-10-05T23:00:00Z: count = 5

table version
    9-157-128: count = 68
      9-157-2: count = 40

param
    61: count = 40
   237: count = 68

timeUnit
     4: count = 108

vertCoord
     1: count = 108

earthShape
     0: count = 108

uvIsReletive
        false: count = 108

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/RANAL_2006081400.grb

referenceDate
   2006-08-14T00:00:00Z: count = 96

table version
       34-0-3: count = 96

param
     2: count = 1
     7: count = 20
    11: count = 21
    33: count = 21
    34: count = 21
    52: count = 12

timeUnit
    10: count = 96

vertCoord
     1: count = 4
   100: count = 91
   102: count = 1

earthShape
     0: count = 96

uvIsReletive
        false: count = 96

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/RUC.wmo

referenceDate
   2002-12-02T22:00:00Z: count = 190

table version
        7-0-2: count = 190

param
     1: count = 4
     7: count = 34
    11: count = 35
    13: count = 1
    33: count = 37
    34: count = 37
    39: count = 5
    52: count = 36
   129: count = 1

timeUnit
     0: count = 190

vertCoord
     1: count = 2
     4: count = 3
     6: count = 3
     7: count = 4
   100: count = 161
   102: count = 1
   105: count = 4
   116: count = 12

earthShape
     0: count = 189
     1: count = 1

uvIsReletive
        false: count = 189
         true: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/RUC2.wmo

referenceDate
   2004-12-22T00:00:00Z: count = 1171

table version
        7-0-2: count = 1171

param
     1: count = 36
     7: count = 187
     8: count = 10
    11: count = 193
    20: count = 9
    33: count = 202
    34: count = 202
    39: count = 43
    52: count = 137
    54: count = 9
    59: count = 9
    62: count = 10
    63: count = 10
    65: count = 4
    66: count = 9
    77: count = 9
   129: count = 9
   131: count = 9
   140: count = 5
   141: count = 5
   142: count = 5
   143: count = 5
   156: count = 9
   157: count = 9
   180: count = 9
   190: count = 9
   196: count = 9
   197: count = 9

timeUnit
     0: count = 1147
     4: count = 24

vertCoord
     1: count = 152
     2: count = 5
     3: count = 5
     4: count = 27
     6: count = 27
     7: count = 36
   100: count = 860
   102: count = 9
   105: count = 36
   200: count = 9
   243: count = 5

earthShape
     0: count = 1171

uvIsReletive
        false: count = 1171

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/RUC_W.wmo

referenceDate
   2005-01-05T00:00:00Z: count = 1171

table version
        7-0-2: count = 1171

param
     1: count = 36
     7: count = 187
     8: count = 10
    11: count = 193
    20: count = 9
    33: count = 202
    34: count = 202
    39: count = 43
    52: count = 137
    54: count = 9
    59: count = 9
    62: count = 10
    63: count = 10
    65: count = 4
    66: count = 9
    77: count = 9
   129: count = 9
   131: count = 9
   140: count = 5
   141: count = 5
   142: count = 5
   143: count = 5
   156: count = 9
   157: count = 9
   180: count = 9
   190: count = 9
   196: count = 9
   197: count = 9

timeUnit
     0: count = 1147
     4: count = 24

vertCoord
     1: count = 152
     2: count = 5
     3: count = 5
     4: count = 27
     6: count = 27
     7: count = 36
   100: count = 860
   102: count = 9
   105: count = 36
   200: count = 9
   243: count = 5

earthShape
     0: count = 1171

uvIsReletive
        false: count = 1171

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/SST_Global_5x2p5deg_20071119_0000.grib1

referenceDate
   2007-11-19T00:00:00Z: count = 4

table version
        7-4-3: count = 4

param
    80: count = 4

timeUnit
     1: count = 4

vertCoord
     1: count = 4

earthShape
     0: count = 4

uvIsReletive
        false: count = 4

vertCoordInGDS

predefined
    21: count = 1
    22: count = 1
    23: count = 1
    24: count = 1

thin
 Q:/cdmUnitTest/formats/grib1/Temp_blob.grb

referenceDate
   1997-07-01T00:00:00Z: count = 1

table version
        7-1-2: count = 1

param
    11: count = 1

timeUnit
    51: count = 1

vertCoord
   100: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/US058

referenceDate
   2006-07-20T12:00:00Z: count = 1

table version
       58-0-3: count = 1

param
   100: count = 1

timeUnit
     0: count = 1

vertCoord
     1: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/US058GOCN-GR1mdl.0110_0240_14400F0RL2006072012_0001_000000-000000sig_wav_ht

referenceDate
   2006-07-20T12:00:00Z: count = 1

table version
       58-0-3: count = 1

param
   100: count = 1

timeUnit
     0: count = 1

vertCoord
     1: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/US058GOCN-GR1mdl.0110_0240_14400F0RL2006072012_0001_000000-000000swl_wav_ht

referenceDate
   2006-07-20T12:00:00Z: count = 1

table version
       58-0-3: count = 1

param
   105: count = 1

timeUnit
     0: count = 1

vertCoord
     1: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/WAVE.wmo

referenceDate
   2004-11-19T00:00:00Z: count = 116

table version
        7-0-2: count = 112
        7-4-3: count = 4

param
    11: count = 32
    33: count = 16
    34: count = 16
    80: count = 4
   100: count = 16
   101: count = 16
   103: count = 16

timeUnit
     0: count = 80
     1: count = 4
    10: count = 32

vertCoord
     1: count = 84
   105: count = 32

earthShape
     0: count = 116

uvIsReletive
        false: count = 116

vertCoordInGDS

predefined
    21: count = 29
    22: count = 29
    23: count = 29
    24: count = 29

thin
 Q:/cdmUnitTest/formats/grib1/airtmp_zht_000002_000000_1a0061x0061_2010011200_00240000_fcstfld.grib

referenceDate
   2010-01-12T00:00:00Z: count = 1

table version
       58-0-2: count = 1

param
    11: count = 1

timeUnit
    10: count = 1

vertCoord
   105: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/cfs.wmo

referenceDate
   2004-12-30T00:00:00Z: count = 53

table version
        7-0-2: count = 53

param
     1: count = 9
    11: count = 7
    15: count = 1
    16: count = 1
    33: count = 1
    34: count = 1
    51: count = 1
    54: count = 1
    59: count = 1
    65: count = 1
    71: count = 6
    81: count = 1
    84: count = 1
    90: count = 1
    91: count = 1
   121: count = 1
   122: count = 1
   124: count = 1
   125: count = 1
   144: count = 2
   145: count = 1
   146: count = 1
   147: count = 1
   148: count = 1
   155: count = 1
   204: count = 1
   205: count = 1
   211: count = 2
   212: count = 2
   214: count = 1
   221: count = 1

timeUnit
     3: count = 53

vertCoord
     1: count = 22
     8: count = 2
   105: count = 6
   112: count = 4
   200: count = 3
   211: count = 1
   212: count = 1
   213: count = 2
   214: count = 1
   222: count = 1
   223: count = 2
   224: count = 1
   232: count = 1
   233: count = 2
   234: count = 1
   242: count = 1
   243: count = 1
   244: count = 1

earthShape
     0: count = 53

uvIsReletive
        false: count = 53

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/coamps.nrl.grib

referenceDate
   2006-03-02T00:00:00Z: count = 175

table version
       58-0-2: count = 175

param
    13: count = 35
    33: count = 35
    34: count = 35
    40: count = 35
    77: count = 35

timeUnit
    10: count = 175

vertCoord
   105: count = 175

earthShape
     0: count = 175

uvIsReletive
        false: count = 175

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/don_ETA.wmo

referenceDate
   2004-08-13T12:00:00Z: count = 1704

table version
        7-0-2: count = 1704

param
     1: count = 11
     7: count = 210
    11: count = 275
    24: count = 11
    33: count = 275
    34: count = 275
    39: count = 209
    41: count = 55
    52: count = 275
    54: count = 11
    61: count = 10
    63: count = 10
   130: count = 11
   132: count = 11
   156: count = 22
   157: count = 22
   190: count = 11

timeUnit
     0: count = 1684
     4: count = 20

vertCoord
     1: count = 54
   100: count = 1309
   102: count = 11
   105: count = 44
   106: count = 11
   116: count = 264
   200: count = 11

earthShape
     0: count = 1704

uvIsReletive
        false: count = 1704

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/ecmwf_00-003.grib

referenceDate
   2005-04-14T00:00:00Z: count = 1

table version
     98-0-128: count = 1

param
   142: count = 1

timeUnit
     1: count = 1

vertCoord
     1: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/ensemble.wmo

referenceDate
   2004-12-22T00:00:00Z: count = 7170

table version
        7-2-2: count = 7170

param
     2: count = 330
     7: count = 1620
    11: count = 1322
    15: count = 308
    16: count = 308
    33: count = 1322
    34: count = 1322
    52: count = 330
    61: count = 308

timeUnit
     2: count = 616
     4: count = 308
    10: count = 6246

vertCoord
     1: count = 308
   100: count = 4920
   102: count = 330
   105: count = 1612

earthShape
     0: count = 7170

uvIsReletive
        false: count = 7170

vertCoordInGDS

predefined

thin
     0: count = 7170
 Q:/cdmUnitTest/formats/grib1/eta.Y.Q.wmo

referenceDate
   2005-02-27T12:00:00Z: count = 522

table version
        7-0-2: count = 522

param
     1: count = 21
     2: count = 21
    11: count = 42
    20: count = 21
    33: count = 42
    34: count = 42
    52: count = 42
    54: count = 21
    61: count = 20
    63: count = 20
    65: count = 20
   130: count = 21
   131: count = 21
   132: count = 21
   140: count = 21
   141: count = 21
   142: count = 21
   143: count = 21
   156: count = 21
   157: count = 21
   190: count = 21

timeUnit
     0: count = 462
     4: count = 60

vertCoord
     1: count = 228
   101: count = 21
   102: count = 42
   105: count = 84
   106: count = 21
   116: count = 105
   200: count = 21

earthShape
     0: count = 522

uvIsReletive
        false: count = 522

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/extended.wmo

referenceDate
   2004-12-22T00:00:00Z: count = 112

table version
        7-0-2: count = 112

param
     2: count = 28
     7: count = 28
    33: count = 28
    34: count = 28

timeUnit
    10: count = 112

vertCoord
   100: count = 84
   102: count = 28

earthShape
     0: count = 112

uvIsReletive
        false: count = 112

vertCoordInGDS

predefined
    25: count = 56
    26: count = 56

thin
 Q:/cdmUnitTest/formats/grib1/hfsf00010000

referenceDate
   2006-10-11T00:00:00Z: count = 2

table version
     78-255-2: count = 2

param
    33: count = 1
    34: count = 1

timeUnit
     0: count = 2

vertCoord
   105: count = 2

earthShape
     0: count = 2

uvIsReletive
        false: count = 2

vertCoordInGDS
   105: count = 2

predefined

thin
 Q:/cdmUnitTest/formats/grib1/hurr_charley.grb

referenceDate
   2004-08-13T12:00:00Z: count = 5499

table version
        7-0-2: count = 5499

param
     1: count = 91
     2: count = 13
     7: count = 533
    11: count = 611
    13: count = 13
    17: count = 520
    24: count = 13
    33: count = 572
    34: count = 572
    39: count = 546
    51: count = 507
    52: count = 507
    54: count = 13
    61: count = 13
    62: count = 13
    63: count = 13
    71: count = 13
    73: count = 13
    74: count = 13
    75: count = 13
    80: count = 13
    81: count = 13
    84: count = 13
    85: count = 52
    91: count = 13
   121: count = 13
   122: count = 13
   130: count = 13
   132: count = 13
   140: count = 13
   141: count = 13
   142: count = 13
   143: count = 13
   144: count = 52
   156: count = 26
   157: count = 26
   158: count = 507
   190: count = 13
   196: count = 13
   197: count = 13
   224: count = 13
   225: count = 13
   231: count = 13

timeUnit
     0: count = 5460
     4: count = 39

vertCoord
     1: count = 273
     4: count = 13
     7: count = 26
   100: count = 4563
   101: count = 26
   102: count = 26
   105: count = 65
   106: count = 13
   112: count = 104
   116: count = 325
   200: count = 26
   214: count = 13
   224: count = 13
   234: count = 13

earthShape
     0: count = 5499

uvIsReletive
        false: count = 5499

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/jeff.grib

referenceDate
   2004-12-11T15:00:00Z: count = 60

table version
        7-8-2: count = 60

param
   186: count = 30
   236: count = 30

timeUnit
     1: count = 60

vertCoord
   103: count = 60

earthShape
     0: count = 60

uvIsReletive
        false: count = 60

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/murphy.grib

referenceDate
   2005-05-04T00:00:00Z: count = 10
   2005-05-04T12:00:00Z: count = 1
   2005-05-06T00:00:00Z: count = 2

table version
       57-1-2: count = 13

param
     2: count = 1
     7: count = 1
    11: count = 1
    17: count = 1
    20: count = 1
    33: count = 1
    39: count = 1
    52: count = 1
    61: count = 1
    71: count = 1
    77: count = 1
   137: count = 1
   244: count = 1

timeUnit
     0: count = 13

vertCoord
     1: count = 3
   100: count = 3
   102: count = 1
   105: count = 4
   116: count = 1
   200: count = 1

earthShape
     0: count = 13

uvIsReletive
        false: count = 13

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/narr-a_221_20070411_0000_000.grb

referenceDate
   2007-04-11T00:00:00Z: count = 300

table version
     7-15-131: count = 300

param
     1: count = 4
     2: count = 1
     7: count = 30
    11: count = 33
    13: count = 3
    17: count = 1
    20: count = 1
    33: count = 32
    34: count = 32
    39: count = 30
    51: count = 32
    52: count = 2
    65: count = 1
    66: count = 1
    85: count = 5
    86: count = 1
   130: count = 1
   134: count = 1
   135: count = 2
   144: count = 4
   153: count = 29
   158: count = 16
   160: count = 4
   178: count = 29
   207: count = 1
   221: count = 1
   223: count = 1
   226: count = 1
   238: count = 1

timeUnit
     0: count = 300

vertCoord
     1: count = 10
   100: count = 248
   102: count = 2
   105: count = 13
   109: count = 12
   111: count = 1
   112: count = 14

earthShape
     0: count = 300

uvIsReletive
        false: count = 300

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/narr-a_221_20070411_0300_000.grb

referenceDate
   2007-04-11T03:00:00Z: count = 301

table version
     7-15-131: count = 301

param
     1: count = 4
     2: count = 1
     7: count = 30
    11: count = 33
    13: count = 4
    17: count = 1
    20: count = 1
    33: count = 32
    34: count = 32
    39: count = 30
    51: count = 32
    52: count = 2
    65: count = 1
    66: count = 1
    85: count = 5
    86: count = 1
   130: count = 1
   134: count = 1
   135: count = 2
   144: count = 4
   153: count = 29
   158: count = 16
   160: count = 4
   178: count = 29
   207: count = 1
   221: count = 1
   223: count = 1
   226: count = 1
   238: count = 1

timeUnit
     0: count = 301

vertCoord
     1: count = 10
   100: count = 248
   102: count = 2
   105: count = 14
   109: count = 12
   111: count = 1
   112: count = 14

earthShape
     0: count = 301

uvIsReletive
        false: count = 301

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/narr-a_221_20070411_0600_000.grb

referenceDate
   2007-04-11T06:00:00Z: count = 302

table version
     7-15-131: count = 302

param
     1: count = 4
     2: count = 1
     7: count = 30
    11: count = 34
    13: count = 4
    17: count = 1
    20: count = 1
    33: count = 32
    34: count = 32
    39: count = 30
    51: count = 32
    52: count = 2
    65: count = 1
    66: count = 1
    85: count = 5
    86: count = 1
   130: count = 1
   134: count = 1
   135: count = 2
   144: count = 4
   153: count = 29
   158: count = 16
   160: count = 4
   178: count = 29
   207: count = 1
   221: count = 1
   223: count = 1
   226: count = 1
   238: count = 1

timeUnit
     0: count = 302

vertCoord
     1: count = 10
   100: count = 248
   102: count = 2
   105: count = 15
   109: count = 12
   111: count = 1
   112: count = 14

earthShape
     0: count = 302

uvIsReletive
        false: count = 302

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/needsUnsignedLength.grib

referenceDate
   1996-01-01T00:00:00Z: count = 1

table version
        7-0-1: count = 1

param
    11: count = 1

timeUnit
    10: count = 1

vertCoord
     1: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/nogaps.grib

referenceDate
   2005-05-06T18:00:00Z: count = 3

table version
       58-0-2: count = 3

param
     2: count = 1
    33: count = 1
    34: count = 1

timeUnit
     0: count = 3

vertCoord
   100: count = 2
   102: count = 1

earthShape
     0: count = 3

uvIsReletive
        false: count = 3

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/nrukmet.t00z.ukm25

referenceDate
   2007-03-07T00:00:00Z: count = 22

table version
       98-0-1: count = 22

param
     2: count = 5
     7: count = 5
    33: count = 6
    34: count = 6

timeUnit
     0: count = 22

vertCoord
     1: count = 5
   100: count = 17

earthShape
     0: count = 22

uvIsReletive
        false: count = 22

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/output.grib

referenceDate
   2010-06-03T06:00:00Z: count = 2

table version
     98-0-128: count = 2

param
   165: count = 1
   166: count = 1

timeUnit
     0: count = 2

vertCoord
     1: count = 2

earthShape
     0: count = 2

uvIsReletive
        false: count = 2

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/pgbanl.fnl

referenceDate
   2004-09-22T00:00:00Z: count = 314

table version
        7-0-2: count = 314

param
     1: count = 7
     2: count = 1
     7: count = 38
    10: count = 1
    11: count = 44
    13: count = 1
    27: count = 2
    33: count = 41
    34: count = 41
    39: count = 22
    41: count = 31
    51: count = 2
    52: count = 31
    54: count = 1
    65: count = 1
    71: count = 1
    76: count = 1
    81: count = 1
    91: count = 1
   131: count = 1
   132: count = 1
   136: count = 3
   144: count = 2
   153: count = 21
   154: count = 11
   156: count = 2
   157: count = 2
   221: count = 1
   222: count = 1
   230: count = 1

timeUnit
    10: count = 314

vertCoord
     1: count = 11
     4: count = 2
     6: count = 5
     7: count = 6
   100: count = 233
   102: count = 1
   103: count = 9
   105: count = 5
   107: count = 6
   108: count = 4
   112: count = 4
   116: count = 7
   117: count = 12
   200: count = 4
   204: count = 2
   242: count = 1
   243: count = 1
   244: count = 1

earthShape
     0: count = 314

uvIsReletive
        false: count = 314

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/pgbf12.gfs.2004091700

referenceDate
   2004-09-17T00:00:00Z: count = 355

table version
        7-0-2: count = 355

param
     1: count = 13
     2: count = 1
     7: count = 38
    10: count = 1
    11: count = 47
    13: count = 1
    15: count = 1
    16: count = 1
    27: count = 2
    33: count = 41
    34: count = 41
    39: count = 22
    41: count = 31
    51: count = 2
    52: count = 31
    54: count = 1
    59: count = 1
    61: count = 1
    63: count = 1
    65: count = 1
    71: count = 6
    76: count = 1
    81: count = 1
    84: count = 1
    90: count = 1
    91: count = 1
   121: count = 1
   122: count = 1
   124: count = 1
   125: count = 1
   131: count = 1
   132: count = 1
   136: count = 3
   140: count = 1
   141: count = 1
   142: count = 1
   143: count = 1
   144: count = 2
   145: count = 1
   146: count = 1
   147: count = 1
   148: count = 1
   153: count = 21
   154: count = 11
   155: count = 1
   156: count = 2
   157: count = 2
   204: count = 1
   205: count = 1
   211: count = 2
   212: count = 2
   214: count = 1
   221: count = 1
   222: count = 1
   230: count = 1

timeUnit
     2: count = 2
     3: count = 36
     4: count = 3
    10: count = 314

vertCoord
     1: count = 33
     4: count = 2
     6: count = 5
     7: count = 6
     8: count = 2
   100: count = 233
   102: count = 1
   103: count = 9
   105: count = 7
   107: count = 6
   108: count = 4
   112: count = 4
   116: count = 7
   117: count = 12
   200: count = 6
   204: count = 2
   211: count = 1
   212: count = 1
   213: count = 2
   214: count = 1
   222: count = 1
   223: count = 2
   224: count = 1
   232: count = 1
   233: count = 2
   234: count = 1
   242: count = 1
   243: count = 1
   244: count = 1

earthShape
     0: count = 355

uvIsReletive
        false: count = 355

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/radar_national.grib

referenceDate
   2005-01-20T02:15:00Z: count = 1

table version
       60-1-2: count = 1

param
   201: count = 1

timeUnit
     0: count = 1

vertCoord
     1: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/radar_national_rcm.grib

referenceDate
   2005-02-07T17:49:00Z: count = 1

table version
        8-0-2: count = 1

param
    21: count = 1

timeUnit
     2: count = 1

vertCoord
     1: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/radar_regional.grib

referenceDate
   2005-01-20T02:10:00Z: count = 1

table version
       60-1-2: count = 1

param
   201: count = 1

timeUnit
     0: count = 1

vertCoord
     1: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/rotatedlatlon.grb

referenceDate
   2003-12-15T18:00:00Z: count = 1

table version
       96-0-1: count = 1

param
     7: count = 1

timeUnit
     1: count = 1

vertCoord
   105: count = 1

earthShape
     0: count = 1

uvIsReletive
        false: count = 1

vertCoordInGDS
   105: count = 1

predefined

thin
 Q:/cdmUnitTest/formats/grib1/ruc2.t16z.bgrb20anl

referenceDate
   2005-03-08T16:00:00Z: count = 746

table version
        7-0-2: count = 746

param
     1: count = 51
     7: count = 50
    11: count = 1
    13: count = 1
    33: count = 52
    34: count = 52
    39: count = 50
    53: count = 52
    54: count = 1
    59: count = 1
    62: count = 1
    63: count = 1
    65: count = 2
    66: count = 1
    85: count = 6
    89: count = 1
   111: count = 1
   112: count = 1
   121: count = 1
   122: count = 1
   129: count = 1
   144: count = 6
   153: count = 50
   156: count = 1
   157: count = 1
   158: count = 50
   170: count = 50
   171: count = 50
   178: count = 50
   179: count = 50
   186: count = 1
   187: count = 1
   188: count = 1
   189: count = 50
   198: count = 50
   223: count = 1
   224: count = 1
   225: count = 1
   234: count = 1
   235: count = 1
   239: count = 2

timeUnit
     0: count = 743
     4: count = 3

vertCoord
     1: count = 22
     7: count = 4
   102: count = 1
   105: count = 4
   109: count = 700
   111: count = 14
   200: count = 1

earthShape
     0: count = 745
     1: count = 1

uvIsReletive
        false: count = 745
         true: count = 1

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/test.nc

referenceDate

table version

param

timeUnit

vertCoord

earthShape

uvIsReletive

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/testproj.grb

referenceDate
   2007-11-19T06:00:00Z: count = 792
   2007-11-20T06:00:00Z: count = 792

table version
      7-0-129: count = 112
      7-0-130: count = 22
        7-0-2: count = 1450

param
     1: count = 24
     2: count = 2
     7: count = 98
    11: count = 100
    17: count = 86
    20: count = 4
    24: count = 2
    33: count = 96
    34: count = 96
    35: count = 84
    39: count = 84
    41: count = 18
    51: count = 88
    52: count = 98
    54: count = 2
    58: count = 84
    59: count = 2
    65: count = 2
    71: count = 2
    72: count = 2
    73: count = 2
    74: count = 2
    75: count = 2
    81: count = 2
    83: count = 2
    84: count = 2
    85: count = 10
    86: count = 2
    87: count = 2
    91: count = 2
   118: count = 2
   121: count = 2
   122: count = 2
   124: count = 2
   125: count = 2
   130: count = 2
   131: count = 2
   132: count = 2
   134: count = 2
   135: count = 4
   136: count = 4
   137: count = 2
   138: count = 2
   139: count = 2
   140: count = 4
   141: count = 4
   142: count = 2
   143: count = 2
   144: count = 8
   145: count = 2
   153: count = 84
   155: count = 2
   156: count = 8
   157: count = 8
   158: count = 84
   161: count = 2
   170: count = 84
   171: count = 86
   180: count = 2
   181: count = 2
   190: count = 4
   196: count = 2
   197: count = 2
   203: count = 4
   204: count = 4
   205: count = 4
   206: count = 2
   207: count = 2
   208: count = 2
   211: count = 90
   212: count = 4
   214: count = 2
   216: count = 4
   219: count = 2
   221: count = 2
   223: count = 2
   224: count = 2
   225: count = 2
   226: count = 2
   230: count = 2
   231: count = 2
   238: count = 2
   240: count = 2
   246: count = 2
   247: count = 2
   248: count = 2
   249: count = 2
   250: count = 4
   251: count = 4
   252: count = 2
   253: count = 2

timeUnit
     0: count = 1584

vertCoord
     1: count = 110
     2: count = 6
     3: count = 8
     4: count = 2
     7: count = 2
   100: count = 1278
   101: count = 2
   102: count = 4
   105: count = 16
   106: count = 10
   109: count = 18
   111: count = 2
   112: count = 20
   116: count = 58
   200: count = 20
   204: count = 2
   206: count = 2
   207: count = 2
   214: count = 2
   215: count = 2
   224: count = 2
   234: count = 2
   242: count = 2
   243: count = 2
   245: count = 2
   248: count = 2
   249: count = 2
   251: count = 2
   252: count = 2

earthShape
     0: count = 1584

uvIsReletive
        false: count = 1584

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/testproj2.grb

referenceDate
   2007-11-19T00:00:00Z: count = 4
   2007-11-19T06:00:00Z: count = 655

table version
        7-0-2: count = 655
        7-4-3: count = 4

param
     1: count = 19
     7: count = 101
    11: count = 115
    13: count = 5
    33: count = 125
    34: count = 125
    39: count = 24
    52: count = 120
    59: count = 4
    61: count = 4
    62: count = 4
    63: count = 4
    80: count = 4
   129: count = 5

timeUnit
     0: count = 643
     1: count = 4
     4: count = 12

vertCoord
     1: count = 25
     4: count = 15
     6: count = 15
     7: count = 20
   100: count = 499
   102: count = 5
   105: count = 20
   116: count = 60

earthShape
     0: count = 654
     1: count = 5

uvIsReletive
        false: count = 654
         true: count = 5

vertCoordInGDS

predefined
    21: count = 1
    22: count = 1
    23: count = 1
    24: count = 1

thin
 Q:/cdmUnitTest/formats/grib1/us057g1010t04a000000000

referenceDate
   2005-07-07T00:00:00Z: count = 1961

table version
       57-1-2: count = 1961

param
     1: count = 43
     2: count = 1
     7: count = 67
    11: count = 103
    13: count = 26
    14: count = 25
    17: count = 66
    20: count = 52
    33: count = 105
    34: count = 105
    39: count = 24
    40: count = 67
    41: count = 24
    43: count = 24
    44: count = 24
    52: count = 95
    53: count = 66
    54: count = 1
    62: count = 1
    63: count = 1
    71: count = 1
    77: count = 1
    85: count = 6
   111: count = 1
   112: count = 1
   121: count = 1
   122: count = 1
   131: count = 1
   134: count = 42
   135: count = 1
   136: count = 1
   137: count = 1
   140: count = 1
   141: count = 1
   142: count = 1
   143: count = 1
   145: count = 4
   146: count = 4
   148: count = 4
   149: count = 4
   151: count = 4
   152: count = 4
   155: count = 42
   157: count = 1
   161: count = 24
   163: count = 1
   165: count = 1
   166: count = 38
   167: count = 1
   168: count = 1
   169: count = 1
   170: count = 1
   171: count = 1
   173: count = 1
   191: count = 1
   192: count = 41
   193: count = 1
   194: count = 1
   197: count = 41
   199: count = 1
   208: count = 1
   209: count = 1
   210: count = 1
   211: count = 1
   212: count = 1
   213: count = 1
   214: count = 1
   215: count = 1
   216: count = 1
   217: count = 1
   218: count = 1
   219: count = 65
   220: count = 65
   221: count = 65
   222: count = 65
   223: count = 65
   224: count = 1
   225: count = 1
   226: count = 91
   227: count = 1
   228: count = 1
   230: count = 1
   231: count = 1
   232: count = 5
   233: count = 1
   234: count = 1
   235: count = 14
   236: count = 1
   237: count = 1
   238: count = 1
   239: count = 1
   241: count = 1
   244: count = 1
   245: count = 91
   246: count = 13
   248: count = 1
   249: count = 1
   250: count = 38
   251: count = 38
   252: count = 38
   253: count = 38
   254: count = 38

timeUnit
     1: count = 1959
     4: count = 2

vertCoord
     1: count = 33
     2: count = 1
     3: count = 1
     4: count = 1
     7: count = 4
   100: count = 696
   101: count = 9
   102: count = 1
   103: count = 192
   104: count = 1
   105: count = 155
   106: count = 5
   107: count = 822
   109: count = 24
   112: count = 6
   116: count = 4
   200: count = 6

earthShape
     0: count = 1961

uvIsReletive
        false: count = 1961

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/us057g1011t48b180000000.grb

referenceDate
   2012-11-01T18:00:00Z: count = 2292

table version
     57-1-132: count = 2292

param
     1: count = 58
     2: count = 1
     7: count = 88
    11: count = 121
    13: count = 29
    14: count = 28
    17: count = 84
    20: count = 55
    33: count = 130
    34: count = 130
    39: count = 27
    40: count = 85
    41: count = 27
    43: count = 27
    44: count = 27
    52: count = 113
    53: count = 84
    54: count = 1
    62: count = 1
    63: count = 1
    65: count = 1
    71: count = 1
    77: count = 1
    81: count = 1
    85: count = 4
   111: count = 1
   112: count = 1
   121: count = 1
   122: count = 1
   131: count = 1
   134: count = 57
   135: count = 1
   136: count = 1
   137: count = 1
   140: count = 1
   141: count = 1
   142: count = 1
   143: count = 1
   145: count = 4
   146: count = 4
   148: count = 4
   149: count = 4
   151: count = 4
   152: count = 4
   155: count = 57
   157: count = 1
   158: count = 1
   160: count = 4
   161: count = 3
   163: count = 1
   165: count = 1
   166: count = 41
   167: count = 1
   168: count = 1
   169: count = 1
   170: count = 1
   171: count = 1
   173: count = 1
   191: count = 1
   193: count = 1
   194: count = 1
   197: count = 56
   199: count = 1
   208: count = 1
   209: count = 1
   210: count = 1
   211: count = 1
   212: count = 1
   213: count = 1
   214: count = 3
   215: count = 1
   216: count = 1
   217: count = 1
   218: count = 1
   219: count = 83
   220: count = 83
   221: count = 83
   222: count = 83
   223: count = 83
   224: count = 1
   225: count = 1
   226: count = 109
   227: count = 1
   228: count = 1
   230: count = 1
   231: count = 1
   232: count = 5
   233: count = 1
   234: count = 1
   235: count = 14
   236: count = 1
   237: count = 1
   238: count = 1
   239: count = 1
   241: count = 1
   244: count = 1
   245: count = 109
   246: count = 13
   248: count = 1
   249: count = 1
   250: count = 41
   251: count = 41
   252: count = 41
   253: count = 41
   254: count = 41

timeUnit
     1: count = 2292

vertCoord
     1: count = 36
     2: count = 1
     3: count = 1
     4: count = 1
     7: count = 4
   100: count = 756
   101: count = 9
   102: count = 1
   103: count = 192
   104: count = 1
   105: count = 155
   106: count = 6
   107: count = 1066
   109: count = 24
   112: count = 8
   113: count = 20
   116: count = 4
   200: count = 6
   218: count = 1

earthShape
     0: count = 2292

uvIsReletive
        false: count = 2292

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/wave-wind_238_20050418_0000_000.grb

referenceDate
   2005-04-18T00:00:00Z: count = 9

table version
        7-0-0: count = 9

param
    33: count = 1
    34: count = 1
   100: count = 1
   101: count = 1
   103: count = 1
   107: count = 1
   108: count = 1
   109: count = 1
   110: count = 1

timeUnit
     0: count = 9

vertCoord
     1: count = 9

earthShape
     0: count = 9

uvIsReletive
        false: count = 9

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/wrf-em.wmo

referenceDate
   2005-02-24T12:00:00Z: count = 145

table version
        7-0-2: count = 145

param
     1: count = 3
     2: count = 1
     7: count = 12
    11: count = 17
    13: count = 1
    17: count = 2
    24: count = 2
    33: count = 16
    34: count = 16
    39: count = 10
    41: count = 9
    51: count = 2
    52: count = 15
    54: count = 2
    61: count = 2
    62: count = 2
    63: count = 2
    65: count = 2
    71: count = 1
    85: count = 5
    99: count = 1
   121: count = 1
   122: count = 1
   130: count = 1
   131: count = 1
   132: count = 1
   135: count = 1
   140: count = 1
   141: count = 1
   142: count = 1
   143: count = 1
   144: count = 4
   156: count = 2
   157: count = 2
   190: count = 2
   196: count = 1
   197: count = 1

timeUnit
     0: count = 137
     4: count = 8

vertCoord
     1: count = 20
     2: count = 2
     3: count = 3
   100: count = 63
   101: count = 1
   102: count = 2
   103: count = 2
   105: count = 6
   106: count = 4
   111: count = 1
   112: count = 8
   116: count = 31
   200: count = 2

earthShape
     0: count = 145

uvIsReletive
        false: count = 145

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/wrf-nmm.wmo

referenceDate
   2005-02-24T12:00:00Z: count = 164

table version
        7-0-2: count = 164

param
     1: count = 3
     2: count = 1
     7: count = 12
    11: count = 17
    13: count = 1
    17: count = 10
    24: count = 2
    33: count = 16
    34: count = 16
    39: count = 10
    41: count = 9
    51: count = 2
    52: count = 15
    54: count = 2
    61: count = 2
    62: count = 2
    63: count = 2
    65: count = 2
    71: count = 1
    85: count = 5
    99: count = 1
   121: count = 2
   122: count = 2
   130: count = 1
   131: count = 1
   132: count = 1
   135: count = 1
   140: count = 1
   141: count = 1
   142: count = 1
   143: count = 1
   144: count = 4
   156: count = 2
   157: count = 2
   190: count = 2
   196: count = 1
   197: count = 1
   204: count = 1
   205: count = 2
   211: count = 3
   212: count = 3

timeUnit
     0: count = 149
     3: count = 7
     4: count = 8

vertCoord
     1: count = 29
     2: count = 2
     3: count = 3
     8: count = 2
   100: count = 71
   101: count = 1
   102: count = 2
   103: count = 2
   105: count = 6
   106: count = 4
   111: count = 1
   112: count = 8
   116: count = 31
   200: count = 2

earthShape
     0: count = 164

uvIsReletive
        false: count = 164

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/wrf.grib

referenceDate
   2005-02-22T00:00:00Z: count = 421

table version
     60-0-129: count = 1
       60-0-2: count = 420

param
     1: count = 1
     7: count = 40
    11: count = 41
    17: count = 40
    20: count = 1
    33: count = 40
    34: count = 40
    39: count = 39
    41: count = 39
    51: count = 1
    52: count = 40
    54: count = 1
    58: count = 39
    61: count = 2
    62: count = 1
    63: count = 1
    71: count = 1
    85: count = 4
   130: count = 1
   131: count = 1
   140: count = 1
   141: count = 1
   142: count = 1
   143: count = 1
   144: count = 4
   153: count = 39
   212: count = 1

timeUnit
     0: count = 417
     4: count = 4

vertCoord
     1: count = 12
   100: count = 390
   101: count = 1
   102: count = 1
   105: count = 6
   112: count = 8
   200: count = 3

earthShape
     0: count = 421

uvIsReletive
        false: count = 421

vertCoordInGDS

predefined

thin
 Q:/cdmUnitTest/formats/grib1/wrf_d03_201308080200.grb

referenceDate
   2013-08-06T06:00:00Z: count = 289

table version
     60-0-129: count = 21
     60-0-130: count = 5
       60-0-2: count = 263

param
     1: count = 4
     2: count = 1
     7: count = 18
    11: count = 16
    13: count = 12
    17: count = 15
    20: count = 1
    24: count = 1
    33: count = 15
    34: count = 15
    35: count = 7
    39: count = 11
    41: count = 7
    51: count = 13
    52: count = 15
    54: count = 2
    57: count = 1
    58: count = 11
    59: count = 1
    61: count = 1
    62: count = 1
    63: count = 1
    80: count = 1
    81: count = 1
    83: count = 1
    85: count = 5
    86: count = 1
    87: count = 1
    91: count = 1
   118: count = 1
   124: count = 1
   125: count = 1
   130: count = 1
   131: count = 1
   132: count = 1
   133: count = 11
   135: count = 8
   136: count = 1
   137: count = 1
   138: count = 1
   139: count = 1
   140: count = 2
   141: count = 1
   142: count = 1
   143: count = 1
   144: count = 4
   153: count = 11
   154: count = 1
   156: count = 4
   157: count = 4
   158: count = 7
   160: count = 4
   170: count = 11
   171: count = 11
   176: count = 1
   177: count = 1
   180: count = 1
   190: count = 2
   196: count = 1
   197: count = 1
   207: count = 1
   211: count = 4
   212: count = 1
   214: count = 1
   221: count = 1
   235: count = 1
   252: count = 1
   253: count = 1

timeUnit
     4: count = 6
    10: count = 283

vertCoord
     1: count = 31
     4: count = 1
     5: count = 2
     8: count = 1
   100: count = 194
   101: count = 1
   102: count = 2
   105: count = 8
   106: count = 4
   109: count = 4
   111: count = 1
   112: count = 14
   116: count = 17
   200: count = 7
   204: count = 1
   215: count = 1

earthShape
     0: count = 289

uvIsReletive
        false: count = 289

vertCoordInGDS

predefined

thin
ScanIssues - all files

referenceDate
   1900-01-01T12:00:00Z: count = 1
   1900-02-01T12:00:00Z: count = 1
   1900-03-01T12:00:00Z: count = 1
   1900-04-01T12:00:00Z: count = 1
   1900-05-01T12:00:00Z: count = 1
   1900-06-01T12:00:00Z: count = 1
   1900-07-01T12:00:00Z: count = 1
   1900-08-01T12:00:00Z: count = 1
   1900-09-01T12:00:00Z: count = 1
   1900-10-01T12:00:00Z: count = 1
   1993-01-01T00:00:00Z: count = 1
   1996-01-01T00:00:00Z: count = 1
   1997-01-01T00:00:00Z: count = 86
   1997-01-01T06:00:00Z: count = 86
   1997-07-01T00:00:00Z: count = 1
   2002-05-13T00:00:00Z: count = 45
   2002-12-02T22:00:00Z: count = 190
   2003-12-15T18:00:00Z: count = 1
   2004-08-13T12:00:00Z: count = 7203
   2004-09-17T00:00:00Z: count = 355
   2004-09-20T00:00:00Z: count = 1
   2004-09-22T00:00:00Z: count = 314
   2004-10-24T12:00:00Z: count = 215
   2004-11-12T00:00:00Z: count = 4
   2004-11-19T00:00:00Z: count = 116
   2004-12-11T15:00:00Z: count = 120
   2004-12-12T00:00:00Z: count = 901
   2004-12-15T12:00:00Z: count = 2
   2004-12-22T00:00:00Z: count = 8453
   2004-12-30T00:00:00Z: count = 53
   2005-01-05T00:00:00Z: count = 1171
   2005-01-20T02:10:00Z: count = 1
   2005-01-20T02:15:00Z: count = 1
   2005-01-22T18:00:00Z: count = 1233
   2005-02-01T18:00:00Z: count = 1
   2005-02-07T17:49:00Z: count = 1
   2005-02-22T00:00:00Z: count = 421
   2005-02-24T12:00:00Z: count = 309
   2005-02-27T12:00:00Z: count = 522
   2005-03-08T16:00:00Z: count = 746
   2005-04-14T00:00:00Z: count = 1
   2005-04-15T12:00:00Z: count = 1
   2005-04-18T00:00:00Z: count = 9
   2005-05-04T00:00:00Z: count = 10
   2005-05-04T12:00:00Z: count = 1
   2005-05-06T00:00:00Z: count = 2
   2005-05-06T18:00:00Z: count = 3
   2005-07-07T00:00:00Z: count = 1961
   2005-07-14T06:00:00Z: count = 448
   2005-07-27T12:00:00Z: count = 845
   2005-09-15T00:00:00Z: count = 565
   2006-03-02T00:00:00Z: count = 175
   2006-07-17T12:00:00Z: count = 56
   2006-07-20T12:00:00Z: count = 3
   2006-08-14T00:00:00Z: count = 96
   2006-09-14T00:00:00Z: count = 2
   2006-10-11T00:00:00Z: count = 2
   2006-10-21T00:00:00Z: count = 901
   2007-01-04T18:00:00Z: count = 788
   2007-03-07T00:00:00Z: count = 22
   2007-04-11T00:00:00Z: count = 300
   2007-04-11T03:00:00Z: count = 301
   2007-04-11T06:00:00Z: count = 302
   2007-05-04T00:00:00Z: count = 869
   2007-11-19T00:00:00Z: count = 8
   2007-11-19T06:00:00Z: count = 2239
   2007-11-20T06:00:00Z: count = 1584
   2009-06-28T18:00:00Z: count = 37
   2009-07-01T06:00:00Z: count = 205
   2010-01-12T00:00:00Z: count = 1
   2010-06-03T06:00:00Z: count = 2
   2010-10-05T00:00:00Z: count = 4
   2010-10-05T01:00:00Z: count = 4
   2010-10-05T02:00:00Z: count = 4
   2010-10-05T03:00:00Z: count = 6
   2010-10-05T04:00:00Z: count = 6
   2010-10-05T05:00:00Z: count = 4
   2010-10-05T06:00:00Z: count = 6
   2010-10-05T07:00:00Z: count = 5
   2010-10-05T08:00:00Z: count = 5
   2010-10-05T09:00:00Z: count = 5
   2010-10-05T10:00:00Z: count = 3
   2010-10-05T11:00:00Z: count = 2
   2010-10-05T12:00:00Z: count = 12
   2010-10-05T13:00:00Z: count = 4
   2010-10-05T14:00:00Z: count = 4
   2010-10-05T15:00:00Z: count = 4
   2010-10-05T16:00:00Z: count = 4
   2010-10-05T17:00:00Z: count = 3
   2010-10-05T18:00:00Z: count = 4
   2010-10-05T19:00:00Z: count = 3
   2010-10-05T20:00:00Z: count = 5
   2010-10-05T21:00:00Z: count = 4
   2010-10-05T22:00:00Z: count = 2
   2010-10-05T23:00:00Z: count = 5
   2012-11-01T18:00:00Z: count = 2292
   2013-08-06T06:00:00Z: count = 289

table version
      131-0-2: count = 1
       34-0-3: count = 96
       54-0-1: count = 10
       54-0-2: count = 565
     57-1-132: count = 2292
       57-1-2: count = 2422
       58-0-2: count = 182
       58-0-3: count = 3
      58-42-2: count = 172
     60-0-129: count = 22
     60-0-130: count = 5
       60-0-2: count = 683
       60-1-2: count = 2
     60-255-2: count = 2
        7-0-0: count = 9
        7-0-1: count = 1
      7-0-129: count = 276
      7-0-130: count = 54
        7-0-2: count = 21504
        7-1-2: count = 1
    7-138-130: count = 37
     7-15-131: count = 903
        7-2-2: count = 7170
        7-4-3: count = 16
        7-8-2: count = 120
       74-0-2: count = 45
     78-255-2: count = 3
        8-0-2: count = 1
    9-157-128: count = 68
      9-157-2: count = 40
       96-0-1: count = 1
       98-0-1: count = 22
     98-0-128: count = 3
       98-0-2: count = 56
       99-0-1: count = 205

param
     1: count = 652
     2: count = 530
     7: count = 4612
     8: count = 20
    10: count = 2
    11: count = 4668
    13: count = 137
    14: count = 53
    15: count = 310
    16: count = 310
    17: count = 958
    18: count = 90
    20: count = 163
    21: count = 1
    24: count = 34
    27: count = 4
    33: count = 4735
    34: count = 4733
    35: count = 217
    39: count = 1625
    40: count = 253
    41: count = 535
    43: count = 51
    44: count = 51
    51: count = 847
    52: count = 3161
    53: count = 202
    54: count = 105
    57: count = 3
    58: count = 260
    59: count = 39
    60: count = 2
    61: count = 482
    62: count = 47
    63: count = 138
    65: count = 48
    66: count = 26
    67: count = 1
    71: count = 49
    72: count = 5
    73: count = 19
    74: count = 19
    75: count = 19
    76: count = 2
    77: count = 56
    80: count = 30
    81: count = 24
    83: count = 6
    84: count = 20
    85: count = 131
    86: count = 13
    87: count = 7
    88: count = 20
    89: count = 1
    90: count = 2
    91: count = 23
    92: count = 1
    99: count = 2
   100: count = 34
   101: count = 17
   103: count = 17
   105: count = 1
   107: count = 1
   108: count = 1
   109: count = 1
   110: count = 1
   111: count = 4
   112: count = 4
   118: count = 5
   121: count = 27
   122: count = 28
   124: count = 8
   125: count = 8
   129: count = 26
   130: count = 57
   131: count = 76
   132: count = 55
   133: count = 13
   134: count = 107
   135: count = 28
   136: count = 42
   137: count = 10
   138: count = 6
   139: count = 6
   140: count = 62
   141: count = 61
   142: count = 59
   143: count = 56
   144: count = 114
   145: count = 15
   146: count = 12
   147: count = 4
   148: count = 11
   149: count = 8
   151: count = 10
   152: count = 8
   153: count = 440
   154: count = 23
   155: count = 107
   156: count = 120
   157: count = 122
   158: count = 823
   160: count = 20
   161: count = 35
   162: count = 4
   163: count = 2
   165: count = 3
   166: count = 80
   167: count = 2
   168: count = 2
   169: count = 2
   170: count = 275
   171: count = 278
   173: count = 2
   176: count = 1
   177: count = 2
   178: count = 137
   179: count = 50
   180: count = 24
   181: count = 5
   186: count = 64
   187: count = 4
   188: count = 1
   189: count = 51
   190: count = 79
   191: count = 5
   192: count = 43
   193: count = 2
   194: count = 2
   196: count = 39
   197: count = 136
   198: count = 50
   199: count = 3
   200: count = 2
   201: count = 2
   203: count = 8
   204: count = 12
   205: count = 13
   206: count = 4
   207: count = 9
   208: count = 9
   209: count = 2
   210: count = 3
   211: count = 239
   212: count = 22
   213: count = 2
   214: count = 12
   215: count = 2
   216: count = 12
   217: count = 2
   218: count = 2
   219: count = 153
   220: count = 148
   221: count = 160
   222: count = 192
   223: count = 158
   224: count = 21
   225: count = 21
   226: count = 208
   227: count = 2
   228: count = 2
   230: count = 10
   231: count = 21
   232: count = 10
   233: count = 3
   234: count = 3
   235: count = 30
   236: count = 62
   237: count = 70
   238: count = 10
   239: count = 4
   240: count = 8
   241: count = 7
   242: count = 4
   243: count = 2
   244: count = 6
   245: count = 202
   246: count = 33
   247: count = 7
   248: count = 9
   249: count = 7
   250: count = 89
   251: count = 129
   252: count = 85
   253: count = 85
   254: count = 79

timeUnit
     0: count = 17969
     1: count = 4553
     2: count = 619
     3: count = 102
     4: count = 770
     7: count = 10
    10: count = 12968
    51: count = 1

vertCoord
     1: count = 2360
     2: count = 30
     3: count = 37
     4: count = 97
     5: count = 2
     6: count = 192
     7: count = 382
     8: count = 7
   100: count = 24547
   101: count = 74
   102: count = 621
   103: count = 531
   104: count = 2
   105: count = 2705
   106: count = 92
   107: count = 2187
   108: count = 74
   109: count = 1037
   111: count = 25
   112: count = 268
   113: count = 20
   116: count = 1253
   117: count = 24
   119: count = 36
   160: count = 40
   200: count = 192
   204: count = 10
   206: count = 5
   207: count = 5
   211: count = 2
   212: count = 2
   213: count = 4
   214: count = 19
   215: count = 5
   218: count = 1
   222: count = 2
   223: count = 4
   224: count = 19
   232: count = 2
   233: count = 4
   234: count = 19
   242: count = 8
   243: count = 18
   244: count = 3
   245: count = 5
   248: count = 5
   249: count = 5
   251: count = 5
   252: count = 5

earthShape
     0: count = 36985
     1: count = 7

uvIsReletive
        false: count = 36985
         true: count = 7

vertCoordInGDS
   103: count = 5
   105: count = 4
   109: count = 200

predefined
    21: count = 31
    22: count = 31
    23: count = 31
    24: count = 31
    25: count = 56
    26: count = 56
    61: count = 1
    62: count = 1
    63: count = 1
    64: count = 1

thin
     0: count = 8884

 */
