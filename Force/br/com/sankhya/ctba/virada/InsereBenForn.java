package br.com.sankhya.ctba.virada;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class InsereBenForn implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao acao) throws Exception {
		
		insereBen(new BigDecimal(1.00));
		insereBen(new BigDecimal(2.00));
		
		acao.setMensagemRetorno("Terminasse!");
	}
	
	
	
	private void insereBen(BigDecimal codtbe) throws Exception {
		JdbcWrapper jdbc = null;
		
		BigDecimal numcontrato, codposto;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("CODTBE", codtbe);
			
			sql.appendSql(" SELECT POSTO.NUMCONTRATO, POSTO.CODPOSTO FROM AD_POSTOCONTRATO POSTO " );
			sql.appendSql(" LEFT JOIN AD_BENEFFORN BEN ON (POSTO.NUMCONTRATO = BEN.NUMCONTRATO AND POSTO.CODPOSTO = BEN.CODPOSTO AND BEN.CODTBE = :CODTBE)" );
			sql.appendSql(" WHERE BEN.CODTBE IS NULL " );

			
			ResultSet rs = sql.executeQuery();
			
			while (rs.next()) {
				numcontrato = rs.getBigDecimal("NUMCONTRATO");
				codposto = rs.getBigDecimal("CODPOSTO");
				
				//Busca a tabela a ser inserida, com base na instância
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				DynamicVO benVO;

				benVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AD_BENEFFORN");
				//inclui os valores desejados nos campos
				benVO.setProperty("NUMCONTRATO", numcontrato);
				benVO.setProperty("CODPOSTO", codposto);
				benVO.setProperty("CODTBE", codtbe);

				//realiza o insert
				dwfEntityFacade.createEntity("AD_BENEFFORN", (EntityVO) benVO);
			}
			
		}finally {
			jdbc.closeSession();
		}
		
	}

}
