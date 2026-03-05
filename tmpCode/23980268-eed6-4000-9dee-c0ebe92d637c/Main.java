import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (sc.hasNextInt()) {
            int n = sc.nextInt();
            int candidate = 0;
            int count = 0;
            for (int i = 0; i < n; i++) {
                int num = sc.nextInt();
                if (count == 0) {
                    candidate = num;
                }
                count += (num == candidate) ? 1 : -1;
            }
            System.out.println(candidate);
        }
    }
}
