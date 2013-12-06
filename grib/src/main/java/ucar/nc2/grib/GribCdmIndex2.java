package ucar.nc2.grib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.collection.*;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * Description
 *
 * @author John
 * @since 12/5/13
 */
public class GribCdmIndex2 {
  static private final Logger logger = LoggerFactory.getLogger(GribCdmIndex2.class);

   // open GribCollection. caller must close
  static public ucar.nc2.grib.collection.GribCollection openCdmIndex(String indexFile, FeatureCollectionConfig.GribConfig config, Logger logger) throws IOException {
    ucar.nc2.grib.collection.GribCollection gc = null;
    String magic = null;
    RandomAccessFile raf = new RandomAccessFile(indexFile, "r");

    try {
      raf.seek(0);
      byte[] b = new byte[Grib2CollectionBuilder.MAGIC_START.getBytes().length];   // they are all the same
      raf.read(b);
      magic = new String(b);

      switch (magic) {
        case Grib2CollectionBuilder.MAGIC_START:
          gc = Grib2CollectionBuilderFromIndex.createFromIndex(indexFile, null, raf, config, logger);
          break;
      }
      return gc;

    } catch (Throwable t) {
      raf.close();
      throw t;
    }
  }


}
