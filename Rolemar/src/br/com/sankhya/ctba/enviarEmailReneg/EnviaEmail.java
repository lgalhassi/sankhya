package br.com.sankhya.ctba.enviarEmailReneg;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import br.com.sankhya.ctba.campanhaCartao.Util;
//import br.com.sankhya.ctba.listaReg.Registros;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.dwfdata.vo.tmd.MSDFilaMensagemVO;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class EnviaEmail implements EventoProgramavelJava {

	public interface registros {

	}

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {

	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		// Verificar colunas que estão sendo alteradas dentro do evento de update

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

//		if (1 > 0) {
//		throw new Exception("JGP 1: codparc ");
//	}

		DynamicVO tabVO = (DynamicVO) event.getVo();

		BigDecimal Ren_NURENEG = tabVO.asBigDecimal("NURENEG");
		BigDecimal Ren_NUFIN = tabVO.asBigDecimal("NUFIN");

		TituloOriginal(event, Ren_NURENEG, Ren_NUFIN);

	}

	@Override
	public void afterUpdate(PersistenceEvent event) throws Exception {

	}

	@SuppressWarnings({ "unused" })
	public void TituloOriginal(PersistenceEvent evento, BigDecimal Nureneg, BigDecimal Nufin) throws Exception {
	
		ArrayList<Renegociacao> itens; 
		ArrayList<Renegociado> itemRenegociado;
		
		StringBuffer linhas = new StringBuffer();
		StringBuffer linhasRenegociado = new StringBuffer();

		BigDecimal TotalIte = new BigDecimal(0.00);
		BigDecimal ValorLiquido = new BigDecimal(0.00);
		String RazaoSoc = "";
		String email = "";
		JdbcWrapper jdbc = null;
		String nureneg = Nureneg.toPlainString();
		String nufin = Nufin.toPlainString();
		String[] usuario = new String[2];
		String nomeUsuario; 
				
		// **********************************
		// * TITULO RENEGOCIACAO (original)
		// *********************************

		try {

			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NURENEG", nureneg);
			

			sql.appendSql(" SELECT");
			sql.appendSql(" NVL((SELECT ABS(NURENEGORIG) FROM TGFREN REN WHERE REN.NUFIN = FIN.NUFIN), 0) AS NURENEGANT, " );
			sql.appendSql(" FIN.NURENEG ," );
			sql.appendSql(" FIN.NUMNOTA ," );
			sql.appendSql(" FIN.DESDOBRAMENTO , " );
			sql.appendSql(" FIN.DTNEG ," );
			sql.appendSql(" FIN.CODEMP , " );
			sql.appendSql(" DTVENC," );
			sql.appendSql(" FIN.VLRDESDOB ," );
			sql.appendSql(" FIN.VLRJURO ," );
			sql.appendSql(" FIN.VLRMULTA , " );
			sql.appendSql(" FIN.VLRDESC ," );
			sql.appendSql(" NVL(FIN.HISTORICO , ' ') AS HISTORICO," );
			sql.appendSql(" FIN.TIPJURO , " );
			sql.appendSql(" FIN.NUFIN ," );
			sql.appendSql(" FIN.CODPARC , " );
			sql.appendSql(" PAR.RAZAOSOCIAL  " );
			sql.appendSql(" FROM TGFFIN FIN ");
			sql.appendSql(" LEFT JOIN TGFPAR PAR ON(FIN.CODPARC = PAR.CODPARC)" );
			sql.appendSql(" WHERE FIN.NURENEG = :NURENEG" );
			sql.appendSql("   AND FIN.RECDESP = 0" );

			ResultSet rs = sql.executeQuery();

			itens = new ArrayList<Renegociacao>();
			Renegociacao r1 = new Renegociacao();
			
			while (rs.next()) {
				r1.setFin_NuRenegAnt(rs.getBigDecimal("NURENEGANT"));
				r1.setFin_NuRenegAtual(rs.getBigDecimal("NURENEG"));
				r1.setFin_Numnota(rs.getBigDecimal("NUMNOTA"));
				r1.setFin_Db(rs.getString("DESDOBRAMENTO"));
			    r1.setFin_Dtneg(rs.getTimestamp("DTNEG"));
			    r1.setFin_Empresa(rs.getString("CODEMP"));
			    r1.setFin_Dtvenc(rs.getTimestamp("DTVENC"));
			    r1.setFin_VlrDesdbo(rs.getBigDecimal("VLRDESDOB"));
			    r1.setFin_VlrJuro(rs.getBigDecimal("VLRJURO"));
			    r1.setFin_VlrMulta(rs.getBigDecimal("VLRMULTA"));
			    r1.setFin_VlrDesconto(rs.getBigDecimal("VLRDESC"));
			    r1.setFin_Historico(rs.getString("HISTORICO"));
                r1.setFin_Tipo(rs.getBigDecimal("TIPJURO"));
                r1.setFin_NumUnico(rs.getBigDecimal("NUFIN"));
                r1.setFin_CodParc(rs.getBigDecimal("CODPARC"));
                r1.setFin_NomeParc(rs.getString("RAZAOSOCIAL"));
                
				itens.add(r1);
				linhas.append(r1);
				
				
				
			}
		} finally {
			jdbc.closeSession();
		}

		//**********************************
		//* TITULO RENEGOCIACO
		//**********************************
		try {

			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

			NativeSql sql = new NativeSql(jdbc);
			
			sql.setNamedParameter("NURENEG", nureneg);
			
			sql.appendSql(" SELECT" );
			sql.appendSql("   FIN.NURENEG" );
			sql.appendSql(" , FIN.NUMNOTA" );
			sql.appendSql(" , FIN.NUFIN" );
			sql.appendSql(" , FIN.DESDOBRAMENTO" );
			sql.appendSql(" , FIN.DTNEG" );
			sql.appendSql(" , FIN.CODEMP" );
			sql.appendSql(" , FIN.DTVENC" );
			sql.appendSql(" , FIN.VLRDESDOB" );
			sql.appendSql(" , FIN.VLRJURONEGOC" );
			sql.appendSql(" , FIN.VLRMULTANEGOC" );
			sql.appendSql(" , FIN.VLRDESC" );
			sql.appendSql(" , FIN.VLRMULTA" );
			sql.appendSql(" , FIN.VLRJURO" );
			sql.appendSql(" ,NVL( FIN.HISTORICO, ' ') AS HISTORICO" );
			sql.appendSql(" FROM TGFFIN FIN" );
			sql.appendSql(" WHERE " );
			sql.appendSql("    (FIN.NURENEG IN (:NURENEG) AND FIN.RECDESP <> 0)" );
			sql.appendSql("    UNION ALL" );
			sql.appendSql(" SELECT" );
			sql.appendSql("   FIN.NURENEG" );
			sql.appendSql(" , FIN.NUMNOTA" );
			sql.appendSql(" , FIN.NUFIN" );
			sql.appendSql(" , FIN.DESDOBRAMENTO" );
			sql.appendSql(" , FIN.DTNEG" );
			sql.appendSql(" , FIN.CODEMP" );
			sql.appendSql(" , FIN.DTVENC" );
			sql.appendSql(" , FIN.VLRDESDOB" );
			sql.appendSql(" , FIN.VLRJURONEGOC" );
			sql.appendSql(" , FIN.VLRMULTANEGOC" );
			sql.appendSql(" , FIN.VLRDESC" );
			sql.appendSql(" , FIN.VLRMULTA" );
			sql.appendSql(" , FIN.VLRJURO" );
			sql.appendSql(" , NVL(FIN.HISTORICO, ' ') AS HISTORICO" );
			sql.appendSql(" FROM TGFFIN FIN" );
			sql.appendSql(" WHERE " );
			sql.appendSql("    FIN.NUFIN IN (SELECT REN.NUFIN FROM TGFREN REN WHERE REN.NURENEGORIG IN(:NURENEG))" );
			
			ResultSet rs = sql.executeQuery();

			itemRenegociado = new ArrayList<Renegociado>();
	       
			while (rs.next()) {
				Renegociado r2 = new Renegociado();
				r2.setRng_NuRenegPost(rs.getString("NURENEG"));
				r2.setRng_Numnota(rs.getString("NUMNOTA"));
				r2.setRng_NumUnico(rs.getString("NUFIN"));
				r2.setRng_Db(rs.getString("DESDOBRAMENTO"));
				r2.setRng_Dtneg(rs.getTimestamp("DTNEG"));
				r2.setRng_Empresa(rs.getString("CODEMP"));
				r2.setRng_Dtvenc(rs.getTimestamp("DTVENC"));
				r2.setRng_VlrDesdbo(rs.getBigDecimal("VLRDESDOB"));
				r2.setRng_VlrJuro(rs.getBigDecimal("VLRJURO"));
				r2.setRng_VlrMulta(rs.getBigDecimal("VLRMULTA"));
				r2.setRng_VlrJuroNegociado(rs.getBigDecimal("VLRJURONEGOC"));
				r2.setRng_VlrMultaNegociado(rs.getBigDecimal("VLRMULTANEGOC"));
				r2.setRng_VlrDesconto(rs.getBigDecimal("VLRDESC"));
				ValorLiquido = ValorLiquido.add(rs.getBigDecimal("VLRDESDOB"));	
				ValorLiquido = ValorLiquido.add(rs.getBigDecimal("VLRMULTA"));
				ValorLiquido = ValorLiquido.add(rs.getBigDecimal("VLRJURO"));
				ValorLiquido = ValorLiquido.add(rs.getBigDecimal("VLRMULTANEGOC"));
				ValorLiquido = ValorLiquido.add(rs.getBigDecimal("VLRJURONEGOC"));
				ValorLiquido = ValorLiquido.subtract(rs.getBigDecimal("VLRDESC"));
				r2.setRng_VlrLiquido(ValorLiquido);  
				r2.setRng_Historico(rs.getString("HISTORICO"));
				
				
				itemRenegociado.add(r2);
				linhasRenegociado.append(r2);
				
				ValorLiquido = BigDecimal.ZERO;
//****TESTE***
//				if (i > 0) {
//					
//					//throw new Exception("JGP 1: Db " + rs.getString("DESDOBRAMENTO")) ;
//					Renegociado teste = itemRenegociado.get(0);
//					Renegociado teste1 = itemRenegociado.get(1);
//
//					throw new Exception("JGP 1: Db " + teste.getRng_Db()  + "segundo: " + teste1.getRng_Db());
//				}
//				i += 1;	
			
			}
		}

		finally {

			jdbc.closeSession();
		}

//****TESTE****
//		
//		Renegociado teste = itemRenegociado.get(0);
//		Renegociado teste1 = itemRenegociado.get(1);
//		
//		if (1 > 0) {
//			throw new Exception("JGP 1: Db " + teste.getRng_Db() + "segundo: " + teste1.getRng_Db() );
//		}
//		

		
		// **************************************************
		// * PREPARA E ENVIA EMAIL
		// **************************************************
		atualiza(linhas, itens, nufin, nureneg, RazaoSoc, email,
				linhasRenegociado, itemRenegociado);
	}
	
	public static void atualiza(StringBuffer linhas, ArrayList<Renegociacao> itens, String Nufin, String Nureneg,
			String Razao, String mail, StringBuffer linhasRenegociado, ArrayList<Renegociado> itemRenegociado)
			throws Exception {
		// String path = "/home/mgeweb/.sw_file_repository/Sistema/informativo.pdf";

		StringBuffer mensagem = new StringBuffer();
		StringBuffer mensagem1 = new StringBuffer();
		StringBuffer mensagem2 = new StringBuffer();
		StringBuffer mensagem3 = new StringBuffer();
		SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy_HHmmss");
		Timestamp dhatual = (Timestamp) JapeSessionContext.getProperty("dh_atual");
	    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY HH:mm");
		String dhatualFormatada = dateFormat.format(dhatual); 
		
		

		Iterator<Renegociacao> Item = itens.iterator();
		Iterator<Renegociado> ItemRenegociado = itemRenegociado.iterator();
	
		// Gerar chave MD5
		String nomeFile = "TITULO_RENEGOCIADO" + formatter.format(new Date()) + ".html";
		// chave = br.com.sankhya.ctba.cotrans.util.Util.geraMD5(nuMun.toString() +
		// "_AD_ISSRECEBIDOS-" + nomeFile);

		// String pref = Util.getRepositorio();
		String dir = "/home/mgeweb/.sw_file_repository/Sistema/";

		File file = new File(dir);

		String absolutePath = dir + nomeFile;

		file = new File(absolutePath);
		Util.createFile(file);
		Util.createDir(file);

		// if (1 > 0) {
		// throw new Exception("JGP 1: sql " );
		// }

		// Gerar arquivo .txt
		PrintWriter printWriter = new PrintWriter(new FileWriter(file, true));

		printWriter.println("<HTML>		<HEAD>SANKHYA</HEAD> <TITLE>EMAIL RENEGOCIADO</TITLE>");
	
		// **********************************
		// * TITULO RENEGOCIACAO (original)
		// *********************************
		mensagem2.append("<table  align=center border=3>");
		mensagem2.append(" <tr align=center> ");
		mensagem2.append("<td>Reneg.Ant</td> ");
		mensagem2.append("<td>Reneg.Atual</td> ");
		mensagem2.append("<td>N�mero Nota</td> ");
		mensagem2.append("<td width=\"50\">Db.</td> ");
		mensagem2.append("<td width=\"80\">Negocia��o</td> ");
		mensagem2.append("<td>Empresa</td> ");
		mensagem2.append("<td width=\"80\">Vencimento</td> ");
		mensagem2.append("<td width=\"80\">Valor Desdob.</td> ");
		mensagem2.append("<td width=\"80\">Valor Juro</td> ");
		mensagem2.append("<td width=\"80\">Valor Multa</td> ");
		mensagem2.append("<td width=\"80\">Valor Desconto</td> ");
		mensagem2.append("<td>Hist�rico</td> ");
	    mensagem2.append("<td width=\"50\">Tipo</td> ");
	    mensagem2.append("<td width=\"80\">N�mero �nico</td> ");
        mensagem2.append("<td>Parceiro</td> ");
        mensagem2.append("<td width=\"90\">Nome Parc.</td> ");
        								
		mensagem2.append(" </tr> ");

		while (Item.hasNext()) {
			Renegociacao nota = Item.next();

			mensagem2.append(" <tr align=center> ");
			mensagem2.append("<td>" + nota.getFin_NuRenegAnt()  + "</td>" 
			               + "<td>" + nota.getFin_NuRenegAtual() + "</td>"  
					       + "<td>" + nota.getFin_Numnota() + "</td>"
					       + "<td>" + nota.getFin_Db() + "</td>" 
					       + "<td>" + datas(nota.getFin_Dtneg()) + "</td>" 
					       + "<td>" + nota.getFin_Empresa() + "</td>" 
					       + "<td>" + datas(nota.getFin_Dtvenc()) + "</td>"  
					       + "<td>" + moeda(nota.getFin_VlrDesdbo()) + "</td>"  
					       + "<td>" + moeda(nota.getFin_VlrJuro()) + "</td>"  
					       + "<td>" + moeda(nota.getFin_VlrMulta()) + "</td>"
					       + "<td>" + moeda(nota.getFin_VlrDesconto()) + "</td>" 
					       + "<td>" + nota.getFin_Historico() + "</td>"
					       + "<td>" + nota.getFin_Tipo() + "</td>"
					       + "<td>" + nota.getFin_NumUnico() + "</td>"
					       + "<td>" + nota.getFin_CodParc() + "</td>"
					       + "<td>" + nota.getFin_NomeParc() + "</td>");
					       
			
			mensagem2.append(" </tr> ");

		}
		mensagem2.append("</table>");
		
		


		// **********************************
		// * TITULO RENEGOCIADO
		// *********************************
		mensagem3.append("<table  align=center border=3>");
		mensagem3.append(" <tr align=center> ");
		mensagem3.append("<td>Reneg.Post</td> ");
		mensagem3.append("<td>N�mero Nota</td> ");
		mensagem3.append("<td>N�mero �nico</td> ");
		mensagem3.append("<td width=\"50\">Db</td> ");
		mensagem3.append("<td width=\"80\">Negocia��o</td> ");
		mensagem3.append("<td>Empresa</td> ");
		mensagem3.append("<td>Vencimento</td> ");
		mensagem3.append("<td width=\"80\">Valor Desdob.</td> ");
		mensagem3.append("<td width=\"80\">Juros</td> "); 
		mensagem3.append("<td width=\"80\">Multa</td> ");
		mensagem3.append("<td width=\"80\">Juros Negociados</td> "); 
		mensagem3.append("<td width=\"80\">Multa Negociada</td> ");
		mensagem3.append("<td width=\"80\">Valor Desconto</td> ");
		mensagem3.append("<td width=\"80\">Valor L�quido</td> ");
		mensagem3.append("<td>Hist�rico</td> ");
		mensagem3.append(" </tr> ");
		for (int i = 0 ; i< itemRenegociado.size(); i++) {
	
			Renegociado rr = itemRenegociado.get(i);
			
			mensagem3.append(" <tr align=center> ");
			mensagem3.append( "<td>" + rr.getRng_NuRenegPost() + "</td>"	
						    + "<td>" + rr.getRng_Numnota() + "</td>"	
						    + "<td>" + rr.getRng_NumUnico() + "</td>"	
						    + "<td>" + rr.getRng_Db() + "</td>"	
						    + "<td>" + datas(rr.getRng_Dtneg()) + "</td>"	
						    + "<td>" + rr.getRng_Empresa() + "</td>"	
						    + "<td>" + datas(rr.getRng_Dtvenc()) + "</td>"	
						    + "<td>" + moeda(rr.getRng_VlrDesdbo()) + "</td>"	
						    + "<td>" + moeda(rr.getRng_VlrJuro()) + "</td>"	
						    + "<td>" + moeda(rr.getRng_VlrMulta()) + "</td>"	
						    + "<td>" + moeda(rr.getRng_VlrJuroNegociado()) + "</td>"	
							+ "<td>" + moeda(rr.getRng_VlrMultaNegociado()) + "</td>"	
						    + "<td>" + moeda(rr.getRng_VlrDesconto()) + "</td>"	
						    + "<td>" + moeda(rr.getRng_VlrLiquido()) + "</td>"
						    + "<td>" + rr.getRng_Historico() + "</td>" );	
			mensagem3.append(" </tr> ");
			

		}
		mensagem3.append("</table>");

		String msg0 = "\n <br> Existe(m) t�tulo(s) renegociado(s)!";
		String msg1 = "\n <br>N�mero Financeiro : " + Nufin;
		String msg2 = "\n     N�mero da Renegocia��o: " + Nureneg;
		String msg3 = "\n <br>Conforme abaixo seguem o(s) t�tulo(s):";
		String msg4 = "\n <br>T�tulo(s) Original(s): <br><br>";
		String msg5 = "\n <br>T�tulos Renegociados: <br><br>";

		mensagem.append(dhatualFormatada);

		mensagem.append(msg0 + msg1 + msg2 + msg3 + msg4);
		mensagem1.append(msg5);
		printWriter.println(mensagem.toString());
		printWriter.println(mensagem2.toString());
		printWriter.println(linhas.toString());
		
		printWriter.println(mensagem1.toString());
		printWriter.println(mensagem3.toString());
		printWriter.println(linhasRenegociado.toString());
		printWriter.println("</HTML>");
		printWriter.close();
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		// Busca a tabela a ser inserida, com base na instância
		EntityFacade dwfEntityFacade2 = EntityFacadeFactory.getDWFFacade();
		DynamicVO fileVO;

		fileVO = (DynamicVO) dwfEntityFacade2.getDefaultValueObjectInstance("AnexoMensagem");
		// inclui os valores desejados nos campos
		fileVO.setProperty("ANEXO", Files.readAllBytes(file.toPath()));

		// fileVO.setProperty("NOMEARQUIVO", "informativo.pdf");
		// fileVO.setProperty("TIPO", "application/pdf");

		fileVO.setProperty("NOMEARQUIVO", file.toString());
		fileVO.setProperty("TIPO", "application/html");
		fileVO.setProperty("CID", "EMAIL RENEGOCIACAO");

		// realiza o insert
		dwfEntityFacade2.createEntity("AnexoMensagem", (EntityVO) fileVO);
		
		//Leitura de Prefer�ncia
		String destinatario_email;
		destinatario_email =  (String) MGECoreParameter.getParameter("DESTINATARIOEMA");
		
		

		// captura a chave primaria criada após o insert
		//BigDecimal novaPK = (BigDecimal) fileVO.getProperty("NUANEXO");

		// mensagem = "Envio de promoção diária <br> Vigência até 16/07";
		mensagem.append(mensagem2);
		mensagem.append(mensagem1);
		mensagem.append(mensagem3);

		// insere na fila

		MSDFilaMensagemVO filaVO = (MSDFilaMensagemVO) dwfEntityFacade
				.getDefaultValueObjectInstance(DynamicEntityNames.FILA_MSG, MSDFilaMensagemVO.class);

		filaVO.setCODCON(BigDecimal.ZERO);
		filaVO.setCODMSG(null);
		filaVO.setSTATUS("Pendente");
		filaVO.setTIPOENVIO("E");
		filaVO.setMAXTENTENVIO(new BigDecimal(3));
		filaVO.setMENSAGEM(new String(mensagem.toString().getBytes("ISO-8859-1"), "ISO-8859-1").toCharArray());
		filaVO.setASSUNTO("Informativo Renegocia��o ");
		//filaVO.setNUANEXO(novaPK);
		//filaVO.setEMAIL("joao.pugsley@sankhya.com.br");
		filaVO.setEMAIL(destinatario_email);

		dwfEntityFacade.createEntity(DynamicEntityNames.FILA_MSG, filaVO);
		/*
		 * if ( 1>0){ throw new Exception("foi ate Email enviado com suceso! " +
		 * mensagem2.toString()); }
		 */
		// acao.setMensagemRetorno("Email enviado com suceso!");

	}

	public static String datas(Timestamp valor) throws Exception {

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY");

		if (valor != null) {

			return dateFormat.format(valor);
		} else {
			return "";
		}
   }
	public static String moeda(BigDecimal valor) throws Exception {
		
		Locale ptBR = new Locale("pt", "BR");
		NumberFormat moedaFormat = NumberFormat.getCurrencyInstance(ptBR); // para moedas

		if (valor != null) {
			return moedaFormat.format(valor);
		} else {
			return moedaFormat.format(0);
		}
	}

//	public static String espacos(String valor) throws Exception {
//		
//		if (valor != null) {
//			return valor;
//		} else {
//			return "";
//		}
//	}

}
