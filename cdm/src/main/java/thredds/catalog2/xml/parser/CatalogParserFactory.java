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
  private boolean isValidating = false;

  private CatalogParserFactory(){}
  public static CatalogParserFactory getInstance()
  {
    return new CatalogParserFactory();
  }

  public void setValidating( boolean isValidating )
  {
    this.isValidating = isValidating;
  }

  public CatalogParser getCatalogParser()
  {
    SAXCatalogParser catParser = SAXCatalogParser.getInstance();
    catParser.setValidating( this.isValidating );
    return catParser;
  }
}
