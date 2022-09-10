package auto.qinglong.api.res;

import java.util.ArrayList;
import java.util.List;

import auto.qinglong.api.object.Task;
import auto.qinglong.api.object.TaskState;

public class TasksRes {
    private int code;
    private String message;
    private List<Task> data;


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    /**
     * @return 排序：运行>顶置>空闲>禁止
     */
    public List<Task> getData() {
        return data;
    }

    public void setData(List<Task> data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
