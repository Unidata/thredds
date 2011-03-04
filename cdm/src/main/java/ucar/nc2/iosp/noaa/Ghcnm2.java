package ucar.nc2.iosp.noaa;

import com.google.protobuf.InvalidProtocolBufferException;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.ncml.NcmlConstructor;
import ucar.nc2.stream.NcStream;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NOMADS Ghcnm2
 *
 * @author caron
 * @since Feb 26, 2011
 */

  /*
  ftp://ftp.ncdc.noaa.gov/pub/data/ghcn/v3/README

    2.1 METADATA

       The metadata has been carried over from GHCN-Monthly v2.  This would
       include basic geographical station information such as latitude,
       longitude, elevation, station name, etc., and also extended metadata
       information, such as surrounding vegetation, etc.

    2.1.1 METADATA FORMAT

       Variable          Columns      Type
       --------          -------      ----

       ID                 1-11        Integer
       LATITUDE          13-20        Real
       LONGITUDE         22-30        Real
       STNELEV           32-37        Real
       NAME              39-68        Character
       GRELEV            70-73        Integer
       POPCLS            74-74        Character
       POPSIZ            76-79        Integer
       TOPO              80-81        Character
       STVEG             82-83        Character
       STLOC             84-85        Character
       OCNDIS            86-87        Integer
       AIRSTN            88-88        Character
       TOWNDIS           89-90        Integer
       GRVEG             91-106       Character
       POPCSS            107-107      Character

       Variable Definitions:

       ID: 11 digit identifier, digits 1-3=Country Code, digits 4-8 represent
           the WMO stnId if the station is a WMO station.  It is a WMO station if
           digits 9-11="000".

       LATITUDE: latitude of station in decimal degrees

       LONGITUDE: longitude of station in decimal degrees

       STELEV: is the station elevation in meters. -999.0 = missing.

       NAME: station name

       GRELEV: station elevation in meters estimated from gridded digital
               terrain data

       POPCLS: population class
               (U=Urban (>50,000 persons);
               (S=Suburban (>=10,000 and <= 50,000 persons);
               (R=Rural (<10,000 persons)
               City and town boundaries are determined from location of station
               on Operational Navigation Charts with a scale of 1 to 1,000,000.
               For cities > 100,000 persons, population data were provided by
               the United Nations Demographic Yearbook. For smaller cities and
               towns several atlases were uses to determine population.

       POPSIZ: the population of the city or town the station is location in
               (expressed in thousands of persons).

       TOPO: type of topography in the environment surrounding the station,
             (Flat-FL,Hilly-HI,Mountain Top-MT,Mountainous Valley-MV).

       STVEG: type of vegetation in environment of station if station is Rural
              and when it is indicated on the Operational Navigation Chart
              (Desert-DE,Forested-FO,Ice-IC,Marsh-MA).

       STLOC: indicates whether station is near lake or ocean (<= 30 km of
              ocean-CO, adjacent to a lake at least 25 square km-LA).

       OCNDIS: distance to nearest ocean/lake from station (km).

       AIRSTN: airport station indicator (A=station at an airport).

       TOWNDIS: distance from airport to center of associated city or town (km).

       GRVEG: vegetation type at nearest 0.5 deg x 0.5 deg gridded data point of
              vegetation dataset (44 total classifications).

              BOGS, BOG WOODS
              COASTAL EDGES
              COLD IRRIGATED
              COOL CONIFER
              COOL CROPS
              COOL DESERT
              COOL FIELD/WOODS
              COOL FOR./FIELD
              COOL GRASS/SHRUB
              COOL IRRIGATED
              COOL MIXED
              EQ. EVERGREEN
              E. SOUTH. TAIGA
              HEATHS, MOORS
              HIGHLAND SHRUB
              HOT DESERT
              ICE
              LOW SCRUB
              MAIN TAIGA
              MARSH, SWAMP
              MED. GRAZING
              NORTH. TAIGA
              PADDYLANDS
              POLAR DESERT
              SAND DESERT
              SEMIARID WOODS
              SIBERIAN PARKS
              SOUTH. TAIGA
              SUCCULENT THORNS
              TROPICAL DRY FOR
              TROP. MONTANE
              TROP. SAVANNA
              TROP. SEASONAL
              TUNDRA
              WARM CONIFER
              WARM CROPS
              WARM DECIDUOUS
              WARM FIELD WOODS
              WARM FOR./FIELD
              WARM GRASS/SHRUB
              WARM IRRIGATED
              WARM MIXED
              WATER
              WOODED TUNDRA

       POPCSS: population class as determined by Satellite night lights
               (C=Urban, B=Suburban, A=Rural)

    2.2  DATA

         The data within GHCNM v3 beta for the time being consist of monthly
         average temperature, for the 7280 stations contained within GHCNM v2.
         Several new sources have been added to v3 beta, and a new "3 flag"
         format has been introduced, similar to that used within the Global
         Historical Climatology Network-Daily (GHCND).

    2.2.1 DATA FORMAT

          Variable          Columns      Type
          --------          -------      ----

          ID                 1-11        Integer
          YEAR              12-15        Integer
          ELEMENT           16-19        Character
          VALUE1            20-24        Integer
          DMFLAG1           25-25        Character
          QCFLAG1           26-26        Character
          DSFLAG1           27-27        Character
            .                 .             .
            .                 .             .
            .                 .             .
          VALUE12          108-112       Integer
          DMFLAG12         113-113       Character
          QCFLAG12         114-114       Character
          DSFLAG12         115-115       Character

          Variable Definitions:

          ID: 11 digit identifier, digits 1-3=Country Code, digits 4-8 represent
              the WMO stnId if the station is a WMO station.  It is a WMO station if
              digits 9-11="000".

          YEAR: 4 digit year of the station record.

          ELEMENT: element type, currently just "TAVG".

          VALUE: monthly value (MISSING=-9999)

          DMFLAG: data measurement flag, nine possible values:

                  blank = no measurement information applicable
                  a-i = number of days missing in calculation of monthly mean
                        temperature (currently only applies to the 1218 USHCN
                        V2 stations included within GHCNM)

          QCFLAG: quality control flag, seven possibilities within
                  quality controlled unadjusted (qcu) dataset, and 2
                  possibilities within the quality controlled adjusted (qca)
                  dataset.

                  Quality Controlled Unadjusted (QCU) QC Flags:

                  BLANK = no failure of quality control check or could not be
                          evaluated.

                  D = monthly value is part of an annual series of values that
                      are exactly the same (e.fldno. duplicated) within another
                      year in the station's record.

                  K = monthly value is part of a consecutive run (e.fldno. streak)
                      of values that are identical.  The streak must be >= 4
                      months of the same value.

                  L = monthly value is isolated in time within the station
                      record, and this is defined by having no immediate non-
                      missing values 18 months on either side of the value.

                  O = monthly value that is >= 5 bi-weight standard deviations
                      from the bi-weight mean.  Bi-weight statistics are
                      calculated from a series of all non-missing values in
                      the station's record for that particular month.

                  S = monthly value has failed spatial consistency check
                      (relative to their respective climatological means to
                       concurrent z-scores at the nearest 20 neighbors located
                       withing 500 km of the target; A temperature fails if
                       (i) its z-score differs from the regional (target and
                       neighbor) mean z-score by at least 3.5 standard
                       deviations and (ii) the target's temperature anomaly
                       differs by at least 2.5 deg C from all concurrent
                       temperature anomalies at the neighbors.

                  T = monthly value has failed temporal consistency check
                      (temperatures whose anomalies differ by more than
                      4 deg C from concurent anomalies at the five nearest
                      neighboring stations whose temperature anomalies are
                      well correlated with the target (correlation > 0.7 for
                      the corresponding calendar monthly).

                  W = monthly value is duplicated from the previous month,
                      based upon regional and spatial criteria and is only
                      applied from the year 2000 to the present.

                  Quality Controlled Adjusted (QCA) QC Flags:

                  M = values with a non-blank quality control flag in the "qcu"
                      dataset are set to missing the adjusted dataset and given
                      an "M" quality control flag.

                  X = pairwise algorithm removed the value because of too many
                      inhomogeneities.


          DSFLAG: data source flag for monthly value, 18 possibilities:

                  C = Monthly Climatic Data of the World (MCDW) QC completed
                      but value is not yet published

                  G = GHCNM v2 station, that was not a v2 station that had multiple
                      time series (for the same element).

                  K = received by the UK Met Office

                  M = Final (Published) Monthly Climatic Data of the World
                     (MCDW)

                  N = Netherlands, KNMI (Royal Netherlans Meteorological
                      Institute)

                  P = CLIMAT (Data transmitted over the GTS, not yet fully
                      processed for the MCDW)

                  U = USHCN v2

                  W = World Weather Records (WWR), 9th series 1991 through 2000

             0 to 9 = For any station originating from GHCNM v2 that had
                      multiple time series for the same element, this flag
                      represents the 12th digit in the ID from GHCNM v2.
                      See section 2.2.2 for additional information.


    2.2.2 STATIONS WITH MULTIPLE TIME SERIES

          The GHCNM v2 contained several thousand stations that had multiple
          time series of monthly mean temperature data.  The 12th digit of
          each data record, indicated the time series number, and thus there
          was a potential maximum of 10 time series (e.fldno. 0 through 9).  These
          same stations in v3 beta have undergone a merge process, to reduce
          the station time series to one single series, based upon these
          original and at most 10 time series.

          A simple algorithm was applied to perform the merge.  The algorithm
          consisted of first finding the length (based upon number of non
          missing observations) for each of the time series and then
          combining all of the series into one based upon a priority scheme
          that would "write" data to the series for the longest series last.

          Therefore, if station A, had 3 time series of TAVG data, as follows:

          1900 to 1978 (79 years of data) [series 1]
          1950 to 1985 (36 years of data) [series 2]
          1990 to 2007 (18 years of data) [series 3]

          The final series would consist of:

          1900 to 1978 [series 1]
          1979 to 1985 [series 2]
          1990 to 2007 [series 3]

          The original series number in GHCNM v2, is retained in the GHCNM v3
          beta data source flag.

          One caveat to this merge process, is that in the final GHCNM v3 beta
          processing there is still a master level construction process
          performed daily, where the entire dataset is construction according
          to a source order overwrite hiearchy (section 2.3), and it is
          possible that higher order data sources may be interspersed within
          the 3 series listed above.

    2.3 DATA SOURCE HIERARCHY

        The GHCNM v3 beta is reprocessed on a daily basis, which means as a
        part of that reprocessing, the dataset is reconstructed from all
        original sources. The advantage to this process is when source
        datasets are corrected and/or updated the inclusion into GHCNM v3
        beta is seemless.  The following sources (more fully described in
        section 2.2.1) have the following overwrite precedance within the
        daily reprocessing of GHCNM v3 (e.fldno. source K overwrites source P)

        P,K,G,U,0-9,C,N,M,W
  */

