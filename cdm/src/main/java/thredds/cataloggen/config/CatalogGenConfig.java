// $Id: CatalogGenConfig.java 63 2006-07-12 21:50:51Z edavis $

package thredds.cataloggen.config;

import thredds.catalog.InvDataset;

import java.util.ArrayList;


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

  public boolean validate( StringBuffer out)
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

  // @todo Convert this main stuff into a test.
//  public static void main(String[] args)
//  {
//    InvDatasetImpl ds = new InvDatasetImpl(
//      null, "my name", "Unknown", null, "myid", null,
//      "http://motherlode.ucar.edu/dods/");
//
//    CatalogGenConfig cgc = new CatalogGenConfig( ds, "Catalog");
//    DatasetNamer dsNamer = new DatasetNamer(
//      ds, "this dsNamer", "false", "RegExp",
//      "match pattern", "sub pattern", null, null);
//
//    cgc.addDatasetNamer( dsNamer);
//
//    System.out.println( "CatalogGenConfig 1:");
//    System.out.println( "  " + cgc.toString());
//
//    StringBuffer myOut = new StringBuffer();
//    if ( cgc.validate( myOut))
//    {
//      System.out.println( "  Valid :" + myOut + ":");
//    } else
//    {
//      System.out.println( "  Invalid :" + myOut + ":");
//    }
//
//    dsNamer.setAttribName( "junk");
//
//    System.out.println( "CatalogGenConfig 1 (modified):");
//    System.out.println( "  " + cgc.toString());
//
//    myOut = new StringBuffer();
//    if ( cgc.validate( myOut))
//    {
//      System.out.println( "  Valid :" + myOut + ":");
//    } else
//    {
//      System.out.println( "  Invalid :" + myOut + ":");
//    }
//
//    dsNamer.setAttribName( null);
//    cgc.setType( "junk");
//    System.out.println( "CatalogGenConfig 1 (modified2):");
//    System.out.println( "  " + cgc.toString());
//    myOut = new StringBuffer();
//    if ( cgc.validate( myOut))
//    {
//      System.out.println( "  Valid :" + myOut + ":");
//    } else
//    {
//      System.out.println( "  Invalid :" + myOut + ":");
//    }
//  }

  /**
   * Type-safe enumeration of the types of CatalogGenConfig.
   *
   * @author Ethan Davis (from John Caron's thredds.catalog.ServiceType)
   */
  public static class Type
  {
    private static java.util.HashMap hash = new java.util.HashMap(20);

    public final static Type CATALOG = new Type( "Catalog");
    public final static Type AGGREGATION = new Type( "Aggregation");

    private String type;
    private Type( String name)
    {
      this.type = name;
      hash.put(name, this);
    }

    /**
     * Find the Type that matches this name.
     * @param name
     * @return Type or null if no match.
     */
    public static Type getType( String name)
    {
      if ( name == null) return null;
      return (Type) hash.get( name);
    }

    /**
     * Return the string name.
     */
    public String toString()
    {
      return type;
    }
  }
}
