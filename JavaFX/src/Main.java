import ui.MainScreen;

import static javafx.application.Application.launch;

public class  Main {
    public static void main(String[] args) {
       MainScreen mainScreen = new MainScreen();
       mainScreen.launch(mainScreen.getClass(),args);
    }
}