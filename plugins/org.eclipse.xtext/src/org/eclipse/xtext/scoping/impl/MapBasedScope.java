/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.scoping.impl;

import static java.util.Collections.*;

import java.util.Collections;
import java.util.Map;

import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.ISelector;

/**
 * A scope implemented using a {@link Map}. 
 * 
 * This implementation assumes, that the keys of the {@link Map} are the keys of the contained {@link org.eclipse.xtext.resource.EObjectDescription}s
 * as well as the name.
 * 
 * When looking up elements using {@link ISelector.SelectByName} this implementation looks up the the elements from the map, hence are much 
 * more efficient for many {@link IEObjectDescription}s.  
 * 
 * @author Sven Efftinge - Initial contribution and API
 */
public class MapBasedScope extends AbstractScope {

	private Map<QualifiedName, IEObjectDescription> elements;

	public MapBasedScope(IScope parent, Map<QualifiedName, IEObjectDescription> elements) {
		super(parent);
		this.elements = elements;
	}

	@Override
	public Iterable<IEObjectDescription> getLocalElements(ISelector selector) {
		if (selector instanceof ISelector.SelectByName) {
			QualifiedName name = ((ISelector.SelectByName) selector).getName();
			if (elements.containsKey(name)) {
				return selector.applySelector(singleton(elements.get(name)));
			} else {
				return Collections.emptySet();
			}
		}
		return selector.applySelector(elements.values());
	}
	
	@Override
	protected boolean isShadowed(IEObjectDescription fromParent) {
		return elements.containsKey(getKey(fromParent));
	}

}
