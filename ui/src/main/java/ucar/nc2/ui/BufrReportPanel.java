package ucar.nc2.ui;

import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.iosp.bufr.MessageScanner;
import ucar.nc2.iosp.bufr.writer.BufrSplitter2;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;

import java.io.IOException;
import java.util.*;

/**
 * BUFR reports
 *
 * @author caron
 * @since 8/22/13
 */
public class BufrReportPanel extends ReportPanel {

  public static enum Report {
    checkHash, bufrSplitter
  }

  public BufrReportPanel(PreferencesExt prefs) {
    super(prefs);
  }

  @Override
  public Object[] getOptions() {
    return ucar.nc2.ui.BufrReportPanel.Report.values();
  }

  @Override
  protected void doReport(Formatter f, Object option, MCollection dcm, boolean useIndex, boolean eachFile, boolean extra) throws IOException {
    Report which = (Report) option;
    switch (which) {
      case bufrSplitter:
        doBufrSplitter(f, dcm, useIndex);
        break;
      case checkHash:
         doCheckHash(f, dcm, useIndex);
         break;
     }
  }

  ///////////////////////////////////////////////

  private void doCheckHash(Formatter f, MCollection dcm, boolean useIndex) throws IOException {
    f.format("Check Files for hash consistency%n");
    int[] accum = new int[4];
    TrackMessageTypes all = new TrackMessageTypes();

    for (MFile mfile : dcm.getFilesSorted()) {
      String path = mfile.getPath();
      if (path.endsWith(".ncx")) continue;
      f.format("%n %s%n", path);
      try {
        doCheckHash(mfile, f, all, accum);

      } catch (Throwable t) {
        System.out.printf("FAIL on %s%n", mfile.getPath());
        t.printStackTrace();
      }
    }

    f.format("%n================%nTotals countMess=%d countObs = %d%n", accum[0], accum[1]);
    show(all, f);
  }

  private static class Count {
    int nmess = 1;
    int nobs = 0;

    private Count(int nobs) {
      this.nobs = nobs;
    }
  }

  private static class TrackMessageTypes {
    HashMap<Message, Count> map = new HashMap<Message, Count>();

    void add(Message m) {
      Count hashCount = map.get(m);
      if (hashCount == null) {
        map.put(m, new Count(m.getNumberDatasets()));
      } else {
        hashCount.nmess++;
        hashCount.nobs += m.getNumberDatasets();
      }
    }
  }

  private void doCheckHash(MFile ff, Formatter fm, TrackMessageTypes all, int[] accum) throws IOException {
    TrackMessageTypes oneFile = new TrackMessageTypes();

    int countMess = 0;
    int countObs = 0;
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(ff.getPath(), "r");
      MessageScanner scan = new MessageScanner(raf, 0, true);
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) continue;

        oneFile.add(m);
        all.add(m);

        countMess++;
        countObs += m.getNumberDatasets();
      }
    } finally {
      if (raf != null) raf.close();
    }

    show(oneFile, fm);

    accum[0] += countMess;
    accum[1] += countObs;
  }

  private void show(TrackMessageTypes track, Formatter f) throws IOException {
    List<Message> mess = new ArrayList<Message>(track.map.keySet());
    Collections.sort(mess, new Comparator<Message>() {
      @Override
      public int compare(Message o1, Message o2) {
        return o1.getLookup().getCategoryNo().compareTo(o2.getLookup().getCategoryNo());
      }
    });

    f.format("  nmess / nobs (hash) (ddsHash) category - center %n");
    for (Message m : mess) {
      Count hashCount = track.map.get(m);
      f.format("   %6d/%7d (%8s) (%8s) %s - %s%n", hashCount.nmess, hashCount.nobs,
              Integer.toHexString(m.hashCode()),
              Integer.toHexString(m.dds.getDataDescriptors().hashCode()),
              m.getLookup().getCategoryFullName(), m.getLookup().getCenterNo());
    }
  }

    ///////////////////////////////////////////////

  private void doBufrSplitter(Formatter f, MCollection dcm, boolean useIndex) throws IOException {
    long start = System.currentTimeMillis();
    String dirName = dcm.getRoot() +"/split"; // LOOK temp kludge

    f.format("BufrSplitter on files in collection %s, write to %s%n", dcm, dirName);
    BufrSplitter2 splitter = new BufrSplitter2(dirName, f);

    for (MFile mfile : dcm.getFilesSorted()) {
      String path = mfile.getPath();
      if (path.endsWith(".ncx")) continue;
      f.format("%n %s%n", path);
      System.out.printf(" BufrSplitter on %s%n", path);
      long start2 = System.currentTimeMillis();
      try {
        splitter.execute(path);
        long took2 = System.currentTimeMillis() - start2;
        System.out.printf("  %s took %s msecs%n", path, took2);

      } catch (Throwable t) {
        System.out.printf("FAIL on %s%n", mfile.getPath());
        t.printStackTrace();
      }
    }
    splitter.exit();
    long took = (System.currentTimeMillis() - start) /1000;
    System.out.printf("That took %s secs%n", took);
  }
}
