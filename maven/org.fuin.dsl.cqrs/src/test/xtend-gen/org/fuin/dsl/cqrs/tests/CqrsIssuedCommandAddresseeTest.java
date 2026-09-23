package org.fuin.dsl.cqrs.tests;

import com.google.inject.Inject;
import java.util.Collections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.fuin.dsl.cqrs.cqrsDsl.CqrsDslPackage;
import org.fuin.dsl.cqrs.cqrsDsl.DomainModel;
import org.fuin.dsl.cqrs.cqrsDsl.IssuedCommand;
import org.fuin.dsl.cqrs.cqrsDsl.ProcessReaction;
import org.fuin.dsl.cqrs.validation.CqrsDslValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies that a reaction can say which instance a command it issues goes to ('addressed-by').
 * 
 * <p>'correlate-by' names the running process an event belongs to. Where a command then goes is a
 * second question: it coincides with the first only when the command's target is the aggregate the
 * 'instance-key' identifies. A process keyed by a person that records a membership at a household
 * has the household as a component of the event, and before 'addressed-by' no clause could point at it.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(CqrsDslInjectorProvider.class)
@SuppressWarnings("all")
public class CqrsIssuedCommandAddresseeTest {
  @Inject
  private ParseHelper<DomainModel> parseHelper;

  @Inject
  @Extension
  private ValidationTestHelper _validationTestHelper;

  /**
   * The case the clause exists for: keyed by the person, addressed to the household.
   */
  @Test
  public void testACommandIsAddressedByAnEventComponent() {
    try {
      final DomainModel model = this.parseHelper.parse(this.model("PersonId", "issues-commands RecordMembership addressed-by household"));
      this._validationTestHelper.assertNoErrors(model);
      final IssuedCommand issued = IterableExtensions.<IssuedCommand>head(IterableExtensions.<ProcessReaction>head(EcoreUtil2.<ProcessReaction>getAllContentsOfType(model, ProcessReaction.class)).getCommands());
      Assertions.assertFalse(issued.getCommand().eIsProxy(), "the command did not resolve");
      Assertions.assertEquals("RecordMembership", issued.getCommand().getName());
      Assertions.assertEquals("household", issued.getAddressee());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * Every model written before 'addressed-by' existed still parses, and means what it meant.
   */
  @Test
  public void testWithoutAddressedByNothingChanges() {
    try {
      final DomainModel model = this.parseHelper.parse(this.model("PersonId", "issues-commands Notify, RecordMembership"));
      this._validationTestHelper.assertNoErrors(model);
      final EList<IssuedCommand> commands = IterableExtensions.<ProcessReaction>head(EcoreUtil2.<ProcessReaction>getAllContentsOfType(model, ProcessReaction.class)).getCommands();
      final Function1<IssuedCommand, String> _function = (IssuedCommand it) -> {
        return it.getCommand().getName();
      };
      Assertions.assertEquals(Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("Notify", "RecordMembership")), ListExtensions.<IssuedCommand, String>map(commands, _function));
      final Function1<IssuedCommand, Boolean> _function_1 = (IssuedCommand it) -> {
        String _addressee = it.getAddressee();
        return Boolean.valueOf((_addressee == null));
      };
      Assertions.assertTrue(IterableExtensions.<IssuedCommand>forall(commands, _function_1));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * Only some commands of a list may be addressed.
   */
  @Test
  public void testAddressedByIsPerCommand() {
    try {
      final DomainModel model = this.parseHelper.parse(this.model("PersonId", "issues-commands Notify, RecordMembership addressed-by household"));
      this._validationTestHelper.assertNoErrors(model);
      final EList<IssuedCommand> commands = IterableExtensions.<ProcessReaction>head(EcoreUtil2.<ProcessReaction>getAllContentsOfType(model, ProcessReaction.class)).getCommands();
      final Function1<IssuedCommand, String> _function = (IssuedCommand it) -> {
        return it.getAddressee();
      };
      Assertions.assertEquals(Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList((String)null, "household")), ListExtensions.<IssuedCommand, String>map(commands, _function));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * A component the event does not carry would address nothing - and issue nothing, silently.
   */
  @Test
  public void testTheAddresseeMustBeAnEventComponent() {
    try {
      this._validationTestHelper.assertError(this.parseHelper.parse(this.model("PersonId", "issues-commands RecordMembership addressed-by houshold")), 
        CqrsDslPackage.Literals.ISSUED_COMMAND, 
        CqrsDslValidator.PROCESS_ADDRESSEE_UNKNOWN, 
        "The addressee \'houshold\' is not an attribute of event \'MembershipRequested\'");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * A command without a target has no instance to address - a command handler decides.
   */
  @Test
  public void testATargetlessCommandHasNothingToAddress() {
    try {
      this._validationTestHelper.assertError(this.parseHelper.parse(this.model("PersonId", "issues-commands Notify addressed-by household")), 
        CqrsDslPackage.Literals.ISSUED_COMMAND, 
        CqrsDslValidator.PROCESS_ADDRESSEE_WITHOUT_TARGET, 
        "Command \'Notify\' has no target");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * Aimed at the aggregate the instance-key identifies, the command is addressed already.
   */
  @Test
  public void testASecondAddressIsRefused() {
    try {
      this._validationTestHelper.assertError(this.parseHelper.parse(this.model("HouseholdId", "issues-commands RecordMembership addressed-by household")), 
        CqrsDslPackage.Literals.ISSUED_COMMAND, 
        CqrsDslValidator.PROCESS_ADDRESSEE_REDUNDANT, 
        "already goes to the \'Household\' the instance-key identifies");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * The component names an instance of the wrong aggregate.
   */
  @Test
  public void testTheAddresseeMustIdentifyTheTargetAggregate() {
    try {
      this._validationTestHelper.assertError(this.parseHelper.parse(this.model("PersonId", "issues-commands RecordMembership addressed-by id")), 
        CqrsDslPackage.Literals.ISSUED_COMMAND, 
        CqrsDslValidator.PROCESS_ADDRESSEE_WRONG_AGGREGATE, 
        "The addressee \'id\' identifies a \'Person\', but command \'RecordMembership\' goes to a \'Household\'");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  private String model(final String instanceKey, final String issues) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("context c {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("module m {");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("type UUID");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("aggregate-id PersonId identifies Person {}");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("aggregate-id HouseholdId identifies Household {}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("/** A person. */");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("aggregate Person identifier PersonId {");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("/** Asks to become a member. */");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("method requestMembership fires MembershipRequested {");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("HouseholdId household");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("/** A household. */");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("aggregate Household identifier HouseholdId {");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("/** Records a member. */");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("method recordMembership {");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("PersonId person");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("/** A person asked to become a member of a household. */");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("event MembershipRequested {");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("/** The person. */");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("PersonId id");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("/** The household. */");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("HouseholdId household");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("/** Records the membership at the household. */");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("command RecordMembership target Household.recordMembership {");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("/** Notifies someone; no target. */");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("command Notify {");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("/** Whom. */");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("UUID whom");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("/** Keeps memberships in step. */");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("process-manager MembershipProcess {");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("instance-key ");
    _builder.append(instanceKey, "\t\t\t");
    _builder.newLineIfNotEmpty();
    _builder.append("\t\t\t");
    _builder.append("/** A membership was requested. */");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("reacts-to MembershipRequested {");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("correlate-by id");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append(issues, "\t\t\t\t");
    _builder.newLineIfNotEmpty();
    _builder.append("\t\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder.toString();
  }
}
