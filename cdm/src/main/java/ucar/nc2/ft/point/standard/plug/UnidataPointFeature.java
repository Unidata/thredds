/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.ft.point.standard.*;
import ucar.nc2.ft.StationImpl;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Structure;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * "Unidata Point Feature v1.0" Convention
 * @deprecated
 * @author caron
 * @since Apr 23, 2008
 */
public class UnidataPointFeature implements TableConfigurer {

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
    TableConfig nt = new TableConfig(TableType.ArrayStructure, "station");
    nt.featureType = FeatureType.STATION_PROFILE;

    nt.stnId = STN_NAME;
    nt.lat = STN_LAT;
    nt.lon = STN_LON;
    nt.elev = STN_ELEV;

    // make the station array structure in memory
    nt.as = makeIndex(ds);

    TableConfig obs = new TableConfig(TableType.Structure, "obsRecord");
    obs.dim = Evaluator.getDimension(ds, "record", errlog);

    obs.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat);
    obs.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon);
    obs.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height);
    obs.time = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Time);

    obs.stnId = Evaluator.getVariableName(ds, "name", errlog);
    obs.join = new TableConfig.JoinConfig(JoinType.Index);
    // create an IndexJoin and attach to the obs.join
    indexJoin = new IndexJoin(obs.join);
    nt.addChild(obs);

    TableConfig levels = new TableConfig(TableType.Structure, "seq1");
    levels.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height);
    levels.join = new TableConfig.JoinConfig(JoinType.NestedStructure);

    obs.addChild(levels);
    return nt;
  }

  private ArrayStructure makeIndex(NetcdfDataset ds) throws IOException {

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

  private class IndexJoin extends Join {
    IndexJoin(TableConfig.JoinConfig config) {
      super( config);
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
      return child.getStruct();
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
  }

}

