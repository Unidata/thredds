/*
Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
See LICENSE.txt for license information.
*/

package ucar.nc2;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Test accessing special attributes
 */

public class TestSpecialAttributes extends TestCase
{

    @Test
    public void testReadAll() throws IOException
    {
        NetcdfFile ncfile = TestDir.openFileLocal("testSpecialAttributes.nc4");
        // Iterate over all top-level attributes and see if it is special
        for(Attribute a : ncfile.getRootGroup().getAttributes()) {
            Assert.assertTrue("Attribute iteration found special attribute: " + a.getShortName(),
                    !Attribute.isspecial(a));
        }
        ncfile.close();
    }

    @Test
    public void testReadByName() throws IOException
    {
        NetcdfFile ncfile = TestDir.openFileLocal("testSpecialAttributes.nc4");
        // Attempt to read special attributes by name
        for(String name : new String[]{CDM.NCPROPERTIES}) {
            Attribute special = ncfile.getRootGroup().findAttribute(name);
            Assert.assertTrue("Could not access special attribute: " + name,
                    special != null && Attribute.isspecial(special));
        }
        ncfile.close();
    }

}