/*
  dat file
          1         2         3         4         5         6         7         8         9         10        11        12
0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
101603550001878TAVG  890  1  950  1 1110  1 1610  1 1980  1 2240  1 2490  1 2680  1 2320  1 2057E   1370  1 1150  1
101603550001932TAVG 1010  1  980  1-9999    1420  1 1840  1-9999    2290  1-9999    2440  1-9999   -9999   -9999

  see testRegexp.testGhcnm()

  match (\d{11})(\d{4})TAVG([ \-\d]{5})(.)(.)(.)([ \-\d]{5})(.)(.)(.)([ \-\d]{5})(.)(.)(.)([ \-\d]{5})(.)(.)(.)([ \-\d]{5})(.)(.)(.)([ \-\d]{5})(.)(.)(.)([ \-\d]{5})(.)(.)(.)([ \-\d]{5})(.)(.)(.)([ \-\d]{5})(.)(.)(.)([ \-\d]{5})(.)(.)(.)([ \-\d]{5})(.)(.)(.)([ \-\d]{5})(.)?(.)?(.)?.*
  against 101603550001932TAVG 1010  1  980  1-9999    1420  1 1840  1-9999    2290  1-9999    2440  1-9999   -9999   -9999

 matches = true
 group 1 == 10160355000
 group 2 == 1932
 group 3 ==  1010
 group 4 ==
 group 5 ==
 group 6 == 1
 group 7 ==   980
 group 8 ==
 group 9 ==
 group 10 == 1
 group 11 == -9999
 group 12 ==
 group 13 ==
 group 14 ==
 group 15 ==  1420
 group 16 ==
 group 17 ==
 group 18 == 1
 group 19 ==  1840
 group 20 ==
 group 21 ==
 group 22 == 1
 group 23 == -9999
 group 24 ==
 group 25 ==
 group 26 ==
 group 27 ==  2290
 group 28 ==
 group 29 ==
 group 30 == 1
 group 31 == -9999
 group 32 ==
 group 33 ==
 group 34 ==
 group 35 ==  2440
 group 36 ==
 group 37 ==
 group 38 == 1
 group 39 == -9999
 group 40 ==
 group 41 ==
 group 42 ==
 group 43 == -9999
 group 44 ==
 group 45 ==
 group 46 ==
 group 47 == -9999
 group 48 == null
 group 49 == null
 group 50 == null
   */

  /*
inv file
          1         2         3         4         5         6         7         8         9         10        11        12
0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

10160475000  35.4800    8.1300  813.0 TEBESSA                         818U   67MVxxno-9A 2WARM FOR./FIELD B
10160490000  35.6000   -0.6000   90.0 ORAN/ES SENIA       ALGERIA      98U  492HIxxCO10A 6WARM CROPS      B

match (\d{11}) ([ \.\-\d]{8}) ([ \.\-\d]{9}) ([ \.\-\d]{6}) (.{30}) (.{4})(.) (.{4})(..)(..)(..)(..)(.)(..)(.{15})(.)* against 10160490000  35.6000   -0.6000   90.0 ORAN/ES SENIA       ALGERIA      98U  492HIxxCO10A 6WARM CROPS      B


 matches = true
 group 1 == 10160490000
 group 2 ==  35.6000
 group 3 ==   -0.6000
 group 4 ==   90.0
 group 5 == ORAN/ES SENIA       ALGERIA
 group 6 ==   98
 group 7 == U
 group 8 ==  492
 group 9 == HI
 group 10 == xx
 group 11 == CO
 group 12 == 10
 group 13 == A
 group 14 ==  6
 group 15 == WARM CROPS
 group 16 == B
   */

