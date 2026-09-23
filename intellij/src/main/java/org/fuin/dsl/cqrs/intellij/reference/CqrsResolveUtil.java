package org.fuin.dsl.cqrs.intellij.reference;

import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.fuin.dsl.cqrs.intellij.CqrsFile;
import org.fuin.dsl.cqrs.intellij.CqrsFileType;
import org.fuin.dsl.cqrs.intellij.psi.CqrsAggregateDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsAggregateId;
import org.fuin.dsl.cqrs.intellij.psi.CqrsAnnotationDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsBusinessRule;
import org.fuin.dsl.cqrs.intellij.psi.CqrsKeyDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsCommandDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsCommandHandler;
import org.fuin.dsl.cqrs.intellij.psi.CqrsConstraintDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsConstructorDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsContextDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsDataProtectionDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEntityDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEntityId;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEventDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsExceptionDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsExternalType;
import org.fuin.dsl.cqrs.intellij.psi.CqrsImportDecl;
import org.fuin.dsl.cqrs.intellij.psi.CqrsImportFqn;
import org.fuin.dsl.cqrs.intellij.psi.CqrsMethodDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsNamedElement;
import org.fuin.dsl.cqrs.intellij.psi.CqrsModuleDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsNames;
import org.fuin.dsl.cqrs.intellij.psi.CqrsOperationContext;
import org.fuin.dsl.cqrs.intellij.psi.CqrsProcessState;
import org.fuin.dsl.cqrs.intellij.psi.CqrsProjectionDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsPsiUtil;
import org.fuin.dsl.cqrs.intellij.psi.CqrsServiceDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsTypes;
import org.fuin.dsl.cqrs.intellij.psi.CqrsViewDef;
import org.fuin.dsl.cqrs.intellij.remote.CqrsRemoteScopeResolver;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Name resolution for the CQRS DSL. Mirrors the Xtext scoping: a {@code module} is the unit of
 * visibility, so a simple name resolves against the module's own declarations first and then against
 * whatever the module - or its context - {@code import}s. Only that closest tier is returned, so a
 * name reused in a sibling module resolves to the local declaration instead of becoming ambiguous.
 * A name that is neither declared in the module nor imported does <em>not</em> resolve, so the
 * annotator reports it exactly like the Eclipse plugin and the SrcGen4J build do.
 * <p>
 * A {@code dependency} (see {@link CqrsRemoteScopeResolver}) only makes another project's models
 * resolvable; an import still decides which of their types are visible. A fully qualified reference
 * bypasses the imports entirely, matching the Xtext global scope.
 */
public final class CqrsResolveUtil {

    private CqrsResolveUtil() {
    }

    /** Fully qualified name of a declaration: the dotted chain of all enclosing named elements. */
    public static String getQualifiedName(CqrsNamedElement element) {
        Deque<String> parts = new ArrayDeque<>();
        for (PsiElement cur = element; cur != null; cur = cur.getParent()) {
            if (cur instanceof CqrsNamedElement) {
                String name = ((CqrsNamedElement) cur).getName();
                if (name != null && !name.isEmpty()) {
                    parts.addFirst(name);
                }
            }
        }
        return String.join(".", parts);
    }

    /**
     * Fully qualified name ({@code context.module}) of the module enclosing the given element,
     * or "" when there is none. The name includes the context, so it can be compared with the
     * qualified name of a declaration.
     */
    public static String enclosingModuleFqn(PsiElement element) {
        CqrsModuleDef ns = PsiTreeUtil.getParentOfType(element, CqrsModuleDef.class);
        return ns != null ? getQualifiedName(ns) : "";
    }

    /** Name of the context enclosing the element, or "". */
    public static String enclosingContextFqn(PsiElement element) {
        CqrsContextDef ctx = PsiTreeUtil.getParentOfType(element, CqrsContextDef.class);
        return ctx != null ? getQualifiedName(ctx) : "";
    }

