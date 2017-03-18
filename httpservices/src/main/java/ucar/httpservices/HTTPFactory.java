/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *  See the LICENSE file for more information.
 */


package ucar.httpservices;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.message.BasicHttpResponse;

import java.lang.reflect.Constructor;
import java.util.Set;

/**
 * HTTPFactory creates method instance.
 * This code was originally in HttpMethod.
 */

public class HTTPFactory
{
    // In order to test client side code that mocks
    // HTTPMethod, provide a static global
    // than can be set by a test program.

    static public java.lang.Class MOCKMETHODCLASS = null;

    //////////////////////////////////////////////////////////////////////////
    // Static factory methods for creating HTTPSession instances

    static public HTTPSession newSession(String host, int port) throws HTTPException
    {
        return new HTTPSession(host, port);
    }

    static public HTTPSession newSession(String url) throws HTTPException
    {
        return new HTTPSession(url);
    }

    static public HTTPSession newSession(HttpHost target) throws HTTPException
    {
        return new HTTPSession(target);
    }

    @Deprecated
    static public HTTPSession newSession(AuthScope scope) throws HTTPException
    {
        HttpHost hh = new HttpHost(scope.getHost(), scope.getPort(), null);
        return new HTTPSession(hh);
    }

    //////////////////////////////////////////////////////////////////////////
    // Static factory methods for creating HTTPMethod instances

    static public HTTPMethod Get(HTTPSession session, String legalurl) throws HTTPException
    {
        return makemethod(HTTPSession.Methods.Get, session, legalurl);
    }

    static public HTTPMethod Head(HTTPSession session, String legalurl) throws HTTPException
    {
        return makemethod(HTTPSession.Methods.Head, session, legalurl);
    }

    static public HTTPMethod Put(HTTPSession session, String legalurl) throws HTTPException
    {
        return makemethod(HTTPSession.Methods.Put, session, legalurl);
    }

    static public HTTPMethod Post(HTTPSession session, String legalurl) throws HTTPException
    {
        return makemethod(HTTPSession.Methods.Post, session, legalurl);
    }

    static public HTTPMethod Options(HTTPSession session, String legalurl) throws HTTPException
    {
        return makemethod(HTTPSession.Methods.Options, session, legalurl);
    }

    static public HTTPMethod Get(HTTPSession session) throws HTTPException
    {
        return Get(session, null);
    }

    static public HTTPMethod Head(HTTPSession session) throws HTTPException
    {
        return Head(session, null);
    }

    static public HTTPMethod Put(HTTPSession session) throws HTTPException
    {
        return Put(session, null);
    }

    static public HTTPMethod Post(HTTPSession session) throws HTTPException
    {
        return Post(session, null);
    }

    static public HTTPMethod Options(HTTPSession session) throws HTTPException
    {
        return Options(session, null);
    }

    static public HTTPMethod Get(String legalurl) throws HTTPException
    {
        return Get(null, legalurl);
    }

    static public HTTPMethod Head(String legalurl) throws HTTPException
    {
        return Head(null, legalurl);
    }

    static public HTTPMethod Put(String legalurl) throws HTTPException
    {
        return Put(null, legalurl);
    }

    static public HTTPMethod Post(String legalurl) throws HTTPException
    {
        return Post(null, legalurl);
    }

    static public HTTPMethod Options(String legalurl) throws HTTPException
    {
        return Options(null, legalurl);
    }

    /**
     * Common method creation code so we can isolate mocking
     *
     * @param session
     * @return
     * @throws HTTPException
     */
    static protected HTTPMethod makemethod(HTTPSession.Methods m, HTTPSession session, String url)
            throws HTTPException
    {
        HTTPMethod meth = null;
        if(MOCKMETHODCLASS == null) { // do the normal case
            meth = new HTTPMethod(m, session, url);
        } else {//(MOCKMETHODCLASS != null)
            java.lang.Class methodcl = MOCKMETHODCLASS;
            Constructor<HTTPMethod> cons = null;
            try {
                cons = methodcl.getConstructor(HTTPSession.Methods.class, HTTPSession.class, String.class);
            } catch (Exception e) {
                throw new HTTPException("HTTPFactory: no proper HTTPMethod constructor available", e);
            }
            try {
                meth = cons.newInstance(m, session, url);
            } catch (Exception e) {
                throw new HTTPException("HTTPFactory: HTTPMethod constructor failed", e);
            }
        }
        return meth;
    }


    static public Set<String> getAllowedMethods()
    {
        HttpResponse rs = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 0, "");
        Set<String> set = new HttpOptions().getAllowedMethods(rs);
        return set;
    }

}
