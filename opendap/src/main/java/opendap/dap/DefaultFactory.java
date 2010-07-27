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
 * The default client-side Factory for BaseType objects.
 *
 * @author jehamby
 * @version $Revision: 15901 $
 * @see BaseTypeFactory
 */

public class DefaultFactory implements BaseTypeFactory, java.io.Serializable {

    static final long serialVersionUID = 1;


    //..................................

    /**
     * Construct a new DByte.
     *
     * @return the new DByte
     */
    public DByte newDByte() {
        return new DByte();
    }

    /**
     * Construct a new DByte with name n.
     *
     * @param n the variable name
     * @return the new DByte
     */
    public DByte newDByte(String n) {
        return new DByte(n);
    }

    //..................................

    /**
     * Construct a new DInt16.
     *
     * @return the new DInt16
     */
    public DInt16 newDInt16() {
        return new DInt16();
    }

    /**
     * Construct a new DInt16 with name n.
     *
     * @param n the variable name
     * @return the new DInt16
     */
    public DInt16 newDInt16(String n) {
        return new DInt16(n);
    }

    //..................................

    /**
     * Construct a new DUInt16.
     *
     * @return the new DUInt16
     */
    public DUInt16 newDUInt16() {
        return new DUInt16();
    }

    /**
     * Construct a new DUInt16 with name n.
     *
     * @param n the variable name
     * @return the new DUInt16
     */
    public DUInt16 newDUInt16(String n) {
        return new DUInt16(n);
    }

    //..................................

    /**
     * Construct a new DInt32.
     *
     * @return the new DInt32
     */
    public DInt32 newDInt32() {
        return new DInt32();
    }

    /**
     * Construct a new DInt32 with name n.
     *
     * @param n the variable name
     * @return the new DInt32
     */
    public DInt32 newDInt32(String n) {
        return new DInt32(n);
    }

    //..................................

    /**
     * Construct a new DUInt32.
     *
     * @return the new DUInt32
     */
    public DUInt32 newDUInt32() {
        return new DUInt32();
    }

    /**
     * Construct a new DUInt32 with name n.
     *
     * @param n the variable name
     * @return the new DUInt32
     */
    public DUInt32 newDUInt32(String n) {
        return new DUInt32(n);
    }

    //..................................

    /**
     * Construct a new DFloat32.
     *
     * @return the new DFloat32
     */
    public DFloat32 newDFloat32() {
        return new DFloat32();
    }

    /**
     * Construct a new DFloat32 with name n.
     *
     * @param n the variable name
     * @return the new DFloat32
     */
    public DFloat32 newDFloat32(String n) {
        return new DFloat32(n);
    }

    //..................................

    /**
     * Construct a new DFloat64.
     *
     * @return the new DFloat64
     */
    public DFloat64 newDFloat64() {
        return new DFloat64();
    }

    /**
     * Construct a new DFloat64 with name n.
     *
     * @param n the variable name
     * @return the new DFloat64
     */
    public DFloat64 newDFloat64(String n) {
        return new DFloat64(n);
    }

    //..................................

    /**
     * Construct a new DString.
     *
     * @return the new DString
     */
    public DString newDString() {
        return new DString();
    }

    /**
     * Construct a new DString with name n.
     *
     * @param n the variable name
     * @return the new DString
     */
    public DString newDString(String n) {
        return new DString(n);
    }

    //..................................

    /**
     * Construct a new DURL.
     *
     * @return the new DURL
     */
    public DURL newDURL() {
        return new DURL();
    }

    /**
     * Construct a new DURL with name n.
     *
     * @param n the variable name
     * @return the new DURL
     */
    public DURL newDURL(String n) {
        return new DURL(n);
    }

    //..................................

    /**
     * Construct a new DArray.
     *
     * @return the new DArray
     */
    public DArray newDArray() {
        return new DArray();
    }

    /**
     * Construct a new DArray with name n.
     *
     * @param n the variable name
     * @return the new DArray
     */
    public DArray newDArray(String n) {
        return new DArray(n);
    }

    //..................................


    /**
     * Construct a new DGrid.
     *
     * @return the new DGrid
     */
    public DGrid newDGrid() {
        return new DGrid();
    }

    /**
     * Construct a new DGrid with name n.
     *
     * @param n the variable name
     * @return the new DGrid
     */
    public DGrid newDGrid(String n) {
        return new DGrid(n);
    }

    //..................................

    /**
     * Construct a new DStructure.
     *
     * @return the new DStructure
     */
    public DStructure newDStructure() {
        return new DStructure();
    }

    /**
     * Construct a new DStructure with name n.
     *
     * @param n the variable name
     * @return the new DStructure
     */
    public DStructure newDStructure(String n) {
        return new DStructure(n);
    }

    //..................................

    /**
     * Construct a new DSequence.
     *
     * @return the new DSequence
     */
    public DSequence newDSequence() {
        return new DSequence();
    }

    /**
     * Construct a new DSequence with name n.
     *
     * @param n the variable name
     * @return the new DSequence
     */
    public DSequence newDSequence(String n) {
        return new DSequence(n);
    }

}


