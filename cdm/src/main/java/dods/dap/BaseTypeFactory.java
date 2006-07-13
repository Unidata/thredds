/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////
//
// -- 7/14/99 Modified by: Nathan Potter (ndp@oce.orst.edu)
// Added Support For DInt16, DUInt16, DFloat32.
// Added (and commented out) support for DBoolean.
// -- 7/14/99 ndp 
//  
/////////////////////////////////////////////////////////////////////////////

package dods.dap;

/**
 * A Factory for BaseType objects. The DDS parser must implement this
 * interface so that specific instances of <code>DByte</code>, and other
 * <code>BaseType</code>s, can be created.
 * <p>
 * DDS and its parser will use this interface to create specific instances of
 * the various data type classes in DODS. There is an implementation of this
 * interface which creates instances of the <code>DByte</code>, etc. classes.
 * If a new set of classes are defined (e.g. <code>NC_Byte</code>, etc.) this
 * interface should be implemented for those classes such that the methods
 * return instances of the appropriate types.
 *
 * @version $Revision: 48 $
 * @author jehamby
 * @see BaseType
 * @see DefaultFactory
 * @see DDS
 */
public interface BaseTypeFactory {
  //------------------------------------
  /** 
   * Construct a new DBoolean.
   * @return the new DBoolean
   */
//  public DBoolean newDBoolean();

  /**
   * Construct a new DBoolean with name n.
   * @param n the variable name
   * @return the new DBoolean
   */
//  public DBoolean newDBoolean(String n);

  //------------------------------------
  /** 
   * Construct a new DByte.
   * @return the new DByte
   */
  public DByte newDByte();

  /**
   * Construct a new DByte with name n.
   * @param n the variable name
   * @return the new DByte
   */
  public DByte newDByte(String n);

  //------------------------------------
  /** 
   * Construct a new DInt16.
   * @return the new DInt16
   */
  public DInt16 newDInt16();

  /**
   * Construct a new DInt16 with name n.
   * @param n the variable name
   * @return the new DInt16
   */
  public DInt16 newDInt16(String n);

  //------------------------------------
  /** 
   * Construct a new DUInt16.
   * @return the new DUInt16
   */
  public DUInt16 newDUInt16();

  /**
   * Construct a new DUInt16 with name n.
   * @param n the variable name
   * @return the new DUInt16
   */
  public DUInt16 newDUInt16(String n);

  //------------------------------------
  /** 
   * Construct a new DInt32.
   * @return the new DInt32
   */
  public DInt32 newDInt32();

  /**
   * Construct a new DInt32 with name n.
   * @param n the variable name
   * @return the new DInt32
   */
  public DInt32 newDInt32(String n);

  //------------------------------------
  /** 
   * Construct a new DUInt32.
   * @return the new DUInt32
   */
  public DUInt32 newDUInt32();

  /**
   * Construct a new DUInt32 with name n.
   * @param n the variable name
   * @return the new DUInt32
   */
  public DUInt32 newDUInt32(String n);

  //------------------------------------
  /** 
   * Construct a new DFloat32.
   * @return the new DFloat32
   */
  public DFloat32 newDFloat32();

  /**
   * Construct a new DFloat32 with name n.
   * @param n the variable name
   * @return the new DFloat32
   */
  public DFloat32 newDFloat32(String n);

  //------------------------------------
  /** 
   * Construct a new DFloat64.
   * @return the new DFloat64
   */
  public DFloat64 newDFloat64();

  /**
   * Construct a new DFloat64 with name n.
   * @param n the variable name
   * @return the new DFloat64
   */
  public DFloat64 newDFloat64(String n);

  //------------------------------------
  /** 
   * Construct a new DString.
   * @return the new DString
   */
  public DString newDString();

  /**
   * Construct a new DString with name n.
   * @param n the variable name
   * @return the new DString
   */
  public DString newDString(String n);

  //------------------------------------
  /** 
   * Construct a new DURL.
   * @return the new DURL
   */
  public DURL newDURL();

  /**
   * Construct a new DURL with name n.
   * @param n the variable name
   * @return the new DURL
   */
  public DURL newDURL(String n);

  //------------------------------------
  /** 
   * Construct a new DArray.
   * @return the new DArray
   */
  public DArray newDArray();

  /**
   * Construct a new DArray with name n.
   * @param n the variable name
   * @return the new DArray
   */
  public DArray newDArray(String n);

  //------------------------------------
  /** 
   * Construct a new DList.
   * @return the new DList
   */
  public DList newDList();

  /**
   * Construct a new DList with name n.
   * @param n the variable name
   * @return the new DList
   */
  public DList newDList(String n);

  //------------------------------------
  /** 
   * Construct a new DGrid.
   * @return the new DGrid
   */
  public DGrid newDGrid();

  /**
   * Construct a new DGrid with name n.
   * @param n the variable name
   * @return the new DGrid
   */
  public DGrid newDGrid(String n);

  //------------------------------------
  /** 
   * Construct a new DStructure.
   * @return the new DStructure
   */
  public DStructure newDStructure();

  /**
   * Construct a new DStructure with name n.
   * @param n the variable name
   * @return the new DStructure
   */
  public DStructure newDStructure(String n);

  //------------------------------------
  /** 
   * Construct a new DSequence.
   * @return the new DSequence
   */
  public DSequence newDSequence();

  /**
   * Construct a new DSequence with name n.
   * @param n the variable name
   * @return the new DSequence
   */
  public DSequence newDSequence(String n);

 }
