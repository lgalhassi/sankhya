//TFPBEN
package br.com.sankhya.ctba.CentralBeneficios;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class ReprocessaCentralBeneficios implements AcaoRotinaJava {

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
			BigDecimal codben = null, codemp = null, codcbe = null;

			Registro[] registros = contextoAcao.getLinhas();
			// NotaCompra notacompra = new NotaCompra();

			for (Registro registro : registros) {

				// recupera os valores das linhas selecionadas
				codben = (BigDecimal) registro.getCampo("CODBEN");
				codemp = (BigDecimal) registro.getCampo("CODEMP");

			    buscarBeneficiario(codben, codemp, referencia, dtvenc, dtini, dtfim);	
				

			}

		}
		
		contextoAcao.setMensagemRetorno("Complementar de Cálculo criado com sucesso!");
	}


	private void buscarBeneficiario( BigDecimal codben, BigDecimal codemp,
			Timestamp referencia, Timestamp dtvenc, Timestamp dtini, Timestamp dtfim) throws Exception {
		BigDecimal codfunc, sequencia, codcmv, novaPK_codCbe=null;
		JdbcWrapper jdbc = null;

		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("CODBEN", codben);

			sql.appendSql(" SELECT CODEMP, CODFUNC,	SEQUENCIA  ");
			sql.appendSql("   FROM ");
			sql.appendSql("  (SELECT T.CODFUNC, T.CODEMP, T.CODBEN, 0 SEQUENCIA   ");
			sql.appendSql("     FROM TFPFBE T");
			sql.appendSql("    JOIN TFPFUN F ON (F.CODEMP = T.CODEMP AND F.CODFUNC = T.CODFUNC AND F.DTDEM IS NULL) ");
			sql.appendSql(" UNION ALL  ");
			sql.appendSql("   SELECT T1.CODFUNC, T1.CODEMP, T1.CODBEN, T1.SEQUENCIA ");
			sql.appendSql("     FROM TFPDFB T1 ");
			sql.appendSql("  JOIN TFPFUN F1 ON (F1.CODEMP = T1.CODEMP AND F1.CODFUNC = T1.CODFUNC AND F1.DTDEM IS NULL)) B ");	
			sql.appendSql("  WHERE  CODBEN = :CODBEN ");
						
			ResultSet rs = sql.executeQuery();

			while (rs.next()) {

				codemp = rs.getBigDecimal("CODEMP");
				codfunc = rs.getBigDecimal("CODFUNC");
				sequencia = rs.getBigDecimal("SEQUENCIA");

				codcmv = buscarMaxTFPCMV();
				codcmv = codcmv.add(BigDecimal.ONE);
				if ( !verificarSeJaEstaIncluido(codemp, codfunc, codben, referencia))	
				{
					if(validarDatadeAdimissaoMenorDTdeReferencia(codemp, codfunc,referencia)) {
						if(novaPK_codCbe==null) {
							novaPK_codCbe = criarCentralBeneficio(codben, dtvenc, referencia, dtini, dtfim);	
						}
						
						inserirBeneficiario(novaPK_codCbe, codcmv, codemp, codfunc, sequencia);
						
					}
				
				
				}
			}

		} finally {
			jdbc.closeSession();
		}
	}

	private boolean validarDatadeAdimissaoMenorDTdeReferencia(BigDecimal codemp, BigDecimal codfunc, Timestamp referencia) throws Exception {

		SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMM");
		
		JdbcWrapper jdbc = null;

		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);
		
			sql.setNamedParameter("CODEMP", codemp);
			sql.setNamedParameter("CODFUNC", codfunc);
			sql.setNamedParameter("DTADM",dateFormat.format( referencia));

			sql.appendSql(" SELECT 1 as RESULTADO");
			sql.appendSql("   FROM TFPFUN ");
			sql.appendSql("  WHERE CODEMP = :CODEMP ");
			sql.appendSql("    AND CODFUNC = :CODFUNC ");
			sql.appendSql("    AND TO_CHAR(DTADM ,'YYYYMM')  <= :DTADM   ");

			ResultSet rs = sql.executeQuery();

			while (rs.next()) {
				return true;
				
			}

		} finally {
			jdbc.closeSession();
		}
		return false;
	
	}


	private Boolean verificarSeJaEstaIncluido(BigDecimal codemp, BigDecimal codfunc, BigDecimal codben, Timestamp referencia
			) throws Exception {


		JdbcWrapper jdbc = null;

		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);
		
			sql.setNamedParameter("REFERENCIA", referencia);
			sql.setNamedParameter("CODBEN", codben);
			sql.setNamedParameter("CODEMP", codemp);
			sql.setNamedParameter("CODFUNC", codfunc);

			sql.appendSql(" SELECT 1 as RESULTADO");
			sql.appendSql("   FROM TFPCBE CBE");
			sql.appendSql("   JOIN TFPCMV CMV ON CBE.CODCBE = CMV.CODCBE ");
			sql.appendSql("   JOIN TFPFUN FUN ON FUN.CODFUNC = CMV.CODFUNC AND FUN.CODEMP = CMV.CODEMP ");
			sql.appendSql("  AND TO_CHAR(CBE.REFERENCIA,'YYYYMM') >= TO_CHAR(FUN.DTADM ,'YYYYMM') "); 
			sql.appendSql("  WHERE CBE.REFERENCIA = :REFERENCIA ");
			sql.appendSql("    AND CBE.CODBEN = :CODBEN ");
			sql.appendSql("    AND CMV.CODEMP = :CODEMP ");
			sql.appendSql("    AND CMV.CODFUNC = :CODFUNC ");

			ResultSet rs = sql.executeQuery();

			while (rs.next()) {
				return true;
				
			}

		} finally {
			jdbc.closeSession();
		}
		return false;
		
	}

	private void inserirBeneficiario(BigDecimal novaPK_codCbe, BigDecimal codcmv, BigDecimal codemp, BigDecimal codfunc,
			BigDecimal sequencia) throws Exception {

		// tabela : TFPCMV
		// Busca a tabela a ser inserida, com base na inst�ncia
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO benVO;

		benVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("Beneficiario");
		// inclui os valores desejados nos campos
		benVO.setProperty("CODCBE", novaPK_codCbe);
		benVO.setProperty("CODCMV", codcmv);
		benVO.setProperty("CODEMP", codemp);
		benVO.setProperty("CODFUNC", codfunc);
		benVO.setProperty("SEQUENCIA", sequencia);

		// realiza o insert
		dwfEntityFacade.createEntity("Beneficiario", (EntityVO) benVO);

		// captura a chave primaria criada ap�s o insert
		// BigDecimal novaPK = (BigDecimal) parcelasVO.getProperty("NUFIN");

	}

	private BigDecimal buscarMaxTFPCMV() throws Exception {
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
	public BigDecimal  criarCentralBeneficio(BigDecimal codben, Timestamp dtvenc, Timestamp referencia, Timestamp dtini , Timestamp dtfim  ) throws Exception {

		
		//Busca a tabela a ser inserida, com base na inst�ncia
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO centralVO;
			
		centralVO = (DynamicVO) dwfEntityFacade
						.getDefaultValueObjectInstance("CentralBeneficio");
		//inclui os valores desejados nos campos
		centralVO.setProperty("DTVENC", dtvenc);
		centralVO.setProperty("CODBEN", codben);
		centralVO.setProperty("REFERENCIA", referencia);
		centralVO.setProperty("DTINI", dtini);
		centralVO.setProperty("DTFIM", dtfim);
		centralVO.setProperty("NUMNOTA", BigDecimal.ZERO);
		//realiza o insert
		dwfEntityFacade.createEntity("CentralBeneficio", (EntityVO) centralVO);

		//captura a chave primaria criada ap�s o insert	        		
		BigDecimal novaPK_codCbe = (BigDecimal) centralVO.getProperty("CODCBE");
		
		return novaPK_codCbe;
	}
}
