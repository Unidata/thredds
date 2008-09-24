package thredds.util;

import org.springframework.util.StringUtils;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TdsConfiguredPathAliasReplacement
        implements PathAliasReplacement
{
  private final String alias;
  private StartsWithPathAliasReplacement swpar;

  public TdsConfiguredPathAliasReplacement( String alias )
  {
    if ( alias == null ) throw new IllegalArgumentException( "Alias must not be null.");
    alias = StringUtils.cleanPath( alias );

    // Make sure alias does not end with a slash ("/").
    this.alias = alias.endsWith( "/" ) ? alias.substring( 0, alias.length() - 1 ) : alias;

    this.swpar = null;
  }

  public void init( String replacementPath )
  {
    this.swpar = new StartsWithPathAliasReplacement( alias, replacementPath );
  }

  public String getAlias()
  {
    return this.alias;
  }

  public String getReplacementPath()
  {
    if ( swpar == null ) throw new IllegalStateException( "Not yet initialized." );
    return swpar.getReplacementPath();
  }

  public boolean containsPathAlias( String path )
  {
    if ( swpar == null ) throw new IllegalStateException( "Not yet initialized." );
    return swpar.containsPathAlias( path );
  }

  public String replacePathAlias( String path )
  {
    if ( swpar == null ) throw new IllegalStateException( "Not yet initialized." );
    return swpar.replacePathAlias( path );
  }
}
