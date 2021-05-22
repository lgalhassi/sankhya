package br.com.sankhya.ctba.promocao;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.MGECoreParameter;

/*
@author: Jadson Sanches - Sankhya Curitiba
@date: 20/01/2021 

Objetivo: 
Versões: 
	1.0 - 20/01/2021 - Implementação da rotina 
*/

public class ValidaLinhaProduto implements EventoProgramavelJava {
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
		createUpdateLinha(event);
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		createUpdateLinha(event);
	}

	private void createUpdateLinha(PersistenceEvent event) throws Exception {
		final BigDecimal MARGREGRADESC = (BigDecimal) MGECoreParameter.getParameter("MARGREGRADESC");

		DynamicVO linhaProdutoVO = (DynamicVO) event.getVo();
		DynamicVO promocaoVO = getPromocao(linhaProdutoVO.asBigDecimal("CODPROM"));

		String tipo = promocaoVO.asString("TIPOPROMO");
		BigDecimal idAgrup = linhaProdutoVO.asBigDecimal("IDAGRUP");
		BigDecimal codProm = linhaProdutoVO.asBigDecimal("CODPROM");

		if (!"D".equals(tipo)) {
			throw new Exception("<br> <b>Tipo da promoção informada não permite inserir linha.</b> <br>");
		}

		BigDecimal descTot = linhaProdutoVO.asBigDecimal("DESCTOT");
		if (descTot != null) {
			if ((descTot.compareTo(MARGREGRADESC) > 0) == false) {
				throw new Exception(
						"<br> <b> O valor inserido em % DESC TOTAL deve ser maior do que esta cadastrado no parametro MARGREGRADESC, por favor verifique!.</b> <br>");
			}
		}

		setValorMinimoPorAgrupamento(codProm, idAgrup, linhaProdutoVO);
		setDescontoDiaria(MARGREGRADESC, descTot, linhaProdutoVO);
	}

	private void setValorMinimoPorAgrupamento(BigDecimal codProm, BigDecimal idAgrup, DynamicVO linhaProdutoVO)
			throws Exception {
		DynamicVO findOneLinha = getAdLinha(codProm, idAgrup);
		if (findOneLinha != null) {
			linhaProdutoVO.setProperty("VLRMIN", findOneLinha.asBigDecimal("VLRMIN"));
		}
	}

	private void setDescontoDiaria(BigDecimal MARGREGRADESC, BigDecimal descTot, DynamicVO linhaProdutoVO)
			throws Exception {
		BigDecimal porcentagem = new BigDecimal(100);
		BigDecimal diffPorcMARGREGRADESC = porcentagem.subtract(MARGREGRADESC);
		BigDecimal diffPorcDescTot = porcentagem.subtract(descTot);

		float res = (((diffPorcDescTot.floatValue() / diffPorcMARGREGRADESC.floatValue()) - 1) * 100) * -1;

		linhaProdutoVO.setProperty("DESCDIARIA",
				new BigDecimal(Float.toString(res)).setScale(2, BigDecimal.ROUND_HALF_EVEN));
	}

	private DynamicVO getPromocao(BigDecimal codProm) throws Exception {
		JapeWrapper promocaoDAO = JapeFactory.dao("AD_ROLPRO");
		return promocaoDAO.findByPK(codProm);
	}

	private DynamicVO getAdLinha(BigDecimal codProm, BigDecimal idAgrup) throws Exception {
		final JapeWrapper linhaProdutoDAO = JapeFactory.dao("AD_LINHA");
		return linhaProdutoDAO.findOne("this.CODPROM = ? AND this.IDAGRUP = ?", codProm, idAgrup);
	}

}
