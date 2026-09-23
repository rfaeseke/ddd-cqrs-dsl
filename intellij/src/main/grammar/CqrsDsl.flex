package org.fuin.dsl.cqrs.intellij.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.fuin.dsl.cqrs.intellij.psi.CqrsTokenTypes.UNCLOSED_STRING;
import static org.fuin.dsl.cqrs.intellij.psi.CqrsTypes.*;

%%

%public
%class _CqrsDslLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%{
  public _CqrsDslLexer() {
    this((java.io.Reader) null);
  }
%}

WHITE_SPACE=[\ \t\f\r\n]+

LINE_COMMENT="//"[^\r\n]*
DOC_COMMENT="/**"([^*]|"*"+[^*/])*"*"+"/"
BLOCK_COMMENT="/*"([^*]|"*"+[^*/])*"*"+"/"

ID="^"?[A-Za-z][A-Za-z_0-9]*

STRING=\"([^\\\"]|\\[^])*\"|'([^\\']|\\[^])*'
// A string opened but not closed before the end of the line. The longest match wins, so a closed
// string - even a multi line one - is still lexed as STRING.
UNCLOSED_STRING=\"([^\\\"\r\n]|\\[^\r\n])*|'([^\\'\r\n]|\\[^\r\n])*

HEX=0[xX][0-9a-fA-F_]+("#"(([bB][iI])|[lL]))?
INT=[0-9][0-9_]*
DECIMAL={INT}([eE][+-]?{INT})?([bB][iIdD]|[lLdDfF])?
NUMBER={HEX}|({INT}|{DECIMAL})("."({INT}|{DECIMAL}))?

%%

