/*******************************************************************************
 * Copyright (c) 2016 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ide.util;

import static com.google.common.collect.ComparisonChain.*;

import java.util.Comparator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.typefox.lsapi.Position;
import io.typefox.lsapi.Range;

/**
 * Null-safe comparator for {@link Range range} instances.
 * 
 * <p>
 * Compares {@link Range#getStart() start positions} first, then
 * {@link Range#getEnd() end positions}.
 * 
 * @author akos.kitta - Initial contribution and API
 * 
 * @see PositionComparator
 */
@Singleton
public class RangeComparator implements Comparator<Range> {

	private final Comparator<Range> delegate;

	/**
	 * Creates a new range comparator with the give comparator delegate for
	 * {@link Position positions}.
	 * 
	 * @param positionComparator
	 *            the delegate comparator for the positions. Cannot be
	 *            {@code null}.
	 */
	@Inject
	public RangeComparator(final PositionComparator positionComparator) {
		Preconditions.checkNotNull(positionComparator, "positionComparator");
		
		delegate = (left, right) -> start().compare(left.getStart(), right.getStart(), positionComparator)
				.compare(left.getEnd(), right.getEnd(), positionComparator).result();
	}

	@Override
	public int compare(final Range left, final Range right) {
		return Ordering.from(delegate).nullsLast().compare(left, right);
	}

}
