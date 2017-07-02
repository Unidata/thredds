/*
Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
See LICENSE.txt for license information.
*/

package ucar.nc2.stream;

import ucar.ma2.Array;
import ucar.nc2.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Compose an ncstream output stream by passing in
 * various pieces of CDM. This allows for the building of a stream
 * without having a materialized NetcdfFile object.
 * For now, we cheat in that we cache the CDM objects
 * and dump them when finish() is called.
 * <p>
 * Notes:
 * 1. Only explicitly built elements (attributes, Enumerations, etc)
 * will be included in output
 *
 * @author Heimbigner
 *         Derived from NcStreamWriter
 */
public class NcStreamCDMWriter extends NcStreamWriter
{
    //////////////////////////////////////////////////
    // Constants

    //////////////////////////////////////////////////
    // Instance Variables

    protected String location = null;
    protected String title = null;
    protected String ID = null;

    // Selected special elements
    protected Group rootgroup = null;

    // Cache the this.elements.
    // We do this because the ncStream.proto assumes
    // a specific order that might be different from that
    // Assumed by the ncStream protobuf spec.
    // Note also that since we might be passing a subset of
    // e.g. a NetcdfFile, we need to track the specific subset.
    // The way we do so is to track element <<-> container
    // bi-directional map where container is either a group
    // or a Structure. and element is any CDMNode subtype.

    protected ContainerMap elements = new ContainerMap();

    // Build a map from Variable -> Array to track data
    protected Map<Variable, Array> data = new HashMap<>();

    NcStreamBuilder nsb = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public NcStreamCDMWriter()
            throws IOException
    {
    }

    //////////////////////////////////////////////////
    // Accessors

    public byte[]
    getContent()
    {
        byte[] b = this.header.toByteArray();
	return b;
    }

    public NcStreamCDMWriter setLocation(String location)
    {
        this.location = location;
        return this;
    }

    public NcStreamCDMWriter setTitle(String title)
    {
        this.title = title;
        return this;
    }

    public NcStreamCDMWriter setID(String id)
    {
        this.ID = id;
        return this;
    }

    //////////////////////////////////////////////////
    // CDM input API

    // Initially, we assume that we are given CDM elements to encode.
    // It is up to the caller to ensure that element dependency order is
    // correct, except where we can easily check.

    public NcStreamCDMWriter
    addRootGroup(Group root)
            throws IOException
    {
        if(this.elements.size() > 0)
            throw new IllegalStateException("Root group must be first defined element");
        this.rootgroup = root;
        return this;
    }

    public NcStreamCDMWriter
    addGroup(Group parent, Group g)
            throws IOException
    {
        this.elements.put(parent, g);
        return this;
    }

    public NcStreamCDMWriter
    addEnumeration(Group parent, EnumTypedef e)
            throws IOException
    {
        this.elements.put(parent, e);
        return this;
    }

    public NcStreamCDMWriter
    addDimension(Group parent, Dimension d)
            throws IOException
    {
        this.elements.put(parent, d);
        return this;
    }

    public NcStreamCDMWriter
    addAttribute(CDMNode parent, Attribute a)
            throws IOException
    {
        this.elements.put(parent, a);
        return this;
    }

    public NcStreamCDMWriter
    addVariable(CDMNode parent, Variable v, Array data)
            throws IOException
    {
        this.elements.put(parent, v);
        if(data != null)
            this.data.put(v, data);
        return this;
    }

    //////////////////////////////////////////////////
    // Do actual stream construction
    // We cannot assume our caller did things in the right order
    // Except that the root group is the zero'th element.

    public NcStreamCDMWriter
    finish()
            throws IOException
    {
        if(this.elements.size() == 0)
            throw new IOException("No CDM Elements specified");
        this.nsb = new NcStreamBuilder();
        this.nsb.beginRootGroup(this.rootgroup);
        traverseGroup(this.rootgroup);
        this.nsb.endGroup();

        NcStreamProto.Header.Builder headerBuilder = NcStreamProto.Header.newBuilder();

        // Remember: for protobuf, order is important
        headerBuilder.setLocation(this.location == null ? rootgroup.getShortName() : this.location);
        if(this.title != null) headerBuilder.setTitle(this.title);
        if(this.ID != null) headerBuilder.setId(this.ID);
        headerBuilder.setRoot(this.nsb.getRootBuilder());
        headerBuilder.setVersion(this.currentVersion);
        this.header = headerBuilder.build();

        return this;
    }

    //////////////////////////////////////////////////
    // Recursively walk a group

    protected void
    traverseGroup(Group g)
            throws IOException
    {
        // Protobuf spec requires this order:
        // dimensions, (atomic) variables, structures,
        // attributes, groups, enums.

        // Walk dimensions
        Iterator<CDMNode> it = elements.getContents(g).iterator();
        while(it.hasNext()) {
            CDMNode x = it.next();
            if(x.getSort() != CDMSort.DIMENSION) continue;
            this.nsb.addDimension((Dimension) x);
        }

        // Walk atomic variables
        it = elements.getContents(g).iterator();
        while(it.hasNext()) {
            CDMNode x = it.next();
            if(x.getSort() != CDMSort.VARIABLE) continue;
            this.nsb.addVariable((Variable) x);
        }

        // Walk Structure variables
        it = elements.getContents(g).iterator();
        while(it.hasNext()) {
            CDMNode x = it.next();
            if(x.getSort() != CDMSort.STRUCTURE) continue;
            this.nsb.beginStruct((Structure) x);
            traverseStruct((Structure) x);
            this.nsb.endStruct();
        }

        // Walk group level attributes
        it = elements.getContents(g).iterator();
        while(it.hasNext()) {
            CDMNode x = it.next();
            if(x.getSort() != CDMSort.ATTRIBUTE) continue;
            this.nsb.addAttribute((Attribute) x);
        }

        // Recurse on sub-groups
        it = elements.getContents(g).iterator();
        while(it.hasNext()) {
            CDMNode x = it.next();
            if(x.getSort() != CDMSort.GROUP) continue;
            this.nsb.beginGroup((Group) x);
            traverseGroup((Group) x);
            this.nsb.endGroup();
        }

        // Walk enum defs
        it = elements.getContents(g).iterator();
        while(it.hasNext()) {
            CDMNode x = it.next();
            if(x.getSort() != CDMSort.ENUMERATION) continue;
            this.nsb.addEnum((EnumTypedef) x);
        }

    }


    //////////////////////////////////////////////////
    // Recursively walk a structure

    protected void
    traverseStruct(Structure s)
            throws IOException
    {
    }

    //////////////////////////////////////////////////

    /**
     * Build the output stream
     */
    public NcStreamCDMWriter
    add(OutputStream out, String location, String title, String ID)
            throws IOException
    {
        // Validity checks
        if(this.rootgroup == null)
            throw new IllegalStateException("No root group specified");
        if(location == null)
            location = this.rootgroup.getShortName();

        NcStreamProto.Group.Builder rootBuilder = NcStream.encodeGroup(rootgroup, this.sizeToCache);
        NcStreamProto.Header.Builder headerBuilder = NcStreamProto.Header.newBuilder();
        headerBuilder.setLocation(location);
        if(title != null) headerBuilder.setTitle(title);
        if(ID != null) headerBuilder.setId(ID);
        headerBuilder.setRoot(rootBuilder);
        headerBuilder.setVersion(this.currentVersion);

        header = headerBuilder.build();

        return this;
    }

}

