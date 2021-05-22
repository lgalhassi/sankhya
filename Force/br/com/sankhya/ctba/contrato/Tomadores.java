package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
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
Data de Implementação: 12/07/2020
Objetivo: Inclui/exclui cadastro de tomadores com base nas ativações do contrato
Tabela Alvo: TCSOCC
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class Tomadores implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		BigDecimal numcontrato, codserv, codocor, codusu, codparc;

		DynamicVO servVO = (DynamicVO) event.getVo();

		numcontrato = servVO.asBigDecimal("NUMCONTRATO");
		codserv = servVO.asBigDecimal("CODPROD");
		String descricao = servVO.asString("DESCRICAO");
		Timestamp dtocor = servVO.asTimestamp("DTOCOR");
		codocor = servVO.asBigDecimal("CODOCOR");
		codusu = servVO.asBigDecimal("CODUSU");
		codparc = servVO.asBigDecimal("CODPARC");
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		//a regra original é para ocorrencias 1 e 3,
		//	porem foi incluida a situação da ocorrencia 8 E ser contrato guarda chuva devido a mudança do guarda chuva pela force
		if ( 
				( codocor.compareTo(new BigDecimal(1.00)) == 0 || codocor.compareTo(new BigDecimal(3.00)) == 0 
				
				|| codocor.compareTo(new BigDecimal(17.00))==0 //AFASTAMENTO
				
				|| codocor.compareTo(new BigDecimal(18.00))==0 ) //RETORNO DE AFASTAMENTO
				
				||	(codocor.compareTo(new BigDecimal(8.00)) == 0 && helper.conGuardaChuva(numcontrato))
			)
			
			pega_informacoes(numcontrato, codserv, descricao, dtocor, codocor, codusu, codparc);
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
		DynamicVO occVO = (DynamicVO) event.getVo();
		
		BigDecimal codfun, codemp, codocor, codparc;
		
		codocor = occVO.asBigDecimal("CODOCOR");
		codparc = occVO.asBigDecimal("CODPARC");
		
		if (codocor.compareTo(new BigDecimal(1.00)) == 0 
				|| codocor.compareTo(new BigDecimal(3.00)) == 0
				|| codocor.compareTo(new BigDecimal(17.00)) == 0
				|| codocor.compareTo(new BigDecimal(18.00)) == 0) {
		
			codfun = getCodfun(occVO.asBigDecimal("CODPROD"));
			
			//caso nao seja funcionario nao faz a acao
			if(codfun == null)
				return;
			
			codemp = getCodemp(occVO.asBigDecimal("NUMCONTRATO"));
			
			if(codocor.compareTo(new BigDecimal(1.00)) == 0 ||codocor.compareTo(new BigDecimal(18.00)) ==0  ) {
				cleanupTomador(codfun, codemp, occVO.asTimestamp("DTOCOR"));
				update_funcionario(codfun, codemp, seleciona_cidadefunc(codfun, codemp));
			}
			
			//se excluir 3(cancelamento) ou 17 (afastamento) , tirar a data final dos tomadores
			if(codocor.compareTo(new BigDecimal(3.00)) == 0 || codocor.compareTo(new BigDecimal(17.00))==0) { 
				cleanupDtFinal(codfun, codemp, codparc, occVO.asTimestamp("DTOCOR"));
				update_funcionario(codfun, codemp, seleciona_cidadeparc(codparc));
			}
		}

	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {


	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	private void pega_informacoes(BigDecimal numcontrato, BigDecimal codserv, String descricao, Timestamp dtocor, BigDecimal codocor, BigDecimal codusu, BigDecimal codparc)
			throws Exception {
		JdbcWrapper infor = null;
		BigDecimal codemp, codfunc;
		Boolean existeTomador = false;

		try {
			infor = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			NativeSql sql = new NativeSql(infor);
			sql.setNamedParameter("CODSERV", codserv);

			sql.appendSql(" SELECT FUN.CODEMP, FUN.CODFUNC " );
			sql.appendSql(" FROM TFPFUN FUN" );
			sql.appendSql(" WHERE FUN.AD_CODPRODREF = :CODSERV " );

			ResultSet rs = sql.executeQuery();
			if (rs.next()) {
				codemp = rs.getBigDecimal("CODEMP");
				codfunc = rs.getBigDecimal("CODFUNC");

				existeTomador = verificaExiteTomador(codemp, codfunc, codparc);
				
				if ((!existeTomador)) {
					inserir_tomadores(codemp, codfunc, codparc, codusu, dtocor, descricao);
					update_funcionario(codfunc, codemp, seleciona_cidadeparc(codparc));
				}
				else
				{
					if (codocor.compareTo(new BigDecimal(3.00)) == 0 || codocor.compareTo(new BigDecimal(17.00))==0) {
						validarCancelamentoTomador(codemp, codfunc, codparc, codusu, dtocor, codocor, numcontrato,
							codserv, descricao);
					}
					
				}

			}
		} finally {
			infor.closeSession();
		}
	}

	private void validarCancelamentoTomador(BigDecimal codemp, BigDecimal codfunc, BigDecimal codparc, BigDecimal codusu,
			Timestamp dtocor, BigDecimal codocor, BigDecimal numcontrato, BigDecimal codserv, String descricao)
			throws Exception {
		
		if ( (codocor.compareTo(new BigDecimal(3.00)) == 0 || codocor.compareTo(new BigDecimal(17.00))==0) ) {

			String observacao = buscarDataIncioOcorrencia(codemp, codfunc, codparc);
			
			if (codocor.compareTo(new BigDecimal(3.00)) == 0 ) { 
			
			   observacao = observacao + " | Observação Cancelamento: " + descricao;
			}
			if (codocor.compareTo(new BigDecimal(17.00)) == 0 ) { 
				
				   observacao = observacao + " | Observação Afastamento: " + descricao;
			}							

			try {

				FinderWrapper finderUpd = new FinderWrapper("Tomador",
						"CODEMP = ? AND CODFUNC = ? AND CODPARC = ? AND DTFIM IS NULL  ");
				// Insere os argumentos caso existam
				finderUpd.setFinderArguments(new Object[] { codemp, codfunc, codparc });

				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				// Realiza a busca na tabela pelos critérios
				@SuppressWarnings("unchecked")
				Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("Tomador", finderUpd);
				
				for (PersistentLocalEntity libEntity : libCollection) {

					DynamicVO newlibVO = (DynamicVO) libEntity.getValueObject();
					// Insere os valores desejados nos campos

					newlibVO.setProperty("DTFIM", dtocor);
					newlibVO.setProperty("CODUSU", codusu);
					
					if (observacao.length() > 249) {
						newlibVO.setProperty("OBSERVACAO", observacao.substring(0, 249));
					} else {
					   newlibVO.setProperty("OBSERVACAO", observacao);
					}
					// Executa o update
					libEntity.setValueObject((EntityVO) newlibVO);
				}
				update_funcionario(codfunc, codemp, seleciona_cidadefunc(codfunc, codemp));
			} catch (Exception e) {
				e.printStackTrace();
				if (true) {
					throw new Exception("Erro ao realizar o update" + e.toString());
				}
			}
		} 

	}
	
	
	private BigDecimal seleciona_cidadefunc(BigDecimal codfunc, BigDecimal codemp) throws Exception {
		//Busca o registro pela PK
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity destEntity = dwfFacade.findEntityByPrimaryKey("Funcionario", new Object[] { codemp, codfunc });
		DynamicVO funVO = (DynamicVO) destEntity.getValueObject();
		
		return funVO.asBigDecimal("CODCID");

	}

	private String buscarDataIncioOcorrencia(BigDecimal codemp, BigDecimal codfunc, BigDecimal codparc)
			throws Exception {
		String observacao = null;

		JdbcWrapper tom = null;
		try {
			tom = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			NativeSql sql = new NativeSql(tom);

			sql.setNamedParameter("CODEMP", codemp);
			sql.setNamedParameter("CODFUNC", codfunc);
			sql.setNamedParameter("CODPARC", codparc);

			sql.appendSql("select OBSERVACAO from TFPTOM ");
			sql.appendSql("where CODEMP = :CODEMP AND CODFUNC = :CODFUNC AND CODPARC = :CODPARC AND DTFIM IS NULL");

			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				observacao = rs.getString("OBSERVACAO");
			}
		} finally {
			tom.closeSession();
		}

		return observacao;

	}

	private Boolean verificaExiteTomador(BigDecimal codemp, BigDecimal codfunc, BigDecimal codparc) throws Exception {

		FinderWrapper finderUpd = new FinderWrapper("Tomador",
				"CODEMP = ? AND  CODFUNC = ? AND CODPARC = ? AND DTFIM IS NULL");

		// Insere os argumentos caso existam
		finderUpd.setFinderArguments(new Object[] { codemp, codfunc, codparc });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		// Realiza a busca na tabela pelos critérios
		@SuppressWarnings("unchecked")
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("Tomador", finderUpd);
		// Itera entre os registos encontrados
		if (libCollection.isEmpty()) {
			return false;
			
		}

		return true;
	}


	private void inserir_tomadores(BigDecimal codemp, BigDecimal codfunc, BigDecimal codparc, BigDecimal codusu,
			Timestamp dtinicio, String descricao)  {

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO servVO;

		
		
		try {
		servVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("Tomador");
		servVO.setProperty("CODEMP", codemp);
		servVO.setProperty("CODFUNC", codfunc);
		servVO.setProperty("CODPARC", codparc);
		servVO.setProperty("DTINICIO", dtinicio);
		
		if (descricao.length() > 249 ) {
	     	servVO.setProperty("OBSERVACAO", descricao.substring(0, 249));
		} else {
			servVO.setProperty("OBSERVACAO", descricao);
		}
		
		servVO.setProperty("CODUSU", codusu);

		dwfEntityFacade.createEntity("Tomador", (EntityVO) servVO);
		} catch (Exception e) {
			// comentado o stacktrace pois o sistema estava tentando inserir 2 vezes
			e.printStackTrace();
		}
	}

	private BigDecimal seleciona_cidadeparc(BigDecimal codparc) throws Exception {
		JdbcWrapper cidade = null;
		BigDecimal cid = new BigDecimal(0);
		try {
			cidade = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			NativeSql sql = new NativeSql(cidade);
			sql.setNamedParameter("codparc", codparc);

			sql.appendSql("SELECT CODCID FROM TGFPAR WHERE CODPARC = :codparc");
			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				cid = rs.getBigDecimal("CODCID");
			}
		} finally {
			cidade.closeSession();
		}

		return cid;
	}

	private void update_funcionario(BigDecimal codfunc, BigDecimal codemp, BigDecimal codcid) throws Exception {

		try {
			FinderWrapper finderupd = new FinderWrapper("Funcionario", "CODFUNC = ? AND CODEMP = ?");

			finderupd.setFinderArguments(new Object[] { codfunc, codemp });

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

			@SuppressWarnings("unchecked")
			Collection<PersistentLocalEntity> libcollection = dwfFacade.findByDynamicFinder("Funcionario", finderupd);

			for (PersistentLocalEntity libentity : libcollection) {
				DynamicVO newlibVo = (DynamicVO) libentity.getValueObject();

				newlibVo.setProperty("CODCIDTRAB", codcid);

				libentity.setValueObject((EntityVO) newlibVo);
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (true) {
				throw new Exception("Erro ao realizar o update" + e.toString());
			}
		}
	}
	
	private void cleanupTomador(BigDecimal codfunc, BigDecimal codemp, Timestamp dt) throws Exception {
		//Configura o critério de busca
		FinderWrapper finderDelete = new FinderWrapper("Tomador", 
				"this.CODFUNC = ? AND this.CODEMP = ? AND this.DTINICIO = ? ");
		finderDelete.setFinderArguments(new Object[] { codfunc, codemp, dt });
		//Exclui os registros pelo criterio
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		dwfFacade.removeByCriteria(finderDelete);

	}
	
	@SuppressWarnings("unchecked")
	private BigDecimal getCodfun(BigDecimal codserv) throws Exception {
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<DynamicVO> notasList = dwfFacade.findByDynamicFinderAsVO(new FinderWrapper(
				"Funcionario", "this.AD_CODPRODREF = ? ", new Object[] { codserv }));

		if (notasList.isEmpty())
			return null;
		else {
		
			DynamicVO funVO = notasList.iterator().next();
			return funVO.asBigDecimal("CODFUNC");
		}

	}
	
	@SuppressWarnings("unchecked")
	private BigDecimal getCodemp(BigDecimal numcontrato) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<DynamicVO> notasList = dwfFacade.findByDynamicFinderAsVO(new FinderWrapper(
				"Contrato", "this.NUMCONTRATO = ? ", new Object[] { numcontrato }));

		DynamicVO conVO = notasList.iterator().next();
		
		return conVO.asBigDecimal("CODEMP");

	}

	@SuppressWarnings("unchecked")
	private void cleanupDtFinal(BigDecimal codfun, BigDecimal codemp, BigDecimal codparc, Timestamp dtfin) throws Exception {
		//Configura o critério de busca
		FinderWrapper finderUpd = new FinderWrapper("Tomador","CODEMP = ? AND CODFUNC = ? AND CODPARC = ? AND DTFIM = ? ");
		//Insere os argumentos caso existam		
		finderUpd.setFinderArguments(new Object[] { codemp, codfun, codparc, dtfin });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		//Realiza a busca na tabela pelos critérios
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("Tomador", finderUpd);
		//Itera entre os registos encontrados
		for (PersistentLocalEntity libEntity : libCollection) {
			DynamicVO newlibVO = (DynamicVO) libEntity.getValueObject();
			//Insere os valores desejados nos campos
			newlibVO.setProperty("DTFIM", null);
			//Executa o update
			libEntity.setValueObject((EntityVO) newlibVO);
		}

	}
	
	
	
}

