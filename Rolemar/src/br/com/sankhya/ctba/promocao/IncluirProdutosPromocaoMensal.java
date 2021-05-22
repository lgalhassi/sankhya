package br.com.sankhya.ctba.promocao;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.PrePersistEntityState;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class IncluirProdutosPromocaoMensal implements AcaoRotinaJava {

	private BigDecimal agrupmin;

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {
		Timestamp dAtual = (Timestamp) JapeSessionContext.getProperty("d_atual");
		Collection<DynamicVO> promocaoVOs = getPromocoesMensal(dAtual);

		if (promocaoVOs.size() == 0)
			throw new Exception("<br> <b>Não foram encontradas promoções mensais ativas. </b> <br>");

		String codLinha = (String) contextoAcao.getParam("P_LINHA");

		Registro[] regs = contextoAcao.getLinhas();
		Registro registro = regs[0];
		BigDecimal nuNota = (BigDecimal) registro.getCampo("NUNOTA");

		Collection<DynamicVO> produtosVOs = getProdutosbyLinha(codLinha);

		for (DynamicVO produtoVO : produtosVOs) {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			agrupmin = produtoVO.asBigDecimal("AGRUPMIN");
			if (agrupmin == null || agrupmin == BigDecimal.ZERO) {
				agrupmin = new BigDecimal(1);
			}

			DynamicVO iteVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("ItemNota");
			iteVO.setProperty("CODPROD", produtoVO.asBigDecimal("CODPROD"));
			iteVO.setProperty("CODVOL", produtoVO.asString("CODVOL"));
			iteVO.setProperty("QTDNEG", agrupmin);

			AuthenticationInfo authInfo = AuthenticationInfo.getCurrent();

			CACHelper helper = new CACHelper();

			Collection<PrePersistEntityState> itensStateColl = new ArrayList<PrePersistEntityState>();
			itensStateColl.add(PrePersistEntityState.build(dwfEntityFacade, "ItemNota", iteVO));
			helper.incluirAlterarItem(nuNota, authInfo, itensStateColl, true);
		}

		contextoAcao.setMensagemRetorno("Produtos incluidos com sucesso!");
	}

	private Collection<DynamicVO> getPromocoesMensal(Timestamp dToday) throws Exception {
		final JapeWrapper promocaoDAO = JapeFactory.dao("AD_ROLPRO");
		return promocaoDAO.find("this.TIPOPROMO = ? AND this.DTINI <= ? AND (this.DTFIM >= ? OR this.DTFIM is null)" , "M", dToday, dToday);
	}

	private Collection<DynamicVO> getProdutosbyLinha(String codLinha) throws Exception {
		final JapeWrapper produtoDAO = JapeFactory.dao("Produto");
		return produtoDAO.find("this.CODGRUPOPROD = ?", codLinha);
	}

}
