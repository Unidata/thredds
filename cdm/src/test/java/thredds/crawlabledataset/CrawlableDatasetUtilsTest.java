package thredds.crawlabledataset;

import org.junit.Test;
import static org.junit.Assert.*;

public class CrawlableDatasetUtilsTest {

  @Test
  public void checkNullEmptyPath() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( null);
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));

    pathSegments = CrawlableDatasetUtils.getPathSegments( "");
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromNullPath() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( null);
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromEmptyPath() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "");
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test
  public void checkSlashOnly() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/");
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromSlashOnly() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/");
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test
  public void checkSingleSegmentRelative() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "a");
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertTrue( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromSingleSegmentRelative() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "a");
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test
  public void checkTwoSegmentsRelative() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "a/b");
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertTrue( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "b", CrawlableDatasetUtils.stepDownRelativePath(pathSegments));
  }

  @Test
  public void checkThreeSegmentsRelative() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "a/b/c");
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertTrue( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "b/c", CrawlableDatasetUtils.stepDownRelativePath(pathSegments));
  }

  @Test
  public void checkFourSegmentsRelative() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "a/b/c/d");
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertTrue( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "b/c/d", CrawlableDatasetUtils.stepDownRelativePath(pathSegments));
  }

  @Test
  public void checkFourSegmentsRelativeWithTrailingSlash() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "a/b/c/d/");
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertTrue( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "b/c/d", CrawlableDatasetUtils.stepDownRelativePath(pathSegments));
  }

  @Test
  public void checkSingleSegmentAbsolute() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/a");
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromSingleSegmentAbsolute() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/a");
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test
  public void checkSingleSegmentAbsoluteWithTrailingSlash() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/a/");
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromSingleSegmentAbsoluteWithTrailingSlash() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/a/");
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test
  public void checkTwoSegmentsAbsolute() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/a/b");
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    //assertEquals( "b", CrawlableDatasetUtils.stepDownRelativePath(pathSegments));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromTwoSegmentAbsolute() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/a/b");
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test
  public void checkThreeSegmentsAbsolute() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/a/b/c");
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
  }

  @Test
  public void checkFourSegmentsAbsolute() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/a/b/c/d");
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
  }

  @Test
  public void checkFourSegmentsAbsoluteWithTrailingSlash() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/a/b/c/d/");
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
  }
}
