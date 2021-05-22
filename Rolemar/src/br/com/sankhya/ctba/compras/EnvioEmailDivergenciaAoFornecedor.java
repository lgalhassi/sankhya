package br.com.sankhya.ctba.compras;

import java.io.File;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

import com.sankhya.util.StringUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.dwfdata.vo.tmd.MSDFilaMensagemVO;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.modelcore.util.Report;
import br.com.sankhya.modelcore.util.ReportManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

public class EnvioEmailDivergenciaAoFornecedor implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {
		final BigDecimal RELDIVFORN = (BigDecimal) MGECoreParameter.getParameter("RELDIVFORN");
		String dir = "", destinatarios = "";

		Registro[] registros = contextoAcao.getLinhas();
		if (registros.length == 0)
			throw new Exception("<br> <b>Nenhuma nota esta cadastrada para o envio das divergências.</b> <br>");

		for (Registro registro : registros) {
			long time = System.currentTimeMillis();
			dir = getDir(time);
			
			BigDecimal codDivCompras = (BigDecimal) registro.getCampo("CODDIVCOMPRAS");
			BigDecimal notaCompra = (BigDecimal) registro.getCampo("NOTACOMPRA");
			BigDecimal parcCompra = (BigDecimal) registro.getCampo("PARCCOMPRA");

			destinatarios = generateDestinatarios(parcCompra);
			
			Collection<DynamicVO> divergenciasVOs = getDivergenciasVOs(codDivCompras);

			for (DynamicVO divergenciaVO : divergenciasVOs) {
				BigDecimal codDiv = (BigDecimal) divergenciaVO.asBigDecimal("CODDIV");
				BigDecimal nrNota = divergenciaVO.asBigDecimal("NRNOTA");
				String tipo = divergenciaVO.asString("TIPO");

				Map<String, Object> param = new HashMap<String, Object>();
				param.put("TIPO", tipo);
				param.put("NUNOTA", notaCompra);

				generatePdf(param, codDiv, nrNota, RELDIVFORN, dir);
			}

			geraZip(dir);
			destinatarios = uniqueDest(destinatarios);

			sendEmail(destinatarios, dir + "/divergencias.zip", "Divergências");
			cleanupDir(dir);
		}

