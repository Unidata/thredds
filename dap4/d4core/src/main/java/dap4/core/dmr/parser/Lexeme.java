/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser;

public class Lexeme
{
    public String name; // element or attribute name
    public int open; // Bison token for element open
    public int close; // Bison token for element close
    public int atoken; // Bison token if this is an attribute
    public String[] legalAttributes; /* not currently used */

    public Lexeme(String name, int atoken)
    {
        this(name, 0, 0, atoken, null);
    }

    public Lexeme(String name, int open, int close)
    {
        this(name, open, close, 0, null);
    }

    public Lexeme(String name, int open, int close, String[] alist)
    {
        this(name, open, close, 0, alist);
    }

    public Lexeme(String name, int open, int close, int atoken, String[] alist)
    {
        this.name = name;
        this.open = open;
        this.close = close;
        this.atoken = atoken;
        legalAttributes = alist;
    }

    public void attribute(String[] alist)
    {
        legalAttributes = alist;
        // check for duplicates
        for(int i = 0; i < legalAttributes.length; i++) {
            for(int j = i + 1; j < legalAttributes.length; j++)
                assert (!legalAttributes[i].equals(legalAttributes[j]));
        }
    }

    public String toString()
    {
        StringBuilder text = new StringBuilder();
        text.append(String.format("%s open=%d close=%d a=%d", name, open, close, atoken));
        if(legalAttributes != null && legalAttributes.length > 0) {
            text.append(" attributes=");
            for(String s : legalAttributes) {
                text.append(" ");
                text.append(s);
            }
        }
        return text.toString();
    }
} // class Lexeme
