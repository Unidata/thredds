/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.ft.point.standard.*;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;

import java.util.*;
import java.io.IOException;

/**
 * "Unidata Point Feature v1.0" Convention
 * @deprecated
 * @author caron
 * @since Apr 23, 2008
 */
public class UnidataPointFeature  extends TableConfigurerImpl  {

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    // find datatype
    FeatureType featureType = FeatureDatasetFactoryManager.findFeatureType( ds);
    if (featureType != FeatureType.STATION_PROFILE)
      return false;

    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equalsIgnoreCase("Unidata Point Feature v1.0"))
        return true;
    }
    return false;
  }

  private static final String STN_NAME = "name";
  private static final String STN_LAT = "Latitude";
  private static final String STN_LON = "Longitude";
  private static final String STN_ELEV = "Height_of_station";

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    TableConfig nt = new TableConfig(Table.Type.ArrayStructure, "station");
    nt.featureType = FeatureType.STATION_PROFILE;
    nt.structName = "station";

    nt.stnId = STN_NAME;
    nt.lat = STN_LAT;
    nt.lon = STN_LON;
    nt.elev = STN_ELEV;

    // make the station array structure in memory
    // nt.as = makeIndex(ds);

    TableConfig obs = new TableConfig(Table.Type.Structure, "obsRecord");
    obs.structName = "record";
    obs.dimName = Evaluator.getDimensionName(ds, "record", errlog);

    obs.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat);
    obs.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon);
    obs.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height);
    obs.time = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Time);

    obs.stnId = Evaluator.getVariableName(ds, "name", errlog);
    //obs.join = new TableConfig.JoinConfig(Join.Type.Index);
    // create an IndexJoin and attach to the obs.join
    //indexJoin = new IndexJoin(obs.join);
    nt.addChild(obs);

    TableConfig levels = new TableConfig(Table.Type.Structure, "seq1");
    levels.structName = "seq1";
    levels.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height);
    //levels.join = new TableConfig.JoinConfig(Join.Type.NestedStructure);

    obs.addChild(levels);
    return nt;
  }

  /* private ArrayStructure makeIndex(NetcdfDataset ds) throws IOException {

    // read in the index structure data
    Structure s = (Structure) ds.findVariable("obsRecordIndex");
    ArrayStructure as = (ArrayStructure) s.read();
    StructureMembers.Member timeMember = as.getStructureMembers().findMember("time");
    StructureMembers.Member nameMember = as.getStructureMembers().findMember("name");

    // make Index, sort by name and time
    int n = (int) as.getSize();
    index = new ArrayList<Index>(n);
    for (int i = 0; i < n; i++) {
      long time = as.getScalarLong(i, timeMember);
      String name = as.getScalarString(i, nameMember);
      Index indy = new Index(i, time, name);
      index.add(indy);
    }
    Collections.sort(index);

    // make stations : unique names for station
    Structure obs = (Structure) ds.findVariable("obsRecord");
    List<StationImpl> stations = new ArrayList<StationImpl>();
    String name = null;
    int count = 0;
    MyStructureDataIterator last = null;
    for (int i = 0; i < n; i++) {
      Index indy = index.get(i);
      if (!indy.name.equals(name)) {
        if (last != null) last.setCount(count);
        count = 0;
        // read in that obs, make a station
        try {
          StructureData sdata = obs.readStructure(i);
          double lat = sdata.convertScalarDouble(STN_LAT);
          double lon = sdata.convertScalarDouble(STN_LON);
          double elev = sdata.convertScalarDouble(STN_ELEV);
          stations.add(new StationImpl(indy.name, null, null, lat, lon, elev));

          last = new MyStructureDataIterator(i);
          stnMap.put(indy.name, last);

        } catch (InvalidRangeException e) {
          throw new IllegalStateException();
        }
        name = indy.name;
      }
      count++;
    }
    if (last != null) last.setCount(count);

    // make an ArrayStructure for the station table
    StructureMembers sm = new StructureMembers("station");
    sm.addMember(STN_NAME, null, null, DataType.STRING, new int[0]).setDataParam(0);
    sm.addMember(STN_LAT, null, "degrees_north", DataType.DOUBLE, new int[0]).setDataParam(4);
    sm.addMember(STN_LON, null, "degrees_east", DataType.DOUBLE, new int[0]).setDataParam(12);
    sm.addMember(STN_ELEV, null, "m", DataType.DOUBLE, new int[0]).setDataParam(20);
    sm.setStructureSize(28);

    int nstations = stations.size();
    ArrayStructureBB asbb = new ArrayStructureBB(sm, new int[]{nstations});
    ByteBuffer bb = asbb.getByteBuffer();
    for (StationImpl stn : stations) {
      bb.putInt(asbb.addObjectToHeap(stn.getName()));
      bb.putDouble(stn.getLatitude());
      bb.putDouble(stn.getLongitude());
      bb.putDouble(stn.getAltitude());
    }

    // add the station table
    return asbb;
  }

  /* private class IndexJoin extends Join {
    IndexJoin(TableConfig.JoinConfig config) {
      super();
      config.override = this;
    }

    @Override
    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {
      String stnName = parentStruct.getScalarString("name"); // which station is this ?
      MyStructureDataIterator iter = stnMap.get(stnName);  // return iterator for it
      iter.reset();
      return iter;
    }

    Structure getObsStructure() {
      return null; // child.getStruct();
    }
  }

  private IndexJoin indexJoin;
  private Map<String, MyStructureDataIterator> stnMap = new HashMap<String, MyStructureDataIterator>();
  private List<Index> index;

  private class Index implements Comparable<Index> {
    int recno;
    long time;
    String name;

    Index(int recno, long time, String name) {
      this.recno = recno;
      this.time = time;
      this.name = name;
    }

    public int compareTo(Index o) {
      if (name.equals(o.name))
        return (int) (time - o.time);
      return name.compareTo(o.name);
    }
  }

  private class MyStructureDataIterator implements StructureDataIterator {
    int startIndex, count;
    int current;

    MyStructureDataIterator(int startIndex) {
      this.startIndex = startIndex;
      this.current = startIndex;
    }

    void setCount(int count) { this.count = count; }

    public boolean hasNext() throws IOException {
      return (current - startIndex) < count;
    }

    public StructureData next() throws IOException {
      Structure struct = indexJoin.getObsStructure();
      try {
        Index indy = index.get(current++);
        return struct.readStructure(indy.recno);

      } catch (InvalidRangeException e) {
        e.printStackTrace();
        return null;
      }
    }

    public void setBufferSize(int bytes) {
    }

    public StructureDataIterator reset() {
      this.current = startIndex;
      return this;
    }
  } */

}

