/* Copyright */
package ucar.nc2.dataset;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCSBuilder;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestConventionFeatureTypes {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static String base = TestDir.cdmUnitTestDir + "conventions/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{"atd", FeatureType.GRID});
    result.add(new Object[]{"atd-radar", FeatureType.GRID});
    result.add(new Object[]{"avhrr", FeatureType.GRID});
    result.add(new Object[]{"awips", FeatureType.GRID});
    result.add(new Object[]{"cedric", FeatureType.GRID});
    result.add(new Object[]{"cf", FeatureType.GRID});
    result.add(new Object[]{"cf/dsc", FeatureType.POINT});
    result.add(new Object[]{"cfradial", FeatureType.RADIAL});
    result.add(new Object[]{"coards", FeatureType.GRID});
    result.add(new Object[]{"csm", FeatureType.GRID});
    result.add(new Object[]{"gdv", FeatureType.GRID});
    result.add(new Object[]{"gief", FeatureType.GRID});
    result.add(new Object[]{"ifps", FeatureType.GRID});
    result.add(new Object[]{"m3io", FeatureType.GRID});
    result.add(new Object[]{"mars", FeatureType.GRID});
    //result.add(new Object[]{"mm5", FeatureType.GRID});   // Dataset lacks X and Y axes.
    result.add(new Object[]{"nuwg", FeatureType.GRID});
    result.add(new Object[]{"wrf", FeatureType.GRID});
    result.add(new Object[]{"zebra", FeatureType.GRID});

    return result;
  }

  FeatureType type;
  File dir;

  public TestConventionFeatureTypes(String dir, FeatureType type) {
    this.type = type;
    this.dir = new File(base + dir);
  }

  @Test
  public void testFeatureDatasets() throws IOException {
    for (File f : getAllFilesInDirectoryStandardFilter(dir)) {
      logger.debug("Open FeatureDataset {}", f.getPath());
      try (FeatureDataset fd = FeatureDatasetFactoryManager.open(type, f.getPath(), null, new Formatter())) {
        Assert.assertNotNull(f.getPath(), fd);
        if (type == FeatureType.GRID)
          Assert.assertTrue(f.getPath(), fd.getFeatureType().isCoverageFeatureType());
        else if (type == FeatureType.POINT)
          Assert.assertTrue(f.getPath(), fd.getFeatureType().isPointFeatureType());
      }
    }
  }

  @Test
  public void testCoverageDatasets() throws IOException {
    if (type != FeatureType.GRID) {
      return;
    }
    for (File f : getAllFilesInDirectoryStandardFilter(dir)) {
      logger.debug("Open CoverageDataset {}", f.getPath());
      try (NetcdfDataset ds = NetcdfDataset.openDataset(f.getPath())) {
        DtCoverageCSBuilder builder = DtCoverageCSBuilder.classify(ds, new Formatter());
        Assert.assertNotNull(builder);
      }
    }
  }

  private static List<File> getAllFilesInDirectoryStandardFilter(File topDir) {
    if (topDir == null || !topDir.exists()) {
      return Collections.emptyList();
    }

    if ((topDir.getName().equals("exclude")) || (topDir.getName().equals("problem"))) {
      return Collections.emptyList();
    }

    // get list of files
    File[] fila = topDir.listFiles();
    if (fila == null) {
      return Collections.emptyList();
    }

    List<File> files = new ArrayList<>();
    for (File f : fila) {
      if (!f.isDirectory()) {
        files.add(f);
      }
    }

    // eliminate redundant files
    // ".Z", ".zip", ".gzip", ".gz", or ".bz2"
    if (files.size() > 0) {
      Collections.sort(files);
      ArrayList<File> files2 = new ArrayList<>(files);

      File prev = null;
      for (File f : files) {
        String name = f.getName();
        String stem = stem(name);
        if (name.contains(".gbx") || name.contains(".ncx") || name.endsWith(".xml") || name.endsWith(".pdf") ||
                name.endsWith(".txt") || name.endsWith(".tar")) {
          files2.remove(f);

        } else if (prev != null) {
          if (name.endsWith(".ncml")) {
            if (prev.getName().equals(stem) || prev.getName().equals(stem + ".nc")) {
              files2.remove(prev);
            }
          } else if (name.endsWith(".bz2")) {
            if (prev.getName().equals(stem)) {
              files2.remove(f);
            }
          } else if (name.endsWith(".gz")) {
            if (prev.getName().equals(stem)) {
              files2.remove(f);
            }
          } else if (name.endsWith(".gzip")) {
            if (prev.getName().equals(stem)) {
              files2.remove(f);
            }
          } else if (name.endsWith(".zip")) {
            if (prev.getName().equals(stem)) {
              files2.remove(f);
            }
          } else if (name.endsWith(".Z")) {
            if (prev.getName().equals(stem)) {
              files2.remove(f);
            }
          }
        }
        prev = f;
      }

      return files2;
    }

    return files;
  }

  private static String stem(String name) {
    int pos = name.lastIndexOf('.');
    return (pos > 0) ? name.substring(0, pos) : name;
  }
}
