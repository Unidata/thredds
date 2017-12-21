package ucar.nc2;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * @author cwardgar
 * @since 2015/08/21
 */
public class TestFindDimensionInGroup {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    public void findDim() {
        NetcdfFile ncFile = new NetcdfFileSubclass();

        Group subGroup = new Group(ncFile, ncFile.getRootGroup(), "subGroup");
        ncFile.getRootGroup().addGroup(subGroup);

        Group subSubGroup = new Group(ncFile, subGroup, "subSubGroup");
        subGroup.addGroup(subSubGroup);

        Dimension dim = new Dimension("dim", 12);
        ncFile.getRootGroup().addDimension(dim);

        Dimension subDim = new Dimension("subDim", 7);
        subGroup.addDimension(subDim);

        Dimension subSubDim = new Dimension("subSubDim", 3);
        subSubGroup.addDimension(subSubDim);

        /* ncFile looks like:
        netcdf {
          dimensions:
            dim = 12;

          group: subGroup {
            dimensions:
              subDim = 7;

            group: subSubGroup {
              dimensions:
                subSubDim = 3;
            }
          }
        }
         */
        ncFile.finish();

        Assert.assertSame(dim, ncFile.findDimension("dim"));
        Assert.assertSame(dim, ncFile.findDimension("/dim"));
        Assert.assertSame(subDim, ncFile.findDimension("subGroup/subDim"));
        Assert.assertSame(subDim, ncFile.findDimension("/subGroup/subDim"));
        Assert.assertSame(subSubDim, ncFile.findDimension("subGroup/subSubGroup/subSubDim"));

        Assert.assertNull(ncFile.findDimension("subGroup/nonExistentDim"));
        Assert.assertNull(ncFile.findDimension("/subGroup/subDim/"));
    }
}
