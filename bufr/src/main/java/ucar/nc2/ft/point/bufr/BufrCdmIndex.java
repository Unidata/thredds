package ucar.nc2.ft.point.bufr;

import ucar.nc2.iosp.bufr.BufrConfig;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manage cdm index (ncx) for Bufr files.
 * Covers BufrCdmIndexProto.
 *
 * @author caron
 * @since 8/14/13
 */
public class BufrCdmIndex {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrCdmIndex.class);

  public static final String MAGIC_START = "BufrCdmIndex";
  public static final String NCX_IDX = ".ncx";
  private static final int version = 1;

  public static File calcIndexFile(String bufrFilename ) {
    File bufrFile = new File(bufrFilename);
    String name = bufrFile.getName();
    int pos = name.lastIndexOf('/');
    if (pos < 0) pos = name.lastIndexOf('\\');
    if (pos > 0) name = name.substring(pos + 1);
    return new File(bufrFile.getParent(), name + BufrCdmIndex.NCX_IDX);
  }

  public static boolean writeIndex(String bufrFilename, BufrConfig config, File idxFile) throws IOException {
    return new BufrCdmIndex().writeIndex2(bufrFilename, config, idxFile);
  }

  public static BufrCdmIndex readIndex(String indexFilename) throws IOException {
    BufrCdmIndex index =  new BufrCdmIndex();
    RandomAccessFile raf = null;
    try {
       raf = new RandomAccessFile(indexFilename, "r");
       index.readIndex(raf);
    } finally {
      if (raf != null) raf.close();
    }
    return index;
  }

  /////////////////////////////////////////////////////////////////////////////////

  /*
   MAGIC_START
   version
   sizeIndex
   BufrCdmIndexProto (sizeIndex bytes)
  */
  private boolean writeIndex2(String bufrFilename, BufrConfig config, File indexFile) throws IOException {
    if (indexFile.exists()) {
      if (!indexFile.delete())
        log.warn(" BufrCdmIndex cant delete index file {}", indexFile.getPath());
    }
    log.debug(" createIndex for {}", indexFile.getPath());

    RandomAccessFile raf = new RandomAccessFile(indexFile.getPath(), "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    try {
      //// header message
      raf.write(MAGIC_START.getBytes("UTF-8"));
      raf.writeInt(version);

      // build it
      BufrCdmIndexProto.BufrIndex.Builder indexBuilder = BufrCdmIndexProto.BufrIndex.newBuilder();
      indexBuilder.setFilename(bufrFilename);
      root = buildField(config.getRootConverter());
      indexBuilder.setRoot(root);
      indexBuilder.setStart(config.getStart());
      indexBuilder.setEnd(config.getEnd());

      Map<String,BufrConfig.BufrStation> smaps = config.getStationMap();
      if (smaps != null) {
        List<BufrConfig.BufrStation> stations = new ArrayList<BufrConfig.BufrStation>(smaps.values());
        Collections.sort(stations);
        for (BufrConfig.BufrStation s : stations) {
          indexBuilder.addStations(buildStation(s));
        }
      }

      // write it
      BufrCdmIndexProto.BufrIndex index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp

      return true;

    } finally {
      log.debug("  file size =  %d bytes", raf.length());
      raf.close();
    }
  }

  public static boolean writeIndex(BufrCdmIndex index, BufrField root, File indexFile) throws IOException {
    if (indexFile.exists()) {
      if (!indexFile.delete())
        log.warn(" BufrCdmIndex cant delete index file {}", indexFile.getPath());
    }
    log.debug(" createIndex for {}", indexFile.getPath());

    RandomAccessFile raf = new RandomAccessFile(indexFile.getPath(), "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    try {
      //// header message
      raf.write(MAGIC_START.getBytes("UTF-8"));
      raf.writeInt(version);

      // build it
      BufrCdmIndexProto.BufrIndex.Builder indexBuilder = BufrCdmIndexProto.BufrIndex.newBuilder();
      indexBuilder.setFilename(index.bufrFilename);
      BufrCdmIndexProto.Field rootf = buildField(root);
      indexBuilder.setRoot( rootf);
      indexBuilder.setStart(index.start);
      indexBuilder.setEnd(index.end);

      if (index.stations != null) {
        for (BufrCdmIndexProto.Station s : index.stations) {
          indexBuilder.addStations(s);
        }
      }

      // write it
      BufrCdmIndexProto.BufrIndex indexOut = indexBuilder.build();
      byte[] b = indexOut.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      log.debug("  write BufrCdmIndexProto= {} bytes", b.length);

      //System.out.printf("  write BufrCdmIndexProto= %d bytes", b.length);
      //showProtoRoot(rootf);

      return true;

    } finally {
      log.debug("  file size =  %d bytes", raf.length());
      raf.close();
    }
  }

  private BufrCdmIndexProto.Station buildStation(BufrConfig.BufrStation s) {
    BufrCdmIndexProto.Station.Builder builder = BufrCdmIndexProto.Station.newBuilder();

    builder.setId(s.getName());
    builder.setCount(s.count);
    if (s.getWmoId() != null)
      builder.setWmoId(s.getWmoId());
    if (s.getDescription() != null)
      builder.setDesc(s.getDescription());
    builder.setLat( s.getLatitude());
    builder.setLon(s.getLongitude());
    builder.setAlt(s.getAltitude());

    return builder.build();
  }

  private static BufrCdmIndexProto.Field buildField(BufrField fld) {
    BufrCdmIndexProto.Field.Builder fldBuilder = BufrCdmIndexProto.Field.newBuilder();

    fldBuilder.setFxy(fld.getFxy());

    if (fld.getName() != null)
      fldBuilder.setName(fld.getName());

    if (fld.getDesc() != null)
      fldBuilder.setDesc(fld.getDesc());

    if (fld.getUnits() != null)
      fldBuilder.setUnits(fld.getUnits());

    if (fld.getChildren() != null) {
      for (BufrField child : fld.getChildren())
        fldBuilder.addFlds(buildField(child));
    }

    if (fld.getAction() != null && fld.getAction() != BufrCdmIndexProto.FldAction.none)
      fldBuilder.setAction( fld.getAction());

    if (fld.getType() != null)
      fldBuilder.setType(fld.getType());

    if (fld.isSeq()) {
      fldBuilder.setMin( fld.getMin());
      fldBuilder.setMax(fld.getMax());
    }

    return fldBuilder.build();
  }

  //////////////////////////////////////////////////////////////////

  public String idxFilename;
  public String bufrFilename;
  public BufrCdmIndexProto.Field root;
  public List<BufrCdmIndexProto.Station> stations;
  public long start, end;

  protected boolean readIndex(RandomAccessFile raf) throws IOException {
    this.idxFilename = raf.getLocation();

    try {
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      //// header message
      if (!NcStream.readAndTest(raf, MAGIC_START.getBytes())) {
        log.error("BufrCdmIndex {}: invalid index", raf.getLocation());
        return false;
      }

      int indexVersion = raf.readInt();
      boolean versionOk = (indexVersion == version);
      if (!versionOk) {
        log.warn("BufrCdmIndex {}: index found version={}, want version= {}", raf.getLocation(), indexVersion, version);
        return false;
      }

      int size = NcStream.readVInt(raf);
      if ((size < 0) || (size > 100 * 1000 * 1000)) {
        log.warn("BufrCdmIndex {}: invalid or empty index ", raf.getLocation());
        return false;
      }

      byte[] m = new byte[size];
      raf.readFully(m);

      BufrCdmIndexProto.BufrIndex proto =  BufrCdmIndexProto.BufrIndex.parseFrom(m);
      bufrFilename = proto.getFilename();
      root = proto.getRoot();
      stations = proto.getStationsList();
      start = proto.getStart();
      end = proto.getEnd();

      //showProtoRoot(root);

    } catch (Throwable t) {
      log.error("Error reading index " + raf.getLocation(), t);
      return false;
    }

    return true;
  }

  static void showProtoRoot(BufrCdmIndexProto.Field fld) {
    String act = fld.hasAction() ? fld.getAction().toString() : "-";
    System.out.printf("%10s %s%n", act, fld.getName());
    for (BufrCdmIndexProto.Field child : fld.getFldsList())
      showProtoRoot(child);

  }

  public void showIndex(Formatter f) {
    f.format("BufrCdmIndex %n");
    f.format("  idxFilename=%s%n", idxFilename);
    f.format("  bufrFilename=%s%n", bufrFilename);
    f.format("  start=%s%n", CalendarDate.of(start));
    f.format("  end=%s%n", CalendarDate.of(end));
  }

}