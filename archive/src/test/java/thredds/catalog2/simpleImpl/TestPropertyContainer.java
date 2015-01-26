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
package thredds.catalog2.simpleImpl;

import junit.framework.*;
import thredds.catalog2.Property;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestPropertyContainer extends TestCase
{

  public TestPropertyContainer( String name )
  {
    super( name );
  }


  public void testNewContainer()
  {
    PropertyContainer pc = new PropertyContainer();
    assertFalse( pc.isBuilt() );

    assertTrue( "New property container not empty.",
                pc.isEmpty());
    int size = pc.size();
    assertTrue( "New property container not size()==0 ["+size+"].",
                size == 0 );

    assertFalse( "New property container has property [name].",
                 pc.containsPropertyName( "name" ));
    assertTrue( "New property container list of properties not empty.",
                pc.getProperties().isEmpty());
    Property prop = pc.getPropertyByName( "name" );
    if ( prop != null )
      fail( "New property container holds unexpected property [name]/["+prop.getValue()+"].");
    assertTrue( "New property container list of property names not empty.",
                pc.getPropertyNames().isEmpty());
    String value = pc.getPropertyValue( "name" );
    if ( value != null )
      fail( "New property container holds unexpected property value [name]/["+value+"].");
  }

  public void testAddGetReplaceRemoveIsEmpty()
  {
    PropertyContainer pc = new PropertyContainer();

    assertTrue( "New property container not isEmpty().",
                pc.isEmpty() );

    // Add three properties to container.
    String name1 = "name1";
    String value1 = "value1";
    pc.addProperty( name1, value1 );
    assertFalse( "Property container with one entry isEmpty().",
                pc.isEmpty() );

    String name2 = "name2";
    String value2 = "value2";
    pc.addProperty( name2, value2 );
    assertFalse( "Property container with two entries isEmpty().",
                pc.isEmpty() );
    String name3 = "name3";
    String value3 = "value3";
    pc.addProperty( name3, value3 );
    assertFalse( "Property container with three entries isEmpty().",
                pc.isEmpty() );
    int size = pc.size();
    assertTrue( "Property container with three entries has unexpected size() ["+size+"]",
                size == 3 );

    // Check for the three properties added above in the container.
    String testValue = pc.getPropertyValue( name1 );
    assertTrue( "Name[" + name1 + "]/Value["+testValue+"] not as expected ["+name1+"]/["+value1+"].",
                testValue.equals( value1 ) );
    testValue = pc.getPropertyValue( name2 );
    assertTrue( "Name[" + name2 + "]/Value[" + testValue + "] not as expected [" + name2 + "]/[" + value2 + "].",
                testValue.equals( value2 ) );
    testValue = pc.getPropertyValue( name3 );
    assertTrue( "Name[" + name3 + "]/Value[" + testValue + "] not as expected [" + name3 + "]/[" + value3 + "].",
                testValue.equals( value3 ) );

    // Test that an unknown property is not found (returns null).
    String unknownName = "unknownName";
    testValue = pc.getPropertyValue( unknownName );
    assertTrue( "Contained value ["+testValue+"] for unknown name ["+unknownName+"].",
                null == testValue );
                                    
    // Test that a replaced property is handled correctly.
    String value1_new = "value1_new";
    pc.addProperty( name1, value1_new );
    testValue = pc.getPropertyValue( name1 );
    assertTrue( "Value of replaced property ["+name1+"]/["+testValue+"] not as expected ["+name1+"]/["+value1_new+"]",
                testValue.equals( value1_new ));

    // Test removing property.
    if ( ! pc.removeProperty( name1 ))
      fail( "Failed to remove property [" + name1 + "].");
    testValue = pc.getPropertyValue( name1 );
    assertNull( "Found property ["+name1+"]/["+testValue+"] after removal.",
                testValue);

    // Test removing unknown property.
    if ( pc.removeProperty( unknownName ))
      fail( "Succeeded in removing unkown property ["+unknownName+"].");

    // Test removing property from empty container.
    if ( ! pc.removeProperty( name2 ))
      fail( "Failed to remove property [" + name2 + "]." );
    if ( ! pc.removeProperty( name3 ))
      fail( "Failed to remove property [" + name3 + "]." );
    if ( pc.removeProperty( name3 ))
      fail( "Succeeded in removing no longer contained property [" + name3 + "]." );

    assertTrue( "Property container empty due to removes not isEmpty().",
                pc.isEmpty() );
    size = pc.size();
    assertTrue( "Property container empty due to removes has size()!=0 [" + size + "]",
                size == 0 );
  }

  /**
   * Test the various getters including that insertion-ordered lists
   * are returned by the list getters.
   */
  public void testAllGetters()
  {
    PropertyContainer pc = new PropertyContainer();

    // Add three properties to container.
    String name1 = "name1";
    String value1 = "value1";
    pc.addProperty( name1, value1 );
    String name2 = "name2";
    String value2 = "value2";
    pc.addProperty( name2, value2 );
    String name3 = "name3";
    String value3 = "value3";
    pc.addProperty( name3, value3 );

    List<Property> propList = pc.getProperties();
    assertTrue( propList.size() == 3 );
    assertTrue( propList.get( 0 ).getName().equals(name1));
    assertTrue( propList.get( 0 ).getValue().equals(value1));
    assertTrue( propList.get( 1 ).getName().equals(name2));
    assertTrue( propList.get( 1 ).getValue().equals(value2));
    assertTrue( propList.get( 2 ).getName().equals(name3));
    assertTrue( propList.get( 2 ).getValue().equals(value3));

    assertTrue( pc.getPropertyByName( name1 ).getName().equals(name1));
    assertTrue( pc.getPropertyByName( name1 ).getValue().equals(value1));
    assertTrue( pc.getPropertyByName( name2 ).getName().equals(name2));
    assertTrue( pc.getPropertyByName( name2 ).getValue().equals(value2));
    assertTrue( pc.getPropertyByName( name3 ).getName().equals(name3));
    assertTrue( pc.getPropertyByName( name3 ).getValue().equals(value3));

    List<String> nameList = pc.getPropertyNames();
    assertTrue( nameList.size() == 3 );
    assertTrue( nameList.get( 0).equals( name1 ));
    assertTrue( nameList.get( 1).equals( name2 ));
    assertTrue( nameList.get( 2).equals( name3 ));

    assertTrue( pc.getPropertyValue( name1 ).equals( value1));
    assertTrue( pc.getPropertyValue( name2 ).equals( value2));
    assertTrue( pc.getPropertyValue( name3 ).equals( value3));
  }

  public void testBuild()
  {
    PropertyContainer pc = new PropertyContainer();

    // Add three properties to container.
    String name1 = "name1";
    String value1 = "value1";
    pc.addProperty( name1, value1 );
    String name2 = "name2";
    String value2 = "value2";
    pc.addProperty( name2, value2 );
    String name3 = "name3";
    String value3 = "value3";
    pc.addProperty( name3, value3 );

    pc.build();
    assertTrue( pc.isBuilt() );


    try
    {
      pc.addProperty( "name", "value" );
    }
    catch ( IllegalStateException e )
    {
      try
      { pc.removeProperty( "name" ); }
      catch ( IllegalStateException e2)
      { return; }
      fail( "Built property container did not throw IllegalStateException on removeProperty().");
    }
    fail( "Built property container did not throw IllegalStateException on addProperty()." );
  }

}
