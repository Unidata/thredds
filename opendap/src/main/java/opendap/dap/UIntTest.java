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

package opendap.dap;

import java.io.*;

class UIntTest {


    UIntTest() {
    }


    public void sendIt(DataOutputStream fp) throws Exception {

        short s;
        byte b;
        int i;
        long l;

        s = ((short) 65500);
        System.out.println("\nShort assigned to 65500.    System thinks of it as: " + s);
        fp.writeShort(s);
        System.out.println("Wrote it to disk. ");

        s = ((short) 65537);
        System.out.println("\nShort assigned to 65537.    System thinks of it as: " + s);
        fp.writeShort(s);
        System.out.println("Wrote it to disk. ");


        i = ((int) 4294967040L);
        System.out.println("\nInt assigned to 4294967040. System thinks of it as: " + i);
        fp.writeInt(i);
        System.out.println("Wrote it to disk. ");

        i = ((int) 4294967298L);
        System.out.println("\nInt assigned to 4294967298. System thinks of it as: " + i);
        fp.writeInt(i);
        System.out.println("Wrote it to disk. ");

    }


    public void getIt(DataInputStream fp) throws Exception {

        short s;
        int i1, i2;
        long l;


        System.out.println("\nReading data...");
        s = fp.readShort();
        System.out.println("System read short from file as: " + s);
        i1 = ((int) s);
        System.out.println("Converted short to int: " + i1);
        i1 = i1 & 0xFFFF;
        System.out.println("And'd with 0xFFFF (represented as an int in memory): " + i1);

        System.out.println("\nReading data...");
        s = fp.readShort();
        System.out.println("System read short from file as: " + s);
        i1 = ((int) s);
        System.out.println("Converted short to int: " + i1);
        i1 = i1 & 0xFFFF;
        System.out.println("And'd with 0xFFFF (represented as an int in memory): " + i1);


        System.out.println("\nReading data...");
        i2 = fp.readInt();
        System.out.println("\nSystem read int from file as: " + i2);
        l = ((long) i2);
        System.out.println("Converted int to long: " + l);
        l = l & 0xFFFFFFFFL;
        System.out.println("And'd with 0xFFFFFFFFL (represented as a long in memory): " + l);

        System.out.println("\nReading data...");
        i2 = fp.readInt();
        System.out.println("\nSystem read int from file as: " + i2);
        l = ((long) i2);
        System.out.println("Converted int to long: " + l);
        l = l & 0xFFFFFFFFL;
        System.out.println("And'd with 0xFFFFFFFFL (represented as a long in memory): " + l);


    }


    static public void main(String args[]) throws Exception {

        UIntTest b = new UIntTest();
        File f = new File("UIntTest.bin");
        FileOutputStream fp = new FileOutputStream(f);
        DataOutputStream sink = new DataOutputStream(fp);

        b.sendIt(sink);
        sink.close();

        FileInputStream ifp = new FileInputStream(f);
        DataInputStream source = new DataInputStream(ifp);

        b.getIt(source);
        source.close();

    }

}



