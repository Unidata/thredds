package thredds.server.catalogservice;

import org.springframework.validation.DataBinder;
import org.springframework.beans.MutablePropertyValues;

import javax.servlet.http.HttpServletRequest;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class RemoteCatalogRequestDataBinder extends DataBinder
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private static enum FieldInfo
  {
    CATALOG( "catalog", "catalogUri", ""),
    COMMAND( "command", "command", "" ),
    DATASET( "dataset", "dataset", "" ),
    VERBOSE( "verbose", "verbose", "false"),
    HTML_VIEW( "htmlView", "htmlView", "true");

    private String parameterName;
    private String propertyName;
    private String defaultValue;

    FieldInfo( String parameterName, String propertyName, String defaultValue)
    {
      this.parameterName = parameterName;
      this.propertyName = propertyName;
      this.defaultValue = defaultValue;
    }

    public String getParameterName() { return parameterName; }
    public String getPropertyName() { return propertyName; }
    public String getDefaultValue() { return defaultValue; }
  }

  public RemoteCatalogRequestDataBinder( RemoteCatalogRequest target,
                                         String requestObjectName )
  {
    super( target, requestObjectName);
  }

  public void bind( HttpServletRequest req)
  {
    String catPath = req.getParameter( FieldInfo.CATALOG.getParameterName() );
    String command = req.getParameter( FieldInfo.COMMAND.getParameterName() );
    String dataset = req.getParameter( FieldInfo.DATASET.getParameterName() );
    String verbose = req.getParameter( FieldInfo.VERBOSE.getParameterName() );
    String isHtmlView = req.getParameter( FieldInfo.HTML_VIEW.getParameterName() );


    MutablePropertyValues values = new MutablePropertyValues();

    // Don't allow null.
    if ( catPath == null )
      catPath = FieldInfo.CATALOG.getDefaultValue();
    if ( dataset == null )
      dataset = FieldInfo.DATASET.getDefaultValue();

    // Default to SUBSET if a dataset ID is given, otherwise, SHOW.
    if ( command == null )
      command = dataset.equals( FieldInfo.DATASET.getDefaultValue() )
                ? Command.SHOW.name() : Command.SUBSET.name();

    if ( verbose == null)
      verbose = FieldInfo.VERBOSE.getDefaultValue();
    if ( isHtmlView == null )
      isHtmlView = FieldInfo.HTML_VIEW.getDefaultValue();

    values.addPropertyValue( FieldInfo.CATALOG.getPropertyName(), catPath );
    values.addPropertyValue( FieldInfo.COMMAND.getPropertyName(), command );
    values.addPropertyValue( FieldInfo.DATASET.getPropertyName(), dataset );
    values.addPropertyValue( FieldInfo.VERBOSE.getPropertyName(), verbose );
    values.addPropertyValue( FieldInfo.HTML_VIEW.getPropertyName(), isHtmlView );

    super.bind( values );
  }
}