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
import ucar.nc2.dt2.point.standard.PointDatasetStandardFactory;
import ucar.nc2.units.DateUnit;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.iosp.misc.NmcObsLegacy;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.ma2.StructureData;
import ucar.ma2.ArraySequence2;
import ucar.ma2.StructureMembers;

import java.io.IOException;
import java.util.Iterator;

/**
 * StationProfileFeatureCollection for IOSP NmcObsLegacy using NMC Office Note 29 file
 *
 * @author caron
 * @since Feb 29, 2008
 */
public class NmcStationProfileDatasetFactory implements FeatureDatasetFactory {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PointDatasetStandardFactory.class);

  // FeatureDatasetFactory
  public boolean isMine(NetcdfDataset ds) {
    return ds.getIosp() instanceof NmcObsLegacy;
  }

  public FeatureDataset open(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    return new NmcStationProfileDataset(ncd, errlog);
  }

  public FeatureType getFeatureDataType() {
    return FeatureType.POINT;
  }

  ///////////////////////////////////////////////////////////////////////

  private class NmcStationProfileDataset extends PointDatasetImpl {
    private StationHelper stationHelper = new StationHelper();
    private Structure stationProfiles;
    private DateUnit dateUnit;

    NmcStationProfileDataset(NetcdfDataset ncfile, StringBuffer errlog) throws IOException {
      super( ncfile, StationProfileFeature.class);

      stationProfiles = (Structure) ncfile.findVariable("stationProfiles");
      ucar.ma2.StructureDataIterator iter = stationProfiles.getStructureIterator();
      while (iter.hasNext()) {
        StructureData sdata = iter.next();
        String name = sdata.getScalarString("stationName");
        float lat = sdata.getScalarFloat("lat");
        float lon = sdata.getScalarFloat("lon");
        float elev = sdata.getScalarFloat("elev");
        int npts = sdata.getScalarInt("nrecords");
        ArraySequence2 seq = (ArraySequence2) sdata.getArrayStructure("report");
        stationHelper.addStation(new NmcStation(name, lat, lon, elev, seq, npts));
      }

      Structure report = (Structure) stationProfiles.findVariable("report");
      Variable time = report.findVariable("time");
      try {
        dateUnit = new DateUnit(time.getUnitsString());
      } catch (Exception e) {
        log.warn("Invalid date unit string= " + time.getUnitsString());
      }

      Structure mandLevels = (Structure) report.findVariable("mandatoryLevels");
      NmcStationProfileCollection mandLevelsProfiles = new NmcStationProfileCollection(mandLevels);
      setPointFeatureCollection(mandLevelsProfiles);
    }

    private class NmcStation extends StationImpl {
      ArraySequence2 seq;
      int npts;

      NmcStation(String name, double lat, double lon, double elev, ArraySequence2 seq, int npts) {
        super(name, name, lat, lon, elev);
        this.seq = seq;
        this.npts = npts;
      }
    }

    private class NmcStationProfileCollection extends StationProfileCollectionImpl {
      private Structure struct;

      NmcStationProfileCollection(Structure struct) throws IOException {
        super( struct.getName());
        this.struct = struct;
        setStationHelper( stationHelper);
      }

      public StationProfileFeature getStationProfileFeature(Station s) throws IOException {
        return new NmcStationProfileFeature(struct, (NmcStation) s);
      }

      public NestedPointFeatureCollectionIterator getNestedPointFeatureCollectionIterator(int bufferSize) throws IOException {
        return new NmcStationProfileIterator(struct);
      }
    }

    ///////////////////////////////////////////////////////////////////////////////////////

    // iterator over all the Stations in this entire dataset
    private class NmcStationProfileIterator implements NestedPointFeatureCollectionIterator {
      Structure struct;
      Iterator iter;

      NmcStationProfileIterator(Structure struct) {
        this.struct = struct;
        iter = stationHelper.getStations().iterator();
      }

      public boolean hasNext() throws IOException {
        return iter.hasNext();
      }

      // StationProfileFeature
      public NestedPointFeatureCollection nextFeature() throws IOException {
        return new NmcStationProfileFeature(struct, (NmcStation) iter.next());
      }

      public void setBufferSize(int bytes) {
        ;
      }
    }

    // a station profile - time series of profiles at one station
    private class NmcStationProfileFeature extends StationProfileFeatureImpl {
      Structure struct;
      NmcStation s;

      NmcStationProfileFeature(Structure struct, NmcStation s) {
        super(s, dateUnit, s.npts);
        this.struct = struct;
      }

      public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
        return new NmcProfileIterator(struct, this, s.seq);
      }

    }

    // iteration over all the profiles for one station
    private class NmcProfileIterator implements PointFeatureCollectionIterator {
      Structure struct;
      Station s;
      ucar.ma2.StructureDataIterator iter;
      DateUnit dateUnit;
      double time;

      NmcProfileIterator(Structure struct, Station s, ArraySequence2 profile) throws IOException {
        this.struct = struct;
        this.s = s;
        iter = profile.getStructureDataIterator();
      }

      public boolean hasNext() throws IOException {
        return iter.hasNext();
      }

      // Profile
      public PointFeatureCollection nextFeature() throws IOException {
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
        ArraySequence2 levels = (ArraySequence2) sdata.getArrayStructure(struct.getShortName());
        return new NmcProfileFeature(struct, s, time, dateUnit, levels);
      }

      public void setBufferSize(int bytes) {
        iter.setBufferSize( bytes);
      }
    }

    // one profile
    private class NmcProfileFeature extends ProfileFeatureImpl {
      Structure struct;
      Station s;
      double time;
      DateUnit dateUnit;
      ArraySequence2 levels;

      NmcProfileFeature(Structure struct, Station s, double time, DateUnit dateUnit, ArraySequence2 levels) throws IOException {
        super(struct.getName(), s.getLatLon(), -1);
        this.struct = struct;
        this.s = s;
        this.time = time;
        this.dateUnit = dateUnit;
        this.levels = levels;
      }

      public Object getId() {
        return s;
      }

      public String getDescription() {
        return s.getName();
      }

      public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
        return new NmcPointFeatureIterator(s, time, dateUnit, levels.getStructureDataIterator());
      }

    } // NmcProfileFeature

    // iteration over the points in one profile
    private class NmcPointFeatureIterator implements PointFeatureIterator {
      Station s;
      double time;
      DateUnit dateUnit;
      ucar.ma2.StructureDataIterator iter;

      NmcPointFeatureIterator(Station s, double time, DateUnit dateUnit, ucar.ma2.StructureDataIterator iter) {
        this.s = s;
        this.time = time;
        this.dateUnit = dateUnit;
        this.iter = iter;
      }

      public boolean hasNext() throws IOException {
        return iter.hasNext();
      }

      public PointFeature nextData() throws IOException {
        return new NmcPointFeature(s, time, dateUnit, iter.next());
      }

      public void setBufferSize(int bytes) {
        iter.setBufferSize( bytes);
      }
    }

    // a single obs
    private class NmcPointFeature extends PointFeatureImpl {
      private StructureData sdata;

      NmcPointFeature(Station s, double time, DateUnit dateUnit, StructureData sdata) {
        super(s, time, time, dateUnit);
        this.sdata = sdata;
      }

      public StructureData getData() throws IOException {
        return sdata;
      }

      public Object getId() {
        return sdata;
      }
    }
  }

}
