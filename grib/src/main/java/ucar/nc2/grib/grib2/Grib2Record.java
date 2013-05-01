package ucar.nc2.grib.grib2;

import ucar.nc2.grib.QuasiRegular;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;

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
  private Grib2SectionDataRepresentation drs;
  private Grib2SectionBitMap bms;
  private Grib2SectionData dataSection;

  private final byte[] header; // anything in between the records - eg idd header
  private int file; // for multiple files in same dataset
  private boolean bmsReplaced;

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
   */
  public Grib2Record(byte[] header, Grib2SectionIndicator is,
                     Grib2SectionIdentification id,
                     Grib2SectionLocalUse lus,
                     Grib2SectionGridDefinition gdss,
                     Grib2SectionProductDefinition pdss,
                     Grib2SectionDataRepresentation drs,
                     Grib2SectionBitMap bms,
                     Grib2SectionData dataSection,
                     boolean bmsReplaced) {

    this.header = header;
    this.is = is;
    this.id = id;
    this.lus = lus;
    this.gdss = gdss;
    this.pdss = pdss;
    this.drs = drs;
    this.bms = bms;
    this.dataSection = dataSection;
    this.bmsReplaced = bmsReplaced;
  }

  // copy constructor
  Grib2Record(Grib2Record from) {
    this.header = from.header;
    this.is = from.is;
    this.id = from.id;
    this.lus = from.lus;
    this.gdss = from.gdss;
    this.pdss = from.pdss;
    this.drs = from.drs;
    this.bms = from.bms;
    this.dataSection = from.dataSection;
    this.repeat = from.repeat;
    this.bmsReplaced = from.bmsReplaced;
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
    return drs;
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

  private Grib2Pds pds2 = null; // hmmm, should we always cache ?

  public Grib2Pds getPDS() {
    if (pds2 == null)
      try {
        pds2 = pdss.getPDS();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    return pds2;
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
    this.drs = drs;
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

  // isolate dependencies here - in case we have a "minimal I/O" mode where not all fields are available
  public float[] readData(RandomAccessFile raf) throws IOException {
    Grib2Gds gds = gdss.getGDS();

    Grib2DataReader reader = new Grib2DataReader(drs.getDataTemplate(), gdss.getNumberPoints(), drs.getDataPoints(),
            gds.scanMode, gds.getNxRaw(), dataSection.getStartingPosition(), dataSection.getMsgLength());

    byte[] bitmap = bms.getBitmap(raf);
    Grib2Drs gdrs = drs.getDrs(raf);

    float[] data = reader.getData(raf, bitmap, gdrs);

    if (gds.isThin())
      data = QuasiRegular.convertQuasiGrid(data, gds.getNptsInLine(), gds.getNxRaw(), gds.getNyRaw());

    return data;
  }

  // debugging - do not use
  public Grib2Drs.Type40 readDataTest(RandomAccessFile raf) throws IOException {
    Grib2Gds gds = gdss.getGDS();

    Grib2DataReader reader = new Grib2DataReader(drs.getDataTemplate(), gdss.getNumberPoints(), drs.getDataPoints(),
            gds.scanMode, gds.getNxRaw(), dataSection.getStartingPosition(), dataSection.getMsgLength());

    byte[] bitmap = bms.getBitmap(raf);
    Grib2Drs gdrs = drs.getDrs(raf);
    if (gdrs instanceof Grib2Drs.Type40) {
      reader.getData(raf, bitmap, gdrs);
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

    Grib2Gds gds = gdss.getGDS();
    Grib2DataReader reader = new Grib2DataReader(drs.getDataTemplate(), gdss.getNumberPoints(), drs.getDataPoints(),
            gds.scanMode, gds.getNxRaw(), dataSection.getStartingPosition(), dataSection.getMsgLength());

    byte[] bitmap = bms.getBitmap(raf);
    Grib2Drs gdrs = drs.getDrs(raf);

    float[] data = reader.getData(raf, bitmap, gdrs);

    if (gds.isThin())
      data = QuasiRegular.convertQuasiGrid(data, gds.getNptsInLine(), gds.getNxRaw(), gds.getNyRaw());

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

    Grib2DataReader reader = new Grib2DataReader(drs.getDataTemplate(), gdsNumberPoints, drs.getDataPoints(),
            scanMode, nx, dataSection.getStartingPosition(), dataSection.getMsgLength());

    byte[] bitmap = bms.getBitmap(raf);
    Grib2Drs gdrs = drs.getDrs(raf);

    //return reader.getData(raf, bitmap, gdrs);

    float[] data = reader.getData(raf, bitmap, gdrs);

    if (nptsInLine != null)
      data = QuasiRegular.convertQuasiGrid(data, nptsInLine, nx, ny);

    return data;
  }

}
