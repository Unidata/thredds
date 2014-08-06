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
