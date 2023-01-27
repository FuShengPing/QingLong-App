package auto.qinglong.views.popup;

public class ListWindow<T> {
    private String title;
    private String cancelTip = "取消";
    private T adapter;
    private OnActionListener listener;


    public ListWindow() {

    }

    public ListWindow(String title) {
        this.title = title;
    }

    public ListWindow(String title, T adapter) {
        this.title = title;
        this.adapter = adapter;
    }

    public void setAdapter(T adapter) {
        this.adapter = adapter;
    }

    public T getAdapter() {
        return this.adapter;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCancelTip() {
        return cancelTip;
    }

    public void setCancelTip(String cancelTip) {
        this.cancelTip = cancelTip;
    }

    public OnActionListener getListener() {
        return listener;
    }

    public void setListener(OnActionListener listener) {
        this.listener = listener;
    }

    public interface OnActionListener {
        boolean onCancel();
    }
}