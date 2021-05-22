package br.com.sankhya.ctba.LembreteTituloVencido;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.text.MaskFormatter;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.dwfdata.vo.tmd.MSDFilaMensagemVO;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class botaoTitVencido  implements  AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {

		String preferencia = null;

		try {
			preferencia = (String) MGECoreParameter.getParameter("AVISOTITVENC");
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		if (preferencia != null && preferencia.equals("S")) {

			ArrayList<TituloVencido> tituloVencido;
			tituloVencido = new ArrayList<TituloVencido>();

			
				JdbcWrapper jdbc = null;

				jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

				NativeSql sql = new NativeSql(jdbc);


				sql.appendSql(" SELECT FIN.CODPARC, " );
				sql.appendSql("       PAR.RAZAOSOCIAL, " );
				sql.appendSql("       PAR.CGC_CPF," );
				sql.appendSql("       CASE WHEN  LENGTH(CGC_CPF) =14 THEN REGEXP_REPLACE(LPAD(CGC_CPF, 14),'([0-9]{2})([0-9]{3})([0-9]{3})([0-9]{4})','\1.\2.\3/\4-') ELSE" );
				sql.appendSql("                              REGEXP_REPLACE(LPAD(CGC_CPF, 11),'([0-9]{3})([0-9]{3})([0-9]{3})','\1.\2.\3-') END AS CGC_CPF_MASK," );
				sql.appendSql("       CAB.NUMNOTA, " );
				sql.appendSql("       CAB.DTNEG, " );
				sql.appendSql("       FIN.SEQUENCIA, " );
				sql.appendSql("       FIN.DTVENC, " );
				sql.appendSql("       TO_CHAR(FIN.VLRDESDOB, 'FM999G999G999D90') AS VLRDESDOB," );
				sql.appendSql("       LISTAGG (TRIM(CTT.EMAIL), ', ') WITHIN GROUP (ORDER BY FIN.CODPARC) AS EMAIL" );
				sql.appendSql("  FROM TGFCAB CAB" );
				sql.appendSql(" INNER JOIN TGFPAR PAR ON  PAR.CODPARC = CAB.CODPARC " );
				sql.appendSql(" INNER JOIN TGFCTT CTT ON  CTT.CODPARC = PAR.CODPARC  AND CTT.ATIVO = 'S' AND CTT.EMAIL IS NOT NULL" );
				sql.appendSql(" INNER JOIN TGFFIN FIN ON  FIN.NUNOTA = CAB.NUNOTA " );
				sql.appendSql(" WHERE CAB.TIPMOV='V' " );
				sql.appendSql("   AND FIN.RECDESP=1 " );
				sql.appendSql("   AND FIN.DTVENC <= SYSDATE -5 " ); 
				//sql.appendSql("   AND FIN.DTVENC BETWEEN SYSDATE -4 AND SYSDATE  " );
				sql.appendSql("   AND FIN.DHBAIXA IS NULL " );
				sql.appendSql(" GROUP BY FIN.CODPARC, " );
				sql.appendSql("          PAR.RAZAOSOCIAL, " );
				sql.appendSql("          CGC_CPF, " );
				sql.appendSql("          CAB.NUMNOTA, " );
				sql.appendSql("          CAB.DTNEG, " );
				sql.appendSql("          FIN.SEQUENCIA, " );
				sql.appendSql("          FIN.DTVENC, " );
				sql.appendSql("          FIN.VLRDESDOB" );
				sql.appendSql(" ORDER BY CODPARC, DTVENC ASC" );

				

				ResultSet rs = sql.executeQuery();

				while (rs.next()) {
					TituloVencido t = new TituloVencido();

					t.setCodparc(rs.getBigDecimal("CODPARC"));
					t.setNomeparc(rs.getString("RAZAOSOCIAL"));
					t.setCgc_cpf(rs.getString("CGC_CPF"));
					t.setCgc_cpf_mask(converteMascaraCNPJ(rs.getString("CGC_CPF")));
					
				//	if(1>0)throw new Exception("mensagem2: " + rs.getString("CGC_CPF_MASK"));
					
					t.setNumnota(rs.getBigDecimal("NUMNOTA"));
					t.setDtneg(rs.getTimestamp("DTNEG"));
					t.setSequencia(rs.getBigDecimal("SEQUENCIA"));
					t.setDtvenc(rs.getTimestamp("DTVENC"));
					t.setVlrdesdo(rs.getString("VLRDESDOB"));
					t.setEmail(rs.getString("EMAIL"));
					
					tituloVencido.add(t);
				}

		
			//if(1>0)throw new Exception("email: ");
//			try {
				montarLayoutEmail(tituloVencido);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
		}
	}

	public static void montarLayoutEmail(ArrayList<TituloVencido> itens) throws Exception {

		String tituloEmail;
		BigDecimal codParcAnterior = null;
		String cpfAnterior = null;
		String NomeparcAnterior = null;
		String email = null;

		tituloEmail = "Aviso de Título Vencido - Girando Comércio de Peças - ROLEMAR ";

		String linha2 = "<br>Este comunicado tem a finalidade de levar ao conhecimento de V.Sa. que constatamos em <br>";
		String linha3 = "nossos registros os seguintes débitos vencidos, ou seja, sem a comprovação de pagamento:  <br> <br>";
		String linha5 = "<br>Caso não reconheça ou discorde da referida pendência, favor entrar em contato conosco o <br>";
		String linha6 = "mais breve possível. Se é de vosso conhecimento, favor providenciar com urgência a quitação,  <br>";
		String linha7 = "evitando com isso um transtorno com protesto pela Instituição Financeira.  <br><br>";
		String linha8 = "Você pode acessar a segunda via do boleto em nosso site www.rolemar.com.  <br>";

		StringBuffer mensagem = null;
		mensagem = new StringBuffer();
		mensagem.append("<table border=2>");
		mensagem.append(" <tr align=center border=3> ");
		mensagem.append("<td width=\"150\"><b> Data da Emissão </b> </td> ");
		mensagem.append("<td width=\"150\"><b> Vencimento </b> </td> ");
		mensagem.append("<td width=\"150\"><b> Título </b> </td> ");
		mensagem.append("<td width=\"150\"><b> Parcela </b> </td> ");
		mensagem.append("<td width=\"150\"><b> Valor </b> </td> ");
		mensagem.append(" </tr> ");

		Iterator<TituloVencido> Item = itens.iterator();

		while (Item.hasNext()) {
			TituloVencido t = Item.next();

			if (codParcAnterior != null && t.getCodparc().compareTo(codParcAnterior) != 0) {
				mensagem.append("</table>");

				//cpfAnterior = converteMascaraCNPJ(cpfAnterior);

				String linha1 = "À  <br>" + NomeparcAnterior + " <br>" + "CNPJ: " + t.getCgc_cpf_mask() + " <br>";
				String conteudo = linha1 + linha2 + linha3 + mensagem + linha5 + linha6 + linha7 + linha8 + " <br>" +  "<img src=\"&img-embutida:assinatura_email.JPG\" alt=\"Sankhya\" />";
						

	

				dispararEmail(t.getEmail(), tituloEmail, conteudo);
				conteudo = "";
				linha1 = "";

				codParcAnterior = t.getCodparc();
				NomeparcAnterior = t.getNomeparc();
				cpfAnterior = t.getCgc_cpf();

				mensagem = new StringBuffer();
				mensagem.append("<table border=2>");
				mensagem.append(" <tr align=center border=2> ");
				mensagem.append("<td width=\"150\"><b> Data da Emissão </b></td> ");
				mensagem.append("<td width=\"150\"><b> Vencimento </b></td> ");
				mensagem.append("<td width=\"150\"><b> Título </b></td> ");
				mensagem.append("<td width=\"150\"><b> Parcela </b></td> ");
				mensagem.append("<td width=\"150\"><b> Valor </b></td> ");
				mensagem.append(" </tr> ");

			} else {
				codParcAnterior = t.getCodparc();
				NomeparcAnterior = t.getNomeparc();
				cpfAnterior = t.getCgc_cpf();
			}

			mensagem.append(" <tr align=center> ");
			mensagem.append("<td>" + datas(t.getDtneg()) + "</td>" + "<td>" + datas(t.getDtvenc()) + "</td>" + "<td>"
					+ t.getNumnota() + "</td>" + "<td>" + t.getSequencia() + "</td>" + "<td>" + t.getVlrdesdo()
					+ "</td>");

			mensagem.append(" </tr> ");

			email = t.getEmail();
		}

		if (codParcAnterior != null) {

			
		//	cpfAnterior = converteMascaraCNPJ(cpfAnterior);
			mensagem.append("</table>");
			
			String linha1 = "À <br>" + NomeparcAnterior + " <br>" + "CNPJ: " + cpfAnterior + " <br>";
			String conteudo = linha1 + linha2 + linha3 + mensagem + linha5 + linha6 + linha7 + linha8 + "<br>" +   "<img src=\"&img-embutida:assinatura_email.JPG\" alt=\"Sankhya\" />";

	//		 if(1>0)throw new Exception("mensagem2: " + mensagem);
			dispararEmail(email, tituloEmail, conteudo);
			conteudo = "";
			linha1 = "";
		}
	}

	private static String converteMascaraCNPJ(String cpfAnterior) throws Exception {
		MaskFormatter mask;
		if (cpfAnterior.length() == 14) {
			mask = new MaskFormatter("##.###.###/####-##");

		} else {
			mask = new MaskFormatter("##.###.###-#");
		}
		mask.setValueContainsLiteralCharacters(false);
		return mask.valueToString(cpfAnterior);
	}

	public static void dispararEmail(String emailParceiro, String tituloEmail, String conteudo) throws Exception {

		// Busca a tabela a ser inserida, com base na instância
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
		// filaVO.setEMAIL(emailParceiro);
		filaVO.setEMAIL("joao.pugsley@sankhya.com.br");
		//filaVO.setEMAIL("elgson.pereira@sankhya.com.br");

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
