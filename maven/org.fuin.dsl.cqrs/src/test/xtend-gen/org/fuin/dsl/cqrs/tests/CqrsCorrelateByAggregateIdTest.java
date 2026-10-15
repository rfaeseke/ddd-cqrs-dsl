package org.fuin.dsl.cqrs.tests;

import com.google.inject.Inject;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.fuin.dsl.cqrs.cqrsDsl.CqrsDslPackage;
import org.fuin.dsl.cqrs.cqrsDsl.DomainModel;
import org.fuin.dsl.cqrs.validation.CqrsDslValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies what 'correlate-by' may name on an event that comes from an aggregate's method: the
 * aggregate's identifier as 'id', although the event declares no such attribute.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(CqrsDslInjectorProvider.class)
@SuppressWarnings("all")
public class CqrsCorrelateByAggregateIdTest {
  @Inject
  private ParseHelper<DomainModel> parseHelper;

  @Inject
  @Extension
  private ValidationTestHelper _validationTestHelper;

  /**
   * The event copies the method's attributes, so it belongs to the aggregate and 'id' is its identifier.
   */
  @Test
  public void testIdIsTheAggregateIdOfAnEventFromAMethod() {
    try {
      this._validationTestHelper.assertNoErrors(this.parseHelper.parse(this.model("copies-attributes-of Thing.rename", "id")));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * Any other name still has to be an attribute or a parameter.
   */
  @Test
  public void testAnUnknownKeyIsStillRefused() {
    try {
      this._validationTestHelper.assertError(this.parseHelper.parse(this.model("copies-attributes-of Thing.rename", "nope")), 
        CqrsDslPackage.Literals.PROCESS_REACTION, 
        CqrsDslValidator.PROCESS_CORRELATION_KEY_UNKNOWN, 
        "The correlation key \'nope\' is not an attribute of event \'ThingRenamed\'");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * An event with no origin has no aggregate to speak for it: 'id' must be declared.
   */
  @Test
  public void testWithoutAnOriginIdMustBeDeclared() {
    try {
      this._validationTestHelper.assertError(this.parseHelper.parse(this.model("", "id")), 
        CqrsDslPackage.Literals.PROCESS_REACTION, 
        CqrsDslValidator.PROCESS_CORRELATION_KEY_UNKNOWN, 
        "The correlation key \'id\' is not an attribute of event \'ThingRenamed\'");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  private String model(final String origin, final String key) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("context c {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("module m {");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("type String");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("aggregate-id ThingId identifies Thing {}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("/** A thing. */");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("aggregate Thing identifier ThingId {");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("/** Renames it. */");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("method rename fires ThingRenamed {");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("String name");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("/** A thing was renamed. */");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("event ThingRenamed ");
    _builder.append(origin, "\t\t");
    _builder.append(" {");
    _builder.newLineIfNotEmpty();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("/** Keeps things in step. */");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("process-manager ThingProcess {");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("instance-key ThingId");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("/** A thing was renamed. */");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("reacts-to ThingRenamed {");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("correlate-by ");
    _builder.append(key, "\t\t\t\t");
    _builder.newLineIfNotEmpty();
    _builder.append("\t\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder.toString();
  }
}
