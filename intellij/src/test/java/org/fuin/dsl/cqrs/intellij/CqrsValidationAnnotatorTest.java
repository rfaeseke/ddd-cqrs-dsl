package org.fuin.dsl.cqrs.intellij;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Verifies {@link CqrsValidationAnnotator} — the semantic validation ported from the Eclipse/Xtext
 * {@code CqrsDslValidator}. Expected errors/warnings are marked inline with {@code <error>}/
 * {@code <warning>} ranges. Unresolved references are reported separately by
 * {@link CqrsAnnotator}, so the bodies below only use types they declare.
 */
public class CqrsValidationAnnotatorTest extends BasePlatformTestCase {

    /** Asserts the highlighting of {@code body} matches its inline markup (errors + warnings). */
    private void check(String body) {
        myFixture.configureByText("test.cqrs", body);
        myFixture.checkHighlighting(true, false, false);
    }

    // --- value object 'base' ---------------------------------------------------------------------
    // A 'base' does not restrict the attributes: only "base String + exactly one attribute" is
    // generated as a complete class (SimpleStringValueObjectArtifactFactory); every other shape
    // gets an abstract base class plus a hand-written final class supplying asBaseType(). See
    // CombinedValueObjectArtifactFactory.

    public void testValueObjectBaseSingleMatchingAttributeIsValid() {
        check("""
                context p {
                  module c.n {
                    type String
                    value-object Email base String {
                      String value
                    }
                  }
                }
                """);
    }

    /** The PhoneNumber shape: a String-backed VO whose base representation packs several attributes. */
    public void testValueObjectBaseWithSeveralAttributesIsValid() {
        check("""
                context p {
                  module c.n {
                    type String
                    enum PhoneType {
                      instances {
                        MOBILE
                        LANDLINE
                      }
                    }
                    value-object PhoneNumber base String {
                      PhoneType typ
                      String value
                    }
                  }
                }
                """);
    }

    // --- 'base' and the attribute holding the value ----------------------------------------------
    // A base the generator builds a value from - UUID, a number, a decimal - is generated as a wrapper
    // around exactly one declared attribute, because the generated valueOf() and converters call a
    // one-argument constructor written from the attributes. A String base is never instantiated by
    // generated code, so any number of attributes compiles - including none, which leaves supplying
    // the value to the write-once class.

    public void testInstantiatedBaseWithNoAttributeIsRefused() {
        check("""
                context p {
                  module c.n {
                    type UUID
                    value-object Key base <error>UUID</error> {
                    }
                  }
                }
                """);
    }

    public void testInstantiatedBaseWithSeveralAttributesIsRefused() {
        check("""
                context p {
                  module c.n {
                    type UUID
                    value-object Pair base <error>UUID</error> {
                      UUID a
                      UUID b
                    }
                  }
                }
                """);
    }

    public void testStringBaseWithNoAttributeIsLeftAlone() {
        check("""
                context p {
                  module c.n {
                    type String
                    value-object Email base String {
                    }
                  }
                }
                """);
    }

    public void testEntityIdOnIntegerOrUuidNeedsNoAttribute() {
        check("""
                context p {
                  module c.n {
                    type Integer
                    type UUID
                    aggregate-id ThingId identifies Thing base UUID {
                    }
                    aggregate Thing identifier ThingId {
                    }
                    entity-id PartId identifies Part base Integer {
                    }
                    entity-id SlotId identifies Slot base UUID {
                    }
                    entity Part identifier PartId root Thing {
                    }
                    entity Slot identifier SlotId root Thing {
                    }
                  }
                }
                """);
    }

    public void testAggregateIdOnIntegerHasNoSuchShortcut() {
        check("""
                context p {
                  module c.n {
                    type Integer
                    aggregate-id ThingId identifies Thing base <error>Integer</error> {
                    }
                    aggregate Thing identifier ThingId {
                    }
                  }
                }
                """);
    }

    public void testValueObjectBaseWithMismatchingAttributeTypeIsValid() {
        check("""
                context p {
                  module c.n {
                    type String
                    type Integer
                    value-object X base Integer {
                      String value
                    }
                  }
                }
                """);
    }

