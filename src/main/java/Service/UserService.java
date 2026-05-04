package Service;
import FakeDB.FakeDB;
public class UserService {

    public boolean login(String u, String p) {
        return FakeDB.checkLogin(u, p);
    }

    public String register(String u, String p) {
        if (FakeDB.exists(u)) {
            return "Username đã tồn tại";
        }

        FakeDB.addUser(u, p);
        return "OK";
    }
}
