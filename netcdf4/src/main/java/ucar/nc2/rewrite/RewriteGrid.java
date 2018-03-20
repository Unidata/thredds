/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.rewrite;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dt.grid.CFGridWriter2;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 8/5/2014
 */
public class RewriteGrid {

  double rewrite(String filenameIn, String filenameOut,
                     NetcdfFileWriter.Version version, Nc4Chunking.Strategy chunkerType, int deflateLevel, boolean shuffle,
                     Formatter fw) throws IOException {

    ucar.nc2.dt.GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(filenameIn);
    Nc4Chunking chunking = (version == NetcdfFileWriter.Version.netcdf3) ? null : Nc4ChunkingStrategy.factory(chunkerType, deflateLevel, shuffle);
    NetcdfFileWriter writer = NetcdfFileWriter.createNew(version, filenameOut, chunking);

    long start = System.currentTimeMillis();

    long totalBytes;
    try {
      totalBytes = CFGridWriter2.writeFile(gds, null, null, null, 1, null, null, 1, true, writer);
    } catch (Throwable e) {
      e.printStackTrace();
      return 0;
    }

    File fin = new File(filenameIn);
    double lenIn = (double) fin.length();
    lenIn /= 1000 * 1000;

    File fout = new File(filenameOut);
    double lenOut = (double) fout.length();
    lenOut /= 1000 * 1000;

    System.out.format("   %10.3f: %s%n", lenOut / lenIn, fout.getCanonicalPath());
    double took = (System.currentTimeMillis() - start) /1000.0;
    System.out.format("   that took: %f secs%n", took);

    if (fw != null)
      fw.format("%s,%10.3f, %s,%s, %d, %10.3f,%10.3f,%d%n", fin.getName(), lenIn,
            (chunkerType != null) ? chunkerType : "nc3",
            shuffle ? "shuffle" : "",
            deflateLevel,
            lenOut, lenOut / lenIn, totalBytes);

    return lenOut;
  }

}
