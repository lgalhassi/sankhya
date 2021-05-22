package br.com.sankhya.ctba.campanhaCartao;

import java.math.BigDecimal;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class CarregarDadosCampanha {

	public List<DadosCampanha> carregarDadosCampanha(BigDecimal codcam) throws Exception {

		JdbcWrapper jdbc = null;
		List<DadosCampanha> listaCampanha = new ArrayList<DadosCampanha>();
		
		try {

			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);

			sql.setNamedParameter("CODCAM", codcam);
				
			sql.appendSql(" SELECT B.CODPARC, B.VLRPAGO, P.ROLCARTAO, P.CGC_CPF, P.RAZAOSOCIAL ");
			sql.appendSql(" FROM AD_CAMPCAR A");
			sql.appendSql(" JOIN AD_CAMPCARCLI B ON A.CODCAM = B.CODCAM");
			sql.appendSql(" JOIN TGFPAR P ON B.CODPARC = P.CODPARC");
			sql.appendSql(" WHERE A.CODCAM = :CODCAM ");
			sql.appendSql(" AND B.VLRVENDA >= B.VLRMETA");
			sql.appendSql(" AND VLRPAGO > 0");
			sql.appendSql(" AND P.ROLCARTAO IS NOT NULL");
			
			ResultSet rs = sql.executeQuery();

			while (rs.next()) {
				
				 DadosCampanha dados = new DadosCampanha();

				 dados.setCodparc(rs.getBigDecimal("CODPARC"));
				 dados.setValor(rs.getBigDecimal("VLRPAGO"));
				 dados.setRolCartao(rs.getBigDecimal("ROLCARTAO"));
				 dados.setCgcCpf(rs.getString("CGC_CPF"));
				 
				 String razaosocial = rs.getString("RAZAOSOCIAL");
				 
				 if (razaosocial.length()>39) {				 
				    dados.setRazaoSocial(rs.getString("RAZAOSOCIAL").substring(0, 39));
				 } else {
					 dados.setRazaoSocial(rs.getString("RAZAOSOCIAL"));
				 }
				 listaCampanha.add(dados);
			} 

		} finally {
			jdbc.closeSession();
		}
		return listaCampanha;

	}
}
