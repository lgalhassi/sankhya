package br.com.sankhya.ctba.promocao;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class ConfigImpressao implements EventoProgramavelJava {
	DynamicVO promocaoVO;

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
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
		handleClickSaveButton(event);
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		handleClickSaveButton(event);
	}

	private void handleClickSaveButton(PersistenceEvent event) throws Exception {
		DynamicVO configImpVO = (DynamicVO) event.getVo();

		setLinhaMarca(configImpVO);
		setMostrarEstoque(configImpVO);
		validateRepresentatividadePromoMensal(configImpVO);
	}

	private void setLinhaMarca(DynamicVO configImpVO) throws Exception {
		BigDecimal linha = configImpVO.asBigDecimal("CODLINHA");
		String simConv = configImpVO.asString("SIMCONV");

		if ("S".equals(simConv))
			configImpVO.setProperty("LINMARC", linha);
		else {
			DynamicVO linhaVO = getLinhaGrupoProduto(linha);
			configImpVO.setProperty("LINMARC", linhaVO.asBigDecimal("AD_CODIGO"));
		}
	}

	private DynamicVO getLinhaGrupoProduto(BigDecimal linha) throws Exception {
		JapeWrapper linhaDao = JapeFactory.dao("GrupoProduto");
		return linhaDao.findByPK(linha);
	}

	private void setMostrarEstoque(DynamicVO configImpVO) throws Exception {
		promocaoVO = getPromocao(configImpVO);
		String tipo = promocaoVO.asString("TIPOPROMO");

		if (!"M".equals(tipo))
			configImpVO.setProperty("MOSTEST", "N");
	}

	private DynamicVO getPromocao(DynamicVO configImpVO) throws Exception {
		final JapeWrapper promocaoDao = JapeFactory.dao("AD_ROLPRO");
		DynamicVO promocaoVO = promocaoDao.findByPK(configImpVO.asBigDecimal("CODPROM"));

		return promocaoVO;
	}

	private void validateRepresentatividadePromoMensal(DynamicVO configImpVO) throws Exception {
		if (promocaoVO != null) {
			String tipo = promocaoVO.asString("TIPOPROMO");

			if ("L".equals(tipo)) {
				configImpVO.setProperty("DTINI", null);
				configImpVO.setProperty("DTFIM", null);
			}
		}
	}
}
