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
  public void doReport(String spec, boolean useIndex, boolean eachFile, boolean extra, Object option) throws IOException {
    Report which = (Report) option;
    Formatter f = new Formatter();
    f.format("%s on %s useIndex=%s eachFile=%s extra=%s%n", which, spec, useIndex, eachFile, extra);

    MCollection dcm = getCollection(spec, f);
    if (dcm == null) {
      return;
    }

    // CollectionSpecParser parser = dcm.getCollectionSpecParser();

    f.format("top dir = %s%n", dcm.getRoot());
    //f.format("filter = %s%n", parser.getFilter());
    reportPane.setText(f.toString());

    File top = new File(dcm.getRoot());
    if (!top.exists()) {
      f.format("top dir = %s does not exist%n", dcm.getRoot());
    } else {

      switch (which) {
        case misplacedFlds:
          doMisplacedFields(f, dcm, useIndex, eachFile, extra);
          break;
      }
    }

    reportPane.setText(f.toString());
    reportPane.gotoTop();
  }

  ///////////////////////////////////////////////

  private class VarInfo implements Comparable<VarInfo> {
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

  private void doMisplacedFields(Formatter f, MCollection dcm, boolean useIndex, boolean eachFile, boolean extra) {
    try {
      f.format("Check Misplaced Fields%n");
      Map<Integer, VarInfo> varCount = new HashMap<>();

      for (MFile mfile : dcm.getFilesSorted()) {
        f.format("%n%s%n", mfile.getPath());
        countTop(mfile.getPath(), f, varCount);
      }

      f.format("%nTotals%n");
      List<VarInfo> sorted = new ArrayList<>(varCount.values());
      Collections.sort(sorted);
      for (VarInfo vinfo : sorted) {
        f.format(" %20s = %d%n", vinfo.name, vinfo.count);
        if (vinfo.count > 1000) vinfo.ok = true; // LOOK
      }

      f.format("%nFind Misplaced Files%n");
      for (MFile mfile : dcm.getFilesSorted()) {
        File indexFile = new File(mfile.getPath());
        doOneIndex(indexFile, f, varCount, new Indent(2), extra);
      }
      f.format("%nDone%n");

    } catch (IOException ioe) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      ioe.printStackTrace(new PrintStream(bos));
      f.format(bos.toString());
      ioe.printStackTrace();
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
            VarInfo vinfo = varCount.get(vi.hashCode());
            if (vinfo == null) {
              vinfo = new VarInfo(vi.cdmHash, name);
              varCount.put(vi.cdmHash, vinfo);
            }
            vinfo.count += vi.nrecords;
          }
        }
      }
    }
  }

  // recursively look for leaf files of records in vars
  public void doOneIndex(File indexFile, Formatter f, Map<Integer, VarInfo> varCount, Indent indent, boolean showScan) throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig();

    try (ucar.unidata.io.RandomAccessFile raf = new RandomAccessFile(indexFile.getPath(), "r")) {
      GribCdmIndex.GribCollectionType type = GribCdmIndex.getType(raf);
      if (showScan) f.format("%sIndex %s type=%s", indent, indexFile, type);
    }

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
            VarInfo vinfo = varCount.get(vi.cdmHash);
            if (!vinfo.ok) countMisplaced += vi.nrecords;
          }
        }
      }

      if (countMisplaced == 0) {
        if (showScan) f.format(" none%n");
        return;
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
            doOneIndex(nestedIndex, f, varCount, indent.incr(), showScan);
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
              VarInfo vinfo = varCount.get(vi.cdmHash);
              if (!vinfo.ok) {
                vi.readRecords();
                if (vi.getSparseArray() != null) {
                  SparseArray<GribCollection.Record> sa = vi.getSparseArray();
                  for (GribCollection.Record record : sa.getContent()) {
                    String filename = gc.getFilename(record.fileno);
                    f.format("%s%s: %s at pos %d%n", indent, vinfo.name, filename, record.pos);
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
  }


}

