package it.cnr.si.flows.ng.listeners;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static it.cnr.si.flows.ng.utils.Enum.Azione.Annullo;
import static it.cnr.si.flows.ng.utils.Enum.Stato.Annullato;

@Component
public class AnnullaDocumento implements ExecutionListener {

    private static final long serialVersionUID = -56001764662303256L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnullaDocumento.class);

    private Expression nomeFileDaAnnullare;


    @Override
    public void notify(DelegateExecution execution) throws Exception {
        if (!execution.getEventName().equals(ExecutionListener.EVENTNAME_TAKE))
            throw new IllegalStateException("Questo Listener accetta solo eventi 'take'.");
        if (nomeFileDaAnnullare.getValue(execution) == null)
            throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaAnnullare' nella process definition (nel Task Listener - Fields).");

        String nomeVariabileFile = (String) nomeFileDaAnnullare.getValue(execution);



        FlowsAttachment att = (FlowsAttachment) execution.getVariable(nomeVariabileFile);
        att.setAzione(Annullo);
        att.addStato(Annullato);
        att.setFilename(nomeVariabileFile + "_annullato_");
        execution.setVariable(nomeVariabileFile, att);

        String isError = (String) execution.getVariable("error");
        if ("true".equals(isError)) {
            throw new IllegalStateException("L'utente ha selezionato l'opzione errore. Rollback.");
        }

    }

}
