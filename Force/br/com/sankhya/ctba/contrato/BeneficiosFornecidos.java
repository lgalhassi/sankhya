package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;
import java.sql.ResultSet;

import com.sun.xml.internal.ws.api.pipe.ThrowableContainerPropertySet;

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
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 10/07/2020
Objetivo: Tratamento para inclusao/remoção de beneficio
Tabela Alvo: AD_BENEFFORN
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class BeneficiosFornecidos implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		DynamicVO benVO = (DynamicVO) event.getVo();
		
		BigDecimal numcontrato, codposto, codtbe;
		
		numcontrato = benVO.asBigDecimal("NUMCONTRATO");
		codposto = benVO.asBigDecimal("CODPOSTO");
		codtbe = benVO.asBigDecimal("CODTBE");
		
		if(isGenerico(codtbe)) {
			return;
		}
		trataInsercao(numcontrato, codposto, codtbe);
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeDelete(PersistenceEvent event) throws Exception {
		DynamicVO benVO = (DynamicVO) event.getVo();
		
		BigDecimal numcontrato, codtbe, codposto;
		
		numcontrato = benVO.asBigDecimal("NUMCONTRATO");
		codposto = benVO.asBigDecimal("CODPOSTO");
		codtbe = benVO.asBigDecimal("CODTBE");
	
		validaRemocao(numcontrato, codposto, codtbe);
		removeBeneficio(numcontrato, codposto, codtbe);
				
	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		/*	Removido por Renan em 18/09/20 pois após a alteração do modelo de cadastro de tipo de benefício
		 * esta validação deixou de fazer sentido
		
		DynamicVO benVO = (DynamicVO) event.getVo();
		
		BigDecimal codtbe, numcontrato;
		
		codtbe = benVO.asBigDecimal("CODTBE");
		numcontrato = benVO.asBigDecimal("NUMCONTRATO");
		
		if(isGenerico(codtbe)) {
			return;
		}
		else {
			FuncionarioHelper helper = new FuncionarioHelper();
			helper.validaEmpBen(codtbe, numcontrato);
		}
		*/
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		ModifingFields camposModificados = event.getModifingFields();
		
		if (camposModificados.isModifingAny("CODTBE")) {
			throw new Exception("<b> Não é possível alterar o código do benefício </b>");
		}
	}
	
	private void trataInsercao(BigDecimal numcontrato, BigDecimal codposto, BigDecimal codtbe) throws Exception {
		JdbcWrapper jdbc = null;
		BigDecimal codserv, codfunc;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numcontrato);
			sql.setNamedParameter("CODPOSTO", codposto);
			
			
			sql.appendSql(" SELECT PSC.CODPROD AS CODSERV,  FUN.CODFUNC, FUN.CODFUNCAO" );
			sql.appendSql(" FROM TCSPSC PSC JOIN AD_POSTOCONTRATO POSTO ON (POSTO.NUMCONTRATO = PSC.NUMCONTRATO)" );
			sql.appendSql(" JOIN TFPFUN FUN ON (PSC.CODPROD = FUN.AD_CODPRODREF AND FUN.CODFUNCAO = POSTO.CODFUNCAO)" );
			sql.appendSql(" WHERE PSC.NUMCONTRATO = :NUMCONTRATO" );
			sql.appendSql(" AND PSC.SITPROD <> 'C'" );
			sql.appendSql(" AND POSTO.CODPOSTO = :CODPOSTO" );
			
			ResultSet rs = sql.executeQuery();
			
			while (rs.next()) {
				codserv = rs.getBigDecimal("CODSERV");
				codfunc = rs.getBigDecimal("CODFUNC");
				insereFuncBenef(numcontrato, codposto, codtbe, codserv, codfunc);
			}
			
		}finally {
			jdbc.closeSession();
		}
		
	}
	
	private void insereFuncBenef(BigDecimal numcontrato, BigDecimal codposto, BigDecimal codtbe, BigDecimal codserv, BigDecimal codfunc) {
		//Busca a tabela a ser inserida, com base na instância
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO funcBen;

		try {
			funcBen = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AD_FUNBENEF");
			//inclui os valores desejados nos campos
			funcBen.setProperty("CODPOSTO", codposto);
			funcBen.setProperty("NUMCONTRATO", numcontrato);
			funcBen.setProperty("CODPROD", codserv);
			funcBen.setProperty("OPTANTE", "S");
			funcBen.setProperty("CODTBE", codtbe);
			funcBen.setProperty("CODFUNC", codfunc);
			
			//realiza o insert
		
			dwfEntityFacade.createEntity("AD_FUNBENEF", (EntityVO) funcBen);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void removeBeneficio(BigDecimal numcontrato, BigDecimal codposto, BigDecimal codtbe) throws Exception {
		//Configura o critério de busca
		FinderWrapper finderDelete = new FinderWrapper("AD_FUNBENEF", "this.NUMCONTRATO = ? AND this.CODPOSTO = ? AND this.CODTBE = ?");
		finderDelete.setFinderArguments(new Object[] { numcontrato, codposto, codtbe });
		//Exclui os registros pelo criterio
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		dwfFacade.removeByCriteria(finderDelete);

	}
	
	private void validaRemocao(BigDecimal numcontrato, BigDecimal codposto, BigDecimal codtbe) throws Exception {
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numcontrato);
			sql.setNamedParameter("CODPOSTO", codposto);
			sql.setNamedParameter("CODTBE", codtbe);
			
			sql.appendSql(" SELECT 1 FROM" );
			sql.appendSql(" AD_FUNBENEF BEN JOIN TFPTBE TBE ON (BEN.CODTBE = TBE.CODTBE)" );
			sql.appendSql(" WHERE BEN.NUMCONTRATO = :NUMCONTRATO " );
			sql.appendSql(" AND BEN.CODPOSTO = :CODPOSTO " );
			sql.appendSql(" AND BEN.CODTBE = :CODTBE" );
			sql.appendSql(" AND nullValue(TBE.AD_GENERICO,'N') = 'N'" );

			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				throw new Exception ("<b> Não é possível excluir o benefício pois existem funcionários vinculados a ele </b>");
			}
			
		}finally {
			jdbc.closeSession();
		}
	}
	
	
	//não está mais chamando esse método porque a Selma falou em 22/03/2021 que não precisa excluir da Central
	public void removeCentralBeneficio(BigDecimal codfunc, BigDecimal codben) throws Exception {
		
		JdbcWrapper jdbc = null;
		BigDecimal codcbe;
				
		try {
			
			//buscar todos os funcionários que estão no contrato e no posto de trabalho para excluir da central
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("CODFUNC", codfunc);
			sql.setNamedParameter("CODBEN", codben);
			sql.setNamedParameter("STATUS", "PE");
			
			sql.appendSql(" SELECT CMV.CODCBE FROM TFPCMV CMV " );
			sql.appendSql(" JOIN TFPCBE CBE ON (CMV.CODCBE = CBE.CODCBE)" );			
			sql.appendSql(" WHERE CMV.CODFUNC = :CODFUNC " );
			sql.appendSql(" AND CBE.CODBEN = :CODBEN ");
			sql.appendSql(" AND CBE.STATUS = :STATUS");
			
			ResultSet rs = sql.executeQuery();
						
			while (rs.next()) {
				
				codcbe = rs.getBigDecimal("CODCBE");
				
				removeFuncionarioCentral(codfunc, codcbe);				
			}			
		
		} catch (Exception e) {
		}
		finally {
			jdbc.closeSession();
		}
	}
	
	private void removeFuncionarioCentral (BigDecimal codfunc, BigDecimal codcbe) throws Exception {
		
			 //Configura o critério de busca
			FinderWrapper finderDelete = new FinderWrapper("Beneficiario", "this.CODFUNC = ? AND this.CODCBE = ?");
			finderDelete.setFinderArguments(new Object[] {  codfunc, codcbe });
			//Exclui os registros pelo criterio
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(finderDelete);		
	}
	
	
	
	private boolean isGenerico(BigDecimal codtbe) throws Exception {
		String check;

		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity entity = dwfFacade.findEntityByPrimaryKey("TipoBeneficio", new Object[] { codtbe });
		DynamicVO parcVO = (DynamicVO) entity.getValueObject();

		check = parcVO.asString("AD_GENERICO");
		if (check != null && check.equals("S")) {
			return true;
		}
		return false;
	
	}

}


