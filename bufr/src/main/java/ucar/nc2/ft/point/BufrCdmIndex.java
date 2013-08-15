package ucar.nc2.ft.point;

import thredds.inventory.MFile;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Manage cdm index (ncx) for Bufr files
 *
 * @author caron
 * @since 8/14/13
 */
public class BufrCdmIndex {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrFeatureDatasetFactory.class);


  protected static final int minVersionSingle = 9; // if single file, this version and above is ok
  protected static final int version = 1;
  public static final String MAGIC_START = "BufrCdmIndex";

  /*
   MAGIC_START
   version
   sizeRecords
   VariableRecords (sizeRecords bytes)
   sizeIndex
   GribCollectionIndex (sizeIndex bytes)
  */

  private void createIndex(File indexFile, List<Group> groups, ArrayList<MFile> files) throws IOException {

    if (indexFile.exists()) {
      if (!indexFile.delete())
        log.warn(" gc1 cant delete index file {}", indexFile.getPath());
    }
    log.debug(" createIndex for {}", indexFile.getPath());

    RandomAccessFile raf = new RandomAccessFile(indexFile.getPath(), "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    try {
      //// header message
      raf.write(MAGIC_START.getBytes("UTF-8"));
      raf.writeInt(version);
      long lenPos = raf.getFilePointer();
      raf.writeLong(0); // save space to write the length of the record section
      long countBytes = 0;
      int countRecords = 0;
      for (Group g : groups) {
        g.fileSet = new HashSet<Integer>();
        for (Grib1Rectilyser.VariableBag vb : g.rect.getGribvars()) {
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
      if (logger.isDebugEnabled())
        logger.debug("  write RecordMaps: bytes = {} records = {} bytesPerRecord={}", countBytes, countRecords, bytesPerRecord);

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

      // directory and mfile list
      indexBuilder.setDirName(gc.getDirectory().getPath());
      List<GribCollectionBuilder.GcMFile> gcmfiles = GribCollectionBuilder.makeFiles(gc.getDirectory(), files);
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
      Grib1SectionProductDefinition pds = first.getPDSsection();
      indexBuilder.setCenter(pds.getCenter());
      indexBuilder.setSubcenter(pds.getSubCenter());
      indexBuilder.setLocal(pds.getTableVersion());
      indexBuilder.setMaster(0);
      indexBuilder.setGenProcessId(pds.getGenProcess());

      GribCollectionProto.GribCollectionIndex index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      logger.debug("  write GribCollectionIndex= {} bytes", b.length);

    } finally {
      logger.debug("  file size =  %d bytes", raf.length());
      raf.close();

      // remove it on failure
      if (deleteOnClose && !indexFile.delete())
        logger.error(" gc1 cant deleteOnClose index file {}", indexFile.getPath());
    }
  }

}
