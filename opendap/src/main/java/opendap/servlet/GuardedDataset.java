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

package opendap.servlet;

import opendap.dap.parser.ParseException;
import opendap.dap.Server.ServerDDS;
import opendap.dap.DAS;


/**
 * A GuardedDataset allows us to handle multithreaded stateful processing.
 * <p/>
 * In a multi-threaded environment such as a Servlet, multiple requests for the
 * same dataset may happen at the same time. This is a problem only when there is
 * some state that is being maintained. Caching strategies (eg the netcdf server
 * caches open netcdf files) may need to maintain state information in order to manage
 * resources efficiently.
 * <p/>
 * All accesses to the DDS and DAS are made through the GuardedDataset.
 * Typically the server puts a mutex lock on the resource when getDDS() or getDAS()
 * is called. When the dataset processing is complete, release() is called, and
 * the server releases the mutex.
 * <p/>
 * Example use:
 * <p/>
 * <pre><code>
 * public void doGetDAS(HttpServletRequest request,
 * HttpServletResponse response,
 * ReqState rs)
 * throws IOException, ServletException {
 * <p/>
 * response.setContentType("text/plain");
 * response.setHeader("XDODS-Server",  getServerVersion() );
 * response.setHeader("Content-Description", "dods-dds");
 * OutputStream Out = new BufferedOutputStream(response.getOutputStream());
 * <p/>
 * GuardedDataset ds = null;
 * try {
 * ds = getDataset(rs);
 * DAS myDAS = ds.getDAS();            // server would lock here
 * myDAS.print(Out);
 * response.setStatus(response.SC_OK);
 * }
 * catch (DAP2Exception de){
 * dap2ExceptionHandler(de,response);
 * }
 * catch (ParseException pe) {
 * parseExceptionHandler(pe,response);
 * }
 * finally {                           // release lock if needed
 * if (ds != null) ds.release();
 * }
 * }
 * </code></pre>
 * <p/>
 * Its important that the DDS or DAS not be used after release() is called.
 * <p/>
 * See opendap.servers.netcdf.NcDataset for example of implementing a locking
 * GuardedDataset.
 * If a server is not keeping state, it can simply pass the DDS and DAS without locking,
 * and implement a dummy release() method.
 *
 * @author jcaron
 */

public interface GuardedDataset {

    /**
     * Get the DDS for this Dataset.
     *
     * @return the ServerDDS
     * @throws opendap.dap.DAP2Exception
     * @throws ParseException
     */
    public ServerDDS getDDS() throws opendap.dap.DAP2Exception, ParseException;

    /**
     * Get the DAS for this Dataset.
     *
     * @return the DAS
     * @throws opendap.dap.DAP2Exception
     * @throws ParseException
     */
    public DAS getDAS() throws opendap.dap.DAP2Exception, ParseException;

    /**
     * Release the lock, if any, on this dataset.
     */
    public void release();

}


