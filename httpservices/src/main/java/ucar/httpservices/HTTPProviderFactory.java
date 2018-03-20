/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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




