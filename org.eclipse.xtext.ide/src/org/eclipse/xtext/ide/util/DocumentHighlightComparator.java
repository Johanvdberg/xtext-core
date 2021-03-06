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

import io.typefox.lsapi.DocumentHighlight;
import io.typefox.lsapi.DocumentHighlightKind;
import io.typefox.lsapi.Range;

/**
 * Null-safe comparator for {@link DocumentHighlight document highlight}
 * instances.
 * 
 * <p>
 * Compares {@link DocumentHighlight#getRange() ranges} first, then the
 * {@link DocumentHighlightKind highlight kinds} based on the natural ordering.
 * 
 * @author akos.kitta - Initial contribution and API
 * 
 * @see RangeComparator
 */
@Singleton
public class DocumentHighlightComparator implements Comparator<DocumentHighlight> {

	private final Comparator<DocumentHighlight> delegate;

	/**
	 * Creates a new document highlight comparator instance with the delegate
	 * comparator for {@link Range ranges}.
	 * 
	 * @param rangeComparator
	 *            the comparator used for the ranges. Cannot be {@code null}.
	 */
	@Inject
	public DocumentHighlightComparator(final RangeComparator rangeComparator) {
		Preconditions.checkNotNull(rangeComparator, "rangeComparator");
		
		delegate = (left, right) -> start().compare(left.getRange(), right.getRange(), rangeComparator)
				.compare(left.getKind(), right.getKind()).result();
	}

	@Override
	public int compare(final DocumentHighlight left, final DocumentHighlight right) {
		return Ordering.from(delegate).nullsLast().compare(left, right);
	}

}
