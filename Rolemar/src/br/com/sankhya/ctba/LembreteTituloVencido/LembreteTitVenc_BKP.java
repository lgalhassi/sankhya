package br.com.sankhya.ctba.LembreteTituloVencido;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.text.MaskFormatter;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.ctba.enviarEmailReneg.Renegociacao;
import br.com.sankhya.ctba.enviarEmailReneg.Renegociado;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.dwfdata.vo.tmd.MSDFilaMensagemVO;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class LembreteTitVenc_BKP implements ScheduledAction {

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

				sql.appendSql(" SELECT * FROM( ");
				sql.appendSql(" SELECT FIN.CODPARC, ");
				sql.appendSql("        PAR.NOMEPARC, ");
				sql.appendSql("        CGC_CPF, ");
				sql.appendSql("        CAB.NUMNOTA, ");
				sql.appendSql("        CAB.DTNEG, ");
				sql.appendSql("        FIN.SEQUENCIA, ");
				sql.appendSql("        FIN.DTVENC, ");
				sql.appendSql("        to_char(FIN.VLRDESDOB, 'FM999G999G999D90') as VLRDESDOB, ");
				sql.appendSql("        CTT.EMAIL");
				sql.appendSql("   FROM TGFCAB CAB");
				sql.appendSql("  INNER JOIN TGFPAR PAR ON  PAR.CODPARC = CAB.CODPARC ");
				sql.appendSql(
						"  INNER JOIN TGFCTT CTT ON  CTT.CODPARC = PAR.CODPARC  AND CTT.ATIVO = 'S' AND CTT.EMAIL IS NOT NULL");
				sql.appendSql("  INNER JOIN TGFFIN FIN ON  FIN.NUNOTA = CAB.NUNOTA ");
				sql.appendSql("  WHERE CAB.TIPMOV='P' ");
				sql.appendSql("    AND FIN.RECDESP=1 ");
				// sql.appendSql(" AND FIN.DTVENC <= SYSDATE -5 " );
				sql.appendSql("    AND FIN.DTVENC between sysdate -4 and sysdate ");
				sql.appendSql("    AND FIN.DHBAIXA IS NULL ");
				sql.appendSql("  ORDER BY FIN.DTVENC ASC  ");
				sql.appendSql(" ) ORDER BY  CODPARC ASC ");

				ResultSet rs = sql.executeQuery();

				while (rs.next()) {
					TituloVencido t = new TituloVencido();

					t.setCodparc(rs.getBigDecimal("CODPARC"));
					t.setNomeparc(rs.getString("NOMEPARC"));
					t.setCgc_cpf(rs.getString("CGC_CPF"));
					t.setNumnota(rs.getBigDecimal("NUMNOTA"));
					t.setDtneg(rs.getTimestamp("DTNEG"));
					t.setSequencia(rs.getBigDecimal("SEQUENCIA"));
					t.setDtvenc(rs.getTimestamp("DTVENC"));
					t.setVlrdesdo(rs.getString("VLRDESDOB"));
					t.setEmail(rs.getString("EMAIL"));

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

	public static void montarLayoutEmail(ArrayList<TituloVencido> itens) throws Exception {

		String tituloEmail;
		BigDecimal codParcAnterior = null;
		String cpfAnterior = null;
		String NomeparcAnterior = null;
		String email = null;

		String linha2 = "<br>Este comunicado tem a finalidade de levar ao conhecimento de V.Sa. que constatamos em <br>";
		String linha3 = "nossos registros os seguintes débitos vencidos, ou seja, sem a comprovação de pagamento:  <br> <br>";
		String linha5 = "<br>Caso não reconheça ou discorde do(s) referido(s) título(s), favor entrar em contato conosco o  <br>";
		String linha6 = "mais breve possível.  <br>";
		String linha7 = "Você pode acessar a segunda via do boleto em nosso site www.rolemar.com.  <br>";

		tituloEmail = "Aviso de Título Vencido - Girando Comércio de Peças - ROLEMAR ";

		StringBuffer mensagem = null;
		mensagem = new StringBuffer();

		Iterator<TituloVencido> Item = itens.iterator();

		while (Item.hasNext()) {
			TituloVencido t = Item.next();

			if (codParcAnterior != null && t.getCodparc().compareTo(codParcAnterior) != 0) {

				if (cpfAnterior.length() == 14) {
					MaskFormatter mask = new MaskFormatter("##.###.###/####-##");
					mask.setValueContainsLiteralCharacters(false);
					cpfAnterior = mask.valueToString(cpfAnterior);

				} else {
					MaskFormatter mask = new MaskFormatter("##.###.###-#");
					mask.setValueContainsLiteralCharacters(false);
					cpfAnterior = mask.valueToString(cpfAnterior);

				}

				String linha1 = "À " + NomeparcAnterior + " <br>" + "CNPJ: " + cpfAnterior + " <br>";
				String conteudo = linha1 + linha2 + linha3 + mensagem + linha5 + linha6 + linha7 + "<br> email:"
						+ t.getEmail();

				dispararEmail(t.getEmail(), tituloEmail, conteudo);
				conteudo = "";
				linha1 = "";
				codParcAnterior = t.getCodparc();
				NomeparcAnterior = t.getNomeparc();
				cpfAnterior = t.getCgc_cpf();
				mensagem = new StringBuffer();

			} else {
				codParcAnterior = t.getCodparc();
				NomeparcAnterior = t.getNomeparc();
				cpfAnterior = t.getCgc_cpf();
			}

			mensagem.append("Data da Emissão: " + datas(t.getDtneg()) + " Vencimento: " + datas(t.getDtvenc())
					+ " Título: " + t.getNumnota() + " Parcela: " + t.getSequencia() + " Valor: R$ " + t.getVlrdesdo()
					+ "<br>");

			email = t.getEmail();
		}

		if (codParcAnterior != null) {

			String linha1 = "À " + NomeparcAnterior + " <br>" + "CNPJ: " + cpfAnterior + " <br>";
			String conteudo = linha1 + linha2 + linha3 + mensagem + linha5 + linha6 + linha7 + "<br>";

			dispararEmail(email, tituloEmail, conteudo);
			conteudo = "";
			linha1 = "";
		}
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
		filaVO.setEMAIL("elgson.pereira@sankhya.com.br");

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
