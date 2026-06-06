package dto;

public class ResponsePayload {
    private String status; // "SUCCESS" hoặc "FAIL"
    private String message;
    private String action; // Để báo xem đây là phản hồi của lệnh nào (VD: "LOGIN_RESPONSE")
    private Object data;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getAction() {
        return action;
    }

    public Object getData() {
        return data;
    }
}