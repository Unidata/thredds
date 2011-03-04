package ucar.nc2.iosp.noaa;

import com.google.protobuf.InvalidProtocolBufferException;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.ncml.NcmlConstructor;
import ucar.nc2.stream.NcStream;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.URLnaming;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describe
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
  private static final String IDX_EXT = ".ncx";
  private static final String MAGIC_START_IDX = "IgraPorIndex";
  private static final int version = 1;

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    String dataFile = raf.getLocation();
    int pos = dataFile.lastIndexOf(".");
    if (pos <= 0) return false;
    String base = dataFile.substring(0, pos);
    String ext = dataFile.substring(pos);

    // must be data file or station or index file
    if (!ext.equals(DAT_EXT) && !ext.equals(IDX_EXT))
      return false;

    if (ext.equals(IDX_EXT)) {
      // data, stn files must be in the same directory
      File datFile = new File(base + DAT_EXT);
      if (!datFile.exists())
        return false;
      File stnFile = getStnFile(base);
      if (!stnFile.exists())
        return false;

      raf.seek(0);
      byte[] b = new byte[MAGIC_START_IDX.length()];
      raf.read(b);
      String test = new String(b, "UTF-8");
      return test.equals(MAGIC_START_IDX);

    } else if (ext.equals(DAT_EXT)) {
      // stn file must be in the same directory
      File stnFile = getStnFile(base);
      if (!stnFile.exists())
        return false;
      return isValidFile(raf, dataHeaderPattern);

    } else {
      // dat file must be in the same directory
      File stnFile = new File(base + DAT_EXT);
      return stnFile.exists() && isValidFile(raf, stnPattern);
    }
  }

  private File getStnFile(String location) {
    File f = new File(location);
    File p = f.getParentFile();
    return new File(p, STN_FILE);
  }

  private boolean isValidFile(RandomAccessFile raf, Pattern p) throws IOException {
    raf.seek(0);
    String line;
    while (true) {
      line = raf.readLine();
      if (line == null) break;
      if (line.startsWith("#")) continue;
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
  private HashMap<Long, StationIndex> map = new HashMap<Long, StationIndex>(10000);
  private int stn_fldno;
  private Vinfo dataVinfo, stnVinfo;

  @Override
  public void open(RandomAccessFile raff, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    String dataFile = raff.getLocation();
    int pos = dataFile.lastIndexOf(".");
    String base = dataFile.substring(0, pos);
    String ext = dataFile.substring(pos);

    if (ext.equals(IDX_EXT)) {
      dataRaf = new RandomAccessFile(base + DAT_EXT, "r");
      stnRaf = new RandomAccessFile(getStnFile(base).getPath(), "r");

    } else if (ext.equals(DAT_EXT)) {
      dataRaf = raff;
      stnRaf = new RandomAccessFile(getStnFile(base).getPath(), "r");

    } else {
      stnRaf = raff;
      dataRaf = new RandomAccessFile(base + DAT_EXT, "r");
    }

    NcmlConstructor ncmlc = new NcmlConstructor();
    if (!ncmlc.populateFromResource("resources/nj22/iosp/igra-por.ncml", ncfile)) {
      throw new IllegalStateException(ncmlc.getErrlog().toString());
    }
    ncfile.finish();

    dataVinfo = setVinfo(dataRaf, ncfile, dataPattern, "all_data");
    stnVinfo = setVinfo(stnRaf, ncfile, stnPattern, "station");

    StructureMembers.Member m = stnVinfo.sm.findMember(STNID);
    VinfoField f = (VinfoField) m.getDataObject();
    stn_fldno = f.fldno;

    // make index file if needed
    File idxFile = new File(base + IDX_EXT);
    if (!idxFile.exists())
      makeIndex(stnVinfo, dataVinfo, idxFile);
    else
      readIndex(idxFile.getPath());
  }

  private Vinfo setVinfo(RandomAccessFile raff, NetcdfFile ncfile, Pattern p, String seqName) {
    Sequence seq = (Sequence) ncfile.findVariable(seqName);
    StructureMembers sm = seq.makeStructureMembers();
    Vinfo result = new Vinfo(raff, sm, p);
    seq.setSPobject(result);

    int fldno = 1;
    for (StructureMembers.Member m : sm.getMembers()) {
      VinfoField vf = new VinfoField(fldno++);
      /* Variable v = seq.findVariable(m.getName());
     Attribute att = v.findAttribute("scale_factor");
     if (att != null) {
       vf.hasScale = true;
       vf.scale = att.getNumericValue().floatValue();
       v.remove(att);
     } */
      m.setDataObject(vf);
    }

    return result;
  }

  public void close() throws java.io.IOException {
    stnRaf.close();
    dataRaf.close();
  }

  class Vinfo {
    RandomAccessFile rafile;
    StructureMembers sm;
    Pattern p;
    int nelems = -1;

    private Vinfo(RandomAccessFile raff, StructureMembers sm, Pattern p) {
      this.sm = sm;
      this.rafile = raff;
      this.p = p;
    }
  }

  class VinfoField {
    int fldno;
    int stride = 4;
    //float scale;
    //boolean hasScale;

    private VinfoField(int fldno) {
      this.fldno = fldno;
    }
  }

  ////////////////////////////////////////////////////////////////////

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Vinfo vinfo = (Vinfo) v2.getSPobject();
    return new ArraySequence(vinfo.sm, new SeqIter(vinfo), vinfo.nelems);
  }

  @Override
  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    Vinfo vinfo = (Vinfo) s.getSPobject();
    return new SeqIter(vinfo);
  }

  private class SeqIter implements StructureDataIterator {
    private Vinfo vinfo;
    private long bytesRead;
    private long totalBytes;
    private int recno;
    private StructureData curr;

    SeqIter(Vinfo vinfo) throws IOException {
      this.vinfo = vinfo;
      totalBytes = (int) vinfo.rafile.length();
      vinfo.rafile.seek(0);
    }

    @Override
    public StructureDataIterator reset() {
      bytesRead = 0;
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
      boolean more = (bytesRead < totalBytes); // && (recno < 10);
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
        matcher = vinfo.p.matcher(line);
        if (matcher.matches())
          break;
        System.out.printf("FAIL %s%n", line);
      }
      //System.out.printf("%s%n", line);
      bytesRead = vinfo.rafile.getFilePointer();
      recno++;
      return new StructureDataRegexpGhcnm(vinfo.sm, matcher);
    }

    @Override
    public void setBufferSize(int bytes) {
    }

    @Override
    public int getCurrentRecno() {
      return recno - 1;
    }
  }

  //////////////////////////////////////////////////////

  private class StructureDataRegexpGhcnm extends StructureDataRegexp {
    StructureMembers members;
    Matcher matcher;          // matcher on the station ascii

    StructureDataRegexpGhcnm(StructureMembers members, Matcher matcher) {
      super(members, matcher);
      this.members = members;
      this.matcher = matcher;
    }

    @Override
    // nested array sequence must be the stn_data
    public ArraySequence getArraySequence(StructureMembers.Member m) {
      String svalue = matcher.group(stn_fldno).trim();
      Long stnId = Long.parseLong(svalue); // extract the station id
      StationIndex si = map.get(stnId); // find its index
      return new ArraySequence(dataVinfo.sm, new StnDataIter(dataVinfo.sm, si), -1);
    }
  }

  private class StnDataIter implements StructureDataIterator {
    private StructureMembers sm;
    private int countRead;
    private StationIndex stationIndex;

    StnDataIter(StructureMembers sm, StationIndex stationIndex) {
      this.sm = sm;
      this.stationIndex = stationIndex;
      reset();
    }

    @Override
    public StructureDataIterator reset() {
      countRead = 0;
      try {
        dataRaf.seek(stationIndex.dataPos);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    @Override
    public boolean hasNext() throws IOException {
      return (countRead < stationIndex.dataCount);
    }

    @Override
    public StructureData next() throws IOException {
      Matcher matcher;
      String line;
      while (true) {
        line = dataRaf.readLine();
        if (line == null) return null;
        if (line.startsWith("#")) continue;
        if (line.trim().length() == 0) continue;
        matcher = dataPattern.matcher(line);
        if (matcher.matches())
          break;
      }
      //System.out.printf("%s%n", line);
      countRead++;
      return new StructureDataRegexp(sm, matcher);
    }

    @Override
    public void setBufferSize(int bytes) {
    }

    @Override
    public int getCurrentRecno() {
      return countRead - 1;
    }
  }

  ///////////////////////////////////////////

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

  private void makeIndex(Vinfo stnInfo, Vinfo dataInfo, File indexFile) throws IOException {
    // get map of Stations
    StructureMembers.Member m = stnInfo.sm.findMember(STNID);
    VinfoField f = (VinfoField) m.getDataObject();
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
    f = (VinfoField) m.getDataObject();
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
   fout.write(pb); */

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
  }

}

