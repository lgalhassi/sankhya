package br.com.sankhya.ctba.virada;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class InsereFunBenef implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao acao) throws Exception {
		
		JdbcWrapper jdbc = null;
		BigDecimal numcontrato, codposto, codprod, codtbe, codben;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			
			sql.appendSql(" SELECT CODEMP,AD_CODPRODREF,NOMEFUNC,CODDEP,DESCRDEP,CODBEN,CODFUNCAO,CONTRATO," );
			sql.appendSql(" (SELECT CODPOSTO FROM AD_POSTOCONTRATO WHERE NUMCONTRATO = CONTRATO AND CODFUNCAO = A.CODFUNCAO) AS CODPOSTO," );
			sql.appendSql(" CODTBE" );
			sql.appendSql(" FROM (" );
			sql.appendSql(" SELECT FBE.CODEMP,  AD_CODPRODREF, FUN.NOMEFUNC, DEP.CODDEP, DEP.DESCRDEP, FBE.CODBEN, FUN.CODFUNCAO, BENEF.NUMCONTRATO," );
			sql.appendSql(" (SELECT OCC.NUMCONTRATO FROM TCSOCC OCC WHERE OCC.CODPROD = AD_CODPRODREF AND ROWNUM = 1" );
			sql.appendSql("     AND OCC.DTOCOR = (SELECT MAX(DTOCOR) FROM TCSOCC WHERE NUMCONTRATO = OCC.NUMCONTRATO AND CODPROD = AD_CODPRODREF))" );
			sql.appendSql(" AS CONTRATO" );
			sql.appendSql(" , BEN.CODTBE" );
			sql.appendSql(" FROM TFPFBE FBE" );
			sql.appendSql(" JOIN TFPFUN FUN ON (FBE.CODEMP = FUN.CODEMP AND FBE.CODFUNC = FUN.CODFUNC)" );
			sql.appendSql(" JOIN TFPDEP DEP ON (FUN.CODDEP = DEP.CODDEP)" );
			sql.appendSql(" JOIN TFPBEN BEN ON (FBE.CODBEN = BEN.CODBEN)" );
			sql.appendSql(" LEFT JOIN AD_FUNBENEF BENEF ON (FUN.AD_CODPRODREF = BENEF.CODPROD AND FBE.CODBEN = BENEF.CODBEN)" );
			sql.appendSql(" WHERE BEN.CODTBE IN (1,2)" );
			sql.appendSql(" AND BENEF.NUMCONTRATO IS NULL" );
			sql.appendSql(" AND FUN.DTDEM IS NULL" );
			sql.appendSql(" AND FUN.CODEMP <> 3" );
			sql.appendSql(" )A" );
			sql.appendSql(" WHERE CONTRATO IS NOT NULL" );
			sql.appendSql(" AND (SELECT CODPOSTO FROM AD_POSTOCONTRATO WHERE NUMCONTRATO = CONTRATO AND CODFUNCAO = A.CODFUNCAO) IS NOT NULL" );


			
			ResultSet rs = sql.executeQuery();
			
			while (rs.next()) {
				
				numcontrato = rs.getBigDecimal("CONTRATO");
				codposto = rs.getBigDecimal("CODPOSTO");
				codprod = rs.getBigDecimal("AD_CODPRODREF");
				codtbe = rs.getBigDecimal("CODTBE");
				codben = rs.getBigDecimal("CODBEN");
				
				//Busca a tabela a ser inserida, com base na instância
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				DynamicVO benVO;

				benVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AD_FUNBENEF");
				//inclui os valores desejados nos campos
				benVO.setProperty("NUMCONTRATO", numcontrato);
				benVO.setProperty("CODPOSTO", codposto);
				benVO.setProperty("CODPROD", codprod);
				benVO.setProperty("CODTBE", codtbe);
				benVO.setProperty("CODBEN", codben);
				
				if(codtbe.compareTo(new BigDecimal(2.00)) == 0) {
					benVO.setProperty("QTDMES", BigDecimal.ZERO);
				}
				else {
					benVO.setProperty("QTDMES", BigDecimal.ONE);
				}
				
				

				//realiza o insert
				dwfEntityFacade.createEntity("AD_FUNBENEF", (EntityVO) benVO);
				
				/*
				numcontrato = rs.getBigDecimal("NUMCONTRATO");
				codposto = rs.getBigDecimal("CODPOSTO");
				codprod = rs.getBigDecimal("CODPROD");
				
				//Configura o critério de busca
				FinderWrapper finderDelete = new FinderWrapper("AD_FUNBENEF", "this.NUMCONTRATO = ? and this.CODPOSTO = ? and this.CODPROD = ? and this.CODTBE = 1 and this.codben is null");
				finderDelete.setFinderArguments(new Object[] { numcontrato, codposto, codprod });
				//Exclui os registros pelo criterio
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				dwfFacade.removeByCriteria(finderDelete);
*/


			}
			
		}finally {
			jdbc.closeSession();
		}
		
		
		
		acao.setMensagemRetorno("terminasse!");

	}

}

