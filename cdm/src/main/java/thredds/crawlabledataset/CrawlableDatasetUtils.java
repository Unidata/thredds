package thredds.crawlabledataset;

public class CrawlableDatasetUtils {
  /**
   * Split a path into segments, handles null as empty string.
   * <p/>
   * Results:
   * Path []       splits into 1 segments: []
   * Path [a]      splits into 1 segments: [a]
   * Path [a/]     splits into 1 segments: [a]
   * Path [a/b]    splits into 2 segments: [a][b]
   * Path [a/b/c]  splits into 3 segments: [a][b][c]
   * Path [/]      splits into 0 segments:
   * Path [/a]     splits into 2 segments: [][a]
   * Path [/a/b]   splits into 3 segments: [][a][b]
   * Path [/a/b/c] splits into 4 segments: [][a][b][c]
   * <p/>
   * So, absolute paths are valid if length is >1 and first segment is "".
   * Relative paths are valid if length is >0 and first segment is not "".
   *
   * @param path the path to split.
   * @return String[] containing path segments, as per String.split("/").
   */
  public static String[] getPathSegments( String path) {
    return (path == null ? "" : path).split("/");
  }

  public static String getPath( String[] pathSegments ) {
    if ( pathSegments == null )
      return "";

    int start = 0;
    StringBuilder sb = new StringBuilder();
    if ( isValidAbsolutePath(pathSegments)) {
      if ( pathSegments.length == 0 )
        return "/";
      sb.append( "/").append( pathSegments[1]);
      start = 2;
    } else if ( isValidRelativePath( pathSegments)) {
      sb.append( pathSegments[0]);
      start = 1;
      if ( pathSegments.length == 1 ) return pathSegments[0];
    } else {
      throw new IllegalArgumentException( String.format( "Path segment array [%s] not valid path.", toStringForPathSegments( pathSegments)));
    }
    for ( int i = start; i < pathSegments.length; i++ ) {
      sb.append( "/").append( pathSegments[ i]);
    }
    return sb.toString();
  }

  public static boolean isValidAbsolutePath(String[] pathSegments) {
    if ( pathSegments == null )
      return false;
    if ( pathSegments.length == 0 )
      return true; // Root path ("/")
    return pathSegments.length > 1 && pathSegments[0].isEmpty();
  }

  public static boolean isValidRelativePath(String[] pathSegments) {
    return pathSegments != null && pathSegments.length > 0 && !pathSegments[0].isEmpty();
  }

  /**
   * Given a relative path as an array of path segments (see {@link #getPathSegments(String)},
   * return the path relative to the first path segment.
   *
   * I.e., drop the first segmentRejoin the given path segments after dropping the first path segment.
   *
   * @param pathSegments a string array of a relative path as returned by getPathSegments()
   * @return the path relative to the first path segment.
   */
  public static String stepDownRelativePath(String[] pathSegments) {
    if ( !CrawlableDatasetUtils.isValidRelativePath(pathSegments))
      throw new IllegalArgumentException( "Path segments not a valid relative path.");
    if ( pathSegments.length < 2 )
      throw new IllegalArgumentException( "Number of path segments must be > 1.");

    StringBuilder sb = new StringBuilder();
    for ( int i = 1; i < pathSegments.length - 1; i++ ) {
      sb.append( pathSegments[i]).append("/");
    }
    sb.append( pathSegments[pathSegments.length - 1]);
    return sb.toString();
  }

  public static String toStringForPathSegments( String[] pathSegments ) {
    StringBuilder sb = new StringBuilder();
    if ( isValidAbsolutePath( pathSegments)) {
      sb.append( "Absolute: ");
    } else if ( isValidRelativePath( pathSegments)) {
      sb.append( "Relative: ");
    } else {
      sb.append( "Invalid: ");
      if ( pathSegments == null )
        return sb.append( "null").toString();
    }
    for ( String curPathSegment : pathSegments ) {
      sb.append( "[").append( curPathSegment).append("]");
    }
    return sb.toString();
  }

  public static String toStringForPathSegments( String path) {
    String[] pathSegments = getPathSegments(path);
    StringBuilder sb = new StringBuilder();
    sb.append( String.format( "Path [%s] as %d segments: ", path, pathSegments.length));
    for ( String pathSegment : pathSegments) {
      sb.append("[").append( pathSegment).append("]");
    }
    sb.append( " - valid abs[").append( isValidAbsolutePath(pathSegments))
        .append("] rel[").append( isValidRelativePath(pathSegments)).append("]");
    return sb.toString();
  }


}