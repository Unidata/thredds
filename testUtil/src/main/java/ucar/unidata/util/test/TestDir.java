package ucar.unidata.util.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.AliasTranslator;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.*;

/**
 * Manage the test data directories and servers.
 *
 * @author caron
 * @since 3/23/12
 *
 * <p>
 * <table>
 * <tr><th colspan="3">-D Property Names
 * <tr><th>Static Variable<th>Property Name(s)<th>Description
 * <tr><td>testdataDirPropName<td>unidata.testdata.path
 *     <td>Property name for the path to the Unidata test data directory,
 *         e.g unidata.testdata.path=/share/testdata
 * <tr><td>remoteTestServerPropName<td>remotetest
 *     <td>Property name for the hostname of the C-library remote test server.
 * </table>
 * <p>
 * <table>
 * <tr><th colspan="4">Computed Paths
 * <tr><th>Static Variable<th>Property Name(s) (-d)<th>Default Value<th>Description
 * <tr><td>cdmUnitTestDir<td>NA<td>NA
 *     <td>New test data directory. Do not put temporary files in here.
 *         Migrate all test data here eventually.
 * <tr><td>cdmLocalTestDataDir<td>NA<td>../cdm/src/test/data
 *     <td>Level 1 test data directory (distributed with code and MAY be used in Unidata nightly testing).
 * <tr><td>remoteTestServer<td>remotetestserver<td>remotetest.unidata.ucar.edu
 *     <td>The hostname of the test server for doing C library remote tests.
 * </table>
 *
 */
public class TestDir {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Property name for the path to the Unidata test data directory, e.g "unidata.testdata.path=/share/testdata".
   */
  private static String testdataDirPropName ="unidata.testdata.path";

  /**
   * Path to the Unidata test data directory.
   */
  public static String testdataDir = null;

  /**
  * New test data directory. do not put temporary files in here. migrate all test data here eventually
   * Unidata "//fileserver/data/testdata2/cdmUnitTest" directory.
   */
  public static String cdmUnitTestDir = null;

  /**
   * Level 1 test data directory (distributed with code and MAY be used in Unidata nightly testing).
   */
  public static String cdmLocalTestDataDir = "../cdm/src/test/data/";

  /**
   * cdm-test data directory (distributed with code but depends on data not in github)
   */
  public static String cdmTestDataDir = "../cdm-test/src/test/data/";

  //////////////////////////////////////////////////////////////////////
  // Various Test Server machines
  //////////////////////////////////////////////////////////////////////

  // Remote Test server(s)

  private static String remoteTestServerPropName = "remotetestserver";

  static public String remoteTestServer = "localhost:8081";

  // DAP 2 Test server (for testing)

  static public String dap2TestServerPropName = "dts";

  static public String dap2TestServer = "localhost:8082";

  // DAP4 Test server (for testing)

  static public String dap4TestServerPropName = "d4ts";

  static public String dap4TestServer = "localhost:8083";

  //////////////////////////////////////////////////

  static {
    testdataDir = System.getProperty(testdataDirPropName);  // Check the system property.

    // Use default paths if needed.
    if (testdataDir == null) {
      testdataDir = "/share/testdata/";
      logger.warn("No '{}' property found; using default value '{}'.", testdataDirPropName, testdataDir );
    }

    // Make sure paths ends with a slash.
    testdataDir = testdataDir.replace('\\','/'); //canonical
    if (!testdataDir.endsWith("/"))
      testdataDir += "/";

    cdmUnitTestDir = testdataDir + "cdmUnitTest/";

    File file = new File(cdmUnitTestDir);
    if (!file.exists() || !file.isDirectory()) {
      logger.warn("cdmUnitTest directory does not exist: {}", file.getAbsolutePath());
    }

    // Initialize various server values

    String rts = System.getProperty(remoteTestServerPropName);
    if(rts != null && rts.length() > 0)
	    remoteTestServer = rts;

    String dts = System.getProperty(dap2TestServerPropName);
    if(dts != null && dts.length() > 0)
	    dap2TestServer = dts;

    String d4ts = System.getProperty(dap4TestServerPropName);
    if(d4ts != null && d4ts.length() > 0)
      	dap4TestServer = d4ts;

    AliasTranslator.addAlias("${cdmUnitTest}", cdmUnitTestDir);
  }

  public static NetcdfFile open(String filename) throws IOException {
    logger.debug("**** Open {}", filename);
    NetcdfFile ncfile = NetcdfFile.open(filename, null);
    logger.debug("open {}", ncfile);

    return ncfile;
  }

