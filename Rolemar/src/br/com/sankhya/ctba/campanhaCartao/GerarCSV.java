package br.com.sankhya.ctba.campanhaCartao;


import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;



import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;

public class GerarCSV implements AcaoRotinaJava {

	@SuppressWarnings({ "resource" })
	@Override
	public void doAction(ContextoAcao acao) throws Exception {

		List<BigDecimal> listaCampanha = new ArrayList<BigDecimal>();
		BigDecimal codcam = null;
		String nomeFile, dir, chave, pref;
		
		QueryExecutor query = acao.getQuery();
			
		Registro[] registros = acao.getLinhas();
		
		if (registros.length==0) {
			throw new Exception ("Selecione uma campanha");
		}
		
		for (int x = 0; x < registros.length; x++) {

			codcam = (BigDecimal) registros[x].getCampo("CODCAM");
			listaCampanha.add(codcam);

		}
		
		//gerando chave MD5
		nomeFile = "Campanha"+codcam+".CSV";
		chave = br.com.sankhya.ctba.campanhaCartao.Util.geraMD5(codcam.toString() + "_AD_CAMPCAR" + nomeFile);

		pref = Util.getRepositorio();
		dir = pref + "/Sistema/Anexos/AD_CAMPCAR/";

		File file = new File(dir);
		br.com.sankhya.ctba.campanhaCartao.Util.createDir(file);

		String absolutePath = dir + chave;

		file = new File(absolutePath);
		
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absolutePath), StandardCharsets.UTF_8));
		
		for (BigDecimal codCampanha : listaCampanha) {
			List<DadosCampanha> lista = new ArrayList<DadosCampanha>();
			CarregarDadosCampanha carga = new CarregarDadosCampanha();

			lista = carga.carregarDadosCampanha(codCampanha);
	
		    if (lista.isEmpty()) {
		    	throw new Exception("Campanha não possui nenhum parceiro que cumpriu a meta");
		    }
			String cabecalho = "CARTAO;CNPJ;RAZAOSOCIAL;VALOR;DATANASCIMENTO;UNIDADE";
			out.write(cabecalho);			
			for (DadosCampanha a : lista) {	
				
				if (a.getRolCartao() !=null) {
					String valor = a.getValor().setScale(2, BigDecimal.ROUND_HALF_EVEN).toString().replace('.', ',');
					out.newLine();
					out.write(a.getRolCartao().toString()+";"+ 
				              a.getCgcCpf().toString()+";"+
							  a.getRazaoSocial()+";"+
				              valor+";"+
							  "01/01/1990; ;");	
				} else {
					throw new Exception ("O Parceiro: "+a.getRazaoSocial()+ " está com o número do cartão em branco no cadastro");
				}
			}
						
			out.close();
			
			Anexo anexo = new Anexo();
			
			anexo.addAnexo(query, chave, nomeFile, codcam);
			
			query.close();

			acao.setMensagemRetorno("Arquivo salvo em anexo!");
		}
		
	}


}