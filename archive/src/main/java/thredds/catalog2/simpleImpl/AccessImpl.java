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
package thredds.catalog2.simpleImpl;

import thredds.catalog.DataFormatType;
import thredds.catalog2.Access;
import thredds.catalog2.Service;
import thredds.catalog2.builder.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class AccessImpl implements Access, AccessBuilder
{
  private final DatasetImpl parentDs;
  private ServiceImpl service;
  private String urlPath;
  private DataFormatType dataFormat;
  private long dataSize;

  private boolean isBuilt = false;

  AccessImpl( DatasetImpl parentDataset )
  {
    this.parentDs = parentDataset;
  }

  public void setServiceBuilder( ServiceBuilder service )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This AccessBuilder has been built." );
    if ( service == null ) throw new IllegalArgumentException( "Service must not be null." );
    this.service = (ServiceImpl) service;
  }

  public void setUrlPath( String urlPath )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This AccessBuilder has been built." );
    if ( urlPath == null ) throw new IllegalArgumentException( "Path must not be null." );
    this.urlPath = urlPath;
  }

  public void setDataFormat( DataFormatType dataFormat )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This AccessBuilder has been built." );
    this.dataFormat = dataFormat != null ? dataFormat : DataFormatType.NONE;
  }

  public void setDataSize( long dataSize )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This AccessBuilder has been built." );
    if ( dataSize < -1 )
      throw new IllegalArgumentException( "Value must be zero or greater, or -1 if unknown.");
    this.dataSize = dataSize;
  }

  public Service getService()
  {
    if ( !this.isBuilt ) throw new IllegalStateException( "This Access has escaped its AccessBuilder before build() was called." );
    return service;
  }

  public ServiceBuilder getServiceBuilder()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This AccessBuilder has been built." );
    return service;
  }

  public String getUrlPath()
  {
    return urlPath;
  }
  
  public DataFormatType getDataFormat()
  {
    return dataFormat;
  }

  public long getDataSize()
  {
    return dataSize;
  }

  public boolean isBuilt()
  {
    return this.isBuilt;
  }

  public BuilderIssues getIssues()
  {
    BuilderIssues issues = new BuilderIssues();

    if ( this.service == null )
      issues.addIssue( BuilderIssue.Severity.ERROR, "Dataset[\"" + parentDs.getName() + "\"] not accessible[\"" + this.urlPath + "\"] due to null service.", this, null );
    if ( this.urlPath == null )
      issues.addIssue( BuilderIssue.Severity.ERROR, "Dataset[\"" + parentDs.getName() + "\"] not accessible[\"" + this.service != null ? this.service.getName() : "" + "\"] due to null urlPath.", this, null );

    return issues;
  }

  public Access build() throws BuilderException
  {
    this.isBuilt = true;
    return this;
  }
}
