/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.util;

import java.io.*;
import java.util.Stack;

/**
 * Extend PrintWriter to provide indent support
 */

public class IndentWriter extends PrintWriter
{


    //////////////////////////////////////////////////
    // Constants
    static final int MAXDEPTH = 100;

    //////////////////////////////////////////////////
    // Instance variables

    String blanks = "                                        ";
    int delta = 4;
    int depth = 0;

    //////////////////////////////////////////////////
    // Constructors

    public IndentWriter(File file) throws FileNotFoundException
    {
        super(file);
    }

    public IndentWriter(Writer writer)
    {
        super(writer);
    }

    public IndentWriter(OutputStream stream)
    {
        super(stream);
    }


    //////////////////////////////////////////////////
    // Indent methods

    public void indent()
    {
        indent(1);
    }

    public void outdent()
    {
        outdent(1);
    }

    public void outdent(int n)
    {
        indent(-n);
    }

    /**
     * Set depth += n
     *
     * @param n depth increment/decrement(if neg)
     */
    public void indent(int n)
    {
        depth += n;
        if(depth < 0)
            depth = 0;
        else if(depth > MAXDEPTH)
            depth = MAXDEPTH;
    }

    /**
     * Set depth = n
     *
     * @param n absolute depth
     */
    public void setIndent(int n)
    {
        depth = n;
        if(depth < 0)
            depth = 0;
        else if(depth > MAXDEPTH)
            depth = MAXDEPTH;
    }

    public void margin()
    {
        super.write(getMargin());
    }

    public String getMargin()
    {
        int nspaces = delta * depth;
        while(blanks.length() < nspaces)
            blanks = blanks + blanks;
        return blanks.substring(0, nspaces);
    }

    public int getIndent()
    {
        return depth;
    }

    // Provide an indent stack
    Stack<Integer> indentstack = new Stack<Integer>();

    public void push()
    {
        indentstack.push(depth);
    }

    public void pop()
    {
        int n = (indentstack.empty() ? 0 : indentstack.pop());
        setIndent(n);
    }

    //////////////////////////////////////////////////
    // Margin cognizant print functions

    public void eol()
    {
        super.println("");
    }

    public void marginPrintln(String text)
    {
        marginPrint(text + "\n");
    }

    public void marginPrintln()
    {
        marginPrintln("");
    }

    public void marginPrint(String text)
    {
        super.print(getMargin() + text);
    }

    public void marginPrintf(String fmt, Object... args)
    {
        String s = String.format(fmt, args);
        marginPrint(s);
    }


} // class IndentWriter
