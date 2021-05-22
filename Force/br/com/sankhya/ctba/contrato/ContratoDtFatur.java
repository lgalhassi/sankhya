package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.EntityPrimaryKey;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.PrePersistEntityState;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 14/07/2020
Objetivo: Criar cabeçalho para ajustar a rotina de faturamento nativa, incluindo a mensagem de fatuamento anterior pendente
Tabela Alvo: TCSCON
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class ContratoDtFatur implements EventoProgramavelJava {

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
		DynamicVO conVO = (DynamicVO) event.getVo();
		
		Timestamp dtproxfat;

		dtproxfat = conVO.asTimestamp("DTREFPROXFAT");
		
		if(dtproxfat != null) {
			ajustaFaturamento(conVO);
		}
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		DynamicVO conVO = (DynamicVO) event.getVo();
		
		Timestamp dtproxfat;
		
		dtproxfat = conVO.asTimestamp("DTREFPROXFAT");
		
		ModifingFields camposModificados = event.getModifingFields();

		if (camposModificados.isModifingAny("DTREFPROXFAT") ) {

			if(isPrimeiroFat(conVO.asBigDecimal("NUMCONTRATO"))) {
				if (dtproxfat != null) {
					removeAjuste(conVO.asBigDecimal("NUMCONTRATO"));
					ajustaFaturamento(conVO);
				}
				else {
					removeAjuste(conVO.asBigDecimal("NUMCONTRATO"));
				}	
			}
			else {
				removeAjuste(conVO.asBigDecimal("NUMCONTRATO"));
			}
		}

	}
	
	
	
	

	private void ajustaFaturamento(DynamicVO conVO) throws Exception {
		Timestamp dtref, dtrefajust;
		
		dtref = conVO.asTimestamp("DTREFPROXFAT");
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(dtref);
		cal.add(Calendar.MONTH, -1);
		dtrefajust = new Timestamp(cal.getTime().getTime());
		
		BigDecimal modeloNota = (BigDecimal) MGECoreParameter.getParameter("ADMODAJUSTFAT");

		if (modeloNota == null) {
			throw new Exception(
					"Não foi possível encontrar o modelo de nota cadastrado na preferência ADMODAJUSTFAT");
		}
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity cabEntity = dwfFacade.findEntityByPrimaryKey("CabecalhoNota",
				new Object[] { modeloNota });

		DynamicVO cabVOorig = (DynamicVO) cabEntity.getValueObject();
		DynamicVO cabVO = cabVOorig.buildClone();
		
		cabVO.setProperty("NUNOTA", null);
		cabVO.setProperty("NUMNOTA", BigDecimal.ZERO);
		cabVO.setProperty("CODPARC", conVO.asBigDecimal("CODPARC"));
		cabVO.setProperty("CODEMP", conVO.asBigDecimal("CODEMP"));
		cabVO.setProperty("NUMCONTRATO", conVO.asBigDecimal("NUMCONTRATO"));
		cabVO.setProperty("DTVAL", dtrefajust);
		cabVO.setProperty("AD_AJUSTE_REF_CON", "S");
		cabVO.setProperty("TIPMOV", cabVO.asString("TipoOperacao.TIPMOV"));
		cabVO.setProperty("DTNEG", dtrefajust);
		
		AuthenticationInfo authInfo = AuthenticationInfo.getCurrent();
		CACHelper cacHelper = new CACHelper();

		PrePersistEntityState cabState = PrePersistEntityState.build(dwfFacade, "CabecalhoNota", cabVO);
		BarramentoRegra bRegrasCab = cacHelper.incluirAlterarCabecalho(authInfo, cabState);

		Collection<EntityPrimaryKey> pk = bRegrasCab.getDadosBarramento().getPksEnvolvidas();

		if (pk.isEmpty()) {
			throw new MGEModelException("Não foi possível gerar o movimento no portal");
		}
	}
	
	
	private void removeAjuste(BigDecimal numcontrato) throws Exception {
		//Configura o critério de busca
		FinderWrapper finderDelete = new FinderWrapper("CabecalhoNota", "this.NUMCONTRATO = ? "
				+ "AND nullValue(AD_AJUSTE_REF_CON,'N') = 'S'");
		finderDelete.setFinderArguments(new Object[] { numcontrato });
		//Exclui os registros pelo criterio
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		dwfFacade.removeByCriteria(finderDelete);

	}
	
	private boolean isPrimeiroFat(BigDecimal numcontrato) throws Exception {
		//Configura o critério de busca
		FinderWrapper finder = new FinderWrapper("CabecalhoNota",
				"this.NUMCONTRATO = ? AND nullValue(AD_AJUSTE_REF_CON,'N') = 'N' ");
		//Insere os argumentos caso existam	
		finder.setFinderArguments(new Object[] { numcontrato });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		//Realiza a busca na tabela pelos critérios
		@SuppressWarnings("unchecked")
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("CabecalhoNota", finder);
		//Itera entre os registos encontrados
		if (libCollection.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}
	
}
