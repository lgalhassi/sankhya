/*
@author: Jorge Felipe Maceulevicius - Sankhya Curitiba
@date: 13/05/2020
GAP : 176

Objetivo: Buscar a Data do Contrato para Inserir na Vigencia do Beneficio e validar duplicados
Tabela alvo: AD_BENEFFORN
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
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class ValidacoesBeneficio implements EventoProgramavelJava {

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
	public void beforeDelete(PersistenceEvent event) throws Exception {
		// TODO Auto-generated method stub
		DynamicVO benVO = (DynamicVO) event.getVo();
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		if(!helper.conGuardaChuva(benVO.asBigDecimal("NUMCONTRATO"))) {
			validapermissao();	
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		
		BigDecimal numContrato, codtbe, codposto;
		
		Timestamp dataContrato = null;
		
		DynamicVO servVO = (DynamicVO) event.getVo();
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		if(!helper.conGuardaChuva(servVO.asBigDecimal("NUMCONTRATO"))) {
			validapermissao();
		}
		
		numContrato = servVO.asBigDecimal("NUMCONTRATO");
		codtbe = servVO.asBigDecimal("CODTBE");
		codposto = servVO.asBigDecimal("CODPOSTO");
		
		verificaDuplicado(numContrato, codtbe, codposto);
		
		FinderWrapper finderUpd = new FinderWrapper("Contrato", "NUMCONTRATO = ? "); 
		finderUpd.setFinderArguments(new Object[] { numContrato });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		Collection<PersistentLocalEntity>  libCollection = dwfFacade
		               .findByDynamicFinder("Contrato", finderUpd);
			for (PersistentLocalEntity libEntity : libCollection) {
				DynamicVO conVO = ( DynamicVO ) libEntity.getValueObject();
				
				dataContrato = conVO.asTimestamp("DTCONTRATO");
				
				}
			
			servVO.setProperty("DTINIVEN", dataContrato);
			
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		DynamicVO servVO = (DynamicVO) event.getVo();
		BigDecimal numContrato, codtbe, codposto;
		
		FuncionarioHelper helper = new FuncionarioHelper();
		
		if(!helper.conGuardaChuva(servVO.asBigDecimal("NUMCONTRATO"))) {
			validapermissao();	
		}
		
		ModifingFields camposModificados = event.getModifingFields();

		if (camposModificados.isModifingAny("CODTBE")) {
			numContrato = servVO.asBigDecimal("NUMCONTRATO");
			codtbe = servVO.asBigDecimal("CODTBE");
			codposto = servVO.asBigDecimal("CODPOSTO");
			
			verificaDuplicado(numContrato, codtbe, codposto);
		}
	}
	
	private void verificaDuplicado(BigDecimal numcontrato, BigDecimal codtbe, BigDecimal codposto) throws Exception {
		//Configura o critério de busca
		FinderWrapper finder = new FinderWrapper("AD_BENEFFORN",
				"this.NUMCONTRATO = ? and this.CODTBE = ? and this.CODPOSTO = ?");
		//Insere os argumentos caso existam	
		finder.setFinderArguments(new Object[] { numcontrato, codtbe, codposto });
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		//Realiza a busca na tabela pelos critérios
		@SuppressWarnings("unchecked")
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("AD_BENEFFORN", finder);
		//Itera entre os registos encontrados
		if (libCollection.isEmpty()) {
			return;
		} else {
			throw new Exception ("<b>Benefício já cadastrado</b>");
		}
	}
	
	private void validapermissao() throws Exception {
		BigDecimal codusu = (BigDecimal) JapeSessionContext.getProperty("usuario_logado");
		
		JapeWrapper usuDAO = JapeFactory.dao("Usuario");
		DynamicVO usuVO = usuDAO.findByPK(codusu);
		
		String flag = usuVO.asString("AD_PERMALTPOSTOS");
		if (flag == null || flag.equals("N")) {
			throw new Exception("<b>Usuário não tem permissão para alterar o Cadastro de Postos</b>");
		}
	}

}
