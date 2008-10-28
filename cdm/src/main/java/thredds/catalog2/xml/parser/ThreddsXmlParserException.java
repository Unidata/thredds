package thredds.catalog2.xml.parser;

import java.util.List;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsXmlParserException extends Exception
{
  private final List<ThreddsXmlParserIssue> issues;

  public ThreddsXmlParserException( ThreddsXmlParserIssue issue )
  {
    super();
    this.issues = Collections.singletonList( issue );
  }

  public ThreddsXmlParserException( List<ThreddsXmlParserIssue> issues )
  {
    super();
    this.issues = issues;
  }

  public ThreddsXmlParserException( String message )
  {
    super( message);
    this.issues = Collections.emptyList();
  }

  public ThreddsXmlParserException( String message, Throwable cause )
  {
    super( message, cause);
    this.issues = Collections.emptyList();
  }

  public List<ThreddsXmlParserIssue> getSources()
  {
    return Collections.unmodifiableList( this.issues );
  }

  @Override
  public String getMessage()
  {
    if ( this.issues == null || this.issues.isEmpty())
      return super.getMessage();

    StringBuilder sb = new StringBuilder();
    for ( ThreddsXmlParserIssue txpi : this.issues )
      sb.append( txpi.getMessage() ).append( "\n" );
    return sb.toString();
  }
}
