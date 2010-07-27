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

import java.io.*;
import java.util.Vector;

import opendap.dap.NoSuchVariableException;
import opendap.dap.BaseType;
import opendap.dap.Server.SDSequence;
import opendap.dap.Server.ServerMethods;

/**
 * Holds a OPeNDAP Server <code>Sequence</code> value.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @see BaseType
 */
public class test_SDSequence extends SDSequence {

    private static final boolean _Debug = false;

    private int sMaxLength = 5;
    private int sCount = 0;


    /**
     * Constructs a new <code>test_SDSequence</code>.
     */
    public test_SDSequence() {


        super();

    }

    /**
     * Constructs a new <code>test_SDSequence</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public test_SDSequence(String n) {
        super(n);
    }

    // --------------- FileIO Interface

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

        boolean retVal, addRow = false;
        Vector rv = null;

        testEngine te = (testEngine) specialO;

        if (_Debug)
            System.out.println("\nReading row " + sCount + " of Sequence \"" + getName() + "\" from " + datasetName + ":");

        rv = getRowVector();

        for (int i = 0; i < rv.size(); i++) {

            ServerMethods sm = (ServerMethods) rv.get(i);

            if (_Debug)
                System.out.println("Reading variable: " + ((BaseType) sm).getTypeName() + ", " + ((BaseType) sm).getName());

            if (sm.isProject()) {
                sm.read(datasetName, specialO);
                if (_Debug) ((BaseType) rv.get(i)).printVal(System.out, "   ");
            }
        }


        sCount++;
        if (sCount < te.getMaxSequenceLength()) {
            retVal = true;
        } else {
            sCount = 0;
            retVal = false;
        }

        setRead(true);

        if (_Debug) System.out.println("Read finished. Returning: " + retVal);
        if (_Debug && !retVal) System.out.println("\n...........");
        return (retVal);
    }
}















