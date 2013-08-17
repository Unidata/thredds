package ucar.nc2.iosp.bufr;

import org.jdom2.Element;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.SequenceDS;
import ucar.nc2.ft.point.bufr.BufrCdmIndexProto;
import ucar.nc2.ft.point.bufr.StandardFields;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Indent;
import ucar.unidata.geoloc.StationImpl;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.*;

/**
 * Configuration for converting BUFR files to CDM
 * DataDescriptor tree becomes FieldConverter tree with annotations.
 * @author caron
 * @since 8/8/13
 */
public class BufrConfig {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrConfig.class);

  static public BufrConfig scanEntireFile(RandomAccessFile raf) throws IOException {
    return new BufrConfig( raf);
  }

  static public BufrConfig openFromMessage(RandomAccessFile raf, Message m, Element iospParam) throws IOException {
    BufrConfig config = new BufrConfig( raf, m);
    if (iospParam != null)
      config.merge(iospParam);
    return config;
  }

  private String filename;
  private StandardFields.ExtractFromMessage standardFields;
  private FieldConverter rootConverter;
  private int messHash = 0;
  //private int nmess = 0;
  private FeatureType featureType;
  private Map<String, BufrStation> map;
  private long start = Long.MAX_VALUE;
  private long end = Long.MIN_VALUE;

  /*
   * Open file as a stream of BUFR messages, create config file.
   *
   * Examine length of sequences, annotate
   *
   * @param bufrFilename open this file
   * @throws java.io.IOException on IO error
   *
  private BufrConfig(String bufrFilename, boolean read) throws IOException {
    this.filename =  bufrFilename;
    try {
      scanBufrFile(new RandomAccessFile(bufrFilename, "r"), read);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  } */

  private BufrConfig(RandomAccessFile raf) throws IOException {
    this.filename =  raf.getLocation();
    try {
      scanBufrFile(raf);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  private BufrConfig(RandomAccessFile raf, Message m) throws IOException {
    this.filename =  raf.getLocation();
    this.messHash = m.hashCode();
    this.rootConverter = new FieldConverter(m.ids.getCenterId(), m.getRootDataDescriptor());
    standardFields = StandardFields.extract(m);
  }

  public String getFilename() {
    return filename;
  }

  public FieldConverter getRootConverter() {
    return rootConverter;
  }

  public Map<String,BufrStation> getStationMap() {
    return map;
  }

  public int getMessHash() {
    return messHash;
  }

  public FeatureType getFeatureType() {
    return featureType;
  }

  public FieldConverter getStandardField(BufrCdmIndexProto.FldType want) {
    for (FieldConverter fld : rootConverter.flds)
      if (fld.type == want) return fld;
    return null;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  ////////////////////////////////////////////////////////////////////////////

  private void merge(Element iospParam) {
    assert iospParam.getName().equals("iospParam");
    Element bufr2nc = iospParam.getChild("bufr2nc", NcMLReader.ncNS);
    if (bufr2nc == null) return;
    for (Element child : bufr2nc.getChildren("fld", NcMLReader.ncNS))
      merge(child, rootConverter);
  }

  private void merge(Element jdom, FieldConverter parent) {
    if (jdom == null || parent == null) return;

    FieldConverter fld = null;

    // find the corresponding field
    String idxName = jdom.getAttributeValue("idx");
    if (idxName != null) {
      try {
        int idx = Integer.parseInt(idxName);
        fld = parent.getChild(idx);
      } catch (NumberFormatException ne) {
        log.info("BufrConfig cant find Child member index={} for file = {}", idxName, filename);
      }
    }

    if (fld == null) {
      String fxyName = jdom.getAttributeValue("fxy");
      if (fxyName != null) {
        fld = parent.findChildByFxyName(fxyName);
        if (fld == null) {
          log.info("BufrConfig cant find Child member fxy={} for file = {}", fxyName, filename);
        }
      }
    }

    if (fld == null) {
      String name = jdom.getAttributeValue("name");
      if (name != null) {
        fld = parent.findChild(name);
        if (fld == null) {
          log.info("BufrConfig cant find Child member name={} for file = {}", name, filename);
        }
      }
    }

    if (fld == null) {
      log.info("BufrConfig must have idx, name or fxy attribute = {} for file = {}", jdom, filename);
      return;
    }

    String action = jdom.getAttributeValue("action");
    if (action != null && !action.isEmpty())
      fld.setAction(action);

    if (jdom.getChildren("fld") != null) {
      for (Element child : jdom.getChildren("fld", NcMLReader.ncNS)) {
        merge(child, fld);
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  private StandardFields.ExtractFromStructure extract;
  private boolean hasStations = false;
  private boolean hasDate = false;
  private void scanBufrFile(RandomAccessFile raf) throws Exception {
    NetcdfDataset ncd = null;

    try {
      MessageScanner scanner = new MessageScanner(raf);
      Message protoMessage = scanner.getFirstDataMessage();
      messHash = protoMessage.hashCode();
      standardFields = StandardFields.extract(protoMessage);
      rootConverter = new FieldConverter(protoMessage.ids.getCenterId(), protoMessage.getRootDataDescriptor());

       if (standardFields.hasStation()) {
         hasStations = true;
         map = new HashMap<String, BufrStation>(1000);
       }
      featureType = guessFeatureType(standardFields);
      hasDate = standardFields.hasTime();

      ncd = NetcdfDataset.openDataset(raf.getLocation()); // LOOK opening another raf
      Attribute centerAtt = ncd.findGlobalAttribute(BufrIosp2.centerId);
      int center = (centerAtt == null) ? 0 : centerAtt.getNumericValue().intValue();

      SequenceDS seq = (SequenceDS) ncd.findVariable(null, BufrIosp2.obsRecord);
      extract = new StandardFields.ExtractFromStructure(center, seq);

      StructureDataIterator iter = seq.getStructureIterator();
      processSeq( iter, rootConverter, true);

    } finally {
      if (ncd != null) ncd.close();
    }
  }

  private FeatureType guessFeatureType(StandardFields.ExtractFromMessage standardFields) {
    if (standardFields.hasStation()) return FeatureType.STATION;
    if (standardFields.hasTime()) return FeatureType.POINT;
    return FeatureType.NONE;
  }

  /////////////////////////////////////////////////////////////////////////////////////

  static private class FakeNetcdfFile extends NetcdfFile {
  }

  private void processSeq(RandomAccessFile raf, Message m) throws IOException {
    BufrConfig config = BufrConfig.openFromMessage(raf, m, null);
    Construct2 construct = new Construct2(m, config, new FakeNetcdfFile());

    // read all the messages
    ArrayStructure data;
    if (!m.dds.isCompressed()) {
      MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
      data = reader.readEntireMessage(construct.recordStructure, m, m, raf, null);
    } else {
      MessageCompressedDataReader reader = new MessageCompressedDataReader();
      data = reader.readEntireMessage(construct.recordStructure, m, m, raf, null);
    }

    processSeq(data.getStructureDataIterator(), rootConverter, true);
  }

  private void processSeq(StructureDataIterator sdataIter, FieldConverter parent, boolean isTop) throws IOException {
     try {
       while (sdataIter.hasNext()) {
         StructureData sdata = sdataIter.next();

         if (isTop) {
           if (hasStations) processStations(parent, sdata);
           if (hasDate) {
             extract.extract(sdata);
             CalendarDate date = extract.makeCalendarDate();
             long msecs = date.getMillis();
             if (this.start > msecs) this.start = msecs;
             if (this.end < msecs) this.end = msecs;
           }
         }

         int count = 0;
         for (StructureMembers.Member m : sdata.getMembers()) {
           if (m.getDataType() == DataType.SEQUENCE) {
             FieldConverter fld = parent.getChild(count);
             ArraySequence data = (ArraySequence) sdata.getArray(m);
             int n = data.getStructureDataCount();
             fld.trackSeqCounts(n);
             processSeq(data.getStructureDataIterator(), fld, false);
           }
           count++;
         }
       }
     } finally {
       sdataIter.finish();
     }
   }

   private void processStations(FieldConverter parent, StructureData sdata) {
     BufrStation station = new BufrStation();
     station.read(parent, sdata);

     if (station.getName() == null) {
       log.warn("bad station name: "+station);
       return;
     }

     BufrStation check = map.get(station.getName());
     if (check == null)
       map.put(station.getName(), station);
     else {
       check.count++;
       if (!station.equals(check))
         log.warn("bad station doesnt equal "+station+" != "+check);
     }
   }

  public class BufrStation extends StationImpl {
    public int count = 1;

    void read(FieldConverter parent, StructureData sdata) {
      int count = 0;
      List<FieldConverter> flds = parent.getFlds(); // asssume these track exactly the members
      for (StructureMembers.Member m : sdata.getMembers()) {
        FieldConverter fld = flds.get(count++);
        if (fld.getType() == null) continue;

        switch (fld.getType()) {
          case stationId:
            setName(readString(sdata, m));
            break;
          case stationDesc:
            setDescription(sdata.getScalarString(m));
            break;
          case wmoId:
            setWmoId(readString(sdata, m));
            break;
          case lat:
            setLatitude(sdata.convertScalarDouble(m));
            break;
          case lon:
            setLongitude(sdata.convertScalarDouble(m));
            break;
          case height:
            setAltitude(sdata.convertScalarDouble(m));
            break;
          case heightOfStation:
            setAltitude(sdata.convertScalarDouble(m));
            break;
        }
      }
    }

    String readString(StructureData sdata, StructureMembers.Member m) {
      if (m.getDataType().isString())
        return sdata.getScalarString(m);
      else if (m.getDataType().isIntegral())
        return Integer.toString(sdata.getScalarInt(m));
      else if (m.getDataType().isNumeric())
        return Double.toString(sdata.convertScalarDouble(m));
      else return "type "+ m.getDataType();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      BufrStation that = (BufrStation) o;

      if (Double.compare(that.alt, alt) != 0) return false;
      if (Double.compare(that.lat, lat) != 0) return false;
      if (Double.compare(that.lon, lon) != 0) return false;
      if (desc != null ? !desc.equals(that.desc) : that.desc != null) return false;
      if (!name.equals(that.name)) return false;
      if (wmoId != null ? !wmoId.equals(that.wmoId) : that.wmoId != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result;
      long temp;
      temp = Double.doubleToLongBits(lat);
      result = (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(lon);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(alt);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      result = 31 * result + name.hashCode();
      result = 31 * result + (desc != null ? desc.hashCode() : 0);
      result = 31 * result + (wmoId != null ? wmoId.hashCode() : 0);
      return result;
    }

  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////

  public class FieldConverter {
    DataDescriptor dds;
    List<FieldConverter> flds;
    BufrCdmIndexProto.FldType type;
    BufrCdmIndexProto.FldAction action;
    int min = Integer.MAX_VALUE;
    int max = 0;
    boolean isSeq;

    private FieldConverter() {}

    public void addField(FieldConverter child) {
      if (flds == null)
        this.flds = new ArrayList<FieldConverter>(dds.getSubKeys().size());
      this.flds.add(child);
    }

    private FieldConverter(int center, DataDescriptor dds) {
      this.dds = dds;
      this.type = StandardFields.findField(center, dds.getFxyName());

      if (dds.getSubKeys() != null) {
        this.flds = new ArrayList<FieldConverter>(dds.getSubKeys().size());
        for (DataDescriptor subdds : dds.getSubKeys()) {
          FieldConverter subfld = new FieldConverter(center, subdds);
          flds.add(subfld);
        }
      }
    }

    public String getName() {
      return dds.getName();
    }

    public String getDesc() {
      return dds.getDesc();
    }

    public String getUnits() {
      return dds.getUnits();
    }

    public short getFxy() {
      return dds.getFxy();
    }

    public String getFxyName() {
      return dds.getFxyName();
    }

    public BufrCdmIndexProto.FldAction getAction() {
      return action;
    }

    public BufrCdmIndexProto.FldType getType() {
      return type;
    }

    public List<FieldConverter> getFlds() {
      return flds;
    }

    public boolean isSeq() {
      return isSeq;
    }

    public int getMin() {
      return min;
    }

    public int getMax() {
      return max;
    }

    public void setAction(String action) {
      BufrCdmIndexProto.FldAction act = BufrCdmIndexProto.FldAction.valueOf(action);
      if (act != null)
        this.action = act;
    }

    public void setAction(BufrCdmIndexProto.FldAction action) {
      this.action = action;
    }

    FieldConverter findChild(String want) {
      for (FieldConverter child : flds) {
        String name = child.dds.getName();
        if (name != null && name.equals(want))
          return child;
      }
      return null;
    }

    void showChildren() {
      for (FieldConverter child : flds) {
        String name = child.dds.getName();
        System.out.printf("'%s'%n",name );
      }
    }

    FieldConverter findChildByFxyName(String fxyName) {
      for (FieldConverter child : flds) {
        String name = child.dds.getFxyName();
        if (name != null && name.equals(fxyName))
          return child;
      }
      return null;
    }

    public FieldConverter getChild(int i) {
      return flds.get(i);
    }

    void trackSeqCounts(int n) {
      isSeq = true;
      if (n > max) max = n;
      if (n < min) min = n;
    }

    void showRange(Formatter f) {
      if (!isSeq) return;
      if (max == min) f.format(" isConstant='%d'", max);
      else if (max < 2) f.format(" isBinary='true'");
      else f.format(" range='[%d,%d]'", min, max);
    }

    BufrCdmIndexProto.FldAction makeAction() {
      if (!isSeq) return null;
      if (max == 0) return BufrCdmIndexProto.FldAction.remove;
      if (max < 2) return BufrCdmIndexProto.FldAction.asMissing;
      if (max == min) return BufrCdmIndexProto.FldAction.asArray;
      else return null;
    }

    void show(Formatter f, Indent indent, int index) {
      boolean hasContent = false;
      if (isSeq)
        f.format("%s<fld idx='%d' name='%s'", indent, index, dds.getName());
      else
        f.format("%s<fld idx='%d' fxy='%s' name='%s' desc='%s' units='%s' bits='%d'", indent, index,
                dds.getFxyName(), dds.getName(), dds.getDesc(), dds.getUnits(), dds.getBitWidth());

      if (type != null) f.format(" type='%s'", type);
      showRange(f);
      f.format(" action='%s'", makeAction());

      /* if (type != null) {
        f.format(">%n");
        indent.incr();
        f.format("%s<type>%s</type>%n", indent, type);
        indent.decr();
        hasContent = true;
      } */

      if (flds != null) {
        f.format(">%n");
        indent.incr();
        int subidx = 0;
        for (FieldConverter cc : flds) {
          cc.show(f, indent, subidx++);
        }
        indent.decr();
        hasContent = true;
      }

      if (hasContent)
        f.format("%s</fld>%n", indent);
      else
        f.format(" />%n");
    }
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////////


  public void show(Formatter out) {
    if (standardFields != null)
      out.format("Standard Fields%n%s%n%n", standardFields);

    Indent indent = new Indent(2);
    out.format("<bufr2nc location='%s' hash='%s' featureType='%s'>%n", filename, Integer.toHexString(messHash), featureType);
    indent.incr();
    int index = 0;
    for (FieldConverter fld : rootConverter.flds) {
      fld.show(out, indent, index++);
    }
    indent.decr();
    out.format("</bufr2nc>%n");
}

  public static void main(String[] args) throws IOException {

    String filename = "G:/work/manross/split/872d794d.bufr";
    //String filename = "Q:/cdmUnitTest/formats/bufr/US058MCUS-BUFtdp.SPOUT_00011_buoy_20091101021700.bufr";
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    BufrConfig config = BufrConfig.scanEntireFile(raf);

    Formatter out = new Formatter();
    config.show(out);
    System.out.printf("%s%n", out);

    raf.close();
  }
}
