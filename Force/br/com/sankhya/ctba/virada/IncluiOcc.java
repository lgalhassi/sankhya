package br.com.sankhya.ctba.virada;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class IncluiOcc implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {
		//Coleta dos parâmetros da tela
		//dtFim = (Timestamp) contextoAcao.getParam("DTFIM");
		//codprod = new BigDecimal((String) (contextoAcao.getParam("CODPROD")) );

		BigDecimal numcontrato, codparc;
		
		Registro[] registros = contextoAcao.getLinhas();

		for (Registro registro : registros) {
			//recupera os valores das linhas selecionadas
			numcontrato = (BigDecimal) registro.getCampo("NUMCONTRATO");
			codparc = (BigDecimal) registro.getCampo("CODPARC");

			insereOcc(numcontrato, codparc);
		}

		//insere mensagem de retorno no final da execução
		contextoAcao.setMensagemRetorno("Ocorrências inseridas!");


	}
	
	private void insereOcc(BigDecimal numcontrato, BigDecimal codparc) throws Exception {
		JdbcWrapper jdbc = null;
		BigDecimal codprod, coddep;
		Timestamp data;
		
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numcontrato);
			
			sql.appendSql(" SELECT CODPROD, to_date('01/09/2020') AS DATA FROM TCSPSC " );
			sql.appendSql(" WHERE NUMCONTRATO = :NUMCONTRATO " );
			sql.appendSql(" AND CODPROD NOT IN (SELECT CODPROD FROM TCSOCC WHERE NUMCONTRATO = :NUMCONTRATO)" );

			
			ResultSet rs = sql.executeQuery();
			
			while (rs.next()) {
				
				codprod = rs.getBigDecimal("CODPROD");
				data = rs.getTimestamp("DATA");
				
				coddep = getCoddep(codprod);
				
				//Busca a tabela a ser inserida, com base na instância
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				DynamicVO occVO;

				occVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("OcorrenciaContrato");
				//inclui os valores desejados nos campos
				occVO.setProperty("NUMCONTRATO", numcontrato);
				occVO.setProperty("CODPROD", codprod);
				occVO.setProperty("CODPARC", codparc);
				occVO.setProperty("DTOCOR", data);
				occVO.setProperty("CODUSU", BigDecimal.ZERO);
				occVO.setProperty("CODCONTATO", BigDecimal.ONE);
				occVO.setProperty("CODOCOR", BigDecimal.ONE);
				occVO.setProperty("DESCRICAO", "Ativação no contrato guarda-chuva");
				occVO.setProperty("AD_CODDEP", coddep);				
				
				//realiza o insert
				dwfEntityFacade.createEntity("OcorrenciaContrato", (EntityVO) occVO);
			}
			
		}finally {
			jdbc.closeSession();
		}
	}
	
	private BigDecimal getCoddep(BigDecimal codprod) throws Exception {
		JdbcWrapper jdbc = null;
		BigDecimal coddep = BigDecimal.ZERO;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("CODPROD", codprod);
			
			sql.appendSql(" SELECT CODDEP FROM TFPFUN WHERE AD_CODPRODREF = :CODPROD" );

			
			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				coddep = rs.getBigDecimal("CODDEP");
			}
			
		}finally {
			jdbc.closeSession();
		}
		
		return coddep;
	}

}

