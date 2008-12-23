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

  private String DEFAULT_CATALOG_PARAMETER_NAME = "catalog";
  private String DEFAULT_CATALOG_PROPERTY_NAME = "catalog";

  private String DEFAULT_DEBUG_PARAMETER_NAME = "debug";
  private String DEFAULT_DEBUG_PROPERTY_NAME = "debug";

  private String DEFAULT_COMMAND_PARAMETER_NAME = "command";
  private String DEFAULT_COMMAND_PROPERTY_NAME = "command";

  private String DEFAULT_VIEW_PARAMETER_NAME = "htmlView";
  private String DEFAULT_VIEW_PROPERTY_NAME = "htmlView";

  private String DEFAULT_DATASET_PARAMETER_NAME = "dataset";
  private String DEFAULT_DATASET_PROPERTY_NAME = "dataset";

  private boolean localCatalog;
  private boolean xmlOnly;

  public CatalogServiceRequestDataBinder( CatalogServiceRequest target,
                                          boolean localCatalog,
                                          boolean xmlOnly )
  {
    super( target);
    this.localCatalog = localCatalog;
    this.xmlOnly = xmlOnly;
  }

  public void bind( HttpServletRequest req)
  {
    MutablePropertyValues values = new MutablePropertyValues();

    String catPath;
    String isHtmlView = req.getParameter( DEFAULT_VIEW_PARAMETER_NAME );

    // Determine catalog path.
    if ( this.localCatalog )
    {
      catPath = TdsPathUtils.extractPath( req );
      if ( catPath == null )
      
      // Determine if HTML view is desired
      if ( catPath.endsWith( ".html" ))
      {
        if ( xmlOnly )
        {
          this.getBindingResult().addError( new FieldError( "", "htmlView", "catalog ends in \".html\" but only XML view supported.") );
        }
        catPath = catPath.replaceAll( ".html$", ".xml" );
        isHtmlView = "true";
      }
    }
    else
    {
      catPath = req.getParameter( DEFAULT_CATALOG_PARAMETER_NAME );
    }

    String command = req.getParameter( DEFAULT_COMMAND_PARAMETER_NAME );
    String debug = req.getParameter( DEFAULT_DEBUG_PARAMETER_NAME );
    String dataset = req.getParameter( DEFAULT_DATASET_PARAMETER_NAME );


    values.addPropertyValue( DEFAULT_CATALOG_PROPERTY_NAME, catPath != null ? catPath : "" );
    values.addPropertyValue( DEFAULT_VIEW_PROPERTY_NAME, isHtmlView != null ? isHtmlView : "false" );


    values.addPropertyValue( DEFAULT_COMMAND_PROPERTY_NAME, command != null ? command : "" );
    values.addPropertyValue( DEFAULT_DEBUG_PROPERTY_NAME, debug != null ? debug : "false" );
    values.addPropertyValue( DEFAULT_DATASET_PROPERTY_NAME, dataset != null ? dataset : "" );

    super.bind( values );
  }
}
