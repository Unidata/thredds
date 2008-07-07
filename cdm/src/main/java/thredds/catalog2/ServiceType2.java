package thredds.catalog2;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceType2
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( ServiceType2.class );

  public final static ServiceType2 NONE = new ServiceType2( "", null );

  public final static ServiceType2 ADDE = new ServiceType2( "ADDE", null );

  public final static ServiceType2 OPENDAP = new ServiceType2( "OPENDAP", null );
  public final static ServiceType2 DODS = new ServiceType2( "DODS", OPENDAP );

  public final static ServiceType2 OPENDAPG = new ServiceType2( "OPENDAP-G", null );

  public final static ServiceType2 HTTPServer = new ServiceType2( "HTTPServer", null );
  public final static ServiceType2 FTP = new ServiceType2( "FTP", null );
  public final static ServiceType2 GRIDFTP = new ServiceType2( "GridFTP", null );
  public final static ServiceType2 FILE = new ServiceType2( "File", null );
  public final static ServiceType2 NetcdfServer = new ServiceType2( "NetcdfServer", null ); // deprecated
  public final static ServiceType2 NetcdfSubset = new ServiceType2( "NetcdfSubset", null );

  public final static ServiceType2 LAS = new ServiceType2( "LAS", null );
  public final static ServiceType2 WMS = new ServiceType2( "WMS", null );
  public final static ServiceType2 WFS = new ServiceType2( "WFS", null );
  public final static ServiceType2 WCS = new ServiceType2( "WCS", null );
  public final static ServiceType2 WSDL = new ServiceType2( "WSDL", null );

  public final static ServiceType2 WebForm = new ServiceType2( "WebForm", null );

  public final static ServiceType2 CATALOG = new ServiceType2( "Catalog", null );
  public final static ServiceType2 QC = new ServiceType2( "QueryCapability", null );
  public final static ServiceType2 RESOLVER = new ServiceType2( "Resolver", null );
  public final static ServiceType2 COMPOUND = new ServiceType2( "Compound", null );
  public final static ServiceType2 THREDDS = new ServiceType2( "THREDDS", null );

  public final static ServiceType2 NETCDF = new ServiceType2( "NetCDF", null ); // deprecated - use dataFormatType = NetCDF
  public final static ServiceType2 HTTP = new ServiceType2( "HTTP", HTTPServer ); // deprecated - use HTTPServer
  
  private static java.util.List<ServiceType2> members = new java.util.ArrayList<ServiceType2>( 20 );

  private String name;
  private ServiceType2 aliasFor;

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
  private ServiceType2( String name, ServiceType2 aliasFor )
  {
    if ( name == null )
      throw new IllegalArgumentException( "Name must not be null.");
    if ( findType( name ) != null )
      throw new IllegalStateException( "ServiceType2(\"" + name + "\") already exists.");
    if ( aliasFor != null && name.equalsIgnoreCase( aliasFor.getName()) )
      throw new IllegalStateException( "New ServiceType2(\"" + name + "\") must not be alias for itself(\"" + aliasFor.getName() + "\")." );
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
  private ServiceType2( String name )
  {
    if ( name == null )
      throw new IllegalArgumentException( "Name must not be null." );
    if ( findType( name ) != null )
      throw new IllegalStateException( "ServiceType2(\"" + name + "\") already exists." );
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
  public static ServiceType2 findType( String name )
  {
    if ( name == null )
      return null;
    for ( ServiceType2 serviceType : members )
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
  public static ServiceType2 getType( String name )
  {
    if ( name == null ) return null;
    ServiceType2 type = findType( name );
    return type != null ? type : new ServiceType2( name );
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
    if ( !( o instanceof ServiceType2 ) )
      return false;
    return o.hashCode() == this.hashCode();
  }
}
