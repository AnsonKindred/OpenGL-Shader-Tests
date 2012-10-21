import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.IntBuffer;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GL2;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import com.jogamp.opengl.util.FPSAnimator;

public class GLRenderer extends GLCanvas implements GLEventListener, WindowListener
{
	private static final int NUM_THINGS = 100000;
	
	public float viewWidth, viewHeight;
	
	private FPSAnimator animator;
	
	private boolean didInit = false;
	private long startDrawTime = 0;
	private long numDrawIterations = 0;
	
	JFrame the_frame;
	DirtGeometry geometry;
	
	float[] positions = new float[NUM_THINGS*2];
	
	// Shader attributes
	private int shaderProgram, mvpAttribute, positionAttribute;
	
	public static void main(String[] args) 
    {
		new GLRenderer();
    }
	
	public GLRenderer()
	{
		// setup OpenGL Version 2
		super(new GLCapabilities(GLProfile.get(GLProfile.GL2)));
		
		addGLEventListener(this);
		setSize(1800, 1000);
				
	    the_frame = new JFrame("Hello World");
	    the_frame.getContentPane().add(this);
	    the_frame.setSize(the_frame.getContentPane().getPreferredSize());
	    the_frame.setVisible(true);
	    the_frame.addWindowListener(this);
		
		animator = new FPSAnimator(this, 60);
		animator.start();
	}
	
	// Called by the drivers when the gl context is first made available
	public void init(GLAutoDrawable d)
	{		
		final GL2 gl = d.getGL().getGL2();
		
		shaderProgram = ShaderLoader.compileProgram(gl, "default");
        
        gl.glLinkProgram(shaderProgram);
        
        positionAttribute = gl.glGetAttribLocation(shaderProgram, "position");
        mvpAttribute = gl.glGetUniformLocation(shaderProgram, "mvp");
        
		gl.glClearColor(0f, 0f, 0f, 1f);
		
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL2.GL_BLEND);
		
		boolean VBOsupported = gl.isFunctionAvailable("glGenBuffersARB") && gl.isFunctionAvailable("glBindBufferARB")
				&& gl.isFunctionAvailable("glBufferDataARB") && gl.isFunctionAvailable("glDeleteBuffersARB");
		
		System.out.println("VBO Supported: " + VBOsupported);
		
		geometry = DirtGeometry.getInstance(.1f);
		geometry.buildGeometry(viewWidth, viewHeight);
		geometry.finalizeGeometry();
		
		int bytesPerFloat = Float.SIZE / Byte.SIZE;
	    int numBytes = geometry.vertices.length * bytesPerFloat;
	    
		IntBuffer vertexBufferID = IntBuffer.allocate(1);
		gl.glGenBuffers(1, vertexBufferID);
		geometry.vertexBufferID = vertexBufferID.get(0);
		
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, geometry.vertexBufferID);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, numBytes, geometry.vertexBuffer, GL2.GL_STATIC_DRAW);
		gl.glVertexAttribPointer(positionAttribute, 2, GL2.GL_FLOAT, false, 0, 0);
	    gl.glEnableVertexAttribArray(positionAttribute);
	    
		geometry.needsCompile = false;
	}
	
	// Called by me on the first resize call, useful for things that can't be initialized until the screen size is known
	public void viewInit(GL2 gl)
	{
		for(int i = 0; i < NUM_THINGS; i++)
		{
			positions[i*2] = (float) (Math.random()*viewWidth);
			positions[i*2+1] = (float) (Math.random()*viewWidth);
		}
	}
	
	public void display(GLAutoDrawable d)
	{
		if(numDrawIterations == 0)
		{
			startDrawTime = System.currentTimeMillis();
		}
		final GL2 gl = d.getGL().getGL2();
		
		gl.glUseProgram(shaderProgram);
       
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		for(int i = 0; i < NUM_THINGS; i++)
		{
			_render(gl, geometry, positions[i*2], positions[i*2+1]);
		}
		
		numDrawIterations ++;
		if(numDrawIterations > 1)
		{
			long totalDrawTime = System.currentTimeMillis() - startDrawTime;
			System.out.println(totalDrawTime / numDrawIterations);
			numDrawIterations = 0;
		}
	}
	
	public void reshape(GLAutoDrawable d, int x, int y, int width, int height)
	{
		final GL2 gl = d.getGL().getGL2();
		gl.glViewport(0, 0, width, height);
		float ratio = (float) height / width;
		
		viewWidth = 100;
		viewHeight = viewWidth * ratio;
		
		Matrix3x3.ortho(0, viewWidth, 0, viewHeight);
		
		if (!didInit)
		{
			viewInit(gl);
			didInit = true;
		}
	}
	
	public void _render(GL2 gl, Geometry geometry, float x, float y)
	{
		Matrix3x3.push();
		
		Matrix3x3.translate(x, y);
	    
        gl.glUniformMatrix3fv(mvpAttribute, 1, false, Matrix3x3.getMatrix());
        
		gl.glDrawArrays(geometry.drawMode, 0, geometry.getNumPoints());
		
		Matrix3x3.pop();
	}
	
	@Override
	public void dispose(GLAutoDrawable drawable){}
	
	public float getViewWidth()
	{
		return viewWidth;
	}
	
	public float getViewHeight()
	{
		return viewHeight;
	}

	@Override
	public void windowClosing(WindowEvent arg0)
	{
		animator.stop();
		System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent arg0){}
	@Override
	public void windowDeiconified(WindowEvent arg0){}
	@Override
	public void windowIconified(WindowEvent arg0){}
	@Override
	public void windowOpened(WindowEvent arg0){}
	@Override
	public void windowActivated(WindowEvent arg0){}
	@Override
	public void windowClosed(WindowEvent arg0){}
}