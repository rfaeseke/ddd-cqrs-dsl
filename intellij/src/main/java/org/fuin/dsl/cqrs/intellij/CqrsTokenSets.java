package org.fuin.dsl.cqrs.intellij;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.fuin.dsl.cqrs.intellij.psi.CqrsTypes;

/** Shared token-set definitions used by the parser definition and the syntax highlighter. */
public final class CqrsTokenSets {

    public static final TokenSet COMMENTS = TokenSet.create(
            CqrsTypes.LINE_COMMENT, CqrsTypes.BLOCK_COMMENT, CqrsTypes.DOC_COMMENT);

    public static final TokenSet STRINGS = TokenSet.create(CqrsTypes.STRING);

    public static final TokenSet NUMBERS = TokenSet.create(CqrsTypes.NUMBER);

    public static final TokenSet IDENTIFIERS = TokenSet.create(CqrsTypes.ID);

    /** Every reserved word in the language (structural, value and literal keywords). */
    public static final TokenSet KEYWORDS = TokenSet.create(
            CqrsTypes.KW_CONTEXT, CqrsTypes.KW_MODULE, CqrsTypes.KW_IMPORT,
            CqrsTypes.KW_DEPENDENCY, CqrsTypes.KW_LOCAL,
            CqrsTypes.KW_HINT, CqrsTypes.KW_TYPE,
            CqrsTypes.KW_ELEMENT, CqrsTypes.KW_GENERICS, CqrsTypes.KW_CONSTRAINT, CqrsTypes.KW_INPUT,
            CqrsTypes.KW_EXCEPTION, CqrsTypes.KW_BUSINESS_RULE, CqrsTypes.KW_ANNOTATION,
            CqrsTypes.KW_MESSAGE, CqrsTypes.KW_VALUE_OBJECT, CqrsTypes.KW_BASE, CqrsTypes.KW_ENTITY_ID, CqrsTypes.KW_ENTITY_ID_PATH,
            CqrsTypes.KW_IDENTIFIES, CqrsTypes.KW_AGGREGATE_ID, CqrsTypes.KW_ENUM, CqrsTypes.KW_INSTANCES,
            CqrsTypes.KW_DEPRECATED, CqrsTypes.KW_EVENT, CqrsTypes.KW_COPIES_ATTRIBUTES_OF,
            CqrsTypes.KW_ENTITY, CqrsTypes.KW_IDENTIFIER, CqrsTypes.KW_ROOT, CqrsTypes.KW_AGGREGATE,
            CqrsTypes.KW_CONSTRUCTOR, CqrsTypes.KW_FIRES, CqrsTypes.KW_RETURNS, CqrsTypes.KW_METHOD,
            CqrsTypes.KW_REF, CqrsTypes.KW_SLABEL, CqrsTypes.KW_LABEL, CqrsTypes.KW_TOOLTIP,
            CqrsTypes.KW_PROMPT, CqrsTypes.KW_EXAMPLES, CqrsTypes.KW_INVARIANTS, CqrsTypes.KW_PRECONDITIONS,
            CqrsTypes.KW_BUSINESS_RULES, CqrsTypes.KW_OPERATION_CONTEXT, CqrsTypes.KW_SERVICE, CqrsTypes.KW_COMMAND, CqrsTypes.KW_TARGET,
            CqrsTypes.KW_SLA, CqrsTypes.KW_COMMAND_HANDLER, CqrsTypes.KW_HANDLES, CqrsTypes.KW_USES,
            CqrsTypes.KW_PROJECTION, CqrsTypes.KW_VIEW, CqrsTypes.KW_REST_PATH,
            CqrsTypes.KW_PROCESS_MANAGER, CqrsTypes.KW_CRON_SCHEDULE, CqrsTypes.KW_INSTANCE_KEY,
            CqrsTypes.KW_PROCESS_STATES, CqrsTypes.KW_REACTS_TO, CqrsTypes.KW_IN_STATE,
            CqrsTypes.KW_CORRELATE_BY, CqrsTypes.KW_ISSUES_COMMANDS, CqrsTypes.KW_ADDRESSED_BY, CqrsTypes.KW_TRANSITION_TO,
            CqrsTypes.KW_ARM_TIMEOUT, CqrsTypes.KW_CANCEL_TIMEOUT,
            CqrsTypes.KW_CONSISTENCY, CqrsTypes.KW_ACCEPTABLE,
            CqrsTypes.KW_DETECTION, CqrsTypes.KW_RESOLUTION, CqrsTypes.KW_OPTIONAL,
            CqrsTypes.KW_IDENTIFIED_BY,
            CqrsTypes.KW_KEY, CqrsTypes.KW_ATTRIBUTES, CqrsTypes.KW_ON_COLLISION,
            CqrsTypes.KW_DISPLAY_AS, CqrsTypes.KW_NO_KEY,
            CqrsTypes.KW_REFUSE, CqrsTypes.KW_OVERWRITE, CqrsTypes.KW_SKIP,
            CqrsTypes.KW_REQUIRES, CqrsTypes.KW_IS_EMPTY, CqrsTypes.KW_OWN_ID, CqrsTypes.KW_OWN_PATH, CqrsTypes.KW_OWN,
            CqrsTypes.KW_MILLIS, CqrsTypes.KW_SECONDS, CqrsTypes.KW_MINUTES, CqrsTypes.KW_HOURS,
            CqrsTypes.KW_DAYS, CqrsTypes.KW_WEEKS, CqrsTypes.KW_MONTHS, CqrsTypes.KW_YEARS,
            CqrsTypes.KW_WEAK, CqrsTypes.KW_STRONG, CqrsTypes.KW_NEVER,
            CqrsTypes.KW_MANUALLY, CqrsTypes.KW_AUTOMATIC, CqrsTypes.KW_WORKFLOW,
            CqrsTypes.KW_DATA_PROTECTION, CqrsTypes.KW_PROTECTED_BY, CqrsTypes.KW_PROTECTION,
            CqrsTypes.KW_CATEGORY, CqrsTypes.KW_SUBJECT, CqrsTypes.KW_PURPOSE,
            CqrsTypes.KW_LAWFUL_BASIS, CqrsTypes.KW_RETENTION, CqrsTypes.KW_THEN,
            CqrsTypes.KW_NONE, CqrsTypes.KW_PERSONAL, CqrsTypes.KW_SENSITIVE,
            CqrsTypes.KW_CONSENT, CqrsTypes.KW_EXPLICIT_CONSENT, CqrsTypes.KW_CONTRACT,
            CqrsTypes.KW_LEGAL_OBLIGATION, CqrsTypes.KW_VITAL_INTERESTS, CqrsTypes.KW_PUBLIC_TASK,
            CqrsTypes.KW_LEGITIMATE_INTERESTS,
            CqrsTypes.KW_HEALTH, CqrsTypes.KW_GENETIC, CqrsTypes.KW_BIOMETRIC, CqrsTypes.KW_RACIAL,
            CqrsTypes.KW_POLITICAL, CqrsTypes.KW_RELIGIOUS, CqrsTypes.KW_PHILOSOPHICAL,
            CqrsTypes.KW_TRADE_UNION, CqrsTypes.KW_SEX_LIFE, CqrsTypes.KW_SEXUAL_ORIENTATION,
            CqrsTypes.KW_DELETE, CqrsTypes.KW_ANONYMIZE, CqrsTypes.KW_PSEUDONYMIZE,
            CqrsTypes.KW_ARCHIVE, CqrsTypes.KW_REVIEW,
            CqrsTypes.KW_TRUE, CqrsTypes.KW_FALSE, CqrsTypes.KW_NULL);

    public static final TokenSet BRACES = TokenSet.create(
            CqrsTypes.LBRACE, CqrsTypes.RBRACE, CqrsTypes.LBRACKET, CqrsTypes.RBRACKET);

    public static final TokenSet PARENS = TokenSet.create(CqrsTypes.LPAREN, CqrsTypes.RPAREN);

    public static final TokenSet OPERATORS = TokenSet.create(
            CqrsTypes.LT, CqrsTypes.GT, CqrsTypes.AT, CqrsTypes.COMMA, CqrsTypes.DOT,
            CqrsTypes.PIPE, CqrsTypes.STAR, CqrsTypes.COLON);

    private CqrsTokenSets() {
    }

    public static boolean isKeyword(IElementType type) {
        return KEYWORDS.contains(type);
    }
}
