package ucar.nc2.util;

import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author cwardgar
 * @since 2015/06/23
 */
public class DiskCache2Test {

  private static Path testCacheDir;
  private static Path dirForStaticTests;
  private static DiskCache2 diskCache;

  @BeforeClass
  public static void beforeClass() throws IOException {
    String cacheName = DiskCache2Test.class.getSimpleName();
    dirForStaticTests = Files.createTempDirectory(cacheName + "-static-tests");
    testCacheDir = Files.createTempDirectory(cacheName);

    try {
      Files.deleteIfExists(testCacheDir);
    } catch (IOException e) {
      throw new IOException(String.format("Unable to remove existing test cache directory %s - test setup failed.",
          testCacheDir), e);
    }
    Assert.assertFalse(testCacheDir.toFile().exists());
    // create a disk cache for the test to use
    diskCache = new DiskCache2(testCacheDir.toString(), false, 1, 5);
  }

  @Test
  public void canWriteDir() {
    File file = dirForStaticTests.toFile();

    Assert.assertTrue(file.exists());
    Assert.assertTrue(DiskCache2.canWrite(file));
  }

  // On Windows, can't make a directory read-only: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6728842
  // So, we can't have this test:
  // @Test public void cantWriteDir() { }

  @Test
  public void canWriteFile() throws IOException {
    File file = Files.createTempFile(dirForStaticTests, "temp", null).toFile();

    try {
      Assert.assertTrue(file.exists());
      Assert.assertTrue(DiskCache2.canWrite(file));
    } finally {
      file.delete();
    }
  }

  @Test
  public void cantWriteFile() throws IOException {
    File file = Files.createTempFile(dirForStaticTests, "temp", null).toFile();

    try {
      Assert.assertTrue(file.exists());
      Assert.assertTrue(file.setWritable(false));
      Assert.assertFalse(DiskCache2.canWrite(file));
    } finally {
      file.delete();
    }
  }

  @Test
  public void canWriteNonExistentFileWithExistentParent() {
    File file = Paths.get(dirForStaticTests.toString(), "non-existent-file.txt").toFile();

    Assert.assertFalse(file.exists());
    Assert.assertTrue(DiskCache2.canWrite(file));
  }

  @Test
  public void cantWriteNonExistentFileWithNonExistentParent() {
    File file = Paths.get(dirForStaticTests.toString(), "A", "B", "C", "non-existent-file.txt").toFile();

    Assert.assertFalse(file.exists());
    Assert.assertFalse(file.getParentFile().exists());
    Assert.assertFalse(DiskCache2.canWrite(file));
  }

  @Test
  public void checkUniqueFileNames() throws IOException {
    final String prefix  = "pre-";
    final String suffix  = "-suf";

    File first = diskCache.createUniqueFile(prefix, suffix);
    File second = diskCache.createUniqueFile(prefix, suffix);

    // files do not exist yet
    Assert.assertFalse(first.exists());
    Assert.assertFalse(second.exists());

    // make the files
    first.createNewFile();
    second.createNewFile();

    // files should exist now
    Assert.assertTrue(first.exists());
    Assert.assertTrue(second.exists());

    // make sure they start with prefix
    Assert.assertTrue(first.getName().startsWith(prefix));
    Assert.assertTrue(second.getName().startsWith(prefix));

    // make sure they end with suffix
    Assert.assertTrue(first.getName().endsWith(suffix));
    Assert.assertTrue(second.getName().endsWith(suffix));

    // make sure they are different files
    Assert.assertNotEquals(first.getAbsolutePath(), second.getAbsolutePath());
  }

  @AfterClass
  public static void afterClass() throws IOException {
    diskCache.exit();
  }
}
