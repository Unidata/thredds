package thredds.server.config;

import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogImpl;

import java.io.File;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StaticCatalog
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( StaticCatalog.class );

  private final String path;
  private final File catalogFile;
  private final InvCatalog catalog;

  private final StaticCatalog parent;
  private final List<StaticCatalog> children;

  public StaticCatalog( String path, File catalogFile, InvCatalog catalog,
                        StaticCatalog parent, List<StaticCatalog> children )
  {
    if ( path == null || path.equals( "") )
      throw new IllegalArgumentException( "Path may not be null or empty.");
    //if ( catalogFile == null || ! catalogFile.exists())
      //((InvCatalogImpl)catalog).

    this.path = path;
    this.catalogFile = catalogFile;
    this.catalog = catalog;
    this.parent = parent;
    this.children = children;
  }
}
