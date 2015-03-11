/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////



package opendap.dts;

import java.io.*;
import java.util.Vector;

import opendap.dap.NoSuchVariableException;
import opendap.dap.BaseType;
import opendap.servers.SDSequence;
import opendap.servers.ServerMethods;

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
        if(specialO == null)
            throw new IOException("Null test engine");
        boolean retVal, addRow = false;
        Vector rv = null;

        testEngine te = (testEngine) specialO;

//        if (_Debug) System.out.println("\nReading row " + sCount + " of Sequence \"" + getEncodedName() + "\" from " + datasetName + ":");

        rv = getRowVector();

        for (int i = 0; i < rv.size(); i++) {

            ServerMethods sm = (ServerMethods) rv.get(i);

//            if (_Debug) System.out.println("Reading variable: " + ((BaseType) sm).getTypeName() + ", " + ((BaseType) sm).getEncodedName());

            if (sm.isProject()) {
                sm.read(datasetName, specialO);
//                if (_Debug) ((BaseType) rv.get(i)).printVal(System.out, "   ");
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

//        if (_Debug) System.out.println("Read finished. Returning: " + retVal);
//        if (_Debug && !retVal) System.out.println("\n...........");
        return (retVal);
    }
}















