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

package ucar.nc2.grib.grib2.builder;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.stream.NcStream;
import ucar.nc2.util.CloseableIterator;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

import java.io.*;
import java.util.*;

/**
 * Build a GribCollection object for Grib-2 files. Manage grib collection index.
 * Covers GribCollectionProto, which serializes and deserializes.
 * Rectilyse means to turn the collection into a multidimensional variable.
 *
 * @author caron
 * @since 4/6/11
 */
public class Grib2CollectionBuilder extends GribCollectionBuilder {

  public static final String MAGIC_START = "Grib2CollectionIndex";
  protected static final int minVersionSingle = 11;
  protected static final int version = 12;
  private static final boolean showFiles = false;

    // called by tdm
  static public boolean update(MCollection dcm, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    if (!builder.needsUpdate()) return false;
    builder.readOrCreateIndex(CollectionUpdateType.always);
    builder.gc.close();
    return true;
  }

  // from a single file, read in the index, create if it doesnt exist
  static public GribCollection readOrCreateIndexFromSingleFile(MFile file, CollectionUpdateType force, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(file, config, logger);
    builder.readOrCreateIndex(force);
    return builder.gc;
  }

  /**
   * From a CollectionManagerRO, read in the index, create if it doesnt exist or is out of date
   * @param dcm  the CollectionManager, assumed up-to-date
   * @param force  force read the index
   * @param logger log here
   * @return GribCollection
   * @throws IOException on IO error
   */
  static public GribCollection factory(MCollection dcm, CollectionUpdateType force, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    builder.readOrCreateIndex(force);
    return builder.gc;
  }

  /* read in the index, index raf already open
  static public GribCollection createFromIndex(String name, File directory, RandomAccessFile raf, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(name, directory, config, logger);
    if (builder.readIndex(raf))
      return builder.gc;
    throw new IOException("Reading index failed");
  } */

  /* this writes the index always
  static public boolean writeIndexFile(File indexFile, CollectionManagerRO dcm, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    return builder.createIndex(indexFile);
  } */

  // this writes the index always
  static public boolean makeIndex(MCollection dcm, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    File indexFile = builder.gc.getIndexFile();
    boolean ok = builder.createIndex(indexFile, errlog);
    builder.gc.close();
    return ok;
  }

  ////////////////////////////////////////////////////////////////

  protected GribCollection gc;
  protected Grib2Customizer tables; // only gets created in makeAggGroups
  protected String name;
  protected File directory;

