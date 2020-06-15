package ucar.nc2.util;

import java.util.ArrayList;
import java.util.List;
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

  private static Path dirForStaticTests;
  private static DiskCache2 diskCache;

  private static List<File> filesToDelete = new ArrayList<>();

  @BeforeClass
  public static void beforeClass() throws IOException {
    String cacheName = DiskCache2Test.class.getSimpleName();
    dirForStaticTests = Files.createTempDirectory(cacheName + "-static-tests");
    Path testCacheDir = Files.createTempDirectory(cacheName);

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
    Assert.assertTrue(file.exists());
    filesToDelete.add(file);
    Assert.assertTrue(DiskCache2.canWrite(file));
  }

  @Test
  public void cantWriteFile() throws IOException {
    File file = Files.createTempFile(dirForStaticTests, "temp", null).toFile();
    filesToDelete.add(file);

    Assert.assertTrue(file.exists());

    // make unwritable
    Assert.assertTrue(file.setWritable(false));
    Assert.assertFalse(DiskCache2.canWrite(file));

    // change back to writable so we can clean up
    Assert.assertTrue(file.setWritable(true));
    Assert.assertTrue(DiskCache2.canWrite(file));
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
    final String lockFileExtention = ".reserve";

    // loop a few times to make sure it triggers.
    for (int i = 0; i < 5; i++){
      File first = diskCache.createUniqueFile(prefix, suffix);
      File second = diskCache.createUniqueFile(prefix, suffix);

      // files do not exist yet
      Assert.assertFalse(first.exists());
      Assert.assertFalse(second.exists());

      // make the files
      first.createNewFile();
      second.createNewFile();

      // keep track of files that need to be cleaned up
      filesToDelete.add(first);
      filesToDelete.add(second);

      // files should exist now
      Assert.assertTrue(first.exists());
      Assert.assertTrue(second.exists());

      // lock files should exist as well
      File firstLockFile = Paths.get(first.getCanonicalFile().toString() + lockFileExtention).toFile();
      File secondLockFile = Paths.get(second.getCanonicalFile().toString() + lockFileExtention).toFile();
      Assert.assertTrue(firstLockFile.exists());
      Assert.assertTrue(secondLockFile.exists());
      filesToDelete.add(firstLockFile);
      filesToDelete.add(secondLockFile);

      // make sure they start with prefix
      Assert.assertTrue(first.getName().startsWith(prefix));
      Assert.assertTrue(second.getName().startsWith(prefix));

      // make sure they end with suffix
      Assert.assertTrue(first.getName().endsWith(suffix));
      Assert.assertTrue(second.getName().endsWith(suffix));

      // make sure they are different files
      Assert.assertNotEquals(first.getAbsolutePath(), second.getAbsolutePath());
    }
  }

  @AfterClass
  public static void afterClass() throws IOException {
    for (File f : filesToDelete) {
      Files.deleteIfExists(f.toPath());
    }
    Files.delete(dirForStaticTests);

    diskCache.exit();
    Files.deleteIfExists(Paths.get(diskCache.getRootDirectory()));
  }
}
