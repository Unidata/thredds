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



package opendap.dap.test;


import java.io.*;

import opendap.dap.*;
//import opendap.dap.XMLparser.DDSXMLParser;
import opendap.dap.Server.*;


/**
 * @author ndp
 * @version $Revision: 15901 $
 */
public class ddxTest {

    /**
     * Constructs a new <code>XmlTest</code>.
     */
    public ddxTest() {
        super();
    }


    public static void main(String args[]) throws Exception {
        // Parse each file provided on the
        // command line.


        String dasName = args[0];
        String ddsName = args[1];


        try {
            FileInputStream ddsFIS = new FileInputStream(ddsName);
            System.out.println("Parsing DDS File: '" + ddsName + "'");
            DefaultFactory btf = new DefaultFactory();
            //test_ServerFactory btf = new test_ServerFactory();
            ServerDDS dds = new ServerDDS(btf);
            dds.parse(ddsFIS);
            System.out.println("\n\n\n\nTHE DDS:\n");
            dds.print(System.out);

            FileInputStream dasFIS = new FileInputStream(dasName);
            System.out.println("Parsing DAS File: '" + dasName + "'");
            DAS das = new DAS();
            das.parse(dasFIS);
            System.out.println("\n\nThe DAS:\n");
            das.print(System.out);

            dds.ingestDAS(das);
            System.out.println("\n\nThe DDX:\n");
            dds.printXML(System.out);

        } catch (DDSException de) {
            System.out.println("Exception thrown to top level, exiting...");
            System.out.println("MESSAGE: \n" + de.getMessage());
        }


    }


}


