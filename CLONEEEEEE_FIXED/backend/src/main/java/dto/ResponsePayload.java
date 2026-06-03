package dto;

public class ResponsePayload {
    private String status;
    private String message;
    private String action; // Trường action để Frontend biết luồng nào
    private Object data;

    public ResponsePayload(String status, String message, String action, Object data) {
        this.status = status;
        this.message = message;
        this.action = action;
        this.data = data;
    }

    public static ResponsePayload success(String action, String message, Object data) {
        return new ResponsePayload("SUCCESS", message, action, data);
    }

    public static ResponsePayload fail(String action, String message) {
        return new ResponsePayload("FAIL", message, action, null);
    }

    // Các hàm Getter bắt buộc phải có để Gson chuyển đổi thành JSON
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getAction() { return action; }
    public Object getData() { return data; }
}