package eu.clarin.sru.fcs.aggregator.sopt;

/**
 *
 * @author Yana Panchenko
 */
public class Language implements Comparable {
    
    //2-letter code
    private String code_639_1;
    //3-letter terminological code
    private String code_639_2T;
    //3-letter bibliographic code
    private String code_639_2B;
    //english name
    private String nameEn;
    
    public Language(String code_639_1, String code_639_2T, String code_639_2B, String nameEn) {
        this.code_639_1 = code_639_1;
        this.code_639_2T = code_639_2T;
        this.code_639_2B = code_639_2B;
        this.nameEn = nameEn;
    }

    public String getCode_639_1() {
        return code_639_1;
    }

    public String getCode_639_2T() {
        return code_639_2T;
    }

    public String getCode_639_2B() {
        return code_639_2B;
    }

    public String getNameEn() {
        return nameEn;
    }
    
    /**
     * Gets ISO code of the language. It is code 639/2T if known. If not,
     * it is 639/2B if known. If not, it is 639/1.
     * 
     * @return language ISO code
     */
    public String getCode() {
        String code = this.code_639_2T;
        if (code == null || code.isEmpty()) {
            code = this.code_639_2B;
        }
        if (code == null || code.isEmpty()) {
            code = this.code_639_1;
        }
        return code;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.code_639_1 != null ? this.code_639_1.hashCode() : 0);
        hash = 97 * hash + (this.code_639_2T != null ? this.code_639_2T.hashCode() : 0);
        hash = 97 * hash + (this.code_639_2B != null ? this.code_639_2B.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Language other = (Language) obj;
        if ((this.code_639_1 == null) ? (other.code_639_1 != null) : !this.code_639_1.equals(other.code_639_1)) {
            return false;
        }
        if ((this.code_639_2T == null) ? (other.code_639_2T != null) : !this.code_639_2T.equals(other.code_639_2T)) {
            return false;
        }
        if ((this.code_639_2B == null) ? (other.code_639_2B != null) : !this.code_639_2B.equals(other.code_639_2B)) {
            return false;
        }
        if ((this.nameEn == null) ? (other.nameEn != null) : !this.nameEn.equals(other.nameEn)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Object o) {
        int result = this.nameEn.compareTo(((Language) o).nameEn);
        if (result == 0) {
            result = this.code_639_2T.compareTo(((Language) o).code_639_2T);
        }
        if (result == 0) {
            result = this.code_639_2B.compareTo(((Language) o).code_639_2B);
        }
        if (result == 0) {
            result = this.code_639_1.compareTo(((Language) o).code_639_1);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Language{" + "code_639_1=" + code_639_1 + ", code_639_2T=" + code_639_2T + ", code_639_2B=" + code_639_2B + ", nameEn=" + nameEn + '}' + "\n";
    }
    
    
}
