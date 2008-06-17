package thredds.server.config;

import thredds.util.filesource.FileSource;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StandardInitializer
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( StandardInitializer.class );

  private final List<String> rootConfigCatalogList;
  private final FileSource configCatalogFileSource;

  public StandardInitializer( List<String> rootConfigCatalogList,
                              FileSource configCatalogFileSource )
  {
    if ( rootConfigCatalogList == null || rootConfigCatalogList.isEmpty())
      throw new IllegalArgumentException( "Config catalog list must not be null or empty.");
    if ( configCatalogFileSource == null )
      throw new IllegalArgumentException( "Config catalog FileSource must not be null." );

    this.rootConfigCatalogList = rootConfigCatalogList;
    this.configCatalogFileSource = configCatalogFileSource;
  }

  
}
