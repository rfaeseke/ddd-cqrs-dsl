package org.fuin.dsl.cqrs.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.fuin.dsl.cqrs.cqrsDsl.CqrsDslPackage
import org.fuin.dsl.cqrs.cqrsDsl.DomainModel
import org.fuin.dsl.cqrs.cqrsDsl.ProcessReaction
import org.fuin.dsl.cqrs.validation.CqrsDslValidator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

import static extension org.eclipse.xtext.EcoreUtil2.getAllContentsOfType

/**
 * Verifies that a reaction can say which instance a command it issues goes to ('addressed-by').
 *
 * <p>'correlate-by' names the running process an event belongs to. Where a command then goes is a
 * second question: it coincides with the first only when the command's target is the aggregate the
 * 'instance-key' identifies. A process keyed by a person that records a membership at a household
 * has the household as a component of the event, and before 'addressed-by' no clause could point at it.
 */
@ExtendWith(InjectionExtension)
@InjectWith(CqrsDslInjectorProvider)
class CqrsIssuedCommandAddresseeTest {

	@Inject
	ParseHelper<DomainModel> parseHelper

	@Inject
	extension ValidationTestHelper

	/** The case the clause exists for: keyed by the person, addressed to the household. */
	@Test
	def void testACommandIsAddressedByAnEventComponent() {
		val model = parseHelper.parse(model("PersonId", "issues-commands RecordMembership addressed-by household"))
		model.assertNoErrors
		val issued = model.getAllContentsOfType(ProcessReaction).head.commands.head
		Assertions.assertFalse(issued.command.eIsProxy, "the command did not resolve")
		Assertions.assertEquals("RecordMembership", issued.command.name)
		Assertions.assertEquals("household", issued.addressee)
	}

	/** Every model written before 'addressed-by' existed still parses, and means what it meant. */
	@Test
	def void testWithoutAddressedByNothingChanges() {
		val model = parseHelper.parse(model("PersonId", "issues-commands Notify, RecordMembership"))
		model.assertNoErrors
		val commands = model.getAllContentsOfType(ProcessReaction).head.commands
		Assertions.assertEquals(#["Notify", "RecordMembership"], commands.map[command.name])
		Assertions.assertTrue(commands.forall[addressee === null])
	}

	/** Only some commands of a list may be addressed. */
	@Test
	def void testAddressedByIsPerCommand() {
		val model = parseHelper.parse(model("PersonId", "issues-commands Notify, RecordMembership addressed-by household"))
		model.assertNoErrors
		val commands = model.getAllContentsOfType(ProcessReaction).head.commands
		Assertions.assertEquals(#[null, "household"], commands.map[addressee])
	}

	/** A component the event does not carry would address nothing - and issue nothing, silently. */
	@Test
	def void testTheAddresseeMustBeAnEventComponent() {
		parseHelper.parse(model("PersonId", "issues-commands RecordMembership addressed-by houshold")).assertError(
			CqrsDslPackage.Literals.ISSUED_COMMAND,
			CqrsDslValidator.PROCESS_ADDRESSEE_UNKNOWN,
			"The addressee 'houshold' is not an attribute of event 'MembershipRequested'"
		)
	}

	/** A command without a target has no instance to address - a command handler decides. */
	@Test
	def void testATargetlessCommandHasNothingToAddress() {
		parseHelper.parse(model("PersonId", "issues-commands Notify addressed-by household")).assertError(
			CqrsDslPackage.Literals.ISSUED_COMMAND,
			CqrsDslValidator.PROCESS_ADDRESSEE_WITHOUT_TARGET,
			"Command 'Notify' has no target"
		)
	}

	/** Aimed at the aggregate the instance-key identifies, the command is addressed already. */
	@Test
	def void testASecondAddressIsRefused() {
		parseHelper.parse(model("HouseholdId", "issues-commands RecordMembership addressed-by household")).assertError(
			CqrsDslPackage.Literals.ISSUED_COMMAND,
			CqrsDslValidator.PROCESS_ADDRESSEE_REDUNDANT,
			"already goes to the 'Household' the instance-key identifies"
		)
	}

	/** The component names an instance of the wrong aggregate. */
	@Test
	def void testTheAddresseeMustIdentifyTheTargetAggregate() {
		parseHelper.parse(model("PersonId", "issues-commands RecordMembership addressed-by id")).assertError(
			CqrsDslPackage.Literals.ISSUED_COMMAND,
			CqrsDslValidator.PROCESS_ADDRESSEE_WRONG_AGGREGATE,
			"The addressee 'id' identifies a 'Person', but command 'RecordMembership' goes to a 'Household'"
		)
	}

	private def String model(String instanceKey, String issues) '''
		context c {
			module m {
				type UUID

				aggregate-id PersonId identifies Person {}
				aggregate-id HouseholdId identifies Household {}

				/** A person. */
				aggregate Person identifier PersonId {
					/** Asks to become a member. */
					method requestMembership fires MembershipRequested {
						HouseholdId household
					}
				}

				/** A household. */
				aggregate Household identifier HouseholdId {
					/** Records a member. */
					method recordMembership {
						PersonId person
					}
				}

				/** A person asked to become a member of a household. */
				event MembershipRequested {
					/** The person. */
					PersonId id
					/** The household. */
					HouseholdId household
				}

				/** Records the membership at the household. */
				command RecordMembership target Household.recordMembership {
				}

				/** Notifies someone; no target. */
				command Notify {
					/** Whom. */
					UUID whom
				}

				/** Keeps memberships in step. */
				process-manager MembershipProcess {
					instance-key «instanceKey»
					/** A membership was requested. */
					reacts-to MembershipRequested {
						correlate-by id
						«issues»
					}
				}
			}
		}
	'''

}
