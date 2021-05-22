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
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 12/07/2020
Objetivo: Validações de inclusão do serviço
Tabela Alvo: TCSPSC
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class ValidacoesServico implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
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
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		BigDecimal numcontrato, codserv;

		Funcionario fun;
		FuncionarioHelper helper = new FuncionarioHelper();
		
		DynamicVO servVO = (DynamicVO) event.getVo();

		numcontrato = servVO.asBigDecimal("NUMCONTRATO");
		codserv = servVO.asBigDecimalOrZero("CODPROD");

		BigDecimal codGrupo = buscaGrupoUsuarioLogado();
		
		if (codGrupo == null) {
			throw new Exception(
					"<b>Usuário não está vinculado a nenhum grupo, Por Favor, se cadastre a um grupo de Usuários.</b>");
		}
		
		
		fun = helper.getFuncionario(codserv);
		
		if (fun == null) {
			consultaPermissaoUsuarioServ(codGrupo);
		}
		else {
			consultaPermissaoUsuarioFunc(codGrupo);
			
			servVO.setProperty("AD_CODFUNCAO", fun.getCodfuncao());
			
			helper.verificaAtivacaoOutrosCon(numcontrato, codserv);
			helper.verificaPostoContrato(numcontrato, fun.getCodfuncao());
			helper.verificaEscala(numcontrato, fun.getCodfuncao(), fun.getCodserv());
			helper.verificaEmpresa(numcontrato, fun.getCodemp());
			helper.verificaLimitePostos(numcontrato, fun.getCodfuncao(), codserv);
		}
		
		//SE TIVER ALGUM SERVI�O CANCELADO N�O DEIXAR INCLUIR MAIS FUNCION�RIOS
	    validaServicosCancelados(numcontrato);
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		DynamicVO pscVO = (DynamicVO) event.getVo();
		
		BigDecimal numcontrato, codserv;
		
		ModifingFields camposModificados = event.getModifingFields();

		if (camposModificados.isModifingAny("SITPROD") && pscVO.asString("SITPROD").equals("A")) {
			FuncionarioHelper helper = new FuncionarioHelper();
			
			numcontrato = pscVO.asBigDecimal("NUMCONTRATO");
			codserv = pscVO.asBigDecimalOrZero("CODPROD");
			
			Funcionario fun = helper.getFuncionario(codserv);
			if (fun == null) {
				return;
			}
			
			helper.verificaAtivacaoOutrosCon(numcontrato, codserv);
			helper.verificaPostoContrato(numcontrato, fun.getCodfuncao());
			helper.verificaEscala(numcontrato, fun.getCodfuncao(), fun.getCodserv());
			helper.verificaEmpresa(numcontrato, fun.getCodemp());
			helper.verificaLimitePostos(numcontrato, fun.getCodfuncao(), codserv);
		}

	}
	
	@SuppressWarnings("unchecked")
	private BigDecimal buscaGrupoUsuarioLogado() throws Exception {

		BigDecimal codGrupo = null;

		BigDecimal codusu = (BigDecimal) JapeSessionContext.getProperty("usuario_logado");

		FinderWrapper finderUpd = new FinderWrapper("Usuario", "CODUSU = ?");
		finderUpd.setFinderArguments(new Object[] { codusu });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("Usuario", finderUpd);
		for (PersistentLocalEntity libEntity : libCollection) {
			DynamicVO conVO = (DynamicVO) libEntity.getValueObject();

			codGrupo = conVO.asBigDecimal("CODGRUPO");

		}

		return codGrupo;

	}
	
	@SuppressWarnings("unchecked")
	private void consultaPermissaoUsuarioServ(BigDecimal codGrupo) throws Exception {

		FinderWrapper finderUpd = new FinderWrapper("GrupoUsuario", "CODGRUPO = ?");
		finderUpd.setFinderArguments(new Object[] { codGrupo });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("GrupoUsuario", finderUpd);
		for (PersistentLocalEntity libEntity : libCollection) {
			DynamicVO gruVO = (DynamicVO) libEntity.getValueObject();

			String permiteInserirServ = gruVO.asString("AD_PERMINSSERV");

			if (permiteInserirServ == null || permiteInserirServ.equals("N")) {

				BigDecimal codusu = (BigDecimal) JapeSessionContext.getProperty("usuario_logado");

				// SE O usuário NÃO tiver Permissão
				if (!buscaPermissaoNoUsuarioServ(codusu)) {
					throw new Exception("<b>Usuário não tem permissão para inserir Serviços nos Contratos</b>");
				}

			}
		}	
	}
		
	@SuppressWarnings("unchecked")
	private boolean buscaPermissaoNoUsuarioServ(BigDecimal codusu) throws Exception {

		boolean retorno = false;

		FinderWrapper finderUpd = new FinderWrapper("Usuario", "CODUSU = ?");
		finderUpd.setFinderArguments(new Object[] { codusu });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("Usuario", finderUpd);
		for (PersistentLocalEntity libEntity : libCollection) {
			DynamicVO usuVO = (DynamicVO) libEntity.getValueObject();

			String permiteInserirServ = usuVO.asString("AD_PERMINSSERV");

			if (permiteInserirServ != null && permiteInserirServ.equals("S")) {

				retorno = true;

			}

		}

		return retorno;

	}
	
	
	@SuppressWarnings("unchecked")
	private void consultaPermissaoUsuarioFunc(BigDecimal codGrupo) throws Exception {

		FinderWrapper finderUpd = new FinderWrapper("GrupoUsuario", "CODGRUPO = ?");
		finderUpd.setFinderArguments(new Object[] { codGrupo });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("GrupoUsuario", finderUpd);
		for (PersistentLocalEntity libEntity : libCollection) {
			DynamicVO gruVO = (DynamicVO) libEntity.getValueObject();

			String permiteInserirFunc = gruVO.asString("AD_PERMINSFUNC");

			if (permiteInserirFunc == null || permiteInserirFunc.equals("N")) {

				BigDecimal codusu = (BigDecimal) JapeSessionContext.getProperty("usuario_logado");

				// SE O usuário NÃO tiver Permissão
				if (!buscaPermissaoNoUsuarioFunc(codusu)) {

					throw new Exception("<b>Usuário não tem permissão para inserir Funcionários nos Contratos</b>");

				}

			}

		}

	}
	
	@SuppressWarnings("unchecked")
	private boolean buscaPermissaoNoUsuarioFunc(BigDecimal codusu) throws Exception {

		boolean retorno = false;

		FinderWrapper finderUpd = new FinderWrapper("Usuario", "CODUSU = ?");
		finderUpd.setFinderArguments(new Object[] { codusu });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("Usuario", finderUpd);
		for (PersistentLocalEntity libEntity : libCollection) {
			DynamicVO usuVO = (DynamicVO) libEntity.getValueObject();

			String permiteInserirFunc = usuVO.asString("AD_PERMINSFUNC");

			if (permiteInserirFunc != null && permiteInserirFunc.equals("S")) {

				retorno = true;

			}

		}

		return retorno;

	}
	public void validaServicosCancelados(BigDecimal numcontrato) throws Exception {
		JdbcWrapper jdbc = null;
		   BigDecimal qtdeFunc = null;
			
			try {
				jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
				
				NativeSql sql = new NativeSql(jdbc);
				sql.setNamedParameter("NUMCONTRATO", numcontrato);
				sql.setNamedParameter("SITPROD", "C");
								
				sql.appendSql(" SELECT * FROM TCSPSC T " );
				sql.appendSql(" INNER JOIN TGFPRO PROD ON (PROD.CODPROD = T.CODPROD)");
				sql.appendSql(" WHERE NUMCONTRATO = :NUMCONTRATO " );
				sql.appendSql(" AND SITPROD = :SITPROD ");
				sql.appendSql(" AND T.CODPROD NOT IN (SELECT F.AD_CODPRODREF" );
				sql.appendSql("                         FROM TFPFUN F");
				sql.appendSql("                        WHERE F.AD_CODPRODREF = T.CODPROD)");				
				
				ResultSet rs = sql.executeQuery();
				
				if (rs.next()) {
					throw new Exception ("Não é possível inserir, pois o serviço já foi cancelado");
				}						
				
			}finally {
				jdbc.closeSession();
			}
		   
	}

}
