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
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class CampanhaQuantitativaDAO implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public BigDecimal buscarCampanhaProdutoEmpresa(BigDecimal codProd, BigDecimal codEmp, Timestamp data) throws Exception {

		JdbcWrapper jdbc = null;

		try {

			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);

			sql.setNamedParameter("CODPROD", codProd);
			sql.setNamedParameter("CODEMP", codEmp);
			sql.setNamedParameter("DATA", data);
			sql.setNamedParameter("ATIVO", "S");

			sql.appendSql(" SELECT B.DESCONTO FROM AD_CAMPQUANT A");
			sql.appendSql(" JOIN AD_CAMPQUANTPROD B ON (A.CODCAMP = B.CODCAMP)");
			sql.appendSql(" WHERE A.CODEMP = :CODEMP ");
			sql.appendSql("   AND B.CODPROD = :CODPROD ");
			sql.appendSql("   AND :DATA BETWEEN A.DTINICIAL AND A.DTFINAL ");
			sql.appendSql("   AND A.ATIVA = :ATIVO");
			sql.appendSql("   AND A.DTINICIAL = (SELECT MAX(C.DTINICIAL)");
			sql.appendSql("                      FROM AD_CAMPQUANT C");
			sql.appendSql("                     WHERE C.CODEMP = A.CODEMP");
			sql.appendSql("                       AND C.CODCAMP = A.CODCAMP");
			sql.appendSql("                       AND :DATA BETWEEN C.DTINICIAL AND C.DTFINAL");
			sql.appendSql("                       AND C.ATIVA = :ATIVO)");  //SE ENCONTRAR MAIS DE UMA CAMPANHA NO PERÍODO VAI PEGAR A ÚLTIMA DATA INICIAL

			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				return rs.getBigDecimal("DESCONTO");
			} else {
				return BigDecimal.ZERO;
			}

		} finally {
			jdbc.closeSession();
		}

	}
	public BigDecimal buscarCampanhaProdutoSEmpresa(BigDecimal codProd,  Timestamp data) throws Exception {

		JdbcWrapper jdbc = null;

		try {

			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);

			sql.setNamedParameter("CODPROD", codProd);
			sql.setNamedParameter("DATA", data);
			sql.setNamedParameter("ATIVO", "S");

			sql.appendSql(" SELECT B.DESCONTO FROM AD_CAMPQUANT A");
			sql.appendSql(" JOIN AD_CAMPQUANTPROD B ON (A.CODCAMP = B.CODCAMP)");
			sql.appendSql(" WHERE B.CODPROD = :CODPROD ");
			sql.appendSql("   AND :DATA BETWEEN A.DTINICIAL AND A.DTFINAL ");
			sql.appendSql("   AND A.ATIVA = :ATIVO");
			sql.appendSql("   AND A.DTINICIAL = (SELECT MAX(C.DTINICIAL)");
			sql.appendSql("                      FROM AD_CAMPQUANT C, AD_CAMPQUANTPROD D");
			sql.appendSql("                     WHERE C.CODEMP = A.CODEMP");
			sql.appendSql("                       AND D.CODPROD = B.CODPROD");
			sql.appendSql("                       AND C.CODCAMP = A.CODCAMP");
			sql.appendSql("                       AND :DATA BETWEEN C.DTINICIAL AND C.DTFINAL");
			sql.appendSql("                       AND C.ATIVA = :ATIVO)");  //SE ENCONTRAR MAIS DE UMA CAMPANHA NO PERÍODO VAI PEGAR A ÚLTIMA DATA INICIAL

			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				return rs.getBigDecimal("DESCONTO");
			} else {
				return BigDecimal.ZERO;
			}

		} finally {
			jdbc.closeSession();
		}

	}


	public BigDecimal buscarCampanhaGrupoEmpresa(BigDecimal linhaProd, BigDecimal codemp, Timestamp data) throws Exception {

		JdbcWrapper jdbc = null;

		try {

			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);

			sql.setNamedParameter("CODGRUPOPROD", linhaProd);
			sql.setNamedParameter("DATA", data);
			sql.setNamedParameter("ATIVO", "S");
			sql.setNamedParameter("CODEMP", codemp);
			

			sql.appendSql(" SELECT B.DESCONTO ");
			sql.appendSql(" FROM AD_CAMPQUANT A");
			sql.appendSql(" JOIN AD_CAMPQUANTCONFIG B ON A.CODCAMP = B.CODCAMP");
			sql.appendSql(" WHERE A.CODEMP = :CODEMP AND A.CODGRUPOPROD = :CODGRUPOPROD ");
			sql.appendSql("   AND :DATA BETWEEN A.DTINICIAL AND A.DTFINAL ");
			sql.appendSql("   AND A.ATIVA = :ATIVO");
			sql.appendSql("   AND A.DTINICIAL = (SELECT MAX(AA.DTINICIAL)");
			sql.appendSql("                      FROM AD_CAMPQUANT AA");
			sql.appendSql("   					WHERE AA.CODEMP = A.CODEMP AND AA.CODCAMP = A.CODCAMP");
			sql.appendSql("   				      AND AA.CODGRUPOPROD = A.CODGRUPOPROD");
			sql.appendSql("   				      AND :DATA BETWEEN AA.DTINICIAL AND AA.DTFINAL");			
			sql.appendSql("   				      AND AA.ATIVA = :ATIVO)");  //SE ENCONTRAR MAIS DE UMA CAMPANHA NO PERÍODO VAI PEGAR A ÚLTIMA DATA INICIAL
			sql.appendSql("   AND NOT EXISTS (SELECT PROD.CODCAMP");
			sql.appendSql("                      FROM AD_CAMPQUANTPROD PROD");
			sql.appendSql("                     WHERE PROD.CODCAMP = A.CODCAMP)"); //NÃO PODE EXISTIR NA ABA DE PRODUTOS

			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				return rs.getBigDecimal("DESCONTO");
			} else {
				return BigDecimal.ZERO;
			}

		} finally {
			jdbc.closeSession();
		}

	

	}

	public BigDecimal buscarCampanhaGrupoSEmpresa(BigDecimal linhaProd,  Timestamp data) throws Exception {

		JdbcWrapper jdbc = null;

		try {

			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);

			sql.setNamedParameter("CODGRUPOPROD", linhaProd);
			sql.setNamedParameter("DATA", data);
			sql.setNamedParameter("ATIVO", "S");

			sql.appendSql(" SELECT B.DESCONTO ");
			sql.appendSql(" FROM AD_CAMPQUANT A");
			sql.appendSql(" JOIN AD_CAMPQUANTCONFIG B ON A.CODCAMP = B.CODCAMP");
			sql.appendSql(" WHERE A.CODGRUPOPROD = :CODGRUPOPROD ");
			sql.appendSql("   AND :DATA BETWEEN A.DTINICIAL AND A.DTFINAL ");
			sql.appendSql("   AND A.ATIVA = :ATIVO");
			sql.appendSql("   AND A.DTINICIAL = (SELECT MAX(AA.DTINICIAL)");
			sql.appendSql("                      FROM AD_CAMPQUANT AA");
			sql.appendSql("   					WHERE AA.CODCAMP = A.CODCAMP");
			sql.appendSql("   				      AND AA.CODGRUPOPROD = A.CODGRUPOPROD");
			sql.appendSql("   				      AND :DATA BETWEEN AA.DTINICIAL AND AA.DTFINAL");			
			sql.appendSql("   				      AND AA.ATIVA = :ATIVO)");  //SE ENCONTRAR MAIS DE UMA CAMPANHA NO PERÍODO VAI PEGAR A ÚLTIMA DATA INICIAL
			sql.appendSql("   AND NOT EXISTS (SELECT PROD.CODCAMP");
			sql.appendSql("                      FROM AD_CAMPQUANTPROD PROD");
			sql.appendSql("                     WHERE PROD.CODCAMP = A.CODCAMP)"); //NÃO PODE EXISTIR NA ABA DE PRODUTOS

			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				return rs.getBigDecimal("DESCONTO");
			} else {
				return BigDecimal.ZERO;
			}

		} finally {
			jdbc.closeSession();
		}

	}
	public Boolean verificaProdutoCampanha(BigDecimal linhaProd, Timestamp data) throws Exception {

		JdbcWrapper jdbc = null;

		try {

			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);

			sql.setNamedParameter("CODGRUPOPROD", linhaProd);
			sql.setNamedParameter("DATA", data);
			sql.setNamedParameter("ATIVO", "S");

			sql.appendSql(" SELECT A.CODCAMP ");
			sql.appendSql(" FROM AD_CAMPQUANT A");
			sql.appendSql(" WHERE A.CODGRUPOPROD = :CODGRUPOPROD ");
			sql.appendSql("   AND :DATA BETWEEN A.DTINICIAL AND A.DTFINAL ");
			sql.appendSql("   AND A.ATIVA = :ATIVO");
			sql.appendSql("   AND A.DTINICIAL = (SELECT MAX(AA.DTINICIAL)");
			sql.appendSql("                      FROM AD_CAMPQUANT AA");
			sql.appendSql("   					WHERE AA.CODCAMP = A.CODCAMP");
			sql.appendSql("   				      AND :DATA BETWEEN AA.DTINICIAL AND AA.DTFINAL");			
			sql.appendSql("   				      AND AA.ATIVA = :ATIVO)");  //SE ENCONTRAR MAIS DE UMA CAMPANHA NO PERÍODO VAI PEGAR A ÚLTIMA DATA INICIAL
			sql.appendSql("   AND EXISTS (SELECT PROD.CODCAMP");
			sql.appendSql("                      FROM AD_CAMPQUANTPROD PROD");
			sql.appendSql("                     WHERE PROD.CODCAMP = A.CODCAMP)"); 

			ResultSet rs = sql.executeQuery();

			if (rs.next()) {
				return true;
			} else {
				return false;
			}

		} finally {
			jdbc.closeSession();
		}


	}

}
