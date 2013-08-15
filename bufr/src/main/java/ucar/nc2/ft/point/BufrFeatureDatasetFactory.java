package ucar.nc2.ft.point;

import org.jdom2.Element;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.SequenceDS;
import ucar.nc2.ft.*;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.bufr.BufrConfig;
import ucar.nc2.iosp.bufr.BufrIosp2;
import ucar.nc2.iosp.bufr.StandardFields;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Indent;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.StationImpl;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Use BufrConfig to make BUFR files into PointFeatureDataset
 *
 * @author caron
 * @since 8/14/13
 */
public class BufrFeatureDatasetFactory implements FeatureDatasetFactory {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrFeatureDatasetFactory.class);

  @Override
  public Object isMine(FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException {
    IOServiceProvider iosp = ncd.getIosp();
    return (iosp != null && iosp instanceof BufrIosp2) ? true : null;
  }

  @Override
  public FeatureType[] getFeatureType() {
    return new FeatureType[]{FeatureType.ANY_POINT};
  }

  @Override
  public FeatureDataset open(FeatureType ftype, NetcdfDataset ncd, Object analysis, CancelTask task, Formatter errlog) throws IOException {
    BufrIosp2 iosp = (BufrIosp2) ncd.getIosp();
    BufrConfig config = iosp.getConfig();
    Formatter f = new Formatter();
    config.show(f);
    System.out.printf("%s%n", f);

    Element iospParam = iosp.getElem();
    if (iospParam != null) {
      Element parent = iospParam.getChild("bufr2nc", NcMLReader.ncNS);
      show(parent, new Indent(2));

      processSeq((Structure) ncd.findVariable(BufrIosp2.obsRecord), parent);
    }

    return new BufrStationDataset(ncd, config);
  }

  private void show(Element parent, Indent indent) {
    if (parent == null) return;
    for (Element child : parent.getChildren("fld", NcMLReader.ncNS)) {
      String idx = child.getAttributeValue("idx");
      String fxy = child.getAttributeValue("fxy");
      String name = child.getAttributeValue("name");
      String action = child.getAttributeValue("action");
      System.out.printf("%sidx='%s' fxy='%s' name='%s' action='%s'%n", indent, idx, fxy, name, action);
      indent.incr();
      show(child, indent);
      indent.decr();
    }
  }

  private void processSeq(Structure struct, Element parent) throws IOException {
    if (parent == null || struct == null) return;
    List<Variable> vars = struct.getVariables();
    for (Element child : parent.getChildren("fld", NcMLReader.ncNS)) {
      String idxS = child.getAttributeValue("idx");
      int idx = Integer.parseInt(idxS);
      if (idx < 0 || idx >= vars.size()) {
        log.error("Bad index = %s", child);
        continue;
      }
      Variable want = vars.get(idx);
      struct.removeMemberVariable(want);
      System.out.printf("removed %s%n", want);
    }
  }

  private class BufrStationDataset extends PointDatasetImpl {

    // LOOK - need to modify the list of Variables
    //public List<VariableSimpleIF> getDataVariables() {
    //  return null;
    //}

    private BufrStationDataset(NetcdfDataset ncfile, BufrConfig config) {
      super(ncfile, FeatureType.STATION);

      BufrStationCollection bufrCollection = new BufrStationCollection(ncfile, config);
      setPointFeatureCollection(bufrCollection);
    }

    @Override
    public FeatureType getFeatureType() {
      return FeatureType.STATION;
    }

  }

  private class BufrStationCollection extends StationTimeSeriesCollectionImpl {
    BufrConfig config;
    SequenceDS obs;
    String stationMember;

    private BufrStationCollection(NetcdfDataset ncfile, BufrConfig config) {
      super(ncfile.getLocation());
      this.config = config;

      this.obs = (SequenceDS) ncfile.findVariable(BufrIosp2.obsRecord);
      BufrConfig.FieldConverter stationField = config.getStandardField(StandardFields.Type.stationId);
      for (Variable member : this.obs.getVariables()) {
        Attribute att = member.findAttribute("BUFR:TableB_descriptor");
        if (att != null && att.getStringValue().equals(stationField.getFxyName())) {
          this.stationMember = member.getShortName();
          break;
        }
      }
    }

    @Override
    protected void initStationHelper() {
      DateUnit du = null;
      try {
        du = new DateUnit("secs since 1970-01-01T00:00");
      } catch (Exception e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }

      stationHelper = new StationHelper();
      Station s = new StationImpl("RUP54", null, null, 0, 0, 0);
      stationHelper.addStation(new BufrStation(s, du, -1));
    }

    private class BufrStation extends StationFeatureImpl {
      private BufrStation(Station s, DateUnit timeUnit, int npts) {
        super(s, timeUnit, npts);
      }

      @Override
      public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
        return new BufrStationIterator(obs.getStructureIterator(), null);
      }

      // iterates over the records for this station
      public class BufrStationIterator extends PointIteratorFromStructureData {
        int count = 0;

        public BufrStationIterator(StructureDataIterator structIter, PointFeatureIterator.Filter filter) throws IOException {
          super(structIter, filter);
        }

        @Override
        protected PointFeature makeFeature(int recnum, StructureData sdata) throws IOException {
          String stationId = sdata.getScalarString(stationMember).trim();
          //System.out.printf("  '%s' '%s' (%d) %n", s.getName(), stationId, count++);
          //if (count > 10) return null;
          if (!stationId.equals(s.getName())) return null;
          return new BufrStationPoint(s, 0, 0, timeUnit, sdata);  // LOOK  obsTime, nomTime
        }
      }

      public class BufrStationPoint extends PointFeatureImpl {
        StructureData sdata;

        public BufrStationPoint(EarthLocation location, double obsTime, double nomTime, DateUnit timeUnit, StructureData sdata) {
          super(location, obsTime, nomTime, timeUnit);
          this.sdata = sdata;
        }

        @Override
        public StructureData getData() throws IOException {
          return sdata;
        }
      }
    }

  }


}
