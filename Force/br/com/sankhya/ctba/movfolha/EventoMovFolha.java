package br.com.sankhya.ctba.movfolha;

import java.math.BigDecimal;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 30/07/2020
Objetivo: Remover o evento 0 da movimentação da folha
Tabela Alvo: TFPMOV
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class EventoMovFolha implements ScheduledAction {


	@Override
	public void onTime(ScheduledActionContext context) {
		try {
			removeEvento();
			context.log("Evento 0 removido com sucesso!");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void removeEvento() throws Exception {
		
		
		//Configura o critério de busca
		FinderWrapper finderDelete = new FinderWrapper("MovimentoEFixo", 
				"this.CODEVENTO = ? AND this.ORIGEM = ?" );
		finderDelete.setFinderArguments(new Object[] { BigDecimal.ZERO, "B"});
		//Exclui os registros pelo criterio
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		dwfFacade.removeByCriteria(finderDelete);
	}

}
