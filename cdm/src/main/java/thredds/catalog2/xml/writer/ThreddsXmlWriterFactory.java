package thredds.catalog2.xml.writer;

import thredds.catalog2.xml.writer.stax.StaxWriter;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsXmlWriterFactory
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( ThreddsXmlWriterFactory.class );

  private ThreddsXmlWriterFactory() {}
  public static ThreddsXmlWriterFactory newInstance()
  {
    return new ThreddsXmlWriterFactory();
  }

  public ThreddsXmlWriter createThreddsXmlWriter()
  {
    return new StaxWriter();
  }

}
