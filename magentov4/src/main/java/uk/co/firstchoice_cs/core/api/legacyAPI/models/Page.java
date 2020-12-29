package uk.co.firstchoice_cs.core.api.legacyAPI.models;

/**
 * Created by steveparrish on 9/1/17.
 */
public class Page {
    private int CurrentPage = 0;
    private int PageSize = 0;
    private int Returned = 0;

    public int getCurrentpage() {
        return CurrentPage;
    }

    public int getPagesize() {
        return PageSize;
    }

    public int getReturned() {
        return Returned;
    }

    public boolean isEnd() {
        if (PageSize == 0 || Returned == 0) return true;
        int pages = (Returned / PageSize);
        pages += ((Returned % PageSize) == 0) ? 0 : 1;
        // We have number of pages.  However the CurrentPage is zero based, so addBarCodeResult one.
        return ((CurrentPage + 1) >= pages);
    }

    public Page clone() {
        Page p = new Page();
        p.CurrentPage = CurrentPage;
        p.PageSize = PageSize;
        p.Returned = Returned;
        return p;
    }
}