    public void testValueObjectWithoutBaseAllowsSeveralAttributes() {
        check("""
                context p {
                  module c.n {
                    type String
                    type Integer
                    value-object Money {
                      Integer amount
                      String currency
                    }
                  }
                }
                """);
    }

    // --- value object 'base': no constructors / methods -----------------------------------------

    public void testValueObjectBaseWithConstructorIsFlagged() {
        check("""
                context p {
                  module c.n {
                    type String
                    value-object X base String {
                      String value
                      constructor <error>create</error> {
                      }
                    }
                  }
                }
                """);
    }

    public void testValueObjectBaseWithMethodIsFlagged() {
        check("""
                context p {
                  module c.n {
                    type String
                    value-object X base String {
                      String value
                      method <error>doIt</error> {
                        returns String
                      }
                    }
                  }
                }
                """);
    }

    // --- variable naming ------------------------------------------------------------------------

    public void testVariableNameStartingUpperCaseIsWarned() {
        check("""
                context p {
                  module c.n {
                    type String
                    value-object X {
                      String <warning>Value</warning>
                    }
                  }
                }
                """);
    }

    // --- cron-schedule (Spring cron validation) -------------------------------------------------

    public void testViewValidCronScheduleIsAccepted() {
        check("""
                context p {
                  module c.n {
                    projection Pj
                    view V uses Pj {
                      cron-schedule "0 0 12 * * MON-FRI"
                    }
                  }
                }
                """);
    }

    public void testViewMacroCronScheduleIsAccepted() {
        check("""
                context p {
                  module c.n {
                    projection Pj
                    view V uses Pj {
                      cron-schedule "@daily"
                    }
                  }
                }
                """);
    }

    public void testViewInvalidCronScheduleIsReported() {
        check("""
                context p {
                  module c.n {
                    projection Pj
                    view V uses Pj {
                      cron-schedule <error>"not a cron"</error>
                    }
                  }
                }
                """);
    }

    public void testProcessManagerInvalidCronScheduleIsReported() {
        check("""
                context p {
                  module c.n {
                    process-manager PM {
                      cron-schedule <error>"99 * * * * *"</error>
                    }
                  }
                }
                """);
    }

    // --- consistency -----------------------------------------------------------------------------
    // The grammar deliberately accepts a details block with missing clauses so that code completion
    // keeps working while it is being typed; completeness is enforced here instead.

    public void testStrongConsistencyWithoutDetailsIsValid() {
        check(businessRule("consistency strong"));
    }

    public void testWeakConsistencyWithAllDetailsIsValid() {
        check(businessRule("""
                consistency weak {
                          acceptable 1 days
                          detection manually
                          resolution manually
                        }"""));
    }

    public void testWeakConsistencyWithoutDetailsIsReported() {
        check(businessRule("<error>consistency weak</error>"));
    }

    public void testWeakConsistencyWithIncompleteDetailsIsReported() {
        check(businessRule("""
                consistency weak <error>{
                          acceptable 1 days
                        }</error>"""));
    }

    public void testStrongConsistencyWithDetailsIsReported() {
        check(businessRule("""
                consistency strong <error>{
                          acceptable 1 days
                          detection manually
                          resolution manually
                        }</error>"""));
    }

    /**
     * Wraps a consistency clause in the smallest aggregate that can hold a business rule.
     *
     * @param consistency Consistency clause, with any expected error markup.
     *
     * @return Complete model source.
     */
    // --- dependency ------------------------------------------------------------------------------

    public void testWellFormedDependencyIsValid() {
        check("""
                context p {
                  dependency "org.acme:acme-model:1.0.0"
                  dependency "org.acme:other-model:2.0.0" local "../other/src/main/cqrs"
                  module c {
                    type String
                  }
                }
                """);
    }

    public void testMalformedDependencyCoordinateIsError() {
        check("""
                context p {
                  dependency <error descr="A dependency must be 'groupId:artifactId:version', but was 'org.acme:acme-model'">"org.acme:acme-model"</error>
                  module c {
                    type String
                  }
                }
                """);
    }

    // --- import ----------------------------------------------------------------------------------

    public void testUnresolvedImportIsError() {
        check("""
                context p {
                  module c {
                    import <error descr="Import 'nowhere.at.all.*' does not match any context, module or type">nowhere.at.all.*</error>
                    type String
                  }
                }
                """);
    }

