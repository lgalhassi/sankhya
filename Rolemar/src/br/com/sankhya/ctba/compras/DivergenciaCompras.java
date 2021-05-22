package br.com.sankhya.ctba.compras;

import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class DivergenciaCompras implements EventoProgramavelJava {

	Date dtVencimento;
	SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
	String codProdutoSistema;
	String codProdutoXML; 

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		createDivergencia(event);
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
		//createDivergenciaCompras(event);
		/**
		 * estava com problemas quando repetia numero de nota fiscal
		 * foi retirado por Luis e exigido a digitação
		 */
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		// createUpdateDivergenciaCompras(event);
	}

	private void createDivergenciaCompras(PersistenceEvent event) throws Exception {
		DynamicVO divergenciaComprasVO = (DynamicVO) event.getVo();
		BigDecimal numNota = divergenciaComprasVO.asBigDecimal("NOTACOMPRA");

		DynamicVO cabVO = getNotaByNumNota(numNota);
		if (cabVO != null) {
			divergenciaComprasVO.setProperty("SERIENOTA", cabVO.asString("SERIENOTA"));
			divergenciaComprasVO.setProperty("PARCCOMPRA", cabVO.asBigDecimal("CODPARC"));
		}
	}

	private void createDivergencia(PersistenceEvent event) throws Exception {
		DynamicVO divergenciaComprasVO = (DynamicVO) event.getVo();
		BigDecimal numNota = divergenciaComprasVO.asBigDecimal("NOTACOMPRA");
		String serieNota = divergenciaComprasVO.asString("SERIENOTA");
		BigDecimal parcCompra = divergenciaComprasVO.asBigDecimal("PARCCOMPRA");

		BigDecimal codDivCompras = divergenciaComprasVO.asBigDecimal("CODDIVCOMPRAS");

		// DynamicVO cabVO = getNota(numNota);
		// BigDecimal nuNota = cabVO.asBigDecimal("NUNOTA");

		DynamicVO impXmlVO = getImpXmlByNumNota(numNota);
		if (impXmlVO != null) {
			String config = impXmlVO.asString("CONFIG");
			Timestamp dhEmiss = null;

			if (impXmlVO.asTimestamp("DHEMISS") != null)
				dhEmiss = impXmlVO.asTimestamp("DHEMISS");

			List<String> tags = getAttributesCabecalho(config);
			for (String tag : tags) {		
				
				//createDivergenciaItem(codDivCompras, numNota, "PRE", config, dhEmiss);

				if (tag.equals("DIVERGENCIAFINANCEIRO"))
					createDivergenciaItem(codDivCompras, numNota, "PRA", config, dhEmiss);
				else if (tag.equals("DIVERGENCIAIMPOSTOS"))
					createDivergenciaItem(codDivCompras, numNota, "IMP", config, dhEmiss);		
				else if (tag.equals("DIVERGENCIAITENS"))
					createDivergenciaItem(codDivCompras, numNota, "ITE", config, dhEmiss);						
				else if (tag.equals("DIVERGENCIAPEDIDOS"))
					createDivergenciaItem(codDivCompras, numNota, "FIN", config, dhEmiss);
				else if (tag.equals("LIBERACAOLIMITE"))
					createDivergenciaItem(codDivCompras, numNota, "PRE", config, dhEmiss);
			}
		}

		BigDecimal nunota = buscarNunota(numNota,  parcCompra);

		selecionaRecebimento (nunota, codDivCompras);

	}

	public BigDecimal buscarNunota (BigDecimal numnota, BigDecimal parccompra) throws Exception {

		JdbcWrapper jdbc = null;
		BigDecimal numcontrato = BigDecimal.ZERO;
		BigDecimal nunota = BigDecimal.ZERO;

		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMNOTA", numnota);
			sql.setNamedParameter("PARCCOMPRA", parccompra);

			sql.appendSql(" SELECT T.NUNOTA ");
			sql.appendSql(" FROM TGFCAB T" );
			sql.appendSql(" WHERE T.NUMNOTA = :NUMNOTA" );
			sql.appendSql(" AND T.CODPARC = :PARCCOMPRA");

			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				nunota = rs.getBigDecimal("NUNOTA");
			}

		}finally {
			jdbc.closeSession();
		}
		return nunota;
	}


	public void createDivergenciaItem(BigDecimal codDivCompras, BigDecimal numNota, String tipo, String config,
			Timestamp dhEmiss) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		BigDecimal nextCodDiv = new BigDecimal(getNextCodDiv());

		DynamicVO divergenciaVO = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("AD_DIVERGENCIAS2");
		divergenciaVO.setProperty("CODDIVCOMPRAS", codDivCompras);
		divergenciaVO.setProperty("CODDIV", nextCodDiv);
		divergenciaVO.setProperty("NRNOTA", numNota);
		divergenciaVO.setProperty("TIPO", tipo);
		divergenciaVO.setProperty("DESCIRREG", buildDescIrregularidade(divergenciaVO, tipo, config).toCharArray());

		if ("PRA".equals(tipo)) {
			if (dtVencimento != null)
				divergenciaVO.setProperty("PRAZOVENCORI", new Timestamp(dtVencimento.getTime()));

			if (dhEmiss != null) {
				Date dhEmissDate = dhEmiss;
				divergenciaVO.setProperty("DTCTE", dhEmissDate);
			}
		}

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

	private String buildDescIrregularidade(DynamicVO divergenciaVO, String tipo, String config) throws Exception {
		
		BigDecimal coddivcompras = divergenciaVO.asBigDecimal("CODDIVCOMPRAS");
		
		if (tipo.equals("PRE"))			
			return buildDescDivPreco(config, coddivcompras);
		else if (tipo.equals("PRA"))
			return buildDescDivPrazo(config);
		else if (tipo.equals("IMP"))
			return buildDescDivImpostos(config);
		else if (tipo.equals("FTR"))
			return "";
		else if (tipo.equals("FIN"))
			return buildDescFaturamentoInd(config);
		else if (tipo.equals("ITE"))
			return buildDescDivItens(config);			
		else if (tipo.equals("OUT"))
			return "";

		return "";
	}

	private String buildDescDivPreco(String config, BigDecimal coddivcompras) throws Exception {
		String pedidos = "";
		String pedidosAcumula = "";
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();

		InputSource source = new InputSource(new StringReader(config));
		String pedidosLigados = "validacoes/pedidosLigados";
		NodeList nodeListPedidosLigados = (NodeList) xpath.compile(pedidosLigados).evaluate(source,	XPathConstants.NODESET);
		Node tagPedidosLigados = nodeListPedidosLigados.item(0);

		// tagPedidosLigados.getChildNodes() = tags "item"
		for (int i = 0; i < tagPedidosLigados.getChildNodes().getLength(); i++) {
			if (tagPedidosLigados.getChildNodes().item(i).getNodeName().equals("item")) {
				Node tagItem = tagPedidosLigados.getChildNodes().item(i);

				String codProduto = tagItem.getAttributes().getNamedItem("PRODUTO").getTextContent();
				String descrProd = tagItem.getAttributes().getNamedItem("DESCRPROD").getTextContent();
				BigDecimal vlrUnit = new BigDecimal(tagItem.getAttributes().getNamedItem("VLRUNIT").getTextContent());
				BigDecimal codProdutoInserir = new BigDecimal(tagItem.getAttributes().getNamedItem("PRODUTO").getTextContent());

				Node tagLigacao = tagItem.getChildNodes().item(0);
				String nuNota = tagLigacao.getAttributes().getNamedItem("NUNOTA").getTextContent();
				BigDecimal ligacaoVlrUnit = new BigDecimal(	tagLigacao.getAttributes().getNamedItem("VLRUNIT").getTextContent());

				float variacao = ligacaoVlrUnit.floatValue() - vlrUnit.floatValue();

				pedidos = "Houve divergência no valor unitário do produto: \n"
						+ "Ped. orig. " + nuNota + "\n Prod. "
						+ codProduto + " - " + descrProd + ", \n Variação de R$" + variacao + " (Nota: R$" + vlrUnit
						+ " / Pedido: R$" + ligacaoVlrUnit + ") \n";

				pedidosAcumula = pedidosAcumula + pedidos;

				//inserir também na tela de itens para possível seleção

				inserirItensPreco(coddivcompras, codProdutoInserir, descrProd, pedidos);

			}
		}

		return pedidos;
	}

	private String buildDescDivPrazo(String config) throws Exception {
		String parcelas = "";
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();

		InputSource source = new InputSource(new StringReader(config));
		String financeiro = "validacoes/financeiro/parcelasNota";
		NodeList nodeListFinanceiro = (NodeList) xpath.compile(financeiro).evaluate(source, XPathConstants.NODESET);

		if (nodeListFinanceiro.item(0) != null) {
			Node tagParcelasNota = nodeListFinanceiro.item(0);
			parcelas = "<SISTEMA> \n";
			for (int i = 0; i < tagParcelasNota.getChildNodes().getLength(); i++) {
				Node tagParcela = tagParcelasNota.getChildNodes().item(i);
				dtVencimento = formatStringDate(
						(String) tagParcela.getAttributes().getNamedItem("DTVENC").getTextContent());
				String numParcela = tagParcela.getAttributes().getNamedItem("PARCELA").getTextContent();
				String nValorSistema = tagParcela.getAttributes().getNamedItem("VALOR").getTextContent();

				parcelas = parcelas + "Parcela: " + numParcela + " - Dt. Vencimento: " + dateFormat.format(dtVencimento) + " - Valor: "+nValorSistema
						+ " \n \n";
			}
		}

		InputSource sourceXML = new InputSource(new StringReader(config));
		String financeiroXML = "validacoes/financeiro/parcelasXML";
		NodeList nodeListFinanceiroXML = (NodeList) xpath.compile(financeiroXML).evaluate(sourceXML, XPathConstants.NODESET);

		if (nodeListFinanceiroXML.item(0) != null) {
			Node tagParcelasXML = nodeListFinanceiroXML.item(0);
			parcelas = parcelas + "<ARQUIVO> \n";
			for (int i = 0; i < tagParcelasXML.getChildNodes().getLength(); i++) {
				Node tagParcela = tagParcelasXML.getChildNodes().item(i);
				dtVencimento = formatStringDate(
						(String) tagParcela.getAttributes().getNamedItem("DTVENC").getTextContent());
				String numParcela = tagParcela.getAttributes().getNamedItem("PARCELA").getTextContent();
				String nValorSistema = tagParcela.getAttributes().getNamedItem("VALOR").getTextContent();

				parcelas = parcelas + "Parcela: " + numParcela + " - Dt. Vencimento: " + dateFormat.format(dtVencimento) + " - Valor: "+nValorSistema
						+ " \n";
			}
		}

		return parcelas;
	}

	private String buildDescDivImpostos(String config) throws Exception {
		String pedidos = "";
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();

		InputSource source = new InputSource(new StringReader(config));
		String pedidosLigados = "validacoes/impostos/impostoNota";
		NodeList nodeListPedidosLigados = (NodeList) xpath.compile(pedidosLigados).evaluate(source,	XPathConstants.NODESET);
		Node tagPedidosLigados = nodeListPedidosLigados.item(0);

		InputSource sourceXML = new InputSource(new StringReader(config));
		String pedidosLigadosXML = "validacoes/impostos/impostoXML";
		NodeList nodeListPedidosLigadosXML = (NodeList) xpath.compile(pedidosLigadosXML).evaluate(sourceXML,	XPathConstants.NODESET);
		Node tagPedidosLigadosXML = nodeListPedidosLigadosXML.item(0);


		for (int i = 0; i < tagPedidosLigados.getChildNodes().getLength(); i++) {
			if (tagPedidosLigados.getChildNodes().item(i).getNodeName().equals("imposto")) {
				Node tagItem = tagPedidosLigados.getChildNodes().item(i);

				codProdutoSistema = tagItem.getAttributes().getNamedItem("CODPROD").getTextContent();

				String valorIcmsSistema = tagItem.getAttributes().getNamedItem("VLRICMS").getTextContent();
				String valorStSistema = tagItem.getAttributes().getNamedItem("VLRSUBST").getTextContent();
				String valorIpiSistema = tagItem.getAttributes().getNamedItem("VLRIPI").getTextContent();


				Node tagLigacao = tagItem.getChildNodes().item(0);

				pedidos = pedidos + "Houve divergência no valor dos impostos do Produto: "+codProdutoSistema+ " "
						+ " \n <SISTEMA>  ICMS = "+valorIcmsSistema+ " IPI: "+valorIpiSistema+ " ST: "+valorStSistema; 
			}


			for (int ii = 0; ii < tagPedidosLigadosXML.getChildNodes().getLength(); ii++) {
				if (tagPedidosLigadosXML.getChildNodes().item(ii).getNodeName().equals("imposto")) {
					Node tagItemXML = tagPedidosLigadosXML.getChildNodes().item(ii);

					codProdutoXML = tagItemXML.getAttributes().getNamedItem("CODPROD").getTextContent();

					if (codProdutoSistema.equals(codProdutoXML)) {

						String valorIcmsSistema = tagItemXML.getAttributes().getNamedItem("VLRICMS").getTextContent();
						String valorStSistema = tagItemXML.getAttributes().getNamedItem("VLRSUBST").getTextContent();
						String valorIpiSistema = tagItemXML.getAttributes().getNamedItem("VLRIPI").getTextContent();

						pedidos = pedidos + " \n <ARQUIVO> ICMS = "+valorIcmsSistema+ 
								" IPI: "+valorIpiSistema+ 
								" ST: "+valorStSistema; 
					}
				}
			}

		}

		return pedidos;
	}

	private String buildDescFaturamentoInd(String config) throws Exception {
		String produtos = "";
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();

		InputSource source = new InputSource(new StringReader(config));
		String pedidosLigados = "validacoes/pedidosLigados";
		NodeList nodeListPedidosLigados = (NodeList) xpath.compile(pedidosLigados).evaluate(source,
				XPathConstants.NODESET);
		Node tagPedidosLigados = nodeListPedidosLigados.item(0);

		// tagPedidosLigados.getChildNodes() = tags "item"
		for (int i = 0; i < tagPedidosLigados.getChildNodes().getLength(); i++) {
			if (tagPedidosLigados.getChildNodes().item(i).getNodeName().equals("item")) {
				Node tagItem = tagPedidosLigados.getChildNodes().item(i);
				BigDecimal codProduto = new BigDecimal(
						tagItem.getAttributes().getNamedItem("PRODUTO").getTextContent());
				String referencia = getReferenciaProduto(codProduto);
				String descrProd = tagItem.getAttributes().getNamedItem("DESCRPROD").getTextContent();

				produtos = produtos + "Referência: " + referencia + " | Descrição: " + descrProd + " \n";
			}
		}

		return produtos;
	}

	private String buildDescDivItens(String config) throws Exception {

		String produtos = "";
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();

		InputSource source = new InputSource(new StringReader(config));
		String pedidosLigados = "validacoes/produtosParceiro";
		NodeList nodeListPedidosLigados = (NodeList) xpath.compile(pedidosLigados).evaluate(source,
				XPathConstants.NODESET);
		Node tagPedidosLigados = nodeListPedidosLigados.item(0);

		produtos = "Produto não localizado \n";
		for (int i = 0; i < tagPedidosLigados.getChildNodes().getLength(); i++) {
			if (tagPedidosLigados.getChildNodes().item(i).getNodeName().equals("produto")) {
				Node tagItem = tagPedidosLigados.getChildNodes().item(i);

				String descrProd = tagItem.getAttributes().getNamedItem("PRODUTOXML").getTextContent();
				String unidade = tagItem.getAttributes().getNamedItem("UNIDADEXML").getTextContent();
				String qtde = tagItem.getAttributes().getNamedItem("QTDNEG").getTextContent();					

				produtos = produtos + "Produto XML: "+descrProd + " - unidade: "+unidade + " - Quant: "+qtde+" \n";
			}
		}

		return produtos;
	}

	private String getReferenciaProduto(BigDecimal codProduto) throws Exception {
		JapeWrapper produtoDAO = JapeFactory.dao("Produto");
		DynamicVO produtoVO = produtoDAO.findOne("this.CODPROD = ?", codProduto);

		if (produtoVO != null)
			return (produtoVO.asString("REFERENCIA") != null) ? produtoVO.asString("REFERENCIA") : "";

			return "";
	}

	private DynamicVO getNotaByNumNota(BigDecimal numNota) throws Exception {
		final JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");
		return cabDAO.findOne("this.NUMNOTA = ?", numNota);
	}

	private DynamicVO getImpXmlByNumNota(BigDecimal numNota) throws Exception {
		final JapeWrapper impXmlDAO = JapeFactory.dao("ImportacaoXMLNotas");
		return impXmlDAO.findOne("this.NUMNOTA = ?", numNota);
	}

	private List<String> getAttributesCabecalho(String config) throws Exception {
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();

		InputSource source = new InputSource(new StringReader(config));
		String cabecalho = "validacoes/cabecalho";
		NodeList nodeListCabecalho = (NodeList) xpath.compile(cabecalho).evaluate(source, XPathConstants.NODESET);
		NamedNodeMap attributes = nodeListCabecalho.item(0).getAttributes();

		List<String> tags = new ArrayList<String>();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node item = attributes.item(i);
			tags.add(item.getNodeName());
		}

		return tags;
	}

	private Date formatStringDate(String date) throws Exception {
		Date dateStr = new SimpleDateFormat("dd/MM/yyyy").parse(date);
		return dateStr;
	}

	protected void selecionaItens (BigDecimal nurecebimento, BigDecimal nuconferencia, BigDecimal codDivCompras) throws Exception {

		JdbcWrapper jdbc = null;

		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NURECEBIMENTO", nurecebimento);
			sql.setNamedParameter("NUCONFERENCIA", nuconferencia);

			sql.appendSql("SELECT COI.NUCONFERENCIA, CON.NURECEBIMENTO, COI.CODPROD, PRO.DESCRPROD,");
			sql.appendSql("    (MAX (COI.SEQUENCIA)) AS SEQUENCIA,");
			sql.appendSql(" COI.CONTROLE, SUB.QTDNOTA, SUM (COI.QTDECONF) AS QTDECONF, SUM (NVL(COI.QTDAVARIA, 0)) AS QTDAVARIA,");
			sql.appendSql(" CASE WHEN SUB.QTDNOTA > SUM (QTDECONF) AND SUM (QTDAVARIA) <> 0 THEN 'FALTA_AVARIA' ");
			sql.appendSql(" WHEN SUB.QTDNOTA < SUM (QTDECONF) THEN 'SOBRA' ");
			sql.appendSql(" WHEN SUB.QTDNOTA > SUM (QTDECONF) THEN 'FALTA' ");
			sql.appendSql(" WHEN SUM (QTDAVARIA) <> 0 THEN 'AVARIA' ");
			sql.appendSql(" END AS SITUACAODIV, ");
			sql.appendSql(" PRO.TIPCONTESTWMS, ");
			sql.appendSql(" MIN(COI.DTVAL) AS DTVAL, ");
			sql.appendSql(" MIN(COI.DTVALMIN) AS DTVALMIN, ");
			sql.appendSql(" COI.ACEITARDIF, ");
			sql.appendSql(" COI.DEVOLVER, ");
			sql.appendSql(" SUB.CODVOL ");
			sql.appendSql(" FROM TGWCOI COI, ");
			sql.appendSql(" TGWCON CON, ");
			sql.appendSql(" TGFPRO PRO, ");
			sql.appendSql(" (SELECT SUM (TER.QTDWMS) AS QTDNOTA, ITE.CODPROD   ,ITE.CONTROLE , ITE.CODVOL ");
			sql.appendSql(" FROM TGWITER TER ");
			sql.appendSql(" INNER JOIN TGFITE ITE ON (ITE.NUNOTA = TER.NUNOTA AND ITE.SEQUENCIA = TER.SEQNOTA) ");
			sql.appendSql(" WHERE TER.NURECEBIMENTO = :NURECEBIMENTO ");
			sql.appendSql(" GROUP BY ITE.CODPROD  ,ITE.CONTROLE , ITE.CODVOL) SUB ");
			sql.appendSql(" WHERE COI.NUCONFERENCIA = :NUCONFERENCIA ");			      
			sql.appendSql(" AND COI.NUCONFERENCIA = CON.NUCONFERENCIA ");
			sql.appendSql(" AND COI.RECEBERAVARIA = 'N' ");
			sql.appendSql(" AND COI.CODPROD = PRO.CODPROD ");
			sql.appendSql(" AND COI.CODBARRA <> 'FLOWRACK' ");
			sql.appendSql(" AND SUB.CODPROD = COI.CODPROD ");
			sql.appendSql(" AND SUB.CONTROLE = COI.CONTROLE "); 
			sql.appendSql(" AND QTDNOTA <> QTDECONF ");
			sql.appendSql(" GROUP BY COI.NUCONFERENCIA, ");
			sql.appendSql(" CON.NURECEBIMENTO, ");
			sql.appendSql(" COI.CODPROD, ");
			sql.appendSql(" PRO.TIPCONTESTWMS, ");
			sql.appendSql(" PRO.DESCRPROD, ");
			sql.appendSql(" COI.CONTROLE, ");
			sql.appendSql(" SUB.QTDNOTA, ");
			sql.appendSql(" COI.ACEITARDIF, ");
			sql.appendSql(" COI.DEVOLVER, ");
			sql.appendSql(" SUB.CODVOL ");


			ResultSet rs = sql.executeQuery();

			while (rs.next()) {

				BigDecimal sequencia = rs.getBigDecimal("SEQUENCIA");	
				BigDecimal codprod   = rs.getBigDecimal("CODPROD");
				String descrprod     = rs.getString("DESCRPROD");	
				String codvol 	     = rs.getString("CODVOL");
				BigDecimal qtdnota   = rs.getBigDecimal("QTDNOTA");
				BigDecimal qtdconf   = rs.getBigDecimal("QTDECONF");
				String situacaoDiv   = rs.getString("SITUACAODIV");	

				inserirItens(nurecebimento, sequencia,  qtdnota,  qtdconf, codDivCompras, codprod, descrprod, codvol, situacaoDiv, null);

			}

		}finally {
			jdbc.closeSession();
		}

	}

	protected void selecionaRecebimento(BigDecimal nunota,BigDecimal codDivCompras) throws Exception {

		JdbcWrapper jdbc = null;

		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUNOTA", nunota);

			sql.appendSql(" SELECT T.NURECEBIMENTO, T.NUCONFERENCIA ");
			sql.appendSql(" FROM TGWREC T" );		
			sql.appendSql(" WHERE T.NUNOTA = :NUNOTA" );

			ResultSet rs = sql.executeQuery();

			while (rs.next()) {

				BigDecimal nurecebimento = rs.getBigDecimal("NURECEBIMENTO");
				BigDecimal nuconferencia = rs.getBigDecimal("NUCONFERENCIA");


				selecionaItens(nurecebimento, nuconferencia,  codDivCompras);

			}

		}finally {
			jdbc.closeSession();
		}


	}

	private void inserirItens(BigDecimal nurecebimento, BigDecimal sequencia, BigDecimal qtdnota, BigDecimal qtdconf, BigDecimal codDivCompras,
			BigDecimal codprod, String descrprod, String codvol, String situacaodiv, String obs) throws Exception {

		//Busca a tabela a ser inserida, com base na instância
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO funcBen;

		funcBen = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AD_DIVERGENCIASITENS");
		//inclui os valores desejados nos campos

		funcBen.setProperty("CODDIVCOMPRAS", codDivCompras);		
		funcBen.setProperty("NURECEBIMENTO", nurecebimento);
		funcBen.setProperty("SEQUENCIA", sequencia);
		funcBen.setProperty("ENVIAR_EMAIL", "S");
		funcBen.setProperty("QTDNOTA", qtdnota);
		funcBen.setProperty("QTDECONF", qtdconf);
		funcBen.setProperty("CODPROD", codprod);
		funcBen.setProperty("DESCRPROD", descrprod);
		funcBen.setProperty("CODVOL", codvol);
		funcBen.setProperty("SITUACAODIV", situacaodiv);
		funcBen.setProperty("OBS", obs);

		try {
			//realiza o insert
			dwfEntityFacade.createEntity("AD_DIVERGENCIASITENS", (EntityVO) funcBen);
		} catch (Exception e) {
			Throwable cause = null; 
			Throwable result = e;

			while(null != (cause = result.getCause())  && (result != cause) ) {
				result = cause;
			}

			// TODO Auto-generated catch block
			throw new Exception(result.getMessage());
		}
	}

	private void inserirItensPreco(BigDecimal codDivCompras,
			BigDecimal codprod, String descrprod,  String obs) throws Exception {

		//Busca a tabela a ser inserida, com base na instância
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO funcBen;

		funcBen = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AD_DIVERGENCIASITENS");
		//inclui os valores desejados nos campos

		funcBen.setProperty("CODDIVCOMPRAS", codDivCompras);		
		funcBen.setProperty("ENVIAR_EMAIL", "S");
		funcBen.setProperty("CODPROD", codprod);
		funcBen.setProperty("DESCRPROD", descrprod);
		funcBen.setProperty("OBS", obs);

		try {
			//realiza o insert
			dwfEntityFacade.createEntity("AD_DIVERGENCIASITENS", (EntityVO) funcBen);
		} catch (Exception e) {
			Throwable cause = null; 
			Throwable result = e;

			while(null != (cause = result.getCause())  && (result != cause) ) {
				result = cause;
			}

			// TODO Auto-generated catch block
			throw new Exception(result.getMessage());
		}
	}

}

//boolean flagConfirma��o = JapeSession.getPropertyAsBoolean("CabecalhoNota.confirmando.nota", Boolean.valueOf(false)).booleanValue();
//boolean flagconfirmando = JapeSession.getPropertyAsBoolean("CabecalhoNota.confirmando.nota", Boolean.FALSE).booleanValue();