<YYINITIAL> {

  {WHITE_SPACE}              { return WHITE_SPACE; }

  {LINE_COMMENT}             { return LINE_COMMENT; }
  {DOC_COMMENT}              { return DOC_COMMENT; }
  {BLOCK_COMMENT}            { return BLOCK_COMMENT; }

  // ---- Hyphenated keywords (must precede the ID rule) ----
  "value-object"            { return KW_VALUE_OBJECT; }
  "entity-id-path"          { return KW_ENTITY_ID_PATH; }
  "entity-id"               { return KW_ENTITY_ID; }
  "aggregate-id"            { return KW_AGGREGATE_ID; }
  "business-rules"          { return KW_BUSINESS_RULES; }
  "business-rule"           { return KW_BUSINESS_RULE; }
  "command-handler"         { return KW_COMMAND_HANDLER; }
  "copies-attributes-of"    { return KW_COPIES_ATTRIBUTES_OF; }
  "data-protection"         { return KW_DATA_PROTECTION; }
  "protected-by"            { return KW_PROTECTED_BY; }
  "lawful-basis"            { return KW_LAWFUL_BASIS; }
  "process-manager"         { return KW_PROCESS_MANAGER; }
  "cron-schedule"           { return KW_CRON_SCHEDULE; }
  "instance-key"            { return KW_INSTANCE_KEY; }
  "process-states"          { return KW_PROCESS_STATES; }
  "reacts-to"               { return KW_REACTS_TO; }
  "in-state"                { return KW_IN_STATE; }
  "correlate-by"            { return KW_CORRELATE_BY; }
  "issues-commands"         { return KW_ISSUES_COMMANDS; }
  "addressed-by"            { return KW_ADDRESSED_BY; }
  "transition-to"           { return KW_TRANSITION_TO; }
  "arm-timeout"             { return KW_ARM_TIMEOUT; }
  "cancel-timeout"          { return KW_CANCEL_TIMEOUT; }
  "rest-path"               { return KW_REST_PATH; }
  "identified-by"           { return KW_IDENTIFIED_BY; }
  "on-collision"            { return KW_ON_COLLISION; }
  "display-as"              { return KW_DISPLAY_AS; }
  "no-key"                  { return KW_NO_KEY; }
  "is-empty"                { return KW_IS_EMPTY; }
  "own-id"                  { return KW_OWN_ID; }
  "own-path"                { return KW_OWN_PATH; }

  // ---- Structural keywords ----
  "own"                     { return KW_OWN; }
  "context"                 { return KW_CONTEXT; }
  "module"                  { return KW_MODULE; }
  "import"                  { return KW_IMPORT; }
  "dependency"              { return KW_DEPENDENCY; }
  "local"                   { return KW_LOCAL; }
  "hint"                    { return KW_HINT; }
  "type"                    { return KW_TYPE; }
  "element"                 { return KW_ELEMENT; }
  "generics"                { return KW_GENERICS; }
  "constraint"              { return KW_CONSTRAINT; }
  "input"                   { return KW_INPUT; }
  "exception"               { return KW_EXCEPTION; }
  "annotation"              { return KW_ANNOTATION; }
  "message"                 { return KW_MESSAGE; }
  "base"                    { return KW_BASE; }
  "identifies"              { return KW_IDENTIFIES; }
  "enum"                    { return KW_ENUM; }
  "instances"               { return KW_INSTANCES; }
  "deprecated"              { return KW_DEPRECATED; }
  "event"                   { return KW_EVENT; }
  "entity"                  { return KW_ENTITY; }
  "identifier"              { return KW_IDENTIFIER; }
  "root"                    { return KW_ROOT; }
  "aggregate"               { return KW_AGGREGATE; }
  "constructor"             { return KW_CONSTRUCTOR; }
  "fires"                   { return KW_FIRES; }
  "returns"                 { return KW_RETURNS; }
  "method"                  { return KW_METHOD; }
  "ref"                     { return KW_REF; }
  "slabel"                  { return KW_SLABEL; }
  "label"                   { return KW_LABEL; }
  "tooltip"                 { return KW_TOOLTIP; }
  "prompt"                  { return KW_PROMPT; }
  "examples"                { return KW_EXAMPLES; }
  "invariants"              { return KW_INVARIANTS; }
  "preconditions"           { return KW_PRECONDITIONS; }
  "operation-context"       { return KW_OPERATION_CONTEXT; }
  "service"                 { return KW_SERVICE; }
  "command"                 { return KW_COMMAND; }
  "target"                  { return KW_TARGET; }
  "sla"                     { return KW_SLA; }
  "handles"                 { return KW_HANDLES; }
  "uses"                    { return KW_USES; }
  "projection"              { return KW_PROJECTION; }
  "view"                    { return KW_VIEW; }
  "consistency"             { return KW_CONSISTENCY; }
  "acceptable"              { return KW_ACCEPTABLE; }
  "detection"               { return KW_DETECTION; }
  "resolution"              { return KW_RESOLUTION; }
  "optional"                { return KW_OPTIONAL; }

  // ---- Business key and rule predicate keywords ----
  "key"                     { return KW_KEY; }
  "attributes"              { return KW_ATTRIBUTES; }
  "refuse"                  { return KW_REFUSE; }
  "overwrite"               { return KW_OVERWRITE; }
  "skip"                    { return KW_SKIP; }
  "requires"                { return KW_REQUIRES; }

  // ---- Data protection keywords ----
  "protection"              { return KW_PROTECTION; }
  "category"                { return KW_CATEGORY; }
  "subject"                 { return KW_SUBJECT; }
  "purpose"                 { return KW_PURPOSE; }
  "retention"               { return KW_RETENTION; }
  "then"                    { return KW_THEN; }

  // ---- Time unit keywords ----
  "millis"                  { return KW_MILLIS; }
  "seconds"                 { return KW_SECONDS; }
  "minutes"                 { return KW_MINUTES; }
  "hours"                   { return KW_HOURS; }
  "days"                    { return KW_DAYS; }
  "weeks"                   { return KW_WEEKS; }
  "months"                  { return KW_MONTHS; }
  "years"                   { return KW_YEARS; }

  // ---- Consistency / detection / resolution value keywords ----
  "weak"                    { return KW_WEAK; }
  "strong"                  { return KW_STRONG; }
  "never"                   { return KW_NEVER; }
  "manually"                { return KW_MANUALLY; }
  "automatic"               { return KW_AUTOMATIC; }
  "workflow"                { return KW_WORKFLOW; }

  // ---- Protection level value keywords ----
  "none"                    { return KW_NONE; }
  "personal"                { return KW_PERSONAL; }
  "sensitive"               { return KW_SENSITIVE; }

  // ---- Lawful basis value keywords ----
  "explicit_consent"        { return KW_EXPLICIT_CONSENT; }
  "consent"                 { return KW_CONSENT; }
  "contract"                { return KW_CONTRACT; }
  "legal_obligation"        { return KW_LEGAL_OBLIGATION; }
  "vital_interests"         { return KW_VITAL_INTERESTS; }
  "public_task"             { return KW_PUBLIC_TASK; }
  "legitimate_interests"    { return KW_LEGITIMATE_INTERESTS; }

  // ---- Special category value keywords ----
  "health"                  { return KW_HEALTH; }
  "genetic"                 { return KW_GENETIC; }
  "biometric"               { return KW_BIOMETRIC; }
  "racial"                  { return KW_RACIAL; }
  "political"               { return KW_POLITICAL; }
  "religious"               { return KW_RELIGIOUS; }
  "philosophical"           { return KW_PHILOSOPHICAL; }
  "trade_union"             { return KW_TRADE_UNION; }
  "sex_life"                { return KW_SEX_LIFE; }
  "sexual_orientation"      { return KW_SEXUAL_ORIENTATION; }

  // ---- Erasure strategy value keywords ----
  "delete"                  { return KW_DELETE; }
  "anonymize"               { return KW_ANONYMIZE; }
  "pseudonymize"            { return KW_PSEUDONYMIZE; }
  "archive"                 { return KW_ARCHIVE; }
  "review"                  { return KW_REVIEW; }

  // ---- Literal keywords ----
  "true"                    { return KW_TRUE; }
  "false"                   { return KW_FALSE; }
  "null"                    { return KW_NULL; }

  // ---- Punctuation ----
  "{"                       { return LBRACE; }
  "}"                       { return RBRACE; }
  "("                       { return LPAREN; }
  ")"                       { return RPAREN; }
  "<"                       { return LT; }
  ">"                       { return GT; }
  "@"                       { return AT; }
  ","                       { return COMMA; }
  ".."                      { return DOTDOT; }
  "."                       { return DOT; }
  "/"                       { return SLASH; }
  "|"                       { return PIPE; }
  "*"                       { return STAR; }
  ":"                       { return COLON; }
  "["                       { return LBRACKET; }
  "]"                       { return RBRACKET; }
  "=="                      { return EQ; }
  "!="                      { return NEQ; }
  "<="                      { return LE; }
  ">="                      { return GE; }
  "&&"                      { return AND; }
  "||"                      { return OR; }
  "!"                       { return NOT; }

  // ---- Terminals ----
  {STRING}                  { return STRING; }
  {UNCLOSED_STRING}         { return UNCLOSED_STRING; }
  {NUMBER}                  { return NUMBER; }
  {ID}                      { return ID; }

}

[^]                         { return BAD_CHARACTER; }
