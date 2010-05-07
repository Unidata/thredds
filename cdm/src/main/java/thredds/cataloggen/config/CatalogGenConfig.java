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

package thredds.cataloggen.config;

import thredds.catalog.InvDataset;

/**
 * <p>Title: Catalog Generator</p>
 * <p>Description: Tool for generating THREDDS catalogs.</p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>Company: UCAR/Unidata</p>
 * @author Ethan Davis
 * @version 1.0
 */

public class CatalogGenConfig
{
//  private static Log log = LogFactory.getLog( CatalogGenConfig.class );
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatalogGenConfig.class);

  // parent dataset for this CatalogGenConfig
  private InvDataset parentDataset = null;
  // type of catalogGenConfig
  private CatalogGenConfig.Type type = null;

  // list of DatasetSources
  private DatasetSource datasetSource = null;

  // validation flag and log
  private boolean isValid = true;
  private StringBuffer msgLog = new StringBuffer();

  public static final String CATALOG_GEN_CONFIG_NAMESPACE_URI_0_5 =
          "http://www.unidata.ucar.edu/namespaces/thredds/CatalogGenConfig/v0.5";

  /** Constructor */
  public CatalogGenConfig( InvDataset parentDataset, String typeName)
  {
    this( parentDataset, CatalogGenConfig.Type.getType( typeName));
  }

  /** Constructor */
  public CatalogGenConfig( InvDataset parentDataset,
                           CatalogGenConfig.Type type)
  {
    log.debug( "CatalogGenConfig(): type " + type.toString() + ".");

    this.parentDataset = parentDataset;
    this.type = type;
  }

  /** Return the parent dataset of this CatalogGenConfig */
  public InvDataset getParentDataset()
  { return( this.parentDataset); }
  /** Set the type of this CatalogGenConfig */
  public void setParentDataset( InvDataset parentDataset)
  { this.parentDataset = parentDataset; }

  /** Return the type of this CatalogGenConfig */
  public CatalogGenConfig.Type getType()
  { return( this.type); }
  /** Set the type of this CatalogGenConfig */
  public void setType( CatalogGenConfig.Type type)
  { this.type = type; }

  /** Return the DatasetSource for this CatalogGenConfig */
  public DatasetSource getDatasetSource()
  { return( this.datasetSource); }
  /** Set the DatasetSource for this CatalogGenConfig */
  public void setDatasetSource( DatasetSource dsSource)
  { this.datasetSource = dsSource; }

  public boolean validate( StringBuilder out)
  {
    log.debug( "validate(): checking if valid");
    this.isValid = true;

    // If log from construction has content, append to validation output msg.
    if (this.msgLog.length() > 0)
    {
      out.append( this.msgLog);
    }

    // Check that type is not null.
    if ( this.getType() == null)
    {
      isValid = false;
      out.append( " ** CatalogGenConfig (3): null value for type is not valid (set with bad string?).");
    }

    // Validate DatasetSource child element.
    this.isValid &= this.getDatasetSource().validate( out);

    log.debug( "validate(): isValid=" + this.isValid + " message is\n" +
            out.toString());

    return( this.isValid);
  }

  public String toString()
  {
    StringBuffer tmp = new StringBuffer();
    tmp.append( "CatalogGenConfig[type:<")
            .append( this.getType() ).append( "> child ")
            .append( this.getDatasetSource().toString() + ")]");

    return( tmp.toString());
  }

  enum Type
  {
    CATALOG( "Catalog" ),
    AGGREGATION( "Aggregation" );

    private String altId;

    Type( String altId ) {
      this.altId = altId;
    }

    public String toString() {
      return this.altId;
    }

    public static Type getType( String altId )
    {
      if ( altId == null )
        return null;

      for ( Type curType : Type.values() ) {
        if ( curType.altId.equals( altId ) )
          return curType;
      }
      return null;
    }
  }

}
