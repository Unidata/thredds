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
  private String suffixForDirPath = "catalog.html";

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

    // Don't allow null catalog path values.
    if ( catPath == null )
      catPath = FieldInfo.CATALOG.getDefaultValue();
    else if ( catPath.endsWith( "/" ) || catPath.equals( "" ))
      // Append suffix for all directory paths.
      catPath += this.suffixForDirPath;

    // Don't allow null dataset ID values.
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