package org.fuin.dsl.ddd.gen.command

import java.util.Map
import org.fuin.dsl.cqrs.cqrsDsl.AbstractEntity
import org.fuin.dsl.cqrs.cqrsDsl.AbstractEntityId
import org.eclipse.emf.ecore.EObject
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

import static extension org.fuin.dsl.cqrs.extensions.CqrsAbstractElementExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsCollectionExtensions.*
import static extension org.fuin.dsl.ddd.gen.extensions.EventExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsEObjectExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsStringExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsVariableExtensions.*
import static extension org.fuin.dsl.cqrs.extensions.CqrsAbstractEntityExtensions.*
import static extension org.fuin.dsl.ddd.gen.extensions.MapExtensions.*
import java.util.List
import org.fuin.dsl.cqrs.cqrsDsl.Command
import org.fuin.dsl.cqrs.cqrsDsl.Aggregate
import java.io.Serial
import java.time.ZonedDateTime
import org.fuin.dsl.ddd.gen.base.TypeKeys
import org.fuin.dsl.cqrs.cqrsDsl.AggregateId
import static extension org.fuin.dsl.ddd.gen.extensions.JavaStringExtensions.*

class CommandArtifactFactory extends AbstractSource<Command> {

    override getModelType() {
        typeof(Command)
    }

    override getTypeKey() {
        TypeKeys.JAVA_COMMAND
    }

    override create(Command command, Map<String, Object> context, boolean preparationRun) throws GenerateException {

        val Aggregate entity = command.aggregate
        val className = command.getName()
        // The module is optional: derive the package from the element itself - the target
        // aggregate for a domain command, otherwise the command.
        val EObject owner = if (entity === null) command else entity
        val pkg = owner.asPackage
        val fqn = pkg + "." + className
        val filename = fqn.replace('.', '/') + ".java";

        val CodeReferenceRegistry refReg = context.codeReferenceRegistry
        refReg.putReference(TypeKeys.refKey(command), fqn)

        if (preparationRun) {

            // No code generation during preparation phase
            return null
        }

        val SimpleCodeSnippetContext ctx = new SimpleCodeSnippetContext(refReg)
        ctx.addImports(entity, command)
        ctx.addReferences(command)

        var String src;
        if (entity === null) {
            src = createStandardCommand(ctx, command, pkg, className).toString();
        } else {
            src = createDomainCommand(ctx, command, pkg, className).toString();
        }

        return List.of(newArtifact(filename, src.getBytes("UTF-8"), owner));
    }

    def addImports(CodeSnippetContext ctx, AbstractEntity entity, Command command) {
        ctx.requiresImport("org.fuin.ddd4j.core.EventType")
        ctx.requiresImport("org.fuin.esc.api.HasSerializedDataTypeConstant")
        ctx.requiresImport("org.fuin.esc.api.SerializedDataType")
        ctx.requiresImport(Serial.name)
        
        if (entity === null) {
	        if (options.jsonb) {
	            ctx.requiresImport("org.fuin.cqrs4j.jsonb.AbstractCommand")        
	        }
	        if (options.jaxb) {
	            ctx.requiresImport("org.fuin.cqrs4j.jaxb.AbstractCommand")        
	        }
	        if (options.jackson) {
	            ctx.requiresImport("org.fuin.cqrs4j.jackson.AbstractCommand")        
	        }
            // toString() renders KeyValueEL.replace(...) over the same variables the constructor uses:
            // the command's own attributes, or - for a command derived from a method/constructor - the
            // target's parameters. Guard the imports on that same set, not on command.attributes alone.
            val variables = command.commandVariables
            if (variables.nullSafe.size > 0) {
                ctx.requiresImport("org.fuin.objects4j.core.KeyValue")
                ctx.requiresImport("org.fuin.objects4j.core.KeyValueEL")
                ctx.requiresImport("java.util.Objects")
            }
        } else {
	        if (options.jsonb) {
	            ctx.requiresImport("org.fuin.cqrs4j.jsonb.AbstractAggregateCommand")        
	        }
	        if (options.jaxb) {
	            ctx.requiresImport("org.fuin.cqrs4j.jaxb.AbstractAggregateCommand")        
	        }
	        if (options.jackson) {
	            ctx.requiresImport("org.fuin.cqrs4j.jackson.AbstractAggregateCommand")        
	        }
            ctx.requiresImport("org.fuin.objects4j.core.KeyValue")
            ctx.requiresImport("org.fuin.objects4j.core.KeyValueEL")
            ctx.requiresImport("java.util.Objects")
            ctx.requiresImport("org.fuin.ddd4j.core.EventId")
            ctx.requiresImport(ZonedDateTime.name)
        }
    }