    /** All named declarations across the project's {@code .cqrs} files. */
    public static List<CqrsNamedElement> allDeclarations(Project project) {
        List<CqrsNamedElement> result = new ArrayList<>();
        Collection<VirtualFile> files;
        try {
            files = FileTypeIndex.getFiles(CqrsFileType.INSTANCE, GlobalSearchScope.allScope(project));
        } catch (IndexNotReadyException notReady) {
            return result; // indexes are being built (dumb mode); resolve again later
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        for (VirtualFile vf : files) {
            PsiFile psiFile = psiManager.findFile(vf);
            if (psiFile instanceof CqrsFile) {
                result.addAll(PsiTreeUtil.findChildrenOfType(psiFile, CqrsNamedElement.class));
            }
        }
        return result;
    }

    /**
     * Everything that could possibly be addressed from {@code element}: project-local declarations,
     * those of the neighbourhood the file itself is read from when it is not one of the project's,
     * plus those of its dependencies. This is the pool a <em>fully qualified</em> reference resolves
     * against; it is deliberately not narrowed by the imports.
     */
    public static List<CqrsNamedElement> resolvableDeclarations(PsiElement element) {
        Project project = element.getProject();
        List<CqrsNamedElement> all = new ArrayList<>(allDeclarations(project));
        all.addAll(neighbourDeclarations(element));
        all.addAll(dependencyDeclarations(element));
        return dedupByLocation(all);
    }

    /**
     * Declarations addressable by their <em>simple</em> name at the given position: the ones of the
     * enclosing module, plus the ones an {@code import} of that module or of its context covers.
     * This is what makes completion offer only imported types.
     */
    public static List<CqrsNamedElement> visibleDeclarations(PsiElement element) {
        String currentModule = enclosingModuleFqn(element);
        List<String> imports = importedNames(element);
        List<CqrsNamedElement> result = new ArrayList<>();
        for (CqrsNamedElement decl : resolvableDeclarations(element)) {
            if (declaredIn(decl, currentModule) || coveredByImport(getQualifiedName(decl), imports)) {
                result.add(decl);
            }
        }
        return result;
    }

    /** The names imported by the enclosing module and by the enclosing context, in that order. */
    public static List<String> importedNames(PsiElement element) {
        List<String> result = new ArrayList<>();
        CqrsModuleDef module = PsiTreeUtil.getParentOfType(element, CqrsModuleDef.class);
        if (module != null) {
            collectImports(module, result);
        }
        CqrsContextDef context = PsiTreeUtil.getParentOfType(element, CqrsContextDef.class);
        if (context != null) {
            // Only the context's own imports - not those of the modules nested inside it.
            for (CqrsImportDecl imp : PsiTreeUtil.getChildrenOfTypeAsList(context, CqrsImportDecl.class)) {
                String text = importedName(imp);
                if (text != null) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    private static void collectImports(PsiElement block, List<String> target) {
        for (CqrsImportDecl imp : PsiTreeUtil.getChildrenOfTypeAsList(block, CqrsImportDecl.class)) {
            String text = importedName(imp);
            if (text != null) {
                target.add(text);
            }
        }
    }

    /** The dotted name an {@code import} declares, without the keyword and without whitespace. */
    @Nullable
    private static String importedName(CqrsImportDecl imp) {
        CqrsImportFqn fqn = PsiTreeUtil.getChildOfType(imp, CqrsImportFqn.class);
        if (fqn == null) {
            return null;
        }
        String text = fqn.getText();
        return text == null ? null : CqrsNames.unescapeQualified(text.replaceAll("\\s+", ""));
    }

    /**
     * Whether a declaration belongs to the given module, compared by the module's qualified name
     * rather than by containment: a module may be split across several files and still sees all of
     * its own elements. Taking the <em>enclosing</em> module of the declaration also covers an
     * element nested deeper than the module (an {@code event} declared inside a method), and keeps a
     * module named {@code a.b} apart from one named {@code a.b.c}.
     */
    private static boolean declaredIn(CqrsNamedElement decl, String moduleFqn) {
        return !moduleFqn.isEmpty() && moduleFqn.equals(enclosingModuleFqn(decl));
    }

    /**
     * Whether an imported name covers a qualified name: an exact match, or - for a wildcard - any
     * name below its prefix. A single wildcard therefore reaches a whole context as well as a single
     * module, which is what {@code ctx.*} and {@code ctx.mod.*} mean.
     */
    public static boolean coveredByImport(String fqn, List<String> imports) {
        for (String imported : imports) {
            if (imported.endsWith(".*")) {
                if (fqn.startsWith(imported.substring(0, imported.length() - 1))) {
                    return true;
                }
            } else if (imported.equals(fqn)) {
                return true;
            }
        }
        return false;
    }

    /** Declarations a declared {@code dependency} provides. */
    private static List<CqrsNamedElement> dependencyDeclarations(PsiElement element) {
        return CqrsRemoteScopeResolver.getInstance(element.getProject())
                .remoteDeclarations(element.getContainingFile());
    }

    /**
     * Declarations of the neighbourhood a file that is only read lies in - the other entries of its
     * archive, or the other models of its directory. Empty for a file of this project, which the
     * index answers for.
     */
    private static List<CqrsNamedElement> neighbourDeclarations(PsiElement element) {
        return CqrsRemoteScopeResolver.getInstance(element.getProject())
                .neighbourDeclarations(element.getContainingFile());
    }

    /**
     * The declarations that may be referenced at the given position - the visible ones, narrowed to
     * what the grammar allows there.
     *
     * @param element Position the reference is written at.
     *
     * @return Visible declarations of the expected kind.
     */
    public static List<CqrsNamedElement> referenceableDeclarations(PsiElement element) {
        Predicate<CqrsNamedElement> expected = expectedDeclaration(element);
        List<CqrsNamedElement> result = new ArrayList<>();
        for (CqrsNamedElement decl : visibleDeclarations(element)) {
            if (isReferenceableKind(decl) && (expected == null || expected.test(decl))) {
                result.add(decl);
            }
        }
        return result;
    }

    /**
     * Whether a declaration is something a cross reference may point at. A {@code context} and a
     * {@code module} are containers, never targets - no rule of the grammar references one - but they
     * are named elements like any other, so a wildcard import covering their qualified name would
     * otherwise offer them as if they were types.
     */
    private static boolean isReferenceableKind(CqrsNamedElement decl) {
        return !(decl instanceof CqrsContextDef) && !(decl instanceof CqrsModuleDef);
    }

    /**
     * Returns what kind of declaration a reference at the given position may point at, or
     * {@code null} when any type is allowed.
     * <p>
     * Almost every cross-reference in the grammar is typed - {@code fires} names an event, a view's
     * {@code uses} a projection, {@code identifies} an entity - but the IntelliJ grammar parses all of
     * them as a plain {@code type_ref}, so the kind has to be recovered from the syntactic position.
     * The keyword that introduces the clause is what identifies it; a few keywords serve two clauses
     * with different types and are told apart by the declaration they sit in.
     * <p>
     * A position not listed here allows any type - that is the honest answer for {@code returns},
     * {@code instance-key}, an attribute or parameter type, a generic argument and a constraint's
     * {@code input}, all of which are declared {@code [Type|FQN]}.
     *
     * @param element Position the reference is written at.
     *
     * @return Predicate the declaration has to satisfy, or {@code null} for "any type".
     */
    @Nullable
    public static Predicate<CqrsNamedElement> expectedDeclaration(PsiElement element) {
        PsiElement keyword = introducingKeyword(element);
        if (keyword == null) {
            return null;
        }
        IElementType type = keyword.getNode().getElementType();

        if (type == CqrsTypes.KW_OPERATION_CONTEXT) {
            return kind(CqrsServiceDef.class);
        }
        if (type == CqrsTypes.KW_PROTECTED_BY) {
            return kind(CqrsDataProtectionDef.class);
        }
        if (type == CqrsTypes.KW_BASE) {
            return kind(CqrsExternalType.class);
        }
        if (type == CqrsTypes.KW_ROOT) {
            return kind(CqrsAggregateDef.class);
        }
        if (type == CqrsTypes.KW_FIRES || type == CqrsTypes.KW_REACTS_TO) {
            return kind(CqrsEventDef.class);
        }
        if (type == CqrsTypes.KW_REF) {
            return kind(CqrsMethodDef.class);
        }
        if (type == CqrsTypes.KW_INVARIANTS || type == CqrsTypes.KW_PRECONDITIONS) {
            return kind(CqrsConstraintDef.class);
        }
        // A business key derives a uniqueness rule, so an operation names one exactly where it names
        // a rule. Two PSI classes rather than one, as for 'target' below.
        if (type == CqrsTypes.KW_BUSINESS_RULES) {
            return decl -> decl instanceof CqrsBusinessRule || decl instanceof CqrsKeyDef;
        }
        if (type == CqrsTypes.AT) {
            return kind(CqrsAnnotationDef.class);
        }
        if (type == CqrsTypes.KW_HANDLES || type == CqrsTypes.KW_ISSUES_COMMANDS) {
            return kind(CqrsCommandDef.class);
        }
        if (type == CqrsTypes.KW_IN_STATE || type == CqrsTypes.KW_TRANSITION_TO) {
            return kind(CqrsProcessState.class);
        }
        // An operation: a constructor or a method, so two PSI classes rather than one.
        if (type == CqrsTypes.KW_COPIES_ATTRIBUTES_OF || type == CqrsTypes.KW_TARGET) {
            return decl -> decl instanceof CqrsConstructorDef || decl instanceof CqrsMethodDef;
        }
        // 'identifies' points the id at what it identifies, 'identifier' the other way round - which
        // one, and whether an entity or an aggregate, follows from the declaration being written.
        if (type == CqrsTypes.KW_IDENTIFIES) {
            if (PsiTreeUtil.getParentOfType(element, CqrsEntityId.class) != null) {
                return kind(CqrsEntityDef.class);
            }
            if (PsiTreeUtil.getParentOfType(element, CqrsAggregateId.class) != null) {
                return kind(CqrsAggregateDef.class);
            }
            return null;
        }
        if (type == CqrsTypes.KW_IDENTIFIER) {
            if (PsiTreeUtil.getParentOfType(element, CqrsEntityDef.class) != null) {
                return kind(CqrsEntityId.class);
            }
            if (PsiTreeUtil.getParentOfType(element, CqrsAggregateDef.class) != null) {
                return kind(CqrsAggregateId.class);
            }
            return null;
        }
        // A view 'uses' its projection, a command handler the aggregates it works on.
        if (type == CqrsTypes.KW_USES) {
            if (PsiTreeUtil.getParentOfType(element, CqrsViewDef.class) != null) {
                return kind(CqrsProjectionDef.class);
            }
            if (PsiTreeUtil.getParentOfType(element, CqrsCommandHandler.class) != null) {
                return kind(CqrsAggregateDef.class);
            }
            return null;
        }
        // A projection's 'input' are events; a constraint's 'input' is any type.
        if (type == CqrsTypes.KW_INPUT) {
            return PsiTreeUtil.getParentOfType(element, CqrsProjectionDef.class) != null
                    ? kind(CqrsEventDef.class) : null;
        }
        // 'exception' either references one (on a constraint or a business rule) or introduces the
        // declaration of one - and the name of a new declaration is not a reference to anything.
        if (type == CqrsTypes.KW_EXCEPTION) {
            return PsiTreeUtil.getParentOfType(element, CqrsExceptionDef.class) != null
                    ? NONE : kind(CqrsExceptionDef.class);
        }
        return null;
    }

    /** Matches nothing: the position takes the name of a new declaration, not a reference. */
    private static final Predicate<CqrsNamedElement> NONE = decl -> false;

    private static Predicate<CqrsNamedElement> kind(Class<? extends CqrsNamedElement> expected) {
        return expected::isInstance;
    }

    /**
     * The keyword introducing the reference clause the given position belongs to, or {@code null}.
     * <p>
     * Several clauses take a comma-separated list ({@code fires A, B}), so the search walks back over
     * whole references and the commas between them; it stops at anything else. Starting from the token
     * before the position rather than from the PSI node keeps it working while the reference is still
     * being typed, when it is not yet part of its clause.
     *
     * @param element Position to look back from.
     *
     * @return Introducing token, or {@code null} at the start of the file.
     */
    @Nullable
    private static PsiElement introducingKeyword(PsiElement element) {
        PsiElement cur = PsiTreeUtil.prevVisibleLeaf(element);
        while (cur != null && cur.getNode().getElementType() == CqrsTypes.COMMA) {
            cur = PsiTreeUtil.prevVisibleLeaf(cur);
            // Walk over the (possibly qualified) reference that comma separated from this one.
            while (cur != null && (cur.getNode().getElementType() == CqrsTypes.ID
                    || cur.getNode().getElementType() == CqrsTypes.DOT)) {
                cur = PsiTreeUtil.prevVisibleLeaf(cur);
            }
            // 'issues-commands A addressed-by x, B': what was walked over was the addressee, not a
            // reference - step over 'addressed-by' and the command it addresses as well.
            if (cur != null && cur.getNode().getElementType() == CqrsTypes.KW_ADDRESSED_BY) {
                cur = PsiTreeUtil.prevVisibleLeaf(cur);
                while (cur != null && (cur.getNode().getElementType() == CqrsTypes.ID
                        || cur.getNode().getElementType() == CqrsTypes.DOT)) {
                    cur = PsiTreeUtil.prevVisibleLeaf(cur);
                }
            }
        }
        return cur;
    }

    /**
     * Collapses declarations that denote the same physical source location. The project scope
     * ({@link #allDeclarations}) and the remote scope ({@link CqrsRemoteScopeResolver#remoteDeclarations})
     * overlap — a cached model is also indexed as a project file, and one artifact providing several
     * imported modules is served once per module — so the same declaration would otherwise be
     * offered several times and surface as a bogus "Multiple Implementations" choice. Keying on the
     * source location (file path + start offset) rather than the name keeps genuinely distinct,
     * same-named declarations across modules separate.
     */
    private static List<CqrsNamedElement> dedupByLocation(List<CqrsNamedElement> declarations) {
        List<CqrsNamedElement> result = new ArrayList<>(declarations.size());
        Set<String> seen = new LinkedHashSet<>();
        for (CqrsNamedElement decl : declarations) {
            if (seen.add(locationKey(decl))) {
                result.add(decl);
            }
        }
        return result;
    }

    /** Stable identity of a declaration's source location; falls back to object identity in-memory. */
    private static String locationKey(CqrsNamedElement decl) {
        PsiFile file = decl.getContainingFile();
        VirtualFile vf = file != null ? file.getVirtualFile() : null;
        String path = vf != null ? vf.getUrl() : "mem:" + System.identityHashCode(file);
        return path + "#" + decl.getTextRange().getStartOffset();
    }

    /** Resolve a (possibly qualified) reference name to its declaration(s). */
    public static List<CqrsNamedElement> resolve(PsiElement context, String referencedName) {
        if (referencedName == null || referencedName.isEmpty()) {
            return List.of();
        }
        List<CqrsNamedElement> candidates = resolvableDeclarations(context);
        String currentModule = enclosingModuleFqn(context);

        boolean qualified = referencedName.contains(".");
        List<CqrsNamedElement> result = new ArrayList<>();

        if (qualified) {
            // A fully qualified reference always resolves - it needs no import, exactly like the
            // Xtext global scope.
            for (CqrsNamedElement decl : candidates) {
                if (decl.getName() == null) {
                    continue;
                }
                String fqn = getQualifiedName(decl);
                if (fqn.equals(referencedName) || fqn.endsWith("." + referencedName)) {
                    result.add(decl);
                }
            }
            return ofExpectedKind(context, result);
        }

        // A simple name resolves against the closest scope that declares it: the enclosing module
        // first, then whatever that module or its context imports. Only the closest non-empty tier
        // is returned, so a name the module declares itself wins over an imported one instead of
        // turning into an ambiguous match. Anything neither declared here nor imported is invisible.
        List<String> imports = importedNames(context);
        List<List<CqrsNamedElement>> tiers = List.of(new ArrayList<>(), new ArrayList<>());
        for (CqrsNamedElement decl : candidates) {
            String simple = decl.getName();
            if (simple == null || !simple.equals(referencedName)) {
                continue;
            }
            if (declaredIn(decl, currentModule)) {
                tiers.get(0).add(decl);
            } else if (coveredByImport(getQualifiedName(decl), imports)) {
                tiers.get(1).add(decl);
            }
        }
        for (List<CqrsNamedElement> tier : tiers) {
            List<CqrsNamedElement> expected = ofExpectedKind(context, tier);
            if (!expected.isEmpty()) {
                return expected;
            }
        }
        return List.of();
    }

    /**
     * Keeps only the declarations the reference may point to, per {@link #expectedDeclaration}. A
     * command's {@code target} names the operation it triggers and a view's {@code uses} a projection,
     * so anything else is not a candidate: the reference is left unresolved and the annotator reports
     * it, exactly as the Xtext scoping and therefore the build do. Resolving it anyway would be more
     * convenient to navigate, but it would hide a model error until the build fails.
     *
     * @param context Position the reference is written at.
     * @param candidates Declarations matching by name.
     *
     * @return Those of them the grammar allows at that position.
     */
    private static List<CqrsNamedElement> ofExpectedKind(PsiElement context,
            List<CqrsNamedElement> candidates) {
        Predicate<CqrsNamedElement> expected = expectedDeclaration(context);
        List<CqrsNamedElement> result = new ArrayList<>();
        for (CqrsNamedElement decl : candidates) {
            if (isReferenceableKind(decl) && (expected == null || expected.test(decl))) {
                result.add(decl);
            }
        }
        return result;
    }

    private static String moduleOf(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? "" : fqn.substring(0, dot);
    }
}
