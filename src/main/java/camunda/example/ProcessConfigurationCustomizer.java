package camunda.example;

import info.novatec.micronaut.camunda.bpm.feature.DefaultProcessEngineConfigurationCustomizer;
import info.novatec.micronaut.camunda.bpm.feature.ProcessEngineConfigurationCustomizer;
import io.micronaut.context.annotation.Replaces;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.listener.ExpressionExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.listener.ExpressionTaskListener;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.bpm.engine.task.Task;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Optional;

/**
 * @author Lukasz Frankowski
 */
@Singleton @Replaces(DefaultProcessEngineConfigurationCustomizer.class)
public class ProcessConfigurationCustomizer implements ProcessEngineConfigurationCustomizer {

	@Inject protected Provider<ProcessService> processServiceProvider;

	@Override
	public void customize(@Nonnull ProcessEngineConfiguration configuration) {
		StandaloneProcessEngineConfiguration config = (StandaloneProcessEngineConfiguration) configuration;
		ArrayList<BpmnParseListener> list = new ArrayList<>();

		list.add(new AbstractBpmnParseListener() {

			protected String processKey = null;
			protected boolean enableAutoAssignment = false;
			protected boolean enableAutoBind = false;

			@Override
			public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl startEventActivity) {
				super.parseStartEvent(startEventElement, scope, startEventActivity);

				processKey = ((ProcessDefinitionEntity) startEventActivity.getProcessDefinition()).getKey();

				Optional.ofNullable(startEventElement.element("extensionElements"))
					.map(extensionElements -> extensionElements.element("properties"))
					.ifPresent(properties -> properties.elements().forEach(property -> {

						// enable lane auto assignment
						if ("assignment".equals(property.attribute("name"))
							&& "auto".equals(property.attribute("value")))
							enableAutoAssignment = true;

						// enable auto bind
						if ("bind".equals(property.attribute("name"))
							&& "auto".equals(property.attribute("value")))
							enableAutoBind = true;

					}));

			}

			/**
			 * To each use activity we add on task create call to {@link ProcessService#tryAutoAssignTask(Task)} to try auto assign task.
			 */
			@Override
			public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
				super.parseUserTask(userTaskElement, scope, activity);

				if (enableAutoAssignment) {
					((UserTaskActivityBehavior) activity.getActivityBehavior()).getTaskDecorator().getTaskDefinition()
						.addTaskListener(TaskListener.EVENTNAME_CREATE, new ExpressionTaskListener(
							config.getExpressionManager().createExpression("${processService.tryAutoAssignTask(task)}")));
				}

				enableAutoBind(activity);
			}

			@Override
			public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope, ActivityImpl activity) {
				super.parseServiceTask(serviceTaskElement, scope, activity);
				enableAutoBind(activity);
			}

			protected void enableAutoBind(ActivityImpl activity) {
				if (enableAutoBind) {
					activity.addListener(ExecutionListener.EVENTNAME_END, new ExpressionExecutionListener(
						config.getExpressionManager().createExpression(String.format("${%s.bind(execution)}", processKey))));
				}
			}

		});

		config.setCustomPostBPMNParseListeners(list);
	}

}