    def addReferences(CodeSnippetContext ctx, Command command) {    	
        if (command.aggregate !== null) {
            ctx.requiresReference(TypeKeys.refKey(command.entityIdType))
        }
    }

    def AggregateId getAggregateIdType(Command command) {
        if (command.aggregate === null) {
            return null
        }
        return command.aggregate.idType
    }

    def AbstractEntityId getEntityIdType(Command command) {
        if (command.entity === null) {
            return null
        }
        return command.entity.idType
    }

    def createDomainCommand(SimpleCodeSnippetContext ctx, Command command, String pkg, String className) {
    	var variables = command.commandVariables
        val String src = ''' 
            «new SrcJavaDocType(command)»
            «IF options.jaxb»
            «new SrcXmlRootElement(ctx, command.name)»
            «ENDIF»
            @HasSerializedDataTypeConstant
            public final class «className» extends AbstractAggregateCommand<«command.aggregateIdType.name», «command.entityIdType.name»> {
            
            	@Serial
                private static final long serialVersionUID = 1000L;
            
                /** Unique name used to store the command. */
                public static final EventType EVENT_TYPE = new EventType("«command.name»");
                
                /**
                 * Type used to look up the serializer and deserializer. The registry is built by scanning
                 * for the annotation above, so without this constant the command cannot be deserialized
                 * when it arrives at the command endpoint.
                 */
                public static final SerializedDataType SER_TYPE = new SerializedDataType(EVENT_TYPE.asBaseType());
                
                «new SrcVarsDecl(ctx, "private", options, command, true)»
            
                «IF variables.nullSafe.size > 0»
                    /**
                     * Protected default constructor for deserialization and the builder.
                     */
                    @SuppressWarnings("NullAway.Init")
                    protected «command.name»() {
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
                    return Objects.requireNonNull(KeyValueEL.replace("«command.message.javaString»",
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
                
                «new SrcCommandBuilder(ctx, options,command)»
            }
        '''

        new SrcAll(ctx, copyrightHeader, pkg, ctx.imports, src).toString

    }

    def createStandardCommand(SimpleCodeSnippetContext ctx, Command command, String pkg, String className) {
    	var variables = command.commandVariables
        val String src = ''' 
            «new SrcJavaDocType(command)»
            «IF options.jaxb»
            «new SrcXmlRootElement(ctx, command.name)»
            «ENDIF»
            @HasSerializedDataTypeConstant
            public final class «className» extends AbstractCommand {
            
                @Serial
                private static final long serialVersionUID = 1000L;
            
                /** Unique name used to store the command. */
                public static final EventType EVENT_TYPE = new EventType("«command.name»");
                
                /**
                 * Type used to look up the serializer and deserializer. The registry is built by scanning
                 * for the annotation above, so without this constant the command cannot be deserialized
                 * when it arrives at the command endpoint.
                 */
                public static final SerializedDataType SER_TYPE = new SerializedDataType(EVENT_TYPE.asBaseType());
                
                «new SrcVarsDecl(ctx, "private", options, command)»
            
                «IF variables.nullSafe.size > 0»
                    /**
                     * Protected default constructor for deserialization.
                     */
                    @SuppressWarnings("NullAway.Init")
                    protected «command.name»() {
                        super();
                    }
                    
                «ENDIF»
                /**
                 * «command.doc.text»
                 *
                «FOR v : variables»
                    * @param «v.name» «v.superDoc» 
                «ENDFOR»
                */
                public «command.name»(«new SrcParamsDecl(ctx, options, variables.asParameters)») {
                    super();
                    «new SrcParamsAssignment(ctx, variables.asParameters)»
                }
            
                @Override
                public final EventType getEventType() {
                    return EVENT_TYPE;
                }
            
                «new SrcGetters(ctx, options, "public final", variables)»
            
                @Override
                public final String toString() {
                    «IF variables.nullSafe.size == 0»
                        return "«command.message.javaString»";
                    «ELSE»
                        return Objects.requireNonNull(KeyValueEL.replace("«command.message.javaString»"
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
