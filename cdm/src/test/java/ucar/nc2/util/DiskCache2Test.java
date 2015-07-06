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
  private static Path nearest;

  @BeforeClass
  public static void beforeClass() throws IOException {
    nearest = Files.createTempDirectory("nearest");
  }

  @AfterClass
  public static void afterClass() throws IOException {
    Files.delete(nearest);
  }

  @Test
  public void canWriteDir() {
    File file = nearest.toFile();

    Assert.assertTrue(file.exists());
    Assert.assertTrue(DiskCache2.canWrite(file));
  }

  // On Windows, can't make a directory read-only: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6728842
  // So, we can't have this test:
  // @Test public void cantWriteDir() { }

  @Test
  public void canWriteFile() throws IOException {
    File file = Files.createTempFile(nearest, "temp", null).toFile();

    try {
      Assert.assertTrue(file.exists());
      Assert.assertTrue(DiskCache2.canWrite(file));
    } finally {
      file.delete();
    }
  }

  @Test
  public void cantWriteFile() throws IOException {
    File file = Files.createTempFile(nearest, "temp", null).toFile();

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
    File file = Paths.get(nearest.toString(), "non-existent-file.txt").toFile();

    Assert.assertFalse(file.exists());
    Assert.assertTrue(DiskCache2.canWrite(file));
  }

  @Test
  public void cantWriteNonExistentFileWithNonExistentParent() {
    File file = Paths.get(nearest.toString(), "A", "B", "C", "non-existent-file.txt").toFile();

    Assert.assertFalse(file.exists());
    Assert.assertFalse(file.getParentFile().exists());
    Assert.assertFalse(DiskCache2.canWrite(file));
  }
}
