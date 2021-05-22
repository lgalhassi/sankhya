package br.com.sankhya.ctba.girorolemar;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class EmpresaItens implements EventoProgramavelJava {

	final JapeWrapper rolEpnDAO = JapeFactory.dao("RolEmpresasInativas");

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		handleCreateUpdateEmpresaItens(event);
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		handleCreateUpdateEmpresaItens(event);
	}

	private void handleCreateUpdateEmpresaItens(PersistenceEvent event) throws Exception {
		DynamicVO empresaItensVO = (DynamicVO) event.getVo();

		if ("N".equals(empresaItensVO.asString("PRODATIVO"))) {
			BigDecimal codProd = empresaItensVO.asBigDecimal("CODPROD");
			BigDecimal codEmp = empresaItensVO.asBigDecimal("CODEMP");

			if (findEmpresaInativaByProduto(codProd, codEmp) == null) {
				rolEpnDAO.create().set("CODPROD", codProd).set("CODEMP", codEmp)
						.set("MOTIVO", "Produto marcado como inativo na tela Giro Rolemar").save();
			}
		}
	}

	private DynamicVO findEmpresaInativaByProduto(BigDecimal codProd, BigDecimal codEmp) throws Exception {
		return rolEpnDAO.findOne("this.CODPROD = ? AND this.CODEMP = ?", codProd, codEmp);
	}
}
