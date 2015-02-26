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
 */
public class Exports {

	private static final Logger LOGGER = Logger.getLogger(Exports.class.getName());

	public static String getExportCSV(List<Result> resultsProcessed, String separator) {

		boolean noResult = true;
		StringBuilder csv = new StringBuilder();
		if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
			String[] headers = new String[]{
				"LEFT CONTEXT", "KEYWORD", "RIGHT CONTEXT", "PID", "REFERENCE"};
			for (String header : headers) {
				csv.append("\"");
				csv.append(header);
				csv.append("\"");
				csv.append(separator);
			}
			csv.append("\n");

			for (Result result : resultsProcessed) {
				for (Kwic kwic : result.getKwics()) {
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
					csv.append(separator);
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

	public static byte[] getExportExcel(List<Result> resultsProcessed) throws ExportException {

		boolean noResult = true;
		SXSSFWorkbook workbook = null;
		ByteArrayOutputStream excelStream = new ByteArrayOutputStream();
		if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
			try {
				String[] headers = new String[]{
					"LEFT CONTEXT", "KEYWORD", "RIGHT CONTEXT", "PID", "REFERENCE"};

				workbook = new SXSSFWorkbook();
				Sheet sheet = workbook.createSheet();

				Font boldFont = workbook.createFont();
				boldFont.setBoldweight(Font.BOLDWEIGHT_BOLD);

				// Header
				CellStyle headerStyle = workbook.createCellStyle();
				headerStyle.setFont(boldFont);

				Row row = sheet.createRow(0);

				for (int j = 0; j < headers.length; ++j) {
					Cell cell = row.createCell(j, Cell.CELL_TYPE_STRING);
					cell.setCellValue(headers[j]);
					cell.setCellStyle(headerStyle);
				}

				// Body
				Cell cell;
				for (int k = 0; k < resultsProcessed.size(); k++) {
					Result result = resultsProcessed.get(k);
					List<Kwic> kwics = result.getKwics();
					for (int i = 0; i < kwics.size(); i++) {
						Kwic kwic = kwics.get(i);
						row = sheet.createRow(k + i + 1);
						cell = row.createCell(0, Cell.CELL_TYPE_STRING);
						cell.setCellValue(kwic.getLeft());
						cell = row.createCell(1, Cell.CELL_TYPE_STRING);
						cell.setCellValue(kwic.getKeyword());
						cell = row.createCell(2, Cell.CELL_TYPE_STRING);
						cell.setCellValue(kwic.getRight());
						if (kwic.getPid() != null) {
							cell = row.createCell(3, Cell.CELL_TYPE_STRING);
							cell.setCellValue(kwic.getPid());
						}
						if (kwic.getReference() != null) {
							cell = row.createCell(3, Cell.CELL_TYPE_STRING);
							cell.setCellValue(kwic.getReference());
						}
						noResult = false;
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
		if (noResult) {
			return null;
		} else {
			return excelStream.toByteArray();
		}

	}

	public static byte[] getExportTCF(List<Result> resultsProcessed,
			String searchLanguage) throws ExportException {
		String text = getExportText(resultsProcessed);
		if (text == null || text.isEmpty()) {
			return null;
		} else {
			WLData data;
			MetaData md = new MetaData();
			String languageCode = LanguagesISO693.getInstance().code_1ForCode_3(searchLanguage);
			TextCorpusStored tc = new TextCorpusStored(languageCode);
			tc.createTextLayer().addText(text);
			data = new WLData(md, tc);
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

	public static String getExportText(List<Result> resultsProcessed) {
		StringBuilder text = new StringBuilder();
		if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
			for (Result result : resultsProcessed) {
				for (Kwic kwic : result.getKwics()) {
					int i = kwic.getFragments().size() - 1;
					for (Kwic.TextFragment tf : kwic.getFragments()) {
						text.append(tf.text);
						if (i > 0) {
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
