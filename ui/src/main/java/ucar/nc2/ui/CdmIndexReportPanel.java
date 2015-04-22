/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.ui;

import com.google.common.base.*;
import com.google.common.base.Objects;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import thredds.inventory.MCollection;
import ucar.coord.SparseArray;
import ucar.nc2.grib.collection.*;
import ucar.nc2.ui.grib.Grib2ReportPanel;
import ucar.nc2.util.CloseableIterator;
import ucar.nc2.util.Indent;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Run through ncx indices and make reports
 *
 * @author caron
 * @since 5/15/2014
 */
public class CdmIndexReportPanel extends ReportPanel {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2ReportPanel.class);

  public static enum Report {
    dupAndMissing,
    misplacedFlds             // find misplaced records for NCDC
  }

  public CdmIndexReportPanel(PreferencesExt prefs) {
    super(prefs);
  }

  @Override
  public Object[] getOptions() {
    return ucar.nc2.ui.CdmIndexReportPanel.Report.values();
  }

  @Override
  protected void doReport(Formatter f, Object option, MCollection dcm, boolean useIndex, boolean eachFile, boolean extra) throws IOException {
      switch ((Report) option) {
        case dupAndMissing:
          doDupAndMissing(f, dcm, eachFile, extra);
          break;

        case misplacedFlds:
          doMisplacedFields(f, dcm, useIndex, eachFile, extra);
          break;
      }

  }

  ///////////////////////////////////////////////

  private static class Accum {
    int nrecords, ndups, nmissing;

    Accum add(GribCollectionImmutable.VariableIndex v) {
      this.nrecords += v.getNrecords();
      this.ndups += v.getNdups();
      this.nmissing += v.getNmissing();
      return this;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
              .add("nrecords", nrecords)
              .add("ndups", ndups)
              .add("nmissing", nmissing)
              .toString();
    }
  }

  protected void doDupAndMissing(Formatter f, MCollection dcm, boolean eachFile, boolean extra) throws IOException {
    Accum total = new Accum();
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) { // not sorted
      while (iter != null && iter.hasNext()) {
        doDupAndMissingEach(f, iter.next(), eachFile, extra, total);
      }
    }
    f.format("total %s%n", total);
  }

    // seperate report for each file in collection
  private void doDupAndMissingEach(Formatter f, MFile mfile, boolean each, boolean extra, Accum accum) throws IOException {

    if (each) f.format("%nFile %s%n", mfile.getPath());
    FeatureCollectionConfig config = new FeatureCollectionConfig();

    GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(mfile.getPath(), config, false, logger);
    if (gc == null) {
      f.format("Not a grib collection index file=%s%n" + mfile.getPath());
      return;
    }

    for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
      if (each) f.format("%nDataset %s%n", ds.getType());
      for (GribCollectionImmutable.GroupGC g : ds.getGroups()) {
        Accum groupAccum = new Accum();
        if (each) f.format(" Group %s%n", g.getDescription());
        for (GribCollectionImmutable.VariableIndex v : g.getVariables()) {
          if (each && extra) f.format("  %s%n", v.toStringFrom());
          else showIfNonZero(f, v, mfile.getPath());

          groupAccum.add(v);
          accum.add(v);
        }
        if (each) {
          f.format("total %s%n", groupAccum);
        }
      }
    }

  }

  String lastFilename = null;
  void showIfNonZero(Formatter f, GribCollectionImmutable.VariableIndex v, String filename) {
    if ((v.getNdups() != 0) || (v.getNmissing() != 0)) {
      if (!filename.equals(lastFilename))
        f.format(" %s%n  %s%n", filename, v.toStringFrom());
      else
        f.format("  %s%n", v.toStringFrom());
      lastFilename = filename;
    }
  }

  ///////////////////////////////////////////////////
  private static final int MIN_COUNT = 400;

  protected void doMisplacedFields(Formatter f, MCollection dcm, boolean useIndex, boolean eachFile, boolean extra) throws IOException {

    if (eachFile) {

      Set<String> filenames = new HashSet<>();
      try (CloseableIterator<MFile> iter = dcm.getFileIterator()) { // not sorted
        while (iter != null && iter.hasNext()) {
          doMisplacedFieldsEach(f, iter.next(), filenames, extra);
        }
      }

      f.format("%nAll files%n");
      for (String filename : filenames)
      f.format("  %s%n", filename);


    } else {  // eachFile false
          doMisplacedFields(f, dcm, extra);

    }
  }

  // seperate report for each file in collection
  private void doMisplacedFieldsEach(Formatter f2, MFile mfile, Set<String> filenames, boolean extra) throws IOException {
    Formatter f = new Formatter(System.out);
    f.format("Check Misplaced Fields for %s, records count < %d%n", mfile, MIN_COUNT);
    Map<Integer, VarInfo> varCount = new HashMap<>();
    int countMisplaced = 0;

    f.format("%n%s%n", mfile.getPath());
    countTop(mfile.getPath(), f, varCount);

    f.format("%nTotals%n");
    List<VarInfo> sorted = new ArrayList<>(varCount.values());
    Collections.sort(sorted);
    for (VarInfo vinfo : sorted) {
      f.format(" %20s = %d%n", vinfo.name, vinfo.count);
      if (vinfo.count > MIN_COUNT) vinfo.ok = true; // LOOK arbitrary cutoff
      if (!vinfo.ok) countMisplaced += vinfo.count;
    }
    f.format("countMisplaced = %d%n", countMisplaced);

    countMisplaced = 0; // count again
    f.format("%nFind Misplaced Files%n");
    File indexFile = new File(mfile.getPath());
    countMisplaced += doOneIndex(indexFile, f, varCount, filenames, new Indent(2), extra);
    f.format("%nDone countMisplaced=%d (n < %d)%n%nFiles%n", MIN_COUNT, countMisplaced);
    f2.format("%s", f.toString());
  }

   //  report over all files in collection
  private void doMisplacedFields(Formatter f, MCollection dcm, boolean extra) throws IOException {
     f.format("Check Misplaced Fields, records count < %d%n", MIN_COUNT);
     Map<Integer, VarInfo> varCount = new HashMap<>();

     for (MFile mfile : dcm.getFilesSorted()) {
       f.format("%n%s%n", mfile.getPath());
       countTop(mfile.getPath(), f, varCount);
     }

     int countMisplaced=0;
     f.format("%nTotals%n");
     List<VarInfo> sorted = new ArrayList<>(varCount.values());
     Collections.sort(sorted);
     for (VarInfo vinfo : sorted) {
       f.format(" %20s = %d%n", vinfo.name, vinfo.count);
       if (vinfo.count > MIN_COUNT) vinfo.ok = true; // LOOK arbitrary cutoff
       if (!vinfo.ok) countMisplaced += vinfo.count;
     }
     f.format("countMisplaced = %d%n", countMisplaced);

     Set<String> filenames = new HashSet<>();
     countMisplaced=0; // count again
     f.format("%nFind Misplaced Files%n");
     for (MFile mfile : dcm.getFilesSorted()) {
       File indexFile = new File(mfile.getPath());
       countMisplaced += doOneIndex(indexFile, f, varCount, filenames, new Indent(2), extra);
     }
     f.format("%nDone countMisplaced=%d (n < %d)%n%nFiles%n", MIN_COUNT, countMisplaced);
     for (String filename : filenames)
       f.format("  %s%n", filename);
   }

  ///////////////////////////////////////////////

  private static class VarInfo implements Comparable<VarInfo> {
    String name;
    int count = 0;
    boolean ok;

    private VarInfo(String name) {
      this.name = name;
    }

    @Override
    public int compareTo(VarInfo o) {
      return name.compareTo(o.name);
    }
  }

  public void countTop(String indexFile, Formatter f, Map<Integer, VarInfo> varCount) throws IOException {
    // f.format("Dataset %s%n", indexFile.getPath());

    FeatureCollectionConfig config = new FeatureCollectionConfig();
    try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(indexFile, config, false, logger)) {
      if (gc == null)
        throw new IOException(indexFile+ " not a grib collection index file");

      for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
        if (!ds.getType().equals(GribCollectionImmutable.Type.TwoD)) continue;
        for (GribCollectionImmutable.GroupGC g : ds.getGroups()) {
          f.format(" Group %s%n", g.getDescription());

          for (GribCollectionImmutable.VariableIndex vi : g.getVariables()) {
            String name = vi.makeVariableName();                    // LOOK not actually right - some are partitioned by level
            int nrecords = vi.getNRecords();
            f.format("  %7d: %s%n", nrecords, name);
            int hash = vi.hashCode() + g.getGdsHash().hashCode(); // must be both group and var
            VarInfo vinfo = varCount.get(hash);
            if (vinfo == null) {
              vinfo = new VarInfo(name);
              varCount.put(hash, vinfo);
            }
            vinfo.count += nrecords;
          }
        }
      }
    }
  }

  // recursively look for leaf files of records in vars
  private int doOneIndex(File indexFile, Formatter f, Map<Integer, VarInfo> varCount, Set<String> filenames, Indent indent, boolean showScan) throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig();

    try (ucar.unidata.io.RandomAccessFile raf = new RandomAccessFile(indexFile.getPath(), "r")) {
      GribCdmIndex.GribCollectionType type = GribCdmIndex.getType(raf);
      if (showScan) f.format("%sIndex %s type=%s", indent, indexFile, type);
    }

    int totalMisplaced = 0;

    //File parent = indexFile.getParentFile();
    try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(indexFile.getPath(), config, false, logger)) {
      if (gc == null)
        throw new IOException(indexFile+ " not a grib collection index file");

      // see if it has any misplaced
      int countMisplaced = 0;
      for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
        if (ds.getType().equals(GribCollectionImmutable.Type.Best)) continue;
        for (GribCollectionImmutable.GroupGC g : ds.getGroups()) {
          for (GribCollectionImmutable.VariableIndex vi : g.getVariables()) {
            int hash = vi.hashCode() + g.getGdsHash().hashCode();
            VarInfo vinfo = varCount.get(hash);
            if (vinfo == null) f.format("ERROR on vi %s%n", vi);
            else{
              if (!vinfo.ok) countMisplaced += vi.getNRecords();
            }
          }
        }
      }

      if (countMisplaced == 0) {
        if (showScan) f.format(" none%n");
        return 0;
      }

      indent.incr();
      if (gc instanceof PartitionCollectionImmutable) {
        PartitionCollectionImmutable pc = (PartitionCollectionImmutable) gc;
        boolean isPoP =  pc.isPartitionOfPartitions();
        if (showScan) f.format(" isPofP=%s%n", isPoP);
        for (PartitionCollectionImmutable.Partition partition : pc.getPartitions()) {
          //File partParent = new File(partition.getDirectory());
          //File reparent =  new File(parent, partParent.getName());
          // File nestedIndex =  isPoP ? new File(reparent, partition.getFilename()) : new File(parent, partition.getFilename()); // JMJ
          File nestedIndex =  new File(partition.getIndexFilenameInCache());
          if (showScan) f.format("%sPartition index= %s exists=%s%n", indent, nestedIndex, nestedIndex.exists());
          if (nestedIndex.exists()) {
            totalMisplaced += doOneIndex(nestedIndex, f, varCount, filenames, indent.incr(), showScan);
            indent.decr();
          } else {
            f.format("%sdir=%s filename=%s nestedIndex %s NOT EXIST%n", indent, gc.getDirectory(), partition.getFilename(), nestedIndex.getPath());
          }
        }

      } else {
        if (showScan) f.format("%n");
        f.format("%sIndex %s count=%d%n", indent, indexFile, countMisplaced);
        indent.incr();

        for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
          if (ds.getType().equals(GribCollectionImmutable.Type.Best)) continue;
          for (GribCollectionImmutable.GroupGC g : ds.getGroups()) {
            for (GribCollectionImmutable.VariableIndex vi : g.getVariables()) {
              int hash = vi.hashCode() + g.getGdsHash().hashCode();
              VarInfo vinfo = varCount.get(hash);
              if (!vinfo.ok) {
                vi.readRecords();
                if (vi.getSparseArray() != null) {
                  SparseArray<GribCollectionImmutable.Record> sa = vi.getSparseArray();
                  for (GribCollectionImmutable.Record record : sa.getContent()) {
                    String filename = gc.getFilename(record.fileno);
                    f.format(">%s%s: %s at pos %d%n", indent, vinfo.name, filename, record.pos);
                    totalMisplaced++;
                    filenames.add(filename);
                  }
                }
              }
            }
          }
        }
        indent.decr();
      }
    }

    indent.decr();
    return totalMisplaced;
  }


}

