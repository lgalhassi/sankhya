//TGFCAB
package br.com.sankhya.ctba.incrementoContrato;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class IncrementoContrato implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		
		DynamicVO cabVo = (DynamicVO) event.getVo();
		
		Timestamp dtFatur;
		
		dtFatur = cabVo.asTimestamp("DTFATUR");
		
		if(dtFatur != null) {
			
			cabVo.setProperty("DTFATUR", null);
		}
		

	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		
		String statusNFSe;
		BigDecimal codParc, numContrato, nuNota;
		
		ModifingFields camposModificados = event.getModifingFields();
		
		///esta na confirmação da nota apenas para teste
		// deverá voltar para o STATUSNFSE depois das vlaidações
		if (camposModificados.isModifingAny("DTFATUR") && camposModificados.getNewValue("DTFATUR") != null) {
		//if(camposModificados.isModifingAny("STATUSNFSE")){
			
			DynamicVO cabVO = (DynamicVO) event.getVo();
			
			statusNFSe = cabVO.asString("STATUSNFSE");
			codParc = cabVO.asBigDecimal("CODPARC");
			numContrato = cabVO.asBigDecimal("NUMCONTRATO");
			nuNota = cabVO.asBigDecimal("NUNOTA");
			
			if(numContrato.compareTo(BigDecimal.ZERO) == 0)
				return;
			
			//if(statusNFSe != null && statusNFSe.equals("A")) {
				
				cabVO.setProperty("AD_NUMFATCON",buscaSequencia(codParc, numContrato, nuNota));
				
				
			//}
			
		}


	}

	private BigDecimal buscaSequencia(BigDecimal codParc, BigDecimal numContrato, BigDecimal nuNota) throws Exception {
		
		BigDecimal indice = BigDecimal.ZERO;
		
		JdbcWrapper jdbc = null;

		try {
		jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

		NativeSql sql = new NativeSql(jdbc);
		sql.setNamedParameter("NUMCONTRATO", numContrato);
		sql.setNamedParameter("CODPARC", codParc);
		sql.setNamedParameter("NUNOTA", nuNota);

		sql.appendSql("SELECT COUNT(NUNOTA) AS COUNT ");
		sql.appendSql("  FROM TGFCAB ");
		sql.appendSql(" WHERE NUMCONTRATO = :NUMCONTRATO AND CODPARC = :CODPARC ");
		sql.appendSql("   AND NUNOTA <> :NUNOTA");

		ResultSet rs = sql.executeQuery();

		if (rs.next()) {
			
			indice = rs.getBigDecimal("COUNT");
		
		}

		}finally {
			jdbc.closeSession();
		}
		
		//validaçao incluida para desconsiderar contratos do sistema antigo
		//a comissao é paga somente na segunda parcela, desta forma a funcao ira retornar 3 como faturamento
		//e a formula de comissao sera desconsiderada
		if (indice.compareTo(BigDecimal.ZERO) == 0) {
			if (!pagaComissao(numContrato)) {
				indice = new BigDecimal(2.00);
			}
		}	
		
		return indice.add(BigDecimal.ONE);
	}
	
	private boolean pagaComissao(BigDecimal numcontrato) throws Exception {
		//Busca o registro pela PK
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity destEntity = dwfFacade.findEntityByPrimaryKey("Contrato",
				new Object[] { numcontrato });
		DynamicVO conVO = (DynamicVO) destEntity.getValueObject();

		String flag = conVO.asString("AD_PAGARCOMISSAO");

		if (flag != null && flag.equals("S"))
			return true;
		else
			return false;
		
	}

}
