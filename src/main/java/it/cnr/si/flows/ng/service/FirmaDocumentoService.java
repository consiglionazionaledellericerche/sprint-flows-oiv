package it.cnr.si.flows.ng.service;

import it.cnr.si.firmadigitale.firma.arss.ArubaSignServiceException;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.listeners.oiv.service.OivSetGroupsAndVisibility;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsFirmaService;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static it.cnr.si.flows.ng.utils.Enum.Azione.Firma;
import static it.cnr.si.flows.ng.utils.Enum.Stato.Firmato;

import java.io.IOException;
import java.text.ParseException;

import javax.inject.Inject;


@Service
public class FirmaDocumentoService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FirmaDocumentoService.class);

	@Inject
	private FlowsFirmaService flowsFirmaService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;

	public void eseguiFirma(DelegateExecution execution, String nomeVariabileFile)  throws IOException, ParseException  {

		if (nomeVariabileFile == null)
			throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaFirmare' nella process definition (nel Task Listener - Fields).");
		if (execution.getVariable("sceltaUtente") != null && execution.getVariable("sceltaUtente").toString().equals("Firma")) {
			String stringaOscurante = "******";
			// TODO: validare presenza di queste tre variabili
			String username = (String) execution.getVariable("username");
			String password = (String) execution.getVariable("password");
			String otp = (String) execution.getVariable("otp");
			String textMessage = "";

			FlowsAttachment att = (FlowsAttachment) execution.getVariable(nomeVariabileFile);
			byte[] bytes = att.getBytes();

			try {
				byte[] bytesfirmati = flowsFirmaService.firma(username, password, otp, bytes);
				att.setBytes(bytesfirmati);
				att.setFilename(getSignedFilename(att.getFilename()));
				att.setAzione(Firma);
				att.addStato(Firmato);

				flowsAttachmentService.saveAttachment(execution, nomeVariabileFile, att);
				execution.setVariable("otp", stringaOscurante);
				execution.setVariable("password", stringaOscurante);
			} catch (ArubaSignServiceException e) {
				LOGGER.error("firma non riuscita", e);
				if (e.getMessage().indexOf("error code 0001") != -1) {
					textMessage = "controlla il formato del file sottopsto alla firma";
				} else if(e.getMessage().indexOf("error code 0003") != -1) {
					textMessage = "Errore in fase di verifica delle credenziali";
				} else if(e.getMessage().indexOf("error code 0004") != -1) {
					textMessage = "Errore nel PIN";
				} else {
					textMessage = "errore generico";
				}
				throw new BpmnError("500", "firma non riuscita - " + textMessage);
			}

		}
	}


	private static String getSignedFilename(String filename) {
		String result = filename.substring(0, filename.lastIndexOf('.'));
		result += ".signed";
		result += filename.substring(filename.lastIndexOf('.'));
		return result;
	}

}
