package br.com.sankhya.ctba.emailnfse_nao_imp;

import java.util.Collection;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 31/07/2020
Objetivo: Alterar a informação de data de embarque no detalhamento de nota
Tabela Alvo: TMDFMG
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class EmailNFSE implements EventoProgramavelJava {

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
		DynamicVO filaVO = (DynamicVO) event.getVo();
		DynamicVO parcVO;
		
		String email;
		
		email = filaVO.asString("EMAIL");
		
		parcVO = getParcVO(email);
		
		if(parcVO != null) {
			filaVO.setProperty("EMAIL", parcVO.getProperty("AD_CONTATOSNFSE"));
		}

	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}
	
	@SuppressWarnings("unchecked")
	private DynamicVO getParcVO(String email) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<DynamicVO> parcList = dwfFacade.findByDynamicFinderAsVO(new FinderWrapper(
				"Parceiro", "this.EMAILNFSE = ? ", new Object[] { email }));

		if(parcList.isEmpty()) {
			return null;
		}
		
		DynamicVO parcVO = parcList.iterator().next();
		return parcVO;
	}

}
