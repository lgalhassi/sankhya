package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 16/07/2020
Objetivo: Tratativa para trocar departamento do funcionario com base nas ocorrencias do contrato
Tabela Alvo: TCSOCC
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class Departamento implements EventoProgramavelJava {

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
	//realiza o rollback do departameto caso a ocorrencia seja excluida
	public void beforeDelete(PersistenceEvent event) throws Exception {
		DynamicVO occVO = (DynamicVO) event.getVo();
		
		FuncionarioHelper helper = new FuncionarioHelper();
		Funcionario func = helper.getFuncionario(occVO.asBigDecimal("CODPROD"));

		if(func == null) {
			return;
		}
		
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("DTOCOR", occVO.asTimestamp("DTOCOR"));
			sql.setNamedParameter("NUMCONTRATO", occVO.asBigDecimal("NUMCONTRATO"));
			sql.setNamedParameter("CODPROD", occVO.asBigDecimal("CODPROD"));
			
			
			sql.appendSql(" SELECT AD_CODDEP AS CODDEP FROM TCSOCC " );
			sql.appendSql(" WHERE NUMCONTRATO = :NUMCONTRATO AND CODPROD = :CODPROD" );
			sql.appendSql(" AND DTOCOR < :DTOCOR" );
			sql.appendSql(" AND ROWNUM = 1" );
			sql.appendSql(" ORDER BY DTOCOR DESC" );
			
			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				helper.insereDepFunc(func.getCodfunc(), func.getCodemp(), rs.getBigDecimal("CODDEP"));
			}
			
		}finally {
			jdbc.closeSession();
		}

	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		DynamicVO occVO = (DynamicVO) event.getVo();
		
		BigDecimal codocc, coddep, numcontrato;
		
		codocc = occVO.asBigDecimal("CODOCOR");
		coddep = occVO.asBigDecimal("AD_CODDEP");
		numcontrato = occVO.asBigDecimal("NUMCONTRATO");
		
		FuncionarioHelper helper = new FuncionarioHelper();
		Funcionario func = helper.getFuncionario(occVO.asBigDecimal("CODPROD"));

		if(func == null) {
			return;
		}
		
		if(!helper.isDepManual(codocc)) {			
			coddep = func.getCoddep();
			occVO.setProperty("AD_CODDEP", coddep);
			//helper.insereDepFunc(func.getCodfunc(), func.getCodemp(), coddep);
		
		}
		else {
			Object inativContrato;
			
			inativContrato =  JapeSessionContext.getProperty("br.com.sankhya.ctba.contrato.inativacao");
			
			
			if(coddep == null && (inativContrato == null || (boolean) inativContrato == false)) {
				
				throw new Exception ("<b> Necessário informar um departamento </b>");
			}
			helper.insereDepFunc(func.getCodfunc(), func.getCodemp(), coddep);
		}
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		DynamicVO occVO = (DynamicVO) event.getVo();
		
		BigDecimal coddep;
		
		ModifingFields camposModificados = event.getModifingFields();

		if (camposModificados.isModifingAny("AD_CODDEP")) {
			throw new Exception("<b>Não é possível realizar a troca do departamento. Por favor lançar a ocorrência novamente</b>");
			
			/*
			 * Removido a pedido da viviane
			 * 
			coddep = occVO.asBigDecimal("AD_CODDEP");
			
			if(coddep != null ) {
				FuncionarioHelper helper = new FuncionarioHelper();
				Funcionario func = helper.getFuncionario(occVO.asBigDecimal("CODPROD"));
				
				helper.insereDepFunc(func.getCodfunc(), func.getCodemp(), coddep);
			}
			*/
		}

	}
	

}

