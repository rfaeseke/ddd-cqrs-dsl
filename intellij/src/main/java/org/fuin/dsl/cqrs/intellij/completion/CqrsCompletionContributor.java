package org.fuin.dsl.cqrs.intellij.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.fuin.dsl.cqrs.intellij.CqrsIcons;
import org.fuin.dsl.cqrs.intellij.CqrsLanguage;
import org.fuin.dsl.cqrs.intellij.psi.CqrsAggregateDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsAggregateId;
import org.fuin.dsl.cqrs.intellij.psi.CqrsAnnotationDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsBusinessRule;
import org.fuin.dsl.cqrs.intellij.psi.CqrsCommandDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsConstraintDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsConstructorDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsDataProtectionDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEntityDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEntityId;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEnumObject;
import org.fuin.dsl.cqrs.intellij.psi.CqrsEventDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsExceptionDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsMethodDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsNamedElement;
import org.fuin.dsl.cqrs.intellij.psi.CqrsNames;
import org.fuin.dsl.cqrs.intellij.psi.CqrsContextDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsModuleDef;
import org.fuin.dsl.cqrs.intellij.psi.CqrsProcessManager;
import org.fuin.dsl.cqrs.intellij.psi.CqrsProcessReaction;
import org.fuin.dsl.cqrs.intellij.psi.CqrsTokenTypes;
import org.fuin.dsl.cqrs.intellij.psi.CqrsTypes;
import org.fuin.dsl.cqrs.intellij.psi.CqrsValueObject;
import org.fuin.dsl.cqrs.intellij.psi.CqrsViewDef;
import org.fuin.dsl.cqrs.intellij.reference.CqrsResolveUtil;
import java.util.TreeSet;
import org.fuin.dsl.cqrs.intellij.psi.CqrsImportDecl;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Context-aware keyword completion. Declared-name completion is provided by the reference's
 * {@code getVariants()} (see {@link org.fuin.dsl.cqrs.intellij.reference.CqrsReference}); this
 * contributor adds the relevant DSL keywords for the block surrounding the caret.
 */
public final class CqrsCompletionContributor extends CompletionContributor {

    private static final List<String> ELEMENT_KEYWORDS = List.of(
            "type", "value-object", "entity-id", "aggregate-id", "enum", "entity", "aggregate",
            "exception", "event", "command", "command-handler", "projection", "view", "process-manager",
            "constraint", "annotation", "service", "data-protection");

    // ---- process-manager block: manager clauses and reaction clauses ---------------------
    private static final List<String> PM_BODY_KEYWORDS = List.of(
            "cron-schedule", "instance-key", "process-states", "reacts-to");
    private static final List<String> PM_REACTION_KEYWORDS = List.of(
            "correlate-by", "issues-commands", "addressed-by", "transition-to", "arm-timeout", "cancel-timeout");

    private static final List<String> META_KEYWORDS = List.of(
            "slabel", "label", "tooltip", "prompt", "examples");

    /** The {@code literal} rule's keyword alternatives; the others (NUMBER, STRING) are typed out. */
    private static final List<String> LITERAL_KEYWORDS = List.of("null", "true", "false");

    // ---- business-rule block: the consistency clause and its enum values ----------------
    private static final List<String> CONSISTENCY_LEVELS = List.of("weak", "strong");
    private static final List<String> INCONSISTENCY_DETECTIONS = List.of(
            "never", "manually", "automatic");
    private static final List<String> INCONSISTENCY_RESOLUTIONS = List.of(
            "never", "manually", "automatic", "workflow");

    // ---- data-protection block: clause keywords and their enum values --------------------
    private static final List<String> DP_CLAUSE_KEYWORDS = List.of(
            "protection", "category", "subject", "purpose", "lawful-basis", "retention");
    private static final List<String> PROTECTION_LEVELS = List.of("none", "personal", "sensitive");
    private static final List<String> LAWFUL_BASES = List.of(
            "consent", "explicit_consent", "contract", "legal_obligation",
            "vital_interests", "public_task", "legitimate_interests");
    private static final List<String> SPECIAL_CATEGORIES = List.of(
            "health", "genetic", "biometric", "racial", "political", "religious",
            "philosophical", "trade_union", "sex_life", "sexual_orientation");
    private static final List<String> ERASURE_STRATEGIES = List.of(
            "delete", "anonymize", "pseudonymize", "archive", "review");
    // A duration appears in three places: 'retention', 'arm-timeout' and 'acceptable'.
    private static final List<String> TIME_UNITS = List.of(
            "millis", "seconds", "minutes", "hours", "days", "weeks", "months", "years");
    private static final Set<IElementType> TIME_UNIT_TOKENS = Set.of(
            CqrsTypes.KW_MILLIS, CqrsTypes.KW_SECONDS, CqrsTypes.KW_MINUTES, CqrsTypes.KW_HOURS,
            CqrsTypes.KW_DAYS, CqrsTypes.KW_WEEKS, CqrsTypes.KW_MONTHS, CqrsTypes.KW_YEARS);

