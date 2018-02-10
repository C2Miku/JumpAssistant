/**
 * This cheating program works by getting screenshots via ADB and analysing colors in these screenshots.
 * Do not put it in commercial usages.
 * Author: cSquared(GitHub ID: C2Miku)
 * Creating date: 2018/2/6
 */

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import javax.imageio.ImageIO;

public class JumpAssistant {

    private final static String ADB_ROUTE = "/Users/csquared/Library/Android/sdk/platform-tools";
    private final static String TEMP_ROUTE = "/Users/csquared/Desktop";
    private final static int SCREEN_HEIGHT = 1920;
    private final static int SCREEN_WIDTH = 1080;
    private final static double RATIO = 1.39;
    private final static int COLOR_DIFF_MAX = 20;

    private static int currentBlock = 0;
    private static String targetDevice = "";
    private static int distance = 0;

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);
        String[] deviceList = GetDevices();

        if(deviceList.length == 0) {
            System.out.println("Error: No device found!");
        } else {
            for (int i = 1; i <= deviceList.length; i++) {
                System.out.println("[" + i + "] " + deviceList[i - 1]);
            }
            System.out.print("Select a target device: ");
            try {
                int inputInt = input.nextInt();
                while (inputInt < 1 || inputInt > deviceList.length) {
                    System.out.print("Select a valid target device: ");
                    inputInt = input.nextInt();
                }
                targetDevice = deviceList[inputInt - 1];
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        input.close();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                currentBlock++;
                System.out.printf("======== BLOCK %d ========\n", currentBlock);
                ScreenSnap(targetDevice, TEMP_ROUTE);
                BufferedImage bufferedImage = LoadFile(TEMP_ROUTE + "/screenshot.png");
                Coordinate playerPosition = GetPlayerPosition(bufferedImage);
                System.out.println("playerPosition = " + playerPosition.getCoordinateString());
                Coordinate nextBlockPosition = GetNextBlockPosition(bufferedImage, playerPosition);
                System.out.println("nextBlockPosition = " + nextBlockPosition.getCoordinateString());
                distance = GetDistance(playerPosition, nextBlockPosition);
                System.out.println("distance = " + distance);
                ScreenTouch(targetDevice, (int)(distance * RATIO), GetRandomInt(SCREEN_WIDTH / 5, SCREEN_WIDTH / 5 * 4), GetRandomInt(SCREEN_HEIGHT / 5, SCREEN_HEIGHT / 5 * 4));
                System.out.println("======== COMPLETED ========");
            }
        }, 0,  + GetRandomInt(4000, 5000));

    }

    private static String[] GetDevices() {
        String command = ADB_ROUTE + "/adb devices";
        System.out.println("Running: " + command);
        ArrayList<String> devices = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = bufferedReader.readLine();
            while (line != null) {
                if (line.endsWith("device")) {
                    String device = line.substring(0, line.length() - "device".length()).trim();
                    devices.add(device);
                }
                line = bufferedReader.readLine();
            }
            process.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] devicesString = new String[devices.size()];
        for (int i = 1; i <= devices.size(); i++) {
            devicesString[i - 1] = devices.get(i - 1);
        }
        return devicesString;
    }

    private static void ScreenSnap(String device, String toPath) {
        CmdExecute(ADB_ROUTE + "/adb -s " + device + " shell screencap -p /sdcard/JumpTemp.png");
        CmdExecute(ADB_ROUTE + "/adb -s " + device + " pull /sdcard/JumpTemp.png " + toPath + "/screenshot.png");
        CmdExecute(ADB_ROUTE + "/adb -s " + device + " shell rm /sdcard/JumpTemp.png");
    }

    private static void ScreenTouch(String device, int time, int x, int y) {
        CmdExecute(ADB_ROUTE + "/adb -s " + device + " shell input swipe " + x + " " + y + " " + x + " " + y + " " + time);
    }

    private static void CmdExecute(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            System.out.println("Running: " + command);
            process.waitFor();
            process.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage LoadFile(String image) {
        File file = new File(image);
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bufferedImage;
    }

    private static RGB GetPixelColor(BufferedImage bufferedImage, int x, int y) {
        int pixelColor = bufferedImage.getRGB(x, y);
        int r = (pixelColor & 16711680) >> 16;
        int g = (pixelColor & 65280) >> 8;
        int b = (pixelColor & 255);
        return new RGB(r, g, b);
    }

    private static Coordinate GetPlayerPosition(BufferedImage bufferedImage) {
        int leftX = 0, rightX = 0;
        int positionX, positionY = 0;
        for(int x = 0; x < SCREEN_WIDTH; x++) {
            boolean breakFlag = false;
            for(int y = SCREEN_HEIGHT - 1; y > 0 ; y--) {
                RGB currentColor = GetPixelColor(bufferedImage, x, y);
                if(GetColorDiff(new RGB(43, 43, 73), currentColor) < COLOR_DIFF_MAX) {
                    positionY = y;
                    leftX = x;
                    breakFlag = true;
                    break;
                }
            }
            if(breakFlag) {
                break;
            }
        }
        for(int x = SCREEN_WIDTH - 1; x >= 0; x--) {
            RGB currentColor = GetPixelColor(bufferedImage, x, positionY);
            if(GetColorDiff(new RGB(60, 56, 83), currentColor) < COLOR_DIFF_MAX) {
                rightX = x;
                break;
            }
        }
        positionX = (leftX + rightX) / 2;
        return new Coordinate(positionX, positionY);
    }

    private static Coordinate GetNextBlockPosition(BufferedImage bufferedImage, Coordinate currentPosition) {
        if(currentPosition.x < SCREEN_WIDTH / 2) {
            int maxEdgeX = 0;
            int edgeContinuousLen = 0;
            boolean ifFound = false;
            for(int y = SCREEN_HEIGHT / 5; y < currentPosition.y && !ifFound; y++) {
                RGB currentRowBgColor = GetPixelColor(bufferedImage, 0, y);
                for(int x = SCREEN_WIDTH - 1; x > SCREEN_WIDTH / 2; x--) {
                    RGB currentPixelColor = GetPixelColor(bufferedImage, x, y);
                    if (GetColorDiff(currentPixelColor, currentRowBgColor) > 3) {
                        maxEdgeX = x;
                        RGB edgeColor = currentPixelColor;
                        int edgeX = x;
                        while(edgeColor.equals(currentPixelColor)) {
                            edgeX++;
                            currentPixelColor = GetPixelColor(bufferedImage, edgeX, y);
                            edgeContinuousLen++;
                        }
                        ifFound = true;
                        break;
                    }
                }
            }
            return new Coordinate((maxEdgeX + edgeContinuousLen / 2), (int)(- 0.5733 * currentPosition.x + 0.5733 * (maxEdgeX + edgeContinuousLen / 2) + currentPosition.y));
        } else {
            int maxEdgeX = 0;
            int edgeContinuousLen = 0;
            boolean ifFound = false;
            for(int y = SCREEN_HEIGHT / 5; y < currentPosition.y && !ifFound; y++) {
                RGB currentRowBgColor = GetPixelColor(bufferedImage, 0, y);
                for(int x = 0; x < SCREEN_WIDTH / 2; x++) {
                    RGB currentPixelColor = GetPixelColor(bufferedImage, x, y);
                    if (GetColorDiff(currentPixelColor, currentRowBgColor) > 3) {
                        maxEdgeX = x;
                        RGB edgeColor = currentPixelColor;
                        int edgeX = x;
                        while(edgeColor.equals(currentPixelColor)) {
                            edgeX++;
                            currentPixelColor = GetPixelColor(bufferedImage, edgeX, y);
                            edgeContinuousLen++;
                        }
                        ifFound = true;
                        break;
                    }
                }
            }
            return new Coordinate((maxEdgeX + edgeContinuousLen / 2), (int)(- 0.5733 * currentPosition.x + 0.5733 * (maxEdgeX + edgeContinuousLen / 2) + currentPosition.y));
        }
    }

    private static int GetColorDiff(RGB standardColor, RGB testColor) {
        return Math.abs(standardColor.r - testColor.r) + Math.abs(standardColor.g - testColor.g) + Math.abs(standardColor.b - testColor.b);
    }

    private static int GetDistance(Coordinate startPoint, Coordinate endPoint) {
        return (int)(Math.sqrt(Math.pow((startPoint.x - endPoint.x), 2) + Math.pow((startPoint.y - endPoint.y), 2)));
    }

    private static int GetRandomInt(int min, int max) {
        return (int)(min + Math.random() * (max - min));
    }

}