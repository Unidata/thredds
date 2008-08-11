package thredds.catalog2.xml.parser;

import thredds.catalog2.xml.parser.sax.SAXCatalogParser;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogParserFactory
{
  public static CatalogParser newCatalogParser( boolean wantValidating)
  {
    SAXCatalogParser catParser = SAXCatalogParser.getInstance();
    catParser.wantValidating( wantValidating );
    return catParser;
  }

  private CatalogParserFactory(){}
  public static CatalogParserFactory newFactory()
  { return new CatalogParserFactory(); }

  private boolean wantValidating = false;

  public boolean getWantValidating()
  { return this.wantValidating; }

  public void setWantValidating( boolean wantValidating )
  {
    this.wantValidating = wantValidating;
  }

  public CatalogParser getCatalogParser()
  {
    SAXCatalogParser catParser = SAXCatalogParser.getInstance();
    catParser.wantValidating( this.wantValidating );
    return catParser;
  }
}
