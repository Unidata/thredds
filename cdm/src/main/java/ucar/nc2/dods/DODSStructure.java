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
      this.dimensions.add( Dimension.VLEN);
      this.shape = new int[1]; // scalar
    } else
      this.shape = new int[0]; // scalar

    for (DodsV nested : dodsV.children) {
      dodsfile.addVariable(parentGroup, this, nested);
    }

    if (ds instanceof DSequence)
      isVariableLength = true;

    setSPobject(dodsV);
  }

  // constructor called from DODSNetcdfFile.makeVariable() for array of Structure
  DODSStructure( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String shortName,
                 DArray dodsArray, DodsV dodsV) throws IOException {
    this( dodsfile, parentGroup, parentStructure, shortName, dodsV);
    List<Dimension> dims = dodsfile.constructDimensions( parentGroup, dodsArray);
    setDimensions(dims);
    setSPobject(dodsV);
  }

  /** Copy constructor.
   * @param from  copy from this
   */
  private DODSStructure( DODSStructure from) { // boolean reparent) {
    super( from);

    dodsfile = from.dodsfile;
    dodsShortName = from.dodsShortName;
    ds = from.ds;
  }

  // for section and slice
  @Override
  protected Variable copy() {
    return new DODSStructure(this); // dont need to reparent
  }


  DConstructor getDConstructor() { return ds; }

  // need package access
  //protected void calcIsCoordinateVariable() { super.calcIsCoordinateVariable(); }

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
   * @throws java.io.IOException on io error
   */
  public StructureDataIterator getStructureIterator(String CE) throws java.io.IOException {
    return new SequenceIterator(CE);
  }

  private class SequenceIterator implements StructureDataIterator {
    private int nrows, row = 0;
    private ArrayStructure structArray;

    SequenceIterator(String CE) throws java.io.IOException {
      // nothin better to do for now !!
      structArray = (ArrayStructure) read();
      nrows = (int) structArray.getSize();
    }

    @Override
    public boolean hasNext() {
      return row < nrows;
    }

    @Override
    public StructureData next() {
      return structArray.getStructureData(row++);
    }

    @Override
    public void setBufferSize(int bytes) {
    }

    @Override
    public StructureDataIterator reset() {
      row = 0;
      return this;
    }

    @Override
    public int getCurrentRecno() {
      return row - 1;
    }

  }

}