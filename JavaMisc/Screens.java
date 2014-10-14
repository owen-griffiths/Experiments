package com.omg;

import java.awt.*;

public class Main {

    public static void main(String[] args) {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = environment.getScreenDevices();
        System.out.printf("%d screens\n", screens.length);

        for (GraphicsDevice s : screens) {
            System.out.printf("Screen %s is in mode %s\n", s.getIDstring(), formatMode(s.getDisplayMode()));
            System.out.printf("Bounds %s\n", s.getDefaultConfiguration().getBounds());
        }
    }

    private static String formatMode(DisplayMode mode) {
        return String.format("%d x %d", mode.getWidth(), mode.getHeight());
    }

}
