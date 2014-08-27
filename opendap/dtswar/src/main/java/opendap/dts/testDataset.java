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
import opendap.dap.parsers.*;
import opendap.servers.*;
import opendap.servlet.GuardedDataset;
import opendap.servlet.ReqState;
import opendap.util.Debug;


/**
 * This is the OPeNDAP Test servlet (dts). It allows the owner of the server
 * to deliver data in ANY valid DDS to a client. This DDS will be
 * filled with invented data if the client requests a DataDDS.
 * This kind of test fixture is useful for evaluating a clients
 * ability to handle the various complexities of the OPeNDAP data
 * types.
 *
 * @author Nathan David Potter
 * @version $Revision: 15901 $
 */


public class testDataset implements GuardedDataset
{


    private ReqState rs;

    public testDataset(ReqState rs)
    {
        this.rs = rs;
    }

    private Exception DDXfailure;
    private Exception DDSfailure;


    public void release()
    {
    } // noop

    public void close()
    {
    } // noop


    /**
     * ************************************************************************
     * For the test server this method does the following:
     * <ul>
     * <li> Makes a new test_ServerFactory (aka BaseTypeFactory) for the
     * dataset requested.
     * <li> Instantiates a ServerDDS using the test_ServerFactory and populates
     * it (this is accomplished by opening a locally cached DDS from a file
     * and parsing it)
     * <li> Returns this freshly minted ServerDDS object (to the servlet code
     * where it is used.)
     * </ul>
     *
     * @return The <code>ServerDDS</code> for the named data set.
     * @see opendap.servers.ServerDDS
     * @see test_ServerFactory
     */
    public ServerDDS getDDS() throws DAP2Exception, ParseException
    {

        ServerDDS myDDS = null;
        DataInputStream dds_source = null;


        try {

            myDDS = getMyDDS();

            // Try to get an open an InputStream that contains the DDX for the
            // requested dataset.
            dds_source = openCachedDDX(rs);

            if(dds_source != null) { // Did it work?

                // Then parse the DDX
                myDDS.parseXML(dds_source, true);
                //myDDS.setBlobURL(rs.getDodsBlobURL());

            } else { // Ok, no DDX,

                // Try to get an open an InputStream that contains the DDS for the
                // requested dataset.
                dds_source = openCachedDDS(rs);

                if(dds_source != null) { // Did it work?

                    // Then parse the DDS
                    myDDS.parse(dds_source);
                    //myDDS.setBlobURL(rs.getDodsBlobURL());

                    // ADD the DAS to the DDS here!! (REsults in a DDX)

                    myDDS.ingestDAS(getDAS());

                } else { // Ok, no DDS. It's a bum reference.
                    throw new DAP2Exception(opendap.dap.DAP2Exception.CANNOT_READ_FILE,
                        "Cannot find a DDX or DDS file that matches the Dataset you have requested.\n" +
                            getClass().getName() + ".openCachedDDX() said: " + DDXfailure.getMessage() + "\n" +
                            getClass().getName() + ".openCachedDDS() said: " + DDSfailure.getMessage() + "\n");
                }
            }

        } finally {
            try {
                if(dds_source != null)
                    dds_source.close();
            } catch (IOException ioe) {
                throw new DAP2Exception(opendap.dap.DAP2Exception.UNKNOWN_ERROR, ioe.getMessage());
            }

        }

        return (myDDS);

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Opens a DDX cached on local disk. This can be used on OPeNDAP servers (such
     * as the OPeNDAP SQL Server) that rely on locally cached DDX files as opposed
     * to dynamically generated DDS's.
     * <p/>
     * <p>This method uses the <code>ServletConfig</code> object cached by in
     * the  <code>ReqState</code> object to locate the servlet init parameter
     * <i>DDXcache</i> to determine where to look for the cached DDX document.
     * If the init parameter was
     * not set then the <code>ReqState.defaultDDXcache</code> variable is
     * used as the location of the DDX cache.
     * <p/>
     * <p>If the DDX cannot be found an error is sent back to the client.
     *
     * @param rs The <code>ReqState</code> object for this
     *           invocation of the servlet.
     * @return An open <code>DataInputStream</code> from which the DDX can
     * be read.
     * @see ReqState
     */
    public DataInputStream openCachedDDX(ReqState rs)
    {


        String cacheDir = rs.getDDXCache(rs.getRootPath());
        if(Debug.isSet("probeRequest")) {
            DTSServlet.log.debug("DDXCache: " + cacheDir);
            DTSServlet.log.debug("Attempting to open: '" + cacheDir + rs.getDataSet() + "'");
        }

        try {

            // go get a file stream that points to the requested DDSfile.

            File fin = new File(cacheDir + rs.getDataSet());
            FileInputStream fp_in = new FileInputStream(fin);
            DataInputStream ddx_source = new DataInputStream(fp_in);

            return (ddx_source);
        } catch (FileNotFoundException fnfe) {
            DDXfailure = fnfe;
            return (null);
        }


    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Opens a DDS cached on local disk. This can be used on OPeNDAP servers (such
     * as the OPeNDAP SQL Server) that rely on locally cached DDS files as opposed
     * to dynamically generated DDS's.
     * <p/>
     * <p>This method uses the <code>ServletConfig</code> object cached by in
     * the  <code>ReqState</code> object to locate the servlet init parameter
     * <i>DDScache</i> to determine where to look for the cached <code>
     * DDS</code>. If the init parameter was not set then the
     * <code>ReqState.defaultDDScache</code> variable is used as the
     * location of the DDS cache.
     * <p/>
     * <p>If the DDS cannot be found an error is sent back to the client.
     *
     * @param rs The <code>ReqState</code> object for this
     *           invocation of the servlet.
     * @return An open <code>DataInputStream</code> from which the DDS can
     * be read.
     * @see ReqState
     */
    public DataInputStream openCachedDDS(ReqState rs)
    {


        String cacheDir = rs.getDDSCache(rs.getRootPath());

        if(Debug.isSet("probeRequest")) {
            DTSServlet.log.debug("DDSCache: " + cacheDir);
            DTSServlet.log.debug("Attempting to open: '" + cacheDir + rs.getDataSet() + "'");
        }


        try {

            // go get a file stream that points to the requested DDSfile.

            File fin = new File(cacheDir + rs.getDataSet());
            FileInputStream fp_in = new FileInputStream(fin);
            DataInputStream dds_source = new DataInputStream(fp_in);

            return (dds_source);
        } catch (FileNotFoundException fnfe) {
            DDSfailure = fnfe;
            return (null);
        }


    }
    /***************************************************************************/


    /**
     * ***********************************************************************
     */
    private ServerDDS getMyDDS()
    {

        ServerDDS myDDS = null;

        // Get your class factory and instantiate the DDS
        test_ServerFactory sfactory = new test_ServerFactory();


        myDDS = new ServerDDS(rs.getDataSet(), sfactory, rs.getSchemaLocation());

        return (myDDS);

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * <p/>
     * In this (default) implementation of the getDAS() method a locally cached
     * DAS is retrieved and parsed. In this method the DAS for the passed dataset
     * is loaded from the "DAScache" &lt;init-param&gt; in the web.xml
     * file. If the there is no file available a DAP2Exception is
     * thrown. It is certainly possible (and possibly very desirable) to override
     * this method when overriding the getDDS() method. One reason for doing this
     * is if the OPeNDAP server being implemented can generate the DAS information
     * dynamically.
     * <p/>
     * When overriding this method be sure that it does the following:
     * <ul>
     * <li> Instantiates the DAS for the indicated (passed) dataset and
     * populates it. This is accomplished in the default implementation
     * by opening a (cached?) DAS stored in a file and parsing it. In
     * a different implementation it could be created dynamically.
     * <li> Returns this freshly minted DAS object. (to the servlet code where it is used.)
     * </ul>
     *
     * @return The DAS object for the data set specified in the parameter <code>dataSet</code>
     * @see opendap.dap.DAS
     */
    public DAS getDAS() throws DAP2Exception, ParseException
    {

        DAS myDAS = null;
        boolean gotDDX = false;
        boolean gotDDS = false;
        boolean gotDAS = false;
        ServerDDS myDDS = getMyDDS();

        /*
        dds_source = openCachedDDX(rs);
        if (dds_source != null) { // Did it work?

            // Then parse the DDX
            myDDS.parseXML(dds_source, true);
            //myDDS.setBlobURL(rs.getDodsBlobURL());

            // Get the DAS object (if possible)
            myDAS = myDDS.getDAS();
            gotDDX = true;

        } else   // Ok, no DDX
        */
        try {
            try (
                // Try to get an open an InputStream that
                // contains the DDS for the requested DAS.
                DataInputStream dds_source = openCachedDDS(rs);
            ) {
                if(dds_source != null) { // Did it work?
                    // Then parse the DDS
                    if((gotDDS = myDDS.parse(dds_source))) {
                        //myDDS.setBlobURL(rs.getDodsBlobURL());
                        DTSServlet.log.debug("Got DDS.");
                    }
                }
            }
        } catch (IOException ioe) {
            throw new DAP2Exception(ioe);
        }
        DTSServlet.log.debug("-------------");

        myDAS = new DAS();
        try {
            // Try to get an open an InputStream that contains the DDX for the
            // requested dataset.
            try (
                DataInputStream is = openCachedDAS(rs);
            ) {
                if((gotDAS = myDAS.parse(is))) {
                    DTSServlet.log.debug("Got DAS");
                }
                if(gotDAS && gotDDS) {
//                    DTSServlet.log.debug("-------------");
//                    myDAS.print(System.out);
//                    DTSServlet.log.debug("-------------");
                    myDDS.ingestDAS(myDAS);
//                    DTSServlet.log.debug("DDS ingested DAS.");
                    myDAS = myDDS.getDAS();
//                    DTSServlet.log.debug("-------------");
//                    myDAS.print(System.out);
//                    DTSServlet.log.debug("-------------");
                }
            }
        } catch (FileNotFoundException fnfe) { // Ok, no DAS. It's a bum reference.
            // This is no big deal. We just trap it and return an
            // empty DAS object.
            if(gotDDS)
                myDAS = myDDS.getDAS();
            gotDAS = false;
        } catch (IOException ioe) {
            throw new DAP2Exception(ioe);
        }
        if(gotDAS) {
            if(gotDDX)
                DTSServlet.log.debug("Got DAS from DDX for dataset: " + rs.getDataSet());
            else if(gotDDS)
                DTSServlet.log.debug("Got DAS, popped it into a DDS, and got back a complete DAS for dataset: " + rs.getDataSet());
            else
                DTSServlet.log.debug("Successfully opened and parsed DAS cache for dataset: " + rs.getDataSet());
        } else if(gotDDS)
            DTSServlet.log.debug("No DAS! Got a DDS, and sent a complete (but empty) DAS for dataset: " + rs.getDataSet());
        else
            DTSServlet.log.debug("No DAS or DDS present for dataset: " + rs.getDataSet());

        return (myDAS);
    }

    /***************************************************************************/


    /**
     * ************************************************************************
     * Opens a DAS cached on local disk. This can be used on OPeNDAP servers (such
     * as the OPeNDAP SQL Server) that rely on locally cached DAS files as opposed
     * to dynamically generated DAS's.
     * <p/>
     * <p>This method uses the <code>ServletConfig</code> object cached by in
     * the  <code>ReqState</code> object to locate the servlet init parameter
     * <i>DAScache</i> to determine where to look for the cached <code>
     * DAS</code>. If the init parameter was not set then the
     * <code>ReqState.defaultDAScache</code> variable is used as the
     * location of the DAS cache.
     * <p/>
     * <p>If the DAS cannot be found an error is sent back to the client.
     *
     * @param rs The <code>ReqState</code> object for this
     *           invocation of the servlet.
     * @return An open <code>DataInputStream</code> from which the DAS can
     * be read.
     * @throws FileNotFoundException
     * @see ReqState
     */
    public DataInputStream openCachedDAS(ReqState rs) throws FileNotFoundException
    {


        String cacheDir = rs.getDASCache(rs.getRootPath());

        if(Debug.isSet("probeRequest")) {
            DTSServlet.log.debug("DASCache: " + cacheDir);
            DTSServlet.log.debug("Attempting to open: '" + cacheDir + rs.getDataSet() + "'");
        }

        // go get a file stream that points to the requested DASfile.
        File fin = new File(cacheDir + rs.getDataSet());
        FileInputStream fp_in = new FileInputStream(fin);
        DataInputStream das_source = new DataInputStream(fp_in);
        return (das_source);


    }
    /***************************************************************************/


}





