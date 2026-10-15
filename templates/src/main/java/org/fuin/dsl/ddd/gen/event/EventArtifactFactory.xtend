package org.fuin.dsl.ddd.gen.event

import java.util.Map
import org.fuin.dsl.cqrs.cqrsDsl.AbstractEntity
import org.fuin.dsl.cqrs.cqrsDsl.AbstractEntityId
import org.eclipse.emf.ecore.EObject
import org.fuin.dsl.cqrs.cqrsDsl.Event
import org.fuin.dsl.ddd.gen.base.AbstractSource
import org.fuin.dsl.ddd.gen.base.SrcAll
import org.fuin.dsl.ddd.gen.base.SrcGetters
import org.fuin.dsl.ddd.gen.base.SrcJavaDocType
import org.fuin.dsl.ddd.gen.base.SrcParamsAssignment
import org.fuin.dsl.ddd.gen.base.SrcParamsDecl
import org.fuin.dsl.ddd.gen.base.SrcVarsDecl
import org.fuin.dsl.ddd.gen.base.SrcXmlRootElement
import org.fuin.srcgen4j.commons.GenerateException
import org.fuin.srcgen4j.commons.GeneratedArtifact
import org.fuin.srcgen4j.core.emf.CodeReferenceRegistry
import org.fuin.srcgen4j.core.emf.CodeSnippetContext
import org.fuin.srcgen4j.core.emf.SimpleCodeSnippetContext

import static extension org.fuin.dsl.cqrs.extensions.CqrsAbstractEntityExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsAbstractElementExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsCollectionExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsEventExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsStringExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsVariableExtensions.*
import static extension org.fuin.dsl.ddd.gen.extensions.EventExtensions.*
import static extension org.fuin.dsl.ddd.gen.extensions.MapExtensions.*
import java.util.List
import java.io.Serial
import org.fuin.dsl.ddd.gen.base.TypeKeys
import java.time.ZonedDateTime
import static extension org.fuin.dsl.ddd.gen.extensions.JavaStringExtensions.*

class EventArtifactFactory extends AbstractSource<Event> {

    /** DSL annotations that map to an equally named marker interface in "org.fuin.ddd4j.core". */
    static val EVENT_CATEGORY_ANNOTATIONS = #["GenesisEvent", "ExileEvent", "ReturnFromExileEvent", "ExodusEvent"]

    override getModelType() {
        typeof(Event)
    }

    override getTypeKey() {
        TypeKeys.JAVA_EVENT
    }

    override create(Event event, Map<String, Object> context, boolean preparationRun) throws GenerateException {

        val AbstractEntity entity = event.entity;
        val className = event.getName()
        // The module is optional: derive the package from the element itself - the enclosing
        // entity for a domain event, otherwise the event.
        val EObject owner = if (entity === null) event else entity
        val pkg = owner.asPackage
        val fqn = pkg + "." + className
        val filename = fqn.replace('.', '/') + ".java";

        val CodeReferenceRegistry refReg = context.codeReferenceRegistry
        refReg.putReference(TypeKeys.refKey(event), fqn)

        if (preparationRun) {

            // No code generation during preparation phase
            return null
        }

        val SimpleCodeSnippetContext ctx = new SimpleCodeSnippetContext(refReg)
        ctx.addImports(entity, event)
        ctx.addReferences(event)

        var String src;
        if (entity === null) {
            src = createStandardEvent(ctx, event, pkg, className).toString();
        } else {
            src = createDomainEvent(ctx, event, pkg, className).toString();
        }

        return List.of(newArtifact(filename, src.getBytes("UTF-8"), owner));
    }

    /**
     * Returns the names of the "org.fuin.ddd4j.core" marker interfaces the event should implement,
     * based on the DSL annotations attached to it. The order follows EVENT_CATEGORY_ANNOTATIONS and
     * is therefore independent of the order the annotations appear in the model.
     */
    def List<String> eventCategories(Event event) {
        EVENT_CATEGORY_ANNOTATIONS.filter[category|
            event.annotations.nullSafe.exists[annotation?.name == category]
        ].toList
    }

    /** Returns the "implements" clause for the event or an empty string if it has no categories. */
    def String implementsClause(Event event) {
        val categories = event.eventCategories
        if (categories.empty) "" else " implements " + categories.join(", ")
    }

    def addImports(CodeSnippetContext ctx, AbstractEntity entity, Event event) {
        ctx.requiresImport("org.fuin.ddd4j.core.EventType")
        ctx.requiresImport("org.fuin.esc.api.HasSerializedDataTypeConstant")
        ctx.requiresImport("org.fuin.esc.api.SerializedDataType")
        ctx.requiresImport(Serial.name)
        for (category : event.eventCategories) {
            ctx.requiresImport("org.fuin.ddd4j.core." + category)
        }

        if (entity === null) {
	        if (options.jsonb) {
	            ctx.requiresImport("org.fuin.ddd4j.jsonb.AbstractEvent")        
	        }
	        if (options.jaxb) {
	            ctx.requiresImport("org.fuin.ddd4j.jaxb.AbstractEvent")        
	        }
	        if (options.jackson) {
	            ctx.requiresImport("org.fuin.ddd4j.jackson.AbstractEvent")        
	        }
            // toString() renders KeyValueEL.replace(...) over the same variables the constructor
            // uses (see EventExtensions.eventVariables). Guard the imports on that same set, not on
            // event.attributes alone.
            val variables = event.eventVariables
            if (variables.nullSafe.size > 0) {
                ctx.requiresImport("org.fuin.objects4j.core.KeyValue")
                ctx.requiresImport("org.fuin.objects4j.core.KeyValueEL")
                ctx.requiresImport("java.util.Objects")
            }
        } else {
	        if (options.jsonb) {
	            ctx.requiresImport("org.fuin.ddd4j.jsonb.AbstractDomainEvent")        
	        }
	        if (options.jaxb) {
	            ctx.requiresImport("org.fuin.ddd4j.jaxb.AbstractDomainEvent")        
	        }
	        if (options.jackson) {
	            ctx.requiresImport("org.fuin.ddd4j.jackson.AbstractDomainEvent")        
	        }
            ctx.requiresImport("org.fuin.objects4j.core.KeyValue")
            ctx.requiresImport("org.fuin.objects4j.core.KeyValueEL")
            ctx.requiresImport("java.util.Objects")
            ctx.requiresImport("org.fuin.ddd4j.core.EventId")
            ctx.requiresImport(ZonedDateTime.name)
        }
    }

