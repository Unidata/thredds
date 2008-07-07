package thredds.catalog2;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceType
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( ServiceType.class );

  public final static ServiceType NONE = new ServiceType( "", null );

  public final static ServiceType ADDE = new ServiceType( "ADDE", null );

  public final static ServiceType OPENDAP = new ServiceType( "OPENDAP", null );
  public final static ServiceType DODS = new ServiceType( "DODS", OPENDAP );

  public final static ServiceType OPENDAPG = new ServiceType( "OPENDAP-G", null );

  public final static ServiceType HTTPServer = new ServiceType( "HTTPServer", null );
  public final static ServiceType FTP = new ServiceType( "FTP", null );
  public final static ServiceType GRIDFTP = new ServiceType( "GridFTP", null );
  public final static ServiceType FILE = new ServiceType( "File", null );
  public final static ServiceType NetcdfServer = new ServiceType( "NetcdfServer", null ); // deprecated
  public final static ServiceType NetcdfSubset = new ServiceType( "NetcdfSubset", null );

  public final static ServiceType LAS = new ServiceType( "LAS", null );
  public final static ServiceType WMS = new ServiceType( "WMS", null );
  public final static ServiceType WFS = new ServiceType( "WFS", null );
  public final static ServiceType WCS = new ServiceType( "WCS", null );
  public final static ServiceType WSDL = new ServiceType( "WSDL", null );

  public final static ServiceType WebForm = new ServiceType( "WebForm", null );

  public final static ServiceType CATALOG = new ServiceType( "Catalog", null );
  public final static ServiceType QC = new ServiceType( "QueryCapability", null );
  public final static ServiceType RESOLVER = new ServiceType( "Resolver", null );
  public final static ServiceType COMPOUND = new ServiceType( "Compound", null );
  public final static ServiceType THREDDS = new ServiceType( "THREDDS", null );

  public final static ServiceType NETCDF = new ServiceType( "NetCDF", null ); // deprecated - use dataFormatType = NetCDF
  public final static ServiceType HTTP = new ServiceType( "HTTP", HTTPServer ); // deprecated - use HTTPServer
  
  private static java.util.List<ServiceType> members = new java.util.ArrayList<ServiceType>( 20 );

  private String name;
  private ServiceType aliasFor;

  /**
   * Constructor for enumerated ServiceType objects (i.e., the static final
   * instances defined in this enumeration). Only enumerated ServiceType
   * objects are known as "members" of this enumeration.
   *
   * [NOTE: Only used to create static final instances.]
   *
   * @param name the name of the desired ServiceType.
   * @param aliasFor the ServiceType for which this is an alias, or null.
   */
  private ServiceType( String name, ServiceType aliasFor )
  {
    if ( name == null )
      throw new IllegalArgumentException( "Name must not be null.");
    if ( findType( name ) != null )
      throw new IllegalStateException( "ServiceType(\"" + name + "\") already exists.");
    if ( aliasFor != null && name.equalsIgnoreCase( aliasFor.getName()) )
      throw new IllegalStateException( "New ServiceType(\"" + name + "\") must not be alias for itself(\"" + aliasFor.getName() + "\")." );
    this.name = name;
    this.aliasFor = aliasFor;
    members.add( this );
  }

  /**
   * Constructor for un-enumerated ServiceType objects. Not known as
   * "members" of this enumeration.
   *
   * [NOTE: Only used by the getType() method to create "unknown" ServiceType objects.]
   *
   * @param name the name of the desired ServiceType.
   */
  private ServiceType( String name )
  {
    if ( name == null )
      throw new IllegalArgumentException( "Name must not be null." );
    if ( findType( name ) != null )
      throw new IllegalStateException( "ServiceType(\"" + name + "\") already exists." );
    this.name = name;
    this.aliasFor = null;
  }

  /**
   * Return the known ServiceType that matches the given name (ignoring case)
   * or null if the name is unknown.
   *
   * @param name name of the desired ServiceType.
   * @return ServiceType or null if no match.
   */
  public static ServiceType findType( String name )
  {
    if ( name == null )
      return null;
    for ( ServiceType serviceType : members )
    {
      if ( serviceType.name.equalsIgnoreCase( name ) )
        return serviceType.aliasFor != null ? serviceType.aliasFor : serviceType;
    }
    return null;
  }

  /**
   * Return a ServiceType that matches the given name by either matching
   * a known type (ignoring case) or creating an unknown type.
   *
   * @param name name of the desired ServiceType
   * @return the named ServiceType or null if given name is null.
   */
  public static ServiceType getType( String name )
  {
    if ( name == null ) return null;
    ServiceType type = findType( name );
    return type != null ? type : new ServiceType( name );
  }

  public String getName()
  {
    return this.name;
  }

  public String toString()
  {
    return this.name;
  }

  public int hashCode()
  {
    return name.hashCode();
  }

  public boolean equals( Object o )
  {
    if ( this == o )
      return true;
    if ( !( o instanceof ServiceType ) )
      return false;
    return o.hashCode() == this.hashCode();
  }
}
