package thredds.server.config;

import org.springframework.util.StringUtils;

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
    if ( exclusions == null )
      throw new IllegalArgumentException( "Exclusion list must not be null.");
    if ( exclusions.isEmpty())
      throw new IllegalArgumentException( "Exclusion list must not be empty." );

    this.root = new BasicDescendantFileSource( rootDirectoryPath );
    this.exclusions = initExclusions( exclusions );
  }
  public BasicWithExclusionsDescendantFileSource( File rootDirectory,
                                                  List<String> exclusions)
  {
    if ( exclusions == null )
      throw new IllegalArgumentException( "Exclusion list must not be null." );
    if ( exclusions.isEmpty() )
      throw new IllegalArgumentException( "Exclusion list must not be empty." );

    this.root = new BasicDescendantFileSource( rootDirectory );
    this.exclusions = initExclusions( exclusions );
  }

  private List<BasicDescendantFileSource> initExclusions( List<String> exclusions )
  {
    List<BasicDescendantFileSource> list = new ArrayList<BasicDescendantFileSource>();
    BasicDescendantFileSource bdfs;
    for ( String curDfsRdp : exclusions )
    {
      if ( curDfsRdp == null )
        throw new IllegalArgumentException( "Exclusion list may not contain null items.");
      bdfs = (BasicDescendantFileSource) this.root.getDescendant( curDfsRdp );
      if ( bdfs == null )
        throw new IllegalArgumentException( "Exclusion [" + curDfsRdp + "] was null, not relative, or not descendant of root." );

      list.add( bdfs );
    }
    return list;
  }

  public File getFile( String path )
  {
    File file = this.root.getFile( path );
    for ( BasicDescendantFileSource curBdfs : this.exclusions )
    {
      if ( curBdfs.getRootDirectory().equals( file)
           || curBdfs.isDescendant( file ) )
        return null;
    }
    return file;
  }

  public DescendantFileSource getDescendant( String relativePath )
  {
    DescendantFileSource dfs = this.root.getDescendant( relativePath );
    for ( BasicDescendantFileSource curBdfs : this.exclusions )
    {
      if ( curBdfs.getRootDirectory().equals( dfs.getRootDirectory())
           || curBdfs.isDescendant( dfs.getRootDirectory() ) )
        return null;
    }
    return dfs;
  }

  public File getRootDirectory()
  {
    return this.root.getRootDirectory();
  }

  public String getRootDirectoryPath()
  {
    return this.root.getRootDirectoryPath();
  }

  public boolean isDescendant( File file )
  {
    if ( ! this.root.isDescendant( file ) )
      return false;

    for ( BasicDescendantFileSource curBdfs : this.exclusions )
    {
      if ( curBdfs.getRootDirectory().equals( getCleanAbsoluteFile( file ) )
           || curBdfs.isDescendant( file ) )
        return false;
    }
    return true;
  }

  public boolean isDescendant( String filePath )
  {
    return filePath == null ? false : isDescendant( new File( filePath ) );
  }

  public String getRelativePath( File file )
  {
    for ( BasicDescendantFileSource curBdfs : this.exclusions )
    {
      if ( curBdfs.getRootDirectory().equals( getCleanAbsoluteFile( file ))
           || curBdfs.isDescendant( file ) )
        return null;
    }
    return this.root.getRelativePath( file );
  }

  public String getRelativePath( String filePath )
  {
    if ( filePath == null)
      return null;
    return getRelativePath( new File( filePath) );
  }

  private File getCleanAbsoluteFile( File file)
  {
    return new File( StringUtils.cleanPath( file.getAbsolutePath() ).trim() );
  }
}
