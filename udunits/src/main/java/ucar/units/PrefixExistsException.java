/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for prefix database failures.
 * 
 * @author Steven R. Emmerson
 */
public class PrefixExistsException extends PrefixDBException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from an old prefix and a new prefix.
	 * 
	 * @param oldPrefix
	 *            The previously-existing prefix.
	 * @param newPrefix
	 *            The replacement prefix.
	 */
	public PrefixExistsException(final Prefix oldPrefix, final Prefix newPrefix) {
		super("Attempt to replace \"" + oldPrefix + "\" with \"" + newPrefix
				+ "\" in prefix database");
	}
}
