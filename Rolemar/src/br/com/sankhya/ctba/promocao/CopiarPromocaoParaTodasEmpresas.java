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

public class CopiarPromocaoParaTodasEmpresas implements AcaoRotinaJava {

	final JapeWrapper promocaoDAO = JapeFactory.dao("AD_ROLPRO");

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		Registro[] regs = ctx.getLinhas();

		if (regs.length > 1)
			throw new Exception(
					"<br> <b>Mais de uma promoção selecionada. <br>Por favor selecione somente uma.</b> <br>");

		Registro registro = regs[0];

		DynamicVO promocaoVO = getPromocao((BigDecimal) registro.getCampo("CODPROM"));
		Collection<DynamicVO> empresasVOs = getAllEmpresasRolemar();

		// criar linhas
		// criar config impressao
		// criar config impressao produto
		// criar detalhe de produto
		// throw new Exception("<br> <b>01: </b>" +
		// newPromocaoVO.asBigDecimal("CODEMP"));

		for (DynamicVO empresaVO : empresasVOs) {
			createPromocao(promocaoVO, empresaVO);
		}
	}

	private DynamicVO getPromocao(BigDecimal codProm) throws Exception {
		return promocaoDAO.findByPK(codProm);
	}

	private Collection<DynamicVO> getAllEmpresasRolemar() throws Exception {
		final JapeWrapper empresaDAO = JapeFactory.dao("Empresa");
		return empresaDAO.find("this.CODEMP = 1 OR this.CODEMP = 2 OR this.CODEMP = 3");
	}

	private void createPromocao(DynamicVO promocaoVO, DynamicVO empresaVO) throws Exception {
		BigDecimal codProm = promocaoVO.asBigDecimal("CODPROM");
		BigDecimal codEmp = empresaVO.asBigDecimal("CODEMP");

		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO newPromocaoVO = (DynamicVO) promocaoVO;
		newPromocaoVO.setProperty("CODPROM", null);
		newPromocaoVO.setProperty("CODEMP", codEmp);
		dwfFacade.createEntity("AD_ROLPRO", (EntityVO) newPromocaoVO);

		BigDecimal codPromCreated = newPromocaoVO.asBigDecimal("CODPROM");

		createLinhas(codProm, codPromCreated);
		createConfigImp(codProm, codPromCreated);
	}

	private void createLinhas(BigDecimal codProm, BigDecimal codPromCreated) throws Exception {
		Collection<DynamicVO> linhasVOs = getLinhas(codProm);

		for (DynamicVO linhaVO : linhasVOs) {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO newLinhaVO = (DynamicVO) linhaVO;
			newLinhaVO.setProperty("CODPROM", codPromCreated);
			newLinhaVO.setProperty("CODLINHA", linhaVO.asBigDecimal("CODLINHA"));
			dwfFacade.createEntity("AD_LINHA", (EntityVO) newLinhaVO);

			BigDecimal codLinhaCreated = newLinhaVO.asBigDecimal("CODLINHA");

			createEspecial(codProm, codPromCreated, codLinhaCreated);
		}
	}

	private Collection<DynamicVO> getLinhas(BigDecimal codProm) throws Exception {
		final JapeWrapper linhaDAO = JapeFactory.dao("AD_LINHA");
		return linhaDAO.find("this.CODPROM = ?", codProm);
	}

	private void createEspecial(BigDecimal codProm, BigDecimal codPromCreated, BigDecimal codLinhaCreated)
			throws Exception {
		Collection<DynamicVO> especiaisVOs = getEspeciais(codProm, codLinhaCreated);
		
		for (DynamicVO especialVO : especiaisVOs) {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			
			DynamicVO newEspecialVO = (DynamicVO) especialVO;
			newEspecialVO.setProperty("CODPROM", codPromCreated);
			newEspecialVO.setProperty("CODLINHA", codLinhaCreated);
			
			dwfFacade.createEntity("AD_ESPECIAL", (EntityVO) newEspecialVO);
		}
	}

	private Collection<DynamicVO> getEspeciais(BigDecimal codProm, BigDecimal codLinhaCreated) throws Exception {
		final JapeWrapper especialDAO = JapeFactory.dao("AD_ESPECIAL");
		return especialDAO.find("this.CODPROM = ? AND this.CODLINHA = ?", codProm, codLinhaCreated);
	}

	private void createConfigImp(BigDecimal codProm, BigDecimal codPromCreated) throws Exception {
		Collection<DynamicVO> configImpsVOs = getConfigImps(codProm);

		for (DynamicVO configImpVO : configImpsVOs) {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO newConfigImpVO = (DynamicVO) configImpVO;
			newConfigImpVO.setProperty("CODPROM", codPromCreated);
			dwfFacade.createEntity("AD_CFGIMP", (EntityVO) newConfigImpVO);
			
			BigDecimal idConfImpCreated = newConfigImpVO.asBigDecimal("IDCONFIMP");
			
			createConfigImpProduto(codProm, idConfImpCreated);
		}
	}

	private Collection<DynamicVO> getConfigImps(BigDecimal codProm) throws Exception {
		final JapeWrapper configImpDAO = JapeFactory.dao("AD_CFGIMP");
		return configImpDAO.find("this.CODPROM = ?", codProm);
	}
	
	private void createConfigImpProduto(BigDecimal codProm, BigDecimal idConfImpCreated) throws Exception {
		Collection<DynamicVO> configImpProdsVOs = getConfigImpProds(codProm, idConfImpCreated);
		
		for (DynamicVO configImpProdVO : configImpProdsVOs) {
			
		}
	}

	private Collection<DynamicVO> getConfigImpProds(BigDecimal codProm, BigDecimal idConfImpCreated) throws Exception {
		final JapeWrapper configImpProdDAO = JapeFactory.dao("AD_CFGIMPPROD");
		return configImpProdDAO.find("this.CODPROM = ? AND this.IDCONFIMP = ?", codProm, idConfImpCreated);
	}
}
