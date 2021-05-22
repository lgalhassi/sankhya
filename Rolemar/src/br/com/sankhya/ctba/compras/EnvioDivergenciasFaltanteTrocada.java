package br.com.sankhya.ctba.compras;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class EnvioDivergenciasFaltanteTrocada implements AcaoRotinaJava {

	JapeWrapper divergenciaCompraDAO;
	BigDecimal codDivCompras;

	@Override
	public void doAction(ContextoAcao ctxAcao) throws Exception {
		Boolean existeDocumentosRelacionados = false;
		Registro[] registros = ctxAcao.getLinhas();

		if (registros.length == 0)
			throw new Exception(
					"<br> <b>Nenhum recebimento selecionado para o envio da divergência faltante/trocada.</b> <br>");
		else if (registros.length > 1)
			throw new Exception(
					"<br> <b>Vários recebimentos selecionados para o envio da divergência faltante/trocada. Por favor selecione apenas um.</b> <br>");

		Registro registro = registros[0];

		BigDecimal nuRecebimento = (BigDecimal) registro.getCampo("NURECEBIMENTO");
		Collection<DynamicVO> itensRecebimentoVOs = getItensRecebimentosByNuRecebimento(nuRecebimento);

		for (DynamicVO itensRecebimentoVO : itensRecebimentoVOs) {
			BigDecimal nuNota = itensRecebimentoVO.asBigDecimal("NUNOTA");

			DynamicVO documentosRelacionadosVO = getDocumentoRelacionado(nuNota);

//			throw new Exception("<br> <b>01: </b>" + itensRecebimentoVOs.size());

			if (documentosRelacionadosVO != null) {
				existeDocumentosRelacionados = true;
				DynamicVO cabVO = getNota(nuNota);

				if (cabVO.asString("TIPMOV").equals("C") || cabVO.asString("TIPMOV").equals("T")) {
					BigDecimal numNota = cabVO.asBigDecimal("NUMNOTA");
					String serieNota = cabVO.asString("SERIENOTA");
					BigDecimal codParc = cabVO.asBigDecimal("CODPARC");

					DynamicVO divergenciaCompraVO = getDivergenciaCompraByNuNota(nuNota);

					if (divergenciaCompraVO != null)
						createDivergenciaFaltanteTrocada(divergenciaCompraVO.asBigDecimal("CODDIVCOMPRAS"), numNota,
								nuNota, documentosRelacionadosVO.asBigDecimal("QTDATENDIDA"),
								cabVO.asBigDecimal("CODEMP"));
					else {
						createDivergenciaCompras(numNota, serieNota, codParc);
						createDivergenciaFaltanteTrocada(codDivCompras, numNota, nuNota,
								documentosRelacionadosVO.asBigDecimal("QTDATENDIDA"), cabVO.asBigDecimal("CODEMP"));
					}
				}
			}
		}

		if (existeDocumentosRelacionados)
			ctxAcao.setMensagemRetorno("Divergências criadas com suceso!");
		else
			ctxAcao.setMensagemRetorno("Não existe divergências para as notas do recebimento selecionado!");
	}

	private Collection<DynamicVO> getItensRecebimentosByNuRecebimento(BigDecimal nuRecebimento) throws Exception {
		final JapeWrapper itemNotaRecebimentoDAO = JapeFactory.dao("ItemNotaRecebimento");
		return itemNotaRecebimentoDAO.find("this.NURECEBIMENTO = ? ", nuRecebimento).stream()
				.filter(distinctByKey(p -> p.asBigDecimal("NUNOTA"))).collect(Collectors.toList());
	}

	private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	private DynamicVO getDivergenciaCompraByNuNota(BigDecimal nuNota) throws Exception {
		DynamicVO cabVO = getNota(nuNota);
		BigDecimal numNota = cabVO.asBigDecimal("NUMNOTA");

		divergenciaCompraDAO = JapeFactory.dao("AD_DIVNOTACOM");
		return divergenciaCompraDAO.findOne("this.NOTACOMPRA = ?", numNota);
	}

	private DynamicVO getNota(BigDecimal nuNota) throws Exception {
		final JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");
		return cabDAO.findByPK(nuNota);
	}

	private DynamicVO getDocumentoRelacionado(BigDecimal nuNota) throws Exception {
		final JapeWrapper docRelacionadoDAO = JapeFactory.dao("CompraVendavariosPedido");
		return docRelacionadoDAO.findOne("this.NUNOTAORIG = ?", nuNota);
	}

	private void createDivergenciaCompras(BigDecimal numNota, String serieNota, BigDecimal codParc) throws Exception {
		BigDecimal codusu = (BigDecimal) JapeSessionContext.getProperty("usuario_logado");
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

		DynamicVO divergenciaVO = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("AD_DIVNOTACOM");
		divergenciaVO.setProperty("USUINC", codusu);
		divergenciaVO.setProperty("NOTACOMPRA", numNota);
		divergenciaVO.setProperty("SERIENOTA", serieNota);
		divergenciaVO.setProperty("PARCCOMPRA", codParc);

		dwfFacade.createEntity("AD_DIVNOTACOM", (EntityVO) divergenciaVO);

		codDivCompras = divergenciaVO.asBigDecimal("CODDIVCOMPRAS");
	}

	private void createDivergenciaFaltanteTrocada(BigDecimal codDivCompras, BigDecimal numNota, BigDecimal nuNota,
			BigDecimal qtdAtendida, BigDecimal codEmp) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		BigDecimal nextCodDiv = new BigDecimal(getNextCodDiv());

		DynamicVO divergenciaVO = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("AD_DIVERGENCIAS2");
		divergenciaVO.setProperty("CODDIVCOMPRAS", codDivCompras);
		divergenciaVO.setProperty("CODDIV", nextCodDiv);
		divergenciaVO.setProperty("NRNOTA", numNota);
		divergenciaVO.setProperty("TIPO", "FTR");
		divergenciaVO.setProperty("DESCIRREG",
				buildDescIrregularidade(nuNota, numNota, qtdAtendida, codEmp).toCharArray());

		dwfFacade.createEntity("AD_DIVERGENCIAS2", (EntityVO) divergenciaVO);
	}

	private int getNextCodDiv() throws Exception {
		JapeWrapper divergenciaDAO = JapeFactory.dao("AD_DIVERGENCIAS2");
		int num = 1;

		DynamicVO divergenciaVO = divergenciaDAO.findOne("this.CODDIV = ?", num);
		while (divergenciaVO != null) {
			num += 1;
			divergenciaVO = divergenciaDAO.findOne("this.CODDIV = ?", num);
		}

		return num;
	}

	private String buildDescIrregularidade(BigDecimal nuNota, BigDecimal numNota, BigDecimal qtdAtendida,
			BigDecimal codEmp) throws Exception {
		String descIrregularidade = "";

		Collection<DynamicVO> itensNotaVOs = getItensNota(nuNota, codEmp);
		for (DynamicVO itemNotaVO : itensNotaVOs) {
			DynamicVO produtoVO = getProduto(itemNotaVO.asBigDecimal("CODPROD"));

			String descProd = produtoVO.asString("DESCRPROD");
			BigDecimal vlrUnit = itemNotaVO.asBigDecimal("VLRUNIT");
			BigDecimal vlrIpi = itemNotaVO.asBigDecimal("VLRIPI");
			BigDecimal vlrSt = itemNotaVO.asBigDecimal("VLRSUBST");
			BigDecimal sumVlrIpiSt = new BigDecimal(vlrIpi.floatValue() + vlrSt.floatValue());

			descIrregularidade = descIrregularidade + descProd + ", Vlr. Uni: " + vlrUnit + ", Vlr. IPI: " + vlrIpi
					+ ", Vlr. ST: " + vlrSt + ", total: " + sumVlrIpiSt + ", Qtde. Produtos Falt/Troc: " + qtdAtendida
					+ "\n";
		}

		return descIrregularidade;
	}

	private Collection<DynamicVO> getItensNota(BigDecimal nuNota, BigDecimal codEmp) throws Exception {
		final JapeWrapper itemNotaDAO = JapeFactory.dao("ItemNota");
		return itemNotaDAO.find("this.NUNOTA = ? AND this.CODEMP = ?", nuNota, codEmp);
	}

	private DynamicVO getProduto(BigDecimal codProd) throws Exception {
		final JapeWrapper produtoDAO = JapeFactory.dao("Produto");
		return produtoDAO.findByPK(codProd);
	}
}
