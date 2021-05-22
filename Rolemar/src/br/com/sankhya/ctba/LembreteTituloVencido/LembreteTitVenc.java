package br.com.sankhya.ctba.LembreteTituloVencido;

import java.math.BigDecimal;



import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.dwfdata.vo.tmd.MSDFilaMensagemVO;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class LembreteTitVenc implements ScheduledAction {


	@Override
	public void onTime(ScheduledActionContext contexto) {
		String preferencia = null;

		try {
			preferencia = (String) MGECoreParameter.getParameter("AVISOTITVENC");
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		if (preferencia != null && preferencia.equals("S")) {

			ArrayList<TituloVencido> tituloVencido;
			tituloVencido = new ArrayList<TituloVencido>();

			try {
				JdbcWrapper jdbc = null;

				jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

				NativeSql sql = new NativeSql(jdbc);

				sql.appendSql(" SELECT FIN.CODPARC, ");
				sql.appendSql("        PAR.NOMEPARC, ");
				sql.appendSql("        CGC_CPF, ");
				sql.appendSql("        CASE WHEN  LENGTH(CGC_CPF) =14 THEN regexp_replace(LPAD(CGC_CPF, 14),'([0-9]{2})([0-9]{3})([0-9]{3})([0-9]{4})','\1.\2.\3/\4-') ELSE regexp_replace(LPAD(CGC_CPF, 11),'([0-9]{3})([0-9]{3})([0-9]{3})','\1.\2.\3-') END AS CGC_CPF_MASK, ");
				sql.appendSql("        CAB.NUMNOTA, ");
				sql.appendSql("        CAB.DTNEG, ");
				sql.appendSql("        FIN.SEQUENCIA, ");
				sql.appendSql("        FIN.DTVENC, ");
				sql.appendSql("        to_char(FIN.VLRDESDOB, 'FM999G999G999D90') as VLRDESDOB, ");
				sql.appendSql("        EMP.NOMEFANTASIA, ");
				sql.appendSql("        EMP.TELEFONE, ");
				sql.appendSql("        EMP.EMAIL AS EMAILEMPR, ");				
				sql.appendSql("        LISTAGG (TRIM(CTT.EMAIL), ', ') WITHIN GROUP (ORDER BY FIN.CODPARC) AS EMAIL");
				sql.appendSql("   FROM TGFCAB CAB");
				sql.appendSql("  INNER JOIN TGFPAR PAR ON  PAR.CODPARC = CAB.CODPARC ");
				sql.appendSql("   JOIN TSIEMP EMP ON  CAB.CODEMP = EMP.CODEMP ");
				sql.appendSql("  INNER JOIN TGFCTT CTT ON  CTT.CODPARC = PAR.CODPARC  AND CTT.ATIVO = 'S' AND CTT.EMAIL IS NOT NULL");
				sql.appendSql("  INNER JOIN TGFFIN FIN ON  FIN.NUNOTA = CAB.NUNOTA ");
				sql.appendSql("  WHERE CAB.TIPMOV='P' ");
				sql.appendSql("    AND FIN.RECDESP=1 ");
				sql.appendSql(" AND FIN.DTVENC <= SYSDATE -700 " ); 	
				//sql.appendSql(" AND FIN.DTVENC <= SYSDATE -650 " ); 	 //para testes

				sql.appendSql("    AND FIN.DHBAIXA IS NULL ");
				sql.appendSql(" GROUP BY FIN.CODPARC, ");
				sql.appendSql("          PAR.NOMEPARC, ");
				sql.appendSql("          CGC_CPF,  ");
				sql.appendSql("          CAB.NUMNOTA, ");
				sql.appendSql("          CAB.DTNEG, ");
				sql.appendSql("          FIN.SEQUENCIA, ");
				sql.appendSql("          FIN.DTVENC, ");
				sql.appendSql("          FIN.VLRDESDOB,");
				sql.appendSql("          EMP.NOMEFANTASIA, ");
				sql.appendSql("          EMP.TELEFONE, ");
				sql.appendSql("          EMP.EMAIL");

				sql.appendSql(" ORDER BY CODPARC, DTVENC ASC");

				ResultSet rs = sql.executeQuery();

				while (rs.next()) {
					TituloVencido t = new TituloVencido();

					t.setCodparc(rs.getBigDecimal("CODPARC"));
					t.setNomeparc(rs.getString("NOMEPARC"));
					t.setCgc_cpf(rs.getString("CGC_CPF"));
					t.setCgc_cpf_mask(rs.getString("CGC_CPF_MASK"));
					t.setNumnota(rs.getBigDecimal("NUMNOTA"));
					t.setDtneg(rs.getTimestamp("DTNEG"));
					t.setSequencia(rs.getBigDecimal("SEQUENCIA"));
					t.setDtvenc(rs.getTimestamp("DTVENC"));
					t.setVlrdesdo(rs.getString("VLRDESDOB"));
					t.setEmail(rs.getString("EMAIL"));
					t.setNomeFantasiaEmpr(rs.getString("NOMEFANTASIA"));
					t.setFoneEmpr(rs.getString("TELEFONE"));
					t.setEmailEmpr(rs.getString("EMAILEMPR"));
					tituloVencido.add(t);
				}

			} catch (

					Exception e) {

				e.printStackTrace();
			}

			try {
				montarLayoutEmail(tituloVencido);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("null")
	public static void montarLayoutEmail(ArrayList<TituloVencido> itens) throws Exception {

		String tituloEmail;
		BigDecimal codParcAnterior = null;
		String cpfAnterior = null;
		String NomeparcAnterior = null;
		String email = null;
		String caminhoImagem = null;
		StringBuffer dadosEmpresa = null;

		tituloEmail = "Aviso de Título Vencido - Girando Comércio de Peças - ROLEMAR ";

		String linha2 = "<div width:100%><br />Este comunicado tem a finalidade de levar ao conhecimento de V.Sa. que constatamos em nossos registros os <br />";
		String linha3 = " seguintes débitos vencidos, ou seja, sem a comprovação de pagamento: <br /><br /></div>";


		StringBuffer rodape = null;
		rodape = new StringBuffer();

		rodape.append("</table></div>");
		rodape.append("<div style=\"width: 100%; margin-top: 60px; \">");
		rodape.append("Caso não reconheça ou discorde da referida pendência, favor entrar em contato conosco o mais breve possível. Se é de vosso<br />");
		rodape.append(" conhecimento, favor providenciar com urgência a quitação, evitando com isso um transtorno com protesto pela Instituição Financeira. <br /><br /> ");
		rodape.append(" Você pode acessar a segunda via do boleto em nosso site www.rolemar.com.  <br /><br /><br/></div>");

		StringBuffer imagem = null;
		imagem = new StringBuffer();
				
        caminhoImagem = getCaminho("rol_imgassmail");
		imagem.append("<div><img src="+caminhoImagem+"></div>");


		StringBuffer mensagem = null;
		mensagem = new StringBuffer();
		mensagem.append("<div style=\"width: 100%\">");
		mensagem.append("<table align=left border=1 width=100%>");
		mensagem.append("<tr align=center>");
		mensagem.append("<td width=\"80\">Data da Emissão</td> ");
		mensagem.append("<td width=\"80\">Vencimento</td> ");
		mensagem.append("<td width=\"80\">Título</td> ");
		mensagem.append("<td width=\"80\">Parcela</td> ");
		mensagem.append("<td width=\"80\">Valor<br></td></tr>");		

		Iterator<TituloVencido> Item = itens.iterator();


		while (Item.hasNext()) {


			dadosEmpresa = new StringBuffer();
			TituloVencido t = Item.next();


			dadosEmpresa.append("<div width:100%><font size=4><b>"+ t.nomeFantasiaEmpr + "</b></font><br />");
			dadosEmpresa.append("<font size=3>"+"Fone: "+t.foneEmpr + "<br /></font>");
			dadosEmpresa.append("<font size=3>"+"E-mail: "+t.emailEmpr + "<br /></font></div>");


			if (codParcAnterior != null && t.getCodparc().compareTo(codParcAnterior) != 0) {

				String linha1 = "À " + NomeparcAnterior + " <br />" + "CNPJ: " + t.getCgc_cpf_mask() + "<br />";
				String conteudo =  "<div> " + linha1 + linha2 + linha3 + mensagem + rodape + dadosEmpresa + imagem + " </div>";

				dispararEmail(t.getEmail(), tituloEmail, conteudo);
				conteudo = "";
				linha1 = "";
				codParcAnterior = t.getCodparc();
				NomeparcAnterior = t.getNomeparc();
				cpfAnterior = t.getCgc_cpf();
				mensagem = new StringBuffer();
				mensagem.append("<div style=\"width: 100%\">");
				mensagem.append("<table align=left border=1 width=100%>");
				mensagem.append("<tr align=center> ");
				mensagem.append("<td width=\"80\">Data da Emissão</td> ");
				mensagem.append("<td width=\"80\">Vencimento</td> ");
				mensagem.append("<td width=\"80\">Título</td> ");
				mensagem.append("<td width=\"80\">Parcela</td> ");
				mensagem.append("<td width=\"80\">Valor<br></td></tr>");


			} else {
				codParcAnterior = t.getCodparc();
				NomeparcAnterior = t.getNomeparc();
				cpfAnterior = t.getCgc_cpf();
			}

			mensagem.append(" <tr align=center> ");
			mensagem.append("<td>" + datas(t.getDtneg())  + "</td>" 
					+ "<td>" + datas(t.getDtvenc())  + "</td>"
					+ "<td>" + t.getNumnota()  + "</td>"
					+ "<td>" + t.getSequencia()  + "</td>"
					+ "<td>" + t.getVlrdesdo()  + "</td></tr>");


			email = t.getEmail();
		}

		if (codParcAnterior != null) {

			String linha1 = "À " + NomeparcAnterior + " <br />" + "CNPJ: " + cpfAnterior + "<br />";
			String conteudo =  "<div> " + linha1 + linha2 + linha3 +mensagem +  rodape + dadosEmpresa + imagem + " </div>";

			dispararEmail(email, tituloEmail, conteudo);
			conteudo = "";
			linha1 = "";
		}
	}
	
		
	static String getCaminho(String vChave) throws Exception {
		JdbcWrapper jdbc = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("CHAVE", vChave);
			
			sql.appendSql(" SELECT TEXTO" );
			sql.appendSql(" FROM TSIPAR" );
			sql.appendSql(" WHERE CHAVE = :CHAVE" );
			
			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				return rs.getString("TEXTO");
			}
			else {
				return null;
			}
			
		}finally {
			jdbc.closeSession();
		}
	}
	
	

	public static void dispararEmail(String emailParceiro, String tituloEmail, String conteudo) throws Exception {

		// Busca a tabela a ser inserida, com base na inst�ncia
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		// insere na fila

		MSDFilaMensagemVO filaVO = (MSDFilaMensagemVO) dwfEntityFacade
				.getDefaultValueObjectInstance(DynamicEntityNames.FILA_MSG, MSDFilaMensagemVO.class);

		filaVO.setCODCON(BigDecimal.ZERO);
		filaVO.setCODMSG(null);
		filaVO.setSTATUS("Pendente");
		filaVO.setTIPOENVIO("E");
		filaVO.setMAXTENTENVIO(new BigDecimal(3));
		filaVO.setMENSAGEM(new String(conteudo.toString().getBytes("ISO-8859-1"), "ISO-8859-1").toCharArray());
		filaVO.setASSUNTO(tituloEmail);		
		//filaVO.setEMAIL(emailParceiro);
		//filaVO.setEMAIL("ariel.fibbiani@sankhya.com.br");
		//filaVO.setEMAIL("financ.londrina@rolemar.com");
		filaVO.setEMAIL("luis.galhassi@sankhya.com.br");

		dwfEntityFacade.createEntity(DynamicEntityNames.FILA_MSG, filaVO);

	}

	public static String datas(Timestamp valor) throws Exception {

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY");

		if (valor != null) {

			return dateFormat.format(valor);
		} else {
			return "";
		}
	}

}
