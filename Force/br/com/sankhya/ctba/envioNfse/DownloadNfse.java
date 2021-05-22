package br.com.sankhya.ctba.envioNfse;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;

import com.sankhya.util.SessionFile;
import com.sankhya.util.StringUtils;
import com.sankhya.util.UIDGenerator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.modelcore.util.Report;
import br.com.sankhya.modelcore.util.ReportManager;
import br.com.sankhya.ws.ServiceContext;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

public class DownloadNfse implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao contextoAcao) throws Exception {
		BigDecimal nunota, nurel, numnota;
		
		String dir, link;
		
		long time = System.currentTimeMillis();
		
		dir = getDir(time);
		
		Registro[] registros = contextoAcao.getLinhas();
		
		for (Registro registro : registros) {
			//recupera os valores das linhas selecionadas
			nunota = (BigDecimal) registro.getCampo("NUNOTA");
			numnota = (BigDecimal) registro.getCampo("NUMNOTA");
			
			
			nurel = getModeloRel(nunota); 
			if(nurel == null) {
				throw new Exception("TOP sem modelo de impressão de NFS-e configurado");
			}
			
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("NUNOTA", nunota);
			
			geraPdf(param, nurel, dir, numnota);
					
		}
		
		
		geraZip(dir);

		link = downloadZip(dir + "/notas.zip");
		
		cleanupDir(dir);
		//insere mensagem de retorno no final da execução
		contextoAcao.setMensagemRetorno(link);
	}
	
	
	private void geraPdf(Map<String, Object> pk, BigDecimal nurel, String dir, BigDecimal numnota) throws MGEModelException {
		EntityFacade dwf = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwf.getJdbcWrapper();

		try {
			jdbc.openSession();
			Map<String, Object> reportParams = buildReportParams(dwf, pk);
	
			Report report = ReportManager.getInstance().getReport(nurel, dwf);
	
			JasperPrint jasperPrint = report.buildJasperPrint(reportParams, jdbc.getConnection());

			//OutputStream output = new FileOutputStream(new File("/home/forcesnk/\\home\\mgeweb\\modelos\\/Sistema/" + dir + "/" + pk.get("NUNOTA").toString() + ".pdf"));
			OutputStream output = new FileOutputStream(new File(dir + "/" + numnota + ".pdf"));
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
		
        FileOutputStream fos = new FileOutputStream(dir+"/notas.zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        for (String srcFile : arquivosDir) {
            File fileToZip = new File(dir + "/" + srcFile);
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);
 
            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        }
        zipOut.close();
        fos.close();
	}
	
	
	private String  getDir(long time) throws Exception{
		String dir;

		dir = (String) MGECoreParameter.getParameter("FREPBASEFOLDER");
		if (dir == null) {
			dir = ".sw_file_repository";
		} else {
			dir = dir.replace("\n", "");
		}

		if (dir.charAt(dir.length() - 1) != '/') {
			dir = dir + "/";
		}
		
		dir = dir + "/Sistema/Temp/" + time;
		
	    File file = new File(dir);
	    file.mkdir();
	    
		return dir;
	}
	
	private void cleanupDir(String dir) throws Exception {
		File file = new File(dir);
		
		FileUtils.deleteDirectory(file);
		
	}

	private BigDecimal getModeloRel(BigDecimal nunota) throws Exception {
		JdbcWrapper jdbc = null;
		BigDecimal modelo = null;
		
		try {
			jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			
			NativeSql sql = new NativeSql(jdbc);
			sql.setNamedParameter("NUNOTA", nunota);
			
			sql.appendSql(" SELECT MON.NURFE" );
			sql.appendSql(" FROM" );
			sql.appendSql(" TGFCAB CAB JOIN TGFTOP TOP ON (CAB.CODTIPOPER = TOP.CODTIPOPER AND CAB.DHTIPOPER = TOP.DHALTER)" );
			sql.appendSql(" JOIN TGFMON MON ON (TOP.CODMODNFSE = MON.CODMODNF)" );
			sql.appendSql(" WHERE CAB.NUNOTA = :NUNOTA" );

			ResultSet rs = sql.executeQuery();
			
			if (rs.next()) {
				modelo = rs.getBigDecimal("NURFE");
			}
			
		}finally {
			jdbc.closeSession();
		}
		
		return modelo;
	}
	
	private String downloadZip(String zip) throws Exception {
		String link;
		File file = new File(zip);
	    String url = MGECoreParameter.getParameterAsString("URLSKW");
	      
	    byte[] conteudo = Files.readAllBytes(file.toPath());
	    String chave = "download_" + UIDGenerator.getNextID();
	    HttpSession session = ServiceContext.getCurrent().getHttpRequest().getSession();
	    session.setAttribute(chave, SessionFile.createSessionFile("notas.zip", "application/octet-stream", conteudo));
	      
	    link = "<b><u><a href=\"" + url + "/mge/visualizadorArquivos.mge?chaveArquivo=" + chave + "\" target=\"_blank\"><br>Clique aqui para realizar o download<br><br></a></u></b>";

	    
	    return link;
	}
	
}
