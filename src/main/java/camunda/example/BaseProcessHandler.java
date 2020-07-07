package camunda.example;

import io.micronaut.validation.validator.Validator;
import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstantiationBuilder;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;

/**
 * Base class for process handlers.
 *
 * @author Lukasz Frankowski
 */
public abstract class BaseProcessHandler<M extends BaseProcessModel> {

	@Inject protected Validator validator;
	@Inject protected RepositoryService repositoryService;
	@Inject protected FormService formService;
	@Inject protected TaskService taskService;
	@Inject protected RuntimeService runtimeService;
	@Inject protected ProcessService processService;

	protected abstract String name();

	protected abstract String modelName();

	protected abstract Optional<M> newModel();

	protected ProcessInstance start(@Nonnull String processBusinessKey) {
		ProcessInstantiationBuilder builder = runtimeService
			.createProcessInstanceByKey(name())
			.businessKey(processBusinessKey);

		newModel().ifPresent(model -> builder.setVariable(modelName(), model));

		return builder.execute();
	}

	public void bind(@Nonnull DelegateExecution execution) {
		findModel(execution).ifPresent(model -> {

			execution.getVariables().forEach((name, value) -> {
				if (!modelName().equals(name)) {
					try {
						Field field = model.getClass().getDeclaredField(name);
						field.setAccessible(true);
						field.set(model, value);
						execution.removeVariable(name);
					} catch (Exception e) {
						UserRegistrationProcess.logger.debug("Can't bind property: {} on class: {} [{}/{}]", name,
							getClass().getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
					}
				}
			});

			Set<ConstraintViolation<M>> cv = validator.validate(model);
			if (!cv.isEmpty())
				throw new ConstraintViolationException(cv);

		});
	}

	@SuppressWarnings("unchecked")
	protected Optional<M> findModel(@Nonnull DelegateExecution execution) {
		return Optional.ofNullable((M) execution.getVariable(modelName()));
	}

	public ProcessDefinition processDefinition() {
		return processService.processDefinition(name());
	}

}
