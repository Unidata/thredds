// $Id: Dimension.java 64 2006-07-12 22:30:50Z edavis $
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
package ucar.units;

/**
 * Provides support for the concept of "dimension": pairs of base entities and
 * exponents.
 * 
 * @author Steven R. Emmerson
 * @version $Id: Dimension.java 64 2006-07-12 22:30:50Z edavis $
 */
public abstract class Dimension {
    /**
     * The individual elements of this dimension.
     * 
     * @serial
     */
    protected final Factor[]       _factors;

    private transient volatile int hashCode;

    /**
     * Constructs a dimensionless dimension from nothing.
     */
    public Dimension() {
        this(new Factor[0]);
    }

    /**
     * Constructs from a single Factor.
     * 
     * @param factor
     *            The single Factor that defines the dimension.
     */
    protected Dimension(final Factor factor) {
        this(new Factor[] { factor });
    }

    /**
     * Constructs from an array of Factor-s. This is a trusted constructor for
     * use by subclasses only.
     * 
     * @param factors
     *            The factors that define the dimension.
     */
    protected Dimension(final Factor[] factors) {
        _factors = factors;
    }

    /**
     * Returns the rank of this dimension. The rank is the number of base entity
     * and exponent pairs (i.e. the number of Factor-s constituting this
     * dimension).
     * 
     * @return The rank of this dimension.
     */
    public final int getRank() {
        return _factors.length;
    }

    /**
     * Returns the array of Factor-s constituting this dimension.
     * 
     * @return The array of Factor-s constituting this dimension.
     */
    public final Factor[] getFactors() {
        final Factor[] factors = new Factor[_factors.length];
        System.arraycopy(_factors, 0, factors, 0, factors.length);
        return factors;
    }

    /**
     * Multiplies this dimension by another dimension.
     * 
     * @param that
     *            The other dimension.
     * @return The product of the Factor-s of this dimension and the Factor-s of
     *         the other dimension.
     */
    protected Factor[] mult(final Dimension that) {
        // relys on _factors always sorted
        final Factor[] factors1 = _factors;
        final Factor[] factors2 = that._factors;
        int i1 = 0;
        int i2 = 0;
        int k = 0;
        Factor[] newFactors = new Factor[factors1.length + factors2.length];
        for (;;) {
            if (i1 == factors1.length) {
                final int n = factors2.length - i2;
                System.arraycopy(factors2, i2, newFactors, k, n);
                k += n;
                break;
            }
            if (i2 == factors2.length) {
                final int n = factors1.length - i1;
                System.arraycopy(factors1, i1, newFactors, k, n);
                k += n;
                break;
            }
            final Factor f1 = factors1[i1];
            final Factor f2 = factors2[i2];
            final int comp = f1.getID().compareTo(f2.getID());
            if (comp < 0) {
                newFactors[k++] = f1;
                i1++;
            }
            else if (comp == 0) {
                final int exponent = f1.getExponent() + f2.getExponent();
                if (exponent != 0) {
                    newFactors[k++] = new Factor(f1, exponent);
                }
                i1++;
                i2++;
            }
            else {
                newFactors[k++] = f2;
                i2++;
            }
        }
        if (k < newFactors.length) {
            final Factor[] tmp = new Factor[k];
            System.arraycopy(newFactors, 0, tmp, 0, k);
            newFactors = tmp;
        }
        return newFactors;
    }

    /**
     * Raises this dimension to a power.
     * 
     * @param power
     *            The power to raise this dimension by.
     * @return The Factor-s of this dimension raised to the power
     *         <code>power</code>.
     */
    protected Factor[] pow(final int power) {
        Factor[] factors;
        if (power == 0) {
            factors = new Factor[0];
        }
        else {
            factors = getFactors();
            if (power != 1) {
                for (int i = factors.length; --i >= 0;) {
                    factors[i] = factors[i].pow(power);
                }
            }
        }
        return factors;
    }

    /**
     * Indicates if this Dimension is the reciprocal of another dimension.
     * 
     * @param that
     *            The other dimension.
     * @return <code>true</code> if and only if this dimension is the reciprocal
     *         of the other dimension.
     */
    public final boolean isReciprocalOf(final Dimension that) {
        final Factor[] theseFactors = _factors;
        final Factor[] thoseFactors = that._factors;
        boolean isReciprocalOf;
        if (theseFactors.length != thoseFactors.length) {
            isReciprocalOf = false;
        }
        else {
            int i;
            for (i = theseFactors.length; --i >= 0;) {
                if (!theseFactors[i].isReciprocalOf(thoseFactors[i])) {
                    break;
                }
            }
            isReciprocalOf = i < 0;
        }
        return isReciprocalOf;
    }

    /**
     * Indicates if this dimension is semantically identical to an object.
     * 
     * @param object
     *            The object.
     * @return <code>true</code> if and only if this dimension is semantically
     *         identical to <code>object</code>.
     */
    @Override
    public final boolean equals(final Object object) {
        boolean equals;
        if (this == object) {
            equals = true;
        }
        else if (!(object instanceof Dimension)) {
            equals = false;
        }
        else {
            final Factor[] thatFactors = ((Dimension) object)._factors;
            if (_factors.length != thatFactors.length) {
                equals = false;
            }
            else {
                int i = _factors.length;
                while (--i >= 0) {
                    if (!_factors[i].equals(thatFactors[i])) {
                        break;
                    }
                }
                equals = i < 0;
            }
        }
        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * 
     * @return The hash code of this instance.
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int hash = 0;
            for (int i = 0; i < _factors.length; i++) {
                hash ^= _factors[i].hashCode();
            }
            hashCode = hash;
        }
        return hashCode;
    }

    /**
     * Indicates if this dimension is dimensionless. A dimension is
     * dimensionless if it has no Factor-s or if all Factor-s are, themselves,
     * dimensionless.
     * 
     * @return <code>true</code> if and only if this dimension is dimensionless.
     */
    public final boolean isDimensionless() {
        for (int i = _factors.length; --i >= 0;) {
            if (!_factors[i].isDimensionless()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the string representation of this dimension.
     * 
     * @return The string representation of this dimension.
     */
    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer(40);
        for (int i = 0; i < _factors.length; i++) {
            buf.append(_factors[i]).append('.');
        }
        if (buf.length() != 0) {
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }
}
