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

package ucar.nc2.util.net;

import org.apache.http.*;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

abstract public class HTTPUtil
{

    //////////////////////////////////////////////////
    // Header dump interceptors

    static public class RequestHeaderDump implements HttpRequestInterceptor
    {
        @Override
        synchronized public void
        process(HttpRequest request, HttpContext context)
            throws HttpException, IOException
        {
            Header[] hdrs = request.getAllHeaders();
            if(hdrs == null) hdrs = new Header[0];
            System.err.println("Request Headers:");
            for(Header h: hdrs) {
                System.err.println(h.toString());
            }
            System.err.flush();
        }
    }

    static public class ResponseHeaderDump implements HttpResponseInterceptor
    {
        @Override
        synchronized public void
        process(HttpResponse response, HttpContext context)
            throws HttpException, IOException
        {
            Header[] hdrs = response.getAllHeaders();
            if(hdrs == null) hdrs = new Header[0];
            System.err.println("Response Headers:");
            for(Header h: hdrs) {
                System.err.println(h.toString());
            }
            System.err.flush();
        }
    }

}
