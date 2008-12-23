package thredds.server.controller;

import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;
import org.springframework.beans.MutablePropertyValues;

import javax.servlet.http.HttpServletRequest;

import thredds.util.TdsPathUtils;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogServiceRequestDataBinder extends DataBinder
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private static enum FieldInfo
  {
    CATALOG( "catalog", "catalog", ""),
    VERBOSE( "verbose", "verbose", "false"),
    COMMAND( "command", "command", "SHOW"),
    VIEW( "htmlView", "htmlView", "false"),
    DATASET( "dataset", "dataset", "");

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

  private boolean localCatalog;
  private boolean xmlOnly;

  public CatalogServiceRequestDataBinder( CatalogServiceRequest target,
                                          String requestObjectName,
                                          boolean localCatalog,
                                          boolean xmlOnly )
  {
    super( target, requestObjectName);
    this.localCatalog = localCatalog;
    this.xmlOnly = xmlOnly;
  }

  public void bind( HttpServletRequest req)
  {
    String catPath;
    String isHtmlView = req.getParameter( FieldInfo.VIEW.getParameterName() );

    // Determine catalog path.
    if ( this.localCatalog )
    {
      catPath = TdsPathUtils.extractPath( req );
      if ( catPath == null ) catPath = FieldInfo.CATALOG.getDefaultValue();

      // Determine if HTML view is desired
      if ( catPath.endsWith( ".html" ))
      {
        if ( xmlOnly )
        {
          this.getBindingResult().addError(
                  new FieldError( this.getObjectName(),
                                  FieldInfo.VIEW.getPropertyName(),
                                  "catalog ends in \".html\" but only XML view supported.") );
        }
        isHtmlView = "true";
      }
    }
    else
    {
      catPath = req.getParameter( FieldInfo.CATALOG.getParameterName() );
    }

    String command = req.getParameter( FieldInfo.COMMAND.getParameterName() );
    String verbose = req.getParameter( FieldInfo.VERBOSE.getParameterName() );
    String dataset = req.getParameter( FieldInfo.DATASET.getParameterName() );


    MutablePropertyValues values = new MutablePropertyValues();

    if ( catPath == null || catPath.equals( "" ))
      catPath = FieldInfo.CATALOG.getDefaultValue();
    values.addPropertyValue( FieldInfo.CATALOG.getPropertyName(), catPath );

    if ( isHtmlView == null || isHtmlView.equals( "" ))
      isHtmlView = FieldInfo.VIEW.getDefaultValue();
    values.addPropertyValue( FieldInfo.VIEW.getPropertyName(), isHtmlView );

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
