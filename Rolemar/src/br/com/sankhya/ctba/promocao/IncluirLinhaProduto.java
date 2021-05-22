package br.com.sankhya.ctba.promocao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class IncluirLinhaProduto implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {
		Registro regPai = contextoAcao.getLinhaPai();

		BigDecimal codProm = (BigDecimal) regPai.getCampo("CODPROM");
		BigDecimal codEmp = (BigDecimal) regPai.getCampo("CODEMP");
		String tipo = (String) regPai.getCampo("TIPOPROMO");

		DynamicVO promocaoVO = getPromocao(codProm);

		if (promocaoVO != null) {
			Collection<DynamicVO> configImpVOs = getConfigImpByPromocao(codProm);

			try {
				if ("D".equals(tipo)) {
					if (configImpVOs.size() > 0) {
						contextoAcao.confirmar("Linhas encontradas em Configuração de Impressão",
								"Algumas linhas já estão cadastradas na Configuração de Impressão, deseja inserir mesmo assim? ",
								1);
					}

					BigDecimal numSeqImp = new BigDecimal(1);

					Collection<DynamicVO> linhasVOs = getLinhasByPromocao(codProm);
					for (DynamicVO linhaVO : linhasVOs) {
						createConfigImp(linhaVO, numSeqImp);

						Collection<DynamicVO> produtosVOs = getProdutosbyLinha(linhaVO);

//						throw new Exception("<br> <b>PRODUTOS DIARIA: </b>" + produtosVOs.size());

						BigDecimal numSeqImpProd = new BigDecimal(1);

						for (DynamicVO produtoVO : produtosVOs) {
							createConfigProd(linhaVO, produtoVO, numSeqImp, numSeqImpProd);
							numSeqImpProd = numSeqImpProd.add(new BigDecimal(1));
						}

						numSeqImp = numSeqImp.add(new BigDecimal(1));
					}
				} else if ("M".equals(tipo)) {
					checkIfConfigImpExists(configImpVOs);

					for (DynamicVO configImpVO : configImpVOs) {
						Collection<DynamicVO> produtosVOs = getProdutosbyLinha(configImpVO);
//						throw new Exception("<br> <b>PRODUTOS MENSAL: </b>" + produtosVOs.size());

						BigDecimal numSeqImp = configImpVO.asBigDecimal("SEQIMP");
						BigDecimal numSeqImpProd = new BigDecimal(1);

						for (DynamicVO produtoVO : produtosVOs) {
							createConfigProd(configImpVO, produtoVO, numSeqImp, numSeqImpProd);
							numSeqImpProd = numSeqImpProd.add(new BigDecimal(1));
						}
					}
				} else if ("L".equals(tipo)) {
					checkIfConfigImpExists(configImpVOs);

					Collection<DynamicVO> rolEpnVOs = getRolepnByEmpresa(codEmp);
					Collection<DynamicVO> produtoInativosVOs = getProdutosInativosByRolepn(rolEpnVOs);

					for (DynamicVO configImpVO : configImpVOs) {
						BigDecimal numSeqImp = configImpVO.asBigDecimal("SEQIMP");
						BigDecimal codLinha = configImpVO.asBigDecimal("CODLINHA");
						BigDecimal numSeqImpProd = new BigDecimal(1);

						Collection<DynamicVO> produtosInativosEstoquePositivoVOs = filterProdutosInativosByLinha(
								produtoInativosVOs, codLinha, codEmp);

						for (DynamicVO produtoInativoVO : produtosInativosEstoquePositivoVOs) {
							createConfigProd(configImpVO, produtoInativoVO, numSeqImp, numSeqImpProd);
							numSeqImpProd = numSeqImpProd.add(new BigDecimal(1));
						}
					}
				}
			} finally {
				contextoAcao.setMensagemRetorno("Inclusão realizada com sucesso!");
			}
		}
	}

	private DynamicVO getPromocao(BigDecimal codProm) throws Exception {
		final JapeWrapper promocaoDao = JapeFactory.dao("AD_ROLPRO");
		return promocaoDao.findByPK(codProm);
	}

	private Collection<DynamicVO> getConfigImpByPromocao(BigDecimal codProm) throws Exception {
		final JapeWrapper configImpDao = JapeFactory.dao("AD_CFGIMP");
		return configImpDao.find("this.CODPROM = ? ", codProm);
	}

	private Collection<DynamicVO> getLinhasByPromocao(BigDecimal codProm) throws Exception {
		final JapeWrapper linhaDAO = JapeFactory.dao("AD_LINHA");
		return linhaDAO.find("this.CODPROM = ?", codProm);
	}

	private void createConfigImp(DynamicVO linhaVO, BigDecimal numSeqImp) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

		DynamicVO configImpVO = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("AD_CFGIMP");
		configImpVO.setProperty("CODPROM", linhaVO.asBigDecimal("CODPROM"));
		configImpVO.setProperty("SEQIMP", numSeqImp);
		configImpVO.setProperty("CODLINHA", linhaVO.asBigDecimal("CODLINHA"));
		configImpVO.setProperty("SIMCONV", "S");
		configImpVO.setProperty("LINMARC", linhaVO.asBigDecimal("CODLINHA"));
		configImpVO.setProperty("ITENSEST", "S");
		configImpVO.setProperty("MOSTEST", "S");

		dwfFacade.createEntity("AD_CFGIMP", (EntityVO) configImpVO);
	}

	@SuppressWarnings("unchecked")
	private Collection<DynamicVO> getProdutosbyLinha(DynamicVO linhaVO) throws Exception {
		FinderWrapper finderUpd = new FinderWrapper("Produto", "this.CODGRUPOPROD = ? ");

		finderUpd.setFinderArguments(new Object[] { linhaVO.asBigDecimal("CODLINHA") });
		finderUpd.setMaxResults(-1);
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		return dwfFacade.findByDynamicFinderAsVO(finderUpd);
	}

	private void createConfigProd(DynamicVO linhaVO, DynamicVO produtoVO, BigDecimal numSeqImp,
			BigDecimal numSeqImpProd) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

		DynamicVO configProdVO = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("AD_CFGIMPPROD");
		configProdVO.setProperty("CODPROM", linhaVO.asBigDecimal("CODPROM"));
		configProdVO.setProperty("IDCONFIMP", numSeqImp);
		configProdVO.setProperty("REFERENCIA", produtoVO.asString("REFERENCIA"));
		configProdVO.setProperty("CODPROD", produtoVO.asBigDecimal("CODPROD"));
		configProdVO.setProperty("SEQIMP", numSeqImpProd);
		configProdVO.setProperty("DESTAQUE", "S");
		configProdVO.setProperty("MOSTREL", "S");

		dwfFacade.createEntity("AD_CFGIMPPROD", (EntityVO) configProdVO);
	}

	private void checkIfConfigImpExists(Collection<DynamicVO> configImpVOs) throws Exception {
		if (configImpVOs.size() == 0) {
			throw new Exception(
					"<br> <b>Não foi encontrado configurações de impressões cadastradas, verifique!</b> <br>");
		}
	}

	private Collection<DynamicVO> getRolepnByEmpresa(BigDecimal codEmp) throws Exception {
		final JapeWrapper rolEpnDAO = JapeFactory.dao("RolEmpresasInativas");
		return rolEpnDAO.find("this.CODEMP = ?", codEmp);
	}

	private ArrayList<DynamicVO> getProdutosInativosByRolepn(Collection<DynamicVO> rolEpnVOs) throws Exception {
		final JapeWrapper produtoDAO = JapeFactory.dao("Produto");
		ArrayList<DynamicVO> arrayProdutoVOs = new ArrayList<>();

		for (DynamicVO rolEpnVO : rolEpnVOs) {
			arrayProdutoVOs.add(produtoDAO.findOne("this.CODPROD = ?", rolEpnVO.asBigDecimal("CODPROD")));
		}

		return arrayProdutoVOs;
	}

	private Collection<DynamicVO> filterProdutosInativosByLinha(Collection<DynamicVO> produtoVOs, BigDecimal codLinha,
			BigDecimal codEmp) throws Exception {
		Collection<DynamicVO> produtosInativosVOs = produtoVOs.stream()
				.filter(item -> item.asBigDecimal("CODGRUPOPROD").equals(codLinha)).collect(Collectors.toList());

		return getProdutosInativosEstoquePositivo(produtosInativosVOs, codEmp);
	}

	private Collection<DynamicVO> getProdutosInativosEstoquePositivo(Collection<DynamicVO> produtosInativosVOs,
			BigDecimal codEmp) throws Exception {
		final JapeWrapper estoqueDAO = JapeFactory.dao("Estoque");
		ArrayList<DynamicVO> arrayProdutosInativosEstoquePositivoVOs = new ArrayList<>();

		for (DynamicVO produtoInativoVO : produtosInativosVOs) {
			DynamicVO prodEstoqueVO = estoqueDAO.findOne("this.CODEMP = ? AND this.ESTOQUE > 0 AND this.CODPROD = ?",
					codEmp, produtoInativoVO.asBigDecimal("CODPROD"));
			if (prodEstoqueVO != null) {
				arrayProdutosInativosEstoquePositivoVOs.add(produtoInativoVO);
			}
		}

		return arrayProdutosInativosEstoquePositivoVOs;
	}
}
