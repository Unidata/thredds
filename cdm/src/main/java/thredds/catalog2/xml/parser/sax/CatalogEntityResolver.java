package thredds.catalog2.xml.parser.sax;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogEntityResolver implements EntityResolver
{
  private Map<String,RegisteredEntity> entityMap;

  public CatalogEntityResolver()
  {
    this.entityMap = new HashMap<String, RegisteredEntity>();
  }

  public void setEntityMap( Map<String,RegisteredEntity> entityMap )
  {
    this.entityMap = entityMap != null ? entityMap : new HashMap<String, RegisteredEntity>();
  }

  public void addEntity( RegisteredEntity entity )
  {
    if ( entity != null )
    {
      this.entityMap.put( entity.getSystemId(), entity );
    }
  }

  public InputSource resolveEntity( String publicId, String systemId )
          throws SAXException, IOException
  {
    if ( this.entityMap.containsKey( systemId ) )
    {
      RegisteredEntity entity = this.entityMap.get( systemId );
      InputStream inStream = this.getClass().getClassLoader().getResourceAsStream( entity.getResourceName() );
      if ( inStream == null )
        return null;
      InputSource inSource = new InputSource( inStream );
      inSource.setPublicId( publicId );
      inSource.setSystemId( systemId );
      return inSource;
    }
    else
      return null;
  }

  public static class RegisteredEntity
  {
    private String publicId;
    private String systemId;
    private String resourceName;
    public RegisteredEntity() {}

    public String getPublicId()
    {
      return publicId;
    }

    public void setPublicId( String publicId )
    {
      this.publicId = publicId;
    }

    public String getSystemId()
    {
      return systemId;
    }

    public void setSystemId( String systemId )
    {
      this.systemId = systemId;
    }

    public String getResourceName()
    {
      return resourceName;
    }

    public void setResourceName( String resourceName )
    {
      this.resourceName = resourceName;
    }
  }
}
