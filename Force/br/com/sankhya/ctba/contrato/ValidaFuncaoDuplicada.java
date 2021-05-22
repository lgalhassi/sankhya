package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;
import java.util.Collection;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 12/07/2020
Objetivo: Validar postos duplicados no mesmo contrato
Tabela Alvo: AD_POSTOCONTRATO
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class ValidaFuncaoDuplicada implements EventoProgramavelJava {

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
	public void beforeDelete(PersistenceEvent event) throws Exception {
		DynamicVO postoVO = (DynamicVO) event.getVo();
		FuncionarioHelper helper = new FuncionarioHelper();
		
		if(!helper.conGuardaChuva(postoVO.asBigDecimal("NUMCONTRATO"))) {
			validapermissao();	
		}
	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		DynamicVO postoVO = (DynamicVO) event.getVo();
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		if(!helper.conGuardaChuva(postoVO.asBigDecimal("NUMCONTRATO"))) {
			validapermissao();	
		}
		
		checkDuplicados(postoVO.asBigDecimal("CODFUNCAO"), postoVO.asBigDecimal("NUMCONTRATO"));
		

	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		DynamicVO postoVO = (DynamicVO) event.getVo();
		
		ModifingFields camposModificados = event.getModifingFields();

		if (camposModificados.isModifingAny("CODFUNCAO")) {
			throw new Exception("<b>Não é possível alterar a função do posto</b>");
			//checkDuplicados(postoVO.asBigDecimal("CODFUNCAO"), postoVO.asBigDecimal("NUMCONTRATO"));
		}
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		if(!helper.conGuardaChuva(postoVO.asBigDecimal("NUMCONTRATO"))) {
			validapermissao();	
		}
	}
	
	@SuppressWarnings("unchecked")
	private void checkDuplicados(BigDecimal codfuncao, BigDecimal numcontrato) throws Exception {
		//Configura o critério de busca
		FinderWrapper finder = new FinderWrapper("AD_POSTOCONTRATO",
				"this.CODFUNCAO = ? AND this.NUMCONTRATO = ?");
		//Insere os argumentos caso existam	
		finder.setFinderArguments(new Object[] { codfuncao, numcontrato });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		//Realiza a busca na tabela pelos critérios
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("AD_POSTOCONTRATO", finder);
		//Itera entre os registos encontrados
		if (!libCollection.isEmpty()) {
			throw new Exception ("<b>Função já cadastrada</b>");
		} 

	}
	
	private void validapermissao() throws Exception {
		BigDecimal codusu = (BigDecimal) JapeSessionContext.getProperty("usuario_logado");
		
		JapeWrapper usuDAO = JapeFactory.dao("Usuario");
		DynamicVO usuVO = usuDAO.findByPK(codusu);
		
		String flag = usuVO.asString("AD_PERMALTPOSTOS");
		if (flag == null || flag.equals("N")) {
			throw new Exception("<b>Usuário não tem permissão para alterar o Cadastro de Postos</b>");
		}
	}

}
