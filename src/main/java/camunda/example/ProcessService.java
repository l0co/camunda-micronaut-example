package camunda.example;

import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Lukasz Frankowski
 */
@Singleton
public class ProcessService {

	public static final Logger logger = LoggerFactory.getLogger(ProcessService.class);

	@Inject protected RepositoryService repositoryService;
	@Inject protected FormService formService;
	@Inject protected TaskService taskService;
	@Inject protected RuntimeService runtimeService;

	public ProcessDefinition processDefinition(String processDefinitionKey) {
		return repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey).singleResult();
	}

	public ProcessInstance processInstance(String businessKey) {
		return runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(businessKey).singleResult();
	}

	public List<Task> findActiveTasks(@Nonnull String processBusinessKey) {
		return taskService.createTaskQuery()
			.processInstanceBusinessKey(processBusinessKey)
			.active()
			.list();
	}

	public List<Task> findUnassignedActiveTasks(@Nonnull String processBusinessKey, @Nonnull List<String> assignableRoleNames) {
		return taskService.createTaskQuery()
			.processInstanceBusinessKey(processBusinessKey)
			.active()
			.taskUnassigned()
			.taskCandidateGroupIn(assignableRoleNames)
			.list();
	}

	public Task findActiveTask(@Nonnull String processBusinessKey, @Nonnull String taskDefinitionKey) {
		return taskService.createTaskQuery()
			.processInstanceBusinessKey(processBusinessKey)
			.taskDefinitionKey(taskDefinitionKey)
			.active()
			.singleResult();
	}

	public List<Task> findActiveTasksAssignedToUser(@Nonnull String processBusinessKey, @Nonnull String userId) {
		return taskService.createTaskQuery()
			.processInstanceBusinessKey(processBusinessKey)
			.taskAssignee(userId)
			.active()
			.list();
	}

	public void claimTask(@Nonnull Task task, @Nonnull String userId) {
		claimTask(task, userId, true);
	}

	protected void claimTask(@Nonnull Task task, @Nonnull String userId, boolean assignLane) {
		taskService.claim(task.getId(), userId);

		// when the user claims task, we find the appropriate lane and will set him as default user for further auto assignment
		// what surprisingly is not implemented in camunda
		if (assignLane) {
			findTaskLane(task).ifPresent(lane -> {
				// so we have lane found here and we can store user into process variables
				runtimeService.setVariable(task.getProcessInstanceId(), buildAutoAssignmentKey(lane), userId);
			});
		}
	}

	protected Optional<Lane> findTaskLane(@Nonnull Task task) {
		BpmnModelInstance bpmnModel = repositoryService.getBpmnModelInstance(task.getProcessDefinitionId());
		return bpmnModel.getModelElementsByType(Lane.class).stream().filter(lane ->
			lane.getFlowNodeRefs().stream()
				.anyMatch(flowNode -> flowNode.getId().equals(task.getTaskDefinitionKey())))
			.findAny();
	}

	protected String buildAutoAssignmentKey(@Nonnull Lane lane) {
		return String.format("autoassignment.%s", lane.getId());
	}

	public void tryAutoAssignTask(@Nonnull Task task) {
		findTaskLane(task).ifPresent(lane -> {
			// so we have lane found here and we can read user from process variables
			String userId = (String) runtimeService.getVariable(task.getProcessInstanceId(), buildAutoAssignmentKey(lane));
			if (userId!=null) {
				logger.debug("Automatically claiming task: {}, lane: {}, process: {} to user: {}",
					task.getTaskDefinitionKey(), lane.getId(), task.getProcessDefinitionId(), userId);
				claimTask(task, userId, false);
			}
		});
	}

	/**
	 * Completes task with no form filling
	 */
	public void completeTask(@Nonnull Task task) {
		taskService.complete(task.getId());
	}

	public Optional<TaskFormData> getTaskForm(@Nonnull Task task) {
		TaskFormData form = formService.getTaskFormData(task.getId());
		if (form.getFormFields().isEmpty())
			return Optional.empty();
		return Optional.of(formService.getTaskFormData(task.getId()));
	}

	public void completeTask(@Nonnull Task task, @Nonnull Map<String, Object> form) {
		formService.submitTaskForm(task.getId(), form);
	}

}
