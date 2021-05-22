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

public class Escala implements EventoProgramavelJava {

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
		// TODO Auto-generated method stub
		DynamicVO escVO = (DynamicVO) event.getVo();
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		if(!helper.conGuardaChuva(escVO.asBigDecimal("NUMCONTRATO"))) {
			validapermissao();
		}

	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		// TODO Auto-generated method stub
		DynamicVO escVO = (DynamicVO) event.getVo();
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		if(!helper.conGuardaChuva(escVO.asBigDecimal("NUMCONTRATO"))) {
			validapermissao();	
		}

	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		ModifingFields camposModificados = event.getModifingFields();
		DynamicVO escVO = (DynamicVO) event.getVo();
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		if(!helper.conGuardaChuva(escVO.asBigDecimal("NUMCONTRATO"))) {
			validapermissao();	
		}

		if (camposModificados.isModifingAny("CODCARGAHOR")) {
			throw new Exception("<b>Não é possível alterar a carga horária</b>");
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
