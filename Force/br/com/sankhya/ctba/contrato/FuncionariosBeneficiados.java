package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 12/07/2020
Objetivo: Incluir/excluir os beneficios
Tabela Alvo: AD_FUNBENEF
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class FuncionariosBeneficiados implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		DynamicVO benVO = (DynamicVO) event.getVo();
		BigDecimal numcontrato, codtbe, qtdmes, codposto, codprod;
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		numcontrato = benVO.asBigDecimal("NUMCONTRATO");
		codtbe =  benVO.asBigDecimal("CODTBE");
		qtdmes = benVO.asBigDecimal("QTDMES");
		codposto = benVO.asBigDecimal("CODPOSTO");
		codprod = benVO.asBigDecimal("CODPROD");
		
		if (qtdmes != null) {
			//incluir funcao
		   helper.verificaQtdBen(numcontrato, codposto, codtbe, codprod);
		}
			
	}

	@Override
	public void afterUpdate(PersistenceEvent event) throws Exception {
		DynamicVO benVO = (DynamicVO) event.getVo();
		BigDecimal numcontrato, codtbe, qtdmes, codposto, codprod, codfunc, codben;
		String optante;
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		numcontrato = benVO.asBigDecimal("NUMCONTRATO");
		codtbe =  benVO.asBigDecimal("CODTBE");
		qtdmes = benVO.asBigDecimal("QTDMES");
		codposto = benVO.asBigDecimal("CODPOSTO");
		codprod = benVO.asBigDecimal("CODPROD");
		optante = benVO.asString("OPTANTE");
		codfunc = benVO.asBigDecimal("CODFUNC");
		codben =  benVO.asBigDecimal("CODBEN");
		

		//se colocar optante = N, retirar da central de benef�cios os pendentes
		if (optante.equals("N")) {
			buscaBeneficiarioCentral(codfunc, codben);
		}
					
		//Falando com a Selma 24/02/2021 ela pediu para tirar , pois teria que entrar contrato por contrato e acertar a qtde de benefício fornecida,
		//vamos tentar travar de uma outra forma
		//validar quantidade de benef�cios fornecidos
		//Selma disse em 25/03/2021 que corrigiu os contratos e podemos voltar a travar
	    if (qtdmes != null) {
	  	  helper.verificaQtdBen(numcontrato, codposto, codtbe, codprod);
	  	}
			
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeDelete(PersistenceEvent event) throws Exception {
		DynamicVO benVO = (DynamicVO) event.getVo();
		BigDecimal numcontrato, codfunc, codben, codemp;
		
		numcontrato = benVO.asBigDecimal("NUMCONTRATO");
		codfunc = benVO.asBigDecimal("CODFUNC");
		codben =  benVO.asBigDecimal("CODBEN");
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		if(codben != null) {
			helper.removeBeneficio(numcontrato, codfunc, codben);
			helper.geraHistBen(benVO);
		}
		
		BeneficiosFornecidos bene = new BeneficiosFornecidos();
		
		/* Selma falou em 22/03/2021 que não é para excluir da CENTRAL
		bene.removeCentralBeneficio(codfunc, codben);
		*/

	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		
		DynamicVO benVO = (DynamicVO) event.getVo();
		String optante, tipBen, adTipoBeneficio;
		BigDecimal qtddia, codserv, qtdmes, codben, numcontrato, codfunc, codposto, codtbe, codprod;
		
		optante = benVO.asString("OPTANTE");
		qtddia = benVO.asBigDecimal("QTDDIA");
		qtdmes = benVO.asBigDecimal("QTDMES");
		codserv = benVO.asBigDecimal("CODPROD");
		codben = benVO.asBigDecimal("CODBEN");
		numcontrato = benVO.asBigDecimal("NUMCONTRATO");
		
		codtbe =  benVO.asBigDecimal("CODTBE");
		codprod = benVO.asBigDecimal("CODPROD");
		codposto = benVO.asBigDecimal("CODPOSTO");
		
		FuncionarioHelper helper = new FuncionarioHelper();
		Funcionario func = helper.getFuncionario(codserv);
		
		codfunc = func.getCodfunc();
		
		if(func == null)
			return;
		
		tipBen = helper.getTipben(benVO.asBigDecimal("CODTBE"));
		
		
		if(optante == null) {
			benVO.setProperty("OPTANTE", "S");
		}
		
		if(tipBen.equals("T")) {
			if(qtddia == null) {
				benVO.setProperty("QTDDIA", new BigDecimal(2.00));
			}
		}

		if(tipBen.equals("A")) {
			if(qtddia == null) {
				benVO.setProperty("QTDDIA", new BigDecimal(1.00));
			}
			if(qtdmes == null) {
				benVO.setProperty("QTDMES", new BigDecimal(1.00));
			}
		}

		benVO.setProperty("CODFUNC", func.getCodfunc());
		benVO.setProperty("CODCARGAHOR", func.getCodcargahor());
		
		if(codben != null) {
			if(qtdmes == null) {
				throw new Exception("<b>Necessário preencher a quantidade mensal do benefício</b>");
			}
			
			helper.verificaBenDupl(numcontrato, codposto, codben, codprod);
			
			helper.insereBeneficio(numcontrato, codfunc, codben);
		}
		BigDecimal contrato = verificarBeneficioOutroContrato(numcontrato, codprod);	
		if (contrato.compareTo(BigDecimal.ZERO) > 0) {
			throw new Exception ("Já existe benefício cadastrado no contrato: "+contrato);
			}				
	   }		
	
	//validar para não deixar o funcionário ter o mesmo benefício em contratos diferentes
	public BigDecimal verificarBeneficioOutroContrato(BigDecimal numcontrato, BigDecimal codprod) throws Exception {
		
		BigDecimal contrato = new BigDecimal(0);
		
		//Configura o critério de busca
		FinderWrapper finder = new FinderWrapper("AD_FUNBENEF","NUMCONTRATO <> ? AND CODPROD = ?");
		
		//Insere os argumentos caso existam	
		finder.setFinderArguments(new Object[] {numcontrato, codprod});
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		
		//Realiza a busca na tabela pelos critérios
		@SuppressWarnings("unchecked")
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("AD_FUNBENEF", finder);
				
		//Itera entre os registos encontrados
		for (PersistentLocalEntity libEntity : libCollection) {
			DynamicVO conVO = ( DynamicVO ) libEntity.getValueObject();
			contrato = conVO.asBigDecimal("NUMCONTRATO");
		}
		return contrato;
	}
		

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		DynamicVO benVO = (DynamicVO) event.getVo();
		DynamicVO oldVO = (DynamicVO) event.getOldVO();
		
		BigDecimal codben, numcontrato,qtdmes, codfunc, codposto, codbenOld, codtbe, codprod;
		String optante;
		
		codben = benVO.asBigDecimal("CODBEN");
		numcontrato = benVO.asBigDecimal("NUMCONTRATO");
		
		ModifingFields camposModificados = event.getModifingFields();
		FuncionarioHelper helper = new FuncionarioHelper();
		
		numcontrato = benVO.asBigDecimal("NUMCONTRATO");
		codben =  benVO.asBigDecimal("CODBEN");
		qtdmes = benVO.asBigDecimal("QTDMES");
		codfunc = benVO.asBigDecimal("CODFUNC");
		codposto = benVO.asBigDecimal("CODPOSTO");
		codbenOld = oldVO.asBigDecimal("CODBEN");
		codtbe =  benVO.asBigDecimal("CODTBE");
		codprod = benVO.asBigDecimal("CODPROD");
		
		if (camposModificados.isModifingAny("CODPROD") ) {
			throw new Exception("<b>Não é possível alterar o código do funcionário</b>");
		}
		
		if (camposModificados.isModifingAny("CODBEN")) {
			helper.removeBeneficio(numcontrato, codfunc, codbenOld);
		}
		
		
		if (camposModificados.isModifingAny("CODBEN") && codben != null) {
			helper.validaEmpBen(codben, numcontrato);
		}

		optante = benVO.asString("OPTANTE");
		
		if(optante == null || optante.equals("N") || codben == null) {
			helper.removeBeneficio(numcontrato, codfunc, codbenOld);
			return;
		}
		
		if(qtdmes == null) {
			throw new Exception("<b>Necessário preencher a quantidade mensal do benefício</b>");
		}
		
		if (qtdmes.compareTo(BigDecimal.ZERO) > 0) {
			if(codben != null) {
				//helper.verificaBenDupl(numcontrato, codposto, codben, codprod);
				helper.insereBeneficio(numcontrato, codfunc, codben);
			}
		}
		else {
			//incluir funcao
			//helper.removeBeneficio(numcontrato, codfunc, codbenOld);
		}	
		
		BigDecimal contrato = verificarBeneficioOutroContrato(numcontrato, codprod);	
		if (contrato.compareTo(new BigDecimal(0)) > 0) {
			throw new Exception ("Já existe benefício cadastrado no contrato: "+contrato);
			}				
	   }	
	
	   public BigDecimal quantLiberadaPosto(BigDecimal numcontrato, BigDecimal codposto) throws Exception {
		   
		   JdbcWrapper jdbc = null;
		   BigDecimal qtdePosto = null;
			
			try {
				jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
				
				NativeSql sql = new NativeSql(jdbc);
				sql.setNamedParameter("NUMCONTRATO", numcontrato);
				sql.setNamedParameter("CODPOSTO", codposto);
				
				sql.appendSql(" SELECT SUM(QUANTIDADE) AS QUANTIDADE FROM AD_POSTOCONTRATO" );
				sql.appendSql(" WHERE NUMCONTRATO = :NUMCONTRATO " );
				sql.appendSql(" AND CODPOSTO = :CODPOSTO" );
				
				ResultSet rs = sql.executeQuery();
				
				if (rs.next()) {
					qtdePosto = rs.getBigDecimal("QUANTIDADE");
				}
				
				return qtdePosto;					
				
			}finally {
				jdbc.closeSession();
			}
			
	   }
	   
	   public BigDecimal quantFuncionariosCadastrados(BigDecimal numcontrato, BigDecimal codposto) throws Exception {
		   
		   JdbcWrapper jdbc = null;
		   BigDecimal qtdeFunc = null;
			
			try {
				jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
				
				NativeSql sql = new NativeSql(jdbc);
				sql.setNamedParameter("NUMCONTRATO", numcontrato);
				sql.setNamedParameter("CODPOSTO", codposto);
				sql.setNamedParameter("OPTANTE", "S");
				sql.setNamedParameter("SITPROD", "A");
				
				
				sql.appendSql(" SELECT COUNT(DISTINCT A.CODFUNC) AS QUANTIDADE FROM AD_FUNBENEF A" );
				sql.appendSql(" JOIN TFPFUN F ON (F.AD_CODPRODREF = A.CODPROD)");
				sql.appendSql(" JOIN TCSPSC T ON (A.NUMCONTRATO = T.NUMCONTRATO AND A.CODPROD = T.CODPROD)");				
				sql.appendSql(" WHERE A.NUMCONTRATO = :NUMCONTRATO " );
				sql.appendSql(" AND A.CODPOSTO = :CODPOSTO" );
				sql.appendSql(" AND A.OPTANTE = :OPTANTE" );
				sql.appendSql(" AND T.SITPROD = :SITPROD");
				
				ResultSet rs = sql.executeQuery();
				
				if (rs.next()) {
					qtdeFunc = rs.getBigDecimal("QUANTIDADE");
				}
				
				return qtdeFunc;					
				
			}finally {
				jdbc.closeSession();
			}
			
	   }
	   
	   //QUANDO DESMARCAR A OP��O (OPTANTE) NO CONTRATO, TIRAR TAMB�M DA CENTRAL SE ESTIVER PENDENTE
	   public void buscaBeneficiarioCentral(BigDecimal codfunc, BigDecimal codben) throws Exception {
		   
		   JdbcWrapper jdbc = null;
		   BigDecimal qtdeFunc = null;
			
			try {
				jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
				
				NativeSql sql = new NativeSql(jdbc);
				sql.setNamedParameter("CODFUNC", codfunc);
				sql.setNamedParameter("CODBEN", codben);
				sql.setNamedParameter("STATUS", "PE");
				
				sql.appendSql(" SELECT T.CODCBE AS CODCBE FROM TFPCMV T " );
				sql.appendSql(" JOIN TFPCBE BE ON (BE.CODCBE = T.CODCBE) ");
				sql.appendSql(" WHERE T.CODFUNC = :CODFUNC " );
				sql.appendSql(" AND BE.CODBEN = :CODBEN" );
				sql.appendSql(" AND BE.STATUS = :STATUS");
				
				ResultSet rs = sql.executeQuery();
				
				while (rs.next()) {
					removeBeneficioCentral(rs.getBigDecimal("CODCBE"), codfunc);
				}						
				
			}finally {
				jdbc.closeSession();
			}
		   
	   }
	   
	   protected void removeBeneficioCentral(BigDecimal codcbe, BigDecimal codfunc) throws Exception {		   
		 		
			//Configura o critério de busca
			FinderWrapper finderDelete = new FinderWrapper("Beneficiario", "this.CODCBE = ? AND this.CODFUNC = ?");
			finderDelete.setFinderArguments(new Object[] { codcbe, codfunc });
			//Exclui os registros pelo criterio
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(finderDelete);
				
		}
	   
}

