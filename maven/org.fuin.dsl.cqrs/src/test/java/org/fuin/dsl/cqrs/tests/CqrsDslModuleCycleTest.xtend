package org.fuin.dsl.cqrs.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.fuin.dsl.cqrs.cqrsDsl.CqrsDslPackage
import org.fuin.dsl.cqrs.cqrsDsl.DomainModel
import org.fuin.dsl.cqrs.validation.CqrsDslValidator
import org.eclipse.xtext.diagnostics.Severity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Verifies the rule that refuses a dependency cycle between modules.
 *
 * <p>The edges are <b>resolved cross references</b>, not {@code import} lines, so the fixtures below
 * are deliberately written to pull the two apart: one has modules importing each other while only one
 * direction is actually referenced, which must stay clean.</p>
 */
@ExtendWith(InjectionExtension)
@InjectWith(CqrsDslInjectorProvider)
class CqrsDslModuleCycleTest {

	@Inject ParseHelper<DomainModel> parseHelper
	@Inject ValidationTestHelper validationHelper

	/** Two modules whose value objects reference each other close a cycle. */
	@Test
	def void mutualReferenceIsAnError() {
		val model = parseHelper.parse('''
			context p {
				module types {
					type String
				}
				module a {
					import p.types.*
					import p.b.*

					value-object AThing {
						BThing other
					}
				}
				module b {
					import p.types.*
					import p.a.*

					value-object BThing {
						AThing other
					}
				}
			}
		''')
		validationHelper.assertError(model, CqrsDslPackage.Literals.MODULE,
			CqrsDslValidator.MODULE_DEPENDENCY_CYCLE)
	}

	/** The same cycle is only a warning when the build asks for it with the system property. */
	@Test
	def void cycleIsAWarningWhenTheBuildAsksForIt() {
		val model = parseHelper.parse('''
			context p {
				module types {
					type String
				}
				module a {
					import p.types.*
					import p.b.*

					value-object AThing {
						BThing other
					}
				}
				module b {
					import p.types.*
					import p.a.*

					value-object BThing {
						AThing other
					}
				}
			}
		''')
		System.setProperty(CqrsDslValidator.MODULE_CYCLE_SEVERITY_PROPERTY, "warning")
		try {
			validationHelper.assertWarning(model, CqrsDslPackage.Literals.MODULE,
				CqrsDslValidator.MODULE_DEPENDENCY_CYCLE)
			assertTrue(validationHelper.validate(model).filter[
				code == CqrsDslValidator.MODULE_DEPENDENCY_CYCLE && severity == Severity.ERROR
			].empty)
		} finally {
			System.clearProperty(CqrsDslValidator.MODULE_CYCLE_SEVERITY_PROPERTY)
		}
	}

	/** One direction only is a dependency, not a cycle - even though both modules import each other. */
	@Test
	def void oneDirectionIsNotACycle() {
		val model = parseHelper.parse('''
			context p {
				module types {
					type String
				}
				module a {
					import p.types.*
					import p.b.*

					value-object AThing {
						BThing other
					}
				}
				module b {
					import p.types.*
					import p.a.*

					value-object BThing {
						String value
					}
				}
			}
		''')
		// "b" imports "a" and refers to nothing in it. Reading imports as dependencies would call this
		// a cycle; reading cross references does not.
		validationHelper.assertNoIssues(model, CqrsDslPackage.Literals.MODULE)
	}

	/** A module referring to its own types is not a cycle - an edge to itself is not an edge. */
	@Test
	def void selfReferenceIsNotACycle() {
		val model = parseHelper.parse('''
			context p {
				module solo {
					type String

					value-object Inner {
						String value
					}

					value-object Outer {
						Inner inner
					}
				}
			}
		''')
		validationHelper.assertNoIssues(model, CqrsDslPackage.Literals.MODULE)
	}

	/** A three module ring is reported too, not only the two module case. */
	@Test
	def void longerRingIsAnError() {
		val model = parseHelper.parse('''
			context p {
				module types {
					type String
				}
				module a {
					import p.types.*
					import p.c.*
					value-object AThing {
						CThing other
					}
				}
				module b {
					import p.types.*
					import p.a.*
					value-object BThing {
						AThing other
					}
				}
				module c {
					import p.types.*
					import p.b.*
					value-object CThing {
						BThing other
					}
				}
			}
		''')
		validationHelper.assertError(model, CqrsDslPackage.Literals.MODULE,
			CqrsDslValidator.MODULE_DEPENDENCY_CYCLE)
	}

}
