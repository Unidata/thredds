/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.httpservices;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

/**
 * Class HTTPFormBuilder provides a mechanism for
 * constructing a postable entity for use with Post.
 * This code is derived from IDV HttpFormEntry.
 * The basic idea is that a list of HTTPFormBuilder.Field instances
 * is created to represent the fields and attachments of the form.
 * Then, a post method instance is provided and the content of the post
 * is set from the HTTPForm instance.
 */

public class HTTPFormBuilder
{
    //////////////////////////////////////////////////
    // Constants

    public static final Charset DFALTCHARSET = HTTPUtil.UTF8;


    //////////////////////////////////////////////////
    // Type Decls

    static protected enum Sort
    {
        TEXT, BYTES, STREAM, FILE;

        static public ContentType contenttype(Sort sort)
        {
            switch (sort) {
            case TEXT:
                return TEXT_PLAIN;
            case BYTES:
            case STREAM:
            case FILE:
            default:
                return APPLICATION_OCTET_STREAM;
            }
        }

        static public String mimetype(Sort sort)
        {
            return contenttype(sort).getMimeType();
        }
    }

    static protected class Field
    {
        public Sort sort;
        public String fieldname = null;
        public Object value = null;
        public String name = null; // typically file name

        public Field(Sort sort, String fieldname, Object value, String name)
        {
            this.sort = sort;
            this.fieldname = fieldname;
            this.value = value;
            this.name = name;
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected Map<String, Field> parts = new HashMap<>();

    protected Charset charset = DFALTCHARSET;

    protected boolean usemultipart = false;

    //////////////////////////////////////////////////
    // Constructor(s)

    public HTTPFormBuilder()
    {
    }

    //////////////////////////////////////////////////
    // Accessors

    public String getCharset()
    {
        return this.charset.displayName();
    }

    public HTTPFormBuilder setCharset(String charset)
    {
        this.charset = Charset.forName(charset);
	return this;
    }

    public HTTPFormBuilder setCharset(Charset charset)
    {
        this.charset = charset;
	return this;
    }
    //////////////////////////////////////////////////
    // Field construction

    public HTTPFormBuilder
    add(String fieldname, String text)
            throws HTTPException
    {
        if(fieldname == null || text == null || fieldname.length() == 0)
            throw new IllegalArgumentException();
        Field f = new Field(Sort.TEXT, fieldname, text, null);
        parts.put(fieldname, f);
	return this;
    }

    public HTTPFormBuilder
    add(String fieldname, byte[] content, String filename)
            throws HTTPException
    {
        if(isempty(fieldname))
            throw new IllegalArgumentException();
        if(content == null) content = new byte[0];
        if(isempty(filename)) filename = "";

        Field f = new Field(Sort.BYTES, fieldname, content, filename);
        parts.put(fieldname, f);
        this.usemultipart = true;
	return this;
    }

    public HTTPFormBuilder
    add(String fieldname, final InputStream content, String filename)
            throws HTTPException
    {
        if(isempty(fieldname) || content == null || isempty(filename))
            throw new IllegalArgumentException();
        Field f = new Field(Sort.STREAM, fieldname, content, filename);
        parts.put(fieldname, f);
        this.usemultipart = true;
	return this;
    }

    public HTTPFormBuilder
    add(String fieldname, File content)
            throws HTTPException
    {
        if(isempty(fieldname) || content == null)
            throw new IllegalArgumentException();
        Field f = new Field(Sort.FILE, fieldname, content, content.getName());
        parts.put(fieldname, f);
        this.usemultipart = true;
	return this;
    }

    public HttpEntity build()
            throws HTTPException
    {
        if(this.usemultipart)
            return buildmultipart();
        else
            return buildsimple();
    }

    protected HttpEntity buildsimple()
            throws HTTPException
    {
        List<NameValuePair> params = new ArrayList<>();
        for(Map.Entry<String, Field> mapentry : parts.entrySet()) {
            Field field = mapentry.getValue();
            params.add(new BasicNameValuePair(field.fieldname, field.value.toString()));
        }
        try {
            return new UrlEncodedFormEntity(params);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    protected HttpEntity buildmultipart()
            throws HTTPException
    {
        MultipartEntityBuilder mpb = MultipartEntityBuilder.create();
        mpb.setCharset(this.charset);
        mpb.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for(Map.Entry<String, Field> mapentry : parts.entrySet()) {
            Field field = mapentry.getValue();
            Sort sort = field.sort;
            ContentType ct = Sort.contenttype(sort);
            ContentBody body = null;
            switch (sort) {
            case TEXT:
                body = new StringBody(field.value.toString(), ct);
                break;
            case BYTES:
                body = new ByteArrayBody((byte[]) field.value, field.name);
                break;
            case STREAM:
                // There appears to be a bug that make direct use of inputstream
                // fail with a 411 http code.
                // Temporary workaround is to read the input stream
                // as a set of bytes
                try {
                    byte[] tmp = HTTPUtil.readbinaryfile((InputStream) field.value);
                    body = new ByteArrayBody(tmp, field.name);
                } catch (IOException ioe) {
                    throw new HTTPException(ioe);
                }
                break;
            case FILE:
                body = new FileBody((File) field.value, ct, field.name);
                break;
            }
            mpb.addPart(field.fieldname, body);
        }
        return mpb.build();
    }

    protected boolean isempty(String x)
    {
        return x == null || x.length() == 0;
    }
}

