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
package ucar.nc2.dt2.point;

import ucar.nc2.dt2.*;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;
import ucar.nc2.Structure;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.StructureData;
import ucar.ma2.ArraySequence2;
import ucar.ma2.StructureMembers;

import java.io.IOException;
import java.util.List;
import java.util.Iterator;

/**
 * @author caron
 * @since Feb 29, 2008
 */
public class NmcStationProfileDataset extends FeatureDatasetImpl implements StationProfileFeatureCollection {
  private StationHelper stationHelper = new StationHelper();

  public NmcStationProfileDataset(NetcdfDataset ncfile) throws IOException {
    super(ncfile);

    Structure stationProfiles = (Structure) ncfile.findVariable("stationProfiles");
    ucar.ma2.StructureDataIterator iter = stationProfiles.getStructureIterator();
    while (iter.hasNext()) {
      StructureData sdata = iter.next();
      String name = sdata.getScalarString("stationName");
      float lat = sdata.getScalarFloat("lat");
      float lon = sdata.getScalarFloat("lon");
      float elev = sdata.getScalarFloat("elev");
      int npts = sdata.getScalarInt("npts");
      ArraySequence2 seq = (ArraySequence2) sdata.getArrayStructure("report");
      stationHelper.addStation( new NmcStationProfileFeature(name, lat, lon, elev, null, seq, npts));
    }
  }

  public Class getFeatureClass() {
    return StationProfileFeatureImpl.class;
  }

  public FeatureIterator getFeatureIterator(int bufferSize) throws IOException {
    return new NmcStationProfileIterator();
  }

  public DataCost getDataCost() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public StationProfileFeatureCollection subset(List<Station> stations) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public StationProfileFeature getStationProfileFeature(Station s) throws IOException {
    return (StationProfileFeature) stationHelper.getStation(s.getName());
  }

  public StationProfileFeature getStationProfileFeature(Station s, DateRange dateRange) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public PointFeatureDataset subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new PointFeatureDatasetAdapter(this);
  }

  public List<Station> getStations() throws IOException {
    return stationHelper.getStations();
  }

  public List<Station> getStations(LatLonRect boundingBox) throws IOException {
    return stationHelper.getStations(boundingBox);
  }

  public Station getStation(String name) {
    return stationHelper.getStation( name);
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  private class NmcStationProfileIterator implements ucar.nc2.dt2.FeatureIterator {
    Iterator iter;
    NmcStationProfileIterator() {
      iter = stationHelper.getStations().iterator();
    }

    public boolean hasNext() throws IOException {
      return iter.hasNext();
    }

    // StationProfileFeature
    public Feature nextFeature() throws IOException {
      return (NmcStationProfileFeature) iter.next();
    }
  }

  private class NmcStationProfileFeature extends StationProfileFeatureImpl {
    ArraySequence2 seq;
    int npts;

    NmcStationProfileFeature(String name, double lat, double lon, double elev, DateUnit timeUnit, ArraySequence2 seq, int npts) {
      super(NmcStationProfileDataset.this, name, null, lat, lon, elev, timeUnit);
      this.seq = seq;
      this.npts = npts;
    }

    public int getNumberPoints() {
      return npts;
    }

    // returns ProfileFeature
    public FeatureIterator getFeatureIterator(int bufferSize) throws IOException {
      return new NmcProfileIterator(this, seq);
    }

    public DataCost getDataCost() {
      return null;
    }
  }

  private class NmcProfileIterator implements ucar.nc2.dt2.FeatureIterator {
    Station s;
    ucar.ma2.StructureDataIterator iter;
    DateUnit dateUnit;
    double time;

    NmcProfileIterator(Station s, ArraySequence2 profile) throws IOException {
      this.s = s;
      iter = profile.getStructureIterator();
    }

    public boolean hasNext() throws IOException {
      return iter.hasNext();
    }

    // Profile
    public Feature nextFeature() throws IOException {
      StructureData sdata = iter.next();
      StructureMembers.Member timeMember = sdata.findMember("time");
      if (dateUnit == null) {
        String timeUnits = timeMember.getUnitsString();
        try {
          dateUnit = new DateUnit(timeUnits);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      time = sdata.convertScalarDouble(timeMember);
      ArraySequence2 levels = (ArraySequence2) sdata.getArrayStructure("mandatoryLevels");
      return new NmcProfileFeature(s, time, dateUnit, levels);
    }
  }

  private class NmcProfileFeature extends ProfileFeatureImpl {
    Station s;
    double time;
    DateUnit dateUnit;
    ArraySequence2 levels;

    NmcProfileFeature( Station s, double time, DateUnit dateUnit, ArraySequence2 levels) throws IOException {
      super(s.getLatLon());
      this.s = s;
      this.time = time;
      this.dateUnit = dateUnit;
      this.levels = levels;
    }

    public int getNumberPoints() {
      return -1;
    }

    public PointFeatureIterator getDataIterator(int bufferSize) throws IOException {
      return new NmcPointDataIterator( s, time, dateUnit, levels.getStructureIterator());
    }

    public PointFeature makePointFeature(PointData pointData) {
      return new PointFeatureAdapter(pointData.hashCode(), null, pointData);
    }

    public Object getId() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDescription() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
  } // NmcProfileFeature

  class NmcPointDataIterator implements PointFeatureIterator {
    Station s;
    double time;
    DateUnit dateUnit;
    ucar.ma2.StructureDataIterator iter;

    NmcPointDataIterator(Station s, double time, DateUnit dateUnit, ucar.ma2.StructureDataIterator iter) {
      this.s = s;
      this.time = time;
      this.dateUnit = dateUnit;
      this.iter = iter;
    }

    public boolean hasNext() throws IOException {
      return iter.hasNext();
    }

    public PointData nextData() throws IOException {
      StructureData sdata = iter.next();
      return new PointDataImpl(s, time, dateUnit, sdata);
    }
  }

}
