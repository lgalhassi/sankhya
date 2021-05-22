package br.com.sankhya.ctba.promocao;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

/*
@author: Jadson Sanches - Sankhya Curitiba
@date: 20/01/2021 

Objetivo: 
Versões: 
	1.0 - 20/01/2021 - Implementação da rotina 
*/

public class ValidaPromocao implements EventoProgramavelJava {

	private void validaTipoPromocao(DynamicVO promocaoVO) throws Exception {
		String tipo = promocaoVO.asString("TIPOPROMO");
		Timestamp dtIni = promocaoVO.asTimestamp("DTINI");

		if ("D".equals(tipo)) {
			if (dtIni == null) {
				throw new Exception("<br> <b>Data Inicial não foi preenchida.</b> <br>");
			}

			promocaoVO.setProperty("DTFIM", dtIni);
		} else if ("M".equals(tipo)) {
			if (dtIni == null) {
				throw new Exception("<br> <b>Data Inicial não foi preenchida.</b> <br>");
			}
		} else if ("L".equals(tipo)) {
			promocaoVO.setProperty("DTINI", null);
			promocaoVO.setProperty("DTFIM", null);
		}
	}

	private void verificaSeTemLinhas(DynamicVO promocaoVO) throws Exception {
		String tipo = promocaoVO.asString("TIPOPROMO");
		BigDecimal codProm = promocaoVO.asBigDecimal("CODPROM");

		if (!"D".equals(tipo)) {
			JapeWrapper linhaDAO = JapeFactory.dao("AD_LINHA");
			DynamicVO findOneLinha = linhaDAO.findOne("this.CODPROM = ?", codProm);

			if (findOneLinha != null) {
				throw new Exception("<br> <b>Existe linhas cadastradas para essa promoção.</b> <br>"
						+ "<b>Antes de alterar o tipo da promoção, verifique as linhas.</b> <br>");
			}
		}
	}

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void afterUpdate(PersistenceEvent event) throws Exception {

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
		DynamicVO promocaoVO = (DynamicVO) event.getVo();

		validaTipoPromocao(promocaoVO);
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		DynamicVO promocaoVO = (DynamicVO) event.getVo();

		validaTipoPromocao(promocaoVO);
		verificaSeTemLinhas(promocaoVO);
	}

}
