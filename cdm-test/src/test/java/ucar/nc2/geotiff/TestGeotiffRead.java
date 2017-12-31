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

package ucar.nc2.geotiff;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * reading geotiff
 *
 * @author caron
 * @since 7/22/2014
 */
@RunWith(Parameterized.class)
public class TestGeotiffRead {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  static public File topdir = new File(TestDir.cdmUnitTestDir + "/formats/geotiff/");

  // Even if this class is being excluded due to the NeedsCdmUnitTest annotation, JUnit still calls this method.
  // So, it mustn't throw an exception. Instead, when cdmUnitTest/ isn't available, it'll return an empty list.
  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.addAll(getAllFilesInDirectory(topdir, null));
    return result;
  }

  // If cdmUnitTest/ is available, we should not be getting an empty list of files.
  @Category(NeedsCdmUnitTest.class)
  public static class GetAllFilesTest {
    @Test
    public void notEmpty() {
      assert !getTestParameters().isEmpty() : "No test datasets found in " + topdir;
    }
  }

  String filename;

  public TestGeotiffRead(String filename) {
    this.filename = filename;
  }

  // Will not run when we have an empty list of parameters, and thus cannot be used to indicate failure to find the
  // expected datasets. That's why we need GetAllFilesTest.
  @Test
  public void testRead() throws IOException {
    try (GeoTiff geotiff = new GeoTiff(filename)) {
      geotiff.read();
      StringWriter sw = new StringWriter();
      geotiff.showInfo(new PrintWriter(sw));
      logger.debug(sw.toString());

      IFDEntry tileOffsetTag = geotiff.findTag(Tag.TileOffsets);
      if (tileOffsetTag != null) {
        int tileOffset = tileOffsetTag.value[0];
        IFDEntry tileSizeTag = geotiff.findTag(Tag.TileByteCounts);
        int tileSize = tileSizeTag.value[0];
        logger.debug("tileOffset={} tileSize={}", tileOffset, tileSize);

      } else {
        IFDEntry stripOffsetTag = geotiff.findTag(Tag.StripOffsets);
        if (stripOffsetTag != null) {
          int stripOffset = stripOffsetTag.value[0];
          IFDEntry stripSizeTag = geotiff.findTag(Tag.StripByteCounts);
          if (stripSizeTag == null) throw new IllegalStateException();
          int stripSize = stripSizeTag.value[0];
          logger.debug("stripOffset={} stripSize={}", stripOffset, stripSize);
        }
      }
    }
  }

  /**
   * Returns all of the files in {@code topDir} that satisfy {@code filter}.
   *
   * @param topDir  a directory.
   * @param filter  a file filter.
   * @return  the files. An empty list will be returned if {@code topDir == null || !topDir.exists()}.
   */
  private static List<Object[]> getAllFilesInDirectory(File topDir, FileFilter filter) {
    if (topDir == null || !topDir.exists()) {
      return Collections.emptyList();
    }

    List<File> files = new ArrayList<>();

    for (File f : topDir.listFiles()) {
      if (filter != null && !filter.accept(f)) continue;
      files.add( f);
    }
    Collections.sort(files);

    List<Object[]> result = new ArrayList<>();
    for (File f : files) {
      result.add(new Object[] {f.getAbsolutePath()});
      logger.debug("{}", f.getAbsolutePath());
    }

    return result;
  }
}
