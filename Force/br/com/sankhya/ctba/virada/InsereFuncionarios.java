package br.com.sankhya.ctba.virada;

import java.math.BigDecimal;
import java.util.ArrayList;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class InsereFuncionarios implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {
		
		ArrayList<Object[]> lista;
		BigDecimal codprod;
		
		lista = preencheCarga();
		
		for (Object[] dados : lista) {
			codprod = insereServico((String) dados[0], (BigDecimal) dados[1], (BigDecimal) dados[2]);
			atualizaFunc((BigDecimal) dados[1], (BigDecimal) dados[2], codprod);
		}
		
		contextoAcao.setMensagemRetorno("funcionarios inseridos");


	}
	
	private void atualizaFunc(BigDecimal codfunc, BigDecimal codemp, BigDecimal codprod) throws Exception {
		//Busca o registro pela PK
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity parcelaDestEntity = dwfFacade.findEntityByPrimaryKey("Funcionario",
				new Object[] { codemp, codfunc });
		DynamicVO funVO = (DynamicVO) parcelaDestEntity.getValueObject();

		//setar propriedades à serem atualizadas
		funVO.setProperty("AD_CODPRODREF", codprod);

		//realiza o update
		parcelaDestEntity.setValueObject((EntityVO) funVO);

	}
	
	
	private ArrayList<Object[]> preencheCarga() {
		ArrayList<Object[]> lista = new ArrayList<Object[]>();
		
		//lista.add(new Object[]{"LETICIA PERUZZO CORDEIRO", new BigDecimal(18.00), new BigDecimal (1.00)});
		//lista.add(new Object[]{"ESTHER HELOISE FIGUEIREDO SANTOS", new BigDecimal(19.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"JOSIANE STURION FRACASSO", new BigDecimal(20.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"KATIA CRISTINA FERREIRA", new BigDecimal(21.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"ILISANGELA RIBEIRO", new BigDecimal(37.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"MARIMA FERNANDES CORREIA", new BigDecimal(40.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"JOSIANE CRISTINA BRAZ", new BigDecimal(38.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"SUELEN CRISTINA DE LARA", new BigDecimal(39.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"FABIANI INGRID DOS SANTOS", new BigDecimal(41.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"JULIANA DANTAS DE P FONSECA", new BigDecimal(42.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"JANETE ZUSE", new BigDecimal(22.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"MARIA ELVIRA FERREIRA TORAZZI", new BigDecimal(23.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"SOFIA PRCZYBYLA", new BigDecimal(24.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"DAIANE DA ROCHA ASSUNÇÃO", new BigDecimal(25.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"FERNANDA APARECIDA DE ANDRADE", new BigDecimal(26.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"SUELY PEREIRA DUTRA", new BigDecimal(27.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"ALESSANDRA MAIA ALVES", new BigDecimal(28.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"KARINA SOLOTORIW", new BigDecimal(29.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"ROSELI FELIX DE PAULA SILVA", new BigDecimal(30.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"MICHELLI CORREIA", new BigDecimal(31.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"PAMELA SUELEN BART DE CARVALHO", new BigDecimal(32.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"ERMELINDA CASSIA COLISSE", new BigDecimal(33.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"CAROLINE NIGELSKI", new BigDecimal(34.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"MARTA D'ALVES", new BigDecimal(35.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"CIUMARA APARECIDA DOS SANTOS", new BigDecimal(36.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"SUELI APARECIDA MUSSI", new BigDecimal(11.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"ADRIELE DE SOUZA", new BigDecimal(12.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"ISABELE TINE LOPES CALDEIRA", new BigDecimal(13.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"AMANDA RAMALHO NUSSE GOCINHO", new BigDecimal(14.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"CLAUDIA MENDES", new BigDecimal(15.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"CELIA APARECIDA DA CUNHA LIMA ", new BigDecimal(16.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"NATALIA FAVORETO NASCHI DE MORAES", new BigDecimal(17.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"GELIARDE VERETA", new BigDecimal(9033.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"ALBANI MIGUEL DA SILVA", new BigDecimal(5463.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"VITOR CUNHA DA SILVA", new BigDecimal(5454.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"MATHEUS BRITO RAMOS", new BigDecimal(5481.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"TATIANE CRISTINA DA CRUZ", new BigDecimal(5474.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"DAVISSON CESAR VIDOTO", new BigDecimal(5468.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"GUILHERME LUIZ MACHADO DE ARAUJO", new BigDecimal(5475.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"JOSEFA WICHNISKI", new BigDecimal(5450.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"ROSINEIDE SANTOS DA SILVA ", new BigDecimal(44.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"CHRISTYAN LEE GONÇALVES ", new BigDecimal(43.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"HEMMANUELY LETICIA D. DOS SANTOS", new BigDecimal(45.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"MARIA DAS GRACAS DE LIMA", new BigDecimal(46.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"ALESSANDRA MESQUITA ", new BigDecimal(47.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"LEILA DO ROCIO MACIEL DE LIMA", new BigDecimal(48.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"ANDRESSA VITOR DOS SANTOS SILVA ", new BigDecimal(49.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"SIMONE LEAL", new BigDecimal(50.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"ELAINE DA SILVA SANTOS ", new BigDecimal(10.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"EUMENIA STARUCZAK", new BigDecimal(9.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"MARCIA ANDRADE ALBARI", new BigDecimal(8.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"MARCIA FERREIRA PEDROZO", new BigDecimal(5421.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"JUDITE DE FATIMA PRESTES", new BigDecimal(5480.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"VIVIANE MASSANEIRA DE SOUZA OLIVEIRA", new BigDecimal(5473.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"MARINALVA PEREIRA DOS SANTOS", new BigDecimal(5466.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"ANNE CAROLINE VIDAL", new BigDecimal(5477.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"ROSINEI BARBOSA", new BigDecimal(5479.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"PAULO ROGERIO DO PRADO", new BigDecimal(5469.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"MARCIANO RODRIGO PEREIRA", new BigDecimal(9034.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"ERTON LEAL DE FIGUEIREDO", new BigDecimal(9031.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"LEONARDO CARDOSO FERRAZ", new BigDecimal(9032.00) ,new BigDecimal(1.00)});
		lista.add(new Object[]{"LUCAS WILLIAN DA COSTA BARBOSA", new BigDecimal(5489.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"LUIZ FERNANDO ARAUJO MARIA", new BigDecimal(5488.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"ANDREIA DOS SANTOS MACHADO", new BigDecimal(5487.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"MARCIA DE DEUS E SILVA CUNHA", new BigDecimal(5483.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"JOAO REINALDO KODUM", new BigDecimal(5462.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"FELIPE DOS SANTOS FERREIRA FAGUNDES", new BigDecimal(5461.00) ,new BigDecimal(2.00)});
		lista.add(new Object[]{"ROSANE PINHEIRO VIEIRA", new BigDecimal(5467.00) ,new BigDecimal(2.00)});
		
		return lista;
	}
	
	private BigDecimal insereServico(String nomeFunc, BigDecimal codfunc, BigDecimal codemp) throws Exception {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO servVO;
				
			servVO = (DynamicVO) dwfEntityFacade
							.getDefaultValueObjectInstance("Servico");
			servVO.setProperty("CODPROD", null);
			servVO.setProperty("DESCRPROD", nomeFunc); // Descrição
			servVO.setProperty("CODVOL", "UN"); // Unidade Padrão
			servVO.setProperty("CODGRUPOPROD", new BigDecimal("0107000"));//Grupo 
			servVO.setProperty("CODLST", new BigDecimal("1101") );	//Tipo de Serviço
			servVO.setProperty("AD_CODFUNC", codfunc );
			servVO.setProperty("AD_CODEMP_FUNC", codemp );
			servVO.setProperty("ATIVO", "S" );
			
			
			//precisa Alterar o Tipo de serviço !
			
			dwfEntityFacade.createEntity("Servico", (EntityVO) servVO);
	
			BigDecimal novaPK = (BigDecimal) servVO.getProperty("CODPROD");
			
			return novaPK;
		}

}
