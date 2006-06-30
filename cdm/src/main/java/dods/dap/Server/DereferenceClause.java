package dods.dap.Server;

import java.util.*;
import dods.dap.BaseType;
import dods.dap.DODSException;

/** Represents a sub-clause that is a URL reference to remote data.
 *  This feature is not yet supported in Java. Thus this class 
 *  throws an exception in its constructor.
 * @see ClauseFactory
 * @author joew */
public class DereferenceClause 
    extends AbstractClause 
    implements SubClause {

    /** Creates a new DereferenceClause */
    protected DereferenceClause(String url) 
	throws SDODSException {
	this.url = url;
	this.constant = true;
	this.defined = true;
	this.value = retrieve(url);
	this.children = new ArrayList();
    }

    public BaseType getValue() {
	return value;
    }
    
    public BaseType evaluate() {
	return value;
    }

    public Clause getParent() {
	return parent;
    }

    public void setParent(Clause parent) {
	this.parent = parent;
    }

    public String getURL() {
	return url;
    }

    protected BaseType retrieve(String url) 
	throws SDODSException {
	
	throw new SDODSException(DODSException.UNKNOWN_ERROR,"dereferencing not supported");
    }

    public String toString() {
	return "*\"" + url + "\"";
    }

    protected String url;
    protected Clause parent;
    protected BaseType value;
}


