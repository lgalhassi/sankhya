package br.com.sankhya.ctba.treinamento;

import java.math.BigDecimal;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
/*********************************************************************************************************************************************************************************************

Autor: Renan Teixeira da Silva - Sankhya Curitiba
Versão: 1.0
Data de Implementação: 08/01/2020
Objetivo: Alterar a informação de data de embarque no detalhamento de nota
Tabela Alvo: TCSCON
Histórico:
1.0 - Implementação da rotina

**********************************************************************************************************************************************************************************************/

public class Acao implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {
		BigDecimal perc;
		
		contextoAcao.confirmar("Confirmação", "Deseja efetuar o registro de Ativação? "
				+ "<br><b>A rotina irá recalcular os valores do produto selecionado para todos os contratos</b>", 1);

		//Coleta dos parâmetros da tela
		//dtFim = (Timestamp) contextoAcao.getParam("DTFIM");
		
		perc = new BigDecimal ((Double) contextoAcao.getParam("PERC"));
		//perc = new BigDecimal((String) (contextoAcao.getParam("CODPROD")) );
		
		Registro[] registros = contextoAcao.getLinhas();

		for (Registro registro : registros) {
			//recupera os valores das linhas selecionadas
	//		numcontrato = (String) registro.getCampo("LANC_FIN");
	//		qtdParcelas = (BigDecimal) registro.getCampo("QTD_PARCELAS");

			//Dentro do botão de ação é possível fazer um update a linha selecionada diretamente alterando o objeto registro
			registro.setCampo("LANC_FIN", "S");
		}

		//insere mensagem de retorno no final da execução
		contextoAcao.setMensagemRetorno("Rodou com sucesso!");


	}

}
