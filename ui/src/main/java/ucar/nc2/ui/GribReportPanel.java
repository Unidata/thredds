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
 * Describe
 *
 * @author caron
 * @since Dec 13, 2010
 */
public class GribReportPanel extends JPanel {
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

  public void setCollection(String spec) throws IOException {
    Formatter f = new Formatter();
    CollectionSpecParser parser = new CollectionSpecParser(spec, f);

    f.format("top dir = %s%n", parser.getTopDir());
    f.format("filter = %s%n", parser.getFilter());

    File top = new File(parser.getTopDir());
    if (!top.exists()) {
      f.format("top dir = %s does not exist%n", parser.getTopDir());
    } else {

      Map<Long, GdsList> gdsSet = new HashMap<Long, GdsList>();
      for (File file : top.listFiles( new GribFilter( parser.getFilter()))) {
        f.format(" %s%n", file.getPath());
        doOne(file, f, gdsSet);
      }

      for (GdsList gdsl : gdsSet.values()) {
        f.format("%nGDS = %d x %d (%d) %n", gdsl.gds.getNy(), gdsl.gds.getNx(), gdsl.gds.getGdtn());
        for (FileCount fc : gdsl.fileList)
          f.format("  %5d %s%n", fc.count, fc.f.getPath());
      }
    }

    /* DatasetCollectionManager dc = DatasetCollectionManager.open(spec, null, f);
    if (dc != null) {
      dc.scan(null);
      for (MFile mfile : dc.getFiles()) {
        f.format(" %s%n", mfile);
       }
    } */
    reportPane.setText(f.toString());
    reportPane.gotoTop();
  }

  public void save() {
  }

  public void showInfo(Formatter f) {

  }

  private class GribFilter implements FileFilter {
    Pattern pattern;
    GribFilter( Pattern p) {
      this.pattern = p;
    }
    public boolean accept(File file) {
      java.util.regex.Matcher matcher = this.pattern.matcher(file.getName());
      return matcher.matches();
    }
  }

  private void doOne(File ff, Formatter f, Map<Long, GdsList> gdsSet)  throws IOException {
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
}