public class Ghcnm2 extends AbstractIOServiceProvider {
  private static final String dataPatternRegexp =
          "(\\d{11})(\\d{4})TAVG([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)"+
          "([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)"+
          "([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)?(.)?(.)?.*";

  private static final String stnPatternRegexp =
    "(\\d{11}) ([ \\.\\-\\d]{8}) ([ \\.\\-\\d]{9}) ([ \\.\\-\\d]{6}) (.{30}) ([ \\-\\d]{4})(.)([ \\-\\d]{5})(..)(..)(..)([ \\-\\d]{2})(.)(..)(.{16})(.).*";

  private static final Pattern dataPattern = Pattern.compile(dataPatternRegexp);
  private static final Pattern stnPattern = Pattern.compile(stnPatternRegexp);

  private static final String STNID = "stnid";

  private static final String STN_EXT = ".inv";
  private static final String DAT_EXT = ".dat";
  private static final String IDX_EXT = ".ncx";
  private static final String MAGIC_START_IDX = "GhncmIndex";
  private static final int version = 1;

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    String dataFile = raf.getLocation();
    int pos = dataFile.lastIndexOf(".");
    if (pos <= 0) return false;
    String base = dataFile.substring(0, pos);
    String ext = dataFile.substring(pos);

     // must be data file or station or index file
    if (!ext.equals(DAT_EXT) && !ext.equals(STN_EXT)&& !ext.equals(IDX_EXT))
      return false;

