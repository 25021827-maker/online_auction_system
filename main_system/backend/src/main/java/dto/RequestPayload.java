package dto;

public class RequestPayload {
    private String action;
    private String data;

    public RequestPayload(String action, String data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() { return action; }
    public String getData() { return data; }
}