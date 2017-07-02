/*
Copyright (c) 1998-$today.year University Corporation for Atmospheric Research/Unidata
See LICENSE.txt for license information.
*/

package ucar.nc2.stream;

import ucar.nc2.*;

import java.io.IOException;
import java.util.Stack;

/**
 */
public class NcStreamBuilder extends NcStream
{
    //////////////////////////////////////////////////
    // Constants

    static final protected int NOCACHE = -1;

    //////////////////////////////////////////////////
    // Instance variables

    // Stack of open containers
    protected Stack<Object> scope = new Stack<>();

    protected NcStreamProto.Group.Builder rootbuilder = null;

    protected Group rootgroup = null;

    protected int sizeToCache =  NcStreamWriter.sizeToCache;

    //////////////////////////////////////////////////
    // Constructor(s)

    public NcStreamBuilder()
    {
    }

    //////////////////////////////////////////////////
    // Utilities

    protected void
    link(Object o)
            throws IOException
    {
        Object op = scope.peek();
        if(op instanceof NcStreamProto.Group.Builder) {
            NcStreamProto.Group.Builder builder = (NcStreamProto.Group.Builder) op;
            // Wish we had a usable superclass
            if(o instanceof NcStreamProto.Attribute.Builder)
                builder.addAtts((NcStreamProto.Attribute.Builder) o);
            else if(o instanceof NcStreamProto.Dimension.Builder)
                builder.addDims((NcStreamProto.Dimension.Builder) o);
            else if(o instanceof NcStreamProto.EnumTypedef.Builder)
                builder.addEnumTypes((NcStreamProto.EnumTypedef.Builder) o);
            else if(o instanceof NcStreamProto.Group.Builder)
                builder.addGroups((NcStreamProto.Group.Builder) o);
            else if(o instanceof NcStreamProto.Variable.Builder)
                builder.addVars((NcStreamProto.Variable.Builder) o);
            else
                throw new IOException("Illegal Group element");
        } else if(op instanceof NcStreamProto.Structure.Builder) {
            NcStreamProto.Structure.Builder builder = (NcStreamProto.Structure.Builder) op;
            if(o instanceof NcStreamProto.Attribute.Builder)
                builder.addAtts((NcStreamProto.Attribute.Builder) o);
            else if(o instanceof NcStreamProto.Dimension.Builder)
                builder.addShape((NcStreamProto.Dimension.Builder) o);
            else if(o instanceof NcStreamProto.Variable.Builder)
                builder.addVars((NcStreamProto.Variable.Builder) o);
            else
                throw new IOException("Illegal Structure element");
        } else
            throw new IOException("Illegal container type");
    }

    //////////////////////////////////////////////////
    // Accessors

    public NcStreamProto.Group.Builder
    getRootBuilder()
    {
        return this.rootbuilder;
    }

    //////////////////////////////////////////////////
    // API

    NcStreamBuilder
    beginRootGroup(Group g)
            throws IOException
    {
        if(scope.size() != 0)
            throw new IOException("Root group must be first defined container");
        this.rootgroup = g;
        this.rootbuilder = NcStreamProto.Group.newBuilder();
        this.rootbuilder.setName(g.getShortName());
        scope.push(this.rootbuilder);
        return this;
    }

    NcStreamBuilder
    beginGroup(Group g)
            throws IOException
    {
        Object o = scope.peek();
        if(!(o instanceof NcStreamProto.Group.Builder))
            throw new IOException("Group parent must be a group");
        NcStreamProto.Group.Builder builder = NcStreamProto.Group.newBuilder();
        builder.setName(g.getShortName());
        link(builder);
        scope.push(builder);
        return this;
    }

    NcStreamBuilder
    endGroup()
            throws IOException
    {
        Object o = scope.peek();
        if(!(o instanceof NcStreamProto.Group.Builder))
            throw new IOException("Scope mismatch");
        scope.pop();
        return this;
    }

    NcStreamBuilder
    addDimension(Dimension dim)
            throws IOException
    {
        NcStreamProto.Dimension.Builder bdim = super.encodeDim(dim);
        link(bdim);
        return this;
    }

    NcStreamBuilder
    addAttribute(Attribute att)
            throws IOException
    {
        NcStreamProto.Attribute.Builder batt = super.encodeAtt(att);
        link(batt);
        return this;
    }

    NcStreamBuilder
    addEnum(EnumTypedef enumType)
            throws IOException
    {
        NcStreamProto.Group.Builder benum = (NcStreamProto.Group.Builder) scope.peek();
        link(benum);
        return this;
    }

    NcStreamBuilder
    addVariable(Variable var)
            throws IOException
    {
        NcStreamProto.Variable.Builder bvar = null;
        if(var instanceof Structure)
            throw new IOException("Use beginStruct ... endStruct");
        bvar = super.encodeVar(var, this.sizeToCache);
        link(bvar);
        return this;
    }

    NcStreamBuilder
    beginStruct(Structure s)
            throws IOException
    {
        NcStreamProto.Structure.Builder bstruct = NcStreamProto.Structure.newBuilder();
        // Do what encodeVar does
        bstruct.setName(s.getShortName());
        bstruct.setDataType(convertDataType(s.getDataType()));
        for(Dimension dim : s.getDimensions()) {
            bstruct.addShape(encodeDim(dim));
        }
        for(Attribute att : s.getAttributes()) {
            bstruct.addAtts(encodeAtt(att));
        }
        link(bstruct);
        return this;
    }

    NcStreamBuilder
    endStruct()
            throws IOException
    {
        Object o = scope.peek();
        if(!(o instanceof NcStreamProto.Structure.Builder))
            throw new IOException("Scope mismatch");
        scope.pop();
        return this;
    }

}
