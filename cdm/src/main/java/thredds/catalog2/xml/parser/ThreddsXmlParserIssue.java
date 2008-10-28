package thredds.catalog2.xml.parser;

import thredds.catalog2.builder.ThreddsBuilder;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsXmlParserIssue
{
  public enum Severity{ FATAL, ERROR, WARNING };

  private final Severity severity;
  private final String message;
  private final ThreddsBuilder builder;
  private final Exception cause;

  public ThreddsXmlParserIssue( Severity severity, String message, ThreddsBuilder builder, Exception cause )
  {
    this.severity = severity;
    this.message = message;
    this.builder = builder;
    this.cause = cause;
  }

  public Severity getSeverity()
  {
    return this.severity;
  }

  public String getMessage()
  {
    return this.message;
  }

  public ThreddsBuilder getBuilder()
  {
    return this.builder;
  }

  public Exception getCause()
  {
    return this.cause;
  }
}