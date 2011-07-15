package thredds.tds.idd;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.ma2.Section;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class GridDatasetTestUtils
{
  public static boolean equalityOfGridDatasetsByGridNameAndShape( GridDataset gridDs1,
                                                                  GridDataset gridDs2,
                                                                  StringBuilder diffLog )
  {
    boolean isEqual = true;
    isEqual &= doesTargetGridDsContainBaseGridDs( gridDs1, gridDs2, diffLog );
    isEqual &= doesTargetGridDsContainBaseGridDs( gridDs2, gridDs1, diffLog );

    return isEqual;
  }

  /**
   * Return true if each grid in baseGridDs is found (by name) in testGridDs
   * and the matching grids have the same shape. Return false if there are
   * differences, all differences are logged in diffLog.
   *
   * @param baseGridDs the grid dataset against which to compare the test grid dataset.
   * @param testGridDs the grid dataset being tested.
   * @param diffLog    the StringBuilder in which differences are logged.
   * @return true if each grid in baseGridDs is found (by name) in testGridDs
   *         and the matching grids have the same shape.
   */
  public static boolean doesTargetGridDsContainBaseGridDs( GridDataset baseGridDs,
                                                           GridDataset testGridDs,
                                                           StringBuilder diffLog )
  {
    boolean same = true;
    StringBuilder localDiffLog = new StringBuilder();
    for ( GridDatatype grid1 : baseGridDs.getGrids() )
    {
      GridDatatype grid2 = testGridDs.findGridDatatype( grid1.getFullName() );
      if ( grid2 == null )
      {
        same = false;
        localDiffLog.append( "\n  Can't find " ).append( grid1.getFullName() );
      }
      else
      {
        long size1 = new Section( grid1.getShape() ).computeSize();
        long size2 = new Section( grid2.getShape() ).computeSize();
        if ( size1 != size2 )
        {
          same = false;
          localDiffLog.append( "\n  " ).append( grid1.getFullName() ).append( " size mismatch: " )
                  .append( show( grid1 ) ).append( " != " ).append( show( grid2 ) );
        }
      }
    }
    if ( ! same )
    {
      String msg = "Differences between:\n[" + baseGridDs.getLocationURI() + "]\n["
                   + testGridDs.getLocationURI() + "]";
      localDiffLog.insert( 0, msg );
    }

    diffLog.append( localDiffLog);
    return same;
  }

  private static String show( GridDatatype grid )
  {
    Section s = new Section( grid.getShape() );
    return s.toString();
  }
}
