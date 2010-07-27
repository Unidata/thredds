/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.dap;

/**
 * A Factory for BaseType objects. The DDS parser must implement this
 * interface so that specific instances of <code>DByte</code>, and other
 * <code>BaseType</code>s, can be created.
 * <p/>
 * DDS and its parser will use this interface to create specific instances of
 * the various data type classes in OPeNDAP. There is an implementation of this
 * interface which creates instances of the <code>DByte</code>, etc. classes.
 * If a new set of classes are defined (e.g. <code>NC_Byte</code>, etc.) this
 * interface should be implemented for those classes such that the methods
 * return instances of the appropriate types.
 *
 * @author jehamby
 * @version $Revision: 15901 $
 * @see BaseType
 * @see DefaultFactory
 * @see DDS
 */
public interface BaseTypeFactory {

    /**
     * Construct a new BaseType with type as specified by the type string.
     * @return the new BaseType
     */
//   public BaseType newBaseType(String typeString);

    /**
     * Construct a new BaseType with name n and
     * with type as specified by the typeString.
     * @return the new BaseType
     */
//   public BaseType newBaseType(String typeString, String n);


    //------------------------------------


    /**
     * Construct a new DByte.
     *
     * @return the new DByte
     */
    public DByte newDByte();

    /**
     * Construct a new DByte with name n.
     *
     * @param n the variable name
     * @return the new DByte
     */
    public DByte newDByte(String n);

    //------------------------------------

    /**
     * Construct a new DInt16.
     *
     * @return the new DInt16
     */
    public DInt16 newDInt16();

    /**
     * Construct a new DInt16 with name n.
     *
     * @param n the variable name
     * @return the new DInt16
     */
    public DInt16 newDInt16(String n);

    //------------------------------------

    /**
     * Construct a new DUInt16.
     *
     * @return the new DUInt16
     */
    public DUInt16 newDUInt16();

    /**
     * Construct a new DUInt16 with name n.
     *
     * @param n the variable name
     * @return the new DUInt16
     */
    public DUInt16 newDUInt16(String n);

    //------------------------------------

    /**
     * Construct a new DInt32.
     *
     * @return the new DInt32
     */
    public DInt32 newDInt32();

    /**
     * Construct a new DInt32 with name n.
     *
     * @param n the variable name
     * @return the new DInt32
     */
    public DInt32 newDInt32(String n);

    //------------------------------------

    /**
     * Construct a new DUInt32.
     *
     * @return the new DUInt32
     */
    public DUInt32 newDUInt32();

    /**
     * Construct a new DUInt32 with name n.
     *
     * @param n the variable name
     * @return the new DUInt32
     */
    public DUInt32 newDUInt32(String n);

    //------------------------------------

    /**
     * Construct a new DFloat32.
     *
     * @return the new DFloat32
     */
    public DFloat32 newDFloat32();

    /**
     * Construct a new DFloat32 with name n.
     *
     * @param n the variable name
     * @return the new DFloat32
     */
    public DFloat32 newDFloat32(String n);

    //------------------------------------

    /**
     * Construct a new DFloat64.
     *
     * @return the new DFloat64
     */
    public DFloat64 newDFloat64();

    /**
     * Construct a new DFloat64 with name n.
     *
     * @param n the variable name
     * @return the new DFloat64
     */
    public DFloat64 newDFloat64(String n);

    //------------------------------------

    /**
     * Construct a new DString.
     *
     * @return the new DString
     */
    public DString newDString();

    /**
     * Construct a new DString with name n.
     *
     * @param n the variable name
     * @return the new DString
     */
    public DString newDString(String n);

    //------------------------------------

    /**
     * Construct a new DURL.
     *
     * @return the new DURL
     */
    public DURL newDURL();

    /**
     * Construct a new DURL with name n.
     *
     * @param n the variable name
     * @return the new DURL
     */
    public DURL newDURL(String n);

    //------------------------------------

    /**
     * Construct a new DArray.
     *
     * @return the new DArray
     */
    public DArray newDArray();

    /**
     * Construct a new DArray with name n.
     *
     * @param n the variable name
     * @return the new DArray
     */
    public DArray newDArray(String n);

    //------------------------------------


    /**
     * Construct a new DGrid.
     *
     * @return the new DGrid
     */
    public DGrid newDGrid();

    /**
     * Construct a new DGrid with name n.
     *
     * @param n the variable name
     * @return the new DGrid
     */
    public DGrid newDGrid(String n);

    //------------------------------------

    /**
     * Construct a new DStructure.
     *
     * @return the new DStructure
     */
    public DStructure newDStructure();

    /**
     * Construct a new DStructure with name n.
     *
     * @param n the variable name
     * @return the new DStructure
     */
    public DStructure newDStructure(String n);

    //------------------------------------

    /**
     * Construct a new DSequence.
     *
     * @return the new DSequence
     */
    public DSequence newDSequence();

    /**
     * Construct a new DSequence with name n.
     *
     * @param n the variable name
     * @return the new DSequence
     */
    public DSequence newDSequence(String n);

}


