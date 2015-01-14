package ucar.nc2.grib.grib2;

import ucar.nc2.grib.GribData;
import ucar.nc2.grib.QuasiRegular;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.Formatter;

/**
 * Read one Record from a GRIB-2 files
 *
 * @author caron
 * @since 3/28/11
 */
public class Grib2Record {

  //////////////////////////////////////////////////////////////
  private final Grib2SectionIndicator is;
  private final Grib2SectionIdentification id;
  private Grib2SectionLocalUse lus; // local use section
  private Grib2SectionGridDefinition gdss;
  private Grib2SectionProductDefinition pdss;
  private Grib2SectionDataRepresentation drss;
  private Grib2SectionBitMap bms;
  private Grib2SectionData dataSection;

  private Grib2Pds pds2 = null;
  private Grib2Gds gds2 = null;


  private final byte[] header; // anything in between the records - eg idd header
  private int file; // for multiple files in same dataset
  private boolean bmsReplaced;
  private int scanMode;

  public int repeat; // debug = see Grib2Report.doDrsSummary

  /**
   * Construction for Grib2Record.
   *
   * @param header      Grib header
   * @param is          Grib2IndicatorSection
   * @param id          Grib2IdentificationSection
   * @param lus         raw bytes of local use section
   * @param gdss        Grib2GridDefinitionSection
   * @param pdss        Grib2ProductDefinitionSection
   * @param drs         Grib2SectionDataRepresentation
   * @param bms         Grib2SectionBitMap
   * @param dataSection Grib2SectionData
   * @param bmsReplaced Grib2SectionData
   * @param scanMode    from GDS of this record
   */
  public Grib2Record(byte[] header, Grib2SectionIndicator is,
                     Grib2SectionIdentification id,
                     Grib2SectionLocalUse lus,
                     Grib2SectionGridDefinition gdss,
                     Grib2SectionProductDefinition pdss,
                     Grib2SectionDataRepresentation drs,
                     Grib2SectionBitMap bms,
                     Grib2SectionData dataSection,
                     boolean bmsReplaced,
                     int scanMode) {

    this.header = header;
    this.is = is;
    this.id = id;
    this.lus = lus;
    this.gdss = gdss;
    this.pdss = pdss;
    this.drss = drs;
    this.bms = bms;
    this.dataSection = dataSection;
    this.bmsReplaced = bmsReplaced;

    // stored in index file after 4.5 2/6/2014, otherwise equals Grib2Index.ScanModeMissing, so get it from the GDS, which may have wrong one
    this.scanMode = scanMode;
    if (scanMode == Grib2Index.ScanModeMissing && gdss != null) {
      this.scanMode = gdss.getGDS().getScanMode();
    }
  }

  // copy constructor
  Grib2Record(Grib2Record from) {
    this.header = from.header;
    this.is = from.is;
    this.id = from.id;
    this.lus = from.lus;
    this.gdss = from.gdss;
    this.pdss = from.pdss;
    this.drss = from.drss;
    this.bms = from.bms;
    this.dataSection = from.dataSection;
    this.repeat = from.repeat;
    this.bmsReplaced = from.bmsReplaced;
    this.scanMode = from.scanMode;
  }

  public byte[] getHeader() {
    return header;
  }

  public Grib2SectionIndicator getIs() {
    return is;
  }

  public Grib2SectionIdentification getId() {
    return id;
  }

  public boolean hasLocalUseSection() {
    return lus != null && lus.getRawBytes() != null;
  }

  public Grib2SectionLocalUse getLocalUseSection() {
    return lus;
  }

  public Grib2SectionGridDefinition getGDSsection() {
    return gdss;
  }

  public Grib2SectionProductDefinition getPDSsection() {
    return pdss;
  }

  public Grib2SectionDataRepresentation getDataRepresentationSection() {
    return drss;
  }

  public Grib2SectionBitMap getBitmapSection() {
    return bms;
  }

  public Grib2SectionData getDataSection() {
    return dataSection;
  }

  public int getDiscipline() {
    return is.getDiscipline();
  }

  public CalendarDate getReferenceDate() {
    return id.getReferenceDate();
  }

  public Grib2Pds getPDS() {
    if (pds2 == null)
      pds2 = pdss.getPDS();
    return pds2;
  }

  public Grib2Gds getGDS() {
    if (gds2 == null)
      gds2 = gdss.getGDS();
    return gds2;
  }

