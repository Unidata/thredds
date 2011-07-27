package ucar.nc2.grib.grib2;

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
                     Grib2SectionData dataSection) {

    this.header = header;
    this.is = is;
    this.id = id;
    this.lus = lus;
    this.gdss = gdss;
    this.pdss = pdss;
    this.drs = drs;
    this.bms = bms;
    this.dataSection = dataSection;
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

  public void setBms(Grib2SectionBitMap bms) {
    this.bms = bms;
  }

  public void setDataSection(Grib2SectionData dataSection) {
    this.dataSection = dataSection;
  }

  /**
   * A hash code to group records into a CDM variable
   * Herein lies the semantics of a variable object identity.
   * Read it and weep.
   * @param gdsHash can override the gdsHash
   * @return this records hash code, to group like records into a variable
   */
  public int cdmVariableHash(int gdsHash) {
    if (hashcode == 0 || gdsHash != 0) {
      int result = 17;

      if (gdsHash == 0)
        result += result * 37 + gdss.getGDS().hashCode(); // the horizontal grid
      else
        result += result * 37 + gdsHash;

      Grib2Pds pds2 = getPDS();

      result += result * 37 + getDiscipline();
      result += result * 37 + pds2.getLevelType1();
      if (Grib2Utils.isLayer(this)) result += result * 37 + 1;

      result += result * 37 + pds2.getParameterCategory();
      result += result * 37 + pds2.getTemplateNumber();

      if (pds2.isInterval())  // an interval must have a statProcessType
        result += result * 37 + pds2.getStatisticalProcessType();

      result += result * 37 + pds2.getParameterNumber();

      int ensDerivedType = -1;
      if (pds2.isEnsembleDerived()) {  // a derived ensemble must have a derivedForecastType
        Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds2;
        ensDerivedType = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
        result += result * 37 + ensDerivedType;

      } else if (pds2.isEnsemble()) {
        result += result * 37 + 1;
      }

      // each probability interval generates a separate variable; could be a dimension instead
      int probType = -1;
      if (pds2.isProbability()) {
        Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds2;
        probType = pdsProb.getProbabilityType();
        result += result * 37 + pdsProb.getProbabilityHashcode();
      }

      // if this uses any local tables, then we have to add the center id, and subcenter if present
      if ((pds2.getParameterCategory() > 191) || (pds2.getParameterNumber() > 191) || (pds2.getLevelType1() > 191)
              || (pds2.isInterval() && pds2.getStatisticalProcessType() > 191)
              || (ensDerivedType > 191) || (probType > 191)) {
        result += result * 37 + getId().getCenter_id();
        if (getId().getSubcenter_id() > 0)
          result += result * 37 + getId().getSubcenter_id();
      }

      hashcode = result;
    }
    return hashcode;
  }

  private int hashcode = 0;

  public int getFile() {
    return file;
  }

  public void setFile(int file) {
    this.file = file;
  }

  // isolate dependencies here - in case we have a "minimal I/O" mode where not all fields are available
  public float[] readData(RandomAccessFile raf) throws IOException {
    Grib2Gds gds = gdss.getGDS();

    Grib2DataReader reader = new Grib2DataReader(drs.getDataTemplate(), gdss.getNumberPoints(), drs.getDataPoints(),
            gds.scanMode, gds.nx, dataSection.getStartingPosition(), dataSection.getMsgLength());

    boolean[] bitmap = bms.getBitmap(raf, gdss.getNumberPoints());
    Grib2Drs gdrs = drs.getDrs(raf);

    return reader.getData(raf, bitmap, gdrs);
  }

  // debugging - do not use
  public Grib2Drs.Type40 readDataTest(RandomAccessFile raf) throws IOException {
    Grib2Gds gds = gdss.getGDS();

    Grib2DataReader reader = new Grib2DataReader(drs.getDataTemplate(), gdss.getNumberPoints(), drs.getDataPoints(),
            gds.scanMode, gds.nx, dataSection.getStartingPosition(), dataSection.getMsgLength());

    boolean[] bitmap = bms.getBitmap(raf, gdss.getNumberPoints());
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
            gds.scanMode, gds.nx, dataSection.getStartingPosition(), dataSection.getMsgLength());

    boolean[] bitmap = bms.getBitmap(raf, gdss.getNumberPoints());
    Grib2Drs gdrs = drs.getDrs(raf);

    return reader.getData(raf, bitmap, gdrs);
  }

  /**
   * Read data array: use when you want to be independent of the GribRecord
   *
   * @param raf             from this RandomAccessFile
   * @param drsPos          Grib2SectionDataRepresentation starts here
   * @param gdsNumberPoints gdss.getNumberPoints()
   * @param scanMode        gds.scanMode
   * @param nx              gds.nx
   * @return data as float[] array
   * @throws IOException on read error
   */
  static public float[] readData(RandomAccessFile raf, long drsPos, int gdsNumberPoints, int scanMode, int nx) throws IOException {
    raf.seek(drsPos);
    Grib2SectionDataRepresentation drs = new Grib2SectionDataRepresentation(raf);
    Grib2SectionBitMap bms = new Grib2SectionBitMap(raf);
    Grib2SectionData dataSection = new Grib2SectionData(raf);

    Grib2DataReader reader = new Grib2DataReader(drs.getDataTemplate(), gdsNumberPoints, drs.getDataPoints(),
            scanMode, nx, dataSection.getStartingPosition(), dataSection.getMsgLength());

    boolean[] bitmap = bms.getBitmap(raf, gdsNumberPoints);
    Grib2Drs gdrs = drs.getDrs(raf);

    return reader.getData(raf, bitmap, gdrs);
  }

}
