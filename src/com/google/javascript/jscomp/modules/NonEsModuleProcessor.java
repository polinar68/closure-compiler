/*
 * Copyright 2018 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.javascript.jscomp.modules;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.javascript.jscomp.ModuleMetadataMap.ModuleMetadata;
import com.google.javascript.jscomp.deps.ModuleLoader.ModulePath;
import com.google.javascript.jscomp.modules.ModuleMapCreator.ExportTrace;
import com.google.javascript.jscomp.modules.ModuleMapCreator.ModuleProcessor;
import com.google.javascript.jscomp.modules.ModuleMapCreator.ModuleRequestResolver;
import com.google.javascript.jscomp.modules.ModuleMapCreator.ResolveExportResult;
import com.google.javascript.jscomp.modules.ModuleMapCreator.UnresolvedModule;
import com.google.javascript.rhino.Node;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Catch all module processor for non-ES modules that doesn't do any scanning of exports but instead
 * will always treat every name as `munged.name`.
 */
// TODO(johnplaisted): It may not be too hard to actually scan these modules and figure out their
// exported keys. At least, assuming we're rigid with what we allow (no dynamic exports).
final class NonEsModuleProcessor implements ModuleProcessor {

  private static class NonEsModule extends UnresolvedModule {

    private final ModuleMetadata metadata;
    private final ModulePath path;
    private final Node scriptNode;

    NonEsModule(ModuleMetadata metadata, ModulePath path, Node scriptNode) {
      this.metadata = metadata;
      this.path = path;
      this.scriptNode = scriptNode;
    }

    @Nullable
    @Override
    public ResolveExportResult resolveExport(String exportName) {
      throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public ResolveExportResult resolveExport(
        @Nullable String moduleSpecifier,
        String exportName,
        Set<ExportTrace> resolveSet,
        Set<UnresolvedModule> exportStarSet) {
      String namespace = null;
      if (GoogEsImports.isGoogImportSpecifier(moduleSpecifier)) {
        namespace = GoogEsImports.getClosureIdFromGoogImportSpecifier(moduleSpecifier);
      }
      return ResolveExportResult.of(
          Binding.from(
              Export.builder()
                  .localName(exportName)
                  .moduleMetadata(metadata)
                  .modulePath(path)
                  .closureNamespace(namespace)
                  .build(),
              scriptNode));
    }

    @Override
    public Module resolve(@Nullable String moduleSpecifier) {
      String namespace = null;
      if (moduleSpecifier != null && GoogEsImports.isGoogImportSpecifier(moduleSpecifier)) {
        namespace = GoogEsImports.getClosureIdFromGoogImportSpecifier(moduleSpecifier);
      }
      return Module.builder()
          .path(path)
          .metadata(metadata)
          .namespace(ImmutableMap.of())
          .boundNames(ImmutableMap.of())
          .localNameToLocalExport(ImmutableMap.of())
          .closureNamespace(namespace)
          .build();
    }

    @Override
    public boolean isEsModule() {
      return false;
    }

    @Override
    public ImmutableSet<String> getExportedNames() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableSet<String> getExportedNames(Set<UnresolvedModule> visited) {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public UnresolvedModule process(
      ModuleRequestResolver requestResolver,
      ModuleMetadata metadata,
      ModulePath path,
      Node script) {
    return new NonEsModule(metadata, path, script);
  }
}