    public void testDuplicateImportIsError() {
        check("""
                context p {
                  module types {
                    type String
                  }
                  module c {
                    import p.types.*
                    import <error descr="Duplicate import 'p.types.*'">p.types.*</error>
                    value-object V base String {
                      String v
                    }
                  }
                }
                """);
    }

    public void testImportAlreadyDeclaredByContextIsWarning() {
        check("""
                context p {
                  import p.types.*
                  module types {
                    type String
                  }
                  module c {
                    import <warning descr="Import 'p.types.*' is already declared by context 'p'">p.types.*</warning>
                    value-object V base String {
                      String v
                    }
                  }
                }
                """);
    }

    public void testUnusedImportIsWarning() {
        check("""
                context p {
                  module types {
                    type String
                  }
                  module c {
                    import <warning descr="Import 'p.types.*' is not used">p.types.*</warning>
                    type Own
                    value-object V base Own {
                      Own v
                    }
                  }
                }
                """);
    }

    public void testDuplicateDependencyIsError() {
        check("""
                context p {
                  dependency "org.acme:acme-model:1.0.0"
                  dependency <error descr="Duplicate dependency 'org.acme:acme-model:1.0.0'">"org.acme:acme-model:1.0.0"</error>
                  module c {
                    type String
                  }
                }
                """);
    }

    private static String businessRule(String consistency) {
        return """
                context p {
                  module c.n {
                    exception MyException { message "m" }
                    aggregate-id FooId identifies Foo {}
                    aggregate Foo identifier FooId {
                      business-rule Rule exception MyException {
                        %s
                      }
                    }
                  }
                }
                """.formatted(consistency);
    }

    // --- business rules --------------------------------------------------------------------------

    /**
     * A rule the aggregate declares itself is not one of its nested elements, so the check that
     * restricts those must not fire on it. Making 'business-rule' a module element is what put the two
     * within reach of each other.
     */
    public void testAggregatesOwnBusinessRuleIsNoIllegalNestedElement() {
        check("""
                context p {
                  module c.n {
                    type String
                    exception Boom {
                      String id
                      message "Boom ${id}"
                    }
                    aggregate-id OrderId identifies Order base String {
                      slabel "OID"
                      label "Order ID"
                      tooltip "Unique identifier of the order"
                      examples "4711"
                    }
                    /** An order. */
                    aggregate Order identifier OrderId {
                      /** A rule of its own. */
                      business-rule MustNotBeShipped exception Boom {
                        consistency strong
                      }
                      /** Cancels the order. */
                      method cancel business-rules MustNotBeShipped {
                      }
                    }
                  }
                }
                """);
    }

    /** A rule at module level is an element like any other and needs no aggregate around it. */
    public void testModuleLevelBusinessRuleIsValid() {
        check("""
                context p {
                  module c.n {
                    type String
                    exception Boom {
                      String id
                      message "Boom ${id}"
                    }
                    /** Makes sure the entity was not deleted yet. */
                    business-rule EntityMustNotBeDeletedRule exception Boom {
                      consistency strong
                    }
                  }
                }
                """);
    }

    // --- process manager 'addressed-by' ------------------------------------------------------------

    private static String addressedModel(String issues) {
        return """
                context p {
                  module c.n {
                    type UUID
                    aggregate-id HouseholdId identifies Household { }
                    aggregate Household identifier HouseholdId {
                      method record { }
                    }
                    event Requested {
                      UUID id
                      HouseholdId household
                    }
                    command Record target Household.record { }
                    process-manager PM {
                      instance-key UUID
                      reacts-to Requested {
                        correlate-by id
                        %s
                      }
                    }
                  }
                }
                """.formatted(issues);
    }

    /** The addressee is a component of the event. */
    public void testAddressedByAnEventComponentIsValid() {
        check(addressedModel("issues-commands Record addressed-by household"));
    }

    /** A component the event does not carry would address nothing - and issue nothing, silently. */
    public void testAddressedByAnUnknownComponentIsAnError() {
        check(addressedModel("issues-commands Record addressed-by <error descr=\"The addressee 'houshold' is not an attribute of event 'Requested'\">houshold</error>"));
    }
}
