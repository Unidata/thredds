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


import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;


/**
 * Provide Auth related utilities
 */

@org.apache.http.annotation.Immutable
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
