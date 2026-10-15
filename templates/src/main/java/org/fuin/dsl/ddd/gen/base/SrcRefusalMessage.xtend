package org.fuin.dsl.ddd.gen.base

import jakarta.validation.constraints.NotNull
import org.fuin.dsl.cqrs.cqrsDsl.EnumObject
import org.fuin.dsl.cqrs.cqrsDsl.Exception
import org.fuin.objects4j.core.KeyValue
import org.fuin.srcgen4j.core.emf.CodeSnippet
import org.fuin.srcgen4j.core.emf.CodeSnippetContext

import static extension org.fuin.dsl.cqrs.extensions.CqrsCollectionExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsEObjectExtensions.*
import static extension org.fuin.dsl.ddd.gen.extensions.JavaStringExtensions.*

/**
 * The sentence a refusal carries, composed in the language the caller asked for.
 *
 * <p>A refusal is the one piece of model wording the <b>server</b> writes out: it is a sentence rather
 * than a label, built from the values it refused, and it reaches the client already written. Composing
 * it in whatever language the server's machine is set to is what leaves a single English line in an
 * otherwise translated screen.
 *
 * <p>So the template is looked up first and substituted after - a translation is a template too, and
 * the values it names are the same in every language. The model's own sentence travels beside the key
 * as the fallback, so a bundle that is not on the classpath and a key nobody translated are the same
 * non-event they are everywhere else in this generator.
 *
 * <p><b>An enumeration is named by its caption.</b> A placeholder is substituted from the value itself,
 * and an enum's {@code toString} answers its wire name - so {@code ${kind}} read "EXPENSE" where the
 * screen beside it said "Aufwand". Only a <em>bare</em> placeholder is wrapped: {@code ${provider.id}}
 * asks for a property of the value and has to keep the value to ask it of.
 *
 * <p>Events and commands compose their messages the same way and are deliberately left alone. An
 * event's message is an audit line, written once into the event store, and it must not depend on who
 * happened to trigger it; a command's is a confirmation the client renders from its own bundle.
 */
class SrcRefusalMessage implements CodeSnippet {

    static val WORDING = "org.fuin.dsl.cqrs.common.wording.Wording"

    val Exception ex

    new(@NotNull CodeSnippetContext ctx, @NotNull Exception ex) {
        this.ex = ex
        ctx.requiresImport(WORDING)
        if (ex.attributes.nullSafe.size > 0) {
            ctx.requiresImport(KeyValue.name)
            ctx.requiresImport("org.fuin.objects4j.core.KeyValueEL")
        }
    }

    override toString() {
        val template = '''Wording.message("«AbstractSource.bundleName(ex.module)»", "«ex.name»", "«ex.message.javaString»")'''
        if (ex.attributes.nullSafe.size == 0) {
            return template
        }
        return '''KeyValueEL.replace(«template», «FOR a : ex.attributes SEPARATOR ','» new KeyValue("«a.name»", «value(a.name, a.type instanceof EnumObject)»)«ENDFOR»)'''
    }

    /** The value a placeholder is substituted from - the caption where it is an enumeration. */
    def private String value(String name, boolean enumeration) {
        return if (enumeration && bare(name)) "Wording.of(" + name + ")" else name
    }

    /** Whether the message asks for the value itself rather than for a property of it. */
    def private boolean bare(String name) {
        return ex.message.contains("${" + name + "}")
    }

}
