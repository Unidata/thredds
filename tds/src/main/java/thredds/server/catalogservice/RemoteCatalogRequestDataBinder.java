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
    COMMAND( "command", "command", "SHOW" ),
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

    if ( catPath == null || catPath.equals( "" ))
      catPath = FieldInfo.CATALOG.getDefaultValue();
    values.addPropertyValue( FieldInfo.CATALOG.getPropertyName(), catPath );

    if ( isHtmlView == null || isHtmlView.equals( "" ))
      isHtmlView = FieldInfo.HTML_VIEW.getDefaultValue();
    values.addPropertyValue( FieldInfo.HTML_VIEW.getPropertyName(), isHtmlView );

    if ( command == null || command.equals( "" ))
      command = FieldInfo.COMMAND.getDefaultValue();
    values.addPropertyValue( FieldInfo.COMMAND.getPropertyName(), command );

    if ( verbose == null || verbose.equals( ""))
      verbose = FieldInfo.VERBOSE.getDefaultValue();
    values.addPropertyValue( FieldInfo.VERBOSE.getPropertyName(), verbose );

    if ( dataset == null || dataset.equals( "" ))
      dataset = FieldInfo.DATASET.getDefaultValue();
    values.addPropertyValue( FieldInfo.DATASET.getPropertyName(), dataset );

    super.bind( values );
  }
}