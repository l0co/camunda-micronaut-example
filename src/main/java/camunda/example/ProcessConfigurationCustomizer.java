package camunda.example;

import org.camunda.bpm.engine.ProcessEngineConfiguration;

import javax.inject.Singleton;
import java.util.function.Consumer;

/**
 * @author Lukasz Frankowski
 */
@Singleton
public class ProcessConfigurationCustomizer implements Consumer<ProcessEngineConfiguration> {

	@Override
	public void accept(ProcessEngineConfiguration processEngineConfiguration) {
		// TODOLF impl ProcessConfigurationCustomizer.accept
		System.out.println("");
	}
	
}
