package ucar.nc2.iosp.bufr;

import org.jdom2.Element;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.Indent;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Configuration for converting BUFR files to CDM
 * DataDescriptor tree becomes FieldConverter tree with annotations.
 * @author caron
 * @since 8/8/13
 */
public class BufrConfig {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrConfig.class);

  static public BufrConfig openFromBufrFile(RandomAccessFile raf, boolean read) throws IOException {
    return new BufrConfig( raf, read);
  }

  static public BufrConfig openFromMessage(RandomAccessFile raf, Message m, Element iospParam) throws IOException {
    BufrConfig config = new BufrConfig( raf, m);
    if (iospParam != null)
      config.merge(iospParam);
    return config;
  }

  private String filename;
  private StandardFields.Extract standardFields;
  private FieldConverter rootConverter;
  private int messHash = 0;
  private int nmess = 0;
  private FeatureType featureType;

  /**
   * Open file as a stream of BUFR messages, create config file.
   *
   * Examine length of sequences, annotate
   *
   * @param bufrFilename open this file
   * @throws java.io.IOException on IO error
   */
  private BufrConfig(String bufrFilename, boolean read) throws IOException {
    this.filename =  bufrFilename;
    try {
      scanBufrFile(new RandomAccessFile(bufrFilename, "r"), read);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  private BufrConfig(RandomAccessFile raf, boolean read) throws IOException {
    this.filename =  raf.getLocation();
    try {
      scanBufrFile(raf, read);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  private BufrConfig(RandomAccessFile raf, Message m) throws IOException {
    this.filename =  raf.getLocation();
    this.messHash = m.hashCode();
    this.rootConverter = new FieldConverter(m.ids.getCenterId(), m.getRootDataDescriptor());
  }

  public String getFilename() {
    return filename;
  }

  public FieldConverter getRootConverter() {
    return rootConverter;
  }

  public int getMessHash() {
    return messHash;
  }

  public FeatureType getFeatureType() {
    return featureType;
  }

  public FieldConverter getStandardField(StandardFields.Type want) {
    for (FieldConverter fld : rootConverter.flds)
      if (fld.type == want) return fld;
    return null;
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

  private void scanBufrFile(RandomAccessFile raf, boolean read) throws Exception {
    int hashErr = 0;
    int bitcountErr = 0;
    nmess = 0;

    try {
      MessageScanner scan = new MessageScanner(raf);
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) continue;
        if (m.containsBufrTable()) continue;
        if (nmess == 0) {
          messHash = m.hashCode();
          standardFields = StandardFields.extract(m);
          rootConverter = new FieldConverter(m.ids.getCenterId(), m.getRootDataDescriptor());
          if (!read) return;

          /* m.isBitCountOk();
          if (!m.dds.isCompressed() ) {
             Formatter out = new Formatter();
             Indent indent = new Indent(2);

             out.format("Start of message, ndataset=%d%n", m.counterDatasets.length);
             for (BitCounterUncompressed bcu : m.counterDatasets) {
               bcu.toString(out, indent);
             }
             System.out.printf("%s%n", out);
           } */
        }

        if (messHash != m.hashCode()) {
          hashErr++;
          continue; // keep trying
        }

        boolean bitsOk = m.isBitCountOk();
        if (!bitsOk) {
          bitcountErr++;
          continue; // keep trying
        }

        processSeq(raf, m);
        nmess++;
      }
    } finally {
      if (hashErr != 0)
        log.error("DDS has different hash for {}/{} for file = {}", hashErr, nmess, filename);
      if (bitcountErr != 0)
        log.error("Bit counting failed on {}/{} for file = {}", bitcountErr, nmess, filename);
    }

    if (standardFields != null)
      featureType = guessFeatureType(standardFields);
    else
      System.out.println("HEY");
  }

  private FeatureType guessFeatureType(StandardFields.Extract standardFields) {
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

    processSeq(data.getStructureDataIterator(), rootConverter);
  }

  private void processSeq(StructureDataIterator sdataIter, FieldConverter parent) throws IOException {
     try {
       while (sdataIter.hasNext()) {
         StructureData sdata = sdataIter.next();

         for (StructureMembers.Member m : sdata.getMembers()) {
           if (m.getDataType() == DataType.SEQUENCE) {
             FieldConverter fld = parent.findChild(m.getName());
             if (fld == null) {
               log.error("BufrConfig cant find Child member= {} for file = {}", m, filename);
               continue;
             }
             ArraySequence data = (ArraySequence) sdata.getArray(m);
             int n = data.getStructureDataCount();
             fld.trackSeqCounts(n);
             processSeq(data.getStructureDataIterator(), fld);
           }
         }
       }
     } finally {
       sdataIter.finish();
     }
   }

  /////////////////////////////////////////////////////////////////////////////////////////////////////

  public enum Action { remove, asMissing, asArray, concat }

  public class FieldConverter {
    DataDescriptor dds;
    List<FieldConverter> flds;
    StandardFields.Type type;
    Action action;
    int min = Integer.MAX_VALUE;
    int max = 0;
    boolean isSeq;

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
      return dds.name;
    }

    public String getDesc() {
      return dds.desc;
    }

    public String getUnits() {
      return dds.units;
    }

    public String getFxyName() {
      return dds.getFxyName();
    }

    public Action getAction() {
      return action;
    }

    public void setAction(String action) {
      Action act = Action.valueOf(action);
      if (act != null)
        this.action = act;
    }

    FieldConverter findChild(String want) {
      for (FieldConverter child : flds) {
        String name = child.dds.getName();
        if (name != null && name.equals(want))
          return child;
      }
      return null;
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

    Action makeAction() {
      if (!isSeq) return null;
      if (max == 0) return Action.remove;
      if (max < 2) return Action.asMissing;
      if (max == min) return Action.asArray;
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
    out.format("<bufr2nc location='%s' hash='%s' nmess='%s' featureType='%s'>%n", filename, Integer.toHexString(messHash), nmess, featureType);
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
    BufrConfig config = BufrConfig.openFromBufrFile(raf, true);

    Formatter out = new Formatter();
    config.show(out);
    System.out.printf("%s%n", out);

    raf.close();
  }
}