		contextoAcao.setMensagemRetorno("Email enviado com suceso!");
	}

	private String generateDestinatarios(BigDecimal codParc) throws Exception {
		String dest = "";
		Collection<DynamicVO> contatosVOs = getContatos(codParc);

		for (DynamicVO contatoVO : contatosVOs) {
			String mailCont = contatoVO.asString("EMAIL");

			if (dest.equals(""))
				dest = mailCont;
			else
				dest = dest + ";" + mailCont;
		}

		return dest;
	}

	private String uniqueDest(String destinatarios) {
		String email = null;
		String[] auxArray = destinatarios.split(";");

		ArrayList<String> listAux = new ArrayList<String>();

		for (String string : auxArray) {
			listAux.add(string);
		}

		List<String> unique = new ArrayList<String>(new HashSet<String>(listAux));

		for (String string : unique) {
			if (email == null)
				email = string;
			else
				email = email + "," + string;
		}

		return email;
	}

	private Collection<DynamicVO> getContatos(BigDecimal codParc) throws Exception {
		final JapeWrapper contatoDAO = JapeFactory.dao("Contato");
		return contatoDAO.find("this.CODPARC = ? ", codParc);
	}

	private String getDir(long time) throws Exception {
		String dir;

		dir = (String) MGECoreParameter.getParameter("FREPBASEFOLDER");
		if (dir == null)
			dir = ".sw_file_repository";
		else
			dir = dir.replace("\n", "");

		if (dir.charAt(dir.length() - 1) != '/')
			dir = dir + "/";

		dir = dir + "/Sistema/Temp/" + time;

		File file = new File(dir);
		file.mkdir();

		return dir;
	}

	private void generatePdf(Map<String, Object> pk, BigDecimal codDiv, BigDecimal nrNota, BigDecimal nurel, String dir)
			throws MGEModelException {
		EntityFacade dwf = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwf.getJdbcWrapper();

		try {
			jdbc.openSession();
			Map<String, Object> reportParams = buildReportParams(dwf, pk);

			Report report = ReportManager.getInstance().getReport(nurel, dwf);
			JasperPrint jasperPrint = report.buildJasperPrint(reportParams, jdbc.getConnection());
			OutputStream output = new FileOutputStream(new File(dir + "/" + codDiv + " - " + nrNota + ".pdf"));
			JasperExportManager.exportReportToPdfStream(jasperPrint, output);

			JRXlsExporter exporterXLS = new JRXlsExporter();
			exporterXLS.setParameter(JRXlsExporterParameter.JASPER_PRINT, jasperPrint);
			exporterXLS.setParameter(JRXlsExporterParameter.OUTPUT_STREAM, output);
			exporterXLS.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.TRUE);
			exporterXLS.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
			exporterXLS.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
			exporterXLS.exportReport();

		} catch (Exception e) {
			MGEModelException.throwMe(e);
		} finally {
			jdbc.closeSession();
		}
	}

	private static Map<String, Object> buildReportParams(EntityFacade dwf, Map<String, Object> pk) throws Exception {
		Map<String, Object> reportParams = new HashMap<String, Object>();

		String pastaModelos = StringUtils
				.getEmptyAsNull((String) MGECoreParameter.getParameter("os.diretorio.modelos"));

		reportParams.put("REPORT_CONNECTION", dwf.getJdbcWrapper().getConnection());
		reportParams.put("PDIR_MODELO", StringUtils.getEmptyAsNull(pastaModelos));
		reportParams.put("PCODUSULOGADO", AuthenticationInfo.getCurrent().getUserID());
		reportParams.put("PNOMEUSULOGADO", AuthenticationInfo.getCurrent().getName());

		for (Entry<String, Object> entry : pk.entrySet()) {
			reportParams.put(entry.getKey(), entry.getValue());
		}

		return reportParams;
	}

	private void geraZip(String dir) throws IOException {
		File f = new File(dir);
		String[] arquivosDir = f.list();

		FileOutputStream fos = new FileOutputStream(dir + "/divergencias.zip");
		ZipOutputStream zipOut = new ZipOutputStream(fos);
		for (String srcFile : arquivosDir) {
			File fileToZip = new File(dir + "/" + srcFile);
			FileInputStream fis = new FileInputStream(fileToZip);
			ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
			zipOut.putNextEntry(zipEntry);

			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zipOut.write(bytes, 0, length);
			}
			fis.close();
		}
		zipOut.close();
		fos.close();
	}

	private void sendEmail(String destinatarios, String path, String titleEmail) throws Exception {
		String mensagem;
		File file = new File(path);

		// Busca a tabela a ser inserida, com base na inst�ncia
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO fileVO;

		fileVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoMensagem");
		// inclui os valores desejados nos campos
		fileVO.setProperty("ANEXO", Files.readAllBytes(file.toPath()));
		fileVO.setProperty("NOMEARQUIVO", "divergencias.zip");
		fileVO.setProperty("TIPO", "application/zip");

		// realiza o insert
		dwfEntityFacade.createEntity("AnexoMensagem", (EntityVO) fileVO);

		// captura a chave primaria criada ap�s o insert
		BigDecimal novaPK = (BigDecimal) fileVO.getProperty("NUANEXO");

		mensagem = "Envio das divergências encontradas.";

		// insere na fila
		MSDFilaMensagemVO filaVO = (MSDFilaMensagemVO)

		dwfEntityFacade.getDefaultValueObjectInstance(DynamicEntityNames.FILA_MSG, MSDFilaMensagemVO.class);

		filaVO.setCODCON(BigDecimal.ZERO);
		filaVO.setCODMSG(null);
		filaVO.setSTATUS("Pendente");
		filaVO.setTIPOENVIO("E");
		filaVO.setMAXTENTENVIO(new BigDecimal(3));
		filaVO.setMENSAGEM(new String(mensagem.toString().getBytes("ISO-8859-1"), "ISO-8859-1").toCharArray());
		filaVO.setASSUNTO(titleEmail);
		filaVO.setNUANEXO(novaPK);
		//filaVO.setEMAIL(destinatarios);
		filaVO.setEMAIL("LUIS.GALHASSI@SANKHYA.COM.BR");
		filaVO.setCODSMTP(new BigDecimal(1.00));

		dwfEntityFacade.createEntity(DynamicEntityNames.FILA_MSG, filaVO);
	}

	private void cleanupDir(String dir) throws Exception {
		File file = new File(dir);
		FileUtils.deleteDirectory(file);
	}

	private Collection<DynamicVO> getDivergenciasVOs(BigDecimal codDivCompras) throws Exception {
		final JapeWrapper divComprasDAO = JapeFactory.dao("AD_DIVERGENCIAS2");
		return divComprasDAO.find("this.CODDIVCOMPRAS = ?", codDivCompras);
	}
}
