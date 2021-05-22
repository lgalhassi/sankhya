/*********************************************************************************************************************************************************************************************

Autor: Luis Alessandro Galhassi - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 08/04/2021
Objetivo: Buscar se existe desconto de campanha para a venda
Tabela Alvo: AD_CAMPQUANT - AD_CAMPQUANTCONFIG - AD_CAMPQUANTPROD - AD_CAMPQUANTLIM
Histórico:
1.0 - Implementação da rotina

 **********************************************************************************************************************************************************************************************/

package br.com.sankhya.ctba.campanhaQuantitativa;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class CampanhaQuantitativa implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent persistenceEvent) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent persistenceEvent) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterUpdate(PersistenceEvent persistenceEvent) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeCommit(TransactionContext persistenceEvent) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeDelete(PersistenceEvent persistenceEvent) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeInsert(PersistenceEvent persistenceEvent) throws Exception {
		// TODO Auto-generated method stub

		DynamicVO cabVO = (DynamicVO) persistenceEvent.getVo();

		verificarCampanha(cabVO);


	}

	public void verificarCampanha(DynamicVO cabVO) throws Exception {

		BigDecimal codContrato = cabVO.asBigDecimal("CONTRATO");

		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity prodEntity = dwfFacade.findEntityByPrimaryKey("RolAcordoCompra",
				new Object[] { codContrato });
		DynamicVO servVO = (DynamicVO) prodEntity.getValueObject();

		BigDecimal valorCampanha = BigDecimal.ZERO;
		BigDecimal codEmp = BigDecimal.ZERO;
		BigDecimal descqnt = BigDecimal.ZERO;
		Boolean existeProdutoCampanha = false;

		//produto similar
		BigDecimal codProd = cabVO.asBigDecimal("CODSIM"); 

		//linha de produtos
		BigDecimal linhaProd = servVO.asBigDecimal("SEQ");

		codEmp = servVO.asBigDecimal("CODEMP");
		Timestamp  vigor = servVO.asTimestamp("VIGOR");

		//desconto quantitativo
		descqnt = cabVO.asBigDecimal("DESCQNT");

		CampanhaQuantitativaDAO campanha = new CampanhaQuantitativaDAO();

		//vou buscar na campanha se não for digitado 
		if ( descqnt.compareTo(BigDecimal.ZERO) == 0) {

			    //SE O PRODUTO ESTIVER CADASTRADO ELE SERÁ UMA EXCESSÃO, ENTÃO PEGA O DESCONTO DO PRODUTO
			    //SENÃO PEGA O DESCONTO DA LINHA (DOUGLAS 09/04/2021)

				//1º verificar se o produto está dentro de uma campanha para a empresa			
				valorCampanha = campanha.buscarCampanhaProdutoEmpresa (codProd, codEmp, vigor);

				//2º verificar se o produto está dentro de uma campanha sem informar a empresa
				if (valorCampanha.compareTo(BigDecimal.ZERO)==0) {
					valorCampanha = campanha.buscarCampanhaProdutoSEmpresa (codProd,  vigor);
				}
				
				//3º se o produto é do mesmo grupo da campanha e não está na aba de produto
				if (valorCampanha.compareTo(BigDecimal.ZERO)==0 ){
					valorCampanha = campanha.buscarCampanhaGrupoEmpresa(linhaProd, codEmp,  vigor);

				}

				//4º verificar se o produto está dentro de um grupo/linha de uma campanha sem informar a empresa		 
				if (valorCampanha.compareTo(BigDecimal.ZERO)==0) {
					valorCampanha = campanha.buscarCampanhaGrupoSEmpresa(linhaProd, vigor);
				}
		
		} else {
			valorCampanha = descqnt;
		}
		

		atualizarValores(valorCampanha, cabVO);

	}
	public void atualizarValores(BigDecimal valor, DynamicVO cabVO) throws Exception {

		BigDecimal desconto = BigDecimal.ZERO;
		BigDecimal margemLucro = cabVO.asBigDecimal("MARGEMLUC");
		BigDecimal margemLucroApl = BigDecimal.ZERO;
		BigDecimal novaMargem = BigDecimal.ZERO;

		//atualizar desconto quantitativa
		cabVO.setProperty("DESCQNT", valor);		

		//atualizar margem de lucro
		//desconto = valor.divide(new BigDecimal(100));

		//margemLucroApl = margemLucro.multiply(desconto);

		//novaMargem = margemLucro.subtract(margemLucroApl);

		//cabVO.setProperty("MARGEMLUC", novaMargem);	

	}

	@Override
	public void beforeUpdate(PersistenceEvent persistenceEvent) throws Exception {

		DynamicVO cabVO = (DynamicVO) persistenceEvent.getVo();

		verificarCampanha(cabVO);		
	}


}
