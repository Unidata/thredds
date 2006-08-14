// $Id: VTfromExistingData.java 64 2006-07-12 22:30:50Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.unidata.geoloc.vertical;


import ucar.ma2.ArrayDouble;
import ucar.ma2.Array;
import ucar.ma2.MAMath;
import ucar.ma2.InvalidRangeException;

import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VerticalCT;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;

import java.io.IOException;


/**
 * This implements a VerticalTransform using an existing 3D variable.
 * This is a common case when the 3D pressure or height field is stored in the file.
 *
 * @author john
 */
public class VTfromExistingData extends VerticalTransformImpl {

    /** The name of the Parameter whose value is the variable that contains the 2D Height or Pressure field */
    public final static String existingDataField = "existingDataField";

    /** The variable that contains the 2D Height or Pressure field */
    private VariableDS existingData;

    /**
     * Constructor.
     *
     * @param ds _more_
     * @param timeDim _more_
     * @param vCT _more_
     */
    public VTfromExistingData(NetcdfDataset ds, Dimension timeDim,
                              VerticalCT vCT) {
        super(timeDim);
        String vname =
            vCT.findParameterIgnoreCase(existingDataField).getStringValue();
        this.existingData = (VariableDS) ds.findVariable(vname);
        units             = existingData.getUnitsString();
    }

    /**
     * _more_
     *
     * @param timeIndex _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public ArrayDouble.D3 getCoordinateArray(int timeIndex)
            throws IOException, InvalidRangeException {
        Array data = readArray(existingData, timeIndex);

        // copy for now - better to just return Array, with promise its rank 3
        int[] shape = data.getShape();
        ArrayDouble.D3 ddata = (ArrayDouble.D3) Array.factory(double.class,
                                   shape);
        MAMath.copyDouble(ddata, data);
        return ddata;
    }
}

/* Change History:
   $Log: VTfromExistingData.java,v $
   Revision 1.4  2006/02/16 23:02:40  caron
   *** empty log message ***

   Revision 1.3  2006/02/07 13:28:53  caron
   VerticalTransform subsetting

   Revision 1.2  2005/08/11 22:42:13  dmurray
   jindent (I'll leave the javadoc to those who forgot to)

   Revision 1.1  2005/03/11 23:02:15  caron
   *** empty log message ***

*/
