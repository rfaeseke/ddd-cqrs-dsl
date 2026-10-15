package org.fuin.dsl.ddd.gen.entityid

import java.util.Map
import org.fuin.dsl.cqrs.cqrsDsl.EntityId
import org.fuin.dsl.ddd.gen.base.AbstractSource
import org.fuin.dsl.ddd.gen.base.GenerateOptions
import org.fuin.dsl.ddd.gen.base.SrcAll
import org.fuin.dsl.ddd.gen.base.SrcConstructorsWithParamsAssignment
import org.fuin.dsl.ddd.gen.base.SrcIdStringMethods
import org.fuin.dsl.ddd.gen.base.SrcJavaDocType
import org.fuin.dsl.ddd.gen.base.SrcVoBaseMethods
import org.fuin.srcgen4j.commons.GenerateException
import org.fuin.srcgen4j.commons.GeneratedArtifact
import org.fuin.srcgen4j.core.emf.CodeReferenceRegistry
import org.fuin.srcgen4j.core.emf.CodeSnippetContext
import org.fuin.srcgen4j.core.emf.SimpleCodeSnippetContext

import static extension org.fuin.dsl.cqrs.extensions.CqrsAbstractElementExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsCollectionExtensions.*
import static extension org.fuin.dsl.ddd.gen.extensions.MapExtensions.*
import org.fuin.dsl.ddd.gen.base.TypeKeys
import java.util.List

class FinalEntityIdArtifactFactory extends AbstractSource<EntityId> {

    override getModelType() {
        typeof(EntityId)
    }

    override getTypeKey() {
        TypeKeys.JAVA_ENTITY_ID
    }

    override create(EntityId entityId, Map<String, Object> context, boolean preparationRun) throws GenerateException {

        val className = entityId.name
        val abstractClassName = entityId.abstractName
        val pkg = entityId.asPackage
        val fqn = pkg + "." + className
        val filename = fqn.replace('.', '/') + ".java";

        val CodeReferenceRegistry refReg = context.codeReferenceRegistry
        refReg.putReference(TypeKeys.refKey(entityId), fqn)

        if (preparationRun) {

            // No code generation during preparation phase
            return null
        }

        val SimpleCodeSnippetContext ctx = new SimpleCodeSnippetContext(refReg)
        ctx.addImports(entityId)
        ctx.addReferences(entityId)

        return List.of(newArtifact(filename,
            create(ctx, entityId, pkg, className, abstractClassName).toString().getBytes("UTF-8"), entityId));
    }

    def addImports(CodeSnippetContext ctx, EntityId entityId) {
        ctx.requiresImport("java.io.Serial")
        if (entityId.base !== null && options.jaxb) {
            ctx.requiresImport("jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter")
        }
        ctx.requiresImport("javax.annotation.concurrent.Immutable")
        if (entityId.registeredByJandex) {
            ctx.requiresImport("org.fuin.ddd4j.core.HasEntityTypeConstant")
            ctx.requiresImport("org.fuin.objects4j.common.HasPublicStaticIsValidMethod")
            ctx.requiresImport("org.fuin.objects4j.common.HasPublicStaticValueOfMethod")
            ctx.requiresImport("org.jspecify.annotations.Nullable")
        }
    }

    /**
     * Whether the id is one {@code JandexEntityIdFactory} can find - see {@code FinalAggregateIdArtifactFactory}.
     * A {@code base Integer} or {@code base UUID} entity id takes the simple factory, which always had the three
     * annotations; this is the same for the other bases whose {@code valueOf(String)} the generated class has.
     */
    def boolean registeredByJandex(EntityId id) {
        id.base !== null && #["Long", "String"].contains(id.base.name)
    }

    def addReferences(CodeSnippetContext ctx, EntityId entityId) {
        if (entityId.base !== null && options.jaxb) {
            // The "Converter" is a nested class of the generated id itself - no import needed.
        }
        ctx.requiresReference(TypeKeys.refKey(entityId, TypeKeys.JAVA_ENTITY_ID_ABSTRACT))
    }

    def create(SimpleCodeSnippetContext ctx, EntityId id, String pkg, String className, String abstractClassName) {
        val String src = ''' 
            «val idStrings = new SrcIdStringMethods(ctx, className, abstractClassName, id.attributes)»
            «new SrcJavaDocType(id)»
            @Immutable«IF id.base === null && idStrings.supported»
            «idStrings.annotations»«ENDIF»
            «IF id.registeredByJandex»
            @HasEntityTypeConstant
            @HasPublicStaticIsValidMethod
            @HasPublicStaticValueOfMethod
            «ENDIF»
            «IF id.base !== null && options.jaxb»
            @XmlJavaTypeAdapter(«id.name»Converter.class)
            «ENDIF»
            public final class «className» extends «abstractClassName» {
            
                @Serial
                private static final long serialVersionUID = 1000L;
                
                «new SrcConstructorsWithParamsAssignment(ctx, GenerateOptions.empty(), id, false, true)»
                «IF id.base === null && id.attributes.nullSafe.size == 1»
                @Override
                public final String asString() {
                    return "" + get«id.attributes.first.name.toFirstUpper»();
                }

                «ENDIF»
                «new SrcVoBaseMethods(ctx, id)»
                «IF id.base === null»
                    «idStrings»
                «ENDIF»
                «IF id.registeredByJandex && id.base.name != "String"»
                /**
                 * Returns the information if a given String can be converted into an instance of «className»: the
                 * check {@code JandexEntityIdFactory} calls beside {@link #valueOf(String)}. A <code>null</code> value
                 * returns <code>true</code>.
                 * 
                 * @param value
                 *            Value to check.
                 * 
                 * @return TRUE if it's a valid «id.base.name», else FALSE.
                 */
                public static boolean isValid(@Nullable final String value) {
                    if (value == null) {
                        return true;
                    }
                    try {
                        «id.base.name».valueOf(value);
                    } catch (final NumberFormatException ex) {
                        return false;
                    }
                    return true;
                }

                «ENDIF»
            }
            '''

        new SrcAll(ctx, copyrightHeader, pkg, ctx.imports, src).toString

    }

}
