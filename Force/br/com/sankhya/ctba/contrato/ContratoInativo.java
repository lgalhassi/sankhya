/*
@author: Jorge Felipe Maceulevicius - Sankhya Curitiba
@date: 13/05/2020
GAP : 176

Objetivo: Tonar todos os Funcionarios Inativos no Contrato e inserir a Data de Vigencia Final nos Beneficios.
Tabela alvo: TCSCON
Versões: 
	1.0 - 13/05/2020 - Implementação de Personalização.	

*/
package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 12/01/2021
Objetivo: Realizar as operações referentes a inativação de contrato
Tabela Alvo: TCSCON
Histórico:
1.0 - Implementação da rotina
1.1 - Remoção do cancelamento do funcionário na etapa de iantivação do contrato. 
	  Ela foi movida para o cancelamento do contrato conforme solicitação da Force

**********************************************************************************************************************************************************************************************/

public class ContratoInativo implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub


	}

	@Override
	public void afterUpdate(PersistenceEvent event) throws Exception {
		
		ModifingFields camposModificados = event.getModifingFields();
		
		if(camposModificados.isModifingAny("ATIVO") ){
			
			String ativo ;
			
			BigDecimal numContrato, codContato, codParc, coddep;
			
			DynamicVO conVO = (DynamicVO) event.getVo();
			
			ativo = conVO.asString("ATIVO");
			numContrato = conVO.asBigDecimal("NUMCONTRATO");
			codContato = conVO.asBigDecimal("CODCONTATO");
			codParc = conVO.asBigDecimal("CODPARC");
			coddep = conVO.asBigDecimal("AD_CODDEP");
			
			
			if(ativo != null && ativo.equals("N")) {
				/*
				if (coddep == null) {
					throw new Exception("<b>Para inativar o contrato é necessário informar qual o departamento que os funcionários serão alocados no campo \"Departamento (Inativação Contrato)\"</b>");
				}*/

				JapeSessionContext.putProperty("br.com.sankhya.ctba.contrato.inativacao", Boolean.valueOf(true));
				
				insereDatadeVigenciaFinal(numContrato);
				//updateFuncionarios(numContrato, codContato, codParc);
			//	updateServico(numContrato, codContato, codParc, coddep); -> Removido na versão 1.1
				
				JapeSessionContext.putProperty("br.com.sankhya.ctba.contrato.inativacao", Boolean.valueOf(false));
				
			}

		}

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
		DynamicVO conVO = (DynamicVO) event.getVo();
		
		conVO.setProperty("FATURPRORATA", "S");

	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}


	@SuppressWarnings("unchecked")
	private void insereDatadeVigenciaFinal(BigDecimal numContrato) throws Exception {
		
		Timestamp dhatual = (Timestamp) JapeSessionContext.getProperty("dh_atual");
		
		FinderWrapper finderUpd = new FinderWrapper("AD_BENEFFORN", "NUMCONTRATO = ? "); 
		finderUpd.setFinderArguments(new Object[] { numContrato });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<PersistentLocalEntity>  libCollection = dwfFacade
		               .findByDynamicFinder("AD_BENEFFORN", finderUpd);
			for (PersistentLocalEntity libEntity : libCollection) {
				DynamicVO benefVO = ( DynamicVO ) libEntity.getValueObject();
				
				benefVO.setProperty("DTFINVEN", dhatual);
				libEntity.setValueObject((EntityVO) benefVO);

			}
			
	}
	
	@SuppressWarnings("unchecked")
	private void updateServico(BigDecimal numContrato, BigDecimal codContato,BigDecimal codParc, BigDecimal coddep) throws Exception{

		BigDecimal codProd;
		FinderWrapper finderUpd = new FinderWrapper("ProdutoServicoContrato", "NUMCONTRATO = ? "); 
		finderUpd.setFinderArguments(new Object[] { numContrato });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

		Collection<PersistentLocalEntity>  libCollection = dwfFacade.findByDynamicFinder("ProdutoServicoContrato", finderUpd);
		for (PersistentLocalEntity libEntity : libCollection) {
			DynamicVO prodVO = ( DynamicVO ) libEntity.getValueObject();
			
			codProd = prodVO.asBigDecimal("CODPROD");
			insereOcorrencia(codProd, numContrato, codContato,  codParc, coddep);
		}
		
	}

	private void insereOcorrencia(BigDecimal codProd, BigDecimal numContrato, BigDecimal codContrato
			, BigDecimal codParc, BigDecimal coddep) throws Exception {
		
		Timestamp dhatual = (Timestamp) JapeSessionContext.getProperty("d_atual");
		
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO occVO;
			
		occVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("OcorrenciaContrato");
		//inclui os valores desejados nos campos
		occVO.setProperty("CODPROD", codProd);
		occVO.setProperty("NUMCONTRATO", numContrato);
		occVO.setProperty("CODOCOR", new BigDecimal(3));
		occVO.setProperty("DTOCOR", dhatual);
		occVO.setProperty("CODUSU", BigDecimal.ZERO);
		occVO.setProperty("DESCRICAO", "Cancelamento por Inatividade do Contrato");	
		occVO.setProperty("CODPARC", codParc);	
		occVO.setProperty("CODCONTATO", codContrato);
		occVO.setProperty("AD_CODDEP", coddep);	
		
		//realiza o insert
		dwfEntityFacade.createEntity("OcorrenciaContrato", (EntityVO) occVO);
		
		
	}
	
}
