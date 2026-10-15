package org.fuin.dsl.ddd.gen.enumobject

import java.util.HashMap
import jakarta.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.fuin.dsl.cqrs.cqrsDsl.DomainModel
import org.fuin.dsl.cqrs.cqrsDsl.EnumObject
import org.fuin.dsl.ddd.gen.base.GenerateOptions
import org.fuin.dsl.ddd.gen.base.Utils
import org.fuin.dsl.cqrs.tests.CqrsDslInjectorProvider
import org.fuin.srcgen4j.commons.ArtifactFactoryConfig
import org.fuin.srcgen4j.commons.DefaultContext
import org.fuin.srcgen4j.commons.Variable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

import static org.assertj.core.api.Assertions.*

import static extension org.fuin.dsl.cqrs.extensions.CqrsDomainModelExtensions.*
import static extension org.fuin.dsl.ddd.gen.base.TestExtensions.*
import static extension org.fuin.dsl.ddd.gen.extensions.MapExtensions.*

/**
 * A Java {@code enum} already has {@code valueOf(String)} (by name) and the generated code adds the lists
 * {@code ALL}/{@code VALID}/{@code DEPRECATED}: a model may name an instance like one of them, or give the enumeration
 * a base of {@code String}, and the generated class must still compile.
 */
@InjectWith(typeof(CqrsDslInjectorProvider))
@ExtendWith(InjectionExtension)
class EnumNameCollisionTest {

    @Inject
    ParseHelper<DomainModel> parser

    @Inject
    ValidationTestHelper validationTester

    /** A base of String: the lookup by code may not be a second valueOf(String). */
    @Test
    def void testStringBaseLookupIsNotValueOf() {
        val result = create("StrEnum")
        assertThat(result).contains("public static StrEnum valueOfBase(@Nullable final String value) {")
        assertThat(result).doesNotContain("StrEnum valueOf(")
        assertThat(result).contains("{@link #valueOfBase(String)}")
    }

    /** A base other than String keeps valueOf: it is an overload there, not a duplicate. */
    @Test
    def void testIntegerBaseKeepsValueOf() {
        val result = create("IntEnum")
        assertThat(result).contains("public static IntEnum valueOf(@Nullable final Integer value) {")
        assertThat(result).doesNotContain("valueOfBase")
    }

    /** An instance named ALL keeps its name; the list beside it is ALL_INSTANCES, and the lookups walk that list. */
    @Test
    def void testInstanceNamedAllRenamesTheList() {
        val result = create("WhoEnum")
        assertThat(result).contains("public static final List<WhoEnum> ALL_INSTANCES = List.of(")
        assertThat(result).contains("public static final List<WhoEnum> VALID = List.of(")
        assertThat(result).contains("for (final WhoEnum v : ALL_INSTANCES) {")
        assertThat(result).doesNotContain("List<WhoEnum> ALL = ")
    }

    private def String create(String enumName) {
        val context = new HashMap<String, Object>()
        val refReg = context.codeReferenceRegistry
        refReg.putReference("p.x.types.String", "java.lang.String")
        refReg.putReference("p.x.types.Integer", "java.lang.Integer")
        val factory = new EnumArtifactFactory()
        val ArtifactFactoryConfig config = new ArtifactFactoryConfig("enumObject", EnumArtifactFactory.name, "module", "folder")
        config.addVariable(new Variable(GenerateOptions.KEY_BASE_PKG, EXAMPLES_CONCRETE))
        config.addVariable(new Variable(GenerateOptions.KEY_COPYRIGHT_HEADER, Utils.readAsString("required-header.txt")))
        config.init(new DefaultContext(), null)
        factory.init(config)
        val EnumObject enu = model.find(typeof(EnumObject), enumName)
        return new String(factory.create(enu, context, false).iterator().next().data)
    }

    private def model() {
        val DomainModel model = parser.parse('''
            context p {
                module x.types {
                    type String
                    type Integer
                }
                module x.enumobject {
                    import p.x.types.*

                    /** Base String. */
                    enum StrEnum base String {
                        /** Code. */
                        String code
                        instances {
                            /** One. */
                            ONE("1")
                            /** Two. */
                            TWO("2")
                        }
                    }

                    /** Base Integer. */
                    enum IntEnum base Integer {
                        /** Code. */
                        Integer code
                        instances {
                            /** One. */
                            ONE(1)
                        }
                    }

                    /** An instance named ALL. */
                    enum WhoEnum base Integer {
                        /** Code. */
                        Integer code
                        instances {
                            /** Everyone. */
                            ALL(0)
                            /** Some. */
                            SOME(1)
                        }
                    }
                }
            }
        ''')
        validationTester.assertNoErrors(model)
        return model
    }

}
