/**
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtext.xtext.generator;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.emf.mwe.utils.DirectoryCleaner;
import org.eclipse.xtend.lib.annotations.AccessorType;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xtext.generator.IGuiceAwareGeneratorComponent;
import org.eclipse.xtext.xtext.generator.model.IXtextGeneratorFileSystemAccess;
import org.eclipse.xtext.xtext.generator.model.project.IRuntimeProjectConfig;
import org.eclipse.xtext.xtext.generator.model.project.ISubProjectConfig;
import org.eclipse.xtext.xtext.generator.model.project.IXtextProjectConfig;

/**
 * @noextend
 */
@SuppressWarnings("all")
public class XtextDirectoryCleaner implements IGuiceAwareGeneratorComponent {
  @Inject
  private IXtextProjectConfig config;
  
  @Accessors(AccessorType.PUBLIC_SETTER)
  private boolean enabled = true;
  
  @Accessors(AccessorType.PUBLIC_SETTER)
  private boolean useDefaultExcludes = true;
  
  private List<String> excludes = CollectionLiterals.<String>newArrayList();
  
  private List<String> extraDirectories = CollectionLiterals.<String>newArrayList();
  
  public void addExtraDirectory(final String directory) {
    this.extraDirectories.add(directory);
  }
  
  public void addExclude(final String exclude) {
    this.excludes.add(exclude);
  }
  
  public void clean() {
    if ((!this.enabled)) {
      return;
    }
    final ArrayList<String> directories = CollectionLiterals.<String>newArrayList();
    List<? extends ISubProjectConfig> _enabledProjects = this.config.getEnabledProjects();
    final Function1<ISubProjectConfig, IXtextGeneratorFileSystemAccess> _function = (ISubProjectConfig it) -> {
      return it.getSrcGen();
    };
    List<IXtextGeneratorFileSystemAccess> _map = ListExtensions.map(_enabledProjects, _function);
    IRuntimeProjectConfig _runtime = this.config.getRuntime();
    IXtextGeneratorFileSystemAccess _ecoreModel = _runtime.getEcoreModel();
    Iterable<IXtextGeneratorFileSystemAccess> _plus = Iterables.<IXtextGeneratorFileSystemAccess>concat(_map, Collections.<IXtextGeneratorFileSystemAccess>unmodifiableList(CollectionLiterals.<IXtextGeneratorFileSystemAccess>newArrayList(_ecoreModel)));
    Iterable<IXtextGeneratorFileSystemAccess> _filterNull = IterableExtensions.<IXtextGeneratorFileSystemAccess>filterNull(_plus);
    final Function1<IXtextGeneratorFileSystemAccess, String> _function_1 = (IXtextGeneratorFileSystemAccess it) -> {
      return it.getPath();
    };
    Iterable<String> _map_1 = IterableExtensions.<IXtextGeneratorFileSystemAccess, String>map(_filterNull, _function_1);
    final Function1<String, Boolean> _function_2 = (String it) -> {
      File _file = new File(it);
      return Boolean.valueOf(_file.isDirectory());
    };
    Iterable<String> _filter = IterableExtensions.<String>filter(_map_1, _function_2);
    Iterables.<String>addAll(directories, _filter);
    Iterables.<String>addAll(directories, this.extraDirectories);
    final DirectoryCleaner delegate = new DirectoryCleaner();
    delegate.setUseDefaultExcludes(this.useDefaultExcludes);
    final Consumer<String> _function_3 = (String it) -> {
      delegate.addExclude(it);
    };
    this.excludes.forEach(_function_3);
    final Consumer<String> _function_4 = (String it) -> {
      try {
        delegate.cleanFolder(it);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    directories.forEach(_function_4);
  }
  
  @Override
  public void initialize(final Injector injector) {
    injector.injectMembers(this);
  }
  
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }
  
  public void setUseDefaultExcludes(final boolean useDefaultExcludes) {
    this.useDefaultExcludes = useDefaultExcludes;
  }
}
