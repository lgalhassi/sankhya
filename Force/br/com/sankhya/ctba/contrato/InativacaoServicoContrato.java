package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
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
Data de Implementação: 12/01/2021
Objetivo: Cancelar os funcionários dos contratos quando o serviço principal do contrato é inativado
Tabela Alvo: TCSOCC
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class InativacaoServicoContrato implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		DynamicVO occVO = (DynamicVO) event.getVo();
		BigDecimal codserv, codocor, numcontrato;
		Timestamp dtocor;
		
		codserv = occVO.asBigDecimal("CODPROD");		
		codocor = occVO.asBigDecimal("CODOCOR");
		numcontrato = occVO.asBigDecimal("NUMCONTRATO");
		dtocor = occVO.asTimestamp("DTOCOR");
		
		FuncionarioHelper helper = new FuncionarioHelper();
		Funcionario fun = helper.getFuncionario(codserv);
		
		if(fun != null) {
			return;
		}
		
		if (codocor.compareTo(new BigDecimal(3.00)) == 0) {		
			
			
			JapeSessionContext.putProperty("br.com.sankhya.ctba.contrato.inativacao", Boolean.valueOf(true));
			inativaFuncionarios(numcontrato, dtocor, codserv);
			JapeSessionContext.putProperty("br.com.sankhya.ctba.contrato.inativacao", Boolean.valueOf(false));
			
			
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
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}
	
	
	private void inativaFuncionarios(BigDecimal numcontrato, Timestamp dtocor, BigDecimal codserv) throws Exception {
		JapeWrapper conDAO = JapeFactory.dao("Contrato");
		DynamicVO conVO = conDAO.findByPK(numcontrato);
		
		BigDecimal codcontato, codparc, coddep;
		
		codcontato = conVO.asBigDecimal("CODCONTATO");
		codparc = conVO.asBigDecimal("CODPARC");
		coddep = conVO.asBigDecimal("AD_CODDEP");
		
		Timestamp timestamp = dtocor;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(timestamp);
		//calendar.add(Calendar.DAY_OF_MONTH, -1);
		//começou dar erro nessa parte, estou fazendo via banco
		
		// Obtemos a data alterada
		Timestamp dataInativ = new Timestamp(calendar.getTimeInMillis());
		
		//Configura o critério de busca
		FinderWrapper finderUpd = new FinderWrapper("ProdutoServicoContrato", "this.NUMCONTRATO = ? AND this.SITPROD <> 'C' and this.CODPROD <> ?");
	
		//Insere os argumentos caso existam		
		finderUpd.setFinderArguments(new Object[] { numcontrato, codserv });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		
		//Realiza a busca na tabela pelos critérios
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("ProdutoServicoContrato", finderUpd);
		
		//Itera entre os registos encontrados
		for (PersistentLocalEntity libEntity : libCollection) {
			DynamicVO pscVO = (DynamicVO) libEntity.getValueObject();

			insereOcorrencia(pscVO.asBigDecimal("CODPROD"), numcontrato, codcontato, codparc, coddep, dataInativ);
		}

	}	
	
	@SuppressWarnings("deprecation")
	private void insereOcorrencia(BigDecimal codProd, BigDecimal numContrato, BigDecimal codContato
			, BigDecimal codParc, BigDecimal coddep, Timestamp dataInativ) throws Exception {
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO occVO;
		
		//System.out.println("Inativação: "+dataInativ);
				
		occVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("OcorrenciaContrato");
		//inclui os valores desejados nos campos
		occVO.setProperty("CODPROD", codProd);
		occVO.setProperty("NUMCONTRATO", numContrato);
		occVO.setProperty("CODOCOR", new BigDecimal(3));
		occVO.setProperty("DTOCOR", dataInativ);
		occVO.setProperty("CODUSU", BigDecimal.ZERO);
		occVO.setProperty("DESCRICAO", "Cancelamento por Inatividade do Contrato");	
		occVO.setProperty("CODPARC", codParc);	
		occVO.setProperty("CODCONTATO", codContato);
		occVO.setProperty("AD_CODDEP", coddep);	
		
		//System.out.println("********** DEBUG1 ******* Contrato: " + numContrato + " -- Codprod: " + codProd + " --- Codparc: " + codParc + " ---- Contato: " + codContato + "----Data:  "+dataInativ) ;

		//throw new Exception ("Data Cancelamento "+dataInativ);
		
		//realiza o insert
		dwfEntityFacade.createEntity("OcorrenciaContrato", (EntityVO) occVO);
		
		
	}

}
