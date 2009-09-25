package thredds.catalog2.simpleImpl;

import thredds.catalog2.builder.BuilderIssues;
import thredds.catalog2.builder.BuilderIssue;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.Service;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Helper class for tracking services by globally unique names. The THREDDS Catalog specification (and XML Schema)
 * does not allow multiple services with the same name. However, in practice, we allow services with duplicate
 * names and we track those here as well.
 *
 * @author edavis
 * @since 4.0
 */
class GlobalServiceContainer
{
  private Map<String, ServiceImpl> servicesByGloballyUniqueName;

  private List<ServiceImpl> servicesWithDuplicateName;

  GlobalServiceContainer() {
    this.servicesByGloballyUniqueName = new HashMap<String,ServiceImpl>();
    this.servicesWithDuplicateName = new ArrayList<ServiceImpl>();
  }

  boolean isServiceNameInUseGlobally( String name ) {
    return this.servicesByGloballyUniqueName.containsKey( name );
  }

  ServiceImpl getServiceByGloballyUniqueName( String name )
  {
    if ( name == null )
      return null;

    return this.servicesByGloballyUniqueName.get( name );
  }

  void addService( ServiceImpl service )
  {
    if ( service == null )
      return;

    if ( this.servicesByGloballyUniqueName.containsKey( service.getName() ) )
    {
      // Duplicate name.
      boolean success = this.servicesWithDuplicateName.add( service );
      assert success;
      return;
    }
    else
    {
      ServiceImpl replacedService = this.servicesByGloballyUniqueName.put( service.getName(), service );
      assert null == replacedService;
    }
  }

  boolean removeService( ServiceImpl service )
  {
    if ( service == null )
      return false;

    if ( this.servicesWithDuplicateName.remove( service ))
      return true;

    if ( this.servicesByGloballyUniqueName.containsValue( service ))
    {
      ServiceImpl removedService = this.servicesByGloballyUniqueName.remove( service.getName() );
      assert service == removedService;
      boolean promoted = this.promoteFirstServiceWithDuplicateName( service.getName() );
      assert promoted;
      return true;
    }

    return false;
  }

  private boolean promoteFirstServiceWithDuplicateName( String name )
  {
    for ( ServiceImpl service : this.servicesWithDuplicateName )
    {
      if ( service.getName().equals( name ) )
      {
        boolean success = this.servicesWithDuplicateName.remove( service );
        assert success;
        ServiceImpl replacedService = this.servicesByGloballyUniqueName.put( service.getName(), service );
        assert null == replacedService;
        return true;
      }
    }
    return false;
  }

  boolean isEmpty() {
    return this.servicesByGloballyUniqueName.isEmpty();
  }

  int numberOfServicesWithGloballyUniqueNames() {
    return this.servicesByGloballyUniqueName.size();
  }

  int numberOfServicesWithDuplicateNames() {
    return this.servicesWithDuplicateName.size();
  }

  /**
   * Helper method that checks for duplicate service names. Used by CatalogBuilder.isBuildable() and
   * ServiceBuilder.isBuildable().
   *
   * @param responsibleBuilder the ThreddsBuilder which called this helper method.
   * @return true if no issues, false otherwise.
   */
  BuilderIssues getIssues( ThreddsBuilder responsibleBuilder )
  {
    BuilderIssues issues = new BuilderIssues();

    // Check subordinates.
    if ( ! this.servicesWithDuplicateName.isEmpty())
      for ( ServiceImpl s : this.servicesWithDuplicateName )
        issues.addIssue( BuilderIssue.Severity.WARNING, "Catalog contains duplicate service name [" + s.getName() + "].", responsibleBuilder, null );

    return issues;
  }
}
