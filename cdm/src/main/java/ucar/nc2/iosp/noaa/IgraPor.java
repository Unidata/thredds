package ucar.nc2.iosp.noaa;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.ncml.NcmlConstructor;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nomads IGRA files.
 * Can open all data by opening "igra-stations.txt", with data files in subdir "igra-por".
 * Can open single station data by opening <stationId>.dat with igra-stations.txt in same or parent directory.
 *
 * @see "http://www.ncdc.noaa.gov/oa/climate/igra/"
 * @see "ftp://ftp.ncdc.noaa.gov/pub/data/igra"
 * 
 * @author caron
 * @since 3/3/11
 */
public class IgraPor extends AbstractIOServiceProvider {
  private static final String dataPatternRegexp =
          "(\\d{2})([ \\-\\d]{6})(.)([ \\-\\d]{5})(.)([ \\-\\d]{5})(.)([ \\-\\d]{5})([ \\-\\d]{5})([ \\-\\d]{5})$";

  private static final String dataHeaderPatternRegexp =
          "#(\\d{5})(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{4})([ \\d]{4})$";

  private static final String stnPatternRegexp =
          "([A-Z]{2})  (\\d{5})  (.{35}) ([ \\.\\-\\d]{6}) ([ \\.\\-\\d]{7}) ([ \\-\\d]{4}) (.)(.)(.)  ([ \\d]{4}) ([ \\d]{4})$";

  private static final Pattern dataPattern = Pattern.compile(dataPatternRegexp);
  private static final Pattern dataHeaderPattern = Pattern.compile(dataHeaderPatternRegexp);
  private static final Pattern stnPattern = Pattern.compile(stnPatternRegexp);

  private static final String STNID = "stnid";

  private static final String STN_FILE = "igra-stations.txt";
  private static final String DAT_EXT = ".dat";
  private static final String DAT_DIR = "igra-por";
  private static final String IDX_EXT = ".ncx";
  private static final String MAGIC_START_IDX = "IgraPorIndex";
  private static final int version = 1;

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    String location = raf.getLocation();
    File file = new File(location);

    int pos = location.lastIndexOf(".");
    if (pos <= 0) return false;
    String base = location.substring(0, pos);
    String ext = location.substring(pos);

    // must be data file or station file or index file
    if (!ext.equals(DAT_EXT) && !ext.equals(IDX_EXT) && !file.getName().equals(STN_FILE))
      return false;

