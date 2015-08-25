/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xtext.generator.exporting

import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.naming.SimpleNameProvider
import org.eclipse.xtext.xtext.generator.AbstractGeneratorFragment2
import org.eclipse.xtext.xtext.generator.model.GuiceModuleAccess

import static extension org.eclipse.xtext.xtext.generator.model.TypeReference.*

class SimpleNamesFragment2 extends AbstractGeneratorFragment2 {
	
	override generate() {
		new GuiceModuleAccess.BindingFactory()
			.addfinalTypeToType(IQualifiedNameProvider.typeRef, SimpleNameProvider.typeRef)
			.contributeTo(language.runtimeGenModule)
		new GuiceModuleAccess.BindingFactory()
			.addTypeToType('org.eclipse.xtext.ui.refactoring.IDependentElementsCalculator'.typeRef,
					'org.eclipse.xtext.ui.refactoring.impl.DefaultDependentElementsCalculator'.typeRef)
			.contributeTo(language.eclipsePluginGenModule)
	}
	
}