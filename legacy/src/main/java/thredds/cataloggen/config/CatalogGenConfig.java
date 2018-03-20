/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
