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


import org.apache.http.auth.AuthScope;
import org.apache.http.util.LangUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;


import static org.apache.http.auth.AuthScope.*;
import static ucar.httpservices.HTTPAuthScope.*;


/**
 * The standard AuthScope does not provide sufficiently
 * fine grain authorization. In particular, we would
 * to support principals and datasets.
 */

@org.apache.http.annotation.Immutable
abstract public class HTTPAuthScope
{

    //////////////////////////////////////////////////
    // Constants

    public static final String ANY_PRINCIPAL = null;

    public static final AuthScope ANY
        = new AuthScope(ANY_HOST, ANY_PORT, ANY_REALM, ANY_SCHEME);

    //////////////////////////////////////////////////	
    // URL Decomposition

    static URI decompose(String suri)
        throws HTTPException
    {
        try {
            URI uri = new URI(suri);
            return uri;
        } catch (URISyntaxException use) {
            throw new HTTPException("HTTPAuthScope: illegal url: " + suri);
        }
    }

    //////////////////////////////////////////////////
    // Equals and Equivalence interface

    /**
     * Equivalence algorithm:
     * if any field is ANY_XXX, then they are equivalent.
     * Scheme, port, host must all be identical else return false
     * If this.path is prefix of other.path
     * or other.path is prefix of this.path
     * or they are string equals, then return true
     * else return false.
     */
    static public boolean equivalent(AuthScope a1, AuthScope a2)
    {
        if(a1 == null || a2 == null)
            throw new NullPointerException();
        if(a1.getScheme() != ANY_SCHEME && a2.getScheme() != ANY_SCHEME
            && !a1.getScheme().equals(a2.getScheme()))
            return false;
        if(a1.getHost() != ANY_HOST && a2.getHost() != ANY_HOST
            && !a1.getHost().equals(a2.getHost()))
            return false;
        if(a1.getPort() != ANY_PORT && a2.getPort() != ANY_PORT
            && a1.getPort() != a2.getPort())
            return false;
        if(a1.getRealm() != ANY_REALM && a2.getRealm() != ANY_REALM
            && !a1.getRealm().equals(a2.getRealm()))
            return false;
        return true;
    }

    public static boolean identical(AuthScope a1, AuthScope a2)
    {
        if(a2 == null ^ a1 == null)
            return false;
	if(a1 == a2)
	    return true;
        // So it turns out that AuthScope#equals does not
        // test port values correctly, so we need to fix here.
        if(true) {
            boolean b1 = LangUtils.equals(a1.getHost(), a2.getHost());
            int aport = a2.getPort();
            boolean b2 = (a1.getPort() == aport || a1.getPort() == ANY_PORT || aport == ANY_PORT);
            boolean b3 = LangUtils.equals(a1.getRealm(), a2.getRealm());
            boolean b4 = LangUtils.equals(a1.getScheme(), a2.getScheme());
            if(!(b1 && b2 && b3 && b4))
                return false;
        } else if(!a1.equals(a2))
            return false;
        return true;
    }

    /**
     * Check is a AuthScope is "subsumed" by another AuthScope.
     * Alias for equivalence
     */
    static boolean subsumes(AuthScope as, AuthScope has)
    {
        return equivalent(as, has);
    }

    /**
     * Create an AuthScope from a URL; pull out any principal
     *
     * @param surl       to convert
     * @param principalp to store principal from url
     * @returns an AuthScope instance
     */

    static public AuthScope
    urlToScope(String authscheme, String surl, String[] principalp)
        throws HTTPException
    {
        URI uri = HTTPAuthScope.decompose(surl);
        AuthScope scope = new AuthScope(uri.getHost(),
            uri.getPort(),
            ANY_REALM,
            authscheme);
        if(principalp != null)
            principalp[0] = uri.getUserInfo();
        return scope;
    }

    static public boolean
    wildcardMatch(String p1, String p2)
    {
        if((p1 == null ^ p2 == null)|| (p1 == p2))
            return true;
        return (p1.equals(p2));
    }

    static public void serializeScope(AuthScope scope, ObjectOutputStream oos)
        throws IOException
    {
        oos.writeObject(scope.getHost());
        oos.writeInt(scope.getPort());
        oos.writeObject(scope.getRealm());
        oos.writeObject(scope.getScheme());
    }

    static public AuthScope deserializeScope(ObjectInputStream oos)
        throws IOException, ClassNotFoundException
    {
        String host = (String) oos.readObject();
        int port = oos.readInt();
        String realm = (String) oos.readObject();
        String scheme = (String) oos.readObject();
        return new AuthScope(host, port, realm, scheme);
    }

}
