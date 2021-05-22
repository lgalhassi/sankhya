package br.com.sankhya.ctba.divergenciaCompras;

/*********************************************************************************************************************************************************************************************

Autor: Luis Alessandro Galhassi - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 23/04/2021
Objetivo: Após a importação do XML da Nota Fiscal, gerar automaticamente na Tela de Divergência de Compras
Tabela Alvo: AD_DIVNOTACOM
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/


import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class DivergenciaCompras implements EventoProgramavelJava{

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		/**
		 * O objetivo é quando importar o XML , gerar automaticamente na tela de Divergência de Compras
		 * 
		 */
		Timestamp dataSolicitacao;
		BigDecimal notaCompra, parcCompra;
		String serieNota;
				
		DynamicVO xml = (DynamicVO) event.getVo();
		
		BigDecimal codusu = (BigDecimal) JapeSessionContext.getProperty("usuario_logado");
		
		dataSolicitacao = xml.asTimestamp("DHIMPORT");
		notaCompra = xml.asBigDecimal("NUMNOTA");
		serieNota = xml.asString("SERIEDOC");
		parcCompra = xml.asBigDecimal("CODPARC");
		
		//Busca a tabela a ser inserida, com base na instância
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();		
		xml = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AD_DIVNOTACOM");
				
		//inclui os valores desejados nos campos
		xml.setProperty("DATASOLICITACAO", dataSolicitacao);
		xml.setProperty("USUINC", codusu);
		xml.setProperty("NOTACOMPRA", notaCompra);
		xml.setProperty("SERIENOTA", serieNota);
		xml.setProperty("PARCCOMPRA", parcCompra);
		
		//realiza o insert
		dwfEntityFacade.createEntity("AD_DIVNOTACOM", (EntityVO) xml);

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
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	

}
