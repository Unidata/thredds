/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

/**
 Define Bison specific decls.
 */

package dap4.core.dmr.parser;

import org.xml.sax.Locator;

abstract public class Bison
{
    static public class Position
    {
        public Locator location;

        public Position(Locator loc)
        {
            this.location = loc;
        }

        public String toString()
        {
            return location.getLineNumber() + ":" + location.getColumnNumber();
        }

        @Override
        public boolean equals(Object o)
        {
            if(!(o instanceof Position)) return false;
            Position po = (Position) o;
            return po.location.getLineNumber() == po.location.getLineNumber()
                && po.location.getColumnNumber() == po.location.getColumnNumber();
        }

        @Override
        public int hashCode()
        {
            return location.getLineNumber() << 20 | location.getColumnNumber();
        }
    }

    /**
     * A class defining a pair of positions.  Positions, defined by the
     * <code>Bison.Position</code> class, denote a point in the input.
     * Locations represent a part of the input through the beginning
     * and ending positions.
     */
    static public class Location
    {
        /**
         * The first, inclusive, position in the range.
         */
        public Bison.Position begin;

        /**
         * The first position beyond the range.
         */
        public Bison.Position end;

        /**
         * Create a <code>Location</code> denoting an empty range located at
         * a given point.
         *
         * @param loc The position at which the range is anchored.
         */
        public Location(Bison.Position loc)
        {
            this.begin = this.end = loc;
        }

        /**
         * Create a <code>Location</code> from the endpoints of the range.
         *
         * @param begin The first position included in the range.
         * @param end   The first position beyond the range.
         */
        public Location(Bison.Position begin, Bison.Position end)
        {
            this.begin = begin;
            this.end = end;
        }

        /**
         * Print a representation of the location.  For this to be correct,
         * <code>Bison.Position</code> should override the <code>equals</code>
         * method.
         */
        public String toString()
        {
            if(begin.equals(end))
                return begin.toString();
            else
                return begin.toString() + "-" + end.toString();
        }
    }

}
