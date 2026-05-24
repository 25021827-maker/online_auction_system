package FakeDB;

import Model.User;
import Model.Product;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeDB {

    // =========================
    // USER
    // =========================
    public static List<User> users =
            new ArrayList<>();

    static {

        users.add(
                new User("admin", "123")
        );
    }

    public static void addUser(
            String u,
            String p
    ) {

        users.add(
                new User(u, p)
        );
    }

    public static boolean checkLogin(
            String u,
            String p
    ) {

        for (User user : users) {

            if (
                    user.username.equals(u)
                            &&
                            user.password.equals(p)
            ) {

                return true;
            }
        }

        return false;
    }

    public static User getUser(
            String u,
            String p
    ) {

        for (User user : users) {

            if (
                    user.username.equals(u)
                            &&
                            user.password.equals(p)
            ) {

                return user;
            }
        }

        return null;
    }

    public static User getUserByUsername(
            String username
    ) {

        for (User user : users) {

            if (
                    user.getUsername()
                            .equals(username)
            ) {

                return user;
            }
        }

        return null;
    }

    public static boolean exists(
            String u
    ) {

        for (User user : users) {

            if (
                    user.username.equals(u)
            ) {

                return true;
            }
        }

        return false;
    }

    // =========================
    // PRODUCT
    // =========================
    public static List<Product> products =
            new ArrayList<>();

    public static void addProduct(
            Product p
    ) {

        products.add(p);
    }

    // =========================
    // GET ALL PRODUCTS
    // =========================
    public static List<Product> getProducts() {

        return new ArrayList<>(products);
    }

    // =========================
    // FILTER STATUS
    // =========================
    public static List<Product> getByStatus(
            String status
    ) {

        return products.stream()

                .filter(p ->

                        p.getStatus()
                                .equals(status)

                )

                .collect(Collectors.toList());
    }

    // =========================
// GET PRODUCTS BY SELLER
// =========================
    public static List<Product> getProductsBySeller(
            String seller
    ) {

        return products.stream()

                .filter(p ->

                        p.getSeller()
                                .equals(seller)

                )

                .collect(Collectors.toList());
    }

    // =========================
// REMOVE PRODUCT
// =========================
    public static void removeProduct(
            Product product
    ) {

        products.remove(product);
    }


}