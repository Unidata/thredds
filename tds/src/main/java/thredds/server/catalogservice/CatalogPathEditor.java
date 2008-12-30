package thredds.server.catalogservice;

import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalog;
import thredds.servlet.DataRootHandler;

import java.beans.PropertyEditorSupport;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogPathEditor extends PropertyEditorSupport
{
  private InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
  private String catPath;

  public CatalogPathEditor()
  {
    super();
    fac = null;
  }

  @Override
  public String getAsText()
  {
    InvCatalogImpl cat = (InvCatalogImpl) super.getValue();
    return cat != null ? catPath : null;
  }

  @Override
  public void setAsText( String text )
          throws IllegalArgumentException
  {
    if ( text == null )
      throw new IllegalArgumentException( "Catalog path must not be null.");

    this.catPath = text;

    // Check for matching catalog.
    DataRootHandler drh = DataRootHandler.getInstance();

    InvCatalog cat = drh.getCatalog( catPath, null );
    if ( cat == null )
      throw new IllegalArgumentException( "Catalog path does not represent a catalog.");

    super.setValue( cat );
  }
}