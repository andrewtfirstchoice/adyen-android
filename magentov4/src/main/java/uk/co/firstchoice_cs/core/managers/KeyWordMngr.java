package uk.co.firstchoice_cs.core.managers;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.co.firstchoice_cs.App;
import uk.co.firstchoice_cs.core.api.legacyAPI.models.Manufacturer;
import uk.co.firstchoice_cs.core.api.legacyAPI.models.Model;



/**
 * Created by steveparrish on 4/7/17.
 */
public class KeyWordMngr {

    private static final String DEFAULT_KEYWORD_FILE_PATH = App.docDir + "/keywords.json";

    private static KeyWordMngr singleton;
    static private Comparator sortLastViewed = (Comparator<SearchTerm>) (one, two) -> {
        // Descending, two - one
        long value = (two.getLastSearched() - one.getLastSearched());
        if (value == 0L) return 0;
        if (value > 0L) return 1;
        return -1;
    };
    private Search searchKeywords;

    private KeyWordMngr() {
        searchKeywords = new Search();
    }

    public static synchronized KeyWordMngr getInstance() {
        if (singleton == null) {
            singleton = new KeyWordMngr();
        }
        return singleton;
    }

    public SearchTerm update(final String keyword) {
        SearchTerm st = new SearchTerm(keyword);
        return searchKeywords.update(st);
    }

    public SearchTerm update(final SearchTerm searchTerm) {
        return searchKeywords.update(searchTerm);
    }

    public void remove(SearchTerm searchTerm) {
        searchKeywords.remove(searchTerm);
    }

    public List<SearchTerm> getAllKeywords() {
        List<SearchTerm> sortedList = new ArrayList<>(searchKeywords.keywords.keywords);
        Collections.sort(sortedList, sortLastViewed);
        return sortedList;
    }

    static public class SearchTerm {
        private Source source = Source.UNKNOWN;
        private String word = "";
        private long lastSearched = 0;
        private Model model = new Model();
        private Manufacturer manufacturer = new Manufacturer();
        public SearchTerm() {

        }

        public SearchTerm(String word) {
            this.word = word.replace("\"", "").trim();
            source = Source.UNKNOWN;
            lastSearched = System.currentTimeMillis();
        }

        public SearchTerm(String word, Source keywordSource) {
            this(word);
            source = keywordSource;
        }

        public SearchTerm(final Manufacturer man, final Model model) {
            this(man.Name + " " + model.Name);
            this.model = model;
            this.manufacturer = man;
            source = Source.MAN_MODEL;
        }

        void updateLastSearched() {
            lastSearched = System.currentTimeMillis();
        }

        long getLastSearched() {
            return lastSearched;
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof SearchTerm) {
                return (word.equals(((SearchTerm) o).word));
            }
            return false;
        }

        @Override
        public int hashCode() {
            return word.hashCode();
        }

        @NotNull
        @Override
        public String toString() {
            if (source != Source.MAN_MODEL && source != Source.SCANNER) {
                return "\"" + word + "\"";
            }
            return word;
        }

        public String getWord() {
            return word;
        }

        public boolean isManModel() {
            return (source == Source.MAN_MODEL);
        }

        public Source getSource() {
            return source;
        }

        public Model getModel() {
            return model;
        }

        public Manufacturer getManufacturer() {
            return manufacturer;
        }

        public enum Source {
            UNKNOWN,
            MAN_MODEL,
            SCANNER
        }
    }

    static private class Keywords {
        private List<SearchTerm> keywords = new ArrayList<SearchTerm>();

        public SearchTerm add(final SearchTerm searchTerm) {
            SearchTerm st = getSearchTerm(searchTerm);
            if (st == null) {
                keywords.add(searchTerm);
                return searchTerm;
            }
            st.updateLastSearched();
            return st;
        }

        public SearchTerm getSearchTerm(SearchTerm searchTerm) {
            for (SearchTerm st : keywords) {
                if (st.equals(searchTerm)) {
                    return st;
                }
            }
            return null;
        }

        public void remove(SearchTerm searchTerm) {
            // Save to file system
            keywords.remove(searchTerm);
        }
    }

    static private class Search {
        private Keywords keywords;

        private Search() {
            keywords = create();
        }

        static private Keywords create() {
            File file = new File(DEFAULT_KEYWORD_FILE_PATH);
            InputStream input = null;
            try {
                input = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
                return new Keywords();
            }
            Gson gson = new Gson();
            Reader reader = new InputStreamReader(input);
            Keywords keywords;
            try {
                keywords = gson.fromJson(reader, Keywords.class);
            } catch (Exception err) {
                // Do nothing
                return new Keywords();
            }
            if (keywords == null) {
                keywords = new Keywords();
            }
            return keywords;
        }

        public SearchTerm update(SearchTerm searchTerm) {
            // Save to file system
            SearchTerm ret = keywords.add(searchTerm);
            writeKeywords();
            return ret;
        }

        public void remove(SearchTerm searchTerm) {
            // Save to file system
            keywords.remove(searchTerm);
            writeKeywords();
        }

        public List<SearchTerm> getAllKeywords() {
            return keywords.keywords;
        }

        private void writeKeywords() {
            File file = new File(App.docDir);
            if (!file.exists()) {
                file.mkdirs();
            }
            // Save to file system
            file = new File(DEFAULT_KEYWORD_FILE_PATH);
            Gson gson = new Gson();
            try {
                Writer writer = new FileWriter(file);
                String json = gson.toJson(keywords);
                writer.write(json);
                writer.close();
            } catch (IOException e) {
                // Do nothing
            }
        }
    }
}
