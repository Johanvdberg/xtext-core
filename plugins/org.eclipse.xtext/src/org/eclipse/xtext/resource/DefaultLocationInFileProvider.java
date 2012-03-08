/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.resource;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.parsetree.reconstr.IHiddenTokenHelper;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.util.TextRegionWithLineInformation;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * @author Peter Friese - Implementation
 * @author Sven Efftinge - Initial contribution and API
 */
public class DefaultLocationInFileProvider implements ILocationInFileProvider, ILocationInFileProviderExtension {

	@Inject(optional = true)
	private IHiddenTokenHelper hiddenTokenHelper;
	
	public ITextRegion getSignificantTextRegion(EObject obj) {
		return getTextRegion(obj, true);
	}

	public ITextRegion getFullTextRegion(EObject obj) {
		return getTextRegion(obj, false);
	}

	protected ITextRegion getTextRegion(EObject obj, boolean isSignificant) {
		return doGetTextRegion(obj, isSignificant ? RegionDescription.SIGNIFICANT : RegionDescription.FULL);
	}
	
	/**
	 * @since 2.3
	 */
	@Nullable
	public ITextRegion getTextRegion(@NonNull EObject object, @NonNull RegionDescription query) {
		switch(query) {
			// we delegate the implementation to the existing and potentially overridden methods
			case SIGNIFICANT: return getSignificantTextRegion(object);
			case FULL: return getFullTextRegion(object);
			default:
				return doGetTextRegion(object, query);
		}
	}
	
	/**
	 * @since 2.3
	 */
	protected ITextRegion doGetTextRegion(EObject obj, @NonNull RegionDescription query) {
		ICompositeNode node = NodeModelUtils.findActualNodeFor(obj);
		if (node == null) {
			if (obj.eContainer() == null)
				return ITextRegion.EMPTY_REGION;
			return getTextRegion(obj.eContainer(), query);
		}
		List<INode> nodes = null;
		if (query == RegionDescription.SIGNIFICANT)
			nodes = getLocationNodes(obj);
		if (nodes == null || nodes.isEmpty())
			nodes = Collections.<INode>singletonList(node);
		return createRegion(nodes, query);
	}
	
	public ITextRegion getSignificantTextRegion(final EObject owner, EStructuralFeature feature, final int indexInList) {
		return getTextRegion(owner, feature, indexInList, true);
	}

	public ITextRegion getFullTextRegion(EObject owner, EStructuralFeature feature, int indexInList) {
		return getTextRegion(owner, feature, indexInList, false);
	}
	
	/**
	 * @since 2.3
	 */
	@NonNullByDefault
	@Nullable
	public ITextRegion getTextRegion(EObject object, EStructuralFeature feature, int indexInList,
			RegionDescription query) {
		switch(query) {
			// we delegate the implementation to the existing and potentially overridden methods
			case SIGNIFICANT: return getSignificantTextRegion(object, feature, indexInList);
			case FULL: return getFullTextRegion(object, feature, indexInList);
			default:
				return doGetTextRegion(object, feature, indexInList, query);
		}
	}

	private ITextRegion getTextRegion(final EObject owner, EStructuralFeature feature, final int indexInList,
			final boolean isSignificant) {
		if (feature instanceof EAttribute) {
			return getLocationOfAttribute(owner, (EAttribute)feature, indexInList, isSignificant);
		} else if (feature instanceof EReference) {
			EReference reference = (EReference) feature;
			if (reference.isContainment() || reference.isContainer()) {
				return getLocationOfContainmentReference(owner, reference, indexInList, isSignificant);
			} else {
				return getLocationOfCrossReference(owner, reference, indexInList, isSignificant);
			}
		} else {
			return null;
		}
	}
	
	private ITextRegion doGetTextRegion(final EObject owner, EStructuralFeature feature, final int indexInList,
			final RegionDescription query) {
		if (feature == null)
			return null;
		if (feature instanceof EAttribute) {
			return doGetLocationOfFeature(owner, feature, indexInList, query);
		}
		if (feature instanceof EReference) {
			EReference reference = (EReference) feature;
			if (reference.isContainment() || reference.isContainer()) {
				return getLocationOfContainmentReference(owner, reference, indexInList, query);
			} else {
				return doGetLocationOfFeature(owner, reference, indexInList, query);
			}
		}
		return null;
	}

	protected ITextRegion getLocationOfContainmentReference(final EObject owner, EReference feature,
			final int indexInList, boolean isSignificant) {
		return getLocationOfContainmentReference(owner, feature, indexInList, isSignificant ? RegionDescription.SIGNIFICANT : RegionDescription.FULL);
	}
	
	/**
	 * @since 2.3
	 */
	protected ITextRegion getLocationOfContainmentReference(final EObject owner, EReference feature,
			final int indexInList, RegionDescription query) {
		Object referencedElement = null;
		if(feature.isMany()) {
			List<?> values = (List<?>) owner.eGet(feature);
			if(indexInList >= values.size())
				referencedElement = owner;
			else
				referencedElement = values.get(indexInList);
		}
		else {
			referencedElement = owner.eGet(feature);
		}
		return getTextRegion((EObject) referencedElement, query);
	}

