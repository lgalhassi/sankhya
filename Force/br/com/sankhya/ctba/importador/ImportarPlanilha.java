package br.com.sankhya.ctba.importador;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class ImportarPlanilha implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {

		String path, entidade;
		
		
		Registro[] registros = contextoAcao.getLinhas();

		for (Registro registro : registros) {
			//recupera os valores das linhas selecionadas
			path = (String) registro.getCampo("PATH");
			entidade = (String) registro.getCampo("ENTITY");

			System.out.println("******IMPORTADOR - INICIO ********");
			
			
			ImportacaoHelper helper = new ImportacaoHelper(entidade);
			helper.initImportacao(path);
			
			System.out.println("******IMPORTADOR - FIM ********");
		}

		//insere mensagem de retorno no final da execução
		contextoAcao.setMensagemRetorno("Planilha importada com sucesso!");


	}

}