    public CqrsCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(CqrsLanguage.INSTANCE),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        // Nothing the DSL knows about may be proposed inside a string: a Maven
                        // coordinate, a 'local' directory or a label is free text, and offering
                        // keywords or type names there is only noise.
                        IElementType positionType = position.getNode().getElementType();
                        if (positionType == CqrsTypes.STRING
                                || positionType == CqrsTokenTypes.UNCLOSED_STRING) {
                            return;
                        }
                        // Inside an 'import' only the importable paths make sense - no keyword and
                        // no bare type name would be legal there.
                        if (addImportVariants(position, result)) {
                            return;
                        }
                        for (String keyword : keywordsFor(position)) {
                            result.addElement(LookupElementBuilder.create(keyword).bold());
                        }
                        // At the start of a new attribute or parameter the half-typed identifier is
                        // not yet parsed as a type_ref, so the reference's getVariants() never runs.
                        // Offer the visible type declarations here so types show up alongside keywords.
                        if (allowsTypeRef(position)) {
                            addTypeVariants(position, result);
                        }
                    }
                });
    }

    /**
     * Offers what may follow an {@code import}: every context and module wildcard, plus every single
     * type, drawn from everything the file can reach. The own module is left out - importing it would
     * be redundant - and so is anything already imported.
     *
     * @param position Position the caret sits at.
     * @param result Result set to add the proposals to.
     *
     * @return TRUE if the caret is inside an import, so nothing else should be offered.
     */
    private static boolean addImportVariants(PsiElement position, @NotNull CompletionResultSet result) {
        if (PsiTreeUtil.getParentOfType(position, CqrsImportDecl.class) == null
                && prevType(position) != CqrsTypes.KW_IMPORT) {
            return false;
        }
        String ownModule = CqrsResolveUtil.enclosingModuleFqn(position);
        List<String> already = CqrsResolveUtil.importedNames(position);
        Set<String> proposals = new TreeSet<>();
        for (CqrsNamedElement decl : CqrsResolveUtil.resolvableDeclarations(position)) {
            String fqn = CqrsResolveUtil.getQualifiedName(decl);
            if (fqn.isEmpty()) {
                continue;
            }
            if (decl instanceof CqrsContextDef || decl instanceof CqrsModuleDef) {
                if (!fqn.equals(ownModule)) {
                    proposals.add(fqn + ".*");
                }
            } else if (!CqrsResolveUtil.enclosingModuleFqn(decl).equals(ownModule)) {
                proposals.add(fqn);
            }
        }
        proposals.removeAll(already);
        for (String proposal : proposals) {
            result.addElement(LookupElementBuilder.create(proposal));
        }
        return true;
    }

    /** Element type of the token before the caret, or {@code null} if there is none. */
    private static IElementType prevType(PsiElement position) {
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        return prev == null ? null : prev.getNode().getElementType();
    }

    /** Whether a {@code type_ref} (attribute/parameter type) may legally begin at the caret. */
    private static boolean allowsTypeRef(PsiElement position) {
        IElementType prevType = prevType(position);
        if (prevType == CqrsTypes.KW_EXAMPLES || prevType == CqrsTypes.LPAREN
                || prevType == CqrsTypes.KW_TYPE) {
            return false; // a literal or a new declaration's name - never a type reference
        }
        // A business rule is nested in an aggregate/entity/view/service, so the container lookup below
        // finds that enclosing block and would offer every visible type inside the rule - where the
        // only type position is the one right after 'exception'.
        if (enclosingBusinessRule(position) != null) {
            PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
            return prev != null && prev.getNode().getElementType() == CqrsTypes.KW_EXCEPTION;
        }
        return typeMemberContainer(position) != null;
    }

    /**
     * The type-declaring block surrounding the caret, or {@code null}. A half-typed attribute is
     * just one identifier (the type, no name yet), so {@code attribute} (pinned only after its name
     * ID) rolls back and the caret token can land outside the block node via error recovery. When
     * the direct ancestor lookup fails we therefore retry from the previous visible leaf, which is
     * part of the well-formed prefix (the block's {@code &#123;} or a prior member) and so is still
     * inside the block node.
     */
    private static PsiElement typeMemberContainer(PsiElement position) {
        PsiElement container = enclosingContainer(position);
        if (container != null) {
            return container;
        }
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        return prev == null ? null : enclosingContainer(prev);
    }

    private static PsiElement enclosingContainer(PsiElement element) {
        return PsiTreeUtil.getParentOfType(element,
                CqrsConstructorDef.class, CqrsMethodDef.class,
                CqrsValueObject.class, CqrsEntityId.class, CqrsAggregateId.class,
                CqrsEnumObject.class, CqrsEntityDef.class, CqrsAggregateDef.class,
                CqrsConstraintDef.class, CqrsAnnotationDef.class, CqrsExceptionDef.class,
                CqrsEventDef.class, CqrsCommandDef.class);
    }

    /** Adds every visible named declaration as a type candidate (mirrors {@code CqrsReference.getVariants}). */
    private static void addTypeVariants(PsiElement position, @NotNull CompletionResultSet result) {
        Set<String> seen = new HashSet<>();
        for (CqrsNamedElement decl : CqrsResolveUtil.referenceableDeclarations(position)) {
            String name = decl.getName();
            if (name == null || !seen.add(name)) {
                continue;
            }
            // Insert the caret-escaped form when the name is a keyword, but show the plain name.
            result.addElement(LookupElementBuilder.create(decl, CqrsNames.escape(name))
                    .withPresentableText(name)
                    .withIcon(CqrsIcons.FILE)
                    .withTypeText(CqrsResolveUtil.getQualifiedName(decl), true));
        }
    }

    private static Set<String> keywordsFor(PsiElement position) {
        Set<String> keywords = new LinkedHashSet<>();

        // Positions the previous token alone identifies unambiguously, whatever block they sit in.
        IElementType prevType = prevType(position);
        if (prevType == CqrsTypes.KW_EXAMPLES || prevType == CqrsTypes.LPAREN) {
            // 'examples <literal>*' and the '(<literal>, ...)' argument list of a constraint,
            // business-rule, annotation or enum instance - the only places a literal is legal, and
            // '(' starts nothing else in the grammar.
            keywords.addAll(LITERAL_KEYWORDS);
            return keywords;
        }
        if (prevType == CqrsTypes.KW_TYPE) {
            // 'type [element] <Name> [generics <n>]' - an external type declaration.
            keywords.add("element");
            return keywords;
        }

        // A 'data-protection { ... }' block is nested inside a module (or an entity/aggregate),
        // so it must be checked first; inside it we offer the clause keywords and, right after a
        // clause keyword, its enum values.
        if (enclosingDataProtection(position) != null) {
            return dataProtectionKeywords(position);
        }

        // A 'business-rule ... { consistency ... }' block holds nothing but the consistency clause.
        // It is checked before the constructor/method and aggregate/entity branches because it is
        // nested inside them (an aggregate, an entity, a view, or an inline service of a method), and
        // those branches would otherwise claim the caret and offer their own - here invalid - keywords.
        if (enclosingBusinessRule(position) != null) {
            return businessRuleKeywords(position);
        }

        CqrsConstructorDef ctor = enclosingConstructor(position);
        CqrsMethodDef method = enclosingMethod(position);
        if (ctor != null || method != null) {
            keywords.add("optional");
            keywords.add("event");
            // The SPI the operation needs, and the inline declaration of the interface it points at.
            keywords.add("operation-context");
            keywords.add("service");
            if (method != null) {
                keywords.add("returns");
                // 'rest-path' is a header clause of a *view* method (before the '{'): it is an error
                // on any other method, and inside the body it would be invalid syntax.
                if (enclosingView(position) != null && beforeBody(method, position)) {
                    keywords.add("rest-path");
                }
            }
            return keywords;
        }

        if (PsiTreeUtil.getParentOfType(position, CqrsAggregateDef.class) != null
                || PsiTreeUtil.getParentOfType(position, CqrsEntityDef.class) != null) {
            keywords.addAll(META_KEYWORDS);
            keywords.add("optional");
            keywords.add("protected-by");
            keywords.add("business-rule");
            keywords.add("constructor");
            keywords.add("method");
            keywords.add("event");
            return keywords;
        }

        if (PsiTreeUtil.getParentOfType(position, CqrsEnumObject.class) != null) {
            keywords.addAll(META_KEYWORDS);
            keywords.add("instances");
            keywords.add("deprecated");
            return keywords;
        }

        // A 'process-manager { ... }' block (and its 'reacts-to { ... }' reactions) is nested inside a
        // module/context, so it must be checked before them - a caret in a reaction body is offered
        // the reaction clauses; elsewhere in the manager body, the manager clauses.
        if (enclosingProcessManager(position) != null) {
            return processManagerKeywords(position);
        }

        // A 'view ... { ... }' body holds business rules and methods (a caret inside a view method is
        // already handled by the constructor/method branch above).
        CqrsViewDef view = enclosingView(position);
        if (view != null) {
            // 'rest-path' sits in the view header (before the '{'), everything else in the body.
            if (beforeBody(view, position)) {
                keywords.add("rest-path");
                return keywords;
            }
            keywords.add("hint");
            keywords.add("cron-schedule");
            keywords.add("business-rule");
            keywords.add("method");
            return keywords;
        }

        if (enclosingModule(position) != null) {
            keywords.add("dependency");
            keywords.add("import");
            keywords.addAll(ELEMENT_KEYWORDS);
            keywords.addAll(META_KEYWORDS);
            keywords.add("optional");
            keywords.add("protected-by");
            keywords.add("base");
            keywords.add("message");
            keywords.add("constructor");
            keywords.add("method");
            return keywords;
        }

        // inside a context (but not a module)
        if (enclosingContext(position) != null) {
            keywords.add("dependency");
            keywords.add("import");
            keywords.add("hint");
            keywords.add("module");
            return keywords;
        }

        // top level
        keywords.add("context");
        return keywords;
    }

    /**
     * The {@code data-protection} block surrounding the caret, or {@code null}. While the block is
     * being edited the value after a clause keyword is often a parse error, so the caret token can
     * land just outside the pinned {@code data_protection_def} node; we then retry from the previous
     * visible leaf (part of the well-formed prefix). A previous leaf of {@code &#125;} means the
     * block is already closed and the caret is back at the enclosing level, so it is excluded.
     */
    /**
     * Completion inside a {@code process-manager { ... }} block. Inside a {@code reacts-to} reaction
     * body the reaction clauses are offered (and, right after a duration number, the time units for
     * {@code arm-timeout}); before the reaction's brace only {@code in-state} applies. Elsewhere in
     * the manager body the manager clauses are offered.
     */
    private static Set<String> processManagerKeywords(PsiElement position) {
        Set<String> keywords = new LinkedHashSet<>();
        CqrsProcessReaction reaction = enclosingProcessReaction(position);
        if (reaction != null) {
            ASTNode lbrace = reaction.getNode().findChildByType(CqrsTypes.LBRACE);
            boolean inBody = lbrace != null && lbrace.getStartOffset() < position.getTextOffset();
            if (!inBody) {
                keywords.add("in-state"); // still in the 'reacts-to <Event> ...' header
                return keywords;
            }
            PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
            IElementType type = prev == null ? null : prev.getNode().getElementType();
            if (type == CqrsTypes.NUMBER) {
                keywords.addAll(TIME_UNITS); // 'arm-timeout <number> <unit>'
            } else {
                keywords.addAll(PM_REACTION_KEYWORDS);
            }
            return keywords;
        }
        keywords.addAll(PM_BODY_KEYWORDS);
        return keywords;
    }

    /**
     * The {@code process-manager} block surrounding the caret, or {@code null}. Mirrors
     * {@link #enclosingDataProtection}: when the caret token lands just outside the pinned node via
     * error recovery, it retries from the previous visible leaf, excluding a closing {@code &#125;}
     * (which means the block is already closed and the caret is back at the enclosing level).
     */
    private static CqrsProcessManager enclosingProcessManager(PsiElement position) {
        CqrsProcessManager pm = PsiTreeUtil.getParentOfType(position, CqrsProcessManager.class);
        if (pm != null) {
            return pm;
        }
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        if (prev == null || prev.getNode().getElementType() == CqrsTypes.RBRACE) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(prev, CqrsProcessManager.class);
    }

    /**
     * Determines whether the caret sits in the header of the given element - that is, before its
     * opening <code>&#123;</code>. Header clauses such as {@code rest-path} are only legal there.
     * An element whose body brace is missing (still being typed) counts as "header".
     */
    private static boolean beforeBody(PsiElement element, PsiElement position) {
        PsiElement brace = null;
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode() != null && child.getNode().getElementType() == CqrsTypes.LBRACE) {
                brace = child;
                break;
            }
        }
        return brace == null || position.getTextOffset() <= brace.getTextOffset();
    }

    /**
     * The {@code constructor} block surrounding the caret, or {@code null} (same error recovery as
     * above). A body clause is introduced by its keyword, so a half-typed word is not yet part of any
     * of them and error recovery can push the caret token just outside the pinned node.
     */
    private static CqrsConstructorDef enclosingConstructor(PsiElement position) {
        CqrsConstructorDef ctor = PsiTreeUtil.getParentOfType(position, CqrsConstructorDef.class);
        if (ctor != null) {
            return ctor;
        }
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        if (prev == null || prev.getNode().getElementType() == CqrsTypes.RBRACE) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(prev, CqrsConstructorDef.class);
    }

    /** The {@code method} block surrounding the caret, or {@code null} (same recovery as above). */
    private static CqrsMethodDef enclosingMethod(PsiElement position) {
        CqrsMethodDef method = PsiTreeUtil.getParentOfType(position, CqrsMethodDef.class);
        if (method != null) {
            return method;
        }
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        if (prev == null || prev.getNode().getElementType() == CqrsTypes.RBRACE) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(prev, CqrsMethodDef.class);
    }

    /** The {@code view} block surrounding the caret, or {@code null} (same error recovery as above). */
    private static CqrsViewDef enclosingView(PsiElement position) {
        CqrsViewDef view = PsiTreeUtil.getParentOfType(position, CqrsViewDef.class);
        if (view != null) {
            return view;
        }
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        if (prev == null || prev.getNode().getElementType() == CqrsTypes.RBRACE) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(prev, CqrsViewDef.class);
    }

    /**
     * The {@code context} block surrounding the caret, or {@code null} (same error recovery as above).
     * A context is a pinned block, so a caret in an otherwise-empty context lands just outside the
     * pinned node; we retry from the previous visible leaf, excluding a closing {@code &#125;}.
     */
    private static CqrsContextDef enclosingContext(PsiElement position) {
        CqrsContextDef ctx = PsiTreeUtil.getParentOfType(position, CqrsContextDef.class);
        if (ctx != null) {
            return ctx;
        }
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        if (prev == null || prev.getNode().getElementType() == CqrsTypes.RBRACE) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(prev, CqrsContextDef.class);
    }

    /** The {@code module} block surrounding the caret, or {@code null} (same recovery as above). */
    private static CqrsModuleDef enclosingModule(PsiElement position) {
        CqrsModuleDef ns = PsiTreeUtil.getParentOfType(position, CqrsModuleDef.class);
        if (ns != null) {
            return ns;
        }
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        if (prev == null || prev.getNode().getElementType() == CqrsTypes.RBRACE) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(prev, CqrsModuleDef.class);
    }

    /** The {@code reacts-to} reaction surrounding the caret, or {@code null} (same recovery as above). */
    private static CqrsProcessReaction enclosingProcessReaction(PsiElement position) {
        CqrsProcessReaction reaction = PsiTreeUtil.getParentOfType(position, CqrsProcessReaction.class);
        if (reaction != null) {
            return reaction;
        }
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        if (prev == null || prev.getNode().getElementType() == CqrsTypes.RBRACE) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(prev, CqrsProcessReaction.class);
    }

    /**
     * The {@code business-rule} block surrounding the caret, or {@code null} (same error recovery as
     * above). While the consistency clause is being typed its value is often a parse error, so the
     * caret token can land just outside the pinned {@code business_rule} node.
     */
    private static CqrsBusinessRule enclosingBusinessRule(PsiElement position) {
        CqrsBusinessRule rule = PsiTreeUtil.getParentOfType(position, CqrsBusinessRule.class);
        if (rule != null) {
            return rule;
        }
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        if (prev == null || prev.getNode().getElementType() == CqrsTypes.RBRACE) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(prev, CqrsBusinessRule.class);
    }

    /**
     * Completion inside a {@code business-rule ... { consistency ... }} block. A business rule holds
     * exactly one consistency clause, so the token immediately before the caret determines the whole
     * answer: the clause keyword that comes next, or the value set of the clause just named. The
     * value sets are the grammar's {@code consistency_level}, {@code inconsistency_detection} and
     * {@code inconsistency_resolution} enums - nothing else is legal in those positions.
     */
    private static Set<String> businessRuleKeywords(PsiElement position) {
        Set<String> keywords = new LinkedHashSet<>();
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        IElementType type = prev == null ? null : prev.getNode().getElementType();

        if (type == CqrsTypes.KW_BUSINESS_RULE || type == CqrsTypes.KW_EXCEPTION) {
            // Still typing the rule's name, or the exception type - the latter is a type reference
            // resolved by allowsTypeRef()/getVariants(), not a keyword position.
            return keywords;
        }
        if (type == CqrsTypes.KW_CONSISTENCY) {
            keywords.addAll(CONSISTENCY_LEVELS);
        } else if (type == CqrsTypes.KW_WEAK) {
            // 'consistency weak' must be followed by the details block, so the only thing that can
            // come next is its opening brace - and inside it, 'acceptable'.
            keywords.add("acceptable");
        } else if (type == CqrsTypes.KW_ACCEPTABLE) {
            return keywords; // 'acceptable <number> <unit>' - the number has no completion
        } else if (type == CqrsTypes.NUMBER) {
            keywords.addAll(TIME_UNITS); // 'acceptable 1 days'
        } else if (TIME_UNIT_TOKENS.contains(type)) {
            keywords.add("detection");
        } else if (type == CqrsTypes.KW_DETECTION) {
            keywords.addAll(INCONSISTENCY_DETECTIONS);
        } else if (type == CqrsTypes.KW_RESOLUTION) {
            keywords.addAll(INCONSISTENCY_RESOLUTIONS);
        } else if (type == CqrsTypes.KW_NEVER || type == CqrsTypes.KW_MANUALLY
                || type == CqrsTypes.KW_AUTOMATIC) {
            // A detection value is followed by the resolution clause; a resolution value ends the
            // block, but 'workflow' is the only value exclusive to it, so offering 'resolution' after
            // the shared three is the best guess available from the previous token alone.
            keywords.add("resolution");
        } else if (type == CqrsTypes.LBRACE) {
            // Either the rule's own body or the weak-consistency block: which one decides whether
            // 'consistency' or 'acceptable' comes next.
            keywords.add(insideWeakConsistencyBlock(prev) ? "acceptable" : "consistency");
        } else {
            keywords.add("consistency");
        }
        return keywords;
    }

    /**
     * Whether the given <code>&#123;</code> opens the weak-consistency details block rather than the
     * business rule's own body. The details brace is preceded by {@code weak}, the rule's body brace
     * by the exception's type reference.
     */
    private static boolean insideWeakConsistencyBlock(PsiElement lbrace) {
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(lbrace);
        return prev != null && prev.getNode().getElementType() == CqrsTypes.KW_WEAK;
    }

    private static CqrsDataProtectionDef enclosingDataProtection(PsiElement position) {
        CqrsDataProtectionDef dp = PsiTreeUtil.getParentOfType(position, CqrsDataProtectionDef.class);
        if (dp != null) {
            return dp;
        }
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        if (prev == null || prev.getNode().getElementType() == CqrsTypes.RBRACE) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(prev, CqrsDataProtectionDef.class);
    }

    /**
     * Completion inside a {@code data-protection { ... }} block. The token immediately before the
     * caret decides what to offer: the value set of the clause just named (e.g. after
     * {@code protection} → the protection levels), or otherwise the clause keywords themselves.
     */
    private static Set<String> dataProtectionKeywords(PsiElement position) {
        Set<String> keywords = new LinkedHashSet<>();
        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
        IElementType type = prev == null ? null : prev.getNode().getElementType();

        if (type == CqrsTypes.KW_DATA_PROTECTION) {
            return keywords; // user is still typing the policy name
        }
        if (type == CqrsTypes.KW_PROTECTION) {
            keywords.addAll(PROTECTION_LEVELS);
        } else if (type == CqrsTypes.KW_LAWFUL_BASIS) {
            keywords.addAll(LAWFUL_BASES);
        } else if (type == CqrsTypes.KW_CATEGORY || type == CqrsTypes.COMMA) {
            keywords.addAll(SPECIAL_CATEGORIES);
        } else if (type == CqrsTypes.KW_THEN) {
            keywords.addAll(ERASURE_STRATEGIES);
        } else if (type == CqrsTypes.NUMBER) {
            keywords.addAll(TIME_UNITS); // 'retention <number> <unit>'
        } else if (TIME_UNIT_TOKENS.contains(type)) {
            keywords.add("then"); // optional erasure clause after the retention duration
            keywords.addAll(DP_CLAUSE_KEYWORDS);
        } else {
            keywords.addAll(DP_CLAUSE_KEYWORDS);
        }
        return keywords;
    }
}
