package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class PermissoesContrato implements EventoProgramavelJava {

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
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		
		ModifingFields camposModificados = event.getModifingFields();

		if (camposModificados.isModifingAny("CODPARC")) {
			validapermissao();
		}

	}

	private void validapermissao() throws Exception {
		BigDecimal codusu = (BigDecimal) JapeSessionContext.getProperty("usuario_logado");
		
		JapeWrapper usuDAO = JapeFactory.dao("Usuario");
		DynamicVO usuVO = usuDAO.findByPK(codusu);
		
		String flag = usuVO.asString("AD_PERMALTPARCCON");
		if (flag == null || flag.equals("N")) {
			throw new Exception("<b>Usuário não tem permissão para alterar o Cód. do Parceiro</b>");
		}
	}
}
