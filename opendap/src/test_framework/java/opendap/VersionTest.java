package opendap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * 
 * Initial dummy test class for the test_framework structure.
 * It only provides a mock sample of what it should be  
 * 
 * @author mhermida
 *
 */
public class VersionTest {
	
	@Test
	public void getVersionString(){
		assertEquals("0.0.7", Version.getVersionString());
	}
		
}
