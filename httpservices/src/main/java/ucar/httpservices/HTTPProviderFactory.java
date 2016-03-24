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
import org.apache.http.client.CredentialsProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * HTTPProviderFactory creates instances of
 * a CredentialsProvider. This is used by HTTPSession
 * to ensure that every instance of HTTPSession has
 * an individual CredentialsProvider object.
 */

public class HTTPProviderFactory
{
    protected Class<CredentialsProvider> providerclass = null;

    protected Constructor<CredentialsProvider> constructor = null;


    protected HTTPProviderFactory()
            throws HTTPException
    {
        this(null);
    }

    public HTTPProviderFactory(Class<CredentialsProvider> pclass)
            throws HTTPException
    {
        this.providerclass = pclass;
        if(pclass != null) try {
            this.constructor = pclass.getConstructor(AuthScope.class);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new HTTPException("HTTPProviderFactory: no proper constructor available", e);
        }
    }

    public CredentialsProvider
    getProvider(AuthScope scope)
            throws HTTPException
    {
        if(this.providerclass == null)
            return null;
        else try {
            CredentialsProvider cp = this.constructor.newInstance(scope);
            return cp;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new HTTPException("HTTPProvider Factory failure", e);
        }
    }
}


// Define a factory that provides the same CredentialsProvider
// This is package scope because it is a temporary class to support
// the deprecated setGlobalCredentialsProvider

/*package*/
class SingleProviderFactory extends HTTPProviderFactory
{
    protected CredentialsProvider singleprovider;

    public SingleProviderFactory(CredentialsProvider cp)
            throws HTTPException
    {
        this.singleprovider = cp;
    }


    public CredentialsProvider
    getProvider(AuthScope scope)
    {
        return this.singleprovider;
    }

}




