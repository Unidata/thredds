// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package dods.servlet;

import  dods.dap.DODSException;
import  dods.dap.parser.ParseException;
import  dods.dap.Server.ServerDDS;
import  dods.dap.DAS;


/**
 *  A GuardedDataset allows us to handle multithreaded stateful processing.
 *
 *  In a multi-threaded environment such as a Servlet, multiple requests for the
 *  same dataset may happen at the same time. This is a problem only when there is
 *  some state that is being maintained. Caching strategies (eg the netcdf server
 *  caches open netcdf files) may need to maintain state information in order to manage
 *  resources efficiently.
 *
 *  All accesses to the DDS and DAS are made through the GuardedDataset.
 *  Typically the server puts a mutex lock on the resource when getDDS() or getDAS()
 *  is called. When the dataset processing is complete, release() is called, and
 *  the server releases the mutex.
 *
 *  Example use:
 *
 *  <pre><code>
    public void doGetDAS(HttpServletRequest request,
                         HttpServletResponse response,
			 ReqState rs)
			 throws IOException, ServletException {

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server",  getServerVersion() );
        response.setHeader("Content-Description", "dods_dds");
        OutputStream Out = new BufferedOutputStream(response.getOutputStream());

        GuardedDataset ds = null;
        try {
          ds = getDataset(rs);
          DAS myDAS = ds.getDAS();            // server would lock here
          myDAS.print(Out);
          response.setStatus(response.SC_OK);
        }
        catch (DODSException de){
          dodsExceptionHandler(de,response);
        }
        catch (ParseException pe) {
          parseExceptionHandler(pe,response);
        }
        finally {                           // release lock if needed
          if (ds != null) ds.release();
        }
   }
   </code></pre>

 *  Its important that the DDS or DAS not be used after release() is called.
 *
 *  See dods.servers.netcdf.NcDataset for example of implementing a locking
 *  GuardedDataset.
 *  If a server is not keeping state, it can simply pass the DDS and DAS without locking,
 *  and implement a dummy release() method.
 *
 *  @author jcaron
 *  @see dods.servers.netcdf.GuardedDatasetImpl
 */

public interface GuardedDataset {

  /**
   * Get the DDS for this Dataset.
   *
   * @return the ServerDDS
   * @throws DODSException
   * @throws ParseException
   */
  public ServerDDS getDDS() throws DODSException, ParseException;

  /**
   * Get the DAS for this Dataset.
   *
   * @return the DAS
   * @throws DODSException
   * @throws ParseException
   */
  public DAS getDAS() throws DODSException, ParseException;

  /**
   * Release the lock, if any, on this dataset.
   */
  public void release();

}