  public int getScanMode() {
    return scanMode;
  }

  public void show(Formatter f) {
    f.format("discipline=%d ", is.getDiscipline());
    Grib2Pds pds = getPDS();
    pds.show(f);
  }

  //////////////////////////////////////////
  // setters used by repeating records


  public void setLus(Grib2SectionLocalUse lus) {
    this.lus = lus;
  }

  public void setGdss(Grib2SectionGridDefinition gdss) {
    this.gdss = gdss;
  }

  public void setPdss(Grib2SectionProductDefinition pdss) {
    this.pdss = pdss;
  }

  public void setDrs(Grib2SectionDataRepresentation drs) {
    this.drss = drs;
  }

  public void setBms(Grib2SectionBitMap bms, boolean replaced) {
    this.bms = bms;
    this.bmsReplaced = replaced;
  }

  public void setDataSection(Grib2SectionData dataSection) {
    this.dataSection = dataSection;
  }

  public int getFile() {
    return file;
  }

  public void setFile(int file) {
    this.file = file;
  }

  public boolean isBmsReplaced() {
    return bmsReplaced;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Grib2Record{");
    sb.append("file=").append(file);
    sb.append(", ref=").append(getReferenceDate());
    sb.append(", dataPos=").append(dataSection.getStartingPosition());
    sb.append('}');
    return sb.toString();
  }

  // isolate dependencies here - in case we have a "minimal I/O" mode where not all fields are available
  public float[] readData(RandomAccessFile raf) throws IOException {
    Grib2Gds gds = getGDS();

    Grib2DataReader2 reader = new Grib2DataReader2(drss.getDataTemplate(), gdss.getNumberPoints(), drss.getDataPoints(),
            getScanMode(), gds.getNxRaw(), dataSection.getStartingPosition(), dataSection.getMsgLength());

    Grib2Drs gdrs = drss.getDrs(raf);

    float[] data = reader.getData(raf, bms, gdrs);

    if (gds.isThin())
      data = QuasiRegular.convertQuasiGrid(data, gds.getNptsInLine(), gds.getNxRaw(), gds.getNyRaw(), GribData.getInterpolationMethod());

    lastRecordRead = this;
    return data;
  }

  // debugging - do not use
  public int[] readRawData(RandomAccessFile raf) throws IOException {
    Grib2Gds gds = getGDS();

    Grib2DataReader2 reader = new Grib2DataReader2(drss.getDataTemplate(), gdss.getNumberPoints(), drss.getDataPoints(),
            getScanMode(), gds.getNxRaw(), dataSection.getStartingPosition(), dataSection.getMsgLength());

    Grib2Drs gdrs = drss.getDrs(raf);

    return reader.getRawData(raf, bms, gdrs);
  }

  // debugging - do not use
  public Grib2Drs.Type40 readDataTest(RandomAccessFile raf) throws IOException {
    Grib2Gds gds = getGDS();

    Grib2DataReader2 reader = new Grib2DataReader2(drss.getDataTemplate(), gdss.getNumberPoints(), drss.getDataPoints(),
            getScanMode(), gds.getNxRaw(), dataSection.getStartingPosition(), dataSection.getMsgLength());

    Grib2Drs gdrs = drss.getDrs(raf);
    if (gdrs instanceof Grib2Drs.Type40) {
      reader.getData(raf, bms, gdrs);
      return (Grib2Drs.Type40) gdrs;
    }
    return null;
  }

  /**
   * Read data array
   *
   * @param raf    from this RandomAccessFile
   * @param drsPos Grib2SectionDataRepresentation starts here
   * @return data as float[] array
   * @throws IOException on read error
   */
  public float[] readData(RandomAccessFile raf, long drsPos) throws IOException {
    raf.seek(drsPos);
    Grib2SectionDataRepresentation drs = new Grib2SectionDataRepresentation(raf);
    Grib2SectionBitMap bms = new Grib2SectionBitMap(raf);
    Grib2SectionData dataSection = new Grib2SectionData(raf);

    Grib2Gds gds = getGDS();
    Grib2DataReader2 reader = new Grib2DataReader2(drs.getDataTemplate(), gdss.getNumberPoints(), drs.getDataPoints(),
            getScanMode(), gds.getNxRaw(), dataSection.getStartingPosition(), dataSection.getMsgLength());

    Grib2Drs gdrs = drs.getDrs(raf);

    float[] data = reader.getData(raf, bms, gdrs);

    if (gds.isThin())
      data = QuasiRegular.convertQuasiGrid(data, gds.getNptsInLine(), gds.getNxRaw(), gds.getNyRaw(), GribData.getInterpolationMethod());

    lastRecordRead = this;
    return data;
  }

  //         float[] data = Grib2Record.readData(rafData, dr.drsPos, vindex.group.hcs.gdsNumberPoints, vindex.group.hcs.scanMode, vindex.group.hcs.nx);


  /**
   * Read data array: use when you want to be independent of the GribRecord
   *
   * @param raf             from this RandomAccessFile
   * @param drsPos          Grib2SectionDataRepresentation starts here
   * @param bmsPos          if non-zero, use the bms that starts here
   * @param gdsNumberPoints gdss.getNumberPoints()
   * @param scanMode        gds.scanMode
   * @param nx              gds.nx
   * @return data as float[] array
   * @throws IOException on read error
   */
  static public float[] readData(RandomAccessFile raf, long drsPos, long bmsPos, int gdsNumberPoints, int scanMode, int nx, int ny, int[] nptsInLine) throws IOException {
    raf.seek(drsPos);
    Grib2SectionDataRepresentation drs = new Grib2SectionDataRepresentation(raf);
    Grib2SectionBitMap bms = new Grib2SectionBitMap(raf);
    Grib2SectionData dataSection = new Grib2SectionData(raf);

    if (bmsPos > 0)
      bms = Grib2SectionBitMap.factory(raf, bmsPos);

    Grib2DataReader2 reader = new Grib2DataReader2(drs.getDataTemplate(), gdsNumberPoints, drs.getDataPoints(),
            scanMode, nx, dataSection.getStartingPosition(), dataSection.getMsgLength());

    Grib2Drs gdrs = drs.getDrs(raf);

    //return reader.getData(raf, bitmap, gdrs);

    float[] data = reader.getData(raf, bms, gdrs);

    if (nptsInLine != null)
      data = QuasiRegular.convertQuasiGrid(data, nptsInLine, nx, ny, GribData.getInterpolationMethod());

    if (getlastRecordRead)
      lastRecordRead = Grib2RecordScanner.findRecordByDrspos(raf, drsPos);
    return data;
  }

  public void check(RandomAccessFile raf, Formatter f) throws IOException {
    long messLen = is.getMessageLength();
    long startPos = is.getStartPos();
    long endPos = is.getEndPos();

    if (endPos > raf.length()) {
      f.format("End of GRIB message (start=%d len=%d) end=%d > file.length=%d for %s%n", startPos, messLen , endPos, raf.length(), raf.getLocation());
      return;
    }

    raf.seek(endPos-4);
    for (int i = 0; i < 4; i++) {
      if (raf.read() != 55) {
        String clean = StringUtil2.cleanup(header);
        if (clean.length() > 40) clean = clean.substring(0,40) + "...";
        f.format("Missing End of GRIB message (start=%d len=%d) end=%d header= %s for %s (len=%d)%n", startPos, messLen, endPos, clean, raf.getLocation(), raf.length());
        break;
      }
    }

    long dataLen = dataSection.getMsgLength();
    long dataStart = dataSection.getStartingPosition();
    long dataEnd = dataStart + dataLen;

    if (dataEnd > raf.length()) {
      f.format("GRIB data section (start=%d len=%d) end=%d > file.length=%d for %s%n", dataStart, dataLen, dataEnd, raf.length(), raf.getLocation());
      return;
    }

    if (dataEnd > endPos) {
      f.format("GRIB data section (start=%d len=%d) end=%d > message end=%d for %s%n", dataStart, dataLen, dataEnd, endPos, raf.getLocation());
    }

  }

  public GribData.Info getBinaryDataInfo(RandomAccessFile raf) throws IOException {
    GribData.Info info = this.drss.getDrs(raf).getBinaryDataInfo(raf);
    info.bitmapLength = (bms == null) ? 0 : bms.getLength(raf);
    info.msgLength = is.getMessageLength();
    info.dataLength = dataSection.getMsgLength();
    info.ndataPoints = drss.getDataPoints();
    Grib2Gds gds = getGDS();
    info.nPoints = gds.getNx() * gds.getNy();
    return info;
  }

  // debugging do not use
  public static boolean getlastRecordRead;
  public static Grib2Record lastRecordRead;

}
