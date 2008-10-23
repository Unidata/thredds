/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.ft.point.writer;

import ucar.nc2.ft.point.writer.WriterCFPointObsDataset;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;
import ucar.ma2.DataType;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

/**
 * Class Description.
 *
 * @author caron
 * @since Oct 22, 2008
 */
public class CFPointObWriter {
  private WriterCFPointObsDataset ncWriter;

  /**
   * Constructor
   *
   * @param stream write to this stream
   * @param globalAtts optional list of global attributes (may be null)
   * @param altUnits optional altitude units (set to null if no altitude variable)
   * @param dataVars the set of data variables: first the double values, then the String values
   * @throws IOException if write error
   */
  public CFPointObWriter(DataOutputStream stream, List<Attribute> globalAtts, String altUnits, List<PointObVar> dataVars) throws IOException {
    ncWriter = new WriterCFPointObsDataset(stream, globalAtts, altUnits);

    List< VariableSimpleIF > vars = new ArrayList< VariableSimpleIF >(dataVars.size());
    for (PointObVar pvar : dataVars)
      vars.add( new PointObVarAdapter(pvar));

    ncWriter.writeHeader(vars);
  }

  /**
   * Add one data point to the file
   *
   * @param lat latitude value in units of degrees_north
   * @param lon longitude value in units of degrees_east
   * @param alt altitude value in units of altUnits (may be NaN)
   * @param time time value as a date
   * @param vals list of data values, matching dataVars in the constructor
   * @param svals list of String values, matching dataVars in the constructor
   * @throws IOException if write error
   */
  public void addPoint(double lat, double lon, double alt, Date time, double[] vals, String[] svals) throws IOException {
    ncWriter.writeRecord(lat, lon, alt, time, vals, svals);
  }

  /**
   * Call this when all done, output is flushed
   * @throws IOException if write error
   */
  public void finish() throws IOException {
    ncWriter.finish();
  }

  private class PointObVarAdapter implements VariableSimpleIF {
    PointObVar pov;
    PointObVarAdapter( PointObVar pov) {
      this.pov = pov;
    }

    public String getName() {
      return pov.getName();
    }

    public String getShortName() {
      return pov.getName();
    }

    public String getDescription() {
      return pov.getDesc();
    }

    public String getUnitsString() {
      return pov.getUnits();
    }

    public int getRank() {
      return 0;
    }

    public int[] getShape() {
      return new int[0];
    }

    public List<Dimension> getDimensions() {
      return new ArrayList<Dimension>(0);
    }

    public DataType getDataType() {
      return pov.getDataType();
    }

    public List<Attribute> getAttributes() {
      return new ArrayList<Attribute>(0);
    }

    public Attribute findAttributeIgnoreCase(String name) {
      return null;
    }

    public int compareTo(VariableSimpleIF o) {
      return -1;
    }
  }

}
