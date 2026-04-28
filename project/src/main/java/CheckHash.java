import com.bcsport.admin.util.PasswordUtil;

public class CheckHash {
    public static void main(String[] args) {
        String pass = "admin123";
        String salt = "bcsport";
        System.out.println("ResultHash:" + PasswordUtil.encryptPassword(pass, salt));
    }
}
