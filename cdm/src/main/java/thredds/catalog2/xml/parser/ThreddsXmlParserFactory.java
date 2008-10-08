package thredds.catalog2.xml.parser;

import thredds.catalog2.xml.parser.sax.SAXCatalogParser;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsXmlParserFactory
{
  public static ThreddsXmlParser newCatalogParser( boolean wantValidating)
  {
    SAXCatalogParser catParser = SAXCatalogParser.getInstance();
    catParser.wantValidating( wantValidating );
    return catParser;
  }

  private ThreddsXmlParserFactory(){}
  public static ThreddsXmlParserFactory newFactory()
  { return new ThreddsXmlParserFactory(); }

  private boolean wantValidating = false;

  public boolean getWantValidating()
  { return this.wantValidating; }

  public void setWantValidating( boolean wantValidating )
  {
    this.wantValidating = wantValidating;
  }

  public ThreddsXmlParser getCatalogParser()
  {
    SAXCatalogParser catParser = SAXCatalogParser.getInstance();
    catParser.wantValidating( this.wantValidating );
    return catParser;
  }
}
