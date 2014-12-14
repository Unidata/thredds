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

import opendap.dap.*;
import opendap.servers.*;

/**
 * Used by the test server to reset the server output
 * for each new client request.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @see BaseType
 */
public class testEngine {

    private static boolean _Debug = false;

    private int sequenceLength;

    private boolean tBool;
    private byte tByte;

    private float tFloat32;
    private double tFloat64;

    private short tUint16;
    private short tInt16;

    private int tUint32;
    private int tInt32_1;
    private int tInt32_2;

    private String tURL;
    private int tStringCount;


    /**
     *
     * @param sLength Length of test Sequences
     */
    public testEngine(int sLength) {

        //unconstrainedDDS = s;

        sequenceLength = sLength;

        tBool = false;
        tByte = 0;

        tFloat32 = (float) 0.0;
        tFloat64 = 0.0;

        tUint16 = 0;
        tInt16 = 0;

        tUint32 = 0;
        tInt32_1 = 0;
        tInt32_2 = 1;

        tURL = "http://www.dods.org";
        tStringCount = 0;
    }


    public int getMaxSequenceLength() {
        return (sequenceLength);
    }

    public boolean nextBool() {
        tBool = !tBool;
        return (tBool);
    }


    public byte nextByte() {
        return (tByte++);
    }


    public float nextFloat32() {
        float b = (float) (100 * Math.sin(tFloat32));
        tFloat32 += 0.01;
        return (b);
    }


    public double nextFloat64() {
        double b = (double) 1000 * Math.cos(tFloat64);

        tFloat64 += 0.01;
        return (b);
    }


    public short nextUint16() {
        short b = (short) (-16 * tUint16);
        tUint16++;
        return (b);
    }


    public short nextInt16() {
        short b = (short) (16 * tInt16);
        tInt16++;
        return (b);
    }


    public int nextUint32() {
        int b = tUint32++ * tUint32;
        return (b);
    }


    public int nextInt32() {
        int b;
        b = tInt32_1 + tInt32_2;
        tInt32_1 = tInt32_2;
        tInt32_2 = b;
        return (b);
    }


    public String nextURL() {
        return (tURL);
    }


    public String nextString() {
        String b = "This is a data test string (pass " + tStringCount + ").";
        tStringCount++;
        return (b);
    }

    //**************************************************************************
    //
    // New ARRAY LOADER
    //
    //
    //
    //...........................................................................

    /**
     * Loads test Arrays full of data
     *
     * @param datasetName Name of dataset
     * @param ta  Array to load up
     * @throws IOException When crap goes wrong
     */
    public void newLoadTestArray(String datasetName, test_SDArray ta) throws
            IOException {


        if (_Debug) DTSServlet.log.debug("testEngine.newLoadTestArray(" + datasetName + "): ");
        try {
            if (_Debug) DTSServlet.log.debug("Loading: " +
                    ta.getEncodedName() +
                    " an SDArray of " +
                    ta.numDimensions() +
                    " dimension(s).");

            PrimitiveVector pv = ta.getPrimitiveVector();

            if (_Debug) {
                Class cl = pv.getClass();
                DTSServlet.log.debug("PrimitiveVector is a: " + cl.getName());
            }


            pv.setLength(getLength(ta, 0, true));
            if (_Debug) DTSServlet.log.debug("Length: " + pv.getLength());

            newLoadArray(datasetName, ta);


        }
        catch (DAP2Exception e) {
            // Don't bother to do a thing.
        }
        if (_Debug) DTSServlet.log.debug("---------------------------------------------------");


    }


    private int getLength(test_SDArray ta, int dim, boolean constrained) throws InvalidDimensionException {


        int sizeofOtherDims = 1;

        if (dim + 1 < ta.numDimensions())
            sizeofOtherDims = getLength(ta, dim + 1, constrained);

        DArrayDimension dad = ta.getDimension(dim);

        int sizeofThisDim;

        if (constrained) {
            if (_Debug) DTSServlet.log.debug("Scanning Dimension " + dim +
                    "  start: " + dad.getStart() +
                    "  stop: " + dad.getStop() +
                    "  stride: " + dad.getStride());

            sizeofThisDim = 1 + (dad.getStop() - dad.getStart()) / dad.getStride();
        } else {
            sizeofThisDim = dad.getSize();
        }

        int eCount = sizeofThisDim * sizeofOtherDims;

        if (_Debug) DTSServlet.log.debug("  length: " + sizeofThisDim);

        return (eCount);
    }


