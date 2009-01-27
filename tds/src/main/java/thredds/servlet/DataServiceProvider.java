/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.servlet;

import thredds.crawlabledataset.CrawlableDataset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Implement this interface to recognize and handle dataset requests.
 *
 * @author edavis
 * @since Mar 24, 2006 10:57:44 AM
 */
public interface DataServiceProvider
{
  /**
   * If this DataServiceProvider recognizes the request as a dataset request,
   * return the dataset path as a DatasetRequest object. Otherwise, return
   * null. The DatasetRequest object can contain information specific to this
   * type of DataServiceProvider beyond just the dataset path.
   *
   * @param path the request path.
   * @param req the request.
   * @return the dataset path as a DatasetRequest object.
   */
  public DatasetRequest getRecognizedDatasetRequest( String path, HttpServletRequest req );

  /**
   * Handle a dataset request. Should only be called if
   * getRecognizedDatasetRequest() returns a non-null DatasetRequest.
   *
   * Implementations should check that the request is for an allowed dataset
   * by using CatalogRootHandler.findRequestedDatasets(). If it is not an
   * allowed dataset, should respond with an HTTP 404 (Not Found) response.
   *
   * @param dsReq the dataset request info, should be the output of getRecognizedDatasetRequest().
   * @param crDs the CrawlableDataset to be served (corresponds to the DatasetRequest path).
   * @param req the HttpServletRequest
   * @param res the HttpServletResponse
   * @throws IOException if can't complete request due to IO problems.
   * @throws IllegalArgumentException if the DatasetRequest is null or the CrawlableDataset is null.
   */
  public void handleRequestForDataset( DatasetRequest dsReq, CrawlableDataset crDs,
                                       HttpServletRequest req, HttpServletResponse res )
          throws IOException;

  /**
   * Handle requests that are not recognized by this DataServiceProvider and
   * where the CrawlableDataset is not a collection.
   *
   * @param crDs the CrawlableDataset that corresponds to the request path.
   * @param req the HttpServletRequest
   * @param res the HttpServletResponse
   * @throws IOException if the request can't be completed due to IO problems.
   */
  public void handleUnrecognizedRequest( CrawlableDataset crDs,
                                         HttpServletRequest req, HttpServletResponse res )
          throws IOException;

  /**
   * Handle requests that are not recognized by this DataServiceProvider and
   * where the CrawlableDataset is a collection.
   *
   * Note: This method is to allow some flexibility in how unrecognized
   * requests are handled. Can be handled in the same way as
   * handleUnrecognizedRequest() if desired.
   *
   * @param crDs the CrawlableDataset that corresponds to the request path.
   * @param req the HttpServletRequest
   * @param res the HttpServletResponse
   * @throws IOException if the request can't be completed due to IO problems.
   */
  public void handleUnrecognizedRequestForCollection( CrawlableDataset crDs,
                                                      HttpServletRequest req, HttpServletResponse res )
          throws IOException;

  /**
   * Contain information on the dataset request.
   *
   * Note: An implementation of DatasetRequest may contain any dataset request
   * information necessary to the target DataServiceProvider, it must contain
   * the dataset path as specified by this interface.
   */
  public interface DatasetRequest
  {
    /**
     * Return the dataset path for this dataset request.
     *
     * @return the dataset path.
     */
    public String getDatasetPath();
  }
}
/*
 * $Log: DataServiceProvider.java,v $
 * Revision 1.2  2006/04/28 21:45:15  edavis
 * Clean up some logging stuff.
 *
 * Revision 1.1  2006/03/30 23:22:10  edavis
 * Refactor THREDDS servlet framework, especially CatalogRootHandler and ServletUtil.
 *
 */