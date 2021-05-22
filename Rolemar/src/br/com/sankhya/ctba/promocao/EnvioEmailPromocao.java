package br.com.sankhya.ctba.promocao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
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
import br.com.sankhya.jape.util.JapeSessionContext;
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

public class EnvioEmailPromocao implements AcaoRotinaJava {

	public void doAction(ContextoAcao contextoAcao) throws Exception {
		final BigDecimal RELPROMO = (BigDecimal) MGECoreParameter.getParameter("RELPROMO");

		String tipoProm = (String) contextoAcao.getParam("P_TIPOPROM");

		Registro regPai = contextoAcao.getLinhaPai();
		String uf = getUFString(regPai);
		String destinatarios = "", titleEmail = "", dir = "";

		long time = System.currentTimeMillis();
		dir = getDir(time);

		Collection<DynamicVO> promocaoVOs = null;
		Timestamp dAtual = (Timestamp) JapeSessionContext.getProperty("d_atual");

		if ("D".equals(tipoProm)) {
			promocaoVOs = getPromocoes(dAtual, "D");
			if (promocaoVOs.size() == 0) {
				throw new Exception("<br> <b>Não foi encontrado promoções para a data atual.</b> <br>");
			}

			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			String dataStr = dateFormat.format(dAtual);
			titleEmail = "Promoção Diária - " + uf + " - " + dataStr;

		} else if ("M".equals(tipoProm)) {
			promocaoVOs = getPromocoes(dAtual, "M");
			if (promocaoVOs.size() == 0)
				throw new Exception("<br> <b>Não foi encontrado nenhuma promoção mensal para a data atual.</b> <br>");

			titleEmail = "Promoção Mensal - " + uf;

		} else if ("L".equals(tipoProm)) {
			promocaoVOs = getPromocoes(dAtual, "L");
			if (promocaoVOs.size() == 0)
				throw new Exception("<br> <b>Não foi encontrado nenhuma promoção de liquidação ativa.</b> <br>");

			titleEmail = "Promoção Liquidação - " + uf;
		}

		Registro[] registros = contextoAcao.getLinhas();
		if (registros.length == 0)
			throw new Exception("<br> <b>Nenhum parceiro esta cadastrado para o envio das promoções.</b> <br>");

		destinatarios = generateDestinatarios(registros);

		if (destinatarios == null) {
			cleanupDir(dir);
			throw new Exception(
					"Não existe nenhum email de destino cadastrado! <br><br> Necessário verificar o campo <b>E-mail específico p/ envio das promoções</b> no cadastro do Parceiro");
		}
		
		for (Registro registro : registros) {
			BigDecimal envMail = (BigDecimal) registro.getCampo("ENVMAIL");

			for (DynamicVO promocaoVO : promocaoVOs) {
				BigDecimal codProm = promocaoVO.asBigDecimal("CODPROM");

				Map<String, Object> param = new HashMap<String, Object>();
				param.put("ENVMAIL", envMail);

				generatePdf(param, codProm, RELPROMO, dir);
			}
		}


		geraZip(dir);
		destinatarios = uniqueDest(destinatarios);

		sendEmail(destinatarios, dir + "/promocoes.zip", titleEmail);
		cleanupDir(dir);

		contextoAcao.setMensagemRetorno("Email enviado com suceso!");
	}

	private Collection<DynamicVO> getPromocoes(Timestamp dToday, String tipoProm) throws Exception {
		final JapeWrapper promocaoDAO = JapeFactory.dao("AD_ROLPRO");

		if ("D".equals(tipoProm)) {
			return promocaoDAO.find("this.DTINI = ? AND this.TIPOPROMO = ? ", dToday, tipoProm);

		} else if ("M".equals(tipoProm)) {
			return promocaoDAO.find(
					"this.TIPOPROMO = ? AND this.DTINI <= ? AND (this.DTFIM >= ? OR this.DTFIM is null)", tipoProm,
					dToday, dToday);

		} else if ("L".equals(tipoProm)) {
			return promocaoDAO.find("this.TIPOPROMO = ?", tipoProm);
		}

		return null;
	}

	private String generateDestinatarios(Registro[] registros) {
		String dest = "";

		for (Registro registro : registros) {
			String mailCont = (String) registro.getCampo("MAILCONT");

			if (dest.equals(""))
				dest = mailCont;
			else
				dest = dest + ";" + mailCont;
		}

		return dest;
	}

	private DynamicVO getUFVo(BigDecimal sigla) throws Exception {
		final JapeWrapper ufDAO = JapeFactory.dao("UnidadeFederativa");
		return ufDAO.findByPK(sigla);
	}

	private String getUFString(Registro regPai) throws Exception {
		if (regPai.getCampo("SIGLAEST") != null) {
			return getUFVo((BigDecimal) regPai.getCampo("SIGLAEST")).asString("UF");

		} else if (regPai.getCampo("CODCID") != null) {
			final JapeWrapper cidadeDAO = JapeFactory.dao("Cidade");
			DynamicVO cidadeVO = cidadeDAO.findByPK(regPai.getCampo("CODCID"));

			return getUFVo(cidadeVO.asBigDecimal("UF")).asString("UF");
		}
		return "";
	}

	private void sendEmail(String destinatarios, String path, String titleEmail) throws Exception {
		String mensagem;
		File file = new File(path);

		// Busca a tabela a ser inserida, com base na instância
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO fileVO;

		fileVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoMensagem");
		// inclui os valores desejados nos campos
		fileVO.setProperty("ANEXO", Files.readAllBytes(file.toPath()));
		fileVO.setProperty("NOMEARQUIVO", "promocoes.zip");
		fileVO.setProperty("TIPO", "application/zip");

		// realiza o insert
		dwfEntityFacade.createEntity("AnexoMensagem", (EntityVO) fileVO);

		// captura a chave primaria criada após o insert
		BigDecimal novaPK = (BigDecimal) fileVO.getProperty("NUANEXO");

		mensagem = "Envio de promo	ção diária";

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
		filaVO.setEMAIL(destinatarios);
		filaVO.setCODSMTP(new BigDecimal(1.00));

		dwfEntityFacade.createEntity(DynamicEntityNames.FILA_MSG, filaVO);
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

	private void generatePdf(Map<String, Object> pk, BigDecimal codProm, BigDecimal nurel, String dir)
			throws MGEModelException {
		EntityFacade dwf = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwf.getJdbcWrapper();

		try {
			jdbc.openSession();
			Map<String, Object> reportParams = buildReportParams(dwf, pk);

			Report report = ReportManager.getInstance().getReport(nurel, dwf);
			JasperPrint jasperPrint = report.buildJasperPrint(reportParams, jdbc.getConnection());
			OutputStream output = new FileOutputStream(new File(dir + "/" + codProm + ".pdf"));
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

	private void cleanupDir(String dir) throws Exception {
		File file = new File(dir);
		FileUtils.deleteDirectory(file);
	}

	private void geraZip(String dir) throws IOException {
		File f = new File(dir);
		String[] arquivosDir = f.list();

		FileOutputStream fos = new FileOutputStream(dir + "/promocoes.zip");
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

}
