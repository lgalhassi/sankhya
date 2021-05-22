package br.com.sankhya.ctba.promocao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class CalculoDeRepresentatividade implements AcaoRotinaJava {

	JapeWrapper configImpProdDAO = JapeFactory.dao("AD_CFGIMPPROD");
	BigDecimal qtdeTotal = BigDecimal.ZERO;
	BigDecimal vlrTotal = BigDecimal.ZERO;

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {
		Registro regPai = contextoAcao.getLinhaPai();
		BigDecimal codProm = (BigDecimal) regPai.getCampo("CODPROM");
		BigDecimal idConfimp = (BigDecimal) regPai.getCampo("IDCONFIMP");
		BigDecimal codLinha = (BigDecimal) regPai.getCampo("CODLINHA");
		Timestamp dtIni = (Timestamp) regPai.getCampo("DTINI");
		Timestamp dtFim = (Timestamp) regPai.getCampo("DTFIM");

		DynamicVO promocaoVO = getPromocao(codProm);
		String tipo = (String) promocaoVO.asString("TIPOPROMO");
		BigDecimal codEmp = (BigDecimal) promocaoVO.asBigDecimal("CODEMP");

		if ("L".equals(tipo))
			throw new Exception(
					"<br> <b>Cálculo de representatividade não é permitido para promoção de liquidação.</b> <br>");

		if (dtIni == null)
			throw new Exception(
					"<br> <b>Data inicial do cálculo de representatividade não esta preenchida, verifique!</b> <br>");
		else if (dtFim == null)
			throw new Exception(
					"<br> <b>Data final do cálculo de representatividade não esta preenchida, verifique!</b> <br>");

		Collection<DynamicVO> configImpProdutosVOs = getConfigImpProdutoByPromocao(codProm, idConfimp);

		if (configImpProdutosVOs.size() == 0)
			throw new Exception(
					"<br> <b>Não foram encontrados produtos para realizar o cálculo de representatividade.</b> <br>");

		clearCalculoRepresentatividade(configImpProdutosVOs);
		setCalculoRepresentatividade(codEmp, codLinha, dtIni, dtFim, configImpProdutosVOs);
	}

	private DynamicVO getPromocao(BigDecimal codProm) throws Exception {
		final JapeWrapper promocaoDAO = JapeFactory.dao("AD_ROLPRO");
		return promocaoDAO.findByPK(codProm);
	}

	@SuppressWarnings("unchecked")
	private Collection<DynamicVO> getConfigImpProdutoByPromocao(BigDecimal codProm, BigDecimal idConfimp)
			throws Exception {
		FinderWrapper finderUpd = new FinderWrapper("AD_CFGIMPPROD", "CODPROM = ? AND this.IDCONFIMP = ?");

		finderUpd.setFinderArguments(new Object[] { codProm, idConfimp });
		finderUpd.setMaxResults(-1);
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		return dwfFacade.findByDynamicFinderAsVO(finderUpd);
	}

	private void setCalculoRepresentatividade(BigDecimal codEmp, BigDecimal codLinha, Timestamp dtIni, Timestamp dtFim,
			Collection<DynamicVO> configImpProdutosVOs) throws Exception {
		JdbcWrapper jdbc = null;

		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			NativeSql sql = new NativeSql(jdbc);

			sql.resetSqlBuf();
			sql.setScrollableResult(true);
			sql.setNamedParameter("CODEMP", codEmp);
			sql.setNamedParameter("CODLINHA", codLinha);
			sql.setNamedParameter("DTINI", dtIni);
			sql.setNamedParameter("DTFIM", dtFim);
			sql.appendSql(" SELECT CAB.NUNOTA, ITE.CODPROD, ITE.QTDNEG, ITE.VLRTOT");
			sql.appendSql(" FROM TGFCAB CAB");
			sql.appendSql(" INNER JOIN TGFITE ITE ON ITE.NUNOTA = CAB.NUNOTA");
			sql.appendSql(" INNER JOIN TGFPRO PROD ON PROD.CODPROD = ITE.CODPROD");
			sql.appendSql(
					" WHERE CAB.CODEMP = :CODEMP AND CAB.TIPMOV = 'V' AND CAB.DTMOV >= :DTINI AND DTMOV <= :DTFIM AND PROD.CODGRUPOPROD = :CODLINHA");

			ResultSet rs = sql.executeQuery();

			while (rs.next()) {
				qtdeTotal = qtdeTotal.add((BigDecimal) rs.getBigDecimal("QTDNEG"));
				vlrTotal = vlrTotal.add((BigDecimal) rs.getBigDecimal("VLRTOT"));
			}

			rs.beforeFirst();

			while (rs.next()) {
				BigDecimal codProd = rs.getBigDecimal("CODPROD");
				BigDecimal qtdNeg = rs.getBigDecimal("QTDNEG");
				BigDecimal vlrTot = rs.getBigDecimal("VLRTOT");

				Optional<DynamicVO> configImpProdutoVO = configImpProdutosVOs.stream()
						.filter(item -> item.asBigDecimal("CODPROD").equals(codProd)).findFirst();

				if (configImpProdutoVO.isPresent()) {
					qtdNeg = qtdNeg.add(configImpProdutoVO.get().asBigDecimal("QTDEVEND") != null
							? configImpProdutoVO.get().asBigDecimal("QTDEVEND")
							: BigDecimal.ZERO);

					float resQtde = (qtdNeg.floatValue() / qtdeTotal.floatValue()) * 100;
					BigDecimal porcQtdeVend = new BigDecimal(Float.toString(resQtde)).setScale(2,
							BigDecimal.ROUND_HALF_EVEN);
					// (qtdNeg.divide(qtdeTotal).multiply(new BigDecimal(100)));

					vlrTot = vlrTot.add(configImpProdutoVO.get().asBigDecimal("VLRVEND") != null
							? configImpProdutoVO.get().asBigDecimal("VLRVEND")
							: BigDecimal.ZERO);

					float resValor = (vlrTot.floatValue() / vlrTotal.floatValue()) * 100;
					BigDecimal porcVlrVend = new BigDecimal(Float.toString(resValor)).setScale(2,
							BigDecimal.ROUND_HALF_EVEN);

					configImpProdDAO.prepareToUpdate(configImpProdutoVO.get()).set("QTDEVEND", qtdNeg)
							.set("PORCQTDEVEND", porcQtdeVend).set("VLRVEND", vlrTot).set("PORCVLRVEND", porcVlrVend)
							.update();
				}
			}

			configImpProdutosVOs = configImpProdutosVOs.stream().filter(item -> item.asBigDecimal("QTDEVEND") != null)
					.sorted((o1, o2) -> o2.asBigDecimal("QTDEVEND").compareTo(o1.asBigDecimal("QTDEVEND")))
					.collect(Collectors.toList());

			BigDecimal porcQtdeVendAcumulada = BigDecimal.ZERO;
			BigDecimal numSeq = new BigDecimal(1);
			for (DynamicVO configImpProdutoVO : configImpProdutosVOs) {
				porcQtdeVendAcumulada = porcQtdeVendAcumulada.add(configImpProdutoVO.asBigDecimal("PORCQTDEVEND"));

				configImpProdDAO.prepareToUpdate(configImpProdutoVO).set("SEQIMP", numSeq)
						.set("PORCTOTALQTDEVEND", porcQtdeVendAcumulada).update();

				numSeq = numSeq.add(new BigDecimal(1));
			}

			configImpProdutosVOs = configImpProdutosVOs.stream().filter(item -> item.asBigDecimal("VLRVEND") != null)
					.sorted((o1, o2) -> o2.asBigDecimal("VLRVEND").compareTo(o1.asBigDecimal("VLRVEND")))
					.collect(Collectors.toList());

			BigDecimal porcVlrVendAcumulada = BigDecimal.ZERO;
			for (DynamicVO configImpProdutoVO : configImpProdutosVOs) {
				porcVlrVendAcumulada = porcVlrVendAcumulada.add(configImpProdutoVO.asBigDecimal("PORCVLRVEND"));

				configImpProdDAO.prepareToUpdate(configImpProdutoVO).set("PORCTOTALVLRVEND", porcVlrVendAcumulada)
						.update();
			}

//			throw new Exception("<br> <b>SIZE: </b>" + qtdeTotal + " | " + listaProdutos.get(1).getBigDecimal("QTDNEG"));
			rs.close();
		} finally {
			jdbc.closeSession();
		}
	}

	private void clearCalculoRepresentatividade(Collection<DynamicVO> configImpProdutosVOs) throws Exception {
		for (DynamicVO configImpProdutoVO : configImpProdutosVOs) {
			configImpProdDAO.prepareToUpdate(configImpProdutoVO).set("QTDEVEND", null).set("PORCQTDEVEND", null)
					.set("VLRVEND", null).set("PORCVLRVEND", null).set("PORCTOTALQTDEVEND", null)
					.set("PORCTOTALVLRVEND", null).update();
		}
	}
}
