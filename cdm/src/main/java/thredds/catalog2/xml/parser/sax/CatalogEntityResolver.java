/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
