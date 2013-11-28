package thredds.crawlabledataset.mock;

import thredds.crawlabledataset.CrawlableDatasetUtils;

/**
 * _MORE_
 *
 * @author edavis
 */
public class MockCrawlableDatasetTreeBuilderUtils {
  public static MockCrawlableDataset buildCrDsTree( String basePath, String[] descendantPaths) {
    MockCrawlableDatasetTreeBuilder builder
        = new MockCrawlableDatasetTreeBuilder( basePath, true );
    for ( String curDescendantPath : descendantPaths ) {
      String[] pathSegments = CrawlableDatasetUtils.getPathSegments( curDescendantPath);
      if ( ! CrawlableDatasetUtils.isValidRelativePath( pathSegments))
        throw new IllegalArgumentException( String.format( "Current descendant path [%s] not valid relative path.", curDescendantPath));

      addDescendant( builder, pathSegments);
    }

    return builder.build();
  }

  private static void addDescendant( MockCrawlableDatasetTreeBuilder builder,
                                     String[] descendantPathSegments ) {
    if ( ! CrawlableDatasetUtils.isValidRelativePath( descendantPathSegments)) {
      throw new IllegalArgumentException( String.format( "Not a valid relative path [%s].", (Object []) descendantPathSegments));
    }
    for ( String curPathSegment : descendantPathSegments ) {
      String curFullPath = builder.moveDown( curPathSegment );
      if ( curFullPath != null )
        continue;
      else {
        builder.addChild( curPathSegment, curPathSegment.startsWith( "C" ) );
        builder.moveDown( curPathSegment);
      }
    }
    for ( int i = 0; i < descendantPathSegments.length; i++ )
      builder.moveUp();
  }

//  private static void addDescendantsForEachPathSegment( String[] pathSegments, int curIndex ) {
//
//  }
}
