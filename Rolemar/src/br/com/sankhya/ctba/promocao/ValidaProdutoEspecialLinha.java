package br.com.sankhya.ctba.promocao;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class ValidaProdutoEspecialLinha implements EventoProgramavelJava {

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
		createUpdateProdutoEspecialLinha(event);
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		createUpdateProdutoEspecialLinha(event);
	}

	private void createUpdateProdutoEspecialLinha(PersistenceEvent event) throws Exception {
		final BigDecimal MARGREGRADESC = (BigDecimal) MGECoreParameter.getParameter("MARGREGRADESC");

		DynamicVO especialVO = (DynamicVO) event.getVo();
		BigDecimal codProm = especialVO.asBigDecimal("CODPROM");
		BigDecimal codLinha = especialVO.asBigDecimal("CODLINHA");
		BigDecimal descTotalEspecial = especialVO.asBigDecimal("DESCTOTAL");

		DynamicVO promocaoVO = getPromocao(codProm);
		if (!"D".equals(promocaoVO.asString("TIPOPROMO"))) {
			throw new Exception("<br> <b>Tipo da promoção informada não permite inserir descontos especiais.</b> <br>");
		}

		DynamicVO LinhaVO = getLinha(codProm, codLinha);
		BigDecimal descTotalLinha = LinhaVO.asBigDecimal("DESCTOT");
		BigDecimal descDiariaLinha = LinhaVO.asBigDecimal("DESCDIARIA");
		BigDecimal sumDescDiariaAndMARGREGRADESC = descDiariaLinha.add(MARGREGRADESC);

		if (descTotalEspecial != null) {
			if ((descTotalEspecial.compareTo(sumDescDiariaAndMARGREGRADESC) > 0) == false) {
				throw new Exception(
						"<br> <b> O valor inserido em % DESC TOTAL deve ser maior do que esta cadastrado no parametro MARGREGRADESC + a % DESC DIARIA da linha, por favor verifique!.</b> <br>");
			}
		}

		setDescontoEspecial(MARGREGRADESC, descTotalLinha, descTotalEspecial, especialVO);
	}

	private DynamicVO getLinha(BigDecimal codProm, BigDecimal codLinha) throws Exception {
		final JapeWrapper linhaDAO = JapeFactory.dao("AD_LINHA");
		return linhaDAO.findOne("this.CODPROM = ? AND this.CODLINHA = ?", codProm, codLinha);
	}

	private DynamicVO getPromocao(BigDecimal codProm) throws Exception {
		JapeWrapper promocaoDAO = JapeFactory.dao("AD_ROLPRO");
		return promocaoDAO.findByPK(codProm);
	}

	private void setDescontoEspecial(BigDecimal MARGREGRADESC, BigDecimal descTotalLinha, BigDecimal descTotalEspecial,
			DynamicVO especialVO) throws Exception {
		BigDecimal porcentagem = new BigDecimal(100);
//		BigDecimal diffPorcMARGREGRADESC = porcentagem.subtract(MARGREGRADESC); // 90
//		float diffPorcDescDiaria = diffPorcMARGREGRADESC.floatValue() - ((descDiaria.floatValue() / 100) * 100); // 85
//		float diffPorcDescTotal = 100 - (descTotal.floatValue() / 100) * 100; // 75
//		float res = ((diffPorcDescTotal / diffPorcDescDiaria) - 1) * (-100);

		float subtPorcDescTotalLinha = porcentagem.floatValue() - descTotalLinha.floatValue(); // 100 - 15 = 85
		float subtPorcDescTotalEspecial = porcentagem.floatValue() - descTotalEspecial.floatValue(); // 100 - 23,50 = 76,50
		float res = ((subtPorcDescTotalEspecial / subtPorcDescTotalLinha) - 1) * -100; //10

		 especialVO.setProperty("DESCESP", new BigDecimal(Float.toString(res)).setScale(2, BigDecimal.ROUND_HALF_EVEN));
	}
}
