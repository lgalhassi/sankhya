package br.com.sankhya.ctba.campanhaCartao;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class Anexo {
	
	public void addAnexo(QueryExecutor query, String chave, String nomeFile, BigDecimal nuMun) throws Exception {

		Timestamp dhatual = (Timestamp) JapeSessionContext.getProperty("dh_atual");

		// Adicionar registro na tabela TSIANX para aparecer como anexo na tela
		query.nativeSelect("SELECT MAX(NUATTACH) as NUATTACH FROM TSIANX");

		query.next(); // executa a query. Neste caso ela ir� retornar somente 1 registro

		BigDecimal nuattach = query.getBigDecimal("NUATTACH");

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO anexosVO;

		anexosVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoSistema");
		anexosVO.setProperty("NUATTACH", nuattach.add(BigDecimal.ONE));
		anexosVO.setProperty("NOMEINSTANCIA", "AD_CAMPCAR");
		anexosVO.setProperty("CHAVEARQUIVO", chave);
		anexosVO.setProperty("NOMEARQUIVO", nomeFile.toUpperCase());
		anexosVO.setProperty("DESCRICAO", nomeFile);
		anexosVO.setProperty("RESOURCEID", "br.com.sankhya.menu.adicional.AD_CAMPCAR");
		anexosVO.setProperty("TIPOAPRES", "LOC");
		anexosVO.setProperty("TIPOACESSO", "ALL");
		anexosVO.setProperty("CODUSU", BigDecimal.ZERO);
		anexosVO.setProperty("DHALTER", dhatual);
		anexosVO.setProperty("PKREGISTRO", nuMun.toString() + "_AD_CAMPCAR");
		anexosVO.setProperty("CODUSUALT", BigDecimal.ZERO);
		anexosVO.setProperty("DHCAD", dhatual);

		dwfEntityFacade.createEntity("AnexoSistema", (EntityVO) anexosVO);

	}
}
