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

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Structure;
import ucar.nc2.ft.StationImpl;
import ucar.nc2.ft.point.standard.NestedTable;
import ucar.nc2.ft.point.standard.TableAnalyzer;
import ucar.nc2.ft.point.standard.Join;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author caron
 * @since Apr 18, 2008
 */
public class UnidataPointFeatureAnalyzer extends TableAnalyzer {

  public boolean isMine(NetcdfDataset ds) {
    // find datatype
    String datatype = ds.findAttValueIgnoreCase(null, "cdm_datatype", null);
    if (datatype == null)
      return false;
    if (!datatype.equalsIgnoreCase(FeatureType.STATION_PROFILE.toString()))
      return false;

    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equalsIgnoreCase("Unidata"))
        return true;
    }
    return false;
  }

  @Override
  public void annotateDataset() {
    stationInfo.stationId = "name"; // findVariableWithAttribute(ds.getVariables(), "standard_name", "station_name");
    stationInfo.stationDesc = null; // ds.findVariable("station_description");
    stationInfo.stationNpts = null; // ds.findVariable("nrecords");

    stationInfo.latName = "Latitude";
    stationInfo.lonName = "Longitude";
    stationInfo.elevName = "Height_of_station";
  }

  @Override
  protected void makeTables() throws IOException {
    super.makeTables();
    makeIndex();
  }

  private void makeIndex() throws IOException {

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
          double lat = sdata.getScalarDouble("Latitude");
          double lon = sdata.getScalarDouble("Longitude");
          double elev = sdata.getScalarDouble("Height_of_station");
          stations.add(new StationImpl(indy.name, null, lat, lon, elev));

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
    sm.addMember("name", null, null, DataType.STRING, new int[0]).setDataParam(0);
    sm.addMember("Latitude", null, "degrees_north", DataType.DOUBLE, new int[0]).setDataParam(4);
    sm.addMember("Longitude", null, "degrees_east", DataType.DOUBLE, new int[0]).setDataParam(12);
    sm.addMember("Height_of_station", null, "m", DataType.DOUBLE, new int[0]).setDataParam(20);
    sm.setStructureSize(28);

    int nstations = stations.size();
    ArrayStructureBB asbb = new ArrayStructureBB(sm, new int[]{nstations});
    ByteBuffer bb = asbb.getByteBuffer();
    for (StationImpl stn : stations) {
      asbb.addObjectToHeap(stn.getName());
      bb.putInt(asbb.addObjectToHeap(stn.getName()));
      bb.putDouble(stn.getLatitude());
      bb.putDouble(stn.getLongitude());
      bb.putDouble(stn.getAltitude());
    }

    // add the station table
    NestedTable.Table stnTable = new NestedTable.Table("stations", asbb);
    addTable(stnTable);
  }

  public void makeJoins() throws IOException {
    super.makeJoins();

    indexJoin = new IndexJoin();
    NestedTable.Table stnTable = tableFind.get("stations");
    NestedTable.Table obsTable = tableFind.get("obsRecord");
    indexJoin.setTables(stnTable, obsTable);
    joins.add(indexJoin);
  }

  private class IndexJoin extends Join {
    IndexJoin() {
      super(Join.Type.Index);
    }

    @Override
    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {
      String stnName = parentStruct.getScalarString("name"); // which station is this ?
      return stnMap.get(stnName);  // return iterator for it LOOK make a new one for thread safety ??
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
