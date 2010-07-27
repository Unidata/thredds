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



package opendap.servers.test;

import opendap.dap.Server.*;
import opendap.dap.*;

import java.io.*;

/**
 * Holds a OPeNDAP Server <code>Array</code> value.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @see BaseType
 */
public class test_SDArray extends SDArray {

    private boolean Debug = false;
    private int origShape[];

    /**
     * Constructs a new <code>test_SDArray</code>.
     */
    public test_SDArray() {
        super();
    }

    /**
     * Constructs a new <code>test_SDArray</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public test_SDArray(String n) {
        super(n);
    }


    public void cacheShape() {

        origShape = new int[numDimensions()];

        try {
            for (int dim = 0; dim < numDimensions(); dim++) {
                DArrayDimension dad = getDimension(dim);
                origShape[dim] = dad.getSize();
            }
        }
        catch (InvalidParameterException e) {
            System.out.println("ERROR! Unresolved problem in test_SDArray.cacheShape!");
        }

    }

    public int getCachedShape(int dim) {
        if (dim < origShape.length)
            return (origShape[dim]);
        else
            return (-1);
    }

    /**
     * Read a value from the named dataset for this variable.
     *
     * @param datasetName String identifying the file or other data store
     *                    from which to read a vaue for this variable.
     * @param specialO    This <code>Object</code> is a goody that is used by Server implementations
     *                    to deliver important, and as yet unknown, stuff to the read method. If you
     *                    don't need it, make it a <code>null</code>.
     * @return <code>true</code> if more data remains to be read, otherwise
     *         <code>false</code>. This is an abtsract method that must be implemented
     *         as part of the installation/localization of a OPeNDAP server.
     * @throws IOException
     * @throws EOFException
     */
    public boolean read(String datasetName, Object specialO)
            throws NoSuchVariableException, IOException, EOFException {

        testEngine te = (testEngine) specialO;

        te.newLoadTestArray(datasetName, this);

        setRead(true);
        return (false);
    }
}


