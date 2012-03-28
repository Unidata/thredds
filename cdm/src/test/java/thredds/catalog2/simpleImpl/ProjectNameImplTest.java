package thredds.catalog2.simpleImpl;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ProjectNameImplTest
{
  @Test
  public void checkProjectNameCtorAndGet()
  {
    String namingAuthority = "authority";
    String name = "name";
    ThreddsMetadataImpl.ProjectNameImpl pni = new ThreddsMetadataImpl.ProjectNameImpl( namingAuthority, name );

    assertEquals( namingAuthority, pni.getNamingAuthority());
    assertEquals( name, pni.getName());
  }

  @Test(expected=IllegalArgumentException.class)
  public void checkExceptionWithNullProjectName() {
    new ThreddsMetadataImpl.ProjectNameImpl( "auth", null );
  }

  @Test(expected=IllegalArgumentException.class)
  public void checkExceptionWithEmptyProjectName() {
    new ThreddsMetadataImpl.ProjectNameImpl( "auth", "" );
  }
}