	protected ITextRegion getLocationOfCrossReference(EObject owner, EReference reference, int indexInList,
			boolean isSignificant) {
		return doGetLocationOfFeature(owner, reference, indexInList, isSignificant);
	}

	protected ITextRegion getLocationOfAttribute(EObject owner, EAttribute attribute, int indexInList,
			boolean isSignificant) {
		return doGetLocationOfFeature(owner, attribute, indexInList, isSignificant);
	}

	protected ITextRegion doGetLocationOfFeature(EObject owner, EStructuralFeature feature, int indexInList,
			boolean isSignificant) {
		return doGetLocationOfFeature(owner, feature, indexInList, isSignificant ? RegionDescription.SIGNIFICANT : RegionDescription.FULL);
	}
	
	/**
	 * @since 2.3
	 */
	protected ITextRegion doGetLocationOfFeature(EObject owner, EStructuralFeature feature, int indexInList,
			RegionDescription query) {
		if (!feature.isMany())
			indexInList = 0;
		if (indexInList >= 0) {
			List<INode> findNodesForFeature = NodeModelUtils.findNodesForFeature(owner, feature);
			if (indexInList < findNodesForFeature.size()) {
				INode node = findNodesForFeature.get(indexInList);
				return createRegion(Collections.singletonList(node), query);
			}
			return getTextRegion(owner, query);
		}
		return null;
	}

	protected List<INode> getLocationNodes(EObject obj) {
		final EStructuralFeature nameFeature = getIdentifierFeature(obj);
		if (nameFeature != null) {
			List<INode> result = NodeModelUtils.findNodesForFeature(obj, nameFeature);
			if (!result.isEmpty())
				return result;
		}

		List<INode> resultNodes = Lists.newArrayList();
		final ICompositeNode startNode = NodeModelUtils.getNode(obj);
		INode keywordNode = null;
		// use LeafNodes instead of children?
		for (INode child : startNode.getChildren()) {
			EObject grammarElement = child.getGrammarElement();
			if (grammarElement instanceof RuleCall) {
				RuleCall ruleCall = (RuleCall) grammarElement;
				String ruleName = ruleCall.getRule().getName();
				if (ruleName.equals("ID")) {
					resultNodes.add(child);
				}
			} else if (grammarElement instanceof Keyword) {
				// TODO use only keywords, that aren't symbols like '=' ?
				if (keywordNode == null && useKeyword((Keyword) grammarElement, obj)) {
					keywordNode = child;
				}
			}
		}
		if (resultNodes.isEmpty() && keywordNode != null)
			resultNodes.add(keywordNode);
		return resultNodes;
	}

	protected boolean useKeyword(Keyword keyword, EObject context) {
		return true;
	}

	protected EStructuralFeature getIdentifierFeature(EObject obj) {
		final EClass eClass = obj.eClass();
		final EStructuralFeature result = eClass.getEStructuralFeature("name");
		return result != null ? result : eClass.getEStructuralFeature("id");
	}

	protected ITextRegion createRegion(final List<INode> nodes) {
		ITextRegion result = ITextRegion.EMPTY_REGION;
		for (INode node : nodes) {
			if (!isHidden(node)) {
				int length = node.getLength();
				if (length != 0)
					result = result.merge(new TextRegionWithLineInformation(node.getOffset(), length, node.getStartLine() - 1, node.getEndLine() - 1));
			}
		}
		return result;
	}
	
	/**
	 * @since 2.3
	 */
	protected ITextRegion createRegion(List<INode> nodes, RegionDescription query) {
		if (query == RegionDescription.FULL || query == RegionDescription.SIGNIFICANT)
			return createRegion(nodes);
		ITextRegion result = ITextRegion.EMPTY_REGION;
		for (INode node : nodes) {
			for(INode leafNode: node.getLeafNodes()) {
				if (!isHidden(leafNode, query)) {
					int length = leafNode.getLength();
					if (length != 0)
						result = result.merge(new TextRegionWithLineInformation(leafNode.getOffset(), length, leafNode.getStartLine() - 1, leafNode.getEndLine() - 1));
				}
			}
		}
		return result;
	}

	/**
	 * @since 2.3
	 */
	protected boolean isHidden(INode node, RegionDescription query) {
		if (query == RegionDescription.INCLUDING_WHITESPACE) {
			return false;
		}
		if (node instanceof ILeafNode && ((ILeafNode) node).isHidden()) {
			if (query == RegionDescription.INCLUDING_COMMENTS) {
				if (hiddenTokenHelper != null && node.getGrammarElement() instanceof AbstractRule) {
					boolean result = hiddenTokenHelper.isWhitespace((AbstractRule) node.getGrammarElement());
					return result;
				}
				boolean result = node.getText().trim().length() == 0;
				return result;
			} else {
				return true;
			}
		}
		return false;
	}
	
	protected boolean isHidden(INode node) {
		return node instanceof ILeafNode && ((ILeafNode) node).isHidden();
	}
}
