/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import javax.annotation.concurrent.Immutable;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Provides support for base units.
 * 
 * @author Steven R. Emmerson
 */
@Immutable
public class BaseUnit extends DerivedUnitImpl implements Base {
    private static final long                              serialVersionUID = 1L;

    /**
     * The identifier-to-unit map.
     * 
     * @serial
     */
    private static final SortedMap<UnitName, BaseUnit>     nameMap          = new TreeMap<>();

    /**
     * The quantity-to-unit map.
     * 
     * @serial
     */
    private static final SortedMap<BaseQuantity, BaseUnit> quantityMap      = new TreeMap<>();

    /**
     * The base quantity associated with this base unit.
     * 
     * @serial
     */
    private final BaseQuantity                             baseQuantity;

    /**
     * Constructs from identifiers and a base quantity.
     * 
     * @param id
     *            The identifiers for the base unit. <code>
     *				id.getSymbol()</code>
     *            shall not return <code>
     *				null</code>.
     * @param baseQuantity
     *            The base quantity of the base unit.
     * @throws NameException
     *             <code>id.getSymbol()</code> returned <code>
     *				null</code>.
     */
    protected BaseUnit(final UnitName id, final BaseQuantity baseQuantity)
            throws NameException {
        super(id);
        if (id.getSymbol() == null) {
            throw new NameException("Base unit must have symbol");
        }
        setDimension(new UnitDimension(this));
        this.baseQuantity = baseQuantity;
    }

    /**
     * Factory method for creating a new BaseUnit or obtaining a
     * previously-created one.
     * 
     * @param id
     *            The identifier for the base unit. <code>
     *				id.getSymbol()</code>
     *            shall not return <code>
     *				null</code>.
     * @param baseQuantity
     *            The base quantity of the base unit.
     * @throws NameException
     *             <code>id.getSymbol()</code> returned <code>
     *				null</code>.
     * @throws UnitExistsException
     *             Attempt to incompatibly redefine an existing base unit.
     */
    public static synchronized BaseUnit getOrCreate(final UnitName id,
            final BaseQuantity baseQuantity) throws NameException,
            UnitExistsException {
        BaseUnit baseUnit;
        final BaseUnit nameUnit = nameMap.get(id);
        final BaseUnit quantityUnit = quantityMap.get(baseQuantity);
        if (nameUnit != null || quantityUnit != null) {
            baseUnit = nameUnit != null
                    ? nameUnit
                    : quantityUnit;
            if ((nameUnit != null && !baseQuantity.equals(nameUnit
                    .getBaseQuantity()))
                    || (quantityUnit != null && !id.equals(quantityUnit
                            .getUnitName()))) {
                throw new UnitExistsException(
                        "Attempt to incompatibly redefine base unit \""
                                + baseUnit + '"');
            }
        }
        else {
            baseUnit = new BaseUnit(id, baseQuantity);
            quantityMap.put(baseQuantity, baseUnit);
            nameMap.put(id, baseUnit);
        }
        return baseUnit;
    }

    /**
     * Returns the base quantity associated with this base unit.
     * 
     * @return The base quantity associated with this base unit.
     */
    public final BaseQuantity getBaseQuantity() {
        return baseQuantity;
    }

    /**
     * Returns the identifier for this base unit. This is identical to
     * <code>getSymbol()</code>.
     * 
     * @return The identifier for this base unit.
     */
    public final String getID() {
        return getSymbol();
    }

    /**
     * Returns the string representation of this base unit. This is identical to
     * <code>getID()</code>.
     * 
     * @return The string representation of this base unit.
     */
    @Override
    public final String toString() {
        return getID();
    }

    /**
     * Indicates if this base unit is dimensionless.
     * 
     * @return <code>true</code> if and only if this base unit is dimensionless
     *         (e.g. "radian").
     */
    @Override
    public boolean isDimensionless() {
        return baseQuantity.isDimensionless();
    }

    /**
     * Tests this class.
     */
    public static void main(final String[] args) throws Exception {
        final BaseUnit meter = new BaseUnit(UnitName.newUnitName("meter", null,
                "m"), BaseQuantity.LENGTH);
        System.out
                .println("meter.getBaseQuantity()=" + meter.getBaseQuantity());
        System.out
                .println("meter.toDerivedUnit(1.)=" + meter.toDerivedUnit(1.));
        System.out.println("meter.toDerivedUnit(new float[] {2})[0]="
                + meter.toDerivedUnit(new float[] { 2 }, new float[1])[0]);
        System.out.println("meter.fromDerivedUnit(1.)="
                + meter.fromDerivedUnit(1.));
        System.out.println("meter.fromDerivedUnit(new float[] {3})[0]="
                + meter.fromDerivedUnit(new float[] { 3 }, new float[1])[0]);
        System.out.println("meter.isCompatible(meter)="
                + meter.isCompatible(meter));
        final BaseUnit radian = new BaseUnit(UnitName.newUnitName("radian",
                null, "rad"), BaseQuantity.PLANE_ANGLE);
        System.out.println("meter.isCompatible(radian)="
                + meter.isCompatible(radian));
        System.out
                .println("meter.isDimensionless()=" + meter.isDimensionless());
        System.out.println("radian.isDimensionless()="
                + radian.isDimensionless());
    }
}
