package br.com.sankhya.ctba.promocao;

import java.math.BigDecimal;
import java.util.Collection;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class IncluirParceiros implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {
		Registro regPai = contextoAcao.getLinhaPai();

		Collection<DynamicVO> envParcVO = getEnvParcbyPerfil((BigDecimal) regPai.getCampo("PERFIL"));
		if (envParcVO.size() > 0)
			contextoAcao.confirmar("Parceiros encontrados",
					"Alguns parceiros já estão cadastrados para o Envio de Promoções, deseja incluir mesmo assim? ", 1);
		else
			contextoAcao.confirmar("Incluir Parceiros",
					"Deseja realmente incluir todos os parceiros com os filtros acima? ", 1);

		DynamicVO envPromVO = getEnvProm((BigDecimal) regPai.getCampo("PERFIL"));
		BigDecimal codVend = envPromVO.asBigDecimal("CODVEND");
		BigDecimal codVend2 = envPromVO.asBigDecimal("CODVENDDE");
		BigDecimal codCid = envPromVO.asBigDecimal("CODCID");

		Collection<DynamicVO> parceirosVOs = getParceiros(codVend, codVend2, codCid);

		try {
			for (DynamicVO parceiroVO : parceirosVOs) {
				createParceiro(regPai, parceiroVO);
			}
		} finally {
			contextoAcao.setMensagemRetorno("Inclusão dos parceiros realizada com sucesso!");
		}
	}

	private DynamicVO getEnvProm(BigDecimal perfil) throws Exception {
		JapeWrapper envPromDAO = JapeFactory.dao("AD_ENVPROM");
		return envPromDAO.findByPK(perfil);
	}

	private Collection<DynamicVO> getParceiros(BigDecimal codVend, BigDecimal codVend2, BigDecimal codCid)
			throws Exception {
		JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");

		if (codVend2 == null)
			return parceiroDAO.find("this.CODVEND = ? AND this.CODCID = ?", codVend, codCid);

		return parceiroDAO.find("(this.CODVEND = ? OR this.CODVEND = ?) AND this.CODCID = ?", codVend, codVend2,
				codCid);
	}

	private void createParceiro(Registro regPai, DynamicVO parceiroVO) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

		DynamicVO envParVO = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("AD_ENVPAR");
		envParVO.setProperty("PERFIL", regPai.getCampo("PERFIL"));
		envParVO.setProperty("CODPARC", parceiroVO.asBigDecimal("CODPARC"));
		envParVO.setProperty("CNPJ", parceiroVO.asString("CGC_CPF"));
		envParVO.setProperty("NOMEPARC", parceiroVO.asString("NOMEPARC"));
		envParVO.setProperty("RAZSOCPARC", parceiroVO.asString("RAZAOSOCIAL"));
		envParVO.setProperty("BONIFC", parceiroVO.asBigDecimal("ROLBONIFICA"));

		DynamicVO envPromVO = getEnvProm(envParVO.asBigDecimal("PERFIL"));
		BigDecimal codPerfilEmail = envPromVO.asBigDecimal("CODTIPPARC");

		if (codPerfilEmail != null) {
			DynamicVO parceiroEmailVO = getPerfilContatoByCodPerfilEmail(codPerfilEmail);

			envParVO.setProperty("CONTPARC", parceiroEmailVO.asString("NOMECONTATO"));
			envParVO.setProperty("MAILCONT", parceiroEmailVO.asString("EMAIL"));
		}

		dwfFacade.createEntity("AD_ENVPAR", (EntityVO) envParVO);
	}

	private DynamicVO getPerfilContatoByCodPerfilEmail(BigDecimal codTipParc) throws Exception {
		JapeWrapper perfilContatoDAO = JapeFactory.dao("PerfilContato");
		DynamicVO perfilContatoVO = perfilContatoDAO.findOne("this.CODTIPPARC = ?", codTipParc);

		BigDecimal codParc = perfilContatoVO.asBigDecimal("CODPARC");
		return getContatoByPerfilContato(codParc);
	}

	private DynamicVO getContatoByPerfilContato(BigDecimal codParc) throws Exception {
		JapeWrapper contatoDAO = JapeFactory.dao("Contato");
		return contatoDAO.findOne("this.CODPARC = ?", codParc);
	}

	private Collection<DynamicVO> getEnvParcbyPerfil(BigDecimal perfil) throws Exception {
		JapeWrapper envParDAO = JapeFactory.dao("AD_ENVPAR");
		return envParDAO.find("this.PERFIL = ?", perfil);
	}
}
