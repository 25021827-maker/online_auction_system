package Service;

import FakeDB.FakeDB;
import Model.User;

public class UserService {

    public User login(String u, String p) {

        return FakeDB.getUser(u, p);

    }

    public String register(String u, String p) {

        if (FakeDB.exists(u)) {

            return "Username đã tồn tại";

        }

        FakeDB.addUser(u, p);

        return "OK";
    }
}
