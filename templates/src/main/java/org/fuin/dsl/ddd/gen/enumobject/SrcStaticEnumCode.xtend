package org.fuin.dsl.ddd.gen.enumobject

import java.util.ArrayList
import java.util.List
import org.fuin.dsl.cqrs.cqrsDsl.Attribute
import org.fuin.dsl.cqrs.cqrsDsl.EnumInstance
import org.fuin.dsl.cqrs.cqrsDsl.EnumObject
import org.fuin.dsl.cqrs.cqrsDsl.ExternalType
import org.fuin.dsl.cqrs.cqrsDsl.Variable
import org.fuin.dsl.ddd.gen.base.SrcInvokeGetter
import org.fuin.srcgen4j.core.emf.CodeSnippet
import org.fuin.srcgen4j.core.emf.CodeSnippetContext

import org.fuin.dsl.cqrs.extensions.CqrsCollectionExtensions
import static extension org.fuin.dsl.cqrs.extensions.CqrsCollectionExtensions.*
import static extension org.fuin.dsl.ddd.gen.extensions.TypeExtensions.*

/**
 * Creates static source code for an enumeration.
 */
class SrcStaticEnumCode implements CodeSnippet {

    val CodeSnippetContext ctx
    val String className
    val List<Attribute> attributes
    val Variable baseVar
    val List<EnumInstance> instances
    val ExternalType base
    val boolean javaEnum

    /**
     * Static code of an enumeration emitted as a final class.
     */
    new(CodeSnippetContext ctx, EnumObject enumObject) {
        this(ctx, enumObject, false)
    }

    /**
     * Static code of an enumeration.
     *
     * @param javaEnum Whether the enumeration is emitted as a Java {@code enum}: such a type already has the static
     *        {@code valueOf(String)} and {@code values()} of {@link Enum}, which decides two names below.
     */
    new(CodeSnippetContext ctx, EnumObject enumObject, boolean javaEnum) {
        this.ctx = ctx
        this.className = enumObject.name
        this.attributes = enumObject.attributes
        // Workaround for "getFirst()" default method in List that collides with extension
        this.baseVar = CqrsCollectionExtensions.<Attribute>first(attributes.nullSafe)
        this.instances = enumObject.instances
        this.base = enumObject.base
        this.javaEnum = javaEnum
        ctx.requiresImport("org.jspecify.annotations.Nullable")
        ctx.requiresImport(List.name)
    }
    
    /*
     * "isValid" and "valueOf" compare from the parameter rather than from the getter: the parameter is
     * the wrapper type and is known to be non-null at that point, whereas the getter may return a
     * primitive - and a non-optional "int" has no "equals" to call.
     */
    /**
     * Name of one of the instance lists: the plain name, unless an instance has it - {@code ALL(0)} of a
     * "who counts" enumeration is a fair model and the list beside it must not take its name away. Only such an
     * enumeration gets {@code ALL_INSTANCES} (and so on); every other keeps {@code ALL}.
     */
    def String listName(String plain) {
        if (instances.exists[name == plain]) plain + "_INSTANCES" else plain
    }

    /**
     * Name of the lookup by the base value. A Java {@code enum} has {@code valueOf(String)} already (by the constant's
     * NAME), so a base of {@code String} would declare it twice; there the lookup by the base value is
     * {@code valueOfBase}. Every other case keeps {@code valueOf}.
     */
    def String valueOfName() {
        if (javaEnum && base !== null && base.simpleName(ctx) == "String") "valueOfBase" else "valueOf"
    }

    override toString() {
        val all = listName("ALL")
        '''    
        /** All instances. */
        public static final List<«className»> «all» = List.of(
            «FOR in : instances SEPARATOR ", "»«in.name»«ENDFOR»
        );
        
        /** Valid instances. */
        public static final List<«className»> «listName("VALID")» = List.of(
            «FOR in : instances.valid SEPARATOR ", "»«in.name»«ENDFOR»
        );
        
        /** Deprecated instances. */
        public static final List<«className»> «listName("DEPRECATED")» = List.of(
            «FOR in : instances.deprecated SEPARATOR ", "»«in.name»«ENDFOR»
        );
        
        «IF base !== null»
        /**
         * Determines if it's possible to return an enumeration instance for the
         * given value.
         * 
         * @param value
         *            Value to check.
         * 
         * @return TRUE if the {@link #«valueOfName»(«base.simpleName(ctx)»)} will return a value else
         *         FALSE.
         */
        public static boolean isValid(@Nullable final «base.simpleName(ctx)» value) {
            if (value == null) {
                return true;
            }
            for (final «className» v : «all») {
                if (value.equals(«new SrcInvokeGetter(ctx, "v", baseVar)»)) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Returns an enumeration instance for the given value. Throws an
         * {@link IllegalArgumentException} if the value is invalid.
         * 
         * @param value
         *            Value to check.
         * 
         * @return Instance
         */
        @Nullable
        public static «className» «valueOfName»(@Nullable final «base.simpleName(ctx)» value) {
            if (value == null) {
                return null;
            }
            for (final «className» v : «all») {
                if (value.equals(«new SrcInvokeGetter(ctx, "v", baseVar)»)) {
                    return v;
                }
            }
            throw new IllegalArgumentException("Unknown value: " + value);
        }
        
        «ENDIF»
        '''
    }

    def private static List<EnumInstance> valid(List<EnumInstance> all) {
        val List<EnumInstance> list = new ArrayList<EnumInstance>()
        for (instance : all) {
            if (instance.deprecated === null) {
                list.add(instance)
            }
        }
        return list
    }

    def private static List<EnumInstance> deprecated(List<EnumInstance> all) {
        val List<EnumInstance> list = new ArrayList<EnumInstance>()
        for (instance : all) {
            if (instance.deprecated !== null) {
                list.add(instance)
            }
        }
        return list
    }

}
