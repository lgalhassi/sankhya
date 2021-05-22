package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 12/07/2020
Objetivo: Criar serviço atrelado ao cadastro de funcionário
Tabela Alvo: TFPFUN
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class VinculoFuncServico implements EventoProgramavelJava {

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
		
		criaFuncionario( event);

	}



	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		DynamicVO funVO = (DynamicVO) event.getVo();
		
		Timestamp dtdem;
		BigDecimal codserv;
		
		ModifingFields camposModificados = event.getModifingFields();

		if (camposModificados.isModifingAny("DTDEM")) {
			dtdem = funVO.asTimestamp("DTDEM");
			codserv = funVO.asBigDecimal("AD_CODPRODREF");
			
			if (codserv == null)
				return;
			
			if(dtdem == null)
				ativaServ(codserv, "S");
			else
				ativaServ(codserv, "N");
		}


	}
	
	private void criaFuncionario(PersistenceEvent event) throws Exception {
		
		String nomeFunc;
		BigDecimal codprod, codfunc, codemp;
		
		DynamicVO funVO = (DynamicVO) event.getVo();
		
		nomeFunc = funVO.asString("NOMEFUNC");
		codfunc = funVO.asBigDecimal("CODFUNC");
		codemp = funVO.asBigDecimal("CODEMP");
		
		codprod = insereServico(nomeFunc, codfunc, codemp);
		
		funVO.setProperty("AD_CODPRODREF", codprod);
		
	}

	private BigDecimal insereServico(String nomeFunc, BigDecimal codfunc, BigDecimal codemp) throws Exception {
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO servVO;
			
		servVO = (DynamicVO) dwfEntityFacade
						.getDefaultValueObjectInstance("Servico");
		servVO.setProperty("CODPROD", null);
		servVO.setProperty("DESCRPROD", nomeFunc); // Descrição
		servVO.setProperty("CODVOL", "UN"); // Unidade Padrão
		servVO.setProperty("CODGRUPOPROD", new BigDecimal("0107000"));//Grupo 
		servVO.setProperty("CODLST", new BigDecimal("1101") );	//Tipo de Serviço
		servVO.setProperty("AD_CODFUNC", codfunc );
		servVO.setProperty("AD_CODEMP_FUNC", codemp );
		servVO.setProperty("ATIVO", "S" );
		
		
		//precisa Alterar o Tipo de serviço !
		
		dwfEntityFacade.createEntity("Servico", (EntityVO) servVO);

		BigDecimal novaPK = (BigDecimal) servVO.getProperty("CODPROD");
		
		return novaPK;
	}
	
	private void ativaServ(BigDecimal codserv, String ativo) throws Exception {
		//Busca o registro pela PK
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity prodEntity = dwfFacade.findEntityByPrimaryKey("Servico",
				new Object[] { codserv });
		DynamicVO servVO = (DynamicVO) prodEntity.getValueObject();

		//setar propriedades à serem atualizadas
		servVO.setProperty("ATIVO", ativo);

		//realiza o update
		prodEntity.setValueObject((EntityVO) servVO);

	}

}