    if (ext.equals(IDX_EXT)) {
      // data, stn files must be in the same directory
      File datFile = new File(base+DAT_EXT);
      if (!datFile.exists())
         return false;
      File stnFile = new File(base+STN_EXT);
      if (!stnFile.exists())
        return false;

      raf.seek(0);
      byte[] b = new byte[MAGIC_START_IDX.length()];
      raf.read(b);
      String test = new String(b, "UTF-8");
      return test.equals(MAGIC_START_IDX);

    } else if (ext.equals(DAT_EXT)) {
      // stn file must be in the same directory
      File stnFile = new File(base+STN_EXT);
      return stnFile.exists() && isValidFile(raf, dataPattern);

    } else {
      // dat file must be in the same directory
      File stnFile = new File(base+DAT_EXT);
      return stnFile.exists() && isValidFile(raf, stnPattern);
     }
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
    return "GHCNM";
  }

  @Override
  public String getFileTypeDescription() {
    return "GLOBAL HISTORICAL CLIMATOLOGY NETWORK MONTHLY";
  }

  @Override
  public String getFileTypeVersion() {
    return Integer.toString(version);
  }

  /////////////////////////////////////////////////////////////////////////
  private RandomAccessFile stnRaf, dataRaf;
  private HashMap<Long, StationIndex> map = new HashMap<Long, StationIndex>(10000);
  private int stn_fldno;
  private StructureDataRegexp.Vinfo dataVinfo, stnVinfo;

  @Override
  public void open(RandomAccessFile raff, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    String dataFile = raff.getLocation();
    int pos = dataFile.lastIndexOf(".");
    String base = dataFile.substring(0, pos);
    String ext = dataFile.substring(pos);

    if (ext.equals(IDX_EXT)) {
      dataRaf = new RandomAccessFile(base+DAT_EXT, "r");
      stnRaf = new RandomAccessFile(base+STN_EXT, "r");

     } else if (ext.equals(DAT_EXT)) {
      dataRaf = raff;
      stnRaf = new RandomAccessFile(base+STN_EXT, "r");

     } else {
      stnRaf = raff;
      dataRaf = new RandomAccessFile(base+DAT_EXT, "r");
     }

    NcmlConstructor ncmlc = new NcmlConstructor();
    if (!ncmlc.populateFromResource("resources/nj22/iosp/ghcnm.ncml", ncfile)) {
      throw new IllegalStateException(ncmlc.getErrlog().toString());
    }
    ncfile.finish();

    dataVinfo = setVinfo(dataRaf, ncfile, dataPattern, "all_data");
    stnVinfo = setVinfo(stnRaf, ncfile, stnPattern, "station");

    StructureMembers.Member m = stnVinfo.sm.findMember(STNID);
    StructureDataRegexp.VinfoField f = (StructureDataRegexp.VinfoField) m.getDataObject();
    stn_fldno = f.fldno;

     // make index file if needed
    File idxFile = new File(base+IDX_EXT);
    if (!idxFile.exists())
      makeIndex(stnVinfo, dataVinfo, idxFile);
    else
      readIndex(idxFile.getPath());
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
        v.remove(att);
      }
      m.setDataObject( vf);
    }

    return result;
  }

  public void close() throws java.io.IOException {
    stnRaf.close();
    dataRaf.close();
  }
     
  ////////////////////////////////////////////////////////////////////

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    StructureDataRegexp.Vinfo vinfo = (StructureDataRegexp.Vinfo) v2.getSPobject();
    return new ArraySequence( vinfo.sm, new SeqIter(vinfo), vinfo.nelems);
  }

  @Override
  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    StructureDataRegexp.Vinfo vinfo = (StructureDataRegexp.Vinfo) s.getSPobject();
    return new SeqIter(vinfo);
  }

  private class SeqIter implements StructureDataIterator {
    private StructureDataRegexp.Vinfo vinfo;
    private long bytesRead;
    private long totalBytes;
    private int recno;
    private StructureData curr;

    SeqIter(StructureDataRegexp.Vinfo vinfo) throws IOException {
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
      throw new IllegalStateException("Bad version = "+version);

    int count = NcStream.readVInt(fin);

    for (int i=0; i<count; i++) {
      int size = NcStream.readVInt(fin);
      byte[] pb = new byte[size];
      NcStream.readFully(fin, pb);
      StationIndex si = decodeStationIndex(pb);
      map.put(si.stnId, si);
    }
    fin.close();

    System.out.println(" read index map size=" + map.values().size());
  }

 private void makeIndex(StructureDataRegexp.Vinfo stnInfo, StructureDataRegexp.Vinfo dataInfo, File indexFile ) throws IOException {
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

    private byte[] encodeStationProto()  {
      GhcnmProto.StationIndex.Builder builder = GhcnmProto.StationIndex.newBuilder();
      builder.setStnid(stnId);
      builder.setStnPos(stnPos);
      builder.setDataPos(dataPos);
      builder.setDataCount(dataCount);
      ucar.nc2.iosp.noaa.GhcnmProto.StationIndex proto =  builder.build();
      return proto.toByteArray();
    }
  }

}
