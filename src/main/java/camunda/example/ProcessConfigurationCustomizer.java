package camunda.example;

import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.listener.ExpressionTaskListener;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.bpm.engine.task.Task;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * @author Lukasz Frankowski
 */
@Singleton
public class ProcessConfigurationCustomizer implements Consumer<ProcessEngineConfiguration> {

	@Inject protected Provider<ProcessService> processServiceProvider;

	@Override
	public void accept(ProcessEngineConfiguration processEngineConfiguration) {
		StandaloneProcessEngineConfiguration config = (StandaloneProcessEngineConfiguration) processEngineConfiguration;
		ArrayList<BpmnParseListener> list = new ArrayList<>();
		
		list.add(new AbstractBpmnParseListener() {

			/**
			 * To each use activity we add on task create call to {@link ProcessService#tryAutoAssignTask(Task)} to try auto assign task.
			 */
			@Override
			public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
				super.parseUserTask(userTaskElement, scope, activity);

				((UserTaskActivityBehavior) activity.getActivityBehavior()).getTaskDecorator().getTaskDefinition()
					.addTaskListener(TaskListener.EVENTNAME_CREATE, new ExpressionTaskListener(
					config.getExpressionManager().createExpression("${processService.tryAutoAssignTask(task)}")));

			}

		});

		config.setCustomPostBPMNParseListeners(list);
	}

}
