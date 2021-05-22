package br.com.sankhya.ctba.virada;

import java.sql.Timestamp;
import java.util.Collection;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class RefProrata implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {
		Timestamp dtref;
		
		//Coleta dos parâmetros da tela
		dtref = (Timestamp) contextoAcao.getParam("DTREF");

		//Configura o critério de busca
		/*FinderWrapper finderUpd = new FinderWrapper("Contrato", "nvl(FATURPRORATA,'N') = 'S' " + 
				"and nvl(ATIVO, 'N') = 'S' and numcontrato > 0 and numcontrato not in (745,747,748,1039,1178,1884,1104,1885,1301,1859,1882,1860,1863,1861,1862,1883,1730)"
				+ " and codparc in (select codparc from tgfpar where nvl(ativo,'S') = 'S') ");*/
		FinderWrapper finderUpd = new FinderWrapper("Contrato", "codparc in (select codparc from tgfpar where nvl(ativo,'S') = 'S') "
				+ "and numcontrato in (824,831,823,1376,834,826,825,835,721,837,830,832,838,1819,1650,833,827,836,829,828,730,822) ");
		//Insere os argumentos caso existam		
		finderUpd.setFinderArguments(new Object[] { dtref });
		finderUpd.setMaxResults(0);
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		//Realiza a busca na tabela pelos critérios
		Collection<PersistentLocalEntity> libCollection = dwfFacade.findByDynamicFinder("Contrato", finderUpd);
		//Itera entre os registos encontrados
		for (PersistentLocalEntity libEntity : libCollection) {
			DynamicVO newlibVO = (DynamicVO) libEntity.getValueObject();
			//Insere os valores desejados nos campos
			newlibVO.setProperty("DTREFPROXFAT", dtref);
			//Executa o update
			libEntity.setValueObject((EntityVO) newlibVO);
		}
		
		//insere mensagem de retorno no final da execução
		contextoAcao.setMensagemRetorno("Referencias ajustadas");

	}

}
