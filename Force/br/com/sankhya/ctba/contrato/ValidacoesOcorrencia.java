package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 11/01/2021
Objetivo: Alterar a informação de data de embarque no detalhamento de nota
Tabela Alvo: TCSOCC
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class ValidacoesOcorrencia implements EventoProgramavelJava {

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
		DynamicVO occVO = (DynamicVO) event.getVo();
		BigDecimal codserv;
		
		codserv = occVO.asBigDecimal("CODPROD");		
		
		FuncionarioHelper helper = new FuncionarioHelper();
		Funcionario fun = helper.getFuncionario(codserv);
		
		if(fun == null) {
			return;
		}
		
		helper.verificaLancRetroativo(codserv, occVO.asTimestamp("DTOCOR"));

	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

}
