package eu.clarin.sru.fcs.aggregator.search;

import eu.clarin.sru.fcs.aggregator.lang.LanguagesISO693_2;
import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.md.xb.MetaData;
import eu.clarin.weblicht.wlfxb.tc.api.MatchedCorpus;
import eu.clarin.weblicht.wlfxb.tc.api.Token;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
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

	private static byte[] getExportTCF(List<Result> resultsProcessed,
			String searchLanguage) throws ExportException {
        StringBuilder text = new StringBuilder();
        Set<String> resultsLangs = new HashSet<String>();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (Result result : resultsProcessed) {
                resultsLangs.addAll(result.getCorpus().getLanguages());
                for (Kwic kwic : result.getKwics()) {
                    text.append(kwic.getLeft());
                    text.append(" ");
                    text.append(kwic.getKeyword());
                    text.append(" ");
                    text.append(kwic.getRight());
                    text.append("\n");
                }
            }

        }
        if (text.length() == 0) {
			return null;
        } else {
            WLData data;
            MetaData md = new MetaData();
            String resultsLang = "unknown";
            if (resultsLangs.size() == 1) {
                resultsLang = resultsLangs.iterator().next();
				String code2 = LanguagesISO693_2.getInstance().langForCode(resultsLang).getCode_639_1();
                if (code2 != null) {
                    resultsLang = code2;
                }
            } else if (!searchLanguage.equals("anylang")) {
				String code2 = LanguagesISO693_2.getInstance().langForCode(resultsLang).getCode_639_1();
                if (code2 == null) {
                    resultsLang = searchLanguage;
                } else {
                    resultsLang = code2;
                }
            }
            TextCorpusStored tc = new TextCorpusStored(resultsLang);
            tc.createTextLayer().addText(text.toString());
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

	public static byte[] getExportTokenizedTCF(List<Result> resultsProcessed,
			String searchLanguage, TokenizerModel tokenizerModel) throws ExportException {
        StringBuilder text = new StringBuilder();
        Set<String> resultsLangs = new HashSet<String>();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (Result result : resultsProcessed) {
                resultsLangs.addAll(result.getCorpus().getLanguages());
                for (Kwic kwic : result.getKwics()) {
                    text.append(kwic.getLeft());
                    text.append(" ");
                    text.append(kwic.getKeyword());
                    text.append(" ");
                    text.append(kwic.getRight());
                    text.append("\n");
                }
            }

        }
        if (text.length() == 0) {
			return null;
        } else {
            WLData data;
            MetaData md = new MetaData();
            String resultsLang = "unknown";
            if (resultsLangs.size() == 1) {
                resultsLang = resultsLangs.iterator().next();
				String code2 = LanguagesISO693_2.getInstance().langForCode(resultsLang).getCode_639_1();
                if (code2 != null) {
                    resultsLang = code2;
                }
            } else if (!searchLanguage.equals("anylang")) {
				String code2 = LanguagesISO693_2.getInstance().langForCode(resultsLang).getCode_639_1();
                if (code2 == null) {
                    resultsLang = searchLanguage;
                } else {
                    resultsLang = code2;
                }
            }
            TextCorpusStored tc = new TextCorpusStored(resultsLang);
            tc.createTextLayer().addText(text.toString());
			addTokensSentencesMatches(resultsProcessed, tc, tokenizerModel);
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

	private static void addTokensSentencesMatches(List<Result> resultsProcessed, TextCorpusStored tc, TokenizerModel model) {
        if (model == null || !tc.getLanguage().equals("de")) {
            return;
        }
        TokenizerME tokenizer = new TokenizerME(model);

        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            tc.createTokensLayer();
            tc.createSentencesLayer();
            tc.createMatchesLayer("FCS", resultsProcessed.get(0).getSearchString());
            for (Result result : resultsProcessed) {
				MatchedCorpus mCorpus = tc.getMatchesLayer().addCorpus(result.getCorpus().getTitle(), result.getCorpus().getHandle());
                for (Kwic kwic : result.getKwics()) {
                    List<Token> tokens = new ArrayList<Token>();
                    addToTcfTokens(tokens, tc, tokenizer.tokenize(kwic.getLeft()));
                    String[] target = tokenizer.tokenize(kwic.getKeyword());
                    List<Token> targetTokens = addToTcfTokens(tokens, tc, target);
                    addToTcfTokens(tokens, tc, tokenizer.tokenize(kwic.getRight()));
                    tc.getSentencesLayer().addSentence(tokens);
                    List<String> pidAndRef = new ArrayList<String>();
                    if (kwic.getPid() != null) {
                        pidAndRef.add(kwic.getPid());
                    }
                    if (kwic.getReference() != null) {
                        pidAndRef.add(kwic.getReference());
                    }
                    tc.getMatchesLayer().addItem(mCorpus, targetTokens, pidAndRef);
                }
            }
        }
    }

	private static List<Token> addToTcfTokens(List<Token> tokens, TextCorpusStored tc, String[] tokenStrings) {
        List<Token> addedTokens = new ArrayList<Token>(tokenStrings.length);
        for (String tokenString : tokenStrings) {
            Token token = tc.getTokensLayer().addToken(tokenString);
            addedTokens.add(token);
            tokens.add(token);
        }
        return addedTokens;
    }

	public static String getExportText(List<Result> resultsProcessed) {
        StringBuilder text = new StringBuilder();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (Result result : resultsProcessed) {
                for (Kwic kwic : result.getKwics()) {
                    text.append(kwic.getLeft());
                    text.append(" ");
                    text.append(kwic.getKeyword());
                    text.append(" ");
                    text.append(kwic.getRight());
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
