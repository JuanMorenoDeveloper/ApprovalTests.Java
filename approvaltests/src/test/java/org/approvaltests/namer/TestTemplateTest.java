package org.approvaltests.namer;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.approvaltests.Approvals;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

public class TestTemplateTest
{
  @TestTemplate
  @ExtendWith(UserIdGeneratorTestInvocationContextProvider.class)
  public void whenUserIdRequested_thenUserIdIsReturnedInCorrectFormat(UserIdGeneratorTestCase testCase)
  {
    try (NamedEnvironment ne = NamerFactory.withParameters(testCase.isFeatureEnabled))
    {
      final ApprovalNamer approvalNamer = Approvals.createApprovalNamer();
      final String approvalName = approvalNamer.getApprovalName();
      Assertions.assertEquals(approvalName,
          "TestTemplateTest.whenUserIdRequested_thenUserIdIsReturnedInCorrectFormat." + testCase.isFeatureEnabled);
    }
  }
  public static class DisabledOnQAEnvironmentExtension implements ExecutionCondition
  {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
      Properties properties = new Properties();
      if ("qa".equalsIgnoreCase(properties.getProperty("env")))
      {
        String reason = String.format("The test '%s' is disabled on QA environment", context.getDisplayName());
        System.out.println(reason);
        return ConditionEvaluationResult.disabled(reason);
      }
      return ConditionEvaluationResult.enabled("Test enabled");
    }
  }
  public static class GenericTypedParameterResolver<T> implements ParameterResolver
  {
    T data;
    public GenericTypedParameterResolver(T data)
    {
      this.data = data;
    }
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
      return parameterContext.getParameter().getType().isInstance(data);
    }
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
      return data;
    }
  }
  public static class UserIdGeneratorTestCase
  {
    private String  displayName;
    private boolean isFeatureEnabled;
    private String  firstName;
    private String  lastName;
    private String  expectedUserId;
    public UserIdGeneratorTestCase(String displayName, boolean isFeatureEnabled, String firstName, String lastName,
        String expectedUserId)
    {
      this.displayName = displayName;
      this.isFeatureEnabled = isFeatureEnabled;
      this.firstName = firstName;
      this.lastName = lastName;
      this.expectedUserId = expectedUserId;
    }
    public String getDisplayName()
    {
      return displayName;
    }
    public boolean isFeatureEnabled()
    {
      return isFeatureEnabled;
    }
    public String getFirstName()
    {
      return firstName;
    }
    public String getLastName()
    {
      return lastName;
    }
    public String getExpectedUserId()
    {
      return expectedUserId;
    }
  }
  public static class UserIdGeneratorTestInvocationContextProvider implements TestTemplateInvocationContextProvider
  {
    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext)
    {
      return true;
    }
    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
        ExtensionContext extensionContext)
    {
      boolean featureDisabled = false;
      boolean featureEnabled = true;
      return Stream.of(
          featureDisabledContext(new UserIdGeneratorTestCase(
              "Given feature switch disabled When user name is John Smith Then generated userid is JSmith",
              featureDisabled, "John", "Smith", "JSmith")),
          featureEnabledContext(new UserIdGeneratorTestCase(
              "Given feature switch enabled When user name is John Smith Then generated userid is baelJSmith",
              featureEnabled, "John", "Smith", "baelJSmith")));
    }
    private TestTemplateInvocationContext featureDisabledContext(UserIdGeneratorTestCase userIdGeneratorTestCase)
    {
      return new TestTemplateInvocationContext()
      {
        @Override
        public String getDisplayName(int invocationIndex)
        {
          return userIdGeneratorTestCase.getDisplayName();
        }
        @Override
        public List<Extension> getAdditionalExtensions()
        {
          return asList(new GenericTypedParameterResolver(userIdGeneratorTestCase),
              new BeforeTestExecutionCallback()
              {
                @Override
                public void beforeTestExecution(ExtensionContext extensionContext)
                {
                  System.out.println("BeforeTestExecutionCallback:Disabled context");
                }
              }, new AfterTestExecutionCallback()
              {
                @Override
                public void afterTestExecution(ExtensionContext extensionContext)
                {
                  System.out.println("AfterTestExecutionCallback:Disabled context");
                }
              });
        }
      };
    }
    private TestTemplateInvocationContext featureEnabledContext(UserIdGeneratorTestCase userIdGeneratorTestCase)
    {
      return new TestTemplateInvocationContext()
      {
        @Override
        public String getDisplayName(int invocationIndex)
        {
          return userIdGeneratorTestCase.getDisplayName();
        }
        @Override
        public List<Extension> getAdditionalExtensions()
        {
          return asList(new GenericTypedParameterResolver(userIdGeneratorTestCase),
              new DisabledOnQAEnvironmentExtension(), new BeforeEachCallback()
              {
                @Override
                public void beforeEach(ExtensionContext extensionContext)
                {
                  System.out.println("BeforeEachCallback:Enabled context");
                }
              }, new AfterEachCallback()
              {
                @Override
                public void afterEach(ExtensionContext extensionContext)
                {
                  System.out.println("AfterEachCallback:Enabled context");
                }
              });
        }
      };
    }
  }
  public interface UserIdGenerator
  {
    String generate(String firstName, String lastName);
  }
  public static class UserIdGeneratorImpl implements UserIdGenerator
  {
    private boolean isFeatureEnabled;
    public UserIdGeneratorImpl(boolean isFeatureEnabled)
    {
      this.isFeatureEnabled = isFeatureEnabled;
    }
    public String generate(String firstName, String lastName)
    {
      String initialAndLastName = firstName.substring(0, 1).concat(lastName);
      return isFeatureEnabled ? "bael".concat(initialAndLastName) : initialAndLastName;
    }
  }
}
