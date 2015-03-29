

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;

import org.w3c.dom.css.Rect;

public class RobotHelper {
	private static final int customMS = 10;
	
	/**
     * 鼠标单击（左击）,要双击就连续调用
     * 
     * @param robot
     * @param x
     *            x坐标位置
     * @param y
     *            y坐标位置
     * @param delay
     *            该操作后的延迟时间
     */
	public static void cilckLMouse(Robot robot, int x, int y, int delay) {
		robot.mouseMove(x, y);
		robot.mousePress(InputEvent.BUTTON1_MASK);
		robot.delay(customMS);
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
		robot.delay(delay);
	}
	
	/**
     * 鼠标右击,要双击就连续调用
     * 
     * @param robot
     * @param x
     *            x坐标位置
     * @param y
     *            y坐标位置
     * @param delay
     *            该操作后的延迟时间
     */
	public static void clickRMouse(Robot robot, int x, int y, int delay) {
		robot.mouseMove(x, y);
		robot.mousePress(InputEvent.BUTTON2_MASK);
		robot.delay(customMS);
		robot.mouseRelease(InputEvent.BUTTON2_MASK);
		robot.delay(delay);
	}
	
	/**
     * 键盘输入（一次只能输入一个字符）
     * 
     * @param r
     * @param ks
     *            键盘输入的字符数组
     * @param delay
     *            输入一个键后的延迟时间
     */
	public static void pressKey(Robot robot, int[] keys, int delay) {
		for(int i=0; i<keys.length; i++) {
			robot.keyPress(keys[i]);
			robot.delay(customMS);
			robot.keyRelease(keys[i]);
			robot.delay(delay);
		}
	}
	
	/**
     * 捕捉全屏慕
     * 
     * @param r
     * @return
     */
	public static  BufferedImage captureWholeScreen(Robot robot, int delay) {
		Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		BufferedImage image = robot.createScreenCapture(screenRect);
		robot.delay(delay);
		return image;
	}
	
	/**
     * 捕捉屏幕的一个矫形区域
     * 
     * @param r
     * @param x
     *            x坐标位置
     * @param y
     *            y坐标位置
     * @param width
     *            矩形的宽
     * @param height
     *            矩形的高
     * @return
     */
	public static BufferedImage capturePartScreen(Robot robot, int startX, int startY, int width, int height, int delay) {
		Rectangle screenRect = new Rectangle(startX, startY, width, height);
//		robot.mouseMove(startX, startY);
//		robot.delay(customMS);
//		BufferedImage image = robot.createScreenCapture(new Rectangle(width, height));
		BufferedImage image = robot.createScreenCapture(screenRect);
		robot.delay(delay);
		return image;
	}
}
