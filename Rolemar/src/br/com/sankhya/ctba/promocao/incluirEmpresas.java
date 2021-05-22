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

public class incluirEmpresas implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		Registro regPai = ctx.getLinhaPai();
		BigDecimal codProm = (BigDecimal) regPai.getCampo("CODPROM");
		
		Collection<DynamicVO> empresasVOs = getAllEmpresas();
		
		try {
			for (DynamicVO empresaVO : empresasVOs) {
				BigDecimal codEmp = empresaVO.asBigDecimal("CODEMP");
				createEmpresasInPromocao(codProm, codEmp);
			}
		} finally {
			ctx.setMensagemRetorno("Inclusão de todas as empresas realizada com sucesso!");
		}
	}
	
	private void createEmpresasInPromocao(BigDecimal codProm, BigDecimal codEmp) throws Exception{
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		
		DynamicVO adEmpresaVO = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("AD_EMPRESA");
		adEmpresaVO.setProperty("CODPROM", codProm);
		adEmpresaVO.setProperty("CODEMP", codEmp);
		
		dwfFacade.createEntity("AD_EMPRESA", (EntityVO) adEmpresaVO);
	}

	private Collection<DynamicVO> getAllEmpresas() throws Exception{
		final JapeWrapper empresaDAO = JapeFactory.dao("Empresa");
		return empresaDAO.find("this.CODEMP IS NOT NULL");
	}
}
