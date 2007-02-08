// $Id:DODSStructure.java 51 2006-07-12 17:13:13Z caron $
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
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dods;

import ucar.ma2.*;
import ucar.nc2.*;
import opendap.dap.*;

import java.util.*;
import java.io.IOException;

/**
 * A DODS Structure.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class DODSStructure extends ucar.nc2.Structure {
  private DConstructor ds;
  protected DODSNetcdfFile dodsfile; // so we dont have to cast everywhere
  protected String dodsShortName;

  // constructor called from DODSNetcdfFile.makeVariable() for scalar Structure or Sequence
  DODSStructure( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String dodsShortName,
         DodsV dodsV) throws IOException {

    super(dodsfile, parentGroup, parentStructure,  DODSNetcdfFile.makeNetcdfName( dodsShortName));
    this.dodsfile = dodsfile;
    this.ds = (DConstructor) dodsV.bt;
    this.dodsShortName = dodsShortName;

    if (ds instanceof DSequence) {
      this.dimensions.add( Dimension.UNKNOWN);
      this.shape = new int[1]; // scalar
    } else
      this.shape = new int[0]; // scalar

    for (int i = 0; i < dodsV.children.size(); i++) {
      DodsV nested = (DodsV) dodsV.children.get(i);
      dodsfile.addVariable( parentGroup, this, nested);
    }

    if (ds instanceof DSequence)
      isVlen = true;
  }

  // constructor called from DODSNetcdfFile.makeVariable() for array of Structure
  DODSStructure( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String shortName,
                 DArray dodsArray, DodsV dodsV) throws IOException {
    this( dodsfile, parentGroup, parentStructure, shortName, dodsV);
    ArrayList dims = dodsfile.constructDimensions( parentGroup, dodsArray);
    setDimensions(dims);
  }

  DConstructor getDConstructor() { return ds; }

  // package access needed
  protected void calcIsCoordinateVariable() { super.calcIsCoordinateVariable(); }

  /** Let the Structure fill in here. */
  public String writeCDL(String space, boolean useFullName, boolean strict) {
    return super.writeCDL( space, useFullName, strict);
  }

  protected String getDODSshortName() { return dodsShortName; }


  ///////////////////////////////////////////////////////////////////////
  /**
   * Return an iterator over the set of repeated structures. The iterator
   * will return an object of type Structure. When you call this method, the
   * Sequence will be read using the given constraint expression, and the data
   * returned sequentially.
   *
   * <br> If the data has been cached by a read() to an enclosing container, you must
   * leave the CE null. Otherwise a new call will be made to the server.
   *
   * @param CE constraint expression, or null.
   * @return iterator over type DODSStructure.
   * @see DODSStructure
   */
  public Structure.Iterator getStructureIterator(String CE) throws java.io.IOException {
    return new SequenceIterator(CE);
  }

  private class SequenceIterator extends Structure.Iterator {
    private int nrows, row = 0;
    private DSequence seq = null;
    private ArrayStructure structArray;

    SequenceIterator(String CE) throws java.io.IOException {
      super(0);

      // nothin better to do for now !!
      structArray = (ArrayStructure) read();
      nrows = (int) structArray.getSize();

      /* if (CE != null)
        CE = getDODSshortName() + CE;
      else {
        CE = getDODSshortName();
      }

        // contact the server
      try {
        dods.dap.DataDDS dataDDS = dodsfile.readDataDDSfromServer(CE);
        seq = (DSequence) dataDDS.getVariable(shortName);

      } catch (Exception e) {
        System.out.println("DODSSequence read failed on "+getDODSshortName()+"\n"+e);
        e.printStackTrace(System.out);
        throw new IOException( e.getMessage());
      }

      // containing struct array
      structArray = new ArrayStructureW( makeStructureMembers(), new int[0]);  */
    }

    public boolean hasNext() {
      return row < nrows; // seq.getRowCount();
    }

    public StructureData next() {
      return structArray.getStructureData(row++);
      /* Vector v = seq.getRow(row);

      // unpack all of the data that comes back
      Enumeration enumVars = v.elements();
      StructureData data = null; // LOOK dodsfile.convertStructureData(structArray, enumVars, DODSStructure.this);
      structArray.setStructureData( data, 0);

      row++;
      return data; */
    }

    public void remove() { throw new UnsupportedOperationException(); }
  }

}