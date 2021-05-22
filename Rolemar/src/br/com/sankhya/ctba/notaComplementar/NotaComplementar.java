package br.com.sankhya.ctba.notaComplementar;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class NotaComplementar implements EventoProgramavelJava {

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

		trataComplemento(cabVO);

	}

	@Override
	public void beforeUpdate(PersistenceEvent persistenceEvent) throws Exception {

		DynamicVO cabVO = (DynamicVO) persistenceEvent.getVo();
		
		ModifingFields mf = persistenceEvent.getModifingFields();

		if (mf.isModifing("AD_NUNOTAPAI_AUX") && !mf.isModifingAny("AD_NUNOTAPAI,AD_NUNOTAPAI_COMP")) {
			trataComplemento(cabVO);
		}

	}

	public void trataComplemento(DynamicVO cabVO) throws Exception {

		BigDecimal codparc;
		String encontrou = "N";
		BigDecimal notaAux = null;
		String tipoMovimento = null;

		cabVO.setProperty("AD_NUNOTAPAI", null);
		cabVO.setProperty("AD_NUNOTAPAI_COMP", null);

		codparc = cabVO.asBigDecimal("CODPARC");
		notaAux = cabVO.asBigDecimal("AD_NUNOTAPAI_AUX");

		if (notaAux != null) {

			tipoMovimento = buscarTipoMovimento(notaAux);

			if (tipoMovimento.equals("Pedido")) {
				cabVO.setProperty("AD_NUNOTAPAI", notaAux);
			} else if (tipoMovimento.equals("Nota")) {
				cabVO.setProperty("AD_NUNOTAPAI_COMP", notaAux);
			} else {
				throw new Exception("Tipo difente de Pedido e Nota não reconhecido");
			}
		}

		encontrou = verificaNotaPai(codparc);
		cabVO.setProperty("AD_AVISONOTA_COMPL", encontrou);

		if (notaAux != null) {
			copiaNotaComplementarPai(cabVO, notaAux, encontrou);
		}

	}

	public String verificaNotaPai(BigDecimal codparc) throws Exception {

		String encontrou = "N";

		JdbcWrapper jdbc = null;

		try {

			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("CODPARC", codparc);
			sql.setNamedParameter("TIPOMOVIMENTO", "Nota");

			sql.appendSql(" SELECT CAB.CODPARC ");
			sql.appendSql("FROM AD_NOTACOMPL CAB ");
			sql.appendSql("WHERE CAB.CODPARC = :CODPARC ");
			sql.appendSql("AND CAB.TIPOMOVIMENTO = :TIPOMOVIMENTO ");

			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				encontrou = "S";
			}

			return encontrou;

		} finally {
			jdbc.closeSession();
		}

	}

	public String buscarTipoMovimento(BigDecimal nuNotaPai) throws Exception {

		String tipoMovimento = null;

		JdbcWrapper jdbc = null;

		try {

			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUNOTA", nuNotaPai);

			sql.appendSql(" SELECT CAB.TIPOMOVIMENTO ");
			sql.appendSql("FROM AD_NOTACOMPL CAB ");
			sql.appendSql("WHERE CAB.NUNOTA = :NUNOTA ");

			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				tipoMovimento = rs.getString("TIPOMOVIMENTO");
			}

			return tipoMovimento;

		} finally {
			jdbc.closeSession();
		}

	}

	public void copiaNotaComplementarPai(DynamicVO cabVO, BigDecimal nuNotaPai, String encontrou) throws Exception {

		JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO cabPaiVO = cabDAO.findByPK(nuNotaPai);

		cabVO.setProperty("CODTIPVENDA", cabPaiVO.asBigDecimal("CODTIPVENDA"));
		cabVO.setProperty("DHTIPVENDA", ComercialUtils.getTipoNegociacao(cabPaiVO.asBigDecimal("CODTIPVENDA")).asTimestamp("DHALTER"));
		cabVO.setProperty("ROLPRAZO", cabPaiVO.asString("ROLPRAZO"));
		cabVO.setProperty("ROLQTDPARCELAS", cabPaiVO.asBigDecimal("ROLQTDPARCELAS"));
		cabVO.setProperty("CODPARCTRANSP", cabPaiVO.asBigDecimal("CODPARCTRANSP"));
		cabVO.setProperty("CODNAT", cabPaiVO.asBigDecimal("CODNAT"));
		cabVO.setProperty("CODCENCUS", cabPaiVO.asBigDecimal("CODCENCUS"));
		cabVO.setProperty("CIF_FOB", cabPaiVO.asString("CIF_FOB"));
		cabVO.setProperty("OBSERVACAO", cabPaiVO.asString("OBSERVACAO"));
		cabVO.setProperty("AD_AVISONOTA_COMPL", encontrou);
		cabVO.setProperty("TIPFRETE", cabPaiVO.asString("TIPFRETE"));
		cabVO.setProperty("AD_TIPOFRETE", cabPaiVO.asString("AD_TIPOFRETE"));

		BigDecimal empresaPai = cabPaiVO.asBigDecimal("CODEMP");
		BigDecimal empresa = cabVO.asBigDecimal("CODEMP");

		if (empresaPai.compareTo(empresa) != 0) {
			throw new Exception("Empresa do complemento não pode ser diferente da empresa atual");
		}

	}
}
