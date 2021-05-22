package br.com.sankhya.ctba.compras;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class DivergenciaMovFinanceira implements EventoProgramavelJava{

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
		setDescricaoProvidencia(event);
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		setDescricaoProvidencia(event);
	}
	
	private void setDescricaoProvidencia(PersistenceEvent event) throws Exception{
		DynamicVO movFinVO = (DynamicVO) event.getVo();
		BigDecimal adCodDiv = movFinVO.asBigDecimal("AD_CODDIV");
		
		if(adCodDiv != null) {
			DynamicVO divergenciaVO = getDivergenciaVO(adCodDiv);
			
			if(divergenciaVO != null) {
				BigDecimal nrUnico = movFinVO.asBigDecimal("NUFIN");
				Timestamp dhVenc = movFinVO.asTimestamp("DTVENC");
				BigDecimal vlrBaixa = movFinVO.asBigDecimal("VLRBAIXA");
				Timestamp dhBaixa = movFinVO.asTimestamp("DHBAIXA");
				BigDecimal codUsuBaixa = movFinVO.asBigDecimal("CODUSUBAIXA");
				
				String descProv = "Duplicata: " + nrUnico + " | Vencimento: " + formatDate(dhVenc) + " | Valor Baixa: " + vlrBaixa;
				
				final JapeWrapper divergenciaDAO = JapeFactory.dao("AD_DIVERGENCIAS2");
				divergenciaDAO.prepareToUpdate(divergenciaVO)
				.set("STATUSDIVER", "2")
				.set("RETORNFIN", dhBaixa)
				.set("USUFIN", codUsuBaixa)
				.set("DESCPROV", descProv)
				.update();
			}
		}
	}
	
	private DynamicVO getDivergenciaVO(BigDecimal adCodDiv) throws Exception{
		final JapeWrapper divergenciaDAO = JapeFactory.dao("AD_DIVERGENCIAS2");
		return divergenciaDAO.findOne("this.CODDIV = ?", adCodDiv);
	}
	
	private String formatDate(Timestamp date) throws Exception{
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		return dateFormat.format(date);
	}
}
