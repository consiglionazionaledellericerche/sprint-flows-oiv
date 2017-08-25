package it.cnr.si.flows.ng.listeners;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEntityEventImpl;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.utils.Utils;

@Component
public class VisibilitySetter implements ActivitiEventListener {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(VisibilitySetter.class);

    @Inject
    private TaskService taskService;
    @Inject
    private RuntimeService runtimeService;

    @Override
    public void onEvent(ActivitiEvent event) {
        if (event.getType().equals(ActivitiEventType.TASK_CREATED)) {

            ActivitiEntityEventImpl taskEvent = (ActivitiEntityEventImpl) event;
            TaskEntity task = (TaskEntity) taskEvent.getEntity();

            String processInstanceId = event.getProcessInstanceId();
            Map<String, Object> variables = runtimeService.getVariables(event.getExecutionId());

            String processDefinitionId = task.getProcessDefinitionId();
            String currentTaskKey = task.getTaskDefinitionKey();

            setDefaultVisibilityRules(task.getId(), processInstanceId);
            setAdditionalVisibilityRules(event, processInstanceId, processDefinitionId, currentTaskKey);

        } else if (event.getType().equals(ActivitiEventType.PROCESS_STARTED)) {
        	
            ActivitiEntityEventImpl processEvent = (ActivitiEntityEventImpl) event;
            
            String executionId = event.getExecutionId();
            Map<String, Object> variables = runtimeService.getVariables(executionId);
            String processInstanceId = event.getProcessInstanceId();

            String processDefinitionKeyVersionated = (String) variables.get("processDefinitionId"); 
            String processDefinitionKey = processDefinitionKeyVersionated.split(":")[0];
            if (processDefinitionKey != null) {
                runtimeService.addGroupIdentityLink(processInstanceId, "supervisore#"+ processDefinitionKey, Utils.PROCESS_VISUALIZER);
                runtimeService.addGroupIdentityLink(processInstanceId, "responsabile#"+ processDefinitionKey, Utils.PROCESS_VISUALIZER);            	
            }
     
            String idStruttura = (String) variables.get("idStruttura");
            if (idStruttura!= null) {
                // TODO inserire if se è un flusso organizzato per strutture
                runtimeService.addGroupIdentityLink(processInstanceId, "supervisore#"+ processDefinitionKey +"@"+ idStruttura, Utils.PROCESS_VISUALIZER);
                runtimeService.addGroupIdentityLink(processInstanceId, "responsabile#"+ processDefinitionKey +"@"+ idStruttura, Utils.PROCESS_VISUALIZER);
                runtimeService.addGroupIdentityLink(processInstanceId, "supervisore-struttura@"+ idStruttura , Utils.PROCESS_VISUALIZER);
                runtimeService.addGroupIdentityLink(processInstanceId, "responsabile-struttura@"+ idStruttura, Utils.PROCESS_VISUALIZER);            	
            }
        }
    }


    private void setDefaultVisibilityRules(String taskId, String processInstanceId) {
        taskService.getIdentityLinksForTask(taskId).stream()
            .filter(l -> l.getType().equals(IdentityLinkType.CANDIDATE) && l.getGroupId() != null)
            .forEach(l -> runtimeService.addGroupIdentityLink(processInstanceId, l.getGroupId(), Utils.PROCESS_VISUALIZER));
    }


    private void setAdditionalVisibilityRules(ActivitiEvent event, String processInstanceId, String processDefinitionId,
            String currentTaskKey) {
        List<String> groups = VisibilityMapping.GroupVisibilityMappingForProcessInstance.get(processDefinitionId +"-"+ currentTaskKey);

        if (groups != null)
            for (String group : groups) {
                runtimeService.addGroupIdentityLink(processInstanceId, group, Utils.PROCESS_VISUALIZER);
            }

        List<String> users = VisibilityMapping.UserVisibilityMappingForProcessInstance.get(processDefinitionId +"-"+ currentTaskKey);

        if (users != null)
            for (String user : users) {
                runtimeService.addGroupIdentityLink(processInstanceId, user, Utils.PROCESS_VISUALIZER);
            }
    }

    @Override
    public boolean isFailOnException() {
        // TODO Auto-generated method stub
        return false;
    }

}
