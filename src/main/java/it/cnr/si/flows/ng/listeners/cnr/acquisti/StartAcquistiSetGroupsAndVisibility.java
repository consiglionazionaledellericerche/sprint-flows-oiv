package it.cnr.si.flows.ng.listeners.cnr.acquisti;

import it.cnr.si.flows.ng.listeners.oiv.service.StartOivSetGroupsAndVisibility;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.LdapSecurityUtils;
import it.cnr.si.security.SecurityUtils;
import it.cnr.si.service.RelationshipService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.idStruttura;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;


@Service
public class StartAcquistiSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartOivSetGroupsAndVisibility.class);


	@Inject
	private RelationshipService relationshipService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private RuntimeService runtimeService;

	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));

		List<GrantedAuthority> authorities = relationshipService.getAllGroupsForUser(initiator);

		List<String> groups = authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.map(Utils::removeLeadingRole)
				.filter(g -> g.startsWith("ra@"))
				.collect(Collectors.toList());

		if (groups.isEmpty())
			throw new BpmnError("403", "L'utente non e' abilitato ad avviare questo flusso");
		else if ( groups.size() > 1 )
			throw new BpmnError("500", "L'utente appartiene a piu' di un gruppo Responsabile Acquisti");
		else {

			String gruppoRT = groups.get(0);
			String struttura = gruppoRT.substring(gruppoRT.lastIndexOf('@') +1);
			// idStruttura variabile che indica che il flusso è diviso per strutture (implica la visibilità distinta tra strutture)
			execution.setVariable(idStruttura.name(), struttura);
			String gruppoDirettore = "direttore@"+ struttura;
			String gruppoRA = "ra@"+ struttura;
			String gruppoSFD = "sfd@"+ struttura;
			String rup = execution.getVariable("rup", String.class);
			String applicazioneSigla = "sigla";

			LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}, {}", gruppoRT, gruppoSFD, gruppoRA, gruppoDirettore);

			//Check se il gruppo SFD ha membri
			List<String> members = aceBridgeService.getUsersinAceGroup(gruppoSFD);
			if (members.isEmpty()) {
				execution.setVariable("organizzazioneStruttura", "Semplice");
			} else {
				execution.setVariable("organizzazioneStruttura", "Complessa");
			}
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoRT, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirettore, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoRA, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoSFD, PROCESS_VISUALIZER);
			runtimeService.addUserIdentityLink(execution.getProcessInstanceId(), rup, PROCESS_VISUALIZER);
			runtimeService.addUserIdentityLink(execution.getProcessInstanceId(), applicazioneSigla, PROCESS_VISUALIZER);
			//            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), "segreteria@" + struttura, PROCESS_VISUALIZER);

			execution.setVariable("gruppoRT", gruppoRT);
			execution.setVariable("gruppoDirettore", gruppoDirettore);
			execution.setVariable(Enum.VariableEnum.gruppoRA.name(), gruppoRA);
			execution.setVariable("gruppoSFD", gruppoSFD);
			execution.setVariable("sigla", applicazioneSigla);


		}

	}
}