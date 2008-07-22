package thredds.catalog2.xml.parser;

import thredds.catalog2.Element;

import java.net.URI;
import java.io.File;
import java.io.Reader;
import java.io.InputStream;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface ElementParser
{
  public <E extends Element> E readXML( URI uri );

  public <E extends Element> E readXML( File file, URI docBaseUri );

  public <E extends Element> E readXML( Reader reader, URI docBaseUri );

  public <E extends Element> E readXML( InputStream is, URI docBaseUri );

}
