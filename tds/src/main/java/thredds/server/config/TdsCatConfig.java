package thredds.server.config;

import thredds.catalog.InvCatalog;

import java.util.List;
import java.util.Map;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TdsCatConfig
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TdsCatConfig.class );

  private final TdsContext tdsContext;
  //private final List<String> catConfigRootList;

  private Map staticCatalogMap;
  private Map datasetRootMap;
  private Map datasetMap;

  public TdsCatConfig( TdsContext tdsContext)
  {
    this.tdsContext = tdsContext;
  }

  public void init()
  {
    doReplace( buildFull() );
  }

  public InvCatalog findStaticCatalog( String catalogPath )
  {
    return null;
  }

  public String findDatasetRoot( String path ) // return ???
  {
    return null;
  }

  public void reinit()
  {
    doReplace( buildFull() );
  }

  public void reinitPartial( String catalogPath )
  {
    doUpdate( buildUpdate( catalogPath) );
  }

  private AllMaps buildFull()
  {
    return new AllMaps();
  }

  private AllMapsUpdate buildUpdate( String catalogPath )
  {
    // Will need some synchronized blocks around section(s) that
    // determine which parts of maps need to be replaced.
    // Or just need to wrap entire update section?
    return new AllMapsUpdate();
  }

  private synchronized void doReplace( AllMaps replace )
  {

  }

  private synchronized void doUpdate( AllMapsUpdate update )
  {

  }

  private static class AllMaps
  {
    Map staticCatalogMap;
    Map datasetRootMap;
    Map datasetMap;
  }

  private static class AllMapsUpdate
  {
    Map catalogsToRemove;
    Map datasetRootsToRemove;
    Map datasetsToRemove;

    Map catalogsToAdd;
    Map datasetRootsToAdd;
    Map datasetsToAdd;
  }
}
