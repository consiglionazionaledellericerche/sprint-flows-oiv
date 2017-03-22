package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestUtil;

import org.activiti.engine.TaskService;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;


@SpringBootTest(classes = FlowsApp.class)
@RunWith(SpringRunner.class)
public class FlowsProcessInstanceResourceTest {

    @Autowired
    private FlowsTaskResource flowsTaskResource;
    @Autowired
    private FlowsProcessInstanceResource flowsProcessInstanceResource;
    @Autowired
    private TestUtil util;
    @Autowired
    private TaskService taskService;
    @Autowired
    private FlowsProcessDefinitionResource flowsProcessDefinitionResource;

    private String processDefinitionMissioni;
    private String taskId;

    @Before
    public void setUp() throws Exception {
        util.loginAdmin();
        DataResponse ret = (DataResponse) flowsProcessDefinitionResource.getAllProcessDefinitions();

        ArrayList<ProcessDefinitionResponse> processDefinitions = (ArrayList) ret.getData();
        for (ProcessDefinitionResponse pd : processDefinitions) {
            if (pd.getId().contains("missioni")) {
                processDefinitionMissioni = pd.getId();
                break;
            }
        }
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("definitionId", processDefinitionMissioni);
        ResponseEntity<Object> response = flowsTaskResource.completeTask(req);
        assertEquals(response.getStatusCode(), OK);
//        Recupero il taskId
        taskId = taskService.createTaskQuery().singleResult().getId();
    }

    @After
    public void tearDown() {
        //        cancello la Process Instance creata all'inizio del test'
        TaskResponse taskResponse = flowsTaskResource.getTaskInstance(taskId).getBody();
        HttpServletResponse res = new MockHttpServletResponse();
        flowsProcessInstanceResource.delete(res, taskResponse.getProcessInstanceId(), "");
        assertEquals(NO_CONTENT.value(), res.getStatus());
        util.logout();
    }

    @Test
    public void testGetMyTasks() {
        //TODO: Test goes here...
    }

    @Test
    public void testGetAvailableTasks() {
        //TODO: Test goes here...
    }

    @Test
    public void testGetWorkflowInstances() throws Exception {
        //TODO: Test goes here...
    }

    @Test
    public void testGetProcessInstanceById() throws Exception {
        //TODO: Test goes here...
    }

    @Test
    public void testGetActiveProcessInstances() {

        ResponseEntity ret = flowsProcessInstanceResource.getActiveProcessInstances(new MockHttpServletRequest());

        assertEquals(ret.getStatusCode(), HttpStatus.OK);

        ArrayList<ProcessInstanceResponse> entities = (ArrayList<ProcessInstanceResponse>) ret.getBody();
        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getProcessDefinitionId(), processDefinitionMissioni);
    }


    @Test
    public void testGetWorkflowVariables() throws Exception {
        //TODO: Test goes here...
    }
}