    if (ext.equals(IDX_EXT)) {
      // data, stn files must be in the same directory
      File datFile = new File(base + DAT_EXT);
      if (!datFile.exists())
        return false;
      File stnFile = getStnFile(location);
      if (!stnFile.exists())
        return false;

      raf.seek(0);
      byte[] b = new byte[MAGIC_START_IDX.length()];
      raf.read(b);
      String test = new String(b, "UTF-8");
      return test.equals(MAGIC_START_IDX);

    } else if (ext.equals(DAT_EXT)) {
      File stnFile = getStnFile(location);
      if (stnFile == null)
          return false;
      return isValidFile(raf, dataHeaderPattern);

    } else {
      // data directory must exist
      File dataDir = new File(file.getParentFile(), DAT_DIR);
      return dataDir.exists() && dataDir.isDirectory() && isValidFile(raf, stnPattern);
    }
  }

  // stn file must be in the same directory or one up
  private File getStnFile(String location) {
    File file = new File(location);
    File stnFile = new File(file.getParentFile(), STN_FILE);
    if (!stnFile.exists()) {
      if (file.getParentFile() == null) return null;
      stnFile = new File(file.getParentFile().getParentFile(), STN_FILE);
      if (!stnFile.exists()) return null;
    }
    return stnFile;
  }

  private boolean isValidFile(RandomAccessFile raf, Pattern p) throws IOException {
    raf.seek(0);
    String line;
    while (true) {
      line = raf.readLine();
      if (line == null) break;
      if (line.trim().length() == 0) continue;
      Matcher matcher = p.matcher(line);
      return matcher.matches();
    }
    return false;
  }

  @Override
  public String getFileTypeId() {
    return "IGRA-POR";
  }

  @Override
  public String getFileTypeDescription() {
    return "Integrated Global Radiosonde Archive";
  }

  @Override
  public String getFileTypeVersion() {
    return Integer.toString(version);
  }

  /////////////////////////////////////////////////////////////////////////
  private RandomAccessFile stnRaf, dataRaf;
  private File dataDir;
  //private HashMap<Long, StationIndex> map = new HashMap<Long, StationIndex>(10000);
  private int stn_fldno;
  private StructureDataRegexp.Vinfo stnVinfo, seriesVinfo, profileVinfo;
  private String stationId; // if a DAT file

  @Override
  public void open(RandomAccessFile raff, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    String location = raff.getLocation();
    int pos = location.lastIndexOf(".");
    String ext = location.substring(pos);

    File file = new File(location);
    File stnFile = getStnFile(location);

    if (ext.equals(IDX_EXT)) {
      stnRaf = new RandomAccessFile(stnFile.getPath(), "r");

    } else if (ext.equals(DAT_EXT)) {
      stnRaf = new RandomAccessFile(stnFile.getPath(), "r");
      dataRaf = raff;

      //extract the station id
      String name = file.getName();
      stationId = name.substring(0, name.length() - DAT_EXT.length());

    } else { // pointed to the station file
      stnRaf = raff;
      dataDir = new File(file.getParentFile(), DAT_DIR);
    }

    NcmlConstructor ncmlc = new NcmlConstructor();
    if (!ncmlc.populateFromResource("resources/nj22/iosp/igra-por.ncml", ncfile)) {
      throw new IllegalStateException(ncmlc.getErrlog().toString());
    }
    ncfile.finish();

    //dataVinfo = setVinfo(dataRaf, ncfile, dataPattern, "all_data");
    stnVinfo = setVinfo(stnRaf, ncfile, stnPattern, "station");
    seriesVinfo = setVinfo(stnRaf, ncfile, dataHeaderPattern, "station.time_series");
    profileVinfo = setVinfo(stnRaf, ncfile, dataPattern, "station.time_series.levels");

    StructureMembers.Member m = stnVinfo.sm.findMember(STNID);
    StructureDataRegexp.VinfoField f = (StructureDataRegexp.VinfoField) m.getDataObject();
    stn_fldno = f.fldno;

    /* make index file if needed
    File idxFile = new File(base + IDX_EXT);
    if (!idxFile.exists())
      makeIndex(stnVinfo, dataVinfo, idxFile);
    else
      readIndex(idxFile.getPath());  */
  }

  private StructureDataRegexp.Vinfo setVinfo(RandomAccessFile raff, NetcdfFile ncfile, Pattern p, String seqName) {
    Sequence seq = (Sequence) ncfile.findVariable(seqName);
    StructureMembers sm = seq.makeStructureMembers();
    StructureDataRegexp.Vinfo result = new StructureDataRegexp.Vinfo(raff, sm, p);
    seq.setSPobject(result);

    int fldno = 1;
    for (StructureMembers.Member m : sm.getMembers()) {
      StructureDataRegexp.VinfoField vf = new StructureDataRegexp.VinfoField(fldno++);
      Variable v = seq.findVariable(m.getName());
      Attribute att = v.findAttribute("iosp_scale");
      if (att != null) {
        vf.hasScale = true;
        vf.scale = att.getNumericValue().floatValue();
        //v.remove(att);
      }
      m.setDataObject(vf);
    }

    return result;
  }

  public void close() throws java.io.IOException {
    if (stnRaf != null) stnRaf.close();
    if (dataRaf != null) dataRaf.close();
    stnRaf = null;
    dataRaf = null;
  }

  ////////////////////////////////////////////////////////////////////

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    StructureDataRegexp.Vinfo vinfo = (StructureDataRegexp.Vinfo) v2.getSPobject();
    if (stationId != null)
      return new ArraySequence(vinfo.sm, new SingleStationSeqIter(vinfo), vinfo.nelems);
    else
      return new ArraySequence(vinfo.sm, new StationSeqIter(vinfo), vinfo.nelems);
  }

  @Override
  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    StructureDataRegexp.Vinfo vinfo = (StructureDataRegexp.Vinfo) s.getSPobject();
    if (stationId != null)
      return new SingleStationSeqIter(vinfo);
    else
      return new StationSeqIter(vinfo);
  }

  // when theres only one station
  private class SingleStationSeqIter implements StructureDataIterator {
    private StructureDataRegexp.Vinfo vinfo;
    private int recno = 0;

    SingleStationSeqIter(StructureDataRegexp.Vinfo vinfo) throws IOException {
      this.vinfo = vinfo;
      vinfo.rafile.seek(0);
    }

    @Override
    public StructureDataIterator reset() {
      recno = 0;
      return this;
    }

    @Override
    public boolean hasNext() throws IOException {
      return recno == 0;
    }

    @Override
    public StructureData next() throws IOException {
      Matcher matcher;
      while (true) {
        String line = vinfo.rafile.readLine();
        if (line == null) return null;
        if (line.startsWith("#")) continue;
        if (line.trim().length() == 0) continue;
        //System.out.printf("line %s%n", line);
        matcher = vinfo.p.matcher(line);
        if (matcher.matches()) {
          String stnid = matcher.group(stn_fldno).trim();
          if (stnid.equals(stationId)) break;
        }
      }
      recno++;
      return new StationData(vinfo.sm, matcher);
    }

    @Override
    public void setBufferSize(int bytes) {
    }

    @Override
    public int getCurrentRecno() {
      return recno - 1;
    }
  }

    // sequence of stations
  private class StationSeqIter implements StructureDataIterator {
    private StructureDataRegexp.Vinfo vinfo;
    private long totalBytes;
    private int recno;
    private StructureData curr;

    StationSeqIter(StructureDataRegexp.Vinfo vinfo) throws IOException {
      this.vinfo = vinfo;
      totalBytes = (int) vinfo.rafile.length();
      vinfo.rafile.seek(0);
    }

    @Override
    public StructureDataIterator reset() {
      recno = 0;

      try {
        vinfo.rafile.seek(0);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    @Override
    public boolean hasNext() throws IOException {
      boolean more = (vinfo.rafile.getFilePointer() < totalBytes); // && (recno < 10);
      if (!more) {
        vinfo.nelems = recno;
        //System.out.printf("nelems=%d%n", recno);
        return false;
      }
      curr = reallyNext();
      more = (curr != null);
      if (!more) {
        vinfo.nelems = recno;
        //System.out.printf("nelems=%d%n", recno);
        return false;
      }
      return more;
    }

    @Override
    public StructureData next() throws IOException {
      return curr;
    }

    private StructureData reallyNext() throws IOException {
      Matcher matcher;
      while (true) {
        String line = vinfo.rafile.readLine();
        if (line == null) return null;
        if (line.startsWith("#")) continue;
        if (line.trim().length() == 0) continue;
        //System.out.printf("line %s%n", line);
        matcher = vinfo.p.matcher(line);
        if (matcher.matches())
          break;
        System.out.printf("FAIL %s%n", line);
      }
      recno++;
      return new StationData(vinfo.sm, matcher);
    }

    @Override
    public void setBufferSize(int bytes) {
    }

    @Override
    public int getCurrentRecno() {
      return recno - 1;
    }
  }

  private class StationData extends StructureDataRegexp {
    StructureMembers members;
    Matcher matcher;          // matcher on the station ascii

    StationData(StructureMembers members, Matcher matcher) {
      super(members, matcher);
      this.members = members;
      this.matcher = matcher;
    }

    @Override
    // nested array sequence must be the stn_data
    public ArraySequence getArraySequence(StructureMembers.Member m) {
      String stnid = matcher.group(stn_fldno).trim();
      return new ArraySequence(seriesVinfo.sm, new TimeSeriesIter(stnid), -1);
    }
  }

  //////////////////////////////////////////////////////
  // sequence of time series for one station
  private class TimeSeriesIter implements StructureDataIterator {
    private int countRead = 0;
    private long totalBytes;
    private File file;
    private RandomAccessFile timeSeriesRaf = null;
    private boolean exists;

    TimeSeriesIter(String stnid) {
      if (dataRaf != null)
        exists = true;
      else {
        this.file = new File(dataDir, stnid+DAT_EXT);
        exists = file.exists();
      }
    }

    private void init() {
      if (!exists) return;
      try {
        if (dataRaf != null)
          this.timeSeriesRaf = dataRaf; // single station case - data file already open
        else
          this.timeSeriesRaf = new RandomAccessFile( file.getPath(), "r");// LOOK check exists LOOK who closes?

        totalBytes = timeSeriesRaf.length();
        timeSeriesRaf.seek(0);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public StructureDataIterator reset() {
      if (!exists) return this;

      if (timeSeriesRaf == null) init();

      countRead = 0;
      try {
        timeSeriesRaf.seek(0);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    @Override
    public boolean hasNext() throws IOException {
      if (!exists) return false;
      if (timeSeriesRaf == null) init();
      return (timeSeriesRaf.getFilePointer() < totalBytes); // && (recno < 10);   LOOK not perfect, eg trailing blanks
    }

    @Override
    public StructureData next() throws IOException {
      Matcher matcher;
      String line;
      while (true) {
        line = timeSeriesRaf.readLine();
        if (line == null) return null;  // only on EOF
        if (line.trim().length() == 0) continue;
        matcher = seriesVinfo.p.matcher(line);
        if (matcher.matches())
          break;
        System.out.printf("FAIL TimeSeriesIter <%s>%n", line);
      }
      countRead++;
      return new TimeSeriesData(matcher);
    }

    @Override
    public void setBufferSize(int bytes) {
    }

    @Override
    public int getCurrentRecno() {
      return countRead - 1;
    }

    private class TimeSeriesData extends StructureDataRegexp {
      Matcher matcher;          // matcher on the station ascii
      List<String> lines = new ArrayList<String>(30);

      TimeSeriesData(Matcher matcher) throws IOException {
        super(seriesVinfo.sm, matcher);
        this.matcher = matcher;

        String line;
        long pos;
        while (true) {
          pos = timeSeriesRaf.getFilePointer();
          line = timeSeriesRaf.readLine();
          if (line == null) break;
          if (line.trim().length() == 0) continue;
          matcher = profileVinfo.p.matcher(line);
          if (matcher.matches())
            lines.add(line);
          else  {
            timeSeriesRaf.seek(pos); // put the line back
            break;
          }
        }
      }

      @Override
      // nested array sequence must be the stn_data
      public ArraySequence getArraySequence(StructureMembers.Member m) {
        return new ArraySequence(profileVinfo.sm, new ProfileIter(), -1);
      }

      //////////////////////////////////////////////////////
      // sequence of levels for one profile = station-timeSeries
      private class ProfileIter implements StructureDataIterator {
        private int countRead;

        ProfileIter() {
          countRead = 0;
        }

        @Override
        public StructureDataIterator reset() {
          countRead = 0;
          return this;
        }

        @Override
        public boolean hasNext() throws IOException {
          return countRead < lines.size();
        }

        @Override
        public StructureData next() throws IOException {
          if (!hasNext()) return null;
          Matcher matcher = profileVinfo.p.matcher(lines.get(countRead));
          StructureData sd;
          if (matcher.matches())
            sd = new StructureDataRegexp(profileVinfo.sm, matcher);
          else
            throw new IllegalStateException("line = "+lines.get(countRead)+ "pattern = "+profileVinfo.p );
          countRead++;
          return sd;
        }

        @Override
        public void setBufferSize(int bytes) {
        }

        @Override
        public int getCurrentRecno() {
          return countRead - 1;
        }

      }
    }

  }


  ///////////////////////////////////////////
  /*
  private void readIndex(String indexFilename) throws IOException {
    FileInputStream fin = new FileInputStream(indexFilename);

    if (!NcStream.readAndTest(fin, MAGIC_START_IDX.getBytes("UTF-8")))
      throw new IllegalStateException("bad index file");
    int version = fin.read();
    if (version != 1)
      throw new IllegalStateException("Bad version = " + version);

    int count = NcStream.readVInt(fin);

    for (int i = 0; i < count; i++) {
      int size = NcStream.readVInt(fin);
      byte[] pb = new byte[size];
      NcStream.readFully(fin, pb);
      StationIndex si = decodeStationIndex(pb);
      map.put(si.stnId, si);
    }
    fin.close();

    System.out.println(" read index map size=" + map.values().size());
  }

  private void makeIndex(StructureDataRegexp.Vinfo stnInfo, StructureDataRegexp.Vinfo dataInfo, File indexFile) throws IOException {
    // get map of Stations
    StructureMembers.Member m = stnInfo.sm.findMember(STNID);
    StructureDataRegexp.VinfoField f = (StructureDataRegexp.VinfoField) m.getDataObject();
    int stnCount = 0;

    // read through entire file LOOK: could use SeqIter
    stnInfo.rafile.seek(0);
    while (true) {
      long stnPos = stnInfo.rafile.getFilePointer();
      String line = stnInfo.rafile.readLine();
      if (line == null) break;

      Matcher matcher = stnInfo.p.matcher(line);
      if (!matcher.matches()) {
        System.out.printf("FAIL %s%n", line);
        continue;
      }
      String svalue = matcher.group(f.fldno);
      Long id = Long.parseLong(svalue.trim());

      StationIndex s = new StationIndex();
      s.stnId = id;
      s.stnPos = stnPos;
      map.put(id, s);
      stnCount++;
    }

    // assumes that the stn data is in order by stnId
    m = dataInfo.sm.findMember(STNID);
    f = (StructureDataRegexp.VinfoField) m.getDataObject();
    StationIndex currStn = null;
    int totalCount = 0;

    // read through entire data file
    dataInfo.rafile.seek(0);
    while (true) {
      long dataPos = dataInfo.rafile.getFilePointer();
      String line = dataInfo.rafile.readLine();
      if (line == null) break;

      Matcher matcher = dataInfo.p.matcher(line);
      if (!matcher.matches()) {
        System.out.printf("FAIL %s%n", line);
        continue;
      }

      String svalue = matcher.group(f.fldno).trim();
      Long id = Long.parseLong(svalue);

      if ((currStn == null) || (currStn.stnId != id)) {
        StationIndex s = map.get(id);
        if (s == null)
          System.out.printf("Cant find %d%n", id);
        else if (s.dataCount != 0)
          System.out.printf("Not in order %d at pos %d %n", id, dataPos);
        else {
          s.dataPos = dataPos;
          totalCount++;
        }
        currStn = s;
      }
      currStn.dataCount++;
    }
    //System.out.printf("ok stns=%s data=%d%n", stnCount, totalCount);

    //////////////////////////////
    // write the index file
    FileOutputStream fout = new FileOutputStream(indexFile); // LOOK need DiskCache for non-writeable directories
    long size = 0;

    //// header message
    fout.write(MAGIC_START_IDX.getBytes("UTF-8"));
    fout.write(version);
    size += NcStream.writeVInt(fout, stnCount);

    /* byte[] pb = encodeStationListProto( map.values());
   size += NcStream.writeVInt(fout, pb.length);
   size += pb.length;
   fout.write(pb);

    for (StationIndex s : map.values()) {
      byte[] pb = s.encodeStationProto();
      size += NcStream.writeVInt(fout, pb.length);
      size += pb.length;
      fout.write(pb);
    }
    fout.close();

    //System.out.println(" index size=" + size);
  }

  private StationIndex decodeStationIndex(byte[] data) throws InvalidProtocolBufferException {
    ucar.nc2.iosp.noaa.GhcnmProto.StationIndex proto = GhcnmProto.StationIndex.parseFrom(data);
    return new StationIndex(proto);
  }

  private class StationIndex {
    long stnId;
    long stnPos; // file pos in inv file
    long dataPos; // file pos of first data line in the data file
    int dataCount; // number of data records

    StationIndex() {
    }

    StationIndex(ucar.nc2.iosp.noaa.GhcnmProto.StationIndex proto) {
      this.stnId = proto.getStnid();
      this.stnPos = proto.getStnPos();
      this.dataPos = proto.getDataPos();
      this.dataCount = proto.getDataCount();
    }

    private byte[] encodeStationProto() {
      GhcnmProto.StationIndex.Builder builder = GhcnmProto.StationIndex.newBuilder();
      builder.setStnid(stnId);
      builder.setStnPos(stnPos);
      builder.setDataPos(dataPos);
      builder.setDataCount(dataCount);
      ucar.nc2.iosp.noaa.GhcnmProto.StationIndex proto = builder.build();
      return proto.toByteArray();
    }
  } */

}

