package br.com.sankhya.ctba.promocao;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class ParceirosEnvProm implements EventoProgramavelJava {

	private DynamicVO getEnvProm(BigDecimal codPerfil) throws Exception {
		JapeWrapper envPromDAO = JapeFactory.dao("AD_ENVPROM");
		return envPromDAO.findByPK(codPerfil);
	}

	private DynamicVO getParceiroByCodParc(BigDecimal codParc) throws Exception {
		JapeWrapper parcDAO = JapeFactory.dao("Parceiro");
		return parcDAO.findByPK(codParc);
	}

	private DynamicVO getContatoByPerfilContato(BigDecimal codParc) throws Exception {
		JapeWrapper contatoDAO = JapeFactory.dao("Contato");
		return contatoDAO.findOne("this.CODPARC = ?", codParc);
	}

	private DynamicVO getPerfilContatoByCodPerfilEmail(BigDecimal codTipParc) throws Exception {
		JapeWrapper perfilContatoDAO = JapeFactory.dao("PerfilContato");
		DynamicVO perfilContatoVO = perfilContatoDAO.findOne("this.CODTIPPARC = ?", codTipParc);

		BigDecimal codParc = perfilContatoVO.asBigDecimal("CODPARC");
		return getContatoByPerfilContato(codParc);
	}

	private void createUpdateParceiros(PersistenceEvent event) throws Exception {
		DynamicVO parceirosEnvPromVO = (DynamicVO) event.getVo();

		BigDecimal codParc = parceirosEnvPromVO.asBigDecimal("CODPARC");
		DynamicVO parceiroVO = getParceiroByCodParc(codParc);

		parceirosEnvPromVO.setProperty("CNPJ", parceiroVO.asString("CGC_CPF"));
		parceirosEnvPromVO.setProperty("NOMEPARC", parceiroVO.asString("NOMEPARC"));
		parceirosEnvPromVO.setProperty("RAZSOCPARC", parceiroVO.asString("RAZAOSOCIAL"));
		parceirosEnvPromVO.setProperty("BONIFC", parceiroVO.asBigDecimal("ROLBONIFICA"));

		DynamicVO envPromVO = getEnvProm(parceirosEnvPromVO.asBigDecimal("PERFIL"));
		BigDecimal codPerfilEmail = envPromVO.asBigDecimal("CODTIPPARC");

		if (codPerfilEmail != null) {
			DynamicVO parceiroEmailVO = getPerfilContatoByCodPerfilEmail(codPerfilEmail);

			parceirosEnvPromVO.setProperty("CONTPARC", parceiroEmailVO.asString("NOMECONTATO"));
			parceirosEnvPromVO.setProperty("MAILCONT", parceiroEmailVO.asString("EMAIL"));
		}
	}

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
		createUpdateParceiros(event);
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		createUpdateParceiros(event);
	}

}
