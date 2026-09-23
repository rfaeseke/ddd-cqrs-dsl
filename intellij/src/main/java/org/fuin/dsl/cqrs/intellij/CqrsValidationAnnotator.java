package org.fuin.dsl.cqrs.intellij;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.fuin.dsl.cqrs.intellij.psi.CqrsAnnotationDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsAnnotationInstance;
import org.fuin.dsl.cqrs.intellij.psi.CqrsAggregateDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsAggregateId;
import org.fuin.dsl.cqrs.intellij.psi.CqrsAttribute;
import org.fuin.dsl.cqrs.intellij.psi.CqrsCollisionStrategy;
import org.fuin.dsl.cqrs.intellij.psi.CqrsConsistency;
import org.fuin.dsl.cqrs.intellij.psi.CqrsConsistencyLevel;
import org.fuin.dsl.cqrs.intellij.psi.CqrsWeakConsistency;
import org.fuin.dsl.cqrs.intellij.psi.CqrsConstraintDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsConstraintInstance;
import org.fuin.dsl.cqrs.intellij.psi.CqrsConstructorDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsElement;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEntityElement;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEntityPathArgument;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEntityDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsDependencyDecl;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEntityId;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEntityIdPath;
import org.fuin.dsl.cqrs.intellij.psi.CqrsPathSegment;
import org.fuin.dsl.cqrs.intellij.psi.CqrsSegmentRange;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEventDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsExceptionDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsExternalType;
import org.fuin.dsl.cqrs.intellij.psi.CqrsGenericArgs;
import org.fuin.dsl.cqrs.intellij.psi.CqrsHintDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsImportDecl;
import org.fuin.dsl.cqrs.intellij.psi.CqrsImportFqn;
import org.fuin.dsl.cqrs.intellij.psi.CqrsInvariants;
import org.fuin.dsl.cqrs.intellij.psi.CqrsJson;
import org.fuin.dsl.cqrs.intellij.psi.CqrsMethodDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsModuleDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsNamedElement;
import org.fuin.dsl.cqrs.intellij.psi.CqrsNames;
import org.fuin.dsl.cqrs.intellij.psi.CqrsContextDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsReferenceElement;
import org.fuin.dsl.cqrs.intellij.psi.CqrsProcessManager;
import org.fuin.dsl.cqrs.intellij.psi.CqrsPsiUtil;
import org.fuin.dsl.cqrs.intellij.psi.CqrsIssuedCommand;
import org.fuin.dsl.cqrs.intellij.psi.CqrsProcessReaction;
import org.fuin.dsl.cqrs.intellij.psi.CqrsProcessState;
import org.fuin.dsl.cqrs.intellij.psi.CqrsParameter;
import org.fuin.dsl.cqrs.intellij.psi.CqrsPreconditions;
import org.fuin.dsl.cqrs.intellij.psi.CqrsServiceDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsBusinessRule;
import org.fuin.dsl.cqrs.intellij.psi.CqrsBusinessRuleInstance;
import org.fuin.dsl.cqrs.intellij.psi.CqrsKeyDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsTypeRef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsTypes;
import org.fuin.dsl.cqrs.intellij.psi.CqrsValueObject;
import org.fuin.dsl.cqrs.intellij.psi.CqrsViewDef;
import org.fuin.dsl.cqrs.intellij.reference.CqrsResolveUtil;
import org.fuin.dsl.cqrs.intellij.remote.CqrsRemoteScopeResolver;
import org.fuin.dsl.cqrs.intellij.remote.RemoteScopeEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.attributeNames;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.concrete;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.findUnknownVar;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.firstTokenAfter;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.firstTypeRefAfter;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.nameRange;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.parameterNames;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.parseIntOrZero;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.resolve;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.simpleName;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.sameType;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.stringValue;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.typeNames;
import static org.fuin.dsl.cqrs.intellij.CqrsValidationUtil.typeRefsAfter;

/**
 * Semantic validation for the CQRS DSL, ported from the Eclipse/Xtext {@code CqrsDslValidator}. It
 * reproduces the self-contained (single-file) {@code @Check} rules on top of the Grammar-Kit PSI.
 *
 * <p>Cross-file / project-wide rules (duplicate exception CID, aggregate/entity direct-reference
 * containment, the {@code identifies}/{@code root} placement rules) and the constraint-message and
 * internal-type-invariant target checks are intentionally not ported here — see the README/plan.
 *
 * <p>Resolution-dependent checks are lenient: when a referenced declaration cannot be resolved (for
 * example a not-yet-cached remote model) the check is skipped rather than reporting a false error.
 */
public final class CqrsValidationAnnotator implements Annotator {

