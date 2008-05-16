package thredds.server.config;

import org.springframework.util.StringUtils;

import java.io.File;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BasicFileSource implements FileSource
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( BasicFileSource.class );

  public File getFile( String path )
  {
    File file = new File( StringUtils.cleanPath( path ) );
    if ( file.exists())
      return file;
    else
      return null;
  }
}
