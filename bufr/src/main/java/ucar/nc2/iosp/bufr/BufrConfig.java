package ucar.nc2.iosp.bufr;

import ucar.nc2.constants.FeatureType;
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

  static public BufrConfig openFromBufrFile(RandomAccessFile raf) throws IOException {
    return new BufrConfig( raf);
  }

  private String filename;
  private FieldConverter rootConverter;
  private  int messHash = 0;
  private FeatureType featureType;

  /**
   * Open file as a stream of BUFR messages, create config file.
   *
   * Examine length of sequences, annotate
   *
   * @param bufrFilename open this file
   * @throws java.io.IOException on IO error
   */
  private BufrConfig(String bufrFilename) throws IOException {
    this.filename =  bufrFilename;
    try {
      scanBufrFile(new RandomAccessFile(bufrFilename, "r"));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  private BufrConfig(RandomAccessFile raf) throws IOException {
    this.filename =  raf.getLocation();
    try {
      scanBufrFile(raf);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
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

  private void scanBufrFile(RandomAccessFile raf) throws Exception {
    int hashErr = 0;
    int bitcountErr = 0;
    int count = 0;
    StandardFields.Extract standardFields = null;

    try {
      MessageScanner scan = new MessageScanner(raf);
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) continue;
        if (m.containsBufrTable()) continue;
        if (count == 0) {
          messHash = m.hashCode();
          standardFields = StandardFields.extract(m);
          rootConverter = new FieldConverter(m.getRootDataDescriptor());
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
        processSequences(m.getRootDataDescriptor(), rootConverter);
        count++;
      }
    } finally {
      if (hashErr != 0)
        log.error("DDS has different hash for {}/{} for file = {}", hashErr, count, filename);
      if (bitcountErr != 0)
        log.error("Bit counting failed on {}/{} for file = {}", bitcountErr, count, filename);
      if (raf != null)
        raf.close();
    }

    featureType = guessFeatureType(standardFields);
  }

  private FeatureType guessFeatureType(StandardFields.Extract standardFields) {
    if (standardFields.hasStation()) return FeatureType.STATION;
    if (standardFields.hasTime()) return FeatureType.POINT;
    return FeatureType.NONE;
  }

  /////////////////////////////////////////////////////////////////////////////////////
  /* this whole mess if to extract the sequence lengths; maybe use a lower level API
  private void processBufrMessageAsDataset(RandomAccessFile raf, Message m) throws Exception {
    NetcdfDataset ncd = getBufrMessageAsDataset(raf, m);
    SequenceDS obs = (SequenceDS) ncd.findVariable(BufrIosp.obsRecord);
    StructureDataIterator sdataIter = obs.getStructureIterator(-1);
    processSeq(sdataIter, rootConvert);
  }

  // convert one message ino a NetcdfDataset and print data
  private NetcdfDataset getBufrMessageAsDataset(RandomAccessFile raf, Message m) throws IOException {
    BufrIosp iosp = new BufrIosp(); // note BufrIosp2 (2)
    BufrNetcdf ncfile = new BufrNetcdf(iosp, raf.getLocation());
    iosp.open(raf, ncfile, m);
    return new NetcdfDataset(ncfile);
  }

  private class BufrNetcdf extends NetcdfFile {
    protected BufrNetcdf(IOServiceProvider spi, String location) throws IOException {
      super(spi, location);
    }
  }

    // iterate through the observations
  private void processSeq(StructureDataIterator sdataIter, FieldConverter parent) throws IOException {
    try {
      while (sdataIter.hasNext()) {
        StructureData sdata = sdataIter.next();

        for (StructureMembers.Member m : sdata.getMembers()) {
          if (m.getDataType() == DataType.SEQUENCE) {
            FieldConverter seq = parent.findChild(m);
            if (seq == null) {
              log.error("BufrConfig cant find Child member= {} for file = {}", m, filename);
              continue;
            }
            ArraySequence data = (ArraySequence) sdata.getArray(m);
            int n = data.getStructureDataCount();
            seq.trackSeqCounts(n);
            processSeq(data.getStructureDataIterator(), seq);
          }
        }
      }
    } finally {
      sdataIter.finish();
    }
  }
}
  */

  /////////////////////////////////////////////////////////////////////////////////////////////////////

  public enum Action { remove, asMissing, asArray, concat }

  public class FieldConverter {
    DataDescriptor dds;
    List<FieldConverter> flds;
    StandardFields.Type type;
    int min = Integer.MAX_VALUE;
    int max = 0;
    boolean isSeq;

    private FieldConverter(DataDescriptor dds) {
      this.dds = dds;
      this.type = StandardFields.findStandardField(dds.getFxyName());

      if (dds.getSubKeys() != null) {
        this.flds = new ArrayList<FieldConverter>(dds.getSubKeys().size());
        for (DataDescriptor subdds : dds.getSubKeys()) {
          FieldConverter subfld = new FieldConverter(subdds);
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

    Action getAction() {
      if (!isSeq) return null;
      if (max == 0) return Action.remove;
      if (max < 2) return Action.asMissing;
      if (max == min) return Action.asArray;
      else return null;
    }

    void show(Formatter f, Indent indent) {
      boolean hasContent = false;
      if (isSeq)
        f.format("%s<fld name='%s'", indent, dds.getName());
      else
        f.format("%s<fld fxy='%s' name='%s' desc='%s' units='%s' bits='%d'", indent, dds.getFxyName(), dds.getName(), dds.getDesc(), dds.getUnits(), dds.getBitWidth());

      showRange(f);
      f.format(" action='%s'", getAction());

      if (type != null) {
        f.format(">%n");
        indent.incr();
        f.format("%s<type>%s</type>%n", indent, type);
        indent.decr();
        hasContent = true;
      }

      if (flds != null) {
        f.format(">%n");
        indent.incr();
        for (FieldConverter cc : flds) {
          cc.show(f, indent);
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

  private void processSequences(DataDescriptor parentDD, FieldConverter parentFld) throws IOException {
    int count = 0;
    for (DataDescriptor subdd : parentDD.getSubKeys()) {
      if (subdd.replication == 0) { // sequence
        FieldConverter seq = parentFld.getChild(count);
        seq.trackSeqCounts(n);
      }
      count++;
    }
  }
}
