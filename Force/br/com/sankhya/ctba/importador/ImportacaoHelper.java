package br.com.sankhya.ctba.importador;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class ImportacaoHelper {

	private String entidade;
	
	
	public ImportacaoHelper(String ent) {
		this.entidade = ent;
	}
	
	public void initImportacao(String path) throws Exception {
		
		
		if(1>0) { throw new Exception ("Working Directory = " + System.getProperty("user.dir"));}
		
		
		Iterator<Row> rowIterator;
		File arquivo = new File(path);
		rowIterator = getPlanilha(arquivo);
		processaArquivo(rowIterator);
			
		/* LEITURA DE DIRETORIO
		File[] arquivos = listaDiretorio(path);	
		for (File arquivo : arquivos) {
			rowIterator = getPlanilha(arquivo);
			processaArquivo(rowIterator);	
		}
		*/
	}
	
	private File[] listaDiretorio(String diretorio) {
		File[] filesList = null;
		
		try {
			File curDir = new File(diretorio);
		filesList = curDir.listFiles();
		
			
		
		}catch (Exception e) {
			e.printStackTrace();
		}
		return filesList;
	}
	
	public Iterator<Row> getPlanilha(File arquivo) throws Exception {

		//File myFile = new File("C://temp/Employee.xlsx"); 
		FileInputStream fis = new FileInputStream(arquivo); 
		// Finds the workbook instance for XLSX file 
		XSSFWorkbook myWorkBook = new XSSFWorkbook (fis); 
		
		// Return first sheet from the XLSX workbook 
		
		XSSFSheet mySheet = myWorkBook.getSheetAt(0); 
		
		// Get iterator to all the rows in current sheet 
		Iterator<Row> rowIterator = mySheet.iterator(); 
		
		return rowIterator;
	}
	
	private void processaArquivo(Iterator<Row> rowIterator) throws Exception {
		//Busca a tabela a ser inserida, com base na instância
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO linhaVO;
		Row row;
		ArrayList <String> cabecalho = null;
		int count = 1;
		
		
		while (rowIterator.hasNext()) {
			
			row = rowIterator.next(); 
			
			//trata o cabecalho
			if(row.getRowNum() == 0) {
				cabecalho = getCabecalho(row);
			}
			else {
				
				System.out.println("******IMPORTADOR - INICIANDO LINHA " + count + "******");
				
				linhaVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance(entidade);
				
				Iterator<Cell> cellIterator = row.cellIterator();
				
				while (cellIterator.hasNext()) { 
					Cell cell = cellIterator.next();
					
					linhaVO.setProperty(cabecalho.get(cell.getColumnIndex()), getValor(cell));
				}
				dwfEntityFacade.createEntity(entidade, (EntityVO) linhaVO);
				
				System.out.println("******IMPORTADOR - FINALIZADO LINHA " + count + "******");
				count++;	
			}
		}
	}
	
	
	private ArrayList <String> getCabecalho(Row row){
		ArrayList <String> cabecalho = new ArrayList<String>();
		Cell cell;
		
		Iterator<Cell> cellIterator = row.cellIterator();
		
		while (cellIterator.hasNext()) { 
			cell = cellIterator.next();
			cabecalho.add(cell.getStringCellValue());
			
		}
		return cabecalho;
	}
	
	
	private Object getValor(Cell cell) {
		if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
			//valida data
			if(HSSFDateUtil.isCellDateFormatted(cell)) {
				Date date= DateUtil.getJavaDate((double) cell.getNumericCellValue());
				return new Timestamp(date.getTime());
			}
			else {
				//valida casas decimais
				Double valor;
				valor = cell.getNumericCellValue();
				
				if (valor % 1 != 0) {
					return BigDecimal.valueOf(valor).setScale(8,RoundingMode.DOWN);
				}
				else {
					return BigDecimal.valueOf(valor).setScale(0,RoundingMode.DOWN);
				}
					
			}
		}
		
		if (cell.getCellType() == Cell.CELL_TYPE_BLANK)
			return null;
		
		if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
			String valor = cell.getStringCellValue();
			if(valor.equals("SYSDATE")) {
				return (Timestamp) JapeSessionContext.getProperty("dh_atual");
			}
			else {
				return valor;
			}
		}
		return null;
		
	}
}
