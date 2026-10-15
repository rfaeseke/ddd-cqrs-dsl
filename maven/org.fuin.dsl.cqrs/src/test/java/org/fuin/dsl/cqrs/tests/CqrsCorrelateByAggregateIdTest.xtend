package org.fuin.dsl.cqrs.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.fuin.dsl.cqrs.cqrsDsl.CqrsDslPackage
import org.fuin.dsl.cqrs.cqrsDsl.DomainModel
import org.fuin.dsl.cqrs.validation.CqrsDslValidator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

/**
 * Verifies what 'correlate-by' may name on an event that comes from an aggregate's method: the
 * aggregate's identifier as 'id', although the event declares no such attribute.
 */
@ExtendWith(InjectionExtension)
@InjectWith(CqrsDslInjectorProvider)
class CqrsCorrelateByAggregateIdTest {

	@Inject
	ParseHelper<DomainModel> parseHelper

	@Inject
	extension ValidationTestHelper

	/** The event copies the method's attributes, so it belongs to the aggregate and 'id' is its identifier. */
	@Test
	def void testIdIsTheAggregateIdOfAnEventFromAMethod() {
		parseHelper.parse(model("copies-attributes-of Thing.rename", "id")).assertNoErrors
	}

	/** Any other name still has to be an attribute or a parameter. */
	@Test
	def void testAnUnknownKeyIsStillRefused() {
		parseHelper.parse(model("copies-attributes-of Thing.rename", "nope")).assertError(
			CqrsDslPackage.Literals.PROCESS_REACTION,
			CqrsDslValidator.PROCESS_CORRELATION_KEY_UNKNOWN,
			"The correlation key 'nope' is not an attribute of event 'ThingRenamed'"
		)
	}

	/** An event with no origin has no aggregate to speak for it: 'id' must be declared. */
	@Test
	def void testWithoutAnOriginIdMustBeDeclared() {
		parseHelper.parse(model("", "id")).assertError(
			CqrsDslPackage.Literals.PROCESS_REACTION,
			CqrsDslValidator.PROCESS_CORRELATION_KEY_UNKNOWN,
			"The correlation key 'id' is not an attribute of event 'ThingRenamed'"
		)
	}

	private def String model(String origin, String key) '''
		context c {
			module m {
				type String

				aggregate-id ThingId identifies Thing {}

				/** A thing. */
				aggregate Thing identifier ThingId {
					/** Renames it. */
					method rename fires ThingRenamed {
						String name
					}
				}

				/** A thing was renamed. */
				event ThingRenamed «origin» {
				}

				/** Keeps things in step. */
				process-manager ThingProcess {
					instance-key ThingId
					/** A thing was renamed. */
					reacts-to ThingRenamed {
						correlate-by «key»
					}
				}
			}
		}
	'''

}
