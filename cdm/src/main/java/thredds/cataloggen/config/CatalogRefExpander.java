// $Id: CatalogRefExpander.java,v 1.3 2006/01/20 02:08:22 caron Exp $
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

/*
 * $Log: CatalogRefExpander.java,v $
 * Revision 1.3  2006/01/20 02:08:22  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.2  2005/04/05 22:37:01  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.1  2004/12/14 22:47:22  edavis
 * Add simple interface to thredds.cataloggen and continue adding catalogRef capabilities.
 *
 */