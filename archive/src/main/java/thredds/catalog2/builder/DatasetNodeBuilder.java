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
package thredds.catalog2.builder;

import thredds.catalog2.DatasetNode;
import thredds.catalog2.ThreddsMetadata;

import java.util.List;
import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetNodeBuilder extends ThreddsBuilder
{
  public String getId();
  public void setId( String id );

  public String getIdAuthority();
  public void setIdAuthority( String idAuthority );

  public String getName();
  public void setName( String name );

  public void addProperty( String name, String value );
  public boolean removeProperty( String name );
  public List<String> getPropertyNames();
  public String getPropertyValue( String name );

  public ThreddsMetadataBuilder setNewThreddsMetadataBuilder();
  public boolean removeThreddsMetadataBuilder();
  public ThreddsMetadataBuilder getThreddsMetadataBuilder();

  public MetadataBuilder addMetadata();
  public boolean removeMetadata( MetadataBuilder metadataBuilder );
  public List<MetadataBuilder> getMetadataBuilders();

  public CatalogBuilder getParentCatalogBuilder();
  public DatasetNodeBuilder getParentDatasetBuilder();

  public boolean isCollection();

  public DatasetBuilder addDataset( String name );
  public CatalogRefBuilder addCatalogRef( String name, URI reference);

  public boolean removeDatasetNode( DatasetNodeBuilder datasetBuilder );

  public List<DatasetNodeBuilder> getDatasetNodeBuilders();
  public DatasetNodeBuilder getDatasetNodeBuilderById( String id );
  public DatasetNodeBuilder findDatasetNodeBuilderByIdGlobally( String id );

  public boolean isDatasetIdInUseGlobally( String id );

  public DatasetNode build() throws BuilderException;
}
