/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dods.DODSNode;
import opendap.dap.*;

import java.util.*;
import java.io.IOException;

/**
 * A DODS Structure.
 *
 * @author caron
 */

//Coverity[FB.EQ_DOESNT_OVERRIDE_EQUALS]
public class DODSStructure extends ucar.nc2.Structure implements DODSNode
{
  private DConstructor ds;
  protected DODSNetcdfFile dodsfile; // so we dont have to cast everywhere
  protected String dodsShortName;

  // constructor called from DODSNetcdfFile.makeVariable() for scalar Structure or Sequence
  DODSStructure( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String dodsShortName,
         DodsV dodsV) throws IOException {

    super(dodsfile, parentGroup, parentStructure,  DODSNetcdfFile.makeShortName(dodsShortName));
    setDODSName(DODSNetcdfFile.makeDODSName(dodsShortName));
    this.dodsfile = dodsfile;
    this.ds = (DConstructor) dodsV.bt;

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
    setDODSName(from.getDODSName());
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

  protected String getDODSshortName() { return getShortName(); }


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
    public StructureDataIterator reset() {
      row = 0;
      return this;
    }

    @Override
    public int getCurrentRecno() {
      return row - 1;
    }

  }

  ////////////////////////////////////////////
  // DODSNode Interface
  //Coverity[FB.UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD]
  String dodsName = null;
  public String getDODSName() {return dodsName;}
  public void setDODSName(String name) {this.dodsName = name;}

}
