package br.com.sankhya.ctba.compras;

import java.sql.Timestamp;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class GravaSysdateNaDHConfirmacao implements EventoProgramavelJava{

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		DynamicVO cabVO = (DynamicVO) event.getVo();

		//boolean flagconfirmando = JapeSession.getPropertyAsBoolean("CabecalhoNota.confirmando.nota", Boolean.FALSE).booleanValue();
		boolean flagConfirma = JapeSession.getPropertyAsBoolean("CabecalhoNota.confirmando.nota", Boolean.valueOf(false)).booleanValue();
		
		if(flagConfirma) {
			Timestamp dhatual = (Timestamp) JapeSessionContext.getProperty("dh_atual");
			cabVO.setProperty("AD_DHCONFIRMACAO", dhatual);
		}
	}
}
