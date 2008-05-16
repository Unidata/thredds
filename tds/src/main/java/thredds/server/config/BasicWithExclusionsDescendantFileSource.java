package thredds.server.config;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BasicWithExclusionsDescendantFileSource
        implements DescendantFileSource
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( BasicWithExclusionsDescendantFileSource.class );

  private final BasicDescendantFileSource root;
  private final List<BasicDescendantFileSource> exclusions;

  public BasicWithExclusionsDescendantFileSource( String rootDirectoryPath,
                                                  List<String> exclusions)
  {
    this.root = new BasicDescendantFileSource( rootDirectoryPath );
    if ( exclusions == null )
      throw new IllegalArgumentException( "Exclusion list must not be null.");
    if ( exclusions.isEmpty())
      throw new IllegalArgumentException( "Exclusion list must not be empty." );

    List<BasicDescendantFileSource> list = new ArrayList<BasicDescendantFileSource>();
    BasicDescendantFileSource bdfs;
    for ( String curDfsRdp : exclusions )
    {
      bdfs = (BasicDescendantFileSource) this.root.getDescendant( curDfsRdp );
      if ( bdfs == null )
        throw new IllegalArgumentException( "Exclusion [" + curDfsRdp + "] was null, not relative, or not descendant of root.");

      list.add( bdfs );
    }
    this.exclusions = list;
  }
  public BasicWithExclusionsDescendantFileSource( File rootDirectory,
                                                  List<String> exclusions)
  {
    this( rootDirectory.getPath(), exclusions);
  }

  public File getFile( String path )
  {
    File file = this.root.getFile( path );
    for ( BasicDescendantFileSource curBdfs : this.exclusions )
    {
      if ( curBdfs.isDescendant( file ) )
        return null;
    }
    return file;
  }

  public DescendantFileSource getDescendant( String relativePath )
  {
    return null;
  }

  public File getRootDirectory()
  {
    return null;
  }

  public String getRootDirectoryPath()
  {
    return null;
  }

  public boolean isDescendant( File file )
  {
    return false;
  }

  public boolean isDescendant( String filePath )
  {
    return false;
  }

  public String getRelativePath( File file )
  {
    return null;
  }

  public String getRelativePath( String filePath )
  {
    return null;
  }

}
