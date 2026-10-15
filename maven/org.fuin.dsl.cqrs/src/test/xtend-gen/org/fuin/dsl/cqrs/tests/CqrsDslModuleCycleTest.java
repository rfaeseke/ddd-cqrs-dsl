package org.fuin.dsl.cqrs.tests;

import com.google.inject.Inject;
import java.util.Objects;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.fuin.dsl.cqrs.cqrsDsl.CqrsDslPackage;
import org.fuin.dsl.cqrs.cqrsDsl.DomainModel;
import org.fuin.dsl.cqrs.validation.CqrsDslValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies the rule that refuses a dependency cycle between modules.
 * 
 * <p>The edges are <b>resolved cross references</b>, not {@code import} lines, so the fixtures below
 * are deliberately written to pull the two apart: one has modules importing each other while only one
 * direction is actually referenced, which must stay clean.</p>
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(CqrsDslInjectorProvider.class)
@SuppressWarnings("all")
public class CqrsDslModuleCycleTest {
  @Inject
  private ParseHelper<DomainModel> parseHelper;

  @Inject
  private ValidationTestHelper validationHelper;

  /**
   * Two modules whose value objects reference each other close a cycle.
   */
  @Test
  public void mutualReferenceIsAnError() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("context p {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module types {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("type String");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module a {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.types.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.b.*");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("value-object AThing {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("BThing other");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module b {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.types.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.a.*");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("value-object BThing {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("AThing other");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      final DomainModel model = this.parseHelper.parse(_builder);
      this.validationHelper.assertError(model, CqrsDslPackage.Literals.MODULE, 
        CqrsDslValidator.MODULE_DEPENDENCY_CYCLE);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * The same cycle is only a warning when the build asks for it with the system property.
   */
  @Test
  public void cycleIsAWarningWhenTheBuildAsksForIt() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("context p {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module types {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("type String");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module a {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.types.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.b.*");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("value-object AThing {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("BThing other");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module b {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.types.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.a.*");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("value-object BThing {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("AThing other");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      final DomainModel model = this.parseHelper.parse(_builder);
      System.setProperty(CqrsDslValidator.MODULE_CYCLE_SEVERITY_PROPERTY, "warning");
      try {
        this.validationHelper.assertWarning(model, CqrsDslPackage.Literals.MODULE, 
          CqrsDslValidator.MODULE_DEPENDENCY_CYCLE);
        final Function1<Issue, Boolean> _function = (Issue it) -> {
          return Boolean.valueOf((Objects.equals(it.getCode(), CqrsDslValidator.MODULE_DEPENDENCY_CYCLE) && Objects.equals(it.getSeverity(), Severity.ERROR)));
        };
        Assertions.assertTrue(
          IterableExtensions.isEmpty(IterableExtensions.<Issue>filter(this.validationHelper.validate(model), _function)));
      } finally {
        System.clearProperty(CqrsDslValidator.MODULE_CYCLE_SEVERITY_PROPERTY);
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * One direction only is a dependency, not a cycle - even though both modules import each other.
   */
  @Test
  public void oneDirectionIsNotACycle() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("context p {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module types {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("type String");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module a {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.types.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.b.*");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("value-object AThing {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("BThing other");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module b {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.types.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.a.*");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("value-object BThing {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("String value");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      final DomainModel model = this.parseHelper.parse(_builder);
      this.validationHelper.assertNoIssues(model, CqrsDslPackage.Literals.MODULE);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * A module referring to its own types is not a cycle - an edge to itself is not an edge.
   */
  @Test
  public void selfReferenceIsNotACycle() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("context p {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module solo {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("type String");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("value-object Inner {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("String value");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("value-object Outer {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("Inner inner");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      final DomainModel model = this.parseHelper.parse(_builder);
      this.validationHelper.assertNoIssues(model, CqrsDslPackage.Literals.MODULE);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * A three module ring is reported too, not only the two module case.
   */
  @Test
  public void longerRingIsAnError() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("context p {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module types {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("type String");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module a {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.types.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.c.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("value-object AThing {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("CThing other");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module b {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.types.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.a.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("value-object BThing {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("AThing other");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("module c {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.types.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("import p.b.*");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("value-object CThing {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("BThing other");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      final DomainModel model = this.parseHelper.parse(_builder);
      this.validationHelper.assertError(model, CqrsDslPackage.Literals.MODULE, 
        CqrsDslValidator.MODULE_DEPENDENCY_CYCLE);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
