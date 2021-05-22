package br.com.sankhya.ctba.promocao;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class ConfigProduto implements EventoProgramavelJava {

	private DynamicVO getProduto(DynamicVO configProdutoVO) throws Exception {
		JapeWrapper produtoDAO = JapeFactory.dao("Produto");
		DynamicVO produtoVO = produtoDAO.findByPK(configProdutoVO.asBigDecimal("CODPROD"));

		if (produtoVO == null) {
			throw new Exception("<br> <b>Produto não encontrado</b> <br>");
		}

		return produtoVO;
	}

	private void preencheReferencia(PersistenceEvent event) throws Exception {
		DynamicVO configProdutoVO = (DynamicVO) event.getVo();
		DynamicVO produtoVO = getProduto(configProdutoVO);
		
		configProdutoVO.setProperty("REFERENCIA", produtoVO.asString("REFERENCIA"));
	}

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
		preencheReferencia(event);
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		preencheReferencia(event);
	}

}
