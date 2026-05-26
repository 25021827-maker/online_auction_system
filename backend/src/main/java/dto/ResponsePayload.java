package dto;

public class ResponsePayload {
    private String status; // "SUCCESS" hoặc "ERROR"
    private Object data;
    private String correlationId;

    public ResponsePayload(String status, Object data) {
        this.status = status;
        this.data = data;
    }
    // getters & setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}