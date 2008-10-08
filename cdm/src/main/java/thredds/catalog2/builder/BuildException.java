package thredds.catalog2.builder;

import java.util.List;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BuildException extends Exception
{
  private final List<Source> sources;

  public BuildException( Source component )
  {
    super();
    this.sources = Collections.singletonList( component );
  }

  public BuildException( List<Source> sources )
  {
    super();
    this.sources = sources;
  }

  public List<Source> getSources()
  {
    return Collections.unmodifiableList( sources );
  }

  @Override
  public String getMessage()
  {
    StringBuilder sb = new StringBuilder();
    for ( Source bec : this.sources )
      sb.append( bec.getMessage() ).append( "\n");
    return sb.toString();
  }

  public static class Source
  {
    private final String message;
    private final ThreddsBuilder builder;

    public Source( String message, ThreddsBuilder builder )
    {
      this.message = message;
      this.builder = builder;
    }

    public String getMessage() { return this.message; }
    public ThreddsBuilder getBuilder() { return this.builder; }
  }
}
