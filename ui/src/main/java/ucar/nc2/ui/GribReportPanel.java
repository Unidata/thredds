package ucar.nc2.ui;

import thredds.inventory.CollectionSpecParser;
import thredds.inventory.DatasetCollectionManager;
import thredds.inventory.MFile;
import ucar.grib.GribGridRecord;
import ucar.grib.grib2.*;
import ucar.grid.GridIndex;
import ucar.grid.GridRecord;
import ucar.nc2.NetcdfFile;
import ucar.nc2.iosp.grib.GribGridServiceProvider;
import ucar.nc2.iosp.grid.GridServiceProvider;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.util.Misc;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import java.awt.*;
import java.awt.List;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Run through Grib files and make reports
 *
 * @author caron
 * @since Dec 13, 2010
 */
public class GribReportPanel extends JPanel {
  public static enum Report {
    localUseSection, uniqueGds
  }

  ;

  private PreferencesExt prefs;

  private BeanTableSorted recordTable, gdsTable, productTable;
  private JSplitPane split, split2;

  private TextHistoryPane reportPane;

  public GribReportPanel(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    // the info windows
    reportPane = new TextHistoryPane();

    setLayout(new BorderLayout());

    add(reportPane, BorderLayout.CENTER);
  }

  public void setCollection(String spec, boolean useIndex, Report which) throws IOException {
    Formatter f = new Formatter();
    f.format("%s %s %s%n", spec, useIndex, which);

    /* DatasetCollectionManager dc = DatasetCollectionManager.open(spec, null, f);
  if (dc != null) {
    dc.scan(null);
    for (MFile mfile : dc.getFiles()) {
      f.format(" %s%n", mfile);
     }
  }  */

    CollectionSpecParser parser = new CollectionSpecParser(spec, f);

    f.format("top dir = %s%n", parser.getTopDir());
    f.format("filter = %s%n", parser.getFilter());
    reportPane.setText(f.toString());

    File top = new File(parser.getTopDir());
    if (!top.exists()) {
      f.format("top dir = %s does not exist%n", parser.getTopDir());
    } else {

      switch (which) {
        case uniqueGds:
          doUniqueGds(f, parser, useIndex);
          break;
        case localUseSection:
          doLocalUseSection(f, parser, useIndex);
          break;
      }
    }

    reportPane.setText(f.toString());
    reportPane.gotoTop();
  }

  ///////////////////////////////////////////////

  private void doLocalUseSection(Formatter f, CollectionSpecParser parser, boolean useIndex) throws IOException {
    f.format("Show Local Use Section%n");

    File top = new File(parser.getTopDir());
    for (File file : top.listFiles(new GribFilter(parser.getFilter()))) {
      f.format(" %s%n", file.getPath());
      doLocalUseSection(file, f, useIndex);
    }
  }

  private void doLocalUseSection(File ff, Formatter f, boolean useIndex) throws IOException {
    f.format("File = %s%n", ff);

    RandomAccessFile raf = new RandomAccessFile(ff.getPath(), "r");
    Grib2Input reader = new Grib2Input(raf);
    raf.seek(0);
    raf.order(RandomAccessFile.BIG_ENDIAN);
    reader.scan(false, false);

    for (Grib2Record gr : reader.getRecords()) {
      byte[] lus = gr.getLocalUseSection();
      if (lus == null)
        f.format(" %10d == none%n", gr.getPdsOffset());
      else
        f.format(" %10d == %s%n", gr.getPdsOffset(), Misc.showBytes(lus));
    }
    raf.close();
  }

  ///////////////////////////////////////////////

  private void doUniqueGds(Formatter f, CollectionSpecParser parser, boolean useIndex) throws IOException {
    f.format("Show Unique GDS%n");

    File top = new File(parser.getTopDir());

    Map<Long, GdsList> gdsSet = new HashMap<Long, GdsList>();
    for (File file : top.listFiles(new GribFilter(parser.getFilter()))) {
      f.format(" %s%n", file.getPath());
      doUniqueGds(file, useIndex, gdsSet);
    }

    for (GdsList gdsl : gdsSet.values()) {
      f.format("%nGDS = %d x %d (%d) %n", gdsl.gds.getNy(), gdsl.gds.getNx(), gdsl.gds.getGdtn());
      for (FileCount fc : gdsl.fileList)
        f.format("  %5d %s%n", fc.count, fc.f.getPath());
    }
  }

  private class GribFilter implements FileFilter {
    Pattern pattern;

    GribFilter(Pattern p) {
      this.pattern = p;
    }

    public boolean accept(File file) {
      java.util.regex.Matcher matcher = this.pattern.matcher(file.getName());
      return matcher.matches();
    }
  }

  private void doUniqueGds(File ff, boolean useIndex, Map<Long, GdsList> gdsSet) throws IOException {

    RandomAccessFile raf = new RandomAccessFile(ff.getPath(), "r");
    Grib2Input reader = new Grib2Input(raf);
    raf.seek(0);
    raf.order(RandomAccessFile.BIG_ENDIAN);
    reader.scan(false, false);

    for (Grib2Record gr : reader.getRecords()) {
      Grib2GridDefinitionSection gds = gr.getGDS();
      long crc = gds.getGdsVars().calcCRC();
      GdsList gdsl = gdsSet.get(crc);
      if (gdsl == null) {
        gdsl = new GdsList(gds);
        gdsSet.put(crc, gdsl);
      }
      FileCount fc = gdsl.contains(ff);
      if (fc == null) {
        fc = new FileCount(ff);
        gdsl.fileList.add(fc);
      }
      fc.count++;
    }
    raf.close();
  }

  private class GdsList {
    Grib2GridDefinitionSection gds;
    java.util.List<FileCount> fileList = new ArrayList<FileCount>();

    private GdsList(Grib2GridDefinitionSection gds) {
      this.gds = gds;
    }

    FileCount contains(File f) {
      for (FileCount fc : fileList)
        if (fc.f.equals(f)) return fc;
      return null;
    }

  }

  private class FileCount {
    private FileCount(File f) {
      this.f = f;
    }

    File f;
    int count = 0;
  }

  ///////////////////////////////////////////////

  public void save() {
  }

  public void showInfo(Formatter f) {
  }
}
