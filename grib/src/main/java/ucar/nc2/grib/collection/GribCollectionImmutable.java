/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.collection;

import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MFile;
import ucar.coord.Coordinate;
import ucar.coord.CoordinateRuntime;
import ucar.coord.SparseArray;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2SectionProductDefinition;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Describe
 *
 * @author caron
 * @since 11/10/2014
 */
@Immutable
public class GribCollectionImmutable {
  static private final Logger logger = LoggerFactory.getLogger(GribCollectionImmutable.class);

  ////////////////////////////////////////////////////////////////
  protected final String name; // collection name; index filename must be directory/name.ncx2
  protected final File directory;
  protected final FeatureCollectionConfig config;
  protected final boolean isGrib1;
  protected final Info info;

  // protected Map<Integer, MFile> fileMap;    // all the files used in the GC; key in index in original collection, GC has subset of them
  protected final List<Dataset> datasets;
  protected final List<GribCollection.HorizCoordSys> horizCS; // one for each unique GDS
  protected final CoordinateRuntime masterRuntime;
  protected GribTables cust;

  // not stored in index
  private Map<String, MFile> filenameMap;
  protected RandomAccessFile indexRaf; // this is the raf of the index (ncx) file, synchronize any access to it
  protected FileCacheIF objCache = null;  // optional object cache - used in the TDS
  protected String indexFilename;

  public static int countGC;

  public GribCollectionImmutable(String name, File directory, FeatureCollectionConfig config, boolean isGrib1, Info info,
                                 List<Dataset> datasets, List<GribCollection.HorizCoordSys> horizCS, CoordinateRuntime masterRuntime) {
    countGC++;

    if (config == null)
      logger.error("HEY GribCollection {} has empty config%n", name);
    if (name == null)
      logger.error("HEY GribCollection has null name dir={}%n", directory);

    this.name = name;
    this.directory = directory;
    this.config = config;
    this.isGrib1 = isGrib1;
    this.info = info;

    this.datasets = datasets;
    this.horizCS = horizCS;
    this.masterRuntime = masterRuntime;
  }

  public class Info {
    final int version; // the ncx version
    final int center, subcenter, master, local;  // GRIB 1 uses "local" for table version
    final int genProcessType, genProcessId, backProcessId;

    public Info(int version, int center, int subcenter, int master, int local, int genProcessType, int genProcessId, int backProcessId) {
      this.version = version;
      this.center = center;
      this.subcenter = subcenter;
      this.master = master;
      this.local = local;
      this.genProcessType = genProcessType;
      this.genProcessId = genProcessId;
      this.backProcessId = backProcessId;
    }
  }

  @Immutable
  public class Dataset {
    final GribCollection.Type type;
    final List<GroupGC> groups;  // must be kept in order, because PartitionForVariable2D has index into it

    public Dataset(GribCollection.Type type, List<GroupGC> groups) {
      this.type = type;
      this.groups = groups;
    }

    public Iterable<GroupGC> getGroups() {
      return groups;
    }

    public int getGroupsSize() {
      return groups.size();
    }

    public GribCollection.Type getType() {
      return type;
    }

    public boolean isTwoD() {
      return type == GribCollection.Type.TwoD;
    }

    public GroupGC getGroup(int index) {
      return groups.get(index);
    }

    public GroupGC findGroupById(String id) {
      for (GroupGC g : getGroups()) {
        if (g.getId().equals(id))
          return g;
      }
      return null;
    }
  }

  @Immutable
  public class HorizCoordSys { // encapsolates the gds; shared by the GroupHcs
    private final GdsHorizCoordSys hcs;
    private final byte[] rawGds;
    private final int gdsHash;
    private final String id, description;
    private final int predefinedGridDefinition;

    public HorizCoordSys(GdsHorizCoordSys hcs, byte[] rawGds, int gdsHash, String id, int predefinedGridDefinition) {
      this.hcs = hcs;
      this.rawGds = rawGds;
      this.gdsHash = gdsHash;
      this.predefinedGridDefinition = predefinedGridDefinition;

      this.id = id;
      this.description = makeDescription();
    }

    public GdsHorizCoordSys getHcs() {
      return hcs;
    }

    public byte[] getRawGds() {
      return rawGds;
    }

    public int getGdsHash() {
      return gdsHash;
    }

    // unique name for Group
    public String getId() {
      return id;
    }

    // human readable
    public String getDescription() {
      return description;
    }

    public int getPredefinedGridDefinition() {
      return predefinedGridDefinition;
    }

    private String makeDescription() {
      // check for user defined group names
      String result = null;
      if (config.gribConfig.gdsNamer != null)
        result = config.gribConfig.gdsNamer.get(gdsHash);
      if (result != null) return result;

      return hcs.makeDescription(); // default desc
    }
  }

  // this class should be immutable, because it escapes
  @Immutable
  public class GroupGC {
    HorizCoordSys horizCoordSys;
    List<GribCollection.VariableIndex> variList;
    List<Coordinate> coords;      // shared coordinates
    int[] filenose;               // key for GC.fileMap
    Map<Integer, VariableIndex> varMap;
    boolean isTwod = true;        // true for GC and twoD; so should be called "reference" dataset or something

    GroupGC() {
      this.variList = new ArrayList<>();
      this.coords = new ArrayList<>();
    }

