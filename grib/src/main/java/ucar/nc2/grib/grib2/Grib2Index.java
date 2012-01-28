/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib2;

import com.google.protobuf.ByteString;
import thredds.inventory.CollectionManager;
import thredds.inventory.MFile;
import ucar.nc2.grib.GribIndex;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Read and Write Grib2 index (gbx9).
 * Hides GribIndexProto
 *
 * sample use:
 * <pre>
    GribIndex index = new GribIndex();
    if (!index.readIndex(path))
      index.makeIndex(path);

    for (Grib2SectionGridDefinition gds : index.getGds()) {
      if (gdsSet.get(gds.calcCRC()) == null)
        gdsSet.put(gds.calcCRC(), gds);
    }

    for (Grib2Record gr : index.getRecords()) {
      gr.setFile(fileno);

      Grib2Pds pds = gr.getPDSsection().getPDS();
      int discipline = gr.getDiscipline();

      int id = gr.cdmVariableHash();
      Grib2ParameterBean bean = pdsSet.get(id);
      if (bean == null) {
        bean = new Grib2ParameterBean(gr);
        pdsSet.put(id, bean);
        params.add(bean);
      }
      bean.addRecord(gr);
    }
    </pre>
 *
 * @author caron
 * @since 4/1/11
 */
public class Grib2Index extends GribIndex {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib2Index.class);

  private static final String MAGIC_START = "Grib2Index";
  private static final int version = 5;
  private static final boolean debug = false;

  ////////////////////////////////////////////////////////////////////////////////////////////////

  private List<Grib2SectionGridDefinition> gdsList;
  private List<Grib2Record> records;

  public List<Grib2SectionGridDefinition> getGds() {
    return gdsList;
  }

  public List<Grib2Record> getRecords() {
    return records;
  }

  public int getNRecords() {
    return records.size();
  }

  public boolean readIndex(String filename, long gribLastModified) throws IOException {
    return readIndex(filename, gribLastModified, CollectionManager.Force.test);
  }

  public boolean readIndex(String filename, long gribLastModified, CollectionManager.Force force) throws IOException {

    File idxFile = getIndexFile(filename);
    if (!idxFile.exists()) return false;
    long idxModified = idxFile.lastModified();
    if ((force != CollectionManager.Force.nocheck) && (idxModified < gribLastModified)) return false; // force new index if file was updated

    FileInputStream fin = new FileInputStream(idxFile);

    try {
        //// check header is ok
        if (!NcStream.readAndTest(fin, MAGIC_START.getBytes())) {
          log.debug("Bad magic number of grib index, should be= {}" + MAGIC_START);
          return false;
        }

      int v = NcStream.readVInt(fin);
      if (v != version) {
        if ((v == 0) || (v > version))
          throw new IOException("GribIndex found version "+v+", want version " + version+ " on " +filename);
        log.debug("GribIndex found version "+v+", want version " + version+ " on " +filename);
        return false;
      }

      int size = NcStream.readVInt(fin);
      if (size <= 0 || size > 100 * 1000 * 1000) { // try to catch garbage
        log.warn("Grib2Index bad size = {} for {} ", size, filename);
        return false;
      }

      byte[] m = new byte[size];
      NcStream.readFully(fin, m);

      Grib2IndexProto.Grib2Index proto = Grib2IndexProto.Grib2Index.parseFrom(m);
      String fname = proto.getFilename();
      if (debug) System.out.printf("%s for %s%n", fname, filename);

      gdsList = new ArrayList<Grib2SectionGridDefinition>(proto.getGdsListCount());
      for (Grib2IndexProto.GribGdsSection pgds : proto.getGdsListList()) {
        Grib2SectionGridDefinition gds = readGds(pgds);
        gdsList.add(gds);
      }
      if (debug) System.out.printf(" read %d gds%n", gdsList.size());

      records = new ArrayList<Grib2Record>(proto.getRecordsCount());
      for (Grib2IndexProto.Grib2Record precord : proto.getRecordsList()) {
        records.add(readRecord(precord));
      }
      if (debug) System.out.printf(" read %d records%n", records.size());

    } catch (java.lang.NegativeArraySizeException e) {
      log.error("GribIndex failed on "+filename, e);
      return false;

    } finally {
      if (fin != null)
        fin.close();
    }

    return true;
  }

  /*
message Grib2Record {
 // indicator section
 required uint64 gribMessageLength = 1;
 required uint32 discipline = 2;

 // id section
 required GribIdSection ids = 3;

 // other sections
 optional bytes lus = 4;
 required sint64 gdsCrc = 5;
 required bytes pds = 6;
 required uint64 drsPos = 7;
 required uint64 bmsPos = 8;
 required uint64 dataPos = 9;
  */
  private Grib2Record readRecord(Grib2IndexProto.Grib2Record p) {
    Grib2SectionIndicator is = new Grib2SectionIndicator(p.getGribMessageStart(), p.getGribMessageLength(), p.getDiscipline());

    Grib2SectionIdentification ids = readIdMessage(p.getIds());

    Grib2SectionLocalUse lus = null;
    if (p.hasLus()) {
      lus = new Grib2SectionLocalUse(p.getLus().toByteArray());
    }

    int gdsIndex = p.getGdsIdx();
    Grib2SectionGridDefinition gds = gdsList.get(gdsIndex);
    Grib2SectionProductDefinition pds = new Grib2SectionProductDefinition(p.getPds().toByteArray());
    Grib2SectionDataRepresentation drs = new Grib2SectionDataRepresentation(p.getDrsPos(), p.getDrsNpoints(), p.getDrsTemplate());
    Grib2SectionBitMap bms = new Grib2SectionBitMap(p.getBmsPos(), p.getBmsIndicator());
    Grib2SectionData data = new Grib2SectionData(p.getDataPos(), p.getDataLen());

    return new Grib2Record(p.getHeader().toByteArray(), is, ids, lus, gds, pds, drs, bms, data);
  }

  private Grib2SectionIdentification readIdMessage(Grib2IndexProto.GribIdSection p) {
    // Grib2SectionIdentification(int center_id, int subcenter_id, int master_table_version,
    // int local_table_version, int significanceOfRT, int year, int month, int day, int hour, int minute, int second, int productionStatus, int processedDataType) {
    return new Grib2SectionIdentification(p.getCenterId(), p.getSubcenterId(),
            p.getMasterTableVersion(), p.getLocalTableVersion(), p.getSignificanceOfRT(),
            p.getRefDate(0), p.getRefDate(1), p.getRefDate(2), p.getRefDate(3), p.getRefDate(4), p.getRefDate(5),
            p.getProductionStatus(), p.getProcessedDataType());
  }

  private Grib2SectionGridDefinition readGds(Grib2IndexProto.GribGdsSection proto) {
    ByteString bytes = proto.getGds();
    return new Grib2SectionGridDefinition(bytes.toByteArray());
  }

  ////////////////////////////////////////////////////////////////////////////////

  // LOOK what about extending an index ??
  public boolean makeIndex(String filename, Formatter f) throws IOException {
    File idxFile = getIndexFile(filename);
    FileOutputStream fout = new FileOutputStream(idxFile);
    RandomAccessFile raf = null;
    try {
      //// header message
      fout.write(MAGIC_START.getBytes("UTF-8"));
      NcStream.writeVInt(fout, version);

      Map<Long, Integer> gdsMap = new HashMap<Long, Integer>();
      gdsList = new ArrayList<Grib2SectionGridDefinition>();
      records = new ArrayList<Grib2Record>(200);

      Grib2IndexProto.Grib2Index.Builder rootBuilder = Grib2IndexProto.Grib2Index.newBuilder();
      rootBuilder.setFilename(filename);

      raf = new RandomAccessFile(filename, "r");
      Grib2RecordScanner scan = new Grib2RecordScanner(raf);
      while (scan.hasNext()) {
        Grib2Record r = scan.next();
        if (r == null) break; // done
        records.add(r);

        Grib2SectionGridDefinition gds = r.getGDSsection();
        Integer index = gdsMap.get(gds.calcCRC());
        if (index == null) {
          gdsList.add(gds);
          index = gdsList.size()-1;
          gdsMap.put(gds.calcCRC(), index);
          rootBuilder.addGdsList(makeGdsProto(gds));
        }
        rootBuilder.addRecords(makeRecordProto(r, index));
      }

      Grib2IndexProto.Grib2Index index = rootBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(fout, b.length); // message size
      fout.write(b);  // message  - all in one gulp
      f.format("  made gbx9 index for %s size=%d%n", filename, b.length);
      return true;

    } finally {
      fout.close();
      if (raf != null) raf.close();
    }
  }

  private Grib2IndexProto.Grib2Record makeRecordProto(Grib2Record r, int gdsIndex) throws IOException {
    Grib2IndexProto.Grib2Record.Builder b = Grib2IndexProto.Grib2Record.newBuilder();

    b.setHeader(ByteString.copyFrom(r.getHeader()));

    // is
    b.setGribMessageStart(r.getIs().getStartPos());
    b.setGribMessageLength(r.getIs().getMessageLength());
    b.setDiscipline(r.getDiscipline());

    // is
    b.setIds(makeIdProto(r.getId()));

    // lus
    byte[] lus = r.getLocalUseSection().getRawBytes();
    if (lus != null && lus.length > 0)
      b.setLus(ByteString.copyFrom(lus));

    b.setGdsIdx(gdsIndex);
    b.setPds(ByteString.copyFrom(r.getPDSsection().getRawBytes()));

    Grib2SectionDataRepresentation drs = r.getDataRepresentationSection();
    b.setDrsPos(drs.getStartingPosition());
    b.setDrsNpoints(drs.getDataPoints());
    b.setDrsTemplate(drs.getDataTemplate());

    Grib2SectionBitMap bms = r.getBitmapSection();
    b.setBmsPos(bms.getStartingPosition());
    b.setBmsIndicator(bms.getBitMapIndicator());

    Grib2SectionData ds = r.getDataSection();
    b.setDataPos(ds.getStartingPosition());
    b.setDataLen(ds.getMsgLength());

    return b.build();
  }

  private Grib2IndexProto.GribGdsSection makeGdsProto(Grib2SectionGridDefinition gds) throws IOException {
    Grib2IndexProto.GribGdsSection.Builder b = Grib2IndexProto.GribGdsSection.newBuilder();
    b.setGds(ByteString.copyFrom(gds.getRawBytes()));
    return b.build();
  }

  /*
  message GribIdSection {
  required uint32 center_id = 1;
  required uint32 subcenter_id = 2;
  required uint32 master_table_version = 3;
  required uint32 local_table_version = 4;
  required uint32 significanceOfRT = 5;
  repeated uint32 refDate = 6 [packed=true]; // year, month, day, hour, minute, second;
  required uint32 productionStatus = 7;
  required uint32 processedDataType = 8;
}
   */
  private Grib2IndexProto.GribIdSection makeIdProto(Grib2SectionIdentification id) throws IOException {
    Grib2IndexProto.GribIdSection.Builder b = Grib2IndexProto.GribIdSection.newBuilder();

    b.setCenterId(id.getCenter_id());
    b.setSubcenterId(id.getSubcenter_id());
    b.setMasterTableVersion(id.getMaster_table_version());
    b.setLocalTableVersion(id.getLocal_table_version());
    b.setSignificanceOfRT(id.getSignificanceOfRT());
    b.addRefDate(id.getYear());
    b.addRefDate(id.getMonth());
    b.addRefDate(id.getDay());
    b.addRefDate(id.getHour());
    b.addRefDate(id.getMinute());
    b.addRefDate(id.getSecond());
    b.setProductionStatus(id.getProductionStatus());
    b.setProcessedDataType(id.getTypeOfProcessedData());

    return b.build();
  }

  static public void main(String args[]) throws IOException {
    String filename = "G:/tigge/uv/z_tigge_c_kwbc_20110605120000_glob_prod_cf_HGHT_0000_000_10_uv.grib";
    //String filename = "G:/mlode/ndfdProb/extract.grib2";
    new Grib2Index().makeIndex(filename, new Formatter(System.out));
  }

}
