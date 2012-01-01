/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib;

import thredds.inventory.CollectionManager;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grib.grib2.Grib2Collection;
import ucar.nc2.grib.grib2.Grib2Iosp;
import ucar.nc2.grib.grib2.Grib2CollectionBuilder;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A collection of GribCollection objects which are Time Partitioned.
 * A TimePartition is the collection; a  TimePartition.Partition represents one of the GribCollection.
 * Everything is done with lazy instantiation.
 *
 * @author caron
 * @since 4/17/11
 */
public class TimePartition extends Grib2Collection {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TimePartition.class);

  Map<String, Partition> partitionMap;
  List<Partition> partitions;

  // wrapper around a GribCollection
  public class Partition implements Comparable<Partition> {
    protected CollectionManager dcm;
    private GribCollection gribCollection;
    private String name, filename;
    private RandomAccessFile raf;

    // constructor from ncx
    public Partition(String name, String filename) {
      this.name = name;
      this.filename = filename; // grib collection ncx
    }

    // constructor from a TimePartition object
    public Partition(CollectionManager dcm) {
      this.dcm = dcm;
      this.name = dcm.getCollectionName();
    }

    public String getName() {
      return name;
    }

    public String getFilename() {
      return filename;
    }

    // not present if it come from the index
    public CollectionManager getDcm() {
      return dcm;
    }

    // LOOK use ehcache here ??
    public GribCollection getGribCollection() throws IOException {
      return getGribCollection(new Formatter()); // throw away the log info
    }

    public GribCollection getGribCollection(Formatter f) throws IOException {
      if (gribCollection == null) {
        GribCollection gc;
        if (dcm != null) {
          gc = Grib2CollectionBuilder.factory(dcm, CollectionManager.Force.test, f);
        } else {
          raf = new RandomAccessFile(filename, "r");
          rafLocation = filename;
          gc = Grib2CollectionBuilder.createFromIndex(name, null, raf); // LOOK
        }

        gribCollection = gc; // dont set until init() works
        filename = gc.getIndexFile().getPath();
      }
      return gribCollection;
    }

    @Override
    public int compareTo(Partition o) {
      return name.compareTo(o.name);
    }

    @Override
    public String toString() {
      return "Partition{" +
              "dcm=" + dcm +
              ", name='" + name + '\'' +
              ", filename='" + filename + '\'' +
              '}';
    }
  }

  public class VariableIndexPartitioned extends GribCollection.VariableIndex {
    int[] groupno, varno;

    public VariableIndexPartitioned(GribCollection.GroupHcs g, int discipline, int category, int parameter, int levelType, boolean isLayer,
                          int intvType, int ensDerivedType, int probType, String probabilityName,
                          int cdmHash, int timeIdx, int vertIdx, int ensIdx, long recordsPos, int recordsLen) {

      super(g, 0,discipline, category, parameter, levelType, isLayer, intvType, null, ensDerivedType, probType, probabilityName,
              cdmHash, timeIdx, vertIdx, ensIdx, recordsPos, recordsLen);
    }

    void setPartitionIndex(int partno, int groupIdx, int varIdx) {
      groupno[partno] = groupIdx;
      varno[partno] = varIdx;
    }

    public GribCollection.VariableIndex getVindex(int partno) throws IOException {
      // at this point, we need to instantiate the Partition and the vindex.records
      Partition p = getPartitions().get(partno);
      GribCollection gc = p.getGribCollection(); // ensure that its read in

      // the group and varible index may vary across partitions
      GribCollection.GroupHcs g = gc.groups.get(groupno[partno]);
      GribCollection.VariableIndex vindex = g.varIndex.get(varno[partno]);
      vindex.readRecords();
      return vindex;
    }

  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  TimePartition(String name, File directory) {
    super(name, directory);
  }

  @Override
  public File getIndexFile() {
    return new File(directory, name +  GribCollection.IDX_EXT);
  }

  Partition makePartition(CollectionManager dcm) {
    return new Partition(dcm);
  }

  Partition makePartition(String name, String filename) {
    return new Partition(name, filename);
  }

  public Partition getCollection(String name) {
    return partitionMap.get(name);
  }

  GribCollection.VariableIndex makeVariableIndex(GribCollection.GroupHcs group,
                         int discipline, int category, int parameter, int levelType, boolean isLayer, int intvType,
                         int ensDerivedType, int probType, String probabilityName,
                         int cdmHash, int timeIdx, int vertIdx, int ensIdx, long recordsPos, int recordsLen,
                         List<Integer> groupnoList, List<Integer> varnoList) {

    VariableIndexPartitioned vip =  new VariableIndexPartitioned(group, discipline, category, parameter, levelType, isLayer, intvType,
            ensDerivedType, probType, probabilityName, cdmHash, timeIdx, vertIdx, ensIdx, recordsPos, recordsLen);

    int nparts = varnoList.size();
    vip.groupno = new int[nparts];
    vip.varno = new int[nparts];
    for (int i=0; i<nparts; i++) {
      vip.groupno[i] = groupnoList.get(i);
      vip.varno[i] = varnoList.get(i);
    }
    return vip;
  }

  VariableIndexPartitioned makeVariableIndexPartitioned(GribCollection.VariableIndex vi, int nparts) {
    VariableIndexPartitioned vip = new VariableIndexPartitioned(vi.group, vi.discipline, vi.category, vi.parameter, vi.levelType, vi.isLayer, vi.intvType,
            vi.ensDerivedType, vi.probType, vi.probabilityName, vi.cdmHash, vi.timeIdx, vi.vertIdx, vi.ensIdx, vi.recordsPos, vi.recordsLen);

    vip.groupno = new int[nparts];
    vip.varno = new int[nparts];
    for (int i=0; i<nparts; i++) {
      vip.groupno[i] = -1;
      vip.varno[i] = -1;
    }
    return vip;
  }

  public List<Partition> getPartitions() {
    if (partitions == null) {
      List<Partition> c = new ArrayList<Partition>(partitionMap.values());
      Collections.sort(c);
      partitions = c;
    }
    return partitions;
  }

  public void removePartition(Partition p) {
    partitions.remove(p);
    if (null != p.getDcm())
      partitionMap.remove(p.getDcm().getCollectionName());
  }

  /////////////////////////////////////////////
  // stuff for InvDatasetFcGrib

  public void updateProto() {
  }

  // true if things have changed since given time
  public boolean checkInvState(long lastInvChange) throws IOException {
    return false; // this.lastInvChanged > lastInvChange;
  }

  // true if things have changed since given time
  public boolean checkProtoState(long lastProtoChanged) throws IOException {
    return false; // this.lastProtoChanged > lastProtoChanged;
  }

  public void checkNeeded(boolean force) {
  }

  // public abstract ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(String groupName, String filename) throws IOException;
  // public abstract ucar.nc2.dt.GridDataset getGridDataset(String groupName, String filename) throws IOException;


  // LOOK - needs time partition collection iosp or something
  public ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(String groupName) throws IOException {
    GroupHcs want = findGroup(groupName);
    if (want == null) return null;

    Grib2Iosp iosp = new Grib2Iosp(want);
    NetcdfFile ncfile = new MyNetcdfFile(iosp, null, getIndexFile().getPath(), null);
    return new NetcdfDataset(ncfile);
  }

  public ucar.nc2.dt.GridDataset getGridDataset(String groupName) throws IOException {
    GroupHcs want = findGroup(groupName);
    if (want == null) return null;

    Grib2Iosp iosp = new Grib2Iosp(want);
    NetcdfFile ncfile = new MyNetcdfFile(iosp, null, getIndexFile().getPath(), null);
    NetcdfDataset ncd = new NetcdfDataset(ncfile);
    return new ucar.nc2.dt.grid.GridDataset(ncd); // LOOK - replace with custom GridDataset??
  }

  public void showIndex(Formatter f) {
    List<Partition> plist = getPartitions();
    f.format("Partitions (%d)%n", plist.size());
    for (Partition p : plist)
      f.format("  %s%n", p);
    f.format("%n");

    super.showIndex(f);
  }


 //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for Iosp

  public RandomAccessFile getRaf(int partno, int fileno) throws IOException {
    Partition part = getPartitions().get(partno);
    return part.gribCollection.getRaf( fileno);
  }

  public void close() throws java.io.IOException {
    super.close();
    for (Partition part : getPartitions()) {
      if (part.gribCollection != null)
        part.gribCollection.close();
    }
  }

  public static void main(String[] args) throws IOException {
    Formatter f = new Formatter();
    RandomAccessFile raf = new RandomAccessFile("G:/nomads/cfsr/timeseries/collection.ncx", "r");
    TimePartition gtc = TimePartitionBuilder.createFromIndex("test", null, raf);
    gtc.showIndex(f);

    System.out.printf("%s%n", f);
    raf.close();
  }
}
