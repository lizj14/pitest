package org.pitest.mutationtest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.pitest.mutationtest.LocationMother.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.coverage.CoverageDatabase;
import org.pitest.coverage.domain.TestInfo;
import org.pitest.functional.F;
import org.pitest.functional.FCollection;
import org.pitest.functional.Option;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationEngine;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.filter.MutationFilterFactory;
import org.pitest.mutationtest.filter.UnfilteredMutationFilter;
import org.pitest.mutationtest.instrument.ClassLine;

public class MutationSourceTest {

  private MutationSource        testee;

  private MutationConfig        config;

  @Mock
  private MutationFilterFactory filter;

  @Mock
  private CoverageDatabase      coverage;

  @Mock
  private ClassByteArraySource  source;

  @Mock
  private Mutater               mutater;

  @Mock
  private MutationEngine        engine;

  private final ClassName       foo = ClassName.fromString("foo");

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(this.engine.createMutator(this.source)).thenReturn(this.mutater);
    this.config = new MutationConfig(this.engine,
        Collections.<String> emptyList());
    setupFilterFactoryToFilterNothing();
    this.testee = new MutationSource(this.config, this.filter, this.coverage,
        this.source);
  }

  private void setupFilterFactoryToFilterNothing() {
    when(this.filter.createFilter()).thenReturn(
        UnfilteredMutationFilter.INSTANCE);
  }

  @Test
  public void shouldReturnNoMuationsWhenNoneFound() {
    assertEquals(Collections.emptyList(), this.testee.createMutations(this.foo));
  }

  @Test
  public void shouldAssignTestsForRelevantLineToGeneratedMutations() {
    final List<TestInfo> expected = makeTestInfos(0);
    final List<MutationDetails> mutations = makeMutations("foo");
    when(this.coverage.getTestsForClassLine(any(ClassLine.class))).thenReturn(
        expected);

    when(this.mutater.findMutations(any(ClassName.class)))
        .thenReturn(mutations);
    final MutationDetails actual = this.testee.createMutations(this.foo)
        .iterator().next();
    assertEquals(expected, actual.getTestsInOrder());
  }

  @Test
  public void shouldAssignAllTestsForClassWhenMutationInStaticInitialiser() {
    final List<TestInfo> expected = makeTestInfos(0);
    final List<MutationDetails> mutations = makeMutations("<clinit>");
    when(this.coverage.getTestsForClass(this.foo)).thenReturn(expected);
    when(this.mutater.findMutations(any(ClassName.class)))
        .thenReturn(mutations);
    final MutationDetails actual = this.testee.createMutations(this.foo)
        .iterator().next();
    assertEquals(expected, actual.getTestsInOrder());
  }

  @Test
  public void shouldPrioritiseTestsByExecutionTime() {
    final List<TestInfo> unorderedTests = makeTestInfos(100, 1000, 1);
    final List<MutationDetails> mutations = makeMutations("foo");
    when(this.coverage.getTestsForClassLine(any(ClassLine.class))).thenReturn(
        unorderedTests);
    when(this.mutater.findMutations(any(ClassName.class)))
        .thenReturn(mutations);
    final MutationDetails actual = this.testee.createMutations(this.foo)
        .iterator().next();
    assertEquals(makeTestInfos(1, 100, 1000), actual.getTestsInOrder());
  }

  private List<TestInfo> makeTestInfos(final Integer... times) {
    return new ArrayList<TestInfo>(FCollection.map(Arrays.asList(times),
        timeToTestInfo()));
  }

  private F<Integer, TestInfo> timeToTestInfo() {
    return new F<Integer, TestInfo>() {
      public TestInfo apply(final Integer a) {
        return new TestInfo("foo", "bar", a, Option.<ClassName> none(), 0);
      }

    };
  }

  private List<MutationDetails> makeMutations(final String method) {
    final List<MutationDetails> mutations = Arrays.asList(makeMutation(method));
    return mutations;
  }

  private MutationDetails makeMutation(final String method) {
    final MutationIdentifier id = new MutationIdentifier(aLocation().with(foo).withMethod(method), 0, "mutator");
    return new MutationDetails(id, "file", "desc", 1, 2);
  }

}