    private int nuAI(int constrainedIndex, test_SDArray ta) throws InvalidDimensionException {

        int  uI, k, dim;
        DArrayDimension dad;

        int cIndices[] = new int[ta.numDimensions()];
        int uIndices[] = new int[ta.numDimensions()];
        int cDimSteps[] = new int[ta.numDimensions()];
        int uDimSteps[] = new int[ta.numDimensions()];


        if (_Debug) DTSServlet.log.debug("ConstrainedIndex: " + constrainedIndex);


        dim = ta.numDimensions() - 1;
        cDimSteps[dim] = 1;
        uDimSteps[dim] = 1;
        for (dim = ta.numDimensions() - 2; dim >= 0; dim--) {
            dad = ta.getDimension(dim + 1);
            cDimSteps[dim] = cDimSteps[dim + 1] * dad.getSize();
            uDimSteps[dim] = uDimSteps[dim + 1] * ta.getCachedShape(dim + 1);
        }

        if (_Debug) {
            DTSServlet.log.debug("DimSteps: ");
            for (dim = 0; dim < ta.numDimensions(); dim++) {
                DTSServlet.log.debug("    cDimSteps[" + dim + "]: " + cDimSteps[dim]);
            }
            DTSServlet.log.debug("");
            for (dim = 0; dim < ta.numDimensions(); dim++) {
                DTSServlet.log.debug("    uDimSteps[" + dim + "]: " + uDimSteps[dim]);
            }
        }


        if (_Debug) DTSServlet.log.debug("cIndices: ");

        k = 0;
        for (dim = 0; dim < (ta.numDimensions() - 1); dim++) {


            cIndices[dim] = (constrainedIndex - k) / cDimSteps[dim];

            if (_Debug) DTSServlet.log.debug("cIndices[" + dim + "]: " + cIndices[dim] + "  k: " + k);

            k += cIndices[dim] * cDimSteps[dim];

        }

        cIndices[dim] = (constrainedIndex - k);
        if (_Debug) DTSServlet.log.debug("cIndices[" + dim + "]: " + cIndices[dim] + "  k: " + k);


        if (_Debug) DTSServlet.log.debug("uIndices: (");
        for (dim = 0; dim < ta.numDimensions(); dim++) {
            dad = ta.getDimension(dim);
            uIndices[dim] = dad.getStart() + cIndices[dim] * dad.getStride();
            if (_Debug) DTSServlet.log.debug(uIndices[dim] + ", ");
        }
        if (_Debug) DTSServlet.log.debug(")");

        uI = 0;
        for (dim = 0; dim < ta.numDimensions(); dim++) {
            uI += uIndices[dim] * uDimSteps[dim];
        }

        return (uI);


    }


    private void newLoadArray(String dataset, test_SDArray ta) throws InvalidDimensionException, NoSuchVariableException, EOFException, IOException {


        PrimitiveVector pv = ta.getPrimitiveVector();

        if (_Debug) DTSServlet.log.debug("Loading Array... ");

        for (int j = 0; j < pv.getLength(); j++) {
            if (_Debug) DTSServlet.log.debug("..\n");


            int i = nuAI(j, ta);

            if (_Debug) DTSServlet.log.debug("ConstrainedIndex: " + j + "   UnconstrainedIndex: " + i);

            if (pv instanceof BaseTypePrimitiveVector) {

                // get the archetype for this BaseType
                BaseType bt = ((BaseTypePrimitiveVector) pv).getTemplate();

                // Clone It.
                BaseType newBT = (BaseType) bt.clone();

                // Give it a new and appropriate name.
                newBT.setEncodedName(newBT.getEncodedName() + "[" + j + "]");

                // Populate this Array member with the new object.
                ((BaseTypePrimitiveVector) pv).setValue(j, newBT);

                // Get the clone back by use the get value method
                ServerMethods sm = (ServerMethods) ((BaseTypePrimitiveVector) pv).getValue(j);

                // go and read some data into this newly minted bastard
                boolean MoreToRead = !(bt instanceof DSequence);
                while (MoreToRead) {
                    MoreToRead = sm.read(dataset, this);
                }


            }

            if (pv instanceof BooleanPrimitiveVector) {
                if (i % 2 != 0)
                    ((BooleanPrimitiveVector) pv).setValue(j, true);
                else
                    ((BooleanPrimitiveVector) pv).setValue(j, false);
            }
            if (pv instanceof BytePrimitiveVector) {
                ((BytePrimitiveVector) pv).setValue(j, (byte) i);
            }
            if (pv instanceof Float32PrimitiveVector) {
                ((Float32PrimitiveVector) pv).setValue(j, (float) Math.sin(i / 100.0));
            }
            if (pv instanceof Float64PrimitiveVector) {
                ((Float64PrimitiveVector) pv).setValue(j, Math.cos(i / 100.0));
            }
            if (pv instanceof Int16PrimitiveVector) {
                ((Int16PrimitiveVector) pv).setValue(j, (short) (i * 256));
            }
            if (pv instanceof UInt16PrimitiveVector) {
                ((UInt16PrimitiveVector) pv).setValue(j, (short) (i * 1024));
            }
            if (pv instanceof Int32PrimitiveVector) {
                ((Int32PrimitiveVector) pv).setValue(j, i * 2048);
            }
            if (pv instanceof UInt32PrimitiveVector) {
                ((UInt32PrimitiveVector) pv).setValue(j, i * 4096);
            }

        }
    }
    //**************************************************************************


    //**************************************************************************
    //
    // GRID LOADER
    //
    //
    //
    //...........................................................................

    /**
     * Loads test Grids full of data
     */
    public void loadTestGrid(String datasetName, test_SDGrid tg) throws
            NoSuchVariableException,
            IOException,
            EOFException {
        SDArray da = (SDArray) tg.getVar(0);

        if (da.isProject())
            da.read(datasetName, this);

        for (int i = 0; i < da.numDimensions(); i++) {

            try {
                DArrayDimension dad = da.getDimension(i);
            }
            catch (InvalidDimensionException e) {
                throw new NoSuchVariableException(e.getMessage());
            }

            SDArray sam = (SDArray) tg.getVar(i + 1);
            //log.debug("The Map Vector Elements are: " + sam.getName());

            if (sam.isProject())
                sam.read(datasetName, this);

        }

    }
    //**************************************************************************


}


