package br.com.sankhya.ctba.compras;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class Divergencias implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
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
	public void beforeInsert(PersistenceEvent event) throws Exception {
		// createDivergencia(event);
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		updateDivergencia(event);
	}

	private void updateDivergencia(PersistenceEvent event) throws Exception {
		DynamicVO divergenciaVO = (DynamicVO) event.getVo();
		String tipo = divergenciaVO.asString("TIPO");

		if ("FTR".equals(tipo) || "FIN".equals(tipo))
			verifyProvidenciasIfAllowed(divergenciaVO);

		if (divergenciaVO.asTimestamp("DTVENCSUG") != null) {
			String descProv = divergenciaVO.asString("DESCPROV");
			
			if(descProv == null) 
				descProv = "Dt. Vencimento Sugerida: " + divergenciaVO.asTimestamp("DTVENCSUG");
			else {
				if(!verifyIfVencimentoSugInserted(descProv)) 
					descProv = descProv + "\n" + "Dt. Vencimento Sugerida: " + divergenciaVO.asTimestamp("DTVENCSUG");
			}
			
			divergenciaVO.setProperty("DESCPROV", descProv);
		}
	}

	private void verifyProvidenciasIfAllowed(DynamicVO divergenciaVO) throws Exception {
		if ("S".equals(divergenciaVO.asString("PROVALTVENCPRO"))) 
			throw new Exception("<br> <b>Providência marcada não permitida para a divergência.</b> <br>");
	}

	private boolean verifyIfVencimentoSugInserted(String descProv) throws Exception {
		String dtVencSug = "Dt. Vencimento Sugerida:";
		
		if(descProv.contains(dtVencSug))
			return true;
		
		return false;
	}
}
