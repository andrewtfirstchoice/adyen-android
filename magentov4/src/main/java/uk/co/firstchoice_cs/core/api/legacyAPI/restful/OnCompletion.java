package uk.co.firstchoice_cs.core.api.legacyAPI.restful;

/**
 * Created by steveparrish on 4/11/17.
 */
public interface OnCompletion<T1 extends BaseAsync, T2> {
    void onSuccess(T1 task, T2 result);

    void onFailure(T1 task, int errorCode, String errorMsg);
}
