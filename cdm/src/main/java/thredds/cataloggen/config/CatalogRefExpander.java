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
// $Id: CatalogRefExpander.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen.config;
import thredds.catalog.InvDataset;

import java.util.regex.Pattern;

/**
 * Describes when in the expansion of a DatasetSource a catalogRef is created
 * and a child DatasetSource is expanded.
 *
 * @author Ethan Davis
 * @since 2004-12-03T22:51:34-0700
 */
public class CatalogRefExpander
{
  //private static Log log = LogFactory.getLog( CatalogRefExpander.class );
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CatalogRefExpander.class);

  private String name;
  private String directoryMatchPattern; // whenCreateCatalogRef
  private String catalogTitleSubstitutionPattern;
  private String catalogFilenameSubstitutionPattern;
  private boolean expand = true;
  private boolean flattenCatalog = false;

  private java.util.regex.Pattern pattern;
  private java.util.regex.Matcher matcher;

  public CatalogRefExpander( String name, String directoryMatchPattern,
                             String catalogTitleSubstitutionPattern,
                             String catalogFilenameSubstitutionPattern,
                             boolean expand, boolean flattenCatalog )
  {
    this.name = name;
    this.directoryMatchPattern = directoryMatchPattern;
    this.catalogTitleSubstitutionPattern = catalogTitleSubstitutionPattern;
    this.catalogFilenameSubstitutionPattern = catalogFilenameSubstitutionPattern;
    this.expand = expand;
    this.flattenCatalog = flattenCatalog;
  }

  public String getName()
  {
    return name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public String getDirectoryMatchPattern()
  {
    return directoryMatchPattern;
  }

  public void setDirectoryMatchPattern( String directoryMatchPattern )
  {
    this.directoryMatchPattern = directoryMatchPattern;
  }

  public String getCatalogTitleSubstitutionPattern()
  {
    return catalogTitleSubstitutionPattern;
  }

  public void setCatalogTitleSubstitutionPattern( String catalogTitleSubstitutionPattern )
  {
    this.catalogTitleSubstitutionPattern = catalogTitleSubstitutionPattern;
  }

  public String getCatalogFilenameSubstitutionPattern()
  {
    return catalogFilenameSubstitutionPattern;
  }

  public void setCatalogFilenameSubstitutionPattern( String catalogFilenameSubstitutionPattern )
  {
    this.catalogFilenameSubstitutionPattern = catalogFilenameSubstitutionPattern;
  }

  public boolean isExpand()
  {
    return expand;
  }

  public void setExpand( boolean expand )
  {
    this.expand = expand;
  }

  public boolean isFlattenCatalog()
  {
    return flattenCatalog;
  }

  public void setFlattenCatalog( boolean flattenCatalog )
  {
    this.flattenCatalog = flattenCatalog;
  }

  public boolean equals( Object o )
  {
    if ( this == o ) return true;
    if ( !( o instanceof CatalogRefExpander ) ) return false;

    final CatalogRefExpander catalogRefExpander = (CatalogRefExpander) o;

    if ( flattenCatalog != catalogRefExpander.flattenCatalog ) return false;
    if ( expand != catalogRefExpander.expand ) return false;
    if ( catalogFilenameSubstitutionPattern != null ? !catalogFilenameSubstitutionPattern.equals( catalogRefExpander.catalogFilenameSubstitutionPattern ) : catalogRefExpander.catalogFilenameSubstitutionPattern != null ) return false;
    if ( catalogTitleSubstitutionPattern != null ? !catalogTitleSubstitutionPattern.equals( catalogRefExpander.catalogTitleSubstitutionPattern ) : catalogRefExpander.catalogTitleSubstitutionPattern != null ) return false;
    if ( directoryMatchPattern != null ? !directoryMatchPattern.equals( catalogRefExpander.directoryMatchPattern ) : catalogRefExpander.directoryMatchPattern != null ) return false;
    if ( name != null ? !name.equals( catalogRefExpander.name ) : catalogRefExpander.name != null ) return false;

    return true;
  }

  public int hashCode()
  {
    int result;
    result = ( name != null ? name.hashCode() : 0 );
    result = 29 * result + ( directoryMatchPattern != null ? directoryMatchPattern.hashCode() : 0 );
    result = 29 * result + ( catalogTitleSubstitutionPattern != null ? catalogTitleSubstitutionPattern.hashCode() : 0 );
    result = 29 * result + ( catalogFilenameSubstitutionPattern != null ? catalogFilenameSubstitutionPattern.hashCode() : 0 );
    result = 29 * result + ( expand ? 1 : 0 );
    result = 29 * result + ( flattenCatalog ? 1 : 0 );
    return result;
  }

  public boolean makeCatalogRef( InvDataset dataset)
  {
    // @todo double check that dataset is a collection dataset
    pattern = Pattern.compile( this.directoryMatchPattern);
    matcher = pattern.matcher( dataset.getName());
    return( matcher.matches());
  }

  public String catalogRefTitle()
  {
    StringBuffer val = new StringBuffer();
    matcher.appendReplacement( val, this.catalogTitleSubstitutionPattern );
    return( val.toString());
  }

  public String catalogRefFilename()
  {
    StringBuffer val = new StringBuffer();
    matcher.appendReplacement( val, this.catalogFilenameSubstitutionPattern );
    return( val.toString());
  }
}
