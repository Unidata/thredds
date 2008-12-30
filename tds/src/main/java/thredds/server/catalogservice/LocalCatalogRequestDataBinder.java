package thredds.server.catalogservice;

import org.springframework.validation.DataBinder;
import org.springframework.beans.MutablePropertyValues;

import javax.servlet.http.HttpServletRequest;

import thredds.util.TdsPathUtils;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class LocalCatalogRequestDataBinder extends DataBinder
{
  //private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private static enum FieldInfo
  {
    CATALOG( "catalog", "path", ""),
    COMMAND( "command", "command", "SHOW" ),
    DATASET( "dataset", "dataset", "" );

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

  public LocalCatalogRequestDataBinder( LocalCatalogRequest target,
                                        String requestObjectName )
  {
    super( target, requestObjectName);
  }

  public void bind( HttpServletRequest req)
  {
    String catPath = TdsPathUtils.extractPath( req );
    String command = req.getParameter( FieldInfo.COMMAND.getParameterName() );
    String dataset = req.getParameter( FieldInfo.DATASET.getParameterName() );

    MutablePropertyValues values = new MutablePropertyValues();

    if ( catPath == null || catPath.equals( "" ))
      catPath = FieldInfo.CATALOG.getDefaultValue();
    values.addPropertyValue( FieldInfo.CATALOG.getPropertyName(), catPath );

    if ( command == null || command.equals( "" ))
      command = FieldInfo.COMMAND.getDefaultValue();
    values.addPropertyValue( FieldInfo.COMMAND.getPropertyName(), command );

    if ( dataset == null || dataset.equals( "" ))
      dataset = FieldInfo.DATASET.getDefaultValue();
    values.addPropertyValue( FieldInfo.DATASET.getPropertyName(), dataset );

    super.bind( values );
  }
}