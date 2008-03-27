package thredds.server.cataloggen;

import thredds.server.config.TdsContext;

import java.io.File;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenContext
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenContext.class );

  private File contentDirectory;
  private File configDirectory;
  private File configFile;
  private File resultDirectory;

  CatGenContext() {}
  public void init( TdsContext tdsContext, String servletName,
                    String catGenConfigDirName, String catGenConfigFileName,
                    String catGenResultCatalogsDirName )
  {
    this.contentDirectory = new File( tdsContext.getContentDirectory(), servletName );
    this.configDirectory = new File( this.contentDirectory, catGenConfigDirName );
    this.configFile = new File( this.configDirectory, catGenConfigFileName);
    this.resultDirectory = new File( this.contentDirectory, catGenResultCatalogsDirName );
  }

  public File getContentDirectory()
  {
    return contentDirectory;
  }

  public File getConfigDirectory()
  {
    return configDirectory;
  }

  public File getConfigFile()
  {
    return configFile;
  }

  public File getResultDirectory()
  {
    return resultDirectory;
  }
}
