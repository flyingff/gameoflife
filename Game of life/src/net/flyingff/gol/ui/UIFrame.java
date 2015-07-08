package net.flyingff.gol.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.swing.JFrame;

import net.flyingff.gol.func.CalcFunction;
import net.flyingff.gol.func.NewFuntionProvider;
import net.flyingff.gol.gif.AnimatedGifEncoder;

public class UIFrame extends JFrame{
	private static final long serialVersionUID = -549023984946427733L;
	private int width, height;
	private MyPanel pane;
	private boolean recording = false, pause = false;
	private CalcFunction cf;
	public UIFrame(int w, int h) {
		width = w;
		height = h;
		setTitle("Game of Life");
		getContentPane().add(pane = new MyPanel(width, height), BorderLayout.CENTER);
		setSize(w * 2,h * 2);
		setLocationByPlatform(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		enableInputMethods(false);
		addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent arg0) {	}
			@Override public void keyReleased(KeyEvent arg0) {}
			@Override
			public void keyPressed(KeyEvent e) {
				switch(e.getKeyCode()) {
				case KeyEvent.VK_R:
					if (!recording)
						init(); 
					break;
				case KeyEvent.VK_P:
					pause = !pause;
					break;
				case KeyEvent.VK_B:
					if (!recording) {
						setTitle("Game of Life - Recording...");
						pane.beginGif();
						init();
					} else {
						setTitle("Game of Life");
						pane.endGif();
					}
					recording = !recording;
					break;
				case KeyEvent.VK_F:
					pause = true;
					CalcFunction tmp = NewFuntionProvider.prompt();
					cf = tmp == null? cf: tmp;
					pause = false;
					break;
				case KeyEvent.VK_D:
					loadDefaultFunction();
					break;
				}
			}
		});
		loadDefaultFunction();
	}
	public void init(){
		pause = true;
		for(int i = 0; i < width; i++)
			for(int j = 0; j < height; j++) {
				pane.set(i, j, 0);
			}
		for(int i= width / 4; i < 3 * width / 4; i++) {
			for(int j = height / 4; j < 3 * height/ 4; j++)
				pane.set(i, j, Math.random() > 0.2? 0 : (int) (Math.random() * 8));
		}
		pane.flip();
		pause = false;
	}
	public void next(){
		if(pause || cf == null) return;
		int[] aliveNum = new int[7];
		for(int i = 0; i < width; i++) {
			for(int j = 0; j < height; j++) {
				for(int k = 0; k < aliveNum.length; k++) {
					aliveNum[k] = 0;
				}
				int istart = i > 0? i - 1 : i, iend = i + 1 < width? i + 1: i,
						jstart = j > 0? j - 1: j, jend = j + 1 < height? j + 1: j;
				for(int i0 = istart; i0 <= iend; i0++) {
					for(int j0 = jstart; j0 <= jend; j0++) {
						int k;
						if ((i0 != i || j0 != j) && (k = pane.get(i0, j0)) != 0) {
							aliveNum[k - 1] ++;
						}
					}
				}
				pane.set(i, j, cf.nextState(pane.get(i, j), aliveNum));
			}
		}
		pane.flip();
	}
	private void loadDefaultFunction(){
		InputStream is = getClass().getResourceAsStream("default.txt");
		StringBuilder sb = new StringBuilder();
		try {
			byte[] buf = new byte[4096];
			while(is.available() > 0) {
				sb.append(new String(buf, 0, is.read(buf)));
			}
			cf = NewFuntionProvider.prompt(sb.toString());
			is.close();
			if (cf == null) throw new NullPointerException();
		} catch (Exception e) {throw new RuntimeException(e);}
	}
	public static void main(String[] args) throws Exception{
		UIFrame f = new UIFrame(200, 160);
		f.init();
		for(;;){
			Thread.sleep(100);
			f.next();
		}
	}
}

class MyPanel extends Component {
	private static final long serialVersionUID = -6865887005589508294L;
	private final Color colorLine = new Color(251,52,30), colorCell[] = new Color[]{ 
		new Color(251,11,11),
		new Color(253,119,9),
		new Color(248,254,7),
		new Color(106,254,56),
		new Color(11,215,251),
		new Color(45,39,248),
		new Color(200,41,245)
		};
	private int rownum, colnum;
	private BufferedImage frame;
	private int[][] matrix, nextmatrix;
	private AnimatedGifEncoder e;
	private boolean shouldAdd = false;
	public MyPanel(int row, int col) {
		rownum = row;
		colnum = col;
		matrix = new int[row][col];
		nextmatrix = new int[row][col];
	}
	
	public void flip(){
		int[][] tmp = matrix;
		matrix = nextmatrix;
		nextmatrix = tmp;
		shouldAdd = true;
		repaint();
	}
	public int get(int x, int y) {
		return matrix[x][y];
	}
	public void set(int x, int y, int b) {
		nextmatrix[x][y] = b;
	}
	public void beginGif() {
		e = new AnimatedGifEncoder();
		e.setRepeat(0);
		e.start("D:\\gif\\" + System.currentTimeMillis() + ".gif");
	}
	public void endGif(){
		if (e != null){
			e.setDelay(1000);
			e.finish();
		}
		e = null;
	}
	
	@Override
	public void paint(Graphics gx) {
		int w = getWidth(), h = getHeight();
		if (frame == null|| frame.getWidth() != w || frame.getHeight() != h) {
			frame = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		}
		Graphics g = frame.getGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		
		for(int i = 0; i < rownum; i++) {
			for(int j = 0; j < colnum; j++) {
				if (matrix[i][j] != 0) {
					g.setColor(colorCell[matrix[i][j] - 1]);
					int x0 = i * w / rownum, y0 = j * h / colnum, x1 = (i + 1) * w / rownum, y1 = (j + 1) * h / colnum;
					g.fillRect(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
				}
			}
			
		}
		g.setColor(colorLine);
		
		/*for(int i = 0; i <= rownum; i++) {
			int y0 = i * h / rownum;
			g.drawLine(0, y0, w, y0);
		}
		for(int i = 0; i <= colnum; i++) {
			int x0 = i * w / colnum;
			g.drawLine(x0, 0, x0, h);
		}*/
		gx.drawImage(frame, 0, 0, null);
		if (shouldAdd && e != null) {
			e.addFrame(frame);
			e.setDelay(100);
			shouldAdd = false;
		}
	}
	@Override
	public void update(Graphics arg0) {
		paint(arg0);
	}
	
}
