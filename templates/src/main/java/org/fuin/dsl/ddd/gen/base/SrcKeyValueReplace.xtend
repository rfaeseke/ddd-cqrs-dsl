package org.fuin.dsl.ddd.gen.base

import java.util.List
import jakarta.validation.constraints.NotNull
import jakarta.annotation.Nullable
import org.fuin.objects4j.core.KeyValue
import org.fuin.srcgen4j.core.emf.CodeSnippet
import org.fuin.srcgen4j.core.emf.CodeSnippetContext

import static extension org.fuin.dsl.cqrs.extensions.CqrsCollectionExtensions.*
import static extension org.fuin.dsl.ddd.gen.extensions.JavaStringExtensions.*

/**
 * Creates source code for a {@link org.fuin.objects4j.core.KeyValueEL#replace(String, KeyValue...)}.
 * If the list of variable names is empty, the message will be returned without replacing code.
 */
class SrcKeyValueReplace implements CodeSnippet {

    val String message
    val List<String> variables

    new(@NotNull CodeSnippetContext ctx, @NotNull String message, @Nullable List<String> variables) {
        this.message = message
        this.variables = variables

        if (variables.nullSafe.size > 0) {
            ctx.requiresImport(KeyValue.name)
            ctx.requiresImport("org.fuin.objects4j.core.KeyValueEL")
        }
    }

    override toString() {
        if (variables.nullSafe.size == 0) {
            '''"«message.javaString»"'''
        } else {
            '''KeyValueEL.replace("«message.javaString»", «FOR name : variables SEPARATOR ','» new KeyValue("«name»", «name»)«ENDFOR»)'''
        }
    }

}
