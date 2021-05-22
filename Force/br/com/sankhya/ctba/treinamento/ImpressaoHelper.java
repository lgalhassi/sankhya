package br.com.sankhya.ctba.treinamento;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sankhya.util.StringUtils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.util.print.PrintManager;
import br.com.sankhya.modelcore.comercial.util.print.converter.PrintConversionService;
import br.com.sankhya.modelcore.comercial.util.print.model.PrintInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.modelcore.util.Report;
import br.com.sankhya.modelcore.util.ReportManager;
import br.com.sankhya.sps.enumeration.DocTaste;
import br.com.sankhya.sps.enumeration.DocType;
import net.sf.jasperreports.engine.JasperPrint;



public class ImpressaoHelper {

	public boolean imprimirRelatorio(BigDecimal nuRfe, Map<String, Object> pk) throws Exception {
		return imprimirRelatorio(nuRfe, pk, null); 
	}

	public static boolean imprimirRelatorio(BigDecimal nuRfe, Map<String, Object> pk, String printerName)
			throws Exception {

		boolean imprimiu = false;

		EntityFacade dwf = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwf.getJdbcWrapper();

		try {
			jdbc.openSession();

			Map<String, Object> reportParams = buildReportParams(dwf, pk);

			Report report = ReportManager.getInstance().getReport(nuRfe, dwf);

			JasperPrint jasperPrint = report.buildJasperPrint(reportParams, jdbc.getConnection());			

			//jasperPrint.setPageHeight(200);

			byte[] conteudo = PrintConversionService.getInstance().convert(jasperPrint, byte[].class);

			PrintManager printManager = PrintManager.getInstance();

			AuthenticationInfo authInfo = AuthenticationInfo.getCurrent();

			BigDecimal userId = authInfo.getUserID();
			String userName = authInfo.getName();
			String jobDescription = jasperPrint.getName();
			String localPrinterName = "SEM IMPRESSORA";

			if (printerName != null) {
				localPrinterName = printerName;
			}

			PrintInfo printInfo = new PrintInfo();
			printInfo.setCopies(1);
			printInfo.setDocument(conteudo);
			printInfo.setDocTaste(DocTaste.JASPER);
			printInfo.setDocType(DocType.RELATORIO);
			printInfo.setLocalPrinterName(localPrinterName);
			printInfo.setJobDescription(jobDescription);
			printInfo.setUserId(userId);
			printInfo.setUserName(userName);

			imprimiu = printManager.print(printInfo);

			// throw new Exception("Mensagem de erro! </b>" + jasperPrint.getPageHeight());

		} catch (Exception e) {
			MGEModelException.throwMe(e);
		} finally {
			jdbc.closeSession();
		}

		return imprimiu;
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
}
