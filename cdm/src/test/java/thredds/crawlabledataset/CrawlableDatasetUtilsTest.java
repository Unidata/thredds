package thredds.crawlabledataset;

import org.junit.Test;
import static org.junit.Assert.*;

public class CrawlableDatasetUtilsTest {

  @Test
  public void checkNullEmptyPath() {
    String path = null;
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));

    path = "";
    pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath(pathSegments));
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
    String path = "/";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals("Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromSlashOnly() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/");
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test
  public void checkSingleSegmentRelative() {
    String path = "a";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments(path);
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertTrue(CrawlableDatasetUtils.isValidRelativePath(pathSegments));
    assertEquals("Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromSingleSegmentRelative() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "a");
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test
  public void checkTwoSegmentsRelative() {
    String path = "a/b";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertTrue(CrawlableDatasetUtils.isValidRelativePath(pathSegments));
    assertEquals("b", CrawlableDatasetUtils.stepDownRelativePath(pathSegments));
    assertEquals( "Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  @Test
  public void checkThreeSegmentsRelative() {
    String path = "a/b/c";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertTrue( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "b/c", CrawlableDatasetUtils.stepDownRelativePath( pathSegments));
    assertEquals( "Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  @Test
  public void checkFourSegmentsRelative() {
    String path = "a/b/c/d";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertTrue( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "b/c/d", CrawlableDatasetUtils.stepDownRelativePath( pathSegments));
    assertEquals( "Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  @Test
  public void checkFourSegmentsRelativeWithTrailingSlash() {
    String path = "a/b/c/d/";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertFalse( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertTrue( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "b/c/d", CrawlableDatasetUtils.stepDownRelativePath( pathSegments));
    assertEquals( "Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  @Test
  public void checkSingleSegmentAbsolute() {
    String path = "/a";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromSingleSegmentAbsolute() {
    String path = "/a";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test
  public void checkSingleSegmentAbsoluteWithTrailingSlash() {
    String path = "/a/";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromSingleSegmentAbsoluteWithTrailingSlash() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/a/");
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test
  public void checkTwoSegmentsAbsolute() {
    String path = "/a/b";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    //assertEquals( "b", CrawlableDatasetUtils.stepDownRelativePath(pathSegments));
    assertEquals( "Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStepDownFromTwoSegmentAbsolute() {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( "/a/b");
    CrawlableDatasetUtils.stepDownRelativePath( pathSegments);
  }

  @Test
  public void checkThreeSegmentsAbsolute() {
    String path = "/a/b/c";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  @Test
  public void checkFourSegmentsAbsolute() {
    String path = "/a/b/c/d";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  @Test
  public void checkFourSegmentsAbsoluteWithTrailingSlash() {
    String path = "/a/b/c/d/";
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( path);
    assertTrue( CrawlableDatasetUtils.isValidAbsolutePath( pathSegments));
    assertFalse( CrawlableDatasetUtils.isValidRelativePath( pathSegments));
    assertEquals( "Reformulated path", removeAnyTrailingSlash( path), CrawlableDatasetUtils.getPath(pathSegments));
  }

  private static String removeAnyTrailingSlash( String path) {
    if ( path == null || path.length() < 2 || ! path.endsWith( "/"))
      return path;
    return path.substring( 0, path.length() - 1 );
  }
}
