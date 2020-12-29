package uk.co.firstchoice_cs.core.shared;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.Objects;

import uk.co.firstchoice_cs.core.helpers.Helpers;
import uk.co.firstchoice_cs.core.managers.KeyWordMngr;
import uk.co.firstchoice_cs.core.widgets.KeyBoardEditText;
import uk.co.firstchoice_cs.firstchoice.R;

public class ManualsSearchBarView extends LinearLayout {

    private KeyBoardEditText mSearchText;
    private ImageView mClearButton;
    private OnFocusChangeListener focusChangeListener;
    private OnSearchInputListener requestListener;
    private OnClickListener cleanButtonAction = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Objects.requireNonNull(mSearchText.getText()).clear();
            if (requestListener != null) {
                requestListener.onClear();
            }
        }
    };
    private OnClickListener voiceSearchAction = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (requestListener != null) {
                notifySearchKeyword();
                requestListener.onSearchClicked();
            }
        }
    };


    public ManualsSearchBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode())
            init(context, attrs);
    }

    public ManualsSearchBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode())
            init(context, attrs);
    }

    public void setText(String searchParam) {
        mSearchText.setText(searchParam);
    }

    public void setOnFocusChangeListener(final OnFocusChangeListener listener) {
        focusChangeListener = listener;
    }

    public void setOnSearchInputRequest(final OnSearchInputListener listener) {
        requestListener = listener;
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(getContext(), R.layout.manuals_search_bar, this);
        TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.ManualsSearchBarView, 0, 0);
        String hint = styledAttributes.getString(R.styleable.ManualsSearchBarView_hintText);
        styledAttributes.recycle();
        mSearchText = findViewById(R.id.etSearch);

        ImageView mSearchButton = findViewById(R.id.btnSearch);
        mClearButton = findViewById(R.id.btnClear);
        mSearchButton.setImageResource(R.drawable.ic_magnify_grey600_24dp);
        mSearchButton.setOnClickListener(voiceSearchAction);
        mClearButton.setImageResource(R.drawable.ic_clear_black_18dp);
        mClearButton.setOnClickListener(cleanButtonAction);
        if (hint != null)
            mSearchText.setHint(hint);
        setupSearchText();
        getText();
    }
    public boolean keyboardShowing = false;
    private void setupSearchText() {

        mSearchText.setOnFocusChangeListener((view, isFocused) -> {
            if (!isFocused) {
                Helpers.hideKeyboard( mSearchText);
            }
            if (focusChangeListener != null) {
                focusChangeListener.onFocusChange(view, isFocused);
            }
            keyboardShowing = isFocused;
        });

        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getText();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mSearchText.setOnBackButtonListener((showing) -> {
            if (mSearchText.hasFocus()) {
                mSearchText.clearFocus();
                return true;
            }
            return false;
        });

        mSearchText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                notifySearchKeyword();
                return true;
            }
            return false;
        });
        mSearchText.clearFocus();
    }

    KeyWordMngr.SearchTerm getSearchTerm() {
        String text = Objects.requireNonNull(mSearchText.getText()).toString().trim();
        if(text.isEmpty())
            return null;
        else
            return new KeyWordMngr.SearchTerm(text);
    }

    private void getText() {
        String text = Objects.requireNonNull(mSearchText.getText()).toString().trim();
        if (text.length() > 0) {
            mClearButton.setVisibility(View.VISIBLE);
            if (requestListener != null) {
                KeyWordMngr.SearchTerm st = new KeyWordMngr.SearchTerm(text);
                requestListener.onKeywordRequest(st);
            }
        } else {
            mClearButton.setVisibility(View.GONE);
            if (requestListener != null)
                requestListener.onClear();
        }
    }

    private void notifySearchKeyword() {
        String keyword = Objects.requireNonNull(mSearchText.getText()).toString().trim();
        if (!TextUtils.isEmpty(keyword)) {

            KeyWordMngr.SearchTerm st = new KeyWordMngr.SearchTerm(keyword);

            mSearchText.clearFocus();
            if (requestListener != null) {
                requestListener.onKeywordRequest(st);
                requestListener.onSearchClicked();
            }
        }

    }

    public interface OnSearchInputListener {
        void onKeywordRequest(final KeyWordMngr.SearchTerm keyword);
        void onSearchClicked();
        void onClear();
    }
}
