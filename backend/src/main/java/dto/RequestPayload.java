package dto;

public class RequestPayload {
    private String action;
    private String data;
    private String correlationId;  // thêm

    // constructor, getters, setters
    public RequestPayload(String action, String data) {
        this.action = action;
        this.data = data;
    }
    public String getAction() { return action; }
    public String getData() { return data; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}