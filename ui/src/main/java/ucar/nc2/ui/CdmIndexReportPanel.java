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

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import thredds.inventory.MCollection;
import ucar.coord.SparseArray;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollection;
import ucar.nc2.grib.collection.PartitionCollection;
import ucar.nc2.util.CloseableIterator;
import ucar.nc2.util.Indent;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;

import java.io.*;
import java.util.*;

/**
 * Run through ncx2 indices and make reports
 *
 * @author caron
 * @since Dec 13, 2010
 */
public class CdmIndexReportPanel extends ReportPanel {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2ReportPanel.class);

  public static enum Report {
    misplacedFlds
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

    if (eachFile) {

      Set<String> filenames = new HashSet<>();
      try (CloseableIterator<MFile> iter = dcm.getFileIterator()) { // not sorted
        while (iter.hasNext()) {
          switch ((Report) option) {
            case misplacedFlds:
              doMisplacedFieldsEach(f, iter.next(), filenames, extra);
              break;
          }
        }
      }

      f.format("%nAll files%n");
      for (String filename : filenames)
      f.format("  %s%n", filename);


    } else {  // eachFile false

      switch ((Report) option) {
        case misplacedFlds:
          doMisplacedFields(f, dcm, useIndex, extra);
          break;
      }

    }
  }

  // seperate report for each file in collection
  private void doMisplacedFieldsEach(Formatter f2, MFile mfile, Set<String> filenames, boolean extra) throws IOException {
    Formatter f = new Formatter(System.out);
    f.format("Check Misplaced Fields for %s%n", mfile);
    Map<Integer, VarInfo> varCount = new HashMap<>();
    int countMisplaced = 0;

    f.format("%n%s%n", mfile.getPath());
    countTop(mfile.getPath(), f, varCount);

    f.format("%nTotals%n");
    List<VarInfo> sorted = new ArrayList<>(varCount.values());
    Collections.sort(sorted);
    for (VarInfo vinfo : sorted) {
      f.format(" %20s = %d%n", vinfo.name, vinfo.count);
      if (vinfo.count > 400) vinfo.ok = true; // LOOK arbitrary cutoff
      if (!vinfo.ok) countMisplaced += vinfo.count;
    }
    f.format("countMisplaced = %d%n", countMisplaced);

    countMisplaced = 0; // count again
    f.format("%nFind Misplaced Files%n");
    File indexFile = new File(mfile.getPath());
    countMisplaced += doOneIndex(indexFile, f, varCount, filenames, new Indent(2), extra);
    f.format("%nDone countMisplaced=%d (n < 400)%n%nFiles%n", countMisplaced);
    f2.format("%s", f.toString());
  }

   //  report over all files in collection
  private void doMisplacedFields(Formatter f, MCollection dcm, boolean useIndex, boolean extra) throws IOException {
     f.format("Check Misplaced Fields%n");
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
       if (vinfo.count > 400) vinfo.ok = true; // LOOK arbitrary cutoff
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
     f.format("%nDone countMisplaced=%d (n < 400)%n%nFiles%n", countMisplaced);
     for (String filename : filenames)
       f.format("  %s%n", filename);
   }

  ///////////////////////////////////////////////

  private static class VarInfo implements Comparable<VarInfo> {
    int hash;
    String name;
    int count = 0;
    boolean ok;

    private VarInfo(int hash, String name) {
      this.hash = hash;
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
    try (GribCollection gc = GribCdmIndex.openCdmIndex(indexFile, config, false, logger)) {
      if (gc == null)
        throw new IOException(indexFile+ " not a grib collection index file");

      for (GribCollection.Dataset ds : gc.getDatasets()) {
        if (!ds.getType().equals(GribCollection.Type.TwoD)) continue;
        for (GribCollection.GroupGC g : ds.getGroups()) {
          f.format(" Group %s%n", g.getDescription());

          for (GribCollection.VariableIndex vi : g.getVariables()) {
            String name = gc.makeVariableName(vi);                    // LOOK not actually right - some are partitioned by level
            f.format("  %7d: %s%n", vi.nrecords, name);
            int hash = vi.cdmHash + g.getGdsHash(); // must be both group and var
            VarInfo vinfo = varCount.get(hash);
            if (vinfo == null) {
              vinfo = new VarInfo(hash, name);
              varCount.put(hash, vinfo);
            }
            vinfo.count += vi.nrecords;
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

    File parent = indexFile.getParentFile();
    try (GribCollection gc = GribCdmIndex.openCdmIndex(indexFile.getPath(), config, false, logger)) {
      if (gc == null)
        throw new IOException(indexFile+ " not a grib collection index file");

      // see if it has any misplaced
      int countMisplaced = 0;
      for (GribCollection.Dataset ds : gc.getDatasets()) {
        if (ds.getType().equals(GribCollection.Type.Best)) continue;
        for (GribCollection.GroupGC g : ds.getGroups()) {
          for (GribCollection.VariableIndex vi : g.getVariables()) {
            int hash = vi.cdmHash + g.getGdsHash();
            VarInfo vinfo = varCount.get(hash);
            if (vinfo == null) f.format("ERROR on vi %s%n", vi);
            else{
              if (!vinfo.ok) countMisplaced += vi.nrecords;
            }
          }
        }
      }

      if (countMisplaced == 0) {
        if (showScan) f.format(" none%n");
        return 0;
      }

      indent.incr();
      if (gc instanceof PartitionCollection) {
        PartitionCollection pc = (PartitionCollection) gc;
        boolean isPoP =  pc.isPartitionOfPartitions();
        if (showScan) f.format(" isPofP=%s%n", isPoP);
        for (PartitionCollection.Partition partition : pc.getPartitions()) {
          File partParent = new File(partition.getDirectory());
          File reparent =  new File(parent, partParent.getName());
          File nestedIndex =  isPoP ? new File(reparent, partition.getFilename()) : new File(parent, partition.getFilename()); // JMJ
          if (showScan) f.format("%sPartition index= %s exists=%s%n", indent, nestedIndex, nestedIndex.exists());
          if (nestedIndex.exists()) {
            totalMisplaced += doOneIndex(nestedIndex, f, varCount, filenames, indent.incr(), showScan);
            indent.decr();
          } else {
            f.format("%sdir=%s filename=%s nestedIndex %s NOT EXIST%n", indent, partition.getDirectory(), partition.getFilename(), nestedIndex.getPath());
          }
        }

      } else {
        if (showScan) f.format("%n");
        f.format("%sIndex %s count=%d%n", indent, indexFile, countMisplaced);
        indent.incr();

        for (GribCollection.Dataset ds : gc.getDatasets()) {
          if (ds.getType().equals(GribCollection.Type.Best)) continue;
          for (GribCollection.GroupGC g : ds.getGroups()) {
            for (GribCollection.VariableIndex vi : g.getVariables()) {
              int hash = vi.cdmHash + g.getGdsHash();
              VarInfo vinfo = varCount.get(hash);
              if (!vinfo.ok) {
                vi.readRecords();
                if (vi.getSparseArray() != null) {
                  SparseArray<GribCollection.Record> sa = vi.getSparseArray();
                  for (GribCollection.Record record : sa.getContent()) {
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

