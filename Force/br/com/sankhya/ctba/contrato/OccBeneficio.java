package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;

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
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 09/07/2020
Objetivo: Incluir o funcionario na tabela de beneficios e na tabela de funcionarios beneficiados do contrato
Tabela Alvo: TCSOCC
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class OccBeneficio implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent event) throws Exception {
		// TODO Auto-generated method stub
		
		DynamicVO occVO = (DynamicVO) event.getVo();
		
		BigDecimal numcontrato, codocor, codserv, codfunc = null;
        Timestamp dtocor;
        
		numcontrato = occVO.asBigDecimal("NUMCONTRATO");
		codserv = occVO.asBigDecimal("CODPROD");
		codocor = occVO.asBigDecimal("CODOCOR");
		dtocor = occVO.asTimestamp("DTOCOR");
		
		Timestamp timestamp = dtocor;
		Calendar calendar = Calendar.getInstance();		 
		
		if (codocor.compareTo(new BigDecimal(3.00)) == 0) {
			codfunc = getCodfun(codserv);
			
	     if (codfunc == null)
	    	 
			 calendar.setTime(timestamp);
			 calendar.set(Calendar.HOUR_OF_DAY, 0);
			 calendar.set(Calendar.MINUTE, 0);
			 calendar.set(Calendar.SECOND, 0);
			 calendar.set(Calendar.MILLISECOND, 0);
			 calendar.add(Calendar.DAY_OF_MONTH, -1);
			 
			 Timestamp dtocorAlterada = new Timestamp(calendar.getTimeInMillis());
			
			// Obtemos a data alterada
			Timestamp dataInativ = new Timestamp(calendar.getTimeInMillis());
	         selecionaFuncionariosAtivar(numcontrato, codocor, dtocorAlterada);
	    
		}
	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		DynamicVO occVO = (DynamicVO) event.getVo();
		
		BigDecimal numcontrato, codocor, codserv, codfunc;
		Timestamp dtocor;
		
		Timestamp dataDeHoje = new Timestamp(System.currentTimeMillis());
		
		numcontrato = occVO.asBigDecimal("NUMCONTRATO");
		codserv = occVO.asBigDecimal("CODPROD");
		codocor = occVO.asBigDecimal("CODOCOR");
		dtocor = occVO.asTimestamp("DTOCOR");
		
		if (codocor.compareTo(new BigDecimal(1.00)) == 0 || codocor.compareTo(new BigDecimal(18.00))==00) {
			codfunc = getCodfun(codserv);
			
			if (codfunc == null)
				return;
			
			 trataBeneficios(numcontrato, codserv, codfunc);
		}
		
		//Foi pedido para excluir os benefícios , quando o funcionário estiver ocorrência 17 (Afastamento)		
		if (codocor.compareTo(new BigDecimal(3.00)) == 0 || codocor.compareTo(new BigDecimal(17.00))==0) {
			codfunc = getCodfun(codserv);
			
			if (codfunc == null)
				return;
			
			//excluir somente do dia, pois podem lançar cancelamento com data posterior
			if (dtocor.compareTo(dataDeHoje) > 0) 
				return;
						
			removeFuncBenef(numcontrato, codserv);
			
		}
		

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
		
		BigDecimal numcontrato, codocor, codserv, codfunc = null;
		Timestamp dtocor;
		
		numcontrato = occVO.asBigDecimal("NUMCONTRATO");
		codserv = occVO.asBigDecimal("CODPROD");
		codocor = occVO.asBigDecimal("CODOCOR");
		dtocor = occVO.asTimestamp("DTOCOR");
		
		validaMGE(occVO);
		
		if (codocor.compareTo(new BigDecimal(1.00)) == 0 || codocor.compareTo(new BigDecimal(18.00))==0)  {
			codfunc = getCodfun(codserv);
			
			if (codfunc == null)
				return;
			
			removeFuncBenef(numcontrato, codserv);
			
		}
		
		if (codocor.compareTo(new BigDecimal(3.00)) == 0 || codocor.compareTo(new BigDecimal(17.00))==0) {
			codfunc = getCodfun(codserv);
			
			if (codfunc == null)
				return;
			
			
			trataBeneficios(numcontrato, codserv, codfunc);
		
		}			
		

	}
    public void selecionaFuncionariosAtivar(BigDecimal numcontrato, BigDecimal codocor, Timestamp data) throws Exception {
    	
    	JdbcWrapper jdbc = null;
    	BigDecimal codprod;
    	
		
		try {
			
			
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
				
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numcontrato);
			
			Timestamp dataDeHoje = new Timestamp(System.currentTimeMillis());
			
			sql.setNamedParameter("SITPROD", "C");
			sql.setNamedParameter("CODOCOR", codocor);
			sql.setNamedParameter("DATA", data);
			
			//verificar se o lançamento não foi posterior aí o status do funcionário ainda não mudou para Cancelado
			if (data.after(dataDeHoje)) {
				sql.setNamedParameter("SITPROD", "A");				
			}
								
			sql.appendSql(" SELECT T.NUMCONTRATO, T.CODPROD ");
			sql.appendSql("   FROM TCSPSC T"); 
			sql.appendSql("   JOIN TFPFUN F ON (F.AD_CODPRODREF = T.CODPROD)"); 
			sql.appendSql("  WHERE T.NUMCONTRATO = :NUMCONTRATO "); 
			sql.appendSql("    AND F.AD_CODPRODREF = T.CODPROD "); 
			sql.appendSql("    AND T.SITPROD = :SITPROD "); 
			sql.appendSql("     AND EXISTS (SELECT O.NUMCONTRATO FROM TCSOCC O ");
			sql.appendSql("                   WHERE O.NUMCONTRATO = T.NUMCONTRATO ");
			sql.appendSql("                     AND O.CODPROD = T.CODPROD " );
		 	sql.appendSql("                     AND O.CODOCOR = :CODOCOR" );
		 	sql.appendSql("                     AND O.DTOCOR = :DATA)" );
		 				
			ResultSet rs = sql.executeQuery();						
			
			while (rs.next()) {
				
				codprod = rs.getBigDecimal("CODPROD");	
				numcontrato = rs.getBigDecimal("NUMCONTRATO");
				
				ativarFuncionarios(numcontrato, codprod);
				excluirCancelamentos(numcontrato, codprod, data);
		        					
			}
			

			
		}finally {
			jdbc.closeSession();
		}
    
    };
    public void excluirCancelamentos(BigDecimal numcontrato, BigDecimal codprod, Timestamp data) throws Exception {
    	
    	BigDecimal codocor = new BigDecimal(3);
    	
    	//Configura o critério de busca
		FinderWrapper finderDelete = new FinderWrapper("OcorrenciaContrato", "this.NUMCONTRATO = ? AND this.CODPROD = ? AND CODOCOR = ? AND DTOCOR = ? ");
		
		finderDelete.setFinderArguments(new Object[] { numcontrato, codprod, codocor, data });
				
		//Exclui os registros pelo criterio
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		
		dwfFacade.removeByCriteria(finderDelete);
	
    	
    }
    public void ativarFuncionarios(BigDecimal numcontrato, BigDecimal codprod) throws Exception {
    	//Busca o registro pela PK
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		
		PersistentLocalEntity parcelaDestEntity = dwfFacade.findEntityByPrimaryKey("ProdutoServicoContrato",
				new Object[] { numcontrato, codprod });
		DynamicVO contratoVO = (DynamicVO) parcelaDestEntity.getValueObject();

		//setar propriedades à serem atualizadas
		contratoVO.setProperty("SITPROD", "A");

		//realiza o update
		parcelaDestEntity.setValueObject((EntityVO) contratoVO);

    }
    public void excluirCancelamentos() {
    	
    }

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		DynamicVO occVO = (DynamicVO) event.getVo();
		
		BigDecimal numcontrato, codocor, codserv, codfunc;
		Timestamp dtocor;
		
		numcontrato = occVO.asBigDecimal("NUMCONTRATO");
		codserv = occVO.asBigDecimal("CODPROD");
		codocor = occVO.asBigDecimal("CODOCOR");
		dtocor = occVO.asTimestamp("DTOCOR");
		
		ajustaUsuario(occVO);
		
		JapeWrapper ocoDAO = JapeFactory.dao("Ocorrencia");
		DynamicVO ocoVO = ocoDAO.findByPK(codocor);
		
		if(ocoVO.asString("SITPROD").equals("A")) {
			FuncionarioHelper helper = new FuncionarioHelper();
			Funcionario fun = helper.getFuncionario(codserv);
			
			if(fun != null) {
				helper.verificaAtivacaoOutrosCon(numcontrato, codserv);
				helper.verificaPostoContrato(numcontrato, fun.getCodfuncao());
				helper.verificaEscala(numcontrato, fun.getCodfuncao(), fun.getCodserv());
				helper.verificaEmpresa(numcontrato, fun.getCodemp());
				helper.verificaLimitePostos(numcontrato, fun.getCodfuncao(), codserv);
				
				
				if (codocor.compareTo(new BigDecimal(16)) != 0 && 
						codocor.compareTo(new BigDecimal(11)) != 0	&&
						codocor.compareTo(new BigDecimal(14)) != 0 &&
						codocor.compareTo(new BigDecimal(9)) != 0){
				
					helper.verificaDiaAtivacao(codserv, dtocor);
			
				}
			}
		}
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		DynamicVO occVO = (DynamicVO) event.getVo();
		
		ModifingFields camposModificados = event.getModifingFields();

		if (camposModificados.isModifingAny("CODOCOR")) {
			throw new Exception("<b>Não é possível realizar a troca da ocorrência, por gentileza refazer o lançamento</b>");
		}

		
		validaMGE(occVO);
		ajustaUsuario(occVO);
		
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
	
	
	private void trataBeneficios(BigDecimal numcontrato, BigDecimal codserv, BigDecimal codfunc) throws Exception {
		BigDecimal codfuncao, codposto, codtbe;	
	
		codfuncao = getCodfuncao(numcontrato, codserv);
	
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUMCONTRATO", numcontrato);
			sql.setNamedParameter("CODFUNCAO", codfuncao);
			sql.setNamedParameter("CODSERV", codserv);
			
			sql.appendSql(" SELECT FUNBEN.CODPOSTO, FUNBEN.CODTBE FROM AD_BENEFFORN FUNBEN" );
			sql.appendSql(" JOIN AD_POSTOCONTRATO POSTO ON (FUNBEN.NUMCONTRATO = POSTO.NUMCONTRATO AND FUNBEN.CODPOSTO = POSTO.CODPOSTO)" );
			sql.appendSql(" JOIN TFPTBE BEN ON (FUNBEN.CODTBE = BEN.CODTBE)" );
			sql.appendSql(" AND FUNBEN.NUMCONTRATO = :NUMCONTRATO" );
			sql.appendSql(" AND POSTO.CODFUNCAO = :CODFUNCAO" );
			sql.appendSql(" AND nullValue(BEN.AD_GENERICO, 'N') = 'N'" );
			sql.appendSql(" AND FUNBEN.CODTBE NOT IN (SELECT CODTBE FROM AD_FUNBENEF WHERE NUMCONTRATO = FUNBEN.NUMCONTRATO AND CODPOSTO = FUNBEN.CODPOSTO AND CODPROD = :CODSERV)" );

			
			ResultSet rs = sql.executeQuery();
			
			while (rs.next()) {
				codposto = rs.getBigDecimal("CODPOSTO");
				codtbe = rs.getBigDecimal("CODTBE");
				
				insereFuncBenef(numcontrato, codposto, codtbe, codserv, codfunc);
			}
			
		}finally {
			jdbc.closeSession();
		}
		
	}

	private BigDecimal getCodfuncao(BigDecimal numcontrato, BigDecimal codserv) throws Exception {
		//Busca o registro pela PK
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity destEntity = dwfFacade.findEntityByPrimaryKey("ProdutoServicoContrato", new Object[] { numcontrato, codserv });
		DynamicVO pscVO = (DynamicVO) destEntity.getValueObject();

		return pscVO.asBigDecimal("AD_CODFUNCAO");
		
	}
	
	private void insereFuncBenef(BigDecimal numcontrato, BigDecimal codposto, BigDecimal codtbe, BigDecimal codserv, BigDecimal codfunc) throws Exception {
		//Busca a tabela a ser inserida, com base na instância
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO funcBen;

		funcBen = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AD_FUNBENEF");
		//inclui os valores desejados nos campos
		funcBen.setProperty("CODPOSTO", codposto);
		funcBen.setProperty("NUMCONTRATO", numcontrato);
		funcBen.setProperty("CODPROD", codserv);
		funcBen.setProperty("OPTANTE", "S");
		funcBen.setProperty("CODTBE", codtbe);
		funcBen.setProperty("CODFUNC", codfunc);
		
		
		try {
			//realiza o insert
			dwfEntityFacade.createEntity("AD_FUNBENEF", (EntityVO) funcBen);
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
	
	
	private void removeFuncBenef(BigDecimal numcontrato, BigDecimal codserv) throws Exception {
		//Configura o critério de busca
		FinderWrapper finderDelete = new FinderWrapper("AD_FUNBENEF", "this.NUMCONTRATO = ? AND this.CODPROD = ?");
		finderDelete.setFinderArguments(new Object[] { numcontrato, codserv });
	
		//Exclui os registros pelo criterio
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		dwfFacade.removeByCriteria(finderDelete);

	}
	
	private void ajustaUsuario(DynamicVO occVO) {
		BigDecimal codusu = (BigDecimal) JapeSessionContext.getProperty("usuario_logado");
		
		occVO.setProperty("CODUSU", codusu);
	}
	
	private void validaMGE(DynamicVO occVO) throws Exception {
		String flag = occVO.asString("AD_LANCMGE");
		Timestamp dtocor = occVO.asTimestamp("DTOCOR");
		BigDecimal codocor = occVO.asBigDecimal("CODOCOR");
		BigDecimal qtd;
		
		if(flag != null && flag.equals("S")) {
			throw new Exception ("<b>Não é possível alterar um evento com origem do RH: </b>"+ codocor );
		}
		
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("DTOCOR", dtocor);
			sql.setNamedParameter("NUMCONTRATO", occVO.asBigDecimal("NUMCONTRATO"));
			sql.setNamedParameter("CODPROD", occVO.asBigDecimal("CODPROD"));
			
			
			sql.appendSql(" SELECT COUNT(*) AS QTD " );
			sql.appendSql(" FROM TCSOCC " );
			sql.appendSql(" WHERE NUMCONTRATO = :NUMCONTRATO " );
			sql.appendSql(" AND CODPROD = :CODPROD " );
			sql.appendSql(" AND TRUNC(DTOCOR) = TRUNC(:DTOCOR)" );
			sql.appendSql(" AND AD_LANCMGE IS NOT NULL" );
			

			
			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				qtd = rs.getBigDecimal("QTD");
				
				if (qtd.compareTo(BigDecimal.ONE) > 0) {	
					throw new Exception ("<b>Não é possível alterar um evento com origem do RH: </b>"+qtd);
				}
			}
			
		}finally {
			jdbc.closeSession();
		}
		
		
	}
	
}
