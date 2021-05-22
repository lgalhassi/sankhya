package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.dwfdata.listeners.tfp.FuncionarioListener;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class FuncionarioHelper {

	FuncionarioHelper(){
		
	}
	
	@SuppressWarnings("unchecked")
	protected Funcionario getFuncionario(BigDecimal codserv) throws Exception {
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<DynamicVO> funcList = dwfFacade.findByDynamicFinderAsVO(new FinderWrapper(
				"Funcionario", "this.AD_CODPRODREF = ?", new Object[] { codserv }));

		if (funcList.isEmpty()) {
			return null;
		}
		
		DynamicVO funVO = funcList.iterator().next();
		Funcionario func = new Funcionario();
		
		func.setCodcargahor(funVO.asBigDecimal("CODCARGAHOR"));
		func.setCodemp(funVO.asBigDecimal("CODEMP"));
		func.setCodfunc(funVO.asBigDecimal("CODFUNC"));
		func.setCodfuncao(funVO.asBigDecimal("CODFUNCAO"));
		func.setCoddep(funVO.asBigDecimal("CODDEP"));
		func.setCodserv(codserv);
		
		return func;
	}
	
	protected void verificaDiaAtivacao(BigDecimal codserv, Timestamp dtocor) throws Exception {
		JdbcWrapper jdbc = null;
		BigDecimal numcontrato = BigDecimal.ZERO;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("CODSERV", codserv);
			sql.setNamedParameter("DTOCOR", dtocor);
			
			sql.appendSql(" SELECT NUMCONTRATO FROM TCSOCC" );
			sql.appendSql(" WHERE CODPROD = :CODSERV AND TRUNC(DTOCOR) = :DTOCOR" );
			sql.appendSql(" ORDER BY DTOCOR DESC" );
			
			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				numcontrato = rs.getBigDecimal("NUMCONTRATO");
			}
			
		}finally {
			jdbc.closeSession();
		}
		
		if(numcontrato.compareTo(BigDecimal.ZERO) > 0) {
			throw new Exception("<b>Já existe uma ocorrência lançada para este dia no contrato " + numcontrato + "</b>");
		}
	}
	
	protected void verificaAtivacaoOutrosCon(BigDecimal numcontrato, BigDecimal codserv) throws Exception {
		FinderWrapper finderUpd = new FinderWrapper("ProdutoServicoContrato",
				"CODPROD = ? AND SITPROD = 'A' AND NUMCONTRATO <> ?");
		finderUpd.setFinderArguments(new Object[] { codserv, numcontrato });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		@SuppressWarnings("unchecked")
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("ProdutoServicoContrato",
				finderUpd);
		if (!libCollection.isEmpty()) {
			for (PersistentLocalEntity libEntity : libCollection) {
				DynamicVO conVO = (DynamicVO) libEntity.getValueObject();

				BigDecimal numContratoExist = conVO.asBigDecimal("NUMCONTRATO");

				throw new Exception("<b>Funcionário já cadastrado em contrato Ativo N° " + numContratoExist + "</b>");

			}
		}
	}
	
	
	protected void verificaPostoContrato(BigDecimal numcontrato, BigDecimal codfuncao) throws Exception {
		JdbcWrapper postos = null;
		try {
			postos = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			NativeSql sql = new NativeSql(postos);
			sql.setNamedParameter("CODFUNCAO", codfuncao);
			sql.setNamedParameter("NUMCONTRATO", numcontrato);

			sql.appendSql(" SELECT 1 FROM AD_POSTOCONTRATO" );
			sql.appendSql(" WHERE NUMCONTRATO = :NUMCONTRATO AND CODFUNCAO = :CODFUNCAO" );

			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				return;
			} else {
				throw new Exception("<b> O funcionário não pode ser inserido neste contrato pois ele possui uma função"
						+ " diferente das permitidas </b>");
			}
		} finally {
			postos.closeSession();
		}
	}
	
	protected void verificaEscala(BigDecimal numContrato, BigDecimal codfuncao, BigDecimal codserv) throws Exception {
	
		
		String ignorarValidacaoEscala = verificaPermissaoUsuarioLogado();
		
		if(ignorarValidacaoEscala != null && ignorarValidacaoEscala.equals("S")) {
			return;
		}
		
		if(conGuardaChuva(numContrato)) {
			return;
		}
		
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numContrato);
			sql.setNamedParameter("CODFUNCAO", codfuncao);
			sql.setNamedParameter("CODSERV", codserv);
			
			sql.appendSql(" SELECT 1 FROM AD_POSTOCONTRATO POSTO " );
			sql.appendSql(" JOIN AD_POSTOESCALA ESC ON (POSTO.CODPOSTO = ESC.CODPOSTO AND POSTO.NUMCONTRATO = ESC.NUMCONTRATO)" );
			sql.appendSql(" JOIN TFPFUN FUN ON (ESC.CODCARGAHOR = FUN.CODCARGAHOR)" );
			sql.appendSql(" WHERE POSTO.NUMCONTRATO = :NUMCONTRATO" );
			sql.appendSql(" AND POSTO.CODFUNCAO = :CODFUNCAO " );
			sql.appendSql(" AND FUN.AD_CODPRODREF = :CODSERV" );

			ResultSet rs = sql.executeQuery();
			
			if (!rs.next()) {
				throw new Exception ("<b> Carga horária do funcionário não permitida para as escalas do posto </b>");
			}
			
		}finally {
			jdbc.closeSession();
		}
	}
	
	
	private String verificaPermissaoUsuarioLogado() throws Exception {
		String ignoravalidescala = null;
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			
			sql.appendSql(" SELECT AD_IGNORAVALIDESCALA FROM TSIUSU USU " );
			sql.appendSql(" WHERE CODUSU = STP_GET_CODUSULOGADO" );

			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				ignoravalidescala = rs.getString("AD_IGNORAVALIDESCALA");
			}
			
		}finally {
			jdbc.closeSession();
		}

		return ignoravalidescala;
	}

	protected void verificaEmpresa(BigDecimal numcontrato, BigDecimal codEmpFun) throws Exception {
		JdbcWrapper jdbc = null;
		BigDecimal codEmpCon;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numcontrato);
			
			sql.appendSql(" SELECT CODEMP FROM TCSCON WHERE NUMCONTRATO = :NUMCONTRATO" );

			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				codEmpCon = rs.getBigDecimal("CODEMP");
				
				if(codEmpCon.compareTo(codEmpFun) != 0) {
					throw new Exception ("<b> A empresa do funcionário ( " + codEmpFun.toString() 
					+ " ) é diferente da empresa do contrato ( " + codEmpCon.toString() + " )</b>");
				}
			}
			
		}finally {
			jdbc.closeSession();
		}
	}
	
	protected void verificaLimitePostos(BigDecimal numcontrato, BigDecimal codfuncao, BigDecimal codprod) throws Exception {
		BigDecimal qtdPosto, qtdFunc;
		
		qtdPosto = buscaQuantidadePosto(numcontrato, codfuncao);

		qtdFunc = buscaQuantidadeFuncionarios(numcontrato, codfuncao, codprod);
		
		if (qtdPosto.compareTo(qtdFunc) <= 0) {
			throw new Exception("<b> Quantidade de profissionais ativos excedidos </b>");
		}
	}
	
	private BigDecimal buscaQuantidadePosto(BigDecimal numContrato, BigDecimal codfuncao) throws Exception {

		BigDecimal qtd = BigDecimal.ZERO;

		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numContrato);
			sql.setNamedParameter("CODFUNCAO", codfuncao);
			
			sql.appendSql(" SELECT nullValue(SUM(QUANTIDADE),0) AS QTD FROM AD_POSTOCONTRATO POSTO " );
			sql.appendSql(" WHERE POSTO.NUMCONTRATO = :NUMCONTRATO AND POSTO.CODFUNCAO = :CODFUNCAO" );

			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				qtd = rs.getBigDecimal("QTD");
			}
			
		}finally {
			jdbc.closeSession();
		}
		return qtd;
	}
	
	private BigDecimal buscaQuantidadeFuncionarios(BigDecimal numContrato, BigDecimal codfuncao, BigDecimal codprod) throws Exception {

		BigDecimal qtd = BigDecimal.ZERO;

		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numContrato);
			sql.setNamedParameter("CODFUNCAO", codfuncao);
			sql.setNamedParameter("CODPROD", codprod);
			
			sql.appendSql(" SELECT COUNT(*) AS QTD" );
			sql.appendSql(" FROM  TCSPSC PSC" );
			sql.appendSql(" WHERE PSC.NUMCONTRATO = :NUMCONTRATO" );
			sql.appendSql(" AND AD_CODFUNCAO = :CODFUNCAO" );
			sql.appendSql(" AND PSC.SITPROD = 'A' " );
			sql.appendSql(" AND PSC.CODPROD <> :CODPROD " );

			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				qtd = rs.getBigDecimal("QTD");
			}
			
		}finally {
			jdbc.closeSession();
		}
		return qtd ;
	}
	
	protected void insereBeneficio(BigDecimal numcontrato, BigDecimal codfunc, BigDecimal codben) throws Exception {
		
		BigDecimal codemp = getCodempContrato(numcontrato);
		
		if(possuiBeneficio(numcontrato, codfunc, codben)) {
			return;
		}
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO funVO;
		
		funVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("FuncionarioBeneficio");
		funVO.setProperty("CODBEN", codben);
		funVO.setProperty("CODFUNC", codfunc);
		funVO.setProperty("CODEMP", codemp);

		dwfEntityFacade.createEntity("FuncionarioBeneficio", (EntityVO) funVO);
	}
	
	@SuppressWarnings("unchecked")
	protected BigDecimal getCodempContrato(BigDecimal numcontrato) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<DynamicVO> notasList = dwfFacade.findByDynamicFinderAsVO(new FinderWrapper(
				"Contrato", "this.NUMCONTRATO = ? ", new Object[] { numcontrato }));

		DynamicVO conVO = notasList.iterator().next();
		
		return conVO.asBigDecimal("CODEMP");

	}
	
	protected void removeBeneficio(BigDecimal numcontrato, BigDecimal codfunc, BigDecimal codtbe) throws Exception {
		BigDecimal codemp = getCodempContrato(numcontrato);
		
		if(codtbe == null) {
			return;
		}
		
		//Configura o critério de busca
		FinderWrapper finderDelete = new FinderWrapper("FuncionarioBeneficio", "this.CODEMP = ? AND this.CODFUNC = ?"
				+ " AND this.CODBEN = ?");
		finderDelete.setFinderArguments(new Object[] { codemp, codfunc, codtbe });
		//Exclui os registros pelo criterio
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		dwfFacade.removeByCriteria(finderDelete);

	}

	protected void verificaQtdBen(BigDecimal numcontrato, BigDecimal codposto, BigDecimal codtbe, BigDecimal codprod) throws Exception {
		BigDecimal qtdtotal, qtdutilizada, qtddia;
		
		qtdtotal = getQtdBenTot(numcontrato, codposto, codtbe);		
		qtdutilizada = getQtdbenUt(numcontrato, codposto, codtbe);
		
		if(qtdtotal.compareTo(qtdutilizada) < 0 ) {
			throw new Exception ("<b> Quantidade de benefícios fornecedidos excedidos, no posto foi digitado: " + qtdtotal + ", a soma dos funcionários está dando: "+ qtdutilizada+" </b>");
		}
				
		qtddia = getQtdbenDia(numcontrato, codposto, codtbe, codprod);
		
		if(qtddia != null && qtddia.compareTo(new BigDecimal(2.00)) > 0) {
			throw new Exception ("<b> Quantidade de benefícios diários excedidos </b>");
		}
				
	}
	
	private BigDecimal getQtdBenTot(BigDecimal numcontrato, BigDecimal codposto, BigDecimal codtbe) throws Exception {
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numcontrato);
			sql.setNamedParameter("CODPOSTO", codposto);
			sql.setNamedParameter("CODTBE", codtbe);
			
			
			sql.appendSql(" SELECT nullValue(VLRBENEF,0) AS VLRBENEF FROM AD_BENEFFORN " );
			sql.appendSql(" WHERE NUMCONTRATO = :NUMCONTRATO" );
			sql.appendSql(" AND CODPOSTO = :CODPOSTO" );
			sql.appendSql(" AND CODTBE = (SELECT CODTBE FROM TFPTBE WHERE NVL(AD_GENERICO,'N') = 'S' AND" );
			sql.appendSql("     AD_TIPO = (SELECT AD_TIPO FROM TFPTBE WHERE CODTBE = :CODTBE) )" );
			
			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				return rs.getBigDecimal("VLRBENEF");
			}
			else
				return BigDecimal.ZERO;
			
		}finally {
			jdbc.closeSession();
		}
	}
	
	private BigDecimal getQtdbenUt(BigDecimal numcontrato, BigDecimal codposto, BigDecimal codtbe) throws Exception {
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numcontrato);
			sql.setNamedParameter("CODPOSTO", codposto);
			sql.setNamedParameter("CODTBE", codtbe);
			
			sql.appendSql(" SELECT nullValue(SUM(FUN_BEN.QTDMES),0) AS QTDMES" );
			sql.appendSql(" FROM AD_FUNBENEF FUN_BEN" );
			sql.appendSql(" JOIN TCSPSC PSC ON (FUN_BEN.NUMCONTRATO = PSC.NUMCONTRATO AND FUN_BEN.CODPROD = PSC.CODPROD)" );
			sql.appendSql(" WHERE PSC.SITPROD = 'A'" );
			sql.appendSql(" AND FUN_BEN.NUMCONTRATO = :NUMCONTRATO" );
			sql.appendSql(" AND FUN_BEN.CODPOSTO = :CODPOSTO" );
			sql.appendSql(" AND FUN_BEN.CODTBE IN (SELECT CODTBE FROM TFPTBE WHERE NVL(AD_GENERICO,'N') = 'N' AND" );
			sql.appendSql(" AD_TIPO = (SELECT AD_TIPO FROM TFPTBE WHERE CODTBE = :CODTBE ))" );


			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				return rs.getBigDecimal("QTDMES");
			}
			else
				return BigDecimal.ZERO;
			
		}finally {
			jdbc.closeSession();
		}
	}
	
	private BigDecimal getQtdbenDia(BigDecimal numcontrato, BigDecimal codposto, BigDecimal codtbe, BigDecimal codprod) throws Exception {
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numcontrato);
			sql.setNamedParameter("CODPOSTO", codposto);
			sql.setNamedParameter("CODTBE", codtbe);
			sql.setNamedParameter("CODPROD", codprod);
			
			sql.appendSql(" SELECT nullValue(SUM(FUN_BEN.QTDDIA),0) AS QTDDIA" );
			sql.appendSql(" FROM AD_FUNBENEF FUN_BEN" );
			sql.appendSql(" JOIN TCSPSC PSC ON (FUN_BEN.NUMCONTRATO = PSC.NUMCONTRATO AND FUN_BEN.CODPROD = PSC.CODPROD)" );
			sql.appendSql(" WHERE PSC.SITPROD = 'A'" );
			sql.appendSql(" AND FUN_BEN.NUMCONTRATO = :NUMCONTRATO" );
			sql.appendSql(" AND FUN_BEN.CODPOSTO = :CODPOSTO" );
			sql.appendSql(" AND FUN_BEN.CODPROD = :CODPROD" );
			sql.appendSql(" AND FUN_BEN.CODTBE IN (SELECT CODTBE FROM TFPTBE WHERE NVL(AD_GENERICO,'N') = 'N' AND" );
			sql.appendSql(" AD_TIPO = (SELECT AD_TIPO FROM TFPTBE WHERE CODTBE = :CODTBE ))" );


			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				return rs.getBigDecimal("QTDDIA");
			}
			else
				return BigDecimal.ZERO;
			
		}finally {
			jdbc.closeSession();
		}
	}
	
	private boolean possuiBeneficio(BigDecimal numcontrato, BigDecimal codfunc, BigDecimal codtbe) throws Exception {
		//Configura o critério de busca
		FinderWrapper finder = new FinderWrapper("FuncionarioBeneficio",
				"this.CODBEN = ? AND this.CODFUNC = ? AND this.CODEMP = ?");
		//Insere os argumentos caso existam	
		finder.setFinderArguments(new Object[] { codtbe, codfunc, getCodempContrato(numcontrato) });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		//Realiza a busca na tabela pelos critérios
		@SuppressWarnings("unchecked")
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("FuncionarioBeneficio", finder);
		//Itera entre os registos encontrados
		if (libCollection.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}
	
	protected String getTipben(BigDecimal codtbe) throws Exception {
		//Busca o registro pela PK
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity destEntity = dwfFacade.findEntityByPrimaryKey("TipoBeneficio", new Object[] { codtbe });
		DynamicVO destVO = (DynamicVO) destEntity.getValueObject();
		
		return destVO.asString("AD_TIPO");

	}
	
	protected BigDecimal getCodEmpben(BigDecimal codben) throws Exception {
		//Busca o registro pela PK
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity destEntity = dwfFacade.findEntityByPrimaryKey("Beneficio", new Object[] { codben });
		DynamicVO destVO = (DynamicVO) destEntity.getValueObject();
		
		return destVO.asBigDecimal("CODEMP");

	}
	
	
	protected BigDecimal getDepParc(BigDecimal numcontrato) throws Exception {
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numcontrato);
			
			sql.appendSql(" SELECT CODDEP" );
			sql.appendSql(" FROM TFPDEP DEP JOIN TCSCON CON ON (DEP.CODPARC = CON.CODPARC)" );
			sql.appendSql(" WHERE CON.NUMCONTRATO = :NUMCONTRATO" );
			
			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				return rs.getBigDecimal("CODDEP");
			}
			else {
				return BigDecimal.ZERO;
			}
			
		}finally {
			jdbc.closeSession();
		}
	}
	
	protected void insereDepFunc(BigDecimal codfunc, BigDecimal codemp, BigDecimal coddep) throws Exception {
		
		if (coddep == null)
			return;
		
		//Busca o registro pela PK
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity parcelaDestEntity = dwfFacade.findEntityByPrimaryKey("Funcionario",
				new Object[] { codemp, codfunc });
		DynamicVO funVO = (DynamicVO) parcelaDestEntity.getValueObject();
		
		//setar propriedades à serem atualizadas
		funVO.setProperty("CODDEP", coddep);

		//realiza o update
		parcelaDestEntity.setValueObject((EntityVO) funVO);

	}
		 	
	
	protected void validaEmpBen(BigDecimal codben, BigDecimal numcontrato) throws Exception {
		BigDecimal codempBen, codempCon;
		
		codempCon = getCodempContrato(numcontrato);
		codempBen = getCodEmpben(codben);
		
		if(codempBen.compareTo(codempCon) != 0 ) {
			throw new Exception ("<b> Não é possível incluir este benefício pois a empresa do contrato ( " +
					codempCon + " ) é diferente da empresa do benefício ( " + codempBen + " ) </b>" );
		}
	}
	
	
	protected boolean isDepManual(BigDecimal codocc) throws Exception {
		String check;

		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity entity = dwfFacade.findEntityByPrimaryKey("Ocorrencia", new Object[] { codocc });
		DynamicVO occVO = (DynamicVO) entity.getValueObject();

		check = occVO.asString("AD_DEPMAN");
		if (check != null && check.equals("S")) {
			return true;
		}
		return false;
		
	}
	
	protected boolean conGuardaChuva(BigDecimal numcontrato) throws Exception {
		String check;

		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity entity = dwfFacade.findEntityByPrimaryKey("Contrato", new Object[] { numcontrato });
		DynamicVO conVO = (DynamicVO) entity.getValueObject();

		check = conVO.asString("AD_GUARDA_CHUVA");
		if (check != null && check.equals("S")) {
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	protected void verificaBenDupl(BigDecimal numcontrato, BigDecimal codposto, BigDecimal codben, BigDecimal codprod) throws Exception {
		//Configura o critério de busca
		FinderWrapper finder = new FinderWrapper("AD_FUNBENEF",
				"this.NUMCONTRATO = ? AND this.CODPOSTO = ? AND this.CODBEN = ? AND CODPROD = ?");
		//Insere os argumentos caso existam	
		finder.setFinderArguments(new Object[] { numcontrato, codposto, codben, codprod });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		//Realiza a busca na tabela pelos critérios
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("AD_FUNBENEF", finder);
		//Itera entre os registos encontrados
		if (!libCollection.isEmpty()) {
			throw new Exception("<b>Benefício já cadastrado para o funcionário</b>");
		}
	}
	
	protected void geraHistBen(DynamicVO benVO) throws Exception {
		if(benVO.asBigDecimal("CODBEN") == null) {
			return;
		}
		
		//Busca a tabela a ser inserida, com base na instância
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO histVO;

		histVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AD_HISTBEN");
		//inclui os valores desejados nos campos
		histVO.setProperty("NUMCONTRATO", benVO.asBigDecimal("NUMCONTRATO"));
		histVO.setProperty("DHMOV", (Timestamp) JapeSessionContext.getProperty("dh_atual"));
		histVO.setProperty("CODFUNCAO", benVO.asBigDecimal("CODPOSTO"));
		histVO.setProperty("CODPROD", benVO.asBigDecimal("CODPROD"));
		histVO.setProperty("OPTANTE", benVO.asString("OPTANTE"));
		histVO.setProperty("QTDMES", benVO.asBigDecimal("QTDMES"));
		histVO.setProperty("QTDDIA", benVO.asBigDecimal("QTDDIA"));
		histVO.setProperty("CODTBE", benVO.asBigDecimal("CODTBE"));
		histVO.setProperty("CODFUNC", benVO.asBigDecimal("CODFUNC"));
		histVO.setProperty("CODCARGAHOR", benVO.asBigDecimal("CODCARGAHOR"));
		histVO.setProperty("CODBEN", benVO.asBigDecimal("CODBEN"));
		histVO.setProperty("CODEMP", getCodEmpben(benVO.asBigDecimal("CODBEN")));
		
		
		
		//realiza o insert
		dwfEntityFacade.createEntity("AD_HISTBEN", (EntityVO) histVO);
	}
	
	
	protected void verificaLancRetroativo(BigDecimal codprod, Timestamp dtocor) throws Exception {
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("CODPROD", codprod);
			sql.setNamedParameter("DTOCOR", dtocor);
			
			sql.appendSql(" SELECT NUMCONTRATO, DTOCOR FROM TCSOCC" );
			sql.appendSql(" WHERE trunc(DTOCOR) > :DTOCOR AND CODPROD = :CODPROD " );
			sql.appendSql(" AND AD_NUOCOR IS NULL ORDER BY DTOCOR DESC" );
			
			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				throw new Exception("<b>Não é possível incluir a ocorrência pois já existe uma ocorrência com data posterior no contrato " + rs.getBigDecimal("NUMCONTRATO") + "</b>");
			}
			
		}finally {
			jdbc.closeSession();
		}
	}
}
