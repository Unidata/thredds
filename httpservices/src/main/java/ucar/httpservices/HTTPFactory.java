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

package ucar.httpservices;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

/**
 * HTTPFactory creates method instance.
 * This code was originally in HttpMethod.
 */

public class HTTPFactory
{

    //////////////////////////////////////////////////////////////////////////
    // Static factory methods for creating HTTPSession instances

    static public HTTPSession newSession() throws HTTPException
    {
        return new HTTPSession();
    }

    static public HTTPSession newSession(String legalurl) throws HTTPException
    {
        return new HTTPSession(legalurl);
    }

    //////////////////////////////////////////////////////////////////////////
    // Static factory methods for creating HTTPMethod instances

    static public HTTPMethod Get(HTTPSession session) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Get, session, null);
    }

    static public HTTPMethod Head(HTTPSession session) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Head, session, null);
    }

    static public HTTPMethod Put(HTTPSession session) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Put, session, null);
    }

    static public HTTPMethod Post(HTTPSession session) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Post, session, null);
    }

    static public HTTPMethod Options(HTTPSession session) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Options, session, null);
    }

    static public HTTPMethod Get(HTTPSession session, String legalurl) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Get, session, legalurl);
    }

    static public HTTPMethod Head(HTTPSession session, String legalurl) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Head, session, legalurl);
    }

    static public HTTPMethod Put(HTTPSession session, String legalurl) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Put, session, legalurl);
    }

    static public HTTPMethod Post(HTTPSession session, String legalurl) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Post, session, legalurl);
    }

    static public HTTPMethod Options(HTTPSession session, String legalurl) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Options, session, legalurl);
    }

    static public HTTPMethod Get(String legalurl) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Get, legalurl);
    }

    static public HTTPMethod Head(String legalurl) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Head, legalurl);
    }

    static public HTTPMethod Put(String legalurl) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Put, legalurl);
    }

    static public HTTPMethod Post(String legalurl) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Post, legalurl);
    }

    static public HTTPMethod Options(String legalurl) throws HTTPException
    {
        return new HTTPMethod(HTTPSession.Methods.Options, legalurl);
    }

}
