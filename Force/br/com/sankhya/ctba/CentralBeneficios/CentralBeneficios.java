//TFPCMV
package br.com.sankhya.ctba.CentralBeneficios;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class CentralBeneficios implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {

		String preferencia = null;

		try {
			preferencia = (String) MGECoreParameter.getParameter("INSCENTRALBENEF");
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		if (preferencia != null && preferencia.equals("S")) {
			
			QueryExecutor query = contextoAcao.getQuery();

			Timestamp dtvenc = (Timestamp) contextoAcao.getParam("DTVENC");
			Timestamp referencia = (Timestamp) contextoAcao.getParam("REFERENCIA");
			Timestamp dtini = (Timestamp) contextoAcao.getParam("DTINI");
			Timestamp dtfim = (Timestamp) contextoAcao.getParam("DTFIM");
			BigDecimal codben = null, codemp = null;
			
			
			Registro[] registros = contextoAcao.getLinhas();
			//NotaCompra notacompra = new NotaCompra();

			for (Registro registro : registros) {
								
				// recupera os valores das linhas selecionadas
				codben = (BigDecimal)  registro.getCampo("CODBEN");
				codemp = (BigDecimal) registro.getCampo("CODEMP");

				BigDecimal novaPK_codCbe = criarCentralBeneficio(codben, dtvenc, referencia, dtini, dtfim);
				buscarBeneficiario(novaPK_codCbe, codben, codemp);

				}						
		}		
		contextoAcao.setMensagemRetorno("Central de Benefícios criada com sucesso!");
	}


	private void buscarBeneficiario(BigDecimal novaPK_codCbe, BigDecimal codben, BigDecimal codemp) throws Exception {
		BigDecimal codfunc, sequencia, codcmv;
		JdbcWrapper jdbc = null;

		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("CODBEN", codben);

			sql.appendSql(" SELECT CODEMP, CODFUNC,	SEQUENCIA  ");
			sql.appendSql("   FROM ");
			sql.appendSql("  (SELECT T.CODFUNC, T.CODEMP, T.CODBEN, 0 SEQUENCIA   ");
			sql.appendSql("     FROM TFPFBE T");
			sql.appendSql("    JOIN TFPFUN F ON (F.CODEMP = T.CODEMP AND F.CODFUNC = T.CODFUNC) ");
			sql.appendSql(" UNION ALL  ");
			sql.appendSql("   SELECT T1.CODFUNC, T1.CODEMP, T1.CODBEN, T1.SEQUENCIA ");
			sql.appendSql("     FROM TFPDFB T1 ");
			sql.appendSql("  JOIN TFPFUN F1 ON (F1.CODEMP = T1.CODEMP AND F1.CODFUNC = T1.CODFUNC)) B ");	
			sql.appendSql("  WHERE  CODBEN = :CODBEN ");

			ResultSet rs = sql.executeQuery();

			while (rs.next()) {
				
				codemp = rs.getBigDecimal("CODEMP");
				codfunc = rs.getBigDecimal("CODFUNC");
				sequencia = rs.getBigDecimal("SEQUENCIA");				
				
				codcmv =  buscarMaxTFPCMV();
				codcmv = codcmv.add(BigDecimal.ONE);
				inserirBeneficiario(novaPK_codCbe, codcmv, codemp, codfunc, sequencia);
			}

		} finally {
			jdbc.closeSession();
		}		
	}


	private void inserirBeneficiario(BigDecimal novaPK_codCbe, BigDecimal codcmv, BigDecimal codemp, BigDecimal codfunc,
			BigDecimal sequencia)  throws Exception{
		
		//tabela : TFPCMV
		//Busca a tabela a ser inserida, com base na inst�ncia
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO benVO;
			
		benVO = (DynamicVO) dwfEntityFacade
						.getDefaultValueObjectInstance("Beneficiario");
		//inclui os valores desejados nos campos
		benVO.setProperty("CODCBE", novaPK_codCbe);
		benVO.setProperty("CODCMV", codcmv);
		benVO.setProperty("CODEMP", codemp);
		benVO.setProperty("CODFUNC", codfunc);
		benVO.setProperty("SEQUENCIA", sequencia);

		//realiza o insert
		dwfEntityFacade.createEntity("Beneficiario", (EntityVO) benVO);

		//captura a chave primaria criada ap�s o insert	        		
		//BigDecimal novaPK = (BigDecimal) parcelasVO.getProperty("NUFIN");
		
	}





	private BigDecimal buscarMaxTFPCMV()  throws Exception{
		BigDecimal codcmv = BigDecimal.ZERO;
		JdbcWrapper jdbc = null;

		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);

			sql.appendSql(" SELECT MAX(CODCMV) as CODCMV FROM TFPCMV ");
			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				codcmv = rs.getBigDecimal("CODCMV");
			}

		} finally {
			jdbc.closeSession();
		}		
		
		
		return codcmv;
	}



	public BigDecimal  criarCentralBeneficio(BigDecimal codben, Timestamp dtvenc, Timestamp referencia, Timestamp dtini, Timestamp dtfim ) throws Exception {

		
		//Busca a tabela a ser inserida, com base na inst�ncia
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO centralVO;
		
		Timestamp dhatual = (Timestamp) JapeSessionContext.getProperty("dh_atual");
			
		centralVO = (DynamicVO) dwfEntityFacade
						.getDefaultValueObjectInstance("CentralBeneficio");
		//inclui os valores desejados nos campos
		centralVO.setProperty("DTVENC", dtvenc);
		centralVO.setProperty("CODBEN", codben);
		centralVO.setProperty("REFERENCIA", referencia);
		centralVO.setProperty("DTINI", dtini);
		centralVO.setProperty("DTFIM", dtfim);
		centralVO.setProperty("NUMNOTA", BigDecimal.ZERO);
		centralVO.setProperty("AD_DTINCL", dhatual);
		//realiza o insert
		dwfEntityFacade.createEntity("CentralBeneficio", (EntityVO) centralVO);

		//captura a chave primaria criada ap�s o insert	        		
		BigDecimal novaPK_codCbe = (BigDecimal) centralVO.getProperty("CODCBE");
		
		return novaPK_codCbe;
	}
}