  public static NetcdfFile openFileLocal(String filename) throws IOException {
    return open(TestDir.cdmLocalTestDataDir + filename);
  }

  static public long checkLeaks() {
    if (RandomAccessFile.getOpenFiles().size() > 0) {
      logger.debug("RandomAccessFile still open:");
      for (String filename : RandomAccessFile.getOpenFiles()) {
        logger.debug("  open= {}", filename);
      }
    } else {
      logger.debug("RandomAccessFile: no leaks");
    }

    logger.debug("RandomAccessFile: count open={}, max={}",
            RandomAccessFile.getOpenFileCount(), RandomAccessFile.getMaxOpenFileCount());
    return RandomAccessFile.getOpenFiles().size();
  }

  ////////////////////////////////////////////////

  public interface Act {
    /**
     * @param filename file to act on
     * @return count
     * @throws IOException  on IO error
     */
    int doAct( String filename) throws IOException;
  }

  public static class FileFilterFromSuffixes implements FileFilter {
    String[] suffixes;
    public FileFilterFromSuffixes(String suffixes) {
      this.suffixes = suffixes.split(" ");
    }

    @Override
    public boolean accept(File file) {
      for (String s: suffixes)
        if (file.getPath().endsWith(s)) return true;
      return false;
    }
  }

  public static class FileFilterNoWant implements FileFilter {
    String[] suffixes;
    public FileFilterNoWant(String suffixes) {
      this.suffixes = suffixes.split(" ");
    }

    @Override
    public boolean accept(File file) {
      for (String s: suffixes)
        if (file.getPath().endsWith(s)) return false;
      return true;
    }
  }

  /**
   * Call act.doAct() of each file in dirName passing
   * @param dirName
   * @param ff
   * @param act
   * @return
   * @throws IOException
   */
  public static int actOnAll(String dirName, FileFilter ff, Act act) throws IOException {
    return actOnAll( dirName, ff, act, true);
  }

  public static int actOnAllParameterized(String dirName, FileFilter ff, Collection<Object[]> filenames) throws IOException {
    return actOnAll( dirName, ff, new ListAction(filenames), true);
  }

  static class ListAction implements Act {
    Collection<Object[]> filenames;

    ListAction(Collection<Object[]> filenames) {
      this.filenames = filenames;
    }

    @Override
    public int doAct(String filename) throws IOException {
      filenames.add( new Object[] {filename} );
      return 0;
    }
  }

  /**
   * @param dirName recurse into this directory
   * @param ff for files that pass this filter, may be null
   * @param act perform this acction
   * @param recurse recurse into subdirectories
   * @return count
   * @throws IOException on IO error
   */
  public static int actOnAll(String dirName, FileFilter ff, Act act, boolean recurse) throws IOException {
    int count = 0;

    logger.debug("---------------Reading directory {}", dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      logger.debug("---------------INVALID {}", dirName);
      throw new FileNotFoundException("Cant open "+dirName);
    }

    List<File> flist = Arrays.asList(allFiles);
    Collections.sort(flist);

    for (File f : flist) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude") ) {
        name = StringUtil2.substitute(name, "\\", "/");
        logger.debug("----acting on file {}", name);
        count += act.doAct(name);
      }
    }

    if (!recurse) return count;

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude")&& !f.getName().equals("problem"))
        count += actOnAll(f.getAbsolutePath(), ff, act);
    }

    return count;
  }

  ////////////////////////////////////////////////////////////////////////////

  public static void readAll(String filename) throws IOException {
    ReadAllVariables act = new ReadAllVariables();
    act.doAct(filename);
  }

  private static class ReadAllVariables implements Act {
    @Override
    public int doAct(String filename) throws IOException {
      try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
        return readAllData(ncfile);
      }
    }
  }

  static int max_size = 1000 * 1000 * 10;
  static Section makeSubset(Variable v) throws InvalidRangeException {
    int[] shape = v.getShape();
    shape[0] = 1;
    Section s = new Section(shape);
    long size = s.computeSize();
    shape[0] = (int) Math.max(1, max_size / size);
    return new Section(shape);
  }

  static public int readAllData(NetcdfFile ncfile) throws IOException {
    logger.debug("------Reading ncfile {}", ncfile.getLocation());
    try {
      for (Variable v : ncfile.getVariables()) {
        if (v.getSize() > max_size) {
          Section s = makeSubset(v);
          logger.debug("  Try to read variable {} size={} section={}", v.getNameAndDimensions(), v.getSize(), s);
          v.read(s);
        } else {
          logger.debug("  Try to read variable {} size={}", v.getNameAndDimensions(), v.getSize());
          v.read();
        }
      }

      return 1;
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
  }
}
