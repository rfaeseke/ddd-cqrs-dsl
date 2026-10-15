package org.fuin.dsl.ddd.gen.constr

import java.util.HashMap
import jakarta.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.fuin.dsl.cqrs.cqrsDsl.Constraint
import org.fuin.dsl.cqrs.cqrsDsl.DomainModel
import org.fuin.dsl.ddd.gen.base.GenerateOptions
import org.fuin.dsl.ddd.gen.base.Utils
import org.fuin.dsl.cqrs.tests.CqrsDslInjectorProvider
import org.fuin.srcgen4j.commons.ArtifactFactoryConfig
import org.fuin.srcgen4j.commons.DefaultContext
import org.fuin.srcgen4j.commons.Variable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

import static org.assertj.core.api.Assertions.*
import static org.fuin.dsl.ddd.gen.extensions.JavaStringExtensions.*

import static extension org.fuin.dsl.cqrs.extensions.CqrsDomainModelExtensions.*
import static extension org.fuin.dsl.ddd.gen.base.TestExtensions.*
import static extension org.fuin.dsl.ddd.gen.extensions.MapExtensions.*

/**
 * Model text arrives without its escapes; written between two quotes as it stands it ends the Java literal early.
 */
@InjectWith(typeof(CqrsDslInjectorProvider))
@ExtendWith(InjectionExtension)
class MessageLiteralEscapeTest {

    @Inject
    ParseHelper<DomainModel> parser

    @Inject
    ValidationTestHelper validationTester

    @Test
    def void testJavaString() {
        assertThat(javaString('say "hi"')).isEqualTo('say \\"hi\\"')
        assertThat(javaString('a\\b')).isEqualTo('a\\\\b')
        assertThat(javaString("line\nnext")).isEqualTo('line\\nnext')
        assertThat(javaString("plain")).isEqualTo("plain")
        assertThat(javaString(null)).isNull
    }

    @Test
    def void testConstraintMessageWithQuotes() {
        val context = new HashMap<String, Object>()
        val refReg = context.codeReferenceRegistry
        refReg.putReference("p.x.types.String", "java.lang.String")
        refReg.putReference("p.x.constr.QuotedValidator", "p.x.constr.QuotedValidator")
        val factory = new ValidatorAnnotationArtifactFactory()
        val ArtifactFactoryConfig config = new ArtifactFactoryConfig("validatorAnnotation", ValidatorAnnotationArtifactFactory.name, "module", "folder")
        config.addVariable(new Variable(GenerateOptions.KEY_BASE_PKG, EXAMPLES_CONCRETE))
        config.addVariable(new Variable(GenerateOptions.KEY_COPYRIGHT_HEADER, Utils.readAsString("required-header.txt")))
        config.init(new DefaultContext(), null)
        factory.init(config)
        val DomainModel model = parser.parse('''
            context p {
                module x.types {
                    type String
                }
                module x.constr {
                    import p.x.types.*
                    /** Quoted. */
                    constraint Quoted input String {
                        message "From \"von\" to \"bis\""
                    }
                }
            }
        ''')
        validationTester.assertNoErrors(model)
        val result = new String(factory.create(model.find(typeof(Constraint), "Quoted"), context, false).iterator().next().data)
        assertThat(result).contains('String message() default "From \\"von\\" to \\"bis\\"";')
    }

}
