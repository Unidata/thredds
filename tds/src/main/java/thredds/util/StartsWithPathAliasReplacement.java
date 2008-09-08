package thredds.util;

import org.springframework.util.StringUtils;


/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StartsWithPathAliasReplacement
        implements PathAliasReplacement
{
  private final String alias;
  private final String replacementPath;

  public StartsWithPathAliasReplacement( String alias, String replacementPath )
  {
    if ( alias == null ) throw new IllegalArgumentException( "Alias must not be null.");
    if ( replacementPath == null ) throw new IllegalArgumentException( "Replacment path must not be null.");

    alias = StringUtils.cleanPath( alias );
    replacementPath = StringUtils.cleanPath( replacementPath );

    // Make sure neither alias nor replacementPath ends with a slash ("/").
    this.alias = alias.endsWith( "/" ) ? alias.substring( 0, alias.length() -1 ) : alias;
    this.replacementPath = replacementPath.endsWith( "/" ) ? replacementPath.substring( 0, replacementPath.length() - 1 ) : replacementPath;
  }

  public String getAlias()
  { return this.alias; }

  public String getReplacementPath()
  { return this.replacementPath; }

  public boolean containsPathAlias( String path )
  {
    if ( path == null ) throw new IllegalArgumentException( "Path must not be null.");
    path = StringUtils.cleanPath( path );
    return path.startsWith( alias + "/" );
  }

  public String replacePathAlias( String path )
  {
    if ( path == null ) throw new IllegalArgumentException( "Path must not be null." );
    path = StringUtils.cleanPath( path );
    if ( ! path.startsWith( alias + "/" ))
      throw new IllegalArgumentException( "Path [" + path + "] does not contain alias [startWith( \"" + alias + "/\" )].");
    return replacementPath + path.substring( alias.length() );
  }
}