  // single file
  private Grib2CollectionBuilder(MFile file, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    super(new CollectionSingleFile(file, logger), true, logger);
    this.name = file.getName();
    this.directory = new File(dcm.getRoot());

    try {
      if (config != null) dcm.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config);
      this.gc = new Grib2Collection(this.name, this.directory, config);

    } catch (Exception e) {
      logger.error("Failed to index single file", e);
      throw new IOException(e);
    }
  }

  private Grib2CollectionBuilder(MCollection dcm, org.slf4j.Logger logger) {
    super(dcm, false, logger);
    this.name = dcm.getCollectionName();
    this.directory = new File(dcm.getRoot());

    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    this.gc = new Grib2Collection(this.name, this.directory, config);
  }

  protected Grib2CollectionBuilder(MCollection dcm, boolean isSingleFile, org.slf4j.Logger logger) {
    super(dcm, isSingleFile, logger);
  }

  // read or create index
  private void readOrCreateIndex(CollectionUpdateType ff) throws IOException {

    // force new index or test for new index needed
    boolean force = ((ff == CollectionUpdateType.always) || (ff == CollectionUpdateType.test && needsUpdate()));

    // otherwise, we're good as long as the index file exists
    File idx = gc.getIndexFile();
    if (force || !idx.exists() || !readIndex(idx.getPath()) )  {
       // write out index
       idx = gc.makeNewIndexFile(logger); // make sure we have a writeable index
       logger.info("{}: createIndex {}", gc.getName(), idx.getPath());
       createIndex(idx, null);

       // read back in index
       RandomAccessFile indexRaf = new RandomAccessFile(idx.getPath(), "r");
       gc.setIndexRaf(indexRaf);
       readIndex(indexRaf);
     }
  }

  public boolean needsUpdate() throws IOException {
    if (dcm == null) return false;
    File idx = gc.getIndexFile();
    return !idx.exists() || needsUpdate(idx.lastModified());
  }

  private boolean needsUpdate(long idxLastModified) throws IOException {
    CollectionManager.ChangeChecker cc = GribIndex.getChangeChecker();
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) {
     while (iter.hasNext()) {
       if (cc.hasChangedSince(iter.next(), idxLastModified)) return true;
     }
   }
    return false;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // reading

  private boolean readIndex(String filename) throws IOException {
    return readIndex( new RandomAccessFile(filename, "r") );
  }

  private boolean readIndex(RandomAccessFile indexRaf) throws IOException {
    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    try {
      gc = Grib2CollectionBuilderFromIndex.createFromIndex(this.name, this.directory, indexRaf, config, logger);
      return true;
    } catch (IOException ioe) {
      return false;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////
  // writing

  private class Group {
    public Grib2SectionGridDefinition gdss;
    public int gdsHash; // may have been modified
    public Grib2Rectilyser rect;
    public List<Grib2Record> records = new ArrayList<>();
    public String nameOverride;
    public Set<Integer> fileSet; // this is so we can show just the component files that are in this group

    private Group(Grib2SectionGridDefinition gdss, int gdsHash) {
      this.gdss = gdss;
      this.gdsHash = gdsHash;
    }
  }


  ///////////////////////////////////////////////////
  // create the index

  private boolean createIndex(File indexFile, Formatter errlog) throws IOException {
    if (dcm == null) {
      logger.error("Grib2CollectionBuilder "+gc.getName()+" : cannot create new index ");
      throw new IllegalStateException();
    }

    long start = System.currentTimeMillis();

    List<MFile> files = new ArrayList<>();
    List<Group> groups = makeAggregatedGroups(files, errlog);
    List<MFile> allFiles = Collections.unmodifiableList(files);
    createIndex(indexFile, groups, allFiles);

    long took = System.currentTimeMillis() - start;
    logger.debug("That took {} msecs", took);
    return true;
  }

  // read all records in all files,
  // divide into groups based on GDS hash
  // each group has an arraylist of all records that belong to it.
  // for each group, run rectlizer to derive the coordinates and variables
  private List<Group> makeAggregatedGroups(List<MFile> allFiles, Formatter errlog) throws IOException {
    Map<Integer, Group> gdsMap = new HashMap<Integer, Group>();
    Map<String, Boolean> pdsConvert = null;

    logger.debug("GribCollection {}: makeAggregatedGroups", gc.getName());
    int fileno = 0;
    Grib2Rectilyser.Counter statsAll = new Grib2Rectilyser.Counter(); // debugging

    logger.debug(" dcm={}", dcm);
    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    Map<Integer, Integer> gdsConvert = (config != null) ?  config.gdsHash : null;
    FeatureCollectionConfig.GribIntvFilter intvMap = (config != null) ?  config.intvFilter : null;
    if (config != null) pdsConvert = config.pdsHash;

    int totalRecords = 0;
    for (MFile mfile : dcm.getFilesSorted()) { // LOOK do we need sorted ??
      Grib2Index index;
      try {                  // LOOK here is where gbx9 files get recreated
        index = (Grib2Index) GribIndex.readOrCreateIndexFromSingleFile(false, !isSingleFile, mfile, config, CollectionUpdateType.test, logger);
        allFiles.add(mfile);  // add on success

      } catch (IOException ioe) {
        if (errlog != null) errlog.format("ERR Grib2CollectionBuilder %s: reading/Creating gbx9 index for file %s failed err=%s%n", gc.getName(), mfile.getPath(), ioe.getMessage());
        logger.error("Grib2CollectionBuilder "+gc.getName()+" : reading/Creating gbx9 index for file "+ mfile.getPath()+" failed", ioe);
        continue;
      }
      int n = index.getNRecords();
      totalRecords += n;
      if (showFiles) {
        System.out.printf("Open %d %s number of records = %d (%d) %n", fileno, mfile.getPath(), n, totalRecords);
      }

      for (Grib2Record gr : index.getRecords()) {
        if (this.tables == null) {
          Grib2SectionIdentification ids = gr.getId(); // so all records must use the same table (!)
          this.tables = Grib2Customizer.factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version());
          if (config != null) tables.setTimeUnitConverter(config.getTimeUnitConverter());
        }
        if (intvMap != null && filterOut(gr, intvMap)) {
          statsAll.filter++;
          continue; // skip
        }

        gr.setFile(fileno); // each record tracks which file it belongs to
        int gdsHash = gr.getGDSsection().getGDS().hashCode();  // use GDS hash code to group records
        if (gdsConvert != null && gdsConvert.get(gdsHash) != null) // allow external config to muck with gdsHash. Why? because of error in encoding
          gdsHash = gdsConvert.get(gdsHash);             // and we need exact hash matching

        Group g = gdsMap.get(gdsHash);
        if (g == null) {
          g = new Group(gr.getGDSsection(), gdsHash);
          gdsMap.put(gdsHash, g);
        }
        g.records.add(gr);
      }
      fileno++;
      statsAll.recordsTotal += index.getRecords().size();
    }
    // System.out.printf("%s: Open %d files %d records%n",  gc.getName(), fileno, totalRecords);

    List<Group> groups = new ArrayList<>(gdsMap.values());
    for (Group g : groups) {
      Grib2Rectilyser.Counter stats = new Grib2Rectilyser.Counter(); // debugging
      g.rect = new Grib2Rectilyser(tables, g.records, g.gdsHash, pdsConvert);
      g.rect.make(stats, Collections.unmodifiableList(allFiles), errlog);
      if (errlog != null) errlog.format("Group hash=%d %s%n", g.gdsHash, stats.show());
      statsAll.add(stats);
    }

    // debugging and validation
    if (logger.isDebugEnabled()) logger.debug(statsAll.show());
    if (errlog != null) errlog.format("%s%n", statsAll.show());

    return groups;
  }

  // true means remove
  private boolean filterOut(Grib2Record gr, FeatureCollectionConfig.GribIntvFilter intvFilter) {
    int[] intv = tables.getForecastTimeIntervalOffset(gr);
    if (intv == null) return false;
    int haveLength = intv[1] - intv[0];

    // HACK
    if (haveLength == 0 && intvFilter.isZeroExcluded()) {  // discard 0,0
      if ((intv[0] == 0) && (intv[1] == 0)) {
        //f.format(" FILTER INTV [0, 0] %s%n", gr);
        return true;
      }
      return false;

    } else if (intvFilter.hasFilter()) {
      int discipline = gr.getIs().getDiscipline();
      Grib2Pds pds = gr.getPDS();
      int category = pds.getParameterCategory();
      int number = pds.getParameterNumber();
      int id = (discipline << 16) + (category << 8) + number;

      int prob = Integer.MIN_VALUE;
      if (pds.isProbability()) {
        Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
        prob = (int) (1000 * pdsProb.getProbabilityUpperLimit());
      }
      return !intvFilter.filterOk(id, haveLength, prob);
    }
    return false;
  }


  ///////////////////////////////////////////////////
  // heres where the actual writing is

  /*
   MAGIC_START
   version
   sizeRecords
   VariableRecords (sizeRecords bytes)
   sizeIndex
   GribCollectionIndex (sizeIndex bytes)
   */

  private boolean createIndex(File indexFile, List<Group> groups, List<MFile> files) throws IOException {
    Grib2Record first = null; // take global metadata from here
    boolean deleteOnClose = false;

    if (indexFile.exists()) {
      if (!indexFile.delete()) {
        logger.error("gc2 cant delete index file {}", indexFile.getPath());
      }
    }
    logger.debug(" createIndex for {}", indexFile.getPath());

    RandomAccessFile raf = new RandomAccessFile(indexFile.getPath(), "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    try {
      //// header message
      raf.write(MAGIC_START.getBytes(CDM.utf8Charset));
      raf.writeInt(version);
      long lenPos = raf.getFilePointer();
      raf.writeLong(0); // save space to write the length of the record section
      long countBytes = 0;
      int countRecords = 0;

      for (Group g : groups) {
        g.fileSet = new HashSet<Integer>();
        for (Grib2Rectilyser.VariableBag vb : g.rect.getGribvars()) {
          if (first == null) first = vb.first;
          GribCollectionProto.VariableRecords vr = writeRecordsProto(vb, g.fileSet);
          byte[] b = vr.toByteArray();
          vb.pos = raf.getFilePointer();
          vb.length = b.length;
          raf.write(b);
          countBytes += b.length;
          countRecords += vb.recordMap.length;
        }
      }

      long bytesPerRecord = countBytes / ((countRecords == 0) ? 1 : countRecords);
      if (logger.isDebugEnabled()) logger.debug("  write RecordMaps: bytes = {} record = {} bytesPerRecord={}", countBytes, countRecords, bytesPerRecord);

      if (first == null) {
        deleteOnClose = true;
        logger.error("GribCollection {}: has no files", gc.getName());
        throw new IOException("GribCollection " + gc.getName() + " has no files");
      }

      long pos = raf.getFilePointer();
      raf.seek(lenPos);
      raf.writeLong(countBytes);
      raf.seek(pos); // back to the output.

      GribCollectionProto.GribCollectionIndex.Builder indexBuilder = GribCollectionProto.GribCollectionIndex.newBuilder();
      indexBuilder.setName(gc.getName());
      indexBuilder.setDirName(gc.getDirectory().getPath());

      // directory and mfile list
      indexBuilder.setDirName(gc.getDirectory().getPath());
      List<GribCollectionBuilder.GcMFile> gcmfiles = GribCollectionBuilder.makeFiles(gc.getDirectory(), files, null);
      for (GribCollectionBuilder.GcMFile gcmfile : gcmfiles) {
        indexBuilder.addMfiles(gcmfile.makeProto());
      }

      for (Group g : groups)
        indexBuilder.addGroups(writeGroupProto(g));


      /* int count = 0;
      for (DatasetCollectionManager dcm : collections) {
        indexBuilder.addParams(makeParamProto(new Parameter("spec" + count, dcm.())));
        count++;
      } */

      // what about just storing first ??
      Grib2SectionIdentification ids = first.getId();
      indexBuilder.setCenter(ids.getCenter_id());
      indexBuilder.setSubcenter(ids.getSubcenter_id());
      indexBuilder.setMaster(ids.getMaster_table_version());
      indexBuilder.setLocal(ids.getLocal_table_version());

      Grib2Pds pds = first.getPDS();
      indexBuilder.setGenProcessType(pds.getGenProcessType());
      indexBuilder.setGenProcessId(pds.getGenProcessId());
      indexBuilder.setBackProcessId(pds.getBackProcessId());

      GribCollectionProto.GribCollectionIndex index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      logger.debug("  write GribCollectionIndex= {} bytes", b.length);

    } finally {
      logger.debug("  file size =  {} bytes", raf.length());
      if (raf != null) raf.close();

            // remove it on failure
      if (deleteOnClose && !indexFile.delete())
        logger.error(" gc2 cant deleteOnClose index file {}", indexFile.getPath());
    }

    return true;
  }

  private GribCollectionProto.VariableRecords writeRecordsProto(Grib2Rectilyser.VariableBag vb, Set<Integer> fileSet) throws IOException {
    GribCollectionProto.VariableRecords.Builder b = GribCollectionProto.VariableRecords.newBuilder();
    b.setCdmHash(vb.cdmHash);

    for (Grib2Rectilyser.Record ar : vb.recordMap) {
      GribCollectionProto.Record.Builder br = GribCollectionProto.Record.newBuilder();

      if (ar == null || ar.gr == null) {
        br.setFileno(0);
        br.setPos(0); // missing : ok to use 0 since drsPos > 0

      } else {
        br.setFileno(ar.gr.getFile());
        fileSet.add(ar.gr.getFile());
        Grib2SectionDataRepresentation drs = ar.gr.getDataRepresentationSection();
        br.setPos(drs.getStartingPosition());
        if (ar.gr.isBmsReplaced()) {
          Grib2SectionBitMap bms = ar.gr.getBitmapSection();
          br.setBmsPos(bms.getStartingPosition());
        }
      }
      b.addRecords(br);
    }
    return b.build();
  }

  private GribCollectionProto.Group writeGroupProto(Group g) throws IOException {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGds(ByteString.copyFrom(g.gdss.getRawBytes()));
    b.setGdsHash(g.gdsHash);

    for (Grib2Rectilyser.VariableBag vb : g.rect.getGribvars())
      b.addVariables(writeVariableProto(g.rect, vb));

    List<TimeCoord> timeCoords = g.rect.getTimeCoords();
    for (int i = 0; i < timeCoords.size(); i++)
      b.addTimeCoords(writeCoordProto(timeCoords.get(i), i));

    List<VertCoord> vertCoords = g.rect.getVertCoords();
    for (int i = 0; i < vertCoords.size(); i++)
      b.addVertCoords(writeCoordProto(vertCoords.get(i), i));

    List<EnsCoord> ensCoords = g.rect.getEnsCoords();
    for (int i = 0; i < ensCoords.size(); i++)
      b.addEnsCoords(writeCoordProto(ensCoords.get(i), i));

    for (Integer aFileSet : g.fileSet)
      b.addFileno(aFileSet);

    if (g.nameOverride != null)
      b.setName(g.nameOverride);

    return b.build();
  }

  private GribCollectionProto.Variable writeVariableProto(Grib2Rectilyser rect, Grib2Rectilyser.VariableBag vb) throws IOException {
    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(vb.first.getDiscipline());
    Grib2Pds pds = vb.first.getPDS();
    b.setCategory(pds.getParameterCategory());
    b.setParameter(pds.getParameterNumber());
    b.setLevelType(pds.getLevelType1());
    b.setIsLayer(Grib2Utils.isLayer(pds));
    b.setIntervalType(pds.getStatisticalProcessType());
    b.setCdmHash(vb.cdmHash);

    b.setRecordsPos(vb.pos);
    b.setRecordsLen(vb.length);
    b.setTimeIdx(vb.timeCoordIndex);
    if (vb.vertCoordIndex >= 0)
      b.setVertIdx(vb.vertCoordIndex);
    if (vb.ensCoordIndex >= 0)
      b.setEnsIdx(vb.ensCoordIndex);

    if (pds.isEnsembleDerived()) {
      Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
      b.setEnsDerivedType(pdsDerived.getDerivedForecastType()); // derived type (table 4.7)
    }

    if (pds.isProbability()) {
      Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
      b.setProbabilityName(pdsProb.getProbabilityName());
      b.setProbabilityType(pdsProb.getProbabilityType());
    }

    if (pds.isTimeInterval())
      b.setIntvName(rect.getTimeIntervalName(vb.timeCoordIndex));

    int genType = pds.getGenProcessType();
    if (genType != GribNumbers.UNDEFINED)
      b.setGenProcessType(pds.getGenProcessType());

    return b.build();
  }

  protected GribCollectionProto.Parameter writeParamProto(Parameter param) throws IOException {
    GribCollectionProto.Parameter.Builder b = GribCollectionProto.Parameter.newBuilder();

    b.setName(param.getName());
    if (param.isString())
      b.setSdata(param.getStringValue());
    else {
      for (int i = 0; i < param.getLength(); i++)
        b.addData(param.getNumericValue(i));
    }

    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(TimeCoord tc, int index) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setIndex(index);
    b.setCode(tc.getCode());
    b.setUnit(tc.getUnits());
    float scale = (float) tc.getTimeUnitScale(); // deal with, eg, "6 hours" by multiplying values by 6
    if (tc.isInterval()) {
      for (TimeCoord.Tinv tinv : tc.getIntervals()) {
        b.addValues(tinv.getBounds1() * scale);
        b.addBound(tinv.getBounds2() * scale);
      }
    } else {
      for (int value : tc.getCoords())
        b.addValues(value * scale);
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(VertCoord vc, int index) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setIndex(index);
    b.setCode(vc.getCode());
    String units = vc.getUnits();
    if (units == null) units = "";
    b.setUnit(units);
    for (VertCoord.Level coord : vc.getCoords()) {
      if (vc.isLayer()) {
        b.addValues((float) coord.getValue1());
        b.addBound((float) coord.getValue2());
      } else {
        b.addValues((float) coord.getValue1());
      }
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(EnsCoord ec, int index) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setIndex(index);
    b.setCode(0);
    b.setUnit("");
    for (EnsCoord.Coord coord : ec.getCoords()) {
      b.addValues((float) coord.getCode());
      b.addValues((float) coord.getEnsMember());
    }
    return b.build();
  }

}
