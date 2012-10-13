import java.io.FileInputStream;
import java.io.IOException;

import javax.media.opengl.GL2;

public class ShaderLoader
{
	private static final String PATH = "shaders/";
	
	public static int compileProgram(GL2 gl, String shaderName)
	{
		// Create shaders
        // OpenGL ES retuns a index id to be stored for future reference.
        int vertexShader = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
        int fragmentShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
        
        String vertexShaderCode = "";
        String fragmentShaderCode = "";
		try
		{
			vertexShaderCode = readShaderFromFile(shaderName+".vertex");
			fragmentShaderCode = readShaderFromFile(shaderName+".fragment");
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
        
        compileShader(gl, vertexShaderCode, vertexShader);
        compileShader(gl, fragmentShaderCode, fragmentShader);

        int shaderProgram = gl.glCreateProgram();
        gl.glAttachShader(shaderProgram, vertexShader);
        gl.glAttachShader(shaderProgram, fragmentShader);
        
        return shaderProgram;
	}
	
	public static void compileShader(GL2 gl, String shaderCode, int shaderID)
	{
		String[] lines = new String[] { shaderCode };
        int[] vlengths = new int[] { lines[0].length() };
        gl.glShaderSource(shaderID, lines.length, lines, vlengths, 0);
        gl.glCompileShader(shaderID);
        
        //Check compile status.
        int[] compiled = new int[1];
        gl.glGetShaderiv(shaderID, GL2.GL_COMPILE_STATUS, compiled, 0);
        if(compiled[0] != 0)
        {
        	System.out.println("Horray! shader compiled");
        }
        else 
        {
            int[] logLength = new int[1];
            gl.glGetShaderiv(shaderID, GL2.GL_INFO_LOG_LENGTH, logLength, 0);

            byte[] log = new byte[logLength[0]];
            gl.glGetShaderInfoLog(shaderID, logLength[0], (int[])null, 0, log, 0);

            System.err.println("Error compiling the vertex shader: " + new String(log));
            System.exit(1);
        }
	}

	public static String readShaderFromFile(String filename) throws IOException
	{
		FileInputStream f = new FileInputStream(PATH+filename);
		
		int size = (int)(f.getChannel().size());
		byte[] source = new byte[size];
		
		f.read(source,0,size);
		
		f.close();
		
		return new String(source,0,size);
	}
}
