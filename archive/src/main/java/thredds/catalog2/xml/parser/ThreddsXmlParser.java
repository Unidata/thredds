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
package thredds.catalog2.xml.parser;

import thredds.catalog2.Catalog;
import thredds.catalog2.Dataset;
import thredds.catalog2.Metadata;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.DatasetBuilder;
import thredds.catalog2.builder.MetadataBuilder;

import java.net.URI;
import java.io.File;
import java.io.Reader;
import java.io.InputStream;

/**
 * Provide methods for parsing THREDDS catalog XML documents and
 * generating Catalog, CatalogBuilder, Dataset, DatasetBuilder,
 * Metadata, or MetadataBuilder object.
 *
 * @author edavis
 * @since 4.0
 */
public interface ThreddsXmlParser
{
  public Catalog parse( URI documentUri) throws ThreddsXmlParserException;
  public Catalog parse( File file, URI docBaseUri) throws ThreddsXmlParserException;
  public Catalog parse( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
  public Catalog parse( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;

  public CatalogBuilder parseIntoBuilder( URI documentUri) throws ThreddsXmlParserException;
  public CatalogBuilder parseIntoBuilder( File file, URI docBaseUri) throws ThreddsXmlParserException;
  public CatalogBuilder parseIntoBuilder( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
  public CatalogBuilder parseIntoBuilder( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;

//  public Dataset parseDataset( URI documentUri ) throws ThreddsXmlParserException;
//  public Dataset parseDataset( File file, URI docBaseUri ) throws ThreddsXmlParserException;
//  public Dataset parseDataset( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
//  public Dataset parseDataset( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;
//
//  public DatasetBuilder parseDatasetIntoBuilder( URI documentUri ) throws ThreddsXmlParserException;
//  public DatasetBuilder parseDatasetIntoBuilder( File file, URI docBaseUri ) throws ThreddsXmlParserException;
//  public DatasetBuilder parseDatasetIntoBuilder( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
//  public DatasetBuilder parseDatasetIntoBuilder( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;
//
//  public Metadata parseMetadata( URI documentUri ) throws ThreddsXmlParserException;
//  public Metadata parseMetadata( File file, URI docBaseUri ) throws ThreddsXmlParserException;
//  public Metadata parseMetadata( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
//  public Metadata parseMetadata( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;
//
//  public MetadataBuilder parseMetadataIntoBuilder( URI documentUri ) throws ThreddsXmlParserException;
//  public MetadataBuilder parseMetadataIntoBuilder( File file, URI docBaseUri ) throws ThreddsXmlParserException;
//  public MetadataBuilder parseMetadataIntoBuilder( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
//  public MetadataBuilder parseMetadataIntoBuilder( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;
}