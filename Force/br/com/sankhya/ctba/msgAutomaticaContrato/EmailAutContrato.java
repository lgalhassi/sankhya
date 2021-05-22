//TCSCON
package br.com.sankhya.ctba.msgAutomaticaContrato;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.dwfdata.vo.tmd.MSDFilaMensagemVO;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class EmailAutContrato<WrapperVO> implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {

	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		BigDecimal numcontrato, codparc, codnat;
		DynamicVO manuVo = (DynamicVO) event.getVo();

		numcontrato = manuVo.asBigDecimal("NUMCONTRATO");
		codparc = manuVo.asBigDecimal("CODPARC");
		codnat = manuVo.asBigDecimal("CODNAT");

//		 if(1>0) throw new Exception("numcontrato" + numcontrato + " codparc " +
//		 codparc + " codnat " + codnat);
		configurarEmail(numcontrato, codparc, codnat, "Insert", null);

	}

	@Override
	public void afterUpdate(PersistenceEvent event) throws Exception {

	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {

	}

	@Override
	public void beforeDelete(PersistenceEvent event) throws Exception {
		BigDecimal numcontrato, codparc, codnat;
		DynamicVO manuVo = (DynamicVO) event.getVo();

		numcontrato = manuVo.asBigDecimal("NUMCONTRATO");
		codparc = manuVo.asBigDecimal("CODPARC");
		codnat = manuVo.asBigDecimal("CODNAT");

		// if(1>0) throw new Exception("numcontrato" + numcontrato + " codparc " +
		// codparc + " codnat " + codnat);
		configurarEmail(numcontrato, codparc, codnat, "Delete", null);
	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {

	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		BigDecimal numcontrato, codparc, codnat;
		String ativo;
		DynamicVO manuVo = (DynamicVO) event.getVo();

		numcontrato = manuVo.asBigDecimal("NUMCONTRATO");
		codparc = manuVo.asBigDecimal("CODPARC");
		codnat = manuVo.asBigDecimal("CODNAT");
		
		
		ModifingFields camposModificados = event.getModifingFields();
		if (camposModificados.isModifingAny("ATIVO")){
			ativo = manuVo.asString("ATIVO");
			configurarEmail(numcontrato, codparc, codnat, "Update", ativo);
		}

		// if(1>0) throw new Exception("numcontrato" + numcontrato + " codparc " +
		// codparc + " codnat " + codnat);


	}

	public void configurarEmail(BigDecimal numcontrato, BigDecimal codparc, BigDecimal codnat, String tipo, String ativo)
			throws Exception {

		String nomeparc, naturezaContrato =null, mensagem, msg1 = null, msg2 = null, msg3 = null, titulo = null;




		nomeparc = bsucaNomeParceiro(codparc);
		if (codnat != null) {
			naturezaContrato = bsucaNaturezaContrato(codnat);
		}

		if (tipo.equals("Insert")) {
			msg1 = "\n Gerado Novo Contrato Nro: <b> " + numcontrato + "</b>";
			msg2 = "\n \n Parceiro: <b>" + codparc + " - " + nomeparc + "</b>";
			msg3 = "\n Tipo: <b>" + naturezaContrato + "</b>";
			titulo = "Novo Contrato";
			mensagem = msg1 + msg2 + msg3;
			disparaEMail(mensagem, titulo);

		} else if (tipo.equals("Delete")) {
			msg1 = "\n Contrato Excluído Nro: <b> " + numcontrato + "</b>";
			msg2 = "\n \n Parceiro: <b>" + codparc + " - " + nomeparc + "</b>";
			msg3 = "\n Tipo: <b>" + naturezaContrato + "</b>";
			titulo = "Contrato Excluído";
			mensagem = msg1 + msg2 + msg3;
			disparaEMail(mensagem, titulo);

		}
		if (tipo.equals("Update")) {
			
			if ((ativo == null) || (ativo.equals("N"))){
				msg1 = "\n Alterado Contrato Nro: <b> " + numcontrato + "</b>";
				msg2 = "\n \n Parceiro: <b>" + codparc + " - " + nomeparc + "</b>";
				msg3 = "\n Tipo: <b>" + naturezaContrato + "</b>";
				titulo = "Contrato Cancelado";
				mensagem = msg1 + msg2 + msg3;
				disparaEMail(mensagem, titulo);
				
			}	
			
//				else if ((ativo != null) && (ativo.equals("S"))){
//					msg1 = "\n Alterado Contrato Nro: <b> " + numcontrato + "</b>";
//					msg2 = "\n \n Parceiro: <b>" + codparc + " - " + nomeparc + "</b>";
//					msg3 = "\n Tipo: <b>" + naturezaContrato + "</b>";
//					titulo = "Altera��o de Contrato";
//					mensagem = msg1 + msg2 + msg3;
//					disparaEMail(mensagem, titulo);
//
//				} else if (ativo == null){
//					msg1 = "\n Alterado Contrato Nro: <b> " + numcontrato + "</b>";
//					msg2 = "\n \n Parceiro: <b>" + codparc + " - " + nomeparc + "</b>";
//					msg3 = "\n Tipo: <b>" + naturezaContrato + "</b>";
//					titulo = "Altera��o de Contrato";
//					mensagem = msg1 + msg2 + msg3;
//					disparaEMail(mensagem, titulo);
//				}

			
		}
	}

	private String bsucaNomeParceiro(BigDecimal codparc) throws Exception {

		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity parcelaDestEntity = dwfFacade.findEntityByPrimaryKey("Parceiro",
				new Object[] { codparc });
		DynamicVO parcVO = (DynamicVO) parcelaDestEntity.getValueObject();

		return parcVO.asString("NOMEPARC");

	}

	private String bsucaNaturezaContrato(BigDecimal codnat) throws Exception {

		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity parcelaDestEntity = dwfFacade.findEntityByPrimaryKey("Natureza", new Object[] { codnat });
		DynamicVO parcVO = (DynamicVO) parcelaDestEntity.getValueObject();

		return parcVO.asString("DESCRNAT");

	}

	public static void disparaEMail(String mensagem, String titulo) throws Exception {

		String destEmail;
		
		destEmail = (String) MGECoreParameter.getParameter("DESTEMAILCONTRA");
		
		if(destEmail == null || destEmail.equals("")) {
			return;
		}
		
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
		filaVO.setMENSAGEM(new String(mensagem.toString().getBytes("ISO-8859-1"), "ISO-8859-1").toCharArray());
		filaVO.setASSUNTO(titulo);
		filaVO.setEMAIL(destEmail);

		dwfEntityFacade.createEntity(DynamicEntityNames.FILA_MSG, filaVO);

	}
}