    /** Bases whose generated valueOf/converter builds the type from a single value. */
    private static final List<String> CONSTRUCTED_FROM_BASE = List.of("UUID", "Integer", "Long", "BigDecimal");

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // A model that is only read - an entry of a dependency's archive, or a file of a 'local'
        // directory outside the project - is reported on by whoever authors it, not here.
        if (!CqrsRemoteScopeResolver.inProject(element.getContainingFile())) {
            return;
        }
        if (element instanceof CqrsValueObject) {
            CqrsValueObject vo = (CqrsValueObject) element;
            checkValueObjectBase(vo, holder);
            checkRowAnswersTheGatesItOffers(vo, holder);
            checkBaseTypeAttributes(vo, vo.getAttributeList(), "value-object", holder);
        } else if (element instanceof CqrsEntityId) {
            CqrsEntityId entityId = (CqrsEntityId) element;
            checkBaseTypeAttributes(entityId, entityId.getAttributeList(), "entity-id", holder);
        } else if (element instanceof CqrsAggregateId) {
            CqrsAggregateId aggregateId = (CqrsAggregateId) element;
            checkBaseTypeAttributes(aggregateId, aggregateId.getAttributeList(), "aggregate-id", holder);
        } else if (element instanceof CqrsAttribute) {
            CqrsAttribute attribute = (CqrsAttribute) element;
            checkVariableNameLowerCase(attribute, holder);
            checkGenericArgs(attribute.getTypeRef(), attribute.getGenericArgs(), holder);
            checkConstraintInstancesTargetType(attribute.getInvariants(), attribute.getTypeRef(),
                    "The allowed input types of the constraint", "do not match the attribute type", holder);
        } else if (element instanceof CqrsParameter) {
            CqrsParameter parameter = (CqrsParameter) element;
            checkVariableNameLowerCase(parameter, holder);
            checkGenericArgs(parameter.getTypeRef(), parameter.getGenericArgs(), holder);
            CqrsPreconditions preconditions = parameter.getPreconditions();
            checkConstraintInstancesTargetType(preconditions == null ? null : preconditions.getConstraintInstanceList(),
                    parameter.getTypeRef(), "The input type of the constraint", "does not match the parameter type", holder);
        } else if (element instanceof CqrsExceptionDef) {
            checkExceptionMessage((CqrsExceptionDef) element, holder);
        } else if (element instanceof CqrsEventDef) {
            checkEventMessage((CqrsEventDef) element, holder);
        } else if (element instanceof CqrsConsistency) {
            checkConsistency((CqrsConsistency) element, holder);
        } else if (element instanceof CqrsAggregateDef) {
            checkAggregate((CqrsAggregateDef) element, holder);
        } else if (element instanceof CqrsEntityDef) {
            checkEntity((CqrsEntityDef) element, holder);
        } else if (element instanceof CqrsMethodDef) {
            checkNoEventsInServiceMethod((CqrsMethodDef) element, holder);
        } else if (element instanceof CqrsAnnotationInstance) {
            checkAnnotationInstanceArgs((CqrsAnnotationInstance) element, holder);
        } else if (element instanceof CqrsProcessManager) {
            CqrsProcessManager pm = (CqrsProcessManager) element;
            checkProcessManager(pm, holder);
            checkProcessManagerCronSchedule(pm, holder);
        } else if (element instanceof CqrsDependencyDecl) {
            checkDependency((CqrsDependencyDecl) element, holder);
        } else if (element instanceof CqrsImportDecl) {
            checkImport((CqrsImportDecl) element, holder);
        } else if (element instanceof CqrsProcessReaction) {
            checkProcessReaction((CqrsProcessReaction) element, holder);
        } else if (element instanceof CqrsViewDef) {
            checkViewCronSchedule((CqrsViewDef) element, holder);
        } else if (element instanceof CqrsHintDef) {
            checkHintJson((CqrsHintDef) element, holder);
        } else if (element instanceof CqrsModuleDef) {
            checkModuleDependencyCycle((CqrsModuleDef) element, holder);
        } else if (element instanceof CqrsEntityIdPath) {
            checkEntityIdPathShape((CqrsEntityIdPath) element, holder);
        } else if (element instanceof CqrsSegmentRange) {
            checkSegmentRange((CqrsSegmentRange) element, holder);
        } else if (element instanceof CqrsEntityPathArgument) {
            checkOwnPath((CqrsEntityPathArgument) element, holder);
        } else if (element instanceof CqrsKeyDef) {
            CqrsKeyDef key = (CqrsKeyDef) element;
            checkKeyExceptionMatchesCollision(key, holder);
            checkOneDisplayKeyPerType(key, holder);
            checkDisplayAsVariables(key, holder);
            checkKeyRuleNameIsFree(key, holder);
        } else if (element instanceof CqrsBusinessRuleInstance) {
            checkKeyUsage((CqrsBusinessRuleInstance) element, holder);
        }
    }

    // --- business keys --------------------------------------------------------------------------

    /**
     * Reports a key whose collision behaviour and exception disagree.
     *
     * <p>The port of the Xtext validator's {@code checkKeyExceptionMatchesCollision}. Only a key that
     * refuses throws anything: {@code overwrite} and {@code skip} say what the handler does with the
     * second occurrence, and neither refuses anybody. An exception declared beside one of those is
     * generated, never raised, and reads as a refusal that can happen.</p>
     *
     * <p>The Xtext validator also refuses a key written outside a type. There is nothing to port: this
     * grammar only matches {@code key_def} inside an aggregate or an entity, so the editor rejects it
     * as a parse error before any annotator sees it.</p>
     */
    private void checkKeyExceptionMatchesCollision(@NotNull CqrsKeyDef key, @NotNull AnnotationHolder holder) {
        CqrsCollisionStrategy strategy = key.getCollisionStrategy();
        if (strategy == null) {
            return;
        }
        boolean refuses = "refuse".equals(strategy.getText().trim());
        CqrsTypeRef exception = key.getTypeRef();
        if (refuses && exception == null) {
            error(holder, nameRange(key),
                    "'" + key.getName() + "' refuses a collision, so it needs the exception that says so");
        } else if (!refuses && exception != null) {
            error(holder, exception, "'" + key.getName() + "' " + strategy.getText().trim()
                    + "s a collision rather than refusing it, so it has nothing to throw");
        }
    }

    /**
     * Reports a second key saying how its type is displayed.
     *
     * <p>The port of the Xtext validator's {@code checkOneDisplayKeyPerType}. {@code display-as}
     * carries a format and its presence is the marking, so two of them are two answers to "what does a
     * picker show" with no precedence rule to choose between them.</p>
     */
    private void checkOneDisplayKeyPerType(@NotNull CqrsKeyDef key, @NotNull AnnotationHolder holder) {
        PsiElement format = key.getString();
        if (format == null) {
            return;
        }
        List<CqrsKeyDef> displaying = new ArrayList<>();
        for (CqrsKeyDef sibling : PsiTreeUtil.getChildrenOfTypeAsList(key.getParent(), CqrsKeyDef.class)) {
            if (sibling.getString() != null) {
                displaying.add(sibling);
            }
        }
        if (displaying.size() > 1 && displaying.get(0) != key) {
            error(holder, format, "'" + displaying.get(0).getName() + "' already says how this type is displayed");
        }
    }

    /**
     * Reports a {@code display-as} format naming something the declaring type does not have.
     *
     * <p>The port of the Xtext validator's {@code checkVariablesInDisplayAs}. Deliberately the whole
     * type rather than the key's own attributes: what a person recognises a thing by is not always what
     * makes it unique - an account is keyed by its IBAN and read as "Business current account
     * (CH93...)", and its name is no part of the key.</p>
     */
    private void checkDisplayAsVariables(@NotNull CqrsKeyDef key, @NotNull AnnotationHolder holder) {
        PsiElement format = key.getString();
        if (format == null) {
            return;
        }
        List<CqrsAttribute> attributes = PsiTreeUtil.getChildrenOfTypeAsList(key.getParent(), CqrsAttribute.class);
        String unknown = findUnknownVar(attributeNames(attributes), stringValue(format));
        if (unknown != null) {
            error(holder, format, "A variable '" + unknown + "' is not an attribute of this type");
        }
    }

    /**
     * Reports a key whose derived rule collides with one written by hand.
     *
     * <p>The port of the Xtext validator's {@code checkKeyRuleNameIsFree}. A key called {@code Iban}
     * derives {@code IbanMustBeUnique}, which is what the rule it replaces is usually already called;
     * both generate a class of that name into one package and the second one written wins silently.</p>
     */
    private void checkKeyRuleNameIsFree(@NotNull CqrsKeyDef key, @NotNull AnnotationHolder holder) {
        CqrsModuleDef module = PsiTreeUtil.getParentOfType(key, CqrsModuleDef.class);
        if (module == null || key.getName() == null) {
            return;
        }
        String derived = CqrsKeys.ruleName(key);
        for (CqrsBusinessRule rule : PsiTreeUtil.findChildrenOfType(module, CqrsBusinessRule.class)) {
            if (derived.equals(rule.getName())) {
                error(holder, nameRange(key), key.getName() + " derives a rule called " + derived
                        + ", and one of that name is already written by hand - the key replaces it,"
                        + " so the declared rule goes");
                return;
            }
        }
    }

    /**
     * Reports what an operation cannot say about a business key it names.
     *
     * <p>The port of the Xtext validator's {@code checkKeyUsageRefuses},
     * {@code checkDerivedKeyHasSomewhereToAsk} and {@code checkDerivedKeyActualsBind}. A usage that
     * names the key and stops has everything else derived from the key and the operation, and the three
     * shapes that cannot be derived are refused rather than guessed - a uniqueness check made against
     * the wrong value still compiles, still runs and still passes.</p>
     */
    private void checkKeyUsage(@NotNull CqrsBusinessRuleInstance instance, @NotNull AnnotationHolder holder) {
        CqrsNamedElement declaration = resolve(instance.getTypeRef());
        if (!(declaration instanceof CqrsKeyDef)) {
            return;
        }
        CqrsKeyDef key = (CqrsKeyDef) declaration;
        if (!CqrsKeys.refuses(key)) {
            String strategy = key.getCollisionStrategy() == null ? "" : key.getCollisionStrategy().getText().trim();
            error(holder, instance.getTypeRef(), key.getName() + " " + strategy
                    + "s a collision rather than refusing it, so no operation is guarded by it");
            return;
        }
        if (!instance.getRuleArgumentList().isEmpty()) {
            // The model says what it hands over, so nothing is derived and nothing below applies.
            return;
        }
        PsiElement operation = CqrsKeys.operationOf(instance);
        if (operation == null) {
            return;
        }
        String operationName = ((CqrsNamedElement) operation).getName();
        if (!CqrsKeys.hasOwnOperationContext(operation)) {
            error(holder, instance.getTypeRef(), operationName + " checks " + key.getName()
                    + ", which asks whether the key is taken, so it needs an 'operation-context' service"
                    + " of its own for that answer to be declared on");
            return;
        }
        List<String> ambiguous = CqrsKeys.ambiguousAttributes(key, operation);
        if (!ambiguous.isEmpty()) {
            error(holder, instance.getTypeRef(), operationName + " has more than one parameter of "
                    + ambiguous.get(0) + "'s type, so " + key.getName()
                    + " cannot say which one it is keyed on - write the actuals out");
            return;
        }
        if (CqrsKeys.creates(operation)) {
            for (String name : CqrsKeys.attributeNames(key)) {
                if (!CqrsKeys.bindsToAParameter(key, name, operation)) {
                    error(holder, instance.getTypeRef(), operationName
                            + " creates, so it has no prior state to read '" + name
                            + "' from, and takes no parameter of its type");
                    return;
                }
            }
        }
    }

    // --- module dependency cycle ----------------------------------------------------------------

    /**
     * Reports a module that takes part in a dependency cycle.
     *
     * <p>The port of the Xtext validator's {@code checkModuleDependencyCycle}, so the editor refuses
     * what the build refuses. A cycle means neither module can be reasoned about - or switched off -
     * without the other, and the generated module dependency graph has no topological order to offer.</p>
     *
     * <p><b>A module, not a bounded context.</b> The nodes are the {@code module} blocks as declared,
     * which is what keeps the seams between contexts legal: a process manager may react to another
     * context's events without dragging the context it lives beside into a cycle.</p>
     *
     * <p>Annotating the module block rather than each reference inside it keeps the project-wide graph
     * to a handful of lookups per file, and the graph itself is cached per PSI modification.</p>
     */
    private void checkModuleDependencyCycle(@NotNull CqrsModuleDef module, @NotNull AnnotationHolder holder) {
        String name = CqrsResolveUtil.getQualifiedName(module);
        if (name == null || name.isEmpty()) {
            return;
        }
        List<String> cycle = CqrsModuleGraph.cycleThrough(name, CqrsModuleGraph.of(module.getProject()));
        if (cycle.isEmpty()) {
            return;
        }
        PsiElement range = module.getNameIdentifier() != null ? module.getNameIdentifier() : module;
        error(holder, range, "Module '" + name + "' is part of a dependency cycle: " + String.join(" -> ", cycle));
    }

    // --- hint JSON ------------------------------------------------------------------------------

    /**
     * Validates a hint's JSON against the schema that matches its name (a schema violation is an error)
     * and warns when a 'JpaHint' is declared outside a view (it generates nothing there).
     */
    private void checkHintJson(@NotNull CqrsHintDef hint, @NotNull AnnotationHolder holder) {
        String name = hint.getName();
        String schema = CqrsHintJson.schemaForHintName(name);
        CqrsJson json = hint.getJson();
        if (schema != null && json != null) {
            for (String message : CqrsHintJson.validate(json, schema)) {
                error(holder, json, message);
            }
        }
        if ("JpaHint".equals(CqrsHintJson.simpleName(name))
                && PsiTreeUtil.getParentOfType(hint, CqrsViewDef.class) == null) {
            holder.newAnnotation(HighlightSeverity.WARNING, "JpaHint only generates code inside a view")
                    .range(nameRange(hint))
                    .create();
        }
    }

    // --- dependency -----------------------------------------------------------------------------

    /**
     * Reports a coordinate that is not a Maven GAV, and a coordinate declared twice in the same
     * block. Mirrors {@code CqrsDslValidator.checkDependencyCoordinate/checkDuplicateDependency}.
     */
    private void checkDependency(@NotNull CqrsDependencyDecl dependency, @NotNull AnnotationHolder holder) {
        String coordinate = CqrsPsiUtil.getDependencyCoordinate(dependency);
        PsiElement range = CqrsValidationUtil.firstTokenAfter(
                dependency, CqrsTypes.KW_DEPENDENCY, CqrsTypes.STRING);
        if (range == null) {
            return;
        }
        String local = CqrsPsiUtil.getDependencyLocal(dependency);
        RemoteScopeEntry entry = RemoteScopeEntry.parse(coordinate, local);
        if (entry == null) {
            error(holder, range, "A dependency must be 'groupId:artifactId:version', but was '" + coordinate + "'");
            return;
        }
        PsiElement parent = dependency.getParent();
        if (parent == null) {
            return;
        }
        for (CqrsDependencyDecl sibling : PsiTreeUtil.getChildrenOfTypeAsList(parent, CqrsDependencyDecl.class)) {
            if (sibling == dependency) {
                break; // only the later occurrence is reported
            }
            if (coordinate != null && coordinate.equals(CqrsPsiUtil.getDependencyCoordinate(sibling))) {
                error(holder, range, "Duplicate dependency '" + coordinate + "'");
                return;
            }
        }

        // A well formed coordinate must also resolve to something. Reads only the on-disk cache and
        // what the background download already reported, so nothing blocks here.
        PsiFile file = dependency.getContainingFile();
        if (file != null) {
            String problem = CqrsRemoteScopeResolver.getInstance(dependency.getProject())
                    .problem(file, entry);
            if (problem != null) {
                error(holder, range, "Cannot resolve dependency '" + coordinate + "': " + problem);
            }
        }
    }

    // --- import ---------------------------------------------------------------------------------

    /**
     * Reports an import that matches nothing, an import declared twice in the same block, an import
     * a module repeats from its context, and an import nothing in the block refers to. Mirrors
     * {@code CqrsDslValidator.checkImportResolves/checkDuplicateImport/checkUnusedImport}.
     */
    private void checkImport(@NotNull CqrsImportDecl imp, @NotNull AnnotationHolder holder) {
        CqrsImportFqn fqnElement = PsiTreeUtil.getChildOfType(imp, CqrsImportFqn.class);
        if (fqnElement == null) {
            return;
        }
        String imported = CqrsNames.unescapeQualified(fqnElement.getText().replaceAll("\\s+", ""));
        if (imported == null || imported.isEmpty()) {
            return;
        }

        PsiElement parent = imp.getParent();
        if (parent == null) {
            return;
        }

        // Duplicate in the same block - only the later occurrence is reported.
        for (CqrsImportDecl sibling : PsiTreeUtil.getChildrenOfTypeAsList(parent, CqrsImportDecl.class)) {
            if (sibling == imp) {
                break;
            }
            if (imported.equals(importedNameOf(sibling))) {
                error(holder, fqnElement, "Duplicate import '" + imported + "'");
                return;
            }
        }

        List<String> single = List.of(imported);
        boolean matchesSomething = false;
        for (CqrsNamedElement decl : CqrsResolveUtil.resolvableDeclarations(imp)) {
            String fqn = CqrsResolveUtil.getQualifiedName(decl);
            if (CqrsResolveUtil.coveredByImport(fqn, single) || fqn.equals(imported)) {
                matchesSomething = true;
                break;
            }
        }
        if (!matchesSomething) {
            error(holder, fqnElement, "Import '" + imported + "' does not match any context, module or type");
            return;
        }

        // A module repeating an import its context already declares is only a warning.
        if (parent instanceof CqrsModuleDef) {
            CqrsContextDef context = PsiTreeUtil.getParentOfType(imp, CqrsContextDef.class);
            if (context != null) {
                for (CqrsImportDecl ctxImport : PsiTreeUtil.getChildrenOfTypeAsList(context, CqrsImportDecl.class)) {
                    if (imported.equals(importedNameOf(ctxImport))) {
                        holder.newAnnotation(HighlightSeverity.WARNING,
                                        "Import '" + imported + "' is already declared by context '"
                                                + context.getName() + "'")
                                .range(fqnElement)
                                .create();
                        return;
                    }
                }
            }
        }

        if (!isImportUsed(parent, imported)) {
            holder.newAnnotation(HighlightSeverity.WARNING, "Import '" + imported + "' is not used")
                    .range(fqnElement)
                    .create();
        }
    }

    /** Whether any reference inside the block resolves to something the import covers. */
    private boolean isImportUsed(@NotNull PsiElement block, @NotNull String imported) {
        List<String> single = List.of(imported);
        for (CqrsReferenceElement ref : PsiTreeUtil.findChildrenOfType(block, CqrsReferenceElement.class)) {
            String name = ref.getReferencedName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            for (CqrsNamedElement decl : CqrsResolveUtil.resolve(ref, name)) {
                if (CqrsResolveUtil.coveredByImport(CqrsResolveUtil.getQualifiedName(decl), single)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static String importedNameOf(@NotNull CqrsImportDecl imp) {
        CqrsImportFqn fqn = PsiTreeUtil.getChildOfType(imp, CqrsImportFqn.class);
        return fqn == null ? null : CqrsNames.unescapeQualified(fqn.getText().replaceAll("\\s+", ""));
    }

    // --- process manager ------------------------------------------------------------------------

    /** Reports duplicate state names within a process manager. */
    private void checkProcessManager(@NotNull CqrsProcessManager pm, @NotNull AnnotationHolder holder) {
        Set<String> seen = new HashSet<>();
        for (CqrsProcessState state : pm.getProcessStateList()) {
            String name = state.getId().getText();
            if (!seen.add(name)) {
                error(holder, state, "Duplicate process state '" + name + "'");
            }
        }
    }

    /** Reports a process-manager 'cron-schedule' that is not a valid Spring cron expression. */
    private void checkProcessManagerCronSchedule(@NotNull CqrsProcessManager pm, @NotNull AnnotationHolder holder) {
        checkCronSchedule(pm.getString(), holder);
    }

    // --- view -----------------------------------------------------------------------------------

    /** Reports a view 'cron-schedule' that is not a valid Spring cron expression. */
    private void checkViewCronSchedule(@NotNull CqrsViewDef view, @NotNull AnnotationHolder holder) {
        // 'view' has two STRING tokens (rest-path, cron-schedule), so there is no named accessor;
        // reach the cron STRING by scanning for the token after the 'cron-schedule' keyword.
        checkCronSchedule(firstTokenAfter(view, CqrsTypes.KW_CRON_SCHEDULE, CqrsTypes.STRING), holder);
    }

    private void checkCronSchedule(@Nullable PsiElement cronToken, @NotNull AnnotationHolder holder) {
        if (cronToken == null) {
            return;
        }
        String value = stringValue(cronToken);
        if (value != null && !SpringCronExpression.isValid(value)) {
            error(holder, cronToken, "Invalid Spring cron expression: '" + value + "'");
        }
    }

    /**
     * Reports a 'correlate-by' key, and an 'addressed-by' addressee, that is not an attribute of the
     * reacted event.
     */
    private void checkProcessReaction(@NotNull CqrsProcessReaction reaction, @NotNull AnnotationHolder holder) {
        PsiElement key = reaction.getId(); // the 'correlate-by' identifier (optional)
        List<PsiElement> addressees = new ArrayList<>();
        for (CqrsIssuedCommand issued : reaction.getIssuedCommandList()) {
            if (issued.getId() != null) {
                addressees.add(issued.getId());
            }
        }
        if (key == null && addressees.isEmpty()) {
            return;
        }
        List<CqrsTypeRef> refs = reaction.getTypeRefList();
        if (refs.isEmpty()) {
            return;
        }
        CqrsNamedElement target = resolve(refs.get(0)); // the 'reacts-to' event
        if (!(target instanceof CqrsEventDef)) {
            return; // unresolved or not an event: be lenient and skip the check
        }
        CqrsEventDef event = (CqrsEventDef) target;
        List<String> vars = new ArrayList<>(attributeNames(event.getAttributeList()));
        CqrsTypeRef origin = event.getTypeRef(); // 'copies-attributes-of'
        if (origin != null) {
            CqrsNamedElement originTarget = resolve(origin);
            if (originTarget == null) {
                return; // origin present but unresolved: be lenient
            }
            vars.addAll(parameterNames(originTarget));
        }
        if (key != null && !vars.contains(key.getText())) {
            error(holder, key,
                    "The correlation key '" + key.getText() + "' is not an attribute of event '" + event.getId().getText() + "'");
        }
        for (PsiElement addressee : addressees) {
            if (!vars.contains(addressee.getText())) {
                error(holder, addressee,
                        "The addressee '" + addressee.getText() + "' is not an attribute of event '" + event.getId().getText() + "'");
            }
        }
    }

    // --- value object 'base' --------------------------------------------------------------------

    private void checkValueObjectBase(@NotNull CqrsValueObject vo, @NotNull AnnotationHolder holder) {
        CqrsTypeRef base = vo.getTypeRef();
        if (base == null) {
            return;
        }
        for (CqrsConstructorDef constructor : vo.getConstructorDefList()) {
            error(holder, nameRange(constructor), "A value object with a 'base' is not allowed to have constructors");
        }
        for (CqrsMethodDef method : vo.getMethodDefList()) {
            error(holder, nameRange(method), "A value object with a 'base' is not allowed to have methods");
        }
    }

    /**
     * Reports a 'base' whose attributes cannot produce compiling code.
     *
     * <p>The port of the Xtext validator's {@code checkBaseTypeAttributes}. A base the generator builds
     * a value from - UUID, a number, a decimal - is generated as a wrapper around exactly one declared
     * attribute, because the generated valueOf() and the converters call a one-argument constructor
     * written from the attributes. With none there is no constructor at all, with several none of that
     * shape. An entity id on Integer or UUID and an aggregate id on UUID are emitted whole from the
     * base type alone, and a String base is never instantiated by generated code, so both are exempt.
     */
    private void checkBaseTypeAttributes(@NotNull PsiElement element, @NotNull List<CqrsAttribute> attributes,
            @NotNull String kind, @NotNull AnnotationHolder holder) {
        CqrsTypeRef base = firstTypeRefAfter(element, CqrsTypes.KW_BASE);
        if (base == null) {
            return;
        }
        String baseName = simpleName(base);
        if (!CONSTRUCTED_FROM_BASE.contains(baseName) || generatedFromBaseAlone(kind, baseName)) {
            return;
        }
        int count = attributes.size();
        if (count != 1) {
            error(holder, base, "A '" + kind + "' with 'base " + baseName
                    + "' is built from a single value, so it must declare exactly one attribute holding it, but has "
                    + (count == 0 ? "none" : count));
        }
    }

    private static boolean generatedFromBaseAlone(@NotNull String kind, @NotNull String baseName) {
        if ("entity-id".equals(kind)) {
            return "Integer".equals(baseName) || "UUID".equals(baseName);
        }
        if ("aggregate-id".equals(kind)) {
            return "UUID".equals(baseName);
        }
        return false;
    }

    // --- variable naming ------------------------------------------------------------------------

    private void checkVariableNameLowerCase(@NotNull CqrsNamedElement variable, @NotNull AnnotationHolder holder) {
        String name = variable.getName();
        PsiElement id = variable.getNameIdentifier();
        if (name != null && !name.isEmpty() && id != null && !Character.isLowerCase(name.charAt(0))) {
            holder.newAnnotation(HighlightSeverity.WARNING, "Variable names should start with a lower case")
                    .range(id)
                    .create();
        }
    }

    // --- message variables ----------------------------------------------------------------------

    private void checkExceptionMessage(@NotNull CqrsExceptionDef exception, @NotNull AnnotationHolder holder) {
        PsiElement message = exception.getString();
        if (message == null) {
            return;
        }
        String unknown = findUnknownVar(attributeNames(exception.getAttributeList()), stringValue(message));
        if (unknown != null) {
            error(holder, message, "A variable '" + unknown + "' is not defined in the exception");
        }
    }

    private void checkEventMessage(@NotNull CqrsEventDef event, @NotNull AnnotationHolder holder) {
        PsiElement message = event.getString();
        if (message == null) {
            return;
        }
        List<String> vars = new ArrayList<>(attributeNames(event.getAttributeList()));
        CqrsTypeRef origin = event.getTypeRef(); // 'copies-attributes-of'
        if (origin != null) {
            CqrsNamedElement target = resolve(origin);
            if (target == null) {
                return; // origin present but unresolved: be lenient and skip the check
            }
            vars.addAll(parameterNames(target));
        }
        String unknown = findUnknownVar(vars, stringValue(message));
        if (unknown != null) {
            error(holder, message, "A variable with the name '" + unknown + "' is unknown");
        }
    }

    // --- consistency ----------------------------------------------------------------------------

    private void checkConsistency(@NotNull CqrsConsistency consistency, @NotNull AnnotationHolder holder) {
        CqrsConsistencyLevel level = consistency.getConsistencyLevel();
        if (level == null) {
            return;
        }
        String value = level.getText();
        CqrsWeakConsistency details = consistency.getWeakConsistency();
        if ("weak".equals(value)) {
            if (details == null) {
                error(holder, consistency, "You must define the details for weak consistency");
            } else if (details.getWcAcceptable() == null || details.getWcDetection() == null
                    || details.getWcResolution() == null) {
                // The grammar accepts a partial block so that completion keeps working while it is
                // being typed; completeness is a semantic rule, checked here.
                error(holder, details,
                        "Weak consistency requires 'acceptable', 'detection' and 'resolution'");
            }
        } else if ("strong".equals(value) && details != null) {
            error(holder, details, "No details required for strong consistency");
        }
    }

    // --- aggregate / entity structure -----------------------------------------------------------

    private void checkAggregate(@NotNull CqrsAggregateDef aggregate, @NotNull AnnotationHolder holder) {
        List<CqrsAggregateId> ids = new ArrayList<>();
        for (CqrsEntityElement element : aggregate.getEntityElementList()) {
            if (element.getAggregateId() != null) {
                ids.add(element.getAggregateId());
            } else if (!isAllowedInAggregate(element)) {
                error(holder, concrete(element), "Allowed elements in an aggregate are: 'aggregate-id', 'entity',"
                        + " 'event', 'command' and 'value-object'");
            }
        }
        for (int i = 1; i < ids.size(); i++) {
            error(holder, nameRange(ids.get(i)), "Only one 'aggregate-id' can be defined inside an aggregate");
        }

        boolean hasIdentifier = aggregate.getTypeRef() != null; // 'identifier' reference
        boolean hasNestedId = !ids.isEmpty();
        if (!hasIdentifier && !hasNestedId) {
            error(holder, nameRange(aggregate), "Aggregate does not define an ID");
        } else if (hasIdentifier && hasNestedId) {
            error(holder, nameRange(aggregate),
                    "Aggregate cannot reference an ID using 'identifier' and define another inside the aggregate");
        }
    }

    private static boolean isAllowedInAggregate(@NotNull CqrsEntityElement element) {
        return element.getAggregateId() != null
                || element.getEntityDef() != null
                || element.getEventDef() != null
                || element.getCommandDef() != null
                || element.getValueObject() != null;
    }

    private void checkEntity(@NotNull CqrsEntityDef entity, @NotNull AnnotationHolder holder) {
        List<CqrsEntityId> ids = new ArrayList<>();
        for (CqrsEntityElement element : entity.getEntityElementList()) {
            if (element.getEntityId() != null) {
                ids.add(element.getEntityId());
            } else if (!isAllowedInEntity(element)) {
                error(holder, concrete(element),
                        "Allowed elements in an entity are: 'entity-id', 'event' and 'value-object'");
            }
        }
        for (int i = 1; i < ids.size(); i++) {
            error(holder, nameRange(ids.get(i)), "Only one 'entity-id' can be defined inside an entity");
        }

        boolean hasIdentifier = firstTypeRefAfter(entity, CqrsTypes.KW_IDENTIFIER) != null;
        boolean hasNestedId = !ids.isEmpty();
        if (!hasIdentifier && !hasNestedId) {
            error(holder, nameRange(entity), "Entity does not define an ID");
        } else if (hasIdentifier && hasNestedId) {
            error(holder, nameRange(entity),
                    "Entity cannot reference an ID using 'identifier' and define another inside the aggregate");
        }
    }

    private static boolean isAllowedInEntity(@NotNull CqrsEntityElement element) {
        return element.getEntityId() != null
                || element.getEventDef() != null
                || element.getValueObject() != null;
    }

    // --- service methods ------------------------------------------------------------------------

    private void checkNoEventsInServiceMethod(@NotNull CqrsMethodDef method, @NotNull AnnotationHolder holder) {
        if (PsiTreeUtil.getParentOfType(method, CqrsServiceDef.class) == null) {
            return;
        }
        for (CqrsTypeRef fired : typeRefsAfter(method, CqrsTypes.KW_FIRES)) {
            error(holder, fired, "A service method cannot fire events");
        }
        for (CqrsEventDef declared : method.getEventDefList()) {
            error(holder, nameRange(declared), "A service method cannot declare events");
        }
    }

    // --- annotation instance argument count -----------------------------------------------------

    private void checkAnnotationInstanceArgs(@NotNull CqrsAnnotationInstance instance, @NotNull AnnotationHolder holder) {
        CqrsNamedElement declaration = resolve(instance.getTypeRef());
        if (!(declaration instanceof CqrsAnnotationDef)) {
            return; // unresolved, or not an annotation: skip
        }
        int required = ((CqrsAnnotationDef) declaration).getAttributeList().size();
        int actual = instance.getLiteralList().size();
        if (required != actual) {
            error(holder, instance, "The number of arguments does not match the number required by the type: "
                    + ((CqrsAnnotationDef) declaration).getName());
        }
    }

    // --- generic argument count -----------------------------------------------------------------

    private void checkGenericArgs(@Nullable CqrsTypeRef typeRef, @Nullable CqrsGenericArgs generics,
                                  @NotNull AnnotationHolder holder) {
        CqrsNamedElement declaration = resolve(typeRef);
        if (!(declaration instanceof CqrsExternalType)) {
            return; // generics count only applies to external types
        }
        int required = parseIntOrZero(((CqrsExternalType) declaration).getNumber());
        int actual = generics == null ? 0 : generics.getTypeRefList().size();
        if (required != actual) {
            PsiElement anchor = generics != null ? generics : typeRef;
            error(holder, anchor, "The number of arguments does not match the number required by the type: " + required);
        }
    }

    // --- constraint target-type (attribute invariants / parameter preconditions) ----------------

    private void checkConstraintInstancesTargetType(@Nullable CqrsInvariants invariants, @Nullable CqrsTypeRef targetType,
                                                    @NotNull String prefix, @NotNull String suffix,
                                                    @NotNull AnnotationHolder holder) {
        checkConstraintInstancesTargetType(invariants == null ? null : invariants.getConstraintInstanceList(),
                targetType, prefix, suffix, holder);
    }

    private void checkConstraintInstancesTargetType(@Nullable List<CqrsConstraintInstance> instances,
                                                    @Nullable CqrsTypeRef targetType, @NotNull String prefix,
                                                    @NotNull String suffix, @NotNull AnnotationHolder holder) {
        if (instances == null || targetType == null) {
            return;
        }
        for (CqrsConstraintInstance instance : instances) {
            CqrsNamedElement declaration = resolve(instance.getTypeRef());
            if (!(declaration instanceof CqrsConstraintDef)) {
                continue; // unresolved constraint: skip
            }
            List<CqrsTypeRef> inputs = typeRefsAfter(declaration, CqrsTypes.KW_INPUT);
            if (inputs.isEmpty()) {
                continue; // a constraint without 'input' applies to any type
            }
            boolean matches = false;
            for (CqrsTypeRef input : inputs) {
                if (sameType(input, targetType)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                error(holder, instance, prefix + " (" + typeNames(inputs) + ") " + suffix);
            }
        }
    }

    // --- entity id path -------------------------------------------------------------------------

    /**
     * Reports a declared path that no real identifier path could have.
     *
     * <p>The port of the Xtext validator's {@code checkEntityIdPathShape}, so the editor refuses what the
     * build refuses. A path begins at an aggregate root and names the chain of children down to the thing
     * it addresses: {@code ANNUAL_TRANSACTIONS 2026-a/TRANSACTION 45}.</p>
     *
     * <p>An aggregate reached <em>inside</em> a composite identifier is not a step:
     * {@code AnnualTransactionsId} is made of an account and a year, and the path still begins at
     * {@code ANNUAL_TRANSACTIONS}. Writing the account as a first step is the mistake this catches most
     * often, because the model's own prose calls the pair "the natural composite key (account, year)".</p>
     */
    private void checkEntityIdPathShape(@NotNull CqrsEntityIdPath path, @NotNull AnnotationHolder holder) {
        final List<CqrsPathSegment> segments = path.getPathSegmentList();
        if (segments.isEmpty()) {
            return;
        }
        final PsiElement nameRange = path.getNameIdentifier() != null ? path.getNameIdentifier() : path;
        if (segments.size() < 2) {
            final CqrsNamedElement only = resolve(segments.get(0).getTypeRef());
            error(holder, nameRange, "A path of one step is the identifier itself - use '"
                    + (only == null ? "the identifier type" : only.getName()) + "' rather than a path");
            return;
        }

        final CqrsNamedElement first = resolve(segments.get(0).getTypeRef());
        if (first != null && !(first instanceof CqrsAggregateId)) {
            error(holder, segments.get(0), "A path starts at an aggregate root, and '" + first.getName()
                    + "' does not identify one");
            return;
        }
        final CqrsNamedElement root = first == null ? null
                : resolve(firstTypeRefAfter(first, CqrsTypes.KW_IDENTIFIES));

        for (final CqrsPathSegment segment : segments.subList(1, segments.size())) {
            final CqrsNamedElement type = resolve(segment.getTypeRef());
            if (type == null) {
                continue;
            }
            if (!(type instanceof CqrsEntityId)) {
                error(holder, segment, "Only the first step is an aggregate; '" + type.getName()
                        + "' has to identify an entity of '" + (root == null ? "the root" : root.getName()) + "'");
                continue;
            }
            if (root == null) {
                continue;
            }
            final CqrsNamedElement entity = resolve(firstTypeRefAfter(type, CqrsTypes.KW_IDENTIFIES));
            if (!(entity instanceof CqrsEntityDef)) {
                continue;
            }
            final CqrsNamedElement entityRoot = resolve(firstTypeRefAfter(entity, CqrsTypes.KW_ROOT));
            if (entityRoot != null && entityRoot != root) {
                error(holder, segment, "'" + entity.getName() + "' belongs to '" + entityRoot.getName()
                        + "', not to '" + root.getName() + "', so it cannot be a step of this path");
            }
        }
    }

    /**
     * Reports an 'own-path' the model cannot give a type to.
     *
     * <p>The port of the Xtext validator's {@code checkOwnPath}. It addresses the carrier from its root,
     * so it needs a carrier that has one: an aggregate is addressed by its own id and has nothing to
     * prepend, and a constructor has no carrier at all. It also needs an 'entity-id-path' declared for
     * the chain it would read as - without one there is no type to hand over, and falling back to an
     * anonymous path would put back the untyped reference the declaration exists to remove.</p>
     */
    private void checkOwnPath(@NotNull CqrsEntityPathArgument argument, @NotNull AnnotationHolder holder) {
        if (PsiTreeUtil.getParentOfType(argument, CqrsConstructorDef.class) != null) {
            error(holder, argument, "'own-path' has nothing to read in a constructor, which is what creates the carrier");
            return;
        }
        final CqrsEntityDef entity = PsiTreeUtil.getParentOfType(argument, CqrsEntityDef.class);
        if (entity == null) {
            error(holder, argument, "'own-path' addresses an entity from its root; an aggregate is addressed by 'own-id'");
            return;
        }
        final CqrsNamedElement root = resolve(firstTypeRefAfter(entity, CqrsTypes.KW_ROOT));
        if (root == null) {
            return;
        }
        if (pathTypeFor(entity, root) == null) {
            error(holder, argument, "No 'entity-id-path' addresses a '" + entity.getName() + "' inside '"
                    + root.getName() + "', so 'own-path' has no type to read as; declare one");
        }
    }

    /**
     * The declared path addressing the given entity from its root, or {@code null} if the project declares
     * none. Matched on the chain rather than on a name, the same way the Xtext side matches it.
     *
     * <p>Project-wide rather than per file: a module may be declared in more than one file, and the path
     * is commonly declared in the half the entity is not in.</p>
     */
    private @Nullable CqrsEntityIdPath pathTypeFor(@NotNull CqrsEntityDef entity, @NotNull CqrsNamedElement root) {
        for (final CqrsEntityIdPath path : declaredPaths(entity)) {
            final List<CqrsPathSegment> segments = path.getPathSegmentList();
            if (segments.size() != 2) {
                continue;
            }
            final CqrsNamedElement first = resolve(segments.get(0).getTypeRef());
            final CqrsNamedElement last = resolve(segments.get(1).getTypeRef());
            if (!(first instanceof CqrsAggregateId) || !(last instanceof CqrsEntityId)) {
                continue;
            }
            if (resolve(firstTypeRefAfter(first, CqrsTypes.KW_IDENTIFIES)) == root
                    && resolve(firstTypeRefAfter(last, CqrsTypes.KW_IDENTIFIES)) == entity) {
                return path;
            }
        }
        return null;
    }

    /** Every path declaration in the project, or just this file's while the index is still building. */
    private static Collection<CqrsEntityIdPath> declaredPaths(@NotNull CqrsEntityDef entity) {
        final Project project = entity.getProject();
        final Collection<VirtualFile> files;
        try {
            files = FileTypeIndex.getFiles(CqrsFileType.INSTANCE, GlobalSearchScope.projectScope(project));
        } catch (IndexNotReadyException notReady) {
            return PsiTreeUtil.findChildrenOfType(entity.getContainingFile(), CqrsEntityIdPath.class);
        }
        final List<CqrsEntityIdPath> paths = new ArrayList<>();
        final PsiManager psiManager = PsiManager.getInstance(project);
        for (final VirtualFile vf : files) {
            final PsiFile psiFile = psiManager.findFile(vf);
            if (psiFile instanceof CqrsFile) {
                paths.addAll(PsiTreeUtil.findChildrenOfType(psiFile, CqrsEntityIdPath.class));
            }
        }
        return paths;
    }

    /**
     * Reports a step whose range can match nothing.
     *
     * <p>Written out rather than marked with a symbol, because "may repeat" does not say whether the step
     * may also be absent - so the bounds are stated, and stated bounds can contradict each other. Caught
     * where they are written rather than at runtime, where an impossible range silently rejects every
     * path it is given.</p>
     */
    private void checkSegmentRange(@NotNull CqrsSegmentRange range, @NotNull AnnotationHolder holder) {
        final List<Integer> bounds = new ArrayList<>();
        for (PsiElement child = range.getFirstChild(); child != null; child = child.getNextSibling()) {
            final IElementType type = child.getNode().getElementType();
            if (type == CqrsTypes.STAR) {
                return; // unbounded, so only the lower bound is written and it cannot contradict
            }
            if (type == CqrsTypes.NUMBER) {
                try {
                    bounds.add(Integer.valueOf(child.getText()));
                } catch (final NumberFormatException ex) {
                    return; // not a whole number, which the parser will already have complained about
                }
            }
        }
        if (bounds.size() < 2) {
            return;
        }
        final int min = bounds.get(0);
        final int max = bounds.get(1);
        if (max < 1) {
            error(holder, range,
                    "A step that accepts no identifier at all cannot be part of a path; leave it out instead");
        } else if (max < min) {
            error(holder, range, "The range is empty: at least " + min + " but at most " + max);
        }
    }

    // --- row gates ------------------------------------------------------------------------------

    /**
     * Reports a row that offers a command whose client-answerable gates it cannot answer.
     *
     * <p>The port of the Xtext validator's {@code checkRowAnswersTheGatesItOffers}, so the editor says
     * what the build says. A menu is drawn on a row, and a command gated by a rule over the aggregate's
     * own state can be left out of it rather than offered and refused - which the client decides from
     * what the row publishes. A row that offers the command and omits what the gate reads makes the gate
     * work on one screen and quietly do nothing on another.</p>
     *
     * <p><b>A warning rather than an error, deliberately.</b> Whether a row publishes what a rule reads
     * is a modelling decision with real costs on the other side - a count is what a person wants to read
     * where a collection is what the rule asks. What this removes is the silence, not the choice.</p>
     */
    private void checkRowAnswersTheGatesItOffers(@NotNull CqrsValueObject row, @NotNull AnnotationHolder holder) {
        List<CqrsRowGates.Unanswered> unanswered = CqrsRowGates.of(row);
        if (unanswered.isEmpty()) {
            return;
        }
        PsiElement range = row.getNameIdentifier() != null ? row.getNameIdentifier() : row;
        for (CqrsRowGates.Unanswered gate : unanswered) {
            holder.newAnnotation(HighlightSeverity.WARNING,
                    "'" + row.getName() + "' offers '" + gate.command() + "', which is gated by '"
                            + gate.rule() + "', but does not publish " + quoted(gate.missing())
                            + " - so a client cannot tell whether to offer the action and always will")
                    .range(range).create();
        }
    }

    /** Names as a reader would list them: 'a', 'b' and 'c'. */
    private static @NotNull String quoted(@NotNull List<String> names) {
        List<String> quoted = new ArrayList<>();
        for (String name : names) {
            quoted.add("'" + name + "'");
        }
        if (quoted.size() == 1) {
            return quoted.get(0);
        }
        return String.join(", ", quoted.subList(0, quoted.size() - 1)) + " and " + quoted.get(quoted.size() - 1);
    }

    // --- helper ---------------------------------------------------------------------------------

    private static void error(@NotNull AnnotationHolder holder, @NotNull PsiElement range, @NotNull String message) {
        holder.newAnnotation(HighlightSeverity.ERROR, message).range(range).create();
    }
}
