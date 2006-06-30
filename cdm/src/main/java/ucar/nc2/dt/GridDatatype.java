
package ucar.nc2.dt;

import ucar.ma2.*;
import ucar.nc2.*;

import java.util.*;


/**
 * A data variable that uses a cartesian (x,y,z) coordinate system, where neighbors in index space
 *   are neighbors in real space.
 * @author caron
 * @version $Revision: 1.5 $ $Date: 2005/05/23 20:18:35 $
 */
public interface GridDatatype extends ucar.nc2.VariableSimpleIF {

  public int getRank();
  public Dimension getDimension(int dimIndex);
  public List getDimensions();

  public ucar.nc2.dt.grid.GridCoordSys getCoordinateSystem();
  public ucar.unidata.geoloc.ProjectionImpl getProjection();

  public Dimension getTimeDimension();
  public int getTimeDimensionIndex();

  public Dimension getXDimension();
  public int getXDimensionIndex();

  public Dimension getYDimension();
  public int getYDimensionIndex();

  public Dimension getZDimension();
  public int getZDimensionIndex();

  public ArrayList getLevels();
  public ArrayList getTimes();

  public Array getDataSlice(int t, int z, int y, int x) throws java.io.IOException;
  public Array readVolumeData(int t) throws java.io.IOException;
  public Array readYXData(int y, int x) throws java.io.IOException;
  public Array readZYData(int z, int y) throws java.io.IOException;

  public boolean hasMissingData();
  public boolean isMissingData(double val);
  public float[] setMissingToNaN(float[] data);
  public MAMath.MinMax getMinMaxSkipMissingData(Array data);

}

/* Change History:
   $Log: GridDatatype.java,v $
   Revision 1.5  2005/05/23 20:18:35  caron
   refactor for scale/offset/missing

   Revision 1.4  2005/05/11 19:58:09  caron
   add VariableSimpleIF, remove TypedDataVariable

   Revision 1.3  2005/03/04 20:18:23  caron
   *** empty log message ***

   Revision 1.2  2005/03/04 19:36:18  caron
   improve javadoc

   Revision 1.1  2005/03/03 20:52:24  caron
   datatype checkin

*/