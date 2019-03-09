package suite;

import com.quorum.tessera.config.CommunicationType;
import com.quorum.tessera.test.DBType;
import com.quorum.tessera.test.ProcessManager;
import config.ConfigGenerator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

public class TestSuite extends Suite {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface ProcessConfig {

        DBType dbType();

        CommunicationType communicationType();

        SocketType socketType();
    }

    public TestSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, builder);
    }

    @Override
    public void run(RunNotifier notifier) {

        ProcessConfig testConfig = Arrays.stream(getRunnerAnnotations())
                .filter(ProcessConfig.class::isInstance)
                .map(ProcessConfig.class::cast)
                .findAny()
                .orElseThrow(() -> new AssertionError("No Test config found"));

        ExecutionContext.Builder.create()
                .with(testConfig.communicationType())
                .with(testConfig.dbType())
                .with(testConfig.socketType())
                .createAndSetupContext();

        ConfigGenerator configGenerator = new ConfigGenerator();
        configGenerator.generateConfigs(ExecutionContext.currentContext());

        ProcessManager processManager = new ProcessManager(ExecutionContext.currentContext());

        try {
            processManager.startNodes();
        } catch (Exception ex) {
            Description de = Description.createSuiteDescription(getTestClass().getJavaClass());
            notifier.fireTestFailure(new Failure(de, ex));
        }

        super.run(notifier);

        try{
            processManager.stopNodes();
        } catch (Exception ex) {
            Description de = Description.createSuiteDescription(getTestClass().getJavaClass());
            notifier.fireTestFailure(new Failure(de, ex));
        }

        ExecutionContext.destoryContext();

    }

}
