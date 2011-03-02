package ucar.nc2;

import junit.framework.TestCase;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 2/28/11
 */
public class TestAllSlow extends TestCase {
  static int countGood = 0;
  static int countFail = 0;
  static int countTotal = 0;
  static boolean verbose = true;

  public TestAllSlow(String name) {
    super(name);
  }

   class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      countTotal++;
      if (pathname.getName().endsWith(".gbx8")) return false;
      if (pathname.getName().endsWith(".ncx")) return false;
      if (pathname.getName().endsWith(".xml")) return false;
      if (pathname.getName().endsWith(".java")) return false;
      return true;
    }
  }

  public void testAllFormat() throws IOException {
    openAllInDir("Q:/cdmUnitTest/formats", new MyFileFilter());
    int countExclude = countTotal - countGood - countFail;
    System.out.printf("Good=%d File=%d Exclude=%d%n", countGood, countFail, countExclude);
  }

  // these are fairly complete hdf4 files from nsidc
  public void testHdf4() throws IOException {
    openAllInDir("F:/data/formats/hdf4", new MyFileFilter());
    int countExclude = countTotal - countGood - countFail;
    System.out.printf("Good=%d Fail=%d Exclude=%d%n", countGood, countFail, countExclude);
  }

  public static void openAllInDir(String dirName, FileFilter ff) throws IOException {
    if (verbose) System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID "+dirName);
      return;
    }
    List<File> flist = Arrays.asList(allFiles);
    Collections.sort(flist);

    for (File f : flist) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if ((ff == null) || ff.accept(f)) {
        NetcdfFile ncfile = null;
        try {
          ncfile = NetcdfDataset.openFile(name, null);
          if (verbose) System.out.printf("  GOOD on %s == %s%n", name, ncfile.getFileTypeId());
          countGood++;
        } catch (Throwable t) {
          System.out.printf("  FAIL on %s == %s%n", name, t.getMessage());
          countFail++;
        } finally {
          if (ncfile != null) ncfile.close();
        }
      }
    }

    for (File f : flist) {
      if (f.isDirectory() && (!f.getName().equals("problem")))
        openAllInDir(f.getAbsolutePath(), ff);
    }

  }

}
