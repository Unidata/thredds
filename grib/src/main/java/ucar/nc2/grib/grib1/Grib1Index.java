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

package ucar.nc2.grib.grib1;

import com.google.protobuf.ByteString;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.GribIndex;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Read and Write Grib1 index (gbx9).
 * Hides Grib1IndexProto
 *
 * sample use:
 * <pre>
      Grib1Index index = new Grib1Index();
      if (!index.readIndex(path))
        index.makeIndex(path);

      for (Grib1SectionGridDefinition gds : index.getGds()) {
        if (gdsSet.get(gds.calcCRC()) == null)
          gdsSet.put(gds.calcCRC(), gds);
      }

      for (Grib1Record gr : index.getRecords()) {
        gr.setFile(fileno);

        Grib1Pds pds = gr.getPDSsection().getPDS();
        int discipline = gr.getDiscipline();

        int id = gr.cdmVariableHash();
        Grib1ParameterBean bean = pdsSet.get(id);
        if (bean == null) {
          bean = new Grib1ParameterBean(gr);
          pdsSet.put(id, bean);
          params.add(bean);
        }
        bean.addRecord(gr);
      }
      </pre>
 *
 * @author John
 * @since 9/5/11
 */
public class Grib1Index extends GribIndex {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1Index.class);

  public static final String MAGIC_START = "Grib1Index";
  private static final int version = 5;
  private static final boolean debug = false;

  ////////////////////////////////////////////////////////////////////////////////////////////////

  private List<Grib1SectionGridDefinition> gdsList;
  private List<Grib1Record> records;

  public List<Grib1SectionGridDefinition> getGds() {
    return gdsList;
  }

  public List<Grib1Record> getRecords() {
    return records;
  }

  @Override
  public int getNRecords() {
    return records.size();
  }

  public boolean readIndex(String filename, long gribLastModified) throws IOException {
    return readIndex(filename, gribLastModified, CollectionUpdateType.test);
  }

  public boolean readIndex(String filename, long gribLastModified, CollectionUpdateType force) throws IOException {
    String idxPath = filename;
    if (!idxPath.endsWith(GBX9_IDX)) idxPath += GBX9_IDX;
    File idxFile = GribIndexCache.getExistingFileOrCache(idxPath);
    if (idxFile == null) return false;

    long idxModified = idxFile.lastModified();
    if ((force != CollectionUpdateType.nocheck) && (idxModified < gribLastModified)) return false; // force new index if file was updated

    try (FileInputStream fin = new FileInputStream(idxFile)) {
      //// check header is ok
      if (!NcStream.readAndTest(fin, MAGIC_START.getBytes(CDM.utf8Charset))) {
        logger.info("Bad magic number of grib index, on file" + idxFile);
        return false;
      }

      int v = NcStream.readVInt(fin);
      if (v != version) {
        if ((v == 0) || (v > version))
          throw new IOException("Grib1Index found version " + v + ", want version " + version + " on " + filename);
        if (logger.isDebugEnabled()) logger.debug("Grib1Index found version " + v + ", want version " + version + " on " + filename);
        return false;
      }

      int size = NcStream.readVInt(fin);
      if (size <= 0 || size > 100 * 1000 * 1000) { // try to catch garbage
        logger.warn("Grib1Index bad size = {} for {} ", size, filename);
        return false;
      }

      byte[] m = new byte[size];
      NcStream.readFully(fin, m);

      Grib1IndexProto.Grib1Index proto = Grib1IndexProto.Grib1Index.parseFrom(m);
      if (debug) System.out.printf("%s for %s%n",  proto.getFilename(), filename);

      gdsList = new ArrayList<>(proto.getGdsListCount());
      for (Grib1IndexProto.Grib1GdsSection pgds : proto.getGdsListList()) {
        Grib1SectionGridDefinition gds = readGds(pgds);
        gdsList.add(gds);
      }
      if (debug) System.out.printf(" read %d gds%n", gdsList.size());

      records = new ArrayList<>(proto.getRecordsCount());
      for (Grib1IndexProto.Grib1Record precord : proto.getRecordsList()) {
        records.add(readRecord(precord));
      }
      if (debug) System.out.printf(" read %d records%n", records.size());

    } catch (java.lang.NegativeArraySizeException e) {
      logger.error("GribIndex failed on " + filename, e);
      return false;

    } catch (IOException e) {
      //for (String fn : RandomAccessFile.getOpenFiles()) {
      //  System.out.printf("  %s%n", fn);
      //}

      logger.error("GribIndex failed on " + filename, e);
      return false;

    }

    return true;
  }

  // deserialize the Grib1Record object
  private Grib1Record readRecord(Grib1IndexProto.Grib1Record p) {
    Grib1SectionIndicator is = new Grib1SectionIndicator(p.getGribMessageStart(), p.getGribMessageLength());
    Grib1SectionProductDefinition pds = new Grib1SectionProductDefinition(p.getPds().toByteArray());

    Grib1SectionGridDefinition gds = pds.gdsExists() ? gdsList.get(p.getGdsIdx()) : new Grib1SectionGridDefinition(pds);
    Grib1SectionBitMap bms = pds.bmsExists() ? new Grib1SectionBitMap(p.getBmsPos()) : null;

    Grib1SectionBinaryData dataSection = new Grib1SectionBinaryData(p.getDataPos(), p.getDataLen());
    return new Grib1Record(p.getHeader().toByteArray(), is, gds, pds, bms, dataSection);
  }

  private Grib1SectionGridDefinition readGds(Grib1IndexProto.Grib1GdsSection proto) {
    ByteString bytes = proto.getGds();
    return new Grib1SectionGridDefinition(bytes.toByteArray());
  }

  ////////////////////////////////////////////////////////////////////////////////

  // LOOK what about extending an index ??
  public boolean makeIndex(String filename, RandomAccessFile dataRaf) throws IOException {
    String idxPath = filename;
    if (!idxPath.endsWith(GBX9_IDX)) idxPath += GBX9_IDX;
    File idxFile = GribIndexCache.getFileOrCache(idxPath);
    File idxFileTmp = GribIndexCache.getFileOrCache(idxPath + ".tmp");

    RandomAccessFile raf = null;
    try (FileOutputStream fout = new FileOutputStream(idxFileTmp)) {
      //// header message
      fout.write(MAGIC_START.getBytes(CDM.utf8Charset));
      NcStream.writeVInt(fout, version);

      Map<Long, Integer> gdsMap = new HashMap<>();
      gdsList = new ArrayList<>();
      records = new ArrayList<>(200);

      Grib1IndexProto.Grib1Index.Builder rootBuilder = Grib1IndexProto.Grib1Index.newBuilder();
      rootBuilder.setFilename(filename);

      if (dataRaf == null)  { // open if dataRaf not already open
        raf = RandomAccessFile.acquire(filename);
        dataRaf = raf;
      }

      Grib1RecordScanner scan = new Grib1RecordScanner(dataRaf);
      while (scan.hasNext()) {
        Grib1Record r = scan.next();
        if (r == null) break; // done
        records.add(r);

        Grib1SectionGridDefinition gdss = r.getGDSsection();
        Integer index = gdsMap.get(gdss.calcCRC());
        if (gdss.getPredefinedGridDefinition() >= 0) // skip predefined gds - they dont have raw bytes
          index = 0;
        else if (index == null) {
          gdsList.add(gdss);
          index = gdsList.size() - 1;
          gdsMap.put(gdss.calcCRC(), index);
          rootBuilder.addGdsList(makeGdsProto(gdss));
        }
        rootBuilder.addRecords(makeRecordProto(r, index));
      }

      ucar.nc2.grib.grib1.Grib1IndexProto.Grib1Index index = rootBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(fout, b.length); // message size
      fout.write(b);  // message  - all in one gulp
      logger.debug("  made gbx9 index for {} size={}", filename, b.length);
      return true;

    } finally {
      if (raf != null) raf.close();   // only close if it was opened here

            // now switch
      RandomAccessFile.eject(idxFile.getPath());
      boolean deleteOk = !idxFile.exists() || idxFile.delete();
      boolean renameOk = idxFileTmp.renameTo(idxFile);
      if (!deleteOk)
        logger.error("  could not delete Grib2Index= {}", idxFile.getPath());
      if (!renameOk)
        logger.error("  could not rename Grib2Index= {}", idxFile.getPath());
    }
  }

  private ucar.nc2.grib.grib1.Grib1IndexProto.Grib1Record makeRecordProto(Grib1Record r, int gdsIndex) throws IOException {
    Grib1IndexProto.Grib1Record.Builder b = Grib1IndexProto.Grib1Record.newBuilder();

    b.setHeader(ByteString.copyFrom(r.getHeader()));

    b.setGribMessageStart(r.getIs().getStartPos());
    b.setGribMessageLength(r.getIs().getMessageLength());

    b.setGdsIdx(gdsIndex);
    Grib1SectionProductDefinition pds = r.getPDSsection();
    b.setPds(ByteString.copyFrom(pds.getRawBytes()));

    if (pds.bmsExists()) {
      Grib1SectionBitMap bms = r.getBitMapSection();
      b.setBmsPos(bms.getStartingPosition());
    }

    Grib1SectionBinaryData ds = r.getDataSection();
    b.setDataPos(ds.getStartingPosition());
    b.setDataLen(ds.getLength());

    return b.build();
  }

  private Grib1IndexProto.Grib1GdsSection makeGdsProto(Grib1SectionGridDefinition gds) throws IOException {
    Grib1IndexProto.Grib1GdsSection.Builder b = Grib1IndexProto.Grib1GdsSection.newBuilder();
    b.setGds(ByteString.copyFrom(gds.getRawBytes()));
    return b.build();
  }

  static public void main(String args[]) throws IOException {
   String gribName = args[0];
   new Grib1Index().makeIndex(gribName, null);
  }

}

