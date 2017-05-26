package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.FlowsPermissionException;
import it.cnr.si.flows.ng.service.CounterService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.SecurityUtils;
import it.cnr.si.service.UserService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.*;

/**
 * @author mtrycz
 *
 */
@RestController
@RequestMapping("api/tasks")
public class FlowsTaskResource {

    private static final String TASK_EXECUTOR = "esecutore";
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsTaskResource.class);
    @Autowired
    protected RestResponseFactory restResponseFactory;
    Utils utils = new Utils();
    @Inject
    private UserService userService;
    @Inject
    private CounterService counterService;
    @Inject
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private FlowsAttachmentService attachmentService;
    @Autowired
    private HistoryService historyService;
    @Inject
    private FlowsAttachmentResource attachmentResource;

    // TODO magari un giorno avremo degli array, ma per adesso ce lo facciamo andare bene cosi'
    private static Map<String, Object> extractParameters(MultipartHttpServletRequest req) {

        Map<String, Object> data = new HashMap<>();

        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            data.put(paramName, req.getParameter(paramName));
        }

        return data;

    }

    @RequestMapping(value = "/mytasks", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getMyTasks(
            HttpServletRequest req,
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order) {

        String username = SecurityUtils.getCurrentUserLogin();

        TaskQuery taskQuery = taskService.createTaskQuery()
                .taskAssignee(username)
                .includeProcessVariables();

        if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
            taskQuery.processDefinitionKey(processDefinition);

        taskQuery = (TaskQuery) utils.searchParamsForTasks(req, taskQuery);

        utils.orderTasks(order, taskQuery);

        List<TaskResponse> list = restResponseFactory.createTaskResponseList(taskQuery.listPage(firstResult, maxResults));

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(list.size());
        response.setTotal(taskQuery.count());
        response.setData(list);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/availabletasks", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getAvailableTasks(
            HttpServletRequest req,
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order) {

        String username = SecurityUtils.getCurrentUserLogin();
        List<String> authorities =
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(FlowsTaskResource::removeLeadingRole)
                .collect(Collectors.toList());

        TaskQuery taskQuery = taskService.createTaskQuery()
                .taskCandidateUser(username)
                .taskCandidateGroupIn(authorities)
                .includeProcessVariables();

        taskQuery = (TaskQuery) utils.searchParamsForTasks(req, taskQuery);

        if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
            taskQuery.processDefinitionKey(processDefinition);

        if (order.equals(ASC))
            taskQuery.orderByTaskCreateTime().asc();
        else if (order.equals(DESC))
            taskQuery.orderByTaskCreateTime().desc();

        List<TaskResponse> list = restResponseFactory.createTaskResponseList(taskQuery.listPage(firstResult, maxResults));

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(list.size());
        response.setTotal(taskQuery.count());
        response.setData(list);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable("id") String taskId) {

        Map<String, Object> response = new HashMap<>();
        Task taskRaw = taskService.createTaskQuery().taskId(taskId).includeProcessVariables().singleResult();

        // task + variables
        TaskResponse task = restResponseFactory.createTaskResponse(taskRaw);
        response.put("task", task);

        // attachments
        ResponseEntity<List<FlowsAttachment>> attachementsEntity = attachmentResource.getAttachementsForTask(taskId);
        Map<String, Object> attachments = new TreeMap<>();
        attachementsEntity.getBody().stream()
        .sorted((a1, a2) -> a1.getName().compareTo(a2.getName()))
        .forEach(a -> {
            a.setBytes(null);
            attachments.put(a.getName(), a);
        });
        response.put("attachments", attachments);
        response.put("attachmentsList", attachementsEntity.getBody());

        return ResponseEntity.ok(response);
    }

    /**
     * @param req
     * @param taskId
     * @param params
     * @return
     */
    @RequestMapping(value = "/claim/{taskId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Map<String, Object>> claimTask(
            HttpServletRequest req,
            @PathVariable("taskId") String taskId) {

        String username = SecurityUtils.getCurrentUserLogin();
        LOGGER.info("Setting owner of task {} to {}", taskId, username);

        List<IdentityLink> list = taskService.getIdentityLinksForTask(taskId);
        String groupCandidate = null;
        for (IdentityLink link : list) {
            if (link.getType().equals("candidate")) {
                groupCandidate = link.getGroupId();
                break;
            }
        }
        final String finalGroupCandidate = groupCandidate;
        boolean canClaim = userService.getUserWithAuthorities().getAuthorities().stream().anyMatch(autority -> autority.getName().equals(finalGroupCandidate));

        if (canClaim)
            taskService.claim(taskId, username);
        else
            return new ResponseEntity<Map<String, Object>>(HttpStatus.FORBIDDEN);

        return new ResponseEntity<Map<String,Object>>(HttpStatus.OK);
    }

    @RequestMapping(value = "/claim/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Map<String, Object>> unclaimTask(
            HttpServletRequest req,
            @PathVariable("id") String id) {

        String username = SecurityUtils.getCurrentUserLogin();
        String assignee = taskService.createTaskQuery()
                .taskId(id)
                .singleResult().getAssignee();

        if (username.equals(assignee)) {
            taskService.unclaim(id);
            return new ResponseEntity<Map<String,Object>>(HttpStatus.OK);
        } else {
            return new ResponseEntity<Map<String,Object>>(HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value = "/{id}/{user}", method = RequestMethod.PUT)
    @Timed
    public ResponseEntity<Map<String, Object>> assignTask(
            HttpServletRequest req,
            @PathVariable("id") String id,
            @PathVariable("user") String user) {

        //    todo: test
        //    todo: chi può asegnare un task?
        //        String username = SecurityUtils.getCurrentUserLogin();

        taskService.setAssignee(id, user);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    // TODO verificare almeno che l'utente abbia i gruppi necessari
    @RequestMapping(value = "canComplete/{taskId}/{user}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public boolean canCompleteTask(
            @PathVariable("taskId") String taskId,
            @PathVariable("username") Optional<String> user) {

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        String username = user.orElse(SecurityUtils.getCurrentUserLogin());

        // TODO get authorities from username NOT currentuser
        List<String> authorities =
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(FlowsTaskResource::removeLeadingRole) //todo: vedere con Martin (le authorities sono ROLE_USER (come ora) o USER (come prima))
                .collect(Collectors.toList());

        if ( username.equals(taskService.createTaskQuery().taskId(taskId).singleResult().getAssignee()) )
            return true;
        else
            return identityLinks.stream()
                    .filter(l -> l.getType().equals("candidate"))
                    .anyMatch(l -> authorities.contains(l.getGroupId()) );
    }

    // TODO verificare almeno che l'utente abbia i gruppi necessari
    @RequestMapping(value = "complete", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Object> completeTask(MultipartHttpServletRequest req) {

        String username = SecurityUtils.getCurrentUserLogin();

        String taskId = (String) req.getParameter("taskId");
        String definitionId = (String) req.getParameter("processDefinitionId");
        if ( isEmpty(taskId) && isEmpty(definitionId) )
            return ResponseEntity.badRequest().body("{'success': false, 'message': 'Fornire almeno un taskId o un definitionId'}");

        try {
            Map<String, Object> data = extractParameters(req);
            data.putAll(attachmentService.extractAttachmentsVariables(req));

            if ( isNotEmpty(taskId) ) {
                if (!canCompleteTask(taskId, Optional.of(username)))
                    throw new FlowsPermissionException();

                // aggiungo l'identityLink che indica l'utente che esegue il task
                taskService.addUserIdentityLink(taskId, username, TASK_EXECUTOR);

                taskService.setVariablesLocal(taskId, data);
                taskService.complete(taskId, data);

                return new ResponseEntity<>(HttpStatus.OK);

            } else {
                ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult();

                String counterId = processDefinition.getName() +"-"+ Calendar.getInstance().get(Calendar.YEAR);
                String key =  counterId +"-"+ counterService.getNext(counterId);

                data.put("title", key);
                data.put("initiator", username);
                data.put("startDate", new Date());

                ProcessInstance instance = runtimeService.startProcessInstanceById(definitionId, key, data);

                LOGGER.debug("Avviata istanza di processo {}, id: {}", key, instance.getId());

                ProcessInstanceResponse response = restResponseFactory.createProcessInstanceResponse(instance);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (IOException e) {
            LOGGER.error("Errore nel processare i files:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore nel processare i files");
        } catch (FlowsPermissionException e) {
            LOGGER.error("L'utente {} non e' abilitato a completare il task {} / avviare il flusso {}", username, taskId, definitionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("L'utente non e' abilitato ad eseguire l'azione richiesta");
        }
    }

    /**
     * Funzionalità di Ricerca delle Process Instances.
     *
     * @param req               the req
     * @param processInstanceId Il processInstanceId della ricerca
     * @param active            Boolean che indica se ricercare le Process Insrtances attive o terminate
     * @param order             L'ordine in cui vogliamo i risltati ('ASC' o 'DESC')
     * @return le response entity frutto della ricerca
     */
    @RequestMapping(value = "/search/{processInstanceId}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Object> search(
            HttpServletRequest req,
            @PathVariable("processInstanceId") String processInstanceId,
            @RequestParam("active") boolean active,
            @RequestParam("order") String order,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults) {

        Map<String, Object> result = new HashMap<>();

        HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery();

        if (!processInstanceId.equals(ALL_PROCESS_INSTANCES))
            taskQuery.processDefinitionKey(processInstanceId);

        if (active)
            taskQuery.unfinished();
        else
            taskQuery.finished();
        String jsonString = "";

        try {
            jsonString = IOUtils.toString(req.getReader());
        } catch (Exception e) {
            LOGGER.error(ERRORE_NELLA_LETTURE_DELLO_STREAM_DELLA_REQUEST, e);
        }

        taskQuery = (HistoricTaskInstanceQuery) utils.extractProcessSearchParams(taskQuery, new JSONObject(jsonString).getJSONArray(PROCESS_PARAMS));
        taskQuery = (HistoricTaskInstanceQuery) utils.orderTasks(order, taskQuery);

        long totalItems = taskQuery.includeProcessVariables().count();
        result.put("totalItems", totalItems);

        List<HistoricTaskInstance> taskRaw = taskQuery.includeProcessVariables().listPage(firstResult, maxResults);
        List<HistoricTaskInstanceResponse> tasks = restResponseFactory.createHistoricTaskInstanceResponseList(taskRaw);
        result.put("tasks", tasks);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/taskCompletedByMe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Object> getTasksCompletedByMe(
            HttpServletRequest req,
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order) {

        String username = SecurityUtils.getCurrentUserLogin();

        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().taskInvolvedUser(username)
                .includeProcessVariables().includeTaskLocalVariables();

        query = (HistoricTaskInstanceQuery) utils.searchParamsForTasks(req, query);

        if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
            query.processDefinitionKey(processDefinition);

        query = (HistoricTaskInstanceQuery) utils.orderTasks(order, query);

        List<HistoricTaskInstance> taskList = new ArrayList<>();
        for (HistoricTaskInstance task : query.list()) {
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForTask(task.getId());
            for (HistoricIdentityLink hil : identityLinks) {
                if (hil.getType().equals(TASK_EXECUTOR) && hil.getUserId().equals(username))
                    taskList.add(task);
            }
        }
        List<HistoricTaskInstanceResponse> resultList = restResponseFactory.createHistoricTaskInstanceResponseList(
                taskList.subList(firstResult, (firstResult + maxResults <= taskList.size()) ? firstResult + maxResults : taskList.size()));

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(resultList.size());// numero di task restituiti
        response.setTotal(taskList.size()); //numero totale di task avviati da me
        response.setData(resultList);

        return ResponseEntity.ok(response);
    }

    public static String removeLeadingRole(String in) {
        return in.startsWith("ROLE_") ? in.substring(5) : in;
    }
}