    // copy constructor for PartitionBuilder
    GroupGC(GroupGC from) {
      this.horizCoordSys = from.horizCoordSys;     // reference
      this.variList = new ArrayList<>(from.variList.size());
      this.coords = new ArrayList<>(from.coords.size());
      this.isTwod = from.isTwod;
    }

    public String getId() {
      return horizCoordSys.getId();
    }

  }

  @Immutable
  public class VariableIndex {
    final GroupGC group;     // belongs to this group
    final VariableIndex.Info info;

    final List<Integer> coordIndex;  // indexes into group.coords
    final long recordsPos;    // where the records array is stored in the index. 0 means no records
    final int recordsLen;

    // read in on demand
    private SparseArray<Record> sa;   // for GC only; lazily read; same array shape as variable, minus x and y

    private VariableIndex(GroupGC g, VariableIndex.Info info, List<Integer> index, long recordsPos, int recordsLen) {
      this.group = g;
      this.info = info;
      this.coordIndex = index;
      this.recordsPos = recordsPos;
      this.recordsLen = recordsLen;
    }

    @Immutable
    public final class Info {
      final int tableVersion;   // grib1 only : can vary by variable
      final int discipline;     // grib2 only
      final byte[] rawPds;      // grib1 or grib2
      final int cdmHash;

      // derived from pds
      final int category, parameter, levelType, intvType, ensDerivedType, probType;
      final String intvName;  // eg "mixed intervals, 3 Hour, etc"
      final String probabilityName;
      final boolean isLayer, isEnsemble;
      final int genProcessType;

      public Info(int tableVersion, int discipline, byte[] rawPds, int cdmHash, int category, int parameter, int levelType, int intvType, int ensDerivedType,
                  int probType, String intvName, String probabilityName, boolean isLayer, boolean isEnsemble, int genProcessType) {
        this.tableVersion = tableVersion;
        this.discipline = discipline;
        this.rawPds = rawPds;
        this.cdmHash = cdmHash;
        this.category = category;
        this.parameter = parameter;
        this.levelType = levelType;
        this.intvType = intvType;
        this.ensDerivedType = ensDerivedType;
        this.probType = probType;
        this.intvName = intvName;
        this.probabilityName = probabilityName;
        this.isLayer = isLayer;
        this.isEnsemble = isEnsemble;
        this.genProcessType = genProcessType;
      }

      private Info(GribTables customizer, int discipline, String intvName, byte[] rawPds, int cdmHash) {
        this.discipline = discipline;
        this.intvName = intvName;
        this.rawPds = rawPds;
        this.cdmHash = cdmHash;

        if (isGrib1) {
          Grib1Customizer cust = (Grib1Customizer) customizer;
          Grib1SectionProductDefinition pds = new Grib1SectionProductDefinition(rawPds);

          // quantities that are stored in the pds
          this.category = 0;
          this.tableVersion = pds.getTableVersion();
          this.parameter = pds.getParameterNumber();
          this.levelType = pds.getLevelType();
          Grib1ParamTime ptime = pds.getParamTime(cust);
          if (ptime.isInterval()) {
            this.intvType = pds.getTimeRangeIndicator();
          } else {
            this.intvType = -1;
          }
          this.isLayer = cust.isLayer(pds.getLevelType());

          this.ensDerivedType = -1;
          this.probType = -1;
          this.probabilityName = null;

          this.genProcessType = pds.getGenProcess(); // LOOK process vs process type ??
          this.isEnsemble = pds.isEnsemble();

        } else {
          Grib2SectionProductDefinition pdss = new Grib2SectionProductDefinition(rawPds);
          Grib2Pds pds = null;
          try {
            pds = pdss.getPDS();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          this.tableVersion = -1;

          // quantities that are stored in the pds
          this.category = pds.getParameterCategory();
          this.parameter = pds.getParameterNumber();
          this.levelType = pds.getLevelType1();
          this.intvType = pds.getStatisticalProcessType();
          this.isLayer = Grib2Utils.isLayer(pds);

          if (pds.isEnsembleDerived()) {
            Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
            ensDerivedType = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
          } else {
            this.ensDerivedType = -1;
          }

          if (pds.isProbability()) {
            Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
            probabilityName = pdsProb.getProbabilityName();
            probType = pdsProb.getProbabilityType();
          } else {
            this.probType = -1;
            this.probabilityName = null;
          }

          this.genProcessType = pds.getGenProcessType();
          this.isEnsemble = pds.isEnsemble();
        }
      }
    }

    @Immutable
    public class Record {
      public final int fileno;    // which file
      public final long pos;      // offset on file where data starts
      public final long bmsPos;   // if non-zero, offset where bms starts
      public final int scanMode;  // from gds

      public Record(int fileno, long pos, long bmsPos, int scanMode) {
        this.fileno = fileno;
        this.pos = pos;
        this.bmsPos = bmsPos;
        this.scanMode = scanMode;
      }

      @Override
      public String toString() {
        final StringBuilder sb = new StringBuilder("GribCollection.Record{");
        sb.append("fileno=").append(fileno);
        sb.append(", pos=").append(pos);
        sb.append(", bmsPos=").append(bmsPos);
        sb.append(", scanMode=").append(scanMode);
        sb.append('}');
        return sb.toString();
      }
    }
  }
}
