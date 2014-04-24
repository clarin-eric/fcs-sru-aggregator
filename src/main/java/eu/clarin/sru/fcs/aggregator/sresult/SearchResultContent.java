package eu.clarin.sru.fcs.aggregator.sresult;

import eu.clarin.sru.fcs.aggregator.app.SearchResults;
import eu.clarin.sru.fcs.aggregator.app.WebAppListener;
import eu.clarin.sru.fcs.aggregator.sopt.Languages;
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
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Messagebox;

/**
 * Utility for representing SearchResult data in different formats.
 * 
 * @author Yana Panchenko
 */
public class SearchResultContent {

    private static final Logger LOGGER = Logger.getLogger(SearchResultContent.class.getName());

    public String getExportCSV(List<SearchResult> resultsProcessed, String separator) {

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

            for (SearchResult result : resultsProcessed) {
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
            Messagebox.show("Nothing to export!");
            return null;
        } else {
            return csv.toString();
        }
    }

    private CharSequence escapeQuotes(String text) {
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

    public byte[] getExportExcel(List<SearchResult> resultsProcessed) {

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
                    SearchResult result = resultsProcessed.get(k);
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
                // should not happen
                Logger.getLogger(SearchResults.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (workbook != null) {
                    workbook.dispose();
                }
            }
        }
        if (noResult) {
            Messagebox.show("Nothing to export!");
            return null;
        } else {
            return excelStream.toByteArray();
        }

    }

    private byte[] getExportTCF(List<SearchResult> resultsProcessed,
            String searchLanguage, Languages languages) {
        StringBuilder text = new StringBuilder();
        Set<String> resultsLangs = new HashSet<String>();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (SearchResult result : resultsProcessed) {
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
        ByteArrayOutputStream os = null;
        if (text.length() == 0) {
            Messagebox.show("Nothing to export!");
        } else {
            WLData data;
            MetaData md = new MetaData();
            String resultsLang = "unknown";
            if (resultsLangs.size() == 1) {
                resultsLang = resultsLangs.iterator().next();
                String code2 = languages.langForCode(resultsLang).getCode_639_1();
                if (code2 != null) {
                    resultsLang = code2;
                }
            } else if (!searchLanguage.equals("anylang")) {
                String code2 = languages.langForCode(resultsLang).getCode_639_1();
                if (code2 == null) {
                    resultsLang = searchLanguage;
                } else {
                    resultsLang = code2;
                }
            }
            TextCorpusStored tc = new TextCorpusStored(resultsLang);
            tc.createTextLayer().addText(text.toString());
            data = new WLData(md, tc);
            os = new ByteArrayOutputStream();
            try {
                WLDObjector.write(data, os);
            } catch (WLFormatException ex) {
                LOGGER.log(Level.SEVERE, "Error exporting TCF {0} {1}", new String[]{ex.getClass().getName(), ex.getMessage()});
                Messagebox.show("Sorry, export error!");
            }
        }
        if (os == null) {
            return null;
        } else {
            return os.toByteArray();
        }
    }

    public byte[] getExportTokenizedTCF(List<SearchResult> resultsProcessed,
            String searchLanguage, Languages languages) {
        StringBuilder text = new StringBuilder();
        Set<String> resultsLangs = new HashSet<String>();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (SearchResult result : resultsProcessed) {
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
        ByteArrayOutputStream os = null;
        if (text.length() == 0) {
            Messagebox.show("Nothing to export!");
        } else {
            WLData data;
            MetaData md = new MetaData();
            String resultsLang = "unknown";
            if (resultsLangs.size() == 1) {
                resultsLang = resultsLangs.iterator().next();
                String code2 = languages.langForCode(resultsLang).getCode_639_1();
                if (code2 != null) {
                    resultsLang = code2;
                }
            } else if (!searchLanguage.equals("anylang")) {
                String code2 = languages.langForCode(resultsLang).getCode_639_1();
                if (code2 == null) {
                    resultsLang = searchLanguage;
                } else {
                    resultsLang = code2;
                }
            }
            TextCorpusStored tc = new TextCorpusStored(resultsLang);
            tc.createTextLayer().addText(text.toString());
            addTokensSentencesMatches(resultsProcessed, tc);
            data = new WLData(md, tc);
            os = new ByteArrayOutputStream();
            try {
                WLDObjector.write(data, os);
            } catch (WLFormatException ex) {
                LOGGER.log(Level.SEVERE, "Error exporting TCF {0} {1}", new String[]{ex.getClass().getName(), ex.getMessage()});
                Messagebox.show("Sorry, export error!");
            }
        }
        if (os == null) {
            return null;
        } else {
            return os.toByteArray();
        }
    }

    private void addTokensSentencesMatches(List<SearchResult> resultsProcessed, TextCorpusStored tc) {

        TokenizerModel model = (TokenizerModel) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.DE_TOK_MODEL);

        if (model == null || !tc.getLanguage().equals("de")) {
            return;
        }
        TokenizerME tokenizer = new TokenizerME(model);

        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            tc.createTokensLayer();
            tc.createSentencesLayer();
            tc.createMatchesLayer("FCS", resultsProcessed.get(0).getSearchString());
            for (SearchResult result : resultsProcessed) {
                MatchedCorpus mCorpus = tc.getMatchesLayer().addCorpus(result.getCorpus().getDisplayName(), result.getCorpus().getHandle());
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

    private List<Token> addToTcfTokens(List<Token> tokens, TextCorpusStored tc, String[] tokenStrings) {
        List<Token> addedTokens = new ArrayList<Token>(tokenStrings.length);
        for (String tokenString : tokenStrings) {
            Token token = tc.getTokensLayer().addToken(tokenString);
            addedTokens.add(token);
            tokens.add(token);
        }
        return addedTokens;
    }

    public String getExportText(List<SearchResult> resultsProcessed) {
        StringBuilder text = new StringBuilder();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (SearchResult result : resultsProcessed) {
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
            Messagebox.show("Nothing to export!");
            return null;
        } else {
            return text.toString();
        }
    }
}
