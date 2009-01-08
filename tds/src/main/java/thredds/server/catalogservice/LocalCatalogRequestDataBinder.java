package thredds.server.catalogservice;

import org.springframework.validation.DataBinder;
import org.springframework.beans.MutablePropertyValues;

import javax.servlet.http.HttpServletRequest;

import thredds.util.TdsPathUtils;

/**
 * Binds an HttpServletRequest to a {@link LocalCatalogRequest} command object.
 *
 * More details in {@link LocalCatalogServiceController}
 *
 * @author edavis
 * @since 4.0
 * @see LocalCatalogServiceController
 * @see LocalCatalogRequest
 */
public class LocalCatalogRequestDataBinder extends DataBinder
{
  //private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private static enum FieldInfo
  {
    CATALOG( "catalog", "path", ""),
    COMMAND( "command", "command", "" ),
    DATASET( "dataset", "dataset", "" );

    private String parameterName;
    private String propertyName;
    private String defaultValue;

    FieldInfo( String parameterName, String propertyName, String defaultValue )
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

    // Don't allow null values.
    if ( catPath == null )
      catPath = FieldInfo.CATALOG.getDefaultValue();
    if ( dataset == null )
      dataset = FieldInfo.DATASET.getDefaultValue();

    // Default to SUBSET if a dataset ID is given, otherwise, SHOW.
    if ( command == null  )
      command = dataset.equals( FieldInfo.DATASET.getDefaultValue())
                ? Command.SHOW.name() : Command.SUBSET.name();

    values.addPropertyValue( FieldInfo.CATALOG.getPropertyName(), catPath );
    values.addPropertyValue( FieldInfo.COMMAND.getPropertyName(), command );
    values.addPropertyValue( FieldInfo.DATASET.getPropertyName(), dataset );

    super.bind( values );
  }
}