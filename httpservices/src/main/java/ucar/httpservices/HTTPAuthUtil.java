/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.httpservices;


import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;

import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;


/**
 * Provide Auth related utilities
 */

@Immutable
abstract public class HTTPAuthUtil
{

    //////////////////////////////////////////////////
    // Constants

    //////////////////////////////////////////////////
    // AuthScope Utilities

    /**
     * Given a session url AuthScope and a Method url AuthScope,
     * Indicate it the are "compatible" as defined as follows.
     * The method AuthScope is <i>compatible</i> with the session AuthScope
     * if its host+port is the same as the session's host+port and its scheme is
     * compatible, where e.g. http is compatible with https.
     * The scope realm is ignored.
     *
     * @param ss Session AuthScope
     * @param ms Method AuthScope
     * @return
     */
    static boolean
    authscopeCompatible(AuthScope ss, AuthScope ms)
    {
        assert (ss.getScheme() != null && ms.getScheme() != null);
        if(!ss.getHost().equalsIgnoreCase(ms.getHost()))
            return false;
        if(ss.getPort() != ms.getPort())
            return false;
        String sss = ss.getScheme().toLowerCase();
        String mss = ms.getScheme().toLowerCase();
        if(!sss.equals(mss)) {
            // Do some special casing
            if(sss.endsWith("s")) sss = sss.substring(0, sss.length() - 1);
            if(mss.endsWith("s")) mss = mss.substring(0, mss.length() - 1);
            if(!sss.equals(mss))
                return false;
        }
        return true;
    }

    /**
     * Given a session url AuthScope and a Method url AuthScope,
     * return a new AuthScope that is the upgrade/merge of the other two.
     * Here, upgrade changes the scheme (only) to move http -> https.
     * Assumes authscopeCompatible() is true.
     *
     * @param ss Session authScope
     * @param ms Method authScope
     * @return upgraded AuthScope.
     */
    static AuthScope
    authscopeUpgrade(AuthScope ss, AuthScope ms)
    {
        assert (HTTPAuthUtil.authscopeCompatible(ss, ms));
        String sss = ss.getScheme().toLowerCase();
        String mss = ms.getScheme().toLowerCase();
        String upgrade = sss;
        if(sss.startsWith("http") && mss.startsWith("http")) {
            if(sss.equals("https") || mss.equals("https"))
                upgrade = "https";
        }
        AuthScope host = new AuthScope(ss.getHost(), ss.getPort(), AuthScope.ANY_REALM, upgrade);
        return host;
    }

    static AuthScope
    uriToAuthScope(String surl)
            throws HTTPException
    {
        try {
            URI uri = HTTPUtil.parseToURI(surl);
            return uriToAuthScope(uri);
        } catch (URISyntaxException e) {
            throw new HTTPException(e);
        }
    }

    /**
     * Create an AuthScope from a URI; remove any principal
     *
     * @param uri to convert
     * @returns an AuthScope instance
     */

    static AuthScope
    uriToAuthScope(URI uri)
    {
        assert (uri != null);
        return new AuthScope(uri.getHost(), uri.getPort(), AuthScope.ANY_REALM, uri.getScheme());
    }

    static URI
    authscopeToURI(AuthScope authScope)
            throws HTTPException
    {
        try {
            URI url = new URI(authScope.getScheme(),
                    null,
                    authScope.getHost(),
                    authScope.getPort(),
                    "", null, null);
            return url;
        } catch (URISyntaxException mue) {
            throw new HTTPException(mue);
        }
    }

    static HttpHost authscopeToHost(AuthScope scope)
    {
        return new HttpHost(scope.getHost(), scope.getPort(), scope.getScheme());
    }

    static AuthScope hostToAuthScope(HttpHost host)
    {
        return new AuthScope(host.getHostName(), host.getPort(), AuthScope.ANY_REALM, host.getSchemeName());
    }

    static public AuthScope bestmatch(AuthScope scope, Set<AuthScope> scopelist)
    {
        Credentials creds = null;
        int bestMatchFactor = -1;
        AuthScope bestMatch = null;
        for(final AuthScope current : scopelist) {
            final int factor = scope.match(current);
            if(factor > bestMatchFactor) {
                bestMatchFactor = factor;
                bestMatch = current;
            }
        }
        return bestMatch;
    }

}