    def addReferences(CodeSnippetContext ctx, Event event) {    	
        if (event.entity !== null) {
            ctx.requiresReference(TypeKeys.refKey(event.entityIdType))
        }
    }

    def AbstractEntityId getEntityIdType(Event event) {
        if (event.entity === null) {
            return null
        }
        return event.entity.idType
    }

    def createDomainEvent(SimpleCodeSnippetContext ctx, Event event, String pkg, String className) {
    	var variables = event.eventVariables
        val String src = ''' 
            «new SrcJavaDocType(event)»
            «IF options.jaxb»
            «new SrcXmlRootElement(ctx, event.name)»
            «ENDIF»
            @HasSerializedDataTypeConstant
            public final class «className» extends AbstractDomainEvent<«event.entityIdType.name»>«event.implementsClause» {
            
                @Serial
                private static final long serialVersionUID = 1000L;
            
                /** Unique name used to store the event. */
                public static final EventType EVENT_TYPE = new EventType("«event.name»");
                
                /**
                 * Type used to look up the serializer and deserializer. The event store registry is built
                 * by scanning for the annotation above, so without this constant the type is unknown at
                 * runtime and neither storing nor reading it back works.
                 */
                public static final SerializedDataType SER_TYPE = new SerializedDataType(EVENT_TYPE.asBaseType());
                
                «new SrcVarsDecl(ctx, "private", options, event, true)»
            
                «IF variables.nullSafe.size > 0»
                    /**
                     * Protected default constructor for deserialization and the builder.
                     */
                    @SuppressWarnings("NullAway.Init")
                    protected «event.name»() {
                        super();
                    }
                    
                «ENDIF»
                @Override
                public EventType getEventType() {
                    return EVENT_TYPE;
                }
            
                «new SrcGetters(ctx, options, "public", variables)»
            
                @Override
                public String toString() {
                    return Objects.requireNonNull(KeyValueEL.replace("«event.message.javaString»",
                        new KeyValue("entityIdPath", getEntityIdPath())
                        «FOR v : variables»
                            , new KeyValue("«v.name»", «v.name»)
                        «ENDFOR»
                    ));
                }
                
                /**
                 * Creates a new builder instance.
                 *
                 * @return New builder instance.
                 */
                public static Builder builder() {
                    return new Builder();
                }
                
                «new SrcEventBuilder(ctx, options, event)»
            }
            
        '''

        new SrcAll(ctx, copyrightHeader, pkg, ctx.imports, src).toString

    }

    def createStandardEvent(SimpleCodeSnippetContext ctx, Event event, String pkg, String className) {
    	var variables = event.eventVariables
        val String src = ''' 
            «new SrcJavaDocType(event)»
            «IF options.jaxb»
            «new SrcXmlRootElement(ctx, event.name)»
            «ENDIF»
            @HasSerializedDataTypeConstant
            public final class «className» extends AbstractEvent«event.implementsClause» {
            
                @Serial
                private static final long serialVersionUID = 1000L;
            
                /** Unique name used to store the event. */
                public static final EventType EVENT_TYPE = new EventType("«event.name»");
                
                /**
                 * Type used to look up the serializer and deserializer. The event store registry is built
                 * by scanning for the annotation above, so without this constant the type is unknown at
                 * runtime and neither storing nor reading it back works.
                 */
                public static final SerializedDataType SER_TYPE = new SerializedDataType(EVENT_TYPE.asBaseType());
                
                «new SrcVarsDecl(ctx, "private", options, event)»
            
                «IF variables.nullSafe.size > 0»
                    /**
                     * Protected default constructor for deserialization.
                     */
                    @SuppressWarnings("NullAway.Init")
                    protected «event.name»() {
                        super();
                    }
                    
                «ENDIF»
                /**
                 * «event.doc.text»
                 *
                «FOR v : variables»
                    * @param «v.name» «v.superDoc» 
                «ENDFOR»
                */
                public «event.name»(«new SrcParamsDecl(ctx, options, variables.asParameters)») {
                    super();
                    «new SrcParamsAssignment(ctx, variables.asParameters)»
                }
            
                @Override
                public EventType getEventType() {
                    return EVENT_TYPE;
                }
            
                «new SrcGetters(ctx, options, "public", variables)»
            
                @Override
                public String toString() {
                    «IF variables.nullSafe.size == 0»
                        return "«event.message.javaString»";
                    «ELSE»
                        return Objects.requireNonNull(KeyValueEL.replace("«event.message.javaString»"
                        «FOR v : variables»
                            , new KeyValue("«v.name»", «v.name»)
                        «ENDFOR»
                        ));
                    «ENDIF»
                }
                
            }
        '''

        new SrcAll(ctx, copyrightHeader, pkg, ctx.imports, src).toString

    }

}
