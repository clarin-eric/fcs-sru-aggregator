package eu.clarin.sru.fcs.aggregator.search;

import eu.clarin.sru.fcs.aggregator.util.LanguagesISO693;
import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.md.xb.MetaData;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Utility for representing SearchResult data in different formats.
 *
 * @author Yana Panchenko
 * @author ljo
 */
public class Exports {

	private static final Logger LOGGER = Logger.getLogger(Exports.class.getName());

	public static String getExportCSV(List<Result> resultsProcessed, String filterLanguage, String separator) {
		boolean noResult = true;
		boolean firstRow = true;
		StringBuilder csv = new StringBuilder();
		if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
		    for (Result result : resultsProcessed) {
			if (result.getAdvancedLayers().size() == 0) {
			    for (Kwic kwic : result.getKwics()) {
				if (firstRow) {
				    String[] headers = new String[]{
					"PID", "REFERENCE", "LEFT CONTEXT", "KEYWORD", "RIGHT CONTEXT"};
				    for (String header : headers) {
					csv.append("\"");
					csv.append(header);
					csv.append("\"");
					csv.append(separator);
				    }
				    csv.append("\n");
				    firstRow = false;
				}
				if (filterLanguage != null && !filterLanguage.equals(kwic.getLanguage())) {
				    continue;
				}
				csv.append("\"");
				if (kwic.getPid() != null) {
				    csv.append(escapeQuotes(kwic.getPid()));
				}
				csv.append("\"");
				csv.append(separator);
				csv.append("\"");
				if (kwic.getReference() != null) {
				    csv.append(escapeQuotes(kwic.getReference()));
				}
				csv.append("\"");
				csv.append(separator);
				csv.append("\"");
				csv.append(escapeQuotes(kwic.getLeft()));
				csv.append("\"");
				csv.append(separator);
				csv.append("\"");
				csv.append(escapeQuotes(kwic.getKeyword()));
				csv.append("\"");
				csv.append(separator);
				csv.append("\"");
				csv.append(escapeQuotes(kwic.getRight()));
				csv.append("\"");
				csv.append("\n");
				noResult = false;
			    }
			}
			firstRow = true;
			for (AdvancedLayer layer : result.getAdvancedLayers()) {
			    if (firstRow) {
				String[] headers = new String[]{
				    "PID", "REFERENCE", "SPANS"};
				for (String header : headers) {
				    csv.append("\"");
				    csv.append(header);
				    csv.append("\"");
				    csv.append(separator);
				}
				csv.append("\n");
				firstRow = false;
			    }
			    if (filterLanguage != null && !filterLanguage.equals(layer.getLanguage())) {
				continue;
			    }
			    
			    csv.append("\"");
			    if (layer.getPid() != null) {
				csv.append(escapeQuotes(layer.getPid()));
			    }
			    csv.append("\"");
			    csv.append(separator);
			    csv.append("\"");
			    if (layer.getReference() != null) {
				csv.append(escapeQuotes(layer.getReference()));
			    }
			    csv.append("\"");
			    csv.append(separator);
			    for (AdvancedLayer.Span span : layer.getSpans()) {
				csv.append("\"");
				csv.append(escapeQuotes(span.getText()));
				csv.append("\"");
				csv.append(separator);
			    }
			    csv.append("\n");
			    noResult = false;
			}
		    }
		}
		if (noResult) {
			return null;
		} else {
			return csv.toString();
		}
	}

	private static CharSequence escapeQuotes(String text) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == '"') {
				sb.append('"');
			}
			sb.append(ch);
		}
		return sb;
	}

    public static byte[] getExportExcel(List<Result> resultsProcessed, String filterLanguage) throws ExportException {
	    SXSSFWorkbook workbook = null;
		ByteArrayOutputStream excelStream = new ByteArrayOutputStream();
		int rownum = 0;
		boolean firstRow = true;
		if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
		    try {
			workbook = new SXSSFWorkbook();
			Sheet sheet = workbook.createSheet();
			
			Font boldFont = workbook.createFont();
			boldFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
			
			// Header
			CellStyle headerStyle = workbook.createCellStyle();
			headerStyle.setFont(boldFont);
			
			Row row = sheet.createRow(rownum++);
			
			Cell cell;
			for (Result result : resultsProcessed) {
			    if (result.getAdvancedLayers().size() == 0) {
				for (Kwic kwic : result.getKwics()) {
				    if (firstRow) {
					String[] headers = new String[]{
					    "PID", "REFERENCE", "LEFT CONTEXT", "KEYWORD", "RIGHT CONTEXT"};
					for (int j = 0; j < headers.length; ++j) {
					    cell = row.createCell(j, Cell.CELL_TYPE_STRING);
					    cell.setCellValue(headers[j]);
					    cell.setCellStyle(headerStyle);
					}
					firstRow = false;
				    }
				    // Body
				    
				    if (filterLanguage != null && !filterLanguage.equals(kwic.getLanguage())) {
					continue;
				    }
				    row = sheet.createRow(rownum++);
				    cell = row.createCell(0, Cell.CELL_TYPE_STRING);
				    if (kwic.getPid() != null) {
					cell.setCellValue(kwic.getPid());
				    }
				    cell = row.createCell(1, Cell.CELL_TYPE_STRING);
				    if (kwic.getReference() != null) {
					cell.setCellValue(kwic.getReference());
				    }
				    
				    cell = row.createCell(2, Cell.CELL_TYPE_STRING);
				    cell.setCellValue(kwic.getLeft());
				    cell = row.createCell(3, Cell.CELL_TYPE_STRING);
				    cell.setCellValue(kwic.getKeyword());
				    cell.setCellStyle(headerStyle);
				    cell = row.createCell(4, Cell.CELL_TYPE_STRING);
				    cell.setCellValue(kwic.getRight());
				}
			    }
			    firstRow = true;
			    for (AdvancedLayer layer : result.getAdvancedLayers()) {
				if (firstRow) {
				    String[] headers = new String[]{
					"PID", "REFERENCE", "SPANS"};
				    for (int j = 0; j < headers.length; ++j) {
					cell = row.createCell(j, Cell.CELL_TYPE_STRING);
					cell.setCellValue(headers[j]);
					cell.setCellStyle(headerStyle);
				    }
				    firstRow = false;
				}
				
				if (filterLanguage != null && !filterLanguage.equals(layer.getLanguage())) {
				    continue;
				}
				row = sheet.createRow(rownum++);
				int j = 0;
				cell = row.createCell(j, Cell.CELL_TYPE_STRING);
				if (layer.getPid() != null) {
				    cell.setCellValue(layer.getPid());
				}
				j++;
				cell = row.createCell(j, Cell.CELL_TYPE_STRING);
				if (layer.getReference() != null) {
				    cell.setCellValue(layer.getReference());
				}
				j++;
				for (AdvancedLayer.Span span : layer.getSpans()) {
				    cell = row.createCell(j, Cell.CELL_TYPE_STRING);
				    cell.setCellValue(span.getText());
				    if (span.isHit()) {
					cell.setCellStyle(headerStyle);
				    }
				    j++;
				}
			    }
			}
			workbook.write(excelStream);
		    } catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
			throw new ExportException("Exception exporting Excel", ex);
		    } finally {
			if (workbook != null) {
			    workbook.dispose();
			}
		    }
		}
		if (rownum <= 1) {
			return null;
		} else {
			return excelStream.toByteArray();
		}
	}

	public static byte[] getExportTCF(List<Result> resultsProcessed,
			String searchLanguage, String filterLanguage) throws ExportException {
		String text = getExportText(resultsProcessed, filterLanguage);
		if (text == null || text.isEmpty()) {
			return null;
		} else {
			String lang = LanguagesISO693.getInstance().code_1ForCode_3(searchLanguage);
			if (filterLanguage != null) {
				lang = LanguagesISO693.getInstance().code_1ForCode_3(filterLanguage);
			}
			TextCorpusStored tc = new TextCorpusStored(lang);
			tc.createTextLayer().addText(text);
			WLData data = new WLData(new MetaData(), tc);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				WLDObjector.write(data, os);
			} catch (WLFormatException ex) {
				LOGGER.log(Level.SEVERE, "Error exporting TCF {0} {1}", new String[]{ex.getClass().getName(), ex.getMessage()});
				throw new ExportException("Error exporting TCF", ex);
			}
			return os.toByteArray();
		}
	}

	public static String getExportText(List<Result> resultsProcessed, String filterLanguage) {
		StringBuilder text = new StringBuilder();
		if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
			for (Result result : resultsProcessed) {
				for (Kwic kwic : result.getKwics()) {
					if (filterLanguage != null && !filterLanguage.equals(kwic.getLanguage())) {
						continue;
					}
					int i = kwic.getFragments().size() - 1;
					for (Kwic.TextFragment tf : kwic.getFragments()) {
						text.append(tf.text);
						char last = text.length() > 0 ? text.charAt(text.length() - 1) : ' ';
						if (i > 0 && !Character.isWhitespace(last)) {
							text.append(" ");
						}
						i--;
					}
					text.append("\n");
				}
			}
		}
		if (text.length() == 0) {
			return null;
		} else {
			return text.toString();
		}
	}
